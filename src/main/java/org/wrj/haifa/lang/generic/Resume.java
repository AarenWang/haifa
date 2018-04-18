package org.wrj.haifa.lang.generic;

/**
 * Created by wangrenjun on 2018/2/1.
 */
public class Resume implements Readable {

    private String name;

    private String specialty;

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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    @Override
    public String toString() {
        return "Resume{" + "name='" + name + '\'' + ", specialty='" + specialty + '\'' + '}';
    }
}
