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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.CheckGutsCache;
import org.systemsbiology.biotapestry.cmd.flow.AbstractControlFlow;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.VisualChangeResult;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.ImageExporter;
import org.systemsbiology.biotapestry.ui.dialogs.ExportSettingsPublishDialogFactory;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Handle exporting images for publication
*/

public class ExportPublish extends AbstractControlFlow {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private FilePreparer fprep_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ExportPublish(BTState appState) {
    super(appState);
    fprep_ = appState_.getLSSupport().getFprep(); 
    name = "command.ExportPublish";
    desc = "command.ExportPublish";
    icon = "Export24.gif";
    mnem  = "command.ExportPublishMnem";
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
  ** Final visualization step
  ** 
  */
  
  @Override
  public VisualChangeResult visualizeResults(DialogAndInProcessCmd last) {
    return (new VisualChangeResult(false));
  }
  
  /***************************************************************************
  **
  ** The new interface
  ** 
  */ 
  
  public DialogAndInProcessCmd processNextStep(ServerControlFlowHarness cfh, DialogAndInProcessCmd last) {
    DialogAndInProcessCmd next;
    while (true) {
      if (last == null) {
        ExportPublishState nss = new ExportPublishState(appState_);
        nss.fprep = fprep_;
        next = nss.getSettingsDialog(cfh);    
      } else {
        ExportPublishState ans = (ExportPublishState)last.currStateX;
        if (ans.getNextStep().equals("extractExportSettings")) {
          next = ans.extractExportSettings(last);       
        } else if (ans.getNextStep().equals("getFileDialog")) {
          next = ans.getFileDialog(last);
        } else if (ans.getNextStep().equals("processCommand")) {
          next = ans.processCommand();
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
        
  public static class ExportPublishState implements DialogAndInProcessCmd.CmdState {
     
    private BTState appState_;
    private ExportSettings set;
    private File file;
    private FilePreparer fprep;
    private String nextStep_;
 
    public String getNextStep() {
      return (nextStep_);
    }
      
    public ExportPublishState(BTState appState) {
      appState_ = appState;      
    }
       
    /***************************************************************************
    **
    ** Get the export settings dialog
    ** 
    */
 
    DialogAndInProcessCmd getSettingsDialog(ServerControlFlowHarness cfh) {
      Dimension dim = appState_.getZoomTarget().getBasicSize(true, true, ZoomTarget.VISIBLE_MODULES);
      ExportSettingsPublishDialogFactory.BuildArgs ba = 
        new ExportSettingsPublishDialogFactory.BuildArgs(null, dim.width, dim.height);
      ExportSettingsPublishDialogFactory df = new ExportSettingsPublishDialogFactory(cfh);
      this.nextStep_ = "extractExportSettings";
      return (new DialogAndInProcessCmd(df.getDialog(ba), this));
    }
    
    /***************************************************************************
    **
    ** Get the export settings dialog
    ** 
    */
 
    DialogAndInProcessCmd extractExportSettings(DialogAndInProcessCmd cmd) {
      set = (ExportSettings)cmd.cfhui;     
      nextStep_ = "getFileDialog"; 
      return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, this));
    }

    /***************************************************************************
    **
    ** Get the file
    ** 
    */
   
    DialogAndInProcessCmd getFileDialog(DialogAndInProcessCmd cmd) {
      List<String> suffs = ImageExporter.getFileSuffixesForType(set.formatType);     
      ArrayList<FileFilter> filts = new ArrayList<FileFilter>();
      filts.add(new FileExtensionFilters.MultiExtensionFilter(appState_, suffs, "filterName.img"));
      String pref = ImageExporter.getPreferredSuffixForType(set.formatType);
      file = fprep.getOrCreateWritableFileWithSuffix("ExportDirectory", filts, suffs, pref); 
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
     
    private DialogAndInProcessCmd processCommand() {
      try {
        appState_.getSUPanel().exportToFile(file, false, set.formatType, set.res, set.zoomVal, set.size, appState_);
        fprep.setPreference("ExportDirectory", file.getAbsoluteFile().getParent());
        return (new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.DONE, this));
      } catch (IOException ioe) {
        SimpleUserFeedback suf = fprep.generateSUFForWriteError(ioe);
        return (new DialogAndInProcessCmd(suf, this));
      }
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** User input data
  ** 
  */

  public static class ExportSettings implements ServerControlFlowHarness.UserInputs {
    public double zoomVal;
    public String formatType;
    public ImageExporter.ResolutionSettings res;
    public Dimension size;
    private boolean haveResult;
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }     
    
    public boolean isForApply() {
      return (false);
    } 
  } 
}
