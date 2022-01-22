package server.services.share;

import models.ErrorMessage;
import server.errors.ShareNotAvailableException;
import server.errors.UserNotFoundException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareBaseService implements ShareService {

    private final Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public ShareBaseService(Connection connection) {
        this.connection = connection;
        start();
    }

    @Override
    public void start() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("create table if not exists share (idShare int auto_increment primary key unique,idUser int not null, filename MEDIUMTEXT not null , filepath MEDIUMTEXT not null, foreign key (idUser) references users(idUsers));");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shareFile(String nickName, String fileName, String filePath) {
        int idUser;
        try {
            preparedStatement = connection.prepareStatement("select idUsers from users where nickname = ?;");
            preparedStatement.setString(1, nickName);
            resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) throw new UserNotFoundException("");
            idUser = resultSet.getInt("idUsers");
            preparedStatement = connection.prepareStatement("insert into share (idUser, filename,filepath) values (?, ?, ?);");
            preparedStatement.setInt(1, idUser);
            preparedStatement.setString(2, fileName);
            preparedStatement.setString(3, filePath);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }catch (SQLIntegrityConstraintViolationException e){
           throw new ShareNotAvailableException("");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> loadShareFiles(String nickName) {
        List<String> fileNames = new ArrayList<>();
        try {
            preparedStatement = connection.prepareStatement("select idShare,filepath from share join users on idUser = idUsers where nickname = ?;");
            preparedStatement.setString(1, nickName);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                fileNames.add((resultSet.getString("filepath")));
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            preparedStatement = connection.prepareStatement("delete from share where filepath = ?;");
            preparedStatement.setString(1, filePath);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
