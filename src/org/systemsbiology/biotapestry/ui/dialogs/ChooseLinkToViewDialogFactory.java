package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

public class ChooseLinkToViewDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
	public ChooseLinkToViewDialogFactory(ServerControlFlowHarness cfh) {
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
	    
	    Vector<ObjChoiceContent> choices = buildLinkChoices(dniba.genomeID, dniba.linkIds, dniba.isRoot,cfh.getDataAccessContext());
	    
	    switch(platform.getPlatform()) {
	    	case DESKTOP:
		      return (new DesktopDialog(cfh, dniba.genomeID, dniba.isRoot, choices));    		
	    	case WEB:
	    		return (new SerializableDialog(cfh, dniba.genomeID, dniba.isRoot, choices, dniba.linkObjID));
	    	default:
	    		throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
	    }
	  }
	
	  protected Vector<ObjChoiceContent> buildLinkChoices(String genomeID, Set<String> linkIDs, boolean forRoot, DataAccessContext dacx) {
		    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
		    Set<String> seen = new HashSet<String>();
		    Genome genome = (forRoot) ? dacx.getDBGenome() : dacx.getGenomeSource().getGenome(genomeID);
		    Iterator<String> lit = linkIDs.iterator();
		    while (lit.hasNext()) {
		      String linkID = lit.next();
		      linkID = (forRoot) ? GenomeItemInstance.getBaseID(linkID) : linkID;
		      if (!seen.contains(linkID)) {
		        Linkage link = genome.getLinkage(linkID);
		        if (link == null) { // for incomplete VFNs
		          continue;
		        }
		        retval.add(new ObjChoiceContent(link.getDisplayString(genome, false), linkID));
		        seen.add(linkID);
		      }
		    }
		    return (retval);
		  } 
	
	////////////////////////////////////////////////////////
	// SerializableDialog
	////////////////////////////////////////////////////////
	
	public static class SerializableDialog implements SerializableDialogPlatform.Dialog {
		private static final long serialVersionUID = 1L;
		
		private String linkResult_;
		private boolean haveResult_;
		protected ResourceManager rMan_;
		private ServerControlFlowHarness cfh_;
		private LinkRequest linkReq_;
		private Vector<ObjChoiceContent> choices_;
		private XPlatPrimitiveElementFactory primElemFac_;
		private String linkObjID_;
		
		private XPlatUIDialog xplatDialog_;
		
		public SerializableDialog(ServerControlFlowHarness cfh,String genomeID, boolean forRoot, Vector<ObjChoiceContent> choices, String linkObjID) {
			this.cfh_ = cfh;
			this.choices_ = choices;
			this.rMan_ = cfh.getDataAccessContext().getRMan();
			this.primElemFac_ = new XPlatPrimitiveElementFactory(this.rMan_);
			this.linkObjID_ = linkObjID;
		}
		
		
		private void buildDialog(String title, int height,int width,FlowKey okayKey) {
			Map<String,String> clickActions = new HashMap<String,String>();		  
			clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");
			clickActions.put("ok", okayKey.toString());
		  
			this.xplatDialog_ = new XPlatUIDialog(title,height,width,"");
		  
			this.xplatDialog_.setParameter("id", "popDisplayLinkData");
			
			XPlatUICollectionElement dialogCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
			dialogCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
			dialogCollection.setParameter("gutters", "false");
			
			this.xplatDialog_.createCollectionList("main", "center");
			this.xplatDialog_.createCollectionList("main", "bottom");
			
			// the link combo
			Map<String,Object> linkChoices = new HashMap<String,Object>();
			for(ObjChoiceContent choice : choices_) {
				linkChoices.put(choice.val,choice.name);
			}
			XPlatUIPrimitiveElement linkList = this.primElemFac_.makeTxtComboBox(
				"linkList", choices_.get(0).val, null, this.rMan_.getString("linkToView.choose"), linkChoices
			);
			linkList.setParameter("bundleAs", "linkID");
			linkList.setParameter("style", "width: 450px;");
			dialogCollection.addElement("center", linkList);			
			
			// The buttons
			XPlatUIPrimitiveElement okBtn = this.primElemFac_.makeOkButton(clickActions.get("ok"), null, true);
			XPlatUIPrimitiveElement cancelBtn = this.primElemFac_.makeCancelButton(clickActions.get("cancel"),null);
			okBtn.setParameter("class", "RightHand");
			cancelBtn.setParameter("class", "RightHand");
			okBtn.getEvent("click").addParameter("objID", linkObjID_);
			dialogCollection.addElement("bottom",cancelBtn);
			dialogCollection.addElement("bottom",okBtn);
			
			this.xplatDialog_.setCancel(clickActions.get("cancel"));
			
			this.xplatDialog_.setUserInputs(new LinkRequest(true));
			
		}
		
		public XPlatUIDialog getDialog() {

			return getDialog(FlowMeister.PopFlow.DISPLAY_LINK_DATA);
		}

		public XPlatUIDialog getDialog(FlowKey keyVal) {
			
			buildDialog(this.rMan_.getString("linkToView.title"),200,700,keyVal);
			return this.xplatDialog_;
		}
		
		
		public SimpleUserFeedback checkForErrors(UserInputs ui) {
			return null;
		}		
		
		public boolean dialogIsModal() {
      return (true);
    }

		public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {

			return null;
		}

	}
	
	
	////////////////////////////////////////////////////
	// DesktopDialog
	////////////////////////////////////////////////////
	
	
	/****************************************************************************
	**
	** Dialog box for choosing a link to view
	*/

	public static class DesktopDialog extends JDialog implements DesktopDialogPlatform.Dialog {
	      
	  ////////////////////////////////////////////////////////////////////////////
	  //
	  // PRIVATE INSTANCE MEMBERS
	  //
	  ////////////////////////////////////////////////////////////////////////////  

	  private JComboBox chooseCombo_;
	  private String linkResult_;
	  private static final long serialVersionUID = 1L;
	  protected JPanel cp_;
	  protected ResourceManager rMan_;
	  protected DialogSupport ds_;
	  protected GridBagConstraints gbc_;
	  protected int rowNum_;
	  protected int columns_;
	  private ClientControlFlowHarness cfh_;
	  private LinkRequest linkReq_;
	  private UIComponentSource uics_;
	  private DataAccessContext dacx_;
	  
	  ////////////////////////////////////////////////////////////////////////////
	  //
	  // PUBLIC CONSTRUCTORS
	  //
	  ////////////////////////////////////////////////////////////////////////////    

	  /***************************************************************************
	  **
	  ** Constructor 
	  */ 
	  
	  public DesktopDialog(ServerControlFlowHarness cfh, String genomeID, boolean forRoot, Vector choices) {   
		  
		    super(cfh.getUI().getTopFrame(), cfh.getDataAccessContext().getRMan().getString("linkToView.title"), true);
		    setSize(700, 200);
		    uics_ = cfh.getUI();
		    dacx_ = cfh.getDataAccessContext();
		    cp_ = (JPanel)getContentPane();
		    cp_.setBorder(new EmptyBorder(20, 20, 20, 20));
		    cp_.setLayout(new GridBagLayout());
		    gbc_ = new GridBagConstraints();
		    rowNum_ = 0;
		    columns_ = 3;
		    cfh_ = cfh.getClientHarness();
		    rMan_ = dacx_.getRMan();   
	   
	    //
	    // Build the link choices:
	    //
	    
	    JLabel label = new JLabel(rMan_.getString("linkToView.choose"));
	    chooseCombo_ = new JComboBox(choices);
	    
	    double vFac = 1.0;
	    int eorw = UiUtil.E;
	    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, eorw, 0.0, vFac);
	    cp_.add(label, gbc_);
	    UiUtil.gbcSet(gbc_, 1, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, vFac);    
	    cp_.add(chooseCombo_, gbc_); 
	    
	    FixedJButton okBtn = new FixedJButton(rMan_.getString("dialogs.ok"));
	    okBtn.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
	        try {
	        	linkReq_ = new LinkRequest(((ObjChoiceContent)chooseCombo_.getSelectedItem()).val);
				cfh_.sendUserInputs(linkReq_);
				DesktopDialog.this.setVisible(false);
				DesktopDialog.this.dispose();
	        } catch (Exception ex) {
	          uics_.getExceptionHandler().displayException(ex);
	        } catch (OutOfMemoryError oom) {
	          uics_.getExceptionHandler().displayOutOfMemory(oom);
	        }
	      }
	    });     
	    FixedJButton cancelBtn = new FixedJButton(rMan_.getString("dialogs.cancel"));
	    cancelBtn.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
	        try {
				cfh_.sendUserInputs(null);
				DesktopDialog.this.setVisible(false);
				DesktopDialog.this.dispose();
	        } catch (Exception ex) {
	          uics_.getExceptionHandler().displayException(ex);
	        } catch (OutOfMemoryError oom) {
	          uics_.getExceptionHandler().displayOutOfMemory(oom);
	        }
	      }
	    });    
	    
	    Box buttonPanel = Box.createHorizontalBox();
	    buttonPanel.add(Box.createHorizontalGlue());
	    buttonPanel.add(okBtn);
	    buttonPanel.add(Box.createHorizontalStrut(10));    
	    buttonPanel.add(cancelBtn);

	    UiUtil.gbcSet(gbc_, 0, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
	    cp_.add(buttonPanel, gbc_);
	    
	    setLocationRelativeTo(uics_.getTopFrame());
	    
	  }

	  public boolean dialogIsModal() {
		  return true;
	  }

	  /***************************************************************************
	  **
	  ** Get the result type
	  ** 
	  */
	  
	  public String getLink() {
	    return (linkResult_);
	  }  

	  ////////////////////////////////////////////////////////////////////////////
	  //
	  // PROTECTED/PRIVATE METHODS
	  //
	  ////////////////////////////////////////////////////////////////////////////
 

	  /***************************************************************************
	  **
	  ** Stash our results for later interrogation
	  ** 
	  */
	  
	  protected boolean stashForOK() {  
	    ObjChoiceContent linkSelection = (ObjChoiceContent)chooseCombo_.getSelectedItem();
	    linkResult_ = linkSelection.val;
	    return (true);
	  }    
	}
	
	
	
	
	  ////////////////////////////////////////////////////////////////////////////
	  //
	  // BUILD ARGUMENT CLASS
	  //
	  ////////////////////////////////////////////////////////////////////////////    
	  
	  public static class BuildArgs extends DialogBuildArgs { 
	    
	    Genome genome;
	    String genomeID;
	    boolean isRoot;
	    Set<String> linkIds;
	    String linkObjID;
	          
	    public BuildArgs(Genome genome, String genomeID, Set<String> linkIds, boolean isRoot,String linkObjID) {
	      super(genome);
	      this.genome = genome;
	      this.genomeID = genomeID;
	      this.isRoot = isRoot;
	      this.linkIds = linkIds; 
	      this.linkObjID = linkObjID;
	    } 
	  }
	  
		/***************************************************************************
		 ** 
		 ** User input data transmitted from dialog
		 ** 
		 */

		public static class LinkRequest implements ServerControlFlowHarness.UserInputs {

			public String linkID;

			public boolean haveResult; // not used...

			public LinkRequest() {

			}
			
			public LinkRequest(String linkID) {
				this.linkID = linkID;
				this.haveResult = true;
			}

			/**
			 * When an object is being serialized, null object members will NOT be
			 * included, so give empty values to make sure everything is
			 * transmitted.
			 * 
			 * 
			 * @param forTransit
			 */
			public LinkRequest(boolean forTransit) {
				this.linkID = "";
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