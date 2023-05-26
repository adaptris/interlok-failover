package com.adaptris.failover.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class PropertiesHelperTest {

  private static final String PROPERTIES_PATH = "./src/test/resources/bootstrap.properties";

  @Test
  public void testLoadProperties() throws Exception {
    Properties properties = PropertiesHelper.load(PROPERTIES_PATH);
    assertEquals("tcp", properties.getProperty(Constants.SOCKET_MODE));
    assertEquals("4444", properties.getProperty(Constants.FAILOVER_TCP_PORT_KEY));
    assertEquals("localhost:4445;localhost:4446", properties.getProperty(Constants.FAILOVER_TCP_PEERS_KEY));
  }

  @Test
  public void testLoadPropertiesDoesnExist() throws Exception {
    try {
      PropertiesHelper.load("IDoNotExist");
      fail("Should fail, properties does not exist.");
    } catch (IOException ex) {
      // expected
    }
  }

  @Test
  public void testVerifyProperties() throws Exception {
    Properties properties = PropertiesHelper.load(PROPERTIES_PATH);
    PropertiesHelper.verifyProperties(properties, Constants.SOCKET_MODE, Constants.FAILOVER_TCP_PORT_KEY, Constants.FAILOVER_TCP_PEERS_KEY);
  }

  @Test
  public void testVerifyPropertiesDoesntExist() throws Exception {
    Properties properties = PropertiesHelper.load(PROPERTIES_PATH);
    try {
      PropertiesHelper.verifyProperties(properties, "MyMadeUpProperty");
      fail("Should fail, required property does not exist");
    } catch (Exception ex) {
      // expected
    }
  }

}
