/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;

/***************************************************************************
**
** Class for representing a key-value pair
*/ 


public class NameValuePair implements Cloneable {
  
  private String name_;
  private String value_;
  
  public NameValuePair() {
	  
  }

  public NameValuePair(String name, String value) {
    if ((name == null) || (value == null)) {
      throw new IllegalArgumentException();
    }
    name_ = name;
    value_ = value;
  }  
  
  // FOR IO ONLY:
  
  NameValuePair(String name) throws IOException {
    if (name == null) {
      throw new IllegalArgumentException();
    }
    name_ = name;
  }
  
  // FOR IO ONLY:
  
  void setValue(String value) throws IOException {
    value_ = value;
    return;
  }  
  
    
  public NameValuePair clone() {
    try {
      NameValuePair newPair = (NameValuePair)super.clone();
      return (newPair);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }    
  }
  
  public int hashCode() {
    return (name_.hashCode() + value_.hashCode());
  }
  
  public String getName() {
    return (name_);
  }
  
  public String getValue() {
    return (value_);
  }  
  
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof NameValuePair)) {
      return (false);
    }
    NameValuePair otherNVP = (NameValuePair)other;
    return ((this.name_.equals(otherNVP.name_)) && (this.value_.equals(otherNVP.value_)));
  }
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();   
    out.print("<nameValPair name=\""); 
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" >");          
    out.print(CharacterEntityMapper.mapEntities(value_, false));
    out.println("</nameValPair>");     
    return;
  }  
  
  public String toString() {
    return ("name: " + name_ + " value: " + value_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NameValPairWorker extends AbstractFactoryClient {
    
    private StringBuffer charBuf_;
    
    public NameValPairWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nameValPair");
      charBuf_ = new StringBuffer();
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nameValPair")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nvPair = buildFromXML(elemName, attrs);
        retval = board.nvPair;
        charBuf_.setLength(0);
      }
      return (retval);
    }

    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("nameValPair")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nvPair.setValue(CharacterEntityMapper.unmapEntities(charBuf_.toString(), false));
      }
      return;
    }

    protected void localProcessCharacters(char[] chars, int start, int length) {
      String nextString = new String(chars, start, length);
      charBuf_.append(nextString);
      return;
    }         
     
    private NameValuePair buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "nameValPair", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new NameValuePair(name));
    }
  } 
}
