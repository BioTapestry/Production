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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.NameValuePairTablePanel;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.NameValuePairList;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Factory for dialog boxes for editing properties of a network module
*/

public class NetModulePropertiesDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetModulePropertiesDialogFactory(ServerControlFlowHarness cfh) {
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
   
    NetModulePropsArgs dniba = (NetModulePropsArgs)ba;
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
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
  
  public static class NetModulePropsArgs extends DialogBuildArgs { 
    
    //
    // Stash all this stuff right at the start to keep everything LOCAL
    //
    
    Set<String> existingMembers;
    Set<String> existingNames;
    Vector<TrueObjChoiceContent> typeChoices;
    String modID;
    String desc;
    NetOverlayProperties.OvrType origType;
    int origModType;
    boolean hideLinks;
    NameValuePairList origNvpl;
    String origColor;
    String origFill;
    FontManager.FontOverride origFont;
    int origFade;
    boolean origIsHiding;
    String origLineBrk;
    String origDesc; 
    String origName;
    TreeSet<String> origTags;
    HashSet<String> globalTags;
    HashSet<String> allNames;
    HashSet<String> allValues;
    HashMap<String, Set<String>> valsForNames;
 
    public NetModulePropsArgs(DataAccessContext dacx, String modID) {
      super(dacx.getCurrentGenome()); 
      this.modID = modID;
      Layout layout = dacx.getLayoutSource().getLayout(dacx.getCurrentLayoutID());
      String ovrKey = dacx.getOSO().getCurrentOverlay();
      NetOverlayOwner owner = dacx.getGenomeSource().getOverlayOwnerFromGenomeKey(genome.getID());
      NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
      NetOverlayProperties nop = layout.getNetOverlayProperties(ovrKey);
      NetModuleProperties nmop = nop.getNetModuleProperties(modID);
      NetModule nmod = novr.getModule(modID);
         
      existingMembers = new HashSet<String>();
      Iterator<NetModuleMember> memit = nmod.getMemberIterator();
      while (memit.hasNext()) {
        NetModuleMember nmm = memit.next();
        existingMembers.add(nmm.getID());
      }
          
      existingNames = new HashSet<String>();
      Iterator<NetModule> mit = novr.getModuleIterator();
      while (mit.hasNext()) {
        NetModule nm = mit.next();
        if (nm != nmod) {
          existingNames.add(nm.getName());
        }
      }
     
      origColor = nmop.getColorTag();
      origFill = nmop.getFillColorTag();
      origFont = nmop.getFontOverride();
      origFade = nmop.getNameFadeMode();
      origIsHiding = nmop.isHidingLabel();
      origLineBrk = nmop.getLineBreakDef();
      origDesc = nmod.getDescription(); 
      origName = nmod.getName();      
      origNvpl = nmod.getNVPairs();   
      typeChoices = NetOverlayProperties.getOverlayTypes(dacx);
      desc = novr.getDescription();  
      hideLinks = nop.hideLinks();    
      origType = nop.getType(); 
      origTags = buildTagSet(nmod);
      origModType = nmop.getType();
           
      globalTags = new HashSet<String>(); // Will NOT include tags _just_ belonging to nmod..
      HashSet<String> otherNames = new HashSet<String>();
      allNames = new HashSet<String>();
      allValues = new HashSet<String>();
      valsForNames = new HashMap<String, Set<String>>();
      HashSet<String> junkSet = new HashSet<String>();
      HashMap<String, Set<String>> junkMap = new HashMap<String, Set<String>>();
      FullGenomeHierarchyOracle oracle = dacx.getFGHO();
      oracle.getGlobalTagsAndNVPairsForModules(junkMap, otherNames, junkSet, globalTags, nmod);   
      oracle.getGlobalTagsAndNVPairsForModules(valsForNames, allNames, allValues, junkSet, null);   
    }
    
    /***************************************************************************
    **
    ** Build the current tag list
    ** 
    */
    
