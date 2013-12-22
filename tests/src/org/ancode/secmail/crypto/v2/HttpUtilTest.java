package org.ancode.secmail.crypto.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ancode.secmail.mail.crypto.v2.AESKeyGenerator;
import org.ancode.secmail.mail.crypto.v2.AESKeyObject;
import org.ancode.secmail.mail.crypto.v2.AesCryptor;
import org.ancode.secmail.mail.crypto.v2.CryptorException;
import org.ancode.secmail.mail.crypto.v2.HttpPostServiceV2;
import org.ancode.secmail.mail.crypto.v2.PostResultV2;
import org.ancode.secmail.mail.crypto.v2.RSACryptor;

import android.test.AndroidTestCase;
import android.util.Log;

public class HttpUtilTest extends AndroidTestCase{

	public static final String TAG = "lxc2";
	
	public void testGenerateAesKey() {
		String aesKey = AESKeyGenerator.generateAesKey();
		Log.i(TAG, "aesKey: " + aesKey);
	}
	
	public void testPostRegRequest() {
		
		String mail = "ludivicio@163.com";
		String aesKey = "";
		String key = "";
		
		try {
			key = RSACryptor.getInstance(getContext()).encrypt(aesKey);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "encrypt aeskey failed!");
		}
		
		PostResultV2 pr = HttpPostServiceV2.postRegRequest(mail, key);
		
		if(pr.isSuccess()) {
			Log.i(TAG, "申请成功");
			Log.i(TAG, "mail: " + mail);
			Log.i(TAG, "aesKey: " + aesKey);
			Log.i(TAG, "device: " + pr.getDeviceUuid());
		} else {
			Log.e(TAG, "申请失败");
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
			Log.e(TAG, "get the encrypted key failed!");
		}

		Log.i(TAG, "aesKey: " + aesKey);
		Log.i(TAG, "regCode: " + regCode);
		Log.i(TAG, "deviceUuid: " + deviceUuid);

		PostResultV2 pr = HttpPostServiceV2.postRegConfirm(mail, key, deviceUuid);
		
		if(pr.isSuccess()) {
			Log.i(TAG, "确认成功");
		}
		
	}
	
	public void testDecrypt() throws CryptorException {
		String aesKey = "3iFKf0kL0s0Z20p3Nn9641xXH3c55Z91";
		
		String key = "roTAqH90XYVLHEZr0uy5t3Khv5l31ewm";
		
		String regCode = "epIMwXNBu2MuwXpklKs2FiAsy8bBM6Ib";
		
		String verify = null;
		
		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
			Log.e("lxc", "加密前key: " + key);
			Log.e("lxc", "加密后key: " + cryptor.encrypt(key));
			Log.e("lxc", "加密后key: " + new AesCryptor(aesKey).encrypt(key));
		} catch (CryptorException e) {
			Log.e("lxc", "Encrypt failed");
		}
		
		
