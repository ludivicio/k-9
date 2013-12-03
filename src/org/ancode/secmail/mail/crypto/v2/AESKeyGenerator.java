package org.ancode.secmail.mail.crypto.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AESKeyGenerator {

	public static String generateAesKey() {
		return generateRandomString(32);
	}

	private static String generateRandomString(int length) {
		char[] chars = new char[length];
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			switch (random.nextInt(3)) {
			case 0:
				chars[i] = (char) ('A' + random.nextInt(26));
				break;
			case 1:
				chars[i] = (char) ('a' + random.nextInt(26));
				break;
			case 2:
				chars[i] = (char) ('0' + random.nextInt(10));
				break;
			}
		}

		return new String(chars);

	}

	public static List<String> getAesKeyList(int length, int size) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			list.add(generateRandomString(length));
		}
		return list;
	}
}
