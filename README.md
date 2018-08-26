# easy-netty
wrapping netty library, providing fast and lightweight client/server.

server example:
```xml
	short hiPacketOpcode = (short)1;
	
	EasyNettyServer easyNettyServer = new EasyNettyServer(9000, 4, true);
	easyNettyServer.addListener(new IEasyNettyServerListener()
	{
		@Override
		public void serverShutdown()
		{
			log.info("server shutdown");
		}

		@Override
		public void clientDisconnected(long channelId, ChannelHandlerContext ctx)
		{
			log.info("client {} disconnected", channelId);
		}

		@Override
		public void clientConnected(long channelId, ChannelHandlerContext ctx)
		{
			log.info("client {} connected", channelId);
		}
	});

	easyNettyServer.registerHandler(hiPacketOpcode, new AbstractPacketHandler()
	{
		@Override
		public void handlePacket(Packet requestPacket) throws EasyNettyException
		{
			log.info("server recieved message: {}", requestPacket.getString());

			Packet packet = new Packet(requestPacket.getOpcode(), requestPacket.getChannelId());
			packet.putString("hi from server");

			easyNettyServer.sendAsyncMessage(packet);
		}
	});
```

client exmalpe:
```xml
	short hiPacketOpcode = (short)1;
	
	EasyNettyClient easyNettyClient = new EasyNettyClient("localhost", 9000, 4, true);
	easyNettyClient.registerHandler(hiOpcode, new AbstractPacketHandler()
	{
		@Override
		public void handlePacket(Packet requestPacket) throws EasyNettyException
		{
			log.info("client recieved message: {}", requestPacket.getString());
		}
	});

	Packet packet = new Packet(hiPacketOpcode);
	packet.putString("hi from client");

	easyNettyClient.sendAsyncMessage(packet);
```