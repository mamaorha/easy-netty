package co.il.nmh.easy.netty.server.pipeline.handlers;

import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import co.il.nmh.easy.netty.server.EasyNettyServer;

/**
 * @author Maor Hamami
 */

public class BaseServerMessageHandler extends SimpleChannelHandler
{
	// input
	protected EasyNettyServer easyNettyServer;
	protected long channelId;

	// variables
	protected Map<String, Object> context;

	public BaseServerMessageHandler(EasyNettyServer easyNettyServer, long channelId)
	{
		this.easyNettyServer = easyNettyServer;
		this.channelId = channelId;

		this.context = new HashMap<>();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		easyNettyServer.connected(channelId, ctx);

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		easyNettyServer.disconnected(channelId, ctx);

		super.channelDisconnected(ctx, e);
	}
}
