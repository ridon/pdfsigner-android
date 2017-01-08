package id.sivion.pdfsign.verification;

import org.spongycastle.tsp.TimeStampToken;

import java.util.List;

/**
 * Created by miftakhul on 29/11/16.
 */

public class SignInfo {

    private boolean signStatus;
    private String signBy;
    private String location;
    private String reason;
    private String signLocalTime;
    private TimeStampToken timesTamp;
    private List<CertificateInfo> certificateInfos;

    public boolean isSignStatus() {
        return signStatus;
    }

    public void setSignStatus(boolean signStatus) {
        this.signStatus = signStatus;
    }

    public String getSignBy() {
        return signBy;
    }

    public void setSignBy(String signBy) {
        this.signBy = signBy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSignLocalTime() {
        return signLocalTime;
    }

    public void setSignLocalTime(String signLocalTime) {
        this.signLocalTime = signLocalTime;
    }

    public TimeStampToken getTimesTamp() {
        return timesTamp;
    }

    public void setTimesTamp(TimeStampToken timesTamp) {
        this.timesTamp = timesTamp;
    }

    public List<CertificateInfo> getCertificateInfos() {
        return certificateInfos;
    }

    public void setCertificateInfos(List<CertificateInfo> certificateInfos) {
        this.certificateInfos = certificateInfos;
    }
}
