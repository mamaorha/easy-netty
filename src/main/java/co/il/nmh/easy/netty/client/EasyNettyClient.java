package co.il.nmh.easy.netty.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import co.il.nmh.easy.netty.client.listeners.IEasyNettyClientListener;
import co.il.nmh.easy.netty.core.AbstractPacketHandler;
import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.core.PacketManager;
import co.il.nmh.easy.netty.data.EasyNettyConstants;
import co.il.nmh.easy.netty.excecptions.EasyNettyException;

/**
 * @author Maor Hamami
 */

public class EasyNettyClient implements Closeable
{
	protected String serverIP;
	protected int port;
	protected boolean compress;
	protected Socket socket;
	protected PacketManager packetManager;
	protected EasyClientReader easyClientReader;
	protected EasyClientWriter easyClientWriter;
	protected Set<IEasyNettyClientListener> easyNettyClientListeners;

	public EasyNettyClient(String serverIP, int port, int numberOfHandlers, boolean compress) throws UnknownHostException, IOException
	{
		this.serverIP = serverIP;
		this.port = port;
		this.compress = compress;

		this.socket = new Socket(serverIP, port);
		this.socket.setSendBufferSize(EasyNettyConstants.SEND_BUFFER_SIZE);
		this.socket.setReceiveBufferSize(EasyNettyConstants.RECEIVE_BUFFER_SIZE);
		this.socket.setTcpNoDelay(true);

		this.easyClientReader = new EasyClientReader(this);
		this.easyClientWriter = new EasyClientWriter(this, compress);
		this.packetManager = new PacketManager(numberOfHandlers);

		this.easyNettyClientListeners = new HashSet<>();
	}

	public void addListener(IEasyNettyClientListener easyNettyClientListener)
	{
		easyNettyClientListeners.add(easyNettyClientListener);
	}

	protected InputStream getInputStream() throws IOException
	{
		return socket.getInputStream();
	}

	protected OutputStream getOutputStream() throws IOException
	{
		return socket.getOutputStream();
	}

	public void sendAsyncMessage(Packet packet)
	{
		easyClientWriter.putPacket(packet);
	}

	@Override
	public void close()
	{
		packetManager.interrupt();
		easyClientReader.interrupt();
		easyClientWriter.interrupt();

		if (null != socket)
		{
			try
			{
				socket.close();
			}

			catch (IOException e)
			{
			}

			socket = null;

			for (IEasyNettyClientListener easyNettyClientListener : easyNettyClientListeners)
			{
				try
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							Thread.currentThread().setName("clientSocketCloseListener");

							easyNettyClientListener.clientSocketClosed();
						}
					}).start();
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	public void handlePacket(Packet packet) throws EasyNettyException
	{
		packetManager.handlePacket(packet);
	}

	public void registerHandler(short opcode, AbstractPacketHandler handler) throws EasyNettyException
	{
		packetManager.registerHandler(opcode, handler);
	}
}
