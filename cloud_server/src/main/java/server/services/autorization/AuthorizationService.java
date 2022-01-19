package server.services.autorization;

import java.sql.SQLException;

public interface AuthorizationService {
    void start();

    void stop();

    String getNicknameByLoginAndPassword(String login, String password) throws SQLException;

    void createNewUser(String login, String password, String nickname);
}
