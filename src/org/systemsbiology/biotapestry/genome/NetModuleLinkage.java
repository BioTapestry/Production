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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.ChoiceContent;

/****************************************************************************
**
** This represents an linkage between two network modules
*/

public class NetModuleLinkage implements Cloneable {

 ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final int NEGATIVE = -1;
  public static final int NONE     =  0;
  public static final int POSITIVE =  1;
  //private static final int NUM_SIGNS_ = 3; 
  public static final int DEFAULT_SIGN_INDEX = 2;  
  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final String NEGATIVE_STR = "negative";
  public static final String NONE_STR     = "neutral";
  public static final String POSITIVE_STR = "positive";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private int sign_;
  private String srcModuleID_;
  private String trgModuleID_;
  private DataAccessContext dacx_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NetModuleLinkage(NetModuleLinkage other) {
    this.dacx_ = other.dacx_;
    this.id_ = other.id_;
    this.sign_ = other.sign_;
    this.srcModuleID_ = other.srcModuleID_;
    this.trgModuleID_ = other.trgModuleID_;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID
  */

  public NetModuleLinkage(NetModuleLinkage other, String newID) {
    this.dacx_ = other.dacx_;
    this.id_ = newID;
    this.sign_ = other.sign_;
    this.srcModuleID_ = other.srcModuleID_;
    this.trgModuleID_ = other.trgModuleID_;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID, and maps for moduleIDs
  */

  public NetModuleLinkage(NetModuleLinkage other, String newID, Map<String, String> moduleMap) {
    this.dacx_ = other.dacx_;
    this.id_ = newID;
    this.sign_ = other.sign_;
    this.srcModuleID_ = moduleMap.get(other.srcModuleID_);
    this.trgModuleID_ = moduleMap.get(other.trgModuleID_);
    if ((this.srcModuleID_ == null) || (this.trgModuleID_ == null)) {
      throw new IllegalStateException();
    }
  }    
 
  /***************************************************************************
  **
  ** Make an new module linkage
  */

  public NetModuleLinkage(DataAccessContext dacx, String id, String srcModuleID, String trgModuleID, int sign) {
    dacx_ = dacx;
    id_ = id;
    sign_ = sign;
    srcModuleID_ = srcModuleID;
    trgModuleID_ = trgModuleID;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  public NetModuleLinkage clone() { 
    try {
      NetModuleLinkage retval = (NetModuleLinkage)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
 
  /***************************************************************************
  **
  ** Get the id
  ** 
  */
  
  public String getID() {
    return (id_);
  }
  
  
  /***************************************************************************
  **
  ** Get the source module ID
  ** 
  */
  
  public String getSource() {
    return (srcModuleID_);
  }
  
  /***************************************************************************
  **
  ** Get the target module ID
  ** 
  */
  
  public String getTarget() {
    return (trgModuleID_);
  }
  
  /***************************************************************************
  **
  ** Get the sign
  ** 
  */
  
  public int getSign() {
    return (sign_);
  }  
  
  /***************************************************************************
  **
  ** Set the sign
  ** 
  */
  
  public void setSign(int sign) {
    sign_ = sign;
    return;
  }  
     
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("NetModuleLinkage: id = " + id_ + " sign = " + sign_ + " src = " + srcModuleID_ + " trg = " + trgModuleID_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();   
    out.print("<netModuleLink id=\""); 
    out.print(id_);
    out.print("\" sign=\"");
    out.print(mapToSignTag(sign_));
    out.print("\" src=\"");    
    out.print(srcModuleID_);
    out.print("\" trg=\"");
    out.print(trgModuleID_);
    out.println("\" />");          
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
      
  public static class NetModuleLinkageWorker extends AbstractFactoryClient {
     
    private DataAccessContext dacx_;
    
    public NetModuleLinkageWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      dacx_ = null;
      myKeys_.add("netModuleLink");
    }

    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("netModuleLink")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netModLink = buildFromXML(elemName, attrs);
        retval = board.netModLink;
      }
      return (retval);
    }
     
    private NetModuleLinkage buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "netModuleLink", "id", true);
      String src = AttributeExtractor.extractAttribute(elemName, attrs, "netModuleLink", "src", true);
      String signTag = AttributeExtractor.extractAttribute(elemName, attrs, "netModuleLink", "sign", true);      
      String trg = AttributeExtractor.extractAttribute(elemName, attrs, "netModuleLink", "trg", true);
      int sign;
      try {
        sign = mapFromSignTag(signTag);
      } catch (IllegalArgumentException ex) {
        throw new IOException();
      }
      return (new NetModuleLinkage(dacx_, id, src, trg, sign));
    }
  } 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return possible sign choices values
  */
  
  public static Vector<ChoiceContent> getLinkSigns(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = NEGATIVE; i <= POSITIVE; i++) {
      retval.add(signForCombo(dacx, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent signForCombo(DataAccessContext dacx, int sign) {
    return (new ChoiceContent(mapSignToDisplay(dacx, sign), sign));
  }  

  /***************************************************************************
  **
  ** Map node types
  */

  public static String mapSignToDisplay(DataAccessContext dacx, int sign) {
    String signTag = mapToSignTag(sign);
    return (dacx.getRMan().getString("nModLink." + signTag));
  }  

  /***************************************************************************
  **
  ** Map signs to sign tags
  */

  public static String mapToSignTag(int val) {
    switch (val) {
      case NEGATIVE:
        return (NEGATIVE_STR);
      case NONE:
        return (NONE_STR);
      case POSITIVE:
        return (POSITIVE_STR);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map sign tags to signs
  */

  public static int mapFromSignTag(String tag) {
    if (tag.equals(NEGATIVE_STR)) {
      return (NEGATIVE);
    } else if (tag.equals(NONE_STR)) {
      return (NONE);
    } else if (tag.equals(POSITIVE_STR)) {
      return (POSITIVE);
    } else {
      throw new IllegalArgumentException();
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor  Do not use
  */

  @SuppressWarnings("unused")
  private NetModuleLinkage() {
  }  
    
}
