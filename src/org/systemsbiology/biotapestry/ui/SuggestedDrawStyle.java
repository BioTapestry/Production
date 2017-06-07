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
import java.awt.Color;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Contains suggested drawing style info for a link segment
*/

public class SuggestedDrawStyle implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /**
  *** Possible Line Styles:
  **/

  // After no style, must be in order of decreasing ink (determines winner of disputes)
  
  public static final int UNDEFINED_STYLE = -2;
  public static final int VARIOUS_STYLE   = -1;
  public static final int NO_STYLE        = 0;
  public static final int SOLID_STYLE     = 1;
  public static final int DASH_STYLE      = 2;
  public static final int DOTTED_STYLE    = 3;  
  
  public static final String NONE_TAG   = "none";
  public static final String SOLID_TAG  = "solid";  
  public static final String DASH_TAG   = "dash";
  public static final String DOTTED_TAG = "dot";  

  public static final int CUSTOM_THICKNESS = -100;
  public static final String CUSTOM_TAG = "custom"; 
  
  public static final int UNDEFINED_THICKNESS    = -6;
  public static final int VARIOUS_THICKNESS      = -5;
  public static final int NO_THICKNESS_SPECIFIED = -4;    
  public static final int REGULAR_THICKNESS      = -3;  
  public static final int THIN_THICKNESS         = -2;
  public static final int THICK_THICKNESS        = -1;  
  
  public static final String NO_THICK_TAG = "none";
  public static final String REGULAR_TAG  = "regular";  
  public static final String THIN_TAG     = "thin";
  public static final String THICK_TAG    = "thick";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  //  INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int style_;
  private String color_;
  private int thickness_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Constructor
  */

  public SuggestedDrawStyle(String color) {
    style_ = NO_STYLE;
    color_ = color;
    thickness_ = NO_THICKNESS_SPECIFIED;
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public SuggestedDrawStyle(int style, String color, int thickness) {
    style_ = style;
    color_ = color;
    thickness_ = thickness;
  }
    
  /***************************************************************************
  **
  ** XML-based Constructor
  */

  public SuggestedDrawStyle(String color, String style, String thickness) throws IOException {
    
    color_ = color;
       
    if (style == null) {
      style_ = NO_STYLE;
    } else {    
      try {
        style_ = mapLineStyleTag(style); 
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
    if (thickness == null) {
      thickness_ = NO_THICKNESS_SPECIFIED;
    } else { 
      try {
        thickness_ = mapThicknessTag(thickness); 
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Get a display string for the UI
  */
  
  public String getDisplayString(ResourceManager rMan, ColorResolver cRes) {
    String colorName = (color_ == null) ? rMan.getString("drawStyle.defaultColor") : cRes.getNamedColor(color_).name;
    String styName = rMan.getString("drawStyle." + mapLineStyle(style_));
    String thickName;
    if (customThicknessRequest(thickness_)) {
      String form = rMan.getString("drawStyle.customThickFormat");
      thickName = MessageFormat.format(form, new Object[] {new Integer(thickness_)});
    } else {
      thickName = rMan.getString("drawStyle." + mapThickness(thickness_));
    }
    return (colorName + "--" + styName + "--" + thickName);
  }  

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public SuggestedDrawStyle clone() {
    try {
      SuggestedDrawStyle retval = (SuggestedDrawStyle)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** To string
  */
  
  @Override
  public String toString() {
    return ("SuggestedDrawStyle: drawStyle = " + style_ + " color = " + color_ 
            + " thick = " + thickness_);
  }
  
  /***************************************************************************
  **
  ** Hashcode
  */
  
  @Override
  public int hashCode() {
    return (((color_ == null) ? 0 : color_.hashCode()) + (style_ * 10) + thickness_);
  }
  
  /***************************************************************************
  **
  ** Equals.  Note that two styles with unequal guts may resolve to equal results
  ** based on different modulations collapsing to the same result.  Ignoring this
  ** at the moment.  Fix me?
  */
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof SuggestedDrawStyle)) {
      return (false);
    }
    SuggestedDrawStyle otherDS = (SuggestedDrawStyle)other;
    if (this.thickness_ != otherDS.thickness_) {
      return (false);
    }
    if (this.style_ != otherDS.style_) {
      return (false);
    }
    
    if (this.color_ == null) {
      return (otherDS.color_ == null);
    }

    return (this.color_.equalsIgnoreCase(otherDS.color_));
  }

  /***************************************************************************
  **
  ** Remap the color tag
  */
  
  public void mapColorTags(Map<String, String> ctm) {
    String nk = ctm.get(color_);
    if (nk != null) {
      color_ = nk;
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Fill with defaults
  */
  
  public void fillWithDefaults() {
    if (style_ == NO_STYLE) {
      style_ = SOLID_STYLE;
    }
    if (thickness_ == NO_THICKNESS_SPECIFIED) {
      thickness_ = REGULAR_THICKNESS;
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the color name (may be null)
  */
  
  public String getColorName() {
    return (color_);
  }

  /***************************************************************************
  **
  ** Set the color name (may be null)
  */
  
  public void setColorName(String colorName) {
    color_ = colorName;
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the special color (may be null) 
  */
  
  public Color getColor(ColorResolver cRes) {
    if (color_ == null) {
      return (null);
    }
    return (cRes.getColor(color_));
  }
  
  /***************************************************************************
  **
  ** Get the style
  */
  
  public int getLineStyle() {
    return (style_);
  }
  
  /***************************************************************************
  **
  ** Get the line thickness
  */
  
  public int getThickness() {
    return (thickness_);
  }
  
  /***************************************************************************
  **
  ** Set the style
  */
  
  public void setLineStyle(int style) {
    style_ = style;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the line thickness
  */
  
  public void setThickness(int thickness) {
    thickness_ = thickness;
    return;
  }  

  /***************************************************************************
  **
  ** Return a draw style that overrides this one as specified
  */
 
  public SuggestedDrawStyle mergeOverrides(SuggestedDrawStyle other) {
    // This fixes the reason for BT-11-14-07:1.  When a single
    // segment link was collapsed to direct, the pervious version
    // had a retval with NO_THICKNESS_SPECIFIED and NO_STYLE:
    //
    SuggestedDrawStyle retval = this.clone();
    retval.color_ = (other.color_ != null) ? other.color_ : this.color_;
    if (other.thickness_ != NO_THICKNESS_SPECIFIED) {
      retval.thickness_ = other.thickness_;
    }
    if (other.style_ != NO_STYLE) {
      retval.style_ = other.style_;
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Update with winner of thickness dispute
  */
 
  public int returnWinningThick(int currentThick, boolean forModules) {
    //
    // If we are not voting on thickness, we pass the value
    // through
    //
    
    if (thickness_ == NO_THICKNESS_SPECIFIED) {
      return (currentThick);
    }
      
    int myTrueThick = trueThick(thickness_, forModules);
    if (currentThick == NO_THICKNESS_SPECIFIED) {
      return (myTrueThick);
    }    
    
    return ((myTrueThick > currentThick) ? myTrueThick : currentThick);
  }
  
  /***************************************************************************
  **
  ** Update with winner of style dispute
  */
        
  public int returnWinningStyle(int currentStyle) {
    
    if (style_ == NO_STYLE) {
      return (currentStyle);
    } else if (currentStyle == NO_STYLE) {
      return (style_);
    }
    // Depends on order of integer tags: more ink wins!
    return ((currentStyle > style_) ? style_ : currentStyle);
  }
        
  /***************************************************************************
  **
  ** Help out calculation of average color
  */
        
  public int sumColors(ColorResolver cRes, int[] rgbSums, int currentNum) {
    Color myColor = getColor(cRes);
    if (myColor == null) {
      return (currentNum);
    }
    rgbSums[0] += myColor.getRed();
    rgbSums[1] += myColor.getGreen();    
    rgbSums[2] += myColor.getBlue();
    return (currentNum + 1);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<drawStyle ");
    if (color_ != null) {
      out.print("color=\"");
      out.print(color_);    
      out.print("\" ");
    }
    if (style_ != NO_STYLE) {
      out.print("style=\"");
      out.print(mapLineStyle(style_));    
      out.print("\" ");
    }
    if (thickness_ != NO_THICKNESS_SPECIFIED) {
      out.print("thick=\"");
      out.print(mapThickness(thickness_));    
      out.print("\" ");
    }    
    out.println("/>");
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return possible style values
  */
  
  public static Vector<ChoiceContent> getStyleChoices(ResourceManager rMan) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    retval.add(lineStyleForCombo(rMan, SOLID_STYLE));
    retval.add(lineStyleForCombo(rMan, DASH_STYLE)); 
    retval.add(lineStyleForCombo(rMan, DOTTED_STYLE));     
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent lineStyleForCombo(ResourceManager rMan, int style) {
    if (style == NO_STYLE) {
      throw new IllegalArgumentException();
    }
    return (new ChoiceContent(rMan.getString("drawStyle." + mapLineStyle(style)), style));
  }
  
  /***************************************************************************
  **
  ** Determine custom thickness requests
  */
  
  public static boolean customThicknessRequest(int thickVal) {
    return ((thickVal == CUSTOM_THICKNESS) || (thickVal >= 0));
  }
  
  /***************************************************************************
  **
  ** Return possible thickness values
  */

  public static Vector<ChoiceContent> getThicknessChoices(ResourceManager rMan) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    retval.add(thicknessForCombo(rMan, REGULAR_THICKNESS));
    retval.add(thicknessForCombo(rMan, THIN_THICKNESS));
    retval.add(thicknessForCombo(rMan, THICK_THICKNESS));    
    retval.add(thicknessForCombo(rMan, CUSTOM_THICKNESS));
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent thicknessForCombo(ResourceManager rMan, int thickness) {
    if (customThicknessRequest(thickness)) {
      return (new ChoiceContent(rMan.getString("drawStyle." + CUSTOM_TAG), CUSTOM_THICKNESS));   
    }
    if (thickness == NO_THICKNESS_SPECIFIED) {
      throw new IllegalArgumentException();
    }
    return (new ChoiceContent(rMan.getString("drawStyle." + mapThickness(thickness)), thickness));    
  }  
 
  /***************************************************************************
  **
  ** map the style
  */
  
  public static String mapLineStyle(int style) {
    switch (style) {
      case NO_STYLE:
        return (NONE_TAG);      
      case SOLID_STYLE:
        return (SOLID_TAG);
      case DASH_STYLE:
        return (DASH_TAG);
      case DOTTED_STYLE:
        return (DOTTED_TAG);        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the thickness value to a tag (custom tag not allowed)
  */
  
  public static String mapThickness(int thickness) {
    switch (thickness) {
      case CUSTOM_THICKNESS:
        throw new IllegalArgumentException(); 
      case NO_THICKNESS_SPECIFIED:
        return (NO_THICK_TAG);      
      case REGULAR_THICKNESS:
        return (REGULAR_TAG);
      case THIN_THICKNESS:
        return (THIN_TAG);
      case THICK_THICKNESS:
        return (THICK_TAG);        
      default:
        return (Integer.toString(thickness));
    }
  }
  
  /***************************************************************************
  **
  ** map the thickness value to a tag
  */
  
  public static int mapThicknessTag(String thickTag) {
    if (thickTag.equalsIgnoreCase(CUSTOM_TAG)) {
      throw new IllegalArgumentException();
    } else if (thickTag.equalsIgnoreCase(NO_THICK_TAG)) {
      return (NO_THICKNESS_SPECIFIED);
    } else if (thickTag.equalsIgnoreCase(REGULAR_TAG)) {
      return (REGULAR_THICKNESS);
    } else if (thickTag.equalsIgnoreCase(THIN_TAG)) {
      return (THIN_THICKNESS);
    } else if (thickTag.equalsIgnoreCase(THICK_TAG)) {
      return (THICK_THICKNESS);
    }
    
    try {
      int val = Integer.parseInt(thickTag);
      if (val < 0) {
        throw new IllegalArgumentException();
      }
      return (val);
    } catch (NumberFormatException nfex) {
      throw new IllegalArgumentException();
    } 
  }  
  
  /***************************************************************************
  **
  ** map the style
  */
  
  public static int mapLineStyleTag(String styleTag) { 
    if (styleTag.equalsIgnoreCase(NONE_TAG)) {
      return (NO_STYLE);
    } else if (styleTag.equalsIgnoreCase(SOLID_TAG)) {
      return (SOLID_STYLE);
    } else if (styleTag.equalsIgnoreCase(DASH_TAG)) {
      return (DASH_STYLE);
    } else if (styleTag.equalsIgnoreCase(DOTTED_TAG)) {
      return (DOTTED_STYLE);      
    } else {
      throw new IllegalArgumentException();
    }  
  }
  
  /***************************************************************************
  **
  ** Build from old legacy info (may be null)
  */
  
  public static SuggestedDrawStyle buildFromLegacy(int oldStyleTag) {
    switch (oldStyleTag) {
      case LinkProperties.NONE:
        return (null);
      case LinkProperties.SOLID:
        return (new SuggestedDrawStyle(SOLID_STYLE, null, REGULAR_THICKNESS));
      case LinkProperties.THIN:
        return (new SuggestedDrawStyle(SOLID_STYLE, null, THIN_THICKNESS));
      case LinkProperties.DASH:
        return (new SuggestedDrawStyle(DASH_STYLE, null, REGULAR_THICKNESS));
      case LinkProperties.THINDASH:
        return (new SuggestedDrawStyle(DOTTED_STYLE, null, THIN_THICKNESS));        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Figure out true thickness
  */
 
  public static int trueThick(int thick, boolean forModules) {
    if (thick == NO_THICKNESS_SPECIFIED) {
      throw new IllegalArgumentException();
    } else if (thick == REGULAR_THICKNESS) {
      return ((forModules) ? ResolvedDrawStyle.REG_THICK_MOD : ResolvedDrawStyle.REG_THICK) ;
    } else if (thick == THIN_THICKNESS) {
      return ((forModules) ? ResolvedDrawStyle.THIN_THICK_MOD : ResolvedDrawStyle.THIN_THICK);
    } else if (thick == THICK_THICKNESS) {
      return ((forModules) ? ResolvedDrawStyle.THICK_THICK_MOD : ResolvedDrawStyle.THICK_THICK);
    } else {
      return (thick);
    }
  }
  
  /***************************************************************************
  **
  ** For two-pass decisions
  */
 
  public static int drawThick(boolean forModules) {
    return ((forModules) ? ResolvedDrawStyle.REG_THICK_MOD : ResolvedDrawStyle.REG_THICK);
  }  
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class SuggestedDrawStyleWorker extends AbstractFactoryClient {
    
    public SuggestedDrawStyleWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("drawStyle");
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("drawStyle")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.suggSty = buildFromXML(elemName, attrs);
        retval = board.suggSty;
      }
      return (retval);     
    }

    private SuggestedDrawStyle buildFromXML(String elemName, Attributes attrs) throws IOException {
      String colorStr = AttributeExtractor.extractAttribute(elemName, attrs, "drawStyle", "color", false);
      String styleStr = AttributeExtractor.extractAttribute(elemName, attrs, "drawStyle", "style", false);
      String thickStr = AttributeExtractor.extractAttribute(elemName, attrs, "drawStyle", "thick", false);
      //
      // Can't all be empty:
      //
      if ((colorStr == null) && (styleStr == null) && (thickStr == null)) {
        throw new IOException();
      }   
      return (new SuggestedDrawStyle(colorStr, styleStr, thickStr));
    }
  } 
}
