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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.path.android.jobqueue.JobManager;
import com.tooltip.Tooltip;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.CertificateChainJob;
import id.sivion.pdfsign.job.CertificateCheckJob;
import id.sivion.pdfsign.job.DocumentSignPdfJob;
import id.sivion.pdfsign.job.JobStatus;
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

    private String pdfPath;
    private String name, reason, location;
    private boolean useTsa = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_view);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        pdfPath = getIntent().getStringExtra("pdfPath");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.text_wait));
        progressDialog.setCancelable(false);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        jobManager = DroidSignerApplication.getInstance().getJobManager();


        // receive file frome other application
        if (getIntent().getAction() == Intent.ACTION_VIEW){
            pdfPath = getIntent().getData().getPath();
        }else if (getIntent().getAction() == Intent.ACTION_SEND) {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            pdfPath = uri.getPath();
        }

        if (pdfPath != null) {
            openPdf(pdfPath);
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


    private void openPdf(String pdfPath){
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

        checkSignature(pdfFile);

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
        Intent i = new Intent(this, SignDetail.class);
        i.putExtra("pdfPath", pdfPath);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            startActivity(i, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }else {
            startActivity(i);
        }
    }

    private void checkSignature(File pdfFile){
        try {
            PDDocument pdDocument = PDDocument.load(pdfFile);
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
                signDialog.show();

            }
        });

        requestSignDialog.show();

    }

    public void setupSignAlertDialog() {

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
                reason = editReason.getText().toString();
                location = editLocation.getText().toString();

                jobManager.addJobInBackground(new CertificateCheckJob());

            }
        });


        signDialog = builder.create();

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


}
