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

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** Contains custom drawing style info for an evidence level
*/

public class CustomEvidenceDrawStyle implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
        
  ////////////////////////////////////////////////////////////////////////////
  //
  //  INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private PerLinkDrawStyle style_;
  private boolean showDiamonds_;
  private int evidenceLevel_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public CustomEvidenceDrawStyle(int level, boolean showDiamonds, PerLinkDrawStyle style) {
    style_ = style;
    showDiamonds_ = showDiamonds;
    evidenceLevel_ = level;
  }
    
  /***************************************************************************
  **
  ** XML-based Constructor
  */

  public CustomEvidenceDrawStyle(String levelString, String diamondString) throws IOException {
    try {
      evidenceLevel_ = Integer.parseInt(levelString); 
    } catch (NumberFormatException nfex) {
      throw new IOException();
    }
    if ((evidenceLevel_ <= Linkage.LEVEL_NONE) || (evidenceLevel_ > Linkage.MAX_LEVEL)) {
      throw new IOException();
    }    
    showDiamonds_ = Boolean.valueOf(diamondString).booleanValue();    
  }

  /***************************************************************************
  **
  ** Clone
  */

  public CustomEvidenceDrawStyle clone() {
    try {
      CustomEvidenceDrawStyle retval = (CustomEvidenceDrawStyle)super.clone();
      retval.style_ = (this.style_ == null) ? null : (PerLinkDrawStyle)this.style_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  
  
  /***************************************************************************
  **
  ** Get the core draw style  May be null.
  */
  
  public PerLinkDrawStyle getDrawStyle() {
    return (style_);
  }  
  
  /***************************************************************************
  **
  ** Set the core draw style
  */
  
  public void setDrawStyle(PerLinkDrawStyle style) {
    style_ = style;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the level
  */
  
  public int getEvidenceLevel() {
    return (evidenceLevel_);
  }
  
  /***************************************************************************
  **
  ** Answer if we show diamonds
  */
  
  public boolean showDiamonds() {
    return (showDiamonds_);
  }  
  
  /***************************************************************************
  **
  ** Set if we show diamonds
  */
  
  public void setShowDiamonds(boolean doDiamonds) {
    showDiamonds_ = doDiamonds;
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<customEvidenceDrawStyle level=\"");
    out.print(evidenceLevel_);    
    out.print("\" diamonds=\"");
    out.print(showDiamonds_);
    if (style_ != null) {
      out.println("\" >");
      ind.up();
      style_.writeXML(out, ind);
      ind.down().indent();
      out.println("</customEvidenceDrawStyle>");
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class CustomEvidenceDrawStyleWorker extends AbstractFactoryClient {
   
    public CustomEvidenceDrawStyleWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("customEvidenceDrawStyle");
      installWorker(new PerLinkDrawStyle.PerLinkDrawStyleWorker(whiteboard), new MyStyleGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("customEvidenceDrawStyle")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.evidenceDrawSty = buildFromXML(elemName, attrs);
        retval = board.evidenceDrawSty;
      }
      return (retval);     
    }
    
    private CustomEvidenceDrawStyle buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String extentStr = AttributeExtractor.extractAttribute(elemName, attrs, "customEvidenceDrawStyle", "level", true);
      String diamondStr = AttributeExtractor.extractAttribute(elemName, attrs, "customEvidenceDrawStyle", "diamonds", true); 
      return (new CustomEvidenceDrawStyle(extentStr, diamondStr));
    }
  }
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      CustomEvidenceDrawStyle plds = board.evidenceDrawSty;
      PerLinkDrawStyle plSty = board.perLinkSty;
      plds.setDrawStyle(plSty);
      return (null);
    }
  }   
}
