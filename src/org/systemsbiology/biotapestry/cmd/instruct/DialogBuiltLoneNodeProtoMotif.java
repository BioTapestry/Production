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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.OldPadMapper;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.UndoSupport;

/***************************************************************************
**
** Gene/Signal/Bubble construct for dialog-built networks
*/
  
public class DialogBuiltLoneNodeProtoMotif extends DialogBuiltProtoMotif {
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltLoneNodeProtoMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltLoneNodeProtoMotif(String sourceName, int sourceType) {
    super(sourceName, sourceType, null, Node.NO_NODE_TYPE);                               
  }
  
  /***************************************************************************
  **
  ** Clone function
  */
  
  public DialogBuiltLoneNodeProtoMotif clone() {
    DialogBuiltLoneNodeProtoMotif retval = (DialogBuiltLoneNodeProtoMotif)super.clone();
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltLoneProtoMotif: (" + super.toString() + ")");
  }
  
  /***************************************************************************
  **
  ** Pattern match on the genome
  */
  
  public void patternMatchMotif(DBGenome genome, Node oldNode, 
                                DialogBuiltMotifPair pair, 
                                List<DialogBuiltMotifPair> fullList, OldPadMapper oldPads, InvertedSrcTrg ist) {

    if (pair.proto != this) {
      throw new IllegalArgumentException();
    }
    
    //
    // Always add a blank real motif
    //
    
    DialogBuiltLoneNodeMotif real = new DialogBuiltLoneNodeMotif();
    pair.real = real;
    
    if (oldNode == null) {
      return;
    } 
    
    //
    // Build the real motif from existing components in the genome and add it to
    // the pair.
    //
                      
    if (oldNode.getNodeType() != sourceType_) {
      throw new IllegalArgumentException();
    }
    real.setSourceId(oldNode.getID());
    return;
  }
  
  /***************************************************************************
  **
  ** Generate an empty real motif
  */
  
  public DialogBuiltMotif getEmptyRealMotif() {
    return (new DialogBuiltLoneNodeMotif());
  }
  
  /***************************************************************************
  **
  ** Generate a real motif from a proto motif
  */
  
   public void generateRealMotif(DialogBuiltMotifPair pair, DialogBuiltMotif oldMotif, BuildData bd, UndoSupport support) {
  
    //
    // Look in the real motif for corresponding empty slots.  Build the nodes and
    // links needed to fill them in.  Then run down the list and fill in corresponding slots.
    //
    DialogBuiltLoneNodeMotif real = (DialogBuiltLoneNodeMotif)pair.real;
    DialogBuiltLoneNodeMotif oldLone = (DialogBuiltLoneNodeMotif)oldMotif;

    String oldID = oldLone.getSourceId();    
    if ((oldID != null) || !bd.existingOnly) {
      if (real.getSourceId() == null) {
        DBNode newNode = genNode(bd.dacx, bd.genome, bd.oldGenome, sourceName_, sourceType_, oldID, support);
        String newID = newNode.getID();
        real.setSourceId(newID);
        String normSrc = bd.normNames.get(sourceName_);
        Set<Integer> forSrc = bd.nodeToMotifs.get(normSrc);
        if (forSrc != null) {
          Iterator<Integer> fsit = forSrc.iterator();
          while (fsit.hasNext()) {
            int i = fsit.next().intValue();
            DialogBuiltMotifPair nextPair = bd.fullList.get(i);
            DialogBuiltMotifPair oldPair = bd.existingList.get(i);
            nextPair.proto.fillNodesFromMatches(newNode, pair, oldPair, bd.newNodeToOldNode, nextPair.real, bd.normNames);
          }
        }
        if (bd.newNodeToOldNode.get(newID) == null) {
          bd.newTypesByID.put(newID, new Integer(sourceType_));
        }
      }
    }
    
    return; 
  }
  
  /***************************************************************************
  **
  ** Fill in the corresponding real motif from matches in the filled Pair
  */
  
  public void fillNodesFromMatches(DBNode newNode, DialogBuiltMotifPair filledPair, 
                                   DialogBuiltMotifPair oldPair, Map<String, String> newNodeToOldNode, 
                                   DialogBuiltMotif realMotif, Map<String, String> normNames) {
    //
    // We have a newly created node.  Check in our corresponding real motif to see if it can be
    // filled in by that node.
    //
    // We are a lone node motif.  Our one node must have a name, so if we get a nameless node, we
    // can skip it.
    //
    
    String normName = normNames.get(newNode.getName());
    if (normName.equals("")) {
      return;
    }
    
    //
    // The node has a name.  Check our source and destination nodes looking for a match.
    //
    
    if (normNames.get(sourceName_).equals(normName)) {
      if (realMotif.getSourceId() == null) {
        if (newNode.getNodeType() != sourceType_) {
          throw new IllegalStateException();
        } 
        realMotif.setSourceId(newNode.getID());
      }
      if (oldPair.real.getSourceId() != null) {
        newNodeToOldNode.put(newNode.getID(), oldPair.real.getSourceId());
      }
    }
 
    return;
  }
}
