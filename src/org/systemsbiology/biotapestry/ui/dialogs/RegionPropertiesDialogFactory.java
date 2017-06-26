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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatResponse;
import org.systemsbiology.biotapestry.ui.xplat.XPlatStackPage;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;


public class RegionPropertiesDialogFactory extends DialogFactory {

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////    

	/***************************************************************************
	 **
	 ** Constructor 
	 */ 

	public RegionPropertiesDialogFactory(ServerControlFlowHarness cfh) {
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

		RegionPropBuildArgs dniba = (RegionPropBuildArgs)ba;

		if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
			return (new DesktopDialog(cfh, dniba.props, dniba.forSubGroup, dniba.groupName));
		} else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
			return (new SerializableDialog(cfh, dniba.props, dniba.forSubGroup,dniba.groupName));   
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

	public static class RegionPropBuildArgs extends DialogBuildArgs { 

		GroupProperties props;
		boolean forSubGroup;
		String groupName;

		public RegionPropBuildArgs(GroupProperties props, boolean forSubGroup, String groupName) {
			super(null);
			this.groupName = groupName;
			this.forSubGroup = forSubGroup;
			this.props = props;
		} 
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// DIALOG CLASSES
	//
	////////////////////////////////////////////////////////////////////////////    


	/****************************************************************************
	 **
	 ** Desktop Platform dialog box for editing group properties
	 */

	public class DesktopDialog extends BTTransmitResultsDialog implements DesktopDialogPlatform.Dialog {

		////////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INSTANCE MEMBERS
		//
		////////////////////////////////////////////////////////////////////////////  

		private JTextField nameField_;
		private ColorSelectionWidget colorWidget1_;
		private ColorSelectionWidget colorWidget2_;  
		private GroupProperties props_;
		private String layoutKey_;
		private JComboBox layerCombo_;
		private JComboBox tpadCombo_;
		private JComboBox bpadCombo_;
		private JComboBox lpadCombo_;
		private JComboBox rpadCombo_;
		private JComboBox hideCombo_;
		private String groupName_;
		private DataAccessContext dacx_;

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

		public DesktopDialog(ServerControlFlowHarness cfh, GroupProperties props, boolean forSubGroup, String groupName) {     
			super(cfh, "groupprop.title", new Dimension(900,400), 11, new RegionRequest(), true);
			this.groupName_ = groupName;
			props_ = props;
			dacx_ = cfh.getDataAccessContext();
			layoutKey_ = dacx_.getCurrentLayoutID();

			//
			// Build the name panel:
			//

			JLabel label = new JLabel(rMan_.getString("groupprop.name"));
			nameField_ = new JTextField();
			addLabeledWidget(label, nameField_, false, false);
	
			colorWidget1_ = new ColorSelectionWidget(uics_, dacx_.getColorResolver(), cfh.getHarnessBuilder(), null, true, "groupprop.activecolor", false, false);
			ArrayList<ColorDeletionListener> colorDeletionListeners = new ArrayList<ColorDeletionListener>();
			colorDeletionListeners.add(colorWidget1_);
			colorWidget2_ = new ColorSelectionWidget(uics_, dacx_.getColorResolver(), cfh.getHarnessBuilder(), colorDeletionListeners, true, "groupprop.inactivecolor", true, false);    

			addWidgetFullRow(colorWidget1_, false);
			addWidgetFullRow(colorWidget2_, false);    

			//
			// Add the layer and pad choices:
			//

			if (forSubGroup) {
				label = new JLabel(rMan_.getString("groupprop.layer"));
				Vector<Integer> layers = new Vector<Integer>();
				for (int i = 1; i <= GroupProperties.MAX_SUBGROUP_LAYER; i++) {
					layers.add(new Integer(i));
				}
				layerCombo_ = new JComboBox(layers);
				addLabeledWidgetNoStretch(label, layerCombo_, false, false);

			} else {
				layerCombo_ = null;
			}

			
			ArrayList<JLabel> labs = new ArrayList<JLabel>();
			labs.add(new JLabel(rMan_.getString("groupprop.tpad")));
			labs.add(new JLabel(rMan_.getString("groupprop.bpad")));
			labs.add(new JLabel(rMan_.getString("groupprop.lpad"))); 
			labs.add(new JLabel(rMan_.getString("groupprop.rpad")));  
			Vector<Integer> pads = new Vector<Integer>();
			for (int i = -40; i <= 400; i++) {
				pads.add(new Integer(i));
			}
			
			ArrayList<JComponent> comps = new ArrayList<JComponent>();	
			tpadCombo_ = new JComboBox(pads);
			comps.add(tpadCombo_);
			bpadCombo_ = new JComboBox(pads);
			comps.add(bpadCombo_);
			lpadCombo_ = new JComboBox(pads);
			comps.add(lpadCombo_);
			rpadCombo_ = new JComboBox(pads);
			comps.add(rpadCombo_);
		
			addSeriesToRow(labs, comps, 0);

			//
			// Build the hide region name
			//

			JLabel hlabel = new JLabel(rMan_.getString("groupprop.hideLabel"));
			Vector<ChoiceContent> hideChoices = GroupProperties.getLabelHidingChoices(dacx_);
			hideCombo_ = new JComboBox(hideChoices);
			addLabeledWidgetNoStretch(hlabel, hideCombo_, false, false);
			
			finishConstructionForApply();
			displayProperties();
		}

