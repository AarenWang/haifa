package org.wrj.collection;

import java.util.Map;
import java.util.TreeMap;

public class TreeMapTest {
	
	public static void main(String[] args) {
		Map<String,String> map = new TreeMap<>();
		map.put("1F-3A-2C-1A", "1");
		map.put("1F-3A-2C-1B", "1");
		map.put("2F-12A-2C-1C", "1");
		map.put("1F-3A-2C-1C", "1");
		map.put("2F-2A-2C-1C", "1");
		map.put("2F-1A-2C-3C", "1");
		map.put("2F-1A-2C-4D", "1");
		map.put("2F-1A-2C-4C", "1");
		map.put("2F-1A-DC-4C", "1");
		
		for(String key : map.keySet()){
			System.out.println(key);
		}
		
		
	}

}
