package org.ancode.secmail.mail.crypto.v2;

import java.net.InetAddress;

public class Convert {
	public static byte[] intToByteArray(final int integer) {
		int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;
		byte[] byteArray = new byte[4];

		for (int n = 0; n < byteNum; n++)
			byteArray[3 - n] = (byte) (integer >>> (n * 8));

		return (byteArray);
	}

	public static void longToByteArray(final long value, byte[] result) {
		long byteNum = (40 - Long.numberOfLeadingZeros(value < 0 ? ~value : value)) / 8;

		for (int n = 0; n < byteNum; n++)
			result[7 - n] = (byte) (value >>> (n * 8));

	}

	public static void intToByteArray(byte[] buf, final int integer, final int offset) {
		int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;

		for (int n = 0; n < byteNum; n++)
			buf[offset + 3 - n] = (byte) (integer >>> (n * 8));
	}

	public static void shortToByteArray(byte[] buf, final int integer, final int offset) {
		buf[offset] = (byte) (integer & 0xff);
		buf[offset + 1] = (byte) ((integer >> 8) & 0xff);
	}

	/**
	 * Converts a 4 byte array of unsigned bytes to an long
	 * 
	 * @param b
	 *            an array of 4 unsigned bytes
	 * @return a long representing the unsigned int
	 */
	public static final int byteArrayToInt(byte[] b, final int offset) {
		int l = 0;
		l |= b[offset] & 0xFF;
		l <<= 8;
		l |= b[offset + 1] & 0xFF;
		l <<= 8;
		l |= b[offset + 2] & 0xFF;
		l <<= 8;
		l |= b[offset + 3] & 0xFF;
		return l;
	}

	public static final int byteArrayToInt(byte[] b) {
		int l = 0;
		l |= b[0] & 0xFF;
		l <<= 8;
		l |= b[1] & 0xFF;
		l <<= 8;
		l |= b[2] & 0xFF;
		l <<= 8;
		l |= b[3] & 0xFF;
		return l;
	}

	public static int ipAddress2Int(String address) throws NumberFormatException {
		InetAddress inetAddress;
		// let's try to resolve IP address - eventually we need 4/6 bytes
		// representation
		try {
			inetAddress = InetAddress.getByName(address);
		} catch (Exception ex) // probably failed to resolve the host
		{
			return 0;
		}

		// we have host IP address
		byte rawAddress[] = inetAddress.getAddress();
		int ipAddress = 0;
		for (int i = 0; i < rawAddress.length; i++) {
			int temp = ((rawAddress[i]) & 0xFF);
			temp = temp << (i * 8);
			ipAddress |= temp;
		}
		return ipAddress;
	}

	public static String ipAddress2string(int address) {
		String s = "" + ((address >> 0x00) & 0xFF) + "." + ((address >> 0x08) & 0xFF) + "."
				+ ((address >> 0x10) & 0xFF) + "." + ((address >> 0x18) & 0xFF);
		return s;
	}

	/*
	 * public static byte[] hexStringToByte(String hexString) { byte[] hexByte =
	 * null; try { hexByte = new BigInteger(hexString, 16).toByteArray(); }
	 * catch (NullPointerException e) { e.printStackTrace(); } catch
	 * (NumberFormatException e) { e.printStackTrace(); }
	 * 
	 * return hexByte; }
	 */
	public static byte[] hexToBytes(String sHex) {
		char[] hex = sHex.toCharArray();
		int length = hex.length / 2;
		byte[] raw = new byte[length];
		for (int i = 0; i < length; i++) {
			int high = Character.digit(hex[i * 2], 16);
			int low = Character.digit(hex[i * 2 + 1], 16);
			int value = (high << 4) | low;
			if (value > 127)
				value -= 256;
			raw[i] = (byte) value;
		}
		return raw;
	}

}
