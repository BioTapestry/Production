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


package org.systemsbiology.biotapestry.cmd.flow.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.AbstractStepState;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Handle simple export: abstract base class
*/

public abstract class AbstractSimpleExport extends AbstractControlFlow {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  protected BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public AbstractSimpleExport(BTState appState) {
    appState_ = appState;
    fillResources();
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Kids gotta know this stuff
  ** 
  */
  
  protected abstract void fillResources();
   
  /***************************************************************************
  **
  ** Final visualization step
  ** 
  */
  
  @Override
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last) {
    return (new VisualChangeResult(false));
  }  
  
  /***************************************************************************
  **
  ** Handle pre-file dialogs
  ** 
  */
     
  protected ServerControlFlowHarness.Dialog preFileDialog(ServerControlFlowHarness cfh) {
    return (null);
  }
   
  /***************************************************************************
  **
  ** Fill in file specifics
  ** 
  */
    
  protected abstract void prepFileDialog(ExportState es); 
  
  /***************************************************************************
  **
  ** Do the operation
  ** 
  */
    
  protected abstract boolean runTheExport(ExportState es, TabSource tSrc); 
  
  /***************************************************************************
  **
  ** The new interface
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        ExportState es = new ExportState(cfh);
        next = es.getAPreDialog(cfh, this);    
      } else {
        ExportState ans = (ExportState)last.currStateX;
        ans.stockCfhIfNeeded(cfh);
        if (ans.getNextStep().equals("extractPreFileSettings")) {
          next = ans.extractPreFileSettings(last);       
        } else if (ans.getNextStep().equals("getFileDialog")) {
          next = ans.getFileDialog(last, this);
        } else if (ans.getNextStep().equals("processCommand")) {
          next = ans.processCommand(this);
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
        
  public static class ExportState extends AbstractStepState {
     
    //
    // Package-visible so all the child classes can get to them easily:
    //
    
    File file;
    FilePreparer fprep;
    ArrayList<FileFilter> filts;
    ArrayList<String> suffs;
    String fileErrMsg;
    String fileErrTitle;
    String pref;
    String direct;
    PrintWriter out;      
    ServerControlFlowHarness.UserInputs preFileSet;

    /***************************************************************************
    **
    ** Construct
    ** 
    */
        
    public ExportState(ServerControlFlowHarness cfh) {
      super(cfh);
      this.fprep = uics_.getLSSupport().getFprep(dacx_);
      nextStep_ = "getAPreDialog";
    }
    
    /***************************************************************************
    **
    ** Add cfh in if StepState was pre-built
    */
     
    @Override
    public void stockCfhIfNeeded(ServerControlFlowHarness cfh) {
      if (cfh_ != null) {
        return;
      }
      super.stockCfhIfNeeded(cfh);
      this.fprep = uics_.getLSSupport().getFprep(dacx_);
      return;
    }

    /***************************************************************************
    **
    ** Needed by subclasses of AbstractSimpleExport
    */
       
     UIComponentSource getUICS() {
      return (uics_);
    }
    
    /***************************************************************************
    **
    ** Needed by subclasses of AbstractSimpleExport
    */
       
     UndoFactory getUFac() {
      return (uFac_);
    }
     
    /***************************************************************************
    **
    ** Needed by subclasses of AbstractSimpleExport
    */
       
     TabSource getTSrc() {
      return (tSrc_);
    } 
 
    /***************************************************************************
    **
    ** Get any pre-file dialog:
    ** 
    */
 
    DialogAndInProcessCmd getAPreDialog(ServerControlFlowHarness cfh, AbstractSimpleExport ase) {
      ServerControlFlowHarness.Dialog pfd = ase.preFileDialog(cfh);
      if (pfd != null) {
        this.nextStep_ = "extractPreFileSettings";
        return (new DialogAndInProcessCmd(pfd, this));
      } else {
        this.nextStep_ = "getFileDialog";
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));   
      }
    }
    
    /***************************************************************************
    **
    ** Get the export settings dialog
    ** 
    */
 
    DialogAndInProcessCmd extractPreFileSettings(DialogAndInProcessCmd cmd) {
      preFileSet = cmd.cfhui;     
      nextStep_ = "getFileDialog"; 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));
    }

    /***************************************************************************
    **
    ** Get the file
    ** 
    */
   
    DialogAndInProcessCmd getFileDialog(DialogAndInProcessCmd cmd, AbstractSimpleExport ase) {
      filts = new ArrayList<FileFilter>();
      suffs = new ArrayList<String>();
      ase.prepFileDialog(this);
      file = fprep.getOrCreateWritableFileWithSuffix(direct, filts, suffs, pref);
      if (file == null) {
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, this));       
      }
      nextStep_ = "processCommand";
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));
    }
    
    /***************************************************************************
    **
    ** Do the export
    */
     
    private DialogAndInProcessCmd processCommand(AbstractSimpleExport ase) {      
      try {
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
      } catch (IOException ioe) {
        return (new DialogAndInProcessCmd(fprep.generateSUFForWriteError(ioe), this));
      }

      boolean ok = ase.runTheExport(this, tSrc_);
      out.close();
      
      if (!ok) {
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, fileErrMsg, fileErrTitle);
        return (new DialogAndInProcessCmd(suf, this));
      } else {     
        fprep.setPreference(direct, file.getAbsoluteFile().getParent());
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      }
    }
  } 
}
