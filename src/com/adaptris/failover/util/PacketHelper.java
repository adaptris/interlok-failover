package com.adaptris.failover.util;

import static com.adaptris.failover.util.Constants.MASTER;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

import com.adaptris.failover.Ping;

public class PacketHelper {
  
  public static final int STANDARD_PACKET_SIZE = 24;
  
  public static boolean isMasterPing(Ping ping) {
    return ping.getInstanceType() == MASTER;
  }
  
  public static Ping createPingRecord(DatagramPacket datagramPacket) {
    return createPingRecord(datagramPacket.getData());
  }
  
  public static Ping createPingRecord(byte[] data) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    
    Ping ping = new Ping();
    long bigBits = byteBuffer.getLong();
    long littleBits = byteBuffer.getLong();
    ping.setInstanceId(new UUID(bigBits, littleBits));
    ping.setInstanceType(byteBuffer.getInt());
    ping.setSlaveNumber(byteBuffer.getInt());
    
    return ping;
  }
  
  public static DatagramPacket createDatagramPacket(Ping ping, String group, int port) throws UnknownHostException {
    byte[] packetBytes = createDataPacket(ping);
    return new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(group), port);
  }
  
  public static byte[] createDataPacket(Ping ping) throws UnknownHostException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[24]);
    
    byteBuffer.putLong(ping.getInstanceId().getMostSignificantBits());
    byteBuffer.putLong(ping.getInstanceId().getLeastSignificantBits());
    byteBuffer.putInt(ping.getInstanceType());
    byteBuffer.putInt(ping.getSlaveNumber());
    
    return byteBuffer.array();
  }

}
