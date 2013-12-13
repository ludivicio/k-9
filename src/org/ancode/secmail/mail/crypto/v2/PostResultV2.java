package org.ancode.secmail.mail.crypto.v2;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class PostResultV2 implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String OK = "ok";
	private static final String INVALID_KEY = "invalid key";
	private static final String PROTECT_ENABLED = "protect_enabled";
	private Map<String, String> uuidMap;
	
	private String resultCode;
	private String aesKey;
	private String deviceUuid;
	private String invalidKey;
	private Object obj;
	
	
	public PostResultV2(){
		this.uuidMap = new TreeMap<String, String>();
	}
	public String getResultCode() {
		return resultCode;
	}
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}
	
	public Map<String, String> getUuidMap() {
		return uuidMap;
	}
	public void setUuidMap(Map<String, String> uuidMap) {
		this.uuidMap = uuidMap;
	}
	
	public boolean isSuccess(){
		return resultCode != null && resultCode.equalsIgnoreCase(OK);
	}
	
	public boolean hasProtected(){
		return resultCode != null && resultCode.equalsIgnoreCase(PROTECT_ENABLED);
	}
	
	public String getInvalidKey() {
		return invalidKey;
	}
	public void setInvalidKey(String invalidKey) {
		this.invalidKey = invalidKey;
	}
	
	public boolean isInvalidKey(){
		return invalidKey != null && invalidKey.equalsIgnoreCase(INVALID_KEY);
	}
	
	public String getAesKey() {
		return aesKey;
	}
	public void setAesKey(String aesKey) {
		this.aesKey = aesKey;
	}
	
	public String getDeviceUuid() {
		return deviceUuid;
	}
	public void setDeviceUuid(String deviceUuid) {
		this.deviceUuid = deviceUuid;
	}
	
	public Object getExtraData() {
		return obj;
	}
	public void setExtraData(Object obj) {
		this.obj = obj;
	}
	
	@Override
	public String toString() {
		return "PostResultV2 ["
				+ "\n resultCode=" + resultCode 
				+ "\n aesKey=" + aesKey
				+ "\n deviceUuid=" + deviceUuid 
				+ "\n invalidKey=" + invalidKey
				+ "\n uuidMap=" + uuidMap
				+ "\n ]";
	}
	
	
	
}
