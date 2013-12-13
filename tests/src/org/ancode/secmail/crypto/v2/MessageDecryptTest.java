package org.ancode.secmail.crypto.v2;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ancode.secmail.mail.crypto.v2.AesCryptor;
import org.ancode.secmail.mail.crypto.v2.CryptorException;

import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

public class MessageDecryptTest extends AndroidTestCase {

	public void testDecryptMessage() throws Exception {
		String uriString = "content://org.ancode.secmail.attachmentprovider/aef0dd3d-8fb2-401c-952a-598b9240718e/29/RAW";
		Uri uri = Uri.parse(uriString);
		String aesKey = "10p1xW5pe8Uem0M31e0Y2tZH59x76714";
		InputStream in = mContext.getContentResolver().openInputStream(uri);
		OutputStream out = new ByteArrayOutputStream();

		byte[] buf = new byte[1024];
		int numRead = 0;
		while ((numRead = in.read(buf)) >= 0) {
			out.write(buf, 0, numRead);
		}
		
//		AesCryptor cryptor = new AesCryptor(aesKey);
//		cryptor.decrypt(in, out);

		String msg = out.toString();
		Log.i("lxc", "msg:" + msg);
	}
}
