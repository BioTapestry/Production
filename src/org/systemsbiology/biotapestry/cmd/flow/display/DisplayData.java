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


package org.systemsbiology.biotapestry.cmd.flow.display;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.DataPopupManager;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.dialogs.ChooseLinkToViewDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.UsageFrameFactory;

/****************************************************************************
**
** Handle some info providers.
*/

public class DisplayData extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum InfoType {
    LINK("linkPopup.experimentalData", "linkPopup.experimentalData", "FIXME24.gif", "linkPopup.experimentalDataMnem", null),
    NODE_USAGES("nodePopup.FindUsages", "nodePopup.FindUsages", "FIXME24.gif", "nodePopup.FindUsagesMnem", null),
    LINK_USAGES("linkPopup.FindUsages", "linkPopup.FindUsages", "FIXME24.gif", "linkPopup.FindUsagesMnem", null),
    NODE("genePopup.ExpData", "genePopup.ExpData", "FIXME24.gif", "genePopup.ExpDataMnem", null),
    PENDING_NODE(),
    PENDING_LINK(),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    InfoType(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
    }
    
    InfoType() {
      this.name_ = null;  
      this.desc_ = null;
      this.icon_ = null;
      this.mnem_ = null;
      this.accel_ = null;
    }  

    public String getName() {
      return (name_);      
    }
     
    public String getDesc() {
      return (desc_);      
    }
     
    public String getIcon() {
      return (icon_);      
    }
     
    public String getMnem() {
      return (mnem_);      
    }
     
