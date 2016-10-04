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

package org.systemsbiology.biotapestry.ui;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.awt.geom.Rectangle2D;
import java.util.TreeSet;

import org.xml.sax.Attributes;

//import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.util.Vector2D;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.ui.freerender.LinkageFree;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;

/****************************************************************************
**
** Contains the information needed to layout a link tree
*/

public class BusProperties extends LinkProperties implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CLASS INITIALIZATION
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null Constructor
  */

  protected BusProperties() {
    super(new LinkageFree());
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public BusProperties(BusProperties other) {
    super(other);
  } 
 
  /***************************************************************************
  **
  ** Constructor: Single Link Properties IO replacement
  */

  public BusProperties(Genome genome, String color, 
                       String lineStyle, String txtX, String txtY, String txtDir, 
                       Linkage forLink) throws IOException {
    
    super(new LinkageFree());
    loadFromXML(color, forLink.getSource(), lineStyle, txtX, txtY, txtDir);
 
    //
    // Add the first two bus drops:
    //
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
        
    bd = new BusDrop(forLink.getID(), null, BusDrop.END_DROP,  BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);  
  }  
    
  /***************************************************************************
  **
  ** Constructor: SingleLinkProperties replacement UI-based Constructor
  */  
  
  public BusProperties(Genome genome, String ourLink, float txtX, float txtY, String color) {
    super(txtX, txtY, color, genome.getLinkage(ourLink).getSource(), new LinkageFree());
   
    //
    // Add the first two bus drops to get the ball rolling:
    //
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
    
    bd = new BusDrop(ourLink, null, BusDrop.END_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
  }  

  /***************************************************************************
  **
  ** Constructor to change source
  */  
  
  public BusProperties(BusProperties other, String newSource) {
    super(other);
    this.srcTag_ = newSource;
  }   

  /***************************************************************************
  **
  ** Constructor: Make a direct link to the given target
  */  
  
  public BusProperties(BusProperties other, String newSource, BusDrop keepDrop) {
    super(other);
    this.srcTag_ = newSource;
    
    //
    // Add the two bus drops
    //
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP,  BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
    
    bd = new BusDrop(keepDrop.getTargetRef(), null, BusDrop.END_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    drops_.add(bd);
  }   
  
  /***************************************************************************
  **
  ** Build a Bus Properties that retains A SINGLE PATH from the source to the
  ** given drop.
  */

  public BusProperties(BusProperties other, BusDrop retain) {
    super(other);
    this.clearOutAllSegments();
    this.drops_.clear();
    this.srcTag_ = other.srcTag_;
    Iterator<LinkBusDrop> dit = other.drops_.iterator();
      
    //
    // Only retain two drops
    //
    while (dit.hasNext()) {      
      BusDrop nextDrop = (BusDrop)dit.next();
      if ((nextDrop.getDropType() == BusDrop.START_DROP) ||
          (nextDrop.getTargetRef().equals(retain.getTargetRef()))) {
        this.drops_.add(new BusDrop(nextDrop));
      }
    }

    //
    // Only retain the path back to root
    //
    List<LinkSegment> pathToRoot = other.getSegmentsToRoot(retain);  // May be empty for no-seg cases
    Iterator<LinkSegment> sit = pathToRoot.iterator();
    while (sit.hasNext()) {
      LinkSegment segToRoot = sit.next();
      this.addSegment(new LinkSegment(segToRoot));      
    }
    
    //
    // Fixup terminal drops that connect to the start of a non-degenerate
    // link segment, since we don't have that segment anymore!
    //
    
    // FIX ME? Saw non-single case here for compress group following subset layout of group.  But
    // have not been able to reproduce after group layout development was completed.
    
    LinkBusDrop term = getTargetDrop();
    if (term.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) {
      LinkSegment seg = other.getSegment(term.getConnectionTag());
      if (!seg.isDegenerate()) {
        term.setConnectionTag(seg.getParent());
        term.setConnectionSense(BusDrop.CONNECT_TO_END);
      }
    }
  }

  /***************************************************************************
  **
  ** Constructor: SingleLinkProperties replacement.  Used to downPropagate a new 
  ** link property to other layouts.
  */  
  
  public BusProperties(BusProperties other,
                       String linkID, Genome genome, Vector2D offset) {
       
    this(other, null, linkID, offset);
    Linkage theLink = genome.getLinkage(linkID);
    this.srcTag_ = theLink.getSource();
  }  
  
  /***************************************************************************
  **
  ** Used to downPropagate a new bus property to other layouts.
  */

  public BusProperties(BusProperties other, String srcTag, String linkID, Vector2D offset) {
    this(other);
    if (!other.isSingleDropTree()) {
      throw new IllegalArgumentException();
    }
    this.srcTag_ = srcTag;
    Iterator<LinkSegment> sit = this.getSegments();
    while (sit.hasNext()) {
      LinkSegment seg = sit.next();
      seg.shiftBoth(offset.getX(), offset.getY());
    }
    LinkBusDrop drop = getTargetDrop();
    drop.setTargetRef(linkID);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return a new link properties that is a direct link to the given target
  */  
  
  public LinkProperties deriveDirectLink(String newSource, LinkBusDrop keepDrop, Object extraInfo) {   
    if (extraInfo != null) {
      throw new IllegalArgumentException();
    }
    BusProperties retval = this.clone();
    retval.srcTag_ = newSource;
    // This is the fix for BT-01-04-10:1
    retval.clearOutAllSegments();
    retval.drops_.clear();
 
    //
    // Add the two bus drops
    //
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP,  BusDrop.NO_SEGMENT_CONNECTION);
    retval.drops_.add(bd);
    
    bd = new BusDrop(keepDrop.getTargetRef(), null, BusDrop.END_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    retval.drops_.add(bd);
    return (retval);
  }     

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public BusProperties clone() {
    BusProperties retval = (BusProperties)super.clone();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Support for debug
  **
  */
  
  @Override
  public String toString() {
    String retval = super.toString();
    return ("BusProperties " + retval);
  }

  /***************************************************************************
  **
  ** Insert a node somewhere in the tree
  */
  
  public BusProperties insertNodeInTree(LinkSegmentID segID,
                                        String newNodeID, 
                                        String firstLinkID, Map<String, String> linkMap, 
                                        DataAccessContext icx,
                                        Map<String, NodeInsertionDirective> needDirectFixup, NodeInsertionDirective nid) {
    BusProperties newProps = null;
    if (segID.isDirect()) {
      needDirectFixup.put(firstLinkID, nid);
      newProps = insertNodeOnDirect(newNodeID, firstLinkID, linkMap, icx);
    } else if (segID.isForDrop()) {
      BusDrop drop = (BusDrop)getDrop(segID);
      newProps = insertNodeOnDrop(drop, newNodeID, firstLinkID, linkMap, needDirectFixup, nid);
    } else {
      LinkSegment seg = getSegment(segID.getLinkSegTag());
      newProps = (BusProperties)newTreeBelowSegment(seg, newNodeID, firstLinkID, linkMap, null);
    }
    return (newProps);
  }
  
 /***************************************************************************
  **
  ** Merge complementary links in the tree. This assumes we have confirmed that
  ** all links are mergeable.
  */
  
  public void mergeComplementaryLinks(Map<String, String> oldToNew) {
    
    Iterator<String> kit = oldToNew.keySet().iterator();
    while (kit.hasNext()) {
      String oldKey = kit.next();
      String newKey = oldToNew.get(oldKey);
      LinkSegmentID segID = LinkSegmentID.buildIDForEndDrop(oldKey);
      BusDrop drop = (BusDrop)getDropOrNot(segID);
      if (drop != null) {
        drop.setTargetRef(newKey);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Get a fake "link segment" for a direct path that goes to the pads.
  */
  
  public LinkSegment getDirectLinkPath(DataAccessContext icx) {
    Genome genome = icx.getGenome();
    LinkBusDrop targDrop = getTargetDrop();
    Linkage link = genome.getLinkage(targDrop.getTargetRef());
    
    int launch = link.getLaunchPad();
    NodeProperties npSrc = icx.getLayout().getNodeProperties(link.getSource());
    INodeRenderer sRender = npSrc.getRenderer();
    Point2D srcLoc = npSrc.getLocation();
    Node src = genome.getNode(link.getSource());
    Vector2D lauPO = sRender.getLaunchPadOffset(launch, src, icx);
    Point2D sLoc = lauPO.add(srcLoc);
    
    int land = link.getLandingPad();
    int sign = link.getSign();
    NodeProperties npTrg = icx.getLayout().getNodeProperties(link.getTarget());    
    INodeRenderer tRender = npTrg.getRenderer();
    Point2D trgLoc = npTrg.getLocation();
    Node trg = genome.getNode(link.getTarget());
    Vector2D lanPO = tRender.getLandingPadOffset(land, trg, sign, icx);
    Point2D eLoc = lanPO.add(trgLoc);
    
    LinkSegment retval = new LinkSegment(sLoc, eLoc);
    return (retval);  
  }   
  
  /***************************************************************************
  **
  ** When inserting a new node into a tree segment, we want a new link properties
  ** matching the old below the break, and a new drop going into the new node.
  */
  
  private BusProperties insertNodeOnDrop(BusDrop drop,
                                         String newSourceID, String newLinkToNodeID, 
                                         Map<String, String> linkMap, Map<String, NodeInsertionDirective> needDirectFixup, 
                                         NodeInsertionDirective nid) {
    //
    // If the insert is occurring in a start drop, we need to have a SingleLinkProps
    // and the start drop needs to be modified.  We also map all links.
    //
    // If the insert is occurring in an end drop, we need to mod the end drop, and
    // return a singleLinkProps for the enddrop.
    //
    
    int dropType = drop.getDropType();
    if (dropType == LinkBusDrop.START_DROP) {
      needDirectFixup.put(newLinkToNodeID, nid);
      return (insertNodeOnStartDrop(newSourceID, newLinkToNodeID, linkMap));    
    } else {
      return (insertNodeOnEndDrop(drop, newSourceID, newLinkToNodeID, linkMap));
    }
  }  

  /***************************************************************************
  **
  ** For inserting a new node into a direct drop
  */
  
  private BusProperties insertNodeOnDirect(String newSourceID,                                         
                                           String newLinkToNodeID, Map<String, String> linkMap,
                                           DataAccessContext icx) {
    //
    // The return bus will be a direct drop.  On this direct drop, we need to create
    // a degenerate bus, and add a new drop to the new node.
    //

    splitNoSegmentBus(icx);
    LinkSegment rootSeg = getRootSegment(); 
    BusDrop newDrop = new BusDrop(newLinkToNodeID, 
                                  rootSeg.getID(), BusDrop.END_DROP,
                                  BusDrop.CONNECT_TO_START);
    addDrop(newDrop);        
    
    //
    // New direct bus:
    //
    
    BusProperties retval = getBareBus();
    retval.srcTag_ = newSourceID;
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP, 
                             BusDrop.NO_SEGMENT_CONNECTION);
    retval.addDrop(bd);
    if (linkMap.size() != 1) {
      throw new IllegalArgumentException();
    }
    String newID = linkMap.values().iterator().next();
    bd = new BusDrop(newID, null, BusDrop.END_DROP, BusDrop.NO_SEGMENT_CONNECTION);
    retval.addDrop(bd);
    return (retval);
  }  
  
  
  /***************************************************************************
  **
  ** For inserting a new node into a start drop
  */
  
  private BusProperties insertNodeOnStartDrop(String newSourceID, 
                                              String newLinkToNodeID, Map<String, String> linkMap) {
    

    BusProperties retval = new BusProperties(this);
    retval.srcTag_ = newSourceID;

    //
    // Change the starting bus drop to be to the new node:
    //
    
    SuggestedDrawStyle sds = null;
    Iterator<LinkBusDrop> dit = retval.getDrops();
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      if (drop.getDropType() == LinkBusDrop.START_DROP) {
        sds = drop.getSpecialDropDrawStyle(); 
        break;
      }
    }
   
    //
    // Change the link IDs to match the new links:
    //
    
    Iterator<LinkBusDrop> drit = retval.getDrops();
    while (drit.hasNext()) {
      LinkBusDrop drop = drit.next();
      String targRef = drop.getTargetRef();
      if (targRef != null) {
        String mappedRef = linkMap.get(targRef);
        drop.setTargetRef(mappedRef);
      }
    }
    
    //
    // On THIS bus (not the return value) add a new drop to the new node.
    // This drop will later be converted to a direct link, after the
    // other segments are thrown away.
    //
    
    LinkSegment rootSeg = getRootSegment(); 
    BusDrop newDrop = new BusDrop(newLinkToNodeID, 
                                  rootSeg.getID(), BusDrop.END_DROP,
                                  BusDrop.CONNECT_TO_START);
    newDrop.setDrawStyleForDrop((sds == null) ? null : (SuggestedDrawStyle)sds.clone());
    addDrop(newDrop);               
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get a copy with no segs or drops
  */
  
  private BusProperties getBareBus() {
    BusProperties retval = new BusProperties(this);
    retval.segments_.clear();
    retval.drops_.clear();
    retval.labels_ = new UniqueLabeller();
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** For inserting a new node into an end drop
  */
  
  private BusProperties insertNodeOnEndDrop(BusDrop drop, String newSourceID,
                                            String newLinkToNodeID, Map<String, String> linkMap) {
    //
    // If the insert is occurring in an end drop, we need to mod the end drop, and
    // return a LinkProps for the enddrop.
    //

    //
    // On THIS bus (not the return value) add a new drop to the new node
    //
    
    BusDrop newDrop = new BusDrop(drop);
    newDrop.setTargetRef(newLinkToNodeID);
    addDrop(newDrop);               
    
    BusProperties retval = getBareBus();
    retval.srcTag_ = newSourceID;
    
    BusDrop bd = new BusDrop(null, null, BusDrop.START_DROP, 
                             BusDrop.NO_SEGMENT_CONNECTION);
    bd.setDrawStyleForDrop(drop.getSpecialDropDrawStyle());
    retval.addDrop(bd);
    
    if (linkMap.size() != 1) {
      throw new IllegalArgumentException();
    }
    String newID = linkMap.values().iterator().next();    
    bd = new BusDrop(newID, null, BusDrop.END_DROP, 
                     BusDrop.NO_SEGMENT_CONNECTION);
    bd.transferSpecialStyles(drop);    
    retval.addDrop(bd);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the launch pad for the tree
  */
  
  public int getLaunchPad(Genome genome, Layout layout) {
    //
    // Crank through the bus drops, get the linkages, find the launch pads.
    // They should all match, just for kicks, let's make sure.
    //
    // WJRL 3-27-09:  (BT-03-27-09:1) Nasty bug.  Note that we now allow
    // launch pads with negative values, including -1!  So the old default
    // "error" value of -1 was bogus!  Fix it to MIN_VALUE.
    //
    int retval = Integer.MIN_VALUE;
    Iterator<LinkBusDrop> drops = drops_.iterator();
    while (drops.hasNext()) {
      LinkBusDrop drop = drops.next();
      String ref = drop.getTargetRef();
      if (ref != null) {
        Linkage theLink = genome.getLinkage(ref);
        if (theLink == null) {
          continue;
        }
        int ourPad = theLink.getLaunchPad();
        if (retval == Integer.MIN_VALUE) {
          retval = ourPad;
        } else if (ourPad != retval) {
          System.err.println("Launch pad mismatch " + theLink.getID() + " : " + ourPad + " vs " + retval);
          throw new IllegalStateException();
        }
      }
    }
    if (retval == Integer.MIN_VALUE) {
      System.err.println("Launch pad: no pad"); 
      throw new IllegalStateException();
    } else {
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Answer if the given link is in the model
  */
  
  @Override
  public boolean linkIsInModel(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, String linkID) {
    if (linkID == null) {
      throw new IllegalArgumentException();
    }
    Linkage link = genome.getLinkage(linkID);
   // VFN may not include some drops!
    return (link != null);
  }
    
  /***************************************************************************
  **
  ** Init a grid to use for ortho fixing
  */
  
  @Override
  protected LinkPlacementGrid initGridForOrthoFix(LinkRouter router, DataAccessContext rcx,
                                                  String overID,
                                                  BTProgressMonitor monitor) throws AsynchExitRequestException {
    List<String> doNotRender = getLinkageList();     
    return (router.initGrid(rcx, new HashSet<String>(doNotRender), INodeRenderer.LAX_FOR_ORTHO, monitor));
  }
 
  /***************************************************************************
  **
  ** Abstracted source check
  */
  
  @Override
  protected void sourceSanityCheck(Genome genome, LinkProperties other) {  
   //
    // Do some sanity checking
    //

    LinkBusDrop targDrop = other.getTargetDrop();
    Linkage link = genome.getLinkage(targDrop.getTargetRef());
    String slpSrc = link.getSource();
       
    if (!slpSrc.equals(this.getSourceTag())) {
      System.err.println(slpSrc);      
      System.err.println(other.getSourceTag());
      System.err.println(this.getSourceTag());      
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get link label.  May be null
  */
  
  @Override
  protected String getLinkLabel(Genome genome, String linkID) {
    Linkage link = genome.getLinkage(linkID);
    return (link.getName());  
  }

  /***************************************************************************
  **
  ** Get link target.
  */
  
  @Override
  public String getLinkTarget(Genome genome, String linkID) {
    Linkage link = genome.getLinkage(linkID);
    return (link.getTarget());  
  }  
 
  /***************************************************************************
  **
  ** Answer if the source drop is non-orthogonal
  */
  
  @Override
  protected boolean srcDropIsNonOrtho(DataAccessContext icx) {
    Genome genome = icx.getGenome();
    Linkage link = genome.getLinkage(getALinkID(genome));
    String src = link.getSource();
    Node srcNode = genome.getNode(src);
    NodeProperties npSrc = icx.getLayout().getNodeProperties(src);
    INodeRenderer sRender = npSrc.getRenderer();
    Point2D srcLoc = npSrc.getLocation();
    int launch = link.getLaunchPad();
    Vector2D lauPO = sRender.getLaunchPadOffset(launch, srcNode, icx);
    Point2D sPadLoc = lauPO.add(srcLoc);
    LinkBusDrop drop = getRootDrop();  
    LinkSegment rootSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? rootSeg.getStart() : rootSeg.getEnd();
    return ((segPt.getX() != sPadLoc.getX()) && (segPt.getY() != sPadLoc.getY()));
  }   
    
  /***************************************************************************
  **
  ** Get some link ID for the bus
  */
  
  @Override
  public String getALinkID(Genome genome) {    
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        String retval = bd.getTargetRef();
        // FIX FOR BT-10-04-06:1 The link ID must exist in a VFN
        if (genome.getLinkage(retval) != null) {
          return (retval);
        }
      }
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** Get a stable link ID for the bus:
  */
  
  public String getAStableLinkID(Genome genome) {
    TreeSet<String> retval = new TreeSet<String>();
    Iterator<LinkBusDrop> dit = getDrops();
    while (dit.hasNext()) {
      LinkBusDrop bd = dit.next();
      if (bd.getDropType() != LinkBusDrop.START_DROP) {
        String tref = bd.getTargetRef();
        // FIX FOR BT-10-04-06:1 The link ID must exist in a VFN
        if (genome.getLinkage(tref) != null) {
          retval.add(tref);
        }
      }
    }
    return (retval.first());
  }
    
  /***************************************************************************
  **
  ** Answer if the target drop is non-orthogonal
  */
  
  @Override
  protected boolean targetDropIsNonOrtho(LinkBusDrop drop, DataAccessContext icx) {
    String trg = drop.getTargetRef();
    if (trg == null) {
      throw new IllegalStateException();
    }
    Genome genome = icx.getGenome();
    Linkage link = genome.getLinkage(trg);
    int sign = link.getSign();
    int land = link.getLandingPad();
    String trgID = link.getTarget();
    Node trgNode = genome.getNode(trgID);    
    NodeProperties npTrg = icx.getLayout().getNodeProperties(trgID);    
    INodeRenderer tRender = npTrg.getRenderer();
    Point2D trgLoc = npTrg.getLocation();    
    Vector2D lanPO = tRender.getLandingPadOffset(land, trgNode, sign, icx);
    Point2D tPadLoc = lanPO.add(trgLoc);

    LinkSegment trgSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? trgSeg.getStart() : trgSeg.getEnd();
 
    return ((segPt.getX() != tPadLoc.getX()) && (segPt.getY() != tPadLoc.getY()));
  }    

  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  @Override
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<linkBus ");
    writeXMLSupport(out, ind);
    ind.down().indent();
    out.println("</linkBus>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get the displayed label
  */
  
  public String getLabel(Genome genome) {
    StringBuffer label = new StringBuffer();
    Iterator<LinkBusDrop> dit = getDrops();   
    while (dit.hasNext()) {
      LinkBusDrop drop = dit.next();
      String busID = drop.getTargetRef();  // may be null
      if (drop.getDropType() != LinkBusDrop.START_DROP) {
        Linkage link = genome.getLinkage(busID);
        // VFN may not include some drops!
        if (link == null) {
          continue;
        }
        String newLabel = link.getName();
        if ((newLabel != null) && (!newLabel.trim().equals(""))) {
          if (label.length() > 0) {
            label.append('-');
          }
          label.append(newLabel);
        }
      }
    }
    return (label.toString());
  }
  
  /***************************************************************************
  **
  ** Get the start drop segment for this link tree
  */
  
  @Override
  protected LinkSegment getSourceDropSegment(DataAccessContext icx, boolean forceToGrid) {
    Genome genome = icx.getGenome();
    Linkage link = genome.getLinkage(getALinkID(genome));
    String src = link.getSource();
    Node srcNode = genome.getNode(src);
    NodeProperties npSrc = icx.getLayout().getNodeProperties(src);
    INodeRenderer sRender = npSrc.getRenderer();
    Point2D srcLoc = npSrc.getLocation();
    int launch = link.getLaunchPad();
    Vector2D lauPO = sRender.getLaunchPadOffset(launch, srcNode, icx);
    Point2D sPadLoc = lauPO.add(srcLoc);
    // Fixes BT-06-29-05:2:
    if (forceToGrid) {
      UiUtil.forceToGrid(sPadLoc.getX(), sPadLoc.getY(), sPadLoc, UiUtil.GRID_SIZE);
    }
    LinkBusDrop drop = getRootDrop();
    LinkSegment rootSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? rootSeg.getStart() : rootSeg.getEnd();
    return (new LinkSegment(sPadLoc, segPt));
  }   
  
  /***************************************************************************
  **
  ** Get a target drop segment
  */
  
  @Override
  protected LinkSegment getTargetDropSegment(LinkBusDrop drop, DataAccessContext icx) {
    String trg = drop.getTargetRef();
    if (trg == null) {
      throw new IllegalStateException();
    }
    Genome genome = icx.getGenome();
    Linkage link = genome.getLinkage(trg);
    int sign = link.getSign();
    int land = link.getLandingPad();
    String trgID = link.getTarget();
    Node trgNode = genome.getNode(trgID);    
    NodeProperties npTrg = icx.getLayout().getNodeProperties(trgID);    
    INodeRenderer tRender = npTrg.getRenderer();
    Point2D trgLoc = npTrg.getLocation();    
    Vector2D lanPO = tRender.getLandingPadOffset(land, trgNode, sign, icx);
    Point2D tPadLoc = lanPO.add(trgLoc);

    LinkSegment trgSeg = getSegment(drop.getConnectionTag());
    Point2D segPt = 
      (drop.getConnectionSense() == LinkBusDrop.CONNECT_TO_START) ? trgSeg.getStart() : trgSeg.getEnd();
    return (new LinkSegment(segPt, tPadLoc)); 
  }
  
 /***************************************************************************
  **
  ** Build custom drop type:
  */

  @Override
  protected LinkBusDrop generateBusDrop(LinkBusDrop spDrop, String ourConnection, 
                                        int dropType, int connectionEnd) {    
    return (new BusDrop(spDrop.getTargetRef(), ourConnection, dropType, connectionEnd));
  }  
  
 /***************************************************************************
  **
  ** Build custom drop type:
  */

  @Override
  protected LinkBusDrop generateBusDrop(String targetRef, String ourConnection, 
                                        int dropType, int connectionEnd, Object extraInfo) {    
    return (new BusDrop(targetRef, ourConnection, dropType, connectionEnd));
  }
  
  /***************************************************************************
  **
  ** Get a quick kill thickess:
  */  
  
  @Override
  protected ThickInfo maxThickForIntersect(GenomeSource gSrc, Genome genome, OverlayStateOracle oso, double intersectTol) {
    return (maxThickForIntersectSupport(gSrc, genome, oso, false, intersectTol));
  }
  
  /***************************************************************************
  **
  ** Handle source move operations:
  */  
  
  @Override
  protected void moveSource(Object extraInfo) {
    return;
  } 
  
 /***************************************************************************
  **
  ** Shift drop ends
  */
  
  @Override
  protected void shiftDropEnds(double dx, double dy, Vector2D sideDir) {
    return;
  }
  
  /***************************************************************************
  **
  ** Shift drop ends
  */  
  
  @Override
  protected void moveDropEndsPerMap(Map<Point2D, Point2D> mappedPositions, boolean forceToGrid) {
    return;
  }
  
  /***************************************************************************
  **
  ** Shift drop ends
  */
  
  @Override
  protected void shiftSelectedDropEnds(double dx, double dy, LinkFragmentShifts fragShifts) {
    return;
  }
  
 /***************************************************************************
  **
  ** Shift drop ends
  */
  
  @Override
  protected void shiftDropEndsForExpandCompressOps(SortedSet<Integer> newRows, SortedSet<Integer> newCols, Rectangle bounds, double sign, int mult) {
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer of we can relocate an internal segment to another parent
  */
  
  @Override
  public boolean canRelocateSegmentOnTree(LinkSegmentID moveID) {

    if (moveID.isDirect()) {
      return (false);
    }

    if (moveID.isForStartDrop()) {  // root drop
      return (false);
    }
    
    if (moveID.isForEndDrop()) {
      return (segments_.size() > 1);
    }   

    //
    // Non-drop cannot be moved if it is connected to root, and no other internal
    // link is connected to the root.
    //
    
    String myID = moveID.getLinkSegTag();
    String rootSegID = getRootSegment().getID();
    boolean iAmRootChild = false;
    int rootChildCount = 0;
    Iterator<LinkSegment> sit = segments_.values().iterator();
    while (sit.hasNext()) {
      LinkSegment curr = sit.next();
      String cPar = curr.getParent();
      if ((cPar != null) && cPar.equals(rootSegID)) {
        rootChildCount++;
        if (curr.getID().equals(myID)) {
          iAmRootChild = true;
        }
      }
    }
    return (!(iAmRootChild && (rootChildCount == 1)));
  }  
 
  /***************************************************************************
  **
  ** For debug
  */  
 
  public String segNumsToString() {
    return (segments_.keySet().toString());
  }

  /***************************************************************************
  **
  ** Get a forced source direction
  */
  
  @Override
  protected Vector2D getSourceForcedDir(Genome genome, Layout lo) {
    String srcID = getSourceTag();
    Node src = genome.getNode(srcID);
    NodeProperties np = lo.getNodeProperties(srcID);
    INodeRenderer render = np.getRenderer();
    return (render.getDepartureDirection(getLaunchPad(genome, lo), src, lo));  
  }
  
  /***************************************************************************
  **
  ** Get a forced target direction
  */
  
  @Override
  protected Vector2D getTargetForcedDir(Genome genome, Layout lo, LinkSegmentID segID, boolean isDirect) {
     String linkID = (isDirect) ? segID.getDirectLinkRef() : segID.getEndDropLinkRef();
     Linkage link = genome.getLinkage(linkID);
     String trgID = link.getTarget();
     Node trg = genome.getNode(trgID);
     NodeProperties np = lo.getNodeProperties(trgID);    
     INodeRenderer render = np.getRenderer();
     return (render.getArrivalDirection(link.getLandingPad(), trg, lo));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class BusPropertiesWorker extends AbstractFactoryClient {
     
    public BusPropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("linkBus");
      installWorker(new LinkSegment.LinkSegmentWorker(whiteboard), new MySegmentGlue());
      installWorker(new BusDrop.BusDropWorker(whiteboard), new MyBusDropGlue());
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("linkBus")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.busProps = buildFromXML(elemName, attrs);
        retval = board.busProps;
      }
      return (retval);     
    }
    
    private BusProperties buildFromXML(String elemName, Attributes attrs) throws IOException {
      BusProperties newProp = new BusProperties();
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
      LinkProperties.buildFromXMLSupport(elemName, attrs, newProp, board, "linkBus");
      return (newProp);
    }
  }
  
  public static class MySegmentGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      BusProperties busProps = board.busProps;
      LinkSegment seg = board.linkSegment;
      busProps.addSegment(seg);
      return (null);
    }
  }  
  
  public static class MyBusDropGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      BusProperties busProps = board.busProps;
      BusDrop newDrop = board.busDrop; 
      busProps.addDrop(newDrop);
      if (newDrop.getDropType() == BusDrop.END_DROP) {
        String targetTag = newDrop.getTargetRef();
        layout.setLinkProperties(targetTag, busProps);
      }
      return (null);
    }
  }
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      BusProperties busProps = board.busProps;
      SuggestedDrawStyle suggSty = board.suggSty;
      busProps.setDrawStyle(suggSty);
      return (null);
    }
  }
 
  /***************************************************************************
  **
  ** Holds onto props across link rebuild
  */
  
  public static class RememberProps {
    
    private SuggestedDrawStyle myStyle;
    private Point2D textPos;
    private int textDir;
    private Set<String> linkagesForTextPos;
    private int textPosSegmentCardinality;
    private double textPosWeight;
    private double textPosOffset;
    private HashMap<String, PerLinkDrawStyle> linkSpecialStyles; 
 
    // FIX ME!!! Not remembering per link stuff, passing data via legacy settings!
    public RememberProps(RememberProps other) {
      this.myStyle = other.myStyle.clone();
      this.textPos = (other.textPos == null) ? null : (Point2D)other.textPos.clone();
      this.linkagesForTextPos = (other.linkagesForTextPos == null) ? null : new HashSet<String>(other.linkagesForTextPos);
      this.textPosSegmentCardinality = other.textPosSegmentCardinality;      
      this.textPosWeight = other.textPosWeight;
      this.textPosOffset = other.textPosOffset;
      this.textDir = other.textDir;
      this.linkSpecialStyles = new HashMap<String, PerLinkDrawStyle>();
      Iterator<String> ssit = other.linkSpecialStyles.keySet().iterator();
      while (ssit.hasNext()) {
        String linkID = ssit.next();
        PerLinkDrawStyle plds = other.linkSpecialStyles.get(linkID);
        this.linkSpecialStyles.put(linkID, plds.clone()); 
      }
    }    
        
    public RememberProps(BusProperties lp, DataAccessContext rcx) {
      this.myStyle = lp.getDrawStyle().clone();
      this.textPos = (lp.getTextPosition() == null) ? null : (Point2D)lp.getTextPosition().clone();
      
      LinkSegmentID lsid = lp.getSegmentIDForLabel(rcx, null);
      if (lsid != null) {
        this.linkagesForTextPos = lp.resolveLinkagesThroughSegment(lsid);
        LinkSegment seg;
        if (lsid.isForSegment()) {
          seg = lp.getSegment(lsid);
          this.textPosSegmentCardinality = lp.getSegmentsToRoot(seg).size();

        } else if (lsid.isForEndDrop()) {
          LinkBusDrop bd = lp.getTargetDrop(lsid);
          seg = lp.getTargetDropSegment(bd, rcx);
          this.textPosSegmentCardinality = lp.getSegmentsToRoot(bd).size() + 1;
        } else if (lsid.isDirect()) {
          seg = lp.getDirectLinkPath(rcx);
          this.textPosSegmentCardinality = 1;
        } else {
          seg = lp.getSegmentGeometryForID(lsid, rcx, false);
          this.textPosSegmentCardinality = 1;   
        }
        this.textPosWeight = seg.fractionOfRun(this.textPos);
        Point2D textProjection = seg.pointAtFraction(this.textPosWeight);
        Vector2D normOffset = new Vector2D(textProjection, this.textPos);
        Vector2D norm = seg.getNormal();
        this.textPosOffset = (norm == null) ? normOffset.length() : norm.dot(normOffset);
      }
      this.textDir = lp.getTextDirection();
      this.linkSpecialStyles = new HashMap<String, PerLinkDrawStyle>();
      Iterator<LinkBusDrop> bdit = lp.getDrops();
      while (bdit.hasNext()) {
        LinkBusDrop drop = bdit.next();
        if (drop.getDropType() != LinkBusDrop.START_DROP) {
          String linkID = drop.getTargetRef();      
          PerLinkDrawStyle plds = drop.getDrawStyleForLink();
          if (plds != null) {
            this.linkSpecialStyles.put(linkID, plds.clone());
          }
        }
      }
    }

    //
    // Need to derive props from root
    //
    
    public RememberProps buildInheritedProps(GenomeInstance gi) {
      RememberProps retval = new RememberProps(this);
      //
      // For each link in the special styles, need to create multiples for 
      // each instance in the child.
      //
      retval.linkSpecialStyles.clear();
      Iterator<Linkage> lit = gi.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String linkID = link.getID();
        String baseID = GenomeItemInstance.getBaseID(linkID);
        PerLinkDrawStyle plds = this.linkSpecialStyles.get(baseID);
        if (plds != null) {
          retval.linkSpecialStyles.put(linkID, plds.clone());
        }
      }
      return (retval);
    }
       
    public void remapLinks(Map<String, String> linkIDMap) {
      HashMap<String, PerLinkDrawStyle> mappedlinkSpecialStyles = new HashMap<String, PerLinkDrawStyle>();

      Iterator<String> kit = linkSpecialStyles.keySet().iterator();
      while (kit.hasNext()) {
        String linkID = kit.next();
        String mappedID = linkIDMap.get(linkID);
        PerLinkDrawStyle plds = linkSpecialStyles.get(linkID);
        if (plds != null) {
          mappedlinkSpecialStyles.put((mappedID == null)? linkID : mappedID, plds);
        }
      }
      linkSpecialStyles = mappedlinkSpecialStyles;      
      
      if (linkagesForTextPos != null) {
        HashSet<String> mappedlinkagesForTextPos = new HashSet<String>();
        Iterator<String> lit = linkagesForTextPos.iterator();
        while (lit.hasNext()) {
          String linkID = lit.next();
          String mappedID = linkIDMap.get(linkID); 
          mappedlinkagesForTextPos.add((mappedID == null)? linkID : mappedID);
        }
        linkagesForTextPos = mappedlinkagesForTextPos;
      }
      return;
    }    
    
    public void restore(BusProperties lp, DataAccessContext rcx) {
      lp.setDrawStyle(myStyle);
      lp.setTextDirection(textDir);
      
      Iterator<String> kit = linkSpecialStyles.keySet().iterator();
      while (kit.hasNext()) {
        String linkID = kit.next();     
        PerLinkDrawStyle plds = linkSpecialStyles.get(linkID);
        if ((plds != null) && lp.haveTargetDrop(linkID)) {  // May no longer be present
          LinkBusDrop drop = lp.getTargetDrop(linkID);
          drop.setDrawStyleForLink(plds);
        }
      } 
      //
      // Restoring label locations is a separate step.  Just do default
      //
      lp.setTextPosition(textPos);
      return;
    }  
    
    public void restoreLabelPosition(BusProperties lp, DataAccessContext rcx, Rectangle2D rect) {
      if (linkagesForTextPos != null) {
        LinkSegmentID segID = lp.findBestSegmentForLabel(rcx.getGenomeSource(), linkagesForTextPos, textPosSegmentCardinality, rcx.getGenome(), null);      
        LinkSegment seg = lp.getSegmentGeometryForID(segID, rcx, false);        
        Point2D textProjection = seg.pointAtFraction(textPosWeight);
        Vector2D segNorm = seg.getNormal();
        segNorm = (segNorm == null) ? new Vector2D(0.0, 0.0) : segNorm.scaled(textPosOffset);
        textProjection = segNorm.add(textProjection);
        // If we have a rectangle and this value is nuts, pull it back to reality:
        alwaysRealityBased(textProjection, rect);  
        lp.setTextPosition(textProjection);        
      } else if ((rect != null) && (textPos != null)) {
        // If we have a rectangle and no position info, put on the boundary:
        Point2D newPt = new Point2D.Double(textPos.getX(), textPos.getY());
        alwaysRealityBased(newPt, rect);     
        lp.setTextPosition(newPt);
      } else {
        lp.setTextPosition(textPos);
      }
      return;
    }   
    
    private void alwaysRealityBased(Point2D textProjection, Rectangle2D rect) { 
      if ((rect != null) && !rect.contains(textProjection)) {
        double pullBackX = textProjection.getX();
        double pullBackY = textProjection.getY();
        if (pullBackX < rect.getMinX()) pullBackX = rect.getMinX();
        if (pullBackY < rect.getMinY()) pullBackY = rect.getMinY();
        if (pullBackX > rect.getMaxX()) pullBackX = rect.getMaxX();
        if (pullBackY > rect.getMaxY()) pullBackY = rect.getMaxY();
        textProjection.setLocation(pullBackX, pullBackY);
      }
      UiUtil.forceToGrid(textProjection, UiUtil.GRID_SIZE);        
      return;
    }   
  }
}
