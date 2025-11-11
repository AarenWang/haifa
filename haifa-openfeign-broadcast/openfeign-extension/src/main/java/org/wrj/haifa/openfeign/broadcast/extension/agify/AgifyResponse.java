package org.wrj.haifa.openfeign.broadcast.extension.agify;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload returned by the Agify public API.
 */
public class AgifyResponse {

    private String name;

    private Integer age;

    private Integer count;

    @JsonProperty("country_id")
    private String countryId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }
}
