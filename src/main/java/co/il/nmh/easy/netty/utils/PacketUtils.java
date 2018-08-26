package co.il.nmh.easy.netty.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import co.il.nmh.easy.netty.core.Packet;
import co.il.nmh.easy.netty.data.EasyNettyConstants;

/**
 * @author Maor Hamami
 */

public class PacketUtils
{
	public static byte[] buildPacketData(Packet packet, boolean compress)
	{
		byte[] header = new byte[] { -86, 85 };
		byte[] data = packet.getData();
		byte[] opcodeBytes = new byte[] { (byte) packet.getOpcode() };
		byte[] compressIndicator = new byte[] { 0 };
		byte[] tail = new byte[] { 85, -86 };

		if (data == null)
		{
			data = new byte[0];
		}

		if (compress)
		{
			byte[] compressedData = CompressionUtils.compress(data);

			if (compressedData.length < data.length)
			{
				compressIndicator[0] = 1;
				data = compressedData;
			}
		}

		byte[] lengthBytes = intToByteArray(data.length + 2); // 1 for opcode and 1 for compress indicator

		int length = header.length + EasyNettyConstants.PACKET_LENGTH_SIZE + opcodeBytes.length + compressIndicator.length + data.length + tail.length;

		ByteBuffer byteBuffer = ByteBuffer.allocate(length);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.put(header);
		byteBuffer.put(lengthBytes);
		byteBuffer.put(opcodeBytes);
		byteBuffer.put(compressIndicator);
		byteBuffer.put(data);
		byteBuffer.put(tail);

		return byteBuffer.array();
	}

	public static short byteArrayToShort(byte[] data, int startPosition)
	{
		short value = 0;

		for (int i = 0; i < data.length - startPosition; i++)
		{
			value += (data[i + startPosition] & 0xffL) << (8 * i);
		}

		return value;
	}

	public static byte[] intToByteArray(int value)
	{
		byte[] ret = new byte[4];

		ret[0] = (byte) (value & 0xff);
		ret[1] = (byte) ((value >> 8) & 0xff);
		ret[2] = (byte) ((value >> 16) & 0xff);
		ret[3] = (byte) ((value >> 24) & 0xff);

		return ret;
	}
}
