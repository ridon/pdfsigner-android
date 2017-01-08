package id.sivion.pdfsign.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.verification.VerifyPdfActivity;

/**
 * Created by miftakhul on 06/01/17.
 */

public class OptionPdfActivity extends AppCompatActivity {

    @BindView(R.id.btn_sign_pdf)
    Button btnSignPdf;
    @BindView(R.id.btn_verify_pdf)
    Button btnVerifyPdf;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionpdf_layout);
        ButterKnife.bind(this);

        btnSignPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(OptionPdfActivity.this, CertificateActivity.class);
                startActivity(i);
            }
        });

        btnVerifyPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(OptionPdfActivity.this, VerifyPdfActivity.class);
                startActivity(i);
            }
        });
    }
}
