package id.sivion.pdfsign.job;

import android.security.KeyChain;
import android.security.KeyChainException;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.security.cert.X509Certificate;

import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;

/**
 * Created by akm on 08/06/16.
 */
public class CertificateChainJob extends Job {

    private String subjectDN;
    private String issuer;
    private String expire;
    private String alias;
    private Long serialNumber;

    public CertificateChainJob(String alias) {
        super(new Params(1).persist());
        this.alias = alias;
    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new CertificateChainEvent(subjectDN, issuer, expire, serialNumber, JobStatus.ADDED));
    }

    @Override
    public void onRun() throws Throwable {
        try {
            X509Certificate[] chain = new X509Certificate[0];
            chain = KeyChain.getCertificateChain(DroidSignerApplication.getInstance(), alias);

            X509Certificate cert = chain[0];

            subjectDN = cert.getSubjectDN().getName();
            issuer = cert.getIssuerDN().getName();
            expire = cert.getNotAfter().toString();
            serialNumber = Long.parseLong(cert.getSerialNumber().toString());

            EventBus.getDefault().post(new CertificateChainEvent(subjectDN, issuer, expire, serialNumber, JobStatus.SUCCESS));

        } catch (KeyChainException e) {
            EventBus.getDefault().post(new CertificateChainEvent(subjectDN, issuer, expire, serialNumber, JobStatus.SYSTEM_ERROR));
            e.printStackTrace();
        } catch (InterruptedException e) {
            EventBus.getDefault().post(new CertificateChainEvent(subjectDN, issuer, expire, serialNumber, JobStatus.SYSTEM_ERROR));
            e.printStackTrace();
        }

    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new CertificateChainEvent(subjectDN, issuer, expire, serialNumber, JobStatus.SYSTEM_ERROR));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    public static class CertificateChainEvent {
        private String subjectDN;
        private String issuer;
        private String expire;
        private Long serialNumber;

        private int status;

        public CertificateChainEvent(String subjectDN, String issuer, String expire, Long serialNumber, int status) {
            this.subjectDN = subjectDN;
            this.issuer = issuer;
            this.expire = expire;
            this.serialNumber = serialNumber;
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public String getSubjectDN() {
            return subjectDN;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getExpire() {
            return expire;
        }

        public Long getSerialNumber() {
            return serialNumber;
        }
    }
}
