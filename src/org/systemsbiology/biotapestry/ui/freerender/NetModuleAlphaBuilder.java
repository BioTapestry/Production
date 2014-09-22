/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.freerender;

/****************************************************************************
**
** This calculates component alpha for network overlay rendering
*/

public class NetModuleAlphaBuilder {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public final static int MINIMAL_ALL_MEMBER_VIZ = 50;     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private final static double LABEL_START_     = 1.00;
  private final static double LABEL_END_       = 0.80;
  private final static double LABEL_FLOOR_VAL_ = 0.00; 
  
  private final static double FILL_START_      = 1.00;
  private final static double FILL_END_        = 0.50;
  private final static double FILL_FLOOR_VAL_  = 0.00;   

  private final static double BOUND_START_     = 0.40;   
  private final static double BOUND_END_       = 0.00;  
  private final static double BOUND_FLOOR_VAL_ = 0.10;
  
  private final static double BACK_START_      = 0.60;
  private final static double BACK_END_        = 0.40;
  private final static double BACK_FLOOR_VAL_  = 0.00;     
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public NetModuleAlphaBuilder() {
    super();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Calculate individual values from a single master value.  Returns the
  ** intersection masking value
  */
  
  public void alphaCalc(double masterVal, NetModuleFree.CurrentSettings settings) {

    if ((masterVal < 0.0) || (masterVal > 100.0)) {
      throw new IllegalArgumentException();
    }
     
    double masterFraction = ((double)masterVal) / 100.0;

    settings.regionFillAlpha = calcRegionFill(masterFraction);
    settings.regionBoundaryAlpha = calcRegionBoundary(masterFraction);
    settings.regionLabelAlpha = calcFastLabel(masterFraction);
    settings.fastDecayLabelVisible = (settings.regionLabelAlpha > 0.0);
    settings.backgroundOverlayAlpha = calcBackground(masterFraction);
    
    double maskCutoff = NetModuleFree.CurrentSettings.INTERSECTION_CUTOFF;
    if (settings.regionFillAlpha > maskCutoff) {
      settings.intersectionMask = NetModuleFree.CurrentSettings.ALL_MASKED;
    } else if (settings.backgroundOverlayAlpha > maskCutoff) {
      settings.intersectionMask = NetModuleFree.CurrentSettings.NON_MEMBERS_MASKED;
    } else {
      settings.intersectionMask = NetModuleFree.CurrentSettings.NOTHING_MASKED;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if the alpha settings are consistent with hidden module contents:
  */
  
  public boolean modContentsMasked(NetModuleFree.CurrentSettings settings) {
    return (settings.regionFillAlpha > FILL_FLOOR_VAL_);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the region fill transparency
  */
  
  private double calcRegionFill(double masterFraction) {
    return (calcPiecewise(masterFraction, FILL_START_, FILL_END_, FILL_FLOOR_VAL_));
  } 
  
  /***************************************************************************
  **
  ** Get the region boundaty transparency
  */
  
  private double calcRegionBoundary(double masterFraction) {
    return (calcPiecewise(masterFraction, BOUND_START_, BOUND_END_, BOUND_FLOOR_VAL_));
  } 
  
  /***************************************************************************
  **
  ** Get the region label transparency for quickly attenuated labels
  */
  
  private double calcFastLabel(double masterFraction) {
    return (calcPiecewise(masterFraction, LABEL_START_, LABEL_END_, LABEL_FLOOR_VAL_));
  }   
  
  /***************************************************************************
  **
  ** Get the opaque background transparency
  */
  
  private double calcBackground(double masterFraction) {
    return (calcPiecewise(masterFraction, BACK_START_, BACK_END_, BACK_FLOOR_VAL_));
  }   
  
  /***************************************************************************
  **
  ** Piecewise linear reduction
  */
  
  private double calcPiecewise(double masterFraction, double startx, double endx, double yFloor) {
    if (masterFraction > startx) {
      return (1.0);
    } else if (masterFraction < endx) {
      return (yFloor);
    } else {  
      double xfrac = (masterFraction - endx) / (startx - endx);
      double delY = 1.0 - yFloor;
      return (yFloor + (delY * xfrac));  
    }
  }   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
}
