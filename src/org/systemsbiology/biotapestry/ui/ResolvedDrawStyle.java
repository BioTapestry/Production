/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.awt.BasicStroke;

import org.systemsbiology.biotapestry.db.ColorResolver;

/****************************************************************************
**
** Contains drawing style info for a link segment
*/

public class ResolvedDrawStyle implements Cloneable {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final int REG_THICK        = 3;
  public static final int THIN_THICK       = 2;
  public static final int THICK_THICK      = 7;
  
  public static final int REG_THICK_MOD    = 5;
  public static final int THIN_THICK_MOD   = 3;
  public static final int THICK_THICK_MOD  = 9;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private static final float DOT_      = 4.0F;
  private static final float DOT_SP_   = 6.0F;
  private static final float DASH_     = 10.0F;
  private static final float DASH_SP_  = 6.0F;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  //  INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int style_;
  private Color calculatedColor_;
  private int thickness_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor: Convert suggestion to resolved values
  */

  public ResolvedDrawStyle(SuggestedDrawStyle currStyle, boolean colorMatters, boolean forModules, ColorResolver cRes, DisplayOptions dopt) {    
    if (!colorMatters) {
      calculatedColor_ = dopt.getInactiveGray();
    } else {
      calculatedColor_ = currStyle.getColor(cRes);
    }
    style_ = currStyle.getLineStyle();
    thickness_ = SuggestedDrawStyle.trueThick(currStyle.getThickness(), forModules);
  } 
  
  /***************************************************************************
  **
  ** Constructor: Convert suggestion to resolved values, with possible overrides
  */

  public ResolvedDrawStyle(SuggestedDrawStyle currStyle, boolean colorMatters, 
                           int maxThick, int maxStyle, Color newColor, boolean forModules, ColorResolver cRes, DisplayOptions dopt) {
    this(currStyle, colorMatters, forModules, cRes, dopt);
    if (maxThick != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
      thickness_ = SuggestedDrawStyle.trueThick(maxThick, forModules);
    }
    if (maxStyle != SuggestedDrawStyle.NO_STYLE) {
      style_ = maxStyle;
    }     
    if (colorMatters && (newColor != null)) {
      calculatedColor_ = newColor;
    }    
  }   

  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      ResolvedDrawStyle retval = (ResolvedDrawStyle)super.clone();
      // Don't need to clone color; it is immutable
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Hashcode
  */
  
  public int hashCode() {
    return (calculatedColor_.hashCode() + (style_ * 10) + thickness_);
  }
  
  /***************************************************************************
  **
  ** Equals
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof ResolvedDrawStyle)) {
      return (false);
    }
    ResolvedDrawStyle otherDS = (ResolvedDrawStyle)other;
    if (this.thickness_ != otherDS.thickness_) {
      return (false);
    }
    if (this.style_ != otherDS.style_) {
      return (false);
    }   
    return (this.calculatedColor_.equals(otherDS.calculatedColor_));
  }

  /***************************************************************************
  **
  ** Get the color
  */
  
  public Color getColor() {
    return (calculatedColor_);
  }  

  /***************************************************************************
  **
  ** Modulate color by activity
  */

  public void modulateColor(double level, DisplayOptions dopt) {
    float[] colHSB = new float[3];    
    Color.RGBtoHSB(calculatedColor_.getRed(), calculatedColor_.getGreen(), calculatedColor_.getBlue(), colHSB);
    float[] iag = dopt.getInactiveGrayHSV();
    // Hue stays the same:
    colHSB[1] = iag[1] + ((float)level * (colHSB[1] - iag[1])); 
    colHSB[2] = iag[2] + ((float)level * (colHSB[2] - iag[2]));         
    calculatedColor_ = Color.getHSBColor(colHSB[0], colHSB[1], colHSB[2]);
    return;
  }             

  /***************************************************************************
  **
  ** Modulate the line thickness by activity
  */

  public void modulateThickness(double level) {
    double modThick = 1.0 + (level * ((double)thickness_ - 1.0));     
    thickness_ = (int)Math.round(modThick);
    return;
  }       
   
  /***************************************************************************
  **
  ** Do a final override
  */
  
  public void masterUpdate(SuggestedDrawStyle masterDrawStyle, boolean isActive, boolean isGhosted, boolean forModules, ColorResolver cRes) {
    int masterThick = masterDrawStyle.getThickness();
    if (masterThick != SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) {
      thickness_ = SuggestedDrawStyle.trueThick(masterThick, forModules);
    }
    
    int masterStyle = masterDrawStyle.getLineStyle();
    if (masterStyle != SuggestedDrawStyle.NO_STYLE) {
      style_ = masterStyle;
    }
    
    if (!isActive || isGhosted) {
      return;  // color not affected by suggestions
    }
    
    Color masterColor = masterDrawStyle.getColor(cRes);
    if (masterColor != null) {
      calculatedColor_ = masterColor;
    }
    
    return;
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
  ** Calculate the stroke
  */

  public BasicStroke calcStroke() {     

    int butt = BasicStroke.CAP_BUTT;
    int join = BasicStroke.JOIN_ROUND;
    float miter = 10.0F;
    
    float[] patternArray;
    if ((style_ == SuggestedDrawStyle.NO_STYLE) || (style_ == SuggestedDrawStyle.SOLID_STYLE)) {
      patternArray = null;
    } else if (style_ == SuggestedDrawStyle.DASH_STYLE) {
      patternArray = new float[] {DASH_, DASH_SP_};
    } else if (style_ == SuggestedDrawStyle.DOTTED_STYLE) {
      patternArray = new float[] {DOT_, DOT_SP_};
    } else {
      throw new IllegalStateException();
    }

    if (patternArray == null) {
      return (new BasicStroke(thickness_, butt, join));
    } else {
      return (new BasicStroke(thickness_, butt, join, miter, patternArray, 0F));
    }
  }
}
