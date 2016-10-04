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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle merging links that are duplicates
*/

public class MergeDuplicateLinks extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MergeDuplicateLinks(BTState appState) {
    super(appState);
    name = "linkPopup.MergeLinks";
    desc = "linkPopup.MergeLinks";
    mnem = "linkPopup.MergeLinksMnem";             
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
   
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    if (!isSingleSeg) {
      return (false);
    }
    Genome genome = rcx.getGenome();
    return (genome instanceof DBGenome);
  } 

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
   
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new StepState(appState_, dacx));  
  }
  
  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        throw new IllegalArgumentException();
      } else {
        StepState ans = (StepState)last.currStateX;
        if (ans.cfh == null) {
          ans.cfh = cfh;
        }
        if (ans.getNextStep().equals("mergeDups")) {
          next = ans.mergeDups();      
        } else {
          throw new IllegalStateException();
        }
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupPointCmdState {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    private Intersection inter_;
    private DataAccessContext rcxT_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      rcxT_ = dacx;
      nextStep_ = "mergeDups";
    }
    
    /***************************************************************************
    **
    ** Next step...
    */ 
     
    public String getNextStep() {
      return (nextStep_);
    }
      
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection intersect) {
      inter_ = intersect;
      return;
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setPopupPoint(Point2D ptp) {
      return;
    }
    
    /***************************************************************************
    **
    ** Generate the list
    */
      
    private DialogAndInProcessCmd mergeDups() {
    
      LinkProperties lp = rcxT_.getLayout().getLinkProperties(inter_.getObjectID());
      LinkSegmentID lsid = inter_.segmentIDFromIntersect();
      Set<String> resolved = lp.resolveLinkagesThroughSegment(lsid);
      String linkID = null;
      if (resolved.size() > 1) {       
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      } else {
        linkID = resolved.iterator().next();
      }
      mergeDuplicateLinks(appState_, linkID, rcxT_, null);     
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
  }
  
  /***************************************************************************
  **
  ** Generate the list
  */
    
  public static Set<String> mergeDuplicateLinks(BTState appState, String linkID, DataAccessContext rcx, UndoSupport support) {
    
    if (!rcx.genomeIsRootGenome()) {
      throw new IllegalArgumentException();
    }
  
    HashSet<String> retval = new HashSet<String>();
    Set<String> mergie = rcx.getGenomeAsDBGenome().mergeableLinks(linkID);
    boolean canMerge = rcx.fgho.noInstanceOverlap(linkID, mergie);
    
    if (!canMerge) {
      return (retval);
    }
    
    retval.addAll(mergie);
    
    //
    // Going top-down, one root instance with its kids at a time:
    //
    
    List<String> ordered = rcx.fgho.orderedModels();
    Iterator<String> iit = ordered.iterator();
    
    boolean gottaFinish = false;
    if (support == null) {
      support = new UndoSupport(appState, "undo.mergeDupLinks");
      gottaFinish = true;
    }
    
    HashMap<String, String> oldToNew = new HashMap<String, String>();   
    while (iit.hasNext()) {
      String key = iit.next();
      Genome genome = rcx.getGenomeSource().getGenome(key);
      if (genome instanceof DBGenome) {
        continue;
      }
      GenomeInstance gi = (GenomeInstance)genome;
      Layout currLayout = null;
      if (gi.isRootInstance()) {
        oldToNew.clear();
        currLayout = rcx.lSrc.getLayoutForGenomeKey(gi.getID());
      }
      GenomeChange[] gcm = gi.mergeComplementaryLinks(linkID, mergie, oldToNew);
      for (int i = 0; i < gcm.length; i++) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcx, gcm[i]);
        support.addEdit(gcc); 
      } 
      support.addEvent(new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      
      if (gi.isRootInstance()) {
        DatabaseChange dc = rcx.lSrc.startLayoutUndoTransaction(currLayout.getID());
        currLayout.mergeLinkProperties(oldToNew);
        dc = rcx.lSrc.finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(appState, rcx, dc));
        support.addEvent(new LayoutChangeEvent(currLayout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));              
      }
    }
  
   //
   // Clean up the core genome.
   // Remember the model change events are what triggers the clearing the dynamic proxy caches!
   //
    
   Iterator<String> dsit = mergie.iterator();
    while (dsit.hasNext()) {
      String key = dsit.next();
      GenomeChange gc = rcx.getGenome().removeLinkage(key);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState, rcx, gc);
        support.addEdit(gcc);
        support.addEvent(new ModelChangeEvent(rcx.getGenome().getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }
    }

    Iterator<Layout> layit = rcx.lSrc.getLayoutIterator();
    while (layit.hasNext()) {
      Layout layout = layit.next();
      if (layout.getTarget().equals(rcx.getGenomeID())) {
        DatabaseChange dc = rcx.lSrc.startLayoutUndoTransaction(layout.getID());       
        layout.removeMultiLinkProperties(mergie);          
        dc = rcx.lSrc.finishLayoutUndoTransaction(dc);
        support.addEdit(new DatabaseChangeCmd(appState, rcx, dc));
        support.addEvent(new LayoutChangeEvent(layout.getID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));
      }
    }
    
    if (gottaFinish) {
      support.finish();
    }
    return (retval);
  }
}
