package id.or.rootca.sivion.toolkit.commons;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class KeyStoreUtils {
	public static X509Certificate getCertificate(InputStream input, char[] password, String keyStoreType) 
			throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, 
					CertificateException, IOException {
		return getCertificate(input, null, password, keyStoreType);
	}
	
	public static X509Certificate getCertificate(InputStream input, String alias, 
			char[] password, String keyStoreType) throws KeyStoreException, 
					NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore keyStore = getKeyStore(input, password, keyStoreType);
		
		return getCertificate(keyStore, alias);
	}
	
	public static X509Certificate getCertificate(KeyStore keyStore) throws KeyStoreException {
		return getCertificate(keyStore, null);
	}
	
	public static X509Certificate getCertificate(KeyStore keyStore, String alias) 
			throws KeyStoreException {
		if (alias == null) {
			Enumeration<String> aliases = keyStore.aliases();
			if (aliases.hasMoreElements()) {
				alias = aliases.nextElement();
			} else {
				throw new KeyStoreException("No aliases");
			}
		}
		
		return (X509Certificate) keyStore.getCertificate(alias);
	}
	
	public static KeyStore getKeyStore(InputStream p12File, char[] password, String keyStoreType)
			throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, 
					CertificateException, IOException {
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(p12File, password);
		
		return keyStore;
	}
}
