package co.il.nmh.easy.netty.core;

import co.il.nmh.easy.netty.excecptions.EasyNettyException;

/**
 * @author Maor Hamami
 */

public abstract class AbstractPacketHandler
{
	public abstract void handlePacket(Packet requestPacket) throws EasyNettyException;

	public void close()
	{
	}
}
