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
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This represents a Region in BioTapestry
*/

public class Group implements GenomeItem, Cloneable {

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
  private HashSet<GroupMember> members_;
  private String ref_;
  private boolean inherited_;
  private String activeSubset_;
  private boolean usingParent_;
  private String parentID_;
  private String noName_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public Group(Group other) {
    this.noName_ = other.noName_;
    this.id_ = other.id_;
    this.name_ = other.name_;
    this.members_ = new HashSet<GroupMember>();
    Iterator<GroupMember> mit = other.members_.iterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      this.members_.add(new GroupMember(mem));
    }
    this.ref_ = other.ref_;
    this.inherited_ = other.inherited_;
    this.activeSubset_ = other.activeSubset_;
    this.usingParent_ = other.usingParent_;
    this.parentID_ = other.parentID_;
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor with a new ID
  */

  public Group(Group other, String newID) {
    this.noName_ = other.noName_;
    this.id_ = newID;
    this.name_ = other.name_;
    this.members_ = new HashSet<GroupMember>();
    Iterator<GroupMember> mit = other.members_.iterator();
    while (mit.hasNext()) {
      GroupMember mem = mit.next();
      this.members_.add(new GroupMember(mem));
    }
    this.ref_ = other.ref_;
    this.inherited_ = other.inherited_;
    this.activeSubset_ = other.activeSubset_;
    this.usingParent_ = other.usingParent_;
    this.parentID_ = other.parentID_;
  }  
  
  /***************************************************************************
  **
  ** Make an empty group (UI Creation)
  */

  public Group(ResourceManager rMan, String id, String name) {
    this(rMan, id, name, null);
  }
  
  /***************************************************************************
  **
  ** Make an empty group
  */

  public Group(ResourceManager rMan, String id, String name, String parentID) {
    noName_ = rMan.getString("groupName.noName");
    id_ = id.trim();
    name_ = name;
    members_ = new HashSet<GroupMember>();
    inherited_ = false;
    activeSubset_ = null;
    usingParent_ = false;
    parentID_ = parentID;
  }

  /***************************************************************************
  **
  ** Make an inherited group
  */

  public Group(String ref, String usingParent, String activeSubset, ResourceManager rMan) {
    noName_ = rMan.getString("groupName.noName");
    id_ = ref + ":0";
    ref_ = ref;
    inherited_ = true;
    activeSubset_ = activeSubset;
    members_ = new HashSet<GroupMember>();    
    usingParent_ = Boolean.valueOf(usingParent).booleanValue();
    parentID_ = null; // FIX ME?  Only set if active parent, child both present in genome
  }  

  /***************************************************************************
  **
  ** Make an inherited group (UI Creation)
  */

