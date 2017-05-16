package id.sivion.pdfsign.verification;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.path.android.jobqueue.JobManager;
import com.tooltip.Tooltip;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x500.style.IETFUtils;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.activity.SignPdfActivity;
import id.sivion.pdfsign.job.CertificateChainJob;
import id.sivion.pdfsign.job.CertificateCheckJob;
import id.sivion.pdfsign.job.DocumentSignPdfJob;
import id.sivion.pdfsign.job.JobStatus;
import id.sivion.pdfsign.utils.NetworkUtil;
import id.sivion.pdfsign.utils.TsaClient;

/**
 * Created by miftakhul on 01/12/16.
 */

public class PdfView extends AppCompatActivity implements KeyChainAliasCallback {

    @BindView(R.id.pdf_view)
    PDFView pdfView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.icon_info)
    ImageView iconInfo;
    @BindView(R.id.signature_info)
    RelativeLayout signatureInfo;

    private TextView editSignatureName;
    private Button btnBrowseCertificate;
    private TextView textSubjectdn;
    private TextView textIssuer;
    private TextView textExpire;
    private LinearLayout layoutCertificateInfo;
    private Button btnNext;

    private SharedPreferences preferences;
    private ProgressDialog progressDialog;
    private JobManager jobManager;
    private AlertDialog signDialog, requestSignDialog;

    private Uri pdfUri;
    private String name, reason, location;
    private boolean useTsa = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_view);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        pdfUri = getIntent().getParcelableExtra("pdfUri");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.text_wait));
        progressDialog.setCancelable(false);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        jobManager = DroidSignerApplication.getInstance().getJobManager();


        // receive file frome other application
        if (getIntent().getAction() == Intent.ACTION_VIEW){
            pdfUri = getIntent().getData();
        }else if (getIntent().getAction() == Intent.ACTION_SEND) {
            pdfUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

        }

        if (pdfUri != null) {
            openPdf(pdfUri);
        }

        setupSignAlertDialog();

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
    }


    @Override
    public void alias(String alias) {
        preferences.edit().putString("alias", alias).apply();

        jobManager.addJobInBackground(new CertificateChainJob(alias));
    }


    private void openPdf(Uri uri){

        pdfView.fromUri(uri)
                .defaultPage(1)
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        failedOpenPdf();
                    }
                })
                .load();

        checkSignature(uri);

    }

    private void failedOpenPdf(){
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

    public void showSignatureInfo(View view) {

        if (NetworkUtil.isConnected(this)) {

            Intent i = new Intent(this, SignDetail.class);
            i.putExtra("pdfUri", pdfUri);
            startActivity(i);
        }else {
            dialogNoInternet();
        }
    }

    private void checkSignature(Uri uri){
        try {
            InputStream documentUri = getContentResolver().openInputStream(uri);
            PDDocument pdDocument = PDDocument.load(documentUri);
            if (!pdDocument.getSignatureDictionaries().isEmpty()){
                signatureInfo.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSign(View view){
        View viewLayout = LayoutInflater.from(this).inflate(R.layout.certificate_popup, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(viewLayout);

        requestSignDialog = builder.create();

        textSubjectdn = (TextView) viewLayout.findViewById(R.id.text_subjectdn);
        textIssuer = (TextView) viewLayout.findViewById(R.id.text_issuer);
        textExpire = (TextView) viewLayout.findViewById(R.id.text_expire);
        layoutCertificateInfo = (LinearLayout) viewLayout.findViewById(R.id.layout_certificate_info);
        btnNext = (Button) viewLayout.findViewById(R.id.btn_next_step);

        String getAlias = preferences.getString("alias", "");

        if (!getAlias.equalsIgnoreCase("")){
            jobManager.addJobInBackground(new CertificateChainJob(getAlias));
        }

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestSignDialog.dismiss();
                jobManager.addJobInBackground(new CertificateCheckJob());

            }
        });

        requestSignDialog.show();

    }

    public void browseCertificate(View view){
        if (Build.VERSION.SDK_INT <= 22) {
            Log.d(getClass().getSimpleName(), "loli");
            KeyChain.choosePrivateKeyAlias(this, this, null, null, null, -1, null);
        } else {
            Log.d(getClass().getSimpleName(), "masrs");
            KeyChain.choosePrivateKeyAlias(this, this, null, null, null, null);
        }
    }

    public void setupSignAlertDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.popup_sign, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        final LinearLayout layoutOptions = (LinearLayout) view.findViewById(R.id.layout_sign_options);
        TextView signOptions = (TextView) view.findViewById(R.id.sign_options);
        final TextView editName = (TextView) view.findViewById(R.id.edit_name);
        final EditText editLocation = (EditText) view.findViewById(R.id.edit_location);
        final Spinner spinReason = (Spinner) view.findViewById(R.id.spin_reason);
        CheckBox checkUseTsa = (CheckBox) view.findViewById(R.id.check_use_tsa);
        final Button btnTsaHelp = (Button) view.findViewById(R.id.btn_tsa_help);
        editSignatureName = editName;

        String[] reasons = new String[]{"Menyetujui Dokumen","Telah membaca dan mengerti Dokumen","Menyatakan Kebenaran Informasi"};
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
                int backgroundColor = ContextCompat.getColor(PdfView.this, R.color.colorGrey);
                int textColor = ContextCompat.getColor(PdfView.this, android.R.color.white);

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

                name = editName.getText().toString();
                location = editLocation.getText().toString();

                jobManager.addJobInBackground(DocumentSignPdfJob.
                        newInstance(pdfUri,
                                name,
                                spinReason.getSelectedItem().toString(),
                                location, useTsa));
            }
        });


        signDialog = builder.create();

    }

    private void dialogNoInternet(){
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


    public void onEventMainThread(CertificateChainJob.CertificateChainEvent event) {
        if (event.getStatus() == JobStatus.SUCCESS) {

            textSubjectdn.setText(event.getSubjectDN());
            textIssuer.setText(event.getIssuer());
            textExpire.setText(event.getExpire());

            Log.d(getClass().getSimpleName(), " subject " + event.getSubjectDN() + " serial number " + event.getSerialNumber().toString());

            layoutCertificateInfo.setVisibility(View.VISIBLE);
            btnNext.setEnabled(true);
        }

    }


    public void onEventMainThread(CertificateCheckJob.CheckEvent event) {
        if (event.getStatus() == CertificateCheckJob.CheckEvent.VALID) {
            if (useTsa && !NetworkUtil.isConnected(this)) {
                dialogNoInternet();
                return;
            }

            signDialog.show();

            try {
                X509Certificate cert = (X509Certificate) event.getObject();
                X500Name x500Name = new JcaX509CertificateHolder(cert).getSubject();
                RDN cn = x500Name.getRDNs(BCStyle.CN)[0];

                editSignatureName.setText(IETFUtils.valueToString(cn.getFirst().getValue()));
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


}
