package id.sivion.pdfsign.verification;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import id.sivion.pdfsign.R;


/**
 * Created by miftakhul on 15/11/16.
 */

public class VerifyPdfActivity extends AppCompatActivity {

    static int REQUEST_CODE_BROWSE_PDF = 100;

    private AlertDialog alertFileNotValid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_activity);

        setupAlertFileNotValid();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BROWSE_PDF && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.d(getClass().getSimpleName(), " log file " + uri.toString() + "\n" +
                    "schema " + uri.getScheme() + "\n" +
                    "path " + uri.getPath());

            showPdf(uri);

        }
    }

    public void browseFile(View view) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/pdf");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Pilih Pdf"), REQUEST_CODE_BROWSE_PDF);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(VerifyPdfActivity.this, "Install file manager", Toast.LENGTH_LONG).show();
        }
    }


    private void showPdf(Uri uri) {
        Intent i = new Intent(this, PdfView.class);
        i.putExtra("pdfUri", uri);
        startActivity(i);

    }

    private void setupAlertFileNotValid() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.text_document_not_valid)
                .setPositiveButton("Tutup", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        alertFileNotValid = builder.create();
    }


}
