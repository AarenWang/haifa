package org.wrj.haifa.lang.generic;

/**
 * Created by wangrenjun on 2018/2/1.
 */
public class ReadableService {


    public <T extends Readable> String getReadableInfo(T r) {
        String info = r.readInfo();
        return info;
    }

    public <T extends  Readable> Class<? extends Readable>  getClass(T r){
        return  r.getClass();
    }
}
