package org.wrj.common;

public class Encode {

	public static String encodeByBase64(String str) {
		if (str == null) {
			return null;
		}
		return (new sun.misc.BASE64Encoder()).encode(str.getBytes());
	}

	public static String decodeFromBase64(String str) {
		if (str == null) {
			return null;
		}
		sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
		try {
			byte[] b = decoder.decodeBuffer(str);
			return new String(b);
		} catch (Exception e) {
			return null;
		}
	}

}
