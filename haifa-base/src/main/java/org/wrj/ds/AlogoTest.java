package org.wrj.ds;

import java.util.HashMap;
import java.util.Map;

public class AlogoTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(oneCount(2500));
		//System.out.println(oneCount2(5000));
		System.out.println(oneCount3(100000000));

	}
	
	private static int nCount(int n){
		String nStr = Integer.toString(n);
		int nCount = 0;
		for(int i = 0; i < nStr.length(); i++){
			if(nStr.charAt(i) == '1'){
				nCount++;
			}
		}
		return nCount;
	}
	
	/*
	 * 计算从1 到整数N 包含数字1的个数相加
	 */
	public static int  oneCount(int n){
		if( n <= 0){
			throw new IllegalArgumentException("n must > 0 ,but is " + n);
		}
		if(n == 1){
			return 1;
		}  else {
			return nCount(n) + oneCount(n -1 );
		}
	}
	
	public static int oneCount2(int n){
		Map<String,Integer> map  = new HashMap<String,Integer>();
		map.put("acc", 0);
		oneCount2Acc(n,map);
		return map.get("acc");
	}
	
	private static void oneCount2Acc(int n, Map<String,Integer> map){
		if( n <= 0){
			throw new IllegalArgumentException("n must > 0 ,but is " + n);
		}
		if(n == 1){
			map.put("acc", map.get("acc") + 1);
		} 
		else {
			
			map.put("acc", map.get("acc") + nCount(n));
			oneCount2Acc(n -1 , map);
		}
	}
	
	public static int oneCount3(int n){
		if( n <= 0){
			throw new IllegalArgumentException("n must > 0 ,but is " + n);
		}
		int count = 0;
		for(int i =0; i <= n; i++){
			count += nCount(i);
		}
		return count;
	}

}
