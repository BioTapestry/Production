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

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.util.ChoiceContent;

/****************************************************************************
**
** Contains the information needed to layout a group
*/

public class GroupProperties {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public final static int TOP    = 0;  
  public final static int BOTTOM = 1;  
  public final static int LEFT   = 2;  
  public final static int RIGHT  = 3;
  
  /**
  *** Possible styles:
  **/

  public static final int AUTOBOUND = 0;
  
  /**
  *** Possible layers in subgroups:
  **/

  public static final int MAX_SUBGROUP_LAYER = 10;  
 
  /**
  *** Label hiding modes:
  **/

  public static final int SHOW_LABEL                = 0;    
  public static final int HIDE_LABEL_ALL_LEVELS     = 1;    
  public static final int HIDE_LABEL_TOP_LEVEL_ONLY = 2;    
  private static final int NUM_LABEL_MODES_         = 3;    
 
  public static final String SHOW_LABEL_TAG                = "showLabel";    
  public static final String HIDE_LABEL_ALL_LEVELS_TAG     = "hideLabel";    
  public static final String HIDE_LABEL_TOP_LEVEL_ONLY_TAG = "hideLabelTopOnly";  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int layer_;
  private int order_;  
  private int style_;  
  private String groupID_;
  private int[] pads_;

  private String colorTag_;
  private String inactiveColorTag_;
  private String styleTag_;
  private Point2D labelLocation_;
  private boolean hideLabel_;
  private boolean hideLabelTopOnly_;
  private ArrayList hideInModels_;
  private GroupFree renderer_;
  private boolean skipRender_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Simple default Constructor
  */

  public GroupProperties(int count, String ref, Point2D groupCenter, int order, ColorResolver colR) {
    this(count, ref, null, groupCenter, order, colR);
  }
  
  /***************************************************************************
  **
  ** Simple default Constructor (deprecated; use above)
  */

  public GroupProperties(int count, String ref, Layout layout, Point2D groupCenter, int order,  ColorResolver colR) {
    this(ref, groupCenter, order, colR.activeColorCycle(count), colR.inactiveColorCycle(count));
  }  
  
  /***************************************************************************
  **
  ** Simple default Constructor with color spec
  */

  public GroupProperties(String ref, Point2D groupCenter, int order, String activeColor, String inactiveColor) {
    colorTag_ = activeColor;
    inactiveColorTag_ = inactiveColor;    
    style_ = AUTOBOUND;
    layer_ = 0;
    order_ = order;
    pads_ = new int[] {30, 30, 30, 30};
    labelLocation_ = (Point2D)groupCenter.clone();
    styleTag_ = "autobound";
    groupID_ = ref;
    hideLabel_ = false;
    hideLabelTopOnly_ = false;
    hideInModels_ = new ArrayList();
    renderer_ = new GroupFree();
    skipRender_ = false;
  }  

  /***************************************************************************
  **
  ** Simple Constructor for subgroups
  */

