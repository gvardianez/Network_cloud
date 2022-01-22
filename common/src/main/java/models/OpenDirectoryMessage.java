package models;

import lombok.Data;

import java.nio.file.Path;

@Data
public class OpenDirectoryMessage implements AbstractMessage {

    private String name;

    public OpenDirectoryMessage(String name){
        this.name = name;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.OPEN_DIR;
    }
}
