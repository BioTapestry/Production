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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.ui.freerender.NetOverlayFree;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** Contains the information needed to layout a net overlay
*/

public class NetOverlayProperties implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public enum OvrType {
    TRANSPARENT("trans"),
    OPAQUE("opaque"),
    UNDERLAY("underlay"),
    ;
 
    private String tag_;
     
    OvrType(String tag) {
      this.tag_ = tag;  
    }

    public String getTag() {
      return (tag_);      
    }
  }

 
  public static final int NO_RELAYOUT_OPTION               = -1;
  public static final int RELAYOUT_TO_MEMBER_ONLY          = 0;
  public static final int RELAYOUT_SHIFT_AND_RESIZE_SHAPES = 1;
  public static final int RELAYOUT_NO_CHANGE               = 2;
  private static final int NUM_RELAYOUT_OPTIONS_           = 3;
 
  public static final int NO_CPEX_LAYOUT_OPTION            = -1;
  public static final int CPEX_LAYOUT_TO_MEMBER_ONLY       = 0;
  public static final int CPEX_LAYOUT_APPLY_ALGORITHM      = 1;
  private static final int NUM_CPEX_LAYOUT_OPTIONS_        = 2;  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String RELAYOUT_TO_MEMBER_ONLY_STR_          = "relayoutMemOnly"; 
  private static final String RELAYOUT_SHIFT_AND_RESIZE_SHAPES_STR_ = "relayoutShiftResizeShape";
  private static final String RELAYOUT_NO_CHANGE_STR_               = "relayoutNoChange";     
   
  private static final String CPEX_LAYOUT_TO_MEMBER_ONLY_STR_   = "cpexLayoutMemberOnly"; 
  private static final String CPEX_LAYOUT_APPLY_ALGORITHM_STR_  = "cpexApplyAlgorithm";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String ref_;
  private OvrType type_;
  private boolean hideLinks_;
  private HashMap<String, NetModuleProperties> modules_;
  private HashMap<String, String> linkToTree_;
  private HashMap<String, NetModuleLinkageProperties> linkTreeProps_;
  private NetOverlayFree renderer_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Simple constructor
  */

  public NetOverlayProperties(String ref, OvrType type, boolean hideLinks) {
    ref_ = ref;
    type_ = type;
    hideLinks_ = hideLinks;
    modules_ = new HashMap<String, NetModuleProperties>();
    linkToTree_  = new HashMap<String, String>();
    linkTreeProps_  = new HashMap<String, NetModuleLinkageProperties>();
    renderer_ = new NetOverlayFree();
  }   

  /***************************************************************************
  **
  ** Constructor for I/O
  */

  public NetOverlayProperties(String ref, String typeStr, String hideStr) throws IOException {
    
    if ((ref == null) || (typeStr == null)) {
      throw new IOException();
    }    
    
    renderer_ = new NetOverlayFree();
       
    ref_ = ref;

    try {
      type_ = mapFromTypeTag(typeStr);
    } catch (IllegalArgumentException iae) {
      throw new IOException();
    }
    
    hideLinks_ = (hideStr != null) ? Boolean.valueOf(hideStr).booleanValue() : false;
        
    modules_ = new HashMap<String, NetModuleProperties>();
    linkToTree_  = new HashMap<String, String>();
    linkTreeProps_  = new HashMap<String, NetModuleLinkageProperties>();
  }
  
  /***************************************************************************
  **
  ** Copy constructor with ID changes
  */

  public NetOverlayProperties(GenomeSource gSrc, NetOverlayProperties other, String newRef, Map<String, String> modIDMap, Map<String, String> modLinkIDMap, boolean retainTreeIDs) {
    this.renderer_ = other.renderer_;
    
    this.ref_ = newRef;
    this.type_ = other.type_;
    this.hideLinks_ = other.hideLinks_;
      
    this.modules_ = new HashMap<String, NetModuleProperties>();
    // Note that module ID id for all mods in multiple overlays.  So we only
    // want those relevant to this overlay
    Iterator<String> mpit = other.modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String key = mpit.next();
      NetModuleProperties np = other.modules_.get(key);
      String newKey = (modIDMap == null) ? null : modIDMap.get(key);
      if (newKey != null) {  // i.e. this overlay owns it....
        NetModuleProperties newNp = new NetModuleProperties(np, newKey, null);
        this.modules_.put(newKey, newNp);
      }
    }
   
    HashSet<String> treesToRecover = new HashSet<String>();
    Iterator<String> mlkit = other.linkToTree_.keySet().iterator();
    while (mlkit.hasNext()) {
      String oldLinkKey = mlkit.next();
      String newLinkKey = modLinkIDMap.get(oldLinkKey);
      if (newLinkKey != null) {
        String oldPropKey = other.linkToTree_.get(oldLinkKey);
        treesToRecover.add(oldPropKey);
      }  
    }

    DBGenome rootGenome = (DBGenome)gSrc.getRootDBGenome();
    
    HashMap<String, String> treeToTree = new HashMap<String, String>();
    this.linkTreeProps_ = new HashMap<String, NetModuleLinkageProperties>();
    Iterator<String> t2rit = treesToRecover.iterator();
    while (t2rit.hasNext()) {
      String key = t2rit.next();
      NetModuleLinkageProperties nmlp = other.linkTreeProps_.get(key);
      String newKey = (retainTreeIDs) ? nmlp.getID() : rootGenome.getNextKey();
      NetModuleLinkageProperties newNmlp = new NetModuleLinkageProperties(nmlp, newKey, newRef, modIDMap, modLinkIDMap);
      treeToTree.put(key, newKey);
      this.linkTreeProps_.put(newKey, newNmlp);
    }        
    
    this.linkToTree_ = new HashMap<String, String>();
    Iterator<String> l2tit = modLinkIDMap.keySet().iterator();
    while (l2tit.hasNext()) {
      String oldLinkKey = l2tit.next();
      String oldPropKey = other.linkToTree_.get(oldLinkKey);
      // Again, many of the links in the ID map do not belong to this overlay.  It is a map
      // global to all overlays, so skip most of them:
      if (oldPropKey != null) {
        String newLinkKey = modLinkIDMap.get(oldLinkKey);
        String newPropKey = treeToTree.get(oldPropKey);
        if ((newLinkKey == null) || (newPropKey == null) || (oldPropKey == null) || (oldLinkKey == null)) {
          System.err.println("newLinkKey = " + newLinkKey + " newPropKey = " + newPropKey + 
                             " oldPropKey = " + oldPropKey + " oldLinkKey = " + oldLinkKey);
          throw new IllegalStateException();
        }       
        this.linkToTree_.put(newLinkKey, newPropKey);
      }
    }    
  }     
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

   
  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  @Override
  public NetOverlayProperties clone() { 
    try {
      NetOverlayProperties retval = (NetOverlayProperties)super.clone();      
      retval.modules_ = new HashMap<String, NetModuleProperties>();
      Iterator<String> mpit = this.modules_.keySet().iterator();
      while (mpit.hasNext()) {
        String key = mpit.next();
        NetModuleProperties np = this.modules_.get(key);
        retval.modules_.put(key, np.clone());
      }
      retval.linkTreeProps_ = new HashMap<String,NetModuleLinkageProperties>();
      Iterator<String> ltpit = this.linkTreeProps_.keySet().iterator();
      while (ltpit.hasNext()) {
        String key = ltpit.next();
        NetModuleLinkageProperties nmlp = this.linkTreeProps_.get(key);
        retval.linkTreeProps_.put(key, nmlp.clone());
      }
      // Shallow copy works for String->String map:
      retval.linkToTree_ = new HashMap<String, String>(this.linkToTree_);
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Get the renderer
  */
  
  public NetOverlayFree getRenderer() {
    return (renderer_);
  }  
  
  /***************************************************************************
  **
  ** Get the ref of the NetOverlay which we describe
  */
  
  public String getReference() {
    return (ref_);
  }  
  
  /***************************************************************************
  **
  ** Get the type
  */
  
  public OvrType getType() {
    return (type_);
  }
  
  /***************************************************************************
  **
  ** Set the type
  */
  
  public void setType(OvrType type) {
    type_ = type;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get if we are to not show links
  */
  
  public boolean hideLinks() {
    return (hideLinks_);
  }
  
  /***************************************************************************
  **
  ** Set if links are to be hidden
  */
  
  public void setHideLinks(boolean hideLinks) {
    hideLinks_ = hideLinks;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the NetModuleProperties keys 
  */
  
  public Iterator<String> getNetModulePropertiesKeys() {
    return (modules_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the NetModuleLinkageProperties TREE ID keys 
  */
  
  public Iterator<String> getNetModuleLinkagePropertiesKeys() {
    return (linkTreeProps_.keySet().iterator());
  }  
   
  /***************************************************************************
  **
  ** Get an iterator over the NetModuleLinkageProperties LINK ID keys 
  */
  
  public Iterator<String> getNetModuleLinkagePropertiesLinkKeys() {
    return (linkToTree_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** Get a NetModuleProperties
  */
  
  public NetModuleProperties getNetModuleProperties(String moduleId) {
    return (modules_.get(moduleId));
  }
  
  /***************************************************************************
  **
  ** Get a NetModuleLinkageProperties
  */
  
  public NetModuleLinkageProperties getNetModuleLinkageProperties(String linkID) {
    //
    // Map linkID to tree ID.  Then go get the properties:
    //
    String treeID = linkToTree_.get(linkID);
    NetModuleLinkageProperties props = linkTreeProps_.get(treeID);
    return (props);
  }
  
  /***************************************************************************
  **
  ** Get a NetModuleLinkageProperties from a TREE ID
  */
  
  public NetModuleLinkageProperties getNetModuleLinkagePropertiesFromTreeID(String treeID) {
    return (linkTreeProps_.get(treeID));
  }  
  
 
  /***************************************************************************
  **
  ** Get a NetModuleLinkageProperties ID for the given link
  */
  
  public String getNetModuleLinkagePropertiesID(String linkID) {
    return (linkToTree_.get(linkID));
  }  
  
  /***************************************************************************
  **
  ** Set a NetModuleLinkageProperties: note we have a tree ID!
  */
  
  public void setNetModuleLinkageProperties(String treeId, NetModuleLinkageProperties nmlp) {
    linkTreeProps_.put(treeId, nmlp);
    return;
  }  
  
  /***************************************************************************
  **
  ** Tie a NetModuleLinkageProperties to a given link
  */
  
  public void tieNetModuleLinkagePropertiesForLink(String linkId, String treeId) {
    linkToTree_.put(linkId, treeId);
    return;
  }  
  
  /***************************************************************************
  **
  ** Untie a NetModuleLinkageProperties for a given link
  */
  
  public void untieNetModuleLinkagePropertiesForLink(String linkId) {
    linkToTree_.remove(linkId);
    return;
  }    
 
  /***************************************************************************
  **
  ** Set a NetModuleProperties
  */
  
  public void setNetModuleProperties(String moduleId, NetModuleProperties nmp) {
    modules_.put(moduleId, nmp);
    return;
  }  

  /***************************************************************************
  **
  ** Remove a NetModuleProperties
  */
  
  public void removeNetModuleProperties(String moduleId) {
    modules_.remove(moduleId);
    return;
  }

  /***************************************************************************
  **
  ** Remove an entire tree.  We handle dropping the treeID key.
  */  

  public void removeNetModuleLinkagePropertiesWithTreeID(GenomeSource gSrc, String treeID) {
    linkTreeProps_.remove(treeID);
    ((DBGenome)gSrc.getRootDBGenome()).removeKey(treeID);
    return;
  }  
 
  /***************************************************************************
  **
  ** Remove support for a given link.  Return dropped key for undo recovery
  ** if appropriate
  */  

  public String removeNetModuleLinkageProperties(GenomeSource gSrc, String linkID) {
    //
    // If the link is the only one supported by the tree, we drop the
    // tree.  Else we modify the tree.
    //

    String treeID = linkToTree_.get(linkID);
    linkToTree_.remove(linkID);
    if (!linkToTree_.values().contains(treeID)) {
      linkTreeProps_.remove(treeID);
      ((DBGenome)gSrc.getRootDBGenome()).removeKey(treeID);
      return (treeID);
    }
    
    NetModuleLinkageProperties nmlp = linkTreeProps_.get(treeID); 
    nmlp.removeLinkSupport(linkID);
    return (null);
  }  
  
  /***************************************************************************
  **
  ** How many link trees are sourced to the given module?
  */
  
  public int getOutboundTreeCountForModule(String modID) {
    int count = 0; 
    Iterator<String> nmlpit = getNetModuleLinkagePropertiesKeys();
    while (nmlpit.hasNext()) {
      String treeID = nmlpit.next();
      NetModuleLinkageProperties nmlp = getNetModuleLinkagePropertiesFromTreeID(treeID);
      String src = nmlp.getSourceTag();
      if (modID.equals(src)) {
        count++;
      }
    }
    return (count);
  }
  
  
  /***************************************************************************
  **
  ** Replace color with given value
  */
  
  public boolean replaceColor(String oldID, String newID) {
    boolean retval = false;
    Iterator<String> mpit = modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String key = mpit.next();
      NetModuleProperties np = modules_.get(key);
      if (np.replaceColor(oldID, newID)) {
        retval = true;
      }
    }
    
    Iterator<String> ltpit = linkTreeProps_.keySet().iterator();
    while (ltpit.hasNext()) {
      String key = ltpit.next();
      NetModuleLinkageProperties nmlp = linkTreeProps_.get(key);
      if (nmlp.replaceColor(oldID, newID)) {
        retval = true;
      }
    }    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Compress the overlay by deleting extra rows and columns
  */
  
  public void compressOverlay(SortedSet<Integer> emptyRows, SortedSet<Integer> emptyCols, Rectangle bounds, 
                              Map<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay,
                              Map<String, Map<String, LinkProperties.LinkFragmentShifts>> fragMap, String gid) {
    Iterator<String> mpit = modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String modKey = mpit.next();
      NetModuleProperties np = modules_.get(modKey);
      List<NetModuleProperties.TaggedShape> oldAndNew = null;
      if (oldToNewPerOverlay != null) {
        oldAndNew = new ArrayList<NetModuleProperties.TaggedShape>();
        oldToNewPerOverlay.put(modKey, oldAndNew);
      }
      np.compress(emptyRows, emptyCols, bounds, oldAndNew);
    }
    
    Iterator<NetModuleLinkageProperties> ltpit = linkTreeProps_.values().iterator();
    while (ltpit.hasNext()) {
      NetModuleLinkageProperties nmlp = ltpit.next();
      nmlp.compress(emptyRows, emptyCols, bounds);
    }
    
    //
    // If this layout does not actaully own any module links, but we are being asked to
    // handle an unattached set of data, do that here.
    //
    
    if (fragMap != null) {
      Iterator<String> ksit = fragMap.keySet().iterator();
      while (ksit.hasNext()) {
        String tkey = ksit.next();
        Map<String, LinkProperties.LinkFragmentShifts> fragForGrps = fragMap.get(tkey);
        if (fragForGrps != null) {
          LinkProperties.LinkFragmentShifts lfs = fragForGrps.get(gid);
          if (lfs != null) {
            NetModuleLinkageProperties.expandCompressSelectedSegmentsAndDrops(emptyRows, emptyCols, bounds, false, 0, lfs);
          }
        }
      }
    }
 
    return;
  }  
  
  /***************************************************************************
  **
  ** Expand the overlay by inserting extra rows and columns
  */
  
  public void expandOverlay(SortedSet<Integer> insertRows, SortedSet<Integer> insertCols, int mult, 
                            Map<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay, 
                            Map<String, Map<String, LinkProperties.LinkFragmentShifts>> fragMap, String gid) {
    Iterator<String> mpit = modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String modKey = mpit.next();
      NetModuleProperties np = modules_.get(modKey);
      ArrayList<NetModuleProperties.TaggedShape> oldAndNew = null;
      if (oldToNewPerOverlay != null) {
        oldAndNew = new ArrayList<NetModuleProperties.TaggedShape>();
        oldToNewPerOverlay.put(modKey, oldAndNew);
      }
      np.expand(insertRows, insertCols, mult, oldAndNew);
    }
 
    Iterator<NetModuleLinkageProperties> ltpit = linkTreeProps_.values().iterator();
    while (ltpit.hasNext()) {
      NetModuleLinkageProperties nmlp = ltpit.next();
      nmlp.expand(insertRows, insertCols, mult);
    }
    
      //
    // If this layout does not actaully own any module links, but we are being asked to
    // handle an unattached set of data, do that here.
    //
    
    if (fragMap != null) {
      Iterator<String> ksit = fragMap.keySet().iterator();
      while (ksit.hasNext()) {
        String tkey = ksit.next();
        Map<String, LinkProperties.LinkFragmentShifts> fragForGrps = fragMap.get(tkey);
        if (fragForGrps != null) {
          LinkProperties.LinkFragmentShifts lfs = fragForGrps.get(gid);
          if (lfs != null) {
            NetModuleLinkageProperties.expandCompressSelectedSegmentsAndDrops(insertRows, insertCols, null, true, mult, lfs);
          }
        }
      }
    }
  
    return;
  } 
  
  /***************************************************************************
  **
  ** Clip the modules in the overlay to the given bounds
  */
  
  public void clipOverlay(Rectangle2D clipBounds, Map<String, List<NetModuleProperties.TaggedShape>> oldToNewPerOverlay) {
    Iterator<String> mpit = modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String modKey = mpit.next();
      NetModuleProperties np = modules_.get(modKey);
      List<NetModuleProperties.TaggedShape> oldAndNew = (oldToNewPerOverlay != null) ? oldToNewPerOverlay.get(modKey) : null;
      np.clipShapes(clipBounds, oldAndNew);
    }
    return;
  }   
 
  /***************************************************************************
  **
  ** Fill in identity maps for transferring mods and links
  */
  
  public void fillModAndLinkIdentityMaps(Map<String, String> modIDMap, Map<String, String> modLinkIDMap) {   
  
    Iterator<String> mpit = modules_.keySet().iterator();
    while (mpit.hasNext()) {
      String key = mpit.next();
      modIDMap.put(key, key);
    }
   
    Iterator<String> mlkit = linkToTree_.keySet().iterator();
    while (mlkit.hasNext()) {
      String key = mlkit.next();
      modLinkIDMap.put(key, key);
    }
 
    return;
  }
  
  /***************************************************************************
  **
  ** Write the property to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<nOvrProp");
    out.print(" ref=\"");
    out.print(ref_);
    out.print("\" type=\"");
    out.print(mapToTypeTag(type_));
    if (hideLinks_) {
      out.print("\" hideLinks=\"true");
    }
    if (modules_.size() == 0) {
      out.println("\" />");          
      return;
    }  
    out.println("\" >");
    ind.up().indent();
    out.println("<nModProps>");
    TreeSet<String> ts = new TreeSet<String>(modules_.keySet());
    Iterator<String> mi = ts.iterator();
    ind.up();
    while (mi.hasNext()) {
      String key = mi.next();
      NetModuleProperties mod = modules_.get(key);
      mod.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</nModProps>");
    ind.indent();
    out.println("<nModLinkProps>");
    TreeSet<String> tsl = new TreeSet<String>(linkTreeProps_.keySet());
    Iterator<String> liit = tsl.iterator();
    ind.up();
    while (liit.hasNext()) {
      String key = liit.next();
      NetModuleLinkageProperties lprop = linkTreeProps_.get(key);
      lprop.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</nModLinkProps>");
    ind.down().indent();
    out.println("</nOvrProp>");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return possible type choices values
  */
  
  public static Vector<TrueObjChoiceContent> getOverlayTypes(DataAccessContext dacx) {
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
    for (OvrType daType : OvrType.values()) {
      retval.add(typeForCombo(dacx, daType));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static TrueObjChoiceContent typeForCombo(DataAccessContext dacx, OvrType type) {
    return (new TrueObjChoiceContent(mapTypeToDisplay(dacx, type), type));
  }  

  /***************************************************************************
  **
  ** Map node types
  */

  public static String mapTypeToDisplay(DataAccessContext dacx, OvrType type) {
    String typeTag = mapToTypeTag(type);
    return (dacx.getRMan().getString("noverlay." + typeTag));
  }  

  /***************************************************************************
  **
  ** Map types to type tags
  */

  public static String mapToTypeTag(OvrType type) {
    return (type.getTag());
  }
  
  /***************************************************************************
  **
  ** Map type tags to types
  */

  public static OvrType mapFromTypeTag(String tag) {
     for (OvrType daType : OvrType.values()) {
       if (daType.getTag().equals(tag)) {
         return (daType);
       }
    }
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Return possible relayout modes
  */
  
  public static Vector<ChoiceContent> getRelayoutOptions(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_RELAYOUT_OPTIONS_; i++) {
      retval.add(relayoutForCombo(dacx, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element for relayout
  */
  
  public static ChoiceContent relayoutForCombo(DataAccessContext dacx, int option) {
    return (new ChoiceContent(mapRelayoutToDisplay(dacx, option), option));
  }  

  /***************************************************************************
  **
  ** Map relayout options
  */

  public static String mapRelayoutToDisplay(DataAccessContext dacx, int option) {
    String optionTag = mapToRelayoutTag(option);
    return (dacx.getRMan().getString("noverlay." + optionTag));
  }  

  /***************************************************************************
  **
  ** Map relayout options to relayout tags
  */

  public static String mapToRelayoutTag(int val) {
    switch (val) {
      case RELAYOUT_TO_MEMBER_ONLY:
        return (RELAYOUT_TO_MEMBER_ONLY_STR_);
      case RELAYOUT_SHIFT_AND_RESIZE_SHAPES:
        return (RELAYOUT_SHIFT_AND_RESIZE_SHAPES_STR_);
      case RELAYOUT_NO_CHANGE:
        return (RELAYOUT_NO_CHANGE_STR_);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map relayout tags to relayout options
  */

  public static int mapFromRelayoutTag(String tag) {
    if (tag.equals(RELAYOUT_TO_MEMBER_ONLY_STR_)) {
      return (RELAYOUT_TO_MEMBER_ONLY);
    } else if (tag.equals(RELAYOUT_SHIFT_AND_RESIZE_SHAPES_STR_)) {
      return (RELAYOUT_SHIFT_AND_RESIZE_SHAPES);
    } else if (tag.equals(RELAYOUT_NO_CHANGE_STR_)) {
      return (RELAYOUT_NO_CHANGE);
    } else {
      throw new IllegalArgumentException();
    }
  }  

  /***************************************************************************
  **
  ** Return possible cpex layout modes
  */
  
  public static Vector<ChoiceContent> getCompressExpandLayoutOptions(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_CPEX_LAYOUT_OPTIONS_; i++) {
      retval.add(cpexLayoutForCombo(dacx, i));
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get a combo box element for cpex layout
  */
  
  public static ChoiceContent cpexLayoutForCombo(DataAccessContext dacx, int option) {
    return (new ChoiceContent(mapCpexLayoutToDisplay(dacx, option), option));
  }  

  /***************************************************************************
  **
  ** Map cpex layout options
  */

  public static String mapCpexLayoutToDisplay(DataAccessContext dacx, int option) {
    String optionTag = mapToCpexLayoutTag(option);
    return (dacx.getRMan().getString("noverlay." + optionTag));
  }  

  /***************************************************************************
  **
  ** Map cpex layout options to cpex layout tags
  */

  public static String mapToCpexLayoutTag(int val) {
    switch (val) {
      case CPEX_LAYOUT_TO_MEMBER_ONLY:
        return (CPEX_LAYOUT_TO_MEMBER_ONLY_STR_);
      case CPEX_LAYOUT_APPLY_ALGORITHM:
        return (CPEX_LAYOUT_APPLY_ALGORITHM_STR_); 
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map cpex layout tags to cpex layout options
  */

  public static int mapFromCpexLayoutTag(String tag) {
    if (tag.equals(CPEX_LAYOUT_TO_MEMBER_ONLY_STR_)) {
      return (CPEX_LAYOUT_TO_MEMBER_ONLY);
    } else if (tag.equals(CPEX_LAYOUT_APPLY_ALGORITHM_STR_)) {
      return (CPEX_LAYOUT_APPLY_ALGORITHM);
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetOverlayPropertiesWorker extends AbstractFactoryClient {
   
    private MyLinkGlue mlg_;
    
    public NetOverlayPropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("nOvrProp");
      installWorker(new NetModuleProperties.NetModulePropertiesWorker(whiteboard), new MyGlue());
      mlg_ = new MyLinkGlue();
      installWorker(new NetModuleLinkageProperties.NetModuleLinkagePropertiesWorker(whiteboard), mlg_);      
    }
  
    public void installContext(DataAccessContext dacx) {
      mlg_.installContext(dacx);
      return;
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("nOvrProp")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netOvrProps = buildFromXML(elemName, attrs);
        retval = board.netOvrProps;
      }
      return (retval);     
    }
    
    private NetOverlayProperties buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String ref = AttributeExtractor.extractAttribute(elemName, attrs, "nOvrProp", "ref", true);
      String type = AttributeExtractor.extractAttribute(elemName, attrs, "nOvrProp", "type", true);
      String hideLinks = AttributeExtractor.extractAttribute(elemName, attrs, "nOvrProp", "hideLinks", false);      
      return (new NetOverlayProperties(ref, type, hideLinks));
    }
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      NetOverlayProperties nop = board.netOvrProps;
      NetModuleProperties nmp = board.netModProps;
      layout.setNetModuleProperties(nmp.getID(), nmp, nop.getReference());
      return (null);
    }
    
    
  }
  public static class MyLinkGlue implements GlueStick {
    
    private DataAccessContext dacx_;
    
    public MyLinkGlue() {
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
    }

    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      NetOverlayProperties nop = board.netOvrProps;
      NetModuleLinkageProperties nmlp = board.netModLinkProps;
      // We assign globally unique TREE_IDs to nmlp's, so that intersections
      // are not ambiguous.  But this means we need to keep that source (the
      // root Genome) informed:
      try {
        dacx_.getDBGenome().addKey(nmlp.getID());
      } catch (IllegalStateException isex) {
        throw new IOException();
      }
      layout.setNetModuleLinkagePropertiesForIO(nmlp.getID(), nmlp, nop.getReference());
      return (null);
    }
  }   
}
