package models;

import lombok.Data;

@Data
public class RefreshMessage implements AbstractMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.REFRESH;
    }
}
