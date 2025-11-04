package org.wrj.haifa.openfeign.broadcast.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Feign client method that should be broadcast to every available instance of the target
 * service.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FeignBroadcast {

    /**
     * When {@code true}, the broadcast stops and throws immediately on the first failure.
     * Otherwise, the broadcast keeps calling the remaining instances and re-throws the last failure
     * (if any) after all attempts.
     */
    boolean failFast() default true;
}
