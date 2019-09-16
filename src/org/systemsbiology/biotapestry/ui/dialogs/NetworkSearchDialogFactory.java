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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.search.NetworkSearch;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.ui.SourceAndTargetSelector;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent.XPlatUIElementProp;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory.RegionType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent.XPlatUIElementActionType;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
 ** 
 ** Dialog box for searching for nodes _or_ network modules
 */

public class NetworkSearchDialogFactory extends DialogFactory {

	// //////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	// //////////////////////////////////////////////////////////////////////////

	/***************************************************************************
	 ** 
	 ** Constructor
	 */

	public NetworkSearchDialogFactory(ServerControlFlowHarness cfh) {
		super(cfh);
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// FACTORY METHOD
	//
	// //////////////////////////////////////////////////////////////////////////

	/***************************************************************************
	 ** 
	 ** Get the appropriate dialog
	 */

	public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) {
		SearchDialogBuildArgs sdba = (SearchDialogBuildArgs) ba;

		switch (platform.getPlatform()) {
			case DESKTOP:
				return (new DesktopDialog(cfh, sdba.selectedName, sdba.currOvr,sdba.linksHidden));
			case WEB:
				return (new SerializableDialog(cfh, sdba.selectedName,sdba.currOvr, sdba.linksHidden));
			default:
				throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
		}

	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// COMMON HELPERS
	//
	// //////////////////////////////////////////////////////////////////////////

	/***************************************************************************
	 ** 
	 ** Get combo box guts
	 */

	private static Vector<TrueObjChoiceContent> getMatchChoices(BTState appState) {
		ResourceManager rMan = appState.getRMan();
		Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
		retval.add(new TrueObjChoiceContent(rMan.getString("nsearch.fullMatch"),NetworkSearch.MatchTypes.FULL_MATCH_));
		retval.add(new TrueObjChoiceContent(rMan.getString("nsearch.partialMatch"),NetworkSearch.MatchTypes.PARTIAL_MATCH_));
		return (retval);
	}

	/***************************************************************************
	 ** 
	 ** Get combo box guts for module method
	 */

	private static Vector<ChoiceContent> getModuleMethods(BTState appState,
		boolean haveNV, boolean haveTags) {
		ResourceManager rMan = appState.getRMan();
		Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
		if (haveTags) {
			retval.add(new ChoiceContent(rMan.getString("nsearch.moduleByKey"),NetOverlayOwner.MODULE_BY_KEY));
		}
		if (haveNV) {
			retval.add(new ChoiceContent(rMan.getString("nsearch.moduleByNameVal"),NetOverlayOwner.MODULE_BY_NAME_VALUE));
		}
		return (retval);
	}

	/***************************************************************************
	 ** 
	 ** Get combo box guts for tags
	 */

	private static Vector<String> getTagChoices(Set<String> globalTags) {
		Vector<String> retval = new Vector<String>();
		TreeSet<String> tagSet = new TreeSet<String>(globalTags);
		Iterator<String> tsit = tagSet.iterator();
		while (tsit.hasNext()) {
			String tag = tsit.next();
			retval.add(tag);
		}
		return (retval);
	}

	/***************************************************************************
	 ** 
	 ** Get combo box guts for names
	 */

	private static Vector<String> getNameChoices(Set<String> allNames) {
		Vector<String> retval = new Vector<String>();
		TreeSet<String> nameSet = new TreeSet<String>(allNames);
		Iterator<String> nsit = nameSet.iterator();
		while (nsit.hasNext()) {
			String name = nsit.next();
			retval.add(name);
		}
		return (retval);
	}

	/***************************************************************************
	 ** 
	 ** Get combo box guts for values (for a given name)
	 */

	private static Vector<String> getValueChoices(String name,
		Map<String, Set<String>> valsForNames) {
		Vector<String> retval = new Vector<String>();
		String normName = DataUtil.normKey(name);
		TreeSet<String> valSet = new TreeSet<String>(valsForNames.get(normName));
		Iterator<String> vsit = valSet.iterator();
		while (vsit.hasNext()) {
			String val = vsit.next();
			retval.add(val);
		}
		return (retval);
	}

	/***************************************************************************
	 ** 
	 ** Common module search build
	 */

	private static boolean buildModuleSearch(BTState appState, String currOvr,
		Map<String, Set<String>> valsForNames, HashSet<String> globalTags,
		HashSet<String> allNames) {
		HashSet<String> allValues = new HashSet<String>();
		FullGenomeHierarchyOracle oracle = new FullGenomeHierarchyOracle(appState);
		oracle.getGlobalTagsAndNVPairsForModules(valsForNames, allNames,allValues, globalTags, null);
		boolean enableModuleSearch = (currOvr != null) && ((allNames.size() > 0) || (globalTags.size() > 0));
		return (enableModuleSearch);
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// BUILD ARGUMENT CLASS
	//
	// //////////////////////////////////////////////////////////////////////////

	public static class SearchDialogBuildArgs extends DialogBuildArgs {

		String selectedName;
		String currOvr;
		boolean linksHidden;

		public SearchDialogBuildArgs(Genome genome, String selectedName,String currOvr, boolean linksHidden) {
			super(genome);
			this.selectedName = selectedName;
			this.currOvr = currOvr;
			this.linksHidden = linksHidden;
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// DIALOG CLASSES
	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////
	// SerializableDialog
	// //////////////////////////////////////////////////////
	//
	//

	public static class SerializableDialog implements SerializableDialogPlatform.Dialog {

		private BTState appState_;
		private XPlatUIDialog xplatDialog_;
		private ServerControlFlowHarness scfh_;
		private XPlatPrimitiveElementFactory primElemFac_;
		private ResourceManager rMan_;
		private String selectedName_;
		private String currOvr_;
		private boolean linksHidden_;

		public SerializableDialog(ServerControlFlowHarness cfh,String selectedName, String currOvr, boolean linksHidden) {
			this.scfh_ = cfh;
			this.appState_ = cfh.getBTState();
			this.rMan_ = this.appState_.getRMan();
			this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan_);
			this.currOvr_ = currOvr;
			this.linksHidden_ = linksHidden;
			this.selectedName_ = selectedName;
		}

		public boolean isModal() {
			return (true);
		}

		/**
		 * buildDialog
		 * 
		 * 
		 * 
		 * @param title
		 * @param height
		 * @param width
		 * @param keyVal
		 * 
		 */
		private void buildDialog(String title, int height, int width,FlowKey keyVal) {

			Map<String, String> clickActions = new HashMap<String, String>();
			clickActions.put("search",(keyVal == null ? "DO_NOTHING" : keyVal.toString()));
			clickActions.put("cancel", "CLIENT_CANCEL_COMMAND");

			this.xplatDialog_ = new XPlatUIDialog(title, height, width, "");

			this.xplatDialog_.setParameter("id", title.replaceAll("\\s+", "_").toLowerCase());

			// Set up the main dialog layout

			XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main",XPlatUIElementType.LAYOUT_CONTAINER);
			layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
			layoutCollection.setParameter("gutters", "false");
			this.xplatDialog_.createCollectionList("main", "center");
			this.xplatDialog_.createCollectionList("main", "bottom");

			// Set up the Tab container

			XPlatUICollectionElement tabContainer = new XPlatUICollectionElement(XPlatUIElementType.TAB_CONTAINER);
			tabContainer.setParameter("style", "height: " + (height - 30)+ "px; width: " + (width - 10) + "px;");
			tabContainer.setParameter(
				"id", this.primElemFac_.generateId("networkSearchTabCtrl", XPlatUIElementType.TAB_CONTAINER)
			);
			tabContainer.setParameter("gutters", "false");
			tabContainer.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.CENTER, 0));

			String nodeSearch = this.rMan_.getString("nsearch.nameSearch");
			String moduleSearch = this.rMan_.getString("nsearch.moduleSearch");

			tabContainer.createList(nodeSearch);
			tabContainer.createList(moduleSearch);

			tabContainer.addGroupOrder(nodeSearch, new Integer(0));
			tabContainer.addGroupOrder(moduleSearch, new Integer(1));

			tabContainer.addElementGroupParam(moduleSearch, "index", "1");
			tabContainer.addElementGroupParam(nodeSearch, "index", "0");

			tabContainer.setSelected(nodeSearch);

			XPlatUICollectionElement nodesLayout = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
			nodesLayout.createList("center");
			nodesLayout.createList("bottom");
			tabContainer.addElement(nodeSearch, nodesLayout);

			buildNodeSearchTab(nodesLayout);

			XPlatUICollectionElement networkModulesLayout = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
			networkModulesLayout.createList("center");
			networkModulesLayout.createList("bottom");
			tabContainer.addElement(moduleSearch, networkModulesLayout);

			this.xplatDialog_.addElementToCollection("main", "center",tabContainer);

			if (!buildModuleSearchTab(networkModulesLayout)) {
				tabContainer.addElementGroupParam(moduleSearch, "disabled","true");
			}

			XPlatUIPrimitiveElement searchBtn = this.primElemFac_.makeBasicButton(
				this.rMan_.getString("nsearch.search"),"search", clickActions.get("search"), null
			);
			searchBtn.getEvent("click").addUiElementProp(
				new XPlatUIElementProp((String) tabContainer.getParameter("id"), "selectedChildWidget","index", "tab", "whichTab")
			);
			searchBtn.setParameter("class", "RightHand");
			searchBtn.addUiElementEventAction("click",XPlatUIElementActionType.GET_ELEMENT_PROPS);
			
			XPlatUIPrimitiveElement cancelBtn = this.primElemFac_.makeClientCancelButton(null, keyVal);
			cancelBtn.getEvent("click").addParameter("action",keyVal.toString());
			cancelBtn.setParameter("class", "RightHand");
			
			this.xplatDialog_.addElementToCollection("main", "bottom", cancelBtn);
			this.xplatDialog_.addElementToCollection("main", "bottom", searchBtn);

			this.xplatDialog_.addDefaultState_(SourceAndTargetSelector.Searches.DIRECT_SELECT.toString(),"true");
			this.xplatDialog_.addDefaultState_(SourceAndTargetSelector.Searches.TARGET_SELECT.toString(),"false");
			this.xplatDialog_.addDefaultState_(SourceAndTargetSelector.Searches.SOURCE_SELECT.toString(),"false");
			this.xplatDialog_.addDefaultState_("searchMode", "0");

			this.xplatDialog_.setCancel(clickActions.get("cancel"));
			this.xplatDialog_.addCancelParameter("action", keyVal.toString());

			this.xplatDialog_.setUserInputs(new SearchRequest(true));

		}

		/**
		 * buildModuleSearchTab
		 * 
		 * 
		 * 
		 * 
		 * @param container
		 * 
		 */
		private boolean buildModuleSearchTab(XPlatUICollectionElement container) {

			Map<String, Set<String>> valsForNames_ = new HashMap<String, Set<String>>();
			HashSet<String> globalTags = new HashSet<String>();
			HashSet<String> allNames = new HashSet<String>();
			boolean enableModuleSearch = buildModuleSearch(appState_,this.currOvr_, valsForNames_, globalTags, allNames);

			if (enableModuleSearch) {
				// Search Type Combo
				Map<String, Object> methodVals = new HashMap<String, Object>();
				for (ChoiceContent cc : getModuleMethods(appState_,allNames.size() > 0, globalTags.size() > 0)) {
					methodVals.put(Integer.toString(cc.val), cc.name);
				}

				XPlatUIPrimitiveElement searchModeCombo = this.primElemFac_.makeTxtComboBox(
					"searchMode",Integer.toString(getModuleMethods(appState_,allNames.size() > 0,globalTags.size() > 0).get(0).val),
					null, true,this.rMan_.getString("nsearch.moduleMethod"),methodVals
				);
				searchModeCombo.setFloat(false);
				searchModeCombo.setParameter("bundleAs", "searchMode");
				searchModeCombo.setParameter("style", "width: 350px;");

				container.addElement("center", searchModeCombo);

				Map<String, Object> onChangeParams = new HashMap<String, Object>();
				onChangeParams.put("conditionValueLoc", "ELEMENT_NEWVAL");
				onChangeParams.put("conditionCol", "id");
				onChangeParams.put("conditionName", "searchMode");

				searchModeCombo.setEvent("change", new XPlatUIEvent("change","CLIENT_SET_ELEMENT_CONDITION", onChangeParams));

				// Key/Tag combo
				Map<String, Object> keys = new HashMap<String, Object>();
				for (String key : getTagChoices(globalTags)) {
					keys.put(key, key);
				}

				XPlatUIPrimitiveElement keyCombo = this.primElemFac_.makeTxtComboBox(
					"tagValue",Integer.toString(getModuleMethods(appState_,allNames.size() > 0,globalTags.size() > 0).get(0).val),
					null, true,this.rMan_.getString("nsearch.tagChoices"),keys
				);
				keyCombo.setFloat(false);
				keyCombo.setParameter("bundleAs", "tagVal");
				keyCombo.setValidity("searchMode",Integer.toString(NetOverlayOwner.MODULE_BY_KEY));

				container.addElement("center", keyCombo);

				// Name/Value Combos

				Map<String, Object> valueByName = new HashMap<String, Object>();
				Map<String, Object> nameVals = new HashMap<String, Object>();

				Vector<String> nameChoices = getNameChoices(allNames);

				for (String name : nameChoices) {
					List<String> valChoices = getValueChoices(name,valsForNames_);
					for (String val : valChoices) {
						Map<String, String> valContents = new HashMap<String, String>();
						valContents.put("name", name);
						valContents.put("label", val);
						valContents.put("id", val);
						valueByName.put(val + ":" + name, valContents);
					}
					nameVals.put(name, name);
				}

				XPlatUIPrimitiveElement valCombo = this.primElemFac_.makeTxtComboBox(
					"valChoice",Integer.toString(getModuleMethods(appState_,allNames.size() > 0,globalTags.size() > 0).get(0).val),
					null, true,this.rMan_.getString("nsearch.valueChoices"),valueByName
				);
				valCombo.setFloat(true);
				valCombo.setParameter("style", "width: 175px;");
				valCombo.setParameter("bundleAs", "valChoice");
				valCombo.setValidity("searchMode",Integer.toString(NetOverlayOwner.MODULE_BY_NAME_VALUE));

				XPlatUIPrimitiveElement nameCombo = this.primElemFac_.makeTxtComboBox(
					"nameChoice",Integer.toString(getModuleMethods(appState_,allNames.size() > 0,globalTags.size() > 0).get(0).val),
					null, true,	this.rMan_.getString("nsearch.nameChoices"),nameVals
				);
				nameCombo.setFloat(true);
				nameCombo.setParameter("bundleAs", "nameChoice");
				nameCombo.setParameter("style","width: 175px; margin-right: 15px;");
				nameCombo.setValidity("searchMode",Integer.toString(NetOverlayOwner.MODULE_BY_NAME_VALUE));
				XPlatUIEvent nameFilter = new XPlatUIEvent("change","DO_NOTHING");
				// TODO filter element applied locally
				nameFilter.addUiElementAction(XPlatUIElementActionType.FILTER_ELEMENT);
				nameFilter.addParameter("elementToFilter",valCombo.getParameter("id"));
				nameFilter.addParameter("filterOn", "name");
				nameFilter.addParameter("filterWith", "id");
				nameCombo.setEvent("change", nameFilter);

				XPlatUICollectionElement lowerPane = new XPlatUICollectionElement(XPlatUIElementType.PANE);
				lowerPane.setParameter("style", "height: 250px;");
				lowerPane.createList("main");
				lowerPane.addElement("main", nameCombo);
				lowerPane.addElement("main", valCombo);
				lowerPane.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 0));

				container.addElement("bottom", lowerPane);
			}

			return enableModuleSearch;

		}

