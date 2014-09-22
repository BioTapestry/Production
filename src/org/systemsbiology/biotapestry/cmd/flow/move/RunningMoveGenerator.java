/*
**    Copyright (C) 2003-201 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.move;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.GenomePresentation;
import org.systemsbiology.biotapestry.ui.GenomePresentation.NetModuleIntersect;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.ui.freerender.LinkageFree;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This handles the generation of Running Moves
*/

public class RunningMoveGenerator {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private GenomePresentation gPre_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public RunningMoveGenerator(GenomePresentation gPre) {
    gPre_ = gPre;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get a running move for the start point
  */
  
  public List<RunningMove> getRunningMove(Point2D start, DataAccessContext rcxM) {

    String ovrKey = rcxM.oso.getCurrentOverlay();
    TaggedSet modKeys = rcxM.oso.getCurrentNetModules();
    int intersectionMask = rcxM.oso.getCurrentOverlaySettings().intersectionMask;
    Set<String> netMods = (modKeys == null) ? null : modKeys.set;
    TaggedSet revKeys = rcxM.oso.getRevealedModules();
    Set<String> revMods = (revKeys == null) ? null : revKeys.set;
    
    ArrayList<RunningMove> retval = new ArrayList<RunningMove>();
    
    //
    // Current group move semantics.  If we are already selected, then move all selected items
    // as a group.  If not, we just move the current item.  This might change to having the
    // drag click adopt selection semantics (see above).
    //
    
    
    int x = (int)start.getX();
    int y = (int)start.getY();
    
      
    if (!gPre_.linksAreHidden(rcxM)) {
      // Intersect and move linkage labels:
      HashSet<String> seenSrcs = new HashSet<String>();
      Iterator<Linkage> lit = rcxM.getGenome().getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String lSrc = link.getSource();
        if (seenSrcs.contains(lSrc)) {
          continue;
        }
        seenSrcs.add(lSrc);
        LinkProperties lp = rcxM.getLayout().getLinkProperties(link.getID());
        LinkageFree render = (LinkageFree)lp.getRenderer();
        if (render.intersectsLabel(link, lp, rcxM, start)) {
          Intersection dummy = new Intersection(null, null, 0.0);
          dummy = gPre_.filterForModuleMask(rcxM, start, dummy, ovrKey, netMods, revMods, intersectionMask);
          if (dummy != null) {
            RunningMove rm = new RunningMove(RunningMove.MoveType.LINK_LABEL, start);
            rm.linkID = link.getID();
            retval.add(rm);
          }
        }
      }
    }
    
    // Intersect and move model data:
    String modelKey = gPre_.intersectModelData(rcxM, start, ovrKey, netMods, intersectionMask);
    if (modelKey != null) {
      RunningMove rm = new RunningMove(RunningMove.MoveType.MODEL_DATA, start);
      rm.modelKey = modelKey;
      retval.add(rm);
    } 
    
    if (ovrKey != null) {
      //
      // What is the module boundary drag policy?  1) We cannot drag a boundary that is excluding
      // a member.  2) If we try to drag an auto member, that drags the member only.
      //
      Intersection nameBound = gPre_.intersectANetModuleElement(x, y, rcxM, NetModuleIntersect.NET_MODULE_NAME);
      if (nameBound != null) {
        RunningMove rm = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
        rm.ovrId = ovrKey;
        rm.modIntersect = nameBound;
        retval.add(rm);
      }

      Intersection bound = gPre_.intersectANetModuleElement(x, y, rcxM, NetModuleIntersect.NET_MODULE_BOUNDARY);
      if (bound != null) {
        NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)bound.getSubID();
        // Direct means we hit a member or non-member auto rect:
        if ((ei.directShape != null) && !ei.nonDirectAlternative) {
          if (ei.directShape.shapeClass == NetModuleFree.TaggedShape.NON_MEMBER_SHAPE) {
            RunningMove rm = new RunningMove(); // i.e. "not permitted"
            retval.add(rm);
          } else if (ei.directShape.shapeClass == NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE) {            
            String nodeID = ei.directShape.memberID;
            Intersection nodeInt = new Intersection(nodeID, null, 0.0);
            RunningMove rm = new RunningMove(RunningMove.MoveType.INTERSECTIONS, start);
            rm.toMove = new Intersection[1];
            rm.toMove[0] = nodeInt;
            retval.add(rm);
          }
        }
        RunningMove rm = new RunningMove(RunningMove.MoveType.NET_MODULE_EDGE, start);
        rm.ovrId = ovrKey;
        rm.modIntersect = bound;
        retval.add(rm);
      }
      
