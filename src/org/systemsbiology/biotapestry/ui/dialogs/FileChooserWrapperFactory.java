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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.app.PathAndFileSource;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.FilePreparer;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;


/****************************************************************************
**
** Frames for showing a path dialog
*/

public class FileChooserWrapperFactory extends DialogFactory {
	
	private static final String charEncoding_ = "UTF-8";
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public FileChooserWrapperFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
	  BuildArgs dniba = (BuildArgs)ba;
    
	  switch(platform.getPlatform()) {
	  	case DESKTOP:   
      		return (new DesktopFileChooserWrapper(cfh, dniba));   
      	case WEB:
      		return (new SerializableDialog(cfh, dniba));
      	default:
      		throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
	  }   
  }
  
  
  public static class SerializableDialog implements SerializableDialogPlatform.Dialog {
	  private XPlatUIDialog xplatDialog_;
	  private ServerControlFlowHarness scfh_;
	  private XPlatPrimitiveElementFactory primElemFac_; 
	  private BuildArgs ba_;
	  private List<String> fileNames_;
	  
	  public SerializableDialog(
		  ServerControlFlowHarness cfh, 
		  BuildArgs ba
      ){
		  this.scfh_ = cfh;
		  ResourceManager rMan = scfh_.getDataAccessContext().getRMan();
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan);
		  this.ba_ = ba;
	  }
	  
	  public boolean dialogIsModal() {
		  return (true);
	  }

	  private void buildDialog(String title, int height,int width) {
		  
		  this.xplatDialog_ = new XPlatUIDialog(title,height,width);
		  
		  XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		  layoutCollection.setParameter("style", "height: " + (height-50) + "px; width: " + (width-50) + "px;");
		  
		  this.xplatDialog_.createCollectionList("main", "center");
		  this.xplatDialog_.createCollectionList("main", "bottom");
		  		  
		  List<Map<String,Object>> selCol = new ArrayList<Map<String,Object>>();
		  List<Map<String, Object>> files = getServerFiles(this.scfh_.getPathAndFileSource());
		  
		  Map<String,Object> fileName = new HashMap<String,Object>();
		  fileName.put("label","File Name");
		  fileName.put("field","fileName");
		  
		  Map<String,Object> fileSize = new HashMap<String,Object>();
		  fileSize.put("label","File Size");
		  fileSize.put("field","fileSize");
		  
		  Map<String,Object> checked = new HashMap<String,Object>();
		  checked.put("label","checked");
		  checked.put("field","checked");
		  checked.put("hidden","true");
		  
		  selCol.add(fileName);
		  selCol.add(fileSize);
		  selCol.add(checked);
		  
		  this.fileNames_ = new ArrayList<String>();
		  
		  for(Map<String,Object> file : files) {
			  file.put("checked","true");
			  fileNames_.add((String)file.get("fileName"));
		  }
		  
		  Collections.sort(this.fileNames_);
		  		  		  
		  XPlatUIPrimitiveElement selectionList = this.primElemFac_.makeListMultiSelection(
			  null, "file_list_grid",null, selCol, files, null,true,true,true
		  );
		  		  
		  selectionList.setParameter("id", "file_open_chooser_list");
		  selectionList.setParameter("name", "selection");
		  
		  selectionList.setEvent("dgrid-select", new XPlatUIEvent("dgrid-select","CLIENT_SET_ELEMENT_CONDITION"));
		  selectionList.getEvent("dgrid-select").addParameter("conditionValueLoc", "ELEMENT_ROWS");
		  selectionList.getEvent("dgrid-select").addParameter("conditionCol", "checked");
		  
		  selectionList.setEvent("dgrid-deselect", new XPlatUIEvent("dgrid-deselect","CLIENT_SET_ELEMENT_CONDITION"));
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionValueLoc", "EVENT");
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionCol", "checked");
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionValue", "false");
		  
		  selectionList.setParameter("bundleAs", "fileName");
		  
		  this.xplatDialog_.addElementToCollection("main", "center", selectionList);
		  
		  this.xplatDialog_.addDefaultState_("checked", "false");
		  
		  this.xplatDialog_.setUserInputs(new FileChooserResults(true));
		  
	  }
	  
	  public XPlatUIDialog getDialog() {
		 return getDialog("LOAD_FILE");
	  }
	  
	  public XPlatUIDialog getDialog(FlowKey key) {
		  return getDialog(key.toString());
	  }
	  
	  public XPlatUIDialog getDialog(String  okKey) {
		  
		  this.buildDialog("Select a file to open", 250, 575);
		  
		  XPlatUIPrimitiveElement okBtn = this.primElemFac_.makeOkButton(okKey, null, true);
		  okBtn.setValidity("checked","true");
		  XPlatUIPrimitiveElement cancelBtn = this.primElemFac_.makeCancelButton("CLIENT_CANCEL_COMMAND", null);
		  
		  this.xplatDialog_.setCancel("CLIENT_CANCEL_COMMAND");
		  this.xplatDialog_.addElementToCollection("main","bottom",okBtn);
		  this.xplatDialog_.addElementToCollection("main","bottom",cancelBtn);
		  
		  return this.xplatDialog_;
	  }

	/**
	 * checkForErrors(UserInputs ui)
	 * 
	 * 
	 * @param ui The UserInputs object which will contain the filename to be opened; this object
	 * can be null!
	 * 
	 */
	public SimpleUserFeedback checkForErrors(UserInputs ui) {
	  				
	  PathAndFileSource pafs = this.scfh_.getPathAndFileSource();
		if(ui != null && Collections.binarySearch(this.fileNames_,((FileChooserResults)ui).fileName) >= 0) {
			((FileChooserResults)ui).filePath = pafs.getFullServletContextPath() + pafs.getServerBtpDirectory().toString() + "/";
		} else {
			// TODO: file name requested was not found in the list of approved file names; this should produce an error
		}
		
		return null;
	}

	public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
		return null;
	}
	  
	  
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  /***************************************************************************
  **
  ** Get the names that are OK for the server files:
  *
  */  
  
  protected static List<Map<String,Object>> getServerFiles(PathAndFileSource pafs) {   
	  
	  String modelListFileName = pafs.getServerBtpFileList();
	  String configDirectory = pafs.getFullServletContextPath() + pafs.getServerConfigDirectory().toString() + "/";
	  String fileDirectory = pafs.getFullServletContextPath() + pafs.getServerBtpDirectory().toString() + "/";
	  
	  Scanner scanner = null;
  
	  List<Map<String,Object>> fileList = null;
	  
	  try {

		if(modelListFileName != null) {
			ArrayList<String> modelList = null;
			DecimalFormat df = new DecimalFormat("#.##");
			
			File modelListFile = new File(configDirectory + modelListFileName);
			if(modelListFile.exists()) {
				scanner = new Scanner(new FileInputStream(modelListFile), charEncoding_);							
		    	modelList = new ArrayList<String>();
		    	while (scanner.hasNextLine()){
		    		modelList.add(scanner.nextLine());
		    	}
		    	if(modelList.size() > 0) {
		    		fileList = new ArrayList<Map<String,Object>>();
					for(String filename : modelList) {

						long fileSize = new File(fileDirectory + filename).length();
						
						if(fileSize > 0) {
							String filesize = (fileSize > 1024*1024 
									? df.format((double) fileSize/(double)(1024*1024)) + " M" 
									: df.format((double) fileSize/(double)1024) + " K"
								) + "B";
							Map<String,Object> fileEntry = new HashMap<String,Object>();
							fileEntry.put("fileName",filename);
							fileEntry.put("fileSize",filesize);
							fileList.add(fileEntry);
						}
					}

		    	} else {
		    		throw new IllegalArgumentException("No model file names found in " + modelListFile);
		    	}
				    	
			} else {
				throw new IllegalArgumentException("Could not open the model file list " + modelListFile);
			}
		} else {
			throw new IllegalArgumentException("No model list file was supplied in configuration.txt!");
		}
	  } catch (Exception e) {
		  throw new IllegalArgumentException(e.getMessage());
	  } finally {
		  if(scanner != null) {
			  scanner.close();
		  }
	  }
	  
	  return fileList;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
   
  public enum DialogMode { EXISTING_IMPORT, WRITABLE_WITH_SUFFIX; }
    
    DialogMode mode;
    FileFilter myFilter;
    FilePreparer fPrep;
    String pref;
    String prefSuffix;
    List<FileFilter> filters; 
    List<String> suffixes;
          
    public BuildArgs(DialogMode mode, FilePreparer fPrep, FileFilter daFilter, String pref) {
      super(null);
      this.mode = mode;
      this.fPrep = fPrep;
      this.myFilter = daFilter;
      this.pref = pref;
    }
    
    public BuildArgs(DialogMode mode,FilePreparer fPrep, List<FileFilter> filters, String pref) {
      super(null);
      this.mode = mode;
      this.fPrep = fPrep;
      this.filters = filters;
      this.pref = pref;
    }

    public BuildArgs(FilePreparer fPrep, 
                     List<FileFilter> filters, List<String> suffixes, String prefSuffix, String prefDir) {
      super(null);
      this.fPrep = fPrep;
      this.filters = filters;
      this.suffixes = suffixes;
      this.prefSuffix = prefSuffix;     
      this.pref = prefDir;
    }
  }
  
  public static class DesktopFileChooserWrapper implements DesktopDialogPlatform.Dialog {

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
     
    private BuildArgs myBA_;
    protected ClientControlFlowHarness cfh_;
      
    private static final long serialVersionUID = 1L;
  
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    public DesktopFileChooserWrapper(ServerControlFlowHarness cfh, BuildArgs myBA) {
      myBA_ = myBA;
      cfh_ = cfh.getClientHarness();
    }     

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** These are required.
    */  
 
    public boolean dialogIsModal() {
      return (true);
    }
    
    /***************************************************************************
    **
    ** Get the info and send it back
    */  
    
    public void launch() {
      File result;
      
      switch(myBA_.mode) {
        case EXISTING_IMPORT:
          if (myBA_.myFilter != null) {
            result = myBA_.fPrep.getExistingImportFile(myBA_.pref, myBA_.myFilter);
          } else if (myBA_.filters != null) {
            result = myBA_.fPrep.getExistingImportFile("ImportDirectory", myBA_.filters);
          } else {
            throw new IllegalStateException();
          }
          break;
        case WRITABLE_WITH_SUFFIX:
          result = myBA_.fPrep.getOrCreateWritableFileWithSuffix(myBA_.pref, myBA_.filters, myBA_.suffixes, myBA_.prefSuffix);
          break;
        default:
          throw new IllegalStateException();
      }
 
      FileChooserResults fcr = new FileChooserResults();
      if (result == null) {
        cfh_.sendUserInputs(null);
      } else {
        fcr.haveResult = true;       
        fcr.fileName = result.getName();
        fcr.filePath = result.getParentFile().getAbsolutePath();
        cfh_.sendUserInputs(fcr);
      }
      return;  
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class FileChooserResults implements ServerControlFlowHarness.UserInputs {   
      
    public String fileName;
    public String filePath;
    public boolean haveResult;
    
    public FileChooserResults(boolean forTransit) {
    	if(forTransit) {
	    	this.fileName = "";
	    	this.filePath = "";
	    	this.haveResult = true;
    	}
    }
    
    public FileChooserResults() {
    }    
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }
    
	public void setHasResults() {
		this.haveResult = true;
		return;
	}   
     
    public boolean isForApply() {
      return (false);
    }
  }  
}
