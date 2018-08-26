package co.il.nmh.easy.netty.server.listeners;

import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * @author Maor Hamami
 */

public interface IEasyNettyServerListener
{
	public void clientConnected(long channelId, ChannelHandlerContext ctx);

	public void clientDisconnected(long channelId, ChannelHandlerContext ctx);

	public void serverShutdown();
}
