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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
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
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.IntegerEditor;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.ProtoInteger;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing gene properties
** FIX ME!!! Share implementation with node properties dialog!
*/

public class GenePropertiesDialog extends JDialog implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JTextField localNameField_;
  private JLabel localNameLabel_;
  private ColorSelectionWidget colorWidget_; 
  private JComboBox evidenceCombo_;    
  private JCheckBox localBox_;
  private JCheckBox regionBox_;

  private NodeProperties props_;
  private String layoutKey_;
  private JCheckBox doLinksBox_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private List regionEvidenceCells_;
  private Gene gene_;
  private Genome genome_;
  
  private EditableTable est_;
 
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
  
  public GenePropertiesDialog(BTState appState, DataAccessContext dacx, NodeProperties props) {     
    super(appState.getTopFrame(), appState.getRMan().getString("gprop.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    props_ = props;
    layoutKey_ = dacx_.getLayoutID();
    
    genome_ = dacx_.getGenome();
    boolean forRoot = dacx_.genomeIsRootGenome();
    boolean topTwoLevels = forRoot;
    if (genome_ instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome_;
      topTwoLevels = gi.isRootInstance();
    }
    String ref = props_.getReference();
    gene_ = genome_.getGene(ref);
    
    nps_ = new NodeAndLinkPropertiesSupport(appState_, dacx_, ref); 
    
    ResourceManager rMan = appState.getRMan();    
    setSize(750, 550);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
 
    //
    // Build the tabs.
    //

    JTabbedPane tabPane = new JTabbedPane();
    if (!(genome_ instanceof DynamicGenomeInstance)) {   
      tabPane.addTab(rMan.getString("propDialogs.modelProp"), buildModelTab());
    }
    tabPane.addTab(rMan.getString("propDialogs.layoutProp"), buildLayoutTab());
    if (topTwoLevels) {
      tabPane.addTab(rMan.getString("propDialogs.freeText"), nps_.buildTextTab(null, forRoot));
      tabPane.addTab(rMan.getString("propDialogs.URLTab"), nps_.buildUrlTab(null, forRoot));
    }
    
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tabPane, gbc);
    
    //
    // Build the button panel:
    //

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
  
  private JPanel buildModelTab() {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    
    if (genome_ instanceof DynamicGenomeInstance) {
      throw new IllegalArgumentException();
    }
    
    int rownum = 0;
    JLabel label = new JLabel(rMan.getString("nprop.name"));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    retval.add(nameField_, gbc);
 
    if (genome_ instanceof GenomeInstance) {
      localBox_ = new JCheckBox(rMan.getString("nprop.uselocal"));
      UiUtil.gbcSet(gbc, 0, rownum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
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
      UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      retval.add(localNameLabel_, gbc);

      UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
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
    // Evidence levels:
    //
    
    evidenceCombo_ = new JComboBox(DBGene.getEvidenceChoices(appState_));    
    
    label = new JLabel(rMan.getString("nprop.evidence"));
    UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    retval.add(evidenceCombo_, gbc);
    
    //
    // Extra pads:
    //
    
    JPanel xtraPads = nps_.extraPadsUI(false, NodeAndLinkPropertiesSupport.generatePadChoices(gene_), false, null);     
    if (xtraPads != null) {
      UiUtil.gbcSet(gbc, 0, rownum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(xtraPads, gbc);
    }   
    
    //
    // Region specification:
    //
    
    regionBox_ = new JCheckBox(rMan.getString("nprop.useRegions"));
    UiUtil.gbcSet(gbc, 0, rownum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(regionBox_, gbc);
    regionBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean selected = regionBox_.isSelected();
          est_.setEnabled(selected);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }        
      }
    });    

    regionEvidenceCells_ = buildEvidenceCombos();
    est_ = new EditableTable(appState_, new RegionTableModel(appState_), appState_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.cancelEditOnDisable = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(3), new EditableTable.EnumCellInfo(false, regionEvidenceCells_));  
    JPanel tablePan = est_.buildEditableTable(etp);
    UiUtil.gbcSet(gbc, 0, rownum, 11, 9, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(tablePan, gbc);
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
           
    //
    // Build the color panel.
    //
    
    colorWidget_ = new ColorSelectionWidget(appState_, dacx_, null, true, "nprop.color", true, false);

    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    retval.add(colorWidget_, gbc); 

    //
    // Build the do links too box
    //        
    
    doLinksBox_ = new JCheckBox(rMan.getString("nprop.doLinks"));
    // WJRL 4/30/09: Having the check box set has bad side effects: unexpected 
    // color changes when setting other tabs.  Drop it.  
    //doLinksBox_.setSelected(NodeProperties.setWithLinkColor(Node.GENE));
    UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(doLinksBox_, gbc);    

    //
    // Build the direction panel
    //
    
    JPanel orientPanel = nps_.orientUI(gene_);
    if (orientPanel != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      retval.add(orientPanel, gbc);
    }
    
    //
    // Font override:
    //
    
    JPanel fonto = nps_.fontOverrideUI(false);
    UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    retval.add(fonto, gbc);
    
    //
    // Line breaks:
    //
    
    boolean haveDynInstance = (genome_ instanceof DynamicGenomeInstance);
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
    boolean haveDynInstance = (genome_ instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (genome_ instanceof GenomeInstance) && !haveDynInstance;
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = (GenomeInstance)genome_;
      topTwoLevels = gi.isRootInstance();
    }
  
    if (haveRoot) {
      nps_.setLineBreak(props_.getLineBreakDef(), false);
      nameField_.setText(gene_.getName());
    } else if (!haveDynInstance) {
      NodeInstance ni = (NodeInstance)gene_;      
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
    
    if (!haveDynInstance) {
      int evidence = gene_.getEvidenceLevel();
      evidenceCombo_.setSelectedItem(DBGene.evidenceTypeForCombo(appState_, evidence));
      int pads = gene_.getPadCount();
      boolean amBig = (pads > DBGene.DEFAULT_PAD_COUNT);
      nps_.setExtraPads(pads, amBig, NodeProperties.UNDEFINED_GROWTH);
    }

    colorWidget_.setCurrentColor(props_.getColorName());
    
    nps_.orientDisplay(props_.getOrientation());
  
    if (!haveDynInstance) {
      boolean doRegions = (gene_.getNumRegions() > 0);
      regionBox_.setSelected(doRegions);
      est_.setEnabled(doRegions);
      int pads = gene_.getPadCount();
      List<DBGeneRegion> regions = buildRegionList();
      ((RegionTableModel)est_.getModel()).extractValues(regions, pads);
    }
    if (topTwoLevels) {
      nps_.displayNodeFreeText();
      nps_.displayNodeURLs();
    }
    
    nps_.fontOverrideDisplay(props_.getFontOverride(), FontManager.GENE_NAME);
   
    return;
  }
   
  /***************************************************************************
  **
  ** Apply our UI values to the node properties
  ** 
  */
  
  private boolean applyProperties() {
    String ref = props_.getReference();

    Layout layout = dacx_.getLayout();
    boolean haveDynInstance = (genome_ instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (genome_ instanceof GenomeInstance) && !haveDynInstance;    
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = (GenomeInstance)genome_;
      topTwoLevels = gi.isRootInstance();
    }
    boolean globalNameChange = false;
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.fgho.getGlobalNetModuleLinkPadNeeds();

    //
    // Check for correctness before continuing:
    //
    
    RegionTableModel rtm = (RegionTableModel)est_.getModel();
    
    if (!haveDynInstance && regionBox_.isSelected()) {
      if (!rtm.tableIntegerCheck()) {
        return (false);
      }
      Integer newPadsObj = nps_.getExtraPads();
      int newPads = (newPadsObj == null) ? DBGene.DEFAULT_PAD_COUNT : newPadsObj.intValue();
      if (!rtm.regionSemanticsCheck(newPads)) {   
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      dacx_.rMan.getString("regTable.badRegions"), 
                                      dacx_.rMan.getString("regTable.badRegionTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      }
    }
    
    // One instance of BT-10-27-09:4 crashes here.  Need to not check name field with dynamic instance:
    String newName = null;
    String oldName = null;
    if (!haveDynInstance) {
      newName = nameField_.getText().trim();
      if (newName.equals("")) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      dacx_.rMan.getString("geneProp.badName"), 
                                      dacx_.rMan.getString("geneProp.badNameTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      }  
      oldName = (!haveRoot) ? ((GeneInstance)gene_).getRootName() : gene_.getName();
      if (!newName.equals(oldName)) {
        if (dacx_.fgho.matchesExistingGeneOrNodeName(newName, genome_, ref)) { 
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        dacx_.rMan.getString("geneProp.dupName"), 
                                        dacx_.rMan.getString("geneProp.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                       dacx_.rMan.getString("instructWarning.changeMessage"), 
                                       dacx_.rMan.getString("instructWarning.changeTitle"),
                                       JOptionPane.WARNING_MESSAGE);
        }
      }
    }
    
    boolean useLocal = false;
    String oldLocal = null;
    String newLocal = null;
    boolean localShift = false;

    if (haveStatInstance) {
      GeneInstance gi = (GeneInstance)gene_;
      useLocal = localBox_.isSelected();
      oldLocal = gi.getOverrideName();
      newLocal = localNameField_.getText().trim();
      if (useLocal && newLocal.equals("")) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      dacx_.rMan.getString("geneProp.badName"), 
                                      dacx_.rMan.getString("geneProp.badNameTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      } 
      localShift = ((oldLocal == null) && useLocal) ||
                    ((oldLocal != null) && !useLocal) ||
                     (useLocal && !newLocal.equals(oldLocal));
      if (localShift) {
        if (dacx_.fgho.matchesExistingGeneOrNodeName(newLocal, genome_, ref)) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        dacx_.rMan.getString("geneProp.dupName"), 
                                        dacx_.rMan.getString("geneProp.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                       dacx_.rMan.getString("instructWarning.changeMessage"), 
                                       dacx_.rMan.getString("instructWarning.changeTitle"),
                                       JOptionPane.WARNING_MESSAGE);
        }
      }
          
      NodeInstance.ActivityState newNiAs = nps_.getActivityLevel();
      if (newNiAs == null) {
        return (false);
      }
 
      int oldActivity = gi.getActivity();
      boolean levelChanged = false;
      if (newNiAs.activityState == NodeInstance.VARIABLE) {
        if (oldActivity == NodeInstance.VARIABLE) {
          double oldLevel = gi.getActivityLevel();
          levelChanged = (oldLevel != newNiAs.activityLevel.doubleValue());
        }
      }

      if ((oldActivity != newNiAs.activityState) || levelChanged) {
        GenomeItemInstance.ActivityTracking tracking = gi.calcActivityBounds((GenomeInstance)genome_);
        if (!nps_.checkActivityBounds(tracking, newNiAs)) {
          return (false);
        }
      }
    }

    //
    // Undo/Redo support
   
    
    UndoSupport support = new UndoSupport(appState_, "undo.gprop");         
    
    //
    // If name change disconnects from underlying data, we let user choose how
    // to handle it:
    //
    
    if (!haveDynInstance) {
      if (!DataUtil.keysEqual(newName.trim(), oldName) && dacx_.getExpDataSrc().hasDataAttachedByDefault(ref)) {    
        NameChangeChoicesDialog nccd = new NameChangeChoicesDialog(appState_, dacx_, ref, oldName, newName, support);  
        nccd.setVisible(true);
        if (nccd.userCancelled()) {
          return (false);
        }
      }

      boolean rootChange = false;
      if (!newName.trim().equals(oldName)) {
        GenomeChange gc = genome_.changeGeneName(ref, newName);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          globalNameChange = true;
          support.addEdit(gcc);
          rootChange = true;
        }
      }

      int oldEvidence = gene_.getEvidenceLevel();
      int newEvidence = ((ChoiceContent)evidenceCombo_.getSelectedItem()).val;    
      if (oldEvidence != newEvidence) {
        GenomeChange gc = genome_.changeGeneEvidence(ref, newEvidence);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
          rootChange = true;
        }
      }
      
      //
      // Extra size:
      //
      
      Integer newPadsObj = nps_.getExtraPads();
      int newPads = (newPadsObj == null) ? DBGene.DEFAULT_PAD_COUNT : newPadsObj.intValue();
      int pads = gene_.getPadCount();
      if (pads != newPads) {
        GenomeChange gc = genome_.changeGeneSize(ref, newPads);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
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
           
      //
      // Handle application of region data:
      //
        
      if (regionBox_.isSelected()) {
        rootChange |= rtm.applyValues(support, (newPadsObj != null), newPads);
      } else {
        rootChange |= rtm.dumpValues(support);
      }
    
      if (rootChange) {
        String id = (!haveRoot) ? appState_.getDB().getGenome().getID() : genome_.getID();
        ModelChangeEvent mcev = new ModelChangeEvent(id, ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    
      boolean instanceChange = false;
      if (haveStatInstance) {       
        NodeInstance.ActivityState newNiAs = nps_.getActivityLevel();
        if (newNiAs == null) {
          throw new IllegalStateException();
        }
 
        GeneInstance gi = (GeneInstance)gene_;    
        int oldActivity = gi.getActivity();
        boolean levelChanged = false;
        if (newNiAs.activityState == NodeInstance.VARIABLE) {
          if (oldActivity == NodeInstance.VARIABLE) {
            double oldLevel = gi.getActivityLevel();
            levelChanged = (oldLevel != newNiAs.activityLevel.doubleValue());
          }
        }

        if ((oldActivity != newNiAs.activityState) || localShift || levelChanged) {
          GeneInstance copyGene = (GeneInstance)gene_.clone();
          copyGene.overrideName((!useLocal) ? null : newLocal);
          copyGene.setActivity(newNiAs.activityState);
          if (newNiAs.activityState == NodeInstance.VARIABLE) {
            copyGene.setActivityLevel(newNiAs.activityLevel.doubleValue());
          }  
          GenomeChange gc = genome_.replaceGene(copyGene);
          if (gc != null) {
            GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
            support.addEdit(gcc);
            instanceChange = true;
          }
        }
      }
    
      if (rootChange || instanceChange) {
        // Actually, all subinstances are affected by a root change, not just me!  FIX ME??
        ModelChangeEvent mcev = new ModelChangeEvent(genome_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
        support.addEvent(mcev);
      }
    }
    
    //
    // Though we hold layout's property directly, make change by changing
    // a clone and submitting it:
    //
    
    NodeProperties changedProps = props_.clone(); 
    
    String currCol = colorWidget_.getCurrentColor();
    changedProps.setColor(currCol);
    
    if (NodeProperties.getOrientTypeCount(Node.GENE) > 1) { 
      changedProps.setOrientation(nps_.getOrientation());    
    }
    
    //
    // Handle font overrides:
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
    lpc[0] = layout.replaceNodeProperties(props_, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);
      props_ = changedProps;
      LayoutChangeEvent ev = new LayoutChangeEvent(layoutKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev);
      
      if (doLinksBox_.isSelected()) {
        applyColorToLinks(support, genome_, gene_, layout, currCol);
      }
    }
    
    
    if (globalNameChange) {
      LineBreaker.LineBreakChangeSteps steps = new LineBreaker.LineBreakChangeSteps();
      ModificationCommands.changeNodeNameBreaks(appState_, dacx_, ref, genome_, steps, nameField_.getText(), support);
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
  
  private void applyColorToLinks(UndoSupport support, Genome genome, Gene gene, 
                                 Layout layout, String colName) {
                
    //
    // Find any link properties for an outbound link:
    //

    BusProperties props = null;
    String srcID = gene.getID();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(srcID)) {
        props = layout.getLinkProperties(link.getID());
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
    lpc[0] = layout.replaceLinkProperties(props, changedProps); 
        
    if (lpc[0] != null) {
      PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
      support.addEdit(mov);    
    } 

    return;
  }
 
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class RegionTableModel extends EditableTable.TableModel {
    
    private final static int NAME_     = 0;
    private final static int START_    = 1;
    private final static int END_      = 2;
    private final static int EVIDENCE_ = 3;
    private final static int NUM_COL_  = 4; 
    
    private static final long serialVersionUID = 1L;
    
    private String[] greek_ = {"α","β","γ","δ","ε","ζ","η","θ","ι","κ","λ","μ","ν","ξ","ο","π","ρ","σ","τ","υ","φ","χ","ψ","ω"};
   
    RegionTableModel(BTState appState) {      
      super(appState, NUM_COL_);
      colNames_ = new String[] {"regtable.name",
                                "regtable.start",
                                "regtable.end",
                                "regtable.evidence"};
      colClasses_ = new Class[] {String.class,
                                 ProtoInteger.class,
                                 ProtoInteger.class,
                                 EnumCell.class};
    }
    
    public List getValuesFromTable() {
      throw new UnsupportedOperationException();
    }
    
    public boolean addRow(int[] rows) {
      super.addRow(rows);
      int lastIndex = rowCount_ - 1;
      columns_[NAME_].set(lastIndex, genName());
      columns_[START_].set(lastIndex, new ProtoInteger(0)); 
      columns_[END_].set(lastIndex, new ProtoInteger(0));
      columns_[EVIDENCE_].set(lastIndex, 
                              getEvidenceForLevel(Gene.LEV_NONE_STR, regionEvidenceCells_));
      return (true);
    }
  
    private String genName() {
      int numGreek = greek_.length;
      int numName = columns_[NAME_].size();
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i <= numGreek; i++) {
        for (int j = 0; j < numGreek; j++) {
          buf.setLength(0);
          if (i > 0) {
            buf.append(greek_[i - 1]);
          }
          buf.append(greek_[j]);
          String retval = buf.toString();
          boolean skipIt = false;
          for (int k = 0; k < numName; k++) {
            if (retval.equals(columns_[NAME_].get(k))) {
              skipIt = true;
              break;
            }
          }
          if (!skipIt) {
            return (retval);
          }
        }
      }
      ResourceManager rMan = appState_.getRMan();      
      return (rMan.getString("regtable.newRegion"));
    }
    
    void extractValues(List regions, int padCount) {
      super.extractValues(regions);
      Iterator<DBGeneRegion> rit = regions.iterator();
      while (rit.hasNext()) {
        DBGeneRegion region = rit.next();
        columns_[NAME_].add(region.getName());
        int startPad = region.getStartPad();
        startPad = mapBoundToDisplay(startPad, padCount);
        columns_[START_].add(new ProtoInteger(startPad));
        int endPad = region.getEndPad();
        endPad = mapBoundToDisplay(endPad, padCount);        
        columns_[END_].add(new ProtoInteger(endPad));
        String evtag = DBGene.mapToEvidenceTag(region.getEvidenceLevel());
        columns_[EVIDENCE_].add(getEvidenceForLevel(evtag, regionEvidenceCells_));
      }
      return;
    }

    boolean applyValues(UndoSupport support, boolean useCount, int padCount) {
      if (gene_ == null) {
        return (false);
      }
      boolean retval = false;
      List<DBGeneRegion> newRegions = regionsChanged(useCount, padCount);
      if (newRegions != null) {
        GenomeChange gc = genome_.changeGeneRegions(gene_.getID(), newRegions);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
          retval = true;
        }
      }
      return (retval);
    }
    
    boolean dumpValues(UndoSupport support) {
      if (gene_.getNumRegions() != 0) {
        GenomeChange gc = genome_.changeGeneRegions(gene_.getID(), new ArrayList());
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
          return (true);
        }
      }
      return (false);
    }
    
    private List<DBGeneRegion> regionsChanged(boolean useCount, int padCount) {
      //
      // Crank thru the regions and compare to table values.  If we hit a change,
      // we return the new list of regions:
      //
      ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
      if (rowCount_ != gene_.getNumRegions()) {
        return (regionTransfer(useCount, padCount));
      }
      Iterator<DBGeneRegion> rit = gene_.regionIterator();
      int count = 0;
      boolean haveChange = false;
      while (rit.hasNext()) {
        DBGeneRegion oldReg = rit.next();
        String name = (String)columns_[NAME_].get(count);
        int startPad = ((ProtoInteger)columns_[START_].get(count)).value;
        startPad = mapBoundToInternal(startPad, useCount, padCount);
        int endPad = ((ProtoInteger)columns_[END_].get(count)).value;
        endPad = mapBoundToInternal(endPad, useCount, padCount);
        int newEvidence = ((EnumCell)columns_[EVIDENCE_].get(count)).value;
        DBGeneRegion newReg = new DBGeneRegion(name, startPad, endPad, newEvidence);
        if (!oldReg.equals(newReg)) {
          haveChange = true;
        }
        retval.add(newReg);
        count++;
      }
      return (haveChange ? retval : null);
    }
    
    private List<DBGeneRegion> regionTransfer(boolean useCount, int padCount) {
      ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
      for (int i = 0; i < rowCount_; i++) {
        String name = (String)columns_[NAME_].get(i);
        int startPad = ((ProtoInteger)columns_[START_].get(i)).value;
        startPad = mapBoundToInternal(startPad, useCount, padCount);
        int endPad = ((ProtoInteger)columns_[END_].get(i)).value;
        endPad = mapBoundToInternal(endPad, useCount, padCount);
        int newEvidence = ((EnumCell)columns_[EVIDENCE_].get(i)).value;        
        DBGeneRegion newReg = new DBGeneRegion(name, startPad, endPad, newEvidence);        
        retval.add(newReg);
      }
      return (retval);
    }
    
    boolean tableIntegerCheck() {
      for (int i = 0; i < rowCount_; i++) {
        ProtoInteger num = (ProtoInteger)columns_[START_].get(i);
        if (!num.valid) {
          IntegerEditor.triggerWarning(appState_, appState_.getTopFrame());
          return (false);
        }
        num = (ProtoInteger)columns_[END_].get(i);
        if (!num.valid) {
          IntegerEditor.triggerWarning(appState_, appState_.getTopFrame());
          return (false);
        }
      }
      return (true);
    }
    
    boolean regionSemanticsCheck(int maxPad) {
      HashSet<Integer> usedPads = new HashSet<Integer>();
      HashSet<String> usedNames = new HashSet<String>();

      for (int i = 0; i < rowCount_; i++) {
        int startPad = ((ProtoInteger)columns_[START_].get(i)).value;          
        int endPad = ((ProtoInteger)columns_[END_].get(i)).value;
        if ((startPad < 0) || (endPad < 0)) {
          return (false);
        }
        if ((startPad >= maxPad) || (endPad >= maxPad)) {
          return (false);
        }
        if (startPad > endPad) {
          return (false);
        }
        // Check for overlap: none allowed
        for (int j = startPad; j <= endPad; j++) {
          if (!usedPads.add(new Integer(j))) {
            return (false);
          }
        }
        // duplicate names not allowed, except for multiple blank names
        String name = (String)columns_[NAME_].get(i);
        if (name == null) {
          return (false);
        }
        name = name.trim();
        if (name.equals("")) {
          continue;
        }
        if (DataUtil.containsKey(usedNames, name)) {
          return (false);
        }
        usedNames.add(name);
      }
      return (true);
    }
   
  
    private int mapBoundToInternal(int bound, boolean useCount, int padCount) {
      int extraPads = (useCount) ? padCount - DBGene.DEFAULT_PAD_COUNT : 0;      
      return (bound - extraPads);
    }
    
    private int mapBoundToDisplay(int bound, int padCount) {
      int extraPads = (padCount > DBGene.DEFAULT_PAD_COUNT) ? padCount - DBGene.DEFAULT_PAD_COUNT : 0;
      return (bound + extraPads);
    }
    
  }
  
  /***************************************************************************
  **
  ** Evidence combos
  */
 
  private List buildEvidenceCombos() {  
    ArrayList retval = new ArrayList();
    StringBuffer buf = new StringBuffer();
    ResourceManager rMan = appState_.getRMan();
    Set evals = DBGene.evidenceLevels();
    Iterator evit = evals.iterator();
    int count = 0;
    while (evit.hasNext()) {
      String elev = (String)evit.next();
      buf.setLength(0);
      buf.append("nprop.");
      buf.append(elev);        
      String desc = rMan.getString(buf.toString());     
      retval.add(new EnumCell(desc, elev, DBGene.mapFromEvidenceTag(elev), count++));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the combo for the desired level
  */
 
  private EnumCell getEvidenceForLevel(String matchElev, List cells) {  
    Iterator cit = cells.iterator();
    while (cit.hasNext()) {
      EnumCell elev = (EnumCell)cit.next();
      if (elev.internal.equals(matchElev)) {
        return (new EnumCell(elev));
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Build a region list
  */
  
  private List<DBGeneRegion> buildRegionList() {
    ArrayList<DBGeneRegion> retval = new ArrayList<DBGeneRegion>();
    if (gene_ == null) {
      return (retval);
    }
    Iterator<DBGeneRegion> rit = gene_.regionIterator();
    while (rit.hasNext()) {
      DBGeneRegion region = rit.next();
      retval.add(region);
    }
    return (retval);
  }
}
