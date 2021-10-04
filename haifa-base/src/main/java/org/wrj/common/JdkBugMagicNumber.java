package org.wrj.common;

public class JdkBugMagicNumber {
	public static void main(String[] args) {
		String magicString = "2.2250738585072012e-308";
		Double magicResult = Double.valueOf(magicString);
		System.out.println(magicResult);
	}
}