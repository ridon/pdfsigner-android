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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.path.android.jobqueue.JobManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.job.CertificateChainJob;
import id.sivion.pdfsign.job.JobStatus;
import id.sivion.pdfsign.utils.UriUtil;

/**
 * Created by miftakhul on 08/09/16.
 */
public class CertificateActivity extends AppCompatActivity implements KeyChainAliasCallback {

    private static final int REQUEST_CODE_BROWSE_PDF = 20;


    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.btn_browse_certificate)
    Button btnBrowseCertificate;
    @BindView(R.id.text_subjectdn)
    TextView textSubjectdn;
    @BindView(R.id.text_issuer)
    TextView textIssuer;
    @BindView(R.id.text_expire)
    TextView textExpire;
    @BindView(R.id.layout_certificate_info)
    LinearLayout layoutCertificateInfo;
    @BindView(R.id.btn_browse_pdf)
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_setting){
            settingDialog();
        }

        return true;
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
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/pdf");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Select Pdf"), REQUEST_CODE_BROWSE_PDF);
        }catch (android.content.ActivityNotFoundException e){
            Toast.makeText(this, "Install file manager", Toast.LENGTH_LONG).show();
        }
    }


    private void next(Uri uri){

        Intent i = new Intent(this, SignPdfActivity.class);
        i.putExtra("pdfUri", uri);
        startActivity(i);
    }


    private void settingDialog(){
        final SharedPreferences.Editor editPreference = preferences.edit();
        View view = LayoutInflater.from(this).inflate(R.layout.tsa_popup, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        final CheckBox defaultTsa = (CheckBox) view.findViewById(R.id.check_tsa_url);
        final EditText editTsa = (EditText) view.findViewById(R.id.edit_tsa_url);
        final Button cancel = (Button) view.findViewById(R.id.btn_cancel);
        Button apply = (Button) view.findViewById(R.id.btn_apply);

        final String savedTsaUrl = preferences.getString(DroidSignerApplication.CONSTANT_TSA_URL, "");
        final String baseTsaUrl = getString(R.string.app_tsa_url);

        if (savedTsaUrl.equals(baseTsaUrl)){
            defaultTsa.setChecked(true);
            editTsa.setEnabled(false);
        }else {
            editTsa.setText(savedTsaUrl);
        }

        defaultTsa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

                editTsa.setError(null);

                if (checked){
                    editTsa.setEnabled(false);
                    editTsa.getText().clear();

                }else {

                    editTsa.setEnabled(true);
                    if (!savedTsaUrl.equals(baseTsaUrl)){
                        editTsa.setText(savedTsaUrl);
                    }

                }

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newTsaUrl;
                newTsaUrl = editTsa.getText().toString();

                if (!defaultTsa.isChecked()){

                    if (newTsaUrl.isEmpty()){
                        editTsa.setError(getString(R.string.text_tsa_empty));
                    }else {
                        editPreference.putString(DroidSignerApplication.CONSTANT_TSA_URL, newTsaUrl);
                        editPreference.apply();
                        dialog.dismiss();

                    }

                }else {
                    editPreference.putString(DroidSignerApplication.CONSTANT_TSA_URL, baseTsaUrl);
                    editPreference.apply();
                    dialog.dismiss();
                }
            }
        });


        dialog.show();


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

            next(uri);

        }
    }


}
