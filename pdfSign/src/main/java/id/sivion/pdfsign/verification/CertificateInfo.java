package id.sivion.pdfsign.verification;

/**
 * Created by miftakhul on 07/12/16.
 */

public class CertificateInfo {
    private String Serial;
    private String validity;
    private String subject;
    private String issuer;
    private String publicKey;
    private String algorithm;
    private String fingerPrint;
    private boolean certificateValidity;
    private boolean certificateVerified;
    private boolean certificateTrusted;

    public String getSerial() {
        return Serial;
    }

    public void setSerial(String serial) {
        Serial = serial;
    }

    public String getValidity() {
        return validity;
    }

    public void setValidity(String validity) {
        this.validity = validity;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public void setFingerPrint(String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }

    public boolean isCertificateValidity() {
        return certificateValidity;
    }

    public void setCertificateValidity(boolean certificateValidity) {
        this.certificateValidity = certificateValidity;
    }

    public boolean isCertificateVerified() {
        return certificateVerified;
    }

    public void setCertificateVerified(boolean certificateVerified) {
        this.certificateVerified = certificateVerified;
    }

    public boolean isCertificateTrusted() {
        return certificateTrusted;
    }

    public void setCertificateTrusted(boolean certificateTrusted) {
        this.certificateTrusted = certificateTrusted;
    }
}
