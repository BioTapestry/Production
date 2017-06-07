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

package org.systemsbiology.biotapestry.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.GroupMembership;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.Note;
import org.systemsbiology.biotapestry.ui.freerender.GroupFree;
import org.systemsbiology.biotapestry.ui.freerender.LinkageFree;
import org.systemsbiology.biotapestry.ui.freerender.MultiSubID;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleLinkageFree;
import org.systemsbiology.biotapestry.ui.freerender.NodeBounder;
import org.systemsbiology.biotapestry.ui.freerender.PlacementGridRenderer;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Pattern;
import org.systemsbiology.biotapestry.util.PatternGrid;
import org.systemsbiology.biotapestry.util.PatternPlacerSpiral;
import org.systemsbiology.biotapestry.util.QuadTree;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.Vector2D;


/****************************************************************************
**
** This represents a geometric layout (including colors) for a genome
*/

public class Layout implements Cloneable {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /**
  ** Data locations
  */
  
  public static final String DATE   = "date";
  public static final String ATTRIB = "attrib";
  public static final String TITLE  = "title"; 
  public static final String KEY    = "key";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String name_;
  private String targetGenome_;  
  private HashMap<String, NodeProperties> nodeProps_;
  private HashMap<String, BusProperties> linkProps_;
  private HashMap<String, GroupProperties> groupProps_;
  private HashMap<String, NetOverlayProperties> ovrProps_;  
  private HashMap<String, NoteProperties> noteProps_;
  private HashMap<String, Point2D> dataProps_; 
  private LayoutMetadata lmeta_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for new models
  */

