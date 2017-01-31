package id.sivion.pdfsign.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.path.android.jobqueue.JobManager;
import com.tooltip.Tooltip;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.CertificateCheckJob;
import id.sivion.pdfsign.job.DocumentSignPdfJob;
import id.sivion.pdfsign.job.JobStatus;
import id.sivion.pdfsign.utils.NetworkUtil;
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

    private DroidSignerApplication app;
    private JobManager jobManager;
    private ProgressDialog progressDialog;

    private AlertDialog signAlert;

    private String pdfPath;
    private String name, reason, location;
    private boolean useTsa = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_pdf_activity);
        ButterKnife.bind(this);

        app = DroidSignerApplication.getInstance();
        jobManager = app.getJobManager();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.text_wait));
        progressDialog.setCancelable(false);

        pdfPath = getIntent().getStringExtra("pdfPath");

        File pdfFile = new File(pdfPath);
        pdfView.fromFile(pdfFile)
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
        final EditText editName = (EditText) view.findViewById(R.id.edit_name);
        final EditText editReason = (EditText) view.findViewById(R.id.edit_reason);
        final EditText editLocation = (EditText) view.findViewById(R.id.edit_location);
        CheckBox checkUseTsa = (CheckBox) view.findViewById(R.id.check_use_tsa);
        final Button btnTsaHelp = (Button) view.findViewById(R.id.btn_tsa_help);

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

                name = editName.getText().toString();
                reason = editReason.getText().toString();
                location = editLocation.getText().toString();

                jobManager.addJobInBackground(new CertificateCheckJob());

            }
        });

        signAlert = builder.create();

    }

    @OnClick(R.id.btn_sign)
    public void onButtonClick(Button button) {
        signAlert.show();
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

            jobManager.addJobInBackground(DocumentSignPdfJob.
                    newInstance(pdfPath,
                            name,
                            reason,
                            location, useTsa));
        } else {
            dialogCertificateInValid();
        }
    }

    public void onEventMainThread(TsaClient.TsaEvent event) {
        if (event.getStatus() == TsaClient.TsaEvent.FAILED) {

            dialogTsaFailed();
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
