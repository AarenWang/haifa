package org.wrj.haifa.json.jackson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.ZonedDateTime;

public class JsonMapp {

    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        MyPerson p = new MyPerson();
        p.setActiveTime(ZonedDateTime.now());
        p.setName("jack");
        p.setTemperature(37L);
        p.setAge(18);
        String json = objectMapper.writeValueAsString(p);
        System.out.println(json);

    }
}

class MyPerson{
    private String name;

    @JsonFormat(pattern= "yyyy-MM-dd HH:mm:ss.SSS")
    private ZonedDateTime activeTime;

    @JsonIgnore
    private Long  temperature;

    @JsonAlias(value = "age_i")
    private Integer age;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(ZonedDateTime activeTime) {
        this.activeTime = activeTime;
    }

    public Long getTemperature() {
        return temperature;
    }

    public void setTemperature(Long temperature) {
        this.temperature = temperature;
    }
}
