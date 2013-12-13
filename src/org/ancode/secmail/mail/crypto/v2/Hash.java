package org.ancode.secmail.mail.crypto.v2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

public class Hash {
	// Private constructor prevents instantiation from other classes
	private Hash() {
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		public static final Hash INSTANCE = new Hash();
	}

	public static Hash getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public String convertToHex(byte[] data, int length) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public String convertToHex(byte[] data) {
		return convertToHex(data, data.length);
	}

	public String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		return convertToHex(md.digest());
	}

	public byte[] SHA1(byte[] text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("SHA-1");
		md.update(text, 0, text.length);
		return md.digest();
	}

	public byte[] SHA256(byte[] text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance("SHA-256");
		md.update(text, 0, text.length);
		return md.digest();
	}
	
	public String SHA256(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		byte[] bt = text.getBytes("utf-8");
		return convertToHex(SHA256(bt));
	}

	public byte[] FileSHA1(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");

		InputStream fis = new FileInputStream(file);

		byte[] buffer = new byte[4096];
		int numRead;
		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				md.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		fis.close();
		return md.digest();
	}

	private void FileAdler32(File file, byte[] result) {
		try {
			// Compute Adler-32 checksum
			CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new Adler32());
			byte[] tempBuf = new byte[4096];
			while (cis.read(tempBuf) >= 0) {

			}
			long checksum = cis.getChecksum().getValue();
			cis.close();
			Convert.longToByteArray(checksum, result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] FileAdler32(File file) {
		byte[] result = new byte[20];
		FileAdler32(file, result);
		return result;
	}

	public void Adler32(byte[] text, byte[] result) {
		// Compute Adler-32 checksum
		Checksum checksumEngine = new Adler32();
		checksumEngine.update(text, 0, text.length);
		long checksum = checksumEngine.getValue();
		// The checksum engine can be reused again for a different byte array by
		// calling reset()
		Convert.longToByteArray(checksum, result);
	}

	public void Adler32(byte[] data, long dataLength, byte[] result) {
		// Compute Adler-32 checksum
		Checksum checksumEngine = new Adler32();
		checksumEngine.update(data, 0, (int) dataLength);
		long checksum = checksumEngine.getValue();
		// The checksum engine can be reused again for a different byte array by
		// calling reset()
		Convert.longToByteArray(checksum, result);
	}

}
