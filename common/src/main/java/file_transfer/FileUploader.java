package file_transfer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import models.CreateDirectoryMessage;
import models.FileMessage;
import models.RefreshMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class FileUploader {

    private ObjectEncoderOutputStream os;
    private ChannelHandlerContext cxt;
    private final byte[] buffer;
    private final ExecutorService executorService;
    private ProgressBar progressBar;
    private final int buff;
    private long size;
    private double progress, progressPart;

    public FileUploader(ObjectEncoderOutputStream os, int buffer, ProgressBar progressBar) {
        this.os = os;
        this.buff = buffer;
        executorService = Executors.newFixedThreadPool(1);
        this.buffer = new byte[buffer];
        this.progressBar = progressBar;
    }

    public FileUploader(ChannelHandlerContext cxt, int buffer) {
        this.cxt = cxt;
        executorService = Executors.newFixedThreadPool(1);
        this.buff = buffer;
        this.buffer = new byte[buffer];
    }

    public void uploadFileOnServer(List<String> selectedFiles, Path baseDir, VBox serverBox) {
        size = calculateSize(selectedFiles, baseDir);
        progressPart = 1.0 / (int) (size / buff);
        progressBar.setVisible(true);
        executorService.execute(() -> {
            serverBox.setDisable(true);
            for (String selectedFile : selectedFiles) {
                Path filePath = baseDir.resolve(selectedFile);
                try {
                    if (Files.isDirectory(filePath)) {
                        Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                            String pathFile;

                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                pathFile = preparePathForClient(dir, baseDir);
                                os.writeObject(new CreateDirectoryMessage(pathFile));
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                pathFile = preparePathForClient(file, baseDir);
                                uploadMessage(pathFile, file.toFile().toPath());
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        uploadMessage(selectedFile, filePath);
                    }
                    os.writeObject(new RefreshMessage());
                    Platform.runLater(()->progressBar.setProgress(0));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    serverBox.setDisable(false);
                }
            }
        });
    }

    private long calculateSize(List<String> selectedFiles, Path baseDir) {
        size = 0;
        progress = 0;
        selectedFiles.
                stream().
                map(s -> Paths.get(baseDir.toString(), s)).
                forEach(s -> {
                    try (Stream<Path> walk = Files.walk(s)) {
                        size += walk.map(Path::toFile).
                                filter(File::isFile)
                                .mapToLong(File::length).sum();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return size;
    }

    private void uploadMessage(String file, Path filePathBaseDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(
                filePathBaseDir.toString())) {
            int count, part = 0;
            while ((count = fis.read(buffer)) >= 0) {
                progress = progress + progressPart;
                if (part == 0) {
                    os.writeObject(new FileMessage(file, buffer, count, true));
                } else {
                    os.writeObject(new FileMessage(file, buffer, count, false));
                }
                Platform.runLater(() -> progressBar.setProgress(progress));
                part++;
            }
        }
    }

    private String preparePathForClient(Path path, Path baseDir) {
        String pathFile;
        if (baseDir.toString().length() == 3) {
            pathFile = path.toString().substring(baseDir.toString().length());
        } else pathFile = path.toString().substring(baseDir.toString().length() + 1);
        return pathFile;
    }

    public void uploadFileOnClient(List<String> selectedFiles, Path sharePath, Path currentPath, boolean isShareDir) {
        ChannelHandlerContext ctx = this.cxt;
        Path baseDir = isShareDir ? sharePath : currentPath;
        size = calculateSize(selectedFiles, baseDir);
        progressPart = 1.0 / (int) (size / buff);
        executorService.execute(() -> {
            for (String selectedFile : selectedFiles) {
                Path filePath = baseDir.resolve(selectedFile);
                try {
                    if (Files.isDirectory(filePath)) {
                        Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                            String pathFile;

                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                pathFile = preparePathForServer(dir, sharePath, currentPath, isShareDir);
                                System.out.println("file name " + pathFile);
                                ctx.writeAndFlush(new CreateDirectoryMessage(pathFile));
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                pathFile = preparePathForServer(file, sharePath, currentPath, isShareDir);
                                System.out.println("file name " + pathFile);
                                uploadMessage(pathFile, file.toFile().toPath(), ctx);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        uploadMessage(selectedFile, filePath, ctx);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void uploadMessage(String file, Path filePathBaseDir, ChannelHandlerContext ctx) throws IOException {
        try (FileInputStream fis = new FileInputStream(
                filePathBaseDir.toString())) {
            int count, part = 0;
            while ((count = fis.read(buffer)) >= 0) {
                progress = progress + progressPart;
                System.out.println(Thread.currentThread().getName());
                if (part == 0) {
                    ctx.writeAndFlush(new FileMessage(file, buffer, count, true, progress));
                } else {
                    ctx.writeAndFlush(new FileMessage(file, buffer, count, false, progress));
                }
                part++;
            }
        }
    }

    private String preparePathForServer(Path dir, Path sharePath, Path currentPath, boolean isShareDir) {
        if (isShareDir) {
            String path = dir.toString().substring(sharePath.toString().length() + 1);
            if (path.contains("\\")) {
                return path.substring(path.indexOf("\\") + 1);
            } else
                return path;
        } else
            return dir.toString().substring(currentPath.toString().length() + 1);
    }

    public void stop() {
        executorService.shutdown();
    }

}