    private TreeSet<String> buildTagSet(NetModule nmod) {
      TreeSet<String> retval = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      Iterator<String> ti = nmod.getTagIterator();
      while (ti.hasNext()) {
        String tag = ti.next();
        retval.add(tag);
      }
      return (retval);
    }  
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class SerializableDialog implements SerializableDialogPlatform.Dialog {
	  
	  // Original values
	  private NetModulePropsArgs nmpa_;
	  
	  private XPlatUIDialog xplatDialog_;
	  private ServerControlFlowHarness scfh_;
	  private ResourceManager rMan_;
	  
	  
	  public SerializableDialog(ServerControlFlowHarness cfh, NetModulePropsArgs nmpa) {
		 this.scfh_ = cfh; 
		 this.rMan_ = cfh.getDataAccessContext().getRMan();
		 this.nmpa_ = nmpa;
		 
		 buildDialog(); 
	  }
	  
	  
	  
	  
	  ///////////////////
	  // buildDialog
	  ///////////////////
	  //
	  //
	  private void buildDialog() {
		  this.xplatDialog_ = new XPlatUIDialog(this.rMan_.getString("nmodprop.title"),600,650);
		  
		  this.xplatDialog_.setUserInputs(new NetModulePropsRequest(true));
		  
		  
		  
		  
	  }
	  
	  
	public boolean dialogIsModal() {
    return (true);
  }

	public SimpleUserFeedback checkForErrors(UserInputs ui) {
		// TODO Auto-generated method stub
		return null;
	}

	public XPlatUIDialog getDialog() {
		// TODO Auto-generated method stub
		return null;
	}

	public XPlatUIDialog getDialog(FlowKey keyVal) {
		// TODO Auto-generated method stub
		return null;
	}




	public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
		// TODO Auto-generated method stub
		return null;
	}
	  
	  
  }
    
  
  public static class DesktopDialog extends BTTransmitResultsDialog implements ListWidgetClient { 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

    //
    // Orig values
    //
    
    private NetModulePropsArgs nmpa_;
    
