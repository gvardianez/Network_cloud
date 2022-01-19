import server.netty_server.NettyServer;

public class ServerApp {
    public static void main(String[] args) {
        new NettyServer().start();
    }
}
