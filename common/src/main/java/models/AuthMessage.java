package models;

import lombok.Data;

@Data
public class AuthMessage implements AbstractMessage {

    private String login;
    private String password;

    public AuthMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.AUTH;
    }
}
