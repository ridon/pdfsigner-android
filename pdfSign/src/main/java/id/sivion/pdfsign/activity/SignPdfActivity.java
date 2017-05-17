package id.sivion.pdfsign.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.path.android.jobqueue.JobManager;
import com.tooltip.Tooltip;

import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x500.style.IETFUtils;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.CertificateCheckJob;
import id.sivion.pdfsign.job.DocumentSignPdfJob;
import id.sivion.pdfsign.job.GeoCoderJob;
import id.sivion.pdfsign.job.JobStatus;
import id.sivion.pdfsign.utils.GpsTracker;
import id.sivion.pdfsign.utils.NetworkUtil;
import id.sivion.pdfsign.utils.PermissionUtil;
import id.sivion.pdfsign.utils.TsaClient;

/**
 * Created by miftakhul on 20/10/16.
 */

public class SignPdfActivity extends AppCompatActivity {

    @BindView(R.id.pdfView)
    PDFView pdfView;
    @BindView(R.id.btn_sign)
    Button btnSign;
    @BindView(R.id.card_sign)
    CardView cardSign;

    private SharedPreferences preferences;

    private TextView textSignatureName, textLocationStatus;
    private EditText editLocationName;
    private DroidSignerApplication app;
    private JobManager jobManager;
    private ProgressDialog progressDialog;
    private AlertDialog signAlert;
    private GpsTracker gpsTracker;
    private Location currentLocation;

