package server.netty_server;

import server.services.ContextStoreService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import server.services.autorization.AuthorizationService;
import server.services.autorization.DataBaseAuthService;
import server.services.share.ShareBaseService;
import server.services.share.ShareService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NettyServer {

    private final AuthorizationService authService;
    private final ShareService shareService;
    private Connection dataBaseConnection;
    private final static String NAME = "Alex";
    private final static String PASS = "1111";
    private final static String CONNECTION_URL = "jdbc:mysql://localhost:3306/network_cloud?useUnicode=true&serverTimezone=UTC";
    private final NettyServer nettyServer;
    private final List<String> nicknameAuthUsers;

    public NettyServer(){
        try {
            dataBaseConnection = DriverManager.getConnection(CONNECTION_URL, NAME, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        nicknameAuthUsers = new ArrayList<>();
        shareService = new ShareBaseService(dataBaseConnection);
        authService = new DataBaseAuthService(dataBaseConnection);
        nettyServer = this;
    }

    public List<String> getNicknameAuthUsers() {
        return nicknameAuthUsers;
    }

    public void start () {
        HandlerProvider provider = new HandlerProvider(new ContextStoreService());
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(
                                    provider.getSerializePipeline(authService,shareService,nettyServer)
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(8189).sync();
            log.debug("Server started...");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("e=", e);
        } finally {
            authService.stop();
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}