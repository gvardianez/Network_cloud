package models;

import lombok.Data;

import java.util.List;

@Data
public class FileRequest implements AbstractMessage {

    private  List<String> fileNames;

    public FileRequest(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.FILE_REQUEST;
    }
}