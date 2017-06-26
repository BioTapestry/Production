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

import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
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

public class RetitleTabDialogFactory extends DialogFactory {
	
	private static final int MAX_TITLE_LENGTH = 50;
	private static final int MAX_DESC_LENGTH = 500;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public RetitleTabDialogFactory(ServerControlFlowHarness cfh) {
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
        
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.defaultTitle, dniba.defaultDesc));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {   
    	return (new SerializableDialog(cfh,null,dniba.defaultTitle, dniba.defaultDesc));
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
    
    String defaultTitle;
    String defaultDesc;
          
    public BuildArgs(String defaultTitle, String defaultDesc) {
      super(null);
      this.defaultTitle = defaultTitle;
      this.defaultDesc = defaultDesc;
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
    private JTextField descField_;
    
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
    
    DesktopDialog(ServerControlFlowHarness cfh, String defaultTitle, String defaultDesc) {
      super(cfh, "changeTabTitle.title", new Dimension(500, 200), 1, new RetitleRequest(), false);
  
      JLabel label = new JLabel(rMan_.getString("changeTabTitle.enterTitle"));
      addWidgetFullRow(label, true);
      
      nameField_ = new JTextField(defaultTitle);
      addWidgetFullRow(nameField_, true);
      
      label = new JLabel(rMan_.getString("changeTabDesc.enterDesc"));
      addWidgetFullRow(label, true);
      
      descField_ = new JTextField(defaultDesc);
      addWidgetFullRow(descField_, true);
    
      finishConstruction();
    }
  
    public boolean dialogIsModal() {
      return (true);
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
      RetitleRequest crq = (RetitleRequest)request_;           
      crq.titleResult = nameField_.getText().trim();
      crq.fullTitleResult = crq.titleResult;
      crq.descResult = descField_.getText().trim();
      crq.haveResult = false;
      
      if (crq.titleResult.equals("")) {
        ResourceManager rMan = uics_.getRMan();
        String message = rMan.getString("changeTabTitle.emptyTitle");
        String title = rMan.getString("changeTabTitle.TitleErrorTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
      
      if (!queBombForTitle(crq.fullTitleResult)) {
        return (false);
      }
          
      if(crq.titleResult.length() > MAX_TITLE_LENGTH) {
          ResourceManager rMan = uics_.getRMan();
          String message = rMan.getString("changeTabTitle.tooLong");
          String title = rMan.getString("changeTabTitle.TitleErrorTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
          cfh_.showSimpleUserFeedback(suf);

          switch(suf.getIntegerResult()) {
	          case SimpleUserFeedback.YES:
	        	  crq.titleResult = crq.titleResult.substring(0, 46);
	        	  crq.titleResult += "...";
	        	  break;
	        	  
	          case SimpleUserFeedback.NO:
	        	  return (false);
	        	default:
	        	  throw new IllegalStateException();
          }	  
      } 
      
      if(crq.descResult.equals("")) {
          ResourceManager rMan = uics_.getRMan();
          String message = rMan.getString("changeTabDesc.emptyDesc");
          String title = rMan.getString("changeTabDesc.DescWarnTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
          cfh_.showSimpleUserFeedback(suf);
          
          switch(suf.getIntegerResult()) {
          case SimpleUserFeedback.YES:
            break;
            
          case SimpleUserFeedback.NO:
            return (false);
          default:
            throw new IllegalStateException();
          }
      } else if(crq.descResult.length() > MAX_DESC_LENGTH) {
          ResourceManager rMan = uics_.getRMan();
          String message = rMan.getString("changeTabDesc.tooLong");
          String title = rMan.getString("changeTabDesc.DescErrorTitle");
          SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
          cfh_.showSimpleUserFeedback(suf);
          return (false);
      }
      crq.haveResult = true;
      return crq.haveResult;
    }

    /***************************************************************************
     * We restrict tab (full length) titles so they are unique
     *
     * 
     */
     
    
    private boolean queBombForTitle(String titleResult) {
      RemoteRequest daBomb = new RemoteRequest("queBombMatchForTabTitle");
      daBomb.setStringArg("titleResult", titleResult); 
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
	  private XPlatUIDialog xplatDialog_;
	  private ServerControlFlowHarness scfh_;
	  private XPlatPrimitiveElementFactory primElemFac_; 
	  
	  public SerializableDialog(
		  ServerControlFlowHarness cfh,
		  String dialogTitle,
		  String defaultTabTitle,
		  String defaultTabDesc
	  ){
		  this.scfh_ = cfh;
		  ResourceManager rMan = scfh_.getDataAccessContext().getRMan();
		  dialogTitle = (dialogTitle == null ? rMan.getString("changeTabTitle.title") : dialogTitle);
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan);		  
		  buildDialog(dialogTitle,200,550,defaultTabTitle, defaultTabDesc);  
	  }
	  
    public boolean dialogIsModal() {
      return (true);
    }
	  
	  private void buildDialog(String title, int height, int width, String defaultTabTitle, String defaultTabDesc) {
		  ResourceManager rMan = scfh_.getDataAccessContext().getRMan();
	  
		  this.xplatDialog_ = new XPlatUIDialog(title,height,width);
		  
		  this.xplatDialog_.setParameter("id", "changeTabTitle");
		  
		  XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		  layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
		  layoutCollection.setParameter("gutters", "false");
		  
		  this.xplatDialog_.createCollectionList("main", "center");
		  
		  XPlatUIPrimitiveElement tabTitleTextBox = this.primElemFac_.makeTextBox(
			  "tab_title", false, defaultTabTitle, null, true, rMan.getString("changeTabTitle.enterTitle"),
			  true
		  );
		  
		  tabTitleTextBox.setParameter("required", new Boolean(true));
		  tabTitleTextBox.setParameter("style", "width: 325px; margin-top: 7px;");
		  tabTitleTextBox.setParameter("invalidMessage", rMan.getString("changeTabTitle.tooLongWarn"));
		  tabTitleTextBox.setParameter("regExp", "^.{0,50}$");		  
		  tabTitleTextBox.setParameter("missingMessage", rMan.getString("changeTabTitle.emptyTitle"));
		  tabTitleTextBox.setParameter("bundleAs", "titleResult");
		  		  
	      XPlatUIPrimitiveElement tabDescTextBox = this.primElemFac_.makeTextBox(
	        "tab_desc", false, defaultTabDesc, null, true, rMan.getString("changeTabDesc.enterDesc"),
	        true
	      );
	      tabDescTextBox.setParameter("required", false);
	      tabDescTextBox.setParameter("style", "width: 500px; margin-top: 7px;");
	      tabDescTextBox.setParameter("invalidMessage", rMan.getString("changeTabDesc.tooLongWarn"));
	      tabDescTextBox.setParameter("regExp", "^.{0,500}$");
	      tabDescTextBox.setParameter("bundleAs", "descResult");
	      
	      
	      this.xplatDialog_.addElementToCollection(
	        "main",
	        "center",
	        tabDescTextBox
	      );
	      
		  this.xplatDialog_.addElementToCollection(
			  "main",
			  "center",
			  tabTitleTextBox
		  );
		  
		  this.xplatDialog_.createCollectionList("main", "bottom");
		  this.xplatDialog_.setUserInputs(new RetitleRequest());
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
		  clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");
		  clickActions.put("ok", "MAIN_RETITLE_TAB");
		  List<XPlatUIElement> btns = this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false); 
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);
		  
		  return xplatDialog_;
	  }	  
	  
	  
	  public XPlatUIDialog getDialog(FlowKey okKeyVal) {
		  
		  Map<String,String> clickActions = new HashMap<String,String>();
		  clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");
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
	      retval.add("title");
	      return (retval);
	  }   
	      
	  /***************************************************************************
	   **
	   ** Talk to the expert!
	   ** 
	   */
	    public SimpleUserFeedback checkForErrors(UserInputs ui) {

	    	SimpleUserFeedback suf = null;
	    	
	    	RetitleRequest rr = (RetitleRequest)ui;
	    	if(rr.fullTitleResult == null || rr.fullTitleResult.length() <= 0) {
	    		rr.fullTitleResult = rr.titleResult;
	    	}
	    	
	    	Map<String,String> clicks = new HashMap<String,String>();
	    	clicks.put("yes","MAIN_RETITLE_TAB");
	    	clicks.put("no","MAIN_RETITLE_TAB");
	    	ResourceManager rMan = scfh_.getDataAccessContext().getRMan();	
	    	
	    	if(rr.titleResult.length() > MAX_TITLE_LENGTH) {
	    		suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION,rMan.getString("changeTabTitle.tooLong"),rMan.getString("changeTabTitle.TitleErrorTitle"),clicks);
	    	} else if(rr.descResult.equals("")) {
	            suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION,rMan.getString("changeTabDesc.tooLong"),rMan.getString("changeTabDesc.DescWarnTitle"),clicks);
	        } else if(rr.descResult.length() > MAX_DESC_LENGTH) {
	          suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR,rMan.getString("changeTabDesc.tooLong"),rMan.getString("changeTabDesc.DescErrorTitle"));
	        } else {
		    	RemoteRequest daBomb = new RemoteRequest("queBombMatchForTabTitle");
		    	daBomb.setStringArg("titleResult", rr.titleResult); 
		    	RemoteRequest.Result dbres = scfh_.receiveRemoteRequest(daBomb);
		    	suf = dbres.getSimpleUserFeedback();	    		
	    	}
	    	return suf;
	    }
	    
	    
	    /*********************************
	     * handleSufResponse
	     ********************************
	     * 
	     * Deal with any SUF's response
	     * 
	     * @param daipc
	     * @return
	     * 
	     */
	    
	    public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
	    	
	        int choiceVal = daipc.suf.getIntegerResult();
	        RetitleRequest rr = (RetitleRequest)daipc.cfhui;
	        if (choiceVal != SimpleUserFeedback.NO) {
		        rr.fullTitleResult = rr.titleResult;
		        rr.titleResult = rr.titleResult.substring(0,46) + "...";
	        } else {
	        	rr.haveResult = false;
	        	daipc.state = DialogAndInProcessCmd.Progress.HAVE_DIALOG_TO_SHOW;
	        }
	        daipc.suf = null;
	        
	        return daipc;
	    }
	    
  } // Serializable Dialog
  
  
  /***************************************************************************
   **
   ** User input data
   ** 
   */
  
  public static class RetitleRequest implements UserInputs {
	   private boolean haveResult; 
	   public String titleResult;
	   public String fullTitleResult;
	   public String descResult;
	   
	   public RetitleRequest() {
		   this.titleResult = "";
		   this.fullTitleResult = "";
		   this.descResult = "";
		   this.haveResult = true;
	   }
     
	   public void clearHaveResults() {
		   haveResult = false;
		   return;
	   }   
		public void setHasResults() {
			this.haveResult = true;
			return;
		}  
	   public boolean haveResults() {
		   return (haveResult);
	   }      
	   public boolean isForApply() {
		   return (false);
	   } 
	   
	   public String toString() {
		   return "{\n\thaveResult:"+haveResult+","+
			   "\n\ttitleResult: \""+titleResult+"\","+
			   "\n\tfullTitleResult: \""+fullTitleResult+"\","+
			   "\n\tdescResult: \""+descResult+"\"\n}";
	   }
  }
  
}
