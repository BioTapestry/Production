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

package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.InvertedLinkProps;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of a link
*/

public class RemoveLinkage extends AbstractControlFlow {

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
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
  
  public RemoveLinkage() {
    name = "linkPopup.LinkDelete";
    desc = "linkPopup.LinkDelete";     
    mnem = "linkPopup.LinkDeleteMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
    
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
    
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!isSingleSeg) {
      return (false);
    }
    return (true);
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
        throw new IllegalStateException();
      } else {
        StepState ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("stepToRemove")) {
          next = ans.stepToRemove();       
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;
 
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepToRemove";
    }
    
    /***************************************************************************
    **
    ** Set params
    */ 
        
    public void setIntersection(Intersection intersect) {
      intersect_ = intersect;
      return;
    }
      
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
       
      
      UndoSupport support = uFac_.provideUndoSupport("undo.linkDelete", dacx_);
      RemoveSupport.deleteWarningHelper(uics_, dacx_);
      if (deleteLinkFromModel(intersect_, dacx_, support, uFac_)) {
        uics_.getGenomePresentation().clearSelections(uics_, dacx_, support);
        uics_.getSUPanel().drawModel(false);
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
        support.finish();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }
  
  /***************************************************************************
  **
  ** Delete a link from the model
  */  
  
  public static boolean deleteLinkFromModel(Intersection intersect, DataAccessContext rcx, UndoSupport support, UndoFactory uFac) {
 
    //
    // Figure out what linkage property we are dealing with.
    //
     
    BusProperties bp = rcx.getCurrentLayout().getLinkProperties(intersect.getObjectID());
    if (bp == null) {
      return (false);
    }
     
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.linkDelete", rcx);
    }
    
    //
    // Figure out which underlying linkages use the intersected segment, and
    // just kill them off.
    //
    //
    // 1) Go to all genome instances and kill off instances
    // 2) Go to the genome and kill off links
    // 3) Go to all layouts and delete the links from the property and the
    // dictionary entries for the links. If there
    // are no links left, all references to the empty property will be deleted. 
    //
 
     LinkSegmentID segID = intersect.segmentIDFromIntersect();       
     Set<String> resolved = bp.resolveLinkagesThroughSegment(segID);
     Iterator<String> resit = resolved.iterator();
     while (resit.hasNext()) {
       String nextDeadID = resit.next();          
       doLinkDelete(nextDeadID, rcx, support);
     }       
    
     if (localUndo) {support.finish();} 
     return (true);
   }

  /***************************************************************************
  **
  ** Delete a link from the given model and all submodels
  */  
  
  public static void doLinkDelete(String deadID, DataAccessContext rcx, UndoSupport support) {
    
    //
    // Go up to the root to get the root ID:
    //
      
    boolean isRoot;
    boolean removeProperties;
    GenomeInstance inst;
    if (rcx.currentGenomeIsAnInstance()) {
      inst = rcx.getCurrentGenomeAsInstance();
      isRoot = false;
      removeProperties = (inst.getVfgParent() == null);
    } else {
      inst = null;
      isRoot = true;
      removeProperties = true;
    }
  
    //
    // If we are the root, kill off the root and all instances. If an instance, kill 
    // off the instance and all subinstances.  Kill off the display properties at the same time.
    // We find all the instances of the same root link in each genome instance,
    // and kill it off from the instance, and all layouts for the instance.
    //
      
    Iterator<GenomeInstance> iit = rcx.getGenomeSource().getInstanceIterator();
    HashSet<String> deadSet = new HashSet<String>();
    while (iit.hasNext()) {
      deadSet.clear();
      GenomeInstance gi = iit.next();
      if (isRoot || inst.isAncestor(gi)) {  // We are interested in this instance
        Iterator<Linkage> lit = gi.getLinkageIterator();
        while (lit.hasNext()) {
          LinkageInstance li = (LinkageInstance)lit.next();
          if (isRoot && (deadID.equals(li.getBacking().getID())) || (deadID.equals(li.getID()))) {
            deadSet.add(li.getID());
          }
        }
      }
      Iterator<String> dsit = deadSet.iterator();
      while (dsit.hasNext()) {
        String key = dsit.next();
        GenomeChange gc = gi.removeLinkage(key);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(rcx, gc);
          support.addEdit(gcc);        
        }
        if (removeProperties) {
          Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
          while (layit.hasNext()) {
            Layout layout = layit.next();
            if (layout.getTarget().equals(gi.getID())) {
              Layout.PropChange[] lpc = new Layout.PropChange[1];             
              lpc[0] = layout.removeLinkProperties(key);
              if (lpc[0] != null) {
                PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
                support.addEdit(pcc);
              }
            }
          }
        }
      }
    }
    
    //
    // Clean up the core genome, if appropriate:
    //
    
    if (isRoot) {  
      GenomeChange gc = rcx.getCurrentGenome().removeLinkage(deadID);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(rcx, gc);
        support.addEdit(gcc);        
      }
      Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
      while (layit.hasNext()) {
        Layout layout = layit.next();
        if (layout.getTarget().equals(rcx.getCurrentGenomeID())) {
          Layout.PropChange[] lpc = new Layout.PropChange[1];
          lpc[0] = layout.removeLinkProperties(deadID);
          if (lpc[0] != null) {
            PropChangeCmd pcc = new PropChangeCmd(rcx, lpc);
            support.addEdit(pcc);
          }
        }
      }
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Delete a pile of links from the given model and all submodels
  */  
  
  public static void doMultiLinkDelete(Set<String> deadIDSet, DataAccessContext rcx,
                                       UndoSupport support) {
    
    //
    // Go up to the root to get the root ID:
    //
      
    boolean isRoot;
    boolean removeProperties;
    GenomeInstance inst;
    if (rcx.currentGenomeIsAnInstance()) {
      inst = rcx.getCurrentGenomeAsInstance();
      isRoot = false;
      removeProperties = (inst.getVfgParent() == null);
    } else {
      inst = null;
      isRoot = true;
      removeProperties = true;
    }
  
    //
    // If we are the root, kill off the root and all instances. If an instance, kill 
    // off the instance and all subinstances.  Kill off the display properties at the same time.
    // We find all the instances of the same root link in each genome instance,
    // and kill it off from the instance, and all layouts for the instance.
    //
      
    Iterator<GenomeInstance> iit = rcx.getGenomeSource().getInstanceIterator();
    HashSet<String> allDeadSet = new HashSet<String>();
    while (iit.hasNext()) {
      allDeadSet.clear();
      GenomeInstance gi = iit.next();
      if (isRoot || inst.isAncestor(gi)) {  // We are interested in this instance
        Iterator<Linkage> lit = gi.getLinkageIterator();
        while (lit.hasNext()) {
          LinkageInstance li = (LinkageInstance)lit.next();
          if (isRoot && (deadIDSet.contains(li.getBacking().getID())) || (deadIDSet.contains(li.getID()))) {
            allDeadSet.add(li.getID());
          }
        }
      }
      Iterator<String> dsit = allDeadSet.iterator();
      while (dsit.hasNext()) {
        String key = dsit.next();
        GenomeChange gc = gi.removeLinkage(key);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(rcx, gc);
          support.addEdit(gcc);        
        }
      }
      if (removeProperties) {
        Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
        while (layit.hasNext()) {
          Layout layout = layit.next();
          if (layout.getTarget().equals(gi.getID())) {
            DatabaseChange dc = rcx.getLayoutSource().startLayoutUndoTransaction(layout.getID());       
            layout.removeMultiLinkProperties(allDeadSet);          
            dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
            support.addEdit(new DatabaseChangeCmd(rcx, dc));
          }
        }
      }
    }
    
    //
    // Clean up the core genome, if appropriate:
    //
    if (isRoot) {
      Iterator<String> dsit = deadIDSet.iterator();
      while (dsit.hasNext()) {
        String key = dsit.next();
        GenomeChange gc = rcx.getCurrentGenome().removeLinkage(key);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(rcx, gc);
          support.addEdit(gcc);        
        }
      }

      Iterator<Layout> layit = rcx.getLayoutSource().getLayoutIterator();
      while (layit.hasNext()) {
        Layout layout = layit.next();
        if (layout.getTarget().equals(rcx.getCurrentGenomeID())) {
          DatabaseChange dc = rcx.getLayoutSource().startLayoutUndoTransaction(layout.getID());       
          layout.removeMultiLinkProperties(deadIDSet);          
          dc = rcx.getLayoutSource().finishLayoutUndoTransaction(dc);
          support.addEdit(new DatabaseChangeCmd(rcx, dc));
        }
      }
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Delete a pile of links from the model
  */  
  
  public static void deleteLinkSetFromModel(Set<String> linkSet, DataAccessContext rcx, UndoSupport support) {
    doMultiLinkDelete(linkSet, rcx, support);
    return;
  } 
  
  /***************************************************************************
  **
  ** Resolve Links Thru Intersections
  */  
  
  public static Set<String> resolveLinksThruIntersections(Map<String, Intersection> intersections, DataAccessContext rcx) {
  
    //
    // Crank thru the intersections.  Make up a set of all the linkages that resolve
    //
 
    Iterator<String> ikit = intersections.keySet().iterator();
    HashSet<String> resolved = new HashSet<String>();
    HashSet<LinkSegmentID> tempSet = new HashSet<LinkSegmentID>();
    while (ikit.hasNext()) {
      String key = ikit.next();
      Intersection intersect = intersections.get(key);
      //
      // Figure out what linkage property we are dealing with.
      //
      BusProperties lp = rcx.getCurrentLayout().getLinkProperties(key);
      if (lp == null) {  // will this ever happen?
        continue;
      } 
      InvertedLinkProps ilp = new InvertedLinkProps(lp);
      LinkSegmentID[] segIDs = intersect.segmentIDsFromIntersect();
      tempSet.clear();
      for (int i = 0; i < segIDs.length; i++) {
        tempSet.add(segIDs[i]);
      }
      resolved.addAll(lp.resolveLinkagesThroughSegments(tempSet, ilp));
    }
    return (resolved);
  }   

  /***************************************************************************
  **
  ** Delete a pile of links from the model
  */  
  
  public static boolean deleteLinksFromModel(Map<String, Intersection> intersections, DataAccessContext rcx, 
                                             UndoSupport support, UndoFactory uFac) {
    boolean localUndo = false;
    if (support == null) {
      localUndo = true;
      support = uFac.provideUndoSupport("undo.linksDelete", rcx);
    }    
    
    //
    // Crank thru the intersections.  Make up a set of all the linkages that
    // are being tossed.  Then toss them.
    //
      
    Set<String> resolved = resolveLinksThruIntersections(intersections, rcx);
    doMultiLinkDelete(resolved, rcx, support);
    if ((resolved.size() > 0) && localUndo) {support.finish();} 
    return (resolved.size() > 0);
  }   
}