    private Uri pdfUri;
    private String name, reason, location;
    private boolean useTsa = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_pdf_activity);
        ButterKnife.bind(this);

        app = DroidSignerApplication.getInstance();
        jobManager = app.getJobManager();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.text_wait));
        progressDialog.setCancelable(false);

        pdfUri = getIntent().getParcelableExtra("pdfUri");

        pdfView.fromUri(pdfUri)
                .defaultPage(1)
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        failedOpenPdf();
                    }
                })
                .load();

        setupSignAlertDialog();

    }

    private void failedOpenPdf() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(R.string.text_failed_open_pdf);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBackPressed();
            }
        });
        builder.show();
    }

    private void setupSignAlertDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.popup_sign, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        final LinearLayout layoutOptions = (LinearLayout) view.findViewById(R.id.layout_sign_options);
        TextView signOptions = (TextView) view.findViewById(R.id.sign_options);
        textSignatureName = (TextView) view.findViewById(R.id.text_signer_name);
        textLocationStatus = (TextView) view.findViewById(R.id.text_location_status);
        editLocationName = (EditText) view.findViewById(R.id.edit_location);
        final Spinner spinReason = (Spinner) view.findViewById(R.id.spin_reason);
        CheckBox checkUseTsa = (CheckBox) view.findViewById(R.id.check_use_tsa);
        final Button btnTsaHelp = (Button) view.findViewById(R.id.btn_tsa_help);


        String[] reasons = new String[]{"Menyetujui Dokumen", "Telah membaca dan mengerti Dokumen", "Menyatakan Kebenaran Informasi"};
        final ArrayAdapter<String> reasonAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, reasons);
        spinReason.setAdapter(reasonAdapter);

        int selectedReason = preferences.getInt("selected_reason", 0);
        spinReason.setSelection(selectedReason);

        spinReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor edit = preferences.edit();
                edit.putInt("selected_reason", position);
                edit.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        signOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (layoutOptions.getVisibility() == View.VISIBLE) {
                    layoutOptions.setVisibility(View.GONE);
                } else if (layoutOptions.getVisibility() == View.GONE) {
                    layoutOptions.setVisibility(View.VISIBLE);
                }

            }
        });

        checkUseTsa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                useTsa = checked;
            }
        });

        btnTsaHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int backgroundColor = ContextCompat.getColor(SignPdfActivity.this, R.color.colorGrey);
                int textColor = ContextCompat.getColor(SignPdfActivity.this, android.R.color.white);

                new Tooltip.Builder(btnTsaHelp).setText("Waktu saat ini yang didapat dari server")
                        .setBackgroundColor(backgroundColor)
                        .setDismissOnClick(true)
                        .setTextColor(textColor)
                        .setGravity(Gravity.TOP)
                        .show();
            }
        });

        builder.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_key, null));
        builder.setTitle(R.string.app_name);
        builder.setPositiveButton(R.string.sign, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                name = textSignatureName.getText().toString();
                location = editLocationName.getText().toString();

                jobManager.addJobInBackground(DocumentSignPdfJob.
                        newInstance(pdfUri,
                                name,
                                spinReason.getSelectedItem().toString(),
                                location, useTsa));

            }
        });

        signAlert = builder.create();

    }

    @OnClick(R.id.btn_sign)
    public void onButtonClick(Button button) {
        requestSign();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (gpsTracker != null) {
            gpsTracker.stopUsingGps();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    requestSign();
                } else {
                    PermissionUtil.isLocationGranted(this);
                }
            } else {
                PermissionUtil.isLocationGranted(this);
            }
        }
    }

    public void onEventMainThread(DocumentSignPdfJob.DocumentSignEvent event) {

        if (event.getStatus() == JobStatus.ADDED) {
            progressDialog.show();

        } else if (event.getStatus() == JobStatus.SUCCESS) {
            progressDialog.dismiss();
            dialogSuccess(event.getFilePath());

        } else if (event.getStatus() == JobStatus.ABORTED) {
            progressDialog.dismiss();
            Toast.makeText(this, "Gagal menandatangani dokumen", Toast.LENGTH_SHORT).show();
        }

    }

    public void onEventMainThread(CertificateCheckJob.CheckEvent event) {
        if (event.getStatus() == CertificateCheckJob.CheckEvent.VALID) {
            if (useTsa && !NetworkUtil.isConnected(SignPdfActivity.this)) {
                dialogNoInternet();
                return;
            }

            signAlert.show();
            if (currentLocation != null) {
                jobManager.addJobInBackground(new GeoCoderJob(currentLocation));
            } else {
                Log.d(getClass().getSimpleName(), "current location null");
            }

            try {
                X509Certificate cert = (X509Certificate) event.getObject();
                X500Name x500Name = new JcaX509CertificateHolder(cert).getSubject();
                RDN cn = x500Name.getRDNs(BCStyle.CN)[0];

                textSignatureName.setText(IETFUtils.valueToString(cn.getFirst().getValue()));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }

        } else {
            dialogCertificateInValid();
        }
    }

    public void onEventMainThread(TsaClient.TsaEvent event) {
        if (event.getStatus() == TsaClient.TsaEvent.FAILED) {
            dialogTsaFailed();
        }
    }

    public void onEventMainThread(GeoCoderJob.GeoEvent event) {
        if (event.getJobStatus() == JobStatus.ADDED) {
            textLocationStatus.setVisibility(View.VISIBLE);
        }

        if (event.getJobStatus() == JobStatus.SUCCESS) {
            editLocationName.setText(event.getCity());
            textLocationStatus.setVisibility(View.GONE);
        }

        if (event.getJobStatus() == JobStatus.ABORTED) {
            textLocationStatus.setVisibility(View.GONE);
        }
    }

    public void onEventMainThread(GpsTracker.GPSEvent event) {
        Log.d(getClass().getSimpleName(),"new Location available ");
        currentLocation = event.getLocation();
        if (currentLocation != null) {
            jobManager.addJobInBackground(new GeoCoderJob(currentLocation));
        }
    }


    private void requestSign() {
        if (PermissionUtil.isLocationGranted(this)) {
            Log.d(getClass().getSimpleName(), "all permission granted");

            gpsTracker = new GpsTracker(this);
            if (gpsTracker.isCanGetLocation()) {
                Log.d(getClass().getSimpleName(), "gps track available");

                currentLocation = gpsTracker.getLocation();
                if (currentLocation != null) {
                    Log.d(getClass().getSimpleName(), "latitude : " + currentLocation.getLatitude());
                    Log.d(getClass().getSimpleName(), "longitude : " + currentLocation.getLongitude());
                }

                jobManager.addJobInBackground(new CertificateCheckJob());
            } else {
                Log.d(getClass().getSimpleName(), "gps track not available");
                gpsTracker.showSettingsAlert();
            }
        }
    }


    private void dialogCertificateInValid() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.text_certificate);
        builder.setIcon(R.drawable.ic_certificate);
        builder.setMessage(R.string.text_certificate_invalid);
        builder.setPositiveButton(getString(R.string.text_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void dialogTsaFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.text_certificate);
        builder.setIcon(R.drawable.ic_certificate);
        builder.setMessage(R.string.text_tsa_connection_failed);
        builder.setPositiveButton(getString(R.string.text_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void dialogNoInternet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.text_no_internet);
        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void dialogSuccess(final String filePath) {

        View view = LayoutInflater.from(this).inflate(R.layout.popup_sign_success, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();

        TextView pathInfo = (TextView) view.findViewById(R.id.text_path_info);
        Button buttonClose = (Button) view.findViewById(R.id.btn_close);

        pathInfo.setText(getString(R.string.text_document_saved_in) + " " + filePath);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                setResult(RESULT_OK);
                finish();

            }
        });

        Button btnShare = (Button) view.findViewById(R.id.btn_share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.dismiss();
                setResult(RESULT_OK);
                finish();

                File pdfFile = new File(filePath);
                Uri uri = Uri.fromFile(pdfFile);


                Intent iShare = new Intent(Intent.ACTION_SEND);
                iShare.putExtra(Intent.EXTRA_STREAM, uri);
                iShare.setType("application/pdf");
                startActivity(Intent.createChooser(iShare, getString(R.string.text_share)));

            }
        });

        dialog.show();
    }


}
