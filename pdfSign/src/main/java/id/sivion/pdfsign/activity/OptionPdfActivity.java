package id.sivion.pdfsign.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.utils.PermissionUtil;
import id.sivion.pdfsign.verification.VerifyPdfActivity;

/**
 * Created by miftakhul on 06/01/17.
 */

public class OptionPdfActivity extends AppCompatActivity {

    @BindView(R.id.btn_sign_pdf)
    Button btnSignPdf;
    @BindView(R.id.btn_verify_pdf)
    Button btnVerifyPdf;

    private int requestAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionpdf_layout);
        ButterKnife.bind(this);

//        btnSignPdf.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(OptionPdfActivity.this, CertificateActivity.class);
//                startActivity(i);
//            }
//        });
//
//        btnVerifyPdf.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(OptionPdfActivity.this, VerifyPdfActivity.class);
//                startActivity(i);
//            }
//        });
    }

    @OnClick({R.id.btn_sign_pdf, R.id.btn_verify_pdf})
    public void onclick(Button button) {
        if (!PermissionUtil.isWriteExternalGranted(this)) {
            requestAction = button.getId();
            return;
        }

        action(button.getId());
    }

    private void action(int id){
        switch (id) {
            case R.id.btn_sign_pdf:
                Intent i = new Intent(OptionPdfActivity.this, CertificateActivity.class);
                startActivity(i);
                break;
            case R.id.btn_verify_pdf:
                Intent i1 = new Intent(OptionPdfActivity.this, VerifyPdfActivity.class);
                startActivity(i1);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PermissionUtil.REQUEST_WRITE_EXTERNAL_STOREAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(getClass().getSimpleName(), "write permission granted");
                    action(requestAction);
                }
                break;
        }
    }
}
