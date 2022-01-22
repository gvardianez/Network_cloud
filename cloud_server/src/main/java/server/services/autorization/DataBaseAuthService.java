package server.services.autorization;

import server.errors.LoginIsNotAvailableException;
import server.errors.NicknameIsNotAvailableException;
import server.errors.UserNotFoundException;
import server.errors.WrongCredentialsException;

import java.sql.*;

public class DataBaseAuthService implements AuthorizationService {
    private final Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public DataBaseAuthService(Connection connection) {
        this.connection = connection;
        start();
    }

    @Override
    public void start() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("create table if not exists users (idUsers int auto_increment primary key, nickname varchar(45) not null unique , login varchar(45) not null unique , password varchar(45) not null);");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        preparedStatement = connection.prepareStatement("select nickname from users where login = ?");
        preparedStatement.setString(1, login);
        resultSet = preparedStatement.executeQuery();
        if (!resultSet.next()) throw new UserNotFoundException("");
        preparedStatement = connection.prepareStatement("select nickname from users where login = ? and password = ?");
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, password);
        resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString("nickname");
        } else {
            preparedStatement.close();
            resultSet.close();
            throw new WrongCredentialsException("");
        }
    }

    @Override
    public void createNewUser(String login, String password, String nickname) {
        try {
            preparedStatement = connection.prepareStatement("select login from users where login = ?;");
            preparedStatement.setString(1, login);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) throw new LoginIsNotAvailableException("");
            preparedStatement = connection.prepareStatement("select nickname from users where nickname = ?;");
            preparedStatement.setString(1, nickname);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) throw new NicknameIsNotAvailableException("");
            preparedStatement = connection.prepareStatement("INSERT INTO `network_cloud`.`users` (`nickname`, `login`, `password`) VALUES (?, ?, ?);");
            preparedStatement.setString(1, nickname);
            preparedStatement.setString(2, login);
            preparedStatement.setString(3, password);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
