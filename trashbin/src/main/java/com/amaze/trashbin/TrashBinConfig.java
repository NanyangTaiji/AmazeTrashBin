package com.amaze.trashbin;

import java.io.File;

/**
 * path to store trash files and metadata. eg /storage/ID/.demo/TrashBinFiles
 * /storage/ID/.demo/metadata.json
 * triggerCleanupAutomatically - library automatically triggers a cleanup after every delete / move operation
 * setting this false means you're responsible to trigger the cleanup at your own discretion
 */
public class TrashBinConfig {

    public static final int RETENTION_DAYS_INFINITE = -1;
    public static final long RETENTION_BYTES_INFINITE = -1L;
    public static final int RETENTION_NUM_OF_FILES = -1;
    public static final int INTERVAL_CLEANUP_HOURS = 1;
    public static final int TRASH_BIN_CAPACITY_INVALID = -1;
    public static final String TRASH_BIN_DIR = "TrashBinFiles";
    public static final String TRASH_BIN_META_FILE = "metadata.json";

    private final String basePath;
    private final int retentionDays;
    private final long retentionBytes;
    private final int retentionNumOfFiles;
    private final int cleanupHours;
    private final boolean deleteRogueFiles;
    private final boolean triggerCleanupAutomatically;

    public TrashBinConfig(
            String basePath,
            int retentionDays,
            long retentionBytes,
            int retentionNumOfFiles,
            boolean deleteRogueFiles,
            boolean triggerCleanupAutomatically
    ) {
        this.basePath = basePath;
        this.retentionDays = retentionDays;
        this.retentionBytes = retentionBytes;
        this.retentionNumOfFiles = retentionNumOfFiles;
        this.cleanupHours = -1;
        this.deleteRogueFiles = deleteRogueFiles;
        this.triggerCleanupAutomatically = triggerCleanupAutomatically;
    }

    // Getters
    public String getBasePath() {
        return basePath;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public long getRetentionBytes() {
        return retentionBytes;
    }

    public int getRetentionNumOfFiles() {
        return retentionNumOfFiles;
    }

    public int getCleanupHours() {
        return cleanupHours;
    }

    public boolean isDeleteRogueFiles() {
        return deleteRogueFiles;
    }

    public boolean isTriggerCleanupAutomatically() {
        return triggerCleanupAutomatically;
    }



    public String getTrashBinFilesDirectory() {
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File directory = new File(basePath, TRASH_BIN_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return basePath + File.separator + TRASH_BIN_DIR;
    }

    public String getMetaDataFilePath() {
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File file = new File(basePath, TRASH_BIN_META_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return basePath + File.separator + TRASH_BIN_META_FILE;
    }

    public int getCleanupIntervalHours() {
        return cleanupHours;
    }
}

