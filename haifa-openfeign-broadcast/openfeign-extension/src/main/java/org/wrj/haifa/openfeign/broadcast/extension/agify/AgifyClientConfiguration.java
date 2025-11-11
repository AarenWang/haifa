package org.wrj.haifa.openfeign.broadcast.extension.agify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.codec.EncodeException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Custom OpenFeign configuration showcasing how to plug a {@link QueryMapEncoder} so that the
 * {@link AgifyRequest} can be supplied as a Java object while the encoder maps its fields to the
 * query parameters expected by the remote HTTPS API.
 */
@Configuration(proxyBeanMethods = false)
public class AgifyClientConfiguration {

    @Bean
    public QueryMapEncoder agifyQueryMapEncoder() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return new JacksonSnakeCaseQueryMapEncoder(mapper);
    }

    @Bean
    public Logger.Level agifyLoggerLevel() {
        return Logger.Level.BASIC;
    }

    private static final class JacksonSnakeCaseQueryMapEncoder implements QueryMapEncoder {

        private final ObjectMapper objectMapper;

        private JacksonSnakeCaseQueryMapEncoder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Map<String, Object> encode(Object object) throws EncodeException {
            if (object == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> parameters = objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
            });
            return parameters.entrySet()
                    .stream()
                    .filter(entry -> Objects.nonNull(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
