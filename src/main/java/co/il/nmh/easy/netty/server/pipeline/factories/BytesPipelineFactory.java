package co.il.nmh.easy.netty.server.pipeline.factories;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;

import co.il.nmh.easy.netty.server.EasyNettyServer;
import co.il.nmh.easy.netty.server.pipeline.handlers.BytesServerMessageHandler;

/**
 * @author Maor Hamami
 */

public class BytesPipelineFactory extends AbstractPipelineFactory
{
	public BytesPipelineFactory(EasyNettyServer easyNettyServer)
	{
		super(easyNettyServer);
	}

	@Override
	public ChannelPipeline createPipeline(long channelId) throws Exception
	{
		BytesServerMessageHandler serverMessageHandler = new BytesServerMessageHandler(easyNettyServer, channelId);
		return Channels.pipeline(serverMessageHandler);
	}
}
