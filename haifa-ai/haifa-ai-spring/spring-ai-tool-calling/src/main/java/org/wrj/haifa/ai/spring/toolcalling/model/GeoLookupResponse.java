package org.wrj.haifa.ai.spring.toolcalling.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of the payload returned by the geography knowledge API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoLookupResponse(
        @JsonProperty("title") String title,
        @JsonProperty("summary") String summary,
        @JsonProperty("highlights") List<String> highlights) {

    public static GeoLookupResponse empty(String location) {
        return new GeoLookupResponse(location, "", Collections.emptyList());
    }

    public GeoLookupResponse {
        highlights = highlights == null ? Collections.emptyList() : new ArrayList<>(highlights);
    }
}