  public GroupProperties(String ref, Layout layout, int layer, Point2D labelLoc, int order, ColorResolver colR) {
    colorTag_ = colR.distinctActiveColor();
    inactiveColorTag_ = colR.distinctInactiveColor();
    style_ = AUTOBOUND;
    layer_ = layer;
    order_ = order;
    pads_ = new int[] {30, 30, 30, 30};
    labelLocation_ = (Point2D)labelLoc.clone();
    styleTag_ = "autobound";
    groupID_ = ref;
    hideLabel_ = false;
    hideLabelTopOnly_ = false;    
    hideInModels_ = new ArrayList();
    renderer_ = new GroupFree();
    skipRender_ = false;
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public GroupProperties(GroupProperties other) {
    this.renderer_ = other.renderer_;
    this.groupID_ = other.groupID_;
    this.layer_ = other.layer_;
    this.order_ = other.order_;
    this.style_ = other.style_;  
    this.pads_ = new int[4];
    System.arraycopy(other.pads_, 0, this.pads_, 0, 4);
    this.colorTag_ = other.colorTag_;
    this.inactiveColorTag_ = other.inactiveColorTag_;
    this.styleTag_ = other.styleTag_;
    if (other.labelLocation_ != null) {
      this.labelLocation_ = (Point2D)other.labelLocation_.clone();
    }
    this.hideLabel_ = other.hideLabel_;
    this.hideLabelTopOnly_ = other.hideLabelTopOnly_;
    this.hideInModels_ = (ArrayList)other.hideInModels_.clone();
    this.skipRender_ = other.skipRender_;
  }
  
  /***************************************************************************
  **
  ** Transfer
  */

  public GroupProperties(String newID, int newOrder, GroupProperties other) {
    this.renderer_ = other.renderer_;
    this.layer_ = other.layer_;
    this.order_ = newOrder;
    this.style_ = other.style_;  
    this.groupID_ = newID;
    this.pads_ = new int[4];
    System.arraycopy(other.pads_, 0, this.pads_, 0, 4);
    this.colorTag_ = other.colorTag_;
    this.inactiveColorTag_ = other.inactiveColorTag_;
    this.styleTag_ = other.styleTag_;
    if (other.labelLocation_ != null) {
      this.labelLocation_ = (Point2D)other.labelLocation_.clone();
    }
    this.hideLabel_ = other.hideLabel_;
    this.hideLabelTopOnly_ = other.hideLabelTopOnly_;
    this.hideInModels_ = (ArrayList)other.hideInModels_.clone();
    skipRender_ = false;
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public GroupProperties(BTState appState, Layout layout, String ref, String color,
                         String inactiveColorStr, String style, String layer, 
                         String tpad, String bpad, String lpad, String rpad, 
                         String nameX, String nameY, String order, String hideStr, String hideStrTopOnly) throws IOException {
     
    renderer_ = new GroupFree();
    
    if (style == null) {
      style = "autobound";
      style_ = AUTOBOUND;
    } else {
      style = style.trim();
      if (style.equals("")) {
        style_ = AUTOBOUND;      
      } else if (style.equalsIgnoreCase("autobound")) {
        style_ = AUTOBOUND;
      } else {
        throw new IOException();
      }
    }
    
    pads_ = new int[4];
    try {
      order_ = Integer.parseInt(order);
      layer_ = Integer.parseInt(layer);
      pads_[LEFT] = Integer.parseInt(lpad);
      pads_[RIGHT] = Integer.parseInt(rpad);
      pads_[TOP] = Integer.parseInt(tpad);
      pads_[BOTTOM] = Integer.parseInt(bpad);      
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
    if ((nameX != null) && (nameY != null)) {
      try {
        double x = Double.parseDouble(nameX);
        double y = Double.parseDouble(nameY);
        labelLocation_ = new Point2D.Double(x, y);
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
        
    colorTag_ = color;
    inactiveColorTag_ = inactiveColorStr;
    styleTag_ = style;
    groupID_ = ref;
    
    hideLabel_ = Boolean.valueOf(hideStr).booleanValue();
    hideLabelTopOnly_ = Boolean.valueOf(hideStrTopOnly).booleanValue();
    hideInModels_ = new ArrayList();
    skipRender_ = false;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answer if we are to be rendered.
  */
  
  public boolean doNotRender() {
    return (skipRender_);
  }  
  
  /***************************************************************************
  **
  ** Set if we are to be rendered.
  */
  
  public void setDoNotRender(boolean sr) {
    skipRender_ = sr;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the renderer.
  */
  
  public GroupFree getRenderer() {
    return (renderer_);
  }  
   
  /***************************************************************************
  **
  ** Get the reference
  */
  
  public String getReference() {
    return (groupID_);
  }  
    
  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor(boolean isActive, ColorResolver colR) {
    return (colR.getColor(isActive ? colorTag_ : inactiveColorTag_));
  }
  
  /***************************************************************************
  **
  ** Get the color tag
  */
  
  public String getColorTag(boolean isActive) {
    return (isActive ? colorTag_ : inactiveColorTag_);
  }  

  /***************************************************************************
  **
  ** Set the color tag
  */
  
  public void setColor(boolean isActive, String colorTag) {
    if (isActive) {
      colorTag_ = colorTag;
    } else {
      inactiveColorTag_ = colorTag;
    }
    return;
  }

  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    if (colorTag_.equals(oldID)) {
      colorTag_ = newID;
      retval = true;
    }
    if (inactiveColorTag_.equals(oldID)) {
      inactiveColorTag_ = newID;
      retval = true;
    }
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Get the style (AUTOBOUND)
  */
  
  public int getStyle() {
    return (style_);
  }
  
  /***************************************************************************
  **
  ** Get the layer
  */
  
  public int getLayer() {
    return (layer_);
  }

  /***************************************************************************
  **
  ** Set the layer
  */
  
  public void setLayer(int layer) {
    layer_ = layer;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the order
  */
  
  public int getOrder() {
    return (order_);
  }

  /***************************************************************************
  **
  ** Set the order
  */
  
  public void setOrder(int order) {
    order_ = order;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the edge padding
  */
  
  public int getPadding(int which) {
    return (pads_[which]);
  }
  
  /***************************************************************************
  **
  ** Set the edge padding
  */
  
  public void setPadding(int which, int pad) {
    pads_[which] = pad;
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if the user wants the label hidden
  */
  
  public int getHideLabelMode() {
    if (!hideLabel_) {
      return (SHOW_LABEL);
    } else if (hideLabel_ && hideLabelTopOnly_) {
      return (HIDE_LABEL_TOP_LEVEL_ONLY);
    } else if (hideLabel_ && !hideLabelTopOnly_) {
      return (HIDE_LABEL_ALL_LEVELS);
    } else {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Set if the user wants the label hidden
  */
  
  public void setHideLabel(int hideLabelMode) {
    switch (hideLabelMode) {
      case SHOW_LABEL:
        hideLabel_ = false; 
        hideLabelTopOnly_ = false;
        break;
      case HIDE_LABEL_TOP_LEVEL_ONLY:
        hideLabel_ = true; 
        hideLabelTopOnly_ = true;
        break;
      case HIDE_LABEL_ALL_LEVELS:
        hideLabel_ = true; 
        hideLabelTopOnly_ = false;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the label location.  May be null.
  */
  
  public Point2D getLabelLocation() {
    return (labelLocation_);
  }

  /***************************************************************************
  **
  ** Set the label location.
  */
  
  public void setLabelLocation(Point2D newLoc) {
    labelLocation_ = (Point2D)newLoc.clone();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the comparable to determin drawing/intersection order.
  */
  
  public GroupOrdering getGroupOrdering() {
    return (new GroupOrdering(order_, layer_, groupID_));
  }  
 
  /***************************************************************************
  **
  ** Write the property to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<gprop");
    out.print(" id=\"");
    out.print(groupID_);    
    out.print("\" color=\"");
    out.print(colorTag_);
    out.print("\" inactiveColor=\"");
    out.print(inactiveColorTag_);    
    out.print("\" style=\"");
    out.print(styleTag_);
    out.print("\" layer=\"");
    out.print(layer_);
    out.print("\" order=\"");
    out.print(order_);      
    out.print("\" tpad=\"");
    out.print(pads_[TOP]);
    out.print("\" bpad=\"");
    out.print(pads_[BOTTOM]);
    out.print("\" lpad=\"");
    out.print(pads_[LEFT]);
    out.print("\" rpad=\"");
    out.print(pads_[RIGHT]);    
    if (labelLocation_ != null) {
      out.print("\" nameX=\"");
      out.print(labelLocation_.getX());
      out.print("\" nameY=\"");
      out.print(labelLocation_.getY());    
    }
    if (hideLabel_) {
      out.print("\" hideName=\"true");    
    }
    if (hideLabelTopOnly_) {
      out.print("\" hideNameTopOnly=\"true");    
    }    
    int numHides = hideInModels_.size();
    if (numHides == 0) {
      out.println("\" />");
    } else {
      out.println("\" >");
      ind.up().indent();
      out.println("<groupMaskings>");
      ind.up();
      for (int i = 0; i < numHides; i++) {
        ind.indent();
        out.print("<groupMask id=\"");
        out.print(hideInModels_.get(i));    
        out.println("\" />");        
      }  
      ind.down().indent();
      out.println("</groupMaskings>");
      ind.down().indent();
      out.println("</gprop>");
    }
    
    // The skipRender_ field is used for temporary props only!
    
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("gprop");
  }
  
  /***************************************************************************
  **
  ** Handle layout creation
  **
  */
  
  public static GroupProperties buildFromXML(BTState appState, Layout layout, 
                                             Attributes attrs) throws IOException {
                                             
    String ref = null;
    String color = null;
    String inactive = null;
    String style = null;
    String layer = null;
    String order = "0";
    String lpad = "0";
    String bpad = "0";
    String rpad = "0";
    String tpad = "0";    
    String nameX = null;
    String nameY = null;
    String hideStr = "false";
    String hideStrTopOnly = "false";    

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          ref = val;
        } else if (key.equals("color")) {
          color = val;
        } else if (key.equals("inactiveColor")) {
          inactive = val;          
        } else if (key.equals("style")) {
          style = val;
        } else if (key.equals("layer")) {
          layer = val;
        } else if (key.equals("order")) {
          order = val;          
        } else if (key.equals("nameX")) {
          nameX = val;
        } else if (key.equals("nameY")) {
          nameY = val;
        } else if (key.equals("tpad")) {
          tpad = val;
        } else if (key.equals("bpad")) {
          bpad = val;
        } else if (key.equals("lpad")) {
          lpad = val;
        } else if (key.equals("rpad")) {
          rpad = val;
        } else if (key.equals("hideName")) {
          hideStr = val;
        } else if (key.equals("hideNameTopOnly")) {
          hideStrTopOnly = val;
        }
      }
    }
    
    if ((ref == null) || (color == null) ||
        (style == null) || (layer == null)) {
      throw new IOException();
    }
    
    return (new GroupProperties(appState, layout, ref, color, inactive, style, 
                                layer, tpad, bpad, lpad, rpad, 
                                nameX, nameY, order, hideStr, hideStrTopOnly));
  }
  
  
  /***************************************************************************
  **
  ** Return possible style values
  */
  
  public static Vector<ChoiceContent> getLabelHidingChoices(BTState appState) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_LABEL_MODES_; i++) {
      retval.add(labelHidingForCombo(appState, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent labelHidingForCombo(BTState appState, int mode) {
    if ((mode < 0) || (mode >= NUM_LABEL_MODES_)) {
      throw new IllegalArgumentException();
    }
    return (new ChoiceContent(appState.getRMan().getString("groupPropLabelHiding." + mapLabelHiding(mode)), mode));
  }
  
 
  /***************************************************************************
  **
  ** map label hiding
  */
  
  public static String mapLabelHiding(int mode) {
    switch (mode) {
      case SHOW_LABEL:
        return (SHOW_LABEL_TAG);      
      case HIDE_LABEL_TOP_LEVEL_ONLY:
        return (HIDE_LABEL_TOP_LEVEL_ONLY_TAG);
      case HIDE_LABEL_ALL_LEVELS:
        return (HIDE_LABEL_ALL_LEVELS_TAG);
      default:
        throw new IllegalArgumentException();
    }
  } 
  
  /***************************************************************************
  **
  ** map label hiding
  */
  
  public static int mapLabelHidingTag(String showTag) { 
    if (showTag.equalsIgnoreCase(SHOW_LABEL_TAG)) {
      return (SHOW_LABEL);
    } else if (showTag.equalsIgnoreCase(HIDE_LABEL_TOP_LEVEL_ONLY_TAG)) {
      return (HIDE_LABEL_TOP_LEVEL_ONLY);
    } else if (showTag.equalsIgnoreCase(HIDE_LABEL_ALL_LEVELS_TAG)) {
      return (HIDE_LABEL_ALL_LEVELS);
    } else {
      throw new IllegalArgumentException();
    }  
  } 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  **  Used to order drawing
  */
 
  public static class GroupOrdering implements Comparable<GroupOrdering> {

    private int order_;
    private int layer_;
    private String grpID_;
    
    public GroupOrdering(int order, int layer, String grpID) {
      order_ = order;
      layer_ = layer;
      grpID_ = grpID;
    }

    public int hashCode() {
      return ((order_ * 10) + layer_ + grpID_.hashCode());
    }

    public String toString() {
      return ("GroupOrdering: " + order_ + " " + layer_ + grpID_);
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof GroupOrdering)) {
        return (false);
      }
      
      GroupOrdering otherGO = (GroupOrdering)other; 

      return ((this.order_ == otherGO.order_) &&
              (this.layer_ == otherGO.layer_) &&
              (this.grpID_.equals(otherGO.grpID_)));            
    } 

    public int compareTo(GroupOrdering other) {

      //
      // Remember: Return neg if this is less than other
      //
            
      //
      // Order goes first:
      //
      
      if (this.order_ > other.order_) {
        return (1);
      } else if (this.order_ < other.order_) {
        return (-1);
      }
      
      //
      // Layer is next:
      //
      
      if (this.layer_ > other.layer_) {
        return (1);
      } else if (this.layer_ < other.layer_) {
        return (-1);
      }
      
      //
      // Group ID is just an non-varying tie-breaker:
      //
      
      return (this.grpID_.compareTo(other.grpID_));
    }
  }    
}
