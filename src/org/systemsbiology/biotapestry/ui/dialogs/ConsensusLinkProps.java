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

import java.util.Iterator;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;

/****************************************************************************
**
** Holds consensus link properties for multiple link editing
*/

public class ConsensusLinkProps implements ConsensusProps {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
    
  public String consensusColor;
  public boolean variousColor;
  
  public int consensusThickness;
  public int thicknessCoverage;
  
  public int consensusStyle;
  public int styleCoverage;
  
  public int consensusExtent;
  public int extentCoverage;
    
  public GenomeItemInstance.ActivityState consensusActivity;

  public int consensusEvidence;
  public int evidenceCoverage;
  
  public int consensusSign;
  public int signCoverage;
  
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
    
  public ConsensusLinkProps(Genome genome, Layout layout, Set<String> links) {
    consensusColor = null;
    variousColor = false;   
 
    consensusThickness = SuggestedDrawStyle.UNDEFINED_THICKNESS;
    thicknessCoverage = UNDEFINED_OPTION_COVERAGE;
  
    consensusStyle = SuggestedDrawStyle.UNDEFINED_STYLE;
    styleCoverage = UNDEFINED_OPTION_COVERAGE;   
    
    consensusExtent = PerLinkDrawStyle.EXTENT_UNDEFINED;
    extentCoverage = UNDEFINED_OPTION_COVERAGE;   
       
    consensusEvidence = Linkage.LEVEL_UNDEFINED;    
    evidenceCoverage = UNDEFINED_OPTION_COVERAGE;
    
    consensusSign = Linkage.SIGN_UNDEFINED;    
    signCoverage = UNDEFINED_OPTION_COVERAGE;
       
    consensusActivity = null;
        
    buildConsensusProps(genome, layout, links);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
    
  /***************************************************************************
  **
  ** Generate a combined draw style for a link
  ** 
  */

  public static SuggestedDrawStyle completeDrawStyleForLink(SuggestedDrawStyle linkBase, PerLinkDrawStyle plds) {       
    SuggestedDrawStyle alone = plds.getDrawStyle();
    int styleCo = (alone.getLineStyle() == SuggestedDrawStyle.NO_STYLE) ? linkBase.getLineStyle() : alone.getLineStyle();
    String colorCo = (alone.getColorName() == null) ? linkBase.getColorName() : alone.getColorName();
    int thicknessCo = (alone.getThickness() == SuggestedDrawStyle.NO_THICKNESS_SPECIFIED) ? linkBase.getThickness() : alone.getThickness();  
    SuggestedDrawStyle combined = new SuggestedDrawStyle(styleCo, colorCo, thicknessCo);
    return (combined);
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
  
  private void buildConsensusProps(Genome genome, Layout layout, Set<String> links) {
    boolean haveInstance = (genome instanceof GenomeInstance);

    Iterator<String> nit = links.iterator();
    while (nit.hasNext()) {
      String linkID = nit.next();
      LinkProperties lp = layout.getLinkProperties(linkID);
      Linkage link = genome.getLinkage(linkID);

      PerLinkDrawStyle plds = lp.getDrawStyleForLinkage(linkID);
      SuggestedDrawStyle linkBase = lp.getDrawStyle();
      SuggestedDrawStyle sds = (plds != null) ? completeDrawStyleForLink(linkBase, plds) : linkBase;
 
      if (!variousColor) {        
        String colorName = sds.getColorName();
        if (consensusColor == null) {
          consensusColor = colorName;
        } else if (!consensusColor.equals(colorName)) {
          variousColor = true;
        }
      }
      
      if (styleCoverage == UNDEFINED_OPTION_COVERAGE) {
        styleCoverage = FULL_OPTION_COVERAGE;
      }
      int style = sds.getLineStyle();
      if (consensusStyle == SuggestedDrawStyle.UNDEFINED_STYLE) {
        consensusStyle = style;
      } else if (consensusStyle != style) {
        consensusStyle = SuggestedDrawStyle.VARIOUS_STYLE;
      }
      
      if (thicknessCoverage == UNDEFINED_OPTION_COVERAGE) {
        thicknessCoverage = FULL_OPTION_COVERAGE;
      }
      int thick = sds.getThickness();
      if (consensusThickness == SuggestedDrawStyle.UNDEFINED_THICKNESS) {
        consensusThickness = thick;
      } else if (consensusThickness != thick) {
        consensusThickness = SuggestedDrawStyle.VARIOUS_THICKNESS;
      }
      
      //
      // Extent only defined if we are dealing with per-link properties:
      //
   
      if (plds != null) {
        if (extentCoverage == UNDEFINED_OPTION_COVERAGE) {
          extentCoverage = FULL_OPTION_COVERAGE;
        } else if (extentCoverage == NO_OPTION_COVERAGE) {
          extentCoverage = PARTIAL_OPTION_COVERAGE;
        } 
        int extent = plds.getExtent();
        if (consensusExtent == PerLinkDrawStyle.EXTENT_UNDEFINED) {
          consensusExtent = extent;
        } else if (consensusExtent != extent) {
          consensusExtent = PerLinkDrawStyle.EXTENT_VARIOUS;
        }      
      } else {  // extent not applicable to full trees
        if (extentCoverage == UNDEFINED_OPTION_COVERAGE) {
          extentCoverage = NO_OPTION_COVERAGE;
        } else if (extentCoverage == FULL_OPTION_COVERAGE) {
          extentCoverage = PARTIAL_OPTION_COVERAGE;
        }
      }
   
      
      if (!(genome instanceof DynamicGenomeInstance)) {
        //
        // Evidence.  Every link can have assigned evidence, so there
        // is always full option coverage!
        //

        if (evidenceCoverage == UNDEFINED_OPTION_COVERAGE) {
          evidenceCoverage = FULL_OPTION_COVERAGE;
        }
        int evidence = link.getTargetLevel();
        if (consensusEvidence == Linkage.LEVEL_UNDEFINED) {
          consensusEvidence = evidence;   
        } else if (consensusEvidence != evidence) {
          consensusEvidence = Linkage.LEVEL_VARIOUS;
        }

        //
        // Sign.  Every link has a sign, so there
        // is always full option coverage!
        //

        if (signCoverage == UNDEFINED_OPTION_COVERAGE) {
          signCoverage = FULL_OPTION_COVERAGE;
        }
        int sign = link.getSign();
        if (consensusSign == Linkage.SIGN_UNDEFINED) {
          consensusSign = sign;   
        } else if (consensusSign != sign) {
          consensusSign = Linkage.SIGN_VARIOUS;
        }

        if (haveInstance) {
          if ((consensusActivity == null) || (consensusActivity.activityState != LinkageInstance.ACTIVITY_NOT_SET)) {
            LinkageInstance li = (LinkageInstance)link;
            int activityState = li.getActivitySetting();
            double activityLevel = (activityState == LinkageInstance.VARIABLE) ? li.getActivityLevel((GenomeInstance)genome) : 0.0;
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
      
    }
    return;
  } 
}
