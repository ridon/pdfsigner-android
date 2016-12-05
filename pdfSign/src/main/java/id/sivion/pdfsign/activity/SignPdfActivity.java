package id.sivion.pdfsign.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.pdfview.PDFView;
import com.path.android.jobqueue.JobManager;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.DocumentSignPdfJob;
import id.sivion.pdfsign.job.JobStatus;

/**
 * Created by miftakhul on 20/10/16.
 */

public class SignPdfActivity extends AppCompatActivity {

    @Bind(R.id.pdfView)
    PDFView pdfView;
    @Bind(R.id.btn_sign)
    Button btnSign;
    @Bind(R.id.card_sign)
    CardView cardSign;

    private DroidSignerApplication app;
    private JobManager jobManager;
    private ProgressDialog progressDialog;

    private AlertDialog signAlert;

    private String pdfPath;
    private String name,reason,location;

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
                .load();

        setupSignAlertDialog();
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
        signOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (layoutOptions.getVisibility() == View.VISIBLE){
                    layoutOptions.setVisibility(View.GONE);
                }else if (layoutOptions.getVisibility() == View.GONE){
                    layoutOptions.setVisibility(View.VISIBLE);
                }

            }
        });

        builder.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_key, null));
        builder.setTitle(R.string.app_name);
        builder.setPositiveButton(R.string.sign, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                jobManager.addJobInBackground(DocumentSignPdfJob.
                        newInstance(pdfPath,
                                editName.getText().toString(),
                                editReason.getText().toString(),
                                editLocation.getText().toString()));
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

        if (event.getStatus() == JobStatus.ADDED){
            progressDialog.show();

        }else if (event.getStatus() == JobStatus.SUCCESS) {
            progressDialog.dismiss();
            dialogSuccess(event.getFilePath());

        } else if (event.getStatus() == JobStatus.ABORTED) {
            progressDialog.dismiss();
            Toast.makeText(this, "Signing document failed", Toast.LENGTH_SHORT).show();
        }

    }


    private void dialogSuccess(final String filePath) {

        View view = LayoutInflater.from(this).inflate(R.layout.popup_sign_success, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();

        TextView pathInfo = (TextView) view.findViewById(R.id.text_path_info);
        Button buttonClose = (Button) view.findViewById(R.id.btn_close);

        pathInfo.setText(getString(R.string.text_document_saved_in)+" "+filePath);
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
