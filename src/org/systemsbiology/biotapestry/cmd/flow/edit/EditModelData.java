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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.util.ArrayList;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.ModelDataChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.ModelData;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.dialogs.ModelDataDialogFactory;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle editing model data
*/

public class EditModelData extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditModelData(BTState appState) {
    super(appState);
    name =  "command.ChangeModelData";
    desc = "command.ChangeModelData";
    mnem =  "command.ChangeModelDataMnem";
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Handle the full build process
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        EditModelDataState ans = new EditModelDataState(appState_, cfh.getDataAccessContext());
        ans.cfh = cfh;       
        next = ans.stepGetModelDataEditDialog();
      } else {
        EditModelDataState ans = (EditModelDataState)last.currStateX;    
        if (ans.getNextStep().equals("stepExtractAndInstallModelEditData")) {
          next = ans.stepExtractAndInstallModelEditData(last);      
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
  ** Running State: Kinda needs cleanup!
  */
        
  public static class EditModelDataState implements DialogAndInProcessCmd.CmdState {
     
    private ServerControlFlowHarness cfh;
    private String nextStep_;    
    private BTState appState_;
    private DataAccessContext dacx_;
    
    public String getNextStep() {
      return (nextStep_);
    }
        
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditModelDataState(BTState appState, DataAccessContext dacx) {
      appState_ = appState;
      nextStep_ = "stepGetModelDataEditDialog";
      dacx_ = dacx;
    }
  
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetModelDataEditDialog() { 
      Database db = appState_.getDB();
      ModelData mdat = db.getModelData();
      ModelDataDialogFactory.BuildArgs ba = new ModelDataDialogFactory.BuildArgs(mdat);
      ModelDataDialogFactory mddf = new ModelDataDialogFactory(cfh);
      ServerControlFlowHarness.Dialog cfhd = mddf.getDialog(ba);
      DialogAndInProcessCmd retval = new DialogAndInProcessCmd(cfhd, this);         
      nextStep_ = "stepExtractAndInstallModelEditData";
      return (retval);
    } 
    
    /***************************************************************************
    **
    ** Extract and install model data
    */ 
       
    private DialogAndInProcessCmd stepExtractAndInstallModelEditData(DialogAndInProcessCmd cmd) {
        
      ModelDataDialogFactory.ModelDataRequest crq = (ModelDataDialogFactory.ModelDataRequest)cmd.cfhui;   
      if (!crq.haveResults()) {
        DialogAndInProcessCmd retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this);
        return (retval);
      }  
         
      Database db = appState_.getDB();    
      ModelData mdat = new ModelData();
      String date = crq.dateResult;
      mdat.setDate(date);
      String attrib = crq.attribResult;
      mdat.setAttribution(attrib);
      
      ArrayList<String> keys = Splitter.stringBreak(crq.keyResult, "\n", 0, true);
      //
      // Each separate edit caused a new pile of blank lines to appear
      // at the end of the keys, resulting in the key slowly migrating
      // up the screen though the position was fixed.  Chop off all
      // trailing blank lines!
      //
      
      while (true) {
        int lastIndex = keys.size() - 1;
        if (lastIndex == -1) {
          break;
        }
        String lastLine = keys.get(lastIndex);
        if (lastLine.trim().equals("")) {
          keys.remove(lastIndex);
        } else {
          break;
        }
      }
      mdat.setKey(keys);
         
      ModelDataChangeCmd mdcc = new ModelDataChangeCmd(appState_, dacx_, db.getModelData(), mdat);
      db.setModelData(mdat);
  
      UndoSupport support = new UndoSupport(appState_, "undo.mdata");
      support.addEdit(mdcc);
      
      new DataLocator(appState_, dacx_).setDataLocations(support, date, attrib, keys);
   
      support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
      support.finish();
      
      // Either done or looping:
      DialogAndInProcessCmd daipc;
      if (crq.isForApply()) {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.APPLY_PROCESSED, this); // Keep going
        nextStep_ = "stepExtractAndInstallModelEditData";
      } else {
        daipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this); // Done
      }    
      return (daipc);
    } 
  }  
}
