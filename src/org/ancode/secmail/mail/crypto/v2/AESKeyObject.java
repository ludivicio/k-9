package org.ancode.secmail.mail.crypto.v2;

public class AESKeyObject {
	
	private String uuid;
	private String aesKey;
	private String encryptAesKey;
	
	public AESKeyObject(String aesKey, String uuid){
		this.aesKey = aesKey;
		this.uuid = uuid;
	}
	
	public String getAesKey() {
		return aesKey;
	}
	public void setAesKey(String aesKey) {
		this.aesKey = aesKey;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getEncryptAesKey() {
		return encryptAesKey;
	}

	public void setEncryptAesKey(String encryptAesKey) {
		this.encryptAesKey = encryptAesKey;
	}
	
}
