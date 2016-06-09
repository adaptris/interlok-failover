package com.adaptris.failover;

import com.adaptris.core.CoreException;
import com.adaptris.core.DefaultEventHandler;
import com.adaptris.core.Event;
import com.adaptris.core.event.AdapterStopEvent;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("failover-event-handler")
public class FailoverEventHandler extends DefaultEventHandler {
    
  public FailoverEventHandler() throws CoreException {
    super();
  }
  
  @Override
  public void send(Event evt) throws CoreException {
    super.send(evt);
    
    if(evt instanceof AdapterStopEvent)
      AdapterEventListener.getInstance().adapterStopEvent();
  }

}
