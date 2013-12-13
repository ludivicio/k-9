package org.ancode.secmail.crypto.v2;

import org.ancode.secmail.mail.crypto.v2.AESKeyGenerator;
import org.ancode.secmail.mail.crypto.v2.AesCryptor;
import org.ancode.secmail.mail.crypto.v2.CryptorException;
import org.ancode.secmail.mail.crypto.v2.HttpPostServiceV2;
import org.ancode.secmail.mail.crypto.v2.PostResultV2;
import org.ancode.secmail.mail.crypto.v2.RSACryptor;

import android.test.AndroidTestCase;
import android.util.Log;

public class HttpUtilTest extends AndroidTestCase{

	public void testGenerateAesKey() {
		String aesKey = AESKeyGenerator.generateAesKey();
		Log.i("lxc", "aesKey: " + aesKey);
	}
	
	public void testPostRegRequest() {
		
		String mail = "ludivicio@163.com";
		String aesKey = "";
		String key = "";
		
		try {
			key = RSACryptor.getInstance(getContext()).encrypt(aesKey);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("lxc", "encrypt aeskey failed!");
		}
		
		PostResultV2 pr = HttpPostServiceV2.postRegRequest(mail, key);
		
		if(pr.isSuccess()) {
			Log.i("lxc", "申请成功");
			Log.i("lxc", "mail: " + mail);
			Log.i("lxc", "aesKey: " + aesKey);
			Log.i("lxc", "device: " + pr.getDeviceUuid());
		} else {
			Log.e("lxc", "申请失败");
		}
	}
	
	public void testPostRegConfirm() {
		
		String mail = "ludivicio@163.com";
		String aesKey = "";
		String deviceUuid = "";
		String regCode = "";
		String key = null;
		
		try {
			AesCryptor aesCryptor = new AesCryptor(aesKey);
			regCode = aesCryptor.decrypt(regCode);
			String mergeKey = regCode + deviceUuid;
			byte[] encryptData = aesCryptor.encrypt(mergeKey.getBytes());
			key = AesCryptor.byteToHex(encryptData);
		} catch (CryptorException e) {
			Log.e("lxc", "get the encrypted key failed!");
		}

		Log.i("lxc", "aesKey: " + aesKey);
		Log.i("lxc", "regCode: " + regCode);
		Log.i("lxc", "deviceUuid: " + deviceUuid);

		PostResultV2 pr = HttpPostServiceV2.postRegConfirm(mail, key, deviceUuid);
		
		if(pr.isSuccess()) {
			Log.i("lxc", "确认成功");
		}
		
	}
	
}
