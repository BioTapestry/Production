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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.NameValuePairList;

/****************************************************************************
**
** This represents an arbitrary user-defined set of network elements
*/

public class NetModule implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
  private String name_;
  private String desc_;
  private String attachedToGroup_;
  private HashSet<NetModuleMember> members_;
  private TreeSet<String> tags_;
  private NameValuePairList nvPairs_; 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public NetModule(NetModule other) {
    this.id_ = other.id_;
    this.name_ = other.name_;
    this.desc_ = other.desc_;
    this.attachedToGroup_ = other.attachedToGroup_;
    this.members_ = new HashSet<NetModuleMember>();
    Iterator<NetModuleMember> mit = other.members_.iterator();
    while (mit.hasNext()) {
      NetModuleMember mem = mit.next();
      this.members_.add(mem.clone());
    }
    // Just share strings:
    this.tags_ = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    this.tags_.addAll(other.tags_);
    
    this.nvPairs_ = other.nvPairs_.clone();
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID
  */

  public NetModule(NetModule other, String newID) {
    this.id_ = newID;
    this.name_ = other.name_;
    this.desc_ = other.desc_;
    this.attachedToGroup_ = other.attachedToGroup_;
    this.members_ = new HashSet<NetModuleMember>();
    Iterator<NetModuleMember> mit = other.members_.iterator();
    while (mit.hasNext()) {
      NetModuleMember mem = mit.next();
      this.members_.add(mem.clone());
    }
    // Just share strings:
    this.tags_ = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    this.tags_.addAll(other.tags_);
    
    this.nvPairs_ = other.nvPairs_.clone();
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID, and maps for the group and nodes
  */

  public NetModule(NetModule other, String newID, Map<String, String> groupMap, Map<String, String> nodeMap) {
    this.id_ = newID;
    this.name_ = other.name_;
    this.desc_ = other.desc_;
    if ((other.attachedToGroup_ != null) && (groupMap != null)) {
      this.attachedToGroup_ = groupMap.get(other.attachedToGroup_);
      if (this.attachedToGroup_ == null) {
        throw new IllegalStateException();
      }
    } else {
      this.attachedToGroup_ = other.attachedToGroup_;
    } 
    this.members_ = new HashSet<NetModuleMember>();
    Iterator<NetModuleMember> mit = other.members_.iterator();
    while (mit.hasNext()) {
      NetModuleMember mem = mit.next();
      NetModuleMember newMem;
      if (nodeMap != null) {
        newMem = new NetModuleMember(mem, nodeMap);
      } else {
        newMem = mem.clone();
      }
      this.members_.add(newMem);
    }
    // Just share strings:
    this.tags_ = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    this.tags_.addAll(other.tags_);
    
    this.nvPairs_ = other.nvPairs_.clone();
  }    
 
  /***************************************************************************
  **
  ** Make an empty module
  */

  public NetModule(String id, String name, String desc, String groupAttach) {
    id_ = id;
    name_ = name;
    desc_ = desc;
    attachedToGroup_ = groupAttach;
    members_ = new HashSet<NetModuleMember>();
    tags_ = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    nvPairs_ = new NameValuePairList();
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
  
  public NetModule clone() { 
    try {
      NetModule retval = (NetModule)super.clone();
      retval.members_ = new HashSet<NetModuleMember>();
      Iterator<NetModuleMember> mit = this.members_.iterator();
      while (mit.hasNext()) {
        NetModuleMember mem = mit.next();
        retval.members_.add(mem.clone());
      }
      retval.tags_ = new TreeSet<String>(this.tags_);
      retval.nvPairs_ = this.nvPairs_.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

 /***************************************************************************
  **
  ** Dump all of the members
  ** 
  */
  
  public void dropAllMembers() {
    members_.clear();
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
  ** Set the name
  ** 
  */
  
  public void setName(String name) {
    name_ = name;
    return;
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
  ** Get the group we are attached to (if any)
  ** 
  */
  
  public String getGroupAttachment() {
    return (attachedToGroup_);
  } 
  
  /***************************************************************************
  **
  ** Set the group we are attached to (if any)
  ** 
  */
  
  public void setGroupAttachment(String groupAttachment) {
    attachedToGroup_ = groupAttachment;
    return;
  }
  
  /***************************************************************************
  **
  ** Detach us from our group
  ** 
  */
  
  public NetModuleChange detachFromGroup(String noOwnerKey, int ownerMode, String overlayKey) {
    if (attachedToGroup_ == null) {
      return (null);
    }
    NetModuleChange retval = new NetModuleChange();   
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;
    retval.moduleKey = id_;    
    retval.nmmNew = null;
    retval.nmmOrig = null;
    retval.nameChange = false;
    retval.descChange = false;
    retval.detachedGroup = attachedToGroup_;
    attachedToGroup_ = null;
    return (retval);  
  }  
  
  /***************************************************************************
  **
  ** Change the name and description
  ** 
  */
  
  public NetModuleChange changeNameAndDescription(String name, String desc, String noOwnerKey, int ownerMode, String overlayKey) {
    NetModuleChange retval = new NetModuleChange();
    
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;
    retval.moduleKey = id_;    
    retval.nmmNew = null;
    retval.nmmOrig = null;
 
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
  ** Add a member without undo support
  ** 
  */
  
  public void addMember(NetModuleMember member) {
    members_.add(member);
    return;
  }  
   
  /***************************************************************************
  **
  ** Add a member
  ** 
  */
  
  public NetModuleChange addMember(NetModuleMember member, String noOwnerKey, int ownerMode, String overlayKey) {
    NetModuleChange retval = new NetModuleChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;    
    retval.moduleKey = id_;
    retval.nmmNew = member.clone();
    retval.nmmOrig = null;
    members_.add(member);
    return (retval);
  }

  /***************************************************************************
  **
  ** Remove a member
  ** 
  */
  
  public NetModuleChange removeMember(String memberID, String noOwnerKey, int ownerMode, String overlayKey) {    
    NetModuleChange retval = new NetModuleChange();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;
    retval.moduleKey = id_;
    retval.nmmNew = null;
    Iterator<NetModuleMember> gmit = getMemberIterator();
    while (gmit.hasNext()) {
      NetModuleMember nmm = gmit.next();
      if (nmm.getID().equals(memberID)) {
        retval.nmmOrig = nmm.clone();
        members_.remove(nmm);
        return (retval);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a tag.  Use for IO ONLY
  ** 
  */
  
  void addTag(String tag) {
    tags_.add(tag);
    return;
  }
  
  /***************************************************************************
  **
  ** Replace all tags
  ** 
  */
  
  public NetModuleChange replaceAllTags(SortedSet<String> allTags, String noOwnerKey, int ownerMode, String overlayKey) {
    NetModuleChange retval = new NetModuleChange();
    retval.tagsOrig = new TreeSet<String>(tags_);
    tags_.clear();
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;    
    retval.moduleKey = id_;
    retval.tagsNew = new TreeSet<String>(allTags);
    tags_.addAll(allTags);
    return (retval);
  }

  /***************************************************************************
  **
  ** Answer if a String is a tag.  This approach collapses white space
  ** 
  */
  
  public boolean isATag(String tag) {
    return (DataUtil.containsKey(tags_, tag));
  }
 
  /***************************************************************************
  **
  ** Answer if node is a member
  ** 
  */
  
  public boolean isAMember(String nodeID) {
    Iterator<NetModuleMember> gmit = getMemberIterator();
    while (gmit.hasNext()) {
      NetModuleMember nmm = gmit.next();
      if (nmm.getID().equals(nodeID)) {
        return (true);
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Get an iterator over the members.
  ** 
  */
  
  public Iterator<NetModuleMember> getMemberIterator() {
    return (members_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get member count.
  ** 
  */
  
  public int getMemberCount() {
    return (members_.size());
  }
    
  /***************************************************************************
  **
  ** Get an iterator over the tags.
  ** 
  */
  
  public Iterator<String> getTagIterator() {
    return (tags_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get tag count.
  ** 
  */
  
  public int getTagCount() {
    return (tags_.size());
  }  
  
  /***************************************************************************
  **
  ** Get NVPairs
  ** 
  */
  
  public NameValuePairList getNVPairs() {
    return (nvPairs_);
  }    
 
  /***************************************************************************
  **
  ** Set NVPairs.  Use for IO ONLY
  ** 
  */
  
  void setNVPairs(NameValuePairList nvpl) {
    nvPairs_ = nvpl;
    return;
  }
  
  /***************************************************************************
  **
  ** Replace all name value pairs
  ** 
  */
  
  public NetModuleChange replaceAllNVPairs(NameValuePairList nvpl, String noOwnerKey, int ownerMode, String overlayKey) {
    NetModuleChange retval = new NetModuleChange();
    retval.nvplOrig = nvPairs_.clone(); 
    retval.noOwnerKey = noOwnerKey;
    retval.ownerMode = ownerMode;
    retval.overlayKey = overlayKey;    
    retval.moduleKey = id_;
    retval.nvplNew = nvpl.clone();
    nvPairs_ = nvpl;
    return (retval);
  }  

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("NetModule: id = " + id_ + "name = " + name_ + " desc = " + desc_ + " members = " + members_ + " tags = " + tags_  + " nvPairs = " + nvPairs_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();   
    out.print("<netModule id=\""); 
    out.print(id_);
    if (name_ != null) {
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(name_, false));
    }     
    if (attachedToGroup_ != null) {
      out.print("\" group=\"");
      out.print(attachedToGroup_);
    }        
    if ((members_.size() == 0) && (tags_.size() == 0) && (nvPairs_.size() == 0) && ((desc_ == null) || desc_.trim().equals(""))) {
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
    
    if (members_.size() != 0) {
      ind.indent();
      out.println("<modMembers>");
      Iterator<NetModuleMember> mi = members_.iterator();
      ind.up();
      while (mi.hasNext()) {
        NetModuleMember mem = mi.next();
        mem.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</modMembers>");
    }
    
    if (tags_.size() != 0) {
      ind.indent();
      out.println("<modTags>");
      Iterator<String> ti = tags_.iterator();
      ind.up();
      while (ti.hasNext()) {
        ind.indent();
        String tag = ti.next();
        out.print("<modTag>");
        out.print(CharacterEntityMapper.mapEntities(tag, false));
        out.println("</modTag>");
      }
      ind.down().indent();
      out.println("</modTags>");
    }
    
    if (nvPairs_.size() != 0) {
      nvPairs_.writeXML(out, ind);
    }

    ind.down().indent();
    out.println("</netModule>");
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(NetModuleChange undo) {
    if ((undo.tagsOrig != null) && (undo.tagsNew != null)) {
      tags_.clear();
      tags_.addAll(undo.tagsOrig);
 
    } else if ((undo.nmmOrig != null) && (undo.nmmNew != null)) {
      throw new IllegalArgumentException();
    } else if ((undo.nmmOrig == null) && (undo.nmmNew != null)) {
      members_.remove(undo.nmmNew);
    } else if ((undo.nmmOrig != null) && (undo.nmmNew == null)) {
      members_.add(undo.nmmOrig.clone());
    } else if (undo.detachedGroup != null) {
      attachedToGroup_ = undo.detachedGroup;
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
  
  public void changeRedo(NetModuleChange redo) {
    if ((redo.tagsOrig != null) && (redo.tagsNew != null)) {
      tags_.clear();
      tags_.addAll(redo.tagsNew);
      
    } else if ((redo.nmmOrig != null) && (redo.nmmNew != null)) {
      throw new IllegalArgumentException();
    } else if ((redo.nmmOrig == null) && (redo.nmmNew != null)) {
      members_.add(redo.nmmNew.clone());
    } else if ((redo.nmmOrig != null) && (redo.nmmNew == null)) {
      members_.remove(redo.nmmOrig);
    } else if (redo.detachedGroup != null) {
      attachedToGroup_ = null;  
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
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetModuleWorker extends AbstractFactoryClient {
    
    private StringBuffer charBuf_;
    
    public NetModuleWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("netModule");
      installWorker(new NetModuleMember.NetModuleMemberWorker(whiteboard), new MyGlue());
      installWorker(new NameValuePairList.NameValPairListWorker(whiteboard), new MyNVGlue());      
      charBuf_ = new StringBuffer();
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("netModule")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netMod = buildFromXML(elemName, attrs);
        retval = board.netMod;
      } else if (elemName.equals("descrip")) {
        charBuf_.setLength(0);   
      } else if (elemName.equals("modTag")) {
        charBuf_.setLength(0);   
      }
      return (retval);
    }

    protected void localFinishElement(String elemName) throws IOException {
      if (elemName.equals("descrip")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netMod.setDescription(CharacterEntityMapper.unmapEntities(charBuf_.toString(), false));
      } else if (elemName.equals("modTag")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.netMod.addTag(CharacterEntityMapper.unmapEntities(charBuf_.toString(), false));        
      }
      return;
    }

    protected void localProcessCharacters(char[] chars, int start, int length) {
      String nextString = new String(chars, start, length);
      charBuf_.append(nextString);
      return;
    }         
     
    private NetModule buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "netModule", "id", true);
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "netModule", "name", true);
      String groupID = AttributeExtractor.extractAttribute(elemName, attrs, "netModule", "group", false);      
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new NetModule(id, name, null, groupID));
    }
  } 
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetOverlayOwner owner = (board.genome != null) ? (NetOverlayOwner)board.genome : (NetOverlayOwner)board.prox;
      String ownerID = owner.getID();
      int ownerType = board.genomeType;
      NetworkOverlay netOvr = board.netOvr;
      NetModule currMod = board.netMod;
      currMod.addMember((NetModuleMember)kidObj, ownerID, ownerType, netOvr.getID()); 
      return (null);
    }
  }
  
  public static class MyNVGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, Object optionalArgs) {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      NetModule currMod = board.netMod;
      currMod.setNVPairs((NameValuePairList)kidObj); 
      return (null);
    }
  } 
  
  /***************************************************************************
  **
  ** All the info needed to dig down and find a given module.  We support
  ** null modKeys for representing overlays with no modules in some applications
  ** (e.g. layout copying):
  */  
      
  public static class FullModuleKey implements Cloneable {
    
    public int ownerType;
    public String ownerKey;
    public String ovrKey;
    public String modKey;
    
    public FullModuleKey(int ownerType, String ownerKey, String ovrKey, String modKey) {
      this.ownerType = ownerType;
      this.ownerKey = ownerKey;
      this.ovrKey = ovrKey;
      this.modKey = modKey;
    }
    
    public String toString() {
      return ("FullModuleKey owner = " + ownerType + " " + ownerKey + " " + ovrKey  + " " + modKey);
    }
    
    public int hashCode() {
      return (ownerKey.hashCode() + ovrKey.hashCode() + ((modKey == null) ? 0 : modKey.hashCode()) + ownerType);
    }

    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof FullModuleKey)) {
        return (false);
      }
      FullModuleKey otherFMK = (FullModuleKey)other;
      
      if (this.modKey == null) {
        if (otherFMK.modKey != null) {
          return (false);
        }
      } else if (!this.modKey.equals(otherFMK.modKey)) {
        return (false);
      }       
      
      if (!this.ovrKey.equals(otherFMK.ovrKey)) {
        return (false);
      }  
      
      if (!this.ownerKey.equals(otherFMK.ownerKey)) {
        return (false);
      }          
      
      return (this.ownerType == otherFMK.ownerType);
    }
    
    public FullModuleKey clone() {
      try {
        return ((FullModuleKey)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
  }    

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
  private NetModule() {
  }  
    
}
