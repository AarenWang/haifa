package org.wrj.haifa.designpattern.pipeline;

/**
 * 3.2. 验证过滤器
 * 检查数据的业务逻辑（如密码强度、Email格式）。
 */
public class ValidationFilter implements Filter {
    @Override
    public void execute(UserRegistrationContext context) {
        // 如果上一步（解析）已经失败，则跳过此过滤器
        if (!context.isValid()) {
            return;
        }

        System.out.println("  [Validate] Running...");
        if (context.getPassword().length() < 8) {
            context.addError("Password must be at least 8 characters.");
        }
        if (!context.getEmail().contains("@") || !context.getEmail().contains(".")) {
            context.addError("Invalid email format.");
        }

        if (context.isValid()) {
            System.out.println("  [Validate] Success.");
        } else {
            System.out.println("  [Validate] FAILED.");
        }
    }
}
