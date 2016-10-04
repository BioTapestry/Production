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

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.util.TriStateJComboBox;

/****************************************************************************
**
** Holds consensus node properties for multiple node editing
*/

public class ConsensusNodeProps implements ConsensusProps {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static int NO_PAD_CHANGE = -1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  public String consensusColor;
  public boolean variousColor;      
  public FontManager.FontOverride consensusFont;
  public int consensusFontOverride;

  public int consensusNumOrient;
  public int consensusOrient;
  public int orientCoverage;

  public NodeInstance.ActivityState consensusActivity;

  public int consensusHideLabel;
  public int hideLabelCoverage;

  public int consensusEvidence;
  public int evidenceCoverage;

  public int consensusGrowth;
  public int growthCoverage;

  public SortedSet<Integer> consensusPadOptions;
  public int consensusDoExtraPads;
  public int consensusExtraPadCount;
  public int extraPadCoverage;

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
    
  public ConsensusNodeProps(Genome genome, Layout layout, Set<String> nodes) {
    consensusColor = null;
    variousColor = false;   
    consensusFont = null;
    consensusFontOverride = TriStateJComboBox.UNDEFINED_STATE;
    consensusActivity = null;

    consensusNumOrient = 0;
    consensusOrient = NodeProperties.UNDEFINED;    
    orientCoverage = UNDEFINED_OPTION_COVERAGE;

    consensusHideLabel = TriStateJComboBox.UNDEFINED_STATE;
    hideLabelCoverage = UNDEFINED_OPTION_COVERAGE;

    consensusEvidence = Gene.LEVEL_UNDEFINED;    
    evidenceCoverage = UNDEFINED_OPTION_COVERAGE;

    consensusGrowth = NodeProperties.UNDEFINED_GROWTH;    
    growthCoverage = UNDEFINED_OPTION_COVERAGE;


    consensusPadOptions = null;
    consensusDoExtraPads = TriStateJComboBox.UNDEFINED_STATE;
    consensusExtraPadCount = 0;
    extraPadCoverage = UNDEFINED_OPTION_COVERAGE;

    buildConsensusProps(genome, layout, nodes);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Build logic
  ** 
  */
  
  private void buildConsensusProps(Genome genome, Layout layout, Set<String> nodes) {
    boolean haveInstance = (genome instanceof GenomeInstance);

    Iterator<String> nit = nodes.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      NodeProperties np = layout.getNodeProperties(nodeID);
      Node node = genome.getNode(nodeID);
      int nodeType = node.getNodeType();

      if (!variousColor) {        
        String colorName = np.getColorName();
        if (consensusColor == null) {
          consensusColor = colorName;
        } else if (!consensusColor.equals(colorName)) {
          variousColor = true;
        }
      }

      //
      // Font overrides:
      //

      if (consensusFontOverride != TriStateJComboBox.MIXED_STATE) {        
        FontManager.FontOverride fontOver = np.getFontOverride();
        if (fontOver == null) { // not overridden
          if (consensusFontOverride == TriStateJComboBox.UNDEFINED_STATE) {
            consensusFontOverride = TriStateJComboBox.FALSE;
          } else if (consensusFontOverride == TriStateJComboBox.TRUE) {
            consensusFontOverride = TriStateJComboBox.MIXED_STATE;
          }
        } else { // overridden 
          if (consensusFontOverride == TriStateJComboBox.UNDEFINED_STATE) {
            consensusFontOverride = TriStateJComboBox.TRUE;
            consensusFont = fontOver;
          } else if (consensusFontOverride == TriStateJComboBox.TRUE) {
            if (consensusFont == null) {
              throw new IllegalStateException();
            }
            if (!consensusFont.equals(fontOver)) {
              consensusFontOverride = TriStateJComboBox.MIXED_STATE;
            }
          } else if (consensusFontOverride == TriStateJComboBox.FALSE) {  
            consensusFontOverride = TriStateJComboBox.MIXED_STATE;
          }
        }
      }

      //
      // Orientation.  If we have a mixture of types that allow different
      // option counts, we have to bag it:
      //

      int count = NodeProperties.getOrientTypeCount(nodeType);
      if (count > 1) {
        if (consensusNumOrient == 0) {
          consensusNumOrient = count;
        } else if (consensusNumOrient != count) { // Inconsistent number of options
          consensusNumOrient = -1;
          orientCoverage = NO_OPTION_COVERAGE;
        }    
        if (consensusNumOrient != -1) {
          if (orientCoverage == UNDEFINED_OPTION_COVERAGE) {
            orientCoverage = FULL_OPTION_COVERAGE;
          } else if (orientCoverage == NO_OPTION_COVERAGE) {
            orientCoverage = PARTIAL_OPTION_COVERAGE;
          }  
          int orient = np.getOrientation();
          if (consensusOrient == NodeProperties.UNDEFINED) {
            consensusOrient = orient;
          } else if (consensusOrient != orient) {
            consensusOrient = NodeProperties.NONE;
          }
        }
      } else {
        if (orientCoverage == UNDEFINED_OPTION_COVERAGE) {
          orientCoverage = NO_OPTION_COVERAGE;
        } else if (orientCoverage == FULL_OPTION_COVERAGE) {
          orientCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }

      //
      // Label hiding:
      //

      if (NodeProperties.canHideName(nodeType)) {
        if (hideLabelCoverage == UNDEFINED_OPTION_COVERAGE) {
          hideLabelCoverage = FULL_OPTION_COVERAGE;
        } else if (hideLabelCoverage == NO_OPTION_COVERAGE) {
          hideLabelCoverage = PARTIAL_OPTION_COVERAGE;
        } 
        boolean hidden = np.getHideName();
        if (consensusHideLabel == TriStateJComboBox.UNDEFINED_STATE) {
          consensusHideLabel = (hidden) ? TriStateJComboBox.TRUE : TriStateJComboBox.FALSE;
        } else if (consensusHideLabel != TriStateJComboBox.MIXED_STATE) {
          if (consensusHideLabel == TriStateJComboBox.TRUE) {
            if (!hidden) {
              consensusHideLabel = TriStateJComboBox.MIXED_STATE;
            }
          } else { // if (consensusHideLabel == TriStateJComboBox.FALSE)
            if (hidden) {
              consensusHideLabel = TriStateJComboBox.MIXED_STATE;
            }
          }
        }
      } else {  // can't hide name
        if (hideLabelCoverage == UNDEFINED_OPTION_COVERAGE) {
          hideLabelCoverage = NO_OPTION_COVERAGE;
        } else if (hideLabelCoverage == FULL_OPTION_COVERAGE) {
          hideLabelCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }

      //
      // Extra pad.  If we have a mixture of types that allow different
      // pad counts, we have to bag it:
      //

      int padInc = DBNode.getPadIncrement(nodeType);
      if (padInc != 0) {
        SortedSet<Integer> padC = NodeAndLinkPropertiesSupport.generatePadChoices(node);
        if (consensusPadOptions == null) {
          consensusPadOptions = padC;
        } else if (!consensusPadOptions.equals(padC)) { // Inconsistent pad options
          consensusPadOptions = new TreeSet<Integer>();
          extraPadCoverage = NO_OPTION_COVERAGE;
        }    
        if (!consensusPadOptions.isEmpty()) {
          if (extraPadCoverage == UNDEFINED_OPTION_COVERAGE) {
            extraPadCoverage = FULL_OPTION_COVERAGE;
          } else if (extraPadCoverage == NO_OPTION_COVERAGE) {
            extraPadCoverage = PARTIAL_OPTION_COVERAGE;
          }
          int pads = node.getPadCount();
          boolean amBig = (pads > DBNode.getDefaultPadCount(nodeType));

          if (consensusDoExtraPads == TriStateJComboBox.UNDEFINED_STATE) {
            if (amBig) {
              consensusDoExtraPads = TriStateJComboBox.TRUE;
              consensusExtraPadCount = pads;
            } else {
              consensusDoExtraPads = TriStateJComboBox.FALSE;
            }
          } else if (consensusDoExtraPads != TriStateJComboBox.MIXED_STATE) {
            if (consensusDoExtraPads == TriStateJComboBox.TRUE) {
              if (amBig) {
                if (consensusExtraPadCount != pads) {
                  consensusExtraPadCount = NO_PAD_CHANGE;
                }
              } else {
                consensusDoExtraPads = TriStateJComboBox.MIXED_STATE;
                consensusExtraPadCount = 0;
              }
            } else { // if (consensusDoExtraPads == TriStateJComboBox.FALSE)
              if (amBig) {
                consensusDoExtraPads = TriStateJComboBox.MIXED_STATE;
              }
            }
          }
        }
      } else {
        if (extraPadCoverage == UNDEFINED_OPTION_COVERAGE) {
          extraPadCoverage = NO_OPTION_COVERAGE;
        } else if (extraPadCoverage == FULL_OPTION_COVERAGE) {
          extraPadCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }

      //
      // Growth direction:
      //

      if (NodeProperties.usesGrowth(nodeType)) {
        if (growthCoverage == UNDEFINED_OPTION_COVERAGE) {
          growthCoverage = FULL_OPTION_COVERAGE;
        } else if (growthCoverage == NO_OPTION_COVERAGE) {
          growthCoverage = PARTIAL_OPTION_COVERAGE;
        } 
        int growthDir = np.getExtraGrowthDirection();
        if (consensusGrowth == NodeProperties.UNDEFINED_GROWTH) {
          consensusGrowth = growthDir;
        } else if (consensusGrowth != growthDir) {
          consensusGrowth = NodeProperties.NO_GROWTH;
        }
      } else {  // doesn't use growth
        if (growthCoverage == UNDEFINED_OPTION_COVERAGE) {
          growthCoverage = NO_OPTION_COVERAGE;
        } else if (growthCoverage == FULL_OPTION_COVERAGE) {
          growthCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }        

      //
      // Evidence:
      //

      if (nodeType == Node.GENE) {
        if (evidenceCoverage == UNDEFINED_OPTION_COVERAGE) {
          evidenceCoverage = FULL_OPTION_COVERAGE;
        } else if (evidenceCoverage == NO_OPTION_COVERAGE) {
          evidenceCoverage = PARTIAL_OPTION_COVERAGE;
        } 
        int evidence = ((Gene)node).getEvidenceLevel();
        if (consensusEvidence == Gene.LEVEL_UNDEFINED) {
          consensusEvidence = evidence;
        } else if (consensusEvidence != evidence) {
          consensusEvidence = Gene.LEVEL_VARIOUS;
        }
      } else {  // can't set evidence
        if (evidenceCoverage == UNDEFINED_OPTION_COVERAGE) {
          evidenceCoverage = NO_OPTION_COVERAGE;
        } else if (evidenceCoverage == FULL_OPTION_COVERAGE) {
          evidenceCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }

      if (haveInstance) {
        if ((consensusActivity == null) || (consensusActivity.activityState != NodeInstance.ACTIVITY_NOT_SET)) {
          NodeInstance ni = (NodeInstance)node;
          int activityState = ni.getActivity();
          double activityLevel = (activityState == NodeInstance.VARIABLE) ? ni.getActivityLevel() : 0.0;
          if (consensusActivity == null) {
            consensusActivity = new NodeInstance.ActivityState(activityState, activityLevel);
          } else {
            if ((consensusActivity.activityState != activityState) ||
                ((consensusActivity.activityState == NodeInstance.VARIABLE) && 
                 (consensusActivity.activityLevel.doubleValue() != activityLevel))) {
               consensusActivity = new NodeInstance.ActivityState(NodeInstance.ACTIVITY_NOT_SET, 0.0);
            }
          }
        }
      }
    }
    return;
  }   
}
