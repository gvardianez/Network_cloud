package models;

import lombok.Data;

@Data
public class ErrorMessage implements AbstractMessage{

    String errorMessage;

    public ErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ERROR;
    }
}
