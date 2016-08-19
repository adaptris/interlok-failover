package com.adaptris.failover.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.adaptris.core.management.BootstrapProperties;

public class PropertiesHelper {

  public static Properties loadFromFile(String filePath) throws Exception {
    Properties returnProperties = new Properties();    
    InputStream inputStream = null;
    
    File propertiesFile = new File(filePath);
    if (propertiesFile.exists())
      inputStream = new FileInputStream(propertiesFile);
    else 
      inputStream = BootstrapProperties.class.getClassLoader().getResourceAsStream(filePath);
    
    if (inputStream == null)
      throw new IOException("cannot locate resource [" + filePath + "]");
    
    try {
      returnProperties.load(inputStream);
    } finally {
      inputStream.close();
    }

    return returnProperties;
  }
  
  public static void verifyProperties(Properties properties, String... requiredKeys) throws Exception {
    for(String requiredKey : requiredKeys) {
      if(!properties.containsKey(requiredKey))
        throw new Exception("Properties does not contain the required property: " + requiredKey);
    }
  }
  
}
