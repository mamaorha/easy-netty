package co.il.nmh.easy.netty.server.pipeline.handlers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.data.EasyNettyConstants;
import co.il.nmh.easy.netty.excecptions.EasyNettyException;
import co.il.nmh.easy.netty.server.EasyNettyServer;
import co.il.nmh.easy.netty.utils.CompressionUtils;
import co.il.nmh.easy.netty.utils.PacketUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Maor Hamami
 */

@Slf4j
public class BytesServerMessageHandler extends BaseServerMessageHandler
{
	private static final String TAIL_BUFFER = "TAIL_BUFFER";
	private static final int MIN_PACKET_SIZE = EasyNettyConstants.HEADER_SIZE + EasyNettyConstants.TAIL_SIZE + EasyNettyConstants.PACKET_LENGTH_SIZE + EasyNettyConstants.OPCODE_SIZE + EasyNettyConstants.COMPRESS_IND_SIZE;
	private static final String BAD_MESSAGE_TYPE = "Handler Type is [%s], message type should be 'byte[]'";

	public BytesServerMessageHandler(EasyNettyServer easyNettyServer, long channelId)
	{
		super(easyNettyServer, channelId);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws EasyNettyException
	{
		BigEndianHeapChannelBuffer msg = (BigEndianHeapChannelBuffer) e.getMessage();
		byte[] array = msg.array();

		ByteBuffer currBuff = null;

		int capacity = array.length;

		byte[] tailBuff = (byte[]) context.get(TAIL_BUFFER);

		if (tailBuff != null)
		{
			capacity += tailBuff.length;
			currBuff = ByteBuffer.allocate(capacity);
			currBuff.order(ByteOrder.LITTLE_ENDIAN);
			currBuff.put(tailBuff);
			context.remove(TAIL_BUFFER);
			tailBuff = null;
		}

		else
		{
			currBuff = ByteBuffer.allocate(capacity);
		}

		currBuff.order(ByteOrder.LITTLE_ENDIAN);
		currBuff.put(array);
		currBuff.rewind();

		msg.clear();

		int readableBytes = 0;

		while ((capacity - currBuff.position()) > 0)
		{
			readableBytes = (capacity - currBuff.position());

			if (readableBytes < MIN_PACKET_SIZE)
			{
				tailBuff = new byte[readableBytes];
				currBuff.get(tailBuff);
				context.put(TAIL_BUFFER, tailBuff);
				break;
			}

			int tempPosition = currBuff.position();

			byte[] headerByte = new byte[EasyNettyConstants.HEADER_SIZE];
			currBuff.get(headerByte);

			currBuff.position(tempPosition);

			short header = currBuff.getShort();

			if (header == 0 || header != EasyNettyConstants.HEADER_PACKET_VALID_VALUE)
			{
				continue;
			}

			int length = currBuff.getInt();

			readableBytes = (capacity - currBuff.position());

			// packet data length + the tail
			if (readableBytes < length + EasyNettyConstants.SHORT_SIZE)
			{
				tailBuff = new byte[readableBytes + EasyNettyConstants.HEADER_SIZE + EasyNettyConstants.INT_SIZE]; // header + length + rest of the bytes
				currBuff.position(currBuff.position() - EasyNettyConstants.HEADER_SIZE - EasyNettyConstants.INT_SIZE); // go back before the length and header (4 bytes)
				currBuff.get(tailBuff);
				context.put(TAIL_BUFFER, tailBuff);
				break;
			}

			byte[] opcodeByte = new byte[EasyNettyConstants.OPCODE_SIZE];
			currBuff.get(opcodeByte);

			short opcode = PacketUtils.byteArrayToShort(new byte[] { opcodeByte[0] }, 0);

			byte[] compressIndicator = new byte[EasyNettyConstants.COMPRESS_IND_SIZE];
			currBuff.get(compressIndicator);

			byte[] data = new byte[length - EasyNettyConstants.TAIL_SIZE];
			currBuff.get(data);

			byte[] tailByte = new byte[EasyNettyConstants.TAIL_SIZE];
			currBuff.get(tailByte);

			if (tailByte[0] != headerByte[1] || tailByte[1] != headerByte[0])
			{
				throw new EasyNettyException("Packet without tail");
			}

			if (compressIndicator[0] == 1)
			{
				data = CompressionUtils.decompress(data);
			}

			Packet packet = new Packet(opcode, channelId);
			packet.setData(data);
			packet.rewind();

			easyNettyServer.addWork(packet);
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext chctx, MessageEvent e) throws Exception
	{
		Object message = e.getMessage();

		if (message instanceof byte[])
		{
			byte[] bytes = (byte[]) message;

			ChannelBuffer cBuff = ChannelBuffers.buffer(bytes.length);
			cBuff.writeBytes(bytes);
			Channels.write(chctx, e.getFuture(), cBuff);
		}

		else
		{
			log.error(BAD_MESSAGE_TYPE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
	{
		log.error("error occured: {}, error: {}", e.getCause().getMessage(), e.getCause());
	}
}
