package org.wrj.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormaTest {
	
	public static void main(String[] args) throws ParseException {
		//test1();
		test2();
	}

	
	static void test1() throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date now = new Date();
		String dateStr = sdf.format(now);
		System.out.println(dateStr);
		
		sdf = new SimpleDateFormat("F");
		System.out.println(sdf.format(now));
		
		sdf = new SimpleDateFormat("F");
		System.out.println(sdf.format(now));
		
		String todayStr = "2014-05-25";
		Date today = sdf.parse(todayStr);
		System.out.println(today);
	}
	
	static void test2() throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		Date now = new Date();
		String dateStr = sdf.format(now);
		System.out.println("yyyy:"+dateStr);
		
		sdf = new SimpleDateFormat("yy");
		dateStr = sdf.format(now);
		System.out.println("yy:"+dateStr); //14
		
		sdf = new SimpleDateFormat("WF");
		dateStr = sdf.format(now);
		System.out.println("WF:"+dateStr); //54
		
		dateStr = "35-01-01"; //34-2034  35-1935
		sdf = new SimpleDateFormat("yy-MM-dd");
		Date date = sdf.parse(dateStr);
		System.out.println(date);
		
		
		
	}
}
