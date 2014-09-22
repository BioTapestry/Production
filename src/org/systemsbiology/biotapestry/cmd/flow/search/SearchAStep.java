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


package org.systemsbiology.biotapestry.cmd.flow.search;

import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector.Searches;

/****************************************************************************
**
** Handle search and selection one step upstream or downstream
*/

public class SearchAStep extends AbstractControlFlow {

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
 
  private boolean goUpstream_;
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SearchAStep(BTState appState, boolean upstream) {
    super(appState);
    goUpstream_ = upstream;
    if (upstream) {
      name = "command.SearchUpstream"; 
      desc = "command.SearchUpstream";
      icon = "FIXME24.gif"; 
      mnem = "command.SearchUpstreamMnem";
      accel = "command.SearchUpstreamAccel";     
    } else {
      name = "command.SearchDownstream"; 
      desc = "command.SearchDownstream";
      icon = "FIXME24.gif"; 
      mnem = "command.SearchDownstreamMnem";
      accel = "command.SearchDownstreamAccel";  
    }
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
    
  public boolean isEnabled(CheckGutsCache cache) {
    return (cache.haveANodeSelection());
  }
 
  /***************************************************************************
  **
  ** Final visualization step
  ** 
  */
  
  @Override
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last) {
    if (last.state != DialogAndInProcessCmd.Progress.DONE) {
      throw new IllegalStateException();
    }
    StepSearchState ans = (StepSearchState)last.currStateX;
    return (ans.generateVizResult());
  }
  
  /***************************************************************************
  **
  ** The new interface
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        StepSearchState nss = new StepSearchState(appState_, goUpstream_, cfh.getDataAccessContext());
        next = nss.processCommand();
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
  ** Running State:
  */
        
  public static class StepSearchState implements DialogAndInProcessCmd.CmdState {
     
    private boolean myGoUpstream;
    private String nextStep_;
    private DataAccessContext rcxT_;
    //----------------- Result
    private SourceAndTargetSelector.SearchResult sres;
    private BTState appState_;
  
    public String getNextStep() {
      return (nextStep_);
    }
     
    StepSearchState(BTState appState, boolean goUpstream, DataAccessContext dacx) {
      appState_ = appState;
      myGoUpstream = goUpstream;
      rcxT_ = dacx;
    }   
    
    /***************************************************************************
    **
    ** Do the search
    */
     
    DialogAndInProcessCmd processCommand() {
     
      SUPanel sup = appState_.getSUPanel();
     
      Set<String> nodesOnly = sup.getSelectedNodes();
      Searches sc = (myGoUpstream) ? Searches.SOURCE_SELECT : Searches.TARGET_SELECT;
      SourceAndTargetSelector sats = new SourceAndTargetSelector(rcxT_); 
      // When we step back far enough, link segments that were selected start getting unselected, which is a bug. But, in general, 
      // we would not be selecting all possible paths anyway (that is why we only allow link selection in the case of direct 
      // source/targets searches as well.) Do maybe make links active, but only after we figure out how to do it right! 
      sres = sats.doSelection(nodesOnly, sc, true, false); //!linksHidden);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /***************************************************************************
    **
    ** 
    ** 
    */

    VisualChangeResult generateVizResult() {
      VisualChangeResult retval = new VisualChangeResult(true);
      boolean nodeFound = false;
      if (sres != null) {
        if (!sres.found.isEmpty()) {
          retval.addChange(VisualChangeResult.ViewChange.NEW_SELECTIONS);
          retval.setSelections(sres.found, sres.linkIntersections, false);
          nodeFound = true;
        }
      }
      retval.setViewport((nodeFound) ? VisualChangeResult.Viewports.SELECTED : VisualChangeResult.Viewports.NO_CHANGE);       
      return (retval);
    }
  }
}
