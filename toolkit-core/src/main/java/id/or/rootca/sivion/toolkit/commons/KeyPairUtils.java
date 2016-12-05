package id.or.rootca.sivion.toolkit.commons;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class KeyPairUtils {
	public static KeyPair getKeyPair(KeyStore keyStore, char[] password) 
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		return getKeyPair(keyStore, null, password);
	}
	
	public static KeyPair getKeyPair(KeyStore keyStore, String alias, char[] password) 
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		if (alias == null) {
			Enumeration<String> aliases = keyStore.aliases();
			if (aliases.hasMoreElements()) {
				alias = aliases.nextElement();
			} else {
				throw new KeyStoreException("No aliases");
			}
		}
		Key key = keyStore.getKey(alias, password);
		if (key instanceof PrivateKey) {
			Certificate cert = keyStore.getCertificate(alias);
			PublicKey publicKey = cert.getPublicKey();
			
			return new KeyPair(publicKey, (PrivateKey) key);
		}
		
		return null;
	}
	
	public static PublicKey getPublicKey(InputStream certFile) throws CertificateException, NoSuchProviderException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(certFile);
		
		return cert.getPublicKey();
	}
}
