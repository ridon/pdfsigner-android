package id.sivion.pdfsign.utils;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.tsp.TSPException;
import org.spongycastle.tsp.TimeStampRequest;
import org.spongycastle.tsp.TimeStampRequestGenerator;
import org.spongycastle.tsp.TimeStampResponse;
import org.spongycastle.tsp.TimeStampToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Created by miftakhul on 07/11/16.
 */

public class TsaClient {

    private final String TAG = getClass().getSimpleName().toString();

    private final URL url;
    private final String username;
    private final String password;
    private final MessageDigest digest;

    public TsaClient(URL url, String username, String password, MessageDigest digest) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.digest = digest;
    }

    public byte[] getTimeStampToken(byte[] messageImprint) throws IOException {
        digest.reset();
        byte[] hash = digest.digest(messageImprint);

        // 32-bit cryptographic nonce
        SecureRandom random = new SecureRandom();
        int nonce = random.nextInt();

        // generate TSA request
        TimeStampRequestGenerator tsaGenerator = new TimeStampRequestGenerator();
        tsaGenerator.setCertReq(true);
        ASN1ObjectIdentifier oid = getHashObjectIdentifier(digest.getAlgorithm());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));


        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());
        TimeStampResponse response;
        try {
            response = new TimeStampResponse(tsaResponse);
            response.validate(request);
        }catch (TSPException e){
            throw  new IOException(e);
        }

        TimeStampToken token = response.getTimeStampToken();
        if (token == null){
            throw new IOException("Response does not have a time stamp token");
        }

        return token.getEncoded();
    }


    private ASN1ObjectIdentifier getHashObjectIdentifier(String algorithm) {
        if (algorithm.equals("MD2")) {
            return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md2.getId());
        } else if (algorithm.equals("MD5")) {
            return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md5.getId());
        } else if (algorithm.equals("SHA-1")){
            return new ASN1ObjectIdentifier(OIWObjectIdentifiers.idSHA1.getId());
        }else if (algorithm.equals("SHA-224")){
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha224.getId());
        }else if (algorithm.equals("SHA-256")){
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId());
        }else if (algorithm.equals("SHA-384")){
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha384.getId());
        }else if (algorithm.equals("SHA-512")){
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha512.getId());
        }else {
            return new ASN1ObjectIdentifier(algorithm);
        }
    }



    private byte[] getTSAResponse(byte[] reqest) throws  IOException{
        Log.d(TAG, "Open connection to TSA server");

        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type","application/timestamp-query");

        Log.d(TAG, "Established connection to TSA server");

        if (username != null && password != null && !username.isEmpty() && !password.isEmpty()){
            connection.setRequestProperty(username, password);
        }

        OutputStream output = null;
        try {
            output = connection.getOutputStream();
            output.write(reqest);
        }finally {
            IOUtils.closeQuietly(output);
        }

        Log.d(TAG, "waiting for response from TSA server");

        InputStream input = null;
        byte[] response;
        try {
            input = connection.getInputStream();
            response = IOUtils.toByteArray(input);
        }finally {
            IOUtils.closeQuietly(input);
        }

        Log.d(TAG, "Receive response from TSA server");
        Log.d(TAG, "Response : "+Arrays.toString(response));

        return response;

    }
}
