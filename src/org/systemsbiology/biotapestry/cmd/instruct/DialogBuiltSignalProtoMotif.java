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

import java.util.ArrayList;
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
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/***************************************************************************
**
** Gene/Signal/Bubble construct for dialog-built networks
*/
  
public class DialogBuiltSignalProtoMotif extends DialogBuiltProtoMotif {

  private final static int POSITIVE_ = 0; 
  private final static int NEGATIVE_ = 1;   
  
  protected String transFacName_;
  protected int transFacType_;
  protected int signalMode_; 
  protected int evidenceLevel_;
 
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public DialogBuiltSignalProtoMotif() {
  }  
   
  /***************************************************************************
  **
  ** Constructor
  **
  */
  
  public DialogBuiltSignalProtoMotif(String sourceName, int sourceType,
                                     String targetName, int targetType, 
                                     String transFacName, int transFacType,
                                     int signalMode) {
    super(sourceName, sourceType, targetName, targetType);
    transFacName_ = transFacName;
    transFacType_ = transFacType;
    signalMode_ = signalMode;
    evidenceLevel_ = Linkage.LEVEL_NONE;
  }
  
  /***************************************************************************
  **
  ** Clone function
  */
  
  public DialogBuiltSignalProtoMotif clone() {
    DialogBuiltSignalProtoMotif retval = (DialogBuiltSignalProtoMotif)super.clone();
    //retval.transFacName_ = this.transFacName_;  Handled by binary super.clone() copy!
    //retval.transFacType_ = this.transFacType_;
    //retval.signalMode_ = this.signalMode_;    
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Set the transfac Name
  */
  
  public void setTransFacName(String transFacName) {
    transFacName_ = transFacName;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the transfac type
  */
  
  public void setTransFacType(int transFacType) {
    transFacType_ = transFacType;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the signal mode
  */
  
  public int getSignalMode() {
    return (signalMode_);
  }   
  
  /***************************************************************************
  **
  ** Set the signal mode
  */
  
  public void setSignalMode(int signalMode) {
    signalMode_ = signalMode;
    return;
  }
  
  /***************************************************************************
  **
  ** toString override
  */
  
  public String toString() {
    return ("DialogBuiltSignalProtoMotif: (" + super.toString() + ")"  
      + " transFacName_ = " + transFacName_
      + " transFacType_ = " + transFacType_
      + " signalMode_ = " + signalMode_);    
  }
  
  /***************************************************************************
  **
  ** Pattern match on the genome
  */
  
  public void patternMatchMotif(DBGenome genome, Node oldNode, DialogBuiltMotifPair pair, 
                                List<DialogBuiltMotifPair> fullList, OldPadMapper oldPads,
                                InvertedSrcTrg ist) {

    if (pair.proto != this) {
      throw new IllegalArgumentException();
    }

    //
    // Always add a blank real motif
    //
    
    DialogBuiltSignalMotif real = new DialogBuiltSignalMotif();
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
    }
    Node trFacNode = nodeMatchingNameAndType(genome, transFacName_, transFacType_, ist);
    if (trFacNode != null) {
      real.setTransFacId(trFacNode.getID());
    }
    
    //
    // Since all links inside the signal motif go between at least one anonymous node,
    // we don't have to worry about matching general motifs.  If we have already dug
    // up a link in another real motif, reuse it unless it is one of the final links to the
    // target.  (This allows us to reuse common signal path infrastructure between several
    // signal path targets.) In that case, we cannot reuse it.  Two signal motifs that match modulo
    // the link mode (in proto namespace) may have separate links to the target item.
    //

    //
    // Try to match signal bubble. If signal did not match, we quit (too conservative: FIX ME)
    //

    Linkage toSigLink = linkExists(genome, real.getSourceId(), null, Node.MIN_NODE_TYPE - 1,
                                           null, "", Node.INTERCELL);
    
    if (toSigLink == null) {
      return;
    }
    real.setSignalId(toSigLink.getTarget());
    if (toSigLink.getSign() == Linkage.NONE) {
      real.setSignalLinkId(toSigLink.getID());
      if (oldPads != null) {
        oldPads.setPadPair(toSigLink.getSource(), toSigLink.getTarget(), toSigLink.getID(), 
                           toSigLink.getLaunchPad(), toSigLink.getLandingPad());
      }
    }
      
    //
    // Now go after the bubble.  Note that since this is all short-circuited by first matches
    // it will miss non-standard cases. FIX ME!
    //

    Linkage sigBubLink = linkExists(genome, real.getSignalId(), null, Node.MIN_NODE_TYPE - 1,
                                           null, "", Node.BUBBLE);
    if (sigBubLink == null) {
      return;
    }
    String sigBubID = sigBubLink.getTarget();
    
    Linkage trfBubLink = linkExists(genome, real.getTransFacId(), null, Node.MIN_NODE_TYPE - 1,
                                            sigBubID, null, Node.MIN_NODE_TYPE - 1);
    if (trfBubLink == null) {
      return;
    }
    
    real.setBubbleId(sigBubID);
    if (sigBubLink.getSign() == Linkage.POSITIVE) {
      real.setBubbleLinkId(sigBubLink.getID());
      if (oldPads != null) {
        oldPads.setPadPair(sigBubLink.getSource(), sigBubLink.getTarget(), sigBubLink.getID(), 
                           sigBubLink.getLaunchPad(), sigBubLink.getLandingPad());      
      }
    }
    if (trfBubLink.getSign() == Linkage.POSITIVE) {
      real.setTransFacLinkId(trfBubLink.getID());
      if (oldPads != null) {
        oldPads.setPadPair(trfBubLink.getSource(), trfBubLink.getTarget(), trfBubLink.getID(), 
                           trfBubLink.getLaunchPad(), trfBubLink.getLandingPad());      
      }
    }
    
    //
    // Finally, match the links to the target, but only if no other motifs have matched
    // the link.
    //
    
    List<Set<String>> existing = matchingLinkSet(genome, pair, fullList);
    real.setSignalMode(signalMode_);
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if (link.getSource().equals(real.getBubbleId()) &&
          link.getTarget().equals(real.getTargetId())) {
        int linkSign = link.getSign();
        if (linkSign == Linkage.NEGATIVE) {
          if ((signalMode_ == SignalBuildInstruction.REPRESS_SIGNAL) ||
              (signalMode_ == SignalBuildInstruction.SWITCH_SIGNAL)) {
            if (!existing.get(NEGATIVE_).contains(link.getID())) {
              real.setTargetNegLinkId(link.getID());
              if (oldPads != null) {
                oldPads.setPadPair(link.getSource(), link.getTarget(), link.getID(), 
                                   link.getLaunchPad(), link.getLandingPad());
              }
            }
          }
        } else if (linkSign == Linkage.POSITIVE) {
          if ((signalMode_ == SignalBuildInstruction.PROMOTE_SIGNAL) ||
              (signalMode_ == SignalBuildInstruction.SWITCH_SIGNAL)) {
            if (!existing.get(POSITIVE_).contains(link.getID())) {
              real.setTargetPosLinkId(link.getID());
              if (oldPads != null) {
                oldPads.setPadPair(link.getSource(), link.getTarget(), link.getID(), 
                                   link.getLaunchPad(), link.getLandingPad());
              }
            }
          }
        }
      }
      if (satisfied(real)) {
        break;
      }
    }
    
    return; 
  }

  /***************************************************************************
  **
  ** Helper
  */  
  
  private boolean satisfied(DialogBuiltSignalMotif real) {
    int mode = real.getSignalMode();
    if ((mode == SignalBuildInstruction.REPRESS_SIGNAL) && 
        (real.getTargetNegLinkId() != null)) {
      return (true);
    }
    if ((mode == SignalBuildInstruction.PROMOTE_SIGNAL) && 
        (real.getTargetPosLinkId() != null)) {
      return (true);
    }
    if ((mode == SignalBuildInstruction.SWITCH_SIGNAL) && 
        (real.getTargetNegLinkId() != null) &&    
        (real.getTargetPosLinkId() != null)) {
      return (true);
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Helper
  */  
  
  private Linkage linkExists(DBGenome genome, String srcID, String srcName, int srcType,
                                              String trgID, String trgName, int trgType) {
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      //
      // Match source either on node ID or on name/type
      //
      if (srcID != null) {
        if (!srcID.equals(src)) {
          continue;
        }
      } else {
        Node srcNode = genome.getNode(src);
        if ((srcNode.getNodeType() != srcType) || 
            (!DataUtil.keysEqual(srcName, srcNode.getName()))) {
          continue;
        }
      }
      //
      // Do the same for target
      //
      if (trgID != null) {
        if (!trgID.equals(trg)) {
          continue;
        }
      } else {
        Node trgNode = genome.getNode(trg);
        if ((trgNode.getNodeType() != trgType) || 
            (!DataUtil.keysEqual(trgName, trgNode.getName()))) {
          continue;
        }
      }
      //
      // If we get here, we have a link.  Send it back:
      //
      return (link);
    }
    return (null);
  } 
     
  /***************************************************************************
  **
  ** Helper
  */  
  
  private List<Set<String>> matchingLinkSet(DBGenome genome, DialogBuiltMotifPair pair, List<DialogBuiltMotifPair> fullList) {
    DialogBuiltSignalMotif myMotif = (DialogBuiltSignalMotif)pair.real;
    Set<String> retvalPos = new HashSet<String>();
    Set<String> retvalNeg = new HashSet<String>();
     
    int size = fullList.size();
    for (int i = 0 ; i < size; i++) {
      DialogBuiltMotifPair nextPair = fullList.get(i);
      if ((nextPair.real == null) || !(nextPair.real instanceof DialogBuiltSignalMotif)) {
        continue;
      }
      DialogBuiltSignalMotif nextMotif = (DialogBuiltSignalMotif)nextPair.real;
      String bubbleID = nextMotif.getBubbleId();
      String targetID = nextMotif.getTargetId();
      if ((bubbleID == null) || (targetID == null)) {
        continue;
      }
      
      //
      // Bug BT-02-20-07:1 Fixed by making sure we have have a bubble and a target:
      //
      
      String myBubID = myMotif.getBubbleId();
      String myTargID = myMotif.getTargetId();
      if ((myBubID == null) || (myTargID == null)) {
        continue;
      } 
     
      if ((!myBubID.equals(bubbleID)) || (!myTargID.equals(targetID))) {
        continue;
      }
      if (nextMotif.getTargetNegLinkId() != null) {     
        retvalNeg.add(nextMotif.getTargetNegLinkId());
      }
      if (nextMotif.getTargetPosLinkId() != null) {
        retvalPos.add(nextMotif.getTargetPosLinkId());
      }      
    }
    List<Set<String>> retval = new ArrayList<Set<String>>();
    retval.set(POSITIVE_, retvalPos);
    retval.set(NEGATIVE_, retvalNeg);
    return (retval);
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
                                     
    DialogBuiltSignalMotif realSignalMotif = (DialogBuiltSignalMotif)realMotif;
    DialogBuiltSignalMotif oldSignalMotif = (DialogBuiltSignalMotif)oldPair.real;
    
    String normName = normNames.get(newNode.getName());
    
    //
    // Check our source, destination, and transfac nodes looking for a match.
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
      if (oldPair.real.getTargetId() != null) {
        newNodeToOldNode.put(newNode.getID(), oldPair.real.getTargetId());
      }
    }
    if (normNames.get(transFacName_).equals(normName)) {      
      if (realSignalMotif.getTransFacId() == null) { 
        if (newNode.getNodeType() != transFacType_) {
          throw new IllegalStateException();
        }
        realSignalMotif.setTransFacId(newNode.getID());
      }
      if (oldSignalMotif.getTransFacId() != null) {
        newNodeToOldNode.put(newNode.getID(), oldSignalMotif.getTransFacId());
      }
    }
    
    //
    // Only remaining matches may occur if we are dealing with a signal:
    //
    if (!(filledPair.proto instanceof DialogBuiltSignalProtoMotif)) {
      return;
    }
    
    DialogBuiltSignalProtoMotif protoSignalMotif = (DialogBuiltSignalProtoMotif)filledPair.proto;          

    
    //
    // If no name, we may have a bubble or signal, depending on context:
    //
    
    if (normName.equals("")) {
      if ((newNode.getNodeType() == Node.INTERCELL) && 
          normNames.get(protoSignalMotif.sourceName_).equals(normNames.get(sourceName_))) {
        realSignalMotif.setSignalId(newNode.getID());
        if (oldSignalMotif.getSignalId() != null) {
          newNodeToOldNode.put(newNode.getID(), oldSignalMotif.getSignalId());
        }
      } else if ((newNode.getNodeType() == Node.BUBBLE) &&
                  normNames.get(protoSignalMotif.sourceName_).equals(normNames.get(sourceName_)) &&
                  normNames.get(protoSignalMotif.transFacName_).equals(normNames.get(transFacName_))) {
        realSignalMotif.setBubbleId(newNode.getID());
        if (oldSignalMotif.getBubbleId() != null) {
          newNodeToOldNode.put(newNode.getID(), oldSignalMotif.getBubbleId());
        }
      }
    }
      
    return;                                     
  }
  
  /***************************************************************************
  **
  ** Build a map to make large network build practical
  */
  
  @Override
  public void addToMap(Map<String, String> normNames, Map<String, Set<Integer>> nodeToMotifs, int index) {
    super.addToMap(normNames, nodeToMotifs, index);
    String normalizedTF = normNames.get(transFacName_);
    if (normalizedTF == null) {
      normalizedTF = transFacName_.toUpperCase().replaceAll(" ", "");
      normNames.put(transFacName_, normalizedTF);
    }
    Set<Integer> forTF = nodeToMotifs.get(normalizedTF);
    if (forTF == null) {
      forTF = new HashSet<Integer>();
      nodeToMotifs.put(normalizedTF, forTF);
    }
    forTF.add(new Integer(index));
    return;
  }
  
  /***************************************************************************
  **
  ** Generate an empty real motif
  */
  
  public DialogBuiltMotif getEmptyRealMotif() {
    return (new DialogBuiltSignalMotif());
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
    DialogBuiltSignalMotif real = (DialogBuiltSignalMotif)pair.real;
    DialogBuiltSignalMotif oldSig = (DialogBuiltSignalMotif)oldMotif;
    PadCalculatorToo padCalc = new PadCalculatorToo();
    
    //
    // Nodes first
    //
    
    String oldID = oldSig.getSourceId(); 
    if ((oldID != null) || !bd.existingOnly) {
      if (real.getSourceId() == null) {
        DBNode newNode = genNode(bd.appState, bd.dacx, bd.genome, bd.oldGenome, sourceName_, sourceType_, oldID, support);
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
    
    oldID = oldSig.getTargetId();    
    if ((oldID != null) || !bd.existingOnly) {       
      if (real.getTargetId() == null) {
        DBNode newNode = genNode(bd.appState, bd.dacx, bd.genome, bd.oldGenome, targetName_, targetType_, oldID, support);
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

    oldID = oldSig.getTransFacId();    
    if ((oldID != null) || !bd.existingOnly) {          
      if (real.getTransFacId() == null) {
        DBNode newNode = genNode(bd.appState, bd.dacx, bd.genome, bd.oldGenome, transFacName_, transFacType_, oldID, support);
        String newID = newNode.getID();
        real.setTransFacId(newID);
        String normTF = bd.normNames.get(transFacName_);
        Set<Integer> forTF = bd.nodeToMotifs.get(normTF);
        if (forTF != null) {
          Iterator<Integer> ftfit = forTF.iterator();
          while (ftfit.hasNext()) {
            int i = ftfit.next().intValue();
            DialogBuiltMotifPair nextPair = bd.fullList.get(i);
            DialogBuiltMotifPair oldPair = bd.existingList.get(i);
            nextPair.proto.fillNodesFromMatches(newNode, pair, oldPair, bd.newNodeToOldNode, nextPair.real, bd.normNames);
          }
        }
        if (bd.newNodeToOldNode.get(newID) == null) {
          bd.newTypesByID.put(newID, new Integer(transFacType_));
        }
      }
    }
    
    oldID = oldSig.getSignalId();    
    if ((oldID != null) || !bd.existingOnly) {         
      if (real.getSignalId() == null) {
        DBNode newNode = genNode(bd.appState, bd.dacx, bd.genome, bd.oldGenome, "", Node.INTERCELL, oldID, support);
        String newID = newNode.getID();
        real.setSignalId(newID);
        String normBl = bd.normNames.get("");
        Set<Integer> forBL = bd.nodeToMotifs.get(normBl);
        if (forBL != null) {
          Iterator<Integer> fblit = forBL.iterator();
          while (fblit.hasNext()) {
            int i = fblit.next().intValue();
            DialogBuiltMotifPair nextPair = bd.fullList.get(i);
            DialogBuiltMotifPair oldPair = bd.existingList.get(i);
            nextPair.proto.fillNodesFromMatches(newNode, pair, oldPair, bd.newNodeToOldNode, nextPair.real, bd.normNames);
          }
        }
        if (bd.newNodeToOldNode.get(newID) == null) {
          bd.newTypesByID.put(newID, new Integer(Node.INTERCELL));
        }
      }
    }
    
    oldID = oldSig.getBubbleId();    
    if ((oldID != null) || !bd.existingOnly) {          
      if (real.getBubbleId() == null) {
        DBNode newNode = genNode(bd.appState,bd.dacx, bd.genome, bd.oldGenome, "", Node.BUBBLE, oldID, support);
        String newID = newNode.getID();
        real.setBubbleId(newID);
        String normBl = bd.normNames.get("");
        Set<Integer> forBL = bd.nodeToMotifs.get(normBl);
        if (forBL != null) {
          Iterator<Integer> fblit = forBL.iterator();
          while (fblit.hasNext()) {
            int i = fblit.next().intValue();
            DialogBuiltMotifPair nextPair = bd.fullList.get(i);
            DialogBuiltMotifPair oldPair = bd.existingList.get(i);
            nextPair.proto.fillNodesFromMatches(newNode, pair, oldPair, bd.newNodeToOldNode, nextPair.real, bd.normNames);
          }
        }
        if (bd.newNodeToOldNode.get(newID) == null) {
          bd.newTypesByID.put(newID, new Integer(Node.BUBBLE));
        }
      }
    }
    
    //
    // Links
    //
    
    oldID = oldSig.getSignalLinkId(); 
    if ((oldID != null) || !bd.existingOnly) {
      if (real.getSignalLinkId() == null) {
        PadConstraints pc = padCalc.generatePadConstraints(real.getSourceId(), real.getSignalId(), bd.opm,
                                                           oldSig.getSignalLinkId(), bd.newNodeToOldNode);
        DBLinkage newLink = (DBLinkage)AddCommands.autoAddOldOrNewLinkToRoot(bd.appState, bd.dacx, real.getSourceId(), real.getSignalId(), 
                                                                              Linkage.NONE, bd.oldGenome, 
                                                                              support, true, oldID, pc, Linkage.LEVEL_NONE, bd.ist);
        real.setSignalLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getSignalLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getSignalLinkId(), oldSig.getSignalLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
        }
      }
    }
    
    oldID = oldSig.getBubbleLinkId();    
    if ((oldID != null) || !bd.existingOnly) {             
      if (real.getBubbleLinkId() == null) {
        PadConstraints pc = padCalc.generatePadConstraints(real.getSignalId(), real.getBubbleId(), bd.opm,
                                                           oldSig.getBubbleLinkId(), bd.newNodeToOldNode);
        DBLinkage newLink = (DBLinkage)AddCommands.autoAddOldOrNewLinkToRoot(bd.appState, bd.dacx, real.getSignalId(), real.getBubbleId(), 
                                                                              Linkage.POSITIVE, bd.oldGenome, 
                                                                              support, true, oldID, pc, 
                                                                              Linkage.LEVEL_NONE, bd.ist);
        real.setBubbleLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getBubbleLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getBubbleLinkId(), oldSig.getBubbleLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
        }
      }
    }

    oldID = oldSig.getTransFacLinkId();    
    if ((oldID != null) || !bd.existingOnly) {         
      if (real.getTransFacLinkId() == null) {
        PadConstraints pc = padCalc.generatePadConstraints(real.getTransFacId(), real.getBubbleId(), bd.opm,
                                                   oldSig.getTransFacLinkId(), bd.newNodeToOldNode);
        DBLinkage newLink = (DBLinkage)AddCommands.autoAddLinkToRoot(bd.appState, bd.dacx, real.getTransFacId(), real.getBubbleId(), 
                                                                     Linkage.POSITIVE, support, true, oldID, pc, Linkage.LEVEL_NONE, bd.ist);
        real.setTransFacLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getTransFacLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getTransFacLinkId(), oldSig.getTransFacLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
        }
      }
    }
    
    oldID = oldSig.getTargetPosLinkId();    
    if ((oldID != null) || !bd.existingOnly) {         
      if ((real.getTargetPosLinkId() == null) && 
          ((signalMode_ == SignalBuildInstruction.PROMOTE_SIGNAL) ||
           (signalMode_ == SignalBuildInstruction.SWITCH_SIGNAL))) {
        PadConstraints pc = padCalc.generatePadConstraints(real.getBubbleId(), real.getTargetId(), bd.opm,
                                                   oldSig.getTargetPosLinkId(), bd.newNodeToOldNode);
        DBLinkage newLink = (DBLinkage)AddCommands.autoAddLinkToRoot(bd.appState, bd.dacx, real.getBubbleId(), real.getTargetId(),
                                                                     Linkage.POSITIVE, support, true, oldID, pc, evidenceLevel_, bd.ist);
        real.setTargetPosLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getTargetPosLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getTargetPosLinkId(), oldSig.getTargetPosLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
        }
      }
    }
    
    oldID = oldSig.getTargetNegLinkId();    
    if ((oldID != null) || !bd.existingOnly) {             
      if ((real.getTargetNegLinkId() == null) && 
          ((signalMode_ == SignalBuildInstruction.REPRESS_SIGNAL) ||
           (signalMode_ == SignalBuildInstruction.SWITCH_SIGNAL))) { 
        PadConstraints pc = padCalc.generatePadConstraints(real.getBubbleId(), real.getTargetId(), bd.opm,
                                                   oldSig.getTargetNegLinkId(), bd.newNodeToOldNode);
        DBLinkage newLink = (DBLinkage)AddCommands.autoAddLinkToRoot(bd.appState, bd.dacx, real.getBubbleId(), real.getTargetId(), 
                                                                     Linkage.NEGATIVE, support, true, oldID, pc, evidenceLevel_, bd.ist);
        real.setTargetNegLinkId(newLink.getID());
        bd.padConstraintSaver.put(real.getTargetNegLinkId(), pc);
        bd.newLinksToOldLinks.put(real.getTargetNegLinkId(), oldSig.getTargetNegLinkId()); // May be null
        Iterator<Integer> nflit = bd.needFillLinks.iterator();
        while (nflit.hasNext()) {
          int i = nflit.next().intValue();
          DialogBuiltMotifPair nextPair = bd.fullList.get(i);
          nextPair.proto.fillLinksFromMatches(newLink, pair, nextPair.real);
        }
      }
    }
    real.setSignalMode(signalMode_);

    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we need to call fillLinksFromMatches.
  */
  
  @Override
  public boolean needsFillLinksFromMatches() {
    return (true);
  }
  
  /***************************************************************************
  **
  ** Fill in the corresponding real motif from matches in the filled Pair
  */
  
  @Override
  public void fillLinksFromMatches(DBLinkage newLink, DialogBuiltMotifPair filledPair, 
                                   DialogBuiltMotif realMotif) {
    //
    // We have a newly created link  Check in our corresponding real motif to see if it can be
    // filled in by that link.  We can share all links except those going to the target; those
    // are never shared.
    //
  
    if (!(filledPair.real instanceof DialogBuiltSignalMotif)) {
      return;
    }                                     
                                     
    DialogBuiltSignalMotif rsm = (DialogBuiltSignalMotif)realMotif;
    DialogBuiltSignalMotif filledRealSignalMotif = (DialogBuiltSignalMotif)filledPair.real;    
        
    //
    // We can share up to three links.  If the previously filled real motif nodes
    // match our nodes, we can share the link
    //
    
    String filledSource = filledRealSignalMotif.getSourceId();
    String filledSignal = filledRealSignalMotif.getSignalId();
    String filledSignalLink = filledRealSignalMotif.getSignalLinkId();
    
    if (newLink.getID().equals(filledSignalLink)) {
      if ((filledSource != null) && (filledSignal != null) && (filledSignalLink != null) &&
          filledSource.equals(rsm.getSourceId()) &&
          filledSignal.equals(rsm.getSignalId()) &&
          (rsm.getSignalLinkId() == null)) {
        rsm.setSignalLinkId(filledSignalLink);
      }
      return;
    }
    
    String filledBubble = filledRealSignalMotif.getBubbleId();
    String filledBubbleLink = filledRealSignalMotif.getBubbleLinkId();
    
    if (newLink.getID().equals(filledBubbleLink)) {
      if ((filledSignal != null) && (filledBubble != null) && (filledBubbleLink != null) &&
          filledSignal.equals(rsm.getSignalId()) &&
          filledBubble.equals(rsm.getBubbleId()) &&
          (rsm.getBubbleLinkId() == null)) {
        rsm.setBubbleLinkId(filledBubbleLink);
      }
      return;
    }

    String filledTransFac = filledRealSignalMotif.getTransFacId();
    String filledTransFacLink = filledRealSignalMotif.getTransFacLinkId();
    
    if (newLink.getID().equals(filledTransFacLink)) {
      if ((filledBubble != null) && (filledTransFac != null) && (filledTransFacLink != null) &&
          filledBubble.equals(rsm.getBubbleId()) &&
          filledTransFac.equals(rsm.getTransFacId()) &&
          (rsm.getTransFacLinkId() == null)) {
        rsm.setTransFacLinkId(filledTransFacLink);
      }
      return;
    }    
    
    return; 
  }
}
