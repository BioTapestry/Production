/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.nav;

import java.awt.Rectangle;

import org.systemsbiology.biotapestry.ui.DisplayOptions;

/****************************************************************************
**
** Cross-Platform Model tree
*/

public class XPlatModelTree {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private XPlatModelNode root_;
  private boolean hasImages_; 
  private boolean hasOverlays_;
  private XPlatTimeAxisDefinition timeSliderDef_;
  private DisplayOptions.FirstZoom firstZoomMode_;
  private DisplayOptions.NavZoom navZoomMode_;
  private Rectangle allBounds_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor. Hand this a fully stocked root so we can figure out
  ** recursively if the tree holds images or overlays. 
  */ 
  
  public XPlatModelTree(XPlatModelNode root, DisplayOptions dOpt, Rectangle allBounds, XPlatTimeAxisDefinition timeSliderDef) {
    root_ = root;
    hasImages_ = root_.iOrKidsHaveImage();
    hasOverlays_ = root_.iOrKidsHaveOverlays();
    firstZoomMode_ = dOpt.getFirstZoomMode();
    navZoomMode_ = dOpt.getNavZoomMode();
    allBounds_ = allBounds;
    this.timeSliderDef_ = timeSliderDef;
  }
  
  public XPlatModelTree(XPlatModelNode root, DisplayOptions dOpt, Rectangle allBounds) {
	  this(root,dOpt,allBounds,null);
	    root_ = root;
	  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the root
  */ 
   
  public XPlatModelNode getRoot() {
    return (root_);
  }
  
  /***************************************************************************
  **
  ** Get if tree has associated image(s)
  */ 
   
  public boolean getHasImages() {
    return (hasImages_);
  }
  
  /***************************************************************************
  **
  ** Get if tree has associated overlays
  */ 
   
  public boolean getHasOverlays() {
    return (hasOverlays_);
  }

  /***************************************************************************
  **
  ** Get first zoom mode
  */ 
   
  public DisplayOptions.FirstZoom getFirstZoomMode() {
    return (firstZoomMode_);
  }
  
  /***************************************************************************
  **
  ** Get nav zoom mode
  */ 
   
  public DisplayOptions.NavZoom getNavZoomMode() {
    return (navZoomMode_);
  }
  
  /***************************************************************************
  **
  ** Get rectangle to bound all models
  */ 
   
  public Rectangle getAllModelBounds() {
    return (allBounds_);
  }
  
  
  /**
   * Time Slider definition object getter and setter
   * 
   * @return
   */
  
  public XPlatTimeAxisDefinition getTimeSliderDef() {
	return this.timeSliderDef_;
  }

  public void setTimeSliderDef(XPlatTimeAxisDefinition timeSliderDef) {
	this.timeSliderDef_ = timeSliderDef;
  }

/***************************************************************************
  **
  ** Standard string
  */ 
   
  @Override
  public String toString() {
    return ("XPlatModelTree: " + root_.toString());
  }
}
