package id.sivion.pdfsign.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.path.android.jobqueue.JobManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.CertificateChainJob;
import id.sivion.pdfsign.job.JobStatus;

/**
 * Created by miftakhul on 08/09/16.
 */
public class CertificateActivity extends AppCompatActivity implements KeyChainAliasCallback {

    private static final int REQUEST_CODE_BROWSE_PDF = 20;


    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.btn_browse_certificate)
    Button btnBrowseCertificate;
    @Bind(R.id.text_subjectdn)
    TextView textSubjectdn;
    @Bind(R.id.text_issuer)
    TextView textIssuer;
    @Bind(R.id.text_expire)
    TextView textExpire;
    @Bind(R.id.layout_certificate_info)
    LinearLayout layoutCertificateInfo;
    @Bind(R.id.btn_browse_pdf)
    Button btnNext;

    private SharedPreferences preferences;
    private JobManager jobManager;
    private String getAlias, newAlias;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.certificate_activity);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        jobManager = DroidSignerApplication.getInstance().getJobManager();
        getAlias = preferences.getString("alias", "");


        if (!getAlias.equalsIgnoreCase("")) {

            jobManager.addJobInBackground(new CertificateChainJob(getAlias));

        }

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
        this.newAlias = alias;
        preferences.edit().putString("alias", newAlias).apply();

        jobManager.addJobInBackground(new CertificateChainJob(alias));
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


    public void browseFile(View view){
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("application/pdf");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Select Pdf"), REQUEST_CODE_BROWSE_PDF);
        }catch (android.content.ActivityNotFoundException e){
            Toast.makeText(this, "Install file manager", Toast.LENGTH_LONG).show();
        }
    }


    private void uriAction(Uri uri){
        if (uri.getScheme().equalsIgnoreCase("file")){
            next(uri.getPath());
        }else if (uri.getScheme().equalsIgnoreCase("content")){

        }
    }

    private void next(String pdfPath){

        Intent i = new Intent(this, SignPdfActivity.class);
        i.putExtra("pdfPath", pdfPath);
        startActivity(i);
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BROWSE_PDF && resultCode == RESULT_OK){
            Uri uri = data.getData();
            Log.d(getClass().getSimpleName()," log file "+uri.toString()+"\n" +
                    "schema "+uri.getScheme()+"\n" +
                    "path "+uri.getPath());

            uriAction(uri);

        }
    }


}
