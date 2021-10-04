package org.wrj.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class URLCodeTest {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {
		String st1 = URLEncoder.encode("aa","UTF-8");
		String st2 = URLEncoder.encode("�й�","UTF-8");
		System.out.println("aa��URLEncoder.encode�����:"+st1);
		System.out.println("�й��URLEncoder.encode�����:"+st2);
		
		String st3 =URLDecoder.decode(st1,"UTF-8"); 
		String st4 =URLDecoder.decode(st2,"UTF-8");
		System.out.println(st1+"��URLDecoder.decode�����:"+st3);
		System.out.println(st2+"��URLDecoder.decode�����:"+st4);
		
		System.out.println("E5%95%8A%E5%93%88%E5%93%88"+"��URLDecoder.decode�����:"+URLDecoder.decode("E5%95%8A%E5%93%88%E5%93%88","UTF-8"));
		System.out.println("%E7%AC%A6%E5%AE%97%E9%94%A6"+"��URLDecoder.decode�����:"+URLDecoder.decode("%E7%AC%A6%E5%AE%97%E9%94%A6","UTF-8"));
	}

}