		/**
		 * buildNodeSearchTab
		 * 
		 * 
		 * 
		 * 
		 * 
		 * @param container
		 * 
		 */
		private void buildNodeSearchTab(XPlatUICollectionElement container) {
			Map<String, String> searchTypeVals = new HashMap<String, String>();
			searchTypeVals.put(SourceAndTargetSelector.Searches.DIRECT_SELECT.toString(),this.rMan_.getString("nsearch.directSearch"));
			searchTypeVals.put(SourceAndTargetSelector.Searches.TARGET_SELECT.toString(),this.rMan_.getString("nsearch.targetSearch"));
			searchTypeVals.put(SourceAndTargetSelector.Searches.SOURCE_SELECT.toString(),this.rMan_.getString("nsearch.sourceSearch"));
			
			XPlatUIPrimitiveElement searchTypeSelex = this.primElemFac_.makeSelectionGroup(
				"searchType",SourceAndTargetSelector.Searches.DIRECT_SELECT.toString(), null, false, 
				this.rMan_.getString("nsearch.searchType"),searchTypeVals
			);
			List<String> valueOrder = new ArrayList<String>();
			valueOrder.add(SourceAndTargetSelector.Searches.DIRECT_SELECT.toString());
			valueOrder.add(SourceAndTargetSelector.Searches.TARGET_SELECT.toString());
			valueOrder.add(SourceAndTargetSelector.Searches.SOURCE_SELECT.toString());
			
			searchTypeSelex.setParameter("valueOrder", valueOrder);

			Map<String, Object> onChangeParams = new HashMap<String, Object>();
			onChangeParams.put("conditionValueLoc", "ELEMENT_VALUES");

			searchTypeSelex.setEvent("change", new XPlatUIEvent("change","CLIENT_SET_ELEMENT_CONDITION", onChangeParams));
			searchTypeSelex.setParameter("bundleAs", "selectionType");

			container.addElement("center", searchTypeSelex);

			XPlatUIPrimitiveElement itemNameTxtBox = this.primElemFac_.makeTextBox(
				"itemName", false, null, null, true,this.rMan_.getString("nsearch.searchString"), false
			);
			itemNameTxtBox.setFloat(true);
			itemNameTxtBox.setParameter("bundleAs", "search");

			Map<String, Object> matchVals = new HashMap<String, Object>();
			for (TrueObjChoiceContent tocc : getMatchChoices(this.appState_)) {
				matchVals.put(tocc.val.toString(), tocc.name);
			}

			XPlatUIPrimitiveElement matchTypeCombo = this.primElemFac_.makeTxtComboBox(
				"matchType",getMatchChoices(this.appState_).get(1).val.toString(), null, false, null, matchVals
			);
			matchTypeCombo.setFloat(true);
			matchTypeCombo.setParameter("style", "margin-left: 15px;");
			matchTypeCombo.setParameter("bundleAs", "matchType");

			container.addElement("center", itemNameTxtBox);
			container.addElement("center", matchTypeCombo);

			XPlatUICollectionElement borderedPane = new XPlatUICollectionElement(XPlatUIElementType.PANE);
			borderedPane.setParameter("style","border: 2px solid black; height: 150px;");

			List<XPlatUIElement> checkBoxes = new ArrayList<XPlatUIElement>();
			checkBoxes.add(this.primElemFac_.makeCheckbox(
				this.rMan_.getString("nsearch.includeItem"), null,"includeItem", true, "includeItem")
			);
			checkBoxes.add(this.primElemFac_.makeCheckbox(
				this.rMan_.getString("nsearch.includeLinks"), null,"includeLinks", !linksHidden_, "includeLinks")
			);
			checkBoxes.add(this.primElemFac_.makeCheckbox(
				this.rMan_.getString("nsearch.appendToCurrent"), null,"addToCurrent", false, "addToCurrent")
			);

			((XPlatUIPrimitiveElement) checkBoxes.get(0)).setValidity(SourceAndTargetSelector.Searches.DIRECT_SELECT.toString(),"false");
			((XPlatUIPrimitiveElement) checkBoxes.get(1)).setValidity(SourceAndTargetSelector.Searches.DIRECT_SELECT.toString(),"false");

			borderedPane.createList("main");
			borderedPane.addElements("main", checkBoxes);
			borderedPane.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 0));

			container.addElement("bottom", borderedPane);
		}

		/**
		 * getDialog
		 * 
		 * 
		 * 
		 * 
		 */
		public XPlatUIDialog getDialog(FlowKey keyVal) {
			buildDialog(this.rMan_.getString("nsearch.title"), 500, 700, keyVal);
			return xplatDialog_;
		}

		/**
		 * getDialog
		 * 
		 * 
		 * 
		 * 
		 */
		public XPlatUIDialog getDialog() {
			buildDialog(this.rMan_.getString("nsearch.title"), 500, 700, null);
			return xplatDialog_;
		}
		
		/**
		 * generateSearchRequest
		 * 
		 * 
		 * 
		 * 
		 * @param ui
		 * @return SearchRequest
		 * 
		 */
		private SearchRequest generateSearchRequest(UserInputs ui) {

			if (ui == null) {
				return null;
			}

			SearchRequest sr = (SearchRequest) ui;
			
			sr.clearCurrent = !((SearchRequest) ui).addToCurrent;

			if (sr.whichTab == 0) {
				sr.found = queBombForSearch(sr.search,sr.matchType);
			} else {
				if (!(sr.searchMode == NetOverlayOwner.MODULE_BY_KEY)) {
					sr.nvp = new NameValuePair(sr.nameChoice,sr.valChoice);
					sr.tagVal = null;
				} else {
					sr.nvp = null;
				}
				sr.found = queBombForModuleSearch(sr);
			}
			return sr;
		}
		
	    private Set<String> queBombForModuleSearch(SearchRequest request) {
	        RemoteRequest daBomb = new RemoteRequest("queBombFindModuleMatches");
	        if (request.tagVal != null) {
	          daBomb.setStringArg("tagVal", request.tagVal);
	        } else if (request.nvp != null) {
	          daBomb.setObjectArg("nvp", request.nvp);  
	        } else {
	          throw new IllegalArgumentException();
	        }
	        RemoteRequest.Result dbres = this.scfh_.receiveRemoteRequest(daBomb);
	        Set<String> retval = dbres.getSetAnswer("found");
	        return (retval);
	      }

		/**
		 * queBombForSearch
		 * 
		 * 
		 * 
		 * @param search
		 * @param matchType
		 * @return
		 * 
		 */
		private Set<String> queBombForSearch(String search,NetworkSearch.MatchTypes matchType) {
			RemoteRequest daBomb = new RemoteRequest("queBombFindNodeMatches");
			daBomb.setStringArg("search", search);
			daBomb.setObjectArg("matchType", matchType);
			RemoteRequest.Result dbres = this.scfh_.receiveRemoteRequest(daBomb);
			Set<String> retval = dbres.getSetAnswer("found");

			return (retval);
		}

		/**
		 * checkForErrors
		 * 
		 * 
		 * 
		 * 
		 */
		public SimpleUserFeedback checkForErrors(UserInputs ui) {
			SearchRequest sr = generateSearchRequest(ui);
			if (sr != null && sr.found != null && sr.found.isEmpty()) {
				ResourceManager rMan = appState_.getRMan();
				return new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR,
					rMan.getString("nsearch.nothingFound"),
					rMan.getString("nsearch.nothingFoundTitle"));
			}
			return null;
		}

	} // SerializableDialog
	

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////
	// DesktopDialog
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//

	public static class DesktopDialog extends JDialog implements DesktopDialogPlatform.Dialog {

		// //////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INSTANCE MEMBERS
		//
		// //////////////////////////////////////////////////////////////////////////

		private BTState appState_;

		private JCheckBox includeItemBox_;
		private JCheckBox includeLinksBox_;
		private JCheckBox addToCurrentBox_;
		private JComboBox matchTypeCombo_;

		private JTextField stringField_;
		private JRadioButton directSearch_;
		private JRadioButton targetSearch_;
		private JRadioButton sourceSearch_;

		private JTabbedPane tabPane_;
		private JComboBox moduleMethodChoices_;
		private JLabel moduleNameLabel_;
		private JComboBox moduleNameChoices_;
		private JLabel moduleValueLabel_;
		private JComboBox moduleValueChoices_;
		private JLabel moduleTagLabel_;
		private JComboBox moduleTagChoices_;

		private Map<String, Set<String>> valsForNames_;
		private boolean linksHidden_;
		private SearchRequest request_;
		private ClientControlFlowHarness cfh_;

		private static final long serialVersionUID = 1L;

		// //////////////////////////////////////////////////////////////////////////
		//
		// PUBLIC CONSTRUCTORS
		//
		// //////////////////////////////////////////////////////////////////////////

		/***************************************************************************
		 ** 
		 ** Constructor
		 */

		public DesktopDialog(ServerControlFlowHarness cfh, String selectedName,	String currOvr, boolean linksHidden) {
			super(cfh.getBTState().getTopFrame(), cfh.getBTState().getRMan().getString("nsearch.title"), true);
			appState_ = cfh.getBTState();
			linksHidden_ = linksHidden;
			cfh_ = cfh.getClientHarness();

			valsForNames_ = new HashMap<String, Set<String>>();
			HashSet<String> globalTags = new HashSet<String>();
			HashSet<String> allNames = new HashSet<String>();
			boolean enableModuleSearch = buildModuleSearch(appState_, currOvr,
					valsForNames_, globalTags, allNames);

			ResourceManager rMan = appState_.getRMan();
			setSize(600, 500);
			JPanel cp = (JPanel) getContentPane();
			cp.setBorder(new EmptyBorder(20, 20, 20, 20));
			cp.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			tabPane_ = new JTabbedPane();
			tabPane_.addTab(rMan.getString("nsearch.nameSearch"),
					buildNodeSearchTab(selectedName));
			tabPane_.addTab(
				rMan.getString("nsearch.moduleSearch"),buildModuleSearchTab(allNames, globalTags,enableModuleSearch)
			);
			tabPane_.setEnabledAt(1, enableModuleSearch);

			int rowNum = 0;
			UiUtil.gbcSet(gbc, 0, rowNum, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5,
					UiUtil.CEN, 1.0, 1.0);
			cp.add(tabPane_, gbc);

			rowNum += 8;

			//
			// Build the button panel:
			//

			FixedJButton buttonO = new FixedJButton(rMan.getString("nsearch.search"));
			buttonO.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						request_ = generateSearchRequest();
						if ((request_.found != null) && request_.found.isEmpty()) {
							ResourceManager rMan = appState_.getRMan();
							SimpleUserFeedback suf = new SimpleUserFeedback(
								SimpleUserFeedback.JOP.ERROR, rMan.getString("nsearch.nothingFound"),rMan.getString("nsearch.nothingFoundTitle")
							);
							cfh_.showSimpleUserFeedback(suf);
						} else {
							cfh_.sendUserInputs(request_);
						}
					} catch (Exception ex) {
						appState_.getExceptionHandler().displayException(ex);
					}
				}
			});
			FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
			buttonC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						cfh_.sendUserInputs(null);
						DesktopDialog.this.setVisible(false);
						DesktopDialog.this.dispose();
					} catch (Exception ex) {
						appState_.getExceptionHandler().displayException(ex);
					}
				}
			});
			Box buttonPanel = Box.createHorizontalBox();
			buttonPanel.add(Box.createHorizontalGlue());
			buttonPanel.add(buttonO);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(buttonC);

			//
			// Build the dialog:
			//
			UiUtil.gbcSet(gbc, 0, rowNum, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.SE, 1.0, 0.0);
			cp.add(buttonPanel, gbc);
			setLocationRelativeTo(appState_.getTopFrame());
		}

		// //////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INNER CLASSES
		//
		// //////////////////////////////////////////////////////////////////////////

		private class ButtonTracker implements ActionListener {
			public void actionPerformed(ActionEvent ev) {
				try {
					boolean isDirect = directSearch_.isSelected();
					includeItemBox_.setEnabled(!isDirect);
					includeLinksBox_.setEnabled(!linksHidden_ && !isDirect);
				} catch (Exception ex) {
					appState_.getExceptionHandler().displayException(ex);
				}
			}
		}

		// //////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE METHODS
		//
		// //////////////////////////////////////////////////////////////////////////

		/***************************************************************************
		 ** 
		 ** Node search tab
		 */

		private JPanel buildNodeSearchTab(String selectedName) {

			ResourceManager rMan = appState_.getRMan();
			JPanel cp = new JPanel();
			cp.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			//
			// Build the search selection buttons
			//

			JLabel label = new JLabel(rMan.getString("nsearch.searchType"));

			directSearch_ = new JRadioButton(rMan.getString("nsearch.directSearch"), true);
			targetSearch_ = new JRadioButton(rMan.getString("nsearch.targetSearch"), false);
			sourceSearch_ = new JRadioButton(rMan.getString("nsearch.sourceSearch"), false);

			ButtonGroup group = new ButtonGroup();
			group.add(directSearch_);
			group.add(targetSearch_);
			group.add(sourceSearch_);

			ButtonTracker bt = new ButtonTracker();

			directSearch_.addActionListener(bt);
			targetSearch_.addActionListener(bt);
			sourceSearch_.addActionListener(bt);

			int rowNum = 0;
			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,UiUtil.E, 0.0, 1.0);
			cp.add(label, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			cp.add(directSearch_, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			cp.add(targetSearch_, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			cp.add(sourceSearch_, gbc);

			//
			// Build the search string panel:
			//

			label = new JLabel(rMan.getString("nsearch.searchString"));
			if (selectedName != null) {
				stringField_ = new JTextField(selectedName);
			} else {
				stringField_ = new JTextField();
			}

			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,UiUtil.E, 0.0, 1.0);
			cp.add(label, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			cp.add(stringField_, gbc);

			matchTypeCombo_ = new JComboBox(getMatchChoices(appState_));

			UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 0.0, 1.0);
			cp.add(matchTypeCombo_, gbc);
			matchTypeCombo_.setSelectedIndex(1);

			JPanel extras = new JPanel();
			extras.setBorder(new EtchedBorder());
			extras.setLayout(new GridBagLayout());

			//
			// Build the extras panel:
			//

			includeItemBox_ = new JCheckBox(rMan.getString("nsearch.includeItem"), true);

			UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			extras.add(includeItemBox_, gbc);

			includeLinksBox_ = new JCheckBox(rMan.getString("nsearch.includeLinks"), !linksHidden_);

			UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			extras.add(includeLinksBox_, gbc);

			addToCurrentBox_ = new JCheckBox(rMan.getString("nsearch.appendToCurrent"), false);

			UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,UiUtil.CEN, 1.0, 1.0);
			extras.add(addToCurrentBox_, gbc);

			UiUtil.gbcSet(gbc, 0, rowNum, 3, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0,UiUtil.CEN, 1.0, 1.0);
			cp.add(extras, gbc);

			includeItemBox_.setEnabled(false);
			includeLinksBox_.setEnabled(false);
			return (cp);
		}

		/***************************************************************************
		 ** 
		 ** Module search tab
		 */

		private JPanel buildModuleSearchTab(Set<String> allNames,
				Set<String> globalTags, boolean enableModSearch) {

			ResourceManager rMan = appState_.getRMan();
			JPanel cp = new JPanel();
			cp.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			if (!enableModSearch) {
				return (cp);
			}

			//
			// Module method choice:
			//

			boolean haveNV = (allNames.size() > 0);
			boolean haveTags = (globalTags.size() > 0);

			int rowNum = 0;
			JLabel label = new JLabel(rMan.getString("nsearch.moduleMethod"));
			moduleMethodChoices_ = new JComboBox(getModuleMethods(appState_,haveNV, haveTags));
			moduleMethodChoices_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						ChoiceContent cc = (ChoiceContent) moduleMethodChoices_.getSelectedItem();
						boolean useTags = (cc.val == NetOverlayOwner.MODULE_BY_KEY);
						moduleTagLabel_.setEnabled(useTags);
						moduleTagChoices_.setEnabled(useTags);
						moduleNameLabel_.setEnabled(!useTags);
						moduleNameChoices_.setEnabled(!useTags);
						moduleValueLabel_.setEnabled(!useTags);
						moduleValueChoices_.setEnabled(!useTags);
					} catch (Exception ex) {
						appState_.getExceptionHandler().displayException(ex);
					}
					return;
				}
			});

			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,
					UiUtil.E, 0.0, 1.0);
			cp.add(label, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,
					UiUtil.W, 1.0, 1.0);
			cp.add(moduleMethodChoices_, gbc);

			//
			// Tag choice:
			//

			moduleTagLabel_ = new JLabel(rMan.getString("nsearch.tagChoices"));
			moduleTagChoices_ = new JComboBox(getTagChoices(globalTags));

			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,
					UiUtil.E, 0.0, 1.0);
			cp.add(moduleTagLabel_, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,
					UiUtil.W, 1.0, 1.0);
			cp.add(moduleTagChoices_, gbc);

			//
			// Name-value search options:
			//

			moduleNameLabel_ = new JLabel(rMan.getString("nsearch.nameChoices"));
			Vector<String> nameChoices = getNameChoices(allNames);
			moduleNameChoices_ = new JComboBox(nameChoices);
			// changes values when box is selected:
			moduleNameChoices_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						String name = (String) moduleNameChoices_
								.getSelectedItem();
						restockValues(name);
					} catch (Exception ex) {
						appState_.getExceptionHandler().displayException(ex);
					}
					return;
				}
			});

			moduleValueLabel_ = new JLabel(
					rMan.getString("nsearch.valueChoices"));
			moduleValueChoices_ = new JComboBox();

			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,
					UiUtil.E, 1.0, 1.0);
			cp.add(moduleNameLabel_, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,
					UiUtil.CEN, 1.0, 1.0);
			cp.add(moduleNameChoices_, gbc);

			UiUtil.gbcSet(gbc, 2, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5,
					UiUtil.E, 1.0, 1.0);
			cp.add(moduleValueLabel_, gbc);

			UiUtil.gbcSet(gbc, 3, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5,
					UiUtil.CEN, 1.0, 1.0);
			cp.add(moduleValueChoices_, gbc);

			ChoiceContent cc = (ChoiceContent) moduleMethodChoices_
					.getSelectedItem();
			boolean useTags = (cc.val == NetOverlayOwner.MODULE_BY_KEY);
			moduleNameLabel_.setEnabled(!useTags);
			moduleNameChoices_.setEnabled(!useTags);
			moduleValueLabel_.setEnabled(!useTags);
			moduleValueChoices_.setEnabled(!useTags);
			if (!useTags || (moduleMethodChoices_.getItemCount() == 2)) {
				String currName = (String) moduleNameChoices_.getSelectedItem();
				restockValues(currName);
			}

			return (cp);
		}

		/***************************************************************************
		 ** 
		 ** restock value box
		 */

		private void restockValues(String name) {
			moduleValueChoices_.removeAllItems();
			Vector<String> vals = getValueChoices(name, valsForNames_);
			int numVals = vals.size();
			for (int i = 0; i < numVals; i++) {
				moduleValueChoices_.addItem(vals.get(i));
			}
			moduleValueChoices_.validate();
			return;
		}

		/***************************************************************************
		 ** 
		 ** Get the user inputs
		 ** 
		 */

		private SearchRequest generateSearchRequest() {

			int whichTab = tabPane_.getSelectedIndex();
			SearchRequest retval = new SearchRequest(whichTab);

			if (whichTab == 0) {
				if (directSearch_.isSelected()) {
					retval.includeItem = false;
					retval.includeLinks = false;
					retval.selectionType = SourceAndTargetSelector.Searches.DIRECT_SELECT;
				} else {
					retval.includeItem = includeItemBox_.isSelected();
					retval.includeLinks = (!linksHidden_ && includeLinksBox_
							.isSelected());
					if (targetSearch_.isSelected()) {
						retval.selectionType = SourceAndTargetSelector.Searches.TARGET_SELECT;
					} else if (sourceSearch_.isSelected()) {
						retval.selectionType = SourceAndTargetSelector.Searches.SOURCE_SELECT;
					} else {
						throw new IllegalStateException();
					}
				}

				NetworkSearch.MatchTypes matchType = (NetworkSearch.MatchTypes) ((TrueObjChoiceContent) matchTypeCombo_
						.getSelectedItem()).val;
				String search = stringField_.getText().trim();
				retval.found = queBombForSearch(search, matchType);
				retval.clearCurrent = !addToCurrentBox_.isSelected();
			} else {
				ChoiceContent cc = (ChoiceContent) moduleMethodChoices_
						.getSelectedItem();
				retval.searchMode = cc.val;
				retval.useTags = (retval.searchMode == NetOverlayOwner.MODULE_BY_KEY);
				retval.tagVal = null;
				retval.nvp = null;
				if (retval.useTags) {
					retval.tagVal = (String) moduleTagChoices_
							.getSelectedItem();
				} else {
					String currName = (String) moduleNameChoices_
							.getSelectedItem();
					String currVal = (String) moduleValueChoices_
							.getSelectedItem();
					retval.nvp = new NameValuePair(currName, currVal);
				}
				retval.found = queBombForModuleSearch(retval);

			}
			return (retval);
		}

		/***************************************************************************
		 ** 
		 ** Talk to the expert!
		 ** 
		 */

		private Set<String> queBombForSearch(String search,NetworkSearch.MatchTypes matchType) {
			RemoteRequest daBomb = new RemoteRequest("queBombFindNodeMatches");
			daBomb.setStringArg("search", search);
			daBomb.setObjectArg("matchType", matchType);
			RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
			Set<String> retval = dbres.getSetAnswer("found");
			return (retval);
		}
		
		 /***************************************************************************
     ** 
     ** Talk to the expert!
     ** 
     */

    private Set<String> queBombForModuleSearch(SearchRequest request) {
      RemoteRequest daBomb = new RemoteRequest("queBombFindModuleMatches");
      if (request.tagVal != null) {
        daBomb.setStringArg("tagVal", request.tagVal);
      } else if (request.nvp != null) {
        daBomb.setObjectArg("nvp", request.nvp);  
      } else {
        throw new IllegalArgumentException();
      }
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      Set<String> retval = dbres.getSetAnswer("found");
      return (retval);
    }
	}

	/***************************************************************************
	 ** 
	 ** User input data transmitted from dialog
	 ** 
	 */

	public static class SearchRequest implements ServerControlFlowHarness.UserInputs {
		public String search;
		public int whichTab;
		public SourceAndTargetSelector.Searches selectionType;
		public NetworkSearch.MatchTypes matchType;
		public boolean includeItem;
		public boolean includeLinks;
		public boolean clearCurrent;
		public boolean addToCurrent;
		public int searchMode;
		public boolean useTags;

		public Genome genome;
		public Set<String> found;
		public String tagVal;
		public NameValuePair nvp;
		public String nameChoice;
		public String valChoice;
		public boolean haveResult; // not used...

		public SearchRequest() {

		}

		public SearchRequest(int whichTab) {
			this.whichTab = whichTab;
		}

		/**
		 * When an object is being serialized, null object members will NOT be
		 * included, so give empty values to make sure everything is
		 * transmitted.
		 * 
		 * 
		 * 
		 * 
		 * 
		 * @param forTransit
		 */
		public SearchRequest(boolean forTransit) {
			this.whichTab = -1;

			this.selectionType = SourceAndTargetSelector.Searches.DIRECT_SELECT;
			this.matchType = NetworkSearch.MatchTypes.FULL_MATCH_;
			this.includeItem = true;
			this.includeLinks = true;
			this.clearCurrent = true;
			this.addToCurrent = false;
			this.search = "";

			this.searchMode = -1;
			this.nameChoice = "";
			this.valChoice = "";
			this.tagVal = "";
		}

		public void clearHaveResults() {
			haveResult = false;
			return;
		}

		public boolean haveResults() {
			return (haveResult);
		}

		public boolean isForApply() {
			return (true);
		}
	}
}
