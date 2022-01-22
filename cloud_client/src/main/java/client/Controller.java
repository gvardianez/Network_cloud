package client;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import file_handlers.FileDeleter;
import file_transfer.FileDownloader;
import file_transfer.FileUploader;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.AbstractMessage;
import models.*;

public class Controller implements Initializable {

    public ProgressBar progressBarForClient;
    public VBox serverBox;
    public ProgressBar progressBarForServer;
    private FileUploader fileUploader;
    public VBox sharePanel;
    public TextField shareField;
    public VBox createDirOnClient;
    public TextField createDirTextField;
    public VBox createDirOnServer;
    public TextField createDirOnServerTextField;
    public ListView<String> clientFiles;
    public ListView<String> serverFiles;
    public TextField loginField;
    public PasswordField passwordField;
    public TextField loginFieldReg;
    public PasswordField passwordFieldReg;
    public TextField nickFieldReg;
    public VBox loginPanel;
    public VBox registrationPanel;
    public HBox cloudPanel;
    private Path baseDir;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;

    private void read() {
        try {
            while (true) {
                AbstractMessage msg = (AbstractMessage) is.readObject();
                switch (msg.getMessageType()) {
                    case ERROR:
                        ErrorMessage errorMessage = (ErrorMessage) msg;
                        Platform.runLater(() -> showError(errorMessage.getErrorMessage()));
                        break;
                    case AUTH_OK:
                        loginPanel.setVisible(false);
                        cloudPanel.setVisible(true);
                        break;
                    case FILES_LIST:
                        FilesList files = (FilesList) msg;
                        Platform.runLater(() -> fillServerView(files.getFiles()));
                        break;
                    case RECEIVE_SHARE_FILES:
                        ListShareFilesMessage listShareFilesMessage = (ListShareFilesMessage) msg;
                        Platform.runLater(() -> fillServerView(listShareFilesMessage.getShareFiles()));
                        break;
                    case CREATE_DIR:
                        CreateDirectoryMessage createDirectoryMessage = (CreateDirectoryMessage) msg;
                        createDirectory(createDirectoryMessage.getName());
                        Platform.runLater(() -> fillClientView(getFileNames()));
                        break;
                    case FILE:
                        FileMessage fileMessage = (FileMessage) msg;
                        if (fileMessage.getProgress() >= 1) {
                            Platform.runLater(() -> progressBarForServer.setProgress(0));
                            Platform.runLater(() -> fillClientView(getFileNames()));
                        } else {
                            Platform.runLater(() -> progressBarForServer.setProgress(fileMessage.getProgress()));
                        }
                        FileDownloader.downloadFile(fileMessage, baseDir);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ERROR");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private void fillServerView(List<String> list) {
        serverFiles.getItems().clear();
        serverFiles.getItems().addAll(list);
    }

    private void fillClientView(List<String> list) {
        clientFiles.getItems().clear();
        clientFiles.getItems().addAll(list);
    }

    private List<String> getFileNames() {
        try {
            return Files.list(baseDir)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
//            baseDir = Paths.get(System.getProperty("user.home"));
            baseDir = Paths.get("E:\\");
            int BUFFER_SIZE = 256 * 1024;
            byte[] buffer = new byte[BUFFER_SIZE];
            setPropertiesClientFiles();
            setPropertiesServerFiles();
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
            fileUploader = new FileUploader(os, BUFFER_SIZE, progressBarForClient);
            Thread thread = new Thread(this::read);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPropertiesClientFiles() {
        clientFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        clientFiles.getItems().addAll(getFileNames());
        clientFiles.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String file = clientFiles.getSelectionModel().getSelectedItem();
                Path path = baseDir.resolve(file);
                if (Files.isDirectory(path)) {
                    baseDir = path;
                    fillClientView(getFileNames());
                } else {
                    try {
                        Desktop.getDesktop().open(path.toFile());
                    } catch (IOException ioException) {
                        showError(ioException.getMessage());
                        ioException.printStackTrace();
                    }
                }
            }
        });
    }

    private void setPropertiesServerFiles() {
        serverFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serverFiles.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                try {
                    os.writeObject(new OpenDirectoryMessage(serverFiles.getSelectionModel().getSelectedItem()));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    public void upload(ActionEvent actionEvent) {
        List<String> selectedFiles = new ArrayList<>(clientFiles.getSelectionModel().getSelectedItems());
        if (selectedFiles.size() == 0) return;
        fileUploader.uploadFileOnServer(selectedFiles, baseDir, serverBox);
    }

    public void downLoad(ActionEvent actionEvent) throws IOException {
        List<String> selectedFiles = new ArrayList<>(serverFiles.getSelectionModel().getSelectedItems());
        if (selectedFiles.size() == 0) return;
        os.writeObject(new FileRequest(selectedFiles));
    }

    public void registrationAuth(ActionEvent actionEvent) throws IOException {
        if (loginFieldReg.getText().trim().isEmpty() || nickFieldReg.getText().trim().isEmpty() || passwordFieldReg.getText().trim().isEmpty())
            return;
        os.writeObject(new RegistrationAuth(loginFieldReg.getText(), nickFieldReg.getText(), passwordFieldReg.getText()));
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (loginField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty()) return;
            try {
                os.writeObject(new AuthMessage(loginField.getText(), passwordField.getText()));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void registration(ActionEvent actionEvent) {
        loginPanel.setVisible(false);
        registrationPanel.setVisible(true);
    }

    public void goBackOnLoginPanel(ActionEvent actionEvent) {
        loginPanel.setVisible(true);
        registrationPanel.setVisible(false);
    }

    public void deleteOnClient(ActionEvent actionEvent) {
        List<String> selectedFiles = new ArrayList<>(clientFiles.getSelectionModel().getSelectedItems());
        if (selectedFiles.size() == 0) return;
        FileDeleter.deleteFile(selectedFiles, baseDir);
        Platform.runLater(() -> fillClientView(getFileNames()));
    }

    public void deleteFromServer(ActionEvent actionEvent) {
        List<String> selectedFiles = new ArrayList<>(serverFiles.getSelectionModel().getSelectedItems());
        if (selectedFiles.size() == 0) return;
        try {
            os.writeObject(new DeleteFilesMessage(selectedFiles));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void back(ActionEvent actionEvent) {
        if (baseDir.toString().length() != 3) baseDir = baseDir.getParent();
        Platform.runLater(() -> fillClientView(getFileNames()));
    }

    public void refresh(ActionEvent actionEvent) {
        Platform.runLater(() -> fillClientView(getFileNames()));
    }

    public void backOnServer(ActionEvent actionEvent) {
        try {
            os.writeObject(new GoBackDirMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shareFile(ActionEvent actionEvent) {
        cloudPanel.setVisible(false);
        sharePanel.setVisible(true);
    }

    public void shareFileRequest(ActionEvent actionEvent) {
        if (shareField.getText().trim().isEmpty()) return;
        sharePanel.setVisible(false);
        cloudPanel.setVisible(true);
        try {
            os.writeObject(new ShareFileMessage(serverFiles.getSelectionModel().getSelectedItem(), shareField.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDirectory(ActionEvent actionEvent) {
        cloudPanel.setVisible(false);
        createDirOnClient.setVisible(true);
    }

    public void createDirOnClient(ActionEvent actionEvent) {
        cloudPanel.setVisible(true);
        createDirOnClient.setVisible(false);
        createDirectory(createDirTextField.getText());
    }

    private void createDirectory(String fileName) {
        try {
            Files.createDirectory(baseDir.resolve(fileName));
        } catch (FileAlreadyExistsException | InvalidPathException e) {
            Platform.runLater(() -> showError(e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDirOnServer(ActionEvent actionEvent) {
        cloudPanel.setVisible(true);
        createDirOnServer.setVisible(false);
        try {
            os.writeObject(new CreateDirectoryMessage(createDirOnServerTextField.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDirectoryOnServer(ActionEvent actionEvent) {
        cloudPanel.setVisible(false);
        createDirOnServer.setVisible(true);
    }

    public void refreshOnServer(ActionEvent actionEvent) {
        try {
            os.writeObject(new RefreshMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}