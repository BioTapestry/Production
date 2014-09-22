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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;

/****************************************************************************
**
** Abstract base class
*/

public abstract class AbstractOptArgs implements ControlFlow.OptArgs {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final String OPT_ARG_PREF = "cfoa_";
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  protected NameValuePairList pairs_;
  protected String urlArgs_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  protected AbstractOptArgs(Map<String, String> argMap) throws IOException {
    parse(argMap);
    bundle();    
  }

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  protected AbstractOptArgs() {
    pairs_ = new NameValuePairList();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the arguments back
  ** 
  */
     
  public String getURLArgs() {
    return (urlArgs_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get value
  */
  
  protected String getValue(int index) {
    return (pairs_.getPair(getKeys()[index]).getValue());
  }
  
  /***************************************************************************
  **
  ** Drop value
  */
  
  protected boolean dropValue(int index) {
    return (pairs_.dropNameValuePair(getKeys()[index]));
  }
  
  /***************************************************************************
  **
  ** Put value
  */
  
  protected void setValue(int index, String val) {
    String name = getKeys()[index];
    NameValuePair nvp = new NameValuePair(name, val);
    pairs_.addNameValuePair(nvp);
    return;
  }
  
  /***************************************************************************
  **
  ** What keys do we expect?
  */
  
  protected abstract String[] getKeys();  
  
 /***************************************************************************
  **
  ** What classes do we require?
  */
  
  protected Class<?>[] getClasses() {
    return (null);
  }

  /***************************************************************************
  **
  ** parse out!
  ** 
  */
  
  protected void parse(Map<String, String> argMap) throws IOException {   
    pairs_ = new NameValuePairList();
    String[] myKeys = getKeys();
    Class<?>[] checks = getClasses();
    for (int i = 0; i < myKeys.length; i++) {
      String key = myKeys[i];
      String urlKey = OPT_ARG_PREF + key;
      String val = argMap.get(urlKey);
      if (val == null) {
        throw new IOException();
      }
      if ((checks != null) && !checks[i].equals(String.class)) {
        Class<?> checkClass = checks[i];
        if (checkClass.equals(Integer.class)) {
          try {
            Integer.parseInt(val);
          } catch (NumberFormatException nfex) {
            throw new IOException();
          }
        } else if (checkClass.equals(Boolean.class)) {
          if (!(val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))) {
            throw new IOException();
          }
        } else if (checkClass.equals(Double.class)) {
          try {
            Double.parseDouble(val);
          } catch (NumberFormatException nfex) {
            throw new IOException();
          }
        } else {
          throw new IllegalStateException();
        }
      }
      pairs_.addNameValuePair(new NameValuePair(key, val));   
    }
    return;
  }

  /***************************************************************************
  **
  ** bundle up!
  ** 
  */ 
  protected void bundle() { 
    try {
      StringBuffer buf = new StringBuffer();
      Iterator<NameValuePair> nvit = pairs_.getIterator();
      while (nvit.hasNext()) {
        NameValuePair nvp = nvit.next();
        buf.append(OPT_ARG_PREF);
        buf.append(URLEncoder.encode(nvp.getName(), "UTF-8"));
        buf.append("=");
        buf.append(URLEncoder.encode(nvp.getValue(), "UTF-8"));
        if (nvit.hasNext()) {
          buf.append("&");
        }
      }
      urlArgs_ = buf.toString();
    } catch (UnsupportedEncodingException ueex) {
      throw new IllegalStateException();
    }
  } 
}
