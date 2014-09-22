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

package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle removal of selections
*/

public class RemoveSelections extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

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
  
  public RemoveSelections(BTState appState) {
    super(appState);
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
        ans = new StepState(appState_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
      }
      if (ans.getNextStep().equals("stepToWarn")) {
        next = ans.stepToWarn();      
      } else if (ans.getNextStep().equals("stepToCheckDataDelete")) {
        next = ans.stepToCheckDataDelete();   
      } else if (ans.getNextStep().equals("registerCheckDataDelete")) {
        next = ans.registerCheckDataDelete(last);       
      } else if (ans.getNextStep().equals("stepToRemove")) {
        next = ans.stepToRemove();       
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {
    
    private DataAccessContext rcxT_;
    private String nextStep_;    
    private BTState appState_;
    private Map<String, Boolean> dataDelete_;
    private ArrayList<String> deadList_;
    private int deadCheck_;
    private HashMap<String, Intersection> linkInter_;
    private UserDataChoice.Delete multiDelete_;
   
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepToWarn";
      rcxT_ = dacx;
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToWarn() {
     
      Map<String, Intersection> selmap = appState_.getGenomePresentation().getSelections();

      //
      // Crank through the selections.  For selections that are linkages, 
      // blow them apart into segments if they are full intersections.
      // Then hand the list off to the delete command to figure out all
      // the linkages passing through the all the segments.
      //
      
      TreeSet<String> deadSet = new TreeSet<String>();
      linkInter_ = new HashMap<String, Intersection>();
      
      Iterator<String> selKeys = selmap.keySet().iterator();
      while (selKeys.hasNext()) {
        String key = selKeys.next();
        Intersection inter = selmap.get(key);
        Linkage link = rcxT_.getGenome().getLinkage(key);
        if (link != null) {
          if (inter.getSubID() == null) {
            inter = Intersection.fullIntersection(link, rcxT_, true);
          }
          linkInter_.put(key, inter);
        }
        Node node = rcxT_.getGenome().getNode(key);
        if (node != null) {
          deadSet.add(key);
        }
      }

      //
      // Nobody to delete -> get lost
      //
    
      if (deadSet.isEmpty() && linkInter_.isEmpty()) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }

      deadList_ = new ArrayList<String>(deadSet);
      DialogAndInProcessCmd daipc;
      SimpleUserFeedback suf = RemoveSupport.deleteWarningHelperNew(appState_, rcxT_);
      if (suf != null) {
        daipc = new DialogAndInProcessCmd(suf, this);     
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      }
      
      if (!rcxT_.genomeIsRootGenome() || (deadList_.size() == 0)) {
        dataDelete_ = null;
        nextStep_ = "stepToRemove";
      } else {
        multiDelete_ = (deadList_.size() == 1) ? UserDataChoice.Delete.SINGLE_DELETE : UserDataChoice.Delete.ASK_FOR_ALL;
        deadCheck_ = 0;
        dataDelete_ = new HashMap<String, Boolean>();
        nextStep_ = "stepToCheckDataDelete";
      }
      return (daipc); 
    }
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToCheckDataDelete() {
  
      String deadID = deadList_.get(deadCheck_);
      String deadName = rcxT_.getGenome().getNode(deadID).getRootName();
  
      //
      // Under all circumstances, we delete associated mappings to data.
      // We also give the user the option to delete the underlying data
      // tables, unless previously specified
      //
    
      TimeCourseData tcd = rcxT_.getExpDataSrc().getTimeCourseData();
      TemporalInputRangeData tird = rcxT_.getExpDataSrc().getTemporalInputRangeData();
      
      if (((tcd != null) && tcd.haveDataForNodeOrName(deadID, deadName)) ||
          ((tird != null) && tird.haveDataForNodeOrName(deadID, deadName))) {    
        if (multiDelete_ == UserDataChoice.Delete.PREVIOUS_YES) {
          dataDelete_.put(deadList_.get(deadCheck_), Boolean.valueOf(true));
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
          deadCheck_++;
          nextStep_ = (deadCheck_ >= deadList_.size()) ? "stepToRemove" : "stepToCheckDataDelete";
          return (retval);
        } else if (multiDelete_ == UserDataChoice.Delete.PREVIOUS_NO) {
          dataDelete_.put(deadList_.get(deadCheck_), Boolean.valueOf(false));
          DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
          deadCheck_++;
          nextStep_ = (deadCheck_ >= deadList_.size()) ? "stepToRemove" : "stepToCheckDataDelete";
          return (retval);
        } else if (multiDelete_ == UserDataChoice.Delete.SINGLE_DELETE) {
          String daString = MessageFormat.format(rcxT_.rMan.getString("nodeDelete.doDataMessage"), new Object[] {deadName});
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, daString, rcxT_.rMan.getString("nodeDelete.messageTitle"));
          DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(suf, this);     
          nextStep_ = "registerCheckDataDelete";   
          return (daipc);  
        } else if (multiDelete_ == UserDataChoice.Delete.ASK_FOR_ALL) {       
          String doData = rcxT_.rMan.getString("nodeDelete.doDataMessage");
          String msg = MessageFormat.format(doData, new Object[] {deadName});
          Object[] args = new Object[] {
                                        rcxT_.rMan.getString("dialogs.yesToAll"),
                                        rcxT_.rMan.getString("dialogs.noToAll"),
                                        rcxT_.rMan.getString("dialogs.yes"),
                                        rcxT_.rMan.getString("dialogs.no"),
                                        rcxT_.rMan.getString("dialogs.cancel")
                                       };
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.OPTION_OPTION, 
                                                          msg, 
                                                          rcxT_.rMan.getString("nodeDelete.messageTitle"),
                                                          args, rcxT_.rMan.getString("dialogs.no"));
          DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(suf, this);     
          nextStep_ = "registerCheckDataDelete";   
          return (daipc);  
        }          
      }
      deadCheck_++;
      DialogAndInProcessCmd daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      nextStep_ = (deadCheck_ >= deadList_.size()) ? "stepToRemove" : "stepToCheckDataDelete";
      return (daipc);
    } 
  
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd registerCheckDataDelete(DialogAndInProcessCmd daipc) {
      if (multiDelete_ == UserDataChoice.Delete.PREVIOUS_YES) {
        throw new IllegalStateException();    
      } else if (multiDelete_ == UserDataChoice.Delete.PREVIOUS_NO) {
        throw new IllegalStateException();
      } else if (multiDelete_ == UserDataChoice.Delete.SINGLE_DELETE) {
        int result = daipc.suf.getIntegerResult();
        if (result == SimpleUserFeedback.CANCEL) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }     
        dataDelete_.put(deadList_.get(deadCheck_), Boolean.valueOf(result == SimpleUserFeedback.YES));
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
        nextStep_ = "stepToRemove";
        return (retval);  
      } else if (multiDelete_ == UserDataChoice.Delete.ASK_FOR_ALL) {
        int result = daipc.suf.getIntegerResult();
        if ((result == 4) || (result == SimpleUserFeedback.CANCEL)) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }  
        dataDelete_.put(deadList_.get(deadCheck_), Boolean.valueOf((result == 0) || (result == 2)));
        if (result == 0) {
          multiDelete_ = UserDataChoice.Delete.PREVIOUS_YES;
        } else if (result == 1) {
          multiDelete_ = UserDataChoice.Delete.PREVIOUS_NO;
        }
      } else {
        throw new IllegalStateException();
      }
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this); // Keep going
      deadCheck_++;
      nextStep_ = (deadCheck_ >= deadList_.size()) ? "stepToRemove" : "stepToCheckDataDelete";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToRemove() {
      
   
      Map<String, Layout.PadNeedsForLayout> globalPadNeeds = (new FullGenomeHierarchyOracle(appState_)).getGlobalNetModuleLinkPadNeeds();
    
      UndoSupport support = new UndoSupport(appState_, "undo.deleteSelected");        
      appState_.getGenomePresentation().clearSelections(rcxT_, support);
      boolean didDelete = false;
   
      if (RemoveLinkage.deleteLinksFromModel(appState_, linkInter_, rcxT_, support)) {
        didDelete = true;
      }
  
      Iterator<String> dsit = deadList_.iterator();
      while (dsit.hasNext()) {
        String deadID = dsit.next();
        boolean nodeRemoved = RemoveNode.deleteNodeFromModelCore(appState_, deadID, rcxT_, support, dataDelete_, true);
        didDelete |= nodeRemoved;
      }
      
      if (globalPadNeeds != null) {
        ModificationCommands.repairNetModuleLinkPadsGlobally(appState_, rcxT_, globalPadNeeds,false, support);
      }    
   
      if (didDelete) {
        support.addEvent(new ModelChangeEvent(rcxT_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }
  
      support.finish();  // no matter what to handle selection clearing
      appState_.getSUPanel().drawModel(false);
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));  
    }
  }
    
  /***************************************************************************
  **
  ** User data choice
  */
        
  public static class UserDataChoice  {
    
    public enum Delete {
      SINGLE_DELETE,
      ASK_FOR_ALL,
      PREVIOUS_NO,
      PREVIOUS_YES,
      OFFLINE_NO;
    }
   
    public enum Decision {
      NO_DELETION,
      NO_DECISION, 
      YES_FOR_ALL,  
      NO_FOR_ALL;  
    }
 
    Decision decide;  
    boolean deleteUnderlyingTables;
     
    UserDataChoice(Decision decide, boolean deleteUnderlyingTables) {
      this.decide = decide;
      this.deleteUnderlyingTables = deleteUnderlyingTables;
    }   
  } 
}
