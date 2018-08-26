package co.il.nmh.easy.netty.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.data.EasyNettyConstants;
import co.il.nmh.easy.netty.excecptions.EasyNettyException;
import co.il.nmh.easy.netty.utils.CompressionUtils;
import co.il.nmh.easy.netty.utils.PacketUtils;
import co.il.nmh.easy.utils.EasyThread;

/**
 * @author Maor Hamami
 */

public class EasyClientReader extends EasyThread
{
	private static final String CLIENT_READER_THREAD = "easy-netty - ClientReader";

	protected EasyNettyClient easyNettyClient;
	protected InputStream inputStream;

	public EasyClientReader(EasyNettyClient easyNettyClient) throws IOException
	{
		super(CLIENT_READER_THREAD);

		this.easyNettyClient = easyNettyClient;
		this.inputStream = easyNettyClient.getInputStream();

		this.start();
	}

	@Override
	protected boolean loopRun() throws InterruptedException
	{
		try
		{
			int currAvailable;

			byte[] tempB = new byte[EasyNettyConstants.HEADER_SIZE + EasyNettyConstants.PACKET_LENGTH_SIZE];
			inputStream.read(tempB, 0, tempB.length);

			ByteBuffer currBuff = ByteBuffer.allocate(tempB.length);
			currBuff.order(ByteOrder.LITTLE_ENDIAN);
			currBuff.put(tempB);
			currBuff.rewind();

			int tempPosition = currBuff.position();

			byte[] headerByte = new byte[EasyNettyConstants.HEADER_SIZE];
			currBuff.get(headerByte);

			currBuff.position(tempPosition);

			short header = currBuff.getShort();

			if (header != EasyNettyConstants.HEADER_PACKET_VALID_VALUE)
			{
				throw new EasyNettyException("Packet without valid header");
			}

			int length = currBuff.getInt();
			int read = 0;

			tempB = new byte[length + EasyNettyConstants.TAIL_SIZE];

			while (read < length + EasyNettyConstants.TAIL_SIZE)
			{
				currAvailable = inputStream.available();

				if (currAvailable > 0)
				{
					if (read + currAvailable >= tempB.length)
					{
						currAvailable = tempB.length - read;
					}

					inputStream.read(tempB, read, currAvailable);
					read += currAvailable;
				}

				if (read >= length + EasyNettyConstants.TAIL_SIZE)
				{
					Thread.sleep(1);
				}
			}

			currBuff = ByteBuffer.allocate(tempB.length);
			currBuff.order(ByteOrder.LITTLE_ENDIAN);
			currBuff.put(tempB);
			currBuff.rewind();

			byte[] opcodeByte = new byte[EasyNettyConstants.OPCODE_SIZE];
			currBuff.get(opcodeByte);

			short opcode = PacketUtils.byteArrayToShort(new byte[] { opcodeByte[0] }, 0);

			byte[] compressedIndicator = new byte[1];
			currBuff.get(compressedIndicator);

			byte[] data = new byte[length - EasyNettyConstants.TAIL_SIZE];
			currBuff.get(data);

			if (compressedIndicator[0] == 1)
			{
				data = CompressionUtils.decompress(data);
			}

			byte[] tailByte = new byte[EasyNettyConstants.TAIL_SIZE];
			currBuff.get(tailByte);

			if (tailByte[0] != headerByte[1] || tailByte[1] != headerByte[0])
			{
				throw new Exception("Packet without valid tail");
			}

			Packet packet = new Packet(opcode);
			packet.setData(data);
			packet.rewind();

			easyNettyClient.handlePacket(packet);
			return true;
		}

		catch (Exception e)
		{
			try
			{
				easyNettyClient.close();
			}
			catch (Exception ex)
			{
				interrupt();
			}

			return false;
		}
	}

}
