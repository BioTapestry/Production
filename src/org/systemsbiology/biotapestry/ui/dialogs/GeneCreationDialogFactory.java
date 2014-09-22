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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Dialog box for creating gene nodes
*/

public class GeneCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GeneCreationDialogFactory(ServerControlFlowHarness cfh) {
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
    boolean needModOpt = (dniba.overlayKey != null) && (dniba.modKeys.size() > 0);
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.defaultName, dniba.overlayKey, dniba.modKeys, needModOpt));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      //return (new SimpleWebDialog(cfh, dniba.defaultName));   
    	return (new SerializableDialog(cfh, dniba.defaultName,dniba.defaultName));
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    String defaultName;
    String overlayKey; 
    Set<String> modKeys;
          
    public BuildArgs(String defaultName, String overlayKey, Set<String> modKeys) {
      super(null);
      this.defaultName = defaultName;
      this.overlayKey = overlayKey;
      this.modKeys = modKeys;    
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
   
    private JTextField nameField_;
    private JCheckBox addToModule_;
    
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
    
    DesktopDialog(ServerControlFlowHarness cfh, String defaultName, String overlayKey, Set<String> modKeys, boolean needModOpt) {
      super(cfh, "addGene.ChooseTitle", new Dimension(500, 200), 2, new CreateRequest(), false);
  
      JLabel label = new JLabel(rMan_.getString("addGene.ChooseName"));
      nameField_ = new JTextField(defaultName);
      addLabeledWidget(label, nameField_, false, true);
      
      //
      // If net modules are currently on display, then we provide the option of adding
      // into the module automatically:
      //
  
      if (needModOpt) {
        addToModule_ = new JCheckBox(rMan_.getString("addGene.addToModule"));
        addToModule_.setSelected(true);
        addWidgetFullRow(addToModule_, false);       
      }
      
      finishConstruction();
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Do the bundle 
    */
    
    @Override
    protected boolean bundleForExit(boolean forApply) {
      CreateRequest crq = (CreateRequest)request_;           
      crq.nameResult = nameField_.getText().trim();   
      if (crq.nameResult.equals("")) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("addGene.EmptyName");
        String title = rMan.getString("addGene.CreationErrorTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      } else if (!queBombForName(crq.nameResult)) {
        return (false);
      }  
      crq.doModuleAdd = (addToModule_ != null) ? addToModule_.isSelected() : false;
      crq.haveResult = true;
      return (true);
    }
    
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
     
    private boolean queBombForName(String nameResult) {
      RemoteRequest daBomb = new RemoteRequest("queBombNameMatchForGeneCreate");
      daBomb.setStringArg("nameResult", nameResult); 
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      SimpleUserFeedback suf = dbres.getSimpleUserFeedback();
      if (suf != null) {
        cfh_.showSimpleUserFeedback(suf);
      }
      if (dbres.keepGoing() == RemoteRequest.Progress.STOP) {
        return (false);
      } else {
        return (true);
      }
    }
  }

  
  //////////////////////////////////////////////////////////////////////
  // SerializableDialog
  //////////////////////////////////////////////////////////////////////
  //
  // XPlat Implementation of this dialog
  
  public static class SerializableDialog implements SerializableDialogPlatform.Dialog {
	  protected BTState appState_;
	  private XPlatUIDialog xplatDialog_;
	  private ServerControlFlowHarness scfh_;
	  private XPlatPrimitiveElementFactory primElemFac_; 
	  private ResourceManager rMan_;
	  
	  public SerializableDialog(
		  ServerControlFlowHarness cfh,
		  String dialogTitle,
		  String defaultGeneName
	  ){
		  this.scfh_ = cfh;
		  this.appState_ = cfh.getBTState();
		  this.rMan_ = this.appState_.getRMan();
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan_);		  
		  buildDialog(dialogTitle,300,300,defaultGeneName);  
	  }
	  
	  public boolean isModal() {
	    return (true);
	  }
	  
	  private void buildDialog(String title, int height, int width, String defaultGeneName) {
		  ResourceManager rMan = appState_.getRMan();
	  
		  this.xplatDialog_ = new XPlatUIDialog(title,height,width);
		  
		  this.xplatDialog_.setParameter("id", defaultGeneName.replaceAll("\\s+", "_").toLowerCase());
		  
		  XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		  layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
		  layoutCollection.setParameter("gutters", "false");
		  
		  this.xplatDialog_.createCollectionList("main", "center");
		  
		  XPlatUIPrimitiveElement geneNameTxtBox = this.primElemFac_.makeTextBox(
			  "gene_name", false, defaultGeneName, null, true, rMan.getString("addGene.ChooseName"),
			  true
		  );
		  geneNameTxtBox.setParameter("required", new Boolean(true));
		  geneNameTxtBox.setParameter("missingMessage", scfh_.getBTState().getRMan().getString("addGene.EmptyName"));
		  geneNameTxtBox.setParameter("bundleAs", "nameResult");
		  
		  
		  this.xplatDialog_.addElementToCollection(
			  "main",
			  "center",
			  geneNameTxtBox
		  );
		  
		  this.xplatDialog_.createCollectionList("main", "bottom");
		  this.xplatDialog_.setUserInputs(new CreateRequest());
 
	  }
	  
	  /**
	   * Shim to let it implement the interface; we should probably remove getHTML...
	   * 
	   * 
	   * 
	   * @return
	   */
	  public String getHTML(String hiddenForm) {
		  return null;
	  }
	  
	  public XPlatUIDialog getDialog() {
		  
		  Map<String,String> clickActions = new HashMap<String,String>();
		  clickActions.put("cancel", FlowMeister.MainFlow.CANCEL_ADD_MODE.toString());
		  clickActions.put("ok", "DO_NOTHING");
		  
		  List<XPlatUIElement> btns = this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false); 
		  
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  
		  this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);
		  
		  return xplatDialog_;
	  }	  
	  
	  
	  public XPlatUIDialog getDialog(FlowKey okKeyVal) {
		  
		  Map<String,String> clickActions = new HashMap<String,String>();
		  clickActions.put("cancel", FlowMeister.MainFlow.CANCEL_ADD_MODE.toString());
		  clickActions.put("ok", okKeyVal.toString());
		  
		  List<XPlatUIElement> btns = this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false); 
		  
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  
		  this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);
		  
		  return xplatDialog_;
	  }
	  
	  /***************************************************************************
	   **
	   ** Return the parameters we are interested in:
	   */
	    
	  public Set<String> getRequiredParameters() {
		  HashSet<String> retval = new HashSet<String>();
	      retval.add("gene");
	      retval.add("action");
	      return (retval);
	  }   
	      
	  /***************************************************************************
	   **
	   ** Talk to the expert!
	   ** 
	   */
	    public SimpleUserFeedback checkForErrors(UserInputs ui) { 	
	    	RemoteRequest daBomb = new RemoteRequest("queBombNameMatchForGeneCreate");
	    	daBomb.setStringArg("nameResult", ((CreateRequest)ui).nameResult); 
	    	RemoteRequest.Result dbres = scfh_.receiveRemoteRequest(daBomb);
	    	return (dbres.getSimpleUserFeedback());
	    }
	 
	  /***************************************************************************
	   **
	   ** Do the bundle 
	   */  
	  public ServerControlFlowHarness.UserInputs bundleForExit(Map<String, String> params) {
		  return null;
	  }	  
  } // Serializable Dialog
  
  
  
  /***************************************************************************
   **
   ** User input data
   ** 
   */
  
  public static class CreateRequest implements UserInputs {
	   private boolean haveResult; 
	   public String nameResult;
	   public boolean doModuleAdd;
	   
	   public CreateRequest() {
		   this.nameResult = "";
		   this.haveResult = true;
		   this.doModuleAdd = false;
	   }
     
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
