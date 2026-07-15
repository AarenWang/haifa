package org.wrj.haifa.ai.deerflow.middleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the graph lifecycle phase in which a middleware is allowed to run. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareLifecycle {

    MiddlewarePhase value();
}
