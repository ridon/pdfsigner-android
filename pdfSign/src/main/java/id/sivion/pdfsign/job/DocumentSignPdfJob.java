package id.sivion.pdfsign.job;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.security.KeyChain;
import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.Attributes;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationStore;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.tsp.TSPException;
import org.spongycastle.util.Store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import de.greenrobot.event.EventBus;
import id.sivion.pdfsign.DroidSignerApplication;
import id.sivion.pdfsign.utils.TsaClient;

/**
 * Created by akm on 15/10/15.
 */
public class DocumentSignPdfJob extends Job {

    private static String DIRECTORY_NAME = "pdf bertandatangan";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String reason, location, name;
    private Uri uri;
    private TsaClient tsaClient;
    private boolean useTsa;

    public static DocumentSignPdfJob newInstance(Uri uri, String name, String reason, String location, boolean useTsa) {
        DocumentSignPdfJob job = new DocumentSignPdfJob();
        job.uri = uri;
        job.name = name;
        job.reason = reason;
        job.location = location;
        job.useTsa = useTsa;

        return job;
    }

    public DocumentSignPdfJob() {
        super(new Params(1));
    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new DocumentSignEvent("", uri.getPath(), JobStatus.ADDED));
    }

    @Override
    public void onRun() throws Throwable {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DroidSignerApplication.getInstance());
            String alias = preferences.getString("alias", "");
            PrivateKey privateKey = KeyChain.getPrivateKey(DroidSignerApplication.getInstance(), alias);

            X509Certificate[] chain = KeyChain.getCertificateChain(DroidSignerApplication.getInstance(), alias);
            X509Certificate cert = chain[0];

            Assert.assertNotNull(privateKey);
            Assert.assertNotNull(cert);

            byte[] pdfFileByte ;
            InputStream signedPdfFile = DroidSignerApplication.getInstance().getContentResolver().openInputStream(uri);
            pdfFileByte = IOUtils.toByteArray(signedPdfFile);


            Cursor cursor = DroidSignerApplication.getInstance().getContentResolver().query(uri, null, null, null, null);
            int fileNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();

            String fileName = cursor.getString(fileNameIndex);
            cursor.close();

            //create directory
            File directory = new File(Environment.getExternalStorageDirectory() + File.separator + DIRECTORY_NAME);
            if (!directory.exists()) {
                if (directory.mkdir()) {
                    Log.d(getClass().getSimpleName(), " cannot create directory");
                }
            }

            String substring = fileName.substring(0, fileName.lastIndexOf("."));

            File outputDocument = new File(directory, substring + "_signed.pdf");
            FileOutputStream fos = new FileOutputStream(outputDocument);

            // load document
            PDDocument doc = PDDocument.load(pdfFileByte);

            // create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE); // default filter
            // subfilter for basic and PAdES Part 2 signatures
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(name);
            signature.setLocation(location);
            signature.setReason(reason);


            if (useTsa) {
                String tsaUrl = preferences.getString(DroidSignerApplication.CONSTANT_TSA_URL, "");

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                tsaClient = new TsaClient(new URL(tsaUrl), null, null, digest);
            }

            // the signing date, needed for valid signature
            signature.setSignDate(Calendar.getInstance());
            doc.addSignature(signature, new Signer(privateKey, chain, tsaClient));

            // write incremental (only for signing purpose)
            doc.saveIncremental(fos);
            doc.close();

            EventBus.getDefault().post(new DocumentSignEvent("", outputDocument.getPath(), JobStatus.SUCCESS));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
        EventBus.getDefault().post(new DocumentSignEvent("", uri.getPath(), JobStatus.SYSTEM_ERROR));
        return false;
    }


    private static final class Signer implements SignatureInterface {
        private final PrivateKey key;
        private final X509Certificate[] chain;
        private TsaClient tsaClient;

        public Signer(PrivateKey key, X509Certificate[] chain, TsaClient tsaClient) {
            this.key = key;
            this.chain = chain;
            this.tsaClient = tsaClient;
        }

        @Override
        public byte[] sign(InputStream content) {

            try {
                byte[] bytes = IOUtils.toByteArray(content);
                IOUtils.closeQuietly(content);
                Log.d("Private Key", key + "");

                Store certStore = new JcaCertStore(Arrays.asList(chain[0]));
                CMSProcessableByteArray input = new CMSProcessableByteArray(bytes);
                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(key);

                gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().build())
                        .build(signer, chain[0]));

                gen.addCertificates(certStore);

                CMSSignedData sigData = gen.generate(input, false);

                if (tsaClient != null) {
                    Log.d(getClass().getSimpleName(), "using tsa");
                    sigData = signTimeStamps(sigData);
                }

                return sigData.getEncoded();
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage(), e);
                EventBus.getDefault().post(new DocumentSignEvent("", null, JobStatus.ABORTED));
            }

            throw new RuntimeException("Problem while preparing signature");
        }


        private CMSSignedData signTimeStamps(CMSSignedData signedData) throws IOException, TSPException {
            SignerInformationStore signerStore = signedData.getSignerInfos();
            List<SignerInformation> newSigners = new ArrayList<SignerInformation>();

            for (SignerInformation signer : signerStore.getSigners()) {
                newSigners.add(signTimeStamp(signer));
            }

            return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(newSigners));
        }


        private SignerInformation signTimeStamp(SignerInformation signer) throws IOException, TSPException {
            AttributeTable unsignedAttributes = signer.getUnsignedAttributes();

            ASN1EncodableVector vector = new ASN1EncodableVector();
            if (unsignedAttributes != null) {
                vector = unsignedAttributes.toASN1EncodableVector();
            }

            byte[] toke = tsaClient.getTimeStampToken(signer.getSignature());
            ASN1ObjectIdentifier oid = PKCSObjectIdentifiers.id_aa_signatureTimeStampToken;
            ASN1Encodable signatureTimeStamp = new Attribute(oid, new DERSet(ASN1Primitive.fromByteArray(toke)));

            vector.add(signatureTimeStamp);
            Attributes signAttributes = new Attributes(vector);

            SignerInformation newSigner = SignerInformation.replaceUnsignedAttributes(signer, new AttributeTable(signAttributes));

            if (newSigner == null) {
                return signer;
            }

            return newSigner;
        }


    }

    public static class DocumentSignEvent {
        private String password;
        private String filePath;

        private int status;

        public DocumentSignEvent(String password, String filePath, int status) {
            this.filePath = filePath;
            this.password = password;
            this.status = status;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getPassword() {
            return password;
        }

        public int getStatus() {
            return status;
        }
    }


}