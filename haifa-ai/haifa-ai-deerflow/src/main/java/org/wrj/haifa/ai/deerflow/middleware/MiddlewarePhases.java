package org.wrj.haifa.ai.deerflow.middleware;

import java.util.Comparator;
import java.util.List;
import org.springframework.core.annotation.AnnotationUtils;

/** Selects and orders middleware for a concrete graph lifecycle phase. */
public final class MiddlewarePhases {

    private MiddlewarePhases() {
    }

    public static List<AgentMiddleware> select(List<AgentMiddleware> middlewares, MiddlewarePhase phase) {
        if (middlewares == null || middlewares.isEmpty()) {
            return List.of();
        }
        return middlewares.stream()
                .filter(middleware -> phaseOf(middleware) == phase)
                .sorted(Comparator.comparingInt(MiddlewarePhases::orderOf))
                .toList();
    }

    public static MiddlewarePhase phaseOf(AgentMiddleware middleware) {
        MiddlewareLifecycle lifecycle = AnnotationUtils.findAnnotation(
                middleware.getClass(), MiddlewareLifecycle.class);
        if (lifecycle == null) {
            throw new IllegalStateException("AgentMiddleware must declare @MiddlewareLifecycle: "
                    + middleware.getClass().getName());
        }
        return lifecycle.value();
    }

    private static int orderOf(AgentMiddleware middleware) {
        MiddlewareOrder order = AnnotationUtils.findAnnotation(middleware.getClass(), MiddlewareOrder.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }
}
