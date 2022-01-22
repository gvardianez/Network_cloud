package models;

import lombok.Data;

@Data
public class OpenShareDirectoryMessage implements AbstractMessage {

    private String name;

    public OpenShareDirectoryMessage(String name){
        this.name = name;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.OPEN_SHARE_DIR;
    }
}
