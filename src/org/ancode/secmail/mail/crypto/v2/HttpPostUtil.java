package org.ancode.secmail.mail.crypto.v2;

import java.util.ArrayList;
import java.util.List;

import org.ancode.secmail.Account;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class HttpPostUtil {

	/**
	 * 
	 * @param account
	 * @param context
	 * @return
	 */
	public static PostResultV2 postRegRequest(Account account, Context context) {

		// 1. Get the aeskey from account, if not existed, generated it.
		String aesKey = account.getAesKey();
		if (TextUtils.isEmpty(aesKey)) {
			aesKey = AESKeyGenerator.generateAesKey();
			account.setAesKey(aesKey);
		}

		// 2. Using the public key to encrypt the aeskey.
		String key = null;
		try {
			key = RSACryptor.getInstance(context).encrypt(aesKey);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("lxc", "encrypt aeskey failed!");
		}

		// 3. Post the request to server.
		return HttpPostServiceV2.postRegRequest(account.getEmail(), key);
	}

	/**
	 * 
	 * @param account
	 * @param regCode
	 * @return
	 */
	public static PostResultV2 postRegConfirm(Account account, String regCode) {
		String aesKey = account.getAesKey();
		if (TextUtils.isEmpty(aesKey)) {
			return null;
		}

		String deviceUuid = account.getDeviceUuid();
		if (TextUtils.isEmpty(deviceUuid)) {
			return null;
		}

		String key = null;
		try {
			AesCryptor aesCryptor = new AesCryptor(aesKey);
			regCode = aesCryptor.decrypt(regCode);
			
			account.setRegCode(regCode);
			
			String mergeKey = regCode + deviceUuid;
			byte[] encryptData = aesCryptor.encrypt(mergeKey.getBytes());
			key = AesCryptor.byteToHex(encryptData);
		} catch (CryptorException e) {
			Log.e("lxc", "get the encrypted key failed!");
		}

//		Log.i("lxc", "aesKey: " + aesKey);
//		Log.i("lxc", "regCode: " + regCode);
//		Log.i("lxc", "deviceUuid: " + deviceUuid);

		return HttpPostServiceV2.postRegConfirm(account.getEmail(), key,
				deviceUuid);
	}

	/**
	 * 
	 * @param account
	 * @param type
	 * @param verification
	 * @return
	 */
	public static PostResultV2 postProtectRequest(Account account, String type, String verification) {
		
		String email = account.getEmail();
		String aesKey = account.getAesKey();
		String regCode = account.getRegCode();
		String deviceUuid = account.getDeviceUuid();
		String verify = null;
		String protect = null;
		
		if (TextUtils.isEmpty(aesKey)) {
			return null;
		}
		if (TextUtils.isEmpty(deviceUuid)) {
			return null;
		}
		if(TextUtils.isEmpty(regCode)) {
			return null;
		}
		
//		Log.i("lxc", "email: " + email);
//		Log.i("lxc", "aesKey: " + aesKey);
//		Log.i("lxc", "regCode: " + regCode);
//		Log.i("lxc", "deviceUuid: " + deviceUuid);
//		Log.i("lxc", "protect: " + protect);
		
		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
			protect = cryptor.encrypt(verification);
		} catch (CryptorException e) {
			Log.e("lxc", "get the encrypted key failed!");
		}

		Log.i("lxc", "verify: " + verify);
		
		return HttpPostServiceV2.postProtectRequest(email, type, protect, verify, deviceUuid);
	}

	/**
	 * 
	 * @param account
	 * @param from
	 * @param to
	 * @param aesKeyList
	 * @return
	 */
	public static PostResultV2 postSendEmail(Account account, String from, String to, List<AESKeyObject> aesKeyList) {

		String aesKey = account.getAesKey();
		String regCode = account.getRegCode();
		String deviceUuid = account.getDeviceUuid();

		int size = aesKeyList.size();
		String[] uuids = new String[size];
		String[] keys = new String[size];

		String verify = null;

		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
			for (int i = 0; i < size; i++) {
				AESKeyObject aesKeyOjbect = aesKeyList.get(i);
				uuids[i] = aesKeyOjbect.getUuid();
				keys[i] = cryptor.encrypt(aesKeyOjbect.getAesKey());
			}
		} catch (CryptorException e) {
			Log.e("lxc", "Encrypt failed");
		}

//		Log.i("lxc","from: " + from);
//		Log.i("lxc", "to: " + to);
//		Log.i("lxc", "aesKey: " + aesKey);
//		Log.i("lxc", "regCode: " + regCode);
//		Log.i("lxc", "deviceUuid: " + deviceUuid);
//		for( int i = 0; i < size; i ++) {
//			Log.i("lxc", "uuid" + (i + 1) + ": " + uuids[i]);
//			Log.i("lxc", "key:" + (i + 1) + ": " + keys[i]);
//		}
		
		return HttpPostServiceV2.postSendEmail(from, to, verify, deviceUuid, uuids, keys);
	}

	/**
	 * 
	 * @param account
	 * @param uuidList
	 * @return
	 * @throws InvalidKeyCryptorException
	 */
	public static List<String> postReceiveEmail(Account account, List<String> uuidList) throws InvalidKeyCryptorException {

		String owner = account.getEmail();
		String aesKey = account.getAesKey();
		String regCode = account.getRegCode();
		String deviceUuid = account.getDeviceUuid();

		String verify = null;
		AesCryptor cryptor = null;
		try {
			cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
		} catch (CryptorException e) {
			Log.e("lxc", "Encrypt failed");
		}

		PostResultV2 pr = HttpPostServiceV2.postReceiveEmail(owner, verify, deviceUuid, uuidList);
		
		if( pr.isInvalidKey() ) {
			throw new InvalidKeyCryptorException("");
		}
		
		return parseAesKeys(pr, cryptor);
	}

	/**
	 * 
	 * @param pr
	 * @param cryptor
	 * @return
	 */
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
