package models;

import lombok.Data;

import java.nio.file.Path;

@Data
public class CreateDirectoryMessage implements AbstractMessage{

    private String name;

    public CreateDirectoryMessage(String path){
        this.name = path;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CREATE_DIR;
    }
}
