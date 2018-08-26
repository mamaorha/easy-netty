package co.il.nmh.easy.netty.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import co.il.nmh.easy.netty.data.EasyNettyConstants;
import co.il.nmh.easy.netty.excecptions.EasyNettyException;

/**
 * @author Maor Hamami
 */

public class Packet
{
	private Long channelId;
	private short opcode;
	private ByteBuffer byteBuffer;
	private int usedBytes;

	public Packet(short opcode)
	{
		this(opcode, null, null);
	}

	public Packet(short opcode, int packetSize)
	{
		this(opcode, null, packetSize);
	}

	public Packet(short opcode, Long channelId)
	{
		this(opcode, channelId, null);
	}

	public Packet(short opcode, Long channelId, Integer packetSize)
	{
		this.opcode = opcode;
		this.channelId = channelId;

		if (null != packetSize)
		{
			expand(packetSize);
		}
	}

	public void setChannelId(Long channelId)
	{
		this.channelId = channelId;
	}

	public Long getChannelId()
	{
		return channelId;
	}

	public short getOpcode()
	{
		return opcode;
	}

	public void setOpcode(short opcode)
	{
		this.opcode = opcode;
	}

	private void expand(int size)
	{
		if (null != byteBuffer)
		{
			int newSize = byteBuffer.capacity() * 2;

			if (newSize < byteBuffer.position() + size)
			{
				newSize = byteBuffer.position() + size;
			}

			size = newSize;
		}

		ByteBuffer newByteBuffer = ByteBuffer.allocate(size);
		newByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		if (null != byteBuffer)
		{
			newByteBuffer.put(byteBuffer.array());
		}

		this.byteBuffer = newByteBuffer;
		usedBytes = byteBuffer.position();
	}

	public void setData(byte[] data)
	{
		byteBuffer = ByteBuffer.allocate(data.length);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(data);

		usedBytes = byteBuffer.position();
	}

	public byte[] getData()
	{
		if (null == byteBuffer)
		{
			return null;
		}

		int position = byteBuffer.position();
		byteBuffer.rewind();

		byte[] data = new byte[usedBytes];
		byteBuffer.get(data, 0, usedBytes);

		byteBuffer.position(position);

		return data;
	}

	private void checkSpace(int needBytes)
	{
		if (null == byteBuffer || byteBuffer.remaining() < needBytes)
		{
			expand(needBytes);
		}
	}

	public void putInt(int data)
	{
		checkSpace(EasyNettyConstants.INT_SIZE);

		byteBuffer.putInt(data);
		usedBytes = byteBuffer.position();
	}

	public void putShort(short data)
	{
		checkSpace(EasyNettyConstants.SHORT_SIZE);

		byteBuffer.putShort(data);
		usedBytes = byteBuffer.position();
	}

	public String getString()
	{
		try
		{
			if (null != byteBuffer)
			{
				short length = byteBuffer.getShort();

				byte[] bytes = new byte[length];
				byteBuffer.get(bytes);

				return new String(bytes, StandardCharsets.UTF_8);
			}
		}
		catch (Exception e)
		{
		}

		return null;
	}

	public void putByte(int data)
	{
		checkSpace(EasyNettyConstants.BYTE_SIZE);

		byteBuffer.put(new byte[] { (byte) data });
		usedBytes = byteBuffer.position();
	}

	public byte[] getBytes(int size)
	{
		byte[] bytes = new byte[size];
		byteBuffer.get(bytes);

		return bytes;
	}

	public byte getByte()
	{
		if (null != byteBuffer)
		{
			return byteBuffer.get();
		}

		return 0;
	}

	public short getShort()
	{
		if (null != byteBuffer)
		{
			return byteBuffer.getShort();
		}

		return 0;
	}

	public int getInt()
	{
		if (null != byteBuffer)
		{
			return byteBuffer.getInt();
		}

		return 0;
	}

	public void putBytes(byte[] bytes)
	{
		checkSpace(bytes.length);
		byteBuffer.put(bytes);

		usedBytes = byteBuffer.position();
	}

	public void putString(String data) throws EasyNettyException
	{
		if (data.length() > Short.MAX_VALUE)
		{
			throw new EasyNettyException(String.format("data length must be less than %s, consider splitting this string into more packets", Short.MAX_VALUE));
		}

		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

		checkSpace(EasyNettyConstants.SHORT_SIZE + bytes.length);

		// add length
		byteBuffer.putShort((short) (data.length()));

		// add the string bytes
		byteBuffer.put(bytes);

		usedBytes = byteBuffer.position();
	}

	public void rewind()
	{
		if (null != byteBuffer)
		{
			byteBuffer.rewind();
		}
	}
}
