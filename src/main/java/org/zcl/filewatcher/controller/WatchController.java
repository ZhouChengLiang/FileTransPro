package org.zcl.filewatcher.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zcl.filewatcher.service.FileWatchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watch")
public class WatchController {

    @Autowired
    private FileWatchService fileWatchService;

    /**
     * 添加新的监听目录
     * @param request
     * @return
     */
    @PostMapping("/add")
    public Map<String, Object> addWatchDir(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String path = request.get("path");
        
        if (path == null || path.isEmpty()) {
            result.put("success", false);
            result.put("message", "目录路径不能为空");
            return result;
        }
        
        try {
            fileWatchService.startWatchingDir(path);
            result.put("success", true);
            result.put("message", "添加监听成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加监听失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 移除监听目录
     * @param request
     * @return
     */
    @DeleteMapping("/remove")
    public Map<String, Object> removeWatchDir(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String path = request.get("path");
        
        if (path == null || path.isEmpty()) {
            result.put("success", false);
            result.put("message", "目录路径不能为空");
            return result;
        }
        
        try {
            fileWatchService.stopWatchingDir(path);
            result.put("success", true);
            result.put("message", "移除监听成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "移除监听失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取当前监听的目录列表
     * @return
     */
    @GetMapping("/list")
    public List<String> listWatchedDirs() {
        return fileWatchService.getWatchedDirs();
    }
}    