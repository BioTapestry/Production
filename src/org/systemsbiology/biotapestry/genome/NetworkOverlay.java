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

package org.systemsbiology.biotapestry.genome;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This represents a semantically related set of NetModules
*/

public class NetworkOverlay implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int DB_GENOME       = 0;
  public static final int GENOME_INSTANCE = 1;
  public static final int DYNAMIC_PROXY   = 2;  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String name_;
  private String desc_;
  private ArrayList<NetModule> modules_;
  private ArrayList<NetModuleLinkage> linkages_;
  private TaggedSet firstViewMods_;
  private TaggedSet firstViewRevs_;
  private BTState appState_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NetworkOverlay(NetworkOverlay other) {
    this.appState_ = other.appState_;
    this.id_ = other.id_;
    this.name_ = other.name_;
    this.desc_ = other.desc_;    
    this.modules_ = new ArrayList<NetModule>();
    Iterator<NetModule> mit = other.modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      this.modules_.add(mod.clone());
    }
    this.linkages_ = new ArrayList<NetModuleLinkage>();
    Iterator<NetModuleLinkage> lit = other.linkages_.iterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      this.linkages_.add(link.clone());
    }
    this.firstViewMods_ = (other.firstViewMods_ == null) ? null : (TaggedSet)other.firstViewMods_.clone();   
    this.firstViewRevs_ = (other.firstViewRevs_ == null) ? null : (TaggedSet)other.firstViewRevs_.clone();       
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID
  */

  public NetworkOverlay(NetworkOverlay other, String newID) {
    this.appState_ = other.appState_;
    this.id_ = newID;
    this.name_ = other.name_;
    this.desc_ = other.desc_;    
    this.modules_ = new ArrayList<NetModule>();
    Iterator<NetModule> mit = other.modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      this.modules_.add(mod.clone());
    }
    this.linkages_ = new ArrayList<NetModuleLinkage>();
    Iterator<NetModuleLinkage> lit = other.linkages_.iterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      this.linkages_.add(link.clone());
    }
    this.firstViewMods_ = (other.firstViewMods_ == null) ? null : (TaggedSet)other.firstViewMods_.clone();
    this.firstViewRevs_ = (other.firstViewRevs_ == null) ? null : (TaggedSet)other.firstViewRevs_.clone();    
  } 
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID, and mapping inputs/output for groups, nodes, and mods.
  */

  public NetworkOverlay(NetworkOverlay other, String newID, Map<String, String> groupMap, Map<String, String> nodeMap, 
                        Map<String, String> modIDMap, Map<String, String> linkIDMap) {
    this.appState_ = other.appState_;
    this.id_ = newID;
    this.name_ = other.name_;
    this.desc_ = other.desc_;    
    this.modules_ = new ArrayList<NetModule>();
    Iterator<NetModule> mit = other.modules_.iterator();
    DBGenome rootGenome = (DBGenome)appState_.getDB().getGenome();  
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String newModID = rootGenome.getNextKey();
      modIDMap.put(mod.getID(), newModID);
      NetModule newMod = new NetModule(mod, newModID, groupMap, nodeMap);      
      this.modules_.add(newMod);
    }
    this.linkages_ = new ArrayList<NetModuleLinkage>();
    Iterator<NetModuleLinkage> lit = other.linkages_.iterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      String newLinkID = rootGenome.getNextKey();
      linkIDMap.put(link.getID(), newLinkID);
      NetModuleLinkage newLink = new NetModuleLinkage(link, newLinkID, modIDMap);      
      this.linkages_.add(newLink);
    }
    this.firstViewMods_ = (other.firstViewMods_ == null) ? null : new TaggedSet();
    if (this.firstViewMods_ != null) {
      this.firstViewMods_.tag = other.firstViewMods_.tag;
      Iterator<String> fit = other.firstViewMods_.set.iterator();
      while (fit.hasNext()) {
        String modID = fit.next();
        String mapModID = modIDMap.get(modID);
        this.firstViewMods_.set.add(mapModID);
      }
    }
    this.firstViewRevs_ = (other.firstViewRevs_ == null) ? null : new TaggedSet();
    if (this.firstViewRevs_ != null) {
      this.firstViewRevs_.tag = other.firstViewRevs_.tag;
      Iterator<String> fit = other.firstViewRevs_.set.iterator();
      while (fit.hasNext()) {
        String modID = fit.next();
        String mapModID = modIDMap.get(modID);
        this.firstViewRevs_.set.add(mapModID);
      }
    }
  }  
  
  /***************************************************************************
  **
  ** Make an empty overlay
  */

  public NetworkOverlay(BTState appState, String id, String name, String desc) {
    appState_ = appState;
    id_ = id;
    name_ = name;
    desc_ = desc;    
    modules_ = new ArrayList<NetModule>();
    linkages_ = new ArrayList<NetModuleLinkage>();
    firstViewMods_ = null;
    firstViewRevs_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Make this overlay the first one viewed for a model
  ** 
  */  
  
  public NetworkOverlayChange setAsFirstView(TaggedSet firstView, TaggedSet firstRev, String noOwnerKey, int ownerMode) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmNew = null;
    retval.nmOrig = null;
    retval.nameChange = false;
    retval.descChange = false;
 
    retval.firstViewChanged = true;
    
    retval.firstViewModOrig = (firstViewMods_ == null) ? null : (TaggedSet)firstViewMods_.clone();
    firstViewMods_ = (firstView == null) ? null : (TaggedSet)firstView.clone();
    firstViewMods_.tag = TaggedSet.UNTAGGED;
    retval.firstViewModNew = (firstViewMods_ == null) ? null : (TaggedSet)firstViewMods_.clone();
    
    retval.firstViewRevOrig = (firstViewRevs_ == null) ? null : (TaggedSet)firstViewRevs_.clone();
    firstViewRevs_ = (firstRev == null) ? null : (TaggedSet)firstRev.clone();
    firstViewRevs_.tag = TaggedSet.UNTAGGED;
    retval.firstViewRevNew = (firstViewRevs_ == null) ? null : (TaggedSet)firstViewRevs_.clone();
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Set the first view set.  Use for IO, as members are going to be read in
  ** to this held copy...
  ** 
  */  
  
  public void loadForFirstViewMods(TaggedSet firstViewMods) {
    firstViewMods_ = firstViewMods;
    // following routine may not get called if revs are empty, and we MUST have non-null if mods is non-null!
    firstViewRevs_ = new TaggedSet();     
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the first view visible set.  Use for IO, as members are going to be read in
  ** to this held copy...
  ** 
  */  
  
  public void loadForFirstViewRevs(TaggedSet firstViewRevs) {
    firstViewRevs_ = firstViewRevs;
    return;
  }    
 
  /***************************************************************************
  **
  ** Find out first view state (if so assigned)
  ** 
  */  
  
  public boolean getFirstViewState(TaggedSet firstView, TaggedSet firstViewRevs) {
    if (firstViewMods_ == null) {
      return (false);
    }
    
    firstView.tag = firstViewMods_.tag;
    firstView.set.clear();
    firstView.set.addAll(firstViewMods_.set);
    
    firstViewRevs.tag = firstViewRevs_.tag;
    firstViewRevs.set.clear();
    firstViewRevs.set.addAll(firstViewRevs_.set);       
    return (true);
  }    
  
  /***************************************************************************
  **
  ** Clear this overlay as the first one viewed for a model
  ** 
  */  
  
  public NetworkOverlayChange clearAsFirstView(String noOwnerKey, int ownerMode) {
    firstViewMods_ = null;
    
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmNew = null;
    retval.nmOrig = null;
    retval.nameChange = false;
    retval.descChange = false;
 
    retval.firstViewChanged = true;
    retval.firstViewModOrig = (firstViewMods_ == null) ? null : (TaggedSet)firstViewMods_.clone();
    retval.firstViewRevOrig = (firstViewRevs_ == null) ? null : (TaggedSet)firstViewRevs_.clone();    
    firstViewMods_ = null;
    retval.firstViewModNew = null;
    retval.firstViewRevNew = null;    
    return (retval);    
  }   
 
  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  public NetworkOverlay clone() { 
    try {
      NetworkOverlay retval = (NetworkOverlay)super.clone();
      retval.modules_ = new ArrayList<NetModule>();
      Iterator<NetModule> mit = this.modules_.iterator();
      while (mit.hasNext()) {
        NetModule mem = mit.next();
        retval.modules_.add(mem.clone());
      }
      retval.linkages_ = new ArrayList<NetModuleLinkage>();
      Iterator<NetModuleLinkage> lit = this.linkages_.iterator();
      while (lit.hasNext()) {
        NetModuleLinkage link = lit.next();
        retval.linkages_.add(link.clone());
      } 
      retval.firstViewMods_ = (this.firstViewMods_ == null) ? null : (TaggedSet)this.firstViewMods_.clone();      
      retval.firstViewRevs_ = (this.firstViewRevs_ == null) ? null : (TaggedSet)this.firstViewRevs_.clone();            
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
 
  
  /***************************************************************************
  **
  ** Get back an overlay that is stripped of members and group attachments.
  ** Those need to be filled in after network is rebuilt.
  */
  
  public NetworkOverlay getMemberStrippedOverlay() {
   
    NetworkOverlay retval = new NetworkOverlay(appState_, id_, name_, desc_);
    
    Iterator<NetModule> mit = this.modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      NetModule newMod = mod.clone();
      newMod.dropAllMembers();
      newMod.setGroupAttachment(null);
      retval.modules_.add(newMod);
    }
    
    retval.firstViewMods_ = (this.firstViewMods_ == null) ? null : (TaggedSet)this.firstViewMods_.clone();   
    retval.firstViewRevs_ = (this.firstViewRevs_ == null) ? null : (TaggedSet)this.firstViewRevs_.clone(); 
   
    Iterator<NetModuleLinkage> lit = this.linkages_.iterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      retval.linkages_.add(link.clone());
    }      
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Recover module members across a rebuilt network.
  */
  
  public void recoverMappedMembersAndGroups(NetworkOverlay legacy, Map<String, String> oldNodeToNew, Map<String, String> oldGroupToNew) {
        
    Iterator<NetModule> mit = legacy.modules_.iterator();
    while (mit.hasNext()) {
      NetModule legMod = mit.next();
      String modID = legMod.getID();
      NetModule myMod = getModule(modID);
      Iterator<NetModuleMember> memit = legMod.getMemberIterator();
      while (memit.hasNext()) {
        NetModuleMember nmm = memit.next();
        String oldID = nmm.getID();
        String newID = oldNodeToNew.get(oldID);
        if (newID != null) {
          NetModuleMember nmmNew = new NetModuleMember(newID);
          myMod.addMember(nmmNew);
        }
      }
      String legacyGroup = legMod.getGroupAttachment();
      if (legacyGroup != null) {
        String newGroup = oldGroupToNew.get(legacyGroup);
        myMod.setGroupAttachment(newGroup);
      }
    }    
    return;
  } 
  
  /***************************************************************************
  **
  ** When building a reduced layout for just a single region, we need to retain
  ** any overlay data for modules associated with the region.  Generate map
  ** that is used for this purpose
  */
  
  public boolean fillMapForGroupExtraction(String grpID, Map<NetModule.FullModuleKey, NetModule.FullModuleKey> extractMap, 
                                           int ownerMode, String ownerID) {
   
    boolean retval = false;
    Iterator<NetModule> mit = this.modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String mga = mod.getGroupAttachment();
      if ((mga != null) && mga.equals(grpID)) {
        retval = true;
        String modID = mod.getID();
        NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerMode, ownerID, id_, modID);
        extractMap.put(fullKey, fullKey);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Need to know all the modules attached to a given group
  */
  
  public void getModulesAttachedToGroup(String grpID, int ownerType, String ownerKey, Set<NetModule.FullModuleKey> attached) {
    Iterator<NetModule> mit = this.modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      String mga = mod.getGroupAttachment();
      if ((mga != null) && mga.equals(grpID)) {
        NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerType, ownerKey, id_, mod.getID());
        attached.add(fullKey);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the id
  ** 
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Get the name
  ** 
  */
  
  public String getName() {
    return (name_);
  }

  /***************************************************************************
  **
  ** Set the description (use for IO)
  ** 
  */
  
  public void setDescription(String desc) {
    desc_ = desc;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the description
  ** 
  */
  
  public String getDescription() {
    return (desc_);
  }  
  
  /***************************************************************************
  **
  ** Change the name and description
  ** 
  */
  
  public NetworkOverlayChange changeNameAndDescription(String name, String desc, String noOwnerKey, int ownerMode) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmNew = null;
    retval.nmOrig = null;
 
    retval.nameOrig = name_;
    name_ = name;
    retval.nameNew = name_;
 
    retval.descOrig = desc_;
    desc_ = desc;
    retval.descNew = desc_; 
    
    retval.nameChange = true;
    retval.descChange = true;
    return (retval);  
  }
  
  /***************************************************************************
  **
  ** Add a module
  ** 
  */
  
  public NetworkOverlayChange addNetModule(NetModule module, String noOwnerKey, int ownerMode) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmNew = module.clone();
    retval.nmOrig = null;
    retval.index = modules_.size();
    modules_.add(module);
    return (retval);
  }

  /***************************************************************************
  **
  ** Remove a module
  ** 
  */
  
  public NetworkOverlayChange[] removeNetModule(String moduleID, String noOwnerKey, int ownerMode) { 
    
    ArrayList<NetworkOverlayChange> changes = new ArrayList<NetworkOverlayChange>();
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmNew = null;
    Iterator<NetModule> gmit = getModuleIterator();
    int count = 0;
    while (gmit.hasNext()) {
      NetModule nm = gmit.next();
      if (nm.getID().equals(moduleID)) {
        retval.nmOrig = nm.clone();
        modules_.remove(nm);
        retval.index = count;
        changes.add(retval);
        break;
      }
      count++;
    }
    
    //
    // If the removed module was a member of a first view, it needs to be deleted from there too:
    //
    
    if (!changes.isEmpty() && (firstViewMods_ != null)) {
      TaggedSet revisedMods = firstViewMods_.clone();
      TaggedSet revisedRevs = firstViewRevs_.clone();
      boolean doit = false;
      if (firstViewMods_.set.contains(moduleID)) {
        revisedMods.set.remove(moduleID);
        doit = true;
      }
      if (firstViewRevs_.set.contains(moduleID)) {
        revisedRevs.set.remove(moduleID);
        doit = true;
      }
      if (doit) {
        changes.add(setAsFirstView(revisedMods, revisedRevs, noOwnerKey, ownerMode));
      }
    }

    NetworkOverlayChange[] retvalArray = new NetworkOverlayChange[changes.size()];
    return (changes.toArray(retvalArray));
  } 
  
  /***************************************************************************
  **
  ** Get a given module.
  ** 
  */
  
  public NetModule getModule(String id) {
    Iterator<NetModule> mit = modules_.iterator();
    while (mit.hasNext()) {
      NetModule mod = mit.next();
      if (id.equals(mod.getID())) {
        return (mod);
      }
    }
    return (null);
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the modules.
  ** 
  */
  
  public Iterator<NetModule> getModuleIterator() {
    return (modules_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get module count.
  ** 
  */
  
  public int getModuleCount() {
    return (modules_.size());
  }
  
  
  /***************************************************************************
  **
  ** Add a module linkage
  ** 
  */
  
  public NetworkOverlayChange addNetModuleLinkage(NetModuleLinkage link, String noOwnerKey, int ownerMode) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmlNew = link.clone();
    retval.nmlOrig = null;
    retval.index = linkages_.size();
    linkages_.add(link);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Remove a module linkage
  ** 
  */
  
  public NetworkOverlayChange removeNetModuleLinkage(String linkID, String noOwnerKey, int ownerMode) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    retval.nmlNew = null;
    Iterator<NetModuleLinkage> glit = getLinkageIterator();
    int count = 0;
    while (glit.hasNext()) {
      NetModuleLinkage nml = glit.next();
      if (nml.getID().equals(linkID)) {
        retval.nmlOrig = nml.clone();
        linkages_.remove(nml);
        retval.index = count;
        return (retval);
      }
      count++;
    }
    return (retval);    
  } 
  
  /***************************************************************************
  **
  ** Modify an existing net module linkage (currently, only sign change
  ** is supported).
  ** 
  */
  
  public NetworkOverlayChange modifyNetModuleLinkage(String linkID, String noOwnerKey, int ownerMode, int newSign) {
    NetworkOverlayChange retval = new NetworkOverlayChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = id_;
    Iterator<NetModuleLinkage> glit = getLinkageIterator();
    int count = 0;
    while (glit.hasNext()) {
      NetModuleLinkage nml = glit.next();
      if (nml.getID().equals(linkID)) {
        retval.nmlOrig = nml.clone();
        nml.setSign(newSign);
        retval.index = count;
        retval.nmlNew = nml.clone();
        return (retval);
      }
      count++;
    }
    return (null);    
  }   

  /***************************************************************************
  **
  ** Fill in the IDs of links inbound and outbound from the given module
  ** 
  */
  
  public void getNetModuleLinkagesForModule(String moduleID, Set<String> inLinks, Set<String> outLinks) {
    Iterator<NetModuleLinkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      if (link.getSource().equals(moduleID)) {
        outLinks.add(link.getID());
      }
      if (link.getTarget().equals(moduleID)) {
        inLinks.add(link.getID());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Fill in the IDs of links that are going between any modules in the
  ** given set
  ** 
  */
  
  public Set<String> getNetModuleLinkagesBetweenModules(Set<String> moduleIDs) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<NetModuleLinkage> lit = getLinkageIterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      String srcID = link.getSource();
      String trgID = link.getTarget();
      if (moduleIDs.contains(srcID) && moduleIDs.contains(trgID)) {
        retval.add(link.getID());
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answers if the link is a feedback
  ** 
  */
  
  public boolean linkIsFeedback(String linkID) {
    NetModuleLinkage link = getLinkage(linkID);
    return (link.getSource().equals(link.getTarget()));
  }    
  
  /***************************************************************************
  **
  ** Get a given linkage.
  ** 
  */
  
  public NetModuleLinkage getLinkage(String id) {
    Iterator<NetModuleLinkage> lit = linkages_.iterator();
    while (lit.hasNext()) {
      NetModuleLinkage link = lit.next();
      if (id.equals(link.getID())) {
        return (link);
      }
    }
    return (null);
  }  
   
  /***************************************************************************
  **
  ** Get an iterator over the linkages.
  ** 
  */
  
  public Iterator<NetModuleLinkage> getLinkageIterator() {
    return (linkages_.iterator());
  }  
   
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("NetOverlay: id = " + id_ + " name = " + name_ + " desc = " + desc_ + " modules = " + modules_);
  }

  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {

    ind.indent();     
    out.print("<netOverlay id=\""); 
    out.print(id_);
    if (name_ != null) {
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(name_, false));
    }
    if ((modules_.size() == 0) && ((desc_ == null) || desc_.trim().equals(""))) {
      out.println("\" />");          
      return;
    }    
    out.println("\" >");    
    ind.up();    
    if ((desc_ != null) && !desc_.trim().equals("")) {
      ind.indent();
      out.print("<descrip>");
      out.print(CharacterEntityMapper.mapEntities(desc_, false));
      out.println("</descrip>");     
    }
    
    if (modules_.size() != 0) {
      ind.indent();
      out.println("<netModules>");
      Iterator<NetModule> mi = modules_.iterator();
      ind.up();
      while (mi.hasNext()) {
        NetModule mod = mi.next();
        mod.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</netModules>");
    }
    
    if (linkages_.size() != 0) {
      ind.indent();
      out.println("<netModuleLinks>");
      Iterator<NetModuleLinkage> li = linkages_.iterator();
      ind.up();
      while (li.hasNext()) {
        NetModuleLinkage link = li.next();
        link.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</netModuleLinks>");
    }
    
    if (firstViewMods_ != null) {
      ind.indent();
      if (firstViewMods_.set.isEmpty()) {
        out.println("<startupMods/>");
      } else {
        out.println("<startupMods>");
        ind.up();
        firstViewMods_.writeXML(out, ind);
        ind.down().indent();
        out.println("</startupMods>");
      }
      ind.indent();
      if (firstViewRevs_.set.isEmpty()) {
        out.println("<startupViz/>");
      } else {
        out.println("<startupViz>");
        ind.up();
        firstViewRevs_.writeXML(out, ind);
        ind.down().indent();
        out.println("</startupViz>");      
      }
    }
    ind.down().indent();
    out.println("</netOverlay>");
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NetworkOverlayChange undo) {
 
    if ((undo.nmOrig != null) && (undo.nmNew != null)) {
      throw new IllegalArgumentException();
    } else if ((undo.nmOrig == null) && (undo.nmNew != null)) {     
      ((DBGenome)appState_.getDB().getGenome()).removeKey(undo.nmNew.getID());
      modules_.remove(undo.index);
    } else if ((undo.nmOrig != null) && (undo.nmNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(undo.nmOrig.getID());
      modules_.add(undo.index, undo.nmOrig.clone());
    } else if ((undo.nmlOrig != null) && (undo.nmlNew != null)) {
      linkages_.set(undo.index, undo.nmlOrig.clone());
    } else if ((undo.nmlOrig == null) && (undo.nmlNew != null)) {     
      ((DBGenome)appState_.getDB().getGenome()).removeKey(undo.nmlNew.getID());
      linkages_.remove(undo.index);
    } else if ((undo.nmlOrig != null) && (undo.nmlNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(undo.nmlOrig.getID());
      linkages_.add(undo.index, undo.nmlOrig.clone()); 
    } else if (undo.firstViewChanged) {
      firstViewMods_ = (undo.firstViewModOrig == null) ? null : (TaggedSet)undo.firstViewModOrig.clone();
      firstViewRevs_ = (undo.firstViewRevOrig == null) ? null : (TaggedSet)undo.firstViewRevOrig.clone();      
    } else { 
      if (undo.nameChange) {
        name_ = undo.nameOrig;
      }
      if (undo.descChange) {
        desc_ = undo.descOrig;
      }
    }
 
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(NetworkOverlayChange redo) { 
    if ((redo.nmOrig != null) && (redo.nmNew != null)) {
      throw new IllegalArgumentException();
    } else if ((redo.nmOrig == null) && (redo.nmNew != null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(redo.nmNew.getID());
      modules_.add(redo.index, redo.nmNew.clone());
    } else if ((redo.nmOrig != null) && (redo.nmNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).removeKey(redo.nmOrig.getID());
      modules_.remove(redo.index);
    } else if ((redo.nmlOrig != null) && (redo.nmlNew != null)) {
      linkages_.set(redo.index, redo.nmlNew.clone());
    } else if ((redo.nmlOrig == null) && (redo.nmlNew != null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(redo.nmlNew.getID());
      linkages_.add(redo.index, redo.nmlNew.clone());
    } else if ((redo.nmlOrig != null) && (redo.nmlNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).removeKey(redo.nmlOrig.getID());
      linkages_.remove(redo.index);
    } else if (redo.firstViewChanged) {
      firstViewMods_ = (redo.firstViewModNew == null) ? null : (TaggedSet)redo.firstViewModNew.clone();
      firstViewRevs_ = (redo.firstViewRevNew == null) ? null : (TaggedSet)redo.firstViewRevNew.clone();      
    } else {
      if (redo.nameChange) {
        name_ = redo.nameNew;
      }
      if (redo.descChange) {
        desc_ = redo.descNew;
      }
    } 
    return;
  }
 
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetOverlayWorker extends AbstractFactoryClient {
    
    private StringBuffer charBuf_;
    private BTState appState_;
    
    public NetOverlayWorker(BTState appState, FactoryWhiteboard whiteboard) {
      super(whiteboard);
      appState_ = appState;
      myKeys_.add("netOverlay");
      installWorker(new NetModule.NetModuleWorker(whiteboard), new MyGlue());
      installWorker(new NetModuleLinkage.NetModuleLinkageWorker(appState_, whiteboard), new MyLinkageGlue());
      installWorker(new FirstModWorker(whiteboard), null);
      installWorker(new FirstVizWorker(whiteboard), null);      
      charBuf_ = new StringBuffer();
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("netOverlay")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netOvr = buildFromXML(elemName, attrs);
        retval = board.netOvr;
      } else if (elemName.equals("descrip")) {
        charBuf_.setLength(0);   
      }
      return (retval);     
    }

    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("descrip")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netOvr.setDescription(CharacterEntityMapper.unmapEntities(charBuf_.toString(), false));
      }
      return;
    }

    protected void localProcessCharacters(char[] chars, int start, int length) {
      String nextString = new String(chars, start, length);
      charBuf_.append(nextString);
      return;
    } 
    
    private NetworkOverlay buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "netOverlay", "id", true);
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "netOverlay", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new NetworkOverlay(appState_, id, name, null));
    }
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetOverlayOwner owner = (board.genome != null) ? (NetOverlayOwner)board.genome : (NetOverlayOwner)board.prox;
      NetworkOverlay netOvr = board.netOvr;
      NetModule currMod = board.netMod;
      owner.addNetworkModuleAndKey(netOvr.getID(), currMod);
      return (null);
    }
  }  
  
  public static class MyLinkageGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetOverlayOwner owner = (board.genome != null) ? (NetOverlayOwner)board.genome : (NetOverlayOwner)board.prox;
      NetworkOverlay netOvr = board.netOvr;
      NetModuleLinkage currLink = board.netModLink;
      owner.addNetworkModuleLinkageAndKey(netOvr.getID(), currLink);
      return (null);
    }
  } 

  public static class FirstModWorker extends AbstractFactoryClient {
    
    public FirstModWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("startupMods");
      installWorker(new TaggedSet.TaggedSetWorker(whiteboard), new MyTSFMGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      return (null);     
    }
  }
  
  public static class FirstVizWorker extends AbstractFactoryClient {
    
    public FirstVizWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("startupViz");
      installWorker(new TaggedSet.TaggedSetWorker(whiteboard), new MyTSFVGlue());
    }  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      return (null);     
    }
  }  
  
  public static class MyTSFMGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetworkOverlay netOvr = board.netOvr;
      TaggedSet tSet = board.currentTaggedSet;
      netOvr.loadForFirstViewMods(tSet); 
      return (null);
    }
  }
  
  public static class MyTSFVGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetworkOverlay netOvr = board.netOvr;
      TaggedSet tSet = board.currentTaggedSet;
      netOvr.loadForFirstViewRevs(tSet); 
      return (null);
    }
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor  Do not use
  */

  @SuppressWarnings("unused")
  private NetworkOverlay() {
  }  
    
}
