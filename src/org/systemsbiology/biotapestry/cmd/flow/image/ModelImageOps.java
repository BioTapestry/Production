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

package org.systemsbiology.biotapestry.cmd.flow.image;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of IO Operations
*/

public class ModelImageOps extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Possible actions we can perform
  */ 
   
  public enum ImageAction {
    DROP("command.DropImageFromModel", "command.DropImageFromModel", "FIXME24.gif", "command.DropImageFromModelMnem", null),
    ADD("command.AssignImageToModel", "command.AssignImageToModel", "FIXME24.gif", "command.AssignImageToModelMnem", null),
    ;
    
    private String name_;
    private String desc_;
    private String icon_;
    private String mnem_;
    private String accel_;
     
    ImageAction(String name, String desc, String icon, String mnem, String accel) {
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
  
  private ImageAction action_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelImageOps(BTState appState, ImageAction action) {
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
  ** Answer if we are enabled
  ** 
  */
  
  @Override  
  public boolean isEnabled(CheckGutsCache cache) {
    switch (action_) {
      case DROP:
         return (cache.genomeHasImage());
      case ADD:
        return (cache.genomeNotNull());      
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
        ans = new StepState(appState_, action_, cfh.getDataAccessContext());
      } else {
        ans = (StepState)last.currStateX;
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
        
  public static class StepState implements DialogAndInProcessCmd.CmdState {

    private String nextStep_;
    private ImageAction myAction_;
    private LoadSaveSupport myLsSup_;
    private BTState appState_;
    private DataAccessContext dacx_;
     
    public String getNextStep() {
      return (nextStep_);
    }
    
    /***************************************************************************
    **
    ** Construct
    */ 
    
    public StepState(BTState appState, ImageAction action, DataAccessContext dacx) {
      myAction_ = action;
      appState_ = appState;
      nextStep_ = "stepToProcess";
      myLsSup_ = appState_.getLSSupport();
      dacx_ = dacx;
    }
     
    /***************************************************************************
    **
    ** Do the step
    */ 
       
    private DialogAndInProcessCmd stepToProcess() {          
      switch (myAction_) {
        case ADD: 
          return (addAction());
        case DROP: 
          return (dropAction()); 
        default:
          throw new IllegalStateException();
      }
    }

    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd addAction() {

        ImageManager mgr = appState_.getImageMgr();
        ResourceManager rMan = appState_.getRMan();
  
        List<String> supported = mgr.getSupportedFileSuffixes();
        FileExtensionFilters.MultiExtensionFilter filt = new FileExtensionFilters.MultiExtensionFilter(appState_, supported, "filterName.img");
        File file = myLsSup_.getFprep().getExistingImportFile("LoadImageDirectory", filt);
        if (file == null) {
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
        }
        
        UndoSupport support = new UndoSupport(appState_, "undo.setImage");
        String imgKey;
        try {
          ImageManager.TypedImage ti = mgr.loadImageFromFileStart(file);
          if ((ti.getHeight() < 1) || (ti.getWidth() < 1)) {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                          rMan.getString("assignImage.zeroImageMessage"), 
                                          rMan.getString("assignImage.errorMessageTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
          }
          if ((ti.getHeight() > ImageManager.WARN_DIM_Y) || (ti.getWidth() > ImageManager.WARN_DIM_X)) {
            String format = rMan.getString("assignImage.bigImageMessageFormat");
            String formMsg = MessageFormat.format(format, new Object[] {new Integer(ti.getWidth()),
                                                                        new Integer(ti.getHeight()),
                                                                        new Integer(ImageManager.WARN_DIM_X),                    
                                                                        new Integer(ImageManager.WARN_DIM_Y)});
            formMsg = "<html><center>" + formMsg + "</center></html>";
            formMsg = formMsg.replaceAll("\n", "<br>");
            int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), formMsg,
                                                   rMan.getString("assignImage.warningMessageTitle"),
                                                   JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
              return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));
            }
          }          

          ImageManager.NewImageInfo nii = mgr.loadImageFromFileFinish(ti);
          if (nii.change != null) {
            nii.change.genomeKey = dacx_.getGenomeID();  // All undo processing goes through genome
            support.addEdit(new ImageChangeCmd(appState_, dacx_, nii.change));
          }
          imgKey = nii.key;
        } catch (IOException ioex) {
          myLsSup_.getFprep().getFileInputError(ioex).displayFileInputError();
          return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.HAVE_ERROR, this));
        }
        myLsSup_.getFprep().setPreference("LoadImageDirectory", file.getAbsoluteFile().getParent());
        
        ImageChange[] ics = dacx_.getGenome().setGenomeImage(imgKey);
        if (ics != null) {
          int numic = ics.length;
          for (int i = 0; i < numic; i++) {
            support.addEdit(new ImageChangeCmd(appState_, dacx_, ics[i]));
          }
        }        
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.PROPERTY_CHANGE));
        support.finish();
    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    }
    
    
    /***************************************************************************
    **
    ** Command
    */ 

    private DialogAndInProcessCmd dropAction() {
      UndoSupport support = new UndoSupport(appState_, "undo.dropImage");
      ImageChange ic = dacx_.getGenome().dropGenomeImage();
      if (ic != null) {
        support.addEdit(new ImageChangeCmd(appState_, dacx_, ic));
      }
      support.addEvent(new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.PROPERTY_CHANGE));
      support.finish();    
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
    } 
  }
}
