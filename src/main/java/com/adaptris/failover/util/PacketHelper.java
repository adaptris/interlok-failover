package com.adaptris.failover.util;

import static com.adaptris.failover.util.Constants.MASTER;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

import com.adaptris.failover.Ping;

public class PacketHelper {
    
  public static final int STANDARD_PACKET_SIZE = 70;
  
  private static final int MAX_HOST_LENGTH = 40;
  
  private static final int MAX_PORT_LENGTH = 6;
  
  public static boolean isMasterPing(Ping ping) {
    return ping.getInstanceType() == MASTER;
  }
  
  public static Ping createPingRecord(DatagramPacket datagramPacket) {
    return createPingRecord(datagramPacket.getData());
  }
  
  public static Ping createPingRecord(byte[] data) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    
    Ping ping = new Ping();
    byte[] hostArray = new byte[MAX_HOST_LENGTH];
    byteBuffer.get(hostArray);
    byte[] portArray = new byte[MAX_PORT_LENGTH];
    byteBuffer.get(portArray);
    
    ping.setSourceHost(new String(hostArray).trim());
    ping.setSourcePort(new String(portArray).trim());
    long bigBits = byteBuffer.getLong();
    long littleBits = byteBuffer.getLong();
    ping.setInstanceId(new UUID(bigBits, littleBits));
    ping.setInstanceType(byteBuffer.getInt());
    ping.setSecondaryNumber(byteBuffer.getInt());
    
    return ping;
  }
  
  public static DatagramPacket createDatagramPacket(Ping ping, String group, int port) throws UnknownHostException {
    byte[] packetBytes = createDataPacket(ping);
    return new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(group), port);
  }
  
  public static byte[] createDataPacket(Ping ping) throws UnknownHostException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[STANDARD_PACKET_SIZE]);
    
    byteBuffer.put(padStringByteArray(ping.getSourceHost(), MAX_HOST_LENGTH));
    byteBuffer.put(padStringByteArray(ping.getSourcePort(), MAX_PORT_LENGTH));
    byteBuffer.putLong(ping.getInstanceId().getMostSignificantBits());
    byteBuffer.putLong(ping.getInstanceId().getLeastSignificantBits());
    byteBuffer.putInt(ping.getInstanceType());
    byteBuffer.putInt(ping.getSecondaryNumber());
    
    return byteBuffer.array();
  }

  private static byte[] padStringByteArray(String sourceString, int maxLength) {
    byte[] paddedArray = new byte[maxLength];
    System.arraycopy(sourceString.getBytes(), 0, paddedArray, 0, sourceString.getBytes().length);
    
    return paddedArray;
  }

}
