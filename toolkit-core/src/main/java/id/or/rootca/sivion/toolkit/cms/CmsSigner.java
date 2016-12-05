package id.or.rootca.sivion.toolkit.cms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.CMSSignedDataParser;
import org.spongycastle.cms.CMSTypedStream;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationStore;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.Store;

import id.or.rootca.sivion.toolkit.Signer;
import id.or.rootca.sivion.toolkit.commons.KeyPairUtils;
import id.or.rootca.sivion.toolkit.commons.KeyStoreUtils;
import id.or.rootca.sivion.toolkit.pdf.CMSProcessableInputStream;

public class CmsSigner implements Signer<File, File> {
	private final PrivateKey privateKey;
	private X509Certificate certificate;
	private File input;

	private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
	
	public CmsSigner(PrivateKey privateKey, X509Certificate certificate) {
		this.privateKey = privateKey;
		this.certificate = certificate;
	}
	
	@Override
	public void sign(File input, File output) throws Exception {
		Store<?> certs = new JcaCertStore(Arrays.asList(certificate));
		
		ContentSigner sha256Signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
				.build(privateKey);

		CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
		gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
				.build(sha256Signer, certificate));
		gen.addCertificates(certs);
		
		FileInputStream inputStream = new FileInputStream(this.input = input);
		CMSProcessableInputStream msg = new CMSProcessableInputStream(inputStream);
		CMSSignedData signedData = gen.generate(msg, false);
		
		FileOutputStream outputStream = new FileOutputStream(output);
		IOUtils.write(signedData.getEncoded(), outputStream);
		
		IOUtils.closeQuietly(inputStream);
		IOUtils.closeQuietly(outputStream);
	}

	@Override
	public Collection<? extends Certificate> getCertificates(File input) throws Exception {
		FileInputStream cmsData = new FileInputStream(input);
		FileInputStream p7 = new FileInputStream(input);
		
		CMSTypedStream cmsTypedStream = new CMSTypedStream(cmsData);
		CMSSignedDataParser sp = new CMSSignedDataParser(
				new JcaDigestCalculatorProviderBuilder().build(), cmsTypedStream, p7);
		sp.getSignedContent().drain();

		Store<?> certStore = sp.getCertificates();
		SignerInformationStore signers = sp.getSignerInfos();

		Collection<?> c = signers.getSigners();
		Iterator<?> it = c.iterator();

		List<X509Certificate> certificates = new ArrayList<X509Certificate>();
		
		while (it.hasNext()) {
			SignerInformation signer = (SignerInformation) it.next();
			Collection<?> certCollection = certStore.getMatches(signer.getSID());

			Iterator<?> certIt = certCollection.iterator();
			X509CertificateHolder cert = (X509CertificateHolder) certIt.next();
			
			certificates.add(new JcaX509CertificateConverter().getCertificate(cert));
		}
		
		
		IOUtils.closeQuietly(p7);
		return certificates;
	}

	@Override
	public boolean isCertificateExist(File input, Certificate certificate) throws Exception {
		for (Certificate cert : getCertificates(input)) {
			if (cert.equals(certificate)) {
				return true;
			}
		}
		
		return false;
	}

}
