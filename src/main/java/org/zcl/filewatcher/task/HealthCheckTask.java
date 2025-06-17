package org.zcl.filewatcher.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zcl.filewatcher.config.AppConfig;
import org.zcl.filewatcher.service.FileWatchService;

@Component
@Slf4j
public class HealthCheckTask {

    @Autowired
    private FileWatchService fileWatchService;

    @Autowired
    private AppConfig appConfig;

    @Scheduled(fixedDelayString = "${app.health-check-interval}") // 单位:毫秒
    public void checkWatcherHealth() {
        try {
            log.info("开始检测文件监听服务是否在线 {}",fileWatchService.isRunning());
            if (!fileWatchService.isRunning()) {
                log.warn("检测到文件监听服务未运行，尝试重启...");
                fileWatchService.startWatchingDir(appConfig.getWatchPath());
            }
        } catch (Exception e) {
            log.error("健康检查任务执行失败", e);
        }
    }
}    