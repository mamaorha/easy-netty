package co.il.nmh.easy.netty.server.pipeline.factories;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import co.il.nmh.easy.netty.server.EasyNettyServer;

/**
 * @author Maor Hamami
 */

public abstract class AbstractPipelineFactory implements ChannelPipelineFactory
{
	protected EasyNettyServer easyNettyServer;

	public AbstractPipelineFactory(EasyNettyServer easyNettyServer)
	{
		this.easyNettyServer = easyNettyServer;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception
	{
		return createPipeline(easyNettyServer.getNextChannelIdSequence());
	}

	public abstract ChannelPipeline createPipeline(long channelId) throws Exception;
}
