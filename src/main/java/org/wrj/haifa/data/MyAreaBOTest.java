package org.wrj.haifa.data;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyAreaBOTest {

    public static void main(String[] args) throws IOException  {

        List<MyAreaBO> list =  new ArrayList<>();
        for(int i = 0; i < 1_000_000; i++){

            MyAreaBO  myAreaBO = new MyAreaBO();
            myAreaBO.setCanAdd(true);
            myAreaBO.setCanDelete(true);
            myAreaBO.setCanUpdate(true);
            myAreaBO.setDelete(true);
            myAreaBO.setId(1);
            myAreaBO.setName("杭州");
            myAreaBO.setNationalCode("86");
            myAreaBO.setParentId(10);
            myAreaBO.setParentRegionCode("057");
            myAreaBO.setRegionCode("0571");
            myAreaBO.setPathCode("057/0571");
            myAreaBO.setSortCode(1);
            myAreaBO.setStaffCreated(1000L);
            myAreaBO.setStaffModified(20000L);
            myAreaBO.setType(10);

            list.add(myAreaBO);
        }

        FileWriter fileWriter = new FileWriter(System.getProperty("user.home")+ File.separator+"area.txt");
        IOUtils.writeLines(list,"\r\n",fileWriter);

        fileWriter.flush();
        fileWriter.close();
    }
}
