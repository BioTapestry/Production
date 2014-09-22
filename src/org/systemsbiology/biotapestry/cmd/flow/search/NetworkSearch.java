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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector;
import org.systemsbiology.biotapestry.ui.dialogs.NetworkSearchDialogFactory;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Handle Network Searches
*/

public class NetworkSearch extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public enum MatchTypes {FULL_MATCH_, PARTIAL_MATCH_};
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetworkSearch(BTState appState) {
    super(appState);
    name = "command.NodeSearch";
    desc = "command.NodeSearch";
    icon = "Find24.gif"; 
    mnem = "command.NodeSearchMnem";
    accel = "command.NodeSearchAccel";  
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
    return (cache.genomeNotNull());
  }
 
  /***************************************************************************
  **
  ** Handle out-of-band question/response
  ** 
  */
  
  @Override
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
    NetworkSearchState nss = (NetworkSearchState)cms;
    if (qbom.getLabel().equals("queBombFindNodeMatches")) {
      return (nss.queBombFindNodeMatches(qbom));
    } else if (qbom.getLabel().equals("queBombFindModuleMatches")) {
      return (nss.queBombFindModuleMatches(qbom));
    } else {
      throw new IllegalArgumentException();
    }
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
    NetworkSearchState ans = (NetworkSearchState)last.currStateX;
    return (ans.generateVizResult());
  }
  
  /***************************************************************************
  **
  ** For progammatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {
    return (new NetworkSearchState(appState_, dacx));  
  }  
  
  /***************************************************************************
  **
  ** The new interface
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next = null;
    while (true) {
      if (last == null) {
        NetworkSearchState nss = new NetworkSearchState(appState_, cfh.getDataAccessContext());
        nss.sr = null;
        SUPanel sup = appState_.getSUPanel();
        nss.currOvr = nss.rcxT_.oso.getCurrentOverlay();
        nss.linksHidden = sup.linksAreHidden(nss.rcxT_);   
        nss.selectedID = appState_.getGenomePresentation().getSingleNodeSelection(nss.rcxT_);
        nss.sres = null;
        nss.matchMods = null;
        nss.clearCurrent = false; 
        next = nss.getDialog(cfh);    
      } else {
        NetworkSearchState ans = (NetworkSearchState)last.currStateX;
        if (ans.getNextStep().equals("injectUserInputs")) {
          next = ans.injectUserInputs(last);
        } else if (ans.getNextStep().equals("processCommand")) {
          next = ans.processCommand(next);
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
        
  public static class NetworkSearchState implements DialogAndInProcessCmd.CmdState {
     
    private NetworkSearchDialogFactory.SearchRequest sr;
    private String currOvr;
    private boolean linksHidden;
    private DataAccessContext rcxT_;
    private String selectedID;
    private String nextStep_;
    //----------------- Result
    private SourceAndTargetSelector.SearchResult sres;
    private Set<String> matchMods;
    private boolean clearCurrent;
  
    public String getNextStep() {
      return (nextStep_);
    }
     
    NetworkSearchState(BTState appState, DataAccessContext dacx) {
      rcxT_ = dacx;     
    }   
    
    /***************************************************************************
    **
    ** Get the search dialog
    ** 
    */
    
    DialogAndInProcessCmd getDialog(ServerControlFlowHarness cfh) {
      String selectedName = (selectedID == null) ? null : rcxT_.getGenome().getNode(selectedID).getName().trim();   
      NetworkSearchDialogFactory.SearchDialogBuildArgs sdba = 
        new NetworkSearchDialogFactory.SearchDialogBuildArgs(rcxT_.getGenome(), selectedName, currOvr, linksHidden);
      NetworkSearchDialogFactory nsdf = new NetworkSearchDialogFactory(cfh);
      this.nextStep_ = "injectUserInputs";
      return (new DialogAndInProcessCmd(nsdf.getDialog(sdba), this));
    }
    
    /***************************************************************************
    **
    ** Install user inputs
    */
    
    DialogAndInProcessCmd injectUserInputs(DialogAndInProcessCmd cmd) {     
      sr = (NetworkSearchDialogFactory.SearchRequest)cmd.cfhui;     
      nextStep_ = "processCommand"; 
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      retval.dialog = cmd.dialog;
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Do the search
    */
     
    DialogAndInProcessCmd processCommand(DialogAndInProcessCmd curr) {
      clearCurrent = sr.clearCurrent;
      if (sr.whichTab == 0) {
        SourceAndTargetSelector sats = new SourceAndTargetSelector(rcxT_);
        sres = sats.doSelection(sr.found, sr.selectionType, sr.includeItem, sr.includeLinks);
      } else {    
        NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
        Map<String, Set<String>> allMatchMods = owner.findMatchingNetworkModules(sr.searchMode, sr.tagVal, sr.nvp);
        matchMods = allMatchMods.get(currOvr);
      }
      
      Map<String,Object> cmdResults = new HashMap<String,Object>();
            
      DialogAndInProcessCmd result = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this,cmdResults);

      if((sr.whichTab == 0 && (sres == null || sres.found == null || sres.found.size() <= 0)) || (sr.whichTab == 1 && (matchMods == null || matchMods.size() <= 0))) {
      	  result.state = DialogAndInProcessCmd.Progress.SIMPLE_USER_FEEDBACK;
      	  Map<String,String> clickActions = new HashMap<String,String>();
      	  clickActions.put("ok","MAIN_NETWORK_SEARCH");    	  
      	  result.suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rcxT_.rMan.getString("nsearch.nothingFound"), 
  			  rcxT_.rMan.getString("nsearch.nothingFoundTitle"),clickActions);
      	  
      	  this.nextStep_ = "injectUserInputs";
      } else {
    	  if (sr.whichTab == 0) {
    		  Map<String,Object> results = new HashMap<String,Object>();
    		  sres.mapToResults(results);
     
    		  cmdResults.put("SearchResults", results);
    		  if(!clearCurrent) {
    			  cmdResults.put("AppendToSelection", true);
    		  }
    	  } else {
    		  cmdResults.put("SearchResults", new TaggedSet(TaggedSet.UNTAGGED, matchMods));
    	  }
      }
      
      result.dialog = curr.dialog;
      
      return (result);
    }
      
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    RemoteRequest.Result queBombFindNodeMatches(RemoteRequest qbom) {     
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String search = qbom.getStringArg("search");
      search = DataUtil.normKey(search); // Moved here from the desktop dialog; but superfluous (see matches() below)
      NetworkSearch.MatchTypes matchType = (NetworkSearch.MatchTypes)qbom.getObjectArg("matchType");
   
      HashSet<String> found = new HashSet<String>();
      Iterator<Node> nit = rcxT_.getGenome().getAllNodeIterator();
      while (nit.hasNext()) {
        Node node = nit.next();
        if (matches(search, node.getName(), matchType)) {
          found.add(node.getID());
        }
      }
      result.setSetAnswer("found", found);
      return (result);
    }
    
    /***************************************************************************
    **
    ** Process a QueBomb
    */
    
    RemoteRequest.Result queBombFindModuleMatches(RemoteRequest qbom) {     
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      
      String tagVal = qbom.getStringArg("tagVal");
      NameValuePair nvp = (NameValuePair)qbom.getObjectArg("nvp");
      int mode = (tagVal != null) ? NetOverlayOwner.MODULE_BY_KEY : NetOverlayOwner.MODULE_BY_NAME_VALUE;
        
      NetOverlayOwner owner = rcxT_.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxT_.getGenomeID());
      Map<String, Set<String>> allMatchMods = owner.findMatchingNetworkModules(mode, tagVal, nvp);
      matchMods = allMatchMods.get(currOvr);
      
      HashSet<String> found = new HashSet<String>();
      if (matchMods != null) {
        found.addAll(matchMods);
      }
      result.setSetAnswer("found", found);
      return (result);
    }   
  
    /***************************************************************************
    **
    ** Do the match
    ** 
    */
    
    private boolean matches(String searchString, String nodeName, NetworkSearch.MatchTypes matchType) {
    	String searchStringNorm = DataUtil.normKey(searchString);
      String canonicalNodeName = DataUtil.normKey(nodeName);
      switch (matchType) {
        case FULL_MATCH_:
          return (canonicalNodeName.equals(searchStringNorm));
        case PARTIAL_MATCH_:
          return (canonicalNodeName.indexOf(searchStringNorm) != -1);
        default:
          throw new IllegalArgumentException();
      }
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
          retval.setSelections(sres.found, sres.linkIntersections, clearCurrent);
          nodeFound = true;
        } else {
          // Catching no selections is now a client side thing....
          retval.addChange(VisualChangeResult.ViewChange.CLEAR_SELECTIONS);
          retval.setUndoString("undo.selection");
        }
      } else {
        retval.addChange(VisualChangeResult.ViewChange.OVERLAY_STATE);
        retval.setUndoString("undo.networkModuleSearch");
        retval.setModuleMatches(new TaggedSet(TaggedSet.UNTAGGED, matchMods));
      }
      retval.setViewport((nodeFound) ? VisualChangeResult.Viewports.SELECTED : VisualChangeResult.Viewports.NO_CHANGE);       
      return (retval);
    }
  }
 

}
