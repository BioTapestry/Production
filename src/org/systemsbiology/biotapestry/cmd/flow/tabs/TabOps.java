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

package org.systemsbiology.biotapestry.cmd.flow.tabs;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabChange;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.info.ShowInfo;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TabChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TabEditCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.Metabase;
import org.systemsbiology.biotapestry.db.TabNameData;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.TabChangeEvent;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.ui.dialogs.FollowCurrentDataSharingDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.RetitleTabDialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.SetDataSharingDialogFactory;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of Tab removal ops
*/

public class TabOps extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum TabOption {
    DROP_THIS("command.DropThisTab", "command.DropThisTab", "FIXME24.gif", "command.DropThisTabMnem", null),
    DROP_ALL_BUT_THIS("command.DropAllButThisTab", "command.DropAllButThisTab", "FIXME24.gif", "command.DropAllButThisTabMnem", null),
    NEW_TAB("command.NewTab", "command.NewTab", "FIXME24.gif", "command.NewTabMnem", "command.NewTabAccel"),
    CHANGE_TAB("command.ChangeTab", "command.ChangeTab", "FIXME24.gif", "command.ChangeTabMnem", "command.ChangeTabAccel"),
    RETITLE_TAB("command.ChangeTabTitle", "command.ChangeTabTitle", "FIXME24.gif", "command.ChangeTabTitleMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    TabOption(String name, String desc, String icon, String mnem, String accel) {
      this.name_ = name;  
      this.desc_ = desc;
      this.icon_ = icon;
      this.mnem_ = mnem;
      this.accel_ = accel;
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
  
  private TabOption action_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TabOps(BTState appState, TabOption action) {
    appState_ = appState;
    name =  action.getName();
    desc =  action.getDesc();
    icon =  action.getIcon();
    mnem =  action.getMnem();
    accel =  action.getAccel();
    action_ = action;
  }
  
  /***************************************************************************
  **
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case DROP_THIS:
      case DROP_ALL_BUT_THIS:
      case CHANGE_TAB:
        return (cache.moreThanOneTab());    
      case NEW_TAB:
      case RETITLE_TAB:
          return (true);            
      default:
        throw new IllegalStateException();
    }
  }
  
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
        ans = new StepState(appState_, action_, cfh);
      } else {
        ans = (StepState)last.currStateX;
      }
      ans.stockCfhIfNeeded(cfh);
      if (ans.getNextStep().equals("stepToProcess")) {
        next = ans.stepToProcess();       
      } else if (ans.getNextStep().equals("dropAllButAction")){
        next = ans.dropAllButAction(); 
      } else if (ans.getNextStep().equals("stepToCheck")){
        next = ans.stepToCheck(last);         
      } else if (ans.getNextStep().equals("setNewTabTitle")){
    	  next = ans.setNewTabTitle(last);
      } else if (ans.getNextStep().equals("stepNewTabNext")){
        next = ans.stepNewTabNext(last);
      } else if (ans.getNextStep().equals("doNewTab")){
        next = ans.doNewTab(last);
      } else {
        throw new IllegalStateException();
      }
      if (!next.state.keepLooping()) {
        return (next);
      }
      last = next;
    }
  }
   
  @Override     
  public RemoteRequest.Result processRemoteRequest(RemoteRequest qbom, DialogAndInProcessCmd.CmdState cms) {
	    StepState ans = (StepState)cms;
	    if (qbom.getLabel().equals("queBombMatchForTabTitle")) {
	      return (ans.queBombMatchForTabTitle(qbom));
	    } else {
	      throw new IllegalArgumentException();
	    }

  }
  
  
  /***************************************************************************
  **
  ** For programmatic preload
  ** 
  */ 
  
  @Override  
  public DialogAndInProcessCmd.CmdState getEmptyStateForPreload(StaticDataAccessContext dacx) {
    StepState retval = new StepState(appState_, action_, dacx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Running State
  */
        
  public static class StepState extends AbstractStepState implements DialogAndInProcessCmd.TabPopupCmdState {

    private TabOption myAction_;
    private DynamicDataAccessContext ddacx_;
    private int whichIndex_;
    private boolean viaUI_;
    private TabChange uiTc_;
    private boolean forNew_;
    private Metabase.DataSharingPolicy dsp_;
    private String initShareDBKey_;
    private boolean sharingInEffect_;
    private boolean weAreSharing_; 
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, TabOption action, StaticDataAccessContext dacx) {
      super(dacx);
      // We ignore provided dacx, since tab changes cause current one to go stale...
      myAction_ = action;
      nextStep_ = "stepToProcess";
      ddacx_ = new DynamicDataAccessContext(appState);
      forNew_ = false;
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, TabOption action, ServerControlFlowHarness cfh) {
      super(cfh);
      myAction_ = action;
      nextStep_ = "stepToProcess";
      ddacx_ = new DynamicDataAccessContext(appState);
      forNew_ = false;
    }
    
    /***************************************************************************
    **
    ** Command State for Tab Popup
    */ 
  

    public void setTab(int tabNum, boolean viaUI, TabChange tc) {
      whichIndex_ = tabNum;
      viaUI_ = viaUI;
      uiTc_ = tc;
      return;
    }

    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {          
      switch (myAction_) {
        case DROP_THIS: 
          return (dropThisAction());
        case DROP_ALL_BUT_THIS: 
          return (stepDropWarning());
        case NEW_TAB:
          return (startNewTab());    
        case CHANGE_TAB:
          return (doChangeTab());
        case RETITLE_TAB:
            return (doChangeTabTitle());
        default:
          throw new IllegalStateException();
      }
    }
    
    
    /***************************************************************************
    **
    ** Command
    */     
    
    private DialogAndInProcessCmd dropThisAction() {
      UndoSupport support = uFac_.provideUndoSupport("undo.removeThisTab", ddacx_);

      int numTab = tSrc_.getNumTab();
      if (numTab == 1) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
        
      String pushSharedDataTarg = null;
      Metabase mb = dacx_.getMetabase();
      
      Database db = mb.getDB(tSrc_.getDbIdForIndex(whichIndex_));
      String remDBID = db.getID();
      boolean amSharing = db.amUsingSharedExperimentalData();
      
      if (amSharing) {
        boolean sharing = mb.amSharingExperimentalData();
        if (sharing) {
          Set<String> sharDB = mb.tabsSharingData();
          if (sharDB.contains(remDBID)) {
            sharDB.remove(remDBID);
            if (sharDB.size() == 1) {
              pushSharedDataTarg = sharDB.iterator().next();
            }
          }
        }
      }
        
      int currentCurrent = tSrc_.getCurrentTabIndex();
      int newCurrent = currentCurrent;
      if (currentCurrent == whichIndex_) { // Deleting current tab
        if (currentCurrent == (numTab - 1)) { // Deleting last tab
          newCurrent = numTab - 2; // next tab down become current
        } else {
          newCurrent = newCurrent; // next tab up becomes current (which is moving down...)
        }
      } else if (whichIndex_ < currentCurrent) { // Deleting lower tab
        newCurrent--;
      }
      // May be null for headless operation:
      TabChange tc = uics_.getCommonView().removeTabUI(whichIndex_, currentCurrent, newCurrent);
      if (tc != null) {
        support.addEdit(new TabChangeCmd(ddacx_, tc));
      }
      tc = tSrc_.removeATab(whichIndex_, currentCurrent, newCurrent);
      if (tc == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      support.addEdit(new TabChangeCmd(ddacx_, tc));

    //
    // Once tab is gone, we need to possibly adjust the shared data state. Though only if
    // we were sharing. If so, and there is only one other tab out there 
    // who was sharing, we restore ownership to that tab and stop sharing. If there are
    // at least two remaining tabs sharing the data, nothing needs to happen. 
    //

      if (pushSharedDataTarg != null) {
        Database shdb = mb.getDB(pushSharedDataTarg);
        Metabase.DataSharingPolicy dsp = mb.getDataSharingPolicy();
        Metabase.DataSharingPolicy unshareDsp = new Metabase.DataSharingPolicy(false, false, false, false); 
        List<DatabaseChange> dcs = shdb.modifyDataSharing(dsp, unshareDsp);
        for (DatabaseChange dc : dcs) {
          support.addEdit(new DatabaseChangeCmd(ddacx_, dc));
        }
      }
      TabChangeEvent tcev = new TabChangeEvent(remDBID, TabChangeEvent.Change.DELETE);     
      uics_.getEventMgr().sendTabChangeEvent(tcev);
      
      
      support.finish();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }

    /***************************************************************************
    **
    ** Warn of shared data loss
    */
      
    private DialogAndInProcessCmd stepDropWarning() {
      DialogAndInProcessCmd daipc;
      //
      // OK, we want to drop all but one tab. If nobody is sharing data, great. If tabs 
      // except this one are sharing data, put up a dialog to allow cancel.
      //
      
      Metabase mb = dacx_.getMetabase();
      boolean thereIsSharing = mb.amSharingExperimentalData();
      boolean iAmSharing = mb.getDB(tSrc_.getDbIdForIndex(whichIndex_)).amUsingSharedExperimentalData();
      boolean dataWillBeDropped = thereIsSharing && !iAmSharing;  
     
      if (dataWillBeDropped) {
        ResourceManager rMan = ddacx_.getRMan();
        String message = rMan.getString("dropOtherTabs.sharedDataLossMessage");
        String title = rMan.getString("dropOtherTabs.sharedDataLossTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);     
        daipc = new DialogAndInProcessCmd(suf, this);
        nextStep_ = "stepToCheck";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "dropAllButAction";
      }    

      return (daipc);     
    }
    
    /***************************************************************************
    **
    ** Command
    */ 
 
    private DialogAndInProcessCmd stepToCheck(DialogAndInProcessCmd daipc) {      
      int choiceVal = daipc.suf.getIntegerResult();     
      if (choiceVal == SimpleUserFeedback.NO) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this);
        return (retval);
      }
      DialogAndInProcessCmd ret = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = "dropAllButAction";
      return (ret);     
    }

    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd dropAllButAction() {
  
      int numTab = tSrc_.getNumTab();
      if (numTab == 1) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
      }
      //
      // If this tab is sharing data with tabs being deleted, suck the data up into this tab and
      // stop sharing.
      //
          
      Metabase mb = dacx_.getMetabase();
      boolean thereIsSharing = mb.amSharingExperimentalData();
      Database myDb = mb.getDB(tSrc_.getDbIdForIndex(whichIndex_));
      boolean iAmSharing = myDb.amUsingSharedExperimentalData();
      boolean dataMustBePulledUp = thereIsSharing && iAmSharing;
      boolean dataMustBeDropped = thereIsSharing && !iAmSharing;
      
      UndoSupport support = uFac_.provideUndoSupport("undo.dropAllButThisTab", ddacx_);
      
      if (dataMustBeDropped) {     
        DatabaseChange dc = mb.dropAllSharedData();
        support.addEdit(new DatabaseChangeCmd(ddacx_, dc));
      }
      
      int currentCurrent = tSrc_.getCurrentTabIndex();
      TabChange tc = tSrc_.setCurrentTabIndex(whichIndex_);
      support.addEdit(new TabChangeCmd(ddacx_, tc));
      tc = uics_.getCommonView().setVisibleTab(whichIndex_, currentCurrent);
      if (tc != null) {
        support.addEdit(new TabChangeCmd(ddacx_, tc));
      }
            
      for (int i = numTab - 1; i > whichIndex_; i--) {
        String goodBye = tSrc_.getDbIdForIndex(i);
        // May be null for headless operation:
        tc = uics_.getCommonView().removeTabUI(i, whichIndex_, whichIndex_);
        if (tc != null) {
          support.addEdit(new TabChangeCmd(ddacx_, tc));
        }
        tc = tSrc_.removeATab(i, whichIndex_, whichIndex_);
        if (tc == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
        support.addEdit(new TabChangeCmd(ddacx_, tc));
        TabChangeEvent tcev = new TabChangeEvent(goodBye, TabChangeEvent.Change.DELETE);     
        uics_.getEventMgr().sendTabChangeEvent(tcev);       
      }
      
      for (int i = whichIndex_ - 1; i >= 0; i--) {
         String goodBye = tSrc_.getDbIdForIndex(i);
        // May be null for headless operation:
        tc = uics_.getCommonView().removeTabUI(i, i + 1, i);
        if (tc != null) {
          support.addEdit(new TabChangeCmd(ddacx_, tc));
        }
        tc = tSrc_.removeATab(i, i + 1, i);
        if (tc == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
        support.addEdit(new TabChangeCmd(ddacx_, tc));
        
        TabChangeEvent tcev = new TabChangeEvent(goodBye, TabChangeEvent.Change.DELETE);     
        uics_.getEventMgr().sendTabChangeEvent(tcev);        
      }

      if (dataMustBePulledUp) {
        Metabase.DataSharingPolicy dsp = mb.getDataSharingPolicy();
        Metabase.DataSharingPolicy unshareDsp = new Metabase.DataSharingPolicy(false, false, false, false); 
        List<DatabaseChange> dcs = myDb.modifyDataSharing(dsp, unshareDsp);
        for (DatabaseChange dc : dcs) {
          support.addEdit(new DatabaseChangeCmd(ddacx_, dc));
        }
      }

      support.finish();
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
    
 
    /***************************************************************************
    ** doChangeTabTitle
    ** @return DialogAndInProcessCmd
    */
    
    private DialogAndInProcessCmd doChangeTabTitle() {
      TabNameData tndNow = tSrc_.getTabNameDataForIndex(whichIndex_);
      RetitleTabDialogFactory.BuildArgs ba = new RetitleTabDialogFactory.BuildArgs(tndNow.getFullTitle(), tndNow.getDesc());
      RetitleTabDialogFactory rtf = new RetitleTabDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = rtf.getDialog(ba);
      DialogAndInProcessCmd ret  = new DialogAndInProcessCmd(cfhd, this);
    	
    	nextStep_ = "setNewTabTitle";
    	
    	return ret;
    }

    /****************************
     * queBombMatchForTabTitle
     ****************************
     * 
     * 
     * @param qbom RemoteRequest
     * 
     * @return RemoteRequest.Result
     * 
     */
    private RemoteRequest.Result queBombMatchForTabTitle(RemoteRequest qbom) {
      
      //
      // User enters full title
      // System chops it if needed
      // System uses full title as tooltip
      // User can override tooltip
      //
    	
      RemoteRequest.Result result = new RemoteRequest.Result(qbom);
      String titleResult = qbom.getStringArg("titleResult");
 
      List<TabSource.AnnotatedTabData> ald = tSrc_.getTabs();
      int numTab = ald.size();
      for (int i = 0; i < numTab; i++) {
        if (i == whichIndex_) { // Not checking against tab name we are changing, but gotta check all for new tab
          if (!forNew_) {
            continue;
          }
        }
        TabSource.AnnotatedTabData atd = ald.get(i);
        if ((titleResult == null) || DataUtil.keysEqual(titleResult, atd.tnd.getTitle())) {
          String message = UiUtil.convertMessageToHtml(ddacx_.getRMan().getString("tabRename.nameCollisionTitle"));
          String title = ddacx_.getRMan().getString("tabRename.nameCollisionTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
          result.setBooleanAnswer("haveResult", false);
          result.setSimpleUserFeedback(suf);
          result.setDirection(RemoteRequest.Progress.STOP);
          return (result);
        }  
      }
      result.setBooleanAnswer("haveResult", true);
      result.setDirection(RemoteRequest.Progress.DONE);
      return (result);   
    }  

    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd startNewTab() { 
      
      //
      // If we already have tab (which we should...), we need to ask if data should be shared. Every tab after that
      // gets asked as well. If previous tabs are already sharing, that is who we share with. But if we have >= 2 tabs
      // already, and nobody is sharing, we must allow the user to choose which other tab to start sharing with!
      //
      
      List<TabSource.AnnotatedTabData> choices = tSrc_.getTabs();
      sharingInEffect_ = dacx_.getMetabase().amSharingExperimentalData();
      
      DialogAndInProcessCmd retval;
      if (sharingInEffect_) {
        String message = ShowInfo.showDataSharingText(ddacx_, tSrc_);
        FollowCurrentDataSharingDialogFactory.BuildArgs ba = new FollowCurrentDataSharingDialogFactory.BuildArgs(message);
        FollowCurrentDataSharingDialogFactory mddf = new FollowCurrentDataSharingDialogFactory(cfh_);
        ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
        retval = new DialogAndInProcessCmd(cfhd, this);  
      } else {
        SetDataSharingDialogFactory.BuildArgs ba = new SetDataSharingDialogFactory.BuildArgs(sharingInEffect_, choices);
        SetDataSharingDialogFactory mddf = new SetDataSharingDialogFactory(cfh_);
        ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
        retval = new DialogAndInProcessCmd(cfhd, this); 
      }
      nextStep_ = "stepNewTabNext";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract data sharing and continue;
    */ 
       
    private DialogAndInProcessCmd stepNewTabNext(DialogAndInProcessCmd cmd) {
      
      FollowCurrentDataSharingDialogFactory.JoinSharingRequest fcrq = null;
      SetDataSharingDialogFactory.DataSharingRequest scrq = null;
      
      if (sharingInEffect_) {
        fcrq = (FollowCurrentDataSharingDialogFactory.JoinSharingRequest)cmd.cfhui;   
        if (!fcrq.haveResults()) {
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
          return (retval);
        }
      } else {
        scrq = (SetDataSharingDialogFactory.DataSharingRequest)cmd.cfhui;   
        if (!scrq.haveResults()) {
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
          return (retval);
        }
       }
     
      //
      // If we are starting to share, load in who needs to push down experimental data:
      //
      
      if (!sharingInEffect_) { 
        dsp_ = new Metabase.DataSharingPolicy(scrq.shareTimeUnits, scrq.shareTimeCourses, scrq.sharePerts, scrq.sharePerEmbryoCounts);
        weAreSharing_ = dsp_.isSpecifyingSharing();
        if (weAreSharing_) {
          initShareDBKey_ = scrq.startSharingWith;
          if (initShareDBKey_ == null) {
            List<TabSource.AnnotatedTabData> choices = tSrc_.getTabs();
            if (choices.size() != 1) {
              throw new IllegalStateException();
            }
            initShareDBKey_ = choices.get(0).dbID;
          }
        }     
      } else { // We *are* sharing. Does the new tab join in, or go it alone?
        dsp_ = dacx_.getMetabase().getDataSharingPolicy();
        weAreSharing_ = fcrq.joinSharing;   
      }

      forNew_ = true;
      String defTabName = buildUniqueTabName(tSrc_, ddacx_, null);
      RetitleTabDialogFactory.BuildArgs ba = new RetitleTabDialogFactory.BuildArgs(defTabName, defTabName);
      RetitleTabDialogFactory rtf = new RetitleTabDialogFactory(cfh_);
      ServerControlFlowHarness.Dialog cfhd = rtf.getDialog(ba);
      DialogAndInProcessCmd ret = new DialogAndInProcessCmd(cfhd, this); 
      nextStep_ = "doNewTab";
      return (ret);
    }   

    /***************************************************************************
    **
    ** Command
    */ 
   
    private DialogAndInProcessCmd doNewTab(DialogAndInProcessCmd daipc) {     
      RetitleTabDialogFactory.RetitleRequest req = (RetitleTabDialogFactory.RetitleRequest)daipc.cfhui;
      TabNameData tnd = new TabNameData(req.titleResult, req.fullTitleResult, req.fullTitleResult);
      // Integer.MIN_VALUE since it does not matter unless the doLoad arg is true:
      doNewTabStat(tSrc_, uics_, dacx_, uFac_, false, null, Integer.MIN_VALUE, tnd, dsp_, initShareDBKey_, Boolean.valueOf(weAreSharing_), null);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    /*********************************
     * setNewTabTitle
     ********************************* 
     * 
     * 
     * @param daipc DialogAndInProcessCmd housing the results of the Dialog
     * 
     * @return DialogAndInProcessCmd Indicating this process is finished
     * 
     */
    public DialogAndInProcessCmd setNewTabTitle(DialogAndInProcessCmd daipc) {
    	    	
    	UndoSupport support = uFac_.provideUndoSupport("undo.setNewTabTitle", ddacx_);
    	
    	RetitleTabDialogFactory.RetitleRequest req = (RetitleTabDialogFactory.RetitleRequest)daipc.cfhui;
    	
    	if(req.titleResult.trim().length() <= 0) {
    	  return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
    	}
    	
    	int holdIndex = tSrc_.getCurrentTabIndex();
    	TabChange tc = tSrc_.setCurrentTabIndex(whichIndex_);
    	support.addEdit(new TabChangeCmd(ddacx_, tc));
    	GenomeSource grsc = ddacx_.getGenomeSource();
    	TabNameData tndNow = grsc.getTabNameData().clone();
    	tndNow.setTitle(req.titleResult);
    	tndNow.setFullTitle(req.fullTitleResult);
    	tndNow.setDesc(req.descResult);
    	
    	DatabaseChange dc = grsc.setTabNameData(tndNow);
    	support.addEdit(new DatabaseChangeCmd(ddacx_, dc));     
    	TabChange tct = uics_.getCommonView().setTabTitleData(whichIndex_, tndNow);    
    	support.addEdit(new TabEditCmd(ddacx_, tct));
    	tc = tSrc_.setCurrentTabIndex(holdIndex);
    	support.addEdit(new TabChangeCmd(ddacx_, tc));
    	
      TabChangeEvent tcev = new TabChangeEvent(tSrc_.getDbIdForIndex(whichIndex_), TabChangeEvent.Change.RENAME);     
      uics_.getEventMgr().sendTabChangeEvent(tcev);        
    		
    	Map<String,Object> results = new HashMap<String,Object>(); 
    	results.put("title",req.titleResult);
    	results.put("fullTitle",req.fullTitleResult);
    	support.finish();
    	
    	return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this,results));
    }
    
    
    /***************************************************************************
    **
    ** Change tab
    */   
    
    public DialogAndInProcessCmd doChangeTab() {
      UndoSupport support = uFac_.provideUndoSupport("undo.changeTab", ddacx_);
      int currentIndex = tSrc_.getCurrentTabIndex();
      TabChange tc = tSrc_.setCurrentTabIndex(whichIndex_);
      support.addEdit(new TabChangeCmd(ddacx_, tc));
  
      if (!viaUI_) {
        tc = uics_.getCommonView().setVisibleTab(whichIndex_, currentIndex);
        if (tc != null) {
          support.addEdit(new TabChangeCmd(ddacx_, tc));
        }
      } else {
        support.addEdit(new TabChangeCmd(ddacx_, uiTc_));
      }
      
      uics_.getZoomCommandSupportForTab(whichIndex_).tabNowVisible();

      uics_.getPathControls().updateUserPathActions();     
      ModelChangeEvent mcev = new ModelChangeEvent(ddacx_.getGenomeSource().getID(), ddacx_.getCurrentGenomeID(), ModelChangeEvent.MODEL_TABBED);     
      uics_.getEventMgr().sendModelChangeEvent(mcev);
      TabChangeEvent tcev = new TabChangeEvent(tSrc_.getDbIdForIndex(whichIndex_), 
                                               tSrc_.getDbIdForIndex(currentIndex), TabChangeEvent.Change.SWITCHED);     
      uics_.getEventMgr().sendTabChangeEvent(tcev);
      support.finish(); 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }

  }
 
  /***************************************************************************
  **
  ** Crappy hack
  */   
  
  public static void doNewTabStat(TabSource tSrc, UIComponentSource uics, DataAccessContext dacx, UndoFactory uFac, 
                                  boolean forLoad, String id, int tabNum, TabNameData tnd, 
                                  Metabase.DataSharingPolicy dsp, String shareInitDB, Boolean isSharing,
                                  UndoSupport appendTabsUndo) {
    
    // If we are loading from IO, we have no tab name data yet, so don't give us any!
    if (forLoad && (tnd != null)) {
      throw new IllegalArgumentException();
    }
    
    int currentCurrent = tSrc.getCurrentTabIndex();
    UndoSupport support = (appendTabsUndo == null) ? uFac.provideUndoSupport("undo.addATab", dacx) : appendTabsUndo;
    
    //
    // First tab has local experimental data, contained in its database. If the user brings up a second tab, they
    // get to choose whether that tab shares, or uses local. If it shares, the first tab needs to push it's data
    // down into the shared metabase. If not, both databases handle their own data.
    // 
    // Moving to a third tab. If both existing are shared, the new tab can be shared or local. If both existing are local,
    // we can allow a conversion to shared if the two local sources are "consistent". Simple case would be if no data has 
    // been entered, or if consistent time units have been entered. More complex cases would need merging, which we have
    // started to implement. Alternate approach would be to allow user to say which tab to share with.
    //
    // Going to three+ existing tabs, only remaining case is if everybody is local and we want to share. More complex UI would
    // allow user to specify which other tab to share with.
    //
    
    //
    // If metabase says we are not sharing and we get a non-null sharing init key, we push stuff down to the metabase:
    //
    
    Metabase mb = dacx.getMetabase();
    boolean sharing = mb.amSharingExperimentalData();
    if (!sharing && (shareInitDB != null)) {
      int cti = tSrc.getCurrentTabIndex();
      
      int newIndx = tSrc.getTabIndexFromId(shareInitDB);
      
      TabChange tc = tSrc.setCurrentTabIndex(newIndx);
      support.addEdit(new TabChangeCmd(dacx, tc));
      
      // 
      // Push stuff down:
      //
      Database db = mb.getDB(tSrc.getDbIdForIndex(tSrc.getCurrentTabIndex()));
      List<DatabaseChange> dcs = db.modifyDataSharing(mb.getDataSharingPolicy(), dsp);
      for (DatabaseChange dc : dcs) {
        support.addEdit(new DatabaseChangeCmd(dacx, dc));
      }
      
      tc = tSrc.setCurrentTabIndex(cti);
      support.addEdit(new TabChangeCmd(dacx, tc));
    }  
   
    //
    // The database created here is NOT sharing data:
    //
    TabChange tc0 = tSrc.addATab(forLoad, id, tabNum); // Database creation occurring in here (unless from IO).    
    UiUtil.fixMePrintout("adding X tabs for load as new tabs is creating a pile of these undos (that crash).");
    
    support.addEdit(new TabChangeCmd(dacx, tc0));
    tSrc.setCurrentTabIndex(tc0.newChangeIndex);
    UiUtil.fixMePrintout("UGH FIX FIX");
    String tabID = tSrc.getDbIdForIndex(tSrc.getCurrentTabIndex());
    Database newDB = mb.getDB(tabID);
    DynamicDataAccessContext ddacx = new DynamicDataAccessContext(mb.getAppState());
    if (forLoad) {
      newDB.dropViaDACX(ddacx.getTabContext(tabID)); 
    } else {
      newDB.newModelViaDACX(ddacx.getTabContext(tabID));
    }
    //
    // The created database will now either be set not to share, or to share based on
    // the provided policy:
    //
    
    if (isSharing == null || !isSharing.booleanValue()) {
      newDB.installDataSharing(null, ddacx.getTabContext(tabID));
    } else {
      newDB.installDataSharing(mb.getDataSharingPolicy(), ddacx.getTabContext(tabID));
    }
    
    if (tnd != null) {
      DatabaseChange dbxc = newDB.setTabNameData(tnd);
      support.addEdit(new DatabaseChangeCmd(dacx, dbxc));
    }
    
    TabChange tc = uics.getCommonView().addTabUI(tc0.newChangeIndex, currentCurrent, tnd);
    if (tc != null) {
      support.addEdit(new TabChangeCmd(dacx, tc));
    }
    
    NavTree navTree = newDB.getModelHierarchy();
    TreePath dstp = navTree.getDefaultSelection();
    navTree.setSkipFlag(NavTree.Skips.SKIP_EVENT);
    uics.getTree().setTreeSelectionPath(dstp);
    navTree.setSkipFlag(NavTree.Skips.NO_FLAG);
    if (!forLoad) {
      dacx.getZoomTarget().fixCenterPoint(true, null, false);    
      uics.getZoomCommandSupport().setCurrentZoomForNewModel();
    }
    
    tc = uics.getCommonView().setVisibleTab(tc0.newChangeIndex, currentCurrent);
    if (tc != null) {
      support.addEdit(new TabChangeCmd(dacx, tc));
    }
    TabChangeEvent tcev = new TabChangeEvent(tabID, TabChangeEvent.Change.ADD);     
    uics.getEventMgr().sendTabChangeEvent(tcev);
    
    if (appendTabsUndo == null) {
      support.finish();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
 
  public static void clearOutTabs(TabSource tSrc, UIComponentSource uics, int startIndex) {       
    int numT = tSrc.getNumTab();  
    int newCurrent = startIndex - 1;
    tSrc.setCurrentTabIndex(newCurrent);
    for (int i = 0; i < numT; i++) {
      uics.getZoomCommandSupportForTab(i).clearHiddenView();
    }
    uics.getCommonView().setInternalVisibleTab(newCurrent, newCurrent);
    for (int i = numT - 1; i >= startIndex; i--) {
      uics.getCommonView().removeTabUI(i, newCurrent, newCurrent);
      tSrc.removeATab(i, newCurrent, newCurrent);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** THIS IS USED TO TOSS TABS AFTER A FAILED IO OPERATION
  */ 
 
  public static void clearATab(TabSource tSrc, UIComponentSource uics, int index, DataAccessContext dacx) {
    int numTab = tSrc.getNumTab();
    if (numTab == 1) {
      throw new IllegalStateException();
    }
 
    UiUtil.fixMePrintout("NO< THIS IS FOR TOSSING TABS. DON'T NEED SHARING CHANGE CODE HERE???");
    
    String pushSharedDataTarg = null;
    Metabase mb = dacx.getMetabase();
    
    Database db = mb.getDB(tSrc.getDbIdForIndex(index));
    String remDBID = db.getID();
    boolean amSharing = db.amUsingSharedExperimentalData();
    
    if (amSharing) {
      boolean sharing = mb.amSharingExperimentalData();
      if (sharing) {
        Set<String> sharDB = mb.tabsSharingData();
        if (sharDB.contains(remDBID)) {
          sharDB.remove(remDBID);
          if (sharDB.size() == 1) {
            pushSharedDataTarg = sharDB.iterator().next();
          }
        }
      }
    }
     
    int currentCurrent = tSrc.getCurrentTabIndex();
    int newCurrent = currentCurrent;
    if (newCurrent == index) { // Deleting current tab
      if (newCurrent == (numTab - 1)) { // Deleting last tab
        newCurrent = numTab - 2; // next tab down become current
      } else {
        newCurrent = newCurrent + 1; // next tab up becomes current
      }
    }
    
    if (newCurrent != currentCurrent) {
      tSrc.setCurrentTabIndex(newCurrent);
      uics.getCommonView().setInternalVisibleTab(currentCurrent, newCurrent);
    }
    
    UiUtil.fixMePrintout("We need UNDO/REDO here! Do we? See above");
    TabChange tc = uics.getCommonView().removeTabUI(index, currentCurrent, newCurrent);
    TabChange tc2 = tSrc.removeATab(index, currentCurrent, newCurrent);
    
          
  //    tc = appState.setCurrentTabIndex(cti);
  //    support.addEdit(new TabChangeCmd(appState, ddacx, tc));
    
    
    //
    // But if we were not sharing, we are done
    //

    if (!amSharing) {
      return;
    }
    
    //
    // I was sharing, but I no longer exist. If there is only one other tab out there 
    // who was sharing, we restore ownership to that tab and stop sharing. If there are
    // at least two remaining tabs sharing the data, nothing needs to happen. 
    //
    
    if (pushSharedDataTarg != null) {
      Database shdb = mb.getDB(pushSharedDataTarg);
      Metabase.DataSharingPolicy dsp = mb.getDataSharingPolicy();
      Metabase.DataSharingPolicy unshareDsp = new Metabase.DataSharingPolicy(false, false, false, false); 
      List<DatabaseChange> dcs = shdb.modifyDataSharing(dsp, unshareDsp);
      for (DatabaseChange dc : dcs) {
       UiUtil.fixMePrintout("Does this need to return???");
      //  support.addEdit(new DatabaseChangeCmd(appState, ddacx, dc));
      }
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Get a unique name
  */ 
   
  public static String buildUniqueTabName(TabSource tSrc, DataAccessContext dacx, String baseName) {
    List<TabSource.AnnotatedTabData> tabs = tSrc.getTabs();
    HashSet<String> gotNames = new HashSet<String>();
    for (TabSource.AnnotatedTabData atd : tabs) {
      gotNames.add(DataUtil.normKey(atd.tnd.getFullTitle()));
    }
    
    int num;
    String tnfmt;
    if (baseName == null) {
      num = tSrc.getNumTab() + 1;
      tnfmt = dacx.getRMan().getString("database.newTabNameFormat");
    } else {
      num = 2;
      tnfmt = dacx.getRMan().getString("database.baseTabNameFormat");
    }
    
    while (true) {
      Object[] args;
      if (baseName == null) {
        args = new Object[] {Integer.valueOf(num++)};
      } else if (num == 0) {
        args = new Object[] {baseName, ""};
        num = 2;
      } else {
        args = new Object[] {baseName, Integer.valueOf(num++)};
      }
      String defTabName = MessageFormat.format(tnfmt, args).trim();
      if (!gotNames.contains(DataUtil.normKey(defTabName))) {
        return (defTabName);
      }
    }
  } 
  
  /***************************************************************************
  **
  ** Answer if unique name
  */ 
   
  public static boolean isUniqueTabName(TabSource tSrc, String testName) {
    List<TabSource.AnnotatedTabData> tabs = tSrc.getTabs();
    HashSet<String> gotNames = new HashSet<String>();
    for (TabSource.AnnotatedTabData atd : tabs) {
      gotNames.add(DataUtil.normKey(atd.tnd.getFullTitle()));
    }    
    return (!gotNames.contains(DataUtil.normKey(testName)));
  }
}
