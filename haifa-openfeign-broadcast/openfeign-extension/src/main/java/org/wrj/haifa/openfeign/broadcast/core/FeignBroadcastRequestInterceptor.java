package org.wrj.haifa.openfeign.broadcast.core;

import feign.MethodMetadata;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.lang.reflect.Method;

/**
 * Adds the internal broadcast header whenever a Feign method is annotated with
 * {@link FeignBroadcast}.
 */
class FeignBroadcastRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        MethodMetadata metadata = template.methodMetadata();
        if (metadata == null) {
            return;
        }
        Method method = metadata.method();
        if (method == null) {
            return;
        }
        FeignBroadcast annotation = method.getAnnotation(FeignBroadcast.class);
        if (annotation == null) {
            return;
        }
        String value = annotation.failFast() ? FeignBroadcastConstants.FAIL_FAST_TRUE
                : FeignBroadcastConstants.FAIL_FAST_FALSE;
        template.header(FeignBroadcastConstants.BROADCAST_HEADER, value);
    }
}
