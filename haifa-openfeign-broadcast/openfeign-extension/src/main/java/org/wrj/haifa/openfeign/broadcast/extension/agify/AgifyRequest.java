package org.wrj.haifa.openfeign.broadcast.extension.agify;

import java.util.Objects;

/**
 * Request object for the Agify public API.
 */
public class AgifyRequest {

    private final String name;
    private final String countryId;

    public AgifyRequest(String name, String countryId) {
        this.name = name;
        this.countryId = countryId;
    }

    public static AgifyRequest ofName(String name) {
        return new AgifyRequest(name, null);
    }

    public String getName() {
        return name;
    }

    public String getCountryId() {
        return countryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AgifyRequest that = (AgifyRequest) o;
        return Objects.equals(name, that.name) && Objects.equals(countryId, that.countryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, countryId);
    }

    @Override
    public String toString() {
        return "AgifyRequest{" +
                "name='" + name + '\'' +
                ", countryId='" + countryId + '\'' +
                '}';
    }
}
