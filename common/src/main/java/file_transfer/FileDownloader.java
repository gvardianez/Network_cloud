package file_transfer;

import models.FileMessage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileDownloader {

    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static void downloadFile(FileMessage fileMessage, Path baseDir) {
        Path path = baseDir.resolve(fileMessage.getFileName());
        executorService.execute(() -> {
            try {
                if (!path.toFile().exists()) {
                    Files.createFile(path);
                    copyFile(fileMessage, path, false);
                } else copyFile(fileMessage, path, !path.toFile().exists() || (!fileMessage.isFirstPart()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void copyFile(FileMessage fileMessage, Path path, boolean append) {
        try (FileOutputStream fos = new FileOutputStream(
                path.toFile(), append)) {
            fos.write(fileMessage.getBytes(), 0, fileMessage.getCount());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}