package server.netty_server;

import server.services.ContextStoreService;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import server.services.autorization.AuthorizationService;
import server.services.share.ShareService;

public class HandlerProvider {

    private final ContextStoreService contextStoreService;
    public final static int MAX_SIZE = 1024 * 1024 * 1000;

    public HandlerProvider(ContextStoreService contextStoreService) {
        this.contextStoreService = contextStoreService;
    }

    public ChannelHandler[] getSerializePipeline(AuthorizationService authService, ShareService shareService, NettyServer nettyServer) {
        return new ChannelHandler[]{
                new ObjectDecoder(MAX_SIZE, ClassResolvers.cacheDisabled(null)),
                new ObjectEncoder(),
                new AbstractMessageHandler(authService, contextStoreService, shareService, nettyServer)
        };
    }

}