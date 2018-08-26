package co.il.nmh.easy.netty.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.utils.PacketUtils;
import co.il.nmh.easy.utils.EasyThread;

/**
 * @author Maor Hamami
 */

public class EasyClientWriter extends EasyThread
{
	private static final String CLIENT_WRITER_THREAD = "easy-netty - ClientWriter";

	protected OutputStream outputStream;
	protected LinkedBlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
	protected boolean compress;

	public EasyClientWriter(EasyNettyClient easyNettyClient, boolean compress) throws IOException
	{
		super(CLIENT_WRITER_THREAD);

		this.outputStream = easyNettyClient.getOutputStream();
		this.compress = compress;

		start();
	}

	public void putPacket(Packet packet)
	{
		try
		{
			this.packetQueue.put(packet);
		}

		catch (InterruptedException e)
		{
			interrupt();
		}
	}

	@Override
	protected boolean loopRun() throws InterruptedException
	{
		try
		{
			Packet take = packetQueue.take();

			outputStream.write(PacketUtils.buildPacketData(take, compress));
			outputStream.flush();

			return true;
		}
		catch (Exception e)
		{
			interrupt();
			return false;
		}
	}
}
