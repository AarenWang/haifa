package org.wrj.haifa.ai.spring.toolcalling.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a callable geographic knowledge tool. The {@code GeoChatClient}
 * inspects beans with this annotation and delegates questions to the underlying
 * method when a tool call is requested by the chat flow.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GeoTool {

    /**
     * Human readable name exposed to the model.
     */
    String name();

    /**
     * Short description to help the model decide when the tool should be used.
     */
    String description() default "";
}
