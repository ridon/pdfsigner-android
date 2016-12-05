package id.or.rootca.sivion.toolkit;

import java.security.cert.Certificate;
import java.util.Collection;

public interface Signer<Input, Output> {
	void sign(Input input, Output output) throws Exception;
	
	Collection<? extends Certificate> getCertificates(Input input) throws Exception;
	
	boolean isCertificateExist(Input input, Certificate certificate) throws Exception;
}
