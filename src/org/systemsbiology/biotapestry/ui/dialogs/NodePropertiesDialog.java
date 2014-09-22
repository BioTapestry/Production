/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing node properties
*/

public class NodePropertiesDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JTextField localNameField_;
  private JLabel localNameLabel_;
  private JCheckBox localBox_;  
  private NodeProperties props_;
  private BTState appState_; 
  private DataAccessContext dacx_;
  private JCheckBox doLinksBox_;
  private JCheckBox hideNameBox_;
  private ColorSelectionWidget colorWidget1_;
  private ColorSelectionWidget colorWidget2_;
  
  private NodeAndLinkPropertiesSupport nps_;
  
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
  
  public NodePropertiesDialog(BTState appState, DataAccessContext dacx, NodeProperties props) {     
    super(appState.getTopFrame(), appState.getRMan().getString("nprop.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    props_ = props;
     
    Genome genome = dacx_.getGenome();
    boolean forRoot = (genome instanceof DBGenome);
    boolean topTwoLevels = forRoot;
    if (genome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      topTwoLevels = gi.isRootInstance();
    }
    String ref = props_.getReference();
    Node node = genome.getNode(ref);
    nps_ = new NodeAndLinkPropertiesSupport(appState_, dacx_, ref);
           
    ResourceManager rMan = appState_.getRMan();
    setSize(750, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the tabs.
    //

    JTabbedPane tabPane = new JTabbedPane();
    if (!(genome instanceof DynamicGenomeInstance)) {   
      tabPane.addTab(rMan.getString("propDialogs.modelProp"), buildModelTab(node));
    }
    tabPane.addTab(rMan.getString("propDialogs.layoutProp"), buildLayoutTab());
    if (topTwoLevels) {
      tabPane.addTab(rMan.getString("propDialogs.freeText"), nps_.buildTextTab(null, forRoot));
      tabPane.addTab(rMan.getString("propDialogs.URLTab"), nps_.buildUrlTab(null, forRoot));
    }    
    
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tabPane, gbc);
    
    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallButtonBox(cp, 9, 10, true, false); 
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
   
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    if (applyProperties()) {
      nps_.clearNameChangeTracking();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (applyProperties()) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    setVisible(false);
    dispose();
    return;
  } 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build a tab for model properties
  ** 
  */
  
  private JPanel buildModelTab(Node node) {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    
    if (dacx_.getGenome() instanceof DynamicGenomeInstance) {
      throw new IllegalArgumentException();
    }    
    
    int rownum = 0;
    JLabel label = new JLabel(rMan.getString("nprop.name"));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    retval.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(nameField_, gbc);
     
    if (dacx_.getGenome() instanceof GenomeInstance) {
      localBox_ = new JCheckBox(rMan.getString("nprop.uselocal"));
      UiUtil.gbcSet(gbc, 0, rownum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      retval.add(localBox_, gbc);
      localBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            localNameField_.setEnabled(localBox_.isSelected());
            localNameLabel_.setEnabled(localBox_.isSelected());
            nps_.displayLocalForBreaks(localBox_.isSelected());
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      localNameLabel_ = new JLabel(rMan.getString("nprop.localname"));
      localNameField_ = new JTextField();
      UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      retval.add(localNameLabel_, gbc);

      UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(localNameField_, gbc);
    
      
      JPanel activ = nps_.activityLevelUI(false, false);     
      if (activ != null) {
        UiUtil.gbcSet(gbc, 0, rownum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        retval.add(activ, gbc);
      }    
    }
    
    //
    // Track node name changes for name field support:
    //
    
    nps_.trackNodeNameChanges(nameField_, localNameField_); 


    //
    // Big size box and pad setter:
    //
   
    // Fix for BT-12-15-11:5
    SortedSet<Integer> padOptions = NodeAndLinkPropertiesSupport.generatePadChoices(node);
    if (padOptions != null) {
      JPanel xtraPads = nps_.extraPadsUI(false, padOptions, false, null);     
      if (xtraPads != null) {
        UiUtil.gbcSet(gbc, 0, rownum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        retval.add(xtraPads, gbc);
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Build a tab for layout properties
  ** 
  */
  
  private JPanel buildLayoutTab() {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    String ref = props_.getReference();    
    Node node = dacx_.getGenome().getNode(ref);
           
    //
    // Build the color panel.  Intercells use two colors:
    //

    int rowNum = 0;
    
    boolean doSecond = (node.getNodeType() == Node.INTERCELL);  // FIX ME: Make more abstract
    boolean firstHasButton = !doSecond;

    colorWidget1_ = new ColorSelectionWidget(appState_, dacx_, null, true, "nprop.color", firstHasButton, false);
    UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    retval.add(colorWidget1_, gbc);     
    
    if (doSecond) {
      ArrayList<ColorDeletionListener> colorDeletionListeners = new ArrayList<ColorDeletionListener>();
      colorDeletionListeners.add(colorWidget1_);
      colorWidget2_ = new ColorSelectionWidget(appState_, dacx_, colorDeletionListeners, true, "nprop.color2", true, false);
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      retval.add(colorWidget2_, gbc);         
    } 
    
    //
    // Build the do links too box:
    //        
    
    doLinksBox_ = new JCheckBox(rMan.getString("nprop.doLinks"));
    // WJRL 4/30/09: Having the check box set has bad side effects: unexpected 
    // color changes when setting other tabs.  Drop it.
    //doLinksBox_.setSelected(NodeProperties.setWithLinkColor(node.getNodeType()));
    doLinksBox_.setSelected(false);
    UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(doLinksBox_, gbc);   
    
    //
    // Build the hide node name
    // 
    
    int nodeType = node.getNodeType();
    if (NodeProperties.canHideName(nodeType)) {
      hideNameBox_ = new JCheckBox(rMan.getString("nprop.hideNodeName"));
      hideNameBox_.setSelected(props_.getHideName());
      UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(hideNameBox_, gbc);
    }
 
    //
    // Build the direction panel, if needed:
    //
    
    JPanel orientPanel = nps_.orientUI(node);
    if (orientPanel != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      retval.add(orientPanel, gbc);
    }
    
    //
    // Build the extra pad growth orientation:
    //
    
    JPanel xtraG = nps_.extraGrowthUI(false, nodeType, false, null);
    if (xtraG != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      retval.add(xtraG, gbc);
    }
    
    //
    // Font override:
    //
    
    JPanel fonto = nps_.fontOverrideUI(false);
    UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(fonto, gbc);
    
    //
    // Line breaks:
    //
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    if (!haveDynInstance) {
      JPanel lineBr = nps_.getLineBreakUI();
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
      retval.add(lineBr, gbc);    
    }
 
    return (retval);
  }

  /***************************************************************************
  **
  ** Apply the current node property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    String ref = props_.getReference();
    Node node = dacx_.getGenome().getNode(ref);
    
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = dacx_.getGenomeAsInstance();
      topTwoLevels = gi.isRootInstance();
    }
    
    if (haveRoot) {
      nps_.setLineBreak(props_.getLineBreakDef(), false);
      nameField_.setText(node.getName());
    } else if (!haveDynInstance) {
      NodeInstance ni = (NodeInstance)node;    
      String overName = ni.getOverrideName();
      if (overName != null) {
        localNameField_.setEnabled(true);
        localNameLabel_.setEnabled(true); 
        localBox_.setSelected(true);
        nps_.setLineBreak(props_.getLineBreakDef(), true);
        localNameField_.setText(overName);
      } else {
        localNameField_.setEnabled(false);
        localNameLabel_.setEnabled(false); 
        localBox_.setSelected(false);
        nps_.setLineBreak(props_.getLineBreakDef(), false);
        localNameField_.setText(null);
      }
      // happens AFTER localBox is set to stock line break UI correctly!
      nameField_.setText(ni.getRootName());
      
      //
      // Activity level:
      //
      nps_.setActivityLevel(ni);
    } 
   
    colorWidget1_.setCurrentColor(props_.getColorName());    
    
    if (hideNameBox_ != null) {
      hideNameBox_.setSelected(props_.getHideName());    
    }
    
    int nodeType = node.getNodeType();
    
    if (nodeType == Node.INTERCELL) {
      String col2Name = props_.getSecondColorName();      
      if (col2Name == null) {
        col2Name = props_.getColorName();  
      }       
      colorWidget2_.setCurrentColor(col2Name);
    }
       
    if (NodeProperties.getOrientTypeCount(nodeType) > 1) {
      nps_.orientDisplay(props_.getOrientation());
    } 
  
    if (!haveDynInstance) {  // does this ever reject?
      if (DBNode.getPadIncrement(node.getNodeType()) != 0) {
        int pads = node.getPadCount();
        boolean amBig = (pads > DBNode.getDefaultPadCount(nodeType));
        int growthDir = NodeProperties.usesGrowth(nodeType) ? props_.getExtraGrowthDirection() 
                                                            : NodeProperties.UNDEFINED_GROWTH;
        nps_.setExtraPads(pads, amBig, growthDir);
      }
    }  
    if (topTwoLevels) {
      nps_.displayNodeFreeText();
      nps_.displayNodeURLs();
    }
      
    nps_.fontOverrideDisplay(props_.getFontOverride(), FontManager.MEDIUM);       
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the node properties
  ** 
  */
  
  private boolean applyProperties() {
    String ref = props_.getReference();

    Node node = dacx_.getGenome().getNode(ref);
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;   
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = dacx_.getGenomeAsInstance();
      topTwoLevels = gi.isRootInstance();
    }
      
    ResourceManager rMan = appState_.getRMan();
    boolean globalNameChange = false;
     
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.fgho.getGlobalNetModuleLinkPadNeeds();
  
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.nprop");     
    
    if (!haveDynInstance) {
 
      boolean rootChange = false;

      //
      // Even though we do not restrict non-gene node names, they cannot share
      // names with genes.
      //
      boolean useLocal = false;
      String oldLocal = null;
      String newLocal = null;
      boolean localShift = false;
    
      if (haveStatInstance) {
        NodeInstance ni = (NodeInstance)node;
        useLocal = localBox_.isSelected();
        oldLocal = ni.getOverrideName();
        newLocal = localNameField_.getText().trim();
        localShift = ((oldLocal == null) && useLocal) ||
                      ((oldLocal != null) && !useLocal) ||
                       (useLocal && !newLocal.equals(oldLocal));
        if (localShift) {
          if (dacx_.fgho.matchesExistingGeneName(newLocal)) {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                          rMan.getString("nodeProp.dupName"), 
                                          rMan.getString("nodeProp.dupNameTitle"),
                                          JOptionPane.ERROR_MESSAGE);         
            return (false);
          }
          if (dacx_.getInstructSrc().haveBuildInstructions()) {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                         rMan.getString("instructWarning.changeMessage"), 
                                         rMan.getString("instructWarning.changeTitle"),
                                         JOptionPane.WARNING_MESSAGE);
          }
        }
      }
      
      String newName = nameField_.getText().trim();
      String oldName = (!haveRoot) ? ((NodeInstance)node).getRootName() : node.getName();
      if (!newName.equals(oldName)) {
        if (dacx_.fgho.matchesExistingGeneName(newName)) { 
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("nodeProp.dupName"), 
                                        rMan.getString("nodeProp.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                       rMan.getString("instructWarning.changeMessage"), 
                                       rMan.getString("instructWarning.changeTitle"),
                                       JOptionPane.WARNING_MESSAGE);
        }
        //
        // If name change disconnects from underlying data, we let user choose how
        // to handle it:
        //
    
        if (dacx_.getExpDataSrc().hasDataAttachedByDefault(ref)) {    
          NameChangeChoicesDialog nccd = new NameChangeChoicesDialog(appState_, dacx_, ref, oldName, newName, support);  
          nccd.setVisible(true);
          if (nccd.userCancelled()) {
            return (false);
          }
        }
        GenomeChange gc = dacx_.getGenome().changeNodeName(ref, newName);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
          globalNameChange = true;
          rootChange = true;
        }
      }
      
      //
      // New text description
      //
      
      if (topTwoLevels) {
        if (nps_.installNodeFreeText(support)) {
          rootChange = true;
        }
        if (nps_.installNodeURLs(support)) {
          rootChange = true;
        }
      }
      
      if (DBNode.getPadIncrement(node.getNodeType()) != 0) {
        if (!nps_.checkExtraPads(node, false)) {
          return (false);
        }
        Integer extraPadsObj = nps_.getExtraPads();
        int newPads = (extraPadsObj == null) ? DBNode.getDefaultPadCount(node.getNodeType())
                                             : extraPadsObj.intValue();
        int pads = node.getPadCount();
        if (pads != newPads) {
          GenomeChange gc = dacx_.getGenome().changeNodeSize(ref, newPads);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
            support.addEdit(gcc);
            rootChange = true;
          }
        }
      }
      
      if (rootChange) {
        String id = (!haveRoot) ? dacx_.getDBGenome().getID() : dacx_.getGenome().getID();
        ModelChangeEvent mcev = new ModelChangeEvent(id, ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    
      boolean instanceChange = false;
      if (haveStatInstance) {
        NodeInstance ni = (NodeInstance)node;
        NodeInstance.ActivityState newNiAs = nps_.getActivityLevel();
        if (newNiAs == null) {
          return (false);
        }

        int oldActivity = ni.getActivity();
        boolean levelChanged = false;
        if (newNiAs.activityState == NodeInstance.VARIABLE) {
          if (oldActivity == NodeInstance.VARIABLE) {
            double oldLevel = ni.getActivityLevel();
            levelChanged = (oldLevel != newNiAs.activityLevel.doubleValue());
          }
        }

        if ((oldActivity != newNiAs.activityState) || levelChanged) {
          GenomeItemInstance.ActivityTracking tracking = ni.calcActivityBounds(dacx_.getGenomeAsInstance());
          if (!nps_.checkActivityBounds(tracking, newNiAs)) {
            return (false);
          }
        }
        
        if ((oldActivity != newNiAs.activityState) || localShift || levelChanged) {
          NodeInstance copyNode = (NodeInstance)node.clone();
          copyNode.overrideName((!useLocal) ? null : newLocal);
          copyNode.setActivity(newNiAs.activityState);
          if (newNiAs.activityState == NodeInstance.VARIABLE) {
            copyNode.setActivityLevel(newNiAs.activityLevel.doubleValue());
          }          
          GenomeChange gc = dacx_.getGenome().replaceNode(copyNode);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
            support.addEdit(gcc);
            instanceChange = true;
          }
        }
      }
    
      if (rootChange || instanceChange) {
        // Actually, all subinstances are affected by a root change, not just me!  FIX ME??
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    }
    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    NodeProperties changedProps = props_.clone(); 

    String primeColorName = colorWidget1_.getCurrentColor();    
    changedProps.setColor(primeColorName); 
    String linkColorName = primeColorName;

    int nodeType = node.getNodeType();
    if (nodeType == Node.INTERCELL) {
      String secondColorName = colorWidget2_.getCurrentColor();
      if (!secondColorName.equals(primeColorName)) {
        changedProps.setSecondColor(secondColorName);
      } else {
        changedProps.setSecondColor(null);
      }
      linkColorName = secondColorName;
    }  
      
    if (NodeProperties.getOrientTypeCount(nodeType) > 1) { 
      changedProps.setOrientation(nps_.getOrientation());
    }
    
    Integer extraGrowth = nps_.getExtraGrowth(nodeType);
    
    if (extraGrowth != null) {
      changedProps.setExtraGrowthDirection(extraGrowth.intValue());
    }     
    
    boolean doHide = false;      
    if (hideNameBox_ != null) {
      doHide = hideNameBox_.isSelected();
      changedProps.setHideName(doHide);
    }
    
    //
    // Handle font overrrides:
    //
    changedProps.setFontOverride(nps_.getFontOverride());
    
    //
    // Handle line break defs:
    //
    
    if (!haveDynInstance) {
      String untrimmed = nps_.getBaseStringForDef();
      changedProps.trimAndSetLineBreakDef(nps_.getLineBreakDef(), untrimmed);
    }
    
    Layout.PropChange[] lpc = new Layout.PropChange[1];    
    lpc[0] = dacx_.getLayout().replaceNodeProperties(props_, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);
      props_ = changedProps;
      LayoutChangeEvent ev = new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev);
      
      if (doLinksBox_.isSelected()) {
        applyColorToLinks(support, node, linkColorName);
      }      
    }
    
    if (globalNameChange) {
      LineBreaker.LineBreakChangeSteps steps = nps_.getNameChangeTracking();
      ModificationCommands.changeNodeNameBreaks(appState_, dacx_, ref, dacx_.getGenome(), steps, nameField_.getText(), support);
    }    
    
    if (globalPadNeeds != null) {
      ModificationCommands.repairNetModuleLinkPadsGlobally(appState_, dacx_, globalPadNeeds, false, support);
    }

    support.finish();        
    return (true);
  }

  /***************************************************************************
  **
  ** Apply our UI values to the linkage properties
  */
  
  private void applyColorToLinks(UndoSupport support,Node node, String colName) {
                
    //
    // Find any link properties for an outbound link:
    //

    BusProperties props = null;
    String srcID = node.getID();
    Iterator<Linkage> lit = dacx_.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(srcID)) {
        props = dacx_.getLayout().getLinkProperties(link.getID());
        break;
      }
    }
    
    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    if (props == null) {
      return;
    }
    BusProperties changedProps = props.clone(); 
    changedProps.setColor(colName);
   
    Layout.PropChange[] lpc = new Layout.PropChange[1];    
    lpc[0] = dacx_.getLayout().replaceLinkProperties(props, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);    
    } 

    return;
  }  
}