    public String getAccel() {
      return (accel_);      
    }     
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  private InfoType action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DisplayData(BTState appState, InfoType action) {
    super(appState);
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
  
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, DataAccessContext rcx) {
    switch (action_) {    
      case NODE_USAGES:  
      case NODE:
        return (true);
      case LINK:
      case LINK_USAGES:
        if (!isSingleSeg) {
         return (false);
        }   
        return (true);
      case PENDING_NODE:
      case PENDING_LINK:
       return (false);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(DataAccessContext dacx) {  
    return (new StepState(appState_, action_, dacx));
  }     
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
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
        ans = new StepState(appState_, action_, cfh.getDataAccessContext());        
      } else {
        ans = (StepState)last.currStateX;
      }
      ans.cfh = cfh;
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
      } else if(ans.getNextStep().equals("injectUserInputs")) {
    	 next = ans.injectUserInputs(last);
      } else if(ans.getNextStep().equals("processCommand")) {
     	 next = ans.processCommand();
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
        
  public static class StepState implements DialogAndInProcessCmd.PopupCmdState {

    private String nextStep_;
    private InfoType myAction_;
    private BTState appState_;
    private Intersection inter_;
    private ServerControlFlowHarness cfh;
    private DataAccessContext dacx_;
    private String lazyGenomeID_;
    private String lazyObjectID_;

    // Result for link
    private ChooseLinkToViewDialogFactory.LinkRequest lr_;
    
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, InfoType action, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      dacx_ = dacx;
    }
     
    /***************************************************************************
    **
    ** for preload
    */ 
      
    public void setIntersection(Intersection inter) { 
      inter_ = inter;
      return;
    }  
    
    /***************************************************************************
    **
    ** for lazy data
    */ 
      
    public void setLazyData(String genomeID, String objectID) { 
      lazyGenomeID_ = genomeID;
      lazyObjectID_ = objectID;
      return;
    }  
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() { 
      LinkProperties lp;
      LinkSegmentID segID;       
      Set<String> resolved;
      HashMap<String, Object> serverOutputMap = new HashMap<String, Object>();
           
      switch (myAction_) {
        case LINK:   
          lp = dacx_.getLayout().getLinkProperties(inter_.getObjectID());
          segID = inter_.segmentIDFromIntersect();       
          resolved = lp.resolveLinkagesThroughSegment(segID);
          String linkID = null;
          if (resolved.size() > 1) {
            boolean isRoot = (dacx_.getGenome() instanceof DBGenome);
            
            ChooseLinkToViewDialogFactory.BuildArgs ba = new ChooseLinkToViewDialogFactory.BuildArgs(
        		  dacx_.getGenome(),dacx_.getGenomeID(),resolved,isRoot,inter_.getObjectID());
            return (this.getDialog(cfh,ba));
          } else {
            linkID = resolved.iterator().next();
          }
          Map<String,Object> results = appState_.getDataPopupMgr().popupLinkData(dacx_.getGenomeID(), linkID);
          DataPopupManager.DataReturn data = (DataPopupManager.DataReturn)results.get("contents");
       
          if (data != null) {
            serverOutputMap.put("ExperimentalData", data);
          }

          serverOutputMap.put("FrameTitle", results.get("title"));
          nextStep_ = null;
          break;
        case NODE_USAGES:  
          Node node = dacx_.getGenome().getNode(inter_.getObjectID());
          if (node != null) {
            UsageFrameFactory.BuildArgs ba = new UsageFrameFactory.BuildArgs(null, node.getID());
            UsageFrameFactory nocdf = new UsageFrameFactory(cfh);
            ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
            // This dialog is NOT modal, so we are done:
            DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
            nextStep_ = null;
            return (retval);
          }
          break;
        case LINK_USAGES:  
          lp = dacx_.getLayout().getLinkProperties(inter_.getObjectID());
          segID = inter_.segmentIDFromIntersect();
          resolved = lp.resolveLinkagesThroughSegment(segID);          
          HashSet<String> baseLinks = new HashSet<String>();
          String baseSrcID = null; 
          Iterator<String> lit = resolved.iterator();
          while (lit.hasNext()) {
            String resLinkID = lit.next();
            String baseLinkID = GenomeItemInstance.getBaseID(resLinkID);
            if (baseSrcID == null) {
              Linkage baseLink = dacx_.getDBGenome().getLinkage(baseLinkID);
              baseSrcID = baseLink.getSource();
            }
            baseLinks.add(baseLinkID);
          }
          UsageFrameFactory.BuildArgs ba = new UsageFrameFactory.BuildArgs(baseLinks, baseSrcID);
          UsageFrameFactory nocdf = new UsageFrameFactory(cfh);
          ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
          // This dialog is NOT modal, so we are done:
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
          nextStep_ = null;
          return (retval);
        case NODE:  
          String id = inter_.getObjectID();
          DataPopupManager.DataReturn result = appState_.getDataPopupMgr().popupData(dacx_.getGenomeID(), id);
          if (result != null) {
            serverOutputMap.put("ExperimentalData", result);
          }
          break;
        case PENDING_NODE:
          DataPopupManager.DataReturn pendResult = appState_.getDataPopupMgr().popupData(lazyGenomeID_, lazyObjectID_);
          if (pendResult != null) {
            serverOutputMap.put("ExperimentalData", pendResult);
          }
          break;
        case PENDING_LINK:
          Map<String,Object> pendResults = appState_.getDataPopupMgr().popupLinkData(lazyGenomeID_, lazyObjectID_);
          DataPopupManager.DataReturn pendData = (DataPopupManager.DataReturn)pendResults.get("contents");    
          if (pendData != null) {
            serverOutputMap.put("ExperimentalData", pendData);
          }
          serverOutputMap.put("FrameTitle", pendResults.get("title"));
          break;  
        default:
          throw new IllegalStateException();
      }
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this, serverOutputMap));
    }
    
    /***************************************************************************
     **
     ** Get the link to view dialog if needed
     ** 
     */
     
     DialogAndInProcessCmd getDialog(ServerControlFlowHarness cfh, ChooseLinkToViewDialogFactory.BuildArgs ba) {
    	 ChooseLinkToViewDialogFactory chsLinkFac = new ChooseLinkToViewDialogFactory(cfh);
    	 this.nextStep_ = "injectUserInputs";
    	 return (new DialogAndInProcessCmd(chsLinkFac.getDialog(ba), this));
     }
     
     /***************************************************************************
     **
     ** Retrieve response from 'link to view' dialog
     */
     
     DialogAndInProcessCmd injectUserInputs(DialogAndInProcessCmd cmd) {
    	 lr_ = (ChooseLinkToViewDialogFactory.LinkRequest)cmd.cfhui;
    	 nextStep_ = "processCommand"; 
    	 DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
    	 return (retval);
     }   
     
     
     /**
      * processCommand
      * 
      * 
      * 
      * 
      * 
      * @return
      * 
      */
     DialogAndInProcessCmd processCommand() {
    	 HashMap<String, Object> serverOutputMap = new HashMap<String, Object>();
    	 Map<String,Object> results = appState_.getDataPopupMgr().popupLinkData(dacx_.getGenomeID(), lr_.linkID);
    	 DataPopupManager.DataReturn data = (DataPopupManager.DataReturn)results.get("contents");
    	 
       if (data != null) {
      	 serverOutputMap.put("ExperimentalData", data);
       }

       serverOutputMap.put("FrameTitle", results.get("title"));
       
       nextStep_ = null;
    	 
    	 return new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this,serverOutputMap);
     }
     
  }
  

}
