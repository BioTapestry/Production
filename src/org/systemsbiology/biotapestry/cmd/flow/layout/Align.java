/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.LayoutCenteringDialog;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle alignment of all layouts
*/

public class Align extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public Align() {
    name =  "command.CenterAllLayouts";
    desc = "command.CenterAllLayouts";
    mnem =  "command.CenterAllLayoutsMnem";
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
   
  @Override
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.moreThanOneModel());
  }

  /***************************************************************************
  **
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepState ans = new StepState(cfh);
        next = ans.stepDoIt();    
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState {
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepDoIt";
    }
 
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepDoIt() {
      boolean trackOverlays = dacx_.getFGHO().overlayExists();
      boolean emptyRoot = dacx_.getDBGenome().isEmpty();
      
      //
      // If the root is empty and there are no overlays, no point in launching
      // the dialog.  Everything in child models will be centered on the empty 
      // worksheet.
      //
      
      boolean doMatchups;
      boolean doCentering;
      boolean ignoreOverlays;
      if (emptyRoot && !trackOverlays) {
        doMatchups = false;
        doCentering = true;
        ignoreOverlays = true;
      } else {
        LayoutCenteringDialog dialog = new LayoutCenteringDialog(uics_, dacx_, emptyRoot, trackOverlays);
        dialog.setVisible(true);
        if (!dialog.haveResult()) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
          
        }       
        doMatchups = dialog.doMatchups();
        doCentering = dialog.doCentering();
        ignoreOverlays = dialog.ignoreOverlays();
      }
      
      boolean done = alignAllLayouts(doMatchups, doCentering, ignoreOverlays);
      if (done) {
        uics_.getZoomCommandSupport().zoomToFullWorksheet();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    
    /***************************************************************************
    **
    ** Align all layouts to the root
    */  
   
    private boolean alignAllLayouts(boolean doMatchups, boolean doCentering, boolean skipOverlays) {
    
      Map<String, Layout.OverlayKeySet> allKeys = (skipOverlays) ? null : dacx_.getFGHO().fullModuleKeysPerLayout();
      
      UndoSupport support = uFac_.provideUndoSupport("undo.alignAllLayouts", dacx_);
  
      //
      // Iterate through all root instances and line them all up
      //
                                           
      Layout lor = dacx_.getLayoutSource().getRootLayout();
      Layout.OverlayKeySet lorModKeys = (skipOverlays) ? null : allKeys.get(lor.getID());
   
      //
      // Now slide each layout to match the root
      //
  
      Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        if (gi.getVfgParent() != null) {
          continue;
        }
        Layout lo = dacx_.getLayoutSource().getLayoutForGenomeKey(gi.getID());
      
        DatabaseChange dc = dacx_.getLayoutSource().startLayoutUndoTransaction(lo.getID());
        Layout.OverlayKeySet loModKeys = (skipOverlays) ? null : allKeys.get(lo.getID());
        StaticDataAccessContext rcx = new StaticDataAccessContext(dacx_, gi, lo);
        lo.alignToLayout(lor, rcx, doMatchups, loModKeys, lorModKeys, null);
        dc = dacx_.getLayoutSource().finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(dacx_, dc));
        LayoutChangeEvent lcev = new LayoutChangeEvent(lo.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(lcev);
      }
      
      if (doCentering) {
        dacx_.getZoomTarget().fixCenterPoint(true, support, false);
      }
      support.finish();  
      return (true);
    }  
  }
}
