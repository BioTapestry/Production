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

package org.systemsbiology.biotapestry.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

import org.xml.sax.Attributes;

public class NameValuePairList implements Cloneable {  
  //
  // Order is important for consistent presentation, so do it as a list.
  // Should be on the small side, so search efficiency is less important...
  //
  private ArrayList<NameValuePair> nameVals_;

  public NameValuePairList() {
    nameVals_ = new ArrayList<NameValuePair>();
  }
  
  @Override
  public NameValuePairList clone() {
    try {
      NameValuePairList newVal = (NameValuePairList)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }    
  }
  
  public void addNameValuePair(NameValuePair nvPair) {
    nameVals_.add(nvPair);
    return;
  }
  
  public boolean dropNameValuePair(String name) {
    int numNV = nameVals_.size();
    for (int i = 0; i < numNV; i++) {
      NameValuePair nvp = nameVals_.get(i);
      if (nvp.getName().equals(name)) {
        nameVals_.remove(i);
        return (true);
      }
    }
    return (false);
  }
  
  
  public Iterator<NameValuePair> getIterator() {
    return (nameVals_.iterator());
  }
  
  public NameValuePair getPair(String name) {
    Iterator<NameValuePair> nvi = nameVals_.iterator();
    while (nvi.hasNext()) {
      NameValuePair nvp = nvi.next();
      if (nvp.getName().equals(name)) {
        return (nvp);
      }
    }
    return (null);
  }

  public int size() {
    return (nameVals_.size());
  }  

  @Override
  public int hashCode() {
    return (nameVals_.hashCode());
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof NameValuePairList)) {
      return (false);
    }
    NameValuePairList otherNVPL = (NameValuePairList)other;
    return (this.nameVals_.equals(otherNVPL.nameVals_));
  }  

  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();   
    out.println("<nameValPairs>");
    Iterator<NameValuePair> nvi = nameVals_.iterator();
    ind.up();
    while (nvi.hasNext()) {
      NameValuePair nvp = nvi.next();
      nvp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</nameValPairs>");
    return;
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
      
  public static class NameValPairListWorker extends AbstractFactoryClient {
    
    public NameValPairListWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nameValPairs");
      installWorker(new NameValuePair.NameValPairWorker(whiteboard), new MyGlue());
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nameValPairs")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.nvPairList = buildFromXML(elemName, attrs);
        retval = board.nvPairList;
      } 
      return (retval);
    }      
     
    private NameValuePairList buildFromXML(String elemName, Attributes attrs) throws IOException {
      return (new NameValuePairList());
    }
  } 
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NameValuePairList currList = board.nvPairList;
      NameValuePair currPair = board.nvPair;
      currList.addNameValuePair(currPair); 
      return (null);
    }
  }
}
