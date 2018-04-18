package org.wrj.haifa.lang.generic;

/**
 * Created by wangrenjun on 2018/2/1.
 */
public class Report implements Readable {

    private String name;

    private String reportData;

    public Report(String name, String reportData) {
        this.name = name;
        this.reportData = reportData;
    }

    @Override
    public String readInfo() {
        return toString();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReportData() {
        return reportData;
    }

    public void setReportData(String reportData) {
        this.reportData = reportData;
    }

    @Override
    public String toString() {
        return "Report{" +
                "name='" + name + '\'' +
                ", reportData='" + reportData + '\'' +
                '}';
    }
}
