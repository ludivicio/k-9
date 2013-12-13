package org.ancode.secmail.mail.crypto.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;

import org.ancode.secmail.mail.filter.Base64;

import android.content.Context;
import android.content.res.AssetManager;

public class RSACryptor {

	// Define the algorithm constant.
	private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
	private static Context context;

	// Private constructor prevents instantiation from other classes
	private RSACryptor() {

	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		public static final RSACryptor INSTANCE = new RSACryptor();
	}

	public static RSACryptor getInstance(Context context) {
		RSACryptor.context = context;
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Get public key from the assets file.
	 * 
	 * @return public key string
	 * @throws IOException
	 */
	private String readPublicKey() throws IOException {
		AssetManager am = context.getApplicationContext().getAssets();
		InputStream is = am.open("rsa.key");
		int len = -1;
		byte[] data = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((len = is.read(data, 0, 1024)) != -1) {
			baos.write(data, 0, len);
		}
		String key = new String(baos.toByteArray());
		is.close();
		baos.close();
		return key;
	}

	/**
	 * Get the public key object.
	 * 
	 * @return the key object
	 * @throws IOException
	 */
	public PublicKey getPublicKey() throws IOException {
		return getPublicKey(readPublicKey());
	}

	/**
	 * Get the public key from the public key string, and turn it to object.
	 * 
	 * @param publicKeyText
	 *            public key string
	 * @return the key object
	 */
	private PublicKey getPublicKey(String publicKeyText) {
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(
					publicKeyText.getBytes());
			CertificateFactory factory = CertificateFactory
					.getInstance("X.509");
			java.security.cert.Certificate xc = factory.generateCertificate(is);
			PublicKey publicKey = xc.getPublicKey();
			return publicKey;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Use public key to encrypt the content.
	 */
	public String encrypt(String aesKey) throws Exception {
		return encrypt(getPublicKey(), aesKey);
	}

	/**
	 * Use public key to encrypt the content.
	 */
	public String encrypt(PublicKey publicKey, String content) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] b = content.getBytes();
		byte[] data = cipher.doFinal(b);
		return new String(Base64.encodeBase64(data));
	}

}
