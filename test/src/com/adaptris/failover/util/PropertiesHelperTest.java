package com.adaptris.failover.util;

import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class PropertiesHelperTest extends TestCase {
  
  private static final String PROPERTIES_PATH = "./test/resources/bootstrap.properties";
  
  public void setUp() throws Exception {
    
  }
  
  public void tearDown() throws Exception {
    
  }
  
  public void testLoadProperties() throws Exception {
    Properties properties = PropertiesHelper.loadFromFile(PROPERTIES_PATH);
    assertEquals("tcp", properties.getProperty(Constants.SOCKET_MODE));
    assertEquals("4444", properties.getProperty(Constants.FAILOVER_TCP_PORT_KEY));
    assertEquals("localhost:4445;localhost:4446", properties.getProperty(Constants.FAILOVER_TCP_PEERS_KEY));
  }
  
  public void testLoadPropertiesDoesnExist() throws Exception {
    try {
      PropertiesHelper.loadFromFile("IDoNotExist");
      fail("Should fail, properties does not exist.");
    } catch(IOException ex) {
      //expected
    }
  }

  public void testVerifyProperties() throws Exception {
    Properties properties = PropertiesHelper.loadFromFile(PROPERTIES_PATH);
    PropertiesHelper.verifyProperties(properties, Constants.SOCKET_MODE, Constants.FAILOVER_TCP_PORT_KEY, Constants.FAILOVER_TCP_PEERS_KEY);
  }
  
  public void testVerifyPropertiesDoesntExist() throws Exception {
    Properties properties = PropertiesHelper.loadFromFile(PROPERTIES_PATH);
    try {
      PropertiesHelper.verifyProperties(properties, "MyMadeUpProperty");
      fail("Should fail, required property does not exist");
    } catch (Exception ex) {
      //expected
    }
  }
  
}
