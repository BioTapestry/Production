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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.OldPadMapper;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.PadConstraints;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.InvertedSrcTrg;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.util.UndoSupport;

/***************************************************************************
**
** Gene/Signal/Bubble construct for dialog-built networks
*/
  
public class DialogBuiltGeneralProtoMotif extends DialogBuiltProtoMotif {
  
  protected int linkSign_;
  protected int evidenceLevel_;
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltGeneralProtoMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltGeneralProtoMotif(String sourceName, int sourceType,
                                      String targetName, int targetType, 
                                      int linkSign, int evidenceLevel) {
    super(sourceName, sourceType, targetName, targetType);                               
    linkSign_ = linkSign;
    evidenceLevel_ = evidenceLevel;
  }
  
  /***************************************************************************
  **
  ** Clone function
  */
  
  public DialogBuiltGeneralProtoMotif clone() {
    DialogBuiltGeneralProtoMotif retval = (DialogBuiltGeneralProtoMotif)super.clone();
   // retval.linkSign_ = this.linkSign_;  don't need this!  Super.clone is binary copy...
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the link sign
  */
  
  public int getLinkSign() {
    return (linkSign_);
  }   
  
  /***************************************************************************
  **
  ** Set the link sign
  */
  
  public void setLinkSign(int linkSign) {
    linkSign_ = linkSign;
    return;
  }
  
  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltGeneralProtoMotif: (" + super.toString() + ")" 
    + " linkSign_ = " + linkSign_ + " evidenceLevel_ = " + evidenceLevel_);
  }
  
  /***************************************************************************
  **
  ** Pattern match on the genome
  */
  
  public void patternMatchMotif(DBGenome genome, Node oldNode, 
                                DialogBuiltMotifPair pair, List<DialogBuiltMotifPair> fullList, 
                                OldPadMapper oldPads, InvertedSrcTrg ist) {

    if (pair.proto != this) {
      throw new IllegalArgumentException();
    }
    
    //
    // Always add a blank real motif
    //
    
    DialogBuiltGeneralMotif real = new DialogBuiltGeneralMotif();
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
    Node targNode = nodeMatchingNameAndType(genome, targetName_, targetType_, ist);   
    if (targNode != null) {
      real.setTargetId(targNode.getID());
    } else {
      return;
    }
    
    //
    // If we have source and target, find an existing link that matches.  However,
    // we do NOT match on an existing link that is already present in some other
    // real motif in the full list:
    //
    
    Set<String> existing = matchingLinkSet(pair, fullList);
    
    Set<String> lb = ist.getLinksBetween(real.getSourceId(), real.getTargetId());
    
    Iterator<String> lit = lb.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      Linkage link = genome.getLinkage(linkID);
      
   // Iterator<Linkage> lit = genome.getLinkageIterator();
  //  while (lit.hasNext()) {
    //  Linkage link = lit.next();
      if (link.getSource().equals(real.getSourceId()) &&
          link.getTarget().equals(real.getTargetId()) &&
          (GeneralBuildInstruction.mapFromLinkageSign(link.getSign()) == linkSign_) &&
          !existing.contains(link.getID())) {
        real.setLinkId(link.getID());
        if (oldPads != null) {
          oldPads.setPadPair(link.getSource(), link.getTarget(), link.getID(), 
                             link.getLaunchPad(), link.getLandingPad());
        }
        return;
      }
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Generate an empty real motif
  */
  
  public DialogBuiltMotif getEmptyRealMotif() {
    return (new DialogBuiltGeneralMotif());
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
    DialogBuiltGeneralMotif real = (DialogBuiltGeneralMotif)pair.real;
    DialogBuiltGeneralMotif oldGen = (DialogBuiltGeneralMotif)oldMotif;  
    String oldID = oldGen.getSourceId();    
    if ((oldID != null) || !bd.existingOnly) {
      if (real.getSourceId() == null) {
        DBNode newNode = genNode(bd.dacx, bd.genome, bd.oldGenome, sourceName_, sourceType_, oldID, support);
        String newID = newNode.getID();
        real.setSourceId(newID);
        String normSrc =bd. normNames.get(sourceName_);
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

    oldID = oldGen.getTargetId();    
    if ((oldID != null) || !bd.existingOnly) {    
      if ((real.getTargetId() == null) && (targetName_ != null)) {  
        DBNode newNode = genNode(bd.dacx, bd.genome, bd.oldGenome, targetName_, targetType_, oldID, support);
        String newID = newNode.getID();
        real.setTargetId(newID);
        String normTrg = bd.normNames.get(targetName_);
        Set<Integer> forTrg = bd.nodeToMotifs.get(normTrg);
        if (forTrg != null) {
          Iterator<Integer> ftit = forTrg.iterator();
          while (ftit.hasNext()) {
            int i = ftit.next().intValue();
            DialogBuiltMotifPair nextPair = bd.fullList.get(i);
            DialogBuiltMotifPair oldPair = bd.existingList.get(i);
            nextPair.proto.fillNodesFromMatches(newNode, pair, oldPair, bd.newNodeToOldNode, nextPair.real, bd.normNames);
          }
        }
        if (bd.newNodeToOldNode.get(newID) == null) {
          bd.newTypesByID.put(newID, new Integer(targetType_));
        }
      }
    }

    oldID = oldGen.getLinkId();    
    if ((oldID != null) || !bd.existingOnly) {       
      PadCalculatorToo padCalc = new PadCalculatorToo();
      // Note that OldPadMapper being used to force match to old pads appears no longer
      // needed due to reclaiming of link settings from old link. FIX ME? 
      PadConstraints pc = padCalc.generatePadConstraints(real.getSourceId(), real.getTargetId(), bd.opm,
                                                         oldGen.getLinkId(), bd.newNodeToOldNode);
      if ((real.getLinkId() == null) && (targetName_ != null)) { 
        DBLinkage newLink = 
          (DBLinkage)AddCommands.autoAddOldOrNewLinkToRoot(bd.dacx, real.getSourceId(), real.getTargetId(), 
                                                           GeneralBuildInstruction.mapToLinkageSign(linkSign_),
                                                           bd.oldGenome, support, true, oldID, pc, evidenceLevel_, bd.ist);
        real.setLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getLinkId(), oldGen.getLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
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
    // We are a general motif.  Both endpoints must have a name, so if we get a nameless node, we
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
      
    if (normNames.get(targetName_).equals(normName)) {
      if (realMotif.getTargetId() == null) {
        if (newNode.getNodeType() != targetType_) {
          throw new IllegalStateException();
        } 
        realMotif.setTargetId(newNode.getID());
      }
      if (oldPair.real.getTargetId()!= null) {
        newNodeToOldNode.put(newNode.getID(), oldPair.real.getTargetId());
      }      
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Helper
  */  
  
  private Set<String> matchingLinkSet(DialogBuiltMotifPair pair, List<DialogBuiltMotifPair> fullList) {
    DialogBuiltGeneralMotif myMotif = (DialogBuiltGeneralMotif)pair.real;
    if ((myMotif.getSourceId() == null) || 
        (myMotif.getTargetId() == null) ||
        (myMotif.getLinkId() != null)) {
      throw new IllegalArgumentException();
    }
    Set<String> retval = new HashSet<String>();
    int size = fullList.size();
    for (int i = 0 ; i < size; i++) {
      DialogBuiltMotifPair nextPair = fullList.get(i);
      if ((nextPair.real == null) || !(nextPair.real instanceof DialogBuiltGeneralMotif)) {
        continue;
      }
      DialogBuiltGeneralMotif nextMotif = (DialogBuiltGeneralMotif)nextPair.real;      
      if ((nextMotif.getSourceId() == null) || (nextMotif.getTargetId() == null)) {
        continue;
      }
      if ((!myMotif.getSourceId().equals(nextMotif.getSourceId())) ||
          (!myMotif.getTargetId().equals(nextMotif.getTargetId()))) {
        continue;
      }
      if (nextMotif.getLinkId() == null) {
        continue;
      }
      retval.add(nextMotif.getLinkId());
    }
    return (retval);
  }
}
