package id.sivion.pdfsign;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import io.fabric.sdk.android.Fabric;

/**
 * Created by root on 8/12/15.
 */
public class DroidSignerApplication extends Application {
    private static DroidSignerApplication instance;
    public static final String CONSTANT_TSA_URL = "tsa_url";

    private JobManager jobManager;
    private ObjectMapper objectMapper;
    private SharedPreferences preferences;

    public DroidSignerApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor preferenceEditor = preferences.edit();

        Iconify.with(new FontAwesomeModule());
        configureJobManager();

        String savedTsaUrl = preferences.getString("tsa_url", "");
        String baseTsaUrl = getString(R.string.app_tsa_url);

        if ("".equals(savedTsaUrl)){
            preferenceEditor.putString(CONSTANT_TSA_URL, baseTsaUrl);
            preferenceEditor.commit();
        }


    }



    private void configureJobManager() {
        Configuration configuration = new Configuration.Builder(this)
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Log.d(TAG, "d := > " + String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Log.d(TAG, "e := > " + String.format(text, args));
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Log.d(TAG, "e := > " + String.format(text, args));
                    }
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120)//wait 2 minute
                .build();
        jobManager = new JobManager(this, configuration);
    }


    public static DroidSignerApplication getInstance() {
        return instance;
    }


    public JobManager getJobManager() {
        return jobManager;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