  public Layout(String name, String targetGenome) {
    name_ = name;
    targetGenome_ = targetGenome;    
    nodeProps_ = new HashMap<String, NodeProperties>();
    linkProps_ = new HashMap<String, BusProperties>();
    groupProps_ = new HashMap<String, GroupProperties>();
    ovrProps_ = new HashMap<String, NetOverlayProperties>();
    noteProps_ = new HashMap<String, NoteProperties>();
    dataProps_ = new HashMap<String, Point2D>();
    lmeta_ = new LayoutMetadata();
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public Layout(Layout other) {
    copyCore(other, null);
  }
  
  /***************************************************************************
  **
  ** Copy for copied Genome Instances
  */

  public Layout(Layout other, String name, String targetGenome, Map<String, String> groupIDMap) {
    copyCore(other, groupIDMap);
    name_ = name;
    targetGenome_ = targetGenome;     
  }    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Clone
  */

  public Layout clone() {
    try {
      Layout retval = (Layout)super.clone();
      retval.copyCore(this, null);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    

  /***************************************************************************
  **
  ** Modify the color key for IO Tab Appending
  **
  */
  
  public void mapColorKeyForAppend(Map<String, String> daMap) {
 
    for (GroupProperties props : groupProps_.values()) {
      props.mapColorTags(daMap);
    }
    
    for (NodeProperties props : nodeProps_.values()) {
      props.mapColorTags(daMap);
    }
      
    for (BusProperties props : linkProps_.values()) {
      props.mapColorTags(daMap);
    }
    
    for (NoteProperties props : noteProps_.values()) {
      props.mapColorTags(daMap);
    }
 
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String key = opit.next();
      NetOverlayProperties nop = ovrProps_.get(key);
      Iterator<String> mpit = nop.getNetModulePropertiesKeys();
      while (mpit.hasNext()) {
        String mkey = mpit.next();
        NetModuleProperties nmp = nop.getNetModuleProperties(mkey);     
        nmp.mapColorTags(daMap);
      }
      Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
      while (lmpit.hasNext()) {
        String lkey = lmpit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
        nmlp.mapColorTags(daMap);
      }
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Return the correct genome to serve as a layout target
  */
  
  public static Genome determineLayoutTarget(GenomeSource src, String genomeID) {
    Genome trgGenome = src.getGenome(genomeID);
    Genome useGenome;
    if (trgGenome instanceof DBGenome) {
      useGenome = trgGenome;
    } else {
      GenomeInstance gi = (GenomeInstance)trgGenome;
      GenomeInstance pRoot = gi.getVfgParentRoot();
      useGenome = (pRoot == null) ? gi : pRoot;
    }
    return (useGenome);
  }
  
  /***************************************************************************
  **
  ** Return the correct genome to serve as a layout target
  */
  
  public static Genome determineLayoutTarget(Genome genome) {
    Genome useGenome = genome;
    if (genome instanceof GenomeInstance) {
      GenomeInstance gi = (GenomeInstance)genome;
      GenomeInstance rootGI = gi.getVfgParentRoot();
      useGenome = (rootGI == null) ? gi : rootGI;
    }
    return (useGenome);
  }
  
  /***************************************************************************
  **
  ** Completely replace our contents with the other layout
  */
  
  public void replaceContents(Layout other) {
    copyCore(other, null);
    return;
  }
  
  /***************************************************************************
  **
  ** Return the required size of the layout, either with or without links.  Also
  ** can specify to bound all modules, no modules, or specified modules:
  */
  
  // Yes, this really handles both dynamic and static DataAccessContexts:
  public Rectangle getLayoutBounds(DataAccessContext rcx,
                                   boolean doLinks, 
                                   boolean doModules, boolean doModuleLinks, boolean doNotes, 
                                   boolean doLinkLabels,
                                   String overOwnerKey, String ovrKey, 
                                   TaggedSet modSet, OverlayKeySet allKeys) {
    Rectangle retval = null;
    Genome genome = rcx.getGenomeSource().getGenome(getTarget());
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, genome, rcx.getLayoutSource().getLayoutForGenomeKey(getTarget()));
    if (!irx.getCurrentLayout().getID().equals(getID())) {
      throw new IllegalArgumentException();
    }
    
    if (genome instanceof GenomeInstance) {
      retval = getLayoutBoundsViaGroups(irx, doLinks);
    } else {
      retval = getPartialBounds(null, doLinks, doLinkLabels, null, null, true, irx);     
    }
    if (doNotes) {
      Rectangle noteBounds = getNoteBounds(irx);
      if (noteBounds != null) {
        if (retval == null) {
          retval = noteBounds;
        } else {
          Bounds.tweakBounds(retval, noteBounds);
        }
      }
    }

    // With either skip modules, do some, or do all:

    if (!doModules) {  // Skip them
      return (retval);
    }
    Rectangle nmBounds = null;
    if (ovrKey != null) {
      if ((modSet != null) && !modSet.set.isEmpty()) {
        NetOverlayOwner owner = irx.getGenomeSource().getOverlayOwnerWithOwnerKey(overOwnerKey);
        nmBounds = getLayoutBoundsForNetModules(owner, ovrKey, modSet, doModuleLinks, irx);
      }
    } else if (allKeys != null) {
      nmBounds = getLayoutBoundsForAllOverlays(allKeys, doModuleLinks, irx);
    }
    
    if (retval == null) {
      retval = nmBounds;
    } else if (nmBounds != null) {
      Bounds.tweakBounds(retval, nmBounds);
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Return the required size of the layout, either with or without links
  */
  
  public Rectangle getLayoutBoundsViaGroups(StaticDataAccessContext irx, boolean doLinks) {
    GenomeInstance gi = (GenomeInstance)irx.getCurrentGenome();
    Rectangle retval = null;
    Iterator<Group> grit = gi.getGroupIterator();
    while (grit.hasNext()) {
      Group grp = grit.next();
      Rectangle bounds = getLayoutBoundsForGroup(grp, irx, doLinks, false);
      if (retval == null) {
        retval = bounds;
      } else if (bounds != null) {
        Bounds.tweakBounds(retval, bounds);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the note bounds
  */
  
  public Rectangle getNoteBounds(StaticDataAccessContext irx) {
    
    //
    // Crank through the notes and have the renderer return the
    // bounds.
    //
    
    Rectangle retval = null;
    
    Iterator<Note> ntit = irx.getCurrentGenome().getNoteIterator();
    while (ntit.hasNext()) {
      Note note = ntit.next();
      NoteProperties np = getNoteProperties(note.getID());
      IRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(note, irx, null);
      if (retval == null) {
        retval = bounds;
      } else {
        Bounds.tweakBounds(retval, bounds);
      }
    }       

    return (retval);
  }  
   
  /***************************************************************************
  **
  ** Return the partial bounds of the layout, either with or without links
  */
  
  public Rectangle getPartialBounds(Set<String> nodeIDs, boolean doLinks, 
                                    boolean doLinkLabels, Set<String> linkIDs, 
                                    Map<String, MultiSubID> linkSubs, boolean legacySegs, 
                                    StaticDataAccessContext irx) {
    
    //
    // Crank through the genes and nodes and have the renderer return the
    // bounds.
    //
    
    Genome genome = irx.getCurrentGenome();
    Rectangle retval = null;

    Iterator<Node> git = genome.getAllNodeIterator();
    while (git.hasNext()) {
      Node node = git.next();
      String nodeID = node.getID();
      if ((nodeIDs != null) && !nodeIDs.contains(nodeID)) {
        continue;
      }
      NodeProperties np = getNodeProperties(nodeID);
      if (np == null) {  // can be used on partial layout
        continue;
      }
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(node, irx, null);
      if (retval == null) {
        retval = bounds;
      } else {
        Bounds.tweakBounds(retval, bounds);
      }
    }    
    
    
    //
    // Crank thru links (if requested)
    //
    
    if (doLinks) {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      HashSet<BusProperties> seen = new HashSet<BusProperties>();   
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String linkID = link.getID();
        if ((linkIDs != null) && !linkIDs.contains(linkID)) {
          continue;
        }
        BusProperties lprops = getLinkProperties(linkID);
        if (lprops == null) {  // can be used on partial layout
          continue;
        }
        if (seen.contains(lprops)) {
          continue;
        }
        //
        // Refine bounds with link segments:
        //
        
        MultiSubID si = null;
        if (linkSubs != null) {
          si = linkSubs.get(linkID);  // May still be null...
        }
        LinkageFree rend = (LinkageFree)lprops.getRenderer();
        seen.add(lprops);    
        Rectangle bounds;
        if (legacySegs) {        
          bounds = rend.getBoundsOfSegments(link, irx, si, doLinkLabels);
        } else {
          bounds = rend.getBoundsForSinglePath(link, irx);
        }
        
        if (bounds == null) {
          continue;
        }
        if (retval == null) {
          retval = bounds;
        } else {
          Bounds.tweakBounds(retval, bounds);
        }       
      }
    }

    return (retval);
  }  

  /***************************************************************************
  **
  ** Return the required size of the group in the layout, with or without links
  */
  
  public Rectangle getLayoutBoundsForGroup(Group grp, StaticDataAccessContext irx, boolean doLinks, boolean skipModules) {
    
    //
    // Crank through the genes and nodes IN THE GIVEN GROUP and have the renderer return the
    // bounds.
    //

    GenomeInstance gi = irx.getCurrentGenomeAsInstance();
    
    Rectangle retval = null;
    GroupFree gRend = groupProps_.get(grp.getID()).getRenderer();
                                                 
    retval = gRend.getBounds(grp, irx, null, skipModules);
    
    //
    // Crank thru links (if requested).  We combine bounds for each
    // single path to ignore portions of link trees spanning regions.
    // 
    
    Set<String> groupNodes = grp.areInGroup(gi);
    
    if (doLinks) { 
      Iterator<Linkage> glit = gi.getLinkageIterator();
      while (glit.hasNext()) {
        Linkage link = glit.next();
        String src = link.getSource();
        String trg = link.getTarget();
        if (!groupNodes.contains(src)) {
          continue;
        }
        if (!groupNodes.contains(trg)) {
          continue;
        }
        String linkID = link.getID();
        BusProperties lprops = getLinkProperties(linkID);
        if (lprops == null) {  // can be used on partial layout
          continue;
        }
        LinkageFree rend = (LinkageFree)lprops.getRenderer();
        Rectangle bounds = rend.getBoundsForSinglePath(link, irx);
        if (bounds == null) {
          continue;
        }
        if (retval == null) {
          retval = bounds;
        } else {
          Bounds.tweakBounds(retval, bounds);
        }   
      }
    }
    if (retval != null) {
      UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return chopped up module definitions so that each group owns the piece inside 
  ** of it.  Member-only modules are skipped
  */
  
  public Map<NetModule.FullModuleKey, NetModuleProperties.SliceResult> sliceModulesByAllBounds(Map<String, Rectangle> boundsForGroups, 
                                                                                               Map<String, Integer> groupZOrder, 
                                                                                               Layout.OverlayKeySet fullKeys, 
                                                                                               StaticDataAccessContext irx) {
    if (fullKeys == null) {
      return (null);
    }

    HashMap<NetModule.FullModuleKey, NetModuleProperties.SliceResult> retval = new HashMap<NetModule.FullModuleKey, NetModuleProperties.SliceResult>();
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();        
      NetOverlayOwner noo = irx.getGenomeSource().getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
      NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
      NetModule nmod = no.getModule(fullKey.modKey);   
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
      if (nmp.getType() != NetModuleProperties.MEMBERS_ONLY) {    
        Rectangle2D nameBounds = nmp.getRenderer().getNameBounds(nmp, nmod, irx);    
        NetModuleProperties.SliceResult slices = nmp.sliceByAllBounds(boundsForGroups, groupZOrder, nameBounds);      
        retval.put(fullKey, slices);
      }
    }    
    return (retval);    
  } 
  
  /***************************************************************************
  **
  ** Return a map of which module link segments are contained by the given bounds
  ** JEEZE! FIXME the return type is nuts!
  */
  
  public Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> 
    sliceModuleLinksByAllBounds(Map<String, Rectangle> boundsForGroups, Map<String, Integer> groupZOrder, 
                                Layout.OverlayKeySet fullKeys) {
    if (fullKeys == null) {
      return (null);
    }
    // Map by overlay, not by fullkey!
    HashSet<String> seenKeys = new HashSet<String>();
    HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> retval = new HashMap<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>>();
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
      if (seenKeys.contains(fullKey.ovrKey)) {
        continue;
      }
      seenKeys.add(fullKey.ovrKey);
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
      HashMap<String, Map<String, LinkProperties.LinkFragmentShifts>> assignPerTree = new HashMap<String, Map<String, LinkProperties.LinkFragmentShifts>>();
      while (lmpit.hasNext()) {
        String lkey = lmpit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
        Map<String, LinkProperties.LinkFragmentShifts> assignments = nmlp.mapSegmentsToBounds(boundsForGroups, groupZOrder);
        assignPerTree.put(lkey, assignments);
      }
      retval.put(fullKey.ovrKey, assignPerTree);
    }  
    return (retval);    
  } 
 
  /***************************************************************************
  **
  ** Get approx center of the given node and git.
  */  
 
  public Point2D getApproxCenter(Iterator<Node> nit, Iterator<Node> git) {
    HashSet<Point2D> set = new HashSet<Point2D>(); 
    while (nit.hasNext()) {
      Node node = nit.next();
      NodeProperties np = getNodeProperties(node.getID());      
      set.add(np.getLocation());
    }
    while (git.hasNext()) {
      Node gene = git.next();
      NodeProperties np = getNodeProperties(gene.getID());      
      set.add(np.getLocation());
    }    
    return ((set.size() == 0) ? null : AffineCombination.combination(set, 10.0));    
  }
  
  /***************************************************************************
  **
  ** Get approx center of the given iterator over nodes
  */  
 
  public Point2D getApproxCenter(Iterator<Node> nit) {
    HashSet<Point2D> set = new HashSet<Point2D>(); 
    while (nit.hasNext()) {
      Node node = nit.next();
      NodeProperties np = getNodeProperties(node.getID());      
      set.add(np.getLocation());
    }
    return ((set.size() == 0) ? null : AffineCombination.combination(set, 10.0));    
  }
  
  /***************************************************************************
  **
  ** Get approx center of the given iterator over node IDs
  */  
 
  public Point2D getApproxCenterForIDs(Iterator<String> nit) {
    HashSet<Point2D> set = new HashSet<Point2D>(); 
    while (nit.hasNext()) {
      String nodeID = nit.next();
      NodeProperties np = getNodeProperties(nodeID);      
      set.add(np.getLocation());
    }
    return ((set.size() == 0) ? null : AffineCombination.combination(set, 10.0));    
  }  
   
 /***************************************************************************
  **
  ** Get approx center of the given group.  May be null if no points.
  */  
 
  public Point2D getApproxCenterForGroup(GenomeInstance gi, String groupID, boolean forRoot) {
    //
    // Get each group member and add its position to the set.
    //
    HashSet<Point2D> set = new HashSet<Point2D>(); 
    Group grp = gi.getGroup(groupID);
    Iterator<GroupMember> grit = grp.getMemberIterator();
    while (grit.hasNext()) {
      GroupMember memb = grit.next();
      String id = (forRoot) ? GenomeItemInstance.getBaseID(memb.getID()) : memb.getID();
      NodeProperties np = getNodeProperties(id);
      set.add(np.getLocation());
    }
    return ((set.isEmpty()) ? null : AffineCombination.combination(set, 10.0));
  }
  
  /***************************************************************************
  **
  ** Get approx bounds of the given group
  */  
 
  public Rectangle getApproxBounds(Iterator<Node> nit, Iterator<Node> git, StaticDataAccessContext irx) {

    Rectangle retval = new Rectangle(0, 0, 0, 0);
    
    while (git.hasNext()) {
      Node gene = git.next();
      NodeProperties np = getNodeProperties(gene.getID());
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(gene, irx, null);
      Bounds.tweakBounds(retval, bounds);   
    }

    while (nit.hasNext()) {
      Node node = nit.next();
      NodeProperties np = getNodeProperties(node.getID());
      INodeRenderer rend = np.getRenderer();
      Rectangle bounds = rend.getBounds(node, irx, null);
      Bounds.tweakBounds(retval, bounds);
    }

    Bounds.padBounds(retval, 100, 100);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Move the group tag to the given corner:
  */
  
  public void groupTagToCorner(Group grp, StaticDataAccessContext irx, boolean doLinks) {
    
    Rectangle bounds = getLayoutBoundsForGroup(grp, irx, doLinks, true);
    GroupFree gRend = new GroupFree();
    Rectangle labelBounds = gRend.getLabelBounds(grp, irx);
    if (labelBounds == null) {  // no label-> no bounds!
      return;
    }
    GroupProperties gp = groupProps_.get(grp.getID());
    int leftPad = gp.getPadding(GroupProperties.LEFT);
    int bottomPad = gp.getPadding(GroupProperties.BOTTOM);
    int leftX = bounds.x + leftPad;
    int bottomY = bounds.y + bounds.height - bottomPad;
    int labelY = bottomY - (labelBounds.height / 2) - UiUtil.GRID_SIZE_INT;
    int labelX = leftX + (labelBounds.width / 2) + UiUtil.GRID_SIZE_INT;
    Point2D tagCenter = new Point2D.Double(labelX, labelY);
    UiUtil.forceToGrid(tagCenter, UiUtil.GRID_SIZE);
    gp.setLabelLocation(tagCenter);
    return;
  }  
 

  /***************************************************************************
  **
  ** Get the layout center (may be null)
  */
  
  public Point2D getLayoutCenterAllOverlays(StaticDataAccessContext irx,
                                            OverlayKeySet allKeys) {
    Rectangle bounds = getLayoutBounds(irx, true, true, true, true, true, null, null, null, allKeys);
    if (bounds == null) {
      return (null);
    }
    int cx = bounds.x + (bounds.width / 2);
    int cy = bounds.y + (bounds.height / 2);    
    Point2D gridCenter = new Point2D.Double(); 
    UiUtil.forceToGrid(cx, cy, gridCenter, UiUtil.GRID_SIZE);    
    return (gridCenter);
  }  
   
  /***************************************************************************
  **
  ** Get the layout center (may be null)
  */
  
  public Point2D getLayoutCenter(StaticDataAccessContext irx, boolean doModules, String overOwnerKey, 
                                 String ovrKey, TaggedSet modSet, OverlayKeySet fullModKeys) {
    Rectangle bounds = getLayoutBounds(irx, true, doModules, doModules, true, true, overOwnerKey, ovrKey, modSet, fullModKeys);
    if (bounds == null) {
      return (null);
    }
    int cx = bounds.x + (bounds.width / 2);
    int cy = bounds.y + (bounds.height / 2);    
    Point2D gridCenter = new Point2D.Double(); 
    UiUtil.forceToGrid(cx, cy, gridCenter, UiUtil.GRID_SIZE);    
    return (gridCenter);
  }
  
  /***************************************************************************
  **
  ** Center this layout over the given layout.  If the given layout is for the
  ** root, we are allowed to specify matchNodes == true;
  */
  
  public Vector2D alignToLayout(Layout other, StaticDataAccessContext irx, boolean matchNodes, 
                                OverlayKeySet fullModKeys, OverlayKeySet otherFullModKeys, PadNeedsForLayout padReqs) {
    
    Genome myGenome = irx.getGenomeSource().getGenome(getTarget());
    Point2D otherCenter;
    
    if (matchNodes) {
      Genome rootGenome = irx.getGenomeSource().getRootDBGenome();
      if (!other.getTarget().equals(rootGenome.getID()) ||
          !(myGenome instanceof GenomeInstance)) {
        throw new IllegalArgumentException();
      } 
      GenomeInstance gi = (GenomeInstance)myGenome;
      otherCenter = rootOverlay(other, gi, irx, (otherFullModKeys != null), null, null, null, otherFullModKeys);

    } else {  // MAtch centers
      StaticDataAccessContext orx = new StaticDataAccessContext(irx, irx.getGenomeSource().getGenome(other.getTarget()), other);
      otherCenter = other.getLayoutCenter(orx, (otherFullModKeys != null), null, null, null, otherFullModKeys);
    }
    
    // Still might want to line up kid models with a blank root (e.g. notes)
    if (otherCenter == null) {
      Rectangle rect = irx.getWorkspaceSource().getWorkspace().getWorkspace();
      otherCenter = new Point2D.Double(rect.getWidth() / 2.0, rect.getHeight() / 2.0);
    }

    Vector2D diff = recenterLayout(otherCenter, irx, true, (fullModKeys != null), (fullModKeys != null), null, null, null, fullModKeys, padReqs);
    return (diff);
  }

  /***************************************************************************
  **
  ** Figure out a matching center
  */
  
  private Point2D rootOverlay(Layout rootLayout,
                              GenomeInstance gi, StaticDataAccessContext rcx,
                              boolean doModules, String overOwnerKey, String ovrKey, 
                              TaggedSet modSet, OverlayKeySet fullModKeys) {
    //
    // Choose which region to overlay on the root.  With one region, we are
    // done.  Else pick the biggest region at the center of the layout.
    //
    
    Point2D myCenter = getLayoutCenter(rcx, doModules, overOwnerKey, ovrKey, modSet, fullModKeys);
    if (myCenter == null) {
      return (null);
    }
    
    double maxArea = 0.0;
    double minDist = Double.POSITIVE_INFINITY;
    Group matchGroup = null;
    Iterator<Group> git = gi.getGroupIterator();
    if (gi.groupCount() == 1) {
      matchGroup = git.next();
    } else {
      StaticDataAccessContext irx = new StaticDataAccessContext(rcx, gi, rcx.getCurrentLayout());
      while (git.hasNext()) {
        Group grp = git.next();
        if (grp.isASubset(gi)) {
          continue;
        }
        Rectangle bounds = getLayoutBoundsForGroup(grp, irx, true, false);
        if (bounds == null) {
          continue;
        }
        double centerX = bounds.x + (bounds.width / 2.0);
        double centerY = bounds.y + (bounds.height / 2.0);
        double cDiff = myCenter.distance(centerX, centerY);
        if (cDiff > minDist) {
          continue;
        }
        double area = (double)bounds.height * (double)bounds.width;
        if ((cDiff < minDist) || (area > maxArea)) {
          minDist = cDiff;
          maxArea = area;
          matchGroup = grp;
        }
      }
    }
    
    if (matchGroup == null) {
      return (null);
    }
            
    //
    // For each node, figure out the vector offset.  We will choose the vector
    // offset that has the most matches.  FIX ME: With ties, we have no consistency
    // between sibling layouts.
    //

    Vector2D maxCountOff = null;
    int maxCount = 0;
    Map<Vector2D, Integer> offsetCounts = new HashMap<Vector2D, Integer>();
    Iterator<GroupMember> memit = matchGroup.getMemberIterator();
    while (memit.hasNext()) {
      GroupMember gm = memit.next();
      String memberID = gm.getID();
      NodeProperties myNp = getNodeProperties(memberID);
      String baseID = GenomeItemInstance.getBaseID(memberID);
      NodeProperties rootNp = rootLayout.getNodeProperties(baseID);
      Vector2D diff = new Vector2D(myNp.getLocation(), rootNp.getLocation());
      Integer count = offsetCounts.get(diff);
      if (count == null) {
        offsetCounts.put(diff, new Integer(1));
        if (maxCount == 0) {
          maxCount = 1;
          maxCountOff = diff;
        }
      } else {
        int countVal = count.intValue();
        countVal++;
        if (countVal > maxCount) {
          maxCount = countVal;
          maxCountOff = diff;
        }
        offsetCounts.put(diff, new Integer(countVal));
      }
    }
    
    if (maxCountOff == null) {
      return (null);
    }
    
    Point2D shiftCenter = maxCountOff.add(myCenter);
    return (shiftCenter);
  }

  /***************************************************************************
  **
  ** Recenter the layout
  */
  
  public Vector2D recenterLayout(Point2D center, StaticDataAccessContext irx, boolean doLinks, 
                                 boolean doModules, boolean doModuleLinks, String overOwnerKey, String ovrKey,
                                 TaggedSet modSet, OverlayKeySet fullModKeys, PadNeedsForLayout padReqs) {

    Rectangle bounds = getLayoutBounds(irx, doLinks, doModules, doModuleLinks, true, true, overOwnerKey, ovrKey, modSet, fullModKeys);
    if (bounds == null) {
      return (null);
    }    
    int cx = bounds.x + (bounds.width / 2);
    int cy = bounds.y + (bounds.height / 2);    
    Point2D oldCenter = new Point2D.Double(); 
    UiUtil.forceToGrid(cx, cy, oldCenter, UiUtil.GRID_SIZE);
    Vector2D diff = new Vector2D(oldCenter, center);
    shiftLayout(diff, padReqs, irx);
    return (diff);
  }  
  
  /***************************************************************************
  **
  ** Shift the layout
  */
  
  public void shiftLayout(Vector2D diff, Layout.PadNeedsForLayout padReqs, StaticDataAccessContext rcx) {

    double dx = UiUtil.forceToGridValue(diff.getX(), UiUtil.GRID_SIZE);
    double dy = UiUtil.forceToGridValue(diff.getY(), UiUtil.GRID_SIZE);
    
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties props = gpit.next();      
      Point2D loc = props.getLabelLocation();
      if (loc == null) {
        continue;
      }
      props.setLabelLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    }
    
    Iterator<String> npit = nodeProps_.keySet().iterator();
    while (npit.hasNext()) {
      String nodeID = npit.next();      
      NodeProperties props = nodeProps_.get(nodeID);      
      Point2D loc = props.getLocation();
      rigidPadFixes(nodeID, dx, dy, padReqs, rcx);
      props.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    }    

    HashSet<BusProperties> seen = new HashSet<BusProperties>();
    Iterator<BusProperties> lpit = linkProps_.values().iterator();
    while (lpit.hasNext()) {
      BusProperties props = lpit.next();
      if (!seen.contains(props)) {
        props.fullShift(dx, dy);
        seen.add(props);
      }
    }
    
    Iterator<NoteProperties> ntit = noteProps_.values().iterator();
    while (ntit.hasNext()) {
      NoteProperties np = ntit.next();
      Point2D loc = np.getLocation();
      np.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    }
    
    Iterator<String> dpit = dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = dataProps_.get(key);
      if (pt != null) {
        Point2D newLoc = new Point2D.Double(pt.getX() + dx, pt.getY() + dy);
        dataProps_.put(key, newLoc);
      }
    }
    
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String key = opit.next();
      NetOverlayProperties nop = ovrProps_.get(key);
      Iterator<String> mpit = nop.getNetModulePropertiesKeys();
      while (mpit.hasNext()) {
        String mkey = mpit.next();
        NetModuleProperties nmp = nop.getNetModuleProperties(mkey);
        nmp.shift(dx, dy);
      }
      Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
      while (lmpit.hasNext()) {
        String lkey = lmpit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
        nmlp.fullShift(dx, dy);
      }
    }
    
    return;
  }  
 
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Get the target genome
  */
  
  public String getTarget() {
    return (targetGenome_);
  }

  /***************************************************************************
  **
  ** Set the layout metadata.
  */
  
  public void setMetadata(LayoutMetadata lmeta) {
    lmeta_ = lmeta;
  }  
  
  /***************************************************************************
  **
  ** Answer if we care about the given node in our metadata
  */
  
  public boolean haveNodeMetadataDependency(String modelID, String nodeID) {
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      return (false);
    }
    return (ld.haveNodeDependency(modelID, nodeID));
  }  
  
  /***************************************************************************
  **
  ** Drop node dependency
  */
  
  public PropChange dropNodeMetadataDependency(String modelID, String nodeID) {
    PropChange retval = new PropChange();
    retval.layoutKey = name_;
    retval.metaOrig = lmeta_.clone();
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      throw new IllegalStateException();
    }
    ld.removeNodeDependency(modelID, nodeID);
    retval.newMeta = lmeta_.clone();
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Answer if we care about the given group in our metadata
  */
  
  public boolean haveGroupMetadataDependency(String modelID, String groupID) {
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      return (false);
    }
    return (ld.haveGroupDependency(modelID, groupID));
  }  
  
  /***************************************************************************
  **
  ** Drop group dependency
  */
  
  public PropChange dropGroupMetadataDependency(String modelID, String groupID) {
    PropChange retval = new PropChange();
    retval.layoutKey = name_;
    retval.metaOrig = lmeta_.clone();
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      throw new IllegalStateException();
    }
    ld.removeGroupDependency(modelID, groupID);
    retval.newMeta = lmeta_.clone();
    return (retval);
  }      
  
  /***************************************************************************
  **
  ** Answer if we care about the given model in our metadata
  */
  
  public boolean haveModelMetadataDependency(String modelID) {
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      return (false);
    }
    return (ld.haveModelDependency(modelID));
  }  
  
  /***************************************************************************
  **
  ** Drop model dependency
  */
  
  public PropChange dropModelMetadataDependency(String modelID) {
    PropChange retval = new PropChange();
    retval.layoutKey = name_;
    retval.metaOrig = lmeta_.clone();
    LayoutDerivation ld = lmeta_.getDerivation();
    if (ld == null) {
      throw new IllegalStateException();
    }
    ld.removeModelDependency(modelID);
    retval.newMeta = lmeta_.clone();
    return (retval);
  }      

  /***************************************************************************
  **
  ** Get the layout derivation
  */
  
  public LayoutDerivation getDerivation() {
    return (lmeta_.getDerivation());
  }
  
  /***************************************************************************
  **
  ** Set the layout derivation.
  */
  
  public PropChange setDerivation(LayoutDerivation ld) {
    PropChange retval = new PropChange();
    retval.layoutKey = name_;
    retval.metaOrig = lmeta_.clone();
    lmeta_.setDerivation(ld);
    retval.newMeta = lmeta_.clone();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a node property to the Layout
  */
  
  public PropChange setNodeProperties(String itemId, NodeProperties props) {
    PropChange retval = new PropChange();
    retval.nOrig = null;
    retval.nNewProps = props;
    retval.layoutKey = name_;
    nodeProps_.put(itemId, props);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Hide all node names for minor nodes. Do this only inside a db layout
  ** transaction!
  */
  
  public void hideAllMinorNodeNames(StaticDataAccessContext rcx) {
    Genome genome = rcx.getGenomeSource().getRootDBGenome();
    nodeProps_.keySet().iterator();
    Iterator<String> npit = nodeProps_.keySet().iterator();
    while (npit.hasNext()) {
      String pKey = npit.next();
      Node node = genome.getNode(GenomeItemInstance.getBaseID(pKey));
      NodeProperties props = nodeProps_.get(pKey);
      if (NodeProperties.canHideName(node.getNodeType())) {
        props.setHideName(true);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Remove a node properties
  */
  
  public PropChange removeNodeProperties(String itemId) {
    PropChange retval = new PropChange();    
    retval.nOrig = nodeProps_.get(itemId);
    retval.layoutKey = name_;
    // Don't need to make link changes -> these are empty
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
    retval.nNewProps = null;
    nodeProps_.remove(itemId);
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Set a link properties.  Not for undo cases.
  */
  
  public void setLinkProperties(String itemId, BusProperties props) {
    linkProps_.put(itemId, props);
    return;
  }  
  
  /***************************************************************************
  **
  ** Set a link properties for undo.  Mutiple calls need to provide a common
  ** clone
  */
  
  public PropChange setLinkPropertiesForUndo(String itemId, BusProperties props, BusProperties commonClone) {
    PropChange retval = new PropChange();
    retval.orig = null;
    retval.newProps = (commonClone == null) ? (BusProperties)props.clone() : commonClone;
    retval.layoutKey = name_;
    linkProps_.put(itemId, props);
    retval.linkIDs = new HashSet<String>();
    retval.linkIDs.add(itemId);
    return (retval);
  }    

  /***************************************************************************
  **
  ** Remove a link properties for a link.  This involves
  ** modifying the actual property that is still being referenced by other
  ** links.
  */
  
  public void removeLinkPropertiesAndRemember(String itemId, Map<String, BusProperties.RememberProps> rememberProps, 
                                              StaticDataAccessContext rcx) {
    BusProperties lp = getLinkProperties(itemId);
    if (lp == null) {
      return;
    }    
    BusProperties.RememberProps rp = new BusProperties.RememberProps(lp, rcx);
    rememberProps.put(itemId, rp);
    // Trims out support for this one link from the link tree
    lp.removeLinkSupport(itemId);
    linkProps_.remove(itemId);
    return;
  }  
    
  /***************************************************************************
  **
  ** Remove a link properties for a link.  For tree links, this involves
  ** modifying the actual property that is still being referenced by other
  ** links.
  */
  
  public PropChange removeLinkProperties(String itemId) {
    BusProperties lp = getLinkProperties(itemId);
    if (lp == null) {
      return (null);
    }
    PropChange retval = new PropChange();
    undoPreProcess(retval, lp, null, null);
    retval.linkIDs.remove(itemId);  // This guy is being deleted
    retval.removedLinkID = itemId;
    retval.addedLinkID = null;
    if (!retval.linkIDs.isEmpty()) {
      // Trims out support for this one link from the link tree
      lp.removeLinkSupport(itemId);
      undoPostProcess(retval, lp, null, null);
    }
    linkProps_.remove(itemId);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Merge complementary links together. Do in layout transaction
  */
  
  public void mergeLinkProperties(Map<String, String> oldToNew) {
    HashMap<String, BusProperties> forSrc = new HashMap<String, BusProperties>(); 
    Iterator<String> kit = oldToNew.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      String newKey = oldToNew.get(key);
      BusProperties lp = getLinkProperties(key);
      String srcTag = lp.getSourceTag();
      BusProperties forSrcProp = forSrc.get(srcTag);
      if (forSrcProp == null) {
        forSrc.put(srcTag, lp);
        forSrcProp = lp;
      }
      forSrcProp.mergeComplementaryLinks(oldToNew);
      linkProps_.remove(key);
      linkProps_.put(newKey, forSrcProp);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Use for large batch removes within a layout undo transaction
  */
  
  public void removeMultiLinkProperties(Set<String> itemIds) {
    
    //
    // Group the links by source:
    //
    
    HashMap<String, Set<String>> bySrc = new HashMap<String, Set<String>>();
    Iterator<String> idit = itemIds.iterator();
    while (idit.hasNext()) {
      String itemID = idit.next();
      BusProperties lp = getLinkProperties(itemID);
      if (lp == null) {
        continue;
      }
      String srcTag = lp.getSourceTag();
      Set<String> forSrc = bySrc.get(srcTag);
      if (forSrc == null) {
        forSrc = new HashSet<String>();
        bySrc.put(srcTag, forSrc);       
      }
      forSrc.add(itemID);
    }
      
    Iterator<Set<String>> bsvit = bySrc.values().iterator();
    while (bsvit.hasNext()) {
      Set<String> forSrc = bsvit.next();
      BusProperties lp = getLinkProperties(forSrc.iterator().next());
      InvertedLinkProps ilp = new InvertedLinkProps(lp);
      lp.removeLotsaLinksSupport(forSrc, ilp);
    }
      
    idit = itemIds.iterator();
    while (idit.hasNext()) {
      String itemID = idit.next();
      linkProps_.remove(itemID);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Ditch all link properties.  This assumes undo is being handled by a
  ** separate full layout copy
  */
  
  public void dropAllLinkProperties() {
    linkProps_.clear();
    return;
  }

 /***************************************************************************
  **
  ** Drop link, node, group, and overlay properties.  Return info on mapping properties
  */
  
  public Map<String, BusProperties.RememberProps> dropPropertiesIncludingOverlays(StaticDataAccessContext rcx) {
    Map<String, BusProperties.RememberProps> retval = dropProperties(rcx);
    ovrProps_.clear();
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Drop link, node, and group properties.  Return info on mapping properties
  */
  
  public Map<String, BusProperties.RememberProps> dropProperties(StaticDataAccessContext rcx) {
    Map<String, BusProperties.RememberProps> retval = dropNodeAndLinkProperties(rcx);
    groupProps_.clear();
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Drop link, node, but NOT group properties
  */
  
  public Map<String, BusProperties.RememberProps> dropNodeAndLinkProperties(StaticDataAccessContext rcx) {
    Map<String, BusProperties.RememberProps> retval = buildRememberProps(rcx);
    nodeProps_.clear();
    linkProps_.clear();
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Build rememberProps
  */
  
  public Map<String, BusProperties.RememberProps> buildRememberProps(StaticDataAccessContext rcx) {
    HashMap<String, BusProperties.RememberProps> retval = new HashMap<String, BusProperties.RememberProps>();
    Iterator<String> lpkit = linkProps_.keySet().iterator();
    while (lpkit.hasNext()) {
      String lid = lpkit.next();
      BusProperties lp = linkProps_.get(lid);
      BusProperties.RememberProps rp = new BusProperties.RememberProps(lp, rcx);
      retval.put(lid, rp);
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Build rememberProps by inheriting from root
  */
  
  public Map<String, BusProperties.RememberProps> buildInheritedRememberProps(Map<String, BusProperties.RememberProps> rootProps, GenomeInstance gi) {
    HashMap<String, BusProperties.RememberProps> retval = new HashMap<String, BusProperties.RememberProps>();
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage lnk = glit.next();
      String lid = lnk.getID();
      BusProperties.RememberProps rp = rootProps.get(GenomeItemInstance.getBaseID(lid));
      rp = rp.buildInheritedProps(gi);
      retval.put(lid, rp);
    }    
    return (retval);
  }  

 /***************************************************************************
  **
  ** Prior to killing off links, remember associated special properties.
  */
  
  public Map<String, SpecialSegmentTracker> rememberSpecialLinks() {
    HashMap<String, SpecialSegmentTracker> retval = new HashMap<String, SpecialSegmentTracker>();
    HashSet<String> seen = new HashSet<String>();
    Iterator<BusProperties> lpkit = linkProps_.values().iterator();
    while (lpkit.hasNext()) {
      BusProperties bp = lpkit.next();
      String ref = bp.getSourceTag();
      if (!seen.contains(ref)) {
        seen.add(ref);
        SpecialSegmentTracker sst = new SpecialSegmentTracker();
        sst.analyzeSpecialLinks(bp);
        if (sst.hasSpecialLinks()) {
          retval.put(ref, sst);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build specialLinks by inheriting from root
  */
  
  public Map<String, SpecialSegmentTracker> buildInheritedSpecialLinks(Map<String, SpecialSegmentTracker> rootSpecials, GenomeInstance gi) {
    HashMap<String, SpecialSegmentTracker> retval = new HashMap<String, SpecialSegmentTracker>();
    HashSet<String> seen = new HashSet<String>();
    Iterator<BusProperties> lpkit = linkProps_.values().iterator();
    while (lpkit.hasNext()) {
      BusProperties lp = lpkit.next();
      String ref = lp.getSourceTag();
      if (!seen.contains(ref)) {
        seen.add(ref);
        NodeInstance ni = (NodeInstance)gi.getNode(ref);
        String baseID = GenomeItemInstance.getBaseID(ref);
        SpecialSegmentTracker sst = rootSpecials.get(baseID);
        if (sst != null) {
          SpecialSegmentTracker inheritSst = sst.buildInheritedSpecialLinks(lp, ni);
          retval.put(ref, inheritSst);
        }
      }
    }
    return (retval);
  }
      

 /***************************************************************************
  **
  ** Restore special links
  */
  
  public void restoreSpecialLinks(Map<String, SpecialSegmentTracker> guide) {
    HashSet<String> seen = new HashSet<String>();
    Iterator<BusProperties> lpkit = linkProps_.values().iterator();
    while (lpkit.hasNext()) {
      BusProperties bp = lpkit.next();
      String ref = bp.getSourceTag();
      if (!seen.contains(ref)) {
        seen.add(ref);
        SpecialSegmentTracker sst = guide.get(ref);
        if (sst != null) {
          sst.reapplySpecialLinks(bp);
        }
      }
    }
    return;
  }  
  
 /***************************************************************************
  **
  ** Restore special links
  */
  
  public void restoreLabelLocations(Map<String, BusProperties.RememberProps> rememberProps, StaticDataAccessContext rcx, Rectangle2D rect) {
    HashSet<String> seen = new HashSet<String>();
    Iterator<String> lpkit = linkProps_.keySet().iterator();
    while (lpkit.hasNext()) {
      String key = lpkit.next();
      BusProperties bp = linkProps_.get(key);
      String ref = bp.getSourceTag();
      if (!seen.contains(ref)) {
        seen.add(ref);
        BusProperties.RememberProps rp = rememberProps.get(key);
        rp.restoreLabelPosition(bp, rcx, rect);
      }
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Ditch selected link properties.  This assumes undo is being handled by a
  ** separate full layout copy. If set is null, remove all link properties.
  */
  
  public Map<String, BusProperties.RememberProps> dropSelectedLinkProperties(Set<String> toDrop, StaticDataAccessContext rcx) {
    Map<String, BusProperties.RememberProps> retval;
    if (toDrop == null) {
      retval = buildRememberProps(rcx);
      dropAllLinkProperties();
      return (retval);
    }

    retval = new HashMap<String, BusProperties.RememberProps>();
    Iterator<String> tdit = toDrop.iterator();
    while (tdit.hasNext()) {
      String linkID = tdit.next();
      BusProperties lp = linkProps_.get(linkID);
      if (linkProps_.get(linkID) != null) {
        BusProperties.RememberProps rp = new BusProperties.RememberProps(lp, rcx);
        retval.put(linkID, rp);        
        linkProps_.remove(linkID);
      }
    }
    return (retval);
  }   

  /***************************************************************************
  **
  ** Drop useless corners
  */
  
  public void dropUselessCorners(StaticDataAccessContext rcx, String overID, 
                                 double startFrac, double maxFrac, BTProgressMonitor monitor) 
                                 throws AsynchExitRequestException {
    LinkOptimizer opt = new LinkOptimizer();
    
    List<LinkProperties> toProcess = listOfProps(rcx.getCurrentGenome(), overID, null);
    double progFrac = (maxFrac - startFrac) / toProcess.size();
    double currFrac = startFrac;   
       
    int numTP = toProcess.size();   
    for (int i = 0; i < numTP; i++) {
      LinkProperties lp = toProcess.get(i);
      opt.eliminateUselessCornersGridless(lp, rcx, overID, currFrac, currFrac + progFrac, monitor);
      currFrac += progFrac;
      if (monitor != null) {
        boolean keepGoing = monitor.updateProgress((int)(currFrac * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
    }
    return;
  }     

  /***************************************************************************
  **
  ** Helper to generate a list of LinkProperties 
  */
  
  public List<LinkProperties> listOfProps(Genome genome, String overID, Set<String> onlyLinks) {
    
    ArrayList<LinkProperties> toProcess = new ArrayList<LinkProperties>();   
    if (overID == null) {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      HashSet<LinkProperties> seen = new HashSet<LinkProperties>();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String linkID = link.getID();
        BusProperties lprops = getLinkProperties(linkID);
        if (lprops == null) {
          continue;
        }
        if (seen.contains(lprops) || ((onlyLinks != null) && !onlyLinks.contains(linkID))) {
          continue;
        }
        
        toProcess.add(lprops);
        seen.add(lprops);
      }
    } else {
      NetOverlayProperties nop = getNetOverlayProperties(overID);   
      Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
      while (lmpit.hasNext()) {
        String lkey = lmpit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
        toProcess.add(nmlp);
      }     
    }
    return (toProcess);
  }
  
  
  /***************************************************************************
  **
  ** Smooth out staggered runs
  */
  
  public void eliminateStaggeredRuns(int staggerBound, StaticDataAccessContext rcx,
                                     String overID, Set<Point2D> pinnedPoints,
                                     double startFrac, double maxFrac, BTProgressMonitor monitor) 
                                     throws AsynchExitRequestException {
    LinkRouter router = new LinkRouter();    
    LinkPlacementGrid grid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor);
    LinkOptimizer opt = new LinkOptimizer(); 
    
    Genome genome = rcx.getCurrentGenome();
    List<LinkProperties> toProcess = listOfProps(genome, overID, null);
    double progFrac = (maxFrac - startFrac) / toProcess.size();
    double currFrac = startFrac;    
 
    int numTP = toProcess.size();   
    for (int i = 0; i < numTP; i++) {
      LinkProperties lp = toProcess.get(i);
      opt.eliminateStaggeredRuns(lp, grid, staggerBound, rcx, overID, 
                                 pinnedPoints, currFrac, currFrac + progFrac, monitor);
      currFrac += progFrac;
      if (monitor != null) {
        boolean keepGoing = monitor.updateProgress((int)(currFrac * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
    }
    return;
  }      

  /***************************************************************************
  **
  ** Optimize Links
  */
  
  public void optimizeLinks(Set<String> newLinks, LinkPlacementGrid grid, StaticDataAccessContext rcx,
                            String overID,
                            Set<Point2D> pinnedPoints, boolean allowReroutes, BTProgressMonitor monitor,
                            int rounds, double startFrac, double maxFrac) throws AsynchExitRequestException {
    if (rounds == 0) {
      return;
    }
    
    Genome genome = rcx.getCurrentGenome();
    LinkOptimizer opt = new LinkOptimizer();
    int[] opts = opt.getOptimizations(allowReroutes);
    
    double[] perLink = new double[opts.length];
    double currProg = startFrac;
    
    double midFrac = startFrac + ((maxFrac - startFrac) * 0.95);
    
    List<LinkProperties> toProcess = listOfProps(genome, overID, newLinks);
    int linkCount = toProcess.size();
    
    if (monitor != null) {
      double progFrac = midFrac - startFrac;
      //
      // Figure out progress for each link
      //      
      
      for (int i = 0; i < opts.length; i++) {
        if (linkCount > 0) {
          perLink[i] = (progFrac * opt.relativeDifficulty(i, allowReroutes)) / 
                          ((double)opts.length * (double)linkCount * rounds);
        } else {
          perLink[i] = progFrac;
        }
      }
    }
  
    for (int j = 0; j < rounds; j++) {
      for (int i = 0; i < opts.length; i++) { 
        for (int k = 0; k < linkCount; k++) {
          LinkProperties lp = toProcess.get(k);
          double nextProg = currProg + perLink[i];
          opt.optimizeLinks(i, lp, grid, rcx, overID, pinnedPoints, currProg, nextProg, monitor);
          currProg = nextProg;
          if (monitor != null) {
            boolean keepGoing = monitor.updateProgress((int)(currProg * 100.0));
            if (!keepGoing) {
              throw new AsynchExitRequestException();
            }
          }
        }
      }
    }
    
    //
    // After all the passes are finished, one last chance to ditch useless stuff:
    //
    
    dropUselessCorners(rcx, overID, midFrac, maxFrac, monitor);
    
    return;
  }   
  
  /***************************************************************************
  **
  ** Add a group property to the Layout
  */
  
  public PropChange setGroupProperties(String itemId, GroupProperties props) {
    PropChange retval = new PropChange();
    retval.grOrig = null;
    retval.grNewProps = props;
    retval.layoutKey = name_;
    groupProps_.put(itemId, props);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Remove a group property from the Layout
  */
  
  public PropChange removeGroupProperties(String itemId) {
    PropChange retval = new PropChange();
    retval.grOrig = groupProps_.remove(itemId);
    retval.grNewProps = null;
    retval.layoutKey = name_;

    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Add a note property to the Layout
  */
  
  public PropChange setNoteProperties(String itemId, NoteProperties props) {
    PropChange retval = new PropChange();
    retval.ntOrig = null;
    retval.ntNewProps = props;
    retval.layoutKey = name_;
    noteProps_.put(itemId, props);
    return (retval);
  }  
  
  
  /***************************************************************************
  **
  ** Change/add keys for note properties (for GenomeInstance copy support)
  */
  
  public PropChange[] mapNoteProperties(Map<String, String> keyMap, boolean doAdd) {
    
    ArrayList<PropChange> pcs = new ArrayList<PropChange>();
    Iterator<String> kit = keyMap.keySet().iterator();
    while (kit.hasNext()) {
      String oldID = kit.next();
      String newID = keyMap.get(oldID);
      NoteProperties oldProps = noteProps_.get(oldID);
      NoteProperties newProps = new NoteProperties(oldProps, newID);
      PropChange pc = new PropChange();
      if (doAdd) {
        pc.ntOrig = null;
        pc.ntNewProps = newProps.clone();
        pc.layoutKey = name_;
        noteProps_.put(newID, newProps);
      } else { // replace
        pc.ntOrig = oldProps.clone();
        pc.ntNewProps = newProps.clone();
        pc.layoutKey = name_;
        noteProps_.remove(oldID);
        noteProps_.put(newID, newProps);        
      }
      pcs.add(pc);
    }
    PropChange[] retval = new PropChange[pcs.size()];
    pcs.toArray(retval);
    return (retval);
  }   
  
  /***************************************************************************
  **
  ** Change/add keys for overlay properties (for GenomeInstance copy support)
  */
  
  public PropChange[] mapOverlayProperties(Map<String, String> ovrIDMap, Map<String, String> modIDMap, 
                                           Map<String, String> modLinkIDMap, boolean doAdd, StaticDataAccessContext rcx) {
    
    ArrayList<PropChange> pcs = new ArrayList<PropChange>();
    Iterator<String> oit = ovrIDMap.keySet().iterator();
    while (oit.hasNext()) {
      String oldID = oit.next();
      String newID = ovrIDMap.get(oldID);
      NetOverlayProperties oldProps = ovrProps_.get(oldID);
      NetOverlayProperties newProps = new NetOverlayProperties(rcx.getGenomeSource(), oldProps, newID, modIDMap, modLinkIDMap, false);
      PropChange pc = new PropChange();
      if (doAdd) {
        pc.nopOrig = null;
        pc.nopNew = newProps.clone();
        pc.layoutKey = name_;
        ovrProps_.put(newID, newProps);
      } else { // replace
        pc.nopOrig = oldProps.clone();
        pc.nopNew = newProps.clone();
        pc.layoutKey = name_;
        ovrProps_.remove(oldID);
        ovrProps_.put(newID, newProps);        
      }
      pcs.add(pc);
    }
    PropChange[] retval = new PropChange[pcs.size()];
    pcs.toArray(retval);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Remove a note properties
  */
  
  public PropChange removeNoteProperties(String itemId) {
    PropChange retval = new PropChange();    
    retval.ntOrig = noteProps_.get(itemId);
    retval.layoutKey = name_;
    retval.ntNewProps = null;
    noteProps_.remove(itemId);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** get the set of keys for note properties (needed for fixup)
  */
  
  public Set<String> getNotePropertiesKeys() {
    return (noteProps_.keySet());
  }    

  /***************************************************************************
  **
  ** Add a data location to the Layout
  */
  
  public PropChange setDataLocation(String key, Point2D loc) {
    Point2D currLoc = getDataLocation(key);    
    PropChange retval = new PropChange();
    retval.dLocKey = key;    
    retval.dLocOrig = (currLoc != null) ? (Point2D)currLoc.clone() : null;
    retval.layoutKey = name_;
    Point2D newLoc = (Point2D)loc.clone();
    dataProps_.put(key, newLoc);
    retval.dLocNew = (Point2D)newLoc.clone();
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a data location from the Layout
  */
  
  public Point2D getDataLocation(String key) {
    return (dataProps_.get(key));
  }  
  
  /***************************************************************************
  **
  ** Give the layout the ID of the GenomeItem, and we return a NodeProperties
  ** that gives the color and location(s) of the item.
  */
  
  public NodeProperties getNodeProperties(String itemId) {
    return (nodeProps_.get(itemId));
  }
  
  /***************************************************************************
  **
  ** Give the layout the ID of the GenomeItem, and we return a LayoutProperties
  ** that gives the color and location(s) of the item.
  */
  
  public BusProperties getLinkProperties(String itemId) {
    return (linkProps_.get(itemId));
  }  
  
  /***************************************************************************
  **
  ** Give the layout the ID of the Group, and we return a GroupProperties
  ** that gives the color and location(s) of the item.
  */
  
  public GroupProperties getGroupProperties(String groupId) {
    GroupProperties gp = groupProps_.get(groupId);
    return (gp);
  }
  
  /***************************************************************************
  **
  ** Give the layout the ID of the NetOverlay, and we return a NetOverlayProperties
  ** for it.
  */

  public NetOverlayProperties getNetOverlayProperties(String ovrId) {
    return (ovrProps_.get(ovrId));
  }
  
  /***************************************************************************
  **
  ** Add a net module property to the Layout
  */
  
  public PropChange setNetOverlayProperties(String itemId, NetOverlayProperties props) {
    PropChange retval = new PropChange();
    retval.nopOrig = null;
    retval.nopNew = props.clone();
    retval.layoutKey = name_;
    ovrProps_.put(itemId, props);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Replace a net module property in the Layout
  */
  
  public PropChange replaceNetOverlayProperties(String itemId, NetOverlayProperties props) {
    PropChange retval = new PropChange();
    retval.nopOrig = ovrProps_.get(itemId).clone();
    retval.nopNew = props.clone();
    retval.layoutKey = name_;
    ovrProps_.put(itemId, props);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Remove a net overlay properties from the Layout
  */
  
  public PropChange removeNetOverlayProperties(String itemId) {
    PropChange retval = new PropChange();
    retval.nopOrig = ovrProps_.remove(itemId).clone();
    retval.nopNew = null;
    retval.layoutKey = name_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a net overlay property change
  */
  
  public void overlayChangeUndo(PropChange undo) {    
    if ((undo.nopOrig != null) && (undo.nopNew != null)) { 
      ovrProps_.put(undo.nopOrig.getReference(), undo.nopOrig.clone());     
    } else if (undo.nopNew != null) {
      ovrProps_.remove(undo.nopNew.getReference());      
    } else if (undo.nopOrig != null) {
      ovrProps_.put(undo.nopOrig.getReference(), undo.nopOrig.clone());      
    } else {
      throw new IllegalArgumentException();
    }
    if (undo.droppedTreeID != null) {
      ((DBGenome)undo.gSrc.getRootDBGenome()).addKey(undo.droppedTreeID);
    }  
    return;
  }       
     
  /***************************************************************************
  **
  ** Redo a net overlay property change
  */
  
  public void overlayChangeRedo(PropChange redo) {
    if ((redo.nopOrig != null) && (redo.nopNew != null)) {     
      ovrProps_.put(redo.nopNew.getReference(), redo.nopNew.clone());     
    } else if (redo.nopNew != null) {
      ovrProps_.put(redo.nopNew.getReference(), redo.nopNew.clone());            
    } else if (redo.nopOrig != null) {
      ovrProps_.remove(redo.nopOrig.getReference());
    } else {
      throw new IllegalArgumentException();
    }
    if (redo.droppedTreeID != null) {
      ((DBGenome)redo.gSrc.getRootDBGenome()).removeKey(redo.droppedTreeID);
    }   
    return;
  }      

  
  /***************************************************************************
  **
  ** Convenience function, using the TREE ID! 
  */
  
  public NetModuleLinkageProperties getNetModuleLinkagePropertiesFromTreeID(String treeId, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeId);
    return (nmlp);
  }
  
  /***************************************************************************
  **
  ** Add a net module linkage property to the Layout, using the TREE ID!  Caller
  ** must have already registered the new tree Object ID.
  */
  
  public PropChange setNetModuleLinkageProperties(String treeId, NetModuleLinkageProperties props, 
                                                  String ovrRef, StaticDataAccessContext rcx) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.nmlpOrig = null;
    // Return of treeID occurs by extraction from the nmlpNew object, not by
    // the addedTreeID field, since this is handled by the NMLP undo/redo handler!
    retval.nmlpNew = props.clone();
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    retval.gSrc = rcx.getGenomeSource();
    nop.setNetModuleLinkageProperties(treeId, props);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a net module linkage property to the Layout, using the TREE ID!  Caller
  ** must have already registered the new tree Object ID.
  */
  
  public void setNetModuleLinkagePropertiesForIO(String treeId, NetModuleLinkageProperties props, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    nop.setNetModuleLinkageProperties(treeId, props);
    return;
  }

  /***************************************************************************
  **
  ** Replace a net module linkage property to the Layout, using the TREE ID!
  */
  
  public PropChange replaceNetModuleLinkageProperties(String treeId, NetModuleLinkageProperties props, String ovrRef, StaticDataAccessContext rcx) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeId);
    PropChange retval = new PropChange();
    retval.nmlpOrig = nmlp.clone();
    retval.nmlpNew = props.clone();
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    retval.gSrc = rcx.getGenomeSource();
    nop.setNetModuleLinkageProperties(treeId, props);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Remove net module property support for the given link from the layout
  */
  
  public PropChange removeNetModuleLinkageProperties(String linkID, String ovrRef, DataAccessContext rcx) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    //
    // We need to ref the overlay properties, since it may change too:
    //
    retval.nopOrig = nop.clone();
    retval.droppedTreeID = nop.removeNetModuleLinkageProperties(rcx.getGenomeSource(), linkID);
    retval.gSrc = rcx.getGenomeSource();
    retval.nopNew = nop.clone();
    return (retval);
  }  

  /***************************************************************************
  **
  ** Tie a module link to a link tree, using the TREE ID!
  */
  
  public PropChange tieNetModuleLinkToProperties(String linkId, String treeId, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.nmlpTieLinkIDOrig = linkId;
    retval.nmlpTieTreeIDOrig = nop.getNetModuleLinkagePropertiesID(linkId);
    retval.nmlpTieLinkIDNew = linkId;
    retval.nmlpTieTreeIDNew = treeId;
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    nop.tieNetModuleLinkagePropertiesForLink(linkId, treeId);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge the (newly built only!) single link into the link tree at the given segment.
  */
  
  public PropChange mergeNewModuleLinkToTreeAtSegment(NetModuleLinkageProperties newNmlp,
                                                      String treeID, String linkId, String ovrRef,
                                                      LinkSegmentID sid, StaticDataAccessContext rcx) {
 
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    NetModuleLinkageProperties currNmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
    undoPreProcess(retval, currNmlp, ovrRef, rcx);
    currNmlp.mergeSingleToTreeAtSegment(newNmlp, rcx, sid);
    
    //
    // Tie the new link to the combined tree:
    //
    
    retval.nmlpTieLinkIDOrig = linkId;
    retval.nmlpTieTreeIDOrig = nop.getNetModuleLinkagePropertiesID(linkId);
    retval.nmlpTieLinkIDNew = linkId;
    retval.nmlpTieTreeIDNew = treeID;    
    nop.tieNetModuleLinkagePropertiesForLink(linkId, treeID);
    undoPostProcess(retval, currNmlp, ovrRef, rcx);
    return (retval);   
  }  
  

  /***************************************************************************
  **
  ** Add a net module property to the Layout
  */
  
  public PropChange setNetModuleProperties(String itemId, NetModuleProperties props, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.nmpOrig = null;
    retval.nmpNew = props.clone();
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    nop.setNetModuleProperties(itemId, props);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace a net module property in the Layout
  */
  
  public PropChange replaceNetModuleProperties(String itemId, NetModuleProperties props, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.nmpOrig = nop.getNetModuleProperties(itemId).clone();
    retval.nmpNew = props.clone();
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    nop.setNetModuleProperties(itemId, props);
    return (retval);
  }     
  
  /***************************************************************************
  **
  ** Remove a net module property from the Layout
  */
  
  public PropChange removeNetModuleProperties(String itemId, String ovrRef) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrRef);
    PropChange retval = new PropChange();
    retval.nmpOrig = nop.getNetModuleProperties(itemId).clone();
    retval.nmpNew = null;
    retval.layoutKey = name_;
    retval.nopRef = ovrRef;
    nop.removeNetModuleProperties(itemId);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** For a contig rect type, size the region core to match the current boundary
  */
  
  public PropChange sizeCoreToRegionBounds(String ovrKey, String modKey, StaticDataAccessContext rcx) {
    
    String genomeKey = rcx.getCurrentGenomeID();
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);   
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);   
    if (nmp.getType() != NetModuleProperties.CONTIG_RECT) {
      return (null);
    }
    NetModuleProperties modProps = nmp.clone();
    if (!modProps.hasOneShape()) {
      throw new IllegalStateException();
    }   
    Rectangle2D outerRect = new Rectangle2D.Double();
    Genome ownerGenome = rcx.getGenomeSource().getGenome(genomeKey);
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    Genome useGenome = Layout.determineLayoutTarget(rcx.getGenomeSource(), genomeKey);
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    
    Set<String> useGroups = irx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey).getGroupsForOverlayRendering();
    modProps.getRenderer().getModuleShapes(ownerGenome, useGroups, ovrKey, modKey, outerRect, irx);
    
    Rectangle2D oldRect = modProps.getShapeIterator().next();
    modProps.replaceShape(oldRect, outerRect);
    PropChange pc = replaceNetModuleProperties(modKey, modProps, ovrKey);
    return (pc);
  }  
  
  /***************************************************************************
  **
  ** For a contig rect type, size the region core to match given bounds
  */
  
  public PropChange sizeCoreToGivenBounds(String ovrKey, String modKey, Rectangle2D newRect) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);   
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);   
    if (nmp.getType() != NetModuleProperties.CONTIG_RECT) {
      return (null);
    }
    NetModuleProperties modProps = nmp.clone();
    if (!modProps.hasOneShape()) {
      throw new IllegalStateException();
    }
    Rectangle2D oldRect = modProps.getShapeIterator().next();
    
    //
    // With lots of possible module shifting going on, we want to maintain the
    // relative position of the module label inside the module:
    //
    
    Point2D oldLabelLoc = modProps.getNameLoc();    
    if (oldLabelLoc != null) {
      Point2D[] points = new Point2D[3];
      points[0] = new Point2D.Double(oldRect.getMinX(), oldRect.getMinY());
      points[1] = new Point2D.Double(oldRect.getMaxX(), oldRect.getMinY());
      points[2] = new Point2D.Double(oldRect.getMaxX(), oldRect.getMaxY());
      double[] weights = new double[3];
      boolean spans = AffineCombination.getWeights(oldLabelLoc, points, weights);

      Point2D newLabelLoc;
      if (spans) {
        List<Point2D> pointList = new ArrayList<Point2D>();
        pointList.add(new Point2D.Double(newRect.getMinX(), newRect.getMinY()));
        pointList.add(new Point2D.Double(newRect.getMaxX(), newRect.getMinY()));
        pointList.add(new Point2D.Double(newRect.getMaxX(), newRect.getMaxY()));
        newLabelLoc = AffineCombination.combination(pointList, weights, UiUtil.GRID_SIZE);
      } else {
        newLabelLoc = (Point2D)oldLabelLoc.clone();
      } 
      modProps.setNameLocation(newLabelLoc);
    }
    modProps.replaceShape(oldRect, newRect);
    PropChange pc = replaceNetModuleProperties(modKey, modProps, ovrKey);
    return (pc);
  }
  
  /***************************************************************************
  **
  ** Given a mapping of old to new point locations, shift all net module links
  ** in the overlay to match:
  */
  
  public PropChange[] shiftModLinksToNewPositions(String ovrKey, Map<Point2D, Point2D> mappedPositions, boolean forceToGrid, StaticDataAccessContext irx) {
    NetOverlayProperties nop = getNetOverlayProperties(ovrKey);   
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    Iterator<String> trit = nop.getNetModuleLinkagePropertiesKeys(); 
    while (trit.hasNext()) {
      String treeID = trit.next();
      PropChange retval = new PropChange(); 
      NetModuleLinkageProperties currNmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      undoPreProcess(retval, currNmlp, ovrKey, irx);  
      currNmlp.shiftPerMap(mappedPositions, forceToGrid);
      undoPostProcess(retval, currNmlp, ovrKey, irx);
    }
    
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));      
  }
  
  /***************************************************************************
  **
  ** For a contig rect type, answer of the size of the region core matches the current boundary
  */
  
  public boolean coreDefDoesNotMatchVisible(String ovrKey, String modKey, DataAccessContext rcx) {
    String genomeKey = rcx.getCurrentGenomeID();
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);   
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);   
    if (nmp.getType() != NetModuleProperties.CONTIG_RECT) {
      return (true);
    }
    NetModuleProperties modProps = nmp.clone();
    if (!modProps.hasOneShape()) {
      throw new IllegalStateException();
    }   
    Rectangle2D outerRect = new Rectangle2D.Double();
    Genome ownerGenome = rcx.getGenomeSource().getGenome(genomeKey);
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    Genome useGenome = Layout.determineLayoutTarget(rcx.getGenomeSource(), genomeKey);
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    
    Set<String> useGroups = irx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey).getGroupsForOverlayRendering();
    modProps.getRenderer().getModuleShapes(ownerGenome, useGroups, ovrKey, modKey, outerRect, irx);
    
    Rectangle2D oldRect = modProps.getShapeIterator().next();
    return (!oldRect.equals(outerRect));
  }    
  
  /***************************************************************************
  **
  ** Move the net module name by the given amount  Link drop fixups are done
  ** separately.
  */
  
  public PropChange[] moveNetModuleName(Intersection modIntersect, String ovrKey, double dx, double dy) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    String modKey = modIntersect.getObjectID(); 
   
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);
    NetModuleProperties modProps = nmp.clone();
    modProps.moveName(dx, dy);
    PropChange pc = replaceNetModuleProperties(modKey, modProps, ovrKey);
    PropChange[] retval = new PropChange[1];
    retval[0] = pc;
    return (retval);
  }   

  
  /***************************************************************************
  **
  ** Moving nodes may require module pad fixes.  Do the grunt work.
  ** FIXME? Might want to make this occur on only a per-overlay basis, since the
  ** only granularity at the moment is to skip entirely by making the padReqs null!
  */
  
  private void rigidPadFixes(String nodeID, double dx, double dy, Layout.PadNeedsForLayout padReqs, StaticDataAccessContext rcx) {
    
    if (padReqs == null) {
      return;
    }
    GenomeSource gSrc = rcx.getGenomeSource();
    Vector2D moveVec = new Vector2D(dx, dy);
    Iterator<NetModule.FullModuleKey> kit = padReqs.keyIterator();
    while (kit.hasNext()) {
      NetModule.FullModuleKey fullKey = kit.next();
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      // See above comment; could check per-overlay here and maybe skip....
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
      if ((nmp.getType() == NetModuleProperties.MEMBERS_ONLY) || 
          (nmp.getType() == NetModuleProperties.MULTI_RECT)) {      
        NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
        NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
        NetModule mod = no.getModule(fullKey.modKey);
        if (mod.isAMember(nodeID)) {
          NetModuleLinkPadRequirements lpreq = padReqs.getPadRequire(fullKey);
          Iterator<PointNoPoint> pnpit = lpreq.linkPadInfo.keySet().iterator();
          while (pnpit.hasNext()) {
            PointNoPoint pt = pnpit.next();
            NetModuleFree.LinkPad lp = lpreq.linkPadInfo.get(pt);
            if (nodeID.equals(lp.nodeID)) {
              Vector2D prevVec = lpreq.rigidMoves.get(pt);
              if (prevVec != null) {
                Vector2D sumVec = prevVec.add(moveVec);
                lpreq.rigidMoves.put(pt, sumVec); 
              } else {
                lpreq.rigidMoves.put(pt, moveVec);
              }
            }
          }
        }  
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Move the net module region by the given amount.  Link drop fixups are done
  ** separately
  */
  
  public PropChange[] moveNetModule(String genomeKey, Intersection modIntersect, String ovrKey, 
                                    double dx, double dy, Layout.PadNeedsForLayout padReqs, StaticDataAccessContext rcx) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    String nmpKey = modIntersect.getObjectID();
    NetModuleProperties nmp = noProps.getNetModuleProperties(nmpKey);
    NetModuleProperties modProps = nmp.clone();
    NetModuleFree.IntersectionExtraInfo iexi = (NetModuleFree.IntersectionExtraInfo)modIntersect.getSubID();

    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(genomeKey); 
    
    //
    // Any associated links drops need to be moved as well!
    //
     
    int ownerMode = noo.overlayModeForOwner();
    NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerMode, noo.getID(), ovrKey, nmpKey);
    NetModuleLinkPadRequirements lpreq = padReqs.getPadRequire(fullKey);
    HashSet<PointNoPoint> padNeeds = new HashSet<PointNoPoint>(lpreq.linkPadInfo.keySet());    
    
    //
    // Now move the shape:
    //
        
    lpreq.rigidMoves = modProps.moveShape(iexi, dx, dy, padNeeds);   
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    changes.add(replaceNetModuleProperties(nmpKey, modProps, ovrKey));
    
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));            
  } 
 

  /***************************************************************************
  **
  ** Current state of pads for pad needs.
  **
  */
  
  public NetModuleLinkPadRequirements getCurrentNetModuleLinkPadState(NetModule.FullModuleKey fullKey,
                                                                      NetModuleLinkPadRequirements needPads) {
    
 
    NetModuleLinkPadRequirements currPads = new NetModuleLinkPadRequirements();   
    NetOverlayProperties noProps = getNetOverlayProperties(fullKey.ovrKey);
   
    //
    // Go though the original points and map them to the current locations:
    //

    HashMap<PointNoPoint, PointNoPoint> oldToNew = new HashMap<PointNoPoint, PointNoPoint>();
    Iterator<String> tidit = needPads.treeIDToStarts.keySet().iterator();
    while (tidit.hasNext()) {
      String treeID = tidit.next();
      PointNoPoint neededPnp = needPads.treeIDToStarts.get(treeID);    
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
      // I think this could get called on non-existent links after a node region move.  So if
      // no props, do nothing!
      if (nmlp == null) {
        continue;
      }
      PointNoPoint currPnp = neededPnp.clone(); 
      currPnp.point = nmlp.getSourceStart(0.0);
      currPads.treeIDToStarts.put(treeID, currPnp);
      oldToNew.put(neededPnp, currPnp);
    }    
    
    
    Iterator<String> lidit = needPads.linkIDToEnds.keySet().iterator();
    while (lidit.hasNext()) {
      String linkID = lidit.next();
      PointNoPoint neededPnp = needPads.linkIDToEnds.get(linkID);    
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkageProperties(linkID);
      // I think this could get called on non-existent links after a node region move.  So if
      // no props, do nothing!
      if (nmlp == null) {
        continue;
      }
      PointNoPoint currPnp = neededPnp.clone(); 
      currPnp.point = nmlp.getTargetEnd(linkID, 0.0);
      if (neededPnp.noPoint != null) {
        currPnp.noPoint = nmlp.getSourceStart(0.0);
      }
      currPads.linkIDToEnds.put(linkID, currPnp);
      oldToNew.put(neededPnp, currPnp);
    }    
    
    Iterator<PointNoPoint> lpit = needPads.linkPadInfo.keySet().iterator();
    while (lpit.hasNext()) {
      PointNoPoint nextPoint = lpit.next();
      NetModuleFree.LinkPad padData = needPads.linkPadInfo.get(nextPoint);
      PointNoPoint currNextPoint = oldToNew.get(nextPoint);
      // Side effect of above skips
      if (currNextPoint == null) {
        continue;
      }
      NetModuleFree.LinkPad currPadData = padData.clone();
      currPadData.point = (Point2D)currNextPoint.point.clone();
      // 3/5/12 Fix here, need to map to ORIGINAL pad need, not new one, for
      // this ti work correctly.  Related to BT-03-01-12:1
      currPads.linkPadInfo.put(nextPoint, currPadData);
      // WRONG! currPads.linkPadInfo.put(currNextPoint, currPadData);
    }
    
    return (currPads);
  }
  
  /***************************************************************************
  **
  ** Answer if we have link pad usage collisions in the overlay:
  */
  
  public boolean netModuleLinksPadCollisions(String ovrKey) {
    
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);   
    HashSet<Point2D> globalPoints = new HashSet<Point2D>();
    
    Iterator<String> tidit = noProps.getNetModuleLinkagePropertiesKeys();
    while (tidit.hasNext()) {
      String treeID = tidit.next();
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
      Point2D ss = nmlp.getSourceStart(0.0);
      if (globalPoints.contains(ss)) {
        return (true);
      }
      globalPoints.add(ss);
      Iterator<LinkBusDrop> drit = nmlp.getDrops();
      while (drit.hasNext()) {
        NetModuleBusDrop drop = (NetModuleBusDrop)drit.next();
        if (drop.getDropType() == LinkBusDrop.END_DROP) {
          Point2D de = drop.getEnd(0.0);
          if (globalPoints.contains(de)) {
            return (true);
          }
          globalPoints.add(de);
        }
      }
    }
    return (false);
  }
 
  /***************************************************************************
  **
  ** Find pad needs for net module links
  */
  
  public NetModuleLinkPadRequirements findNetModuleLinkPadRequirements(String ownerKey, 
                                                                       String ovrKey, 
                                                                       String modKey,
                                                                       StaticDataAccessContext rcx) {
    
    NetModuleLinkPadRequirements retval = new NetModuleLinkPadRequirements();
    
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);
  
    
    //
    // Find the inbound and outbound links:
    //
    
    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerWithOwnerKey(ownerKey);
    NetworkOverlay no = noo.getNetworkOverlay(ovrKey);
    HashSet<String> inLinks = new HashSet<String>();
    HashSet<String> outLinks = new HashSet<String>();
    if (no == null) {
      System.err.println("No no for " + ovrKey + " " + ownerKey);
    }
    no.getNetModuleLinkagesForModule(modKey, inLinks, outLinks);
    if (inLinks.isEmpty() && outLinks.isEmpty()) {
      return (retval);
    }
    
    //
    // Find out the pads we are using:
    //
     
    retval.feedBacks = new HashSet<String>(inLinks);
    retval.feedBacks.retainAll(outLinks);
   
    HashSet<String> treeIDs = new HashSet<String>();
    Iterator<String> olit = outLinks.iterator();
    while (olit.hasNext()) {
      String linkID = olit.next();
      treeIDs.add(noProps.getNetModuleLinkagePropertiesID(linkID));
    }   
    Iterator<String> tidit = treeIDs.iterator();
    while (tidit.hasNext()) {
      String treeID = tidit.next();
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
      PointNoPoint pnp = new PointNoPoint(nmlp.getSourceStart(0.0), null, null);
      retval.treeIDToStarts.put(treeID, pnp);
    }
    
    Iterator<String> ilit = inLinks.iterator();
    while (ilit.hasNext()) {
      String linkID = ilit.next();
      NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkageProperties(linkID);
      Point2D te = nmlp.getTargetEnd(linkID, 0.0);
      Point2D noPoint = null;
      if (retval.feedBacks.contains(linkID)) {
        noPoint = nmlp.getSourceStart(0.0);
      }
      PointNoPoint pnp = new PointNoPoint(te, noPoint, null);
      retval.linkIDToEnds.put(linkID, pnp);
    } 

    
    NetModule mod = no.getModule(modKey);
    // Rendering requirements can be based upon the rootInstance geometry and membership
    Genome useGenome = rcx.getGenomeSource().getGenome(targetGenome_);
    DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo : null;
    Set<String> useGroups = noo.getGroupsForOverlayRendering();
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    irx.pushFrc(new FontRenderContext(null, true, true));
    Set<NetModuleFree.LinkPad> padsForModule = nmp.getRenderer().padsForModule(dip, useGroups, mod, nmp, irx);
    
    //
    // FIX ME Kludge!
    // Links don't actually know which region of a module they are pointing to.
    // If there is only one node in the region (member only type) they should
    // keep tracking it.  But if two are laid on top of each other, when separated
    // the link source/target region assignments could change!  Need to stash
    // this info in he link drop!
    
    HashMap<Point2D, List<NetModuleFree.LinkPad>> ptToPadLists = new HashMap<Point2D, List<NetModuleFree.LinkPad>>();  
    Iterator<NetModuleFree.LinkPad> pfmit = padsForModule.iterator();
    while (pfmit.hasNext()) {
      NetModuleFree.LinkPad padData = pfmit.next();
      List<NetModuleFree.LinkPad> padList = ptToPadLists.get(padData.point);
      if (padList == null) {
        padList = new ArrayList<NetModuleFree.LinkPad>();
        ptToPadLists.put(padData.point, padList);       
      }
      padList.add(padData);
    }
    

    // Note these guys might be modified by ID needs, so we use a list:
    ArrayList<PointNoPoint> allPoints = new ArrayList<PointNoPoint>();
    allPoints.addAll(retval.treeIDToStarts.values());
    allPoints.addAll(retval.linkIDToEnds.values());     
    Iterator<PointNoPoint> apit = allPoints.iterator();
    while (apit.hasNext()) {
      PointNoPoint nextPoint = apit.next();
      List<NetModuleFree.LinkPad> padList = ptToPadLists.get(nextPoint.point);
      NetModuleFree.LinkPad padData;
      if ((padList == null) || padList.isEmpty()) {
        //
        // One would think this would not happen...but it does!  At extreme wide
        // zoom, we see where the official pad end does NOT match what the renderer
        // says is available.  Could be a font rendering issue?  Anyway, if this
        // really happens, find the CLOSEST that matches, and use it instead!
        // Using an identity transform FRC deal with zoom issue, but having a
        // backup like this is a good way to handle points that get detached via
        // bugs.....
        //
        padData = emergencyClosestExistingPad(ptToPadLists, nextPoint.point);
      } else {
        padData = padList.get(0);
        if (padList.size() > 1) {
          padList.remove(0);
        }
      }
      if (padData == null) {
        continue;  // should not happen...
      }
      if (padData.nodeID != null) {
        nextPoint.nodeID = padData.nodeID;
      }
      retval.linkPadInfo.put(nextPoint, padData);
      // Drain pads unless there is only one left...

    }
 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle weirdo mismatch case
  */
  
  private NetModuleFree.LinkPad emergencyClosestExistingPad(Map<Point2D, List<NetModuleFree.LinkPad>> ptToPadLists, Point2D myPoint) {
    double minDist = Double.POSITIVE_INFINITY;
    List<NetModuleFree.LinkPad> minList = null;
    Point2D minPoint = null;
    Iterator<Point2D> it = ptToPadLists.keySet().iterator();
    while (it.hasNext()) {
      Point2D key = it.next();
      double distSq = key.distanceSq(myPoint);
      if (distSq < minDist) {
        minDist = distSq;
        minList = ptToPadLists.get(key);
        minPoint = key;
      }
    }
    if (minPoint == null) {
      return (null);
    }
    NetModuleFree.LinkPad padData = minList.get(0);
    padData = padData.clone();
    padData.point = (Point2D)myPoint.clone();
    if (minList.size() > 1) {
      minList.remove(0);
    }
    return (padData);
  }
  
 
  /***************************************************************************
  **
  ** Find pad needs for _all_ net module links handled by this layout (we provide
  ** pregenerated map from layout to keys):
  */
  
  public PadNeedsForLayout findAllNetModuleLinkPadRequirements(StaticDataAccessContext rcx, Map<String, Layout.OverlayKeySet> layoutToFullKeys) {
    
    Layout.OverlayKeySet myKeys = layoutToFullKeys.get(name_);   
    HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements> retval = new HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements>();
    
    if (myKeys != null) {
      Iterator<NetModule.FullModuleKey> mkit = myKeys.iterator();
      while (mkit.hasNext()) {
        NetModule.FullModuleKey fullKey = mkit.next();
        NetModuleLinkPadRequirements needPads = 
          findNetModuleLinkPadRequirements(fullKey.ownerKey, fullKey.ovrKey, fullKey.modKey, rcx); 
        retval.put(fullKey, needPads);  
      }
    }
    return (new PadNeedsForLayout(retval));
  } 
  
  /***************************************************************************
  **
  ** Find the member-only geometry
  */
  
  public Map<NetModule.FullModuleKey, Map<String, Rectangle>> stashMemberOnlyGeometry(StaticDataAccessContext rcx, 
                                                                                      Map<String, OverlayKeySet> layoutToFullKeys) {
    
    Layout.OverlayKeySet myKeys = layoutToFullKeys.get(name_);   
    HashMap<NetModule.FullModuleKey, Map<String, Rectangle>> retval = new HashMap<NetModule.FullModuleKey, Map<String, Rectangle>>();
    if (myKeys != null) {
      Iterator<NetModule.FullModuleKey> mkit = myKeys.iterator();
      while (mkit.hasNext()) {
        NetModule.FullModuleKey fullKey = mkit.next();
        NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
        NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
        if (nmp.getType() != NetModuleProperties.MEMBERS_ONLY) {
          continue;
        }
        Map<String, Rectangle> memberOnlyGeom = memberOnlyGeom(fullKey.ownerKey, fullKey.ovrKey, fullKey.modKey, rcx); 
        retval.put(fullKey, memberOnlyGeom);  
      }
    }
    return (retval);
  }   
  
   /***************************************************************************
  **
  ** Find the geometry for a member-only module
  */
  
  private Map<String, Rectangle> memberOnlyGeom(String ownerKey, String ovrKey, String modKey, StaticDataAccessContext rcx) {

    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerWithOwnerKey(ownerKey);
    NetworkOverlay no = noo.getNetworkOverlay(ovrKey);
    NetModule nMod = no.getModule(modKey);
    HashSet<String> nodes = new HashSet<String>();
    Iterator<NetModuleMember> memit = nMod.getMemberIterator();
    while (memit.hasNext()) {
      NetModuleMember nmm = memit.next();
      nodes.add(nmm.getID());
    }

    Genome rootInstance = rcx.getGenomeSource().getGenome(targetGenome_);
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, rootInstance, rcx.getCurrentLayout());
    Map<String, Rectangle> rects = NodeBounder.nodeRects(nodes, irx, UiUtil.GRID_SIZE_INT); 
    return (rects);
  }   
  
  /***************************************************************************
  **
  ** Find pad needs for net module links associated with given (e.g. currently displayed)
  ** overlay handled by this layout.
  */
  
  public Layout.PadNeedsForLayout findAllNetModuleLinkPadRequirementsForOverlay(StaticDataAccessContext rcx) { 
    String ovrKey = rcx.getOSO().getCurrentOverlay(); 
    if (ovrKey == null) {
      return (null);
    }
    HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements> retval = new HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements>();
    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID());
    String ownerKey = noo.getID();
    int ownerType = noo.overlayModeForOwner();
    NetOverlayProperties nop = getNetOverlayProperties(ovrKey);
    Iterator<String> nmpkit = nop.getNetModulePropertiesKeys();
    while (nmpkit.hasNext()) {
      String modKey = nmpkit.next();
      NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerType, ownerKey, ovrKey, modKey);
      NetModuleLinkPadRequirements needPads = findNetModuleLinkPadRequirements(ownerKey, ovrKey, fullKey.modKey, rcx); 
      retval.put(fullKey, needPads);
    }
    return (new Layout.PadNeedsForLayout(retval));
  }  

  /***************************************************************************
  **
  ** Find pad needs for _all_ net module links handled by this layout
  */
  
  public PadNeedsForLayout findAllNetModuleLinkPadRequirements(StaticDataAccessContext rcx) {   
    Map<String, OverlayKeySet> allKeys = rcx.getFGHO().fullModuleKeysPerLayout();
    return (findAllNetModuleLinkPadRequirements(rcx, allKeys));    
  }    
  
  /***************************************************************************
  **
  ** Get the set of keys for all the overlays we manage
  */
  
  public Set<String> getAllOverlayKeys() {   
    return (new HashSet<String>(ovrProps_.keySet()));
  } 

  /***************************************************************************
  **
  ** Shortcut for global orphan-only args 
  */
  
  public Map<String, Boolean> orphansOnlyForAll(boolean val) {
    Boolean valObj = new Boolean(val);
    HashMap<String, Boolean> retval = new HashMap<String, Boolean>();
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String key = opit.next();
      retval.put(key, valObj);
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Make needed fixes for _all_ net module links handled by this layout
  ** This gets used in a DOZEN places.  Almost all of them have "false" for
  ** orphans only.  Only repairNetModuleLinkPadsLocally currently provides the option.
   * ToolCommands.squashGenome and ToolCommands.expandGenome use "true" for the DBGenome operations,
  ** due to the fully consistent expansion/compression operation at that level.  All the other rubber
  ** stamping genome instance stuff uses false
  ** 8/24/12 Need to vary the orphans only arg on a PER-OVERLAY basis, since for overlay-driven
  ** specialty layout we are precise with the overlay driving the show, but not with any other overlays
  ** 
  */
  
  public PropChange[] repairAllNetModuleLinkPadRequirements(StaticDataAccessContext rcx, PadNeedsForLayout needsForLayout, Map<String, Boolean> orphansOnlyPerOverlay) {   
 
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    Iterator<NetModule.FullModuleKey> mkit = needsForLayout.keyIterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
      NetModuleLinkPadRequirements padNeeds = needsForLayout.getPadRequire(fullKey);
      Boolean orphansOnlyObj = orphansOnlyPerOverlay.get(fullKey.ovrKey);
      PropChange[] lpc = fixNetModuleLinkPads(fullKey, padNeeds, rcx, orphansOnlyObj.booleanValue());
      changes.addAll(Arrays.asList(lpc));
    }
    
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));           
  }  
  
  /***************************************************************************
  **
  ** Make needed fixes for _all_ drained member-only net modules
  */
  
  public PropChange[] repairAllEmptyMemberOnlyNetModules(Map<String, Map<NetModule.FullModuleKey, Map<String, Rectangle>>> retainedRectsPerLayout, 
                                                         StaticDataAccessContext rcx) {
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    Map<NetModule.FullModuleKey, Map<String, Rectangle>> keysToRects = retainedRectsPerLayout.get(name_); 
    if (keysToRects != null) {
      Iterator<NetModule.FullModuleKey> mkit = keysToRects.keySet().iterator();
      GenomeSource gSrc = rcx.getGenomeSource();
      while (mkit.hasNext()) {
        NetModule.FullModuleKey fullKey = mkit.next();
        NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey); 
        NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
        // If somebody beat us to the punch and it's gone, we're good to go:
        if (nmp == null) {
          continue;
        }
        if (nmp.getType() != NetModuleProperties.MEMBERS_ONLY) {  // Shouldn't happen?
          continue;
        }
        NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
        NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
        NetModule nMod = no.getModule(fullKey.modKey);
        if (nMod.getMemberCount() == 0) {
          NetModuleProperties changedProps = nmp.clone();
          Map<String, Rectangle> eachRect = keysToRects.get(fullKey);
          changedProps.convertEmptyMemberOnly(eachRect);
          PropChange lpc = replaceNetModuleProperties(fullKey.modKey, changedProps, fullKey.ovrKey);     
          if (lpc != null) {
            changes.add(lpc);
          }     
        }
      }
    }
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));           
  }  

  /***************************************************************************
  **
  ** Repair broken net module link pads
  */
  
  public PropChange[] fixNetModuleLinkPads(NetModule.FullModuleKey fullKey, 
                                           NetModuleLinkPadRequirements padNeeds,
                                           StaticDataAccessContext rcx, boolean orphansOnly) {
    
    NetOverlayProperties noProps = getNetOverlayProperties(fullKey.ovrKey);
    NetModuleProperties nmp = noProps.getNetModuleProperties(fullKey.modKey);
    
    //
    // If a module evaporates 
    
    if (nmp == null) {
      return (new PropChange[0]);
    }
    
    
    if (padNeeds.treeIDToStarts.isEmpty() && padNeeds.linkIDToEnds.isEmpty()) {
      return (new PropChange[0]);
    }    
   
    //
    // Figure out how (if!) the shifted pad choices can be accomodated:
    //   
    GenomeSource gSrc = rcx.getGenomeSource();
    NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
    DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo 
                                                                                           : null;    
    NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
    NetModule mod = no.getModule(fullKey.modKey);
    
    // 03/02/12: orphansOnly = true only used in expand/compress DBGenome!  This builds pad expectations
    // off of correctly transformed Linkage drop ends.  So if the linkage drop ends are not being transformed
    // and tracked correctly, you are messed up!
    
    Map<PointNoPoint, NetModuleFree.LinkPad> currState = null;
    if (orphansOnly) {
      NetModuleLinkPadRequirements currReq = getCurrentNetModuleLinkPadState(fullKey, padNeeds);
      currState = currReq.linkPadInfo;
    }
    
    // Rendering requirements can be based upon the rootInstance geometry and membership, with
    // groups from the target genome.
    
    Genome useGenome = gSrc.getGenome(targetGenome_);
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome);
    //
    // OK, maybe need to change the genome, but we CANNOT change the layout. Per Issue #214, the
    // layout must be the one provided, as it could the be the temporary moving layout.
    //
    irx.setLayout(rcx.getCurrentLayout());
    // End Issue #214 Fix
    Set<String> useGroups = noo.getGroupsForOverlayRendering();
    Map<PointNoPoint, NetModuleFree.LinkPad> bestFits = nmp.getRenderer().closestRemainingPads(dip, useGroups, mod,
                                                                                               nmp, irx, padNeeds.rigidMoves, padNeeds.linkPadInfo, 
                                                                                               currState, orphansOnly);
    HashMap<Point2D, ShiftAndSide> newPadShifts = new HashMap<Point2D, ShiftAndSide>();
    Iterator<PointNoPoint> lpkit = padNeeds.linkPadInfo.keySet().iterator();
    while (lpkit.hasNext()) {
      PointNoPoint oldPNP = lpkit.next();
      NetModuleFree.LinkPad bestFit = bestFits.get(oldPNP);
      if (bestFit == null) {
        //
        // We make every effort to find a place for the link to go.  This case should
        // not happen. 
        //
        System.err.println("Could not find a new module link pad for " + oldPNP);
      } else {
        Vector2D bestFitShift = new Vector2D(oldPNP.point, bestFit.point);
        if (!bestFitShift.isZero()) {
          newPadShifts.put(oldPNP.point, new ShiftAndSide(bestFitShift, bestFit.getNormal().scaled(-1.0)));    
        }
      }
    }
     
    //
    // Now do the changes:
    //
    
    ArrayList<PropChange> changes = new ArrayList<PropChange>(); 
       
    //
    // Shift starts:
    //
    
    Iterator<String> tidit = padNeeds.treeIDToStarts.keySet().iterator();
    while (tidit.hasNext()) {
      String treeID = tidit.next();
      PointNoPoint oldStart = padNeeds.treeIDToStarts.get(treeID); 
      ShiftAndSide shift = newPadShifts.get(oldStart.point);
      if (shift != null) {
        NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
        NetModuleLinkageProperties modLProps = nmlp.clone();
        //
        // Was doing a direct shift based on the noProps point.  But shift calc was based on the
        // oldStart, and intervening layout changes could cause the two to diverge:
        //
        Point2D newStart = shift.shift.add(oldStart.point);
        modLProps.setSourceStart(newStart, shift.toSide);
        changes.add(replaceNetModuleLinkageProperties(treeID, modLProps, fullKey.ovrKey, rcx));
      }
    }
    
    //
    // Shift ends:
    //
  
    HashMap<String, NetModuleLinkageProperties> modTrees = new HashMap<String, NetModuleLinkageProperties>();
    Iterator<String> lidit = padNeeds.linkIDToEnds.keySet().iterator();
    while (lidit.hasNext()) {
      String linkID = lidit.next();
      PointNoPoint oldEnd = padNeeds.linkIDToEnds.get(linkID);
      ShiftAndSide shift = newPadShifts.get(oldEnd.point);
      if (shift != null) {
        String treeID = noProps.getNetModuleLinkagePropertiesID(linkID);
        NetModuleLinkageProperties modLProps = modTrees.get(treeID);
        if (modLProps == null) {
          NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkageProperties(linkID);
          modLProps = nmlp.clone();
          modTrees.put(treeID, modLProps);
        }
        //
        // Was doing a direct shift based on the noProps point.  But shift calc was based on the
        // oldEnd, and intervening layout changes could cause the two to diverge:
        //
        Point2D newEnd = shift.shift.add(oldEnd.point);
        modLProps.setTargetEnd(linkID, newEnd, shift.toSide);
      }
    }  
    
    Iterator<String> mtkit = modTrees.keySet().iterator();
    while (mtkit.hasNext()) {
      String treeID = mtkit.next();
      NetModuleLinkageProperties modLProps = modTrees.get(treeID);
      changes.add(replaceNetModuleLinkageProperties(treeID, modLProps, fullKey.ovrKey, rcx));
    }    
 
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));            
  }  
   
  /***************************************************************************
  **
  ** Move the net module links by the given amount
  */
  
  public PropChange moveNetModuleLinks(Intersection modIntersect, String ovrKey, double dx, double dy, StaticDataAccessContext irx) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    String treeID = modIntersect.getObjectID();
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
    LinkSegmentID[] segIDs = modIntersect.segmentIDsFromIntersect();
  //  if (segID == null) {
  //    selection = rend.fullIntersection(genome, link, layout, frc_, true);
   //   segID = selection.segmentIDsFromIntersect();
  //  }  
              
    PropChange retval = new PropChange();
    undoPreProcess(retval, nmlp, ovrKey, irx);
    dx = UiUtil.forceToGridValue(dx, UiUtil.GRID_SIZE);
    dy = UiUtil.forceToGridValue(dy, UiUtil.GRID_SIZE);    
    nmlp.moveBusLinkSegments(segIDs, null, dx, dy);
    undoPostProcess(retval, nmlp, ovrKey, irx);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Change the target.  We must be given a cloned point forced to the grid
  */
  
  public PropChange changeNetModuleTarget(String ovrKey, String linkID, Point2D newTarget, Vector2D toSide, StaticDataAccessContext irx) {    
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkageProperties(linkID); 
    PropChange retval = new PropChange();
    undoPreProcess(retval, nmlp, ovrKey, irx);
    nmlp.setTargetEnd(linkID, newTarget, toSide);
    undoPostProcess(retval, nmlp, ovrKey, irx);
    return (retval);
  } 

  /***************************************************************************
  **
  ** Change the source  We must be given a cloned point forced to the grid
  */
  
  public PropChange changeNetModuleSource(String ovrKey, String treeID, Point2D newSource, 
                                          Vector2D toSide, StaticDataAccessContext irx) {    
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
    PropChange retval = new PropChange();
    undoPreProcess(retval, nmlp, ovrKey, irx);
    nmlp.setSourceStart(newSource, toSide);
    undoPostProcess(retval, nmlp, ovrKey, irx);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Convert all overlay modules to member only (unchanged if no members)
  ** 
  */  
  
  public PropChange[] convertAllModulesToMemberOnly(OverlayKeySet fullKeys, StaticDataAccessContext rcx) {

    if (fullKeys == null) {
      return (null);
    }
    GenomeSource gSrc = rcx.getGenomeSource();
    HashSet<String> nodeSet = new HashSet<String>();
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    // Rendering requirements can be based upon the rootInstance geometry and membership
    Genome useGenome = gSrc.getGenome(targetGenome_);
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
        
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
         
      NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
      NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
      NetModule nmod = no.getModule(fullKey.modKey);   
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
      
      nodeSet.clear();
      Iterator<NetModuleMember> memit = nmod.getMemberIterator();
      while (memit.hasNext()) {
        NetModuleMember nmm = memit.next();
        nodeSet.add(nmm.getID());
      }
      // Nothing to do....
      if ((nmp.getType() == NetModuleProperties.MEMBERS_ONLY) || nodeSet.isEmpty()) {
        continue;
      }
      List<Rectangle2D> convert = nmp.convertShapes(NetModuleProperties.MEMBERS_ONLY, nodeSet, irx);
      if (convert != null) {
        NetModuleProperties changedProps = nmp.clone();
        changedProps.replaceTypeAndShapes(NetModuleProperties.MEMBERS_ONLY, convert);
        PropChange lpc = replaceNetModuleProperties(fullKey.modKey, changedProps, fullKey.ovrKey);     
        if (lpc != null) {
          changes.add(lpc);
        } 
      }
    } 

    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));             
  }  

 
  /***************************************************************************
  **
  ** Get a map that tells us how each module shape is positioned w.r.t. the
  ** module members it contains.
  ** 
  */  
  
  public Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> getModuleShapeParams(StaticDataAccessContext rcx,
                                                                                                   OverlayKeySet fullKeys, 
                                                                                                   Point2D wsCenter) {
    HashMap<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> retval = 
      new HashMap<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo>();
    if (fullKeys == null) {
      return (retval);
    }
    NetModuleShapeFixer fixer = new NetModuleShapeFixer();  
    Rectangle bounds = getLayoutBounds(rcx, false, false, false, false, false, null, null, null, null);
    Point2D center = (bounds == null) ? wsCenter : new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    GenomeSource gSrc = rcx.getGenomeSource();  
    // Rendering requirements can be based upon the rootInstance geometry and membership
    Genome useGenome = gSrc.getGenome(getTarget());
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();        
      NetModuleShapeFixer.ModuleRelocateInfo mri = fixer.getModuleRelocInfo(fullKey, center, irx);
      retval.put(fullKey, mri);
    }
  
    return (retval);             
  }   
   
 
  /***************************************************************************
  **
  ** Shift module shapes around to accomodate moving member nodes
  ** 
  */  
  
  public PropChange[] shiftModuleShapesPerParams(Layout.OverlayKeySet fullKeys, 
                                                 Map<NetModule.FullModuleKey, NetModuleShapeFixer.ModuleRelocateInfo> relocInfoMap, 
                                                 StaticDataAccessContext rcx) {

    if (fullKeys == null) {
      return (null);
    }
    NetModuleShapeFixer fixer = new NetModuleShapeFixer();
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    // Rendering requirements can be based upon the rootInstance geometry and membership 
    Rectangle bounds = getLayoutBounds(rcx, false, false, false, false, false, null, null, null, null);
    Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    GenomeSource gSrc = rcx.getGenomeSource();
    Genome useGenome = gSrc.getGenome(getTarget());    
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
        
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next(); 
      NetModuleShapeFixer.ModuleRelocateInfo relocInfo = relocInfoMap.get(fullKey);
      NetModuleProperties changedProps = fixer.shiftModuleShapesPerParams(fullKey, relocInfo, center, irx);
      if (changedProps != null) {
        PropChange lpc = replaceNetModuleProperties(fullKey.modKey, changedProps, fullKey.ovrKey);     
        if (lpc != null) {
          changes.add(lpc);
        } 
      }
    }
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));    
  }  
  
  /***************************************************************************
  **
  ** Change the color of a module and all link trees from the module. 
  */
  
  public PropChange[] applySameColorToModuleAndLinks(String overKey, String modKey, String colName, 
                                                     boolean doModule, String skipTree, StaticDataAccessContext rcx) {
 
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    
    NetOverlayProperties nop = getNetOverlayProperties(overKey);
    Iterator<String> trit = nop.getNetModuleLinkagePropertiesKeys(); 
    while (trit.hasNext()) {
      String treeID = trit.next();
      if ((skipTree != null) && treeID.equals(skipTree)) {
        continue;
      }
      NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      if (modKey.equals(nmlp.getSourceTag())) {
        NetModuleLinkageProperties changedNmlp = nmlp.clone();
        changedNmlp.setColor(colName);
        PropChange lpc = replaceNetModuleLinkageProperties(treeID, changedNmlp, overKey, rcx);      
        if (lpc != null) {
          changes.add(lpc);
        } 
      }
    }
    
    if (doModule) {
      NetModuleProperties nmp = nop.getNetModuleProperties(modKey);
      NetModuleProperties changedProps = nmp.clone();
      changedProps.setColorTag(colName);    
      PropChange lpc = replaceNetModuleProperties(modKey, changedProps, overKey);     
      if (lpc != null) {
        changes.add(lpc);
      }     
    }

    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));         
  } 
 
  /***************************************************************************
  **
  ** Return the bounds of the given network modules
  */
  
  public Rectangle getLayoutBoundsForNetModules(NetOverlayOwner owner, String ovrKey, TaggedSet modSet,
                                                boolean doModuleLinks, DataAccessContext rcx) {
    
    Rectangle retval = null;
    
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    
    Genome useGenome = Layout.determineLayoutTarget(rcx.getCurrentGenome());
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    
    DynamicInstanceProxy dip = (owner.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)owner 
                                                                                             : null;
    Set<String> useGroups = owner.getGroupsForOverlayRendering();
    
    NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
    NetOverlayProperties nop = getNetOverlayProperties(ovrKey);
    Iterator<NetModule> mit = novr.getModuleIterator();
    Set<String> modKeys = modSet.set;
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String id = mod.getID();
      if (modKeys.contains(id)) {
        NetModuleProperties nmp = nop.getNetModuleProperties(id);
        Rectangle nmBounds = nmp.getRenderer().bounds(dip, useGroups, mod, ovrKey, irx);
        if (retval == null) {
          retval = nmBounds;
        } else {
          Bounds.tweakBounds(retval, nmBounds);
        }   
      }
    }
    
    if (doModuleLinks) {
      Set<String> linkSet = novr.getNetModuleLinkagesBetweenModules(modKeys);
      Rectangle linkRect = getLayoutBoundsForModuleLinks(ovrKey, linkSet);
      if (linkRect != null) {
        if (retval == null) {
          retval = linkRect;
        } else {
          Bounds.tweakBounds(retval, linkRect);
        }
      }
    }
 
    if (retval != null) {
      UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);
    }
    
    return (retval);
  }    
  
  /***************************************************************************
  **
  ** Return the bounds of the given network modules
  */
  
  public Map<String, Rectangle> getLayoutBoundsForEachNetModule(NetOverlayOwner owner, String ovrKey,
                                                                StaticDataAccessContext rcx) {
    
    HashMap<String, Rectangle> retval = new HashMap<String, Rectangle>();
    
    //
    // We use the rootInstance to drive rendering, so that non-included nodes are not
    // shown as part of the module (important for dynamic instances!)
    //
    Genome useGenome = Layout.determineLayoutTarget(rcx.getCurrentGenome());  
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
    DynamicInstanceProxy dip = (owner.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)owner 
                                                                                             : null;
    Set<String> useGroups = owner.getGroupsForOverlayRendering();    
    NetworkOverlay novr = owner.getNetworkOverlay(ovrKey);
    NetOverlayProperties nop = getNetOverlayProperties(ovrKey);
    Iterator<NetModule> mit = novr.getModuleIterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String id = mod.getID();
      NetModuleProperties nmp = nop.getNetModuleProperties(id);
      Rectangle nmBounds = nmp.getRenderer().bounds(dip, useGroups, mod, ovrKey, irx);
      UiUtil.forceToGrid(nmBounds, UiUtil.GRID_SIZE);
      retval.put(id, nmBounds);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Return the bounds all modules in all overlays
  */
  
  public Rectangle getLayoutBoundsForAllOverlays(OverlayKeySet allKeys, boolean doModuleLinks, 
                                                 StaticDataAccessContext rcx) {
 
    Rectangle retval = null;
    
    if (allKeys != null) {
      //
      // We use the rootInstance to drive rendering, so that non-included nodes are not
      // shown as part of the module (important for dynamic instances!)
      //
      GenomeSource gSrc = rcx.getGenomeSource();
      Genome useGenome = Layout.determineLayoutTarget(rcx.getCurrentGenome());  
      StaticDataAccessContext irx = new StaticDataAccessContext(rcx, useGenome, rcx.getCurrentLayout());
      HashSet<String> overKeys = new HashSet<String>();      
      Iterator<NetModule.FullModuleKey> mkit = allKeys.iterator();
      while (mkit.hasNext()) {
        NetModule.FullModuleKey fullKey = mkit.next();
        NetOverlayOwner noo = gSrc.getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
        DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo 
                                                                                                 : null;
        Set<String> useGroups = noo.getGroupsForOverlayRendering();
        NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
        overKeys.add(fullKey.ovrKey);
        NetModule mod = no.getModule(fullKey.modKey);
        NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
        NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
        Rectangle nmBounds = nmp.getRenderer().bounds(dip, useGroups, mod, fullKey.ovrKey, irx);
        if (retval == null) {
          retval = nmBounds;
        } else {
          Bounds.tweakBounds(retval, nmBounds);
        }
      }

      if (doModuleLinks) {
        Iterator<String> okit = overKeys.iterator();
        while (okit.hasNext()) {
          String ovrKey = okit.next();
          Rectangle linkRect = getLayoutBoundsForModuleLinks(ovrKey, null);
          if (linkRect == null) {
            continue;
          }
          if (retval == null) {
            retval = linkRect;
          } else {
            Bounds.tweakBounds(retval, linkRect);
          }     
        }
      }
    }
    

    if (retval != null) {
      UiUtil.forceToGrid(retval, UiUtil.GRID_SIZE);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the partial bounds of the layout, either with or without links
  */
  
  public Rectangle getLayoutBoundsForModuleLinks(String overKey, Set<String> linkIDs) {
    
    Rectangle retval = null;
       
    NetOverlayProperties nop = getNetOverlayProperties(overKey);
    Iterator<String> trit = (linkIDs == null) ? nop.getNetModuleLinkagePropertiesLinkKeys() : linkIDs.iterator();
                     
    while (trit.hasNext()) {
      String linkID = trit.next();
      NetModuleLinkageProperties nmlp = nop.getNetModuleLinkageProperties(linkID); 
      NetModuleLinkageFree nmlf = (NetModuleLinkageFree)nmlp.getRenderer();
      Rectangle rect = nmlf.getBoundsForSinglePath(nmlp, linkID);
      if (rect == null) {
        continue;
      }
      if (retval == null) {
        retval = rect;
      } else {
        Bounds.tweakBounds(retval, rect);
      }    
    }    
    
    return (retval);
  }   

  /***************************************************************************
  **
  ** Undo a net module property change
  */
  
  public void netModChangeUndo(PropChange undo) {
    NetOverlayProperties nop = getNetOverlayProperties(undo.nopRef);
    if ((undo.nmpOrig != null) && (undo.nmpNew != null)) {     
      nop.setNetModuleProperties(undo.nmpOrig.getID(), undo.nmpOrig.clone());
    } else if (undo.nmpNew != null) {
      nop.removeNetModuleProperties(undo.nmpNew.getID());
    } else if (undo.nmpOrig != null) {
      nop.setNetModuleProperties(undo.nmpOrig.getID(), undo.nmpOrig.clone());
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }       
     
  /***************************************************************************
  **
  ** Redo a net module change
  */
  
  public void netModChangeRedo(PropChange redo) {
    NetOverlayProperties nop = getNetOverlayProperties(redo.nopRef);
    if ((redo.nmpOrig != null) && (redo.nmpNew != null)) {
      nop.setNetModuleProperties(redo.nmpNew.getID(), redo.nmpNew.clone());
    } else if (redo.nmpNew != null) {
      nop.setNetModuleProperties(redo.nmpNew.getID(), redo.nmpNew.clone());
    } else if (redo.nmpOrig != null) {
      nop.removeNetModuleProperties(redo.nmpOrig.getID());
    } else {
      throw new IllegalArgumentException();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Undo a net module link property change
  */
  
  public void netModLinkChangeUndo(PropChange undo) {
    NetOverlayProperties nop = getNetOverlayProperties(undo.nopRef);
    if ((undo.nmlpOrig != null) && (undo.nmlpNew != null)) {
      nop.setNetModuleLinkageProperties(undo.nmlpOrig.getID(), undo.nmlpOrig.clone());
    } else if (undo.nmlpNew != null) {
      // Key removal occurs as part of the nop.removeNetMod... call:
      nop.removeNetModuleLinkagePropertiesWithTreeID(undo.gSrc, undo.nmlpNew.getID());
    } else if (undo.nmlpOrig != null) { // Currently never invoked????
      ((DBGenome)undo.gSrc.getRootDBGenome()).addKey(undo.nmlpOrig.getID());
      nop.setNetModuleLinkageProperties(undo.nmlpOrig.getID(), undo.nmlpOrig.clone());
    } else if (undo.nmlpTieLinkIDOrig != null) {
       // Note asymmetry with redo case.  We do have a case where undo.nmlpTieLinkIDOrig != null and undo.nmlpTieTreeIDOrig == null
       // But there are no cases where redo.nmlpTieLinkIDNew != null and redo.nmlpTieTreeIDNew == null
       if (undo.nmlpTieTreeIDOrig == null) { 
         nop.untieNetModuleLinkagePropertiesForLink(undo.nmlpTieLinkIDOrig);
       } else {
         nop.tieNetModuleLinkagePropertiesForLink(undo.nmlpTieLinkIDOrig, undo.nmlpTieTreeIDOrig);
       }
    } else {
      throw new IllegalArgumentException();
    }  
    return;
  }       
     
  /***************************************************************************
  **
  ** Redo a net module link property change
  */
  
  public void netModLinkChangeRedo(PropChange redo) {   
    NetOverlayProperties nop = getNetOverlayProperties(redo.nopRef);
    if ((redo.nmlpOrig != null) && (redo.nmlpNew != null)) {
      nop.setNetModuleLinkageProperties(redo.nmlpNew.getID(), redo.nmlpNew.clone());
    } else if (redo.nmlpNew != null) {
      ((DBGenome)redo.gSrc.getRootDBGenome()).addKey(redo.nmlpNew.getID());
      nop.setNetModuleLinkageProperties(redo.nmlpNew.getID(), redo.nmlpNew.clone());
    } else if (redo.nmlpOrig != null) { // Currently never invoked????
      // removal handles the treeID key removal:
      nop.removeNetModuleLinkagePropertiesWithTreeID(redo.gSrc, redo.nmlpOrig.getID());
    } else if (redo.nmlpTieLinkIDNew != null) {    
      nop.tieNetModuleLinkagePropertiesForLink(redo.nmlpTieLinkIDNew, redo.nmlpTieTreeIDNew);
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Give the layout the ID of the Note, and we return a NoteProperties
  ** that gives the color and location(s) of the item.
  */
  
  public NoteProperties getNoteProperties(String noteId) {
    return (noteProps_.get(noteId));
  }  
  
  /***************************************************************************
  **
  ** Answers if the given link segment is synonymous with the start drop, i.e.
  ** the links through it match all the links in the tree.  Note that this
  ** calculation must occur in the context of the root or root genome instance.
  */
 
  public boolean segmentSynonymousWithStartDrop(String linkId, LinkSegmentID segID) {
    BusProperties bp = getLinkProperties(linkId);
    Set<String> throughSeg = bp.resolveLinkagesThroughSegment(segID);  
    Set<String> shared = getSharedItems(linkId); 
    return (shared.equals(throughSeg));
  }
  
  /***************************************************************************
  **
  ** Answers if the given link segment is synonymous with a target drop, i.e.
  ** there is only one link through it.  Note that this
  ** calculation must occur in the context of the root or root genome instance.
  */
 
  public String segmentSynonymousWithTargetDrop(String linkId, LinkSegmentID segID) {
    BusProperties bp = getLinkProperties(linkId);
    Set<String> throughSeg = bp.resolveLinkagesThroughSegment(segID);
    if (throughSeg.size() == 1) {
      return (throughSeg.iterator().next());
    } else {
      return (null);
    }
  } 
   
  /***************************************************************************
  **
  ** Answers if the given link segment is synonymous with a target drop, i.e.
  ** there is only one link through it.  This version for modules.
  */
 
  public String segmentSynonymousWithModLinkTargetDrop(String treeID, LinkSegmentID segID, String ovrKey) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
    Set<String> throughSeg = nmlp.resolveLinkagesThroughSegment(segID);
    if (throughSeg.size() == 1) {
      return (throughSeg.iterator().next());
    } else {
      return (null);
    }
  } 
  
  /***************************************************************************
  **
  ** Answers if the given link segment is synonymous with the start drop, i.e.
  ** the links through it match all the links in the tree. This version for modules.
  ** Null if not synonymous; else returns set of links
  */
 
  public Set<String> segmentSynonymousWithModLinkStartDrop(String treeID, LinkSegmentID segID, String ovrKey) {
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(treeID);
    Set<String> throughSeg = nmlp.resolveLinkagesThroughSegment(segID);    
    HashSet<String> shared = new HashSet<String>(nmlp.getLinkageList());
    return ((shared.equals(throughSeg)) ? shared : null);
  }  
 
  /***************************************************************************
  **
  ** For Link trees, the same property is shared by many Linkages.  Return all
  ** the linkages that are sharing with the given linkage.
  */
 
  public Set<String> getSharedItems(String itemId) {
    HashSet<String> retval = new HashSet<String>();
    BusProperties lp = getLinkProperties(itemId);
    Iterator<String> lpit = linkProps_.keySet().iterator();
    while (lpit.hasNext()) {
      String key = lpit.next();
      if (lp == getLinkProperties(key)) {
        retval.add(key);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** A link intersection can either be for a whole link, or for just a set of
  ** segments. Resolve this mess into the set of affected shared links
  */
  
   public Set<String> resolveLinksFromIntersection(Intersection lint) {    
     HashSet<String> links = new HashSet<String>();
     String objID = lint.getObjectID();
     LinkSegmentID[] selsegs = lint.segmentIDsFromIntersect();
     if (selsegs == null) {  // whole link is selected...
       Set<String> shared = getSharedItems(objID);
       links.addAll(shared);
     } else {
       LinkProperties lp = getLinkProperties(objID);
       for (int i = 0; i < selsegs.length; i++) {
         Set<String> resolved = lp.resolveLinkagesThroughSegment(selsegs[i]);
         links.addAll(resolved);
       }
     }
     return (links);
   }
   
  /***************************************************************************
  **
  ** For a particular source, find and return a bus from that source if one
  ** exists.
  */
 
  public BusProperties getBusForSource(String itemId) {
    Iterator<BusProperties> lpit = linkProps_.values().iterator();
    while (lpit.hasNext()) {
      BusProperties bp = lpit.next();
      String srcID = bp.getSourceTag();
      if (srcID.equals(itemId)) {
        return (bp);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** For a particular source, find and return a link property from that source if one
  ** exists.
  */
 
  public BusProperties getLinkPropertiesForSource(String itemId) {
    return (getBusForSource(itemId));
  }  
  
  /***************************************************************************
  **
  ** For a particular source and launch pad, find and return a link property.
  */
 
  public BusProperties getLinkPropertyForSource(String itemId, int launchPad, StaticDataAccessContext rcx) {
    Iterator<BusProperties> lpit = linkProps_.values().iterator();
    Genome genome = rcx.getGenomeSource().getGenome(getTarget());
    while (lpit.hasNext()) {
      BusProperties bp = lpit.next();
      String srcID = bp.getSourceTag();
      int srcPad = bp.getLaunchPad(genome, this);
      if (itemId.equals(srcID) && (launchPad == srcPad)) {
        return (bp);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Repair the topology all link trees to eliminate overlaps.  Ovrkey is null
  ** unless we are working with overlay links
  */
  
  public TopoRepairInfo repairAllTopology(StaticDataAccessContext icx, String overID,
                                          BTProgressMonitor monitor, double minFrac, double maxFrac) throws AsynchExitRequestException {
    return (repairAllTopologyGuts(icx, overID, monitor,  minFrac, maxFrac, null));
  }
    
  /***************************************************************************
  **
  ** Repair the topology all link trees to eliminate overlaps. Use for autolayout
  */
  
  public TopoRepairInfo repairAllTopologyForAuto(StaticDataAccessContext icx,  String overID,
                                                 BTProgressMonitor monitor, double minFrac, double maxFrac) throws AsynchExitRequestException { 
    QuadTree useQuad = new QuadTree(100, 10);
    return (repairAllTopologyGuts(icx, overID, monitor,  minFrac, maxFrac, useQuad));
  }
  
  
  /***************************************************************************
  **
  ** Repair the topology all link trees to eliminate overlaps.  Ovrkey is null
  ** unless we are working with overlay links
  */
  
  private TopoRepairInfo repairAllTopologyGuts(StaticDataAccessContext icx, String overID,
                                               BTProgressMonitor monitor, double minFrac, double maxFrac, QuadTree useQuad) throws AsynchExitRequestException {
    Genome genome = icx.getGenomeSource().getGenome(getTarget());
    List<LinkProperties> lop = listOfProps(genome, overID, null);
    int numLop = lop.size();
    if (numLop == 0) {
      return (null);
    }
     
    double perTree = (maxFrac - minFrac) / numLop;
    double currProg = minFrac;
     
    TopoRepairInfo tri = null;
    for (int i = 0; i < numLop; i++) {
      LinkProperties lp = lop.get(i);
     
      PropChange retval = new PropChange();
      undoPreProcess(retval, lp, overID, icx);
      
      double nextProg = currProg + perTree; 
      TopoRepairInfo nextTri = repairTreeTopologyGuts(lp, icx, overID, monitor, currProg, nextProg, useQuad);
   
      currProg = nextProg; 
      if (nextTri.haveAChange()) {        
        undoPostProcess(retval, lp, overID, icx);
        nextTri.addPropChange(retval);
      }
      if (tri == null) {
        tri = nextTri;
      } else {
        tri.merge(nextTri);
      }
    }  
    return (tri);  // may be null
  }  
 
  /***************************************************************************
  **
  ** Repair the topology of the link tree to eliminate overlaps.  Ovrkey is null
  ** unless we are working with overlay links
  */
  
  public TopoRepairInfo repairTreeTopology(LinkProperties lp, StaticDataAccessContext icx, String overID,
                                           BTProgressMonitor monitor, double minFrac, double maxFrac) throws AsynchExitRequestException {
    PropChange retval = new PropChange();
    undoPreProcess(retval, lp, overID, icx);
    
    TopoRepairInfo tri = repairTreeTopologyGuts(lp, icx, overID, monitor, minFrac, maxFrac, null);
    if (tri.haveAChange()) {        
      undoPostProcess(retval, lp, overID, icx);
      tri.addPropChange(retval);
    }
    return (tri);
  }  
  
  /***************************************************************************
  **
  ** Repair the topology of the link tree to eliminate overlaps.  Ovrkey is null
  ** unless we are working with overlay links
  */
  
  private TopoRepairInfo repairTreeTopologyGuts(LinkProperties lp, StaticDataAccessContext icx, 
                                                String overID,
                                                BTProgressMonitor monitor, double minFrac, double maxFrac, QuadTree useQuad) throws AsynchExitRequestException {
  
    double delFrac = maxFrac - minFrac;
    double delFrac3 = delFrac / 3.0;
    boolean dropped = lp.dropAllZeroSegments();
    boolean elim = false;
    LinkOptimizer opt = new LinkOptimizer();
    double frac1 = minFrac + delFrac3;
    elim = opt.eliminateUselessCornersGridless(lp, icx, overID, minFrac, frac1, monitor);
    double frac2 = minFrac + (2.0 * delFrac3);
    int repairState = lp.repairLinkTree(icx, monitor, frac1, frac2, useQuad);
    boolean someRepaired = (repairState == BusProperties.ALL_REPAIRED) || (repairState == BusProperties.SOME_REPAIRED);
    if (someRepaired) {
      lp.dropAllZeroSegments();
      opt.eliminateUselessCornersGridless(lp, icx, overID, frac2, maxFrac, monitor);
    }
    if (monitor != null) {
      boolean keepGoing = monitor.updateProgress((int)(maxFrac * 100.0));
      if (!keepGoing) {
        throw new AsynchExitRequestException();
      }
    }
    return (new TopoRepairInfo(dropped, elim, repairState));
  }
 
  /***************************************************************************
  **
  ** Reattach the given link to another parent on the tree.  Ovrkey is null
  ** unless we are working with overlay links
  */
  
  public PropChange relocateSegmentOnTree(LinkProperties lp, LinkSegmentID targID, LinkSegmentID moveID, String ovrKey, StaticDataAccessContext rcx) {
    PropChange retval = new PropChange();
    undoPreProcess(retval, lp, ovrKey, rcx);
    if (!lp.moveSegmentOnTree(moveID, targID)) {
      return (null);
    }
    undoPostProcess(retval, lp, ovrKey, rcx);
    return (retval);   
  }  
  
  /***************************************************************************
  **
  ** Reattach the given link to another parent on the tree. Used for batch operations ONLY
  */
  
  public void relocateSegmentOnTreeBatchOnlyNoUndo(LinkProperties lp, LinkSegmentID targID, LinkSegmentID moveID, Map<String, LinkBusDrop> lbdm) {
    lp.moveSegmentOnTreeSpecialtyBatch(moveID, targID, lbdm);
    return;   
  }
  
  /***************************************************************************
  **
  ** Get a fast lookup going for batch reattachments
  */
  
  public Map<String, LinkBusDrop> prepForReattach(LinkProperties lp) {
    return (lp.prepForBatchMoves());
  }
  
  /***************************************************************************
  **
  ** Answer if we can reparent a link segment.  Obviously, the root drop
  ** doesn't qualify (except for net modules!).
  */
  
  public boolean canRelocateSegmentOnTree(LinkProperties lp, LinkSegmentID moveID) {
    return (lp.canRelocateSegmentOnTree(moveID));
  }

  /***************************************************************************
  **
  ** Merge the single link into the link tree at the given segment.
  ** FIX ME: Refactor into above method for root merges?
  */
  
  public PropChange mergeNewLinkToTreeAtSegment(StaticDataAccessContext rcx,
                                                String treeID,
                                                BusProperties newBp,
                                                LinkSegmentID sid) {
    //
    // Only to be used with newly built LinkProperties:
    //
     
    String slpTag = newBp.getSingleLinkage();                                             
    if (getLinkProperties(slpTag) != null) {
      // FIX ME??  Are we really still disallowing this?
      //throw new IllegalArgumentException();
    }
                                                  
    BusProperties bp = getLinkProperties(treeID);        
    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, null, null);

    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, rcx.getGenomeSource().getGenome(getTarget()), rcx.getCurrentLayout());
    bp.mergeSingleToTreeAtSegment(newBp, irx, sid);
    
    //
    // Add the new reference to the single link:
    //
    
    linkProps_.put(slpTag, bp);
    retval.addedLinkID = slpTag;
    retval.removedLinkID = null;  
        
    undoPostProcess(retval, bp, null, null);
    return (retval);   
  }
  
  /***************************************************************************
  **
  ** Merge the single link into the link tree at the root. Use for batch; no undo support
  ** 
  */
  
  public void mergeNewLinkToTreeAtRootBatch(StaticDataAccessContext rcx, String treeID, BusProperties newBp, String slpTag) {
 
    BusProperties bp = getLinkProperties(treeID);    
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, rcx.getGenomeSource().getGenome(getTarget()), rcx.getCurrentLayout());
    bp.mergeSingleToTreeAtSegment(newBp, irx, null);
    linkProps_.put(slpTag, bp);
    return;   
  }

  /***************************************************************************
  **
  ** Set the special drawing props for a segment.  Can be null to drop all
  ** props.
  */
  
  public PropChange setSpecialPropsForSegment(BusProperties bp, LinkSegmentID lsid, SuggestedDrawStyle sds, String ovrKey, StaticDataAccessContext rcx) {
    
    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, ovrKey, rcx); 
    bp.setDrawStyleForID(lsid, sds);    
    undoPostProcess(retval, bp, ovrKey, rcx);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Move a node
  */
  
  public PropChange moveNode(String selection, double dx, double dy, Layout.PadNeedsForLayout padReqs, StaticDataAccessContext rcx) {

    NodeProperties props = getNodeProperties(selection);    
    
    PropChange retval = new PropChange();
    retval.nOrig = props.clone();
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
    retval.layoutKey = name_;
    
    Point2D loc = props.getLocation();
    props.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    retval.nNewProps = props.clone();
    
    rigidPadFixes(selection, dx, dy, padReqs, rcx);

    //
    // 4/16/09 Links are now 100% independent of node locations (drops have no
    // node location info anymore)!
    //

    return (retval);
  }

  /***************************************************************************
  **
  **  Replace a node properties
  */
  
  public PropChange replaceNodeProperties(NodeProperties oldProp, NodeProperties newProp) {

    PropChange retval = new PropChange();
    retval.nOrig = oldProp.clone();
    retval.layoutKey = name_;
    // Don't need to make link changes -> these are empty
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
        
    nodeProps_.put(newProp.getReference(), newProp);
    
    retval.nNewProps = newProp.clone();
    return (retval);
  }  

  /***************************************************************************
  **
  ** Change the node properties type
  */
  
  public PropChange changeNodePropertiesType(String nodeID, int oldType, int newType) {
    NodeProperties existing = nodeProps_.get(nodeID);
    PropChange retval = new PropChange();    
    retval.nOrig = existing.clone();
    retval.layoutKey = name_;
    // Don't need to make link changes -> these are empty
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
    retval.nNewProps = new NodeProperties(existing, oldType, newType);    
    nodeProps_.put(existing.getReference(), retval.nNewProps.clone());
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a node move
  */
  
  public void nodeChangeUndo(PropChange unmove) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((unmove.nOrig != null) && (unmove.nNewProps != null)) {   
      setNodeProperties(unmove.nOrig.getReference(), unmove.nOrig);
      Iterator<BusProperties> olit = unmove.origLinks.iterator();
      while (olit.hasNext()) {
        BusProperties nextLink = olit.next();
        List<String> linkages = nextLink.getLinkageList();
        Iterator<String> lit = linkages.iterator();
        while (lit.hasNext()) {
          String refTag = lit.next();
          linkProps_.put(refTag, nextLink);
        }     
      }
    } else if (unmove.nOrig == null) {
      nodeProps_.remove(unmove.nNewProps.getReference());
    } else {
      nodeProps_.put(unmove.nOrig.getReference(), unmove.nOrig);
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a node change
  */
  
  public void nodeChangeRedo(PropChange unmove) {
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((unmove.nOrig != null) && (unmove.nNewProps != null)) {
      setNodeProperties(unmove.nNewProps.getReference(), unmove.nNewProps);
      Iterator<BusProperties> nlit = unmove.newLinks.iterator();
      while (nlit.hasNext()) {
        BusProperties nextLink = nlit.next();
        List<String> linkages = nextLink.getLinkageList();
        Iterator<String> lit = linkages.iterator();
        while (lit.hasNext()) {
          String refTag = lit.next();
          linkProps_.put(refTag, nextLink);
        }
      }
    } else if (unmove.nOrig == null) {
      nodeProps_.put(unmove.nNewProps.getReference(), unmove.nNewProps);
    } else {
      nodeProps_.remove(unmove.nOrig.getReference());
    }    
    
    return;
  }

  /***************************************************************************
  **
  ** Delete a linkage corner
  */
  
  public PropChange deleteCornerForNetModuleLinkTree(NetModuleLinkageProperties nmlp, LinkSegmentID segID, String ovrKey, StaticDataAccessContext rcx) {

    PropChange retval = new PropChange();
    undoPreProcess(retval, nmlp, ovrKey, rcx);
    nmlp.removeCorner(segID); 
    undoPostProcess(retval, nmlp, ovrKey, rcx);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a linkage corner
  */
  
  public PropChange deleteLinkageCornerForTree(LinkProperties bp, LinkSegmentID segID, String overID, StaticDataAccessContext rcx) {

    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, overID, rcx);
    bp.removeCorner(segID); 
    undoPostProcess(retval, bp, overID, rcx);
    return (retval);
  }

  /***************************************************************************
  **
  ** Fix all non-ortho segments in the layout
  */
  
  public OrthoRepairInfo fixAllNonOrthoForLayout(List<Intersection> nonOrtho, StaticDataAccessContext irx, boolean minCorners, 
                                                 String overID,
                                                 BTProgressMonitor monitor, double startFrac, double endFrac) 
                                                   throws AsynchExitRequestException {
   
    Genome genome = irx.getCurrentGenome(); 
    //
    // Fix the worst offenders first:
    //
    
    int treeCount = 0;
    TreeMap<Double, SortedSet<String>> worstFirst = new TreeMap<Double, SortedSet<String>>(Collections.reverseOrder());
    int nno = nonOrtho.size();
    NetOverlayProperties nop = (overID != null) ? getNetOverlayProperties(overID) : null;
    
    // Note that in the case of busProps, the ortho list has only one intersection per tree 
    for (int i = 0; i < nno; i++) {
      Intersection in = nonOrtho.get(i);
      String objID = in.getObjectID();
      LinkProperties lp;
      String treeID = null;
      if (overID != null) {
        treeID = nop.getNetModuleLinkagePropertiesID(objID);
        lp = nop.getNetModuleLinkagePropertiesFromTreeID(treeID);
      } else {
        lp = getLinkProperties(objID);
      }
      Map<LinkSegmentID, LinkSegment> geoms = lp.getAllSegmentGeometries(irx, false);
      double nonOrthoArea = LinkProperties.getNonOrthogonalArea(geoms);
      Double noaVal = new Double(nonOrthoArea);
      SortedSet<String> perNoa = worstFirst.get(noaVal);
      if (perNoa == null) {
        perNoa = new TreeSet<String>();
        worstFirst.put(noaVal, perNoa);
      }
      // Trying to make the order somewhat non-arbitrary:
      String stable = (overID == null) ? ((BusProperties)lp).getAStableLinkID(genome) : treeID;
      if (!perNoa.contains(stable)) {
        perNoa.add(stable);
        treeCount++;
      }
    }
    
    double progFrac = (endFrac - startFrac) / treeCount;
    double currFrac = startFrac;
      
    int failCount = 0;    
    ArrayList<PropChange> retList = new ArrayList<PropChange>();
    Iterator<SortedSet<String>> wfvit = worstFirst.values().iterator();
    while (wfvit.hasNext()) {
      SortedSet<String> perNoa = wfvit.next();
      Iterator<String> pnit = perNoa.iterator();
      while (pnit.hasNext()) {
        String nextID = pnit.next();
        LinkProperties lp;
        if (overID != null) {
          lp = nop.getNetModuleLinkagePropertiesFromTreeID(nextID);
        } else {
          lp = getLinkProperties(nextID);
        }
        OrthoRepairInfo ori = fixAllNonOrthoForTree(lp, irx, minCorners, overID, monitor, currFrac, currFrac + progFrac);
        currFrac += progFrac;
        if (monitor != null) {
          boolean keepGoing = monitor.updateProgress((int)(currFrac * 100.0));
          if (!keepGoing) {
            throw new AsynchExitRequestException();
          }
        }
        List<PropChange> al = Arrays.asList(ori.chgs);
        retList.addAll(al);
        failCount += ori.failCount;
      }
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }   
    OrthoRepairInfo retval = new OrthoRepairInfo(failCount, false);
    PropChange[] retArr = new PropChange[retList.size()];
    retList.toArray(retArr);
    retval.addPropChanges(retArr);
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Fix all non-ortho segments in a tree
  */
  
  public OrthoRepairInfo fixAllNonOrthoForTree(LinkProperties lp, StaticDataAccessContext icx,
                                               boolean minCorners, String overID, 
                                               BTProgressMonitor monitor, double startFrac, double endFrac) 
                                               throws AsynchExitRequestException {
   
    //
    // The current implementation of fixing a segment is recursive.  So one
    // change will mod the whole tree, and can fix several things at once.
    // So we need to check back each time.  Also, start with the lowest
    // segments in the tree and work up:
    //
    
    HashSet<LinkSegmentID> skipFails = new HashSet<LinkSegmentID>();
    ArrayList<PropChange> retList = new ArrayList<PropChange>();
    
    Set<LinkSegmentID> nonOrtho = lp.getNonOrthoSegments(icx);
    if (nonOrtho.isEmpty()) {
      OrthoRepairInfo retval = new OrthoRepairInfo(0, false);
      PropChange[] retArr = new PropChange[0];     
      retval.addPropChanges(retArr);
      return (retval);
    }
    double progFrac = (endFrac - startFrac) / nonOrtho.size();
    double currFrac = startFrac;    
    boolean first = true;
    while (true) {      
      PropChange retval = new PropChange();
      undoPreProcess(retval, lp, overID, icx);     
      if (first) {
        first = false;        
        TopoRepairInfo tri = repairTreeTopologyGuts(lp, icx, overID, monitor, currFrac, currFrac, null);
        if (tri.topoFix == BusProperties.COULD_NOT_REPAIR) {
          skipFails.addAll(nonOrtho);
          OrthoRepairInfo ori = new OrthoRepairInfo(skipFails.size(), false);
          PropChange[] retArr = new PropChange[0];     
          ori.addPropChanges(retArr);
          return (ori);
        }
      }
      LinkSegmentID lsid = lp.getDeepestNonOrtho(icx, skipFails);
      boolean isDirect = lp.isDirect();
      currFrac += progFrac;
      if (currFrac > endFrac) {
        currFrac = endFrac;
      }
      if (monitor != null) {
        if (lsid == null) {
          currFrac = endFrac;
        }
        boolean keepGoing = monitor.updateProgress((int)(currFrac * 100.0));
        if (!keepGoing) {
          throw new AsynchExitRequestException();
        }
      }
      if (lsid == null) {
        break;
      }
      if (!lp.fixNonOrtho(lsid, icx, minCorners, overID, monitor)) {
        //
        // Issue #210. In complex cases, direct links will not orthogonalize. If this happens,
        // we split the link and give it another chance.
        //
        if (isDirect) {
          HashSet<Point2D> points = new HashSet<Point2D>();
          LinkSegment segGro = lp.getSegmentGeometryForID(lsid, icx, false);
          points.add(segGro.getStart());
          points.add(segGro.getEnd());
          Point2D split = AffineCombination.combination(points, UiUtil.GRID_SIZE);   
          if (!split.equals(segGro.getStart()) && !split.equals(segGro.getEnd())) {
            lp.linkSplitSupport(lsid, split);
            undoPostProcess(retval, lp, overID, icx);      
            retList.add(retval);
          } else {
            skipFails.add(lsid);
          }
        } else {
          skipFails.add(lsid);
        }
        continue;
      }
      double nextFrac = currFrac + progFrac;
      repairTreeTopologyGuts(lp, icx, overID, monitor, currFrac, nextFrac, null);
      undoPostProcess(retval, lp, overID, icx);      
      retList.add(retval);
    }
    OrthoRepairInfo retval = new OrthoRepairInfo(skipFails.size(), false);
    PropChange[] retArr = new PropChange[retList.size()];
    retList.toArray(retArr);
    retval.addPropChanges(retArr);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Fix a non-ortho segment
  */
  
  public OrthoRepairInfo fixNonOrtho(LinkProperties bp, LinkSegmentID segID, StaticDataAccessContext icx,
                                     boolean minCorners, String ovrKey, BTProgressMonitor monitor, double startFrac, double endFrac) 
                                       throws AsynchExitRequestException {
    // BIG NETWORK? 35 seconds to do this for one segment. Maybe 8 of that are the topology fixes. Rest is that the
    // tree strategies take about 220ms each to check for correctness.
    PropChange pch = new PropChange();
    undoPreProcess(pch, bp, ovrKey, icx);
    double midFrac = (endFrac - startFrac) / 2.0;
    repairTreeTopologyGuts(bp, icx, ovrKey, monitor, startFrac, midFrac, null);
    boolean success = bp.fixNonOrtho(segID, icx, minCorners, ovrKey, monitor);
    OrthoRepairInfo retval = new OrthoRepairInfo((success) ? 0 : 1, true);
    if (!success) {
      return (retval);
    }
    if (monitor != null) {
      boolean keepGoing = monitor.updateProgress((int)(endFrac * 100.0));
      if (!keepGoing) {
        throw new AsynchExitRequestException();
      }
    }
    repairTreeTopologyGuts(bp, icx, ovrKey, monitor, midFrac, endFrac, null);
    undoPostProcess(pch, bp, ovrKey, icx);   
    PropChange[] retArr = new PropChange[1];
    retArr[0] = pch;
    retval.addPropChanges(retArr);  
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Make a linkage direct
  */
  
  public PropChange makeLinkageDirect(BusProperties bp) {

    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, null, null);
    bp.makeDirect(); 
    undoPostProcess(retval, bp, null, null);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Make a linkage direct for given ID
  */
  
  public PropChange makeLinkageDirectForLink(String linkID, NodeInsertionDirective nid, StaticDataAccessContext rcx) {
    
    BusProperties bp = getLinkProperties(linkID);
    if (!bp.isSingleDropTree()) {
      return (null);
    }
    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, null, null);
    bp.makeDirect(); 
    
    if ((nid != null) && (nid.landingCorners != null)) {
      int numLand = nid.landingCorners.size();
      for (int i = 0; i < numLand; i++) {
        Point2D newPt = nid.landingCorners.get(i);
        int idType = (bp.isDirect()) ? LinkSegmentID.DIRECT_LINK : LinkSegmentID.END_DROP;
        LinkSegmentID dropSegID = LinkSegmentID.buildIDForType(linkID, idType);    
        splitBusLink(dropSegID, newPt, bp, null, rcx);
      }
    }    
    undoPostProcess(retval, bp, null, null);
    return (retval);
  }   

  /***************************************************************************
  **
  ** Split the Linkage at the given point for a bus
  */
  
  public PropChange splitBusLink(LinkSegmentID segID, Point2D pt, LinkProperties lp, String ovrKey, StaticDataAccessContext rcx) {

    PropChange retval = new PropChange();
    undoPreProcess(retval, lp, ovrKey, rcx);
    if (lp.linkSplitSupport(segID, pt) == null) {
      return (null);
    }    
    undoPostProcess(retval, lp, ovrKey, rcx);
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Split the Linkage at the given point for a bus. NO undo; use for batch undo cse only
  */
  
  public void splitBusLinkBatchOnlyNoUndo(LinkSegmentID segID, Point2D pt, LinkProperties lp) {
    lp.linkSplitSupport(segID, pt);
    return;   
  }

  /***************************************************************************
  **
  ** Split the direct Linkage from a source in half
  */
  
  public PropChange splitDirectLinkInHalf(String srcID, StaticDataAccessContext icx) {
    BusProperties bp = getBusForSource(srcID);
    if (!bp.isDirect()) {
      return (null);
    }
    PropChange retval = new PropChange();
    undoPreProcess(retval, bp, null, null); 
    bp.splitNoSegmentBus(icx);
    undoPostProcess(retval, bp, null, null);
    return (retval);
    
  }  
 
  /***************************************************************************
  **
  ** Handle property changes for changing the source node of a tree
  */
  
  public PropChange[] supportLinkSourceBreakoff(LinkSegmentID segID, Set<String> resolved, 
                                                String newSourceID, LinkSegmentID newConnectionID) {

    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    if (resolved.isEmpty()) {
      throw new IllegalArgumentException();
    }
    String oneID = resolved.iterator().next();
    BusProperties bp = getLinkProperties(oneID);
    
    //
    // Build a new tree for the portion below the source break:
    //
    
    BusProperties newProps = (BusProperties)bp.breakTreePortionToNewSource(segID, resolved, newSourceID, null);    
    
    //
    // Kill off the old links in the existing tree:
    //
 
    Iterator<String> alit = resolved.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      PropChange pc = removeLinkProperties(nextLinkID);
      changes.add(pc);
    }
    
    //
    // Glue to new tree if appropriate:
    //

    PropChange pc2 = null;
    if (newConnectionID != null) {
      BusProperties newbp = getBusForSource(newSourceID);
      pc2 = new PropChange();
      undoPreProcess(pc2, newbp, null, null);
      newbp.mergeTreeToTreeAtSegment(newProps, newConnectionID);      
      undoPostProcess(pc2, newbp, null, null);
      changes.add(pc2);
      newProps = newbp;
    }

    //
    // Assign new props to new links.  Make sure they are assigned a cloned copy
    // on redo, since future ops will mess with the installed version.
    //
    
    PropChange pc3 = new PropChange();
    pc3.orig = null;
    // If merged into a tree, we need to ref _that_ cloned redo copy!
    pc3.newProps = (pc2 != null) ? pc2.newProps : (BusProperties)newProps.clone();
    pc3.layoutKey = name_;
    pc3.linkIDs = new HashSet<String>();
    Iterator<String> lmvit = resolved.iterator();
    while (lmvit.hasNext()) {
      String linkID = lmvit.next();
      linkProps_.put(linkID, newProps);
      pc3.linkIDs.add(linkID);
    }
    changes.add(pc3);
 
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));
  } 
 
  /***************************************************************************
  **
  ** Handle property changes for changing the attachment of a net module link subtree
  ** to another tree from the same module, or directly to the module
  */
  
  public PropChange[] supportModuleLinkTreeSwitch(LinkSegmentID segID, Set<String> resolved, String ovrKey,
                                                  String oldTreeID, String newTreeID,
                                                  LinkSegmentID newConnectionID, Point2D padPoint, 
                                                  Vector2D sideDir, StaticDataAccessContext rcx) {

    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    
    NetOverlayProperties noProps = getNetOverlayProperties(ovrKey);
    NetModuleLinkageProperties nmlp = noProps.getNetModuleLinkagePropertiesFromTreeID(oldTreeID);
    
    
    //
    // If we are not connecting to another segment, we are going direct to the module, and are creating
    // a new tree, and need an ID and the padPoint is important.  Else, we use a fake ID and don't
    // really care about the padPoint (it will be ditched).
    //
    
    String newKey;
    if (newConnectionID == null) {
      // We want trees to have globally unique IDs.  Dropping this is undo is handled by 
      // e.g. setNetModuleLinkageProperties setup
      newKey = ((DBGenome)rcx.getGenomeSource().getRootDBGenome()).getNextKey();
      newTreeID = newKey;
    } else {
      newKey = "fakeID";
      padPoint = new Point2D.Double(0.0, 0.0);  // we don't care...
      sideDir = new Vector2D(0.0, 0.0);
    }
    NetModuleLinkageProperties.DirectLinkExtraInfo dlei = 
      new NetModuleLinkageProperties.DirectLinkExtraInfo(newKey, padPoint, sideDir);
    
    //
    // Build a new tree for the portion below the break.  We are NOT changing the source ID!
    //
    
    NetModuleLinkageProperties newProps = 
      (NetModuleLinkageProperties)nmlp.breakTreePortionToNewSource(segID, resolved, nmlp.getSourceTag(), dlei); 
    
    //
    // Kill off the old links in the existing tree:
    //
 
    Iterator<String> alit = resolved.iterator();
    while (alit.hasNext()) {
      String nextLinkID = alit.next();
      PropChange pc = removeNetModuleLinkageProperties(nextLinkID, ovrKey, rcx);
      changes.add(pc);
    }
    
    //
    // Glue to new tree if appropriate.  Else it becomes a new tree of its own
    //

    PropChange pc2 = null;
    if (newConnectionID != null) {
      NetModuleLinkageProperties nmlpNew = noProps.getNetModuleLinkagePropertiesFromTreeID(newTreeID);
      pc2 = new PropChange();
      undoPreProcess(pc2, nmlpNew, ovrKey, rcx);      
      nmlpNew.mergeTreeToTreeAtSegment(newProps, newConnectionID);      
      undoPostProcess(pc2, nmlpNew, ovrKey, rcx);
    } else {
      pc2 = setNetModuleLinkageProperties(newKey, newProps, ovrKey, rcx);
    }
    changes.add(pc2);

    //
    // Tie links to the new props.
    //
       
    Iterator<String> lmvit = resolved.iterator();
    while (lmvit.hasNext()) {
      String linkID = lmvit.next();
      PropChange pc3 = new PropChange();
      pc3.nmlpTieLinkIDOrig = linkID;
      pc3.nmlpTieTreeIDOrig = null;
      pc3.nmlpTieLinkIDNew = linkID;
      pc3.nmlpTieTreeIDNew = newTreeID;  
      pc3.layoutKey = name_;
      pc3.nopRef = ovrKey;
      noProps.tieNetModuleLinkagePropertiesForLink(linkID, newTreeID);
      changes.add(pc3);
    }
 
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));
  } 

  /***************************************************************************
  **
  ** Handle property changes for splitting a link segment with a new node.
  */
  
  public PropChange[] supportLinkNodeInsertion(LinkSegmentID segID, Set<String> resolved, 
                                               String newNodeID, 
                                               String firstLinkID, Map<String, String> linkMap,
                                               StaticDataAccessContext icx,
                                               Map<String, NodeInsertionDirective> needDirectFixup, NodeInsertionDirective nid) {
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    if (resolved.isEmpty()) {
      throw new IllegalArgumentException();
    }
    String oneID = resolved.iterator().next();
    BusProperties bp = getLinkProperties(oneID);
    PropChange pc = new PropChange();
    undoPreProcess(pc, bp, null, icx);
    
    //
    // The new props is the new downstream tree that needs to be installed
    // on all resolved links.  The existing bp is still needed for all the
    // unaffected linkages, and for the new linkage.  It still holds the
    // linkage we are doing to delete!
    //
    BusProperties newProps = bp.insertNodeInTree(segID, newNodeID, firstLinkID, linkMap, 
                                                 icx, needDirectFixup, nid);
    undoPostProcess(pc, bp, null, icx);  
    changes.add(pc);
    
    //
    // Set the new first link to have the same modified property, and make sure
    // we get updated with the same redo copy as above when doing redo.
    //
    
    PropChange pc2 = new PropChange();
    pc2.orig = null;
    pc2.newProps = pc.newProps;
    pc2.layoutKey = name_;
    linkProps_.put(firstLinkID, bp);
    pc2.linkIDs = new HashSet<String>();
    pc2.linkIDs.add(firstLinkID);
    changes.add(pc2);    
    
    //
    // Assign new props to new links.  Make sure they are assigned a cloned copy
    // on redo, since future ops will mess with the installed version.  (Previous
    // redo bug jsut assigned the original newProps as the redo copy, but this gets
    // messed with in future operations!
    //
    
    PropChange pc3 = new PropChange();
    pc3.orig = null;
    pc3.newProps = newProps.clone();
    pc3.layoutKey = name_;
    pc3.linkIDs = new HashSet<String>();
    String mappedOneLink = null;
    Iterator<String> lmvit = linkMap.values().iterator();
    while (lmvit.hasNext()) {
      String linkID = lmvit.next();
      if (mappedOneLink == null) {
        mappedOneLink = linkID;
      }
      linkProps_.put(linkID, newProps);
      pc3.linkIDs.add(linkID);
    }
    changes.add(pc3);
    
    
    //
    // Add the launch and landing approach fixups if the node wants them:
    //
    
    if ((nid != null) && (nid.landingCorners != null)) {
      BusProperties bp2 = getLinkProperties(firstLinkID);
      int numLand = nid.landingCorners.size();
      for (int i = 0; i < numLand; i++) {
        Point2D newPt = nid.landingCorners.get(i);
        int idType = (bp2.isDirect()) ? LinkSegmentID.DIRECT_LINK : LinkSegmentID.END_DROP;
        LinkSegmentID dropSegID = LinkSegmentID.buildIDForType(firstLinkID, idType);    
        changes.add(splitBusLink(dropSegID, newPt, bp2, null, null));
      }
    }
    
    if ((nid != null) && (nid.launchCorners != null)) {
      BusProperties bp2 = getLinkProperties(mappedOneLink);
      int numLau = nid.launchCorners.size();
      for (int i = 0; i < numLau; i++) {
        Point2D newPt = nid.launchCorners.get(i);
        int idType = (bp2.isDirect()) ? LinkSegmentID.DIRECT_LINK : LinkSegmentID.START_DROP;
        LinkSegmentID dropSegID = LinkSegmentID.buildIDForType(mappedOneLink, idType);    
        changes.add(splitBusLink(dropSegID, newPt, bp2, null, null));
      }
    }    
 
    PropChange[] retval = new PropChange[changes.size()];
    return (changes.toArray(retval));
  } 
  
  /***************************************************************************
  **
  ** Handle property changes for splitting a link to insert a new node, while
  ** trying to match the split with the root genome.
  */
  
  public InheritedLinkNodeInsertionResult supportInheritedLinkNodeInsertion(LinkSegmentID rootSegID, 
                                                        Set<String> resolved,
                                                        BusProperties rootProps, Point2D rootSplit,
                                                        StaticDataAccessContext rcxRoot,
                                                        StaticDataAccessContext icx,
                                                        String newNodeID, String firstLinkID, 
                                                        Map<String, String> linkMap, Map<String, NodeInsertionDirective> needDirectFixup) {
    
    InheritedInsertionInfo iii = findInheritedMatchingLinkSegment(rootSegID, resolved, rootProps, rootSplit, rcxRoot, icx);
    //
    // Didn't have any luck
    
    if (iii == null) {
      return (null);
    }
    
    //
    // Set the new node location to match:
    //
    
    NodeProperties props = getNodeProperties(newNodeID);
    String oneID = resolved.iterator().next();
    BusProperties bp = getLinkProperties(oneID);
    LinkSegment segGeom = bp.getSegmentGeometryForID(iii.segID, icx, true);
    Vector2D travel = segGeom.getRun();
    NodeInsertionDirective nid = props.getRenderer().getInsertionDirective(travel, iii.pt);
    PropChange retval = new PropChange();
    retval.nOrig = new NodeProperties(props);
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
    retval.layoutKey = name_;
    double xCoord = UiUtil.forceToGridValue(iii.pt.getX() + nid.offset.getX(), UiUtil.GRID_SIZE);
    double yCoord = UiUtil.forceToGridValue(iii.pt.getY() + nid.offset.getY(), UiUtil.GRID_SIZE);
    Point2D nodeLoc = new Point2D.Double(xCoord, yCoord);
    props.setLocation(nodeLoc);
    props.setOrientation(nid.orientation);
    retval.nNewProps = props.clone();

    PropChange[] slnPC = supportLinkNodeInsertion(iii.segID, resolved, newNodeID, firstLinkID, linkMap,
                                                  icx, needDirectFixup, nid);
    PropChange[] newRetval = new PropChange[slnPC.length + 1];
    System.arraycopy(slnPC, 0, newRetval, 0, slnPC.length);
    newRetval[slnPC.length] = retval;
    
    //
    // Record the pad changes we are going to need:
    //    
    
    Genome myGenome = icx.getCurrentGenome();
    HashMap<String, PadCalculatorToo.PadResult> padChanges = new HashMap<String, PadCalculatorToo.PadResult>();
    Linkage link = myGenome.getLinkage(firstLinkID);
    PadCalculatorToo.PadResult pres = new PadCalculatorToo.PadResult(link.getLaunchPad(), nid.landingPad);    
    padChanges.put(firstLinkID, pres);
    
    Iterator<String> lmvit = linkMap.values().iterator();
    while (lmvit.hasNext()) {
      String linkID = lmvit.next();
      link = myGenome.getLinkage(linkID);
      pres = new PadCalculatorToo.PadResult(nid.launchPad, link.getLandingPad());
      padChanges.put(linkID, pres);
    }
 
    return (new InheritedLinkNodeInsertionResult(newRetval, padChanges, nid));
  } 
  
  /***************************************************************************
  **
  ** Handle more difficult inserted node positioning.
  */
  
  public PropChange bestFitNodePlacementForInsertion(Layout rootLayout, String newNodeID, StaticDataAccessContext rcx) {
    //
    // No really good fit, so place the node in same approx position w.r.t. sources or targets
    // as it appears in the root layout, using elements in the same region as the new node.
    //
    GenomeInstance gi = (GenomeInstance)rcx.getCurrentGenome();
    
    String rootNewNode = GenomeItemInstance.getBaseID(newNodeID);
    Point2D newRootPos = rootLayout.getNodeProperties(rootNewNode).getLocation();
    NodeInstance newInstance = (NodeInstance)gi.getNode(newNodeID);
    GroupMembership newNodeGroupMemb = gi.getNodeGroupMembership(newInstance);
    if (newNodeGroupMemb.mainGroups.isEmpty()) {
      throw new IllegalStateException();
    }
    String newNodeGrpID = newNodeGroupMemb.mainGroups.iterator().next();
    

    HashSet<Point2D> candPosSet = new HashSet<Point2D>();
    HashSet<Point2D> backupCandPosSet = new HashSet<Point2D>();
    Iterator<Linkage> lit = gi.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String target = link.getTarget();
      String source = link.getSource();
      String otherNodeID;
      if (target.equals(newNodeID)) {
        otherNodeID = source;
      } else if (source.equals(newNodeID)) {
        otherNodeID = target;
      } else {
        continue;
      }
      
      String rootOtherNode = GenomeItemInstance.getBaseID(otherNodeID);
      Point2D otherRootPos = rootLayout.getNodeProperties(rootOtherNode).getLocation();
      Point2D otherPos = getNodeProperties(otherNodeID).getLocation();
      NodeInstance otherInstance = (NodeInstance)gi.getNode(otherNodeID);
      GroupMembership otherNodeGroupMemb = gi.getNodeGroupMembership(otherInstance);
      if (otherNodeGroupMemb.mainGroups.isEmpty()) {
        throw new IllegalStateException();
      }
      String otherNodeGrpID = otherNodeGroupMemb.mainGroups.iterator().next();
 
      Vector2D rootOffset = new Vector2D(otherRootPos, newRootPos);
      Point2D candPos = rootOffset.add(otherPos);
      backupCandPosSet.add(candPos);
      if (otherNodeGrpID.equals(newNodeGrpID)) {
        candPosSet.add(candPos);
      }
    }
    
    //
    // We prefer to place the element WRT targets in it own region, if possible.
    //
    
    Set<Point2D> usePos = (candPosSet.isEmpty()) ? backupCandPosSet : candPosSet;
    
    Point2D newPoint = AffineCombination.combination(usePos, UiUtil.GRID_SIZE);
    newPoint = spiralNodePlacement(rcx, newNodeID, newPoint);
        
    //
    // Set the new node location to match:
    //
    
    NodeProperties props = getNodeProperties(newNodeID);     
    PropChange retval = new PropChange();
    retval.nOrig = props.clone();
    retval.origLinks = new HashSet<BusProperties>();
    retval.newLinks = new HashSet<BusProperties>();
    retval.layoutKey = name_;    
    props.setLocation(newPoint);
    retval.nNewProps = props.clone();
    return (retval);
  }  

  /***************************************************************************
  **
  ** Handle more difficult inserted node positioning.
  */
  
  private Point2D spiralNodePlacement(StaticDataAccessContext rcx, String nodeID, Point2D guessPt) {  
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid targLinkGrid;
    try {
      targLinkGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, null);
    } catch (AsynchExitRequestException ex) {
      throw new IllegalStateException();  // Cannot happen since monitor is null
    } 
    
    PatternGrid targGrid = targLinkGrid.extractPatternGrid(true);
    Node node = rcx.getCurrentGenome().getNode(nodeID);
    NodeProperties props = getNodeProperties(nodeID);   
    PatternGrid nodeGrid = new PatternGrid();
    props.getRenderer().renderToPatternGrid(node, rcx, nodeGrid); 
    Pattern pat = nodeGrid.generatePattern();

    Point startPt = new Point((int)guessPt.getX() / 10, (int)guessPt.getY() / 10);
    PatternPlacerSpiral pps = new PatternPlacerSpiral(targGrid, pat, startPt, 
                                                      PatternPlacerSpiral.CW, PatternPlacerSpiral.UP);
    Point retval = pps.locatePattern();
    pps.sinkPattern(retval);
    Point2D retval10 = new Point2D.Double(retval.getX() * 10.0, retval.getY() * 10.0);   
    return (retval10);
  }

  /***************************************************************************
  **
  ** Find the best matching inherited link segment from the root 
  */
  
  public InheritedInsertionInfo findInheritedMatchingLinkSegment(LinkSegmentID rootSegID, Set<String> resolved, 
                                                                 BusProperties rootBus, Point2D rootSplit,
                                                                 StaticDataAccessContext rcxRoot, 
                                                                 StaticDataAccessContext rcx) {

    //
    // NOTE: FIX ME???  Useless corner points are not being ignored in this process.  Is this
    // a good thing (better match if tree is a subset of root tree) or a bad thing (weird matches
    // that only make sense if you visualize useless points).  Maybe the best is to eliminate
    // useless points on the root but keep any in the child layout that map to useful points in the
    // root.
    //
    
    
    if (resolved.isEmpty()) {                             
      throw new IllegalArgumentException();
    }
    String oneID = resolved.iterator().next();
    BusProperties bp = getLinkProperties(oneID);
    LinkSegmentID lsid = bp.findSegmentSupportingLinkSet(rootBus, rootSegID, rcxRoot, resolved, rcx);
    if (lsid == null) {
      return (null);
    }
    
    Point2D splitPt = (Point2D)rootSplit.clone();
    StaticDataAccessContext irx = new StaticDataAccessContext(rcx, rcxRoot.getCurrentGenome(), rcxRoot.getCurrentLayout());
    LinkSegment rseg = rootBus.getSegmentGeometryForID(rootSegID, irx, true);

    
    double rFrac = rseg.fractionOfRun(splitPt);
    LinkSegment lseg = bp.getSegmentGeometryForID(lsid, rcx, true);
    splitPt = lseg.pointAtFraction(rFrac);
    return (new InheritedInsertionInfo(lsid, splitPt));
  }  

  /***************************************************************************
  **
  ** Move the link label by the given amount 
  */
  
  public PropChange moveLinkLabel(BusProperties lp, double dx, double dy) {
    PropChange retval = new PropChange();
    undoPreProcess(retval, lp, null, null);
    
    Point2D pt = lp.getTextPosition();
    // FIX ME: This is a hack:
    if (pt == null) {
      pt = new Point2D.Double(0.0, 0.0);
    }
    Point2D newPt = new Point2D.Double(pt.getX() + dx, pt.getY() + dy);
    lp.setTextPosition(newPt);

    undoPostProcess(retval, lp, null, null);    
    return (retval);                                  
  }
  
  /***************************************************************************
  **
  ** Move the linkage by the given amount 
  */
  
  public PropChange moveBusLink(LinkSegmentID[] segIDs, double dx, double dy,
                                Point2D strt, BusProperties bp) {

    if (segIDs == null) {
      return (null);
    }                                  
                                  
    PropChange retval = new PropChange();
    retval.orig = new BusProperties(bp);
    retval.layoutKey = name_;                           
    retval.linkIDs = new HashSet<String>();
    //
    // Find out which keys are being modified when we change this
    // bus props:
    //
    Iterator<String> lpit = linkProps_.keySet().iterator();
    while (lpit.hasNext()) {
      String nextKey = lpit.next();
      BusProperties lp = linkProps_.get(nextKey);
      if (lp == bp) {
        retval.linkIDs.add(nextKey);
      }
    }
    
    dx = Math.round(dx / 10.0) * 10.0;
    dy = Math.round(dy / 10.0) * 10.0; 
    
    bp.moveBusLinkSegments(segIDs, strt, dx, dy);

    retval.newProps = new BusProperties(bp);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace the given link properties 
  */
  
  public PropChange replaceLinkProperties(BusProperties oldProp, BusProperties newProp) {                          
                                  
    PropChange retval = new PropChange();
    retval.orig = new BusProperties(oldProp);
    retval.newProps = new BusProperties(newProp);
    
    retval.layoutKey = name_;                           
    retval.linkIDs = new HashSet<String>();
    
    //
    // Find out which keys are being modified when we change this
    // prop, and change it:
    //
    
    Iterator<String> lpit = linkProps_.keySet().iterator();
    while (lpit.hasNext()) {
      String nextKey = lpit.next();
      BusProperties lp = linkProps_.get(nextKey);
      if (lp == oldProp) {
        retval.linkIDs.add(nextKey);
      }
    }
    
    Iterator<String> rtvit = retval.linkIDs.iterator();
    while (rtvit.hasNext()) {
      String nextKey = rtvit.next();
      linkProps_.put(nextKey, newProp);
    }
    
    return (retval);
  }    

  /***************************************************************************
  **
  ** Undo a link move
  */
  
  public void linkChangeUndo(PropChange undo) {
    //
    // Crank through the linkIDs and return the original.
    // If we have an added ID, remove it.
    // If we have a removed ID, add it.
    //
    Iterator<String> lidit = undo.linkIDs.iterator();
    while (lidit.hasNext()) {
      String nextKey = lidit.next();
      if (undo.orig != null) {
        linkProps_.put(nextKey, undo.orig);
      } else {
        linkProps_.remove(nextKey);
      }
    }
    if (undo.removedLinkID != null) {
      linkProps_.put(undo.removedLinkID, undo.orig);
    }
    if (undo.addedLinkID != null) {
      linkProps_.remove(undo.addedLinkID);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a link change
  */
  
  public void linkChangeRedo(PropChange redo) {
    //
    // Crank through the linkIDs and return the original.
    // If we have an added ID, remove it.
    // If we have a removed ID, add it.
    //
    Iterator<String> lidit = redo.linkIDs.iterator();
    while (lidit.hasNext()) {
      String nextKey = lidit.next();
      if (redo.newProps != null) {
        linkProps_.put(nextKey, redo.newProps);
      } else {
        linkProps_.remove(nextKey);
      }
    }
    if (redo.removedLinkID != null) {
      linkProps_.remove(redo.removedLinkID);
    }
    if (redo.addedLinkID != null) {
      linkProps_.put(redo.addedLinkID, redo.newProps);
    }
    return;
  }

  /***************************************************************************
  **
  ** Move a note
  */
  
  public PropChange moveNote(String selection, double dx, double dy) {

    NoteProperties props = getNoteProperties(selection);    
    
    PropChange retval = new PropChange();
    retval.ntOrig = props.clone();
    retval.layoutKey = name_;    
    Point2D loc = props.getLocation();
    props.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    retval.ntNewProps = props.clone();

    return (retval);
  }
  
  /***************************************************************************
  **
  ** Replace the given note properties 
  */
  
  public PropChange replaceNoteProperties(String noteID, NoteProperties newProps) {                          
                                  
    NoteProperties oldProps = getNoteProperties(noteID);    
    
    PropChange retval = new PropChange();
    retval.ntOrig = oldProps.clone();
    retval.layoutKey = name_;
    
    noteProps_.put(noteID, newProps);
    retval.ntNewProps = newProps.clone();    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Move a note
  */
  
  public PropChange moveDataLocation(String key, double dx, double dy) {

    Point2D loc = getDataLocation(key);    
    
    PropChange retval = new PropChange();
    retval.dLocKey = key;    
    retval.dLocOrig = (Point2D)loc.clone();
    retval.layoutKey = name_;
    Point2D newLoc = new Point2D.Double(loc.getX() + dx, loc.getY() + dy);
    dataProps_.put(key, newLoc);
    retval.dLocNew = (Point2D)newLoc.clone();
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Undo a note change
  */
  
  public void noteChangeUndo(PropChange unmove) {
    if (unmove.ntOrig == null) {
      noteProps_.remove(unmove.ntNewProps.getReference());
    } else if (unmove.ntNewProps == null) {
      noteProps_.put(unmove.ntOrig.getReference(), unmove.ntOrig);
    } else {
      String oldRef = unmove.ntOrig.getReference();
      String newRef = unmove.ntNewProps.getReference();
      noteProps_.put(oldRef, unmove.ntOrig);
      if (!oldRef.equals(newRef)) {
        noteProps_.remove(newRef);
      }
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a note change
  */
  
  public void noteChangeRedo(PropChange unmove) {
    if (unmove.ntNewProps == null) {
      noteProps_.remove(unmove.ntOrig.getReference());
    } else if (unmove.ntOrig == null) {
      noteProps_.put(unmove.ntNewProps.getReference(), unmove.ntNewProps);
    } else {
      String oldRef = unmove.ntOrig.getReference();
      String newRef = unmove.ntNewProps.getReference();
      noteProps_.put(newRef, unmove.ntNewProps);
      if (!oldRef.equals(newRef)) {
        noteProps_.remove(oldRef);
      }
    }
    return;    
  }

  /***************************************************************************
  **
  ** Undo a data move
  */
  
  public void dataPosChangeUndo(PropChange unmove) {
    Point2D loc = unmove.dLocOrig;
    if (loc == null) {
      dataProps_.remove(unmove.dLocKey);
    } else {
      dataProps_.put(unmove.dLocKey, (Point2D)loc.clone());
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a data move
  */
  
  public void dataPosChangeRedo(PropChange unmove) {
    Point2D loc = unmove.dLocNew;
    if (loc == null) {
      dataProps_.remove(unmove.dLocKey);
    } else {
      dataProps_.put(unmove.dLocKey, (Point2D)loc.clone());
    }
    return;
  }  

  /***************************************************************************
  **
  ** Undo a metadata change
  */
  
  public void metaChangeUndo(PropChange undo) {
    this.lmeta_ = undo.metaOrig.clone();
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a metadata change
  */
  
  public void metaChangeRedo(PropChange redo) {
    this.lmeta_ = redo.newMeta.clone();
    return;
  }   

  /***************************************************************************
  **
  ** Move a group (i.e. just the label)
  */
  
  public PropChange moveGroup(String selection, double dx, double dy) {

    GroupProperties props = getGroupProperties(selection);    
    
    PropChange retval = new PropChange();
    retval.grOrig = new GroupProperties(props);
    retval.layoutKey = name_;    
    Point2D loc = props.getLabelLocation();
    if (loc == null) {
      return (null);
    }
    props.setLabelLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    retval.grNewProps = new GroupProperties(props);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the top group order
  */
  
  public int getTopGroupOrder() {
    if (groupProps_.size() == 0) {
      return (0);
    }
    int retval = Integer.MIN_VALUE;
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      if (chkOrder > retval) {
        retval = chkOrder;
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the bottom group order
  */
  
  public int getBottomGroupOrder() {
    if (groupProps_.size() == 0) {
      return (0);
    }
    int retval = Integer.MAX_VALUE;
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      if (chkOrder < retval) {
        retval = chkOrder;
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the order to draw groups
  */
  
  public List<String> getGroupDrawingOrder() {
    TreeMap<GroupProperties.GroupOrdering, String> goMap = new TreeMap<GroupProperties.GroupOrdering, String>();
    Iterator<String> gpit = groupProps_.keySet().iterator();
    while (gpit.hasNext()) {
      String gpKey = gpit.next();
      GroupProperties chkProps = groupProps_.get(gpKey);
      GroupProperties.GroupOrdering gOrd = chkProps.getGroupOrdering();
      goMap.put(gOrd, gpKey);
    }    
    return (new ArrayList<String>(goMap.values()));
  }  
  
  /***************************************************************************
  **
  ** Get the order to intersect groups
  */
  
  public List<String> getGroupIntersectionOrder() {
    List<String> drawOrder = getGroupDrawingOrder();
    Collections.reverse(drawOrder);
    return (drawOrder);
  }    
 
  /***************************************************************************
  **
  ** Raise a group.  Return null if nothing happened.
  */
  
  public PropChange[] raiseGroup(String selection) {

    //
    // Groups with the highest order number are rendered last (on top).  Find
    // the group with the next highest number and swap.  Don't forget to swap
    // for subgroups too!
    //
   
    GroupProperties props = getGroupProperties(selection);    
    int oldOrder = props.getOrder();
    int flipOrder = Integer.MAX_VALUE;
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      if ((chkOrder > oldOrder) && (chkOrder < flipOrder)) {
        flipOrder = chkOrder;
      }
    }
    
    if (flipOrder == Integer.MAX_VALUE) {
      return (null);
    }
    
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      boolean raise = (chkOrder == oldOrder);
      boolean lower = (chkOrder == flipOrder);      
      if (raise || lower) {
        PropChange retval = new PropChange();
        retval.grOrig = new GroupProperties(chkProps);
        retval.layoutKey = name_;
        chkProps.setOrder((raise) ? flipOrder : oldOrder);
        retval.grNewProps = new GroupProperties(chkProps);
        changes.add(retval);
      }
    }    
    PropChange[] retval = new PropChange[changes.size()];
    changes.toArray(retval);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Lower a group.  Return null if nothing happened.
  */
  
  public PropChange[] lowerGroup(String selection) {

    //
    // Groups with the highest order number are rendered last (on top).  Find
    // the group with the next highest number and swap.  Don't forget to swap
    // for subgroups too!
    //
   
    GroupProperties props = getGroupProperties(selection);    
    int oldOrder = props.getOrder();
    int flipOrder = Integer.MIN_VALUE;
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      if ((chkOrder < oldOrder) && (chkOrder > flipOrder)) {
        flipOrder = chkOrder;
      }
    }
    
    if (flipOrder == Integer.MIN_VALUE) {
      return (null);
    }
    
    ArrayList<PropChange> changes = new ArrayList<PropChange>();
    gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties chkProps = gpit.next();
      int chkOrder = chkProps.getOrder();
      boolean raise = (chkOrder == flipOrder);
      boolean lower = (chkOrder == oldOrder);      
      if (raise || lower) {
        PropChange retval = new PropChange();
        retval.grOrig = new GroupProperties(chkProps);
        retval.layoutKey = name_;
        chkProps.setOrder((raise) ? oldOrder : flipOrder);
        retval.grNewProps = new GroupProperties(chkProps);
        changes.add(retval);
      }
    }    
    PropChange[] retval = new PropChange[changes.size()];
    changes.toArray(retval);
    return (retval);
  }   
  
  /***************************************************************************
  **
  **  Replace a group properties
  */
  
  public PropChange replaceGroupProperties(GroupProperties oldProp, GroupProperties newProp) {

    PropChange retval = new PropChange();
    retval.grOrig = new GroupProperties(oldProp);
    retval.layoutKey = name_;
        
    groupProps_.put(newProp.getReference(), newProp);
    
    retval.grNewProps = new GroupProperties(newProp);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Undo a group change
  */
  
  public void groupChangeUndo(PropChange undo) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.grOrig != null) && (undo.grNewProps != null)) {
      groupProps_.put(undo.grOrig.getReference(), undo.grOrig);
    } else if (undo.grOrig == null) {
      groupProps_.remove(undo.grNewProps.getReference());
    } else {
      groupProps_.put(undo.grOrig.getReference(), undo.grOrig);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a group move
  */
  
  public void groupChangeRedo(PropChange undo) {
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((undo.grOrig != null) && (undo.grNewProps != null)) {
      groupProps_.put(undo.grNewProps.getReference(), undo.grNewProps);
    } else if (undo.grOrig == null) {
      groupProps_.put(undo.grNewProps.getReference(), undo.grNewProps);
    } else {
      groupProps_.remove(undo.grOrig.getReference());
    }
    return;
  }  

  /***************************************************************************
  **
  ** Fold in a new property
  **
  */
  
  public PropChange foldInNewProperty(Linkage newLink, 
                                      BusProperties spTree, StaticDataAccessContext irx) {
                                         
    String linkID = newLink.getID();
    String source = newLink.getSource();
    int launchPad = newLink.getLaunchPad();
    
    PropChange retval = new PropChange();

    // Find if there is another property already for this source:
    BusProperties useProp = getLinkPropertyForSource(source, launchPad, irx);
    if (useProp == null) {
      linkProps_.put(linkID, spTree);      
      retval.layoutKey = name_;                           
      retval.linkIDs = new HashSet<String>();
      retval.addedLinkID = linkID;
      retval.removedLinkID = null;
      undoPostProcess(retval, spTree, null, null);
    } else {
      undoPreProcess(retval, useProp, null, null);
      retval.addedLinkID = linkID;
      retval.removedLinkID = null;
      useProp.mergeSinglePathTree(spTree, irx);
      linkProps_.put(linkID, useProp);
      undoPostProcess(retval, useProp, null, null);      
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Write the layout to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<layout ");
    out.print("name=\"");
    out.print(name_);
    out.print("\" genome=\"");
    out.print(targetGenome_);
    out.print("\" type=\"");
    out.print("free");
    out.println("\" >");
    
    ind.up();
    lmeta_.writeXML(out, ind);
    ind.down();
    
    ind.up().indent();  
    out.println("<props>");      
    writeNodeProps(out, ind.up());
    writeLinkProps(out, ind);
    writeGroupProps(out, ind);
    writeNoteProps(out, ind);
    writeOverlayProps(out, ind);    
    writeDataProps(out, ind);    
    ind.down().indent();  
    out.println("</props>");
    ind.down().indent();
    out.println("</layout>");
    return;
  }
  
  /***************************************************************************
  **
  ** Replace references to a color
  **
  */
    
  public PropChange[] replaceColor(String oldID, String newID) {
    
    ArrayList<PropChange> retList = new ArrayList<PropChange>();

    PropChange pending = new PropChange();
    pending.layoutKey = name_;                           
 
    //
    // Node fixups
    //
  
    Iterator<NodeProperties> npi = nodeProps_.values().iterator();
    while (npi.hasNext()) {
      NodeProperties np = npi.next();
      pending.nOrig = np.clone();
      if (np.replaceColor(oldID, newID)) {
        pending.nNewProps = np.clone();
        pending.origLinks = new HashSet<BusProperties>();
        pending.newLinks = new HashSet<BusProperties>();          
        retList.add(pending);
        pending = new PropChange();
        pending.layoutKey = name_;
      } else {
        // These restorations of the original null value if nothing changes is the fix for Issue #224.
        // Undo ops typically look for the non-null members to decide what to do. Without these resets
        // (see below as well), we get messed up.
        pending.nOrig = null;
      }
    }    
    
    //
    // Group fixups
    //
    
    Iterator<GroupProperties> gpi = groupProps_.values().iterator();    
    while (gpi.hasNext()) {
      GroupProperties gp = gpi.next();
      pending.grOrig = new GroupProperties(gp);
      if (gp.replaceColor(oldID, newID)) {
        pending.grNewProps = new GroupProperties(gp);
        retList.add(pending);
        pending = new PropChange();
        pending.layoutKey = name_; 
      } else { //Issue #224
        pending.grOrig = null;
      }
    }

    //
    // Note fixups
    //
    
    Iterator<NoteProperties> ntit = noteProps_.values().iterator();
    while (ntit.hasNext()) {
      NoteProperties np = ntit.next();
      pending.ntOrig = np.clone();
      if (np.replaceColor(oldID, newID)) {
        pending.ntNewProps = np.clone();
        retList.add(pending);
        pending = new PropChange();
        pending.layoutKey = name_; 
      } else { // Issue #224
        pending.ntOrig = null;
      }
    }    
    
    //
    // Link fixups
    //
    
    HashSet<BusProperties> treeLinks = new HashSet<BusProperties>();
    Iterator<BusProperties> lpi = linkProps_.values().iterator();
    while (lpi.hasNext()) {
      BusProperties lp = lpi.next();
      if (!treeLinks.contains(lp)) {
        treeLinks.add(lp);
      }
    }
    Iterator<BusProperties> tlit = treeLinks.iterator();
    while (tlit.hasNext()) {
      BusProperties lp = tlit.next();
      undoPreProcess(pending, lp, null, null);
      if (lp.replaceColor(oldID, newID)) {
        undoPostProcess(pending, lp, null, null);
        retList.add(pending);
        pending = new PropChange(); 
      } else { // Issue #224
        pending.orig = null;                 
        pending.linkIDs = null;
      }
    }
    
    //
    // Overlay fixups
    //
    
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String ovrKey = opit.next();
      NetOverlayProperties nop = ovrProps_.get(ovrKey);
      pending.nopOrig = nop.clone();
      if (nop.replaceColor(oldID, newID)) {
        pending.nopNew = nop.clone();
        retList.add(pending);
        pending = new PropChange();
        pending.layoutKey = name_; 
      } else { // Issue #224
        pending.nopOrig = null;
      }
    }
    
    return ((retList.isEmpty()) ? null : retList.toArray(new PropChange[retList.size()]));
  }

  /***************************************************************************
  **
  ** Build identity maps for transferring all our overlay properties for models that
  ** still exist (use fullkeys to figure this out):
  */
  
  public void fillOverlayIdentityMaps(Layout.OverlayKeySet fullKeys, Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, 
                                      Map<String, String> modIDMap, Map<String, String> modLinkIDMap) {
    if (fullKeys == null) {
      return;
    }
    HashSet<String> overKeys = new HashSet<String>();
    Iterator<NetModule.FullModuleKey> mkit = fullKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
      keyMap.put(fullKey, fullKey);
      overKeys.add(fullKey.ovrKey);
    }
      
    Iterator<String> oit = overKeys.iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      NetOverlayProperties oProps = ovrProps_.get(key);
      oProps.fillModAndLinkIdentityMaps(modIDMap, modLinkIDMap);
    }   
    
    return;
  } 
 
  /***************************************************************************
  **
  ** Transfer out layout stuff for given links from legacy layout
  */
  
  public void transferLayoutFromLegacy(Map<String, String> nodeMap, Map<String, String> linkMap, Map<String, String> groupMap,
                                       Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, Map<String, String> modLinkIDMap, 
                                       Map<NetModule.FullModuleKey, DicedModuleInfo> diceMap,
                                       boolean dataOnly, Map<String, BusProperties.RememberProps> rememberMap, 
                                       StaticDataAccessContext rcxMine, StaticDataAccessContext rcxLeg) {
    name_ = rcxLeg.getCurrentLayout().name_;
    targetGenome_ = rcxLeg.getCurrentLayout().targetGenome_;
    Iterator<String> dpit = rcxLeg.getCurrentLayout().dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = rcxLeg.getCurrentLayout().dataProps_.get(key);
      if (pt != null) {
        dataProps_.put(key, (Point2D)pt.clone());
      }
    }
    
    if (dataOnly) {  // Even if data only, still transfer notes and overlays
      Iterator<String> ntit = rcxLeg.getCurrentLayout().noteProps_.keySet().iterator();
      while (ntit.hasNext()) {
        String key = ntit.next();
        NoteProperties np = rcxLeg.getCurrentLayout().noteProps_.get(key);
        noteProps_.put(key, np.clone());
      }
      transferOverlayCore(keyMap, modLinkIDMap, diceMap, rcxMine, rcxLeg);
      return;
    }
    
    transferLayoutCore(nodeMap, linkMap, groupMap, true, keyMap,
                       modLinkIDMap, diceMap, rememberMap, rcxMine, rcxLeg);
    return;
  }
  
  /***************************************************************************
  **
  ** Shift supplemental layout stuff
  */
  
  public Rectangle2D applySupplementalDataCoords(SupplementalDataCoords sdc, StaticDataAccessContext irx, OverlayKeySet fullModKeys) {
    
    if (sdc == null) {
      return (null);
    }

    Rectangle2D rect;
    Rectangle lob = getLayoutBounds(irx, true, (fullModKeys != null), 
                                   (fullModKeys != null), false, false, null, null, null, fullModKeys);
    if (lob == null) {
      rect = getSupplementalBounds();
      if (rect == null) {
        return (null);
      }
    } else {
      rect = new Rectangle2D.Double(lob.x, lob.y, lob.width, lob.height);
    }
      
    double x0 = rect.getX();
    double y0 = rect.getY();
    double xlen = rect.getWidth();
    double ylen = rect.getHeight();
    
    Iterator<String> dpit = dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = dataProps_.get(key);
      if (pt != null) {
        Point2D sdcPt = sdc.dataProps.get(key);
        if (sdcPt != null) {
          double x = (sdcPt.getX() * xlen) + x0;
          double y = (sdcPt.getY() * ylen) + y0;
          Point2D newPt = new Point2D.Double(x, y);
          UiUtil.forceToGrid(x, y, newPt, UiUtil.GRID_SIZE);          
          dataProps_.put(key, newPt);
        }
      }
    }
    
    Iterator<String> ntit = noteProps_.keySet().iterator();
    while (ntit.hasNext()) {
      String key = ntit.next();
      NoteProperties np = noteProps_.get(key);
      Point2D pt = np.getLocation();
      if (pt != null) {
        Point2D sdcPt = sdc.noteProps.get(key);
        if (sdcPt != null) {
          double x = (sdcPt.getX() * xlen) + x0;
          double y = (sdcPt.getY() * ylen) + y0;
          Point2D newPt = new Point2D.Double(x, y);
          UiUtil.forceToGrid(x, y, newPt, UiUtil.GRID_SIZE);
          np.setLocation(newPt);
        }
      }
    }        
    return (rect);    
  } 
  
  /***************************************************************************
  **
  ** Build up a set of supplemental data coords (used to relocate notes/model data)
  */
  
  public SupplementalDataCoords getSupplementalCoordsAllOverlays(StaticDataAccessContext irx, OverlayKeySet allKeys) {  
    Rectangle lob = getLayoutBounds(irx, true, true, true, true, true, null, null, null, allKeys);
    return (getSupplementalCoordsGuts(lob));
  }
  
   /***************************************************************************
  **
  ** Build up a set of supplemental data coords (used to relocate notes/model data)
  */
  
  public SupplementalDataCoords getSupplementalCoords(StaticDataAccessContext irx, OverlayKeySet allKeys) {
    Rectangle lob = getLayoutBounds(irx, true, true, true, true, true, null, null, null, allKeys);
    return (getSupplementalCoordsGuts(lob));
  }

  /***************************************************************************
  **
  ** Build up a set of supplemental data coords (used to relocate notes/model data)
  */
  
  private SupplementalDataCoords getSupplementalCoordsGuts(Rectangle lob) {
    
    //
    // Get the bounds of the layout, and use this as a coordinate frame for supplemental
    // data positions.
    //

    Rectangle2D rect;
    if (lob == null) {
      rect = getSupplementalBounds();
    } else {
      rect = new Rectangle2D.Double(lob.x, lob.y, lob.width, lob.height);
    }
    
    if (rect == null) {
      return (null);
    }
    SupplementalDataCoords retval = new SupplementalDataCoords();    
      
    double x0 = rect.getX();
    double y0 = rect.getY();
    double xlen = rect.getWidth();
    double ylen = rect.getHeight();    
    
    Iterator<String> dpit = dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = dataProps_.get(key);
      if (pt != null) {
        double x = (pt.getX() - x0) / xlen;
        double y = (pt.getY() - y0) / ylen;
        retval.dataProps.put(key, new Point2D.Double(x, y));
      }
    }
    
    Iterator<String> ntit = noteProps_.keySet().iterator();
    while (ntit.hasNext()) {
      String key = ntit.next();
      NoteProperties np = noteProps_.get(key);
      Point2D pt = np.getLocation();
      if (pt != null) {
        double x = (pt.getX() - x0) / xlen;
        double y = (pt.getY() - y0) / ylen;
        retval.noteProps.put(key, new Point2D.Double(x, y));
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** If we have no bounds for regular items, generate bounds based on
  ** supplemental items.  May return null.
  */
  
  public Rectangle2D getSupplementalBounds() {
        
    Rectangle2D retval = null;
    Iterator<String> dpit = dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = dataProps_.get(key);
      if (pt != null) {
        if (retval == null) {
          retval = Bounds.initBoundsWithPoint(pt);
        } else {
          Bounds.tweakBoundsWithPoint(retval, pt);
        }
      }
    }
    
    Iterator<String> ntit = noteProps_.keySet().iterator();
    while (ntit.hasNext()) {
      String key = ntit.next();
      NoteProperties np = noteProps_.get(key);
      Point2D pt = np.getLocation();
      if (pt != null) {
        if (retval == null) {
          retval = Bounds.initBoundsWithPoint(pt);
        } else {
          Bounds.tweakBoundsWithPoint(retval, pt);
        }
      }
    }
    
    if (retval == null) {
      return (null);
    }
    
    //
    // Prevent division by zero downstream.  Always have a rectangle
    //
    
    double height = retval.getHeight();
    double width = retval.getWidth();
    boolean change = false;
    if (height == 0.0) {
      height = 100.0;
      change = true;
    }
    if (width == 0.0) {
      width = 100.0;
      change = true;
    }
    if (change) {
      retval.setRect(retval.getX(), retval.getY(), width, height);
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Transfer out partial layout from another layout
  */
  
  public void extractPartialLayout(Map<String, String> nodeMap, Map<String, String> linkMap, 
                                   Map<String, String> groupMap, 
                                   Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, 
                                   Map<String, String> modLinkIDMap, 
                                   Map<NetModule.FullModuleKey, DicedModuleInfo> diceMap,
                                   boolean transferNotes, boolean transferSuppData, 
                                   Map<String, BusProperties.RememberProps> rememberMap, 
                                   StaticDataAccessContext rcxMine, StaticDataAccessContext rcxOther) {
    transferLayoutCore(nodeMap, linkMap, groupMap, transferNotes, keyMap,
                       modLinkIDMap, diceMap, rememberMap, rcxMine, rcxOther);
    if (transferSuppData) {
      dataProps_ = new HashMap<String, Point2D>();
      Iterator<String> dpit = rcxOther.getCurrentLayout().dataProps_.keySet().iterator();
      while (dpit.hasNext()) {
        String key = dpit.next();
        Point2D pt = rcxOther.getCurrentLayout().dataProps_.get(key);
        if (pt != null) {
          this.dataProps_.put(key, (Point2D)pt.clone());
        }
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Pull in only the layout for the given group
  */
  
  public void extractPartialLayoutForGroup(Group group, Map<String, BusProperties.RememberProps> rememberMap, 
                                           StaticDataAccessContext rcxMine, StaticDataAccessContext rcxOther,
                                           Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, 
                                           Map<String, String> modLinkIDMap,
                                           Map<NetModule.FullModuleKey, DicedModuleInfo> diceMap) {

    GenomeInstance gi = rcxMine.getCurrentGenomeAsInstance();
    
    HashMap<String, String> linkMap = new HashMap<String, String>();
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      if (!group.isInGroup(src, gi)) {
        continue;
      }
      if (!group.isInGroup(trg, gi)) {
        continue;
      }
      String linkID = link.getID();
      if (rcxOther.getCurrentLayout().getLinkProperties(linkID) != null) {  // handles in complete source layouts...
        linkMap.put(linkID, linkID);
      }
    }
    
    HashMap<String, String> nodeMap = new HashMap<String, String>();
    Iterator<GroupMember> gmit = group.getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember memb = gmit.next();
      String mid = memb.getID();
      if (rcxOther.getCurrentLayout().getNodeProperties(mid) != null) {
        nodeMap.put(mid, mid);
      }
    }
 
    HashMap<String, String> groupMap = new HashMap<String, String>();
    String grid = group.getID();
    if (rcxOther.getCurrentLayout().getGroupProperties(grid) != null) {
      groupMap.put(grid, grid);
    }
 
    transferLayoutCore(nodeMap, linkMap, groupMap, true, keyMap, modLinkIDMap, diceMap, rememberMap, rcxMine, rcxOther);
    return;
  }  
  
  /***************************************************************************
  **
  ** Merge another layout that represents the layout for a region, using the
  ** given offset to displace it.
  */
  
  public void mergeLayoutOfRegion(Layout other, Vector2D offset, boolean foldOverlays, 
                                  boolean doName, int baseGroupOrder, 
                                  Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> moduleLinkFragShifts, String regionID) {
    
    double dx = offset.getX();
    double dy = offset.getY();
    
    Iterator<String> npit = other.nodeProps_.keySet().iterator();
    while (npit.hasNext()) {
      String pKey = npit.next();
      NodeProperties props = other.getNodeProperties(pKey).clone();
      Point2D loc = props.getLocation();
      props.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
      setNodeProperties(pKey, props);
    }

    //
    // Build the set of links that are going to be altered in this
    // operation.  For subsets, we also need to re-layout links only
    // present in the vfgRoot, so go there:
    //

    HashMap<BusProperties, BusProperties> seen = new HashMap<BusProperties, BusProperties>();
    Iterator<String> lpit = other.linkProps_.keySet().iterator();
    while (lpit.hasNext()) {
      String pKey = lpit.next();
      BusProperties props = other.getLinkProperties(pKey);
      BusProperties newProps = seen.get(props);
      if (newProps != null) {
        setLinkProperties(pKey, newProps);
        continue;
      }
      newProps = props.clone();
      newProps.fullShift(dx, dy);
      seen.put(props, newProps);
      setLinkProperties(pKey, newProps);
    }


    Iterator<String> gpit = other.groupProps_.keySet().iterator();
    while (gpit.hasNext()) {
      String grKey = gpit.next();
      GroupProperties grProp = other.getGroupProperties(grKey);
      GroupProperties newProp = new GroupProperties(grProp);
      newProp.setOrder(baseGroupOrder + newProp.getOrder());
      Point2D loc = newProp.getLabelLocation();
      if (loc != null) {
        newProp.setLabelLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
      }
      this.groupProps_.put(grKey, newProp);
    }
    
    //
    // If the layout for the group has attached module info, replace with that
    // instead, or fold it in.
    //
    
    Iterator<String> opit = other.ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String oID = opit.next();
      NetOverlayProperties oop = other.ovrProps_.get(oID);
      NetOverlayProperties top = this.ovrProps_.get(oID);
      Iterator<String> nmpkit = oop.getNetModulePropertiesKeys();
      while (nmpkit.hasNext()) {
        String modID = nmpkit.next();
        NetModuleProperties onmp = oop.getNetModuleProperties(modID);
        NetModuleProperties nnmp = onmp.clone();
        nnmp.shift(dx, dy);
        NetModuleProperties tnmp = top.getNetModuleProperties(modID);
        if (foldOverlays && (tnmp != null)) {
          HashSet<Rectangle2D> tshapes = new HashSet<Rectangle2D>(tnmp.initSliceList());
          tshapes.addAll(nnmp.initSliceList());
          tnmp.replaceTypeAndShapes(tnmp.getType(), new ArrayList<Rectangle2D>(tshapes));
          Point2D nameLoc = nnmp.getNameLoc();
          if ((nameLoc != null) && doName) {
            tnmp.setNameLocation((Point2D)nameLoc.clone());
          }
        } else { 
          top.setNetModuleProperties(modID, nnmp);
        }    
      }
      //
      // Shift portions of the net module linkages per the provided mapping to keep
      // them in sync with the moving modules:
      //
      if (moduleLinkFragShifts != null) {
        Map<String, Map<String, LinkProperties.LinkFragmentShifts>> shiftMap = moduleLinkFragShifts.get(oID);
        if (shiftMap != null) {
          Iterator<String> lmpit = top.getNetModuleLinkagePropertiesKeys();
          while (lmpit.hasNext()) {
            String lkey = lmpit.next();
            NetModuleLinkageProperties nmlp = top.getNetModuleLinkagePropertiesFromTreeID(lkey);
            Map<String, LinkProperties.LinkFragmentShifts> fragShiftsForAllRegions = shiftMap.get(lkey);
            LinkProperties.LinkFragmentShifts fragShift = fragShiftsForAllRegions.get(regionID);
            if (fragShift != null) {
              nmlp.shiftSelectedSegmentsAndDrops(dx, dy, fragShift);
            }
          }
        }
      }
    }  
  
    return;
  }
  
  /***************************************************************************
  **
  ** Clip expanded module shapes to ensure they do not go outside of group outline
  */
  
  public void clipExpandedModulesToGroup(Rectangle2D overlayClipper, Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> shapeMap) {        
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String oID = opit.next();
      NetOverlayProperties top = ovrProps_.get(oID);
      Map<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay = (shapeMap != null) ? shapeMap.get(oID) : null;
      top.clipOverlay(overlayClipper, oldToNewPerOverlay);
    }  
    return;
  }  

  /***************************************************************************
  **
  ** Shift the group
  */
  
  public void shiftContentsOfRegion(GenomeInstance gi, Group group, Vector2D offset) {
    
    double dx = offset.getX();
    double dy = offset.getY();  
  
    Iterator<GroupMember> mit = group.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      String nodeID = mem.getID();
      NodeProperties props = getNodeProperties(nodeID);
      Point2D loc = props.getLocation();
      props.setLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    }
    
    //
    // Figure out the links that are not already handled by the layout, and
    // add them here:
    //
    
    HashSet<BusProperties> seen = new HashSet<BusProperties>();
    Iterator<Linkage> glit = gi.getLinkageIterator();
    while (glit.hasNext()) {
      Linkage link = glit.next();
      String src = link.getSource();
      String trg = link.getTarget();
      if (!group.isInGroup(src, gi)) {
        continue;
      }
      if (!group.isInGroup(trg, gi)) {
        continue;
      }
      String linkID = link.getID();
      BusProperties props = getLinkProperties(linkID);
      if (seen.contains(props)) {
        continue;
      }           
      props.fullShift(dx, dy);
      seen.add(props);
    }

    GroupProperties grProp = getGroupProperties(group.getID());
    Point2D loc = grProp.getLabelLocation();
    if (loc != null) {
      grProp.setLabelLocation(new Point2D.Double(loc.getX() + dx, loc.getY() + dy));
    }    
   
    return;
  }
  
  /***************************************************************************
  **
  ** Transfer out layout stuff for given links from legacy layout
  */
  
  private void transferLayoutCore(Map<String, String> nodeMap, Map<String, String> linkMap, Map<String, String> groupMap, 
                                  boolean transferNotes, 
                                  Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, 
                                  Map<String, String> modLinkIDMap,
                                  Map<NetModule.FullModuleKey, DicedModuleInfo> diceMap,
                                  Map<String, BusProperties.RememberProps> rememberMap,
                                  StaticDataAccessContext rcxMine, StaticDataAccessContext rcxOther) {    
    //
    // Copy node properties:
    //
    Iterator<String> nmit = nodeMap.keySet().iterator();
    while (nmit.hasNext()) {
      String newNode = nmit.next();
      String oldNode = nodeMap.get(newNode);
      if (oldNode != null) {
        NodeProperties np = new NodeProperties(newNode, rcxOther.getCurrentLayout().getNodeProperties(oldNode));
        setNodeProperties(newNode, np);
      }
    }
    
    //
    // Copy link properties. We gotta pass down inverted maps, e.g. old->new instead
    // of new->old
    //
    
    HashMap<String, String> invertLink = new HashMap<String, String>();
    Iterator<String> kit = linkMap.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      String val = linkMap.get(key);
      if (val != null) {
        invertLink.put(val, key);
      }
    }
    HashMap<String, String> invertNode = new HashMap<String, String>();
    kit = nodeMap.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      String val = nodeMap.get(key);
      if (val != null) {
        invertNode.put(val, key);
      }
    }
        
    Iterator<String> lmit = linkMap.keySet().iterator();
    HashMap<BusProperties, BusProperties> seen = new HashMap<BusProperties, BusProperties>();
    while (lmit.hasNext()) {
      String newLink = lmit.next();
      String oldLink = linkMap.get(newLink);
      if (oldLink != null) {
        BusProperties lp = rcxOther.getCurrentLayout().getLinkProperties(oldLink);
        // Using default equals() here for LinkProperties!
        BusProperties newLp = seen.get(lp);
        if (newLp != null) {
          setLinkProperties(newLink, newLp);
          continue;
        }
        newLp = lp.clone();
        //
        // This step handles converting a bus properties for all
        // retained links still to be seen in this first pass.
        // There should be no zombie segments.
        //
        newLp.mapToNewLink(invertLink, invertNode);
        setLinkProperties(newLink, newLp);
        seen.put(lp, newLp);
      }
    }
    
    //
    // Remember some properties 
    //
    
    if (rememberMap != null) {
      Iterator<String> legit = rcxOther.getCurrentLayout().linkProps_.keySet().iterator();
      while (legit.hasNext()) {
        String linkKey = legit.next();
        BusProperties lp = rcxOther.getCurrentLayout().getLinkProperties(linkKey);
        BusProperties.RememberProps rp = new BusProperties.RememberProps(lp, rcxOther);
        rememberMap.put(linkKey, rp);
      }
    }

    //
    // Copy group properties:
    //
    
    if (groupMap != null) {
      Iterator<String> gmit = groupMap.keySet().iterator();
      while (gmit.hasNext()) {
        String newGroup = gmit.next();
        String oldGroup = groupMap.get(newGroup);
        if (oldGroup != null) {
          GroupProperties oldProp = rcxOther.getCurrentLayout().getGroupProperties(oldGroup);
          GroupProperties gp = new GroupProperties(newGroup, oldProp.getOrder(), oldProp);
          setGroupProperties(newGroup, gp);
        }
      }    
    }

    //
    // Copy note properties:
    //
    
    if (transferNotes) {
      this.noteProps_ = new HashMap<String, NoteProperties>();
      Iterator<String> ntpit = rcxOther.getCurrentLayout().noteProps_.keySet().iterator();
      while (ntpit.hasNext()) {
        String key = ntpit.next();
        NoteProperties np = rcxOther.getCurrentLayout().noteProps_.get(key);
        this.noteProps_.put(key, np.clone());
      }
    }
    
    //
    // We copy overlay properties if asked to do so.
    //
    
    transferOverlayCore(keyMap, modLinkIDMap, diceMap, rcxMine, rcxOther);
  
    return;
  }  

  /***************************************************************************
  **
  ** Reduce overlay modules to just hold unclaimed slices (and member-only modules)
  */
  
  public void reduceToUnclaimedSlices(OverlayKeySet loModKeys, Map<NetModule.FullModuleKey, DicedModuleInfo> unclaimedSlices) {      
    
    if (loModKeys == null) {
      return;
    }
    
    Iterator<NetModule.FullModuleKey> mkit = loModKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        continue;
      }
      DicedModuleInfo dmi = unclaimedSlices.get(fullKey);
    
      if ((dmi == null) || dmi.dicedRectByRects.isEmpty()) {
        nop.removeNetModuleProperties(fullKey.modKey);    
      } else {
        ArrayList<Rectangle2D> shapes = new ArrayList<Rectangle2D>();      
        int numdrbr = dmi.dicedRectByRects.size();
        for (int i = 0; i < numdrbr; i++) {
          NetModuleShapeFixer.RectResolution rr = dmi.dicedRectByRects.get(i);
          NetModuleShapeFixer.GroupTRKey gtrk = (NetModuleShapeFixer.GroupTRKey)rr.tag;
          if (!gtrk.isName()) {
            shapes.add((Rectangle2D)rr.rect.clone());
          }
        }
        if ((nmp.getType() == NetModuleProperties.CONTIG_RECT) && (shapes.size() == 1)) {
          nmp.replaceTypeAndShapes(NetModuleProperties.CONTIG_RECT, shapes);      
        } else {
          nmp.replaceTypeAndShapes(NetModuleProperties.MULTI_RECT, shapes);     
        }
      }
    }
    return;
  }  
       
  /***************************************************************************
  **
  ** Move unclaimed slices around to their new positions based on their
  ** relationship to claimed slices.
  */
  
  public void shiftUnclaimedSlices(OverlayKeySet loModKeys, Map<NetModule.FullModuleKey, DicedModuleInfo> unclaimedSlices) {      
    
    if (loModKeys == null) {
      return;
    }
    
    Iterator<NetModule.FullModuleKey> mkit = loModKeys.iterator();
    while (mkit.hasNext()) {
      NetModule.FullModuleKey fullKey = mkit.next();
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        continue;
      }
      DicedModuleInfo dmi = unclaimedSlices.get(fullKey);
        
      HashMap<Rectangle2D, Rectangle2D> oldToNew = new HashMap<Rectangle2D, Rectangle2D>();
      int numDS = dmi.dicedShapes.size();
      Rectangle2D nameShape = null;
      for (int i = 0; i < numDS; i++) {
        NetModuleProperties.TaggedShape ts = dmi.dicedShapes.get(i);
        if (ts.isName) {
          nameShape = ts.shape;
        } else {
          oldToNew.put(ts.oldShape, ts.shape);
        }
      }
      
      List<Rectangle2D> shapes = nmp.initSliceList();
      int numShapes = shapes.size();
      for (int i = 0; i < numShapes; i++) {
        Rectangle2D shape = shapes.get(i);
        Rectangle2D newShape = oldToNew.get(shape);
        if (newShape != null) {          
          shapes.set(i, newShape);
        }
      } 
 
      if ((nmp.getType() == NetModuleProperties.CONTIG_RECT) && (shapes.size() == 1)) {
        nmp.replaceTypeAndShapes(NetModuleProperties.CONTIG_RECT, shapes);      
      } else {
        nmp.replaceTypeAndShapes(NetModuleProperties.MULTI_RECT, shapes); 
        nmp.reassembleSlices();
      } 
      if (nameShape != null) {
        // odd name widths cause name drift if we don't floor:
        double centerX = UiUtil.forceToGridValueMin(nameShape.getCenterX(), UiUtil.GRID_SIZE);
        double centerY = UiUtil.forceToGridValueMin(nameShape.getCenterY(), UiUtil.GRID_SIZE);
        Point2D newLoc = new Point2D.Double(centerX, centerY);
        nmp.setNameLocation(newLoc);
      }
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Transfer out layout stuff for overlays
  */
  
  private void transferOverlayCore(Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap, 
                                   Map<String, String> modLinkIDMap, Map<NetModule.FullModuleKey, DicedModuleInfo> diceShapeMap, 
                                   StaticDataAccessContext rcxMine, StaticDataAccessContext rcxOther) {    
    //
    // Warning!  Note how the last argument to the NetOverlayProperties() contructor is true,
    // so netModule link treeIDs are retained, not mapped.  If mapping is required in a
    // future usage, it will need to be driven by a new argument.
    //
    // We copy overlay properties if asked to do so.  Gotta twist the fullKey map
    // into per overlay maps first.  Note that key map drives which modules are brought over
    // in full.
    // 
    
    if ((keyMap == null) && (diceShapeMap == null)) {
      return;
    }

    this.ovrProps_ = new HashMap<String, NetOverlayProperties>();
    
    if (keyMap != null) { 
      HashMap<String, String> overMap = new HashMap<String, String>();
      HashMap<String, Map<String, String>> modsPerOverlay = new HashMap<String, Map<String, String>>();
      Iterator<NetModule.FullModuleKey> kmit = keyMap.keySet().iterator();
      while (kmit.hasNext()) {
        NetModule.FullModuleKey oldFullKey = kmit.next();
        NetModule.FullModuleKey newFullKey = keyMap.get(oldFullKey);
        if (overMap.get(oldFullKey.ovrKey) == null) {
          overMap.put(oldFullKey.ovrKey, newFullKey.ovrKey);
        }
        // Some keys will have no module key if the overlay is bare:
        if (oldFullKey.modKey != null) {
          Map<String, String> modMap = modsPerOverlay.get(oldFullKey.ovrKey);
          if (modMap == null) {
            modMap = new HashMap<String, String>();
            modsPerOverlay.put(oldFullKey.ovrKey, modMap);
          }
          modMap.put(oldFullKey.modKey, newFullKey.modKey);
        }
      }   
      Iterator<String> oit = overMap.keySet().iterator();
      while (oit.hasNext()) {
        String oldOvr = oit.next();
        String newID = overMap.get(oldOvr);
        Map<String, String> modMap = modsPerOverlay.get(oldOvr); // may be null...
        NetOverlayProperties oldProps = rcxOther.getCurrentLayout().ovrProps_.get(oldOvr);
        NetOverlayProperties newProps = new NetOverlayProperties(rcxMine.getGenomeSource(), oldProps, newID, modMap, modLinkIDMap, true);
        this.ovrProps_.put(newID, newProps);
      }
    }
    
    //
    // Dice map says which modules are brought over as fragments:
    //    
      
    if (diceShapeMap != null) {
      Map<String, String> diceOverMap = new HashMap<String, String>();
      Map<String, Map<String, DicedModuleInfo>> dicePerOverlay = new HashMap<String, Map<String, DicedModuleInfo>>();
      Iterator<NetModule.FullModuleKey> dsmit = diceShapeMap.keySet().iterator();
      while (dsmit.hasNext()) {
        NetModule.FullModuleKey oldFullKey = dsmit.next();
        DicedModuleInfo dmi = diceShapeMap.get(oldFullKey);      
        NetModule.FullModuleKey newFullKey = dmi.fullKey;
        if (diceOverMap.get(oldFullKey.ovrKey) == null) {
          diceOverMap.put(oldFullKey.ovrKey, newFullKey.ovrKey);
        }
        Map<String, DicedModuleInfo> diceMap = dicePerOverlay.get(oldFullKey.ovrKey);
        if (diceMap == null) {
          diceMap = new HashMap<String, DicedModuleInfo>();
          dicePerOverlay.put(oldFullKey.ovrKey, diceMap);
        }
        diceMap.put(oldFullKey.modKey, dmi);
      } 
  
      HashMap<String, String> junkMap = new HashMap<String, String>();
      
      Iterator<String> oit = diceOverMap.keySet().iterator();
      while (oit.hasNext()) {
        String oldOvr = oit.next();
        String newID = diceOverMap.get(oldOvr);
        Map<String, DicedModuleInfo> diceMap = dicePerOverlay.get(oldOvr);
        NetOverlayProperties oldProps = rcxOther.getCurrentLayout().ovrProps_.get(oldOvr);
        NetOverlayProperties justAdded = this.ovrProps_.get(newID);
        if (justAdded == null) {          
          justAdded = new NetOverlayProperties(rcxMine.getGenomeSource(), oldProps, newID, junkMap, junkMap, true);
          this.ovrProps_.put(newID, justAdded);                 
        }
        Iterator<String> dmit = diceMap.keySet().iterator();
        while (dmit.hasNext()) {
          String oldModKey = dmit.next();
          DicedModuleInfo dmi = diceMap.get(oldModKey);
          String newModKey = dmi.fullKey.modKey;
          NetModuleProperties nmp = justAdded.getNetModuleProperties(newModKey);
          if (nmp != null) { // added as full module above already
            continue;
          }
          NetModuleProperties oldNmp = oldProps.getNetModuleProperties(oldModKey);
          NetModuleProperties newNmp = new NetModuleProperties(oldNmp, newModKey, null);
          int numDS = dmi.dicedShapes.size();
          ArrayList<Rectangle2D> shapesOnly = new ArrayList<Rectangle2D>();
          Rectangle2D nameShape = null;
          for (int i = 0; i < numDS; i++) {
            NetModuleProperties.TaggedShape ts = dmi.dicedShapes.get(i);
            if (ts.isName) {
              nameShape = ts.shape;
            } else {
              shapesOnly.add(ts.shape);
            }
          }
          if ((newNmp.getType() == NetModuleProperties.CONTIG_RECT) && (shapesOnly.size() == 1)) {
            newNmp.replaceTypeAndShapes(NetModuleProperties.CONTIG_RECT, shapesOnly);      
          } else {
            newNmp.replaceTypeAndShapes(NetModuleProperties.MULTI_RECT, shapesOnly);     
          }
          if (nameShape != null) {
            // odd name widths cause name drift if we don't floor:
            double centerX = UiUtil.forceToGridValueMin(nameShape.getCenterX(), UiUtil.GRID_SIZE);
            double centerY = UiUtil.forceToGridValueMin(nameShape.getCenterY(), UiUtil.GRID_SIZE);
            Point2D newLoc = new Point2D.Double(centerX, centerY);
            newNmp.setNameLocation(newLoc);
          }
          justAdded.setNetModuleProperties(newModKey, newNmp);
        }      
      }
    }
      
    return;
  }    
 
  /***************************************************************************
  **
  ** Reverse the expansion process.
  */
  
  public void reverseExpansion(StaticDataAccessContext irx,
                               ExpansionReversal reversal, 
                               BTProgressMonitor monitor, double startFrac, double endFrac) throws AsynchExitRequestException {
                                              
    //
    // As for efficency, it doesn't get much worse than this!  FIX ME
    //
      
    //
    // First thing, don't compress rows/cols that are now occupied by 
    // new links:
    //
                                              
    LinkRouter router = new LinkRouter();
    LinkPlacementGrid routerGrid = router.initGrid(irx, null, INodeRenderer.STRICT, monitor);
    SortedSet<Integer> filledRows = routerGrid.getAddedCornerRows(reversal.origGrid, monitor);
    SortedSet<Integer> filledCols = routerGrid.getAddedCornerCols(reversal.origGrid, monitor);                                          
    reversal.reduce(filledRows, filledCols);
 
    //
    // do a full collapse:
    //
 
    compress(irx, reversal.expandedRows, reversal.expandedCols, null, null, null, null, monitor, startFrac, endFrac);
    return;                                  
  }

  /***************************************************************************
  **
  ** Determine the compression rows and columns to use.  The provided empty sets
  ** are filled in.
  */
  
  public void chooseCompressionRows(StaticDataAccessContext irx,
                                    double fracV, double fracH, Rectangle bounds,
                                    boolean makeRegionsOpaque,
                                    OverlayKeySet fullKeysForLayout,
                                    SortedSet<Integer> useEmptyRows, SortedSet<Integer> useEmptyCols,
                                    BTProgressMonitor monitor) 
                                    throws AsynchExitRequestException {
    chooseCompressionRows(irx, fracV, fracH, bounds, makeRegionsOpaque,
                          fullKeysForLayout, useEmptyRows, useEmptyCols, monitor, null);
    return;
  }


  /***************************************************************************
  **
  ** Compression requirements for overlays 
  */  
  
 
  private void calcOverlayRequiredRowsAndCols(StaticDataAccessContext rcxi,
                                              OverlayKeySet fullKeysForLayout, Rectangle bounds,
                                              SortedSet<Integer> needRows, SortedSet<Integer> needCols,         
                                              BTProgressMonitor monitor) 
                                                throws AsynchExitRequestException {    
   
    if (fullKeysForLayout == null) {
      return;
    }
   // Rendering requirements can be based upon the rootInstance geometry and membership   
    StaticDataAccessContext irx = new StaticDataAccessContext(rcxi, rcxi.getGenomeSource().getGenome(targetGenome_), rcxi.getCurrentLayout());

    HashMap<String, Map<String, Set<Point2D>>> padCache = new HashMap<String, Map<String, Set<Point2D>>>();
    Iterator<NetModule.FullModuleKey> fkit = fullKeysForLayout.iterator();
    while (fkit.hasNext()) {
      NetModule.FullModuleKey fullKey = fkit.next();
      NetOverlayOwner noo = irx.getGenomeSource().getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
      DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo 
                                                                                             : null;      
      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      if (nop == null) { // For partial layouts
        continue;
      }
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey); 
      if (nmp == null) { // For partial layouts
        continue;
      }
      NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
      NetModule module = no.getModule(fullKey.modKey);
      Map<String, Set<Point2D>> pads = padCache.get(fullKey.ovrKey);
      boolean firstTime = false;
      if (pads == null) {        
        pads = getPadEndsForModules(nop, no);
        padCache.put(fullKey.ovrKey, pads);
        firstTime = true;
      }
      NetModuleFree nmf = nmp.getRenderer();
      Set<Point2D> usedPads = pads.get(fullKey.modKey);
      nmf.needRowsAndCols(dip, module, nmp, irx, bounds, needRows, needCols, usedPads);
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      if (firstTime) {
        Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
        while (lmpit.hasNext()) {
          String lkey = lmpit.next();
          NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
          nmlp.needRowsAndCols(needRows, needCols);
          if ((monitor != null) && !monitor.keepGoing()) {
            throw new AsynchExitRequestException();
          } 
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Expansion restrictions for overlays 
  */  
  
 
  private void calcOverlayNonExpansion(StaticDataAccessContext rcxi,
                                       Rectangle bounds, SortedSet<Integer> excludeRows, SortedSet<Integer> excludeCols,
                                       OverlayKeySet fullKeysForLayout,
                                       BTProgressMonitor monitor) 
                                         throws AsynchExitRequestException {    
   
    if (fullKeysForLayout == null) {
      return;
    }
    // Rendering requirements can be based upon the rootInstance geometry and membership      
    StaticDataAccessContext irx = new StaticDataAccessContext(rcxi, rcxi.getGenomeSource().getGenome(targetGenome_), rcxi.getCurrentLayout());
    
    HashMap<String, Map<String, Set<Point2D>>> padCache = new HashMap<String, Map<String, Set<Point2D>>>();
    Iterator<NetModule.FullModuleKey> fkit = fullKeysForLayout.iterator();
    while (fkit.hasNext()) {
      NetModule.FullModuleKey fullKey = fkit.next();
      NetOverlayOwner noo = irx.getGenomeSource().getOverlayOwnerWithOwnerKey(fullKey.ownerKey);
      DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo 
                                                                                             : null;      

      NetOverlayProperties nop = getNetOverlayProperties(fullKey.ovrKey);
      if (nop == null) { // For partial layouts
        continue;
      }
      NetModuleProperties nmp = nop.getNetModuleProperties(fullKey.modKey); 
      if (nmp == null) { // For partial layouts
        continue;
      }
      NetworkOverlay no = noo.getNetworkOverlay(fullKey.ovrKey);
      NetModule module = no.getModule(fullKey.modKey);
      Map<String, Set<Point2D>> pads = padCache.get(fullKey.ovrKey);
      if (pads == null) {        
        pads = getPadEndsForModules(nop, no);
        padCache.put(fullKey.ovrKey, pads);
      }
      NetModuleFree nmf = nmp.getRenderer();
      Set<Point2D> usedPads = pads.get(fullKey.modKey);
      nmf.expansionExcludedRowsAndCols(dip, module, nmp, irx, bounds, excludeRows, excludeCols, usedPads);
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      } 
      // Net module links have no expansion restrictions beyond pad requirements
      // used above.
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get back a map of pads used by each module in the overlay
  */  
  
 
  private Map<String, Set<Point2D>> getPadEndsForModules(NetOverlayProperties nop, NetworkOverlay no) {
    HashMap<String, Set<Point2D>> retval = new HashMap<String, Set<Point2D>>();
    
    Iterator<String> mpit = nop.getNetModulePropertiesKeys();
    while (mpit.hasNext()) {
      String mkey = mpit.next();
      HashSet<String> inLinks = new HashSet<String>();
      HashSet<String> outLinks = new HashSet<String>();
      no.getNetModuleLinkagesForModule(mkey, inLinks, outLinks);
      HashSet<Point2D> padsForModule = new HashSet<Point2D>();
      Iterator<String> ilit = inLinks.iterator();
      while (ilit.hasNext()) {
        String linkID = ilit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkageProperties(linkID);
        padsForModule.add(nmlp.getTargetEnd(linkID, 0.0));
      }
      Iterator<String> olit = outLinks.iterator();
      while (olit.hasNext()) {
        String linkID = olit.next();
        NetModuleLinkageProperties nmlp = nop.getNetModuleLinkageProperties(linkID);
        padsForModule.add(nmlp.getSourceStart(0.0));
      }
      retval.put(mkey, padsForModule);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Determine the compression rows and columns to use.  The provided empty sets
  ** are filled in.  Note the semantics of bounds!  For example, the X bounds are
  ** used to limit what elements are considered while finding empty ROWS, but they
  ** do NOT limit what columns are returned!
  */
  
  public void chooseCompressionRows(StaticDataAccessContext irx,
                                    double fracV, double fracH, Rectangle bounds,
                                    boolean makeRegionsOpaque,
                                    OverlayKeySet fullKeysForLayout,
                                    SortedSet<Integer> useEmptyRows, SortedSet<Integer> useEmptyCols,
                                    BTProgressMonitor monitor, Set<String> ignoreNodes) 
                                    throws AsynchExitRequestException {  

    LinkRouter router = new LinkRouter();
    int strictness = ((fullKeysForLayout == null) || fullKeysForLayout.isEmpty()) ? INodeRenderer.STRICT : INodeRenderer.MODULE_PADDED;
    LinkPlacementGrid routerGrid = router.initGrid(irx, null, strictness, monitor);
       
    //
    // These are used to find the tested range.  If nobody is in the grid, there is
    // no tested range, so we want the default to allow everybody
    //
    
    MinMax testedRowRange = new MinMax(Integer.MAX_VALUE, Integer.MIN_VALUE);
    MinMax testedColRange = new MinMax(Integer.MAX_VALUE, Integer.MIN_VALUE);
    
    //
    // Now ask the grid for what it allows:
    //
    
    SortedSet<Integer> emptyRows;
    SortedSet<Integer> emptyCols;
      
    if (bounds != null) {
      MinMax xBounds = new MinMax(bounds.x, bounds.x + bounds.width);
      emptyRows = routerGrid.getEmptyRows(xBounds, makeRegionsOpaque, monitor, ignoreNodes, testedRowRange);
      MinMax yBounds = new MinMax(bounds.y, bounds.y + bounds.height);
      emptyCols = routerGrid.getEmptyColumns(yBounds, makeRegionsOpaque, monitor, ignoreNodes, testedColRange);
    } else {
      emptyRows = routerGrid.getEmptyRows(null, makeRegionsOpaque, monitor, ignoreNodes, testedRowRange);
      emptyCols = routerGrid.getEmptyColumns(null, makeRegionsOpaque, monitor, ignoreNodes, testedColRange);
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    } 
    
    //
    // Compression rows are decided by interrogating the link placement grid.  But this
    // needs to be modulated by the requirements of overlays.  We find out what rows and
    // columns are needed by the overlays.  Note that if overlay needs go outside of
    // grid-based needs, we need to add all additional rows and columns outside of the _tested_
    // bounds for potential compression, then reduce those additions based on what is 
    // allowed.
    //
    
    TreeSet<Integer> needRows = new TreeSet<Integer>();
    TreeSet<Integer> needCols = new TreeSet<Integer>();    

    //
    // Pad the results for the overlays, if appropriate:
    //
      
    calcOverlayRequiredRowsAndCols(irx, fullKeysForLayout, bounds, needRows, needCols, monitor);
    if (!needRows.isEmpty()) {
      int minRow = needRows.first().intValue();
      int maxRow = needRows.last().intValue();
      for (int i = minRow; i <= maxRow; i++) {
        if ((i < testedRowRange.min) || (i > testedRowRange.max)) { 
          emptyRows.add(new Integer(i));
        }
      }
    }
    if (!needCols.isEmpty()) {
      int minCol = needCols.first().intValue();
      int maxCol = needCols.last().intValue();
      for (int i = minCol; i <= maxCol; i++) {
        if ((i < testedColRange.min) || (i > testedColRange.max)) {
          emptyCols.add(new Integer(i));        
        }
      }
    } 
    
    //
    // Now reduce the grid-derived requirements with the overlay needs:
    //
    
    emptyRows.removeAll(needRows);
    emptyCols.removeAll(needCols);   
                              
    HashSet<Double> seenVals = new HashSet<Double>();
    
    if (fracV != 0.0) {
      Iterator<Integer> eit = emptyRows.iterator();
      int counter = 0;
      while (eit.hasNext()) {
        Integer obj = eit.next();
        double frac = counter++ * fracV;
        Double floor = new Double(Math.floor(frac));
        if (!seenVals.contains(floor)) {
          useEmptyRows.add(obj);
          seenVals.add(floor);
        }
      }
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }      
   
    if (fracH != 0.0) {
      Iterator<Integer> eit = emptyCols.iterator();
      seenVals.clear();
      int counter = 0;
      while (eit.hasNext()) {
        Integer obj = eit.next();
        double frac = counter++ * fracH;
        Double floor = new Double(Math.floor(frac));
        if (!seenVals.contains(floor)) {
          useEmptyCols.add(obj);
          seenVals.add(floor);
        }
      }
    }
    
    if ((monitor != null) && !monitor.keepGoing()) {
      throw new AsynchExitRequestException();
    }      
    return;
  }  
 
  /***************************************************************************
  **
  ** Compress the layout by squeezing out extra rows and columns.
  */
  
  public void compress(StaticDataAccessContext irx, SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, 
                       Rectangle bounds, 
                       Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> shapeMap, 
                       Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> fragMap, 
                       String gid, BTProgressMonitor monitor, 
                       double startFrac, double endFrac) throws AsynchExitRequestException {
    Iterator<Node> nit = irx.getCurrentGenome().getAllNodeIterator();   
    compressNodes(emptyRows, emptyCols, nit, bounds, monitor);
    
    //
    // Figure out progress for each link
    //

    List<LinkProperties> toProcess = listOfProps(irx.getCurrentGenome(), null, null);
    int linkCount = toProcess.size();
    double perLink = (endFrac - startFrac) / linkCount;
    double currProg = startFrac;
    
    //
    // Do the compression:
    //
 
    for (int i = 0; i < linkCount; i++) {
      LinkProperties lp = toProcess.get(i);
      lp.compress(emptyRows, emptyCols, bounds);
      currProg += perLink;
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }  
    compressGroupAndNoteProps(emptyRows, emptyCols, bounds);
      
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String okey = opit.next();
      NetOverlayProperties nop = ovrProps_.get(okey);
      HashMap<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay = null;
      if (shapeMap != null) {
        oldToNewPerOverlay = new HashMap<String, List<NetModuleProperties.TaggedShape>>();
        shapeMap.put(okey, oldToNewPerOverlay);
      }
      Map<String, Map<String, LinkProperties.LinkFragmentShifts>> fragForOverlay = ((fragMap != null) && (gid != null)) ? fragMap.get(okey) : null;
      nop.compressOverlay(emptyRows, emptyCols, bounds, oldToNewPerOverlay, fragForOverlay, gid);
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Determine the expansion rows and columns to use.  The provided empty sets
  ** are filled in.  If you don't care about overlay preservation, set the full
  ** keys arg to null.
  */
  
  public void chooseExpansionRows(StaticDataAccessContext rcx, 
                                  double fracV, double fracH, Rectangle bounds,
                                  OverlayKeySet fullKeysForLayout, 
                                  SortedSet<Integer> insertRows, SortedSet<Integer> insertCols, 
                                  boolean reversable, BTProgressMonitor monitor) throws AsynchExitRequestException {  

    LinkRouter router = new LinkRouter();
    LinkPlacementGrid routerGrid = router.initGrid(rcx, null, INodeRenderer.STRICT, monitor); 
    SortedSet<Integer> expandRows;
    SortedSet<Integer> expandCols;
    if (bounds != null) {
      MinMax xBounds = new MinMax(bounds.x, bounds.x + bounds.width);  
      expandRows = routerGrid.getExpandableRows(xBounds, reversable, monitor);     
      MinMax yBounds = new MinMax(bounds.y, bounds.y + bounds.height);
      expandCols = routerGrid.getExpandableColumns(yBounds, reversable, monitor); 
    } else {
      expandRows = routerGrid.getExpandableRows(null, reversable, monitor);
      expandCols = routerGrid.getExpandableColumns(null, reversable, monitor);
    }    
    
    //
    // Skip slices through genes that we cannot expand simply:
    //
    
    Genome genome = rcx.getCurrentGenome();
    SortedSet<Integer> doNotExpandCols = new TreeSet<Integer>();
    SortedSet<Integer> doNotExpandRows = new TreeSet<Integer>();
    
    if (!reversable) {
      Iterator<Node> nit = genome.getAllNodeIterator();    
      while (nit.hasNext()) {
        Node node = nit.next();
        NodeProperties np = getNodeProperties(node.getID());
        if (np == null) {  // can use on incomplete layouts...
          continue;
        }
        INodeRenderer render = np.getRenderer();      
        Rectangle rect = render.getNonExpansionRegion(node, rcx);
        if (rect == null) {
          continue;
        }
        for (int i = 0; i < rect.width; i += 10) {
          doNotExpandCols.add(new Integer((rect.x + i) / 10));
        }
        for (int i = 0; i < rect.height; i += 10) {
          doNotExpandRows.add(new Integer((rect.y + i) / 10));
        }
      }
      expandRows.removeAll(doNotExpandRows);
      expandCols.removeAll(doNotExpandCols);
    }
    
    //
    // Reduce the set by what is needed for the overlays, if asked to do so.  But we also
    // first increase the set to make sure we have expansion rows and cols all the way out
    // to the edge of the modules, if they go beyond the grid.
    //
    
    if (fullKeysForLayout != null) {
      TreeSet<Integer> excludeRows = new TreeSet<Integer>();
      TreeSet<Integer> excludeCols = new TreeSet<Integer>();
      
      Rectangle nmBounds = getLayoutBoundsForAllOverlays(fullKeysForLayout, true, rcx); 
      calcOverlayNonExpansion(rcx, bounds, excludeRows, excludeCols, fullKeysForLayout, monitor);
      if (nmBounds != null) {
        int minRow = nmBounds.y / 10;
        int maxRow = (nmBounds.y + nmBounds.height) / 10; 
        TreeSet<Integer> useVals = new TreeSet<Integer>();      
        if (!expandRows.isEmpty()) {
          useVals.add(expandRows.first());
          useVals.add(expandRows.last());
        }
        if (!doNotExpandRows.isEmpty()) {
          useVals.add(doNotExpandRows.first());
          useVals.add(doNotExpandRows.last());
        } 
        int minEmpty = (useVals.isEmpty()) ? Integer.MAX_VALUE : useVals.first().intValue();
        int maxEmpty = (useVals.isEmpty()) ? Integer.MIN_VALUE : useVals.last().intValue();
        for (int i = minRow; i <= maxRow; i++) {
          if ((i < minEmpty) || (i > maxEmpty)) { 
            expandRows.add(new Integer(i));
          }
        }
      }
      if (nmBounds != null) {
        int minCol = nmBounds.x / 10;
        int maxCol = (nmBounds.x + nmBounds.width) / 10;
        // BT-03-02-12:1  Previously refilled in the deleted columns if doNotExpand was on the boundary! Also
        // depended on the expanded args we were provided being reversed to work OK:
        TreeSet<Integer> useVals = new TreeSet<Integer>();      
        if (!expandCols.isEmpty()) {
          useVals.add(expandCols.first());
          useVals.add(expandCols.last());
        }
        if (!doNotExpandCols.isEmpty()) {
          useVals.add(doNotExpandCols.first());
          useVals.add(doNotExpandCols.last());
        } 
        int minEmpty = (useVals.isEmpty()) ? Integer.MAX_VALUE : useVals.first().intValue();
        int maxEmpty = (useVals.isEmpty()) ? Integer.MIN_VALUE : useVals.last().intValue();
        for (int i = minCol; i <= maxCol; i++) {
          if ((i < minEmpty) || (i > maxEmpty)) { 
            expandCols.add(new Integer(i));        
          }
        }
      }
      expandRows.removeAll(excludeRows);
      expandCols.removeAll(excludeCols);
    }

    //
    // Reduce the count to handle user percentage instructions:
    //
 
    HashSet<Double> seenVals = new HashSet<Double>();
    
    Iterator<Integer> eit = expandRows.iterator();
    int counter = 0;
    while (eit.hasNext()) {
      Integer obj = eit.next();
      double frac = counter++ * fracV;
      Double floor = new Double(Math.floor(frac));
      if (!seenVals.contains(floor)) {
        insertRows.add(obj);
        seenVals.add(floor);
      }
    }
      
    eit = expandCols.iterator();
    seenVals.clear();
    counter = 0;
    while (eit.hasNext()) {
      Integer obj = eit.next();
      double frac = counter++ * fracH;
      Double floor = new Double(Math.floor(frac));
      if (!seenVals.contains(floor)) {
        insertCols.add(obj);
        seenVals.add(floor);
      }
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Don't actually expand the layout by inserting extra rows and columns
  */
  
  public void pseudoExpand(StaticDataAccessContext irx, SortedSet<Integer> insertRows, 
                           SortedSet<Integer> insertCols, int mult,
                           Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> shapeMap, 
                           Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> fragMap, String gid) {
    
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String okey = opit.next();
      NetOverlayProperties nop = ovrProps_.get(okey);
      HashMap<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay = null;
      if (shapeMap != null) {
        oldToNewPerOverlay = new HashMap<String, List<NetModuleProperties.TaggedShape>>();
        shapeMap.put(okey, oldToNewPerOverlay);
      }
      Map<String, Map<String, LinkProperties.LinkFragmentShifts>> fragForOverlay = ((fragMap != null) && (gid != null)) ? fragMap.get(okey) : null;
      nop.expandOverlay(insertRows, insertCols, mult, oldToNewPerOverlay, fragForOverlay, gid);
    }

    expandGroupAndNoteProps(insertRows, insertCols, mult);
                               
    return;
  }
  
  /***************************************************************************
  **
  ** Expand the layout by inserting extra rows and columns
  */
  
  public ExpansionReversal expand(StaticDataAccessContext irx, SortedSet<Integer> insertRows, 
                                  SortedSet<Integer> insertCols, int mult, 
                                  boolean reversable,
                                  Map<String, Map<String, List<NetModuleProperties.TaggedShape>>> shapeMap, 
                                  Map<String, Map<String, Map<String, LinkProperties.LinkFragmentShifts>>> fragMap, 
                                  String gid, BTProgressMonitor monitor, 
                                  double startFrac, double endFrac) throws AsynchExitRequestException {
    
    Genome genome = irx.getCurrentGenome();
    Iterator<Node> nit = genome.getAllNodeIterator();
    expandNodes(insertRows, insertCols, nit, mult, monitor);

    //
    // Figure out progress for each link
    //

    List<LinkProperties> toProcess = listOfProps(genome, null, null);
    int linkCount = toProcess.size();
    double perLink = (endFrac - startFrac) / linkCount;
    double currProg = startFrac;
    
    //
    // Do the expansion:
    //
 
    for (int i = 0; i < linkCount; i++) {
      LinkProperties lp = toProcess.get(i);
      lp.expand(insertRows, insertCols, mult);
      currProg += perLink;
      if (monitor != null) {
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    
    Iterator<String> opit = ovrProps_.keySet().iterator();
    while (opit.hasNext()) {
      String okey = opit.next();
      NetOverlayProperties nop = ovrProps_.get(okey);
      Map<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay = null;
      if (shapeMap != null) {
        oldToNewPerOverlay = new HashMap<String, List<NetModuleProperties.TaggedShape>>();
        shapeMap.put(okey, oldToNewPerOverlay);
      }
      Map<String, Map<String, LinkProperties.LinkFragmentShifts>> fragForOverlay = ((fragMap != null) && (gid != null)) ? fragMap.get(okey) : null;
      nop.expandOverlay(insertRows, insertCols, mult, oldToNewPerOverlay, fragForOverlay, gid);
    }

    expandGroupAndNoteProps(insertRows, insertCols, mult);
    ExpansionReversal reverse = null;
                               
    if (reversable) {                                
      LinkRouter router = new LinkRouter();
      LinkPlacementGrid routerGrid = router.initGrid(irx, null, INodeRenderer.STRICT, monitor);              
      reverse = new ExpansionReversal(insertRows, insertCols, mult, routerGrid); //emptyRows, emptyCols, mult);
    }
        
    return (reverse);
  }  

  /***************************************************************************
  **
  ** Compress the layout by squeezing out extra rows and columns
  */
  
  private void compressNodes(SortedSet<Integer> emptyRows, 
                             SortedSet<Integer> emptyCols, Iterator<Node> nit, Rectangle bounds,
                             BTProgressMonitor monitor) throws AsynchExitRequestException {
    double dVal = UiUtil.GRID_SIZE;             
    double minX = (bounds == null) ? 0.0 : (double)bounds.x;
    double maxX = (bounds == null) ? 0.0 : (double)(bounds.x + bounds.width);
    double minY = (bounds == null) ? 0.0 : (double)bounds.y;
    double maxY = (bounds == null) ? 0.0 : (double)(bounds.y + bounds.height);    
    while (nit.hasNext()) {
      Node node = nit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }    
      NodeProperties np = getNodeProperties(node.getID());
      if (np == null) {  // can use this call on partial layouts
        continue;
      }
      Point2D loc = np.getLocation();
      if (bounds != null) {
        double locY = loc.getY();
        double locX = loc.getX();
        if ((locY < minY) || (locY > maxY) || (locX < minX) || (locX > maxX)) {
          continue;
        }
      }
      Iterator<Integer> rit = emptyRows.iterator();
      double delta = 0.0;
      while (rit.hasNext()) {
        Integer row = rit.next();
        if ((row.intValue() * UiUtil.GRID_SIZE) < loc.getY()) {
          delta += dVal;
        }
      }
      np.compressRow(delta);
      delta = 0.0;
      Iterator<Integer> cit = emptyCols.iterator();
      while (cit.hasNext()) {
        Integer col = cit.next();
        if ((col.intValue() * UiUtil.GRID_SIZE) < loc.getX()) {
          delta += dVal;
        }
      }
      np.compressColumn(delta);
    }
    return;
  }


  /***************************************************************************
  **
  ** Compress the layout by deleting extra rows and columns
  */
  
  private void compressGroupAndNoteProps(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, Rectangle bounds) { 
    Iterator<NoteProperties> nit = noteProps_.values().iterator();
    while (nit.hasNext()) {
      NoteProperties np = nit.next();
      Point2D loc = np.getLocation();
      Point2D newLoc = UiUtil.compressPoint(loc, emptyRows, emptyCols, bounds);
      if (newLoc != null) {
        np.setLocation(newLoc);
      }
    }
    
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties props = gpit.next();      
      Point2D loc = props.getLabelLocation();
      if (loc == null) {
        continue;
      }
      Point2D newLoc = UiUtil.compressPoint(loc, emptyRows, emptyCols, bounds);
      if (newLoc != null) {
        props.setLabelLocation(newLoc);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Expand the layout by inserting extra rows and columns
  */
  
  private void expandNodes(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, 
                           Iterator<Node> nit, int mult, BTProgressMonitor monitor) throws AsynchExitRequestException {  
    double dVal = UiUtil.GRID_SIZE * mult;
    while (nit.hasNext()) {
      Node node = nit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      } 
      NodeProperties np = getNodeProperties(node.getID());
      if (np == null) {  // can use this call on partial layouts
        continue;
      }
      Point2D loc = np.getLocation();
      Iterator<Integer> rit = emptyRows.iterator();
      double delta = 0.0;
      while (rit.hasNext()) {
        Integer row = rit.next();
        if ((row.intValue() * UiUtil.GRID_SIZE) < loc.getY()) {
          delta += dVal;
        }
      }
      np.expandRow(delta);
      delta = 0.0;
      Iterator<Integer> cit = emptyCols.iterator();
      while (cit.hasNext()) {
        Integer col = cit.next();
        if ((col.intValue() * UiUtil.GRID_SIZE) < loc.getX()) {
          delta += dVal;
        }
      }
      np.expandColumn(delta);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Expand the layout by inserting extra rows and columns
  */
  
  private void expandGroupAndNoteProps(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, int mult) { 
    Iterator<NoteProperties> nit = noteProps_.values().iterator();
    while (nit.hasNext()) {
      NoteProperties np = nit.next();
      Point2D loc = np.getLocation();
      np.setLocation(UiUtil.expandPoint(loc, emptyRows, emptyCols, mult));
    }
    
    Iterator<GroupProperties> gpit = groupProps_.values().iterator();
    while (gpit.hasNext()) {
      GroupProperties props = gpit.next();      
      Point2D loc = props.getLabelLocation();
      if (loc == null) {
        continue;
      }
      props.setLabelLocation(UiUtil.expandPoint(loc, emptyRows, emptyCols, mult));
    }
    return;
  }  
  
 
  /***************************************************************************
  **
  ** Render the nodes to a pattern grid for interlock testing.  Not all nodes
  ** are present, so we skip those.
  */
  
  public PatternGrid partialFillPatternGridWithNodes(StaticDataAccessContext rcx, Set<String> useNodes) {
    PatternGrid retval = new PatternGrid();
    Genome genome = rcx.getCurrentGenome();
    
    Iterator<Gene> git = genome.getGeneIterator();    
    while (git.hasNext()) {
      Gene node = git.next();
      if ((useNodes != null) && (!useNodes.contains(node.getID()))) {
        continue;
      }
      NodeProperties np = getNodeProperties(node.getID());
      if (np != null) {
        INodeRenderer render = np.getRenderer();      
        render.renderToPatternGrid(node, rcx, retval);
      }
    }
    Iterator<Node> nit = genome.getNodeIterator();    
    while (nit.hasNext()) {
      Node node = nit.next();
      if ((useNodes != null) && (!useNodes.contains(node.getID()))) {
        continue;
      }
      NodeProperties np = getNodeProperties(node.getID());
      if (np != null) {
        INodeRenderer render = np.getRenderer();      
        render.renderToPatternGrid(node, rcx, retval);
      }  
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Render the nodes to a pattern grid for interlock testing.  Not all nodes
  ** are present, so we skip those.
  */
  
  public PatternGrid fillPatternGridWithNodes(StaticDataAccessContext rcx) {
    return (partialFillPatternGridWithNodes(rcx, null));
  }
  
  /***************************************************************************
  **
  ** Return linkID to access those trees that contain non-ortho link segments.
  */
  
  public Set<String> discoverNonOrthoLinks(StaticDataAccessContext icx) {
    Iterator<Linkage> lit = icx.getCurrentGenome().getLinkageIterator();
    HashSet<String> linkIDs = new HashSet<String>();
    HashSet<BusProperties> orthoProps = new HashSet<BusProperties>();
    HashSet<BusProperties> nonOrthoProps = new HashSet<BusProperties>();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      BusProperties lp = getLinkProperties(link.getID());
      if (orthoProps.contains(lp)) {
        continue;
      } else if (nonOrthoProps.contains(lp)) {
        linkIDs.add(link.getID());
      } else {
        Set<LinkSegmentID> nonOrtho = lp.getNonOrthoSegments(icx);
        if (!nonOrtho.isEmpty()) {
          linkIDs.add(link.getID());
          nonOrthoProps.add(lp);
        } else {
          orthoProps.add(lp);
        }
      }
    }
    return (linkIDs);
  }

  /***************************************************************************
  **
  ** Get the link IDs passing through non-ortho link segments.
  */
  
  public Set<String> getLimitedNonOrthoLinks(StaticDataAccessContext irx) {  
    HashSet<String> retval = new HashSet<String>(); 
    List<Intersection> nonOrth = getNonOrthoIntersections(irx, null);
    int numNO = nonOrth.size();
    for (int i = 0; i < numNO; i++) {
      Intersection in = nonOrth.get(i);
      BusProperties bp = getLinkProperties(in.getObjectID());
      LinkSegmentID[] segIDs = in.segmentIDsFromIntersect();
      int numSeg = segIDs.length;
      for (int j = 0; j < numSeg; j++) { 
        Set<String> linksThru = bp.resolveLinkagesThroughSegment(segIDs[j]);
        retval.addAll(linksThru);
      }
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Do repairs for cases where max compression on stacked layout leaves crooked links
  */
  
  public void repairStackedCompressionErrors(StaticDataAccessContext icx) {
    List<Intersection> needRepair = getNonOrthoIntersections(icx, null);
    int numNR = needRepair.size();
    LinkSegmentID[] segIDs = new LinkSegmentID[1];
    for (int j = 0; j < numNR; j++) {
      Intersection needit = needRepair.get(j);
      BusProperties bp = getLinkProperties(needit.getObjectID());          
      MultiSubID msid = ((MultiSubID)needit.getSubID());
      Iterator<LinkSegmentID> mpit = msid.getParts().iterator();
      while (mpit.hasNext()) {
        segIDs[0] = mpit.next();
        double move;
        Point2D mvStart;
        if (segIDs[0].isForSegment()) {
          LinkSegment moveSeg = bp.getSegment(segIDs[0]);
          Point2D end = moveSeg.getEnd();
          Point2D start = moveSeg.getStart();
          segIDs[0] = segIDs[0].clone();
          // This assumes stacked layout with traces on left!
          if (start.getX() < end.getX()) {
            segIDs[0].tagIDWithEndpoint(LinkSegmentID.START);
            mvStart = (Point2D)start.clone();
            move = end.getY() - start.getY();
          } else {
            segIDs[0].tagIDWithEndpoint(LinkSegmentID.END);
            mvStart = (Point2D)end.clone();
            move = start.getY() - end.getY();
          }
        } else {
          // Direct link to node.  This should only be for end drops!
          LinkSegment fake = bp.getSegmentGeometryForID(segIDs[0], icx, true);
          Point2D end = fake.getEnd();
          Point2D start = fake.getStart();
          LinkSegmentID parID = bp.getSegmentIDForParent(segIDs[0]);
          // Should not happen, but skip if it does!
          if (parID == null) {
            continue;
          }
          segIDs[0] = parID.clone();             
          segIDs[0].tagIDWithEndpoint(LinkSegmentID.END);
          mvStart = (Point2D)start.clone();
          move = end.getY() - start.getY();
        }
        bp.moveBusLinkSegments(segIDs, mvStart, 0.0, move);
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Return the list of Intersections for non-orthogonal segments
  */
  
  public List<Intersection> getNonOrthoIntersections(StaticDataAccessContext icx, String overlayKey) {
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    Genome genome = icx.getCurrentGenome();
    if (overlayKey == null) {
      Iterator<Linkage> lit = genome.getLinkageIterator();
      HashSet<BusProperties> seenProps = new HashSet<BusProperties>();
      while (lit.hasNext()) {
        Linkage link = lit.next();
        String linkID = link.getID();
        BusProperties lp = getLinkProperties(linkID);
        if (seenProps.contains(lp)) {
          continue;
        } else {
          nonOrthoIntersections(lp, icx, retval, linkID);
          seenProps.add(lp);
        }
      }
    } else {  
      NetOverlayProperties nop = getNetOverlayProperties(overlayKey);
      Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
      while (lmpit.hasNext()) {
        String lkey = lmpit.next();
        LinkProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
        nonOrthoIntersections(nmlp, icx, retval, nmlp.getALinkID(genome));
      }
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** helper for above
  */
  
  private void nonOrthoIntersections(LinkProperties lp, StaticDataAccessContext icx, List<Intersection> buildList, String aLinkID) {
    MultiSubID retSub = null;
    Set<LinkSegmentID> segs = lp.getNonOrthoSegments(icx);
    Iterator<LinkSegmentID> sit = segs.iterator();
    while (sit.hasNext()) {
      LinkSegmentID lsid = sit.next();
      MultiSubID si = new MultiSubID(lsid);
      if (retSub == null) {
        retSub = si;
      } else {
        retSub.merge(si);
      }                                
    }
    if (retSub != null) {
      buildList.add(new Intersection(aLinkID, retSub, 0.0, true));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Return the list of Intersections for the given linkIDs
  */
  
  public List<Intersection> getIntersectionsForLinks(StaticDataAccessContext irx, Set<String> linkIDs, boolean uniqueBranchesOnly) {
    
    Genome genome = irx.getCurrentGenome();
    ArrayList<Intersection> retval = new ArrayList<Intersection>();
    HashSet<String> analyzed = new HashSet<String>();
    Iterator<String> lit = linkIDs.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      if (analyzed.contains(linkID)) {
        continue;
      }
      //
      // For any link in the list, if all shared items are also in the list, we have a
      // quick kill:
      //     
 
      HashSet prohibitedSegs = new HashSet();
      Set<String> shared = getSharedItems(linkID);
      HashSet<String> setDiff = new HashSet<String>(shared);
      setDiff.removeAll(linkIDs);
      if (setDiff.isEmpty()) {
        Intersection fullInt = new Intersection(linkID, null, 0.0, true);
        retval.add(fullInt);
        analyzed.addAll(shared);
        continue;
      } else if (uniqueBranchesOnly) {
        //
        // If we want only unique segs, we need to avoid the
        // segs used by other non-selected links!
        //
         
        Iterator<String> sit = setDiff.iterator();
        while (sit.hasNext()) {
          String unwantedID = sit.next();
          Linkage unwantedLink = genome.getLinkage(unwantedID);
          //
          // Issue #182. Ongoing VfN null problems here...
          //
          if (unwantedLink == null) {
            continue;
          }
          Intersection unwantedInter = Intersection.pathIntersection(unwantedLink, irx);
          prohibitedSegs.addAll(((MultiSubID)unwantedInter.getSubID()).getParts());
        }

      }
      //
      // Otherwise, build up a single intersection of merges from all the links that
      // are present.
      //
      Intersection mergedInter = null;
      MultiSubID mergedMulti = null;
      Iterator<String> sit = shared.iterator();
      while (sit.hasNext()) {
        String sharedID = sit.next();
        if (!linkIDs.contains(sharedID)) {
          continue;
        }
        Linkage sharedLink = genome.getLinkage(sharedID);
        Intersection sharedInter = Intersection.pathIntersection(sharedLink, irx);
        if (mergedInter == null) {
          mergedInter = sharedInter;
          mergedMulti = (MultiSubID)mergedInter.getSubID();
        } else {
          mergedMulti.merge((MultiSubID)sharedInter.getSubID());
        }
        analyzed.add(sharedID);
      }
      if (!prohibitedSegs.isEmpty()) {
        HashSet reduced = new HashSet(mergedMulti.getParts());
        reduced.removeAll(prohibitedSegs);
        mergedInter = new Intersection(mergedInter, new MultiSubID(reduced));
      }
      retval.add(mergedInter);
    }
    return (retval);
  }  
  
 
  /***************************************************************************
  **
  ** Do rendering to a grid of modules and module links
  */
  
  public LinkPlacementGrid moduleFillLinkPlacementGrid(DataAccessContext rcx,
                                                       Set<String> skipLinks, String overID,
                                                       BTProgressMonitor monitor) 
                                                         throws AsynchExitRequestException {
    
  
    Genome genome = rcx.getCurrentGenome();
    NetOverlayOwner noo = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(genome.getID());
    NetworkOverlay nov = noo.getNetworkOverlay(overID);
    DynamicInstanceProxy dip = (noo.overlayModeForOwner() == NetworkOverlay.DYNAMIC_PROXY) ? (DynamicInstanceProxy)noo : null;

    LinkPlacementGrid retval = new LinkPlacementGrid();
  
    NetModuleFree nmf = new NetModuleFree();  
    NetOverlayProperties nop = getNetOverlayProperties(overID); 
    Iterator<String> nmpit = nop.getNetModulePropertiesKeys();
    while (nmpit.hasNext()) {
      String key = nmpit.next();
      NetModule module = nov.getModule(key);
      nmf.renderToPlacementGrid(module, overID, retval, rcx);
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
    }
 
    PlacementGridRenderer pgr = null;
    Iterator<String> lmpit = nop.getNetModuleLinkagePropertiesKeys();
    while (lmpit.hasNext()) {
      String lkey = lmpit.next();
      NetModuleLinkageProperties nmlp = nop.getNetModuleLinkagePropertiesFromTreeID(lkey);
      if (pgr == null) {
        pgr = (PlacementGridRenderer)nmlp.getRenderer();
      }
      pgr.renderToPlacementGrid(nmlp, retval, skipLinks, overID, rcx);
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
    }

    return (retval);
  }
 
  /***************************************************************************
  **
  ** Render the links to a pattern grid for link layout.  Not all links
  ** are present, so we skip those.
  */
  
  public LinkPlacementGrid limitedFillLinkPlacementGrid(DataAccessContext rcx,                                      
                                                        Set<String> useNodes, 
                                                        Set<String> skipLinks, int strictness,
                                                        BTProgressMonitor monitor) 
                                                        throws AsynchExitRequestException {
    
    //
    // If there is space left over the gene because there are no inbound links, use it:
    //
    
    Genome genome = rcx.getCurrentGenome();
    Map<String, Integer> targetCounts = new HashMap<String, Integer>(); 
    HashMap<String, Integer> minPadForGenes = new HashMap<String, Integer>();
    LinkPlacementGrid retval = new LinkPlacementGrid();
    Iterator<Linkage> lit = genome.getLinkageIterator();    
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }
      String trg = link.getTarget();
      Integer tcount = targetCounts.get(trg);
      Integer newCount = (tcount == null) ? new Integer(1) : new Integer(tcount.intValue() + 1);
      targetCounts.put(trg, newCount);
    
      Node node = genome.getNode(trg);
      if (node.getNodeType() != Node.GENE) {
        continue;
      }
      int currPad = link.getLandingPad();
      Integer min = minPadForGenes.get(trg);
      if ((min == null) || (currPad < min.intValue())) {
        minPadForGenes.put(trg, new Integer(currPad));
      }
    }

    Iterator<Gene> git = genome.getGeneIterator();    
    while (git.hasNext()) {
      Gene node = git.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }    
      if ((useNodes != null) && (!useNodes.contains(node.getID()))) {
        continue;
      }
      NodeProperties np = getNodeProperties(node.getID());
      if (np != null) {
        INodeRenderer render = np.getRenderer();      
        render.renderToPlacementGrid(node, rcx, retval, targetCounts, minPadForGenes, strictness);
      }
    }
    Iterator<Node> nit = genome.getNodeIterator();    
    while (nit.hasNext()) {
      Node node = nit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }    
      if ((useNodes != null) && (!useNodes.contains(node.getID()))) {
        continue;
      }
      NodeProperties np = getNodeProperties(node.getID());
      if (np != null) {
        INodeRenderer render = np.getRenderer();      
        render.renderToPlacementGrid(node, rcx, retval, targetCounts, null, strictness);
      }  
    }
    
    lit = genome.getLinkageIterator();
    HashSet<String> srcs = new HashSet<String>();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }    
      String src = link.getSource();
      String trg = link.getTarget();
      if ((useNodes != null) && 
          (!useNodes.contains(src) || !useNodes.contains(trg))) {
        continue;
      }
      BusProperties lp = getLinkProperties(link.getID());
      if (lp != null) {
        if (!srcs.contains(src)) {
          LinkageFree render = (LinkageFree)lp.getRenderer();
          render.renderToPlacementGrid(lp, retval, skipLinks, null, rcx);
          srcs.add(src);
        }
      }
    }
    if (genome instanceof DBGenome) {
      return (retval);
    }
    GenomeInstance gi = (GenomeInstance)genome;
   
    Iterator<Group> grit = gi.getGroupIterator();
   
    while (grit.hasNext()) {
      Group group = grit.next();
      if ((monitor != null) && !monitor.keepGoing()) {
        throw new AsynchExitRequestException();
      }    
      GroupProperties gp = getGroupProperties(group.getID());
      if ((gp == null) || (gp.getLayer() != 0)) {
        continue;
      }
      GroupFree renderer = gp.getRenderer();
      renderer.renderToPlacementGrid(group, rcx, retval);
    }  
 
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Render the links to a pattern grid for link layout.  Not all links
  ** are present, so we skip those.
  */
  
  public LinkPlacementGrid fillLinkPlacementGrid(DataAccessContext rcx,
                                                 Set<String> skipLinks,
                                                 int strictness,
                                                 BTProgressMonitor monitor) 
                                                 throws AsynchExitRequestException {
    return (limitedFillLinkPlacementGrid(rcx, null, skipLinks, strictness, monitor));
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("layout");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle properties creation
  **
  */
  
  public static Layout buildFromXML(String elemName, Attributes attrs) throws IOException {
    if (!elemName.equals("layout")) {
      return (null);
    }
    String name = null;
    String targGen = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = val;
        } else if (key.equals("genome")) {
          targGen = val;
        } else if (key.equals("type")) {
          //type = val;   Don't care about type anymore...
        }
      }
    }
    
    if ((name == null) || (name.trim().equals(""))) {
      throw new IOException();
    }
    return (new Layout(name, targGen));
  }

  /***************************************************************************
  **
  ** Get data location keyword
  **
  */
  
  public static String getDataLocKeyword() {
    return ("dprop");
  }
  
  /***************************************************************************
  **
  ** Handle data location
  **
  */
  
  public static void dataPropFromXML(Layout layout, Attributes attrs) throws IOException {
    String dKey = null;
    String x = null;
    String y = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("key")) {
          dKey = val;
        } else if (key.equals("x")) {
          x = val;
        } else if (key.equals("y")) {
          y = val;
        }       
      }
    }
    
    if ((dKey == null) || (x == null) || (y == null)) {
      throw new IOException();
    }
    dKey = dKey.trim();
    if (dKey.equals("")) {
      throw new IOException();
    }

    double xPos = 0.0;
    double yPos = 0.0;
    try {
      xPos = Double.parseDouble(x);
      yPos = Double.parseDouble(y);    
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
    layout.dataProps_.put(dKey, new Point2D.Double(xPos, yPos));
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Info needed to record relative positions of supplemental data items (notes & model data)
  **
  */
  
  public class SupplementalDataCoords {
    HashMap<String, Point2D> dataProps;
    HashMap<String, Point2D> noteProps;
 
    SupplementalDataCoords() {
      dataProps = new HashMap<String, Point2D>();
      noteProps = new HashMap<String, Point2D>();
    }
  } 

 /***************************************************************************
  **
  ** Info generated by link node insertion
  **
  */
  
  public class InheritedLinkNodeInsertionResult {
    public PropChange[] changes;
    public Map<String, PadCalculatorToo.PadResult> padChanges;
    public NodeInsertionDirective nid;
 
    InheritedLinkNodeInsertionResult(PropChange[] changes, Map<String, PadCalculatorToo.PadResult> padChanges, NodeInsertionDirective nid) {
      this.changes = changes;
      this.padChanges = padChanges;
      this.nid = nid;
    }
  } 
  
  /***************************************************************************
  **
  ** Used to return inherited insertion info
  */
  
  public static class InheritedInsertionInfo {
    public LinkSegmentID segID;
    public Point2D pt;
    
    InheritedInsertionInfo(LinkSegmentID segID, Point2D pt) {
      this.segID = segID;
      this.pt = pt;
    }
  }  
  
  /***************************************************************************
  **
  ** Used to return link topology repair results
  */
  
  public static class TopoRepairInfo {
    public List<PropChange> changes;
    public boolean dropped; 
    public boolean elim; 
    public int topoFix; 
    
    TopoRepairInfo(boolean dropped, boolean elim, int topoFix) {
      this.changes = new ArrayList<PropChange>();
      this.dropped = dropped;
      this.elim = elim;
      this.topoFix = topoFix;     
    }
 
    public boolean haveAChange() {
      boolean someRepaired = (topoFix == BusProperties.ALL_REPAIRED) || (topoFix == BusProperties.SOME_REPAIRED);
      return (dropped || elim || someRepaired);
    }        
    
    void addPropChange(PropChange pc) {
      changes.add(pc);
      return;
    }
    
    void merge(TopoRepairInfo other) {
      this.dropped = this.dropped || other.dropped;
      this.elim =  this.elim || other.elim;
      this.topoFix = LinkProperties.mergeRepairStates(this.topoFix, other.topoFix);
      changes.addAll(other.changes);
      return;
    }
  
    public boolean convergenceProblem() {
      return ((topoFix == BusProperties.COULD_NOT_REPAIR) || (topoFix == BusProperties.SOME_REPAIRED));
    }     
  }  
 
  /***************************************************************************
  **
  ** Used to return link orthogonalization results
  */
  
  public static class OrthoRepairInfo {
    public PropChange[] chgs;
    public int failCount;
    public boolean singleSeg;
    
    public OrthoRepairInfo(int failCount, boolean singleSeg) {
      this.chgs = null;
      this.failCount = failCount;
      this.singleSeg = singleSeg;
    }

    void addPropChanges(PropChange[] pc) {
      chgs = pc;
      return;
    }  
  } 
  
  /***************************************************************************
  **
  ** Used to return diced up module info
  */
  
  public static class DicedModuleInfo {
    public NetModule.FullModuleKey fullKey;
    public ArrayList<NetModuleProperties.TaggedShape> dicedShapes;
    public ArrayList<NetModuleShapeFixer.RectResolution> dicedRectByRects;
    
    public DicedModuleInfo(NetModule.FullModuleKey fullKey) {
      this.fullKey = fullKey;
      this.dicedShapes = new ArrayList<NetModuleProperties.TaggedShape>();
      this.dicedRectByRects = new ArrayList<NetModuleShapeFixer.RectResolution>();
    }
  }   
   
  /***************************************************************************
  **
  ** Info needed to undo a property change
  **
  */
  
  public class PropChange {
    public NodeProperties nOrig;
    public NodeProperties nNewProps;
    public Set<BusProperties> origLinks;
    public Set<BusProperties> newLinks;
    public BusProperties orig;
    public BusProperties newProps;
    public String addedLinkID;
    public String removedLinkID;    
    public NoteProperties ntOrig;
    public NoteProperties ntNewProps;
    public String dLocKey;
    public Point2D dLocOrig;
    public Point2D dLocNew;
    public GroupProperties grOrig;
    public GroupProperties grNewProps;
    public LayoutMetadata metaOrig;
    public LayoutMetadata newMeta;
    public Set<String> linkIDs;
    public String layoutKey;
    public String nopRef; 
    public NetModuleProperties nmpOrig;
    public NetModuleProperties nmpNew;
    public NetModuleLinkageProperties nmlpOrig;
    public NetModuleLinkageProperties nmlpNew; 
    public String nmlpTieLinkIDOrig;
    public String nmlpTieTreeIDOrig;
    public String nmlpTieLinkIDNew;
    public String nmlpTieTreeIDNew;       
    public NetOverlayProperties nopOrig;
    public NetOverlayProperties nopNew;
    public String droppedTreeID;
    public GenomeSource gSrc;
  }

  /***************************************************************************
  **
  ** Info needed to reverse a network expansion
  **
  */
  
  public class ExpansionReversal {
    public SortedSet<Integer> expandedRows;
    public SortedSet<Integer> expandedCols;
    public LinkPlacementGrid origGrid;
    
    public ExpansionReversal(SortedSet<Integer> insertRows, SortedSet<Integer> insertCols, int mult, LinkPlacementGrid grid) {
      origGrid = grid;
      expandedRows = new TreeSet<Integer>();
      //
      // Have to scale up the rows to the post-expansion values:
      //
      Iterator<Integer> irit = insertRows.iterator();
      int counter = 0;
      while (irit.hasNext()) {
        Integer row = irit.next();
        int rowVal = row.intValue();
        for (int i = 0; i < mult; i++) {
          expandedRows.add(new Integer(rowVal + counter));
          counter++;
        }
      }
      expandedCols = new TreeSet<Integer>();
      //
      // Same with cols:
      //
      Iterator<Integer> icit = insertCols.iterator();
      counter = 0;
      while (icit.hasNext()) {
        Integer col = icit.next();
        int colVal = col.intValue();
        for (int i = 0; i < mult; i++) {
          expandedCols.add(new Integer(colVal + counter));
          counter++;
        }
      }
    }
    
    //
    // Before contracting, have to remove rows and columns that
    // are no longer valid:
    //
    
    public void reduce(SortedSet<Integer> filledRows, SortedSet<Integer> filledCols) {
      TreeSet<Integer> reducedRows = new TreeSet<Integer>();      
      Iterator<Integer> erit = expandedRows.iterator();
      while (erit.hasNext()) {
        Integer row = erit.next();
        if (!filledRows.contains(row)) {
          reducedRows.add(row);
        } 
      }
      expandedRows = reducedRows;

      TreeSet<Integer> reducedCols = new TreeSet<Integer>();      
      Iterator<Integer> ecit = expandedCols.iterator();
      while (ecit.hasNext()) {
        Integer col = ecit.next();
        if (!filledCols.contains(col)) {
          reducedCols.add(col);
        }
      }
      expandedCols = reducedCols;
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Info needed to record Net Module Link pad state
  **
  */  
  
  public static class NetModuleLinkPadRequirements {    
    HashMap<String, PointNoPoint> treeIDToStarts;
    HashMap<String, PointNoPoint> linkIDToEnds;
    HashMap<PointNoPoint, NetModuleFree.LinkPad> linkPadInfo;
    HashSet<String> feedBacks;
    Map<PointNoPoint, Vector2D> rigidMoves;
    
    NetModuleLinkPadRequirements() {
      treeIDToStarts = new HashMap<String, PointNoPoint>();
      linkIDToEnds = new HashMap<String, PointNoPoint>();
      linkPadInfo = new HashMap<PointNoPoint, NetModuleFree.LinkPad>();
      rigidMoves = new HashMap<PointNoPoint, Vector2D>();
    }
    
    public String toString() {
      return ("NetModuleLinkPadRequirements " + treeIDToStarts.size() + " " + linkIDToEnds.size() + " " + linkPadInfo.size());
    }
    
  }
 
  /***************************************************************************
  **
  ** When doing mod pad fixups, some points are not allowed to collapse onto
  ** others (feedback targets onto feedback sources).  We also want to distinquish
  ** points that are associated with particular module member bounds, since in those
  ** cases it is possible that two or more pads actually overlap!
  **
  */
  
  public static class PointNoPoint implements Cloneable {
  
    public Point2D point;
    public Point2D noPoint;
    public String nodeID;
    
    PointNoPoint(Point2D point, Point2D noPoint, String nodeID) {
      this.point = (Point2D)point.clone();
      this.noPoint = (noPoint == null) ? null : (Point2D)noPoint.clone();
      this.nodeID = nodeID;
    } 
    
    public String toString() {
      return ("PointNoPoint " + point + " " + noPoint + " " + nodeID);
    }
    
    public int hashCode() {
      return (((noPoint == null) ? 0 : noPoint.hashCode()) + ((nodeID == null) ? 0 : nodeID.hashCode()) + point.hashCode());
    }

    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof PointNoPoint)) {
        return (false);
      }
      PointNoPoint otherPNP = (PointNoPoint)other;
      
      if (!this.point.equals(otherPNP.point)) {
        return (false);
      }       

      if (this.noPoint == null) {
        if (otherPNP.noPoint != null) {
          return (false);
        }
      } else if (!this.noPoint.equals(otherPNP.noPoint)) {
        return (false);
      }     
      
      if (this.nodeID == null) {
        return (otherPNP.nodeID == null);
      }
      return (this.nodeID.equals(otherPNP.nodeID));                
    }
    
    public PointNoPoint clone() {
      try {
        PointNoPoint retval = (PointNoPoint)super.clone();
        retval.point = (Point2D)this.point.clone();
        retval.noPoint = (this.noPoint == null) ? null : (Point2D)this.noPoint.clone();
        return (retval);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
  }    

  /***************************************************************************
  **
  ** Wrapper OK, here is where 1.5 generic types would be useful!
  **
  */
  
  public static class OverlayMapForLayout {
  
    private HashMap<NetModule.FullModuleKey, ?> map_;
    
    public OverlayMapForLayout(Map<NetModule.FullModuleKey, ?> overlayMap) {
      map_ = new HashMap<NetModule.FullModuleKey, Object>(overlayMap);
    } 
    
    Map<NetModule.FullModuleKey, ?> getMap() {
      return (map_);
    }
    
  }
  
  /***************************************************************************
  **
  ** Wrapper.
  **
  */
  
  public static class OverlayKeySet {
  
    private HashSet<NetModule.FullModuleKey> keys_;
  
    OverlayKeySet(OverlayMapForLayout overlayMap) {
      keys_ = new HashSet<NetModule.FullModuleKey>(overlayMap.getMap().keySet());
    }
    
    public OverlayKeySet() {
      keys_ = new HashSet<NetModule.FullModuleKey>();
    }
    
    OverlayKeySet(Set<NetModule.FullModuleKey> keys) {
      keys_ = new HashSet<NetModule.FullModuleKey>(keys);
    }   
    
    public Iterator<NetModule.FullModuleKey> iterator() {
      return (keys_.iterator());
    }
    
    public boolean isEmpty() {
      return (keys_.isEmpty());
    }
      
    public void addKey(NetModule.FullModuleKey fmk) {
      keys_.add(fmk);
      return;
    }
    
    public OverlayKeySet dropOverlay(String dropOverKey) {
      if (dropOverKey == null) {
        return (this);
      }
      OverlayKeySet retval = new OverlayKeySet();
      Iterator<NetModule.FullModuleKey> mkit = keys_.iterator();
      while (mkit.hasNext()) {
        NetModule.FullModuleKey fullKey = mkit.next();
        if (fullKey.ovrKey.equals(dropOverKey)) {
          continue;
        }
        retval.keys_.add(fullKey);
      }
      return (retval);
    }
  }
  
   /***************************************************************************
  **
  ** Wrapper.  OK, here is where 1.5 generic types would be useful!
  **
  */
  
  public static class PadNeedsForLayout {
  
    private HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements> map_;
  
    public PadNeedsForLayout(Map<NetModule.FullModuleKey, NetModuleLinkPadRequirements> map) {
      map_ = (map == null) ? null : new HashMap<NetModule.FullModuleKey, NetModuleLinkPadRequirements>(map);
    }
    
    public Iterator<NetModule.FullModuleKey> keyIterator() {
      return ((map_ == null) ? (new HashSet<NetModule.FullModuleKey>()).iterator() : map_.keySet().iterator());
    }
    
    public OverlayKeySet getFullModuleKeys() {
      return ((map_ == null) ? null : new OverlayKeySet(map_.keySet()));
    }
       
    public NetModuleLinkPadRequirements getPadRequire(NetModule.FullModuleKey key) {
      return ((map_ == null) ? null : map_.get(key));
    }
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Copy core
  */

  private void copyCore(Layout other, Map<String, String> groupIDMap) {
    this.name_ = other.name_;
    this.targetGenome_ = other.targetGenome_;
    this.lmeta_ = other.lmeta_.clone();
        
    this.nodeProps_ = new HashMap<String, NodeProperties>();
    Iterator<String> npit = other.nodeProps_.keySet().iterator();
    while (npit.hasNext()) {
      String key = npit.next();
      NodeProperties np = other.nodeProps_.get(key);
      this.nodeProps_.put(key, np.clone());
    }
    
    HashMap<BusProperties, BusProperties> copyMap = new HashMap<BusProperties, BusProperties>();
    linkProps_ = new HashMap<String, BusProperties>();
    Iterator<String> lpit = other.linkProps_.keySet().iterator();
    while (lpit.hasNext()) {
      String key = lpit.next();
      BusProperties lp = other.linkProps_.get(key);
      BusProperties newProp = copyMap.get(lp);
      if (newProp == null) {
        newProp = lp.clone();
        copyMap.put(lp, newProp);
      } 
      this.linkProps_.put(key, newProp);
    }
        
    groupProps_ = new HashMap<String, GroupProperties>();
    Iterator<String> gpit = other.groupProps_.keySet().iterator();
    while (gpit.hasNext()) {
      String key = gpit.next();
      GroupProperties gp = other.groupProps_.get(key);
      // used for copying genome instances:
      String newKey;
      GroupProperties newGroup;
      if (groupIDMap != null) {
        newKey = groupIDMap.get(key);
        newGroup = new GroupProperties(newKey, gp.getOrder(), gp);
      } else {
        newKey = key;
        newGroup = new GroupProperties(gp);        
      }
      this.groupProps_.put(newKey, newGroup);
    }
    
    ovrProps_ = new HashMap<String, NetOverlayProperties>();
    Iterator<String> mpit = other.ovrProps_.keySet().iterator();
    while (mpit.hasNext()) {
      String key = mpit.next();
      NetOverlayProperties np = other.ovrProps_.get(key);
      this.ovrProps_.put(key, np.clone());
    }
           
    noteProps_ = new HashMap<String, NoteProperties>();
    Iterator<String> ntpit = other.noteProps_.keySet().iterator();
    while (ntpit.hasNext()) {
      String key = ntpit.next();
      NoteProperties np = other.noteProps_.get(key);
      this.noteProps_.put(key, np.clone());
    }
        
    dataProps_ = new HashMap<String, Point2D>();
    Iterator<String> dpit = other.dataProps_.keySet().iterator();
    while (dpit.hasNext()) {
      String key = dpit.next();
      Point2D pt = other.dataProps_.get(key);
      if (pt != null) {
        this.dataProps_.put(key, (Point2D)pt.clone());
      }
    }
    
    return;
  }    
  
  
  /***************************************************************************
  **
  ** Write the node properties to XML
  **
  */
  
  private void writeNodeProps(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<nprops>");
    TreeSet<String> tsk = new TreeSet<String>(nodeProps_.keySet());
    Iterator<String> tskit = tsk.iterator();       
    ind.up();
    while (tskit.hasNext()) {
      String key = tskit.next();
      NodeProperties np = nodeProps_.get(key);
      np.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</nprops>");
    return;
  }   

  /***************************************************************************
  **
  ** Write the link properties to XML
  **
  */
  
  private void writeLinkProps(PrintWriter out, Indenter ind) {
    HashSet<BusProperties> pending = new HashSet<BusProperties>();
    ind.indent();    
    out.println("<lprops>");
    TreeSet<String> tsk = new TreeSet<String>(linkProps_.keySet());
    Iterator<String> tskit = tsk.iterator();       
    ind.up();
    while (tskit.hasNext()) {
      String key = tskit.next();
      BusProperties lp = linkProps_.get(key);
      // Belt and suspenders.  If we have null entries, don't crash on
      // writing; just skip them
      if (lp == null) {
        continue;
      }
      if (!pending.contains(lp)) {
        pending.add(lp);
        lp.writeXML(out, ind);
      }
    }
    ind.down().indent();    
    out.println("</lprops>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the group properties to XML
  **
  */
  
  private void writeGroupProps(PrintWriter out, Indenter ind) {
    if (groupProps_.size() == 0) {
      return;
    }
    ind.indent();
    out.println("<gprops>");
    TreeSet<String> tsk = new TreeSet<String>(groupProps_.keySet());
    Iterator<String> tskit = tsk.iterator();    
    ind.up();
    while (tskit.hasNext()) {
      String key = tskit.next();
      GroupProperties gp = groupProps_.get(key);
      gp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</gprops>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the note properties to XML
  */
  
  private void writeNoteProps(PrintWriter out, Indenter ind) {
    if (noteProps_.size() == 0) {
      return;
    }
    ind.indent();
    out.println("<ntprops>");
    TreeSet<String> tsk = new TreeSet<String>(noteProps_.keySet());
    Iterator<String> tskit = tsk.iterator();    
    ind.up();
    while (tskit.hasNext()) {
      String key = tskit.next();
      NoteProperties np = noteProps_.get(key);
      np.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</ntprops>");
    return;
  } 
  
  /***************************************************************************
  **
  ** Write module props to XML
  */
  
 private void writeOverlayProps(PrintWriter out, Indenter ind) {
    if (ovrProps_.size() == 0) {
      return;
    }
    ind.indent();
    out.println("<nOvrProps>");
    TreeSet<String> tsk = new TreeSet<String>(ovrProps_.keySet());
    Iterator<String> tskit = tsk.iterator();    
    ind.up();
    while (tskit.hasNext()) {
      String key = tskit.next();
      NetOverlayProperties nmp = ovrProps_.get(key);
      nmp.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</nOvrProps>");
    return;
  }  
    
  /***************************************************************************
  **
  ** Write the data properties to XML
  */
  
  private void writeDataProps(PrintWriter out, Indenter ind) {
    if (dataProps_.size() == 0) {
      return;
    }
    ind.indent();
    out.println("<dprops>");
    TreeSet<String> tsk = new TreeSet<String>(dataProps_.keySet());
    Iterator<String> dit = tsk.iterator();    
    ind.up();
    while (dit.hasNext()) {
      ind.indent();
      String key = dit.next();
      Point2D pt = dataProps_.get(key);
      out.print("<dprop key=\"");
      out.print(key);
      out.print("\" x=\"");
      out.print(pt.getX());
      out.print("\" y=\"");
      out.print(pt.getY());      
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</dprops>");
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle preprocessing for undo
  */
  
  public void undoPreProcess(PropChange propchng, LinkProperties lp, String ovrKey, StaticDataAccessContext rcx) {
    propchng.layoutKey = name_; 
    if (ovrKey == null) {    
      propchng.orig = (BusProperties)lp.clone();                           
      propchng.linkIDs = new HashSet<String>();

      //
      // Find out which keys are being modified when we change this
      // bus props:
      //

      Iterator<String> lpit = linkProps_.keySet().iterator();
      while (lpit.hasNext()) {
        String nextKey = lpit.next();
        BusProperties tp = linkProps_.get(nextKey);
        if (tp == lp) {
          propchng.linkIDs.add(nextKey);
        }
      }
    } else {
      propchng.nopRef = ovrKey;
      propchng.nmlpOrig = (NetModuleLinkageProperties)lp.clone();
      propchng.gSrc = (rcx == null) ? null : rcx.getGenomeSource();
    }
    return;   
  } 
  
  /***************************************************************************
  **
  ** Handle postprocessing for undo
  */
  
  public void undoPostProcess(PropChange propchng, LinkProperties lp, String ovrKey, StaticDataAccessContext rcx) {  
    if (ovrKey == null) {    
      propchng.newProps = (BusProperties)lp.clone();
    } else {
      propchng.nmlpNew = (NetModuleLinkageProperties)lp.clone();
      propchng.gSrc = (rcx == null) ? null : rcx.getGenomeSource();
    }
    return;   
  }

  /***************************************************************************
  **
  ** Check for terminal status:
  */
/*  
  private int terminalStatus(LinkSegment seg, GenomeItem item) {
                              
    SingleLinkProperties lp = (SingleLinkProperties)getLinkProperties(item.getID());
    LinkSegment first = lp.getFirstSegment();
    if (first == null) {
      return (ONLY);
    }
    if (first.isDegenerate()) {
      return ((first.getStart().equals(seg.getEnd())) ? START_D : END_D);
    }

    LinkSegment last = lp.getLastSegment();
    if (first.getStart().equals(seg.getEnd())) {
      return (START);
    } else if (last.getEnd().equals(seg.getStart())) {
      return (END);
    }
    return (NONE);
  }  
*/

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  
  /***************************************************************************
  **
  ** Info needed to pass on module pad shift, along with direction to side
  **
  */  
  
  private static class ShiftAndSide {    
    Vector2D shift;
    Vector2D toSide;
    
    ShiftAndSide(Vector2D shift, Vector2D toSide) {
      this.shift = shift;
      this.toSide = toSide;
    }
    
    public String toString() {
      return ("ShiftAndSide shift = " + shift + " side = " + toSide);
    }  
  }
  
}
