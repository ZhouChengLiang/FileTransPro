package org.zcl.filewatcher.generate;

import java.io.IOException;
import java.nio.file.Path;

public interface FileProcessor {
    void processFile(Path filePath) throws IOException;
}    