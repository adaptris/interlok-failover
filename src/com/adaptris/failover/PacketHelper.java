package com.adaptris.failover;

import java.net.DatagramPacket;

public class PacketHelper {
  
  public static final int STANDARD_PACKET_SIZE = 0;
  
  public static final int COMMAND_REQUEST_SLAVE_NUMBER = 1;
  
  public static final int COMMAND_RECEIVE_SLAVE_NUMBER = 2;
  
  public static final int COMMAND_MASTER_PING = 4;
  
  public static final int COMMAND_SLAVE_PING = 8;
  
  public static boolean isMasterPing(Ping ping) {
    return true;
  }
  
  public static Ping createPingRecord(DatagramPacket datagramPacket) {
    return new Ping();
  }
  
  public static DatagramPacket createDatagramPacket(Ping ping) {
    return new DatagramPacket(ping.getData(), ping.getData().length);
  }

}
