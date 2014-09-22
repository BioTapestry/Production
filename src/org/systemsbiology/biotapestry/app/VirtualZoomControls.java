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


package org.systemsbiology.biotapestry.app;

import java.util.HashMap;
import java.util.Map;

import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.flow.ControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.nav.ZoomChangeTracker;
import org.systemsbiology.biotapestry.nav.ZoomCommandSupport;

/****************************************************************************
**
** Virtual Zoom Controls
*/

public class VirtualZoomControls implements ZoomChangeTracker {
                                   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   

  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private HashMap<FlowMeister.FlowKey, Boolean> buttonStat_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualZoomControls(BTState appState) {
    appState_ = appState;
    appState_.setVirtualZoom(this);
    appState_.setZoomCommandSupport(new ZoomCommandSupport(this, appState_));
    buttonStat_ = new HashMap<FlowMeister.FlowKey, Boolean>();
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get button enabled status
  */ 
 
  public Map<FlowMeister.FlowKey, Boolean> getButtonEnables() {
    return (buttonStat_);
  }
  
  /***************************************************************************
  **
  ** Tell us the zoom state has changed
  */ 
  
  public void zoomStateChanged(boolean scrollOnly) {
    if (!scrollOnly) {
      handleZoomButtons();
    }
    appState_.getZoomTarget().setCurrClipRect(appState_.getZoomCommandSupport().getCurrClipRect());
    //
    // Note that if we actually begin to use the clip rect as a drawing
    // speed up, we would need to call a repaint on each scroll
    // change!  Probably want to track viewport size to model bounds, and
    // also model visual complexity!  Could do this repaint inside of SUPanel
    // and let it decide it it is necessary....
    //
    // appState_.getSUPanel().repaint();
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle zoom buttons
  */ 

  private void handleZoomButtons() {  
    //
    // Enable/disable zoom actions based on zoom limits:
    //
    MainCommands mcmd = appState_.getMainCmds();
    if (!appState_.isHeadless()) {
      MainCommands.ChecksForEnabled zaOutWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_OUT, true);
      MainCommands.ChecksForEnabled zaOutNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_OUT, false);
      MainCommands.ChecksForEnabled zaInWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_IN, true);
      MainCommands.ChecksForEnabled zaInNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_IN, false);            
      if (appState_.getZoomCommandSupport().zoomIsWide()) {
        zaOutWI.setConditionalEnabled(false);
        if (zaOutNI != null) zaOutNI.setConditionalEnabled(false);
        zaInWI.setConditionalEnabled(true);
        if (zaInNI != null) zaInNI.setConditionalEnabled(true);
      } else if (appState_.getZoomCommandSupport().zoomIsMax()) {
        zaOutWI.setConditionalEnabled(true);
        if (zaOutNI != null) zaOutNI.setConditionalEnabled(true);
        zaInWI.setConditionalEnabled(false);
        if (zaInNI != null) zaInNI.setConditionalEnabled(false);        
      } else {
        zaOutWI.setConditionalEnabled(true);
        if (zaOutNI != null) zaOutNI.setConditionalEnabled(true);
        zaInWI.setConditionalEnabled(true);
        if (zaInNI != null) zaInNI.setConditionalEnabled(true);              
      }
    } else {
      buttonStat_.clear();
       
      ControlFlow zoFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.ZOOM_OUT);
      if (!zoFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      ControlFlow ziFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.ZOOM_IN);
      if (!ziFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
           
      if (appState_.getZoomCommandSupport().zoomIsWide()) {
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_OUT, new Boolean(false));   
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_IN, new Boolean(true));
      } else if (appState_.getZoomCommandSupport().zoomIsMax()) {
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_OUT, new Boolean(true));   
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_IN, new Boolean(false));   
      } else {
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_OUT, new Boolean(true));   
        buttonStat_.put(FlowMeister.MainFlow.ZOOM_IN, new Boolean(true));
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle zoom to current button
  */ 

  public void handleZoomToCurrentButton() {  
    //
    // Enable/disable zoom actions based on zoom limits:
    //   
    boolean onSelPath = appState_.getZoomTarget().haveCurrentSelectionForBounds();
    MainCommands mcmd = appState_.getMainCmds();
    if (!appState_.isHeadless()) {     
      MainCommands.ChecksForEnabled z2cWI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED, true);
      MainCommands.ChecksForEnabled z2cNI = mcmd.getCachedActionIfPresent(FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED, false);         
      z2cWI.setConditionalEnabled(onSelPath);
      if (z2cNI != null) z2cNI.setConditionalEnabled(onSelPath);
    } else {
      ControlFlow zcsFlow = mcmd.getCachedFlow(FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED);
      if (!zcsFlow.externallyEnabled()) {
        throw new IllegalStateException();
      }
      buttonStat_.put(FlowMeister.MainFlow.ZOOM_TO_CURRENT_SELECTED, new Boolean(onSelPath));     
    }
    return;
  }
}