  public Group(ResourceManager rMan, String ref, boolean usingParent, String activeSubset) {
    noName_ = rMan.getString("groupName.noName");
    id_ = ref + ":0";
    ref_ = ref;
    inherited_ = true;
    activeSubset_ = activeSubset;
    members_ = new HashSet<GroupMember>();    
    usingParent_ = usingParent;
    parentID_ = null; // FIX ME?  Only set if active parent, child both present in genome 
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
  
  public Group clone() { 
    try {
      Group retval = (Group)super.clone();
      retval.members_ = new HashSet<GroupMember>();
      Iterator<GroupMember> mit = this.members_.iterator();
      while (mit.hasNext()) {
        GroupMember mem = mit.next();
        retval.members_.add(mem.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Make a copy for genome instance copying
  */

  public Group getMappedCopy(DBGenome rootGenome, GenomeInstance oldGi, Map<String, String> groupIDMap) {
    //
    // Here's a reminder of the needlessly complex naming/inheritance/subgroup 
    // scheme that has evolved. YUK! (FIX ME!):
    //
    // Regular groups (root instance level)
    // <group id="86" name="FP" >
    // <group id="85" name="V3" >
    // Subgroups (root instance level)
    // <group id="99a" name="SR1" parent="86" >
    // <group id="99b" name="SR2" parent="85" >
    // Regular groups (VfN level)
    // <group ref="86" inherited="true" />
    // Group with activated subgroup:
    // <group ref="85" inherited="true" activeSubset="99b:0" />
    // Subgroup (included, not activated):
    // <group ref="99a" inherited="true" />
    // Activated subgroup:
    // <group ref="99b" inherited="true" usingParent="true" />
    // In the next level down, start to have :0 per inheritance level:
    // <group ref="85:0" inherited="true" activeSubset="99b:0:0" />
    // <group ref="86:0" inherited="true" />
    // <group ref="99a:0" inherited="true" />
    // <group ref="99b:0" inherited="true" usingParent="true" />
    // Dynamic proxy groups:
    //<dpGroups>
    //    <dpGroup ref="Mes:0" inherited="true" />
    //    <dpGroup ref="Maternal:0" inherited="true" />
    //    <dpGroup ref="Endomes:0" inherited="true" activeSubset="EndomesMes:0:0" />
    //    <dpGroup ref="EndomesMes:0" inherited="true" usingParent="true" />
    //</dpGroups>
      
    int generation = oldGi.getGeneration();
    Group retval = new Group(this);
    boolean iAmInherited = this.isInherited();
    boolean iAmASubset = this.isASubset(oldGi);
    if (!iAmInherited) {
      retval.id_ = rootGenome.getNextKey();
      groupIDMap.put(this.id_, retval.id_);
      if (iAmASubset) {
        retval.parentID_ = groupIDMap.get(this.parentID_);
        if (retval.parentID_ == null) {
          throw new IllegalStateException();  // Gotta copy non-subset groups first!
        }
      }
      if ((this.activeSubset_ != null) || this.usingParent_) {
        throw new IllegalStateException(); // Can't have active subsets at root instance level
      }
    } else {  // VfN levels:  group map may be empty if copy begins below the VfA level
      retval.ref_ = groupIDMap.get(this.ref_);
      if (retval.ref_ == null) {
        retval.ref_ = this.ref_;
      }
      String myBase = getBaseID(this.id_);
      String retBase = groupIDMap.get(myBase);
      if (retBase == null) {
        retBase = myBase;
      }      
      retval.id_ = buildInheritedID(retBase, generation); 
      groupIDMap.put(this.id_, retval.id_);
      if (this.activeSubset_ != null) {
        String myAsBase = getBaseID(this.activeSubset_);
        String retAsBase = groupIDMap.get(myAsBase);
        if (retAsBase == null) {
          retAsBase = myAsBase;
        }    
        retval.activeSubset_ = buildInheritedID(retAsBase, generation);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Make a copy for genome instance copying
  */

  public Group getMappedCopy(DBGenome rootGenome, DynamicInstanceProxy dpi, Map<String, String> groupIDMap) {
    GenomeInstance gi = dpi.getAnInstance(); //.getVfgParent();//.getStaticVfgParent();
    return (getMappedCopy(rootGenome, gi, groupIDMap));
  }  
  
  /***************************************************************************
  **
  ** Make a copy for a dynamic proxy
  */

  public Group copyForProxy() {
    return (new Group(this));
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
  ** Get the display name
  ** 
  */  
  
  public String getDisplayName() {
    return (((name_ == null) || (name_.trim().equals(""))) ? noName_ : name_);
  }

  /***************************************************************************
  **
  ** Get the reference
  ** 
  */
  
  public String getReference() {
    if (!inherited_) {
      System.err.println(this);
      throw new IllegalArgumentException();
    }
    return (ref_);
  }  

  /***************************************************************************
  **
  ** Get the active subset (may be null)
  ** 
  */
  
  public String getActiveSubset() {
    return (activeSubset_);
  }

  /***************************************************************************
  **
  ** Set the active subset (may be null)
  ** 
  */
  
  public void setActiveSubset(String activeSubset) {
    activeSubset_ = activeSubset;
    return;
  }  

  /***************************************************************************
  **
  ** Answer if we are using a parent to render
  ** 
  */
  
  public boolean isUsingParent() {
    return (usingParent_);
  }  

  /***************************************************************************
  **
  ** Answer if we are inherited
  ** 
  */
  
  public boolean isInherited() {
    return (inherited_);
  }  
 
  /***************************************************************************
  **
  ** Get parent ID if we are a subset.  PROBABLY DON'T WANT TO USE THIS!
  ** (see getParentGroup(GenomeInstance gi)) Works OK only at VFA level
  ** 
  */
  
  public String getParentID() {
    return (parentID_);
  } 
  
  /***************************************************************************
  **
  ** Answer if we are a subset group.  Instance may be us, or may be a parent.
  ** 
  */
  
  public boolean isASubset(GenomeInstance gi) {
    // This class is all screwed up on how it deals with subset groups.  FIX ME!    
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    return (rootGroup.parentID_ != null);
  }
  
  /***************************************************************************
  **
  ** Answer if we are a subset group.  Instance may be us, or may be a parent.
  ** 
  */
  
  public boolean isASubset(DynamicInstanceProxy dpi) {
    // This class is all screwed up on how it deals with subset groups.  FIX ME! 
    GenomeInstance gi = dpi.getStaticVfgParent();
    return (isASubset(gi));
  }
    
  /***************************************************************************
  **
  ** Answer if we are a subset group of the given group.  Instance may be us, or may be a parent.
  ** 
  */
  
  public boolean isASubsetOf(String parentID, GenomeInstance gi) {
    // This class is all screwed up on how it deals with subset groups.  FIX ME!    
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    return ((rootGroup.parentID_ != null) && (rootGroup.parentID_.equals(getBaseID(parentID))));
  }  

  /***************************************************************************
  **
  ** Get the parent group.  More genome instance dependent crap that needs
  ** a major overhaul.  Instance MUST be our instance; not a parent.
  ** 
  */
  
  public String getParentGroup(GenomeInstance gi) {
    int generation = gi.getGeneration();
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    String parent = rootGroup.parentID_;
    if (parent == null) {
      return (null);
    }
    return (buildInheritedID(parent, generation));
  }

  /***************************************************************************
  **
  ** Return the set of all subgroups
  */
  
  public Set<String> getSubsets(GenomeInstance gi) {
    return (getSubsetGuts(gi.getGroupIterator(), gi));
  }
  
  /***************************************************************************
  **
  ** Return the set of all subgroups
  */
  
  public Set<String> getSubsets(DynamicInstanceProxy dpi) {
    GenomeInstance gi = dpi.getVfgParent();
    return (getSubsetGuts(dpi.getGroupIterator(), gi));
  }  
 
  /***************************************************************************
  **
  ** Return the set of all subgroups
  */
  
  public Set<String> getSubsetGuts(Iterator<Group> grit, GenomeInstance gi) {
    HashSet<String> retval = new HashSet<String>();
    while (grit.hasNext()) {
      Group group = grit.next();
      if (group.isASubsetOf(id_, gi)) {
        retval.add(group.getID());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Add a member
  ** 
  */
  
  public GroupChange addMember(GroupMember member, String genomeKey) {
    GroupChange retval = new GroupChange();
    retval.genomeKey = genomeKey;
    retval.groupKey = id_;
    retval.grmNew = member;
    retval.grmOrig = null;
    members_.add(member);
    return (retval);
  }

  /***************************************************************************
  **
  ** Remove a member
  ** 
  */
  
  public GroupChange removeMember(String memberID, String genomeKey) {
    GroupChange retval = new GroupChange();
    retval.genomeKey = genomeKey;
    retval.groupKey = id_;
    retval.grmNew = null;
    Iterator<GroupMember> gmit = getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      if (gm.getID().equals(memberID)) {
        retval.grmOrig = gm;
        members_.remove(gm);
        return (retval);
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the members. Warning: USELESS unless we are in a root instance
  ** 
  */
  
  public Iterator<GroupMember> getMemberIterator() {
    return (members_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get member count.  Warning: USELESS unless we are in a root instance
  ** 
  */
  
  public int getMemberCount() {
    return (members_.size());
  }
  
  /***************************************************************************
  **
  ** Answers if we are empty >>>at this inherited level<<< (could be root)
  ** 
  */
  
  public boolean inheritedIsEmpty(GenomeInstance gi) {
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    Iterator<GroupMember> gmit = rootGroup.getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      if (gir == null) {
        return (false);
      }
      if (gi.getNode(gm.getID()) != null) {
        return (false);
      }
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Give back the group in parent.  Not for use in root instances
  ** 
  */
  
  public Group getGroupInParent(GenomeInstance gi) {  
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    String baseGrpID = getBaseID(id_);   
    int genCount = parent.getGeneration();        
    String inherit = buildInheritedID(baseGrpID, genCount);
    Group parentGroup = parent.getGroup(inherit);
    return (parentGroup);
  }

  /***************************************************************************
  **
  ** Answers if the given item is in the group
  ** 
  */
  
  public boolean isInGroup(String itemID, GenomeInstance gi) {
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    Iterator<GroupMember> gmit = rootGroup.getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      if (gm.getID().equals(itemID)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Use instead of above to speed things up in batch cases
  */
  
  public Set<String> areInGroup(GenomeInstance gi) {
    HashSet<String> retval = new HashSet<String>();
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    Iterator<GroupMember> gmit = rootGroup.getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      retval.add(gm.getID());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the instance present in the group for the base ID.
  ** 
  */
  
  public Node getInstanceInGroup(String baseID, GenomeInstance gi) {
    GenomeInstance gir = gi.getVfgParentRoot();
    GenomeInstance wgi = (gir == null) ? gi : gir;
    Group rootGroup = wgi.getGroup(getBaseID(id_));
    Iterator<GroupMember> gmit = rootGroup.getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      if (GenomeItemInstance.getBaseID(gm.getID()).equals(baseID)) {
        return (gi.getNode(gm.getID()));
      }
    }
    return (null);
  }

  /***************************************************************************
  **
  ** Get inherited name
  ** 
  */
  
  public String getInheritedDisplayName(GenomeInstance gi) {
    if (!inherited_) {
      return (getDisplayName());
    }
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    return (rootGroup.getDisplayName());
  }
  
  /***************************************************************************
  **
  ** Get inherited name
  ** 
  */
  
  public String getInheritedTrueName(GenomeInstance gi) {
    if (!inherited_) {
      return (getName());
    }
    GenomeInstance gir = gi.getVfgParentRoot();
    gi = (gir == null) ? gi : gir;
    Group rootGroup = gi.getGroup(getBaseID(id_));
    return (rootGroup.getName());
  }  

  /***************************************************************************
  **
  ** Answers if an instance of the given base item is in the group.  WARNING!
  ** Only use for groups in a VFGRoot!
  ** 
  */
  
  public boolean instanceIsInGroup(String itemID) {
    Iterator<GroupMember> gmit = getMemberIterator();
    while (gmit.hasNext()) {
      GroupMember gm = gmit.next();
      if (GenomeItemInstance.getBaseID(gm.getID()).equals(itemID)) {
        return (true);
      }
    }
    return (false);
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("Group: id = " + id_ + "name = " + name_ + " members = " + members_ 
            + " ref = " + ref_ + " inherited = " + inherited_  + " activeSubset =" +
            activeSubset_  + " usingParent = " + usingParent_ + 
            " parentID = " + parentID_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind, boolean forDynamic) {
    ind.indent();     
    out.print((forDynamic) ? "<dpGroup " : "<group ");    
    if (inherited_) {
      out.print("ref=\""); 
      out.print(ref_);
      out.print("\" inherited=\"true"); 
      if (usingParent_) {       
        out.print("\" usingParent=\"true");
      }
      if (activeSubset_ != null) {       
        out.print("\" activeSubset=\"");
        out.print(activeSubset_);        
      }      
      out.println("\" />");
    } else {
      out.print("id=\""); 
      out.print(id_);
      if (name_ != null) {
        out.print("\" name=\"");
        out.print(CharacterEntityMapper.mapEntities(name_, false));
      } 
      if (parentID_ != null) {
        out.print("\" parent=\"");
        out.print(parentID_);
      }       
      out.println("\" >");    
      ind.up().indent();
      out.println("<members>");
      Iterator<GroupMember> mi = members_.iterator();
      ind.up();
      while (mi.hasNext()) {
        GroupMember mem = mi.next();
        mem.writeXML(out, ind);
      }
      ind.down().indent();    
      out.println("</members>");
      ind.down().indent();
      out.println((forDynamic) ? "</dpGroup> " : "</group>");
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(GroupChange undo) {
    //
    // If both orig and new are present, we are simply undoing a property change.
    // If orig is absent, we are undoing an add.
    // If new is absent, we are undoing a delete.
    if ((undo.grmOrig != null) && (undo.grmNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<GroupMember> gmit = getMemberIterator();
      while (gmit.hasNext()) {
        GroupMember gm = gmit.next();
        if (gm.getID().equals(undo.grmNew.getID())) {
          members_.remove(gm);
          break;
        }
      }      
      members_.add(undo.grmOrig);
    } else if (undo.grmOrig == null) {
      Iterator<GroupMember> gmit = getMemberIterator();
      while (gmit.hasNext()) {
        GroupMember gm = gmit.next();
        if (gm.getID().equals(undo.grmNew.getID())) {
          members_.remove(gm);
          return;
        }
      }
    } else {
      members_.add(undo.grmOrig);
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(GroupChange redo) { 
    //
    // If both orig and new are present, we are simply redoing a property change.
    // If orig is absent, we are redoing an add.
    // If new is absent, we are redoing a delete.
    if ((redo.grmOrig != null) && (redo.grmNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<GroupMember> gmit = getMemberIterator();
      while (gmit.hasNext()) {
        GroupMember gm = gmit.next();
        if (gm.getID().equals(redo.grmOrig.getID())) {
          members_.remove(gm);
          break;
        }
      }
      members_.add(redo.grmNew);
    } else if (redo.grmOrig == null) {
      members_.add(redo.grmNew);
    } else {
      Iterator<GroupMember> gmit = getMemberIterator();
      while (gmit.hasNext()) {
        GroupMember gm = gmit.next();
        if (gm.getID().equals(redo.grmOrig.getID())) {
          members_.remove(gm);
          return;
        }
      }
    }
    return;
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
  
  public static Set<String> keywordsOfInterest(boolean forDynamic) {
    HashSet<String> retval = new HashSet<String>();
    if (forDynamic) {
      retval.add("dpGroup");
    } else {
      retval.add("group");
      retval.add("members");
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle Group creation
  **
  */
  
  public static Group buildFromXML(ResourceManager rMan, String elemName, Attributes attrs) throws IOException {
    
    if ((!elemName.equals("group")) && (!elemName.equals("dpGroup"))) {
      return (null);
    }
                                             
    String id = null;
    String name = null;
    String ref = null;
    String inherited = null; 
    String usingParent = null;
    String activeSubset = null;
    String parentID = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          id = val;
        } else if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("ref")) {
          ref = val;
        } else if (key.equals("inherited")) {
          inherited = val;
        } else if (key.equals("usingParent")) {
          usingParent = val;
        } else if (key.equals("activeSubset")) {
          activeSubset = val;
        } else if (key.equals("parent")) {
          parentID = val;
        }
      }
    }
    
    if ((inherited == null) && (ref == null) && (id == null)) {
      throw new IOException();
    }
    
    boolean isInherited = Boolean.valueOf(inherited).booleanValue();
    
    if (isInherited) {   
      return (new Group(ref, usingParent, activeSubset, rMan));
    } else {
      return (new Group(rMan, id, name, parentID));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the baseID
  ** 
  */
  
  public static String getBaseID(String id) {
    int ind = id.indexOf(":");
    return ((ind == -1) ? id : id.substring(0, ind));
  }

  /***************************************************************************
  **
  ** Build inherited ID
  ** 
  */
  
  public static String buildInheritedID(String id, int generation) {
    StringBuffer buf = new StringBuffer();
    buf.append(getBaseID(id));
    for (int i = 0; i < generation; i++) {
      buf.append(":0");
    }
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Remove a generation from an id
  ** 
  */
  
  public static String removeGeneration(String id) {
    int last = id.lastIndexOf(":0");
    return ((last == -1) ? id : id.substring(0, last));
  }
  
  /***************************************************************************
  **
  ** Add a generation to an id
  ** 
  */
  
  public static String addGeneration(String id) {
    return (id + ":0");
  }
  
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
  private Group() {
  }  
    
}
