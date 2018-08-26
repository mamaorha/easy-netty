package co.il.nmh.easy.netty.core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import co.il.nmh.easy.netty.excecptions.EasyNettyException;
import co.il.nmh.easy.utils.EasyThread;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Maor Hamami
 */
@Slf4j
public class PacketManager extends EasyThread
{
	private static final String PACKET_HANDLER_THREAD = "easy-netty - PacketHandler";
	private static final String OPCODE_IS_ALREADY_HANDLED = "opcode [%s] is already handled";
	private static final String UNHANDLED_PACKET_OPCODE = "unhandled packet - opcode [{}]";

	private Map<Short, AbstractPacketHandler> packetTypesHandlers;
	protected BlockingQueue<Packet> requestQueue;

	protected ExecutorService threadExecutors;

	public PacketManager(int numberOfThreads)
	{
		super(PACKET_HANDLER_THREAD);

		this.packetTypesHandlers = new ConcurrentHashMap<>();
		this.requestQueue = new LinkedBlockingQueue<Packet>();

		this.threadExecutors = Executors.newFixedThreadPool(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++)
		{
			this.threadExecutors.submit(this);
		}
	}

	public void registerHandler(short opcode, AbstractPacketHandler handler) throws EasyNettyException
	{
		if (null != packetTypesHandlers.get(opcode))
		{
			throw new EasyNettyException(String.format(OPCODE_IS_ALREADY_HANDLED, opcode));
		}

		packetTypesHandlers.put(opcode, handler);
	}

	public void handlePacket(Packet requestPacket) throws EasyNettyException
	{
		AbstractPacketHandler handler = packetTypesHandlers.get(requestPacket.getOpcode());

		if (null != handler)
		{
			handler.handlePacket(requestPacket);
		}

		else
		{
			log.warn(UNHANDLED_PACKET_OPCODE, requestPacket.getOpcode());
		}
	}

	public void addWork(Packet packet)
	{
		try
		{
			requestQueue.put(packet);
		}
		catch (Exception e)
		{
		}
	}

	@Override
	public void interrupt()
	{
		if (null != threadExecutors)
		{
			ExecutorService temp = threadExecutors;
			threadExecutors = null;

			temp.shutdownNow();
		}

		super.interrupt();
	}

	@Override
	protected boolean loopRun() throws InterruptedException
	{
		try
		{
			Packet requestPacket = requestQueue.take();

			handlePacket(requestPacket);
		}

		catch (Exception e)
		{
			if (!isInterrupted())
			{
				log.error("error occured {}, error: {}", e.getMessage(), e);
			}
		}

		return true;
	}

	@Override
	protected void runEnded()
	{
		for (AbstractPacketHandler abstractPacketHandler : packetTypesHandlers.values())
		{
			abstractPacketHandler.close();
		}
	}
}
