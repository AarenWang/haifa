package org.wrj.collection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EnumMapTest {
	
	public static void main(String[] args) {
		EnumMap<WEEK, String> map = new EnumMap<>(WEEK.class);
		map.put(WEEK.MONDAY,"1");
		
		if(map instanceof Map<?,?>){
			System.out.println("true");
		} else {
			System.out.println("false");
		}
		
		//List<?> list = new ArrayList<String>();
		//list.add("String");
		//提示The method add(capture#1-of ?) in the type List<capture#1-of ?> is not applicable for the arguments (String)
		
		List<? extends Object> list2 = new ArrayList<>();
		//list2.add(new Object());
		
		//EnumMap map2 = new EnumMap(WEEK.class);
		//map2.put(FRUIT.APPLE, "33");
		//System.out.println(map2.get(FRUIT.APPLE));
		
	}
}




enum WEEK {
	MONDAY,
	TUESDAY,
	WEDNESDAY,
	THURSDAY
}

enum FRUIT {
	APPLE,
	ORAGE
}
