/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.cmd.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Interfaces for "question bombs"  (Question/Response to and from server that
** is out-of-band from the control flow)
*/

public class RemoteRequest {
  
  private String label_;
  private Map<String, Object> args_;
  
  public enum Progress {STOP, CONTINUE, DONE};
  
  public RemoteRequest(String label) {
    label_ = label;
    args_ = new HashMap<String, Object>();
  }
  
  public RemoteRequest(String label, RemoteRequest daBomb) {
    label_ = label;
    if (daBomb == null) {
      args_ = new HashMap<String, Object>();
    } else {
      args_ = daBomb.args_;  // SAME MAP!
    }
  }
  
  public String getLabel() {
    return (label_);
  } 
  
  public void setBooleanArg(String key, boolean arg) {
    args_.put(key, Boolean.valueOf(arg));
    return;
  } 
  
  public void setStringArg(String key, String arg) {
    args_.put(key, arg);
    return;
  }
  
  public String getStringArg(String key) {
    return ((String)args_.get(key));
  } 
  
  public boolean getBooleanArg(String key) {
    return (((Boolean)args_.get(key)).booleanValue());
  }
  
  public void setObjectArg(String key, Object arg) {
    args_.put(key, arg);
    return;
  }
  
  public Object getObjectArg(String key) {
    return (args_.get(key));
  } 
  
  public static class Result {
    
    private SimpleUserFeedback suf_;
    private Map<String, Object> answers_;
    private Progress keepGoing_;
   
    public Result(RemoteRequest daBomb) {
      suf_ = null;
      answers_ = daBomb.args_;  // SAME MAP!
      keepGoing_ = Progress.CONTINUE;
    }
    
    public Result(SimpleUserFeedback suf, Map<String, Object> answers, Progress keepGoing) {
      suf_ = suf;
      answers_ = answers;
      keepGoing_ = keepGoing;
    }
      
    public SimpleUserFeedback getSimpleUserFeedback() {
      return (suf_);
    }
     
    public void setSimpleUserFeedback(SimpleUserFeedback suf) {
      suf_ = suf;
      return;
    }
    
    public void setBooleanAnswer(String key, boolean answer) {
      answers_.put(key, Boolean.valueOf(answer));
      return;
    } 
    
    public void setStringAnswer(String key, String answer) {
      answers_.put(key, answer);
      return;
    }
    
    public void setSetAnswer(String key, Set<String> answer) {
      answers_.put(key, answer);
      return;
    }
    
    public void setObjAnswer(String key, Object answer) {
      answers_.put(key, answer);
      return;
    }
    
  
    public String getStringAnswer(String key) {
      return ((String)answers_.get(key));
    } 
    
    @SuppressWarnings("unchecked")
    public Set<String> getSetAnswer(String key) {
      return ((Set<String>)answers_.get(key));
    }      
    
    public boolean getBooleanAnswer(String key) {
      return (((Boolean)answers_.get(key)).booleanValue());
    }
    
    public Object getObjAnswer(String key) {
      return (answers_.get(key));
    }
     
    public void setDirection(Progress prog) {
      keepGoing_ = prog;
      return;
    } 
    
    public Progress keepGoing() {
      return (keepGoing_);
    }   
  };
 
}