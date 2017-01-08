package id.sivion.pdfsign.job;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerId;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationVerifier;
import org.spongycastle.cms.SignerInformationVerifierProvider;
import org.spongycastle.cms.jcajce.JcaSignerInfoVerifierBuilder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.tsp.TSPException;
import org.spongycastle.tsp.TimeStampToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.R;
import id.sivion.pdfsign.verification.CertificateInfo;
import id.sivion.pdfsign.verification.SignInfo;
import id.sivion.pdfsign.verification.common.CRLVerifier;

/**
 * Created by miftakhul on 08/01/17.
 */

public class SignDetailJob extends Job {

    private String filePath;
    private SignInfo signInfo;
    private List<SignInfo> signInfos = new ArrayList<>();
    private CertificateInfo certificateInfo;

    public SignDetailJob(String filePath) {
        super(new Params(1));

        this.filePath = filePath;
    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new SignDetailEvent(SignDetailEvent.ADD, null));
    }

    @Override
    public void onRun() throws Throwable {
        boolean verified = false;
        File signedPdfFile = new File(filePath);

        Log.d(getClass().getSimpleName(), " verifying process :> pdf path " + filePath);

        PDDocument document = null;
        try {
            document = PDDocument.load(signedPdfFile);

            if (document.getSignatureDictionaries().isEmpty()) {
                EventBus.getDefault().post(new SignDetailEvent(SignDetailEvent.NO_SIGNER, null));
            }

            for (PDSignature sig : document.getSignatureDictionaries()) {
                signInfo = new SignInfo();

                sigNatureInfo(sig);

                COSDictionary sigDic = sig.getCOSObject();
                COSString content = (COSString) sigDic.getDictionaryObject(COSName.CONTENTS);

                FileInputStream fis = new FileInputStream(signedPdfFile);
                byte[] signedContent = sig.getSignedContent(fis);
                fis.close();

                CMSSignedData cmsSignedData = new CMSSignedData(new CMSProcessableByteArray(signedContent), content.getBytes());

                verified = verifySignature(cmsSignedData);
                timesTamp(cmsSignedData);

                isVerified(verified);

                signInfos.add(signInfo);
            }


            EventBus.getDefault().post(new SignDetailEvent(SignDetailEvent.SUCCESS, signInfos));


        } catch (IOException e) {
            e.printStackTrace();
        } catch (CMSException e) {
            e.printStackTrace();
        }

        Log.d(getClass().getSimpleName(), " verifying process :> pdf status " + verified);
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new SignDetailEvent(SignDetailEvent.ERROR, null));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    private boolean verifySignature(final CMSSignedData cmsSignedData) {
        Log.d(getClass().getSimpleName(), " verifying process :> start getting certificate ");

        try {
            boolean verified = cmsSignedData.verifySignatures(new SignerInformationVerifierProvider() {
                JcaX509CertificateConverter converter = new JcaX509CertificateConverter();

                @Override
                public SignerInformationVerifier get(SignerId sid) throws OperatorCreationException {
                    Collection<X509CertificateHolder> certificateHolders = cmsSignedData.getCertificates().getMatches(sid);

                    X509CertificateHolder certificateHolder = certificateHolders.iterator().next();
                    X509Certificate certificate = null;

                    try {

                        certificate = converter.getCertificate(certificateHolder);
                        certificateInfo(certificate);

                        Log.d(getClass().getSimpleName(), " verifying process :> certificate getted");

                        return new JcaSignerInfoVerifierBuilder(
                                new JcaDigestCalculatorProviderBuilder().build()
                        ).build(certificate.getPublicKey());
                    } catch (CertificateException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

            });

            return verified;
        } catch (CMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private void sigNatureInfo(PDSignature signature) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

        signInfo.setSignBy(signature.getName());
        signInfo.setLocation(signature.getLocation());
        signInfo.setReason(signature.getReason());
        signInfo.setSignLocalTime(dateFormat.format(signature.getSignDate().getTime()));

    }


    private void isVerified(boolean verified) {
        if (verified) {
            signInfo.setSignStatus(true);
        } else {
            signInfo.setSignStatus(false);
        }
    }


    private void timesTamp(CMSSignedData cmsSignedData) {
        SignerInformation sigInfo = cmsSignedData.getSignerInfos().iterator().next();
        AttributeTable unsignedAttributeTable = sigInfo.getUnsignedAttributes();

        if (unsignedAttributeTable != null) {

            try {
                Attribute attribute = unsignedAttributeTable.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                ASN1Encodable ae = attribute.getAttributeValues()[0];
                CMSSignedData ce = new CMSSignedData(ae.toASN1Primitive().getEncoded());

                TimeStampToken timeStampToken = new TimeStampToken(ce);

                signInfo.setTimesTamp(timeStampToken);
            } catch (CMSException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TSPException e) {
                e.printStackTrace();
            }
        }
    }


    private void certificateInfo(X509Certificate certificate) {

            certificateInfo = new CertificateInfo();

            certificateValidity(certificate);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm");
            String validity = dateFormat.format(certificate.getNotBefore()) + " - " + dateFormat.format(certificate.getNotAfter());

            String fingerprint = null;
            try {

                fingerprint = new String(Hex.encodeHex(DigestUtils.sha1(certificate.getEncoded()))).toUpperCase();

            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }

            String serial = new String(Hex.encodeHex(certificate.getSerialNumber().toByteArray())).toUpperCase();

            certificateInfo.setSerial(serial);
            certificateInfo.setValidity(validity);
            certificateInfo.setSubject(certificate.getSubjectDN().toString());
            certificateInfo.setIssuer(certificate.getIssuerDN().toString());
            certificateInfo.setPublicKey(certificate.getPublicKey().getAlgorithm());
            certificateInfo.setAlgorithm(certificate.getSigAlgName());
            certificateInfo.setFingerPrint(fingerprint);

            Key key = certificate.getPublicKey();
            if (key instanceof RSAKey) {
                int lengt = ((RSAKey) certificate.getPublicKey()).getModulus().bitLength();
                Log.d(getClass().getSimpleName(), "rsa length " + lengt);

                certificateInfo.setPublicKey(certificate.getPublicKey().getAlgorithm() + " (" + lengt + ")");
            }

        signInfo.setCertificateInfo(certificateInfo);


    }


    private List<X509Certificate> loadCertificate() {
        Log.d(getClass().getSimpleName(), "Load certificate");
        List<X509Certificate> certificateList = new ArrayList<>();

        Field[] raws = R.raw.class.getFields();
        Log.d("Raw size", "" + raws.length);
        for (int x = 0; x < raws.length; x++) {
            Log.d("raw name : ", raws[x].getName());

            try {
                X509Certificate certificate;

                int id = raws[x].getInt(raws[x]);
                InputStream in = DroidSignerApplication.getInstance().getResources().openRawResource(id);


                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                certificate = (X509Certificate) certificateFactory.generateCertificate(in);
                Log.d(getClass().getSimpleName(), "subject dn " + certificate.getSubjectDN());

                certificateList.add(certificate);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        }

        return certificateList;
    }


    private void certificateValidity(X509Certificate certificate) {
        Log.d(getClass().getSimpleName(), "==============================================");


        for (X509Certificate cer : loadCertificate()) {
            Log.d(getClass().getSimpleName(), "validity certificate. certificate subject : " + cer.getSubjectDN() + "\n" +
                    "issuer " + certificate.getIssuerDN() + "\n");

            if (!cer.getSubjectDN().equals(certificate.getIssuerDN())) continue;

            try {
                certificate.verify(cer.getPublicKey());
                certificateInfo.setCertificateTrusted(true);

            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
        }

        try {
            X509CertificateHolder certificateHolder = new X509CertificateHolder(certificate.getEncoded());
            boolean notRevoked = CRLVerifier.verifyCertificateCRLs(new JcaX509CertificateConverter().getCertificate(certificateHolder));

            Log.d(getClass().getSimpleName(), "Revocation status " + notRevoked);
            if (notRevoked) certificateInfo.setCertificateVerified(notRevoked);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }


        // check certificate validity
        try {
            certificate.checkValidity();
            certificateInfo.setCertificateValidity(true);
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            certificateInfo.setCertificateValidity(false);
        }

        Log.d(getClass().getSimpleName(), "==============================================");
    }


    public static class SignDetailEvent{
        public static final int ADD = 100;
        public static final int SUCCESS = 200;
        public static final int NO_SIGNER = 300;
        public static final int ERROR = 400;

        private int status;
        private List<SignInfo> signInfos = new ArrayList<>();

        public SignDetailEvent(int status, List<SignInfo> signInfos) {
            this.status = status;
            this.signInfos = signInfos;
        }

        public int getStatus() {
            return status;
        }

        public List<SignInfo> getSignInfos() {
            return signInfos;
        }
    }

}
