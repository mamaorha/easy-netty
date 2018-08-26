package co.il.nmh.easy.netty.utils;

import com.ning.compress.lzf.LZFDecoder;
import com.ning.compress.lzf.LZFEncoder;

/**
 * @author Maor Hamami
 */

public class CompressionUtils
{
	public static byte[] compress(byte[] content)
	{
		return LZFEncoder.encode(content);
	}

	public static byte[] decompress(byte[] contentBytes)
	{
		try
		{
			return LZFDecoder.decode(contentBytes);
		}
		catch (Exception e)
		{
			return contentBytes;
		}
	}
}