//		AesCryptor cryptor = new AesCryptor(aesKey);
//		
//		{
//			
//			Log.i("lxc", "" + cryptor.encrypt("aaaaaaaaaaaaaaa"));
//			Log.i("lxc", "" + cryptor.encrypt(key));
//			Log.i("lxc", "第1次加密：" + new AesCryptor(aesKey).encrypt(key));
//			Log.i("lxc", "第2次加密：" + new AesCryptor(aesKey).encrypt(key));
//			Log.i("lxc", "第3次加密：" + new AesCryptor(aesKey).encrypt(key));
//			
//			
//		}
//		
//		
//		String encryptUuid = "501D9C36403D139AFC5C310AB619C64ED91F2F83E7E9CAE6EC3B88E9F396985C";
//		String result = cryptor.decrypt(encryptUuid);
//		
//		Log.i(TAG, "key加密后：" + cryptor.encrypt(key));
//		Log.i(TAG, "key解密后：" + result);
		
	}
	
	public void testSendEmail() {
		
		int id = 2;
		
		String from = "";
		String to = "";
		String aesKey = "";
		String regCode = "";
		String deviceUuid = "";
		
		
		if( id == 1) {
			// 163  给  gmail发邮件流程
			from = "ludivicio@163.com";
			to = "ludivicio@gmail.com";
			aesKey = "vYEDNBtVL8dh122OPv0hUjeVQI1TYetx";
			regCode = "2ktxBIe1MjXiRXGyCPpiWu64gvlYLyQB";
			deviceUuid = "82b8091b-7d53-4812-b329-18a0bb768b95";
		} else {
			// gmail 给 163 发邮件流程
			from = "ludivicio@gmail.com";
			to = "ludivicio@163.com";
			
			aesKey = "3iFKf0kL0s0Z20p3Nn9641xXH3c55Z91";
			regCode = "M2yq10rlKFwc2CEay8m2vzlf7BHCfLt4";
			deviceUuid = "0cdea49c-3074-4086-ad35-b86076c3db50";
		}
		
		List<AESKeyObject> aesKeys = genernateAESKeys(1);
		
		String verify = null;

		try {
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = new AesCryptor(aesKey).encrypt(verify);
			for (int i = 0; i < aesKeys.size(); i++) {
				AESKeyObject aesKeyOjbect = aesKeys.get(i);
				aesKeyOjbect.setEncryptAesKey(new AesCryptor(aesKey).encrypt(aesKeyOjbect.getAesKey()));
			}
		} catch (CryptorException e) {
			Log.e(TAG, "Encrypt failed");
		}

		Log.i(TAG, "------------发送邮件处理--------------");
		
		Log.i(TAG,"from: " + from);
		Log.i(TAG, "to: " + to);
		
		Log.i(TAG, "aesKey: " + aesKey);
		Log.i(TAG, "regCode: " + regCode);
		Log.i(TAG, "deviceUuid: " + deviceUuid);
		
		for (int i = 0; i < aesKeys.size(); i++) {
			AESKeyObject aesKeyOjbect = aesKeys.get(i);
			Log.e(TAG, "uuid" + (i + 1) + ": " + aesKeyOjbect.getUuid());
			Log.e(TAG, "key:" + (i + 1) + ": " + aesKeyOjbect.getAesKey());
			Log.e(TAG, "encryptkey:" + (i + 1) + ": " + aesKeyOjbect.getEncryptAesKey());
		}
		
		PostResultV2 pr = HttpPostServiceV2.postSendEmail(from, to, verify, deviceUuid, aesKeys);
		
		if(pr.isSuccess()) {
			Log.i(TAG, from + " 给 " + to + "发送加密邮件成功");
		}
		
		Log.i(TAG, "----------------------------------");
	}
	
	
	public void testReceiveEmail() {
		
		int id = 2;
		
		String owner = "";
		String aesKey = "";
		String regCode = "";
		String deviceUuid = "";
		
		
		if( id == 1) {
			// 163 给 gmail发邮件流程，接收邮件的是gmail
			owner = "ludivicio@gmail.com";
			aesKey = "3iFKf0kL0s0Z20p3Nn9641xXH3c55Z91";
			regCode = "xLPwc3jLEPUGdkmm8el5pogI271125Oe";
			deviceUuid = "a2de5bdd-bf5c-4bad-9ccd-41dda7a92ad3";
			Log.i(TAG, "接收邮件的是gmail");
		} else {
			// gmail 给 163 发邮件流程，接收邮件的是163
			owner = "ludivicio@163.com";
			aesKey = "vYEDNBtVL8dh122OPv0hUjeVQI1TYetx";
			regCode = "2ktxBIe1MjXiRXGyCPpiWu64gvlYLyQB";
			deviceUuid = "82b8091b-7d53-4812-b329-18a0bb768b95";
			Log.i(TAG, "接收邮件的是163");
		}
		
		String uuid = "10739b71-e474-4b09-acf2-97e6e0df6e22";
		
		Log.i(TAG, "------------接收邮件处理--------------");
		Log.i(TAG, "owner:" + owner);
		Log.i(TAG, "aesKey:" + aesKey);
		Log.i(TAG, "regCode:" + regCode);
		Log.i(TAG, "deviceUuid:" + deviceUuid);
		Log.i(TAG, "uuid:" + uuid);
		
		List<String> uuidList = new ArrayList<String>();
		uuidList.add(uuid);
		
		
		String verify = null;
		AesCryptor cryptor = null;
		try {
			cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
		} catch (CryptorException e) {
			Log.e(TAG, "Encrypt failed");
		}

		PostResultV2 pr = HttpPostServiceV2.postReceiveEmail(owner, verify, deviceUuid, uuidList);
	
		if(pr == null) {
			Log.e(TAG, "发生错误");
		}
		
		
		
		List<String> result = parseAesKeys(pr, cryptor);
		
		for (int i = 0; i < result.size(); i++) {
			Log.e(TAG, "aesKey" + (i + 1) + ": " + result.get(i));
		}
		
		Log.i(TAG, "----------------------------------");
	}
	
	
	private List<AESKeyObject> genernateAESKeys(int size) {
		
		List<String> aesKeys = AESKeyGenerator.getAesKeyList(32, size);
		List<AESKeyObject> aeskeyList = new ArrayList<AESKeyObject>();
		for (int i = 0; i < aesKeys.size(); i++) {
			aeskeyList.add(new AESKeyObject(aesKeys.get(i), UUID.randomUUID().toString()));
		}
		
		return aeskeyList;
	}
	
	private static List<String> parseAesKeys(PostResultV2 pr, AesCryptor cryptor) {
		List<String> aesKeyList = new ArrayList<String>();
		if (pr != null && !pr.getUuidMap().isEmpty()) {
			for (int i = 0; i < pr.getUuidMap().size(); i++) {
				String fromUuid = pr.getUuidMap().get("uuid" + (i + 1));
				try {
					if (cryptor != null) {
						aesKeyList.add(cryptor.decrypt(fromUuid));
					}
				} catch (CryptorException e) {
					e.printStackTrace();
				}
			}
		}
		return aesKeyList;
	}
	
}
