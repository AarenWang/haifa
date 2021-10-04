package org.wrj.allspring.jpainternal.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "tb_student")
public class Student {

    @Id
    @GeneratedValue
    private Long id;

    //学生证号
    private String idNum;

    //姓名
    private String fullName;

    //入学成绩总分
    private Integer score;

    //生日
    private Date birthday;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getIdNum() {
        return idNum;
    }

    public void setIdNum(String idNum) {
        this.idNum = idNum;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", idNum='" + idNum + '\'' +
                ", fullName='" + fullName + '\'' +
                ", score=" + score +
                ", birthday=" + birthday +
                '}';
    }
}
