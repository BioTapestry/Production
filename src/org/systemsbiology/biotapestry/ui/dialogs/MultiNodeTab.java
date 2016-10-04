/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JPanel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.ColorSelectionWidget;
import org.systemsbiology.biotapestry.util.TriStateJComboBox;

/****************************************************************************
**
** A tab for editing multiple nodes at once
*/

public class MultiNodeTab {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
     
  private ColorSelectionWidget colorWidget1_;
  private Set<String> nodes_;
  private NodeAndLinkPropertiesSupport nps_;
  private boolean isForGene_;
  private TriStateJComboBox labelHidden_;
  private JComboBox evidenceCombo_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private List<ColorDeletionListener> colorListeners_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Constructor
  ** 
  */
  
  public MultiNodeTab(BTState appState, DataAccessContext dacx, Set<String> nodes, 
                      boolean isForGene, List<ColorDeletionListener> cdls) {
    appState_ = appState;
    dacx_ = dacx;
    nodes_ = nodes;
    isForGene_ = isForGene;
    colorListeners_ = cdls;
    nps_ = new NodeAndLinkPropertiesSupport(appState_, dacx);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
  /***************************************************************************
  **
  ** Build a tab 
  ** 
  */

  public JPanel buildNodeTab(boolean haveStatInstance, boolean haveDynInstance,
                             ConsensusNodeProps gcp, ImageIcon warnIcon) {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();      
    ResourceManager rMan = appState_.getRMan();

    int row = 0;
    JPanel modelPanel = null;
    if (!haveDynInstance) {
      modelPanel = new JPanel();
      modelPanel.setLayout(new GridBagLayout());
      modelPanel.setBorder(new TitledBorder(rMan.getString("multiSelProps.model")));
      UiUtil.gbcSet(gbc, 0, row++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(modelPanel, gbc);
    }

    JPanel layoutPanel = new JPanel();
    layoutPanel.setLayout(new GridBagLayout());
    layoutPanel.setBorder(new TitledBorder(rMan.getString("multiSelProps.layout")));
    UiUtil.gbcSet(gbc, 0, row++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(layoutPanel, gbc);

    int modelRownum = 0;
    int layoutRownum = 0;

    //
    // Evidence levels:
    //

    if (!haveDynInstance) {
      if ((gcp.evidenceCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) && 
          (gcp.evidenceCoverage != ConsensusProps.NO_OPTION_COVERAGE)) {   
        Vector<ChoiceContent> eviOpts = DBGene.getEvidenceChoices(appState_);
        eviOpts.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), Gene.LEVEL_VARIOUS));
        evidenceCombo_ = new JComboBox(eviOpts);    

        JLabel label = new JLabel(rMan.getString("nprop.evidence"));
        UiUtil.gbcSet(gbc, 0, modelRownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
        modelPanel.add(label, gbc);

        UiUtil.gbcSet(gbc, 1, modelRownum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        modelPanel.add(evidenceCombo_, gbc);
      }

      //
      // Activity setting:
      //

      if (haveStatInstance) {
        JPanel activ = nps_.activityLevelUI(true, false);     
        if (activ != null) {
          UiUtil.gbcSet(gbc, 0, modelRownum++, 11, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
          modelPanel.add(activ, gbc);
        }
      }

      //
      // Extra pads:
      //

      if ((gcp.extraPadCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) && 
          (gcp.extraPadCoverage != ConsensusProps.NO_OPTION_COVERAGE)) { 
        boolean isPartial = (gcp.extraPadCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE);
        Vector<ChoiceContent> forExtra = nps_.padsToCC(gcp.consensusPadOptions, true);
        JPanel xtraPads = nps_.extraPadsUI(true, forExtra, isPartial, warnIcon);     
        if (xtraPads != null) {
          UiUtil.gbcSet(gbc, 0, modelRownum++, 11, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.0);
          modelPanel.add(xtraPads, gbc);
        }
      }
    }

    //
    // Color setting:
    //

    colorWidget1_ = new ColorSelectionWidget(appState_, dacx_, colorListeners_, true, "nprop.color", true, true);
    UiUtil.gbcSet(gbc, 0, layoutRownum++, 11, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 0.0);
    layoutPanel.add(colorWidget1_, gbc);     

    //
    // Build the direction panel
    //

    if ((gcp.orientCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) && 
        (gcp.orientCoverage != ConsensusProps.NO_OPTION_COVERAGE)) {
      if (gcp.consensusNumOrient <= 1) {
        throw new IllegalStateException();
      }
      boolean isPartial = (gcp.orientCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE);
      JPanel orientPanel = nps_.orientUIForMulti(gcp.consensusNumOrient, isPartial, warnIcon);
      if (orientPanel != null) {
        UiUtil.gbcSet(gbc, 0, layoutRownum++, 11, 1, UiUtil.NONE, 0, 0, 0, 0, 0, 0, UiUtil.W, 0.0, 1.0);
        layoutPanel.add(orientPanel, gbc);
      }
    }

    //
    // HideLabel :
    //

    if ((gcp.hideLabelCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) && 
        (gcp.hideLabelCoverage != ConsensusProps.NO_OPTION_COVERAGE)) {

      JLabel hideLabelLabel;
      if (gcp.hideLabelCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE) {
        hideLabelLabel = new JLabel(rMan.getString("nprop.hideNodeName"), warnIcon, JLabel.CENTER);
      } else {
        hideLabelLabel = new JLabel(rMan.getString("nprop.hideNodeName"));
      } 
      UiUtil.gbcSet(gbc, 0, layoutRownum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      layoutPanel.add(hideLabelLabel, gbc);
      labelHidden_ = new TriStateJComboBox(appState_);
      UiUtil.gbcSet(gbc, 1, layoutRownum++, 10, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      layoutPanel.add(labelHidden_, gbc);
    }


    //
    // Font override:
    //

    JPanel fonto = nps_.fontOverrideUI(true);
    UiUtil.gbcSet(gbc, 0, layoutRownum++, 11, 1, UiUtil.NONE, 0, 0, 0, 0, 0, 0, UiUtil.W, 0.0, 1.0);
    layoutPanel.add(fonto, gbc);

    //
    // Growth Dir:
    //

    if ((gcp.growthCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) &&
        (gcp.extraPadCoverage != ConsensusProps.UNDEFINED_OPTION_COVERAGE) && 
        (gcp.growthCoverage != ConsensusProps.NO_OPTION_COVERAGE) && 
        (gcp.extraPadCoverage != ConsensusProps.NO_OPTION_COVERAGE)) { 
      boolean isPartial = (gcp.growthCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE);
      JPanel growthUI = nps_.extraGrowthUI(true, Node.NO_NODE_TYPE, isPartial, warnIcon);     
      if (growthUI != null) {
        UiUtil.gbcSet(gbc, 0, layoutRownum++, 11, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 0.0);
        layoutPanel.add(growthUI, gbc);
      }
    }

    //
    // Warning label, if needed:
    //

    if ((gcp.hideLabelCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE) ||
        (gcp.orientCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE) ||
        (gcp.growthCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE) ||
        (gcp.extraPadCoverage == ConsensusProps.PARTIAL_OPTION_COVERAGE)) {
      JLabel warnLabel = new JLabel(rMan.getString("multiSelProps.warnPartial"), warnIcon, JLabel.CENTER);
      UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(warnLabel, gbc); 
    }

    return (retval);     
  }

  /***************************************************************************
  **
  ** Apply the current node property values to our UI components
  ** 
  */

  public void displayForTab(ConsensusNodeProps cgp, boolean haveStatInstance, boolean haveDynamicInstance) {

    //
    // Now do the settings:
    //

    colorWidget1_.setCurrentColor((cgp.variousColor) ? ColorSelectionWidget.VARIOUS_TAG : cgp.consensusColor);   
    nps_.fontOverrideDisplayForMulti(cgp.consensusFontOverride, cgp.consensusFont, FontManager.GENE_NAME);
    if (nps_.haveOrientationForMulti()) {
      nps_.orientDisplayForMulti(cgp.consensusOrient);
    }
    if (labelHidden_ != null) {
      labelHidden_.setSetting(cgp.consensusHideLabel);
    }

    if (!haveDynamicInstance) {
      if (evidenceCombo_ != null) {
        if (cgp.consensusEvidence == Gene.LEVEL_VARIOUS) {
          evidenceCombo_.setSelectedIndex(0);
        } else {          
          evidenceCombo_.setSelectedItem(DBGene.evidenceTypeForCombo(appState_, cgp.consensusEvidence));
        }
      }

      String tag = (cgp.consensusExtraPadCount == ConsensusNodeProps.NO_PAD_CHANGE) ? appState_.getRMan().getString("multiSelProps.various")
                                                                                    : Integer.toString(cgp.consensusExtraPadCount);
      nps_.setExtraPadsForMulti(new ChoiceContent(tag, cgp.consensusExtraPadCount), cgp.consensusDoExtraPads, cgp.consensusGrowth);

      if (haveStatInstance) {
        nps_.setActivityLevelForMulti(cgp.consensusActivity, false);
      } 
    }
    return;
  }

  /***************************************************************************
  **
  ** Check for errors
  ** 
  */

  public boolean errorCheckForTab() { 
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;   
    //
    // Have to error check all activity changes before making the changes:
    //     
    Iterator<String> nit = nodes_.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Node node = dacx_.getGenome().getNode(nodeID);

      if (haveStatInstance) {
        NodeInstance gi = (NodeInstance)node;
        NodeInstance.ActivityState newNiAs = nps_.getActivityLevel();
        if (newNiAs == null) {
          return (false);
        }
        if (newNiAs.activityState == NodeInstance.ACTIVITY_NOT_SET) {
          continue;
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
          GenomeItemInstance.ActivityTracking tracking = gi.calcActivityBounds(dacx_.getGenomeAsInstance());
          if (!nps_.checkActivityBounds(tracking, newNiAs)) {
            return (false);
          }
        }
      }

      if (!nps_.checkExtraPads(node, true)) {
        return (false);
      }     
    }

    return (true);
  }

  /***************************************************************************
  **
  ** Apply our UI values to all the node properties
  ** 
  */

  public boolean applyProperties(UndoSupport support) { 
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance);
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);

    String primeColorName = colorWidget1_.getCurrentColor();

    boolean instanceChange = false;
    boolean modelChange = false;
    boolean doSig = false;
    
    Iterator<String> nit = nodes_.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Node node = dacx_.getGenome().getNode(nodeID);
      int nodeType = node.getNodeType();
      NodeProperties np = dacx_.getLayout().getNodeProperties(nodeID);
      NodeProperties changedProps = np.clone();
      boolean layoutChange = false;

      if (haveStatInstance) {
        NodeInstance ni = (NodeInstance)node;
        NodeInstance.ActivityState newNiAs = nps_.getActivityLevel();
        if (newNiAs == null) {
          throw new IllegalStateException();
        }
        if (newNiAs.activityState != NodeInstance.ACTIVITY_NOT_SET) {       
          int oldActivity = ni.getActivity();
          boolean levelChanged = false;
          if (newNiAs.activityState == NodeInstance.VARIABLE) {
            if (oldActivity == NodeInstance.VARIABLE) {
              double oldLevel = ni.getActivityLevel();
              levelChanged = (oldLevel != newNiAs.activityLevel.doubleValue());
            }
          }
          if ((oldActivity != newNiAs.activityState) || levelChanged) {
            NodeInstance copyNode = ni.clone();
            copyNode.setActivity(newNiAs.activityState);
            if (newNiAs.activityState == NodeInstance.VARIABLE) {
              copyNode.setActivityLevel(newNiAs.activityLevel.doubleValue());
            }  
            GenomeChange gc = (isForGene_) ? dacx_.getGenome().replaceGene((GeneInstance)copyNode) : dacx_.getGenome().replaceNode(copyNode);
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
              support.addEdit(gcc);
              instanceChange = true;
            }
          }
        }
      }

      if (labelHidden_ != null) {
        int labelState = labelHidden_.getSetting();
        if (labelState == TriStateJComboBox.TRUE) {
          layoutChange = true;
          changedProps.setHideName(true);
        } else if (labelState == TriStateJComboBox.FALSE) {
          layoutChange = true;
          changedProps.setHideName(false);
        }
      }

      if (!primeColorName.equals(ColorSelectionWidget.VARIOUS_TAG)) {
        layoutChange = true;
        changedProps.setColor(primeColorName);
        if (nodeType == Node.INTERCELL) {
          changedProps.setSecondColor(null);
        }
      }

      if (nps_.haveOrientationForMulti()) {
        int newOrient = nps_.getOrientation();
        int count = NodeProperties.getOrientTypeCount(nodeType);
        if ((newOrient != NodeProperties.NONE) && (count > 1)) {
          changedProps.setOrientation(newOrient);
          layoutChange = true;
        }
      }

      if (!haveDynInstance) {
        if (evidenceCombo_ != null) {
          int newEvidence = ((ChoiceContent)evidenceCombo_.getSelectedItem()).val;
          if (newEvidence != Gene.LEVEL_VARIOUS) {
            GenomeChange gc = dacx_.getGenome().changeGeneEvidence(nodeID, newEvidence);
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
              support.addEdit(gcc);
              modelChange = true;
            }
          }
        }

        Boolean wantExtra = nps_.wantExtraPadsForMulti();
        if (wantExtra != null) {
          boolean wantExtraPads = wantExtra.booleanValue();
          Integer newPads = null;
          if (wantExtraPads) {
            Integer xp = nps_.getExtraPadsForMulti();
            if (xp != null) {
              newPads = xp;
            }
          } else {
            newPads = Integer.valueOf(DBNode.getDefaultPadCount(nodeType));
          }
          if (newPads != null) {
            GenomeChange gc = (nodeType == Node.GENE) ? dacx_.getGenome().changeGeneSize(nodeID, newPads.intValue()) 
                                                      : dacx_.getGenome().changeNodeSize(nodeID, newPads.intValue());
            if (gc != null) {
              GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
              support.addEdit(gcc);
              modelChange = true;
            }
          }
          if (wantExtraPads) {
            Integer extraGrowth = nps_.getExtraGrowthForMulti();
            if ((extraGrowth != null) && (extraGrowth.intValue() != NodeProperties.NO_GROWTH)) {
              changedProps.setExtraGrowthDirection(extraGrowth.intValue());
              layoutChange = true;
            }
          }
        }
      }

      FontManager.FontOverride fo = nps_.getFontOverrideForMulti();
      if (fo != null) {
        changedProps.setFontOverride(fo);
        layoutChange = true;
      }

      if (layoutChange) {
        Layout.PropChange[] lpc = new Layout.PropChange[1];    
        lpc[0] = dacx_.getLayout().replaceNodeProperties(np, changedProps); 
        if (lpc[0] != null) {
          PropChangeCmd mov = new PropChangeCmd(appState_, dacx_, lpc);
          support.addEdit(mov);
          doSig = true;
        }
      }
    }

    
    if (modelChange) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getDBGenome().getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);
    }   
    
    if (instanceChange) {
      ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);
    }

    if (doSig) {
      LayoutChangeEvent ev = new LayoutChangeEvent(dacx_.getLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev); 
    }
    return (true);
  }
}    
