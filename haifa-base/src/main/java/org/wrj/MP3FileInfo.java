package org.wrj;

import java.io.File;
import java.io.FileInputStream;

/**
 *  
 * @author think
 *
 */
public class MP3FileInfo {
	
//	private static  final String mp3_path = "D:/body_language.mp3";
	private static  final String mp3_path = "D:/何龙雨_错过了缘分错过你.mp3";
	
	public static void main(String[] args) throws Exception{
		//printMp3_ID3V1Tag();
		printMp3_ID3V2Tag();
		//printMP3Format();
		
	}
	
	/**
	 * MP3的标签信息位于文件开始处或结尾处，用于表达MP3文件的相关信息，常见的有ID3、APE等。

      ID3 V1 位于文件最后的128字节，如果读取的是网络文件而服务器又不支持随机读取的话，意味着不对对其解析这部分信息。这128字节共表示7个信息：
	[0..2]       3  bytes: ID3 v1标识 -- 'TAG'
	[3..32]     30 bytes: 标题
	[33..62]   30 bytes: 艺术家
	[63..92]   30 bytes: 专辑名
	[93..96]   4  bytes: 发行年份
	[97..126] 30 bytes: v1.0 -- 注释/附加/备注信息
         v1.1 -- 前29 bytes注释/附加/备注信息，最后1 byte音轨信息
	[127]       1  byte : 流派
	 * @throws Exception
	 */
	private static  void printMp3_ID3V1Tag() throws Exception{
		byte[] tags = new byte[128];
		FileInputStream fis = new FileInputStream(new File(mp3_path));
		int available = fis.available();
		fis.skip(available - 128);
		int offset = 0;
		fis.read(tags, offset, 3);
		System.out.println("tag字符:"+byte2String(tags,3));
		fis.read(tags, offset, 30);
		System.out.println("歌曲名:"+byte2String(tags,30));
		
		fis.read(tags, offset, 30);
		System.out.println("作者:"+byte2String(tags,30));
		
		fis.read(tags, offset, 30);
		System.out.println("专辑名:"+byte2String(tags,30));
		
		fis.read(tags, offset, 4);
		System.out.println("年份:"+byte2String(tags,4));
		
		fis.read(tags, offset, 30);
		System.out.println("附注:"+byte2String(tags,30));
		
		fis.read(tags, offset, 1);
		System.out.println("流派:"+tags[0]); // 13 --POP
		
		
		fis.close();
	}
	
	private static  void printMp3_ID3V2Tag() throws Exception{
		byte[] tags = new byte[128];
		FileInputStream fis = new FileInputStream(new File(mp3_path));
		int offset = 0;
		fis.read(tags, offset, 3);
		if("ID3".equals(byte2String(tags,3))){
			fis.read(tags, offset, 1);
			System.out.println("版本:"+tags[0]);
			
			fis.read(tags, offset, 1);
			System.out.println("副版本:"+tags[0]);
			
			fis.read(tags, offset, 1);
			System.out.println("标志:"+tags[0]);
			
			fis.read(tags, offset, 4);
			System.out.println("标签大小:"+byte2String(tags,4));
			
		} else {
			System.out.println("不是ID3V2标签");
		}
		
		
		fis.close();
	}
	
	private static void printMP3Format() throws Exception{
		byte[] tags = new byte[128];
		FileInputStream fis = new FileInputStream(new File(mp3_path));
		int offset = 0;
		tags = new byte[2];
		fis.read(tags, offset, 2);
		 
		System.out.println(byte2String(tags,2));
	}
	
	private static String byte2String(byte[] tags,int length) throws Exception{
		byte[] bits = new byte[length];
		for(int i = 0;i < length; i++){
			bits[i] = tags[i];
		}
		return new String(bits,"GBK");
	}

}
