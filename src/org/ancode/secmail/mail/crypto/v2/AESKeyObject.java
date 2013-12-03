package org.ancode.secmail.mail.crypto.v2;

public class AESKeyObject {
	
	private String aesKey;
	private String uuid;
	
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
	
}
