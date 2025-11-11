package org.wrj.haifa.openfeign.broadcast.extension.agify;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * OpenFeign client that invokes the public https://api.agify.io endpoint to estimate the age
 * of a person based on their name and optional ISO country identifier.
 */
@FeignClient(
        name = "agifyClient",
        url = "${agify.client.base-url:https://api.agify.io}",
        configuration = AgifyClientConfiguration.class
)
public interface AgifyClient {

    /**
     * Predicts the age distribution for the supplied request.
     *
     * @param request request parameters mapped to query parameters via the custom {@link org.springframework.cloud.openfeign.SpringQueryMap} encoder
     * @return the response containing the aggregated demographic information
     */
    @GetMapping(path = "/")
    AgifyResponse predictAge(@SpringQueryMap AgifyRequest request);
}
