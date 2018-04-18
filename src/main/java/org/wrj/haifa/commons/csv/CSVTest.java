package org.wrj.haifa.commons.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;

/**
 * Created by wangrenjun on 2017/5/22.
 */
public class CSVTest {

    public static void main(String[] args) throws  Exception {
        System.getenv();
        Reader in = new FileReader("data/file.csv");
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
        for (CSVRecord record : records) {
            String lastName = record.get("Last Name");
            String firstName = record.get("First Name");
            System.out.printf("lastName =  %s, firstName= %s",lastName,firstName);
        }

    }
}