      if (rcxM.oso.showingModuleComponents()) {       
        Intersection defBound = gPre_.intersectANetModuleElement(x, y, rcxM, NetModuleIntersect.NET_MODULE_DEFINITION_RECT);
        if (defBound != null) {
          RunningMove rm = new RunningMove(RunningMove.MoveType.NET_MODULE_EDGE, start);
          rm.ovrId = ovrKey;
          rm.modIntersect = defBound;
          retval.add(rm);
        }            
      }
      
      Intersection nmLink = gPre_.intersectNetModuleLinks(x, y, rcxM);
      if (nmLink != null) {
        RunningMove rm = new RunningMove(RunningMove.MoveType.NET_MODULE_LINK, start);
        rm.ovrId = ovrKey;
        rm.modIntersect = nmLink;
        retval.add(rm);
      }
    }
    
    List<Intersection.AugmentedIntersection> augs = gPre_.selectionGuts(x, y, rcxM, true, false, false);
 
    //
    // Expand each selection into a possible move:
    //
    
    if (augs != null) {
      int numAug = augs.size();
      for (int i = 0; i < numAug; i++) {
        Intersection.AugmentedIntersection aug = augs.get(i);
        buildIntersectionToMove(aug, start, rcxM, retval);
      }
    }
    
    return ((retval.isEmpty()) ? null : retval);
  } 

  /***************************************************************************
  **
  ** Move the selected net module
  */
  
  public RunningMove[] getRunningMovesForNetModule(Intersection bound, Point2D start, DataAccessContext rcxA,
                                                   String ovrKey, boolean moveGuts, int moveMode) {
    if (ovrKey == null) {
      throw new IllegalArgumentException();
    }
 
    String moduleID = bound.getObjectID();

    RunningMove modRM = new RunningMove(RunningMove.MoveType.NET_MODULE_WHOLE, start);
    modRM.ovrId = ovrKey;
    switch (moveMode) {
      case NetModuleProperties.ONE_MODULE_SHAPE:
        modRM.modIntersect = bound;
        NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)bound.getSubID();
        if (ei.contigInfo != null) {
          ei.contigInfo.intersectedShape = null;
        }
        break;
      case NetModuleProperties.CONTIG_MODULE_SHAPES:
        modRM.modIntersect = bound;
        break;
      case NetModuleProperties.ALL_MODULE_SHAPES:
        modRM.modIntersect = new Intersection(bound, null);
        break;
      default:
        throw new IllegalArgumentException();
    }
      
    NetworkOverlay nov = rcxA.getGenomeSource().getOverlayOwnerFromGenomeKey(rcxA.getGenomeID()).getNetworkOverlay(ovrKey);     
    NetModule nmod = nov.getModule(moduleID);
    NetOverlayProperties nop = rcxA.getLayout().getNetOverlayProperties(ovrKey);
    NetModuleProperties nmp = nop.getNetModuleProperties(moduleID);
    
    RunningMove interRM = null;
    RunningMove nameRM = null;

    if (moveGuts) {
      NetModuleFree nmf = new NetModuleFree();
      Set<String> memIDs;
      if (moveMode == NetModuleProperties.ONE_MODULE_SHAPE) {
        NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)bound.getSubID();
        // This means just move the members in the actual intersected rectangles: 
        if ((ei.type == NetModuleFree.IntersectionExtraInfo.IS_INTERNAL) && 
            (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) && (ei.contigInfo != null)) {
          memIDs = new HashSet<String>();
          memIDs.addAll(ei.contigInfo.memberShapes.keySet());          
        } else {     
          memIDs = nmf.membersInRegion(bound, nmod, rcxA.getLayout(), ovrKey, moveMode);
          if (memIDs.isEmpty() && (ei.directShape != null)) {
            if ((ei.directShape.memberID != null) && (ei.directShape.shapeClass == NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE)) {
              memIDs.add(ei.directShape.memberID);
            }
          }
          if (nmf.nameInRegion(bound, nmod, ovrKey, moveMode, rcxA)) {
            nameRM = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
            nameRM.ovrId = ovrKey;
            nameRM.modIntersect = new Intersection(moduleID, null, 0.0);
          }
        }
      } else if (moveMode == NetModuleProperties.CONTIG_MODULE_SHAPES) {
        // If we have a contig rect type, we need to move everybody.  Else find out the
        // guys contained in the contig shape we intersected:         
        if (nmp.getType() == NetModuleProperties.CONTIG_RECT) {
          memIDs = new HashSet<String>();
          Iterator<NetModuleMember> memit = nmod.getMemberIterator();
          while (memit.hasNext()) {
            NetModuleMember nmm = memit.next();
            memIDs.add(nmm.getID());
          } 
          nameRM = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
          nameRM.ovrId = ovrKey;
          nameRM.modIntersect = new Intersection(moduleID, null, 0.0);
        } else {         
          memIDs = nmf.membersInRegion(bound, nmod, rcxA.getLayout(), ovrKey, moveMode);
          NetModuleFree.IntersectionExtraInfo ei = (NetModuleFree.IntersectionExtraInfo)bound.getSubID();
          if (memIDs.isEmpty() && (ei.directShape != null)) {
            if ((ei.directShape.memberID != null) && (ei.directShape.shapeClass == NetModuleFree.TaggedShape.AUTO_MEMBER_SHAPE)) {
              memIDs.add(ei.directShape.memberID);
            }
          }
          if (nmf.nameInRegion(bound, nmod, ovrKey, moveMode, rcxA)) {
            nameRM = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
            nameRM.ovrId = ovrKey;
            nameRM.modIntersect = new Intersection(moduleID, null, 0.0);
          }
        }       
      } else {
        memIDs = new HashSet<String>();
        Iterator<NetModuleMember> memit = nmod.getMemberIterator();
        while (memit.hasNext()) {
          NetModuleMember nmm = memit.next();
          memIDs.add(nmm.getID());
        }
        nameRM = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
        nameRM.ovrId = ovrKey;
        nameRM.modIntersect = new Intersection(moduleID, null, 0.0);
      }
      
      
      ArrayList<Intersection> interHolder = new ArrayList<Intersection>();
      //
      // If we are moving guts, move the contained links too:
      //
      
      Map<String, Intersection>  linkMovesPerSrc = getLinkRunningMovesForModule(rcxA, ovrKey, moduleID, memIDs, bound, moveMode); 
      interHolder.addAll(linkMovesPerSrc.values());
           
      Iterator<String> memit = memIDs.iterator();
      while (memit.hasNext()) {
        String nodeID = memit.next();
        Intersection nodeInt = new Intersection(nodeID, null, 0.0);
        interHolder.add(nodeInt);
      }  
      int numHold = interHolder.size();
      interRM = new RunningMove(RunningMove.MoveType.INTERSECTIONS, start);
      interRM.toMove = new Intersection[numHold];
      interHolder.toArray(interRM.toMove);
    }
  
    int retSize = 1;
    if (interRM != null) {
      retSize++;
    }
    if (nameRM != null) {
      retSize++;
    }    
    RunningMove[] rets = new RunningMove[retSize];
    rets[0] = modRM;
    int count = 1;
    if (nameRM != null) {
      rets[count++] = nameRM;
    }
    if (moveGuts) {
      rets[count] = interRM;
    }
    return (rets);
  }  

  /***************************************************************************
  **
  ** Move the given group
  */
  
  public RunningMove[] getRunningMovesForGroup(Point2D start, DataAccessContext rcxI, String groupID) {

    RunningMove retval = new RunningMove(RunningMove.MoveType.INTERSECTIONS, start);
 
    GenomeInstance gi = rcxI.getGenomeAsInstance();
    Group group = gi.getGroup(groupID);
       
    GroupFree gf = rcxI.getLayout().getGroupProperties(groupID).getRenderer();
    Rectangle bounds = gf.getBounds(group, rcxI, null, false);  
    Intersection[] labels = Intersection.getLabelIntersections(gi, group);

    ArrayList<Intersection> retvalHolder = new ArrayList<Intersection>();
    Iterator<Node> anit = gi.getAllNodeIterator();
    while (anit.hasNext()) {
      Node node = anit.next();
      String nodeID = node.getID();
      Group groupFN = gi.getGroupForNode(nodeID, GenomeInstance.ALWAYS_MAIN_GROUP);
      if (groupFN.getID().equals(groupID)) {
        Intersection nodeInt = new Intersection(nodeID, null, 0.0);
        retvalHolder.add(nodeInt);
      }
    }
    
    //
    // Bring the relevant link segments along for the ride:
    //
    
    HashMap<String, Intersection> mergeMapToInter = new HashMap<String, Intersection>();    
    HashMap<String, RunningMove> srcMapToLabel = new HashMap<String, RunningMove>();
    getLinkRunningMovesForGroup(rcxI, bounds, start, groupID, mergeMapToInter, srcMapToLabel);
    retvalHolder.addAll(mergeMapToInter.values());
    
    int numHold = retvalHolder.size(); 
    retval.toMove = new Intersection[labels.length + numHold];
    retvalHolder.toArray(retval.toMove);
    System.arraycopy(labels, 0, retval.toMove, numHold, labels.length);
    
    //
    // If the group has any regions attached, move them too:
    //
    
    RunningMove[] modMoves = moveAttachedModules(rcxI, groupID, start);
    int numMod = (modMoves == null) ? 0 : modMoves.length;   
  
    int numLabels = srcMapToLabel.size();
    RunningMove[] rets = new RunningMove[1 + numLabels + numMod];
    
    int count = 0;
    rets[count++] = retval;
    Iterator<RunningMove> smlit = srcMapToLabel.values().iterator();
    while (smlit.hasNext()) {
      RunningMove labelMove = smlit.next();
      rets[count++] = labelMove;
    }
    
    if (numMod != 0) {
      System.arraycopy(modMoves, 0, rets, count, numMod);
    }
    
    return (rets);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out what pad fixups are needed for a move
  */
  
   public static RunningMove.PadFixup needPadFixupsForMoves(RunningMove[] moves) {
    //
    // An intersection means full-layout fixup.  Else if
    // we have module pieces, we fixup the overlay.  Else
    // no fixup needed.
    //
    boolean haveMod = false;
    boolean haveInter = false;
    for (int i = 0; i < moves.length; i++) {
      RunningMove.MoveType currType = moves[i].type;
      if ((currType == RunningMove.MoveType.NET_MODULE_NAME) ||
          (currType == RunningMove.MoveType.NET_MODULE_EDGE) ||
          (currType == RunningMove.MoveType.NET_MODULE_WHOLE)) {
        haveMod = true;
      }
      if (currType == RunningMove.MoveType.INTERSECTIONS) {
        haveInter = true;
      }
    }
   if (haveInter) {
      return (RunningMove.PadFixup.PAD_FIXUP_FULL_LAYOUT);
    } else if (haveMod) {
      return (RunningMove.PadFixup.PAD_FIXUP_THIS_OVERLAY);
    } else {
      return (RunningMove.PadFixup.NO_PAD_FIXUP);
    } 
  }

  /***************************************************************************
  **
  ** Figure out what pad fixups are needed for a move
  */
  
  public static RunningMove.PadFixup needPadFixupsForMove(RunningMove move) {
    if (move.notPermitted) {
      return (RunningMove.PadFixup.NO_PAD_FIXUP);
    } 
    switch (move.type) {
      case LINK_LABEL:
      case MODEL_DATA:
      case NET_MODULE_LINK:
        return (RunningMove.PadFixup.NO_PAD_FIXUP);
      case NET_MODULE_NAME:
      case NET_MODULE_EDGE:
      case NET_MODULE_WHOLE:        
        return (RunningMove.PadFixup.PAD_FIXUP_THIS_OVERLAY);
      case INTERSECTIONS:
        return (RunningMove.PadFixup.PAD_FIXUP_FULL_LAYOUT);
      default:
        throw new IllegalArgumentException();
    }
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** With an augmented intersection, build a running move 
  */
  
  private void buildIntersectionToMove(Intersection.AugmentedIntersection aug, Point2D start,
                                       DataAccessContext rcx, List<RunningMove> moveList) {
 
    Intersection selection = (aug == null) ? null : aug.intersect;
    if (selection == null) {
      return;
    }
       
    //
    // Intersections of group labels result in that label being moved.  Any other
    // group intersection means nothing happens.
    //
    
    if (Intersection.isGroupSubID(selection)) {
      if (Intersection.getLabelID(selection) == null) {
        return;
      }
      GroupSettings.Setting groupViz = rcx.gsm.getGroupVisibility(rcx.getGenomeID(), selection.getObjectID(), rcx);
      if (groupViz != GroupSettings.Setting.ACTIVE) {
        return;
      }
    }
    
    //
    // Crank through each item and do the move
    //
    
    RunningMove rm = new RunningMove(RunningMove.MoveType.INTERSECTIONS, start, aug.type);
    rm.toMove = gPre_.selectionToArray(selection);
    rm.toMove = gPre_.selectionReduction(rm.toMove, rcx);
    moveList.add(rm);
    return;
  } 
  
  
  /***************************************************************************
  **
  ** Handle link prep for group moves
  */
  
  private void getLinkRunningMovesForGroup(DataAccessContext rcxL, Rectangle bounds, Point2D start, String groupID, 
                                           Map<String, Intersection> mergeMapToInter, Map<String, RunningMove> srcMapToLabel) {
       
    Iterator<Linkage> lit = rcxL.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      GenomeInstance.GroupTuple gtup = rcxL.getGenomeAsInstance().getRegionTuple(link.getID());
      String srcGrp = gtup.getSourceGroup();
      String trgGrp = gtup.getTargetGroup();
      if (!srcGrp.equals(groupID) && !trgGrp.equals(groupID)) {
        // totally outside
        continue;
      }
      String srcID = link.getSource();
      Intersection mergeInter = mergeMapToInter.get(srcID);
      LinkageFree render = (LinkageFree)rcxL.getLayout().getLinkProperties(link.getID()).getRenderer();
      Intersection inter = Intersection.pathIntersection(link, rcxL);
      if (!(srcGrp.equals(groupID) && trgGrp.equals(groupID))) {
        // only partially inside: get part inside!
        Intersection boundsInter = render.intersects(link, bounds, false, rcxL, null);
        if (boundsInter != null) {
          Intersection normInter = new Intersection(inter.getObjectID(), boundsInter.getSubID(), 
                                                    boundsInter.getDistance(), boundsInter.canMerge());        
          inter = Intersection.intersection(inter, normInter);
        } else {
          inter = null;
        }
      }
      
      if ((mergeInter != null) && (inter != null)) {
        Intersection normInter = new Intersection(mergeInter.getObjectID(), inter.getSubID(), 
                                                  inter.getDistance(), inter.canMerge());
        inter = Intersection.mergeIntersect(mergeInter, normInter);
      }
      if (inter == null) {
        continue;
      }
      
      mergeMapToInter.put(srcID, inter);
      RunningMove labelMove = srcMapToLabel.get(srcID);
      if (labelMove == null) {
        BusProperties bp = rcxL.getLayout().getLinkProperties(link.getID());    
        if (render.intersectsLabel(link, bp, rcxL, bounds)) {
          labelMove = new RunningMove(RunningMove.MoveType.LINK_LABEL, start);
          labelMove.linkID = link.getID();
          srcMapToLabel.put(srcID, labelMove);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Handle link prep for module moves
  */
  
  private Map<String, Intersection> getLinkRunningMovesForModule(DataAccessContext rcxF, String ovrKey,
                                                                 String modID, Set<String> memIDs, Intersection intersect, int mode) {
    
    HashMap<String, Intersection> mergeMapToInter = new HashMap<String, Intersection>();
    
    //
    // Build up a shape map for the module:
    //  
    
    Map<String, Rectangle2D> outerRectMap = new HashMap<String, Rectangle2D>(); 
    Map<String, List<Shape>> shapeMap = new HashMap<String, List<Shape>>();
    
    if (mode == NetModuleProperties.ALL_MODULE_SHAPES) {
      HashSet<String> modSet = new HashSet<String>();
      modSet.add(modID);
      gPre_.buildShapeMapForNetModules(rcxF, ovrKey, modSet, outerRectMap, shapeMap);
    } else {
      Rectangle outerBounds = new Rectangle();
      List<Shape> interRects = (new NetModuleFree()).shapesFromIntersection(intersect, modID, rcxF.getLayout(), ovrKey, mode, outerBounds);
      outerRectMap.put(modID, outerBounds);
      shapeMap.put(modID, interRects);
    }
        
    ArrayList<Intersection> partialMatches = new ArrayList<Intersection>();
    ArrayList<Intersection> fullMatches = new ArrayList<Intersection>();
    
    Iterator<Linkage> lit = rcxF.getGenome().getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String srcID = link.getSource();
      String trgID = link.getTarget();
      boolean srcInMod = memIDs.contains(srcID);
      boolean trgInMod = memIDs.contains(trgID); 
      if (!srcInMod && !trgInMod) {
        // totally outside
        continue;
      }
      Intersection inter = Intersection.pathIntersection(link, rcxF);
      if (!(srcInMod && trgInMod)) {
        // only partially inside: get part inside!
        partialMatches.add(inter);
      } else {
        fullMatches.add(inter);
      }
    }
    
    //
    // Cull the partial links down to get the part inside:
    //
    
    List<Intersection> culled = gPre_.intersectCandidatesWithShapes(rcxF, partialMatches, outerRectMap, shapeMap);      
    
    //
    // Merge it all together:
    //
    
    culled.addAll(fullMatches);
    int numCull = culled.size();
    for (int i = 0; i < numCull; i++) {
      Intersection fullInter = culled.get(i);
      String linkID = fullInter.getObjectID();
      Linkage link = rcxF.getGenome().getLinkage(linkID);
      String srcID = link.getSource();
      Intersection mergeInter = mergeMapToInter.get(srcID);
      if (mergeInter != null) {
        Intersection normInter = new Intersection(mergeInter.getObjectID(), fullInter.getSubID(), 
                                                  fullInter.getDistance(), fullInter.canMerge());
        mergeInter = Intersection.mergeIntersect(mergeInter, normInter);
        mergeMapToInter.put(srcID, mergeInter);
      } else {
        mergeMapToInter.put(srcID, fullInter);        
      }
    }
 
    return (mergeMapToInter);
  }  
      
  /***************************************************************************
  **
  ** Get moves for modules attached to a region
  */
  
  private RunningMove[] moveAttachedModules(DataAccessContext rcxA, String groupID, Point2D start) {
    ArrayList<RunningMove[]> retlist = new ArrayList<RunningMove[]>();
    Map<String, Set<String>> moduleMap = rcxA.getGenomeAsInstance().findModulesOwnedByGroup(groupID);
    int numMoves = 0;
    if (!moduleMap.isEmpty()) {
      Iterator<String> mmkit = moduleMap.keySet().iterator();
      while (mmkit.hasNext()) {
        String nokey = mmkit.next();
        Set<String> modSet = moduleMap.get(nokey);
        Iterator<String> msit = modSet.iterator();
        while (msit.hasNext()) {
          String modID = msit.next();
          Intersection inter = new Intersection(modID, null, 0.0);
          RunningMove[] netModMoves = getRunningMovesForNetModule(inter, start, rcxA,
                                                                  nokey, false,
                                                                  NetModuleProperties.ALL_MODULE_SHAPES);
          retlist.add(netModMoves);
          numMoves += netModMoves.length;
          RunningMove[] nameRMs = new RunningMove[1];
          nameRMs[0] = new RunningMove(RunningMove.MoveType.NET_MODULE_NAME, start);
          nameRMs[0].ovrId = nokey;
          nameRMs[0].modIntersect = new Intersection(modID, null, 0.0);
          retlist.add(nameRMs);
          numMoves++;
        }
      }
    }
    
    RunningMove[] rets = new RunningMove[numMoves];
    int count = 0;
    int numRL = retlist.size();
    for (int i = 0; i < numRL; i++) {
      RunningMove[] netModMoves = retlist.get(i);
      for (int j = 0; j < netModMoves.length; j++) {
        rets[count++] = netModMoves[j];
      }
    }
    
    return (rets);
  }  
}
