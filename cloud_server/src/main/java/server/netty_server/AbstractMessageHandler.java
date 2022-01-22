package server.netty_server;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;

import file_handlers.FileDeleter;
import file_transfer.FileDownloader;
import file_transfer.FileUploader;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import server.errors.*;
import server.services.ContextStoreService;
import server.services.autorization.AuthorizationService;
import models.*;
import server.services.share.ShareService;

@Slf4j
public class AbstractMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private final AuthorizationService dataBaseAuthService;
    private final ShareService shareService;
    private final static String BASE_PATH = "serverFiles";
    private FileUploader fileUploader;
    private Path currentPath;
    private String nickName;
    private Path pathFile;
    private boolean isShareDir;
    private boolean isFirstEnterShare;
    private Path sharePath;
    private final ContextStoreService contextStoreService;
    private final NettyServer nettyServer;
    private static final int BUFFER_SIZE = 256 * 1024;

    public AbstractMessageHandler(AuthorizationService authService, ContextStoreService contextStoreService, ShareService shareService, NettyServer nettyServer) {
        this.dataBaseAuthService = authService;
        this.shareService = shareService;
        this.contextStoreService = contextStoreService;
        currentPath = Paths.get(BASE_PATH);
        sharePath = Paths.get(BASE_PATH);
        this.nettyServer = nettyServer;
        isFirstEnterShare = true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                AbstractMessage message) throws IOException {
        System.out.println(Thread.currentThread().getName());
        switch (message.getMessageType()) {
            case AUTH:
                AuthMessage auth = (AuthMessage) message;
                authorization(ctx, auth);
                break;
            case REGISTRATION:
                RegistrationAuth reg = (RegistrationAuth) message;
                registration(ctx, reg);
                break;
            case FILE_REQUEST:
                FileRequest req = (FileRequest) message;
                fileUploader.uploadFileOnClient(req.getFileNames(), sharePath, currentPath, isShareDir);
                break;
            case FILE:
                FileMessage fileMessage = (FileMessage) message;
                FileDownloader.downloadFile(fileMessage, currentPath);
                break;
            case DEL_FILES:
                DeleteFilesMessage deleteFiles = (DeleteFilesMessage) message;
                List<String> filesForDelete = deleteFiles.getFilesForDelete();
                filesForDelete.remove(Paths.get("serverFiles", nickName, "SHARING").toString());
                FileDeleter.deleteFile(filesForDelete, currentPath);
                ctx.writeAndFlush(new FilesList(currentPath));
                break;
            case OPEN_DIR:
                OpenDirectoryMessage openDirectoryMessage = (OpenDirectoryMessage) message;
                openDirectory(ctx, openDirectoryMessage);
                break;
            case CREATE_DIR:
                CreateDirectoryMessage createDirectoryMessage = (CreateDirectoryMessage) message;
                if (checkShareDir(currentPath.toString())) return;
                try {
                    Files.createDirectory(currentPath.resolve(createDirectoryMessage.getName()));
                } catch (FileAlreadyExistsException e) {
                    ctx.writeAndFlush(new ErrorMessage("Already exists " + e.getMessage()));
                }
                ctx.writeAndFlush(new FilesList(currentPath));
                break;
            case GO_BACK_DIR:
                goBackDir(ctx);
                break;
            case SHARE_FILE:
                ShareFileMessage shareFileMessage = (ShareFileMessage) message;
                shareFile(ctx, shareFileMessage);
                break;
            case REFRESH: {
                if (isShareDir) return;
                ctx.writeAndFlush(new FilesList(currentPath));
                break;
            }
        }
    }

    private void shareFile(ChannelHandlerContext ctx, ShareFileMessage shareFileMessage) {
        try {
            shareService.shareFile(shareFileMessage.getNickName(), shareFileMessage.getFileName(), currentPath.resolve(shareFileMessage.getFileName()).toString().substring(12));
        } catch (ShareNotAvailableException e) {
            ctx.writeAndFlush(new ErrorMessage("File With This Name exists"));
        } catch (UserNotFoundException e) {
            ctx.writeAndFlush(new ErrorMessage("User Not Found"));
        }
    }

    private void goBackDir(ChannelHandlerContext ctx) throws IOException {
        if (currentPath.equals(Paths.get("serverFiles", nickName)))
            return;
        if (sharePath.equals(pathFile)) {
            isFirstEnterShare = true;
            sharePath = Paths.get(BASE_PATH);
            ctx.writeAndFlush(new ListShareFilesMessage(shareService.loadShareFiles(nickName)));
            return;
        }
        if ((isShareDir && isFirstEnterShare) || sharePath.toString().equals(BASE_PATH)) {
            currentPath = currentPath.getParent();
            ctx.writeAndFlush(new FilesList(currentPath));
            isShareDir = false;
            isFirstEnterShare = true;
            return;
        }
        if (isShareDir) {
            sharePath = sharePath.getParent();
            ctx.writeAndFlush(new FilesList(sharePath));
        } else {
            currentPath = currentPath.getParent();
            ctx.writeAndFlush(new FilesList(currentPath));
        }
    }

    private void registration(ChannelHandlerContext ctx, RegistrationAuth reg) throws IOException {
        try {
            Files.createDirectory(currentPath.resolve(reg.getNickName()));
            dataBaseAuthService.createNewUser(reg.getLogin(), reg.getPassword(), reg.getNickName());
            File file = Paths.get(currentPath.toString(), reg.getNickName(), "SHARING").toFile();
            Files.createDirectory(file.toPath());
        } catch (InvalidPathException e) {
            ctx.writeAndFlush(new ErrorMessage("Unacceptable symbols in Nickname"));
        } catch (LoginIsNotAvailableException e) {
            ctx.writeAndFlush(new ErrorMessage("Login is Not Available"));
        } catch (NicknameIsNotAvailableException | FileAlreadyExistsException e) {
            ctx.writeAndFlush(new ErrorMessage("Nickname is Not Available"));
        }
    }

    private void authorization(ChannelHandlerContext ctx, AuthMessage auth) throws IOException {
        try {
            nickName = dataBaseAuthService.getNicknameByLoginAndPassword(auth.getLogin(), auth.getPassword());
            List<String> nicknameAuthUser = nettyServer.getNicknameAuthUsers();
            checkAlreadyAuth(nicknameAuthUser);
            nettyServer.getNicknameAuthUsers().add(nickName);
            currentPath = currentPath.resolve(nickName);
            ctx.writeAndFlush(new AuthOkMessage());
            ctx.writeAndFlush(new FilesList(currentPath));
        } catch (AlreadyAuthorizeException e) {
            ctx.writeAndFlush(new ErrorMessage(e.getMessage()));
        } catch (UserNotFoundException e) {
            ctx.writeAndFlush(new ErrorMessage("User Not Found"));
        } catch (WrongCredentialsException e) {
            ctx.writeAndFlush(new ErrorMessage("Wrong Credentials"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openDirectory(ChannelHandlerContext ctx, OpenDirectoryMessage openDirectoryMessage) throws IOException {
        if (isShareDir) {
            openShareDir(ctx, openDirectoryMessage);
            return;
        }
        Path pathTemp = currentPath.resolve(openDirectoryMessage.getName());
        if (checkShareDir(pathTemp.toString()) && !isShareDir) {
            currentPath = pathTemp;
            isShareDir = true;
            ctx.writeAndFlush(new ListShareFilesMessage(shareService.loadShareFiles(nickName)));
            return;
        }
        if (Files.isDirectory(pathTemp)) {
            currentPath = pathTemp;
            ctx.writeAndFlush(new FilesList(currentPath));
        } else {
            Desktop.getDesktop().open(pathTemp.toFile());
        }
    }

    private void openShareDir(ChannelHandlerContext ctx, OpenDirectoryMessage openDirectoryMessage) throws IOException {
        Path pathToOpen = sharePath.resolve(openDirectoryMessage.getName());
        if (Files.isDirectory(pathToOpen)) {
            sharePath = sharePath.resolve(openDirectoryMessage.getName());
            if (isFirstEnterShare) {
                pathFile = sharePath;
                isFirstEnterShare = false;
            }
            ctx.writeAndFlush(new FilesList(sharePath));
        } else {
            try {
                Desktop.getDesktop().open(pathToOpen.toFile());
            } catch (IllegalArgumentException e) {
                ctx.writeAndFlush(new ErrorMessage(e.getMessage()));
                shareService.deleteFile(openDirectoryMessage.getName());
            }
        }
    }

    private void checkAlreadyAuth(List<String> nicknameAuthUser) {
        for (String s : nicknameAuthUser) {
            if (s.equals(nickName)) {
                throw new AlreadyAuthorizeException("Already Authorization");
            }
        }
    }

    public boolean checkShareDir(String pathName) {
        return (pathName.equals(Paths.get("serverFiles", nickName, "SHARING").toString()));
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("Client connected");
        contextStoreService.registerContext(ctx);
        this.fileUploader = new FileUploader(ctx, BUFFER_SIZE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        nettyServer.getNicknameAuthUsers().remove(nickName);
        log.debug("Client disconnected");
        contextStoreService.removeContext(ctx);
    }

}