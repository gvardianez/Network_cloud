package models;

import lombok.Data;

@Data
public class RegistrationAuth implements AbstractMessage{

    private String login;
    private String nickName;
    private String password;

    public RegistrationAuth(String login, String nickName, String password) {
        this.login = login;
        this.nickName = nickName;
        this.password = password;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.REGISTRATION;
    }
}
