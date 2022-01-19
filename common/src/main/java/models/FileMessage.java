package models;

import lombok.Data;

@Data
public class FileMessage implements AbstractMessage {

    private String fileName;
    private byte[] bytes;
    private int count;
    private boolean isFirstPart;
    private double progress;

    public FileMessage(String fileName, byte[] bytes, int count, boolean isFirstPart) {
        this.fileName = fileName;
        this.bytes = bytes;
        this.count = count;
        this.isFirstPart = isFirstPart;
    }

    public FileMessage(String fileName, byte[] bytes, int count, boolean isFirstPart, double progress) {
        this.fileName = fileName;
        this.bytes = bytes;
        this.count = count;
        this.isFirstPart = isFirstPart;
        this.progress = progress;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.FILE;
    }
}
