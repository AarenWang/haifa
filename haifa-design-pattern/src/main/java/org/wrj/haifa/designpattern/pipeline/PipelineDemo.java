package org.wrj.haifa.designpattern.pipeline;

import java.util.List;

/**
 * 6. 客户端 (The Client)
 * 组装并运行管道。
 */
public class PipelineDemo {

    public static void main(String[] args) {
        // 1. 设置管道 (组装过滤器)
        RegistrationPipeline pipeline = new RegistrationPipeline();
        pipeline.addFilter(new ParseFilter());
        pipeline.addFilter(new ValidationFilter());
        pipeline.addFilter(new NormalizationFilter());
        pipeline.addFilter(new SaveUserFilter()); // SaveFilter 是管道的最后一环 (Sink)

        // 2. 定义数据源 (Source)
        List<String> rawData = List.of(
                "alice, password123, ALICE@EXAMPLE.COM, 25",    // 正常数据
                "bob, pass, bob@gmail.com, 30",                 // 密码太短
                "charlie, strongpass, invalid-email, 40",       // Email格式错误
                "david, securepass, DAVID@HOTMAIL.COM"          // 解析会失败（数据不完整）
        );

        System.out.println("--- Starting Data Processing Pipeline ---");

        // 3. 运行管道
        for (String data : rawData) {
            System.out.println("\nProcessing: " + data);
            UserRegistrationContext context = new UserRegistrationContext(data);
            pipeline.process(context);
        }

        System.out.println("\n--- Pipeline Finished ---");
    }
}
