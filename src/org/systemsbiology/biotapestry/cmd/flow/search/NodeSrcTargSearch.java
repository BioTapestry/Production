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


package org.systemsbiology.biotapestry.cmd.flow.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.analysis.GraphSearcher;
import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector;

/****************************************************************************
**
** Handle Network Searches
*/

public class NodeSrcTargSearch extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    

  private boolean isForSource_;
  private boolean genesOnly_;
  private boolean isForLinks_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NodeSrcTargSearch(boolean genesOnly, boolean isForSource) {
    isForSource_ = isForSource;
    isForLinks_ = false;
    if (isForSource) {
      name = (genesOnly) ? "nodePopup.FindSourcesGO" : "nodePopup.FindSources";
      desc = (genesOnly) ? "nodePopup.FindSourcesGO" : "nodePopup.FindSources";
      mnem = (genesOnly) ? "nodePopup.FindSourcesGOMnem" : "nodePopup.FindSourcesMnem";
    } else {
      name = (genesOnly) ? "nodePopup.FindTargetsGO" : "nodePopup.FindTargets";
      desc = (genesOnly) ? "nodePopup.FindTargetsGO" : "nodePopup.FindTargets";
      mnem = (genesOnly) ? "nodePopup.FindTargetsGOMnem" : "nodePopup.FindTargetsMnem";    
    }
    genesOnly_ = genesOnly;
  }
  
  /***************************************************************************
   **
   ** Constructor for links
   */ 
   
   public NodeSrcTargSearch(boolean isForSource) {
     isForSource_ = isForSource;
     isForLinks_ = true;
     if (isForSource) {
       name = "linkPopup.selectSource";
       desc = "linkPopup.selectSource";
       mnem = "linkPopup.selectSourceMnem";       
     } else {
       name = "linkPopup.selectTargets";
       desc = "linkPopup.selectTargets";
       mnem = "linkPopup.selectTargetsMnem";
     }
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
    if (isForLinks_) {
      return (true);
    }
    String oid = inter.getObjectID();
    Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String nod = (isForSource_) ? link.getTarget() : link.getSource();
      if (oid.equals(nod)) {
        return (true);
      }
    }  
    return (false);
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
    NodeSrcSearchState ans = (NodeSrcSearchState)last.currStateX;
    return (ans.generateVizResult());
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    NodeSrcSearchState retval = new NodeSrcSearchState(genesOnly_, isForSource_, isForLinks_, dacx);
    return (retval);
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
        // MUST HAVE BEEN PRELOADED!
        throw new IllegalStateException();
      } else {
        NodeSrcSearchState ans = (NodeSrcSearchState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("processCommand")) {
          next = ans.processCommand();
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
  ** Running State:
  */
         
  public static class NodeSrcSearchState extends AbstractStepState implements DialogAndInProcessCmd.PopupCmdState, GraphSearcher.CriteriaJudge {
    
    private SearchRequestPreload spr;
    private SourceAndTargetSelector.SearchResult sres;
    private boolean myGenesOnly_;
    private boolean myIsForSource_;
    private boolean myIsForLinks_;
    private Intersection intersection_;
   
    /***************************************************************************
    **
    ** Construct
    */
       
    NodeSrcSearchState(boolean genesOnly, boolean isForSource, boolean isForLinks, StaticDataAccessContext dacx) {
      super(dacx);
      myGenesOnly_ = genesOnly;
      myIsForSource_ = isForSource;
      myIsForLinks_ = isForLinks;
      nextStep_ = "processCommand";
    } 

    /***************************************************************************
    **
    ** CriteriaJudge interface:
    */    
    
    public boolean stopHere(String nodeID) {
      if (intersection_.getObjectID().equals(nodeID)) {
        return (false);
      }
      return (dacx_.getCurrentGenome().getGene(nodeID) != null);      
    }
    
    /***************************************************************************
    **
    ** for preload
    */ 
       
    public void setIntersection(Intersection inter) {
      intersection_ = inter;
      CmdSource.SearchModifiers sm = cmdSrc_.getSearchModifiers();
      if (myIsForLinks_) {
        if (myIsForSource_) {
          Linkage link = dacx_.getCurrentGenome().getLinkage(inter.getObjectID());
          String src = link.getSource();
          spr = new NodeSrcTargSearch.SearchRequestPreload(src, dacx_);
          spr.injectFromPop(false, SourceAndTargetSelector.Searches.DIRECT_SELECT, sm.includeQueryNode, false, !sm.appendToCurrent, null);   
        } else { 
          BusProperties bp = dacx_.getCurrentLayout().getLinkProperties(inter.getObjectID());
          LinkSegmentID segID = inter.segmentIDFromIntersect();
          Set<String> linksThru = bp.resolveLinkagesThroughSegment(segID);
          spr = new NodeSrcTargSearch.SearchRequestPreload(null, dacx_);    
          spr.injectLinksFromPop(linksThru, true);      
        }
      } else {   
        spr = new NodeSrcTargSearch.SearchRequestPreload(inter.getObjectID(), dacx_);
        boolean showLinks = (myGenesOnly_) ? false : sm.includeLinks && !uics_.getGenomePresentation().linksAreHidden(dacx_);      
        SourceAndTargetSelector.Searches srch = (myIsForSource_) ? SourceAndTargetSelector.Searches.SOURCE_SELECT : SourceAndTargetSelector.Searches.TARGET_SELECT;  
        spr.injectFromPop(myGenesOnly_, srch, sm.includeQueryNode, showLinks, !sm.appendToCurrent, this);  
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Do the search
    */
      
    private DialogAndInProcessCmd processCommand() {
      SourceAndTargetSelector sats = new SourceAndTargetSelector(spr.rcx);     
      if (spr.linksThru != null) {
        sres = sats.doLinkSelection(spr.linksThru);
      } else if (spr.stype == SourceAndTargetSelector.Searches.DIRECT_SELECT) {
        HashSet<String> nodeSet = new HashSet<String>();
        nodeSet.add(spr.selectedID);
        sres = sats.doSelection(nodeSet, spr.stype, spr.includeItem, spr.includeLinks);
      } else if (spr.genesOnly) {
        sres = sats.doCriteriaSelection(spr.selectedID, spr.stype, spr.includeLinks, spr.judge);
      } else { 
        HashSet<String> nodeSet = new HashSet<String>();
        nodeSet.add(spr.selectedID);
        sres = sats.doSelection(nodeSet, spr.stype, spr.includeItem, spr.includeLinks);
      }
      
      Map<String,Object> cmdResults = new HashMap<String,Object>();
      Map<String,Object> srcResults = new HashMap<String,Object>();
      sres.mapToResults(srcResults);
      cmdResults.put("SearchResults", srcResults);
      
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, cmdResults));
    }
 
    /***************************************************************************
    **
    ** Generate visual changes due to operation
    ** 
    **
    */

    private VisualChangeResult generateVizResult() {
      VisualChangeResult retval = new VisualChangeResult(true);
      boolean nodeFound = false;
      if (sres != null) {
        if (!sres.found.isEmpty()) {
          retval.addChange(VisualChangeResult.ViewChange.NEW_SELECTIONS);
          retval.setSelections(sres.found, sres.linkIntersections, spr.clearCurrent);
          nodeFound = true;
        }
      }
      retval.setViewport((nodeFound) ? VisualChangeResult.Viewports.SELECTED : VisualChangeResult.Viewports.NO_CHANGE);       
      return (retval);
    }
  }
         
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  /***************************************************************************
  **
  ** User input data preloaded before flow 
  */

  public static class SearchRequestPreload implements ServerControlFlowHarness.UserInputs {
  
    public StaticDataAccessContext rcx;
    public String selectedID;
    public Set<String> linksThru;
    public boolean genesOnly;
    public SourceAndTargetSelector.Searches stype;
    public boolean includeItem;
    public boolean includeLinks;
    public boolean clearCurrent;
    public GraphSearcher.CriteriaJudge judge;
    private boolean haveResult;
    
    
    public SearchRequestPreload(String selectedID, StaticDataAccessContext rcx) {
      this.rcx = rcx;
      this.selectedID = selectedID;
    }
      
    public void injectFromPop(boolean genesOnly, SourceAndTargetSelector.Searches stype, 
                              boolean includeItem, boolean includeLinks, boolean clearCurrent, 
                              GraphSearcher.CriteriaJudge judge) {
      this.genesOnly = genesOnly;
      this.stype = stype;
      this.includeItem = includeItem;
      this.includeLinks = includeLinks;
      this.clearCurrent = clearCurrent;
      this.judge = judge;
      this.linksThru = null;
      return;
    }
           
    public void injectLinksFromPop(Set<String> linksThru, boolean clearCurrent) {
      this.genesOnly = false;
      this.linksThru = linksThru;
      this.clearCurrent = clearCurrent;
      return;
    }
    
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
	public void setHasResults() {
		this.haveResult = true;
		return;
	}  
    public boolean haveResults() {
      return (haveResult);
    }  
    public boolean isForApply() {
      return (false);
    } 
  } 
}
