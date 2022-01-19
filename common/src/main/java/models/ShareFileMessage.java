package models;

import lombok.Data;

@Data
public class ShareFileMessage implements AbstractMessage{

    private final String fileName;
    private final String nickName;

    @Override
    public MessageType getMessageType() {
        return MessageType.SHARE_FILE;
    }
}
