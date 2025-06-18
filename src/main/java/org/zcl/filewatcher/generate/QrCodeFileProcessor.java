package org.zcl.filewatcher.generate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.zcl.filewatcher.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author zhouchengliang
 */
@Component
@Slf4j
public class QrCodeFileProcessor implements FileProcessor {

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    @Autowired
    private AppConfig appConfig;
    
    @Override
    public void processFile(Path filePath) throws IOException {
        // 如果文件内容为空，不发送短信
        List<String> content = Files.readAllLines(filePath);
        if(CollectionUtils.isEmpty(content)){
            log.info("processFile 文件内容为空,不处理！！！");
            return;
        }
        // 构建输出文件路径
        String fileName = filePath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        Date currentDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        baseName += formatter.format(currentDate);
        if(!Files.exists(Paths.get(appConfig.getQrCodeOutputPath()))) {
            Files.createDirectories(Paths.get(appConfig.getQrCodeOutputPath()));
        }
        Path outputPath = Paths.get(appConfig.getQrCodeOutputPath(), baseName + ".png");
        
        try {
            // 生成二维码
            qrCodeGenerator.generateQrCodeFromFile(filePath, outputPath);
        } catch (Exception e) {
            System.err.println("生成二维码时出错: " + e.getMessage());
        }
    }
}    