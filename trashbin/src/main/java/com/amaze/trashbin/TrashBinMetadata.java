package com.amaze.trashbin;

import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrashBinMetadata {

    private TrashBinConfig config;
    private long totalSize;
    private List<TrashBinFile> files;

    public TrashBinMetadata(TrashBinConfig config, long totalSize, List<TrashBinFile> files) {
        this.config = config;
        this.totalSize = totalSize;
        this.files = files;
    }

    // Getters
    public TrashBinConfig getConfig() {
        return config;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public List<TrashBinFile> getFiles() {
        return files;
    }

    // Setters
    public void setConfig(TrashBinConfig config) {
        this.config = config;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void setFiles(List<TrashBinFile> files) {
        this.files = files;
    }


    /**
     * Returns percent of trash bin memory used
     */
    public int getCapacity() {
        int numOfFiles = files.size();
        long totalBytes = totalSize;
        int capacityNumOfFiles = 0;
        int capacityBytes = 0;
        if (config.getRetentionNumOfFiles() != TrashBinConfig.RETENTION_NUM_OF_FILES) {
            if (config.getRetentionNumOfFiles() == 0) {
                return 0;
            }
            capacityNumOfFiles = (numOfFiles / config.getRetentionNumOfFiles()) * 100;
        }
        if (config.getRetentionBytes() != TrashBinConfig.RETENTION_BYTES_INFINITE) {
            if (config.getRetentionBytes() == 0L) {
                return 0;
            }
            capacityBytes = (int) ((totalBytes / config.getRetentionBytes()) * 100);
        }
        return (capacityBytes > capacityNumOfFiles) ?
                capacityBytes :
                (capacityNumOfFiles > capacityBytes) ?
                        capacityBytes :
                        TrashBinConfig.TRASH_BIN_CAPACITY_INVALID;
    }



    public List<TrashBinFile> getFilesWithDeletionCriteria() {
        long totalBytes = totalSize;
        int numOfFiles = files.size();

        List<TrashBinFile> sortedFiles = new ArrayList<>(files);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sortedFiles.sort(Comparator.comparingLong(TrashBinFile::getDeleteTime));
        }

        List<TrashBinFile> filteredFiles = new ArrayList<>();

        for (TrashBinFile file : sortedFiles) {
            if (config.getRetentionNumOfFiles() != TrashBinConfig.RETENTION_NUM_OF_FILES &&
                    numOfFiles > config.getRetentionNumOfFiles()) {
                numOfFiles--;
                filteredFiles.add(file);
            } else if (config.getRetentionBytes() != TrashBinConfig.RETENTION_BYTES_INFINITE &&
                    totalBytes > config.getRetentionBytes()) {
                totalBytes -= file.getSizeBytes();
                filteredFiles.add(file);
            } else if (config.getRetentionDays() != TrashBinConfig.RETENTION_DAYS_INFINITE) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
                    LocalDateTime deletionTime = (file.getDeleteTime() != null) ?
                            LocalDateTime.ofEpochSecond(file.getDeleteTime(), 0, ZoneOffset.UTC) :
                            LocalDateTime.ofEpochSecond(System.currentTimeMillis() / 1000, 0, ZoneOffset.UTC);

                    if (deletionTime.plusDays(config.getRetentionDays()).isBefore(LocalDateTime.now())) {
                        filteredFiles.add(file);
                    }
                } else {
                    long retentionSeconds = config.getRetentionDays() * 24 * 60 * 60;
                    long newEpochSeconds = (file.getDeleteTime() != null) ? file.getDeleteTime() :
                            System.currentTimeMillis() / 1000;
                    if (newEpochSeconds + retentionSeconds < System.currentTimeMillis() / 1000) {
                        filteredFiles.add(file);
                    }
                }
            }
        }

        return filteredFiles;
    }

}

