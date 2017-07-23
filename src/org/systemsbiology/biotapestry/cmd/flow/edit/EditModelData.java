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

import java.util.List;

import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.undo.ModelDataChangeCmd;
import org.systemsbiology.biotapestry.db.ModelData;
import org.systemsbiology.biotapestry.db.ModelDataSource;
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
  
  public EditModelData() {
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
        EditModelDataState ans = new EditModelDataState(cfh);
        next = ans.stepGetModelDataEditDialog();
      } else {
        EditModelDataState ans = (EditModelDataState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
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
        
  public static class EditModelDataState extends AbstractStepState  {
        
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public EditModelDataState(ServerControlFlowHarness cfh) {
      super(cfh);
      nextStep_ = "stepGetModelDataEditDialog";
    }
  
    /***************************************************************************
    **
    ** Get the dialog launched
    */ 
      
    private DialogAndInProcessCmd stepGetModelDataEditDialog() { 
      ModelDataSource db = dacx_.getModelDataSource();
      ModelData mdat = db.getModelData();
      ModelDataDialogFactory.BuildArgs ba = new ModelDataDialogFactory.BuildArgs(mdat);
      ModelDataDialogFactory mddf = new ModelDataDialogFactory(cfh_);
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
         
      ModelDataSource db = dacx_.getModelDataSource();
      ModelData mdat = new ModelData();
      String date = crq.dateResult;
      mdat.setDate(date);
      String attrib = crq.attribResult;
      mdat.setAttribution(attrib);
      
      List<String> keys = Splitter.stringBreak(crq.keyResult, "\n", 0, true);
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
         
      ModelDataChangeCmd mdcc = new ModelDataChangeCmd(dacx_, db.getModelData(), mdat);
      db.setModelData(mdat);
  
      UndoSupport support = uFac_.provideUndoSupport("undo.mdata", dacx_);
      support.addEdit(mdcc);
      
      new DataLocator(uics_.getGenomePresentation(), dacx_).setDataLocations(support, date, attrib, keys);
   
      support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
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
