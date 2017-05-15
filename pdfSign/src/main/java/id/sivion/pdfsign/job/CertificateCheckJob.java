package id.sivion.pdfsign.job;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.KeyChain;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.security.cert.X509Certificate;

import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;

/**
 * Created by miftakhul on 05/01/17.
 */

public class CertificateCheckJob extends Job {

    public CertificateCheckJob() {
        super(new Params(1));
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DroidSignerApplication.getInstance());
        String alias = preferences.getString("alias", "");

        X509Certificate[] chain = KeyChain.getCertificateChain(DroidSignerApplication.getInstance(), alias);
        X509Certificate cert = chain[0];

        try {
            cert.checkValidity();
            EventBus.getDefault().post(new CheckEvent(CheckEvent.VALID, cert));
        }catch (Exception e){
            EventBus.getDefault().post(new CheckEvent(CheckEvent.INVALID));
        }


    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new CheckEvent(CheckEvent.VALID));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    public static class CheckEvent{
        public static int VALID = 200;
        public static int INVALID = 100;

        private int status;
        private Object object;

        public CheckEvent(int status) {
            this.status = status;
        }

        public CheckEvent(int status, Object object) {
            this.status = status;
            this.object = object;
        }

        public int getStatus() {
            return status;
        }

        public Object getObject() {
            return object;
        }
    }

}
