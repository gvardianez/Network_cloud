package models;

public class AuthOkMessage implements AbstractMessage{
    @Override
    public MessageType getMessageType() {
        return MessageType.AUTH_OK;
    }
}
