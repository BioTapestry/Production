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
import java.util.Vector;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.xml.sax.Attributes;

/****************************************************************************
**
** Contains drawing style info for a full link
*/

public class PerLinkDrawStyle implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /**
  *** Possible Extents:
  **/
  
  public static final int EXTENT_UNDEFINED = -2;
  public static final int EXTENT_VARIOUS   = -1;
   
  public static final int UNIQUE           = 0;
  public static final int SHARED_CONGRUENT = 1;  
  public static final int TO_ROOT          = 2;
  private static final int NUM_EXTENTS_    = 3;  
  
  public static final String UNIQUE_TAG           = "unique";
  public static final String SHARED_CONGRUENT_TAG = "sharedCongruent";
  public static final String TO_ROOT_TAG          = "toRoot";  
        
  ////////////////////////////////////////////////////////////////////////////
  //
  //  INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private SuggestedDrawStyle style_;
  private int extent_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PerLinkDrawStyle(SuggestedDrawStyle style, int extent) {
    style_ = style;
    extent_ = extent;
  }
    
  /***************************************************************************
  **
  ** XML-based Constructor
  */

  public PerLinkDrawStyle(String extentStr) throws IOException {
    try {
      extent_ = mapExtentTag(extentStr); 
    } catch (IllegalArgumentException iaex) {
      throw new IOException();
    }
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PerLinkDrawStyle clone() {
    try {
      PerLinkDrawStyle retval = (PerLinkDrawStyle)super.clone();
      retval.style_ = style_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Add the core draw style
  */
  
  public void setDrawStyle(SuggestedDrawStyle style) {
    style_ = style;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get the core draw style
  */
  
  public SuggestedDrawStyle getDrawStyle() {
    return (style_);
  }  
   
  /***************************************************************************
  **
  ** Get the extent
  */
  
  public int getExtent() {
    return (extent_);
  }
  
  /***************************************************************************
  **
  ** Set the extent
  */
  
  public void setExtent(int extent) {
    extent_ = extent;
    return;
  }
 
  /***************************************************************************
  **
  ** Get a display string for the UI
  */
  
  public String getDisplayString(ResourceManager rMan, ColorResolver cRes) {
    String extentName = rMan.getString("perLinkDrawStyle." + mapExtent(extent_));
    return (style_.getDisplayString(rMan, cRes) + "--" + extentName);
  }  
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<perLinkDrawStyle extent=\"");
    out.print(mapExtent(extent_));    
    out.println("\" >");
    ind.up();
    style_.writeXML(out, ind);
    ind.down().indent();
    out.println("</perLinkDrawStyle>");
    return;
  }
  
  /***************************************************************************
  **
  ** Remap the color tags
  */
  
  public void mapColorTags(Map<String, String> ctm) {
    style_.mapColorTags(ctm);
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** map the extent
  */
  
  public static String mapExtent(int extent) {
    switch (extent) {
      case UNIQUE:
        return (UNIQUE_TAG);
      case SHARED_CONGRUENT:
        return (SHARED_CONGRUENT_TAG);        
      case TO_ROOT:
        return (TO_ROOT_TAG);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the extent
  */
  
  public static int mapExtentTag(String extentTag) {  
    if (extentTag.equalsIgnoreCase(UNIQUE_TAG)) {
      return (UNIQUE);
    } else if (extentTag.equalsIgnoreCase(SHARED_CONGRUENT_TAG)) {
      return (SHARED_CONGRUENT);      
    } else if (extentTag.equalsIgnoreCase(TO_ROOT_TAG)) {
      return (TO_ROOT);
    } else {
      throw new IllegalArgumentException();
    }  
  }
  
  /***************************************************************************
  **
  ** Return possible extent values
  */
  
  public static Vector<ChoiceContent> getExtentChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_EXTENTS_; i++) {
      retval.add(extentForCombo(dacx, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent extentForCombo(DataAccessContext dacx, int extent) {
    return (new ChoiceContent(dacx.getRMan().getString("perLinkDrawStyle." + mapExtent(extent)), extent));
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
      
  public static class PerLinkDrawStyleWorker extends AbstractFactoryClient {
    
    public PerLinkDrawStyleWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("perLinkDrawStyle");
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("perLinkDrawStyle")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.perLinkSty = buildFromXML(elemName, attrs);
        retval = board.perLinkSty;
      }
      return (retval);     
    }
    
    private PerLinkDrawStyle buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String extentStr = AttributeExtractor.extractAttribute(elemName, attrs, "perLinkDrawStyle", "extent", true);
      return (new PerLinkDrawStyle(extentStr));
    }
  }
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      PerLinkDrawStyle plds = board.perLinkSty;
      SuggestedDrawStyle suggSty = board.suggSty;
      plds.setDrawStyle(suggSty);
      return (null);
    }
  }   
}
