package co.il.nmh.easy.netty.data;

/**
 * @author Maor Hamami
 */

public class EasyNettyConstants
{
	public static final int HEADER_SIZE = 2;
	public static final int TAIL_SIZE = 2;
	public static final int PACKET_LENGTH_SIZE = 4;
	public static final int OPCODE_SIZE = 1;
	public static final int COMPRESS_IND_SIZE = 1;

	public final static int BYTE_SIZE = 1;
	public static final int SHORT_SIZE = 2;
	public static final int INT_SIZE = 4;

	public static final int HEADER_PACKET_VALID_VALUE = 0x55AA;
	public static final String INVALID_PACKET = "Packet without valid header";

	public static final int SEND_BUFFER_SIZE = 65000;
	public static final int RECEIVE_BUFFER_SIZE = 65000;
}
