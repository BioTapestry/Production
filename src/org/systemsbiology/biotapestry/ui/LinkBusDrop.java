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

package org.systemsbiology.biotapestry.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** Describes terminal connections to link buses
*/

public abstract class LinkBusDrop implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  protected String targetRef_;
  protected String connectTag_;
  protected int connectSense_;
  protected int dropType_;
  protected SuggestedDrawStyle dropDrawStyle_;
  protected PerLinkDrawStyle linkDrawStyle_;
  
  protected static final String XML_TAG = "drop";
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /**
  *** Possible drop type
  **/

  public static final int START_DROP = 0;
  public static final int END_DROP   = 1;
  
  /**
  *** Possible connection ends
  **/

  public static final int CONNECT_TO_START      = 0;
  public static final int CONNECT_TO_END        = 1; 
  public static final int NO_SEGMENT_CONNECTION = 2;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS FOR INHERITANCE
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  protected LinkBusDrop(LinkBusDrop other) {
    if (other.dropDrawStyle_ != null) {
      this.dropDrawStyle_ = other.dropDrawStyle_.clone();
    }
    if (other.linkDrawStyle_ != null) {
      this.linkDrawStyle_ = other.linkDrawStyle_.clone();
    }
    this.targetRef_ = other.targetRef_;
    this.connectTag_ = other.connectTag_;
    this.connectSense_ = other.connectSense_;
    this.dropType_ = other.dropType_;
  }
  
  /***************************************************************************
  **
  ** For Package visible only
  */

  protected LinkBusDrop(String targetRef, 
                        String ourConnection, int dropType, 
                        int connectionEnd) {
    dropType_ = dropType;
    connectTag_ = ourConnection;
    connectSense_ = connectionEnd;
    targetRef_ = targetRef; 
  }      
  
  /***************************************************************************
  **
  ** Handle layout creation
  **
  */
  
  protected LinkBusDrop(Layout layout, Genome genome, String startID, Attributes attrs) throws IOException {
    String targetRef = null;
    String connect = null;
    int connectType = END_DROP;
    int connectEnd = NO_SEGMENT_CONNECTION;
    String specialLine = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("ref")) {
          targetRef = val;
          if (val.equals("*")) {
            targetRef = startID;
            connectType = START_DROP;
          }
        } else if (key.equals("connectStart")) {
          connect = val;
          connectEnd = CONNECT_TO_START;
        } else if (key.equals("connectEnd")) {
          connect = val;
          connectEnd = CONNECT_TO_END; 
          // Legacy IO only now:
        } else if (key.equals("specialLine")) {
          specialLine = val;         
        }
      }
    }
    
    if (targetRef == null) {
      throw new IOException();
    }

    int style = LinkProperties.NONE;

    try {
      if (specialLine != null) {      
        style = LinkProperties.mapFromStyleTag(specialLine);
      }
    } catch (IllegalArgumentException iaex) {
      throw new IOException();
    }

    targetRef_ = (connectType == START_DROP) ? null : targetRef;
    dropType_ = connectType;
    connectTag_ = connect;
    connectSense_ = connectEnd;
    
    //
    // Legacy only; this is now loaded in using a subsequent element...
    //
    
    if (style != LinkProperties.NONE) {
      dropDrawStyle_ = SuggestedDrawStyle.buildFromLegacy(style);
    }    

    //
    // Concrete child classes responsible for setting end_!
    //
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Remap the color tags
  */
  
  public void mapColorTags(Map<String, String> ctm) {

   if (dropDrawStyle_ != null) {
      dropDrawStyle_.mapColorTags(ctm);
   }
   if (linkDrawStyle_ != null) {
     linkDrawStyle_.mapColorTags(ctm);
   }
   return;
  }  
  
 /***************************************************************************
  **
  ** Transfer across special styles
  */

  public void transferSpecialStyles(LinkBusDrop other) {
    if (other.dropDrawStyle_ != null) {
      this.dropDrawStyle_ = other.dropDrawStyle_.clone();
    }
    if (other.linkDrawStyle_ != null) {
      this.linkDrawStyle_ = other.linkDrawStyle_.clone();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public LinkBusDrop clone() {
    try {
      LinkBusDrop retval = (LinkBusDrop)super.clone();
      if (this.dropDrawStyle_ != null) {
        retval.dropDrawStyle_ = this.dropDrawStyle_.clone();
      }
      if (this.linkDrawStyle_ != null) {
        retval.linkDrawStyle_ = this.linkDrawStyle_.clone();
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Use to copy props to new link.
  */

  public boolean mapToNewLink(Map<String, String> linkMap, Map<String, String> nodeMap) {
    if (targetRef_ != null) {
      String newRef = linkMap.get(targetRef_);
      if (newRef != null) {
        targetRef_ = newRef;
        return (true);
      } else {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Set the draw style for the drop
  */
  
  public void setDrawStyleForDrop(SuggestedDrawStyle drawStyle) {
    dropDrawStyle_ = drawStyle;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the draw style for the link
  */
  
  public void setDrawStyleForLink(PerLinkDrawStyle drawStyle) {
    if (dropType_ == START_DROP) {
      throw new IllegalStateException();
    }
    linkDrawStyle_ = drawStyle;
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the draw style for the link
  */
  
  public PerLinkDrawStyle getDrawStyleForLink() {
    if (dropType_ == START_DROP) {
      throw new IllegalStateException();
    }
    return (linkDrawStyle_);
  }  

  /***************************************************************************
  **
  ** Get the bus connection tag
  */
  
  public String getConnectionTag() {
    return (connectTag_);
  }

  /***************************************************************************
  **
  ** Set the bus connection tag
  */
  
  public void setConnectionTag(String connectTag) {
    connectTag_ = connectTag;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the bus connection sense
  */
  
  public int getConnectionSense() {
    return (connectSense_);
  }

  /***************************************************************************
  **
  ** Set the bus connection sense
  */
  
  public void setConnectionSense(int connectSense) {
    connectSense_ = connectSense;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the special style.  May be null
  */
  
  public SuggestedDrawStyle getSpecialDropDrawStyle() {
    return (dropDrawStyle_);
  }
  
  /***************************************************************************
  **
  ** Get the target ref  Will be null if drop type is START_DROP
  */
  
  public String getTargetRef() {
    return (targetRef_);
  }
  
  /***************************************************************************
  **
  ** set the target ref  Will be illegal if drop type is START_DROP
  */
  
  public void setTargetRef(String ref) {
    if (dropType_ == START_DROP) {
      throw new IllegalArgumentException();
    }
    targetRef_ = ref;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the drop type
  */
  
  public int getDropType() {
    return (dropType_);
  } 
  
  /***************************************************************************
  **
  ** Set the drop type
  */
  
  public void setDropType(int dropType) {
    dropType_ = dropType;
    return;
  }  
 
  /***************************************************************************
  **
  ** To string
  */
  
  public String toString() {
    String ddss = (dropDrawStyle_ != null) ? dropDrawStyle_.toString() : "(no Special Style)";
    return (" type = " + dropType_ + " ref = " + targetRef_ + " sense = " +
            connectSense_ + " tag = " + connectTag_ + " drop special = " + ddss);
  }  

  /***************************************************************************
  **
  ** Used in concrete classes
  **
  */
  
  protected void writeExtraXML(PrintWriter out, Indenter ind) {
    return;
  }  
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<");
    out.print(XML_TAG);
    out.print(" ref=\"");
    if (dropType_ == START_DROP) {
      out.print("*");
    } else {
      out.print(targetRef_);
    }
    writeExtraXML(out, ind);
    if (connectSense_ == CONNECT_TO_END) {
      out.print("\" connectEnd=\"");
      out.print(connectTag_);
    } else if (connectSense_ == CONNECT_TO_START) {
      out.print("\" connectStart=\"");
      out.print(connectTag_);
    } // else don't output anything...
    
    if ((dropDrawStyle_ != null) || (linkDrawStyle_ != null)) {
      out.println("\" >");
      ind.up();
      if (dropDrawStyle_ != null) {
        dropDrawStyle_.writeXML(out, ind);
      }
      if (linkDrawStyle_ != null) {
        linkDrawStyle_.writeXML(out, ind);
      }
      ind.down().indent();
      out.print("</");     
      out.print(XML_TAG);
      out.println(">");
    } else {
      out.println("\" />");
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
}
