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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This represents a node in BioTapestry
*/

public class DBNode extends DBGenomeItem implements Node {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String BARE_TAG_      = "bare";
  private static final String BOX_TAG_       = "box";
  private static final String BUBBLE_TAG_    = "bubble";
  private static final String INTERCELL_TAG_ = "intercel";
  private static final String SLASH_TAG_     = "slash";
  private static final String DIAMOND_TAG_   = "diamond";  
  public static final String GENE_TAG = "gene";  // FIX THIS...
  
  private static final int MAX_TEXT_PAD_COUNT_ = 100;
  private static final int TEXT_PAD_INCREMENT_ = 4;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected int nodeType_;
  protected DBInternalLogic logic_;
  protected String description_;
  protected int padCount_;
  protected ArrayList<String> urls_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public DBNode(DBNode other) {
    super(other);
    this.nodeType_ = other.nodeType_;
    this.description_ = other.description_;
    this.logic_ = new DBInternalLogic(other.logic_);
    this.padCount_ = other.padCount_;
    this.urls_ = new ArrayList<String>(other.urls_);
  }      

  /***************************************************************************
  **
  ** Name and id for UI construction
  */

  public DBNode(DataAccessContext dacx, int nodeType, String name, String id) {
    super(dacx, name, id);
    nodeType_ = nodeType;
    logic_ = new DBInternalLogic();
    padCount_ = getDefaultPadCount(nodeType);
    description_ = null;
    urls_ = new ArrayList<String>();
  }  

  /***************************************************************************
  **
  ** Name and id: for XML based construction
  */

