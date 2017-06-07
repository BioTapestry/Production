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


package org.systemsbiology.biotapestry.cmd.flow.display;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.dialogs.PathDisplayFrameFactory;

/****************************************************************************
**
** Handle some info providers.
*/

public class DisplayPaths extends AbstractControlFlow {

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum InfoType {
    LINK_PATHS("linkPopup.AnalyzePaths", "linkPopup.AnalyzePaths", "FIXME24.gif", "linkPopup.AnalyzePathsMnem", null),
    PATHS_FOR_NODE(),
    
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
  private String srcID_;
  private String trgID_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DisplayPaths(InfoType action) {
    if (action == InfoType.PATHS_FOR_NODE) {
      throw new IllegalArgumentException();    
    } 
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DisplayPaths(InfoType action, PathAnalysisArgs paargs) {
    if (action != InfoType.PATHS_FOR_NODE) {
      throw new IllegalArgumentException();    
    }
    name = paargs.getNodeName();
    desc = paargs.getNodeName();
    srcID_ = paargs.getSrcID();
    trgID_ = paargs.getTargID();
    action_ = action;
  }

  /***************************************************************************
  **
  ** Answer if we are enabled for a popup case
  ** 
  */
    
  @Override
  public boolean isValid(Intersection inter, boolean isSingleSeg, boolean canSplit, 
                         DataAccessContext rcx, UIComponentSource uics) {
    switch (action_) {
      case LINK_PATHS:
        if (!isSingleSeg) {
          return (false);
        }     
        LinkProperties lp = rcx.getCurrentLayout().getLinkProperties(inter.getObjectID());
        LinkSegmentID segID = inter.segmentIDFromIntersect();       
        Set<String> resolved = lp.resolveLinkagesThroughSegment(segID);
        return (resolved.size() == 1);
      case PATHS_FOR_NODE:
        return (true);
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
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {  
    return (new StepState(action_, srcID_, trgID_, dacx));
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
        ans = new StepState(action_, srcID_, trgID_, cfh);
      } else {
        ans = (StepState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
      }
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();
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

    private InfoType myAction_;
    private Intersection inter_;
    private String mySrcID_;
    private String myTargID_;
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(InfoType action, String sourceID, String targID, StaticDataAccessContext dacx) {
      super(dacx);
      myAction_ = action;
      mySrcID_ = sourceID;
      myTargID_ = targID;
      nextStep_ = "stepToProcess";
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(InfoType action, String sourceID, String targID, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      mySrcID_ = sourceID;
      myTargID_ = targID;
      nextStep_ = "stepToProcess";
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
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() { 
      LinkProperties lp;
      LinkSegmentID segID;       
      Set<String> resolved;
      String src;
      String trg;
           
      switch (myAction_) {
        case LINK_PATHS:      
          lp = dacx_.getCurrentLayout().getLinkProperties(inter_.getObjectID());
          segID = inter_.segmentIDFromIntersect();
          resolved = lp.resolveLinkagesThroughSegment(segID);
          if (resolved.size() > 1) {
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          String linkID = resolved.iterator().next();
          Linkage link = dacx_.getCurrentGenome().getLinkage(linkID);
          src = link.getSource();
          trg = link.getTarget();
          break;
        case PATHS_FOR_NODE:  
          src = mySrcID_;
          trg = myTargID_;
          break;
        default:
          throw new IllegalStateException();
      }
      
      PathDisplayFrameFactory.BuildArgs ba = new PathDisplayFrameFactory.BuildArgs(dacx_.getCurrentGenomeID(), src, trg);
      PathDisplayFrameFactory nocdf = new PathDisplayFrameFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = nocdf.getDialog(ba);
      // This dialog is NOT modal, so we are done:
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = null;
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Arguments
  */
  
  public static class PathAnalysisArgs extends AbstractOptArgs {
    
    private static String[] keys_;
    
    static {
      keys_ = new String[] {"targID", "srcID", "nodeName"};  
    }
    
    public String getTargID() {
      return (getValue(0));
    }
     
    public String getSrcID() {
      return (getValue(1));
    }
   
    public String getNodeName() {
      return (getValue(2));
    }
    
    public PathAnalysisArgs(Map<String, String> argMap) throws IOException {
      super(argMap);
    }
  
    protected String[] getKeys() {
      return (keys_);
    }
    
    public PathAnalysisArgs(String targID, String srcID, String nodeName) {
      super();
      setValue(0, targID);
      setValue(1, srcID);
      setValue(2, nodeName);
      bundle();
    }
  } 
}
