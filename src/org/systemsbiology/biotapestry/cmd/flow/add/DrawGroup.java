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


package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.MainCommands;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.DrawGroupCreationDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.SimpleRestrictedNameDialogFactory;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle Adding new Groups
*/

public class DrawGroup extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DrawGroup(BTState appState) {
    super(appState);
    name =  "command.DrawGroupInInstance";
    desc = "command.DrawGroupInInstance";
    icon = "DrawRegions24.gif";
    mnem =  "command.DrawGroupInInstanceMnem";
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
    return (cache.isNonDynamicInstance());
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
        AddGroupState ans = new AddGroupState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepBiWarning();
      } else {
        AddGroupState ans = (AddGroupState)last.currStateX;
        if (ans.getNextStep().equals("stepBiWarning")) {
          next = ans.stepBiWarning();
        } else if (ans.getNextStep().equals("stepSetToMode")) {
          next = ans.stepSetToMode();           
        } else if (ans.getNextStep().equals("stepOneForRootInstance")) {
          next = ans.stepOneForRootInstance(); 
        } else if (ans.getNextStep().equals("stepRootInstanceDataExtract")) {
          next = ans.stepRootInstanceDataExtract(last);       
        } else if (ans.getNextStep().equals("stepOneForSubsetInstance")) {
          next = ans.stepOneForSubsetInstance();
        } else if (ans.getNextStep().equals("stepSubsetInstanceDataExtract")) {
          next = ans.stepSubsetInstanceDataExtract(last);
        } else if (ans.getNextStep().equals("stepDrawGroupInInstanceStep2")) {
          next = ans.stepDrawGroupInInstanceStep2();       
        } else if (ans.getNextStep().equals("stepDrawGroupInInstanceFinish")) {   
          next = ans.stepDrawGroupInInstanceFinish();          
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
  ** Called as new X, Y info comes in to place the group
  */
  
  @Override
  public DialogAndInProcessCmd processClick(Point theClick, boolean isShifted, double pixDiam, DialogAndInProcessCmd.CmdState cmds) {
    AddGroupState ans = (AddGroupState)cmds;
    ans.x = UiUtil.forceToGridValueInt(theClick.x, UiUtil.GRID_SIZE);
    ans.y = UiUtil.forceToGridValueInt(theClick.y, UiUtil.GRID_SIZE);
    DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, ans);
    ans.nextStep_ = "stepDrawGroupInInstanceFinish"; 
    return (retval);
  }

  /***************************************************************************
  **
  ** Running State.
  */
        
  public static class AddGroupState implements DialogAndInProcessCmd.CmdState, DialogAndInProcessCmd.MouseClickCmdState {
     
    private GroupCreationSupport newHandler;    
    private Group newGroup;
    private DataAccessContext rcxTrg_;
    
    private String groupName;
    private String groupKey;

    private ServerControlFlowHarness cfh;
    private int x;
    private int y;  
    private String nextStep_;    
    private BTState appState_;
     
    /***************************************************************************
    **
    ** step thru
    */
    
    public String getNextStep() {
      return (nextStep_);
    } 
    
    /***************************************************************************
    **
    ** mouse masking
    */
    
    public int getMask() {
      return (MainCommands.ALLOW_NAV_PUSH);
    }
    
    public boolean pushDisplay() {
      return (false);
    }
    
    public boolean noSubModels() {
      return (false);
    }
        
    public boolean noInstances() {
      return (false);
    }
            
    public boolean showBubbles() {
      return (false);
    }
    
    public boolean noRootModel() {
      return (true);
    }
       
    public boolean mustBeDynamic() {
      return (false);
    }
    
    public boolean cannotBeDynamic() {
      return (true);
    }
    
    public boolean hasTargetsAndOverlays() {
      return (false);
    }
    
    public Floater floaterMode() {
      return (DialogAndInProcessCmd.MouseClickCmdState.Floater.OBJECT_FLOATER);
    }
    
    public void setFloaterPropsInLayout(Layout flay) {
      int order = flay.getTopGroupOrder() + 1;
      int groupCount = rcxTrg_.getGenomeAsInstance().groupCount();
      GroupProperties gprop = new GroupProperties(groupCount + 1, newGroup.getID(), new Point2D.Double(0.0, 0.0), order, appState_.getDB());
      flay.setGroupProperties(Group.getBaseID(newGroup.getID()), gprop);
      return;
    }
  
    public Object getFloater(int x, int y) {
      return (newGroup);
    }
    
    public Color getFloaterColor() {
      return (null); // This handler uses setFloaterProps, not this
    }
    
    /***************************************************************************
    **
    ** Construct
    */
      
    public AddGroupState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      rcxTrg_ = dacx;
    }
  
    /***************************************************************************
    **
    ** Warn of build instructions and init some values
    */
       
    private DialogAndInProcessCmd stepBiWarning() {
      DialogAndInProcessCmd doneval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
      if (rcxTrg_.getDBGenome().getID().equals(rcxTrg_.getGenomeAsInstance().getID())) {  // Only works if not root genome
        return (doneval);
      }
      DialogAndInProcessCmd daipc;
      if (appState_.getDB().haveBuildInstructions()) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("instructWarning.message");
        String title = rMan.getString("instructWarning.title");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);      
       } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      newHandler = new GroupCreationSupport(appState_, rcxTrg_, true);    
      groupName = null;
      groupKey = null;
      nextStep_ = (rcxTrg_.getGenomeAsInstance().getVfgParent() == null) ? "stepOneForRootInstance" : "stepOneForSubsetInstance";
      return (daipc);     
    }
     
    /***************************************************************************
    **
    ** Handle the control flow for drawing into root instance
    */ 
      
    private DialogAndInProcessCmd stepOneForRootInstance() {
      
      String messageKey = "createGroup.AssignMsg";
      String titleKey = "createGroup.AssignTitle";
      String defaultName = rcxTrg_.getGenomeAsInstance().getUniqueGroupName();
      Set<String> used = rcxTrg_.getGenomeAsInstance().usedGroupNames();
      
      SimpleRestrictedNameDialogFactory.RestrictedNameBuildArgs ba = 
        new SimpleRestrictedNameDialogFactory.RestrictedNameBuildArgs(titleKey, messageKey, defaultName, used, true);
      SimpleRestrictedNameDialogFactory dgcdf = new SimpleRestrictedNameDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepRootInstanceDataExtract";
      return (retval);
    } 
    
    /***************************************************************************
     **
     ** Handle the control flow for drawing into root instance
     */ 
       
     private DialogAndInProcessCmd stepRootInstanceDataExtract(DialogAndInProcessCmd cmd) {
   
       SimpleRestrictedNameDialogFactory.SimpleNameRequest crq = (SimpleRestrictedNameDialogFactory.SimpleNameRequest)cmd.cfhui;   
       if (!crq.haveResults()) {
         DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
         return (retval);
       }           
       groupName = crq.nameResult;
       nextStep_ = "stepDrawGroupInInstanceStep2";
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
       return (retval);
     } 
 
    /***************************************************************************
    **
    ** Handle the control flow for drawing into subset instance
    */ 
       
     private DialogAndInProcessCmd stepOneForSubsetInstance() {
       // If the user chooses to draw a pre-existing group in a submodel,
       // that will result in an instant add that does not require any
       // drawing mode. So we can exit immediately!
       GenomeInstance rootInstance = rcxTrg_.getGenomeAsInstance().getVfgParentRoot();
       DrawGroupCreationDialogFactory.DrawGroupBuildArgs ba = 
         new DrawGroupCreationDialogFactory.DrawGroupBuildArgs(rcxTrg_.getGenomeAsInstance(), rootInstance.getUniqueGroupName());
       DrawGroupCreationDialogFactory dgcdf = new DrawGroupCreationDialogFactory(cfh);
       ServerControlFlowHarness.Dialog cfhd = dgcdf.getDialog(ba);
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
       nextStep_ = "stepSubsetInstanceDataExtract";
       return (retval);
     } 
     
     /***************************************************************************
     **
     ** Handle the control flow for drawing into subset instance
     */ 
         
     private DialogAndInProcessCmd stepSubsetInstanceDataExtract(DialogAndInProcessCmd cmd) {
       DrawGroupCreationDialogFactory.GroupCreationRequest crq = (DrawGroupCreationDialogFactory.GroupCreationRequest)cmd.cfhui;   
       if (!crq.haveResults()) {
         DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
         return (retval);
       }         
       groupKey = crq.idResult;
       GenomeInstance rootInstance = rcxTrg_.getGenomeAsInstance().getVfgParentRoot();
       groupName = (groupKey == null) ? crq.nameResult : rootInstance.getGroup(groupKey).getName();
       nextStep_ = "stepDrawGroupInInstanceStep2";
       DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
       return (retval);
     } 
  
    /***************************************************************************
    **
    ** Install a mouse handler
    */
     
    private DialogAndInProcessCmd stepSetToMode() {       
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.INSTALL_MOUSE_MODE, this);
      retval.suPanelMode = PanelCommands.Mode.DRAW_GROUP;
      return (retval);
    }
      
    /***************************************************************************
    **
    ** Draw a group directly into an instance model.
    */  
     
    private DialogAndInProcessCmd stepDrawGroupInInstanceStep2() {     
    
      if (groupKey == null) {
        groupKey = rcxTrg_.getNextKey();
      } else {
        // We are working with a pre-existing group.  So we don't need to draw;
        // it can be included immediately.
        newGroup = new Group(rcxTrg_.rMan, groupKey, groupName);
        nextStep_ = "stepDrawGroupInInstanceFinish";
        x = 0;
        y = 0;
        stepDrawGroupInInstanceFinish();
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
    
      // Fake group lives in the top instance for the time being.  Used for floater.
      // Info will be extracted out to correctly leveled group when we are done.
      newGroup = new Group(rcxTrg_.rMan, groupKey, groupName);
      nextStep_ = "stepSetToMode";
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this);
      return (retval);
    }  
    
    /***************************************************************************
    **
    ** Draw a group directly into an instance model
    */  
    
     private DialogAndInProcessCmd stepDrawGroupInInstanceFinish() {
      //
      // Undo/Redo support
      //   
      UndoSupport support = new UndoSupport(appState_, "undo.groupDraw");  
      Point2D groupCenter = new Point2D.Double((double)x, (double)y);
          
      if (rcxTrg_.getGenomeAsInstance().getVfgParent() != null) {
        finishSubsetInstanceGroupDraw(support, groupCenter);
      } else {
        newHandler.handleCreationPartThreeCore(support, newGroup, newGroup.getID(), groupCenter, rcxTrg_.getGenomeAsInstance(), null);
      }
      support.finish();
      //
      // DONE!
      //
      
      return (new DialogAndInProcessCmd(ServerControlFlowHarness.ClickResult.PROCESSED, this));
    }
     
    /***************************************************************************
    **
    ** Finish drawing groups in subsets
    ** 
    */
    
    private void finishSubsetInstanceGroupDraw(UndoSupport support, Point2D groupCenter) {
      
      //
      // Here we (may) need to get the group into the vfg root and then pull it down
      // into ALL ancestors of this group that don't already have it.
      //
      
      ArrayList<GenomeInstance> ancestry = new ArrayList<GenomeInstance>();
      ancestry.add(rcxTrg_.getGenomeAsInstance());
      GenomeInstance parent = rcxTrg_.getGenomeAsInstance().getVfgParent();
      while (parent != null) {
        ancestry.add(parent);
        parent = parent.getVfgParent();
      }
      Collections.reverse(ancestry);
      
      String baseID = Group.getBaseID(newGroup.getID());
      GenomeInstance pgi = null;
      int aNum = ancestry.size();
      for (int i = 0; i < aNum; i++) {
        GenomeInstance gi = ancestry.get(i);
        int genCount = gi.getGeneration();        
        String inherit = Group.buildInheritedID(baseID, genCount);
        Group mySubsetGroup = gi.getGroup(inherit);
        if (mySubsetGroup == null) {
          if (i == 0) {
            Group rootGroup = new Group(rcxTrg_.rMan, baseID, newGroup.getName());
            newHandler.handleCreationPartThreeCore(support, rootGroup, rootGroup.getID(), groupCenter, gi, null);
          } else {
            // work with parent group!
            genCount = pgi.getGeneration();        
            inherit = Group.buildInheritedID(baseID, genCount);
            Group subsetGroup = pgi.getGroup(inherit);
            DataAccessContext rcxI = new DataAccessContext(rcxTrg_, gi);
            newHandler.addNewGroupToSubsetInstance(rcxI, subsetGroup, support);
          }
        }
        pgi = gi;
      }    
      return;   
    } 
  }
}
