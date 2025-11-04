package org.wrj.haifa.designpattern.pipeline;

/**
 * 3.1. 解析过滤器
 * 将原始字符串解析为上下文中的结构化字段。
 */
public class ParseFilter implements Filter {
    @Override
    public void execute(UserRegistrationContext context) {
        try {
            String[] parts = context.getRawInput().split(",");
            if (parts.length != 4) {
                throw new Exception("Input data does not have 4 parts.");
            }
            context.setUsername(parts[0].trim());
            context.setPassword(parts[1].trim());
            context.setEmail(parts[2].trim());
            context.setAge(Integer.parseInt(parts[3].trim()));
            System.out.println("  [Parse] Success.");
        } catch (Exception e) {
            System.out.println("  [Parse] FAILED.");
            context.addError("Parsing failed: " + e.getMessage());
        }
    }
}
