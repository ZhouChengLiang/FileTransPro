package org.zcl.filewatcher.generate;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zcl.filewatcher.util.CompressedUrlEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.*;

@Component
@Slf4j
public class QrCodeGenerator {
    
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    public void generateQrCodeFromFile(Path filePath, Path outputPath) throws Exception {
        String ONLINE_URL = "http://112.124.18.118:8080/scan?mobile={mobile}&code=";
        String parentDirName = filePath.getParent().getFileName().toString();
        if(!StrUtil.isBlank(parentDirName) && PhoneUtil.isMobile(parentDirName)) {
            ONLINE_URL = ONLINE_URL.replaceAll("\\{mobile}",parentDirName);
        }
        // 读取文件内容
        byte[] fileContent = Files.readAllBytes(filePath);
        fileContent = new String(fileContent, UTF_8).getBytes(UTF_8);

        // base64加密
        String base64Content =  CompressedUrlEncoder.encode(new String(fileContent));
        log.info("Base64.getUrlEncoder().encodeToString >>> " + base64Content);
        base64Content = ONLINE_URL.concat(base64Content);
        // 生成二维码
        BitMatrix bitMatrix = new MultiFormatWriter().encode(base64Content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
        // 保存二维码图片
        Files.createDirectories(outputPath.getParent());
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", outputPath);

        log.info("二维码已生成: " + outputPath);

        // 尝试解析二维码内容
        byte[] byteArr = Files.readAllBytes(outputPath);
        log.info("decodeQrCode >>> " + decodeQrCode(byteArr));
    }


    public String decodeQrCode(byte[] qrCodeImage) throws NotFoundException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(qrCodeImage);
        BufferedImage image = ImageIO.read(inputStream);

        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }



}    