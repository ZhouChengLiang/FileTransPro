package org.zcl.filewatcher.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zcl.filewatcher.config.AppConfig;
import org.zcl.filewatcher.generate.FileProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class FileWatchService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private FileProcessor fileProcessor;
    private ConcurrentHashMap<String, WatchMonitor> watchMonitors;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        executor = ThreadUtil.newFixedExecutor(16, "file-watcher",false);
        watchMonitors = new ConcurrentHashMap<>();
        startWatchingInitialDirs();
        running.set(true);
    }

    @PreDestroy
    public void destroy() {
        stopAllWatches();
        executor.shutdown();
        running.set(false);
    }

    // 启动初始配置的监听目录
    private void startWatchingInitialDirs() {
        try {
            String watchPath = appConfig.getWatchPath();
            File watchDir = new File(watchPath);
            File outputDir = new File(appConfig.getQrCodeOutputPath());

            // 确保目录存在
            FileUtil.mkdir(watchDir);
            FileUtil.mkdir(outputDir);

            log.info("开始监听初始目录: {}", watchDir.getAbsolutePath());
            startWatchingDir(watchDir.getAbsolutePath());

            // 递归扫描已存在的子目录并添加监听
            scanAndWatchSubdirs(watchDir, 1);
        } catch (Exception e) {
            log.error("启动初始监听失败", e);
        }
    }

    // 递归扫描并监听子目录
    private void scanAndWatchSubdirs(File parentDir, int currentDepth) {
        if (currentDepth > appConfig.getMaxDepth()) {
            return;
        }

        File[] subFiles = parentDir.listFiles();
        if (subFiles == null) {
            return;
        }

        for (File file : subFiles) {
            if (file.isDirectory()) {
                startWatchingDir(file.getAbsolutePath());
                scanAndWatchSubdirs(file, currentDepth + 1);
            }
        }
    }

    // 启动对指定目录的监听
    public synchronized void startWatchingDir(String dirPath) {
        if (watchMonitors.containsKey(dirPath)) {
            log.info("目录已在监听中: {}", dirPath);
            return;
        }

        try {
            File dir = FileUtil.file(dirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                log.warn("目录不存在或不是目录: {}", dirPath);
                return;
            }

            // 创建目录监听器
            WatchMonitor monitor = WatchUtil.create(dirPath);
            monitor.setWatcher(new RecursiveDirWatcher(dirPath));

            // 启动监听
            executor.submit(() -> {
                try {
                    monitor.start();
                    watchMonitors.put(dirPath, monitor);
                    log.info("成功启动目录监听: {}", dirPath);
                } catch (Exception e) {
                    log.error("启动目录监听失败: {}", dirPath, e);
                }
            });
        } catch (Exception e) {
            log.error("处理目录监听请求失败: {}", dirPath, e);
        }
    }

    // 停止对指定目录的监听
    public synchronized void stopWatchingDir(String dirPath) {
        WatchMonitor monitor = watchMonitors.remove(dirPath);
        if (monitor != null) {
            try {
                monitor.close();
                log.info("成功停止目录监听: {}", dirPath);
            } catch (Exception e) {
                log.error("停止目录监听失败: {}", dirPath, e);
            }
        } else {
            log.info("目录未在监听中: {}", dirPath);
        }
    }

    // 停止所有监听
    public synchronized void stopAllWatches() {
        watchMonitors.forEach((path, monitor) -> {
            try {
                monitor.close();
            } catch (Exception e) {
                log.error("停止监听失败: {}", path, e);
            }
        });
        watchMonitors.clear();
        log.info("已停止所有目录监听");
    }

    // 获取当前监听的目录列表
    public List<String> getWatchedDirs() {
        return new ArrayList<>(watchMonitors.keySet());
    }

    // 检查服务是否运行
    public boolean isRunning() {
        return running.get();
    }

    // 递归目录监听器（处理深层子目录）
    private class RecursiveDirWatcher extends SimpleWatcher {
        private final String baseDir;
        private int currentDepth;

        public RecursiveDirWatcher(String baseDir) {
            this.baseDir = baseDir;
            // 计算当前目录深度
            this.currentDepth = baseDir.split(File.separator).length -
                    appConfig.getWatchPath().split(File.separator).length;
        }

        @Override
        public void onCreate(WatchEvent<?> event, Path currentPath) {
            try {
                Object obj = event.context();
                if (obj instanceof Path) {
                    Path childPath = (Path) obj;
                    Path fullPath = currentPath.resolve(childPath);
                    File file = fullPath.toFile();

                    if (file.isDirectory() && currentDepth < appConfig.getMaxDepth()) {
                        // 新子目录创建，且未超过最大深度，启动监听
                        String dirPath = file.getAbsolutePath();
                        startWatchingDir(dirPath);
                    } else if (file.isFile()) {
                        log.info("检测到文件 onCreate {}: ", fullPath);
                        if(!file.isHidden()){
                            fileProcessor.processFile(fullPath);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理目录创建事件失败", e);
            }
        }

        @Override
        public void onModify(WatchEvent<?> event, Path currentPath) {
            try {
                Object obj = event.context();
                if (obj instanceof Path) {
                    Path childPath = (Path) obj;
                    Path fullPath = currentPath.resolve(childPath);
                    File file = fullPath.toFile();

                    if (file.isDirectory() && currentDepth < appConfig.getMaxDepth()) {
                        String dirPath = file.getAbsolutePath();
                        startWatchingDir(dirPath);
                    } else if (file.isFile()) {
                        log.info("检测到文件 onModify {}: ", fullPath);
                        if(!file.isHidden()){
                            fileProcessor.processFile(fullPath);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("处理目录创建事件失败", e);
            }
        }
    }
}    