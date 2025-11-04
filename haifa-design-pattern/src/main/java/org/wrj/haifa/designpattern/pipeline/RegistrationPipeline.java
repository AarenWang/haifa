package org.wrj.haifa.designpattern.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 5. 管道 (Pipeline)
 * 持有所有过滤器，并按顺序执行它们。
 */
public class RegistrationPipeline {
    private final List<Filter> filters = new ArrayList<>();

    public void addFilter(Filter filter) {
        this.filters.add(filter);
    }

    // 核心方法：让数据上下文流过所有过滤器
    public void process(UserRegistrationContext context) {
        for (Filter filter : filters) {
            filter.execute(context);
        }
    }
}
