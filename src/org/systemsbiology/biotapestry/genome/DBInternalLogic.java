/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** This represents the internal logic of a gene or node
*/

public class DBInternalLogic implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 

  private HashMap<String, InternalFunction> nodes_;
  private HashMap<String, InternalLink> links_;
  private int funcType_;
  private UniqueLabeller labels_;
  private HashMap<String, String> params_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String AND_TAG_ = "AND";
  private static final String OR_TAG_ = "OR";
  private static final String XOR_TAG_ = "XOR";
  private static final String CUSTOM_TAG_ = "CUSTOM";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int AND_FUNCTION     = 0;
  public static final int OR_FUNCTION      = 1;
  public static final int XOR_FUNCTION     = 2;
  public static final int CUSTOM_FUNCTION  = 3;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public DBInternalLogic(DBInternalLogic other) {
    copyGuts(other);
  }  

  /***************************************************************************
  **
  ** For UI-based creation
  */

  public DBInternalLogic() {
    funcType_ = AND_FUNCTION;
    nodes_ = new HashMap<String, InternalFunction>();
    links_ = new HashMap<String, InternalLink>();
    params_ = new HashMap<String, String>();
    labels_ = new UniqueLabeller();    
  }
  
  /***************************************************************************
  **
  ** For XML-based creation
  */

  public DBInternalLogic(String function) throws IOException {
    if (function == null) {
      throw new IOException();
    }
    
    if (function.equals(AND_TAG_)) {
      funcType_ = AND_FUNCTION;
    } else if (function.equals(OR_TAG_)) {
      funcType_ = OR_FUNCTION;
    } else if (function.equals(XOR_TAG_)) {
      funcType_ = XOR_FUNCTION;      
    } else if (function.equals(CUSTOM_TAG_)) {
      funcType_ = CUSTOM_FUNCTION;      
    } else {
      throw new IOException();
    }
    
    nodes_ = new HashMap<String, InternalFunction>();
    links_ = new HashMap<String, InternalLink>();
    params_ = new HashMap<String, String>();
    labels_ = new UniqueLabeller();    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public DBInternalLogic clone() {
    try {
      DBInternalLogic retval = (DBInternalLogic)super.clone();
      retval.copyGuts(this);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
   
  /***************************************************************************
  **
  ** Get the function type
  */
  
  public int getFunctionType() {
    return (funcType_);
  }
  
  /***************************************************************************
  **
  ** Set the function type
  */
  
  public void setFunctionType(int type) {
    funcType_ = type;
    return;
  }

  /***************************************************************************
  **
  ** Get an iterator over the function nodes
  */
  
  public Iterator<InternalFunction> getFunctionNodes() {
    return (nodes_.values().iterator());
  }

  /***************************************************************************
  **
  ** Get an iterator over the function links
  */
  
  public Iterator<InternalLink> getFunctionLinks() {
    return (links_.values().iterator());
  }  

  /***************************************************************************
  **
  ** Get an iterator over the param keys
  */
  
  public Iterator<String> getParameterKeys() {
    return (params_.keySet().iterator());
  }  

  /***************************************************************************
  **
  ** Get the number of parameters
  */
  
  public int getParameterCount() {
    return (params_.size());
  }
  
  /***************************************************************************
  **
  ** Add an internal function node
  */
  
  public void addFunctionNodeWithExistingLabel(InternalFunction ifunc) {
    String id = ifunc.getID();
    if (nodes_.get(id) != null) {
      System.err.println("got an existing function node");
      throw new IllegalArgumentException();
    }
    nodes_.put(id, ifunc);
    return;
  }    

  /***************************************************************************
  **
  ** Add an internal function link
  */
  
  public void addFunctionLinkWithExistingLabel(InternalLink iLink) {
    String id = iLink.getID();
    if (links_.get(id) != null) {
      System.err.println("got an existing link");
      throw new IllegalArgumentException();
    }
    links_.put(id, iLink);
    return;
  }

  /***************************************************************************
  **
  ** Add a simulation parameter
  **
  */
  
  public void addSimParam(String elemName, 
                          Attributes attrs) throws IOException {
    String name = AttributeExtractor.extractAttribute(
                         elemName, attrs, "simParameter", "name", true);
    String value = AttributeExtractor.extractAttribute(
                         elemName, attrs, "simParameter", "value", true);
    params_.put(name, value);
    return;
  }

  /***************************************************************************
  **
  ** Get a simulation parameter.  If we do not have a local value, just use the
  ** default.
  */
  public String getSimulationParam(DataAccessContext dacx, String name, int nodeType) {
    String retval = params_.get(name);
    if (retval == null) {
      String root = SbmlSupport.extractRootIdFromParam(name);
      retval = dacx.getSimParamSource().getSimulationDefaultValue(root, nodeType, funcType_);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Set a simulation parameter
  **
  */
  
  public void setSimulationParam(String key, String value) {
    params_.put(key, value);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get the next key
  */

  public String getNextKey() {
    return (labels_.getNextLabel());
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("DBInternalLogic: funcType = " + funcType_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<simulationLogic");
    out.print(" type=\"");
    out.print(getTag());
    int pSize = params_.size();
    if ((funcType_ != CUSTOM_FUNCTION) && (pSize == 0)) {
      out.println("\" />");
      return;
    }  
    out.println("\" >");
    if (pSize != 0) {
      ind.up().indent();
      out.println("<simParameters>");
      ind.up();
      Iterator<String> psit = params_.keySet().iterator();
      while (psit.hasNext()) {
        String name = psit.next();
        String value = params_.get(name);
        ind.indent();
        out.print("<simParameter name=\"");
        out.print(name);
        out.print("\" value=\"");        
        out.print(value);
        out.println("\" />");        
      }
      ind.down().indent();
      out.println("</simParameters>");
      ind.down();
    }
    if (funcType_ == CUSTOM_FUNCTION) {
      ind.up().indent();
      out.println("<customNetwork>");
      ind.up();      
      Iterator<InternalFunction> nodes = getFunctionNodes();
      while (nodes.hasNext()) {
        InternalFunction ifunc = nodes.next();
        ifunc.writeXML(out, ind);
      }
      Iterator<InternalLink> links = getFunctionLinks();
      while (links.hasNext()) {
        InternalLink iLink = links.next();
        iLink.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</customNetwork>");
      ind.down();
    }    
    ind.indent();    
    out.println("</simulationLogic>");
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle region creation
  **
  */
  
  public static DBInternalLogic buildFromXML(String elemName, Attributes attrs) throws IOException {
    if (!elemName.equals("simulationLogic")) {
      return (null);
    }                                           
    String type = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("type")) {
          type = val;
        }
      }
    }
    return (new DBInternalLogic(type));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("simulationLogic");
    retval.add("simParameters");
    retval.add("customNetwork");    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static String getSimParamKeyword() {
    return ("simParameter");
  }
  
  /***************************************************************************
  **
  ** Return possible logic values
  **
  */
    
  public static Map<Integer, String> getLogicValues() {
    HashMap<Integer, String> retval = new HashMap<Integer, String>();
    retval.put(new Integer(AND_FUNCTION), AND_TAG_);
    retval.put(new Integer(OR_FUNCTION), OR_TAG_);
    retval.put(new Integer(XOR_FUNCTION), XOR_TAG_);
   // retval.put(new Integer(CUSTOM_FUNCTION), CUSTOM_TAG_);    
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  /***************************************************************************
  **
  ** Copy Constructor
  */

  private void copyGuts(DBInternalLogic other) {
    this.funcType_ = other.funcType_;
    
    this.nodes_ = new HashMap<String, InternalFunction>();    
    Iterator<InternalFunction> nit = other.nodes_.values().iterator();
    while (nit.hasNext()) {
      InternalFunction ifunc = nit.next();
      InternalFunction newFunc = ifunc.clone();
      this.nodes_.put(newFunc.getID(), newFunc);
    }
    
    this.links_ = new HashMap<String, InternalLink>();
    Iterator<InternalLink> lit = other.links_.values().iterator();
    while (lit.hasNext()) {
      InternalLink ilink = lit.next();
      InternalLink newLink = ilink.clone();
      this.links_.put(newLink.getID(), newLink);
    }
    
    this.params_ = new HashMap<String, String>();
    Iterator<String> pit = other.params_.keySet().iterator();
    while (pit.hasNext()) {
      String pkey = pit.next();
      String value = other.params_.get(pkey);
      this.params_.put(new String(pkey), new String(value));
    }  
    this.labels_ = other.labels_.clone();    
  }
  
  /***************************************************************************
  **
  ** Get type tag
  */

  private String getTag() {
    switch (funcType_) {
      case AND_FUNCTION:
        return (AND_TAG_);
      case OR_FUNCTION:
        return (OR_TAG_);
      case XOR_FUNCTION:
        return (XOR_TAG_);        
      case CUSTOM_FUNCTION:
        return (CUSTOM_TAG_);        
      default:
        throw new IllegalStateException();
    }
  }  
}
