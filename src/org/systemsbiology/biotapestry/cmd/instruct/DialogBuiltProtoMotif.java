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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.OldPadMapper;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;

/***************************************************************************
**
** Gene/Link construct for dialog-built networks
*/
  
public abstract class DialogBuiltProtoMotif implements Cloneable {
  
  protected int sourceType_;
  protected String sourceName_;
  protected String targetName_;
  protected int targetType_;
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltProtoMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltProtoMotif(String sourceName, int sourceType,
                               String targetName, int targetType) {
    sourceName_ = sourceName;
    sourceType_ = sourceType;
    targetName_ = targetName;
    targetType_ = targetType;    
  }
  
  /***************************************************************************
  **
  ** Clone function
  */
  
  public DialogBuiltProtoMotif clone() {
    try {
      return ((DialogBuiltProtoMotif)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Get the source name
  */
  
  public String getSourceName() {
    return (sourceName_);
  }
  
  /***************************************************************************
  **
  ** Get the source type
  */
  
  public int getSourceType() {
    return (sourceType_);
  }   
  
  /***************************************************************************
  **
  ** Set the source Name
  */
  
  public void setSourceName(String sourceName) {
    sourceName_ = sourceName;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the source type
  */
  
  public void setSourceType(int sourceType) {
    sourceType_ = sourceType;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the target Name
  */
  
  public String getTargetName() {
    return (targetName_);
  }
  
  /***************************************************************************
  **
  ** Get the target type
  */
  
  public int getTargetType() {
    return (targetType_);
  }   
  
  /***************************************************************************
  **
  ** Set the target Name
  */
  
  public void setTargetName(String targetName) {
    targetName_ = targetName;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the target type
  */
  
  public void setTargetType(int targetType) {
    targetType_ = targetType;
    return;
  }
  
  /***************************************************************************
  **
  ** Build a map to make large network build practical
  */
  
  public void addToMap(Map<String, String> normNames, Map<String, Set<Integer>> nodeToMotifs, int index) {
    String normalizedSrc = normNames.get(sourceName_);
    if (normalizedSrc == null) {
      normalizedSrc = sourceName_.toUpperCase().replaceAll(" ", "");
      normNames.put(sourceName_, normalizedSrc);
    }
    Set<Integer> forSrc = nodeToMotifs.get(normalizedSrc);
    if (forSrc == null) {
      forSrc = new HashSet<Integer>();
      nodeToMotifs.put(normalizedSrc, forSrc);
    }
    forSrc.add(new Integer(index));
    
    if (targetName_ != null) {
      String normalizedTrg = normNames.get(targetName_);
      if (normalizedTrg == null) {
        normalizedTrg = targetName_.toUpperCase().replaceAll(" ", "");
        normNames.put(targetName_, normalizedTrg);
      }
      Set<Integer> forTrg = nodeToMotifs.get(normalizedTrg);
      if (forTrg == null) {
        forTrg = new HashSet<Integer>();
        nodeToMotifs.put(normalizedTrg, forTrg);
      }
      forTrg.add(new Integer(index));
    }
    return;
  }
 
  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltProtoMotif:" 
            + " sourceName = " + sourceName_
            + " sourceType = " + sourceType_ 
            + " targetName = " + targetName_
            + " targetType = " + targetType_);             
  }
  
  /***************************************************************************
  **
  ** Pattern match on the genome
  */
  
  public abstract void patternMatchMotif(DBGenome genome, Node oldNode, DialogBuiltMotifPair pair, 
                                         List<DialogBuiltMotifPair> fullList, OldPadMapper oldPads,
                                         InvertedSrcTrg ist);
 
  /***************************************************************************
  **
  ** Helper
  */  
  
  public static Node nodeMatchingNameAndType(DBGenome genome, String name, int type, InvertedSrcTrg ist) {
    if (type == Node.GENE) {
      return (genome.getGene(ist.getMatchingGene(name)));
    }
    // returns the first match, not the only match:
    Set<String> oldNodes = ist.getMatchingNodes(name);
    Iterator<String> onit = oldNodes.iterator();
    while (onit.hasNext()) {
      String nodeID = onit.next();
      Node nextNode = genome.getNode(nodeID);
      if (nextNode.getNodeType() == type) {
        return (nextNode);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Generate a real motif from a proto motif
  */
  
  public abstract void generateRealMotif(DialogBuiltMotifPair pair, DialogBuiltMotif oldMotif, BuildData bd, UndoSupport support);

  /***************************************************************************
  **
  ** Generate an empty real motif
  ** 
  */  
 
  public abstract DialogBuiltMotif getEmptyRealMotif();

  /***************************************************************************
  **
  ** Fill in the corresponding real motif from matches in the filled Pair
  */
  
  public abstract void fillNodesFromMatches(DBNode newNode, DialogBuiltMotifPair filledPair, 
                                            DialogBuiltMotifPair oldPair, Map<String, String> newNodeToOldNode,
                                            DialogBuiltMotif realMotif, Map<String, String> normNames);
  
  /***************************************************************************
  **
  ** Answer if we need to call fillLinksFromMatches. Override if we need to do this.
  */
  
  public boolean needsFillLinksFromMatches() {
    return (false);
  }

  /***************************************************************************
  **
  ** Fill in the corresponding real motif from matches in the filled Pair. Override if
  ** we need to do this.
  */
  
  public void fillLinksFromMatches(DBLinkage newLink, DialogBuiltMotifPair filledPair, DialogBuiltMotif realMotif) {
    return;
  }

  /***************************************************************************
  **
  ** Node generation and bookkeeping helper
  */
  
  protected DBNode genNode(BTState appState, DataAccessContext dacx, DBGenome genome, DBGenome oldGenome, String name, 
                           int type, String oldID, UndoSupport support) {
    String nodeID = (oldID == null) ? genome.getNextKey() : oldID;
    Node oldNode = null;
    if (oldID != null) {
      oldNode = oldGenome.getNode(oldID);
    }
    GenomeChange gc;
    DBNode newNode;
    if (type == Node.GENE) {
      DBGene newGene = (oldNode == null) ? new DBGene(appState, name, nodeID) : new DBGene((DBGene)oldNode);
      gc = genome.addGeneWithExistingLabel(newGene);
      newNode = newGene;
    } else {
      newNode = (oldNode == null) ? new DBNode(appState, type, name, nodeID) : new DBNode((DBNode)oldNode);
      gc = genome.addNodeWithExistingLabel(newNode);
    }
    if (gc == null) {
      throw new IllegalStateException();
    }
    support.addEdit(new GenomeChangeCmd(appState, dacx, gc));
    return (newNode);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Data for Building
  */ 
    
  public static class BuildData {
    BTState appState; 
    DBGenome genome;
    DBGenome oldGenome;
    Map<String, String> normNames;
    Map<String, Set<Integer>> nodeToMotifs;
    Set<Integer> needFillLinks;
    List<DialogBuiltMotifPair> fullList;
    List<DialogBuiltMotifPair> existingList; 
    Map<String, String> newNodeToOldNode;
    Map<String, String> newLinksToOldLinks; 
    Map<String, Integer> newTypesByID;
    OldPadMapper opm;
    Map<String, PadConstraints> padConstraintSaver; 
    InvertedSrcTrg ist;
    boolean existingOnly;
    DataAccessContext dacx;
    
  
    public BuildData(BTState appState, DataAccessContext dacx, DBGenome genome, DBGenome oldGenome,                                     
                     List<DialogBuiltMotifPair> fullList, List<DialogBuiltMotifPair> existingList, 
                     Map<String, String> newNodeToOldNode, Map<String, String> newLinksToOldLinks, 
                     Map<String, Integer> newTypesByID,
                     OldPadMapper opm, Map<String, PadConstraints> padConstraintSaver, 
                     InvertedSrcTrg ist, boolean existingOnly) {
      this.appState = appState;
      this.genome = genome;
      this.oldGenome = oldGenome;
      this.fullList = fullList;
      this.existingList =  existingList;
      this.newNodeToOldNode = newNodeToOldNode;
      this.newLinksToOldLinks =  newLinksToOldLinks;
      this.newTypesByID = newTypesByID;
      this.opm = opm;
      this.padConstraintSaver =  padConstraintSaver;
      this.ist = ist;
      this.existingOnly = existingOnly;
      this.dacx = dacx;

      //
      // Used to be we had M^2 efficiency in creation. Now create a map to chop that down.
      // Also ditch lots of useless one-time String creation
      //
      
      int size = fullList.size();
      nodeToMotifs = new HashMap<String, Set<Integer>>();
      normNames = new HashMap<String, String>();
      needFillLinks = new HashSet<Integer>();
      for (int i = 0; i < size; i++) {                  
        DialogBuiltMotifPair createdPair = fullList.get(i);
        createdPair.proto.addToMap(normNames, nodeToMotifs, i);
        if (createdPair.proto.needsFillLinksFromMatches()) {
          needFillLinks.add(new Integer(i));
        }
      }
    }
    
    public void setExistingOnly(boolean val) {
      existingOnly = val;
      return;
    }  
  } 

}
