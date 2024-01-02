package com.amaze.trashbin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TrashBin {

    private Context context;
    private boolean doTriggerAutoCleanup;
    private TrashBinConfig trashConfig;
    private DeletePermanentlyCallback deletePermanentlySuperCallback;
    private ListTrashBinFilesCallback listTrashBinFilesSuperCallback;
    private TrashBinMetadata metadata;

    public TrashBin(
            Context context,
            boolean doTriggerAutoCleanup,
            TrashBinConfig trashConfig,
            DeletePermanentlyCallback deletePermanentlySuperCallback,
            ListTrashBinFilesCallback listTrashBinFilesSuperCallback
    ) {
        this.context = context;
        this.doTriggerAutoCleanup = doTriggerAutoCleanup;
        this.trashConfig = trashConfig;
        this.deletePermanentlySuperCallback = deletePermanentlySuperCallback;
        this.listTrashBinFilesSuperCallback = listTrashBinFilesSuperCallback;

        initialize();
    }

    private void initialize() {
        trashConfig.getTrashBinFilesDirectory();
        metadata = getTrashBinMetadata();

        if (deletePermanentlySuperCallback != null && doTriggerAutoCleanup &&
                trashConfig.getCleanupIntervalHours() != -1) {
            // check for auto trigger criteria
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    context.getPackageName() + ".com.amaze.trashbin",
                    Context.MODE_PRIVATE
            );
            long lastCleanup = sharedPreferences.getLong(
                    "com.amaze.trashbin.lastCleanup",
                    0
            );
            long currentTime = System.currentTimeMillis();
            long hours = ((currentTime - lastCleanup) / (1000 * 60 * 60));
            Log.i(
                    getClass().getSimpleName(),
                    "auto cleanup pending minutes " +
                            hours + " and interval " + trashConfig.getCleanupIntervalHours()
            );
            if (hours >= trashConfig.getCleanupIntervalHours()) {
                Log.i(getClass().getSimpleName(), "triggering auto cleanup for trash bin");

                // Use your preferred method for starting a new thread or coroutine in Java
                new Thread(() -> {
                    triggerCleanup(deletePermanentlySuperCallback);
                    // Update the last cleanup time in SharedPreferences
                    sharedPreferences.edit().putLong(
                            "com.amaze.trashbin.lastCleanup",
                            currentTime
                    ).apply();
                }).start();
            }
        }
    }


    public boolean deletePermanently(
            List<TrashBinFile> files,
            DeletePermanentlyCallback deletePermanentlyCallback,
            boolean doTriggerCleanup
    ) {
        if (files.isEmpty()) {
            Log.i(getClass().getSimpleName(), "Empty files list to delete permanently");
            return true;
        }

        long totalSize = 0L;
        List<TrashBinFile> filesMetadata = new ArrayList<>(getTrashBinMetadata().getFiles());

        for (TrashBinFile it : files) {
            int indexToRemove = -1;

            // try to find file in metadata
            for (int i = 0; i < filesMetadata.size(); i++) {
                if (it.getPath().equals(filesMetadata.get(i).getPath())) {
                    indexToRemove = i;
                    break;
                }
            }

            if (indexToRemove != -1) {
                // found file in metadata, call delete with trash bin path
                boolean didDelete = deletePermanentlyCallback.invoke(it.getDeletedPath(trashConfig));
                if (didDelete) {
                    filesMetadata.remove(indexToRemove);
                    Log.w(
                            getClass().getSimpleName(),
                            "TrashBin: deleting file in trashbin " +
                                    it.getPath()
                    );
                }
            } else {
                // file not found in metadata, call delete on original file
                deletePermanentlyCallback.invoke(it.getPath());
                Log.w(getClass().getSimpleName(), "TrashBin: deleting original file " + it.getPath());
            }
        }

        for (TrashBinFile fileMetadata : filesMetadata) {
            totalSize += fileMetadata.getSizeBytes();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filesMetadata.sort((trashBinFile1, trashBinFile2) ->
                    Long.compare(trashBinFile1.getDeleteTime(), trashBinFile2.getDeleteTime()) * -1
            );
        }

        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup);
        return true;
    }


    public boolean moveToBin(
            List<TrashBinFile> files,
            boolean doTriggerCleanup,
            MoveFilesCallback moveFilesCallback
    ) {
        if (files.isEmpty()) {
            Log.i(getClass().getSimpleName(), "Empty files list to move to bin");
            return true;
        }

        long totalSize = metadata != null ? metadata.getTotalSize() : 0L;
        List<TrashBinFile> filesMetadata = new ArrayList<>(getTrashBinMetadata().getFiles());

        for (TrashBinFile it : files) {
            boolean didMove = moveFilesCallback.invoke(it.getPath(), it.getDeletedPath(trashConfig));
            if (didMove) {
                filesMetadata.add(it);
                totalSize += it.getSizeBytes();
            } else {
                Log.w(getClass().getSimpleName(), "Failed to move to bin " + it.getPath());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filesMetadata.sort((trashBinFile1, trashBinFile2) ->
                    Long.compare(trashBinFile2.getDeleteTime(), trashBinFile1.getDeleteTime())
            );
        }

        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup);
        return true;
    }

    public boolean restore(
            List<TrashBinFile> files,
            boolean doTriggerCleanup,
            MoveFilesCallback moveFilesCallback
    ) {
        if (files.isEmpty()) {
            Log.i(getClass().getSimpleName(), "Empty files list to restore");
            return true;
        }

        long totalSize = 0L;
        List<TrashBinFile> filesMetadata = new ArrayList<>(getTrashBinMetadata().getFiles());

        for (TrashBinFile it : files) {
            boolean didMove = moveFilesCallback.invoke(it.getDeletedPath(trashConfig), it.getPath());
            if (didMove) {
                int indexToRemove = -1;

                // try to find file in metadata
                for (int i = 0; i < filesMetadata.size(); i++) {
                    if (it.getPath().equals(filesMetadata.get(i).getPath())) {
                        indexToRemove = i;
                        break;
                    }
                }

                if (indexToRemove != -1) {
                    filesMetadata.remove(indexToRemove);
                }
            } else {
                Log.w(getClass().getSimpleName(), "Failed to restore from bin " + it.getPath());
            }
        }

        for (TrashBinFile fileMetadata : filesMetadata) {
            totalSize += fileMetadata.getSizeBytes();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            filesMetadata.sort((trashBinFile1, trashBinFile2) ->
                    Long.compare(trashBinFile2.getDeleteTime(), trashBinFile1.getDeleteTime())
            );
        }

        writeMetadataAndTriggerCleanup(filesMetadata, totalSize, doTriggerCleanup);
        return true;
    }

    public boolean emptyBin(DeletePermanentlyCallback deletePermanentlyCallback) {
        return deletePermanently(metadata != null ? metadata.getFiles() : Collections.emptyList(),
                deletePermanentlyCallback, true);
    }

    public boolean restoreBin(MoveFilesCallback moveFilesCallback) {
        return restore(metadata != null ? metadata.getFiles() : Collections.emptyList(), true,
                moveFilesCallback);
    }

    public List<TrashBinFile> listFilesInBin() {
        return getTrashBinMetadata().getFiles();
    }

    public TrashBinConfig getConfig() {
        return trashConfig;
    }

    public void setConfig(TrashBinConfig trashBinConfig) {
        trashConfig = trashBinConfig;
    }

    public TrashBinMetadata getTrashBinMetadata() {
        return metadata != null ? metadata : loadMetaDataJSONFile();
    }

    private TrashBinMetadata loadMetaDataJSONFile() {
        TypeToken<TrashBinMetadata> metadataType = new TypeToken<TrashBinMetadata>() {
        };
        try (JsonReader reader = new JsonReader(new FileReader(trashConfig.getMetaDataFilePath()))) {
            Gson gson = new Gson();
            metadata = gson.fromJson(reader, metadataType.getType());

            if (metadata == null) {
                metadata = new TrashBinMetadata(trashConfig, 0L, Collections.emptyList());
            } else {
                metadata.setConfig(trashConfig);
            }

            writeMetaDataJSONFile(metadata);
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Failed to load metadata", e);
            metadata = new TrashBinMetadata(trashConfig, 0L, Collections.emptyList());
        }
        return metadata;
    }

    public boolean triggerCleanup(DeletePermanentlyCallback deletePermanentlyCallback) {
        List<TrashBinFile> filesToDelete = metadata.getFilesWithDeletionCriteria();
        if (filesToDelete != null && !filesToDelete.isEmpty()) {
            deletePermanently(filesToDelete, deletePermanentlyCallback, false);
        }
        return true;
    }

    public boolean removeRogueFiles(List<TrashBinFile> files,
                                    ListTrashBinFilesCallback listTrashBinFilesCallback,
                                    DeletePermanentlyCallback deletePermanentlyCallback) {
        List<TrashBinFile> physicalFilesList = listTrashBinFilesCallback.invoke(
                trashConfig.getTrashBinFilesDirectory());

        if (physicalFilesList.size() > files.size()) {
            // ... (same as in the Kotlin version)
        } else {
            List<TrashBinFile> mutableMetaFiles = new ArrayList<>(files);
            for (int i = mutableMetaFiles.size() - 1; i >= 0; i--) {
                boolean foundFileMetadata = false;
                for (TrashBinFile physicalFile : physicalFilesList) {
                    if (physicalFile.getPath().equals(mutableMetaFiles.get(i).getPath())) {
                        foundFileMetadata = true;
                        break;
                    }
                }
                if (!foundFileMetadata) {
                    mutableMetaFiles.remove(i);
                }
            }
            metadata.setFiles(mutableMetaFiles);
            writeMetaDataJSONFile(metadata);
        }
        return true;
    }

    public void writeMetadataAndTriggerCleanup(List<TrashBinFile> files, long totalSize,
                                               boolean doTriggerCleanup) {
        metadata.setConfig(trashConfig);
        metadata.setFiles(files);
        metadata.setTotalSize(totalSize);

        if (trashConfig.isDeleteRogueFiles() && listTrashBinFilesSuperCallback != null &&
                deletePermanentlySuperCallback != null) {
            removeRogueFiles(files, listTrashBinFilesSuperCallback,
                    deletePermanentlySuperCallback);
        } else {
            writeMetaDataJSONFile(metadata);
        }

        if (doTriggerCleanup && deletePermanentlySuperCallback != null) {
            triggerCleanup(deletePermanentlySuperCallback);
        }
    }

    private void writeMetaDataJSONFile(TrashBinMetadata meta) {
        try (FileWriter writer = new FileWriter(trashConfig.getMetaDataFilePath())) {
            Gson gson = new GsonBuilder().serializeNulls().create();
            gson.toJson(meta, writer);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Failed to write metadata", e);
        }
    }


    //-----------------------------------//

    public interface DeletePermanentlyCallback {
        boolean invoke(String deletePath);
    }

    public interface MoveFilesCallback {
        boolean invoke(String source, String dest);
    }

    public interface ListTrashBinFilesCallback {
        List<TrashBinFile> invoke(String parentTrashBinPath);
    }

}


