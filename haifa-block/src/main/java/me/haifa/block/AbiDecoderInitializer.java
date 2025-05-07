package me.haifa.block;

import com.fasterxml.jackson.databind.JsonNode;
import me.haifa.block.abi.decoder.DynamicAbiEventDecoder;
import me.haifa.block.abi.factory.DecoderFactory;
import me.haifa.block.abi.loader.AbiJsonLoader;
import me.haifa.block.abi.registry.EventDecoderRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class AbiDecoderInitializer implements CommandLineRunner {

    @Value("${abi.path:abi}") // 默认加载 resources/abi 目录
    private String abiPath;

    private final AbiJsonLoader abiJsonLoader;
    private final DecoderFactory decoderFactory;
    private final EventDecoderRegistry registry;

    public AbiDecoderInitializer(AbiJsonLoader abiJsonLoader,
                                 DecoderFactory decoderFactory,
                                 EventDecoderRegistry registry) {
        this.abiJsonLoader = abiJsonLoader;
        this.decoderFactory = decoderFactory;
        this.registry = registry;
    }

    @Override
    public void run(String... args) throws Exception {
        File abiDir = new File("haifa-block/src/main/resources/" + abiPath);
        if (!abiDir.exists() || !abiDir.isDirectory()) {
            System.err.println("ABI 目录不存在：" + abiDir.getAbsolutePath());
            return;
        }

        File[] abiFiles = abiDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (abiFiles == null || abiFiles.length == 0) {
            System.err.println("ABI 目录为空：" + abiDir.getAbsolutePath());
            return;
        }

        for (File abiFile : abiFiles) {
            try {
                JsonNode abiJson = abiJsonLoader.loadAbi(abiFile.getAbsolutePath());
                List<DynamicAbiEventDecoder> decoders = decoderFactory.createDecoders(abiJson);
                for (DynamicAbiEventDecoder decoder : decoders) {
                    registry.register(decoder);
                    System.out.println("✅ 注册事件解码器: " + decoder.getEventSignature());
                }
            } catch (Exception e) {
                System.err.println("❌ 加载 ABI 文件失败: " + abiFile.getName());
                e.printStackTrace();
            }
        }
    }
}