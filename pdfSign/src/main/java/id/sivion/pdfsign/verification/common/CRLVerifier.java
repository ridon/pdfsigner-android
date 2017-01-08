/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// https://svn.apache.org/repos/asf/cxf/tags/cxf-2.4.4/distribution/src/main/release/samples/sts_issue_operation/src/main/java/demo/sts/provider/cert/CRLVerifier.java
package id.sivion.pdfsign.verification.common;

import android.util.Log;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERIA5String;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.x509.CRLDistPoint;
import org.spongycastle.asn1.x509.DistributionPoint;
import org.spongycastle.asn1.x509.DistributionPointName;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//import javax.naming.Context;
//import javax.naming.NamingException;
//import javax.naming.directory.Attribute;
//import javax.naming.directory.Attributes;
//import javax.naming.directory.DirContext;
//import javax.naming.directory.InitialDirContext;


public final class CRLVerifier {
	private String TAG = getClass().getSimpleName();
	
	/**
	 * Extracts the CRL distribution points from the certificate (if available)
	 * and checks the certificate revocation status against the CRLs coming from
	 * the distribution points. Supports HTTP, HTTPS, FTP and LDAP based URLs.
	 * 
//	 * @param cert
//	 *            the certificate to be checked for revocation
//	 * @throws CertificateVerificationException
	 *             if the certificate is revoked
	 */

	private CRLVerifier() {
	}

	public static boolean verifyCertificateCRLs(X509Certificate cert) {
		try {
			List<String> crlDistPoints = getCrlDistributionPoints(cert);
			for (String crlDP : crlDistPoints) {
				X509CRL crl = downloadCRL(crlDP);
				if (crl.isRevoked(cert)) {
					return false;
				}
			}
		} catch (Exception ex) {
			Log.e("CRL", "Can not verify CRL for certificate: "+cert.getSubjectX500Principal(), ex);

			throw new RuntimeException("Can not verify CRL for certificate: " + cert.getSubjectX500Principal());
		}
		
		return true;
	}

	/**
	 * Downloads CRL from given URL. Supports http, https, ftp and ldap based
	 * URLs.
	 */
	private static X509CRL downloadCRL(String crlURL)
			throws IOException, CertificateException, CRLException, IOException, ExecutionException, InterruptedException {
		if (crlURL.startsWith("http://") || crlURL.startsWith("https://") || crlURL.startsWith("ftp://")) {
			return downloadCRLFromWeb(crlURL);
		}
//		else if (crlURL.startsWith("ldap://")) {
//			return downloadCRLFromLDAP(crlURL);
//		}
		else {
			Log.e("CRL","Can not download CRL from certificate distribution point: "+crlURL);

			throw new IOException("Can not download CRL from certificate distribution point: " + crlURL);
		}
	}

	/**
	 * Downloads a CRL from given LDAP url, e.g.
	 * ldap://ldap.infonotary.com/dc=identity-ca,dc=infonotary,dc=com
	 * 
	 * @throws IOException
	 */
//	@SuppressWarnings("rawtypes")
//	private static X509CRL downloadCRLFromLDAP(String ldapURL)
//			throws CertificateException, NamingException, CRLException, IOException {
//		Map<String, String> env = new Hashtable<>();
//		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//		env.put(Context.PROVIDER_URL, ldapURL);
//
//		DirContext ctx = new InitialDirContext((Hashtable) env);
//		Attributes avals = ctx.getAttributes("");
//		Attribute aval = avals.get("certificateRevocationList;binary");
//		byte[] val = (byte[]) aval.get();
//		if ((val == null) || (val.length == 0)) {
//			throw new IOException("Can not download CRL from: " + ldapURL);
//		} else {
//			InputStream inStream = new ByteArrayInputStream(val);
//			CertificateFactory cf = CertificateFactory.getInstance("X.509");
//			return (X509CRL) cf.generateCRL(inStream);
//		}
//	}

	/**
	 * Downloads a CRL from given HTTP/HTTPS/FTP URL, e.g.
	 * http://crl.infonotary.com/crl/identity-ca.crl
	 */
	private static X509CRL downloadCRLFromWeb(final String crlURL)
			throws MalformedURLException, IOException, CertificateException, CRLException, ExecutionException, InterruptedException {


		ExecutorService executor = Executors.newSingleThreadExecutor();
		Callable<X509CRL> callable = new Callable<X509CRL>() {
			@Override
			public X509CRL call() throws Exception {
				URL url;
				InputStream crlStream = null;
				try {
					url = new URL(crlURL);
					crlStream = url.openStream();
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					return (X509CRL) cf.generateCRL(crlStream);
				} catch (CertificateException e) {
					e.printStackTrace();
				}finally {
					crlStream.close();
				}
				return null;
			}
		};

		Future<X509CRL> future = executor.submit(callable);
		executor.shutdown();
		return future.get();
	}



	/**
	 * Extracts all CRL distribution point URLs from the "CRL Distribution
	 * Point" extension in a X.509 certificate. If CRL distribution point
	 * extension is unavailable, returns an empty list.
	 */
	public static List<String> getCrlDistributionPoints(X509Certificate cert)
			throws CertificateParsingException, IOException {
		byte[] crldpExt = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());
		if (crldpExt == null) {
			return new ArrayList<String>();
		}
		ASN1InputStream oAsnInStream = new ASN1InputStream(new ByteArrayInputStream(crldpExt));
		ASN1Primitive derObjCrlDP = oAsnInStream.readObject();
		oAsnInStream.close();
		ASN1OctetString dosCrlDP = (DEROctetString) derObjCrlDP;
		byte[] crldpExtOctets = dosCrlDP.getOctets();
		ASN1InputStream oAsnInStream2 = new ASN1InputStream(new ByteArrayInputStream(crldpExtOctets));
		ASN1Primitive derObj2 = oAsnInStream2.readObject();
		oAsnInStream2.close();
		CRLDistPoint distPoint = CRLDistPoint.getInstance(derObj2);
		List<String> crlUrls = new ArrayList<String>();
		for (DistributionPoint dp : distPoint.getDistributionPoints()) {
			DistributionPointName dpn = dp.getDistributionPoint();
			// Look for URIs in fullName
			if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
				GeneralName[] genNames = GeneralNames.getInstance(dpn.getName()).getNames();
				// Look for an URI
				for (int j = 0; j < genNames.length; j++) {
					if (genNames[j].getTagNo() == GeneralName.uniformResourceIdentifier) {
						String url = DERIA5String.getInstance(genNames[j].getName()).getString();
						crlUrls.add(url);
					}
				}
			}
		}
		return crlUrls;
	}

}