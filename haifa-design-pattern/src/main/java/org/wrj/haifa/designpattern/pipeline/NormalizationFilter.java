package org.wrj.haifa.designpattern.pipeline;

/**
 * 3.3. 标准化过滤器 (数据充实/转换)
 * 将Email转换为小写。
 */
public class NormalizationFilter implements Filter {
    @Override
    public void execute(UserRegistrationContext context) {
        // 如果数据已无效，无需进行标准化
        if (!context.isValid()) {
            return;
        }

        System.out.println("  [Normalize] Running...");
        context.setEmail(context.getEmail().toLowerCase());
        System.out.println("  [Normalize] Email converted to lowercase.");
    }
}
