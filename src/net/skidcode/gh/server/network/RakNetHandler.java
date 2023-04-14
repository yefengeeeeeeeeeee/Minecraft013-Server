package net.skidcode.gh.server.network;


import net.skidcode.gh.server.Server;
import net.skidcode.gh.server.player.Player;
import net.skidcode.gh.server.protocol.EncapsulatedPacket;
import net.skidcode.gh.server.raknet.RakNet;
import net.skidcode.gh.server.raknet.server.RakNetServer;
import net.skidcode.gh.server.raknet.server.ServerHandler;
import net.skidcode.gh.server.raknet.server.ServerInstance;
import net.skidcode.gh.server.utils.Logger;

public class RakNetHandler implements ServerInstance{
	
	private RakNetServer raknet;
	private ServerHandler handler;
	
	public RakNetHandler() {
		this.raknet = new RakNetServer(Server.getPort());
		this.handler = new ServerHandler(raknet, this);
	}

    public void process() {
        do{}while(this.handler.handlePacket());
    }
    
    public void notifyShutdown() {
    	this.raknet.shutdown();
    }
    
	@Override
	public void openSession(String identifier, String address, int port, long clientID) {
		Server.addPlayer(identifier, new Player(identifier, clientID, address, port));
	}

	@Override
	public void closeSession(String identifier, String reason) {
		Server.removePlayer(identifier);
	}

	@Override
	public void handleEncapsulated(String identifier, EncapsulatedPacket packet, int flags) {
		Player player = Server.getPlayer(identifier);
		if(player instanceof Player) {
			MinecraftDataPacket dp = getPacket(packet);
			if(dp != null) {
				player.handlePacket(dp);
			}else {
				Logger.warn("Unknown PID: "+packet.buffer[0]);
			}
		}
	}
	
	public void sendPacket(Player player, MinecraftDataPacket packet) {
		packet.encode();
		EncapsulatedPacket pk = new EncapsulatedPacket();
		pk.buffer = packet.getBuffer();
		if (packet.channel != 0) {
			packet.reliability = 3;
			packet.orderChannel = packet.channel;
			packet.orderIndex = 0;
		} else {
			packet.reliability = 2;
		}

		/*if (needACK) {
			int iACK = this.identifiersACK.get(identifier);
			iACK++;
			pk.identifierACK = iACK;
			this.identifiersACK.put(identifier, iACK);
		}*/ //TODO and check is neccessary
		this.handler.sendEncapsulated(player.identifier, pk, 0 | RakNet.PRIORITY_NORMAL);
	}
	
	@Override
	public void handleRaw(String address, int port, byte[] payload) {
		Logger.info("RAW");
	}

	@Override
	public void notifyACK(String identifier, int identifierACK) {
		Logger.info("ACK");
	}

	@Override
	public void handleOption(String option, String value) {
	}
	
	@SuppressWarnings("unchecked")
	public MinecraftDataPacket getPacket(EncapsulatedPacket packet) {
		Class<MinecraftDataPacket> c = ProtocolInfo.packets[packet.buffer[0] & 0xff];
		try {
			MinecraftDataPacket dp = c.newInstance();
			dp.setBuffer(packet.buffer);
			dp.encapsulatedPacket = packet;
			dp.setOffset(1);
			dp.decode();
			return dp;
		} catch (Exception e1) {
			return null;
		}
	}
}
