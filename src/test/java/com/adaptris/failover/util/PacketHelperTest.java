package com.adaptris.failover.util;

import java.net.DatagramPacket;
import java.util.UUID;

import com.adaptris.failover.Ping;

import junit.framework.TestCase;

public class PacketHelperTest extends TestCase {
  
  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 4444;
  private static final int SLAVE = 2;
  private static final int MASTER = 1;
  
  private UUID uuid;
  
  public void setUp() throws Exception {
    uuid = UUID.randomUUID();
  }
  
  public void tearDown() throws Exception {
  }
  
  public void testTcpPacketRoundTrip() throws Exception {
    Ping pingRecord = new Ping();
    pingRecord.setInstanceId(uuid);
    pingRecord.setInstanceType(MASTER);
    pingRecord.setSlaveNumber(5);
    pingRecord.setSourceHost("myHost");
    pingRecord.setSourcePort("1111");
    
    byte[] dataPacket = PacketHelper.createDataPacket(pingRecord);
    
    Ping roundTripPingRecord = PacketHelper.createPingRecord(dataPacket);
    
    assertEquals(pingRecord.getInstanceId(), roundTripPingRecord.getInstanceId());
    assertEquals(pingRecord.getInstanceType(), roundTripPingRecord.getInstanceType());
    assertEquals(pingRecord.getSlaveNumber(), roundTripPingRecord.getSlaveNumber());
    assertTrue(PacketHelper.isMasterPing(roundTripPingRecord));
  }
  
  public void testUdpPacketRoundTrip() throws Exception {
    Ping pingRecord = new Ping();
    pingRecord.setInstanceId(uuid);
    pingRecord.setInstanceType(SLAVE);
    pingRecord.setSlaveNumber(5);
    pingRecord.setSourceHost("myHost");
    pingRecord.setSourcePort("1111");
    
    DatagramPacket dataPacket = PacketHelper.createDatagramPacket(pingRecord, GROUP, PORT);
    
    Ping roundTripPingRecord = PacketHelper.createPingRecord(dataPacket);
    
    assertEquals(pingRecord.getInstanceId(), roundTripPingRecord.getInstanceId());
    assertEquals(pingRecord.getInstanceType(), roundTripPingRecord.getInstanceType());
    assertEquals(pingRecord.getSlaveNumber(), roundTripPingRecord.getSlaveNumber());
    assertFalse(PacketHelper.isMasterPing(roundTripPingRecord));
  }

}
