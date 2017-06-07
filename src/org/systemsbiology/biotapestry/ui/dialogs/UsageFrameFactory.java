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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIValidity;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.Tuple;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Frames for showing and navigating to link and node usages
*/

public class UsageFrameFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public UsageFrameFactory(ServerControlFlowHarness cfh) {
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
        TabPinnedDynamicDataAccessContext tpddacx = new TabPinnedDynamicDataAccessContext(dniba.ddacx, cfh.getTabSource().getCurrentTab());
        if (dniba.linkIDs != null) {
          return (new DesktopLinkUsageFrame(cfh.getUI(), tpddacx, cfh.getHarnessBuilder(), cfh.getTabSource().getCurrentTab(), dniba.linkIDs, dniba.nodeID));
        } else {
          return (new DesktopNodeUsageFrame(cfh.getUI(), tpddacx, cfh.getHarnessBuilder(), cfh.getTabSource().getCurrentTab(), dniba.nodeID));
        }
      case WEB:
    	  return (new SerializableDialog(cfh, dniba.nodeID, dniba.linkIDs));
      default:
        throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
    }
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
    
    Set<String> linkIDs; 
    String nodeID;
    DynamicDataAccessContext ddacx;
          
    public BuildArgs(DynamicDataAccessContext ddacx, Set<String> linkIDs, String nodeID) {
      super(null);
      this.ddacx = ddacx;
      this.linkIDs = linkIDs;
      this.nodeID = nodeID;  
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
	  private ResourceManager rMan_;
	  private String nodeId_;
	  private Set<String> linkIds_;
	  private String validityId = XPlatPrimitiveElementFactory.nRandomChars(5);
	  
	  public SerializableDialog(
		  ServerControlFlowHarness cfh,
		  String nodeId,
		  Set<String> linkIds
	  ){
		  this.nodeId_ = nodeId;
		  this.scfh_ = cfh;
		  this.rMan_ = scfh_.getDataAccessContext().getRMan();
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan_);
	      this.linkIds_ = linkIds;
	      
		  String dialogTitle = null;

	    DBGenome genome = scfh_.getDataAccessContext().getDBGenome();
	      
		  if(this.linkIds_ == null) {
		      String titleFormat = rMan_.getString("nudd.titleFormat");
		      Node rootNode = genome.getNode(GenomeItemInstance.getBaseID(nodeId));
		      String name = (rootNode == null ? "" : rootNode.getName());
		      dialogTitle = MessageFormat.format(titleFormat, new Object[] {name});
		  } else {
			  String aLinkID = (this.linkIds_.isEmpty()) ? null : this.linkIds_.iterator().next();

		      if (aLinkID != null) {
		    	  
		          Linkage rootLinkage = genome.getLinkage(GenomeItemInstance.getBaseID(aLinkID));		    	  

		          if (this.linkIds_.size() == 1) {
		        	  String disp = rootLinkage.getDisplayString(genome, false);
		        	  String titleFormat = this.rMan_.getString("ludd.titleFormatOne");
		        	  dialogTitle = MessageFormat.format(titleFormat, new Object[] {disp});        
		          } else {
		        	  String titleFormat = this.rMan_.getString("ludd.titleFormatMany");
		        	  Node srcNode = genome.getNode(rootLinkage.getSource());
		        	  String nodeName = srcNode.getName();
		        	  dialogTitle = MessageFormat.format(titleFormat, new Object[] {nodeName});        
		          }
		      } else {
		    	  dialogTitle = this.rMan_.getString("ludd.titleFormatNone");
		      }
		  }
	      
		  buildDialog(dialogTitle,700,400);  
	  }
	  
	  public boolean dialogIsModal() {
	    return (false);
	  }
	  
	  private void buildDialog(String title, int height, int width) {
	  
		  this.xplatDialog_ = new XPlatUIDialog(title,height,width);
		  this.xplatDialog_.setParameter("isModal", new Boolean(false));
		  this.xplatDialog_.setOpenOffset(new Point(-250,0));
		  
		  XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		  layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
		  layoutCollection.setParameter("gutters", "false");
		  
		  this.xplatDialog_.createCollectionList("main", "center");
		  this.xplatDialog_.createCollectionList("main", "bottom");
		  
		  List<Map<String,Object>> selCol = new ArrayList<Map<String,Object>>();
		  List<Map<String,Object>> listValues = new ArrayList<Map<String,Object>>();
		  
		  Map<String,Object> displayMap = new HashMap<String,Object>();
		  displayMap.put("label","display");
		  displayMap.put("field","display");
		  selCol.add(displayMap);
		  
		  Map<String,Object> itemIdMap = new HashMap<String,Object>();
		  itemIdMap.put("label","itemId");
		  itemIdMap.put("field","itemId");
		  itemIdMap.put("hidden","true");
		  selCol.add(itemIdMap);
		  
		  Map<String,Object> modelIdMap = new HashMap<String,Object>();
		  modelIdMap.put("label","modelId");
		  modelIdMap.put("field","modelId");
		  modelIdMap.put("hidden","true");
		  selCol.add(modelIdMap);
		  
		  Map<String,Object> checkedMap = new HashMap<String,Object>();
		  checkedMap.put("label","checked_"+validityId);
		  checkedMap.put("field","checked_"+validityId);
		  checkedMap.put("hidden","true");
		  selCol.add(checkedMap);

		  Map<String,Object> needsIndentMap = new HashMap<String,Object>();
		  needsIndentMap.put("label","needsIndent");
		  needsIndentMap.put("field","needsIndent");
		  needsIndentMap.put("hidden","true");
		  selCol.add(needsIndentMap);		  
		  
		  List<ListEntry> usageList;
		  
		  if(this.linkIds_ == null) {
			  usageList = generateNodeUsages(this.nodeId_);  
		  } else {
			  usageList = generateLinkUsages(this.linkIds_);
		  }
		  
		  for(ListEntry entry : usageList) {
			  Map<String,Object> usageEntry = new HashMap<String,Object>();
			  usageEntry.put("itemId", (entry.itemID != null ? entry.itemID : "null item ID"));
			  usageEntry.put("modelId", (entry.modelID != null ? entry.modelID : "null model ID"));
			  usageEntry.put("needsIndent", entry.needsIndent);
			  usageEntry.put("checked_"+validityId, new Boolean(entry.modelID != null).toString());
			  usageEntry.put("display",entry.display);
			  listValues.add(usageEntry);
		  }
		  
		  XPlatUIPrimitiveElement selectionList = this.primElemFac_.makeListMultiSelection(
			  (this.linkIds_ != null ? this.rMan_.getString("ludd.usages") : this.rMan_.getString("nudd.usages")),
			  null,null, selCol, listValues, null,false,false,true
		  );
		  
		  selectionList.setParameter("GridRenderers", "indentedColumn");		  
		  
		  selectionList.setEvent("dgrid-select", new XPlatUIEvent("dgrid-select","CLIENT_SET_ELEMENT_CONDITION"));
		  selectionList.getEvent("dgrid-select").addParameter("conditionValueLoc", "ELEMENT_ROWS");
		  selectionList.getEvent("dgrid-select").addParameter("conditionCol", "checked_"+validityId);
		  
		  selectionList.setEvent("dgrid-deselect", new XPlatUIEvent("dgrid-deselect","CLIENT_SET_ELEMENT_CONDITION"));
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionValueLoc", "EVENT");
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionCol", "checked_"+validityId);
		  selectionList.getEvent("dgrid-deselect").addParameter("conditionValue", false);
		  
		  this.xplatDialog_.addElementToCollection("main","center",selectionList);
		  
		  this.xplatDialog_.addDefaultState_("checked_"+validityId, "false");
		   
	  }
	  
	  ///////////////////////////////////////
	  // generateNodeUsages
	  //////////////////////////////////////
	  //
	  //	  
	  private List<ListEntry> generateNodeUsages(String nodeID) {
		  
		  DBGenome genome = scfh_.getDataAccessContext().getDBGenome();
		  ArrayList<ListEntry> usageList = new ArrayList<ListEntry>();
		  String baseID = GenomeItemInstance.getBaseID(nodeID);

		  String instanceFormat = this.rMan_.getString("toolCmd.instanceNodeUsageFormat");
		  String dynMessage = this.rMan_.getString("toolCmd.dynamicNodeUsageFormat");
		  String noGroup = this.rMan_.getString("toolCmd.noGroup");
	
		  Node rootNode = genome.getNode(baseID);
		  if (rootNode == null) {
			  return (usageList);
		  } 
		  String fullUse = this.rMan_.getString("toolCmd.fullGenomeNodeUsage");   
		  ListEntry le = new ListEntry(fullUse, genome.getID(), baseID);
		  usageList.add(le);
	 
		  Iterator<GenomeInstance> iit = scfh_.getDataAccessContext().getGenomeSource().getInstanceIterator();
		  while (iit.hasNext()) {
			  GenomeInstance gi = iit.next();
			  String giName = gi.getName();
			  Set<String> niSet = gi.getNodeInstances(baseID);
			  Iterator<String> nisit = niSet.iterator();
			  while (nisit.hasNext()) {
				  String nodeInstID = nisit.next();
				  Group grp = gi.getGroupForNode(nodeInstID, GenomeInstance.ALWAYS_MAIN_GROUP);
				  String grpName = (grp == null) ? noGroup : grp.getInheritedDisplayName(gi);
				  String msg = MessageFormat.format(instanceFormat, new Object[] {giName, grpName});
				  ListEntry le1 = new ListEntry(msg, gi.getID(), nodeInstID);
				  usageList.add(le1);
			  }
			  if (!niSet.isEmpty() && gi.haveProxyDecendant()) {
				  String msg = MessageFormat.format(dynMessage, new Object[] {giName});
				  ListEntry le1 = new ListEntry(msg, null, null);
				  usageList.add(le1); 
			  }
		  }    
		  return (usageList);
	  }
	     
	  
	  ///////////////////////////////////////
	  // generateLinkUsages
	  //////////////////////////////////////
	  //
	  //
	  private List<ListEntry> generateLinkUsages(Set<String> linkIds) {
	      DBGenome genome = scfh_.getDataAccessContext().getDBGenome();
	      ArrayList<ListEntry> usageList = new ArrayList<ListEntry>();

	      Iterator<String> lit = linkIds.iterator();
	      while (lit.hasNext()) {
	        String linkID = lit.next();
	        String baseID = GenomeItemInstance.getBaseID(linkID);
	        String instanceFormatOneGroup = rMan_.getString("toolCmd.instanceLinkUsageSingleFormat");
	        String instanceFormatTwoGroups = rMan_.getString("toolCmd.instanceLinkUsageDoubleFormat");
	        String dynMessage = rMan_.getString("toolCmd.dynamicLinkUsageFormat");
	        String linkFormatMessage = rMan_.getString("toolCmd.LinkUsageLinkFormat");
	        String noGroup = rMan_.getString("toolCmd.noGroup");
	  
	        Linkage rootLink = genome.getLinkage(baseID);
	        if (rootLink == null) {
	          throw new IllegalStateException();
	        }
	        String src = rootLink.getSource();
	        String trg = rootLink.getTarget();
	        String linkMsg = MessageFormat.format(linkFormatMessage, new Object[] {genome.getNode(src).getName(), 
	                                                                               genome.getNode(trg).getName()});
	        ListEntry le = new ListEntry(linkMsg, null, null);                                                                       
	        usageList.add(le);
	        
	        String fullUse = "  " + rMan_.getString("toolCmd.fullGenomeLinkUsage");
	        le = new ListEntry(fullUse, genome.getID(), baseID,true);          
	        usageList.add(le);
	  
          Iterator<GenomeInstance> iit = scfh_.getDataAccessContext().getGenomeSource().getInstanceIterator();
	        while (iit.hasNext()) {
	          GenomeInstance gi = iit.next();
	          String giName = gi.getName();
	          Set<String> liSet = gi.returnLinkInstanceIDsForBacking(baseID);
	          Iterator<String> lisit = liSet.iterator();
	          while (lisit.hasNext()) {
	            String linkInstID = lisit.next();
	            GenomeInstance.GroupTuple gtup = gi.getRegionTuple(linkInstID);
	            String srcGrp = gtup.getSourceGroup();
	            String trgGrp = gtup.getTargetGroup();
	            String msg = null;
	            if (srcGrp == null) {
	              if (trgGrp == null) {
	                msg = MessageFormat.format(instanceFormatOneGroup, new Object[] {giName, noGroup});
	              } else {
	                String trgGrpName = gi.getGroup(trgGrp).getInheritedDisplayName(gi);
	                msg = MessageFormat.format(instanceFormatTwoGroups, new Object[] {giName, noGroup, trgGrpName});
	              }
	            } else if (srcGrp.equals(trgGrp)) {
	              String grpName = gi.getGroup(trgGrp).getInheritedDisplayName(gi);
	              msg = MessageFormat.format(instanceFormatOneGroup, new Object[] {giName, grpName});   
	            } else {
	              String srcGrpName = (srcGrp == null) ? noGroup : gi.getGroup(srcGrp).getInheritedDisplayName(gi);
	              String trgGrpName = (trgGrp == null) ? noGroup : gi.getGroup(trgGrp).getInheritedDisplayName(gi);
	              msg = MessageFormat.format(instanceFormatTwoGroups, new Object[] {giName, srcGrpName, trgGrpName});
	            }
	            le = new ListEntry("  " + msg, gi.getID(), linkInstID,true);
	            usageList.add(le);                      
	          }
	          if (!liSet.isEmpty() && gi.haveProxyDecendant()) {
	            String msg = MessageFormat.format(dynMessage, new Object[] {giName});
	            le = new ListEntry("  " + msg, null, null,true);
	            usageList.add(le);
	          }
	        }
	      }
	  
	      return (usageList);		  
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
	  
	  
	  ///////////////////////////////////////
	  // getDialog
	  ///////////////////////////////////////
	  //
	  //
	  
	  public XPlatUIDialog getDialog() {
		  
		  String closeLabel = this.rMan_.getString("dialogs.close");
		  XPlatUIPrimitiveElement closeBtn = this.primElemFac_.makeBasicButton(closeLabel,closeLabel,"CLIENT_END_COMMAND",null);
		  
		  String gotoLabel = (this.linkIds_ == null ? this.rMan_.getString("nudd.goto") : this.rMan_.getString("ludd.goto"));
		  XPlatUIPrimitiveElement gotoBtn = this.primElemFac_.makeBasicButton(gotoLabel,gotoLabel,"CLIENT_GOTO_MODEL_AND_SELECT",null);
		  		  
		  gotoBtn.getEvent("click").addParameter("mapLinks", (this.linkIds_ != null));
		  
		  gotoBtn.setValidity(new XPlatUIValidity("checked_"+validityId,"true"));
		  
		  this.xplatDialog_.setCancel("CLIENT_END_COMMAND");
		  
		  this.xplatDialog_.addElementToCollection("main","bottom",gotoBtn);
		  this.xplatDialog_.addElementToCollection("main","bottom",closeBtn);
		  
		  return this.xplatDialog_;
	  }	  
	  
	  
	  public XPlatUIDialog getDialog(FlowKey keyVal) {

		  String closeLabel = this.rMan_.getString("dialogs.close");
		  XPlatUIPrimitiveElement closeBtn = this.primElemFac_.makeBasicButton(closeLabel,closeLabel,"CLIENT_END_COMMAND",null);
		  closeBtn.getEvent("click").addParameter("action", keyVal.toString());
		  
		  String gotoLabel = (this.linkIds_ == null ? this.rMan_.getString("nudd.goto") : this.rMan_.getString("ludd.goto"));
		  XPlatUIPrimitiveElement gotoBtn = this.primElemFac_.makeBasicButton(gotoLabel,gotoLabel,keyVal.toString(),null);
		  		  
		  gotoBtn.getEvent("click").addParameter("mapLinks", (this.linkIds_ != null));
		  gotoBtn.setValidity(new XPlatUIValidity("checked_"+validityId,"true"));
		  
		  this.xplatDialog_.setCancel("CLIENT_END_COMMAND");
		  this.xplatDialog_.addCancelParameter("action", keyVal.toString());
		  
		  this.xplatDialog_.addElementToCollection("main","bottom",gotoBtn);
		  this.xplatDialog_.addElementToCollection("main","bottom",closeBtn);
		  
		  return this.xplatDialog_;
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
	  public SimpleUserFeedback checkForErrors(Map<String, String> params) {

	      return (null);
	  }
	 
	  
	    public SimpleUserFeedback checkForErrors(UserInputs ui) { 	
	    	return null;
	    }
	  
	  /***************************************************************************
	   **
	   ** Do the bundle 
	   */  
	  public ServerControlFlowHarness.UserInputs bundleForExit(Map<String, String> params) {
		 /*
		  String val = params.get("gene");
		  CreateRequest crq = new CreateRequest();
	      crq.nameResult = val;
	      crq.haveResult = true;
	      return (crq);
	      */
		  return null;
	  }

	public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
		// TODO Auto-generated method stub
		return null;
	}	  
  }
  
  public static class DesktopLinkUsageFrame extends JFrame implements ListSelectionListener, DesktopDialogPlatform.Dialog {

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    private FixedJButton gotoButton_;
    private UsageSelector usageSel_;
    private JList jlist_;
    private String baseSrcID_;
    private Set<String> linkIDs_;
    private boolean processing_;
    private String tabKey_;
    private UIComponentSource uics_;
    private TabPinnedDynamicDataAccessContext tpddacx_;
    
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
    
    public DesktopLinkUsageFrame(UIComponentSource uics, TabPinnedDynamicDataAccessContext tpddacx, HarnessBuilder hBld, String tabKey, Set<String> linkIDs, String baseSrcID) {
      super();
      uics_ = uics;
      tpddacx_ = tpddacx;
      tabKey_ = tabKey;
      URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
      setIconImage(new ImageIcon(ugif).getImage());
      linkIDs_ = new HashSet<String>(linkIDs); 
      baseSrcID_ = baseSrcID;
      setWindowTitle();
           
      final String closeID = baseSrcID;    
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          // Fix for Bug BT-10-27-09:16
          uics_.getDataPopupMgr().dropLinkSelectionWindow(new Tuple<String, String>(tabKey_, closeID));
        }
      });    
      
      usageSel_ = new UsageSelector(hBld, this);
      processing_ = false;
      
      setSize(800, 500);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
  
      ResourceManager rMan = uics_.getRMan();
      JLabel lab = new JLabel(rMan.getString("ludd.usages"));
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
      cp.add(lab, gbc);
  
      jlist_ = new JList();
      jlist_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      refreshList();    
          
      JScrollPane jsp = new JScrollPane(jlist_);
      UiUtil.gbcSet(gbc, 0, 1, 1, 20, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
      cp.add(jsp, gbc);
      jlist_.addListSelectionListener(this);
  
      
      gotoButton_ = new FixedJButton(rMan.getString("ludd.goto"));
      gotoButton_.setEnabled(false);
      gotoButton_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            usageSel_.goToModelAndSelectLink();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });      
       
      //
      // Build the button panel:
      //
    
      FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.close"));
      buttonO.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            uics_.getDataPopupMgr().dropLinkSelectionWindow(new Tuple<String, String>(tabKey_, closeID));
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
        
      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.add(Box.createHorizontalStrut(10)); 
      buttonPanel.add(gotoButton_);
      buttonPanel.add(Box.createHorizontalGlue()); 
      buttonPanel.add(buttonO);
      
      //
      // Build the dialog:
      //
      UiUtil.gbcSet(gbc, 0, 22, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      setLocationRelativeTo(uics_.getTopFrame());
    }
    
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** These are required. Note we are a frame, i.e. not modal:
    */
    
    public boolean dialogIsModal() {
      return (false);
    }
    
    /***************************************************************************
    **
    ** Handle selections
    */
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (processing_) {
          return;
        }
        if (lse.getValueIsAdjusting()) {
          return;
        }      
        int[] sel = jlist_.getSelectedIndices();
        if ((sel == null) || (sel.length != 1)) {
          usageSel_.setSelected(null);
        } else {
          usageSel_.setSelected((ListEntry)jlist_.getModel().getElementAt(sel[0]));
        }
        handleButtons();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;             
    }
    
    /***************************************************************************
    **
    ** Update the list of entries
    */
    
    public void updateLinkSet(Set<String> linkIDs) {
      linkIDs_ = new HashSet<String>(linkIDs);
      refreshList();
      return;
    }   
    
    /***************************************************************************
    **
    ** Get the link set
    */
    
    public Set<String> getLinkSet() {
      return (linkIDs_);
    }
    
    /***************************************************************************
    **
    ** Get the source node ID
    */
    
    public String getSrcID() {
      return (baseSrcID_);
    }   
   
    /***************************************************************************
    **
    ** Get the tab key
    */
    
    public String getTabKey() {
      return (tabKey_);
    } 
    
    /***************************************************************************
    **
    ** Update the list of entries
    */
    
    public void refreshList() {
      List<ListEntry> usageList = generateLinkUsages(linkIDs_);
      processing_ = true; 
      jlist_.setListData(usageList.toArray());
      setWindowTitle();
      processing_ = false;
      return;
    } 
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Handle the buttons
    */
    
    private void handleButtons() {
      ListEntry sel = usageSel_.getSelected();
      gotoButton_.setEnabled((sel != null) && (sel.modelID != null));
      gotoButton_.revalidate();
      return;             
    } 
    
    /***************************************************************************
    **
    ** Return A base link, or null
    */  
   
    private Linkage getABaseLink(String linkID) { 
      DBGenome genome = tpddacx_.getDBGenome();
      String baseID = GenomeItemInstance.getBaseID(linkID);
      Linkage rootLink = genome.getLinkage(baseID);
      return (rootLink);
    }
    
    /***************************************************************************
    **
    ** Set the title
    */  
   
    private void setWindowTitle() { 
      ResourceManager rMan = uics_.getRMan();  
      String aLinkID = (linkIDs_.isEmpty()) ? null : linkIDs_.iterator().next();
      DBGenome genome = tpddacx_.getDBGenome();
      String msg;
      if (aLinkID != null) {
        Linkage rootLinkage = getABaseLink(aLinkID);
        if (linkIDs_.size() == 1) {
          String disp = rootLinkage.getDisplayString(genome, false);
          String titleFormat = rMan.getString("ludd.titleFormatOne");
          msg = MessageFormat.format(titleFormat, new Object[] {disp});        
        } else {
          String srcID = rootLinkage.getSource();
          String titleFormat = rMan.getString("ludd.titleFormatMany");
          Node srcNode = genome.getNode(srcID);
          String nodeName = srcNode.getName();
          msg = MessageFormat.format(titleFormat, new Object[] {nodeName});        
        }
      } else {
        msg = rMan.getString("ludd.titleFormatNone");
      }
      setTitle(msg);
      return;
    }
    
    /***************************************************************************
    **
    ** Generate the list of link usages
    */  
   
    public List<ListEntry> generateLinkUsages(Set<String> linkIDs) {
      
      DBGenome genome = tpddacx_.getDBGenome();
      ArrayList<ListEntry> usageList = new ArrayList<ListEntry>();
      
      ResourceManager rMan = uics_.getRMan();
      Iterator<String> lit = linkIDs.iterator();
      while (lit.hasNext()) {
        String linkID = lit.next();
        String baseID = GenomeItemInstance.getBaseID(linkID);     
        String instanceFormatOneGroup = rMan.getString("toolCmd.instanceLinkUsageSingleFormat");
        String instanceFormatTwoGroups = rMan.getString("toolCmd.instanceLinkUsageDoubleFormat");
        String dynMessage = rMan.getString("toolCmd.dynamicLinkUsageFormat");
        String linkFormatMessage = rMan.getString("toolCmd.LinkUsageLinkFormat");
        String noGroup = rMan.getString("toolCmd.noGroup");
  
        Linkage rootLink = genome.getLinkage(baseID);
        if (rootLink == null) {
          throw new IllegalStateException();
        }
        String src = rootLink.getSource();
        String trg = rootLink.getTarget();
        String linkMsg = MessageFormat.format(linkFormatMessage, new Object[] {genome.getNode(src).getName(), 
                                                                               genome.getNode(trg).getName()});
        ListEntry le = new ListEntry(linkMsg, null, null);                                                                       
        usageList.add(le);
        
        String fullUse = "  " + rMan.getString("toolCmd.fullGenomeLinkUsage");
        le = new ListEntry(fullUse, genome.getID(), baseID);          
        usageList.add(le);
  
        Iterator<GenomeInstance> iit = tpddacx_.getGenomeSource().getInstanceIterator();
        while (iit.hasNext()) {
          GenomeInstance gi = iit.next();
          String giName = gi.getName();
          Set<String> liSet = gi.returnLinkInstanceIDsForBacking(baseID);
          Iterator<String> lisit = liSet.iterator();
          while (lisit.hasNext()) {
            String linkInstID = lisit.next();
            GenomeInstance.GroupTuple gtup = gi.getRegionTuple(linkInstID);
            String srcGrp = gtup.getSourceGroup();
            String trgGrp = gtup.getTargetGroup();
            String msg = null;
            if (srcGrp == null) {
              if (trgGrp == null) {
                msg = MessageFormat.format(instanceFormatOneGroup, new Object[] {giName, noGroup});
              } else {
                String trgGrpName = gi.getGroup(trgGrp).getInheritedDisplayName(gi);
                msg = MessageFormat.format(instanceFormatTwoGroups, new Object[] {giName, noGroup, trgGrpName});
              }
            } else if (srcGrp.equals(trgGrp)) {
              String grpName = gi.getGroup(trgGrp).getInheritedDisplayName(gi);
              msg = MessageFormat.format(instanceFormatOneGroup, new Object[] {giName, grpName});   
            } else {
              String srcGrpName = (srcGrp == null) ? noGroup : gi.getGroup(srcGrp).getInheritedDisplayName(gi);
              String trgGrpName = (trgGrp == null) ? noGroup : gi.getGroup(trgGrp).getInheritedDisplayName(gi);
              msg = MessageFormat.format(instanceFormatTwoGroups, new Object[] {giName, srcGrpName, trgGrpName});
            }
            le = new ListEntry("  " + msg, gi.getID(), linkInstID);
            usageList.add(le);                      
          }
          if (!liSet.isEmpty() && gi.haveProxyDecendant()) {
            String msg = MessageFormat.format(dynMessage, new Object[] {giName});
            le = new ListEntry("  " + msg, null, null);
            usageList.add(le);
          }
        }
      }
  
      return (usageList);
    }  
  }

  
  /****************************************************************************
  **
  ** Frame for displaying node usages
  */
  
  public class DesktopNodeUsageFrame extends JFrame implements ListSelectionListener, DesktopDialogPlatform.Dialog {
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    private FixedJButton gotoButton_;
    private UsageSelector usageSel_;
    private JList jlist_;
    private String nodeID_;
    private String tabKey_;
    private boolean processing_;
    private UIComponentSource uics_;
    private TabPinnedDynamicDataAccessContext tpddacx_;
    
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
    
    public DesktopNodeUsageFrame(UIComponentSource uics, TabPinnedDynamicDataAccessContext tpddacx, HarnessBuilder hBld, String tabKey, String nodeID) {     
      super();
      uics_ = uics;
      tpddacx_ = tpddacx;
      tabKey_ = tabKey;
      URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
      setIconImage(new ImageIcon(ugif).getImage());
      nodeID_ = GenomeItemInstance.getBaseID(nodeID);
      setNodeTitle();
      
      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          uics_.getDataPopupMgr().dropSelectionWindow(new Tuple<String, String>(tabKey_, nodeID_));
        }
      });
     
      usageSel_ = new UsageSelector(hBld, this);
      processing_ = false;
      
      setSize(500, 700);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
  
      ResourceManager rMan = uics_.getRMan();
      JLabel lab = new JLabel(rMan.getString("nudd.usages"));
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
      cp.add(lab, gbc);
  
      jlist_ = new JList();
      jlist_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      refreshList();
         
      JScrollPane jsp = new JScrollPane(jlist_);
      UiUtil.gbcSet(gbc, 0, 1, 1, 20, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
      cp.add(jsp, gbc);
      jlist_.addListSelectionListener(this);
          
      //
      // Build the button panel:
      //
      
      gotoButton_ = new FixedJButton(rMan.getString("nudd.goto"));
      gotoButton_.setEnabled(false);
      gotoButton_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            usageSel_.goToModelAndSelectNode();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });    
       
      FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.close"));
      buttonO.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            uics_.getDataPopupMgr().dropSelectionWindow(new Tuple<String, String>(tabKey_, nodeID_));
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
        
      Box buttonPanel = Box.createHorizontalBox();
      buttonPanel.add(Box.createHorizontalStrut(10)); 
      buttonPanel.add(gotoButton_);
      buttonPanel.add(Box.createHorizontalGlue()); 
      buttonPanel.add(buttonO);
      
      //
      // Build the dialog:
      //
      UiUtil.gbcSet(gbc, 0, 22, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      
      setLocationRelativeTo(uics_.getTopFrame());
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /***************************************************************************
    **
    ** Handle selections
    */
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (processing_) {
          return;
        }
        if (lse.getValueIsAdjusting()) {
          return;
        }      
        int[] sel = jlist_.getSelectedIndices();
        if ((sel == null) || (sel.length != 1)) {
          usageSel_.setSelected(null);
        } else {
          usageSel_.setSelected((ListEntry)jlist_.getModel().getElementAt(sel[0]));
        }
        handleButtons();
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return;             
    } 
    
    /***************************************************************************
    **
    ** Update the list of entries
    */
    
    public void refreshList() {
      List<ListEntry> usageList = generateNodeUsages(nodeID_);
      processing_ = true; 
      jlist_.setListData(usageList.toArray());
      setNodeTitle();
      processing_ = false;
      return;
    } 
     
    /***************************************************************************
    **
    ** Get our base node ID
    */
    
    public String getBaseNodeID() {
      return (nodeID_);
    }
    
    /***************************************************************************
    **
    ** These are required. Note we are a frame, i.e. not modal:
    */
 
    public boolean dialogIsModal() {
      return (false);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Handle the buttons
    */
    
    private void handleButtons() {
      ListEntry sel = usageSel_.getSelected();
      gotoButton_.setEnabled((sel != null) && (sel.modelID != null));
      gotoButton_.revalidate();
      return;             
    } 
    
    /***************************************************************************
    **
    ** Return the base node, or null
    */  
   
    private Node getTheBaseNode(String nodeID) { 
      DBGenome genome = tpddacx_.getDBGenome();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      Node rootNode = genome.getNode(baseID);
      return (rootNode);
    }
    
    /***************************************************************************
    **
    ** Get the tab key
    */
    
    public String getTabKey() {
      return (tabKey_);
    } 

    /***************************************************************************
    **
    ** Return the node name
    */  
   
    private String getNodeName(Node rootNode) { 
      String name = (rootNode == null) ? "" : rootNode.getName();    
      return (name);
    } 
    
    /***************************************************************************
    **
    ** Set the title
    */  
   
    private void setNodeTitle() { 
      ResourceManager rMan = uics_.getRMan();
      String titleFormat = rMan.getString("nudd.titleFormat");
      Node rootNode = getTheBaseNode(nodeID_);
      String name = getNodeName(rootNode);
      String msg = MessageFormat.format(titleFormat, new Object[] {name});
      setTitle(msg);
      return;
    }
     
    /***************************************************************************
    **
    ** Generate the list of node usages
    */  
   
    private List<ListEntry> generateNodeUsages(String nodeID) {
      
      DBGenome genome = tpddacx_.getDBGenome();
      ArrayList<ListEntry> usageList = new ArrayList<ListEntry>();
      String baseID = GenomeItemInstance.getBaseID(nodeID);
      ResourceManager rMan = uics_.getRMan();
      String instanceFormat = rMan.getString("toolCmd.instanceNodeUsageFormat");
      String dynMessage = rMan.getString("toolCmd.dynamicNodeUsageFormat");
      String noGroup = rMan.getString("toolCmd.noGroup");
      
      Node rootNode = genome.getNode(baseID);
      if (rootNode == null) {
        return (usageList);
      } 
      String fullUse = rMan.getString("toolCmd.fullGenomeNodeUsage");   
      ListEntry le = new ListEntry(fullUse, genome.getID(), baseID);
      usageList.add(le);
   
      Iterator<GenomeInstance> iit = tpddacx_.getGenomeSource().getInstanceIterator();
      while (iit.hasNext()) {
        GenomeInstance gi = iit.next();
        String giName = gi.getName();
        Set<String> niSet = gi.getNodeInstances(baseID);
        Iterator<String> nisit = niSet.iterator();
        while (nisit.hasNext()) {
          String nodeInstID = nisit.next();
          Group grp = gi.getGroupForNode(nodeInstID, GenomeInstance.ALWAYS_MAIN_GROUP);
          String grpName = (grp == null) ? noGroup : grp.getInheritedDisplayName(gi);
          String msg = MessageFormat.format(instanceFormat, new Object[] {giName, grpName});
          ListEntry le1 = new ListEntry(msg, gi.getID(), nodeInstID);
          usageList.add(le1);
        }
        if (!niSet.isEmpty() && gi.haveProxyDecendant()) {
          String msg = MessageFormat.format(dynMessage, new Object[] {giName});
          ListEntry le1 = new ListEntry(msg, null, null);
          usageList.add(le1); 
        }
      }    
      return (usageList);
    }   
  } 
 
  /****************************************************************************
  **
  ** Helper for displaying link and node usages
  */
  
  public static class UsageSelector {
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    private ListEntry selected_;
    private JFrame parent_;
    private HarnessBuilder hBld_;
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    public UsageSelector(HarnessBuilder hBld, JFrame parent) {
      // NOT THE APP WINDOW!!!!
      parent_ = parent;
      selected_ = null;
      hBld_ = hBld;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Set current selection
    ** 
    */
    
    public void setSelected(ListEntry selected) {
      selected_ = selected;
      return;
    }
   
    /***************************************************************************
    **
    ** Get current selection
    ** 
    */
    
    public ListEntry getSelected() {
      return (selected_);
    }
  
    /***************************************************************************
    **
    ** Transmit to nav system
    ** 
    */
    
    public void goToModelAndSelectNode() {
      if ((selected_ == null) || (selected_.modelID == null)) {
        return;
      }
      HashSet<String> nodes = new HashSet<String>();
      nodes.add(selected_.itemID);
      HashSet<String> links = new HashSet<String>();
      sendRequest(selected_.modelID, nodes, links, FlowMeister.OtherFlowKey.MODEL_AND_NODE_SELECTION);
      return;
    }
    
    /***************************************************************************
    **
    ** Transmit to nav system
    ** 
    */
    
    public void goToModelAndSelectLink() {
      if ((selected_ == null) || (selected_.modelID == null)) {
        return;
      }
      HashSet<String> links = new HashSet<String>();
      links.add(selected_.itemID);
      HashSet<String> nodes = new HashSet<String>();
      sendRequest(selected_.modelID, nodes, links, FlowMeister.OtherFlowKey.MODEL_AND_LINK_SELECTION);
      return;
    }
    
     /***************************************************************************
     **
     ** This is an independent frame, so it is sending off a control flow request (there is no
     ** currently running control flow):
     */
    
    public void sendRequest(String modelID, Set<String> nodes, Set<String> links, FlowMeister.FlowKey flowKey) {
      
      HarnessBuilder.PreHarness pH = hBld_.buildHarness(flowKey, parent_);
      SetCurrentModel.StepState agis = (SetCurrentModel.StepState)pH.getCmdState();
      agis.setPreload(modelID, nodes, links);
      hBld_.runHarness(pH);
      return;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Holds onto info for selections
  */  
  
  public static class ListEntry {
    public String display;
    public String modelID;
    public String itemID;
    public boolean needsIndent;
    
    public ListEntry(String display, String modelID, String itemID) {
      this(display,modelID,itemID,false);
    }
    
    public ListEntry(String display, String modelID, String itemID,boolean needsIndent) {
        this.display = display;
        this.modelID = modelID;
        this.itemID = itemID;   
        this.needsIndent = needsIndent;
      }    
    
    @Override
    public String toString() {
      return (display);
    }
  }
}
