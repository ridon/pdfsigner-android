package id.sivion.pdfsign.verification;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.path.android.jobqueue.JobManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.SignDetailJob;

/**
 * Created by miftakhul on 15/11/16.
 */

public class SignDetail extends AppCompatActivity {


    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.list_document)
    RecyclerView listDocument;

    private SignInfo signInfo;
    private List<SignInfo> signInfos = new ArrayList<>();
    private CertificateInfo certificateInfo;
    private List<CertificateInfo> certificateInfos = new ArrayList<>();

    private AlertDialog noSignerDialog;
    private DocumentAdapter documentAdapter;
    private JobManager jobManager;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_detail);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

        }

        documentAdapter = new DocumentAdapter(this);
        jobManager = DroidSignerApplication.getInstance().getJobManager();

        settupDialogNoSigner();

        setSupportActionBar(toolbar);
        ActionBar ac = getSupportActionBar();
        ac.setDisplayHomeAsUpEnabled(true);
        ac.setTitle("");

        listDocument.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        listDocument.setItemAnimator(new DefaultItemAnimator());
        listDocument.setAdapter(documentAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Tolong tunggu");

        Log.d(getClass().getSimpleName(), " verifying process :> start");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        documentAdapter.clear();
        jobManager.addJobInBackground(new SignDetailJob(getIntent().getStringExtra("pdfPath").toString()));
    }


    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SignDetailJob.SignDetailEvent event) {
        if (event.getStatus() == SignDetailJob.SignDetailEvent.ADD) {
            progressDialog.show();
        }
        else if (event.getStatus() == SignDetailJob.SignDetailEvent.NO_SIGNER){
            progressDialog.dismiss();
            settupDialogNoSigner();
        }
        else if (event.getStatus() == SignDetailJob.SignDetailEvent.SUCCESS){
            progressDialog.dismiss();
            documentAdapter.addSignInfos(event.getSignInfos());
        }
        else if (event.getStatus() == SignDetailJob.SignDetailEvent.ERROR){
            progressDialog.dismiss();
        }
    }



    private void settupDialogNoSigner() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.text_dont_have_signature)
                .setCancelable(false)
                .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        onBackPressed();
                    }
                });
        noSignerDialog = builder.create();
    }


}
