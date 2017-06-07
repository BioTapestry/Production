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


package org.systemsbiology.biotapestry.embedded;

import java.util.List;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;

/****************************************************************************
**
** Contains external inventory info for a user of an embedded BioTapestry
** panel
*/

public abstract class ExternalInventoryItem {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  protected BuilderArgs bArgs_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the info
  */ 
  
  public ExternalInventoryItem(BuilderArgs bArgs) {
    bArgs_ = bArgs;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Get the internal item ID
  */ 
  
  public String getInternalID() {
    return (bArgs_.itemID);
  }
  
  /***************************************************************************
  **
  ** Get the internal model ID
  */ 
  
  public String getModelID() {
    return (bArgs_.genomeKey);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Holds args 
  */  
  
  public static class BuilderArgs {
    public String tabKey;
    public String genomeKey;
    public String itemID;
    public boolean isALink;
    public TabPinnedDynamicDataAccessContext tpdacx;
  
    public BuilderArgs(TabPinnedDynamicDataAccessContext tpdacx, String tabKey, String genomeKey, String itemID, boolean isALink) {
      this.tabKey = tabKey;
      this.tpdacx = tpdacx;
      this.genomeKey = genomeKey;
      this.itemID = itemID;
      this.isALink = isALink;
    }
  }
  
  /***************************************************************************
  **
  ** Generate args for external link:
  */ 
  
  public static class ArgsForExternalLink {
    public String tabName;
    public String[] modelNameChain;
    public String srcNodeName; 
    public String trgNodeName;
    public String srcRegionName;
    public String trgRegionName;
    public String linkDisplay;
    public String simpleLinkDisplay;

    public ArgsForExternalLink(BuilderArgs args) {

      //
      // External plug-ins need all sorts of names resolved:
      //
      GenomeSource gs = args.tpdacx.getGenomeSource();
      tabName = gs.getTabNameData().getTitle();
      Genome genome = gs.getGenome(args.genomeKey);
      List<String> names = genome.getNamesToRoot();
      modelNameChain = names.toArray(new String[names.size()]);
      Linkage link = genome.getLinkage(args.itemID);
      String srcID = link.getSource();
      String trgID = link.getTarget();
      Node srcNode = genome.getNode(srcID);
      Node trgNode = genome.getNode(trgID);
      srcNodeName = srcNode.getName();
      trgNodeName = trgNode.getName();
      srcRegionName = null;
      trgRegionName = null;
      if (genome instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)genome;
        Group srcGroup = gi.getGroupForNode(srcID, GenomeInstance.ALWAYS_MAIN_GROUP);
        Group trgGroup = gi.getGroupForNode(trgID, GenomeInstance.ALWAYS_MAIN_GROUP);
        srcRegionName = srcGroup.getInheritedDisplayName(gi);
        trgRegionName = trgGroup.getInheritedDisplayName(gi);    
      }
      linkDisplay = link.getDisplayString(genome, false); 
      simpleLinkDisplay = link.getDisplayString(genome, true); 
    }
  }
  
  /***************************************************************************
  **
  ** Generate args for external node:
  */ 
  
  public static class ArgsForExternalNode {
    public String tabName;
    public String[] modelNameChain;
    public String nodeDisplay;
    public String simpleNodeDisplay;
    public String regionName;
    public String nodeName;
    
    public ArgsForExternalNode(BuilderArgs args) {
      
      GenomeSource gs = args.tpdacx.getGenomeSource();
      tabName = gs.getTabNameData().getTitle();
      Genome genome = gs.getGenome(args.genomeKey);
      List<String> names = genome.getNamesToRoot();
      modelNameChain = names.toArray(new String[names.size()]);
      regionName = null;
      if (genome instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)genome;
        Group srcGroup = gi.getGroupForNode(args.itemID, GenomeInstance.ALWAYS_MAIN_GROUP);
        regionName = srcGroup.getInheritedDisplayName(gi);
      }
      Node node = genome.getNode(args.itemID);
      nodeDisplay = node.getDisplayString(genome, false);
      simpleNodeDisplay = node.getDisplayString(genome, true);
      nodeName = node.getName();
    }
  }  
}