		public boolean dialogIsModal() {
			return (true);
		}
		
		////////////////////////////////////////////////////////////////////////////
		//
		// INNER CLASSES
		//
		////////////////////////////////////////////////////////////////////////////  

		/***************************************************************************
	    **
	    ** Do handling of the inputPane. Override if using input pane function.
	    */
  
		@Override
		public boolean processInputAnswer() {
		  throw new UnsupportedOperationException();
		}
		
		////////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE METHODS
		//
		////////////////////////////////////////////////////////////////////////////

		/***************************************************************************
		 **
		 ** Apply the current group property values to our UI components
		 ** 
		 */

		private void displayProperties() {
			String ref = props_.getReference();
			Layout layout = dacx_.getLayoutSource().getLayout(layoutKey_);
			String genomeKey = layout.getTarget();
			GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
			Group group = genomeInstance.getGroup(ref);

			nameField_.setText(group.getName());
			colorWidget1_.setCurrentColor(props_.getColorTag(true));    
			colorWidget2_.setCurrentColor(props_.getColorTag(false));

			if (layerCombo_ != null) {
				layerCombo_.setSelectedItem(new Integer(props_.getLayer()));
			}
			tpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.TOP)));
			bpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.BOTTOM)));
			lpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.LEFT)));
			rpadCombo_.setSelectedItem(new Integer(props_.getPadding(GroupProperties.RIGHT)));

			int hideMode = props_.getHideLabelMode();
			hideCombo_.setSelectedItem(GroupProperties.labelHidingForCombo(dacx_, hideMode));

			return;
		}


		//////////////////////
		// bundleForExit
		/////////////////////
		//
		//

		protected boolean bundleForExit(boolean forApply) {
			RegionRequest rrq = (RegionRequest)request_;
			rrq.setForApply(forApply);

			String nameResult = nameField_.getText();
			nameResult.trim();			

			//
			// This can get called from the main panel, or from the overlaid RegionNameChangeChoicesPanel. If the latter,
			// we dont't want to do the same check again. The RegionRequest has been modified with the answer from the overlay
			//

			String oldName = null;
			if (!showingInputPane()) {
				Layout layout = dacx_.getLayoutSource().getLayout(dacx_.getCurrentLayoutID());
				String genomeKey = layout.getTarget();
				GenomeInstance genomeInstance = (GenomeInstance)dacx_.getGenomeSource().getGenome(genomeKey);
				Group group = genomeInstance.getGroup(props_.getReference());
				oldName = group.getName();			
				// Names can be empty/null, but they cannot be duplicated, and changing them
				// might disconnect from underlying data
				if(!nameResult.equals(oldName)) {
					RemoteRequest daBomb = new RemoteRequest("queBombCheckName");
					daBomb.setStringArg("nameResult", nameResult); 
					daBomb.setStringArg("propsRef",props_.getReference());
					RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
					SimpleUserFeedback suf = dbres.getSimpleUserFeedback();
					if(suf != null) {
						cfh_.showSimpleUserFeedback(suf);
						return (false);
					}

					daBomb = new RemoteRequest("queBombCheckDataDisconnect");
					daBomb.setStringArg("nameResult", nameResult); 
					dbres = cfh_.routeRemoteRequest(daBomb);
					if (dbres.getBooleanAnswer("resolveDisconnect")) {
						rrq.nameMapping = false;
						daBomb = new RemoteRequest("queBombCheckOtherRegionsAttachedByDefault");
						dbres = cfh_.routeRemoteRequest(daBomb);
						boolean orabd = dbres.getBooleanAnswer("hasOtherRegionsAttached");		    	 
						JPanel pan = new RegionNameChangeChoicesPanel(this, uics_, dacx_, rrq, oldName, nameResult, orabd);
						buildExtraPane(pan);
						showInputPane(true);
						return (false);
					}
				}
			}

			//
			// See Issue #225. We MUST STOP, not provide an option to continue, if the user is saying to change the table guts.
			//

			if (!nameResult.equals(oldName)) {
			  if (rrq.handleDataNameChanges) {
  				RemoteRequest daBomb = new RemoteRequest("queBombDataNameCollision");
  				daBomb.setStringArg("nameResult", nameResult); 
  				RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
  				if (dbres.getBooleanAnswer("hasDataNameCollision")) {
  					ResourceManager rMan = dacx_.getRMan();
  					String message = MessageFormat.format(rMan.getString("regionNameChange.nameCollision"), new Object[] {nameResult}); 
  					String title = rMan.getString("regionNameChange.nameCollisionTitle");
  					SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
  					cfh_.showSimpleUserFeedback(suf);
  				  return (false);
  				}
			  }
			}
	
			rrq.nameResult = nameResult;

			rrq.lpadResult = ((Integer)lpadCombo_.getSelectedItem()).intValue();
			rrq.rpadResult = ((Integer)rpadCombo_.getSelectedItem()).intValue();
			rrq.tpadResult = ((Integer)tpadCombo_.getSelectedItem()).intValue();
			rrq.bpadResult = ((Integer)bpadCombo_.getSelectedItem()).intValue();

			rrq.hideLabelResult = ((ChoiceContent)hideCombo_.getSelectedItem()).val;

			rrq.activeColorResult = colorWidget1_.getCurrentColor();
			rrq.inactiveColorResult = colorWidget2_.getCurrentColor();

			if (layerCombo_ != null) {
				rrq.layerResult = ((Integer)layerCombo_.getSelectedItem()).intValue();
			}

			rrq.setHasResults();

			return true;
		}
	} // DesktopDialog

	////////////////////////////////////////////////////////////////////////////
	//
	// WEB DIALOG CLASS
	//
	////////////////////////////////////////////////////////////////////////////    

	public static class SerializableDialog implements SerializableDialogPlatform.Dialog { 

		////////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INSTANCE MEMBERS
		//
		////////////////////////////////////////////////////////////////////////////  

		// The dialog box
		private XPlatUIDialog xplatDialog_;

		private ServerControlFlowHarness scfh_;
		private ResourceManager rMan_; 
		private XPlatPrimitiveElementFactory primElemFac_;
		private boolean forSubGroup_;
		private GroupProperties props_;
		private FlowKey thisFlow_;

		////////////////////////////////////////////////////////////////////////////
		//
		// PUBLIC CONSTRUCTORS
		//
		////////////////////////////////////////////////////////////////////////////    

		/***************************************************************************
		 **
		 ** Constructor 
		 */ 

		public SerializableDialog(ServerControlFlowHarness cfh, GroupProperties props, boolean forSubGroup,String groupName) { 
			this.scfh_ = cfh;
			this.rMan_ = scfh_.getDataAccessContext().getRMan();
			this.primElemFac_ = new XPlatPrimitiveElementFactory(this.rMan_);
			this.forSubGroup_ = forSubGroup;
			this.props_ = props;

			buildDialog(this.rMan_.getString("groupprop.title"), 400, 650,groupName);
		}

		/////////////////
		// buildDialog
		////////////////
		//
		///
		private void buildDialog(String title, int height, int width,String groupName) {

			this.xplatDialog_ = new XPlatUIDialog(title,height,width);

	    this.xplatDialog_.setParameter("id", "regPropsDiag_" + (groupName != null ? groupName.replaceAll("\\s+", "_").toLowerCase() : "null"));
	    	
			XPlatUICollectionElement stackContainer = this.xplatDialog_.createCollection("main", XPlatUIElementType.STACK_CONTAINER);
			
			stackContainer.setParameter("id", this.xplatDialog_.getParameter("id")+XPlatUIElementType.STACK_CONTAINER.getIdSuffix());
			
			stackContainer.createList("RegProps");
			stackContainer.createList("NameChange");
			
			// Build our two stack pages
			XPlatUICollectionElement regPropsPage = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
			stackContainer.addElement("RegProps", regPropsPage);			
			buildRegPropsPage(regPropsPage,groupName,(String)stackContainer.getParameter("id"));
			
			XPlatUICollectionElement nameChangePage = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
			stackContainer.addElement("NameChange", nameChangePage);
			buildRegNameChangeChoicesPage(nameChangePage,groupName,(String)stackContainer.getParameter("id"),(String)regPropsPage.getParameter("id"));

			// Set the stack page order and current 'top' page
			stackContainer.addGroupOrder("RegProps", new Integer(0));
			stackContainer.addGroupOrder("NameChange", new Integer(1));

			stackContainer.addElementGroupParam("RegProps", "index", "0");
			stackContainer.addElementGroupParam("NameChange", "index", "1");

			stackContainer.setSelected("RegProps");
			
			this.xplatDialog_.setUserInputs(new RegionRequest(true));
		}

		
		//////////////////////
		// buildRegPropsPage
		//////////////////////
		//
		// Constructs the stack page which contains the initial set of region properties option
		// 
		public void buildRegPropsPage(XPlatUICollectionElement propsPage,String groupName,String stackContId) {
			
			propsPage.setParameter("id", stackContId + "_stackpage_0");
			propsPage.setParameter("style", "height: " + (this.xplatDialog_.getHeight()-10) + "px; width: " + (this.xplatDialog_.getWidth()-10) + "px;");
			propsPage.setParameter("gutters", "false");
			
			propsPage.createList("center");
			propsPage.createList("bottom");
			
			XPlatUIPrimitiveElement nameTxt = this.primElemFac_.makeTextBox("regionName", false, groupName, null, true, this.rMan_.getString("groupprop.name"), false);
			nameTxt.setParameter("bundleAs", "nameResult");
			
	    	List<NamedColor> ncList = new ArrayList<NamedColor>();
	    	DataAccessContext dacx = scfh_.getDataAccessContext();
	        Iterator<String> ckit = dacx.getColorResolver().getColorKeys();
	                
	        while (ckit.hasNext()) {
	        	String colorKey = ckit.next();
	        	NamedColor nc = dacx.getColorResolver().getNamedColor(colorKey);
	        	ncList.add(new NamedColor(nc));
	        }
	        
	        Collections.sort(ncList); 
	        
	        Map<String,Object> colorVals = new HashMap<String,Object>();
	        int count = 0; 
	        
	        for(NamedColor nc : ncList) {
	        	Map<String,String> colorMap = new HashMap<String,String>();
	        	colorMap.put("id", nc.key);
	        	colorMap.put("label", nc.name);
	        	colorMap.put("index",Integer.toString(count));
	        	colorMap.put("color", "#"+Integer.toHexString(nc.color.getRGB()).substring(2));
	        	colorVals.put(nc.key,colorMap);
	        	count++;
	        }
			
			XPlatUIPrimitiveElement inactiveColorCombo = this.primElemFac_.makeColorChooserComboBox(
				"inactive_color", props_.getColorTag(false), null, this.rMan_.getString("groupprop.inactivecolor"), colorVals
			);
			inactiveColorCombo.setFloat(true);
			inactiveColorCombo.setParameter("bundleAs", "inactiveColorResult");
			XPlatUIPrimitiveElement activeColorCombo = this.primElemFac_.makeColorChooserComboBox(
				"active_color", props_.getColorTag(true), null, this.rMan_.getString("groupprop.activecolor"), colorVals
			);
			activeColorCombo.setParameter("bundleAs", "activeColorResult");
			
	        XPlatUIPrimitiveElement colorEdBtn = this.primElemFac_.makeBasicButton(
        		this.rMan_.getString("colorSelector.colorNew"), "launch_color_ed", "CLIENT_LAUNCH_COLOR_EDITOR", null
        		);
	        colorEdBtn.setFloat(true);
	        colorEdBtn.getEvent("click").addParameter("colorChoices", colorVals);
	        	        
	        Map<String,Object> vals = new HashMap<String,Object>();
	        for (int i = -40; i <= 400; i++) {
	          vals.put(Integer.toString(i), Integer.toString(i));
	        }   
	        
	        XPlatUIPrimitiveElement lpadCombo = this.primElemFac_.makeTxtComboBox(
        		"lpad", Integer.toString(props_.getPadding(GroupProperties.LEFT)), null, this.rMan_.getString("groupprop.lpad"), vals
    		);
	        lpadCombo.setFloat(true);
	        lpadCombo.setParameter("class", "MiniCombo");
	        lpadCombo.setParameter("bundleAs", "lpadResult");
	        XPlatUIPrimitiveElement rpadCombo = this.primElemFac_.makeTxtComboBox(
        		"rpad", Integer.toString(props_.getPadding(GroupProperties.RIGHT)), null, this.rMan_.getString("groupprop.rpad"), vals
    		);
	        rpadCombo.setFloat(true);
	        rpadCombo.setParameter("class", "MiniCombo");
	        rpadCombo.setParameter("bundleAs", "rpadResult");
	        XPlatUIPrimitiveElement tpadCombo = this.primElemFac_.makeTxtComboBox(
        		"tpad", Integer.toString(props_.getPadding(GroupProperties.TOP)), null, this.rMan_.getString("groupprop.tpad"), vals
    		);
	        tpadCombo.setFloat(true);
	        tpadCombo.setParameter("class", "MiniCombo");
	        tpadCombo.setParameter("bundleAs", "tpadResult");
	        XPlatUIPrimitiveElement bpadCombo = this.primElemFac_.makeTxtComboBox(
        		"bpad", Integer.toString(props_.getPadding(GroupProperties.BOTTOM)), null, this.rMan_.getString("groupprop.bpad"), vals
    		);
	        bpadCombo.setFloat(true);
	        bpadCombo.setParameter("class", "MiniCombo");
	        bpadCombo.setParameter("bundleAs", "bpadResult");
	        
	        List<ChoiceContent> hideChoices = GroupProperties.getLabelHidingChoices(dacx);
	        Map<String,Object> hideVals = new HashMap<String,Object>();
	        
	        for(ChoiceContent hide : hideChoices) {
	        	hideVals.put(Integer.toString(hide.val),hide.name);
	        }
	        
	        XPlatUIPrimitiveElement hideCombo = this.primElemFac_.makeTxtComboBox(
        		"hide_label", Integer.toString(props_.getHideLabelMode()), null, this.rMan_.getString("groupprop.hideLabel"), hideVals
    		);
	        hideCombo.setParameter("bundleAs", "hideLabelResult");
	        
	        XPlatUIPrimitiveElement layerCombo = null;
	        if(this.forSubGroup_) {
				Map<String,Object> layers = new HashMap<String,Object>();
				for (int i = 1; i <= GroupProperties.MAX_SUBGROUP_LAYER; i++) {
					layers.put(Integer.toString(i),Integer.toString(i));
				}
				layerCombo = this.primElemFac_.makeTxtComboBox(
					"layer",Integer.toString(this.props_.getLayer()), null, this.rMan_.getString("groupprop.layer"), layers
				);
				layerCombo.setParameter("bundleAs", "layerResult");
	        }
	        
	        XPlatUICollectionElement topCenter = new XPlatUICollectionElement(XPlatUIElementType.PANE);
	        topCenter.createList("main");
	        XPlatUICollectionElement midCenter1 = new XPlatUICollectionElement(XPlatUIElementType.PANE);
	        midCenter1.createList("main");
	        XPlatUICollectionElement midCenter2 = new XPlatUICollectionElement(XPlatUIElementType.PANE);
	        midCenter2.createList("main");
	        XPlatUICollectionElement btmCenter = new XPlatUICollectionElement(XPlatUIElementType.PANE);
	        btmCenter.createList("main");
	        
	        topCenter.addElement("main", nameTxt);
	        
	        midCenter1.addElement("main", activeColorCombo);
	        midCenter1.addElement("main", inactiveColorCombo);
	        midCenter1.addElement("main", colorEdBtn);
	        
	        if(layerCombo != null) {
	        	midCenter2.addElement("main", layerCombo);	
	        }
	        midCenter2.addElement("main", hideCombo);
	        
	        btmCenter.addElement("main", tpadCombo);
	        btmCenter.addElement("main", bpadCombo);
	        btmCenter.addElement("main", lpadCombo);
	        btmCenter.addElement("main", rpadCombo);
	        
	        propsPage.addElement("center", topCenter);
	        propsPage.addElement("center", midCenter1);
	        propsPage.addElement("center", midCenter2);
	        propsPage.addElement("center", btmCenter);
			
			return;
		}
		
		
		////////////////////////////////////
		// buildRegNameChangeChoicesPage
		///////////////////////////////////
		//
		//
		public XPlatUICollectionElement buildRegNameChangeChoicesPage(
				XPlatUICollectionElement rnccPage,String oldName,String stackContId,String regPropsPgId
		) {
			// This is a placeholder which will be switched out later
			String newName = "{{newName}}";

			rnccPage.setParameter("style", "height: " + (this.xplatDialog_.getHeight()-10) + "px; width: " + (this.xplatDialog_.getWidth()-10) + "px;");
			rnccPage.setParameter("gutters", false);
			rnccPage.setParameter("id", stackContId + "_stackpage_1");
			
			XPlatUIPrimitiveElement desc = this.primElemFac_.makeTextMessage(
				"about", MessageFormat.format(this.rMan_.getString("regionNameChange.changeType"), new Object[] {oldName, newName}), null
			);
			
			XPlatUIPrimitiveElement choices = this.primElemFac_.makeSelectionGroup("nameChangeChoice", "", null, false, null, null);

			choices.addAvailVal("handleDataNameChanges", MessageFormat.format(this.rMan_.getString("regionNameChange.changeDataName"), new Object[] {oldName, newName}));
			choices.addAvailVal("networksToo", MessageFormat.format(this.rMan_.getString("regionNameChange.changeDataNameAndNetwork"), new Object[] {oldName, newName}));
		    choices.addAvailVal("createCustomMap",MessageFormat.format(this.rMan_.getString("regionNameChange.createCustomMap"), new Object[] {oldName}));     
		    choices.addAvailVal("breakAssociation", MessageFormat.format(this.rMan_.getString("regionNameChange.breakAssociation"), new Object[] {oldName}));

		    choices.setParameter("selValue", (choices.getAvailableValues().get("changeDataName") == null) ? "createCustomMap" : "changeDataName");
		    choices.setParameter("class", "Justified");
		    choices.setParameter("bundleAs", "nameChangeChoice");
		    
		    List<XPlatUIElement> btns = new ArrayList<XPlatUIElement>();
		    
		    XPlatUIPrimitiveElement okBtn = this.primElemFac_.makeOkButton("", null, true);
		    XPlatUIPrimitiveElement cancelBtn = this.primElemFac_.makeCancelButton("CLIENT_SHOW_STACKPAGE", null,false);
		    cancelBtn.getEvent("click").setParameter("containerId", stackContId);
		    cancelBtn.getEvent("click").setParameter("pageId", regPropsPgId);
		    
		    btns.add(okBtn);
		    btns.add(cancelBtn);
		    
		    rnccPage.createList("top");
		    rnccPage.addElement("top", desc);
		    rnccPage.createList("center");
		    rnccPage.addElement("center", choices);
		    rnccPage.createList("bottom");
		    rnccPage.addElements("bottom", btns);
			
			return rnccPage;
		}


		////////////////////////////////////////////////////////////////////////////
		//
		// PUBLIC METHODS
		//
		////////////////////////////////////////////////////////////////////////////


		///////////////
		// getDialog
		//////////////
		//
		//
		public XPlatUIDialog getDialog() {

			Map<String,String> clickActions = new HashMap<String,String>();
			clickActions.put("close", "CLIENT_CANCEL_COMMAND");
			clickActions.put("ok", "DO_NOTHING");
			clickActions.put("apply", "DO_NOTHING");

			List<XPlatUIElement> btns = this.primElemFac_.makeApplyOkCloseButtons(clickActions.get("ok"),false,clickActions.get("apply"),clickActions.get("close")); 

			this.xplatDialog_.setCancel(clickActions.get("close"));

			XPlatUICollectionElement regPropsPg = (XPlatUICollectionElement) this.xplatDialog_.getDialogElementCollection("main").getList("RegProps").get(0);
			regPropsPg.addElements("bottom", btns);
			
			XPlatUICollectionElement nameChangePg = (XPlatUICollectionElement) this.xplatDialog_.getDialogElementCollection("main").getList("NameChange").get(0);
			((XPlatUIPrimitiveElement)nameChangePg.getList("bottom").get(0)).getEvent("click").setCmdAction(clickActions.get("ok"));

			return xplatDialog_;
		}	  

		///////////////////////
		// getDialog(FlowKey)
		//////////////////////
		//
		//
		public XPlatUIDialog getDialog(FlowKey keyVal) {

			this.thisFlow_ = keyVal;
			
			Map<String,String> clickActions = new HashMap<String,String>();
			clickActions.put("close", "CLIENT_CANCEL_COMMAND");
			clickActions.put("ok", keyVal.toString());
			clickActions.put("apply", keyVal.toString());

			List<XPlatUIElement> btns = this.primElemFac_.makeApplyOkCloseButtons(clickActions.get("ok"),false,clickActions.get("apply"),clickActions.get("close"));
			
			this.xplatDialog_.setCancel(clickActions.get("close"));

			XPlatUICollectionElement regPropsPg = (XPlatUICollectionElement) this.xplatDialog_.getDialogElementCollection("main").getList("RegProps").get(0);
			regPropsPg.addElements("bottom", btns);
			
			XPlatUICollectionElement nameChangePg = (XPlatUICollectionElement) this.xplatDialog_.getDialogElementCollection("main").getList("NameChange").get(0);
			XPlatUIPrimitiveElement nameChangeOk = (XPlatUIPrimitiveElement)nameChangePg.getList("bottom").get(0);
			nameChangeOk.getEvent("click").setCmdAction(clickActions.get("ok"));
			nameChangeOk.getEvent("click").addParameter("nameMapping", true);
			nameChangeOk.setParameter("bundleAs", "nameMapping");
			
			return xplatDialog_;
		}

		///////////////
		// isModal()
		//////////////
		//
		//
		public boolean dialogIsModal() {
			return (true);
		}

		// Stub to satisfy interface
		public String getHTML(String hiddenForm) {
			return null;
		}   

		///////////////////
		// checkForErrors
		///////////////////
		//
		//
		public XPlatResponse checkForErrors(UserInputs ui) { 
			RegionRequest rrq = (RegionRequest)ui;
			
			if(rrq.nameMapping) {
				if(rrq.nameChangeChoice.equalsIgnoreCase("handleDataNameChanges")) {
					rrq.handleDataNameChanges = true;
				} else if(rrq.nameChangeChoice.equalsIgnoreCase("networksToo")) {
					rrq.handleDataNameChanges = true;
					rrq.networksToo = true;
				} else if(rrq.nameChangeChoice.equalsIgnoreCase("createCustomMap")) {
					rrq.createCustomMap = true;
				} else if(rrq.nameChangeChoice.equalsIgnoreCase("breakAssociation")) {
					rrq.breakAssociation = true;
				} else {
					throw new IllegalArgumentException("[ERROR] Invalid value for nameChangeChoice in ReqionRequesr!");
				}
				rrq.setHasResults();
				return null;
			}
			
			String nameResult = rrq.nameResult.trim();
	    		
			Layout layout = this.scfh_.getDataAccessContext().getLayoutSource().getLayout(this.scfh_.getDataAccessContext().getCurrentLayoutID());
			String genomeKey = layout.getTarget();
			GenomeInstance genomeInstance = (GenomeInstance)this.scfh_.getDataAccessContext().getGenomeSource().getGenome(genomeKey);
			Group group = genomeInstance.getGroup(props_.getReference());
			String oldName = group.getName();
			
			// If there's no name change, there's nothing to check
			if(nameResult.equals(oldName)) {
				rrq.setHasResults();
				return null;
			}
			
			// Names can be empty/null, but they cannot be duplicated, and changing them
			// might disconnect from underlying data
			
	    	RemoteRequest daBomb = new RemoteRequest("queBombCheckName");
	    	daBomb.setStringArg("nameResult", rrq.nameResult); 
	    	daBomb.setStringArg("propsRef",props_.getReference());
	    	RemoteRequest.Result dbres = scfh_.receiveRemoteRequest(daBomb);
			if(dbres.getSimpleUserFeedback() != null) {
				rrq.clearHaveResults();
				return (dbres.getSimpleUserFeedback());
			}

			daBomb = new RemoteRequest("queBombCheckDataDisconnect");
			daBomb.setStringArg("nameResult", nameResult); 
			dbres = scfh_.receiveRemoteRequest(daBomb);
			if (dbres.getBooleanAnswer("resolveDisconnect")) {
				String containerId = (String)this.xplatDialog_.getDialogElementCollection("main").getParameter("id");
				String pageId = containerId+"_stackpage_1";
				
				XPlatStackPage stackPage = new XPlatStackPage(containerId,pageId);
				rrq.nameMapping = false;
				daBomb = new RemoteRequest("queBombCheckOtherRegionsAttachedByDefault");
				dbres = scfh_.receiveRemoteRequest(daBomb);
				boolean orabd = dbres.getBooleanAnswer("hasOtherRegionsAttached");
				
				List<String> invalidChoices = null;
				
				if (!nameResult.trim().equals("")) {
					if (!orabd) {
						invalidChoices = new ArrayList<String>();
						invalidChoices.add("networksToo");
					}
				} else {
					invalidChoices = new ArrayList<String>();
					invalidChoices.add("handleDataNameChanges");
					invalidChoices.add("networksToo");
				}

				XPlatUICollectionElement nameChangePg = (XPlatUICollectionElement) this.xplatDialog_.getDialogElementCollection("main").getList("NameChange").get(0);
				String nameCCId = (String)((XPlatUIPrimitiveElement)nameChangePg.getList("center").get(0)).getParameter("id");
				String textMsgId = (String)((XPlatUIPrimitiveElement)nameChangePg.getList("top").get(0)).getParameter("id");
				
				if(invalidChoices != null) {
					stackPage.addWidgetSetting(nameCCId,"INVALID_CHOICES",invalidChoices);
				}
				
				Map<String,String> nameReplace = new HashMap<String,String>();
				
				nameReplace.put("oldVal", "{{newName}}");
				nameReplace.put("newVal",nameResult);
				
				stackPage.addWidgetSetting(nameCCId, "CHOICE_LABEL_STRING_REPLACE", nameReplace);
				stackPage.addWidgetSetting(textMsgId, "TEXT_CONTENT_REPLACE", nameReplace);
				
				return stackPage;
			}
			
			daBomb = new RemoteRequest("queBombDataNameCollision");
			daBomb.setStringArg("nameResult", nameResult); 
			dbres = this.scfh_.receiveRemoteRequest(daBomb);
			if (dbres.getBooleanAnswer("hasDataNameCollision") && !rrq.collisionIsOkay) {
				String message = MessageFormat.format(this.rMan_.getString("regionNameChange.nameCollision"), new Object[] {nameResult}); 
				String title = this.rMan_.getString("regionNameChange.nameCollisionTitle");
				rrq.clearHaveResults();
		    	Map<String,String> clicks = new HashMap<String,String>();
		    	clicks.put("yes",this.thisFlow_.toString());
		    	clicks.put("no",this.thisFlow_.toString());
				return new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title,clicks);
			}
			
			rrq.setHasResults();
	    	return null;
		}

		// Stub to satisfy interface
		public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
	        int choiceVal = daipc.suf.getIntegerResult();
	        RegionRequest rrq = (RegionRequest)daipc.cfhui;
	        if (choiceVal == SimpleUserFeedback.YES) {
	        	rrq.collisionIsOkay = true;
	        	rrq.setHasResults();
	         } else {
	        	 rrq.clearHaveResults();
	        	 daipc.state = DialogAndInProcessCmd.Progress.HAVE_DIALOG_TO_SHOW;
	         }
	        daipc.suf = null;
	        
	        return daipc;
		}
	} // SerializableDialog

	/***************************************************************************
	 **
	 ** Return Results
	 ** 
	 */

	public static class RegionRequest implements ServerControlFlowHarness.UserInputs {

		public String nameResult;
		// Because Layer result might not have a value, default to -1 so it will
		// default to 'unset'
		public int layerResult = -1;
		public int lpadResult;
		public int rpadResult;
		public int tpadResult;
		public int bpadResult;
		public String activeColorResult;
		public String inactiveColorResult;
		public int hideLabelResult;
		
		// If there was a name change, we may have data disconnect results as well
		public boolean nameMapping;
		public String nameChangeChoice;
		public boolean handleDataNameChanges;
		public boolean networksToo;
		public boolean createCustomMap;
		public boolean breakAssociation;
		
		// If there is a name collision, we need to make sure the user is OK with that
		// TODO: The flow should stop here and enforce a fix to the data table
		public boolean collisionIsOkay;
		
		private boolean haveResult;
		private boolean forApply_;

		public RegionRequest() {
			nameMapping = false;
		}

		// For serialization when null fields are excluded
		public RegionRequest(boolean forTransit) {
			haveResult = false;
			forApply_ = false;
			
			// Primary set of properties
			nameResult = "";
			layerResult = -1;
			lpadResult = -1;
			rpadResult = -1;
			tpadResult = -1;
			bpadResult = -1;
			activeColorResult = "";
			inactiveColorResult = "";
			hideLabelResult = -1;
			
			nameMapping = false;
			// Actual Radio button chosen
			nameChangeChoice = "";
			// The four possible values the radio button effects
			handleDataNameChanges = false;
			networksToo = false;
			createCustomMap = false;
			breakAssociation = false;
			
			// Response to SUF about name collision in data tables
			collisionIsOkay = false;
		}

		public void clearHaveResults() {
			haveResult = false;
			return;
		}

		public void setHasResults() {
			haveResult = true;
			return;
		}

		public boolean haveResults() {
			return (haveResult);
		}  

		public boolean isForApply() {
			return (forApply_);
		}

		// setters/getters for serialization

		public void setForApply(boolean forApply) {
			forApply_ = forApply;
			return;
		}

		public boolean getForApply() {
			return this.forApply_;
		}

		public boolean getHaveResult() {
			return haveResult;
		}

		public void setHaveResult(boolean haveResult) {
			this.haveResult = haveResult;
		}
	} // RegionRequest
}