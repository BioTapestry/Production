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


package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.dialogs.CisModuleEditDialogFactory;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle super add
*/

public class EditCisRegModule extends AbstractControlFlow {

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
  
  public EditCisRegModule() {
    name = "genePopup.CisModuleEdit" ;
    desc = "genePopup.CisModuleEdit";
    mnem = "genePopup.CisModuleEditMnem";
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
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    if (!rcx.currentGenomeIsRootDBGenome()) {
      return (false);
    }
    String geneID = inter.getObjectID();
    Gene gene = rcx.getCurrentGenome().getGene(geneID);
    return ((gene != null) && (gene.getNumRegions() > 0));
  }
  
  
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
  ** Handle the flow
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
  
    DialogAndInProcessCmd next;
    while (true) {
      StepState ans;
      if (last == null) {
        throw new IllegalArgumentException();
      } else { 
        ans = (StepState)last.currStateX;
      }
      ans.stockCfhIfNeeded(cfh);
      if (ans.getNextStep().equals("stepState")) {
        next = ans.stepState();
      } else if (ans.getNextStep().equals("stepDataExtract")) {
        next = ans.stepDataExtract(last);
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
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState {
    
    private Intersection intersect_;  
    private String geneID_;
    private List<DBGeneRegion> origList_;
    private Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla_;
    private Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr_;
     
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(StaticDataAccessContext dacx) {
      super(dacx);
      nextStep_ = "stepState";
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
       
    private DialogAndInProcessCmd stepState() {
      geneID_ = intersect_.getObjectID();
      Gene gene = dacx_.getCurrentGenome().getGene(geneID_);
      if (gene == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }

      origList_ = DBGeneRegion.initTheList(gene); 
     
      FullGenomeHierarchyOracle fgo = dacx_.getFGHO();
      
      gla_ = fgo.analyzeLinksIntoModules(geneID_);   
      if (FullGenomeHierarchyOracle.hasModuleProblems(gla_)) {
        throw new IllegalStateException();
      }
      lhr_ = DBGeneRegion.linkPadRequirement(gla_, geneID_);  
 
      //
      // We will not allow holders that have inbound unassigned links to be obliterated. Turn them
      // into non-holders *JUST* for the purpose of user editing.
      //
      
      Set<DBGeneRegion.DBRegKey> linkHolders = fgo.holdersWithLinks(geneID_, gla_);
      List<DBGeneRegion> glist = new ArrayList<DBGeneRegion>();
      int num = origList_.size();
      for (int i = 0; i < num; i++) {
        DBGeneRegion reg = origList_.get(i);
        if (linkHolders.contains(reg.getKey())) {
          glist.add(new DBGeneRegion(reg.getStringID(), reg.getStartPad(), reg.getEndPad(), reg.getEvidenceLevel(), false, true));           
        } else {
          glist.add(reg.clone());
        }
      }
    
      uics_.getSUPanel().pushBubblesAndShow();
      CisModuleEditDialogFactory.CisModBuildArgs ba = 
        new CisModuleEditDialogFactory.CisModBuildArgs(geneID_, gene, glist, dacx_.getRMan(), gla_, lhr_);
      CisModuleEditDialogFactory dgcdf = new CisModuleEditDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepDataExtract";
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the work
    */
         
    private DialogAndInProcessCmd stepDataExtract(DialogAndInProcessCmd cmd) {
   
      CisModuleEditDialogFactory.CisModRequest crq = (CisModuleEditDialogFactory.CisModRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }
      
      List<DBGeneRegion> newRegs;
      Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew = new HashMap<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey>();
      DBGeneRegion.DBRegKey reallocate = null;
      DBGeneRegion.DBRegKey delReg = null;
      boolean noLinksToMove = false;
      
      if (crq.delMode != null) {
        newRegs = processForDelete(crq, oldToNew);
        delReg = crq.selectedKey;
        reallocate = crq.deleteLinkTarget;
      } else if (crq.slideMode != null) {
        newRegs = processForSlide(crq, lhr_, oldToNew);        
      } else if (crq.moveMode != null) {
        newRegs = processForMove(crq, oldToNew);
      } else if (crq.nameMode != null) {
        newRegs = processForNameEvidence(crq, oldToNew);
       // noLinksToMove = true; Should not need to move, but last-minute bugfix says keep things the same if possible!
      } else if (crq.sizeMode != null) {
        newRegs = processForSize(crq, oldToNew);  
      } else {
        throw new IllegalArgumentException();
      }

      UndoSupport support = uFac_.provideUndoSupport("undo.changeCisReg", dacx_);  
       
      //
      // Deal with links in a deletion region:
      //
      
      SortedMap<Integer, List<String>> delLinkMap = null;
      Set<String> delLinks = null;
      SortedMap<Integer, List<String>> reallocLinkMap = null;
      List<MinMax> targs = null;
      Integer padZeroOffset = null;
      if (delReg != null) {
        delLinkMap = new TreeMap<Integer, List<String>>();
        delLinks = new HashSet<String>();
        deletionLinks(delReg, delLinkMap, delLinks);
        // User has told us where to put links:
        if (reallocate != null) {
          DBGeneRegion delTarg = DBGeneRegion.getRegion(newRegs, reallocate);
          targs = new ArrayList<MinMax>();
          targs.add(new MinMax(delTarg.getStartPad(), delTarg.getEndPad()));
          reallocLinkMap = new TreeMap<Integer, List<String>>();
          deletionLinks(reallocate, reallocLinkMap, new HashSet<String>());
          padZeroOffset = Integer.valueOf(delTarg.getStartPad());
        } else if (crq.delMode != CisModuleEditDialogFactory.CisModRequest.DeleteMode.DELETE_BLANK) {
          // User doesn't care where they go. If we are leaving a gap, cool! If not, we need to find some 
          // non-module space after everything is moved:
          DBGeneRegion delRego = DBGeneRegion.getRegion(origList_, delReg);
          targs = findBlankTargRegions(newRegs, crq, delRego.getWidth()); 
        } else {
          // Issue 264 crops up because the deleted region becomes a holder that is merged to the holder region
          // next to it, and those links were then relocated because they had offsets WRT their original holder
          // region. OOPS! Better to add them the relocate exclusion list, but this is safer so close to release.
          // There will be no links to move in the delete blank case!
          noLinksToMove = true;
        }
      }

      GenomeChange gc = dacx_.getDBGenome().changeGeneRegions(geneID_, newRegs);
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
        support.addEdit(gcc);
      }
  
      if (!noLinksToMove) {
        LinkSupport.moveCisRegModLinks(support, newRegs, oldToNew, gla_, lhr_, dacx_, geneID_, delLinks);
      }
      
      //
      // Now that links have been moved around, time to move the links in the deleted region:
      // Three cases. 
      // 1) We have enough pads to reassign links to pads using original offsets. Same arrangement. (Case for non-reassignment
      //    both when deleted region is collapsed, or retained).
      // 2) Assignment to another module. We have enough pads to reassign links in same order, but some or all gaps 
      //    must be squeezed out first. May be the case that we need to skip or reuse pads in use.
      // 3) Assignment to another module. We have to reuse pads to get everybody into the specified module.
      //
      //
      
      if ((delLinks != null) && (targs != null)) {
        if (reallocate == null) {
          assignVoidedRegionLinks(delLinkMap, targs, support);
        } else {
          reassignDeletedRegionLinks(delLinkMap, reallocLinkMap, targs, padZeroOffset.intValue(), support);     
        }
      }
      
      //
      // Fix for Issue #265. Last deletion results in a full-span holder, which we need to drop. More logical to
      // to it above, but safer to do it here just before we are done, after link adjustment work.
      //
      //
      if (newRegs.size() == 1) {
        DBGeneRegion lastReg = newRegs.get(0);
        if (lastReg.isHolder()) {
          List<DBGeneRegion> empty = new ArrayList<DBGeneRegion>();
          gc = dacx_.getDBGenome().changeGeneRegions(geneID_, empty);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
            support.addEdit(gcc);
          }
        }
      }
 
      support.finish();
      uics_.getSUPanel().popBubbles();
      uics_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));     
    }
 
   /***************************************************************************
    **
    ** Dump deleted region links into another assigned region.
    */
      
    private void reassignDeletedRegionLinks(SortedMap<Integer, List<String>> delLinkMap, 
                                            SortedMap<Integer, List<String>> reallocLinkMap,
                                            List<MinMax> targs, int reallocPadZeroOffset, UndoSupport support) {       
    
      SortedSet<Integer> freePads = new TreeSet<Integer>();
      if (targs.size() != 1) {
        throw new IllegalArgumentException();
      }
      MinMax nextMax = targs.get(0);
      freePads.add(Integer.valueOf(nextMax.min));
      freePads.add(Integer.valueOf(nextMax.max));
      freePads = DataUtil.fillOutHourly(freePads);
      
      int needPads = delLinkMap.size();
      int haveEmptyPads = freePads.size() - reallocLinkMap.size();
      
      //
      // These are the pads (absolute coord, not offset) that are available for moving into:
      
      ArrayList<Integer> padsAvail = new ArrayList<Integer>();
      Set<Integer> usedPads = reallocLinkMap.keySet();
      for (Integer freePad : freePads) {
        if (!usedPads.contains(freePad - reallocPadZeroOffset)) {
          padsAvail.add(freePad);
        }
      }
      
      ArrayList<Integer> padsToUse = new ArrayList<Integer>();  
      //
      // There are all sorts of fancy ways to try to retain existing gap geometries while overlaying
      // the source and target pad requirements. Forget it. Just get everybody slotted in:
      if (haveEmptyPads >= needPads) {
        // We may need to drop existing gaps, but everybody will get a space!
        // CASES  A & B
        for (int i = 0; i < needPads; i++) {
          padsToUse.add(padsAvail.get(Integer.valueOf(i)));  
        } 
      } else if (haveEmptyPads > 0) {
        // There are some empty pads we can use to crowd in. Do it by binning.
        // CASE C
        int binSize = (int)Math.ceil((double)needPads / (double)padsAvail.size());
        for (int i = 0; i < needPads; i++) {
          padsToUse.add(padsAvail.get(Integer.valueOf(i / binSize)));  
        }       
      } else {
        // We need to steal some used pads in the existing region to fit in at all. Just use offset zero.
        // CASE D
        for (int i = 0; i < needPads; i++) {
          padsToUse.add(Integer.valueOf(0) + reallocPadZeroOffset);  
        }
      }
      
      int count = 0;
      for (Integer pad : delLinkMap.keySet()) {
        int usePad = padsToUse.get(count++);
        List<String> linksToPad = delLinkMap.get(pad);
        for (String linkID : linksToPad) {         
          LinkSupport.switchLinkRegion(support, linkID, usePad, dacx_);
        }
      }
      return;
    }

    /***************************************************************************
    **
    ** If a deleted region has links that are not reassigned, get them down.
    */
      
    private void assignVoidedRegionLinks(SortedMap<Integer, List<String>> delLinkMap, List<MinMax> targs, UndoSupport support) {       
    
      List<Integer> freePads = new ArrayList<Integer>();
      for (int i = 0; i < targs.size(); i++) {
        MinMax nextMax = targs.get(i);
        for (int j = nextMax.min; j <= nextMax.max; j++) {
          freePads.add(Integer.valueOf(j));
        }
      }
      int minPad = Integer.MAX_VALUE;
      for (Integer pad : delLinkMap.keySet()) {
        if (minPad == Integer.MAX_VALUE) {
          minPad = pad.intValue();
        }
        int index = pad.intValue() - minPad;
        List<String> linksToPad = delLinkMap.get(pad);
        for (String linkID : linksToPad) {         
          int newPad = freePads.get(index).intValue();
          LinkSupport.switchLinkRegion(support, linkID, newPad, dacx_);
        }
      }
      return;
    }

    /***************************************************************************
    **
    ** Get links that are in a deleted region
    */
      
    private void deletionLinks(DBGeneRegion.DBRegKey delReg, SortedMap<Integer, List<String>> linksInDelRegion, Set<String> links) {   
  
      //
      // If user has selected a reallocation region, all links in deleted region must go there. If not, links must
      // be moved to non-region pads. If the user left a gap, we don't need to do anything.
      //
       
      String baseID = GenomeItemInstance.getBaseID(geneID_);
      DBGenome dbg = dacx_.getDBGenome();
      // Pre-move analysis:
      Map<String, DBGeneRegion.LinkAnalysis> cla = gla_.get(dbg.getID());
      DBGeneRegion.LinkAnalysis ls = cla.get(baseID); 
      for (String lkey : ls.offsets.keySet()) {
        DBGeneRegion.PadOffset poff = ls.offsets.get(lkey);
        if (poff.regKey.equals(delReg)) {
          Integer off = Integer.valueOf(poff.offset);
          List<String> forOff = linksInDelRegion.get(off);
          if (forOff == null) {
            forOff = new ArrayList<String>();
            linksInDelRegion.put(off, forOff);
          }
          forOff.add(lkey);
          links.add(lkey);
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Extract delete info
    */
      
    private List<MinMax> findBlankTargRegions(List<DBGeneRegion> newRegs,
                                              CisModuleEditDialogFactory.CisModRequest crq, int delWidth) { 
      
      int num = newRegs.size();
      ArrayList<MinMax> retval = new ArrayList<MinMax>();
    
      switch (crq.delMode) {
        case DELETE_LEFT:
          DBGeneRegion lr = newRegs.get(0);
          retval.add(new MinMax(lr.getEndPad() - delWidth + 1, lr.getEndPad()));
          break;

        case DELETE_RIGHT:
          DBGeneRegion rr = newRegs.get(num - 1);
          retval.add(new MinMax(rr.getStartPad(), rr.getStartPad() + delWidth));
          break;
        case DELETE_EQUAL:
          lr = newRegs.get(0);
          retval.add(new MinMax(lr.getEndPad() - (delWidth / 2) + 1, lr.getEndPad()));
          rr = newRegs.get(num - 1);
          retval.add(new MinMax(rr.getStartPad(), rr.getStartPad() + (delWidth / 2)));
          break; 
        default:
          throw new IllegalArgumentException();
        
      }
      return (retval);
    }
 
    /***************************************************************************
    **
    ** Extract delete info
    */
      
    private List<DBGeneRegion> processForDelete(CisModuleEditDialogFactory.CisModRequest crq, 
                                                Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) { 
      
      DBGeneRegion currReg = DBGeneRegion.getRegion(origList_, crq.selectedKey);
      int currPos = DBGeneRegion.regionIndex(origList_, crq.selectedKey);
      
      int size = currReg.getWidth();
      int num = origList_.size();
      int firstPad = origList_.get(0).getStartPad();
      int lastPad = origList_.get(num - 1).getEndPad();       

      Set<Integer> avoid = DBGeneRegion.getExistingHolderKeys(origList_);
      List<DBGeneRegion> newRegs = new ArrayList<DBGeneRegion>();
         
      switch (crq.delMode) {
        case DELETE_BLANK:
          for (int i = 0; i < num; i++) {
            if (i != currPos) {
              newRegs.add(origList_.get(i).clone());
            } else { // Replace with holder
              DBGeneRegion repl = origList_.get(i);
              DBGeneRegion newReg = new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), repl.getStartPad(), repl.getEndPad(), Gene.LEVEL_NONE, true);
              newRegs.add(newReg);
            }
          }
          break;
        case DELETE_RIGHT:
          for (int i = 0; i < num; i++) {
            if (i < currPos) {
              newRegs.add(origList_.get(i).clone());
            } else if (i == currPos) {
              continue;
            } else {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() - size, 
                                                  orig.getEndPad() - size, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            }
          }
          // backfill right end with new holder
          newRegs.add(new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), lastPad - size + 1, lastPad, Gene.LEVEL_NONE, true));     
          break;
        case DELETE_LEFT:
          newRegs.add(new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), firstPad, firstPad + size - 1, Gene.LEVEL_NONE, true)); 
          for (int i = 0; i < num; i++) {
            if (i < currPos) {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + size, 
                                                  orig.getEndPad() + size, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            } else if (i == currPos) {
              continue;
            } else {
              newRegs.add(origList_.get(i).clone());
            }
          }     
          break;
        case DELETE_EQUAL:
          int leftShift = size / 2;
          int rightShift = size - leftShift;
          newRegs.add(new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), firstPad, firstPad + leftShift, Gene.LEVEL_NONE, true)); 
          for (int i = 0; i < num; i++) {
            if (i < currPos) {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + leftShift, 
                                                  orig.getEndPad() + leftShift, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            } else if (i == currPos) {
              continue;
            } else {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() - rightShift, 
                                                  orig.getEndPad() - rightShift, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            }
          }
          newRegs.add(new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), lastPad - rightShift + 1, lastPad, Gene.LEVEL_NONE, true)); 
          break; 
        default:
          throw new IllegalArgumentException();
        
      }
           
      //
      // Might have introduced contiguous runs of holders:
      //
      
      Gene gene = dacx_.getCurrentGenome().getGene(geneID_);    
      newRegs = DBGeneRegion.fillGapsWithHolders(newRegs, gene);
      newRegs = DBGeneRegion.mergeHolders(newRegs, oldToNew);
     
      return (newRegs);
    }
        
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private List<DBGeneRegion> processForSlide(CisModuleEditDialogFactory.CisModRequest crq,
                                               Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> padUse,
                                               Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) {
   
      boolean pullIt = (crq.slideMode == CisModuleEditDialogFactory.CisModRequest.SlideMode.SLIDE_PULL);
      List<DBGeneRegion> newRegs = new ArrayList<DBGeneRegion>();
      
      int num = origList_.size();
      int currPos = DBGeneRegion.regionIndex(origList_, crq.selectedKey);
      int slideAmt = crq.slideAmt;
      
      TreeMap<Integer, DBGeneRegion> newSet = new TreeMap<Integer, DBGeneRegion>();
      DBGeneRegion orig = origList_.get(currPos);
      if (orig.isHolder()) {
        throw new IllegalStateException();
      }
      DBGeneRegion moved = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + slideAmt, 
                                            orig.getEndPad() + slideAmt, orig.getEvidenceLevel(), orig.isHolder());
      newSet.put(Integer.valueOf(currPos), moved);
      
      boolean goingLeft = (slideAmt < 0);
 
      if (goingLeft) {
        if (currPos == 0) { // We cannot be going left if we are 0
          throw new IllegalStateException();
        }
        int remainingSlide = -slideAmt;
        DBGeneRegion onLeft = origList_.get(currPos - 1);
        if (onLeft.isHolder()) {
          int holderWidth = onLeft.getWidth();
          SortedSet<Integer> pads = padUse.get(onLeft.getKey());
          int required = (pads == null) ? 0 : pads.size();
          int preferred = holderWidth - remainingSlide;
          int remains = Math.max(preferred, required);
          int reduction = holderWidth - remains;
          if (remains > 0) {
            DBGeneRegion mod = new DBGeneRegion(onLeft.getStringID(), onLeft.getStartPad() + slideAmt + reduction, onLeft.getEndPad() + slideAmt, Gene.LEVEL_NONE, true);
            newSet.put(Integer.valueOf(currPos - 1), mod);
          }
          remainingSlide -= reduction;   
        } else { // Not a holder? Needs to be dealt with
          moved = new DBGeneRegion(onLeft.getStringID(), onLeft.getStartPad() - remainingSlide, 
                                   onLeft.getEndPad() - remainingSlide, onLeft.getEvidenceLevel(), onLeft.isHolder());
          newSet.put(Integer.valueOf(currPos - 1), moved);        
        }
        // Guys to left just get slid 
        for (int i = 0; i < currPos - 1; i++) {
          orig = origList_.get(i);
          // Left shoulder is reduced or eliminated
          if ((i == 0) && orig.isHolder()) {
            int holderWidth = orig.getWidth();
            SortedSet<Integer> pads = padUse.get(orig.getKey());
            int required = (pads == null) ? 0 : pads.size();
            int preferred = holderWidth - remainingSlide;
            int remains = Math.max(preferred, required);
            int reduction = holderWidth - remains;
            if (reduction != remainingSlide) {
              throw new IllegalStateException();
            }
            if (remains > 0) {            
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad(), orig.getEndPad() - reduction, Gene.LEVEL_NONE, true);
              newSet.put(Integer.valueOf(0), mod);
            } // else the holder is ditched
          } else {                  
            moved = new DBGeneRegion(orig.getStringID(), orig.getStartPad() - remainingSlide, 
                                     orig.getEndPad() - remainingSlide, orig.getEvidenceLevel(), orig.isHolder());
            newSet.put(Integer.valueOf(i), moved);
          }
        }
        // Guys to right get pulled or stay in place
        for (int i = currPos + 1; i < num; i++) {
          orig = origList_.get(i);
          if (pullIt) {
            moved = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + slideAmt, 
                                     orig.getEndPad() + slideAmt, orig.getEvidenceLevel(), orig.isHolder());   
          } else {
            moved = orig.clone();
          }
          newSet.put(Integer.valueOf(i), moved);
        } 
      } else { // Going right!
        if (currPos == (num - 1)) { // We cannot be going right if we are the last
          throw new IllegalStateException();
        }
        int remainingSlide = slideAmt;
        DBGeneRegion onRight = origList_.get(currPos + 1);
        if (onRight.isHolder()) {
          int holderWidth = onRight.getWidth();
          SortedSet<Integer> pads = padUse.get(onRight.getKey());
          int required = (pads == null) ? 0 : pads.size();
          int preferred = holderWidth - remainingSlide;
          int remains = Math.max(preferred, required);
          int reduction = holderWidth - remains;
          if (remains > 0) {
            DBGeneRegion mod = new DBGeneRegion(onRight.getStringID(), onRight.getStartPad() + slideAmt, onRight.getEndPad() + slideAmt - reduction, Gene.LEVEL_NONE, true);
            newSet.put(Integer.valueOf(currPos + 1), mod);
          }  
          remainingSlide -= reduction;   
        } else { // Not a holder? Needs to be dealt with
          moved = new DBGeneRegion(onRight.getStringID(), onRight.getStartPad() + remainingSlide, 
                                   onRight.getEndPad() + remainingSlide, onRight.getEvidenceLevel(), onRight.isHolder());
          newSet.put(Integer.valueOf(currPos + 1), moved);        
        }
        // Guys to right just get slid 
        for (int i = currPos + 2; i < num; i++) {
          orig = origList_.get(i);
          if ((i == (num - 1)) && orig.isHolder()) {    
            int holderWidth = orig.getWidth();
            SortedSet<Integer> pads = padUse.get(orig.getKey());
            int required = (pads == null) ? 0 : pads.size();
            int preferred = holderWidth - remainingSlide;
            int remains = Math.max(preferred, required);
            int reduction = holderWidth - remains;
            if (reduction != remainingSlide) {
              throw new IllegalStateException();
            }
            if (remains > 0) {
              DBGeneRegion mod = new DBGeneRegion(orig.getInternalName(), orig.getStartPad() + remainingSlide, orig.getEndPad(), Gene.LEVEL_NONE, true);
              newSet.put(Integer.valueOf(num - 1), mod);
            } // else the holder is ditched
          } else {
            moved = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + remainingSlide, 
                                     orig.getEndPad() + remainingSlide, orig.getEvidenceLevel(), orig.isHolder());
            newSet.put(Integer.valueOf(i), moved);
          }
        }
        // Guys to left get pulled or stay in place
        for (int i = 0; i < currPos; i++) {
          orig = origList_.get(i);
          if (pullIt) {
            moved = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + slideAmt, 
                                     orig.getEndPad() + slideAmt, orig.getEvidenceLevel(), orig.isHolder());   
          } else {
            moved = orig.clone();
          }
          newSet.put(Integer.valueOf(i), moved);
        }
      }
      
      for (DBGeneRegion nreg : newSet.values()) {
        newRegs.add(nreg);
      }     
 
      Gene gene = dacx_.getCurrentGenome().getGene(geneID_);    
      newRegs = DBGeneRegion.fillGapsWithHolders(newRegs, gene);
      newRegs = DBGeneRegion.mergeHolders(newRegs, oldToNew);

      return (newRegs);
    }
    
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private List<DBGeneRegion> processForMove(CisModuleEditDialogFactory.CisModRequest cxrq, 
                                              Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) { 
      
      //
      // When we move, we just shift everybody around to stay within the same bounds.
      //
      
      List<DBGeneRegion> newRegs = new ArrayList<DBGeneRegion>();
      int num = origList_.size();
      List<DBGeneRegion.DBRegKey> pair = cxrq.movePair;
      Set<Integer> avoid = DBGeneRegion.getExistingHolderKeys(origList_);
      
      
      int currPos = DBGeneRegion.regionIndex(origList_, cxrq.selectedKey);
      
      //
      // If one of the pair is null, we are working with a list with no shoulder holder
      // region, and need to stick the region on the end. If the pair is not contiguous
      // we are dealing with inserting the region into a holder region, splitting the
      // region into two, unless the holder is only one pad wide.
      //
     
      boolean onEnd = (pair.size() == 1);
      
      int lxPos;
      int rxPos;
      boolean inHolder = false;
      if (!onEnd) {
        lxPos = DBGeneRegion.regionIndex(origList_, pair.get(0));
        rxPos = DBGeneRegion.regionIndex(origList_, pair.get(1));
        inHolder = (rxPos != (lxPos + 1));
      }
      
      
      DBGeneRegion.DBRegKey leftEndKey = origList_.get(0).getKey();
      DBGeneRegion.DBRegKey rightEndKey = origList_.get(num - 1).getKey();
      
      boolean putOnLeft = false;
      boolean putOnRight = false;
      if (onEnd) {
        if (pair.get(0).equals(leftEndKey)) {
          putOnLeft = true;
        } else if (pair.get(0).equals(rightEndKey)) {
          putOnRight = true;
        } else {
          throw new IllegalStateException();
        }
      }
      
      DBGeneRegion currReg = origList_.get(currPos);
      int width = currReg.getWidth();
      int diff = width - 1;
           
      //
      // If we are putting it on the left where there is no holder, that situation will not change: no new holder. Just
      // put on left, shove everybody to right:
      //
        
      if (putOnLeft) {
        DBGeneRegion orig = origList_.get(0);
        int lPos = orig.getStartPad();
        DBGeneRegion mod = new DBGeneRegion(currReg.getStringID(), lPos, lPos + diff, currReg.getEvidenceLevel(), currReg.isHolder());
        newRegs.add(mod);
        for (int i = 0; i < num; i++) {
          if (i < currPos) {
            DBGeneRegion reg = origList_.get(i);
            mod = new DBGeneRegion(reg.getStringID(), reg.getStartPad() + width, reg.getEndPad() + width, reg.getEvidenceLevel(), reg.isHolder());
            newRegs.add(mod);
          } else if (i == currPos) {
            continue;
          } else {
            newRegs.add(origList_.get(i).clone());
          }
        }
      } else if (putOnRight) {
        int rPos = Integer.MIN_VALUE;
        for (int i = 0; i < num; i++) {
          if (i < currPos) {
            newRegs.add(origList_.get(i).clone());
          } else if (i == currPos) {
            continue;
          } else {
            DBGeneRegion reg = origList_.get(i);
            DBGeneRegion mod = new DBGeneRegion(reg.getStringID(), reg.getStartPad() - width, reg.getEndPad() - width, reg.getEvidenceLevel(), reg.isHolder());
            newRegs.add(mod);
            rPos = reg.getEndPad();
          }
        }
        DBGeneRegion mod = new DBGeneRegion(currReg.getStringID(), rPos - diff, 
                                            rPos, currReg.getEvidenceLevel(), currReg.isHolder());
        newRegs.add(mod);
      } else { // Inserting between two regions
        int lPos = DBGeneRegion.regionIndex(origList_, pair.get(0));
        int rPos = DBGeneRegion.regionIndex(origList_, pair.get(1));
        
        boolean shiftToLeft = false;
        boolean shiftToRight = false;
        if (rPos < currPos) {
          shiftToLeft = true;
        } else if (lPos > currPos){
          shiftToRight = true;
        } else {
          throw new IllegalStateException();
        }
        
        if (shiftToLeft) {  
          DBGeneRegion splitReg = null;
          for (int i = 0; i < num; i++) {
            if (i <= lPos) {
              newRegs.add(origList_.get(i).clone());
            } else if (i == rPos) {
              DBGeneRegion reg = origList_.get(i);         
              int currLoc = reg.getStartPad();
              int holderLeftW = 0;
              int holderRightW = 0;
              if (inHolder && (splitReg.getWidth() > 1)) { // Insert half of original holder
                holderLeftW = (splitReg.getWidth() / 2) + 1;
                holderRightW = splitReg.getWidth() - holderLeftW;
                currLoc = splitReg.getStartPad() + holderLeftW;
                DBGeneRegion mod = new DBGeneRegion(splitReg.getStringID(), splitReg.getStartPad(), 
                                                    currLoc - 1, Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              }            
              DBGeneRegion mod = new DBGeneRegion(currReg.getStringID(), currLoc, currLoc + width - 1, currReg.getEvidenceLevel(), currReg.isHolder());
              newRegs.add(mod);          
              if (inHolder) {
                mod = new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), currLoc + width, currLoc + width + holderRightW - 1, Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              }
              mod = new DBGeneRegion(reg.getStringID(), reg.getStartPad() + width, reg.getEndPad() + width, reg.getEvidenceLevel(), reg.isHolder());
              newRegs.add(mod);
            } else if ((i > lPos) && (i < rPos)) {
              if (!inHolder) {
                throw new IllegalStateException();
              }
              splitReg = origList_.get(i);        
            } else if (i < currPos) {
              DBGeneRegion reg = origList_.get(i);          
              DBGeneRegion mod = new DBGeneRegion(reg.getStringID(), reg.getStartPad() + width, reg.getEndPad() + width, reg.getEvidenceLevel(), reg.isHolder());
              newRegs.add(mod);
            } else if (i == currPos) {
              continue;
            } else if (i > currPos) {
              newRegs.add(origList_.get(i).clone());
            } else {
              throw new IllegalStateException();
            }
          }
        } else if (shiftToRight) {
          DBGeneRegion splitReg = null;
          for (int i = 0; i < num; i++) {
            if (i < currPos) {
              newRegs.add(origList_.get(i).clone());
            } else if (i == currPos) {
              continue;
            } else if (i <= lPos) {
              DBGeneRegion reg = origList_.get(i);            
              DBGeneRegion mod = new DBGeneRegion(reg.getStringID(), reg.getStartPad() - width, reg.getEndPad() - width, reg.getEvidenceLevel(), reg.isHolder());
              newRegs.add(mod);
            } else if (i == rPos) {
              DBGeneRegion reg = origList_.get(i);
              int currLoc = reg.getStartPad() - width;
              int holderLeftW = 0;
              int holderRightW = 0;
              if (inHolder && (splitReg.getWidth() > 1)) { // Insert half of original holder
                holderLeftW = (splitReg.getWidth() / 2) + 1;
                holderRightW = splitReg.getWidth() - holderLeftW;
                currLoc = splitReg.getStartPad() - width + holderLeftW;
                DBGeneRegion mod = new DBGeneRegion(splitReg.getStringID(), splitReg.getStartPad() - width, 
                                                    currLoc - 1, Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              }            
              DBGeneRegion mod = new DBGeneRegion(currReg.getStringID(), currLoc, currLoc + width - 1, currReg.getEvidenceLevel(), currReg.isHolder());
              newRegs.add(mod);             
              if (inHolder) {
                mod = new DBGeneRegion(DBGeneRegion.getNextHolderKey(newRegs, avoid), currLoc + width, currLoc + width + holderRightW - 1, Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              }
              newRegs.add(reg.clone());
            } else if (i > rPos) {
              newRegs.add(origList_.get(i).clone());  
            } else if ((i > lPos) && (i < rPos)) {
              if (!inHolder) {
                throw new IllegalStateException();
              }
              splitReg = origList_.get(i);
            } else {  
              throw new IllegalStateException();
            }
          }
        }
      }
      
      Gene gene = dacx_.getCurrentGenome().getGene(geneID_);    
      newRegs = DBGeneRegion.fillGapsWithHolders(newRegs, gene);
      newRegs = DBGeneRegion.mergeHolders(newRegs, oldToNew);

      return (newRegs);
    }
    
    /***************************************************************************
    **
    ** Bundle for rename
    */
      
    private List<DBGeneRegion> processForNameEvidence(CisModuleEditDialogFactory.CisModRequest cxrq, 
                                                      Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) {
      
      
            
      List<DBGeneRegion> newRegs = new ArrayList<DBGeneRegion>();
      int num = origList_.size();   
      int currPos = DBGeneRegion.regionIndex(origList_, cxrq.selectedKey);
      
      for (int i = 0; i < num; i++) {
        DBGeneRegion orig = origList_.get(i);
        if (i != currPos) {
          if (DataUtil.normKeyWithGreek(orig.getName()).equals(DataUtil.normKeyWithGreek(cxrq.newName))) {
            throw new IllegalArgumentException();
          } else {
            newRegs.add(orig.clone());
          }
        } else {
          DBGeneRegion mod = new DBGeneRegion(cxrq.newName, orig.getStartPad(), orig.getEndPad(), cxrq.newEvidence, false);
          newRegs.add(mod);
          if (!cxrq.newName.equals(orig.getName())) {
            oldToNew.put(mod.getKey(), orig.getKey());           
          }
        }
      } 

      return (newRegs);
    } 

    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private List<DBGeneRegion> processForSize(CisModuleEditDialogFactory.CisModRequest cxrq, 
                                              Map<DBGeneRegion.DBRegKey, DBGeneRegion.DBRegKey> oldToNew) {
     
      //
      // These are the shifts to apply to the boundaries of the specified current region:
      //
      
      int leftMove = cxrq.resizeLeftAmt;
      int rightMove = cxrq.resizeRightAmt;
      
      List<DBGeneRegion> newRegs = new ArrayList<DBGeneRegion>();
      int num = origList_.size();   
      int currPos = DBGeneRegion.regionIndex(origList_, cxrq.selectedKey);
      
      //
      // If we select shrink, and we are right next to a holder, we can blow the holder away if the
      // user chooses a width as big or bigger than the holder:
      //
      
      boolean ditchLeftHolder = (currPos > 0) && (origList_.get(currPos - 1).isHolder()) && (-leftMove >= origList_.get(currPos - 1).getWidth());
      int leftDitch = (ditchLeftHolder) ?  origList_.get(currPos - 1).getWidth() : 0;
      boolean ditchRightHolder = (currPos < (num - 1)) && (origList_.get(currPos + 1).isHolder()) && (rightMove >= origList_.get(currPos + 1).getWidth());
      int rightDitch = (ditchRightHolder) ?  origList_.get(currPos + 1).getWidth() : 0;
 
      //
      // Crank thru all the regions from left to right, change regions as needed to accommodate
      // left or right shift
      //
      for (int i = 0; i < num; i++) {
        if (i < currPos) {
          // If shift left, everybody moves:
          if (cxrq.shiftLeftOnMove) {
            DBGeneRegion orig = origList_.get(i);
            if ((i == 0) && orig.isHolder()) {
              int holderWidth = orig.getWidth();
              if (holderWidth > -leftMove) {
                DBGeneRegion mod = new DBGeneRegion(orig.getInternalName(), orig.getStartPad(), orig.getEndPad() + leftMove, Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              } // else the holder is ditched
            } else {
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + leftMove, 
                                                  orig.getEndPad() + leftMove, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            }
          // If shrink left, only neighbor resizes, with possible loss of holder:
          } else if (cxrq.shrinkLeftOnMove) {
            if (ditchLeftHolder && (i >= currPos - 2) && (i < currPos)) {
              if (i == (currPos - 2)) {
                DBGeneRegion orig = origList_.get(i);
                DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad(), 
                                                    orig.getEndPad() + leftMove + leftDitch , orig.getEvidenceLevel(), orig.isHolder());
                newRegs.add(mod);
              } else if (i == (currPos - 1)) {  
                continue; // Drop the holder               
              }
            } else if (i == currPos - 1) {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad(), 
                                                  orig.getEndPad() + leftMove, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            } else {
              newRegs.add(origList_.get(i).clone());
            }
          }
        } else if (i == currPos) {
          DBGeneRegion orig = origList_.get(i);
          DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + leftMove, 
                                              orig.getEndPad() + rightMove, orig.getEvidenceLevel(), orig.isHolder());
          newRegs.add(mod);
        } else if (i > currPos) {
          // If shift right, everybody moves. But shoulder holders might get ditched: 
          if (cxrq.shiftRightOnMove) {
            DBGeneRegion orig = origList_.get(i);
            if ((i == (num - 1)) && orig.isHolder()) {
              int holderWidth = orig.getWidth();
              if (holderWidth > rightMove) {
                DBGeneRegion mod = new DBGeneRegion(orig.getInternalName(), orig.getStartPad() + rightMove, orig.getEndPad(), Gene.LEVEL_NONE, true);
                newRegs.add(mod);
              } // else the holder is ditched
            } else {
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + rightMove, 
                                                  orig.getEndPad() + rightMove, orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            }
          // If shrink right, only neighbor resizes:
          } else if (cxrq.shrinkRightOnMove) {
            if (ditchRightHolder && (i > currPos) && (i <= currPos + 2)) {
              if (i == (currPos + 2)) {
                DBGeneRegion orig = origList_.get(i);
                DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + rightMove - rightDitch,
                                                    orig.getEndPad() , orig.getEvidenceLevel(), orig.isHolder());
                newRegs.add(mod);
              } else if (i == (currPos + 1)) {  
                continue; // Drop the holder
              }
            } else if (i == currPos + 1) {
              DBGeneRegion orig = origList_.get(i);
              DBGeneRegion mod = new DBGeneRegion(orig.getStringID(), orig.getStartPad() + rightMove, 
                                                  orig.getEndPad(), orig.getEvidenceLevel(), orig.isHolder());
              newRegs.add(mod);
            } else {
              newRegs.add(origList_.get(i).clone());
            }
          }
        } else {
          throw new IllegalStateException();
        }
      }
      
      Gene gene = dacx_.getCurrentGenome().getGene(geneID_);    
      newRegs = DBGeneRegion.fillGapsWithHolders(newRegs, gene);
      newRegs = DBGeneRegion.mergeHolders(newRegs, oldToNew);
      return (newRegs);
    }
  }
}
