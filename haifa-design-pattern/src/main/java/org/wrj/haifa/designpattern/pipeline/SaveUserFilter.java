package org.wrj.haifa.designpattern.pipeline;

/**
 * 4. 存储过滤器 (The Sink)
 * 管道的终点。根据数据是否有效，决定是“保存”还是“记录错误”。
 */
public class SaveUserFilter implements Filter {
    @Override
    public void execute(UserRegistrationContext context) {
        System.out.println("  [Save] Running...");
        if (context.isValid()) {
            // 模拟保存到数据库
            System.out.println("✅ SAVING USER: [Username=" + context.getUsername()
                    + ", Email=" + context.getEmail()
                    + ", Age=" + context.getAge() + "]");
        } else {
            // 模拟记录到错误日志
            System.err.println("❌ FAILED REGISTRATION for input: '" + context.getRawInput() + "'");
            for (String error : context.getErrors()) {
                System.err.println("   - " + error);
            }
        }
    }
}
