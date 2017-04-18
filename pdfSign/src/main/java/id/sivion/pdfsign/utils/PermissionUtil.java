package id.sivion.pdfsign.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

/**
 * Created by miftakhul on 18/04/17.
 */

public class PermissionUtil {

    public static final int REQUEST_WRITE_EXTERNAL_STOREAGE = 1;

    public static boolean isWriteExternalGranted(final Activity activity){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Ijin dibutuhkan untuk menjalankan fungsi aplikasi");
            builder.setTitle("Permission");
            builder.setPositiveButton("Ijinkan", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    makeWriteExternalRequest(activity);
                }
            });

            builder.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        }else {
            makeWriteExternalRequest(activity);
        }

        return false;
    }

    public static void makeWriteExternalRequest(Activity activity){
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STOREAGE);
    }

}
