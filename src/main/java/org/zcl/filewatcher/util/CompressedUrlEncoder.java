package org.zcl.filewatcher.util;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressedUrlEncoder {

    // 压缩并编码为URL安全的Base64
    public static String encode(String data) throws Exception {
        byte[] input = data.getBytes("UTF-8");
        
        // 压缩数据
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] compressedData = outputStream.toByteArray();
        
        // 使用URL安全的Base64编码
        return Base64.getUrlEncoder().withoutPadding().encodeToString(compressedData);
    }

    // 解码并解压缩
    public static String decode(String encodedData) throws Exception {
        // 解码URL安全的Base64
        byte[] compressedData = Base64.getUrlDecoder().decode(encodedData);
        
        // 解压缩
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] decompressedData = outputStream.toByteArray();
        
        return new String(decompressedData, "UTF-8");
    }
}