    private JTextField nameField_;
    private ColorSelectionWidget colorWidget1_;
    private ColorSelectionWidget colorWidget2_;
    private JCheckBox doLinksBox_;
    private JCheckBox hideLabelBox_;
    private NameValuePairTablePanel nvptab_;
    private JTextField descField_; 
    private ListWidget tlw_;
    private HashSet<String> globalTags_;
    private TreeSet<String> currTags_;
    private ArrayList<String> currTagsList_;
    private JComboBox typeCombo_;
    private JComboBox fadeCombo_;
    private JLabel fadeLabel_;  
    private NodeAndLinkPropertiesSupport nps_;
    private StaticDataAccessContext dacx_;
    private UIComponentSource uics_;
    private HarnessBuilder hBld_;
       
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, NetModulePropsArgs nmpa) {      
      super(cfh, "nmodprop.title", new Dimension(650, 600), 1, new NetModulePropsRequest(), true);
      nmpa_ = nmpa;
      dacx_ = cfh.getDataAccessContext();
      hBld_ = cfh.getHarnessBuilder();
      currTags_ = new TreeSet<String>(nmpa.origTags);
      currTagsList_ = new ArrayList<String>(currTags_);
      globalTags_ = new HashSet<String>(nmpa.globalTags);
      nps_ = new NodeAndLinkPropertiesSupport(uics_, dacx_);
      nvptab_ = new NameValuePairTablePanel(uics_, uics_.getTopFrame(), nmpa.origNvpl, nmpa.allNames, nmpa.allValues, nmpa.valsForNames, false);     
      
      //
      // Build the tabs.
      //
  
      JTabbedPane tabPane = new JTabbedPane();
      tabPane.addTab(rMan_.getString("propDialogs.modelProp"), buildModelTab());
      tabPane.addTab(rMan_.getString("propDialogs.layoutProp"), buildLayoutTab());
      addTable(tabPane, 8);
      
      finishConstruction();
      displayProperties();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////  
    
    public boolean dialogIsModal() {
      return (true);
    }
    
    /***************************************************************************
    **
    ** Add a row to the list
    */
    
    public List addRow(ListWidget widget) {
  
      TagWorkingDialog twd = new TagWorkingDialog(uics_, dacx_, null, globalTags_, currTags_); 
      twd.setVisible(true);
      
      if (!twd.haveResult()) {
        return (null);
      }
      
      String newTag = twd.getTag();
      currTags_.add(newTag);
      currTagsList_.clear();
      currTagsList_.addAll(currTags_);
      
      return (new ArrayList<String>(currTagsList_));
   
    }
    
    /***************************************************************************
    **
    ** Delete rows from the list
    */
    
    public List deleteRows(ListWidget widget, int[] selectedRows) {
     for (int i = 0; i < selectedRows.length; i++) {
        currTagsList_.remove(selectedRows[i]);
        for (int j = i + 1; j < selectedRows.length; j++) {
          if (selectedRows[j] > selectedRows[i]) {
            selectedRows[j]--;
          }
        }
      }   
      currTags_.clear();
      currTags_.addAll(currTagsList_);
      currTagsList_.clear();
      currTagsList_.addAll(currTags_);
     
      return (new ArrayList<String>(currTagsList_));
    }
    
    /***************************************************************************
    **
    ** Edit a row from the list
    */
    
    public List editRow(ListWidget widget, int[] selectedRows) {
      if (selectedRows.length != 1) {
        throw new IllegalArgumentException();
      }
   
      TagWorkingDialog twd = new TagWorkingDialog(uics_, dacx_, currTagsList_.get(selectedRows[0]), globalTags_, currTags_); 
      twd.setVisible(true);
      
      if (!twd.haveResult()) {
        return (null);
      }
  
      String newTag = twd.getTag();
      currTagsList_.set(selectedRows[0], newTag);
      currTags_.clear();
      currTags_.addAll(currTagsList_);
      currTagsList_.clear();
      currTagsList_.addAll(currTags_);
  
      return (new ArrayList<String>(currTagsList_));    
    }
  
    /***************************************************************************
    **
    ** Merge rows from the list
    */
    
    public List combineRows(ListWidget widget, int[] selectedRows) {
      throw new UnsupportedOperationException();
    }    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Build the model tab
    */ 
    
    private JPanel buildModelTab() {
      
      JPanel retval = new JPanel();
      ResourceManager rMan = dacx_.getRMan();
      retval.setBorder(new EmptyBorder(20, 20, 20, 20));
      retval.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();   
      
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan.getString("nmodprop.name"));
      nameField_ = new JTextField();
      int rowNum = 0;
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      retval.add(label, gbc);
  
      UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(nameField_, gbc);
        
      //
      // Track node name changes for name field support:
      //
      
      nps_.trackNodeNameChanges(nameField_, null);          
      
      //
      // Add the module description:
      //
        
      label = new JLabel(rMan.getString("nmodprop.desc"));
      descField_ = new JTextField();
    
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      retval.add(label, gbc);
  
      UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(descField_, gbc);  
      
      //
      // Add the module tags:
      //
        
      tlw_ = new ListWidget(uics_.getHandlerAndManagerSource(), new ArrayList(), this);    
      
      label = new JLabel(rMan.getString("nmodprop.tags")); 
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      retval.add(label, gbc);
  
      UiUtil.gbcSet(gbc, 1, rowNum, 10, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(tlw_, gbc);      
      
      rowNum += 5;  
      
      //
      // Add the name/val pair table:
      //
      
      label = new JLabel(rMan.getString("nmodprop.nvpairs")); 
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      retval.add(label, gbc);
  
      UiUtil.gbcSet(gbc, 1, rowNum, 10, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(nvptab_, gbc);      
      
      rowNum += 5;  
   
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Build tab for layout
    */ 
    
    private JPanel buildLayoutTab() {
      
      JPanel retval = new JPanel();
      ResourceManager rMan = dacx_.getRMan();   
      retval.setBorder(new EmptyBorder(20, 20, 20, 20));
      retval.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();   
      
      //
      // Build the module type
      //
      
      int rowNum = 0;
      JLabel label = new JLabel(rMan.getString("nmodule.displayType"));
      Vector<ChoiceContent> choices = NetModuleProperties.getDisplayTypes(uics_, true);
      typeCombo_ = new JComboBox(choices);
  
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      retval.add(label, gbc);
      
      UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(typeCombo_, gbc);    
      
      //
      // Add the color selectors.  Button for editing goes onto first widget, so it is
      // built last to hand it listener:
      //
      
      colorWidget2_ = new ColorSelectionWidget(uics_, dacx_.getColorResolver(), hBld_, null, true, "nmodprop.fillColor", false, false);
      ArrayList<ColorDeletionListener> colorDeletionListeners = new ArrayList<ColorDeletionListener>();
      colorDeletionListeners.add(colorWidget2_);
      colorWidget1_ = new ColorSelectionWidget(uics_, dacx_.getColorResolver(), hBld_, colorDeletionListeners, true, "nmodprop.borderColor", true, false);    
      
      UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(colorWidget1_, gbc); 
      
      UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(colorWidget2_, gbc);
    
      doLinksBox_ = new JCheckBox(rMan.getString("nmodprop.doLinks"));
      UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(doLinksBox_, gbc);
        
      hideLabelBox_ = new JCheckBox(rMan.getString("nmodprop.hideLabel"));
      UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(hideLabelBox_, gbc); 
      hideLabelBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            fadeLabel_.setEnabled(!hideLabelBox_.isSelected());
            fadeCombo_.setEnabled(!hideLabelBox_.isSelected());
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
  
      fadeLabel_ = new JLabel(rMan.getString("nmodProp.fadeMode"));
      Vector<ChoiceContent> fadeChoices = NetModuleProperties.getNameFades(uics_);
      fadeCombo_ = new JComboBox(fadeChoices);
  
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      retval.add(fadeLabel_, gbc);
      
      UiUtil.gbcSet(gbc, 1, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(fadeCombo_, gbc);    
  
      //
      // Font override:
      //
      
      JPanel fonto = nps_.fontOverrideUI(false);
      UiUtil.gbcSet(gbc, 0, rowNum++, 11, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(fonto, gbc);   
      
      //
      // Line breaks:
      //
      
      JPanel lineBr = nps_.getLineBreakUI();
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(lineBr, gbc);    
       
      return (retval);    
    }    
     
    /***************************************************************************
    **
    ** Apply the current overlay property values to our UI components
    ** 
    */
    
    private void displayProperties() {
   
      nps_.setLineBreak(nmpa_.origLineBrk, false);
      if (nmpa_.origName != null) {
        nameField_.setText(nmpa_.origName);
      }
      if (nmpa_.origDesc != null) {
        descField_.setText(nmpa_.origDesc);
      }
      colorWidget1_.setCurrentColor(nmpa_.origColor);
      colorWidget2_.setCurrentColor(nmpa_.origFill);
      if (nmpa_.origType == NetOverlayProperties.OvrType.TRANSPARENT) {
        colorWidget2_.setEnabled(false);
      }

      typeCombo_.setSelectedItem(NetModuleProperties.typeForCombo(uics_, nmpa_.origModType)); 
       
      hideLabelBox_.setSelected(nmpa_.origIsHiding);
      fadeCombo_.setSelectedItem(NetModuleProperties.fadeForCombo(uics_, nmpa_.origFade)); 
      fadeLabel_.setEnabled(!hideLabelBox_.isSelected());
      fadeCombo_.setEnabled(!hideLabelBox_.isSelected());
      
      tlw_.update(currTagsList_);
      
      nvptab_.displayProperties();
      
      nps_.fontOverrideDisplay(nmpa_.origFont, FontManager.NET_MODULE);
      
      return;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Do the bundle 
    ** 
    */
      
    protected boolean bundleForExit(boolean forApply) {
      NetModulePropsRequest nmprq = (NetModulePropsRequest)request_;
      nmprq.setForApply(forApply);
       
      //
      // Any number of changes can mess up the link pad needs.  Figure out what we have to start:
      //
      
      nmprq.color1 = colorWidget1_.getCurrentColor();
      nmprq.color2 = colorWidget2_.getCurrentColor();
      nmprq.colorChange = !nmprq.color1.equals(nmpa_.origColor) || !nmprq.color2.equals(nmpa_.origFill); 
      
      nmprq.newFont = nps_.getFontOverride();
      nmprq.fontChange = (nmpa_.origFont != null) ? !nmpa_.origFont.equals(nmprq.newFont) : (nmprq.newFont != null);
      
      nmprq.hideLabel = hideLabelBox_.isSelected();
      nmprq.hideLabelChg = (nmpa_.origIsHiding != nmprq.hideLabel); 
      
      ChoiceContent fadeSelection = (ChoiceContent)fadeCombo_.getSelectedItem();
      nmprq.newFade = fadeSelection.val;
      nmprq.fadeChg = (!hideLabelBox_.isSelected() && (nmpa_.origFade != nmprq.newFade));
        
      nmprq.untrimmed = nps_.getBaseStringForDef();
     
      nmprq.newLineBrkDef = NetModuleProperties.trimOps(nps_.getLineBreakDef(), nmprq.untrimmed);
      nmprq.brkChg = (nmpa_.origLineBrk != null) ? !nmpa_.origLineBrk.equals(nmprq.newLineBrkDef) : (nmprq.newLineBrkDef != null);  
   
      //
      // Change of display type requires us to change the rectangle representation:
      //
      
      ChoiceContent typeSelection = (ChoiceContent)typeCombo_.getSelectedItem();
      nmprq.typeVal = typeSelection.val;   
      nmprq.typeValChange = (nmpa_.origModType != typeSelection.val);   
      if (nmprq.typeValChange) {   
        //
        // Not allowed to convert to an autobox type if there are no members of the
        // module!
        //  
        if ((typeSelection.val == NetModuleProperties.MEMBERS_ONLY) && nmpa_.existingMembers.isEmpty()) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("nmodprop.needMembersForMembersOnly"), 
                                        rMan.getString("nmodprop.typeConversionErrorTitle"),
                                        JOptionPane.ERROR_MESSAGE); 
          return (false);
        } 
      }
          
      nmprq.submit = false;   
      String newName = nameField_.getText().trim();
  
      nmprq.nameToSubmit = nmpa_.origName;
      if (!newName.equals(nmpa_.origName)) {
        if (newName.equals("")) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("nmodprop.emptyName"), 
                                        rMan.getString("nmodprop.emptyNameTitle"),
                                        JOptionPane.ERROR_MESSAGE); 
          return (false);
        }          
  
        if (DataUtil.containsKey(nmpa_.existingNames, newName)) {
          ResourceManager rMan = dacx_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        rMan.getString("nmodprop.dupName"), 
                                        rMan.getString("nmodprop.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE); 
          return (false);
        }
        
        nmprq.submit = true;     
        nmprq.nameToSubmit = newName;
      }
      
      nmprq.doLinks = doLinksBox_.isSelected();
       
      String newDesc = descField_.getText().trim();
      nmprq.descToSubmit = nmpa_.origDesc;
      if (!newDesc.equals(nmpa_.origDesc)) {
        nmprq.submit = true;     
        nmprq.descToSubmit = newDesc;
      }
      
      nmprq.submitTags = false;
      if (!nmpa_.origTags.equals(currTags_)) {
        nmprq.submitTags = true;
        nmprq.newTags = new TreeSet<String>(currTags_);
      }
      
      nmprq.submitNV = false;
      nmprq.newNvpl = null;
      NameValuePairList nvpl = nvptab_.extractData();
      if (nvpl == null) {
        return (false);
      }   
      if (!nmpa_.origNvpl.equals(nvpl)) {
        nmprq.submitNV = true;
        nmprq.newNvpl = nvpl.clone();
      }
      nmprq.haveResult = true;
      return (true);
    }
  }
 
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class NetModulePropsRequest implements ServerControlFlowHarness.UserInputs {   
   
    public boolean submit;
    public boolean submitTags;
    public boolean submitNV;
    public String nameToSubmit;
    public String descToSubmit;
    public String color1;
    public String color2;
    public boolean colorChange;
    public FontManager.FontOverride newFont;
    public boolean fontChange;    
    public TreeSet<String> newTags;
    public NameValuePairList newNvpl;    
    public boolean hideLabelChg;  
    public int typeVal;    
    public boolean typeValChange;
    public boolean doLinks;  
    public boolean hideLabel;   
    public int newFade ;
    public boolean fadeChg;      
    public String untrimmed;
    public String newLineBrkDef;
    public boolean brkChg;  
   
    private boolean haveResult;
    private boolean forApply_;
    
    public NetModulePropsRequest() {
    	
    }
    
    // constructor which initializes everything so it can be serialized with null excludes
    public NetModulePropsRequest(boolean forTransit) {
        submit = false;
        submitTags = false;
        submitNV = false;
        nameToSubmit = "";
        descToSubmit = "";
        color1 = "";
        color2 = "";
        colorChange = false;
        newFont = new FontManager.FontOverride(-2,false,false,false);
        fontChange = false;    
        newTags = new TreeSet<String>();
        newNvpl = new NameValuePairList();    
        hideLabelChg = false;  
        typeVal = -1;    
        typeValChange = false;
        doLinks = false;  
        hideLabel = false;   
        newFade  = -1;
        fadeChg = false;      
        untrimmed = "";
        newLineBrkDef = "";
        brkChg = false;
    }

    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    
    public boolean haveResults() {
      return (haveResult);
    }
    
    public boolean isForApply() {
      return (forApply_);
    }
    
    // getters/setters for serialization
    
    public void setForApply(boolean forApply) {
      forApply_ = forApply;
      return;
    }
    	
	public boolean getForApply() {
        return (forApply_);
    }
	
	public void setHasResults() {
		this.haveResult = true;
		return;
	}  

    public boolean getHaveResult() {
		return haveResult;
	}
  }
}