  public DBNode(DataAccessContext dacx, String elemName, String name, String id, String size) throws IOException {
    super(dacx, name, id);
    if (elemName == null) {
      throw new IOException();
    }
    nodeType_ = mapFromTag(elemName);
    logic_ = new DBInternalLogic();
    padCount_ = getDefaultPadCount(nodeType_);
    if (size != null) {
      try {
        padCount_ = Integer.parseInt(size);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    urls_ = new ArrayList<String>();
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
  public DBNode clone() {
    DBNode retval = (DBNode)super.clone();
    retval.logic_ = this.logic_.clone();
    retval.urls_ = new ArrayList<String>(this.urls_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get node type
  */

  public int getNodeType() {
    return (nodeType_);
  }

  /***************************************************************************
  **
  ** Set node type
  */

  public void setNodeType(int type) {
    nodeType_ = type;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the root name
  */

  public String getRootName() {
    return (name_);
  } 
  
  /***************************************************************************
  **
  ** Set the internal logic
  */

  public void setInternalLogic(DBInternalLogic logic) {
    logic_ = logic;
    return;
  }  

  /***************************************************************************
  **
  ** Get the internal logic
  */

  public DBInternalLogic getInternalLogic() {
    return (logic_);
  }
  
  /***************************************************************************
  **
  ** Get the pad count
  */
  
  public int getPadCount() {
    return (padCount_);  
  }
  
  /***************************************************************************
  **
  ** Set the pad count
  */
  
  public void setPadCount(int padCount) {
    padCount_ = padCount;
    return;
  }      
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
     return ("DBNode: name = " + name_ + " id = " + id_ + " type = " + getTag());
  }

  /***************************************************************************
  **
  ** Get the url iterator
  **
  */
  
  public Iterator<String> getURLs() {
    return (urls_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the url count
  **
  */
  
  public int getURLCount() {
    return (urls_.size());
  }      
  
  /***************************************************************************
  **
  ** Add a URL (I/O usage)
  **
  */
  
  public void addUrl(String url) {
    urls_.add(url);
    return;
  }    
  
  /***************************************************************************
  **
  ** Set all URLs
  **
  */
  
  public void setAllUrls(List<String> urls) {
    urls_.clear();
    urls_.addAll(urls);
    return;
  }      
  
  /***************************************************************************
  **
  ** Get the description
  **
  */
  
  public String getDescription() {
    return (description_);
  }
  
  /***************************************************************************
  **
  ** Set the description
  **
  */
  
  public void setDescription(String description) {
    description_ = description;    
    return;
  }  

  /***************************************************************************
  **
  ** Append to the description
  **
  */
  
  public void appendDescription(String description) {
    if (description_ == null) {
      description_ = description;
    } else {
      description_ = description_.concat(description);
    }
    description_ = CharacterEntityMapper.unmapEntities(description_, false);    
    return;
  }
  
  /***************************************************************************
  **
  ** Get display string.  Always ignore type preface
  */

  public String getDisplayString(Genome genome, boolean typePreface) {
    ResourceManager rMan = dacx_.getRMan();
    String format = rMan.getString("ncreate.importFormat");
    String typeDisplay = mapTypeToDisplay(rMan, nodeType_);
    String nodeMsg = MessageFormat.format(format, new Object[] {typeDisplay, name_});    
    return (nodeMsg);
  }   
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(getTag());
    out.print(" name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" id=\"");
    out.print(id_);
    if (padCount_ != getDefaultPadCount(nodeType_)) {
      out.print("\" size=\"");
      out.print(padCount_);    
    } 
    int numURL = urls_.size();
      
    if ((logic_ == null) && (description_ == null) && (numURL == 0)) {
      out.println("\" />");
      return;
    }    
    out.println("\" >"); 
    
    writeLogicDescUrlToXML(out, ind);
 
    ind.indent(); 
    out.print("</");   
    out.print(getTag());   
    out.println(">");
    return;
  }
  
  /***************************************************************************
  **
  ** support
  **
  */
  
  protected void writeLogicDescUrlToXML(PrintWriter out, Indenter ind) {   
    (new CommonGenomeItemCode()).writeDescUrlToXML(out, ind, urls_, description_, "node");
    if (logic_ != null) {
      ind.up();    
      logic_.writeXML(out, ind);
      ind.down(); 
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Check for extra pads:
  */
  
  public boolean haveExtraPads() {  
    return (padCount_ > DBNode.getDefaultPadCount(nodeType_));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Node creation
  **
  */
  
  public static DBNode buildFromXML(DataAccessContext dacx, String elemName,
                                    Attributes attrs) throws IOException {

    String name = null;
    String id = null;
    String size = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("id")) {
          id = val;
        } else if (key.equals("size")) {
          size = val;
        }
      }
    }
    return (new DBNode(dacx, elemName, name, id, size));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(BARE_TAG_);
    retval.add(BOX_TAG_);
    retval.add(BUBBLE_TAG_);
    retval.add(INTERCELL_TAG_);
    retval.add(SLASH_TAG_);
    retval.add(DIAMOND_TAG_);    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the description keyword
  **
  */
  
  public static String descriptionKeyword() {
    return ("node" + CommonGenomeItemCode.FT_TAG_ROOT);
  }
  
  /***************************************************************************
  **
  ** Return the URL keyword
  **
  */
  
  public static String urlKeyword() {
    return ("node" + CommonGenomeItemCode.URL_TAG_ROOT);
  }  
  
  /***************************************************************************
  **
  ** Return possible type choices
  */
  
  public static Vector<ChoiceContent> getTypeChoices(ResourceManager rMan, boolean haveGene) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    retval.add(typeForCombo(rMan, BARE));
    retval.add(typeForCombo(rMan, BOX)); 
    retval.add(typeForCombo(rMan, BUBBLE));
    if (haveGene) {
      retval.add(typeForCombo(rMan, GENE));
    }
    retval.add(typeForCombo(rMan, INTERCELL));
    retval.add(typeForCombo(rMan, SLASH));
    retval.add(typeForCombo(rMan, DIAMOND));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return possible type choices
  */
  
  public static Vector<ChoiceContent> getTypeChoices(ResourceManager rMan, int skipType) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    if (skipType != BARE) {
      retval.add(typeForCombo(rMan, BARE));
    }
    if (skipType != BOX) {
      retval.add(typeForCombo(rMan, BOX));
    }
    if (skipType != BUBBLE) {
      retval.add(typeForCombo(rMan, BUBBLE));
    }
    if (skipType != GENE) {
      retval.add(typeForCombo(rMan, GENE));
    }
    if (skipType != INTERCELL) {
      retval.add(typeForCombo(rMan, INTERCELL));
    }
    if (skipType != SLASH) {
      retval.add(typeForCombo(rMan, SLASH));
    }
    if (skipType != DIAMOND) {
      retval.add(typeForCombo(rMan, DIAMOND));
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent typeForCombo(ResourceManager rMan, int type) {
    return (new ChoiceContent(mapTypeToDisplay(rMan, type), type));
  }  

  /***************************************************************************
  **
  ** Map node types
  */

  public static String mapTypeToDisplay(ResourceManager rMan, int type) {
    String typeTag = mapToTag(type);
    return (rMan.getString("nprop." + typeTag));
  }  
   
  /***************************************************************************
  **
  ** Map tags to values
  */
  
  public static int mapFromTag(String tag) {
    if (tag.equals(BARE_TAG_)) {
      return (BARE);
    } else if (tag.equals(BOX_TAG_)) {
      return (BOX);
    } else if (tag.equals(BUBBLE_TAG_)) {
      return (BUBBLE);
    } else if (tag.equals(INTERCELL_TAG_)) {
      return (INTERCELL);
    } else if (tag.equals(SLASH_TAG_)) {
      return (SLASH);
    } else if (tag.equals(DIAMOND_TAG_)) {
      return (DIAMOND);      
    } else if (tag.equals(GENE_TAG)) {
      return (GENE);      
    } 
    throw new IllegalArgumentException();
  }

  /***************************************************************************
  **
  ** Map tags to values
  */
  
  public static String mapToTag(int val) {
    switch (val) {
      case BARE:
        return (BARE_TAG_);
      case BOX:
        return (BOX_TAG_);
      case BUBBLE:
        return (BUBBLE_TAG_);
      case INTERCELL:
        return (INTERCELL_TAG_);
      case SLASH:
        return (SLASH_TAG_);
      case DIAMOND:
        return (DIAMOND_TAG_);      
      case GENE:
        return (GENE_TAG);              
      default:
        throw new IllegalArgumentException();
    }
  } 
  
 /***************************************************************************
  **
  ** Answer if we only provide the pad count option if forced to do so.
  */

  public static boolean onlyOfferForcedPadCount(int nodeType) {
    return (nodeType == SLASH);
  }   
  
 /***************************************************************************
  **
  ** Get the default pad count
  */

  public static int getDefaultPadCount(int nodeType) {
    switch (nodeType) {
      case BARE:
      case BOX:
      case BUBBLE:
      case DIAMOND:
        return (4);
      case INTERCELL:
      case SLASH:
        return (1);  // Only one landing pad
      case GENE:
        return (DBGene.DEFAULT_PAD_COUNT);        
      default:
        throw new IllegalArgumentException();
    }
  }  
  
  /***************************************************************************
  **
  ** Get the max pad count
  */

  public static int getMaxPadCount(int nodeType) {
    switch (nodeType) {
      case BARE:
      case BOX:
      case BUBBLE:
      case DIAMOND:        
        return (MAX_TEXT_PAD_COUNT_);
      case INTERCELL:
      case SLASH:
        return (11);
      case GENE:    
        return (DBGene.MAX_PAD_COUNT);        
      default:
        throw new IllegalArgumentException();
    }
  }    
  
  /***************************************************************************
  **
  ** Get the pad increment
  */

  public static int getPadIncrement(int nodeType) {
    switch (nodeType) {
      case BARE:
      case BOX:
      case BUBBLE:
      case DIAMOND:        
        return (TEXT_PAD_INCREMENT_);
      case INTERCELL:
      case SLASH:
        return (2);
      case GENE:       
        return (1);        
      default:
        throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Used for node type conversions.  Map pad counts.  
  */  
  
  public static int mapPadCount(int newNodeType, int oldPadCount) {

    //
    // If we don't support extra pads, we are done:
    //
    
    int newPadInc = getPadIncrement(newNodeType);
    int newPadDefault = getDefaultPadCount(newNodeType);
    if (newPadInc == 0) {
      return (newPadDefault);
    }
    
    //
    // If the old pad count is less than the new default, we just go to the new default:
    //
    
    if (oldPadCount <= newPadDefault) {
      return (newPadDefault);
    }
    
    //
    // We need extra pads.  Not all pad counts are valid; it depends on the
    // start and the increment.  We need to find the closest 
    //
    
    int currVal = newPadDefault;
    while (currVal < oldPadCount) {
      currVal += newPadInc;
    }
    return (currVal);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get XML tag
  */

  private String getTag() {
    switch (nodeType_) {
      case BARE:
        return (BARE_TAG_);
      case BOX:
        return (BOX_TAG_);
      case BUBBLE:
        return (BUBBLE_TAG_);
      case INTERCELL:
        return (INTERCELL_TAG_);
      case SLASH:
        return (SLASH_TAG_);
      case DIAMOND:
        return (DIAMOND_TAG_);        
      case GENE:
        return (GENE_TAG);               
      default:
        throw new IllegalStateException();
    }
  }
}
