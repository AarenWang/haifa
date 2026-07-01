package org.wrj.haifa.ai.deerflow.web;

import org.springframework.web.server.ServerWebExchange;

public class UserIdResolver {

    public static final String USER_ID_HEADER = "X-User-Id";

    public static String resolve(ServerWebExchange exchange) {
        if (exchange == null) {
            return "default-user";
        }
        String headerVal = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        if (headerVal != null && !headerVal.isBlank()) {
            return headerVal;
        }
        return "default-user";
    }
}
