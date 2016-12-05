package id.or.rootca.sivion.toolkit.signer;


import android.util.Base64;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import id.or.rootca.sivion.toolkit.Signer;
import id.or.rootca.sivion.toolkit.commons.KeyPairUtils;
import id.or.rootca.sivion.toolkit.commons.KeyStoreUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Created by akm on 07/09/15.
 */
public class JwtObjectSigner implements Signer<Object, File> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private KeyPair keyPair;
    private X509Certificate certificate;

    public static final String SIGNED_QNAME = "content";
    public static final String CERT_QNAME = "cert";

    public JwtObjectSigner(KeyStore keyStore, String alias, char[] password) throws Exception{
        this.keyPair = KeyPairUtils.getKeyPair(keyStore, alias, password);
        this.certificate = KeyStoreUtils.getCertificate(keyStore, alias);
    }
    public JwtObjectSigner(KeyStore keyStore, char[] password) throws Exception {
        this(keyStore, null, password);
    }
    @Override
    public void sign (Object input, File output) throws Exception{
        Claims claims = Jwts.claims()
                .setSubject(certificate.getSubjectDN().getName())
                .setIssuedAt(new Date())
                .setIssuer(certificate.getIssuerDN().getName())
                .setNotBefore(certificate.getNotBefore());
        claims.put(SIGNED_QNAME, input);
        claims.put(CERT_QNAME, certificate.getEncoded());

        FileWriter writer = new FileWriter(output);
        writer.write(Jwts.builder().setClaims(claims)
        .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
        .compact());
        IOUtils.closeQuietly(writer);

    }

    @Override
    public Collection<? extends Certificate> getCertificates(Object input) throws Exception {
        if (!(input instanceof String)){
            throw new RuntimeException("Input must be string");
        }
        String stringJwt = (String) input;

        try {
            Jwt jwt = Jwts.parser().setSigningKey(certificate.getPublicKey())
                    .parse(stringJwt);
            Map<String, Object> body = (Map<String, Object>) jwt.getBody();
            if (body.containsKey(CERT_QNAME)){
                byte[] cert = Base64.decode((String) body.get(CERT_QNAME),Base64.DEFAULT);

                InputStream c = new ByteArrayInputStream(cert);
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certi = (X509Certificate) certificateFactory.generateCertificate(c);
                IOUtils.closeQuietly(c);

                return Arrays.asList(certi);
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return new ArrayList<Certificate>();
    }

    @Override
    public boolean isCertificateExist(Object input, Certificate certificate) throws Exception {
        for (Certificate cert: getCertificates(input)){
            if (cert.equals(certificate)) {
                return true;
            }
        }
        return false;
    }
}
