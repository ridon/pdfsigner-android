package id.sivion.pdfsign.utils;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by miftakhul on 08/01/17.
 */

public class NetworkUtil {

    public static boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }


}
