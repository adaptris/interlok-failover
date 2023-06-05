package com.adaptris.failover.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.DatagramPacket;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.failover.Ping;

public class PacketHelperTest {

  private static final String GROUP = "204.0.0.1";
  private static final int PORT = 4444;
  private static final int SECONDARY = 2;
  private static final int PRIMARY = 1;

  private UUID uuid;

  @BeforeEach
  public void setUp() throws Exception {
    uuid = UUID.randomUUID();
  }

  @Test
  public void testTcpPacketRoundTrip() throws Exception {
    Ping pingRecord = new Ping();
    pingRecord.setInstanceId(uuid);
    pingRecord.setInstanceType(PRIMARY);
    pingRecord.setSecondaryNumber(5);
    pingRecord.setSourceHost("myHost");
    pingRecord.setSourcePort("1111");

    byte[] dataPacket = PacketHelper.createDataPacket(pingRecord);

    Ping roundTripPingRecord = PacketHelper.createPingRecord(dataPacket);

    assertEquals(pingRecord.getInstanceId(), roundTripPingRecord.getInstanceId());
    assertEquals(pingRecord.getInstanceType(), roundTripPingRecord.getInstanceType());
    assertEquals(pingRecord.getSecondaryNumber(), roundTripPingRecord.getSecondaryNumber());
    assertTrue(PacketHelper.isPrimaryPing(roundTripPingRecord));
  }

  @Test
  public void testUdpPacketRoundTrip() throws Exception {
    Ping pingRecord = new Ping();
    pingRecord.setInstanceId(uuid);
    pingRecord.setInstanceType(SECONDARY);
    pingRecord.setSecondaryNumber(5);
    pingRecord.setSourceHost("myHost");
    pingRecord.setSourcePort("1111");

    DatagramPacket dataPacket = PacketHelper.createDatagramPacket(pingRecord, GROUP, PORT);

    Ping roundTripPingRecord = PacketHelper.createPingRecord(dataPacket);

    assertEquals(pingRecord.getInstanceId(), roundTripPingRecord.getInstanceId());
    assertEquals(pingRecord.getInstanceType(), roundTripPingRecord.getInstanceType());
    assertEquals(pingRecord.getSecondaryNumber(), roundTripPingRecord.getSecondaryNumber());
    assertFalse(PacketHelper.isPrimaryPing(roundTripPingRecord));
  }

}
