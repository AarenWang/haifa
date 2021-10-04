package org.wrj.collection;

import java.util.BitSet;

public class BitSetTest {
	
	public static void main(String[] args) {
		BitSet set1 = new BitSet(8);
		set1.set(0, true);
		set1.set(2, true);
		System.out.printf("%s %s %s \n", set1.get(0),set1.get(1),set1.get(2));
		for(int i = 0; i < set1.length();i++){
			System.out.println(set1.get(i));
		}
		
		BitSet set2 = new BitSet(8);
		set2.set(1, true);
		set2.set(3, true);
		
		set1.or(set2);
		System.out.println("OR result");
		for(int i = 0; i < set1.length();i++){
			System.out.println(set1.get(i));
		}
		
		System.out.println("set1 toString = "+ set1);
		
		set2 = new BitSet(128);
		set2.set(127, true);
		set2.set(89, true);
		
		set1.or(set2);
		System.out.println("OR result");
		for(int i = 0; i < set1.length();i++){
			System.out.println(set1.get(i));
		}
		
		System.out.println("set1 toString = "+ set1);
		
	}

}
