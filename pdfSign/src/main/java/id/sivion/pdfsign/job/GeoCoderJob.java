package id.sivion.pdfsign.job;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;

/**
 * Created by miftakhul on 17/05/17.
 */

public class GeoCoderJob extends Job {

    private Geocoder geocoder;
    private Location location;

    public GeoCoderJob(Location location) {
        super(new Params(1));
        this.location = location;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        geocoder = new Geocoder(DroidSignerApplication.getInstance(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            Log.d(getClass().getSimpleName(), "city : " + addresses.get(0).getAddressLine(3));
            Log.d(getClass().getSimpleName(), "all : " + addresses.get(0).toString());

            EventBus.getDefault().post(new GeoEvent(addresses.get(0).getAddressLine(3), JobStatus.SUCCESS));
        } catch (IOException e) {
            EventBus.getDefault().post(new GeoEvent("", JobStatus.ABORTED));
            e.printStackTrace();
        }
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new GeoEvent("", JobStatus.ABORTED));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        EventBus.getDefault().post(new GeoEvent("", JobStatus.ABORTED));
        return false;
    }

    public static class GeoEvent {
        private String city;
        private int jobStatus;

        public GeoEvent(String city, int jobStatus) {
            this.city = city;
            this.jobStatus = jobStatus;
        }

        public String getCity() {
            return city;
        }

        public int getJobStatus() {
            return jobStatus;
        }
    }

}
