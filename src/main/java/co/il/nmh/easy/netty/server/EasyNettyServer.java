package co.il.nmh.easy.netty.server;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import co.il.nmh.easy.netty.core.AbstractPacketHandler;
import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.core.PacketManager;
import co.il.nmh.easy.netty.excecptions.EasyNettyException;
import co.il.nmh.easy.netty.server.listeners.IEasyNettyServerListener;
import co.il.nmh.easy.netty.server.pipeline.factories.BytesPipelineFactory;
import co.il.nmh.easy.netty.utils.PacketUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Maor Hamami
 */
@Slf4j
public class EasyNettyServer implements Closeable
{
	public static final String TCP_NO_DELAY = "child.tcpNoDelay";
	public static final String RECEIVE_BUFFER_SIZE = "child.receiveBufferSize";
	public static final String SEND_BUFFER_SIZE = "child.sendBufferSize";
	public static final String KEEP_ALIVE = "child.keepAlive";

	private static final String CLIENT_CONNECTED = "client connected [{}]";
	private static final String CLIENT_DISCONNECTED = "client disconnected [{}]";
	private static final String RESPONSE_PACKET_IS_EMPTY = "Response packet is empty";
	private static final String TIMEOUT_MS = "Got timeout for response (size=%s bytes) to be completed within the specified time limit [%s] milliseconds";

	// variables
	protected ServerBootstrap serverBootstrap;
	protected AtomicLong channelIdSeq;
	protected Map<Long, ChannelPipeline> channelPipelineMap;
	protected Channel serverChannel;
	protected PacketManager packetManager;
	protected boolean compress;
	protected Set<IEasyNettyServerListener> easyNettyServerListeners;

	public EasyNettyServer(int port, int numberOfThreads, boolean compress)
	{
		this.channelIdSeq = new AtomicLong(0);
		this.channelPipelineMap = new ConcurrentHashMap<>();
		this.easyNettyServerListeners = new HashSet<>();

		this.serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
		this.serverBootstrap.setPipelineFactory(new BytesPipelineFactory(this));

		init();

		// Bind and start to accept incoming connections.
		this.serverChannel = serverBootstrap.bind(new InetSocketAddress(port));
		this.packetManager = new PacketManager(numberOfThreads);

		this.compress = compress;
	}

	protected void init()
	{
		serverBootstrap.setOption(TCP_NO_DELAY, true);
		serverBootstrap.setOption(RECEIVE_BUFFER_SIZE, 1048576);
		serverBootstrap.setOption(SEND_BUFFER_SIZE, 1048576);
		serverBootstrap.setOption(KEEP_ALIVE, true);
		serverBootstrap.setOption("tcpNoDelay", true);
		serverBootstrap.setOption("reuseAddress", true);
		serverBootstrap.setOption("receiveBufferSize", 1048576);
	}

	public void addListener(IEasyNettyServerListener easyNettyServerListener)
	{
		easyNettyServerListeners.add(easyNettyServerListener);
	}

	public long getNextChannelIdSequence()
	{
		return channelIdSeq.getAndIncrement();
	}

	// opcode is packet type, for example we can say login opcode is 0, logout opcode is 1 and so on...
	public void registerHandler(short opcode, AbstractPacketHandler handler) throws EasyNettyException
	{
		packetManager.registerHandler(opcode, handler);
	}

	public void connected(long channelId, ChannelHandlerContext ctx)
	{
		if (!channelPipelineMap.containsKey(channelId))
		{
			channelPipelineMap.put(channelId, ctx.getPipeline());

			for (IEasyNettyServerListener easyNettyServerListener : easyNettyServerListeners)
			{
				easyNettyServerListener.clientConnected(channelId, ctx);
			}

			log.info(CLIENT_CONNECTED, channelId);
		}
	}

	public void disconnected(long channelId, ChannelHandlerContext ctx)
	{
		ChannelPipeline channelPipeline = channelPipelineMap.get(channelId);

		if (channelPipeline != null)
		{
			channelPipelineMap.remove(channelId);

			for (IEasyNettyServerListener easyNettyServerListener : easyNettyServerListeners)
			{
				easyNettyServerListener.clientDisconnected(channelId, ctx);
			}

			log.info(CLIENT_DISCONNECTED, channelId);
		}
	}

	public void disconnect(long channelId)
	{
		ChannelPipeline channelPipeline = channelPipelineMap.get(channelId);

		if (channelPipeline != null)
		{
			channelPipeline.getChannel().close();
			channelPipelineMap.remove(channelId);
		}
	}

	@Override
	public void close()
	{
		List<ChannelPipeline> pipelines = new ArrayList<>();

		// copy the pipelines to a list -> closing them will lead to delete from the map, so we can't iterate directly
		for (ChannelPipeline pipeLine : channelPipelineMap.values())
		{
			pipelines.add(pipeLine);
		}

		// close the pipelines
		for (ChannelPipeline pipeLine : pipelines)
		{
			pipeLine.getChannel().close();
		}

		this.serverChannel.disconnect();
		this.serverChannel.close();
		this.serverBootstrap.releaseExternalResources();
		this.packetManager.interrupt();

		for (IEasyNettyServerListener easyNettyServerListener : easyNettyServerListeners)
		{
			easyNettyServerListener.serverShutdown();
		}
	}

	public void addWork(Packet packet)
	{
		packetManager.addWork(packet);
	}

	public void sendAsyncMessage(Packet packet) throws EasyNettyException
	{
		sendMessage(packet, null);
	}

	public void sendMessage(Packet packet, Long timeoutInMillis) throws EasyNettyException
	{
		if (packet == null)
		{
			throw new RuntimeException(RESPONSE_PACKET_IS_EMPTY);
		}

		byte[] packetData = PacketUtils.buildPacketData(packet, compress);

		ChannelPipeline channelPipeline = channelPipelineMap.get(packet.getChannelId());

		if (channelPipeline != null)
		{
			if (channelPipeline.getChannel().isConnected())
			{
				ChannelFuture channelFuture = channelPipeline.getChannel().write(packetData);

				if (null != timeoutInMillis)
				{
					boolean isCompleted = channelFuture.awaitUninterruptibly(timeoutInMillis, TimeUnit.MILLISECONDS);

					if (!isCompleted)
					{
						throw new EasyNettyException(String.format(TIMEOUT_MS, packetData.length, timeoutInMillis));
					}
				}
			}

			else
			{
				channelPipelineMap.remove(packet.getChannelId());
			}
		}
	}
}
