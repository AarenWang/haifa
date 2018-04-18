package org.wrj.haifa.lang.generic;

/**
 * Created by wangrenjun on 2018/2/1.
 */
public class ReadableUtil {

    public static void main(String[] args) {

        ReadableService readableService = new ReadableService();

        Readable readable = new Report("My Report","Data is mock");
        String info = readableService.getReadableInfo(readable);
        System.out.println(info);

        //Class<Readable> clazz = readableService.getClass(readable);

    }
}
