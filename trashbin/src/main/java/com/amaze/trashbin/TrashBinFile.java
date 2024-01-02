package com.amaze.trashbin;

import android.os.Build;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @param fileName name of file
 * @param isDirectory whether file is directory
 * @param path original path of file
 * @param sizeBytes size of file
 * @param deleteTime time of deletion, provide custom or initialized by default implementation to current time
 */
public class TrashBinFile {

    private final String fileName;
    private final boolean isDirectory;
    private final String path;
    private final long sizeBytes;
    private Long deleteTime;

    public TrashBinFile(String fileName, boolean isDirectory, String path, long sizeBytes, Long deleteTime) {
        this.fileName = fileName;
        this.isDirectory = isDirectory;
        this.path = path;
        this.sizeBytes = sizeBytes;

        if (deleteTime == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.deleteTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            } else {
                this.deleteTime = System.currentTimeMillis() / 1000;
            }
        } else {
            this.deleteTime = deleteTime;
        }
    }

    // Getters
    public String getFileName() {
        return fileName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getPath() {
        return path;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Long getDeleteTime() {
        return deleteTime;
    }

    public String getDeletedPath(TrashBinConfig config) {
        return config.getTrashBinFilesDirectory() + File.separator + fileName;
    }
}

