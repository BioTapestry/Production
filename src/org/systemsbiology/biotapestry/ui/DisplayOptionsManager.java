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

package org.systemsbiology.biotapestry.ui;

import java.io.PrintWriter;

import java.util.Map;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.DisplayOptionsChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;

/****************************************************************************
**
** Display Options Manager.
*/

public class DisplayOptionsManager implements MinimalDispOptMgr {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private DisplayOptions options_;
  private boolean bigScreen_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */
    
  public DisplayOptionsManager(BTState appState) {
    appState_ = appState;
    options_ = new DisplayOptions(appState_);
    bigScreen_ = false;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** set for big screen
  */

  public void setForBigScreen(boolean bigScreen) {
    bigScreen_ = bigScreen;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Answer for big screen
  */

  public boolean isForBigScreen() {
    return (bigScreen_);
  }
  
  /***************************************************************************
  ** 
  ** Get the display options
  */

  public DisplayOptions getDisplayOptions() {
    return (options_);
  }
  
  /***************************************************************************
  ** 
  ** Set the display options for IO.  Further custom setting might be applied
  ** after this.
  */

  public void setDisplayOptionsForIO(DisplayOptions opts) {
    options_ = opts;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Set the display options
  */

  public DisplayOptionsChange setDisplayOptions(DisplayOptions opts) {
    DisplayOptionsChange retval = new DisplayOptionsChange();
    retval.oldOpts = options_.clone();
    options_ = opts;
    retval.newOpts = options_.clone();    
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Do modifications if the perturbation data changes
  */

  public void modifyForPertDataChange(UndoSupport support, DataAccessContext dacx) {
    
    Map<String, String> revMap = options_.haveInconsistentMeasurementDisplayColors();
    String revSTag = options_.haveInconsistentScaleTag();
    if ((revMap != null) || (revSTag != null)) {
      DisplayOptions revDO = options_.clone();
      if (revMap != null) {
        revDO.setMeasurementDisplayColors(revMap);
      }
      if (revSTag != null) {      
        revDO.setPerturbDataDisplayScaleKey(revSTag);
      }
      DisplayOptionsChange doc = setDisplayOptions(revDO);
      DisplayOptionsChangeCmd docc = new DisplayOptionsChangeCmd(appState_, dacx, doc);
      support.addEdit(docc);
    }
    return;   
  }

  /***************************************************************************
  **
  ** Turn on special link branches
  ** 
  */
  
  public void turnOnSpecialLinkBranches(UndoSupport support, DataAccessContext dacx, boolean doFinish) { 
    
    DisplayOptions newOpts = options_.clone();
    newOpts.setBranchMode(DisplayOptions.OUTLINED_BUS_BRANCHES);
    DisplayOptionsChange doc = setDisplayOptions(newOpts);
    if (support != null) {
      DisplayOptionsChangeCmd docc = new DisplayOptionsChangeCmd(appState_, dacx, doc);
      support.addEdit(docc);
      LayoutChangeEvent lcev = new LayoutChangeEvent(dacx.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(lcev);
      if (doFinish) {
        support.finish();
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Write the options to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    options_.writeXML(out, ind);
    return;
  }
 
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(DisplayOptionsChange undo) {
    options_ = undo.oldOpts.clone();
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(DisplayOptionsChange redo) {
    options_ = redo.newOpts.clone();
    return;
  }
}
