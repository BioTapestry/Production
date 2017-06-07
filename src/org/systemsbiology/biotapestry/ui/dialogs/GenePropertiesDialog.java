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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.link.LinkSupport;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
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
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
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

  private NodeProperties props_;
  private String layoutKey_;
  private JCheckBox doLinksBox_;
  private StaticDataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private HarnessBuilder hBld_;
  private Gene gene_;
  private Genome genome_;
  
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
  
  public GenePropertiesDialog(UIComponentSource uics, StaticDataAccessContext dacx, HarnessBuilder hBld, NodeProperties props, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("gprop.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    hBld_ = hBld;
    props_ = props;
    layoutKey_ = dacx_.getCurrentLayoutID();
    
    genome_ = dacx_.getCurrentGenome();
    boolean forRoot = dacx_.currentGenomeIsRootDBGenome();
    boolean topTwoLevels = forRoot;
    if (genome_ instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome_;
      topTwoLevels = gi.isRootInstance();
    }
    String ref = props_.getReference();
    gene_ = genome_.getGene(ref);
    
    nps_ = new NodeAndLinkPropertiesSupport(uics_, dacx_, ref); 
    
    ResourceManager rMan = dacx_.getRMan();    
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

    DialogSupport ds = new DialogSupport(this, uics_, dacx_, gbc);
    ds.buildAndInstallButtonBox(cp, 9, 10, true, false); 
    setLocationRelativeTo(uics_.getTopFrame());
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
    ResourceManager rMan = dacx_.getRMan();
    
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
            uics_.getExceptionHandler().displayException(ex);
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
    
    evidenceCombo_ = new JComboBox(DBGene.getEvidenceChoices(dacx_));    
    
    label = new JLabel(rMan.getString("nprop.evidence"));
    UiUtil.gbcSet(gbc, 0, rownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, rownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    retval.add(evidenceCombo_, gbc);
    
    //
    // Extra pads:
    //
    
    Vector<ChoiceContent> forExtra = nps_.padsToCC(NodeAndLinkPropertiesSupport.generatePadChoices(gene_), false);
    JPanel xtraPads = nps_.extraPadsUI(false, forExtra, false, null);     
    if (xtraPads != null) {
      UiUtil.gbcSet(gbc, 0, rownum++, 11, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      retval.add(xtraPads, gbc);
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
    ResourceManager rMan = dacx_.getRMan();
           
    //
    // Build the color panel.
    //
    
    colorWidget_ = new ColorSelectionWidget(uics_, dacx_, hBld_, null, true, "nprop.color", true, false);

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
      evidenceCombo_.setSelectedItem(DBGene.evidenceTypeForCombo(dacx_, evidence));
      int pads = gene_.getPadCount();
      boolean amBig = (pads > DBGene.DEFAULT_PAD_COUNT);
      nps_.setExtraPads(new ChoiceContent(Integer.toString(pads), pads), amBig, NodeProperties.UNDEFINED_GROWTH);
    }

    colorWidget_.setCurrentColor(props_.getColorName());
    
    nps_.orientDisplay(props_.getOrientation());
 
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

    Layout layout = dacx_.getCurrentLayout();
    boolean haveDynInstance = (genome_ instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (genome_ instanceof GenomeInstance) && !haveDynInstance;    
    boolean haveRoot = !haveStatInstance && !haveDynInstance;
    boolean topTwoLevels = haveRoot;
    if (haveStatInstance) {
      GenomeInstance gi = (GenomeInstance)genome_;
      topTwoLevels = gi.isRootInstance();
    }
    boolean globalNameChange = false;
    Map<String, Layout.PadNeedsForLayout> globalPadNeeds = dacx_.getFGHO().getGlobalNetModuleLinkPadNeeds();
    Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla = null;
    Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr = null;
    String baseGene = GenomeItemInstance.getBaseID(gene_.getID());
    //
    // Check for correctness before continuing:
    //
    
    boolean hasReg = (gene_.getNumRegions() > 0);
    List<DBGeneRegion> modRegions = null;
    if (!haveDynInstance && hasReg) {    
      Integer newPadsObj = nps_.getExtraPads();
      int newPads = (newPadsObj == null) ? DBGene.DEFAULT_PAD_COUNT : newPadsObj.intValue();
      int currPads = gene_.getPadCount();
 
      if (newPads != currPads) {
        gla = dacx_.getFGHO().analyzeLinksIntoModules(baseGene);
        lhr = DBGeneRegion.linkPadRequirement(gla, gene_.getID());
        Set<DBGeneRegion.DBRegKey> linkHolders = dacx_.getFGHO().holdersWithLinks(baseGene, gla);
        List<DBGeneRegion> regList = DBGeneRegion.initTheList(gene_);
        // With growth, we just need to add pads to the region definitions:
        if (newPads > currPads) {
          modRegions = DBGeneRegion.stretchTheList(regList, newPads); 
        } else if (newPads < currPads) {
          int minRegions = DBGeneRegion.compressedRegionsWidth(baseGene, regList, gla, linkHolders);
          if (minRegions > newPads) {
            JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                          dacx_.getRMan().getString("geneProp.badRegions"), 
                                          dacx_.getRMan().getString("geneProp.badRegionTitle"),
                                          JOptionPane.ERROR_MESSAGE);         
            return (false);
          }
          modRegions = DBGeneRegion.compressTheList(baseGene, regList, currPads, newPads, gla, linkHolders);
        }
      }
    }
    
    if (!haveDynInstance ) {
      //
      // Starting in 7.1, can't make gene shorter than the links into it:
      //
      if (!nps_.checkExtraPads(gene_, false)) {
        return (false);
      }  
    }
    
    // One instance of BT-10-27-09:4 crashes here.  Need to not check name field with dynamic instance:
    String newName = null;
    String oldName = null;
    if (!haveDynInstance) {
      newName = nameField_.getText().trim();
      if (newName.equals("")) {
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      dacx_.getRMan().getString("geneProp.badName"), 
                                      dacx_.getRMan().getString("geneProp.badNameTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      }  
      oldName = (!haveRoot) ? ((GeneInstance)gene_).getRootName() : gene_.getName();
      if (!newName.equals(oldName)) {
        if (dacx_.getFGHO().matchesExistingGeneOrNodeName(newName, genome_, ref)) { 
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        dacx_.getRMan().getString("geneProp.dupName"), 
                                        dacx_.getRMan().getString("geneProp.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                       dacx_.getRMan().getString("instructWarning.changeMessage"), 
                                       dacx_.getRMan().getString("instructWarning.changeTitle"),
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
        JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                      dacx_.getRMan().getString("geneProp.badName"), 
                                      dacx_.getRMan().getString("geneProp.badNameTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      } 
      localShift = ((oldLocal == null) && useLocal) ||
                    ((oldLocal != null) && !useLocal) ||
                     (useLocal && !newLocal.equals(oldLocal));
      if (localShift) {
        if (dacx_.getFGHO().matchesExistingGeneOrNodeName(newLocal, genome_, ref)) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                        dacx_.getRMan().getString("geneProp.dupName"), 
                                        dacx_.getRMan().getString("geneProp.dupNameTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }
        if (dacx_.getInstructSrc().haveBuildInstructions()) {
          JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                       dacx_.getRMan().getString("instructWarning.changeMessage"), 
                                       dacx_.getRMan().getString("instructWarning.changeTitle"),
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
   
    
    UndoSupport support = uFac_.provideUndoSupport("undo.gprop", dacx_);         
    
    //
    // If name change disconnects from underlying data, we let user choose how
    // to handle it:
    //
    
    if (!haveDynInstance) {
      if (!DataUtil.keysEqual(newName.trim(), oldName) && dacx_.getExpDataSrc().hasDataAttachedByDefault(ref)) {    
        NameChangeChoicesDialog nccd = new NameChangeChoicesDialog(uics_, dacx_, ref, oldName, newName, support);  
        nccd.setVisible(true);
        if (nccd.userCancelled()) {
          return (false);
        }
      }

      boolean rootChange = false;
      if (!newName.trim().equals(oldName)) {
        GenomeChange gc = genome_.changeGeneName(ref, newName);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
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
          GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
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
          GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
          support.addEdit(gcc);
          rootChange = true;
        }
      }
      
      if (modRegions != null) {
        GenomeChange gc = dacx_.getDBGenome().changeGeneRegions(gene_.getID(), modRegions);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
          support.addEdit(gcc);
          rootChange = true;
        }
        LinkSupport.moveCisRegModLinks(support, modRegions, null, gla, lhr, dacx_, GenomeItemInstance.getBaseID(gene_.getID()), null);
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
    
      if (rootChange) {
        String id = (!haveRoot) ? dacx_.getDBGenomeID() : genome_.getID();
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), id, ModelChangeEvent.UNSPECIFIED_CHANGE);
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
            GenomeChangeCmd gcc = new GenomeChangeCmd(dacx_, gc);
            support.addEdit(gcc);
            instanceChange = true;
          }
        }
      }
    
      if (rootChange || instanceChange) {
        // Actually, all subinstances are affected by a root change, not just me!  FIX ME??
        ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), genome_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
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
      PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
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
      ModificationCommands.changeNodeNameBreaks(dacx_, ref, steps, nameField_.getText(), support);
    }
    
    if (globalPadNeeds != null) {
      ModificationCommands.repairNetModuleLinkPadsGlobally(dacx_, globalPadNeeds, false, support);
    }    
    
        
    support.finish();
    gene_ = genome_.getGene(ref); // If doing APPLY, this changes!
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
      PropChangeCmd mov = new PropChangeCmd(dacx_, lpc);
      support.addEdit(mov);    
    } 

    return;
  }
  
  /***************************************************************************
  **
  ** Evidence combos
  */
 
  private List<EnumCell> buildEvidenceCombos() {  
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    StringBuffer buf = new StringBuffer();
    ResourceManager rMan = dacx_.getRMan();
    Set<String> evals = DBGene.evidenceLevels();
    Iterator<String> evit = evals.iterator();
    int count = 0;
    while (evit.hasNext()) {
      String elev = evit.next();
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
