package org.ancode.secmail.mail.crypto.v2;

import java.util.ArrayList;
import java.util.List;

import org.ancode.secmail.Account;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class HttpPostUtil {

	public static final String TAG = "HttpPostUtil";
	
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
			Log.e(TAG, "encrypt aeskey failed!");
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
			AesCryptor cryptor = new AesCryptor(aesKey);
			regCode = cryptor.decrypt(regCode);
			account.setRegCode(regCode);
			String mergeKey = regCode + deviceUuid;
			byte[] encryptData = cryptor.encrypt(mergeKey.getBytes());
			key = AesCryptor.byteToHex(encryptData);
		} catch (CryptorException e) {
			Log.e(TAG, "get the encrypted key failed!");
		}

//		Log.i(TAG, "aesKey: " + aesKey);
//		Log.i(TAG, "regCode: " + regCode);
//		Log.i(TAG, "deviceUuid: " + deviceUuid);

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
		
		try {
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = new AesCryptor(aesKey).encrypt(verify);
			protect = new AesCryptor(aesKey).encrypt(verification);
		} catch (CryptorException e) {
			Log.e(TAG, "get the encrypted key failed!");
		}
		
//		Log.i(TAG, "email: " + email);
//		Log.i(TAG, "aesKey: " + aesKey);
//		Log.i(TAG, "regCode: " + regCode);
//		Log.i(TAG, "deviceUuid: " + deviceUuid);
//		Log.i(TAG, "protect: " + protect);
//		Log.i(TAG, "verify: " + verify);
		
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

		String verify = null;

		try {
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = new AesCryptor(aesKey).encrypt(verify);
			for (AESKeyObject aesKeyObject : aesKeyList) {
				aesKeyObject.setEncryptAesKey(new AesCryptor(aesKey).encrypt(aesKeyObject.getAesKey()));
			}
		} catch (CryptorException e) {
			Log.e(TAG, "Encrypt failed");
		}

		Log.i(TAG,"from: " + from);
		Log.i(TAG, "to: " + to);
		Log.i(TAG, "aesKey: " + aesKey);
		Log.i(TAG, "regCode: " + regCode);
		Log.i(TAG, "deviceUuid: " + deviceUuid);
		for(int i = 0; i < aesKeyList.size(); i ++) {
			Log.e(TAG, "uuid" + (i + 1) + ": " + aesKeyList.get(i).getUuid());
			Log.e(TAG, "key" + (i + 1) + ": " + aesKeyList.get(i).getAesKey());
			Log.e(TAG, "encryptkey" + (i + 1) + ": " + aesKeyList.get(i).getEncryptAesKey());
		}
		
		return HttpPostServiceV2.postSendEmail(from, to, verify, deviceUuid, aesKeyList);
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
		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
		} catch (CryptorException e) {
			Log.e(TAG, "Encrypt failed");
		}

		PostResultV2 pr = HttpPostServiceV2.postReceiveEmail(owner, verify, deviceUuid, uuidList);
		
		if( pr == null ) {
			return null;
		}
		
		if( pr.isInvalidKey() ) {
			throw new InvalidKeyCryptorException("");
		}
		
		List<String> aesKeyList = parseAesKeys(pr, aesKey);
		
		Log.i(TAG, "owner:" + owner);
		Log.i(TAG, "aesKey:" + aesKey);
		Log.i(TAG, "regCode:" + regCode);
		Log.i(TAG, "deviceUuid:" + deviceUuid);
		
		for (int i = 0; i < uuidList.size(); i++) {
			Log.e(TAG, "uuid" + (i + 1) + ": " + uuidList.get(i));
			Log.e(TAG, "key" + (i + 1) + ": " + aesKeyList.get(i));
		}
		
		return aesKeyList;
	}

	/**
	 * 
	 * @param pr
	 * @param cryptor
	 * @return
	 */
	private static List<String> parseAesKeys(PostResultV2 pr, String aesKey) {
		List<String> aesKeyList = new ArrayList<String>();
		if (pr != null && !pr.getUuidMap().isEmpty()) {
			for (int i = 0; i < pr.getUuidMap().size(); i++) {
				String fromUuid = pr.getUuidMap().get("uuid" + (i + 1));
				try {
					aesKeyList.add(new AesCryptor(aesKey).decrypt(fromUuid));
				} catch (CryptorException e) {
					e.printStackTrace();
				}
			}
		}

		return aesKeyList;
	}
}
