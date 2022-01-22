package server.services;

import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.channel.ChannelHandlerContext;

public class ContextStoreService {

    private final ConcurrentLinkedQueue<ChannelHandlerContext> queue;

    public ContextStoreService() {
        queue = new ConcurrentLinkedQueue<>();
    }

    public ConcurrentLinkedQueue<ChannelHandlerContext> getContexts() {
        return queue;
    }

    public void registerContext(ChannelHandlerContext ctx) {
        queue.add(ctx);
    }

    public void removeContext(ChannelHandlerContext ctx) {
        queue.remove(ctx);
    }
}