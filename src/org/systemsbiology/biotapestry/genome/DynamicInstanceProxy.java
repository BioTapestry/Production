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

package org.systemsbiology.biotapestry.genome;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.MessageFormat;
import java.util.Arrays;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.StartupView;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.UserTreePathChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This is a proxy for dynamically generated model instances
*/

public class DynamicInstanceProxy implements Cloneable, NetOverlayOwner {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private BTState appState_;
  private String vfgParent_;  
  private String name_;
  private String id_;
  private HashMap<String, DynamicGenomeInstance> cache_;
  private HashMap<Integer, String> imageKeys_;
  private OverlayOpsSupport ovrops_;
  private int min_;
  private int max_;
  private boolean isSingle_;
  private TreeSet<Integer> sortedTimes_;
  private ArrayList<Group> groups_;
  private ArrayList<AddedNode> addedNodes_;
  private ArrayList<Note> notes_;
  private UniqueLabeller labels_;  
 
  private static final String KEY_PREF_ = ":";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for UI-based creation
  */

  public DynamicInstanceProxy(BTState appState, String name, String id, 
                              GenomeInstance vfgParent, boolean isSingle, 
                              int minTime, int maxTime) {
    appState_ = appState;
    name_ = name;
    id_ = id;
    vfgParent_ = vfgParent.getID();
    cache_ = new HashMap<String, DynamicGenomeInstance>();
    groups_ = new ArrayList<Group>();
    addedNodes_ = new ArrayList<AddedNode>();
    notes_ = new ArrayList<Note>();
    labels_ = new UniqueLabeller();
    labels_.setFixedPrefix(id_ + KEY_PREF_);
    imageKeys_ = new HashMap<Integer, String>();
    ovrops_ = new OverlayOpsSupport(appState_, NetworkOverlay.DYNAMIC_PROXY, id_);
    isSingle_ = isSingle;
    min_ = minTime;
    max_ = maxTime;
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public DynamicInstanceProxy(BTState appState, String name, String id, 
                              GenomeInstance vfgParent, String isSingle, 
                              String minTime, String maxTime) throws IOException {
    appState_ = appState;
    name_ = name;
    id_ = id;
    vfgParent_ = vfgParent.getID();
    cache_ = new HashMap<String, DynamicGenomeInstance>();
    groups_ = new ArrayList<Group>();
    addedNodes_ = new ArrayList<AddedNode>();
    notes_ = new ArrayList<Note>();
    labels_ = new UniqueLabeller();
    labels_.setFixedPrefix(id_ + KEY_PREF_);    
    imageKeys_ = new HashMap<Integer, String>();
    ovrops_ = new OverlayOpsSupport(appState_, NetworkOverlay.DYNAMIC_PROXY, id_);

    isSingle_ = (isSingle == null) ? false : Boolean.valueOf(isSingle).booleanValue();    
        
    try {
      if (minTime != null) {
        min_ = Integer.parseInt(minTime);
      } else {
        min_ = -1;
      }
      if (maxTime != null) {
        max_ = Integer.parseInt(maxTime);
      } else {
        max_ = -1;
      }
    } catch (NumberFormatException nfex) {
      throw new IOException();
    }
  }

  /***************************************************************************
  **
  ** Copy Constructor: Use clone instead
  */

  private DynamicInstanceProxy(DynamicInstanceProxy other) {
    this.appState_ = other.appState_;
    this.name_ = other.name_;
    this.id_ = other.id_;
    this.vfgParent_ = other.vfgParent_;
    this.cache_ = new HashMap<String, DynamicGenomeInstance>();  // keep empty
    
    this.groups_ = new ArrayList<Group>();
    int numGrp = other.groups_.size();
    for (int i = 0; i < numGrp; i++) {
      this.groups_.add(other.groups_.get(i).clone());
    }
    
    this.addedNodes_ = new ArrayList<AddedNode>();
    int numAN = other.addedNodes_.size();
    for (int i = 0; i < numAN; i++) {
      this.addedNodes_.add(other.addedNodes_.get(i).clone());      
    }
   
    this.notes_ = new ArrayList<Note>();
    int numNo = other.notes_.size();
    for (int i = 0; i < numNo; i++) {
      this.notes_.add(other.notes_.get(i).clone());
    }
   
    this.labels_ = other.labels_.clone();
    
    this.isSingle_ = other.isSingle_;
    this.min_ = other.min_;
    this.max_ = other.max_;
 
    this.sortedTimes_ = null;
    if (other.sortedTimes_ != null) {
      this.sortedTimes_ = new TreeSet<Integer>(other.sortedTimes_); // contains immutable integers
    }
    
    //
    // Image keys
    //
    
    this.imageKeys_ = new HashMap<Integer, String>();
    Iterator<Integer> ikit = other.imageKeys_.keySet().iterator();
    while (ikit.hasNext()) {
      Integer timeKey = ikit.next();
      String imgKey = other.imageKeys_.get(timeKey);
      this.imageKeys_.put(timeKey, imgKey);
    }
    
    this.ovrops_ = other.ovrops_.clone();
    
    for (int i = 0; i < numAN; i++) {
      this.addedNodes_.add(other.addedNodes_.get(i).clone());      
    }
  }    
 
  /***************************************************************************
  **
  ** Copy constructor that is used to create a sibling or cousin.  Group IDs are modified
  ** and recorded in the provided map, image keys ref counts are bumped
  */

  public DynamicInstanceProxy(DynamicInstanceProxy other, String newName, 
                              String newVfgParentID, String newID, 
                              Map<String, String> groupIDMap, Map<String, String> noteIDMap, 
                              Map<String, String> ovrIDMap, Map<String, String> modIDMap, 
                              Map<String, String> modLinkIDMap, List<ImageChange> imageChanges) {
    this(other);
    this.name_ = newName;
    this.id_ = newID;
    // Part of Issue 195 Fix
    this.ovrops_.resetOwner(this.id_);
    this.vfgParent_ = newVfgParentID;

    this.labels_ = other.labels_.mappedPrefixCopy(other.id_ + KEY_PREF_, this.id_ + KEY_PREF_);

    this.notes_ = new ArrayList<Note>();
    int numNo = other.notes_.size();
    for (int i = 0; i < numNo; i++) {
      Note oldNote = other.notes_.get(i);
      String myNtID = UniqueLabeller.mapKeyPrefix(oldNote.getID(), other.id_ + KEY_PREF_, this.id_ + KEY_PREF_, null);
      noteIDMap.put(oldNote.getID(), myNtID);
      this.notes_.add(new Note(oldNote, myNtID));
    }
 
    //
    // Groups require wierdo, two-pass handling:
    //
    
    DBGenome rootGenome = (DBGenome)appState_.getDB().getGenome();
    this.groups_ = new ArrayList<Group>();
    int numGrp = other.groups_.size();
    for (int i = 0; i < numGrp; i++) {
      Group otherGroup = other.groups_.get(i);
      if (otherGroup.isASubset(other)) {
        continue;
      }
      Group groupCopy = otherGroup.getMappedCopy(rootGenome, other, groupIDMap);
      this.groups_.add(groupCopy);
    }
    //
    // Subgroups get done on a second pass:
    //
    
    for (int i = 0; i < numGrp; i++) {
      Group otherGroup = other.groups_.get(i);
      if (!otherGroup.isASubset(other)) {
        continue;
      }
      Group groupCopy = otherGroup.getMappedCopy(rootGenome, other, groupIDMap);
      this.groups_.add(groupCopy);
    }
    
    //
    // Added nodes reference groups IDs, so map these also:
    //
    
    this.addedNodes_ = new ArrayList<AddedNode>();
    int numAN = other.addedNodes_.size();
    for (int i = 0; i < numAN; i++) {
      AddedNode an = other.addedNodes_.get(i);
      AddedNode myAn = an.clone();
      String mappedGroup = groupIDMap.get(an.groupToUse);
      myAn.groupToUse = (mappedGroup == null) ? an.groupToUse : mappedGroup;      
      this.addedNodes_.add(myAn);
    }
    
    //
    // Overlays and modules have globally unique IDs, so generate new IDs and track the mapping.
    // We gotta know group mappings, so we do this after groups above.
    //
    
    ovrops_.copyWithMap(other.ovrops_, rootGenome, groupIDMap, ovrIDMap, modIDMap, modLinkIDMap);
  
    //
    // Bump image key ref counts while duping
    //
    
    ImageManager imgr = appState_.getImageMgr();
    this.imageKeys_ = new HashMap<Integer, String>();
    Iterator<Integer> ikit = other.imageKeys_.keySet().iterator();
    while (ikit.hasNext()) {
      Integer timeKey = ikit.next();
      String imgKey = other.imageKeys_.get(timeKey);
      imageKeys_.put(timeKey, imgKey);
      ImageChange ic = imgr.registerImageUsage(imgKey);
      ic.timeKey = timeKey;
      ic.proxyKey = getID();
      imageChanges.add(ic);
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
  
  public DynamicInstanceProxy clone() { 
    try {
      DynamicInstanceProxy retval = (DynamicInstanceProxy)super.clone();
      retval.cache_ = new HashMap<String, DynamicGenomeInstance>();  // keep empty

      retval.groups_ = new ArrayList<Group>();
      int numGrp = this.groups_.size();
      for (int i = 0; i < numGrp; i++) {
        retval.groups_.add(this.groups_.get(i).clone());
      }

      retval.addedNodes_ = new ArrayList<AddedNode>();
      int numAN = this.addedNodes_.size();
      for (int i = 0; i < numAN; i++) {
        retval.addedNodes_.add(this.addedNodes_.get(i).clone());      
      }

      retval.notes_ = new ArrayList<Note>();
      int numNo = this.notes_.size();
      for (int i = 0; i < numNo; i++) {
        retval.notes_.add(this.notes_.get(i).clone());
      }
        
      retval.ovrops_ = this.ovrops_.clone();
  
      retval.labels_ = this.labels_.clone();

      retval.sortedTimes_ = null;
      if (this.sortedTimes_ != null) {
        retval.sortedTimes_ = new TreeSet<Integer>(this.sortedTimes_); // contains immutable integers
      }        

      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
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
  ** Get the next key
  */

  public String getNextKey() {
    return (labels_.getNextLabel());
  }
  
  /***************************************************************************
  **
  ** Get the Vfg parent.  Will not be null.
  **
  */
  
  public GenomeInstance getVfgParent() {
    Database db = appState_.getDB();
    return ((GenomeInstance)db.getGenome(vfgParent_));
  }

 /***************************************************************************
  **
  ** Get the lowest static Vfg parent.  Will not be null.
  **
  */
  
  public GenomeInstance getStaticVfgParent() {
    Database db = appState_.getDB();
    String currParent = vfgParent_;
    while (true) {
      GenomeInstance gi = (GenomeInstance)db.getGenome(currParent);
      if (!isDynamicInstance(gi.getID())) {
        return (gi);
      }
      currParent = gi.getVfgParent().getID();
    }
  }  
 
  /***************************************************************************
  **
  ** Answers if the given STATIC instance is an ancestor
  **
  */
  
  public boolean instanceIsAncestor(GenomeInstance gi) {
    if (gi instanceof DynamicGenomeInstance) {
      throw new IllegalArgumentException();
    }
    //
    // Crank back up through proxies to the first static instance.
    // Then check.
    //

    String parent = vfgParent_;
    Database db = appState_.getDB();
    String staticParent = null;
    while (true) {
      if (isDynamicInstance(parent)) {
        DynamicInstanceProxy pdip = db.getDynamicProxy(extractProxyID(parent));
        parent = pdip.vfgParent_;
      } else {
        staticParent = vfgParent_;
        break;
      }
    }
    GenomeInstance gifirst = (GenomeInstance)db.getGenome(staticParent);
    return (gi.isAncestor(gifirst));
  }  
  
  /***************************************************************************
  **
  ** Answers if the given proxy is an ancestor
  **
  */
  
  public boolean proxyIsAncestor(String target) {
    if (id_.equals(target)) {
      return (true);
    }
    String parent = vfgParent_;
    Database db = appState_.getDB();
    while (true) {
      if (isDynamicInstance(parent)) {
        String pid = extractProxyID(parent);
        if (pid.equals(target)) {
          return (true);
        }
        DynamicInstanceProxy pdip = db.getDynamicProxy(pid);
        parent = pdip.vfgParent_;
      } else {
        return (false);
      }
    }
  }  

  /***************************************************************************
  **
  ** Get the proxy parent.  May be null.
  **
  */
  
  public DynamicInstanceProxy getProxyParent() {
    Database db = appState_.getDB();
    if (isDynamicInstance(vfgParent_)) {
      String pid = extractProxyID(vfgParent_);
      return (db.getDynamicProxy(pid));
    }
    return (null);
  }  
 
  /***************************************************************************
  **
  ** Answer if this is a single-time point proxy
  **
  */
  
  public boolean isSingle() {
    return (isSingle_);
  }  

  /***************************************************************************
  **
  ** Get the first genome instance key 
  **
  */
  
  public String getFirstProxiedKey() {  
    return (getProxiedKeys().get(0));
  }
   
  /***************************************************************************
  **
  ** Get a List of IDs for genome instances we are proxying.
  **
  */
  
  public List<String> getProxiedKeys() {
    ArrayList<String> retval = new ArrayList<String>();
    getProxiedTimes();
    GenomeInstance parent = getVfgParent();
    GenomeInstance root = parent.getVfgParentRoot();
    if (root == null) {
      root = parent;
    }
    String rootID = root.getID();
    Iterator<Integer> timit = getSortedTimes().iterator();
    StringBuffer buf = new StringBuffer();
    if (isSingle_) {
      buf.append("{DiP}@");      
      buf.append(rootID);
      buf.append("-");
      buf.append(id_);
      buf.append(":");
      buf.append("ALL");
      retval.add(buf.toString());
    } else {
      while (timit.hasNext()) {
        buf.setLength(0);
        Integer time = timit.next();
        buf.append("{DiP}@");      
        buf.append(rootID);
        buf.append("-");
        buf.append(id_);
        buf.append(":");
        buf.append(time);
        retval.add(buf.toString());
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the ID for the single genome instance
  **
  */
  
  public String getSingleKey(boolean forceConsistency) {
    GenomeInstance parent = getVfgParent();
    GenomeInstance root = parent.getVfgParentRoot();
    if (root == null) {
      root = parent;
    }
    String rootID = root.getID();
    StringBuffer buf = new StringBuffer();
    if (forceConsistency && !isSingle_) {
      throw new IllegalArgumentException();
    }
    buf.append("{DiP}@");      
    buf.append(rootID);
    buf.append("-");
    buf.append(id_);
    buf.append(":");
    buf.append("ALL");
    return (buf.toString());
  }    

  /***************************************************************************
  **
  ** Get the ID for the Genome instance for the given hour
  **
  */
  
  public String getKeyForTime(int time, boolean forceConsistency) {
    getProxiedTimes();
    GenomeInstance parent = getVfgParent();
    GenomeInstance root = parent.getVfgParentRoot();
    if (root == null) {
      root = parent;
    }
    String rootID = root.getID();
    StringBuffer buf = new StringBuffer();
    if (forceConsistency && isSingle_) {
      throw new IllegalArgumentException();
    }
    buf.setLength(0);
    buf.append("{DiP}@");      
    buf.append(rootID);
    buf.append("-");
    buf.append(id_);
    buf.append(":");
    buf.append(time);
    return (buf.toString());
  }  

  /***************************************************************************
  **
  ** Get the minimum time.  May be -1.
  */
  
  public int getMinimumTime() {
    return (min_);
  }
  
  /***************************************************************************
  **
  ** Get the maximum time.
  */
  
  public int getMaximumTime() {
    return (max_);
  }  
  
  /***************************************************************************
  **
  ** Get a List of times for genome instances we are proxying.
  */
  
  public List<Integer> getProxiedTimes() {
    Iterator<Integer> timit = getSortedTimes().iterator();
    ArrayList<Integer> retval = new ArrayList<Integer>();    
    while (timit.hasNext()) {
      retval.add(timit.next());
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Add a group to the proxy
  **
  */
  
  public void addGroup(Group group) {
    String id = group.getID();
    Iterator<Group> grit = groups_.iterator();
    while (grit.hasNext()) {
      Group g = grit.next();
      if (id.equals(g.getID())) {
        System.err.println("Proxy has group: " + g.getID());
        throw new IllegalArgumentException();
      }
    }
    groups_.add(group.copyForProxy());
    return;
  }

  /***************************************************************************
  **
  ** Activate a subgroup to the proxy.  Assumes already added 
  */
  
  public void activateSubGroup(Group parent, Group sub) {
       
    // change the parent
    // group to have an active subset:
    //
    
    String id = parent.getID();
    Iterator<Group> grit = groups_.iterator();
    while (grit.hasNext()) {
      Group g = grit.next();
      if (id.equals(g.getID())) {
        g.setActiveSubset(sub.getID());        
      }
    } 
    return;
  } 
  
  /***************************************************************************
  **
  ** Return all the group IDs that are subgroups
  */
 
  public Set<String> getAllSubsets() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Group> git = getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(this)) {
        retval.add(group.getID());
      }
    }
    return (retval);
  }  

  /***************************************************************************
  **
  ** Change the properties
  **
  */
  
  public ChangeBundle changeProperties(BTState appState, String name, boolean perTime, 
                                       int min, int max) {
    
    ChangeBundle retval = new ChangeBundle();
            
    retval.proxyChange = new ProxyChange();
    retval.proxyChange.proxyID = id_;
    
    retval.proxyChange.nameOld = name_;
    retval.proxyChange.hourlyOld = !isSingle_;
    retval.proxyChange.minOld = min_;
    retval.proxyChange.maxOld = max_;
    retval.proxyChange.oldImageKeys = new HashMap<Integer, String>(imageKeys_);
    
    retval.imageChanges = doImageChangesForPropertyChange(appState, perTime, min, max);
    retval.pathChanges = doPathChangesForPropertyChange(appState, perTime, min, max);
    retval.dc = doStartupViewChangesForPropertyChange(appState, perTime, min, max);
    name_ = name;
    isSingle_ = !perTime;
    min_ = min;
    max_ = max;
    
    retval.proxyChange.nameNew = name_;
    retval.proxyChange.hourlyNew = !isSingle_;
    retval.proxyChange.minNew = min_;
    retval.proxyChange.maxNew = max_;
    retval.proxyChange.newImageKeys = new HashMap<Integer, String>(imageKeys_);

    sortedTimes_ = null;
    return (retval);
  }
  

  /***************************************************************************
  **
  ** Handle image changes due to property change.
  **
  */
  
  public ImageChange[] doImageChangesForPropertyChange(BTState appState, boolean newPerTime, int newMin, int newMax) {  

    boolean newIsSingle = !newPerTime;
    ImageManager mgr = appState.getImageMgr();
    
    //
    // If the proxy stays for a single model, nothing is lost, though the
    // time key will need to change if the min value changes.
    // Going from single to multiple, we will just assign the image to the first
    // instance, so we get the same code:
    //
    
    if (isSingle_) {
      if (newMin != min_) {
        Iterator<Integer> ikit = imageKeys_.keySet().iterator();
        if (ikit.hasNext()) {
          Integer onlyKey = ikit.next();
          if (ikit.hasNext()) {
            throw new IllegalStateException();
          }
          String onlyImage = imageKeys_.get(onlyKey);
          imageKeys_.clear();
          imageKeys_.put(new Integer(newMin), onlyImage);
        }
      }
      return (new ImageChange[0]);
    } else if (!isSingle_ && newIsSingle) {
      //
      // Going multi to single, we drop all images:
      //
      ImageChange[] ics = dropGenomeImages();
      return ((ics == null) ? new ImageChange[0] : ics);
    } else if (!isSingle_ && !newIsSingle) {
      //
      // Going multi to multi, we drop lost hours:
      //
      HashSet<Integer> oldTimes = new HashSet<Integer>();
      for (int i = min_; i <= max_; i++) {
        oldTimes.add(new Integer(i));
      }
      HashSet<Integer> newTimes = new HashSet<Integer>();
      for (int i = newMin; i <= newMax; i++) {
        newTimes.add(new Integer(i));
      }      
      
      HashSet<Integer> lostKeys = new HashSet<Integer>(oldTimes);
      lostKeys.removeAll(newTimes);
      Iterator<Integer> lkit = lostKeys.iterator();
      ArrayList<ImageChange> changes = new ArrayList<ImageChange>();
      while (lkit.hasNext()) {
        Integer lostKey = lkit.next();
        String lostImage = imageKeys_.get(lostKey);
        if (lostImage != null) {
          ImageChange lostChange = mgr.dropImageUsage(lostImage);
          lostChange.timeKey = lostKey;
          lostChange.proxyKey = getID();
          changes.add(lostChange);
          imageKeys_.remove(lostKey);
        }
      }
      return (changes.toArray(new ImageChange[changes.size()]));
    }
    throw new IllegalStateException();
  }

  /***************************************************************************
  **
  ** Handle path changes due to property change.
  **
  */
  
  public UserTreePathChange[] doPathChangesForPropertyChange(BTState appState, boolean newPerTime, int newMin, int newMax) {  

    UserTreePathController utpc = appState.getPathController();
    boolean newIsSingle = !newPerTime;
    ArrayList<UserTreePathChange> retval = new ArrayList<UserTreePathChange>();

    if (isSingle_) {
      if (newIsSingle) {
        //
        // If the proxy stays for a single model, the instance key
        // is the same, so nothing happens:
        //
      } else {
        //
        // If the proxy goes to a multiple model, we keep the stop
        // on the first model.  This means a key change for the 
        // path manager.
        //
        String oldModelKey = getFirstProxiedKey();
        String newMinKey = getKeyForTime(newMin, false);
        UserTreePathChange[] chgs = utpc.replaceStopsOnModel(oldModelKey, newMinKey);
        retval.addAll(Arrays.asList(chgs));
      }
    } else { // (!isSingle_)
      if (newIsSingle) {
        //
        // Going multi to single, we drop all path visits:
        //
        List<String> allKeys = getProxiedKeys();
        Iterator<String> lkit = allKeys.iterator();
        while (lkit.hasNext()) {
          String keyToDrop = lkit.next();
          UserTreePathChange[] chgs = utpc.dropStopsOnModel(keyToDrop);
          retval.addAll(Arrays.asList(chgs));
        }
      } else { //!newIsSingle
        //
        // Going multi to multi, we drop lost hours:
        //
        HashSet<Integer> oldTimes = new HashSet<Integer>();
        for (int i = min_; i <= max_; i++) {
          oldTimes.add(new Integer(i));
        }
        HashSet<Integer> newTimes = new HashSet<Integer>();
        for (int i = newMin; i <= newMax; i++) {
          newTimes.add(new Integer(i));
        }      

        HashSet<Integer> lostKeys = new HashSet<Integer>(oldTimes);
        lostKeys.removeAll(newTimes);
        Iterator<Integer> lkit = lostKeys.iterator();
        while (lkit.hasNext()) {
          Integer lostKey = lkit.next();        
          String keyToDrop = getKeyForTime(lostKey.intValue(), true);
          UserTreePathChange[] chgs = utpc.dropStopsOnModel(keyToDrop);
          retval.addAll(Arrays.asList(chgs));
        }
      }
    }
    return (retval.toArray(new UserTreePathChange[retval.size()]));
  }

  /***************************************************************************
  **
  ** Drop the startup view on a property change, if needed
  **
  */  
    
  public DatabaseChange doStartupViewChangesForPropertyChange(BTState appState, boolean newPerTime, int newMin, int newMax) {    
    Database db = appState.getDB();
    StartupView sView = db.getStartupView();
    if (sView == null) {
      return (null);
    }
    String modelId = sView.getModel();
    if (modelId == null) {
      return (null);
    }
    
    boolean newIsSingle = !newPerTime;
    
    if (isSingle_) {
      if (newIsSingle) {
        //
        // If the proxy stays for a single model, the instance key
        // is the same, so nothing happens:
        //
        return (null);
      } else {
        //
        // If the proxy goes to a multiple model, we keep the startup
        // on the first model.
        //
        
        String oldModelKey = getFirstProxiedKey();
        if (oldModelKey.equals(modelId)) {
          String newMinKey = getKeyForTime(newMin, false);
          return (db.setStartupView(new StartupView(newMinKey, null, null, null)));
        }
      }
    } else { // (!isSingle_)
      if (newIsSingle) {
        //
        // Going multi to single, we change the startup to the single:
        //
        List<String> allKeys = getProxiedKeys();
        Iterator<String> lkit = allKeys.iterator();
        while (lkit.hasNext()) {
          String oldModelKey = lkit.next();        
          if (oldModelKey.equals(modelId)) {
            String newKey = getSingleKey(false);
            return (db.setStartupView(new StartupView(newKey, null, null, null)));
          } 
        }
      } else { //!newIsSingle
        //
        // Going multi to multi, if the startup hour is lost, we
        // switch to the first hour:
        //
        HashSet<Integer> oldTimes = new HashSet<Integer>();
        for (int i = min_; i <= max_; i++) {
          oldTimes.add(new Integer(i));
        }
        HashSet<Integer> newTimes = new HashSet<Integer>();
        for (int i = newMin; i <= newMax; i++) {
          newTimes.add(new Integer(i));
        }      

        HashSet<Integer> lostKeys = new HashSet<Integer>(oldTimes);
        lostKeys.removeAll(newTimes);
        Iterator<Integer> lkit = lostKeys.iterator();
        while (lkit.hasNext()) {
          Integer lostKey = lkit.next();        
          String oldModelKey = getKeyForTime(lostKey.intValue(), true);
          if (oldModelKey.equals(modelId)) {
            String newKey = getKeyForTime(newMin, true);
            return (db.setStartupView(new StartupView(newKey, null, null, null)));
          } 
        }
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Set the genome image
  **
  */
  
  public Integer setGenomeImage(String newKey, String instanceID) {
    Integer retval;
    if (isSingle_) {
      retval = new Integer(min_);
      if (newKey != null) {
        imageKeys_.put(retval, newKey);
      } else {
        imageKeys_.remove(retval);
      }
    } else {
      DynamicGenomeInstance pi = getProxiedInstance(instanceID);
      retval = pi.getTime();
      if (newKey != null) {
        imageKeys_.put(retval, newKey);
      } else {
        imageKeys_.remove(retval);
      } 
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Drop the genome image
  **
  */
  
  public Integer dropGenomeImage(String instanceID) {
    Integer retval;
    if (isSingle_) {
      retval = new Integer(min_);
      imageKeys_.clear();
    } else {
      DynamicGenomeInstance pi = getProxiedInstance(instanceID);
      retval = pi.getTime();
      imageKeys_.remove(retval);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Set the genome image for a time.  Null key used for deletion.
  **
  */
  
  private void setGenomeImageForTime(String newKey, Integer time) {
    if (newKey != null) {
      imageKeys_.put(time, newKey);
    } else {
      imageKeys_.remove(time);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Answers if we have genome images
  **
  */
  
  public boolean hasGenomeImage() {
    return (!imageKeys_.isEmpty());
  }  

  /***************************************************************************
  **
  ** Undo an image change
  */
  
  public void imageChangeUndo(ImageChange undo) {
    if (undo.timeKey == null) {
      return;
    }
    if (undo.countOnlyKey != null) {
      // FIX FOR BT-10-25-07:1 ??
      String cok = (undo.newCount > undo.oldCount) ? null : undo.countOnlyKey;
      setGenomeImageForTime(cok, undo.timeKey);
    } else if (undo.newKey != null) {
      setGenomeImageForTime(null, undo.timeKey);
    } else if (undo.oldKey != null) {
      setGenomeImageForTime(undo.oldKey, undo.timeKey);
    }
    //
    // If we are not called via a proxied instance, we need to handle
    // manager changes:
    //
    
    if (undo.proxyKey != null) {
      ImageManager mgr = appState_.getImageMgr();
      mgr.changeUndo(undo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an image change
  */
  
  public void imageChangeRedo(ImageChange redo) {
    if (redo.timeKey == null) {
      return;
    }
    if (redo.countOnlyKey != null) {
      // FIX FOR BT-10-25-07:1 ??
      String cok = (redo.oldCount > redo.newCount) ? null : redo.countOnlyKey;
      setGenomeImageForTime(cok, redo.timeKey);
    } else if (redo.newKey != null) {
      setGenomeImageForTime(redo.newKey, redo.timeKey);
    } else if (redo.oldKey != null) {
      setGenomeImageForTime(null, redo.timeKey);
    }
    //
    // If we are not called via out proxied instance, we need to handle
    // manager changes:
    //
    
    if (redo.proxyKey != null) {
      ImageManager mgr = appState_.getImageMgr();
      mgr.changeRedo(redo);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drop all genome images
  */

  public ImageChange[] dropGenomeImages() {
    ArrayList<ImageChange> allChanges = new ArrayList<ImageChange>();
    ImageManager imgr = appState_.getImageMgr();
    Iterator<Integer> ikit = (new HashSet<Integer>(imageKeys_.keySet())).iterator();
    while (ikit.hasNext()) {
      Integer timeKey = ikit.next();
      String imgKey = imageKeys_.get(timeKey);
      imageKeys_.remove(timeKey);
      ImageChange ic = imgr.dropImageUsage(imgKey);
      ic.timeKey = timeKey;
      ic.proxyKey = getID();
      allChanges.add(ic);
    }
    int changeCount = allChanges.size();
    if (changeCount == 0) {
      return (null);
    }
    ImageChange[] retval = new ImageChange[changeCount];
    allChanges.toArray(retval);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(ProxyChange undo) {
    if (undo.moduleChanges != null) {
      for (int i = 0; i < undo.moduleChanges.length; i++) {
        NetworkOverlay no = getNetworkOverlay(undo.moduleChanges[i].overlayKey);
        NetModule nm = no.getModule(undo.moduleChanges[i].moduleKey);
        nm.changeUndo(undo.moduleChanges[i]);
      }
    }    

    if (undo.removedGroup != null) {
      groups_.add(undo.removedGroup);
    } else if ((undo.addedNew == null) && (undo.addedOld == null)) {
      name_ = undo.nameOld;
      isSingle_ = !undo.hourlyOld;
      min_ = undo.minOld;
      max_ = undo.maxOld;
      imageKeys_ = new HashMap<Integer, String>(undo.oldImageKeys);
      sortedTimes_ = null;
    } else {
      if ((undo.addedOld != null) && (undo.addedNew == null)) {
        addedNodes_.add(undo.addedOld);
      } else if (undo.addedOld != null) {
        for (int i = 0; i < addedNodes_.size(); i++) {
          AddedNode an = addedNodes_.get(i);
          if (an.nodeName.equals(undo.addedOld.nodeName)) {
            addedNodes_.set(i, undo.addedOld);
            break;
          }
        }
      } else if ((undo.addedOld == null) && (undo.addedNew != null)) {
        for (int i = 0; i < addedNodes_.size(); i++) {
          AddedNode an = addedNodes_.get(i);       
          if (an.nodeName.equals(undo.addedNew.nodeName)) {
            addedNodes_.remove(i);
            break;
          }
        }
      }    
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(ProxyChange undo) {

        
    if (undo.removedGroup != null) {
      Iterator<Group> git = groups_.iterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (undo.removedGroup.getID().equals(group.getID())) {
          groups_.remove(group);
          break;
        }
      }
    } else if ((undo.addedNew == null) && (undo.addedOld == null)) {    
      name_ = undo.nameNew;
      isSingle_ = !undo.hourlyNew;
      min_ = undo.minNew;
      max_ = undo.maxNew;
      imageKeys_ = new HashMap<Integer, String>(undo.newImageKeys);
      sortedTimes_ = null;
    } else {
      if ((undo.addedNew != null) && (undo.addedOld == null)) {
        addedNodes_.add(undo.addedNew);
      } else if ((undo.addedNew != null) && (undo.addedOld != null)) {
        for (int i = 0; i < addedNodes_.size(); i++) {
          AddedNode an = addedNodes_.get(i);
          if (an.nodeName.equals(undo.addedNew.nodeName)) {
            addedNodes_.set(i, undo.addedNew);
            break;
          }
        }        
      } else if ((undo.addedNew == null) && (undo.addedOld != null)) {
        for (int i = 0; i < addedNodes_.size(); i++) {
          AddedNode an = addedNodes_.get(i);
          if (an.nodeName.equals(undo.addedOld.nodeName)) {
            addedNodes_.remove(i);
            break;
          }
        }
      }  
    }
    
    if (undo.moduleChanges != null) {
      for (int i = 0; i < undo.moduleChanges.length; i++) {
        NetworkOverlay no = getNetworkOverlay(undo.moduleChanges[i].overlayKey);
        NetModule nm = no.getModule(undo.moduleChanges[i].moduleKey);
        nm.changeRedo(undo.moduleChanges[i]);
      }
    }    

    return;
  }
  
  /***************************************************************************
  **
  ** Add a note to the proxy
  **
  */
  
  public void addNote(Note note) {
    String id = note.getID();
    Iterator<Note> nit = notes_.iterator();
    while (nit.hasNext()) {
      Note n = nit.next();
      if (id.equals(n.getID())) {
        throw new IllegalArgumentException();
      }
    }    
    if (!labels_.addExistingLabel(id)) {
      System.err.println("Don't like " + id);
      throw new IllegalArgumentException();
    }        
    notes_.add(note);  // Don't make copy; text is still coming in from XML  
    return;
  }

  /***************************************************************************
  **
  ** Add a note to the proxy
  **
  */
  
  public void addNoteWithExistingLabel(Note note) {
    String id = note.getID();
    Iterator<Note> nit = notes_.iterator();
    while (nit.hasNext()) {
      Note n = nit.next();
      if (id.equals(n.getID())) {
        throw new IllegalArgumentException();
      }
    }    
    notes_.add(new Note(note));
    return;
  }  

  
  /***************************************************************************
  **
  ** Change the note
  **
  */
  
  public void changeNote(Note oldNote, Note newNote) {
    Iterator<Note> nit = notes_.iterator();
    while (nit.hasNext()) {
      Note note = nit.next();
      if (note.getID().equals(oldNote.getID())) {
        notes_.remove(note);
        break;
      }
    }
    notes_.add(new Note(newNote));
    return;
  }

  /***************************************************************************
  **
  ** Change the note
  **
  */
  
  public void removeNote(String key) {
    Iterator<Note> nit = notes_.iterator();
    while (nit.hasNext()) {
      Note note = nit.next();
      if (note.getID().equals(key)) {
        notes_.remove(note);
        break;
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Undo a note change
  */
  
  public void undoNoteChange(GenomeChange undo) {
    if ((undo.ntOrig != null) && (undo.ntNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<Note> nit = notes_.iterator();
      while (nit.hasNext()) {
        Note note = nit.next();
        if (note.getID().equals(undo.ntNew.getID())) {
          notes_.remove(note);
          break;
        }
      }      
      notes_.add(new Note(undo.ntOrig));
    } else if (undo.ntOrig == null) {
      Iterator<Note> nit = notes_.iterator();
      while (nit.hasNext()) {
        Note note = nit.next();
        if (note.getID().equals(undo.ntNew.getID())) {
          notes_.remove(note);
          break;
        }
      }
    } else {
      notes_.add(new Note(undo.ntOrig));
    }    
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a note change
  */
  
  public void redoNoteChange(GenomeChange undo) {
    if ((undo.ntOrig != null) && (undo.ntNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<Note> nit = notes_.iterator();
      while (nit.hasNext()) {
        Note note = nit.next();
        if (note.getID().equals(undo.ntOrig.getID())) {
          notes_.remove(note);
          break;
        }
      }
      notes_.add(new Note(undo.ntNew));
    } else if (undo.ntOrig == null) {
      notes_.add(new Note(undo.ntNew));
    } else {
      Iterator<Note> nit = notes_.iterator();
      while (nit.hasNext()) {
        Note note = nit.next();
        if (note.getID().equals(undo.ntOrig.getID())) {
          notes_.remove(note);
          break;
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Remove the group
  **
  */
  
  public void removeGroup(String key) {
    Iterator<Group> git = groups_.iterator();
    Group match = null;
    while (git.hasNext()) {
      Group group = git.next();
      if (group.getID().equals(key)) {
        groups_.remove(group);
        match = group;
        break;
      }
    }
    if (match == null) {
      throw new IllegalArgumentException();
    }
    Set<String> subs = match.getSubsets(this);
    Iterator<String> subit = subs.iterator();
    while (subit.hasNext()) {
      String subID = subit.next();
      Iterator<Group> grit = groups_.iterator();
      while (grit.hasNext()) {
        Group group = grit.next();
        if (group.getID().equals(subID)) {
          groups_.remove(group);
          break;
        }
      }
    }
    
    return;
  }
   
  /***************************************************************************
  **
  ** Remove the group; used for group deletions during builds from instructions.
  ** Standard method of group deletion (via a dynamic instance) does not work
  ** because all parent references to these groups are gone.  We don't worry
  ** about subgroup issues because they are thrown out before instructions are
  ** applied.
  */
  
  public ProxyChange removeGroupNoChecks(String key) {
    Iterator<Group> git = groups_.iterator();
    Group match = null;
    while (git.hasNext()) {
      Group group = git.next();
      if (group.getID().equals(key)) {
        groups_.remove(group);
        match = group;
        break;
      }
    }
    ProxyChange retval = new ProxyChange();
    retval.proxyID = id_;
    retval.removedGroup = match;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Remove the group
  **
  */
  
  public void removeSubGroup(String key) {
    //
    // If there is a parent group that has us as an active
    // subset, it needs to drop that reference.
    //
    Iterator<Group> git = groups_.iterator();
    while (git.hasNext()) {
      Group grpChk = git.next();
      String active = grpChk.getActiveSubset();
      if ((active != null) && active.equals(key)) {
        grpChk.setActiveSubset(null);
        break;
      }
    }

    git = groups_.iterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.getID().equals(key)) {
        if (!group.isASubset(getVfgParent())) {
          throw new IllegalArgumentException();
        }
        groups_.remove(group);
        return;
      }
    }
    throw new IllegalArgumentException();
  }

  /***************************************************************************
  **
  ** Undo a group change
  */
  
  public void undoGroupChange(GenomeChange undo) {
    if ((undo.grOrig != null) && (undo.grNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<Group> git = groups_.iterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.getID().equals(undo.grNew.getID())) {
          groups_.remove(group);
          break;
        }
      }
      groups_.add(undo.grOrig.copyForProxy());
    } else if (undo.grOrig == null) {
      Iterator<Group> git = groups_.iterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.getID().equals(undo.grNew.getID())) {
          groups_.remove(group);
          break;
        }
      }
    } else {
      groups_.add(undo.grOrig.copyForProxy());
    }    
    return;
  }
    
  /***************************************************************************
  **
  ** Redo a group change
  */
  
  public void redoGroupChange(GenomeChange undo) {
    if ((undo.grOrig != null) && (undo.grNew != null)) {  // FIX ME??  DOES THIS EVER HAPPEN?
      Iterator<Group> git = groups_.iterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.getID().equals(undo.grOrig.getID())) {
          groups_.remove(group);
          break;
        }
      }
      groups_.add(undo.grNew.copyForProxy());
    } else if (undo.grOrig == null) {
      groups_.add(undo.grNew.copyForProxy());
    } else {
      Iterator<Group> git = groups_.iterator();
      while (git.hasNext()) {
        Group group = git.next();
        if (group.getID().equals(undo.grOrig.getID())) {
          groups_.remove(group);
          break;
        }
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Add an extra node
  **
  */
  
  public ProxyChange addExtraNode(AddedNode added) {
    Iterator<AddedNode> anit = addedNodes_.iterator();
    while (anit.hasNext()) {
      AddedNode an = anit.next();
      if (added.nodeName.equals(an.nodeName)) {
        throw new IllegalArgumentException();
      }
    }
    addedNodes_.add(new AddedNode(added));
    ProxyChange retval = new ProxyChange();
    retval.proxyID = id_;
    retval.addedNew = new AddedNode(added);
    retval.addedOld = null;
    return (retval);
  }

  /***************************************************************************
  **
  ** Delete an extra node
  **
  */
  
  public ProxyChange deleteExtraNode(String nodeID) {
        
    ProxyChange retval = new ProxyChange();
    retval.proxyID = id_; 
    retval.addedNew = null;    
    for (int i = 0; i < addedNodes_.size(); i++) {
      AddedNode an = addedNodes_.get(i);
      if (an.nodeName.equals(nodeID)) {
        retval.addedOld = new AddedNode(an);
        addedNodes_.remove(i);
        break;
      }
    }
    retval.moduleChanges = dropModuleMembersForExtraNode(nodeID);
    return (retval);
  }

  /***************************************************************************
  **
  ** Delete illegal extra nodes
  */
  
  public ProxyChange[] deleteIllegalExtraNodes() {
    //
    // Following a change in group status for a proxy, some extra nodes may
    // no longer be legit, i.e. they may belong to one of the included or
    // active groups.  If we detect a clash, ditch the extra nodes. 
    //
   
    //
    // If an added node now turns out to belong to one of our groups, then it
    // can be ditched:
    //
    
    GenomeInstance parent = getVfgParent();
    int generation = parent.getGeneration();    
    ArrayList<String> deadList = new ArrayList<String>();
    for (int i = 0; i < addedNodes_.size(); i++) {
      AddedNode an = addedNodes_.get(i);
      // FIX ME: Maybe something other than LEGACY_MODE is desired?
      Group nodeGroup = parent.getGroupForNode(an.nodeName, GenomeInstance.LEGACY_MODE);
      if (nodeGroup != null) { 
        for (int j = 0; j < groups_.size(); j++) {
          Group g = groups_.get(j);
          String activeID = g.getActiveSubset();
          if (activeID == null) { // actual group is in instance       
            String myID = Group.buildInheritedID(g.getID(), generation);
            if (myID.equals(nodeGroup.getID())) {
              deadList.add(an.nodeName);
            }
          } 
        }
      }
    }
    
    ArrayList<ProxyChange> retvalList = new ArrayList<ProxyChange>();
    for (int i = 0; i < deadList.size(); i++) {
      String nodeID = deadList.get(i);
      retvalList.add(deleteExtraNode(nodeID));
    }    
    return (retvalList.toArray(new ProxyChange[retvalList.size()]));
  }
  
  /***************************************************************************
  **
  ** Delete orphaned extra nodes.  Does not work on top level dynamic proxies!
  */
  
  public ProxyChange[] deleteOrphanedExtraNodes() {
    //
    // Following a change in group status for a proxy, some extra nodes may
    // no longer be legit, i.e. they may belong to one of the included or
    // active groups.  If we detect a clash, ditch the extra nodes. 
    //
    
    //
    // To have an extra node in the kid, it must be an extra node in the
    // parent too.  If not there anymore, throw it away:
    //
    
    DynamicInstanceProxy dip = getProxyParent();
    if (dip == null) {
      return (new ProxyChange[0]);
    }
    ArrayList<String> deadList = new ArrayList<String>();
    for (int i = 0; i < addedNodes_.size(); i++) {
      AddedNode an = addedNodes_.get(i);
      if (!dip.hasAddedNode(an.nodeName)) {
        deadList.add(an.nodeName);
      }
    }
 
    ArrayList<ProxyChange> retvalList = new ArrayList<ProxyChange>();
    for (int i = 0; i < deadList.size(); i++) {
      String nodeID = deadList.get(i);
      retvalList.add(deleteExtraNode(nodeID));
    }    
    return (retvalList.toArray(new ProxyChange[retvalList.size()]));
  }  

  /***************************************************************************
  **
  ** Delete any extra nodes associated with the given group
  */
  
  public ProxyChange[] deleteExtraNodesForGroup(String groupID) {
    ArrayList<String> deadList = new ArrayList<String>();
    for (int i = 0; i < addedNodes_.size(); i++) {
      AddedNode an = addedNodes_.get(i);
      if (an.groupToUse.equals(groupID)) {
        deadList.add(an.nodeName);
      }
    }

    ArrayList<ProxyChange> retvalList = new ArrayList<ProxyChange>();
    for (int i = 0; i < deadList.size(); i++) {
      String nodeID = deadList.get(i);
      retvalList.add(deleteExtraNode(nodeID));
    }    
    return (retvalList.toArray(new ProxyChange[retvalList.size()]));
  }  

  /***************************************************************************
  **
  ** change the group binding of an added node
  **
  */
  
  public ProxyChange changeExtraNodeGroupBinding(String nodeID, String groupID) {
    ProxyChange retval = new ProxyChange();
    retval.proxyID = id_; 
    for (int i = 0; i < addedNodes_.size(); i++) {
      AddedNode an = addedNodes_.get(i);
      if (an.nodeName.equals(nodeID)) {
        retval.addedOld = new AddedNode(an);
        an.groupToUse = groupID;
        retval.addedNew = new AddedNode(an);
        break;
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** After changes in group status, module members (which are only supposed to
  ** reference nodes that are in the groups, plus extra nodes) need to be
  ** kept consistent with that state.  Do that here.  NOTE: Gotta be called
  ** AFTER group or extra node is deleted!
  */  
  
  public NetModuleChange[] adjustDynamicGroupModuleMembers(DynamicGenomeInstance dgi) {  
       
    ArrayList<NetModuleChange> preRetval = new ArrayList<NetModuleChange>();
    int numgrp = groups_.size();
    HashSet<String> deadMembers = new HashSet<String>();
    Iterator<NetworkOverlay> oit = getNetworkOverlayIterator();
    while (oit.hasNext()) {
      NetworkOverlay no = oit.next();
      String noID = no.getID();
      Iterator<NetModule> nmit = no.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        deadMembers.clear();
        Iterator<NetModuleMember> mit = nmod.getMemberIterator();
        while (mit.hasNext()) {
          NetModuleMember nmm = mit.next();
          String nmID = nmm.getID();
          boolean inAGroup = false;
          boolean isExtra = false;
          for (int i = 0; i < numgrp; i++) {
            Group grp = groups_.get(i);
            // If the group has an active subset, _that_ is what is controlling membership!
            if (grp.getActiveSubset() != null) {
              continue;
            }
            if (grp.isInGroup(nmID, dgi)) {
              inAGroup = true;
              break;            
            }
          }
          if (!inAGroup) {
            for (int i = 0; i < addedNodes_.size(); i++) {
              AddedNode an = addedNodes_.get(i);
              if (an.nodeName.equals(nmID)) {
                isExtra = true;
              }
            }
          }         
          if ((!inAGroup) && (!isExtra)) {
            deadMembers.add(nmID);
          }
        }
        Iterator<String> dit = deadMembers.iterator();
        while (dit.hasNext()) {
          String memID = dit.next();
          NetModuleChange nmc = deleteMemberFromNetworkModule(noID, nmod, memID);
          if (nmc != null) {
            preRetval.add(nmc);
          }
        }
      }
    }
    return (preRetval.toArray(new NetModuleChange[preRetval.size()]));
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the groups
  **
  */
  
  public Iterator<Group> getGroupIterator() {
    return (groups_.iterator());
  }
  
  
  /***************************************************************************
  **
  ** Needed by NetOverlayOwner interface
  **
  */
  
  public Set<String> getGroupsForOverlayRendering() {
    HashSet<String> retval = new HashSet<String>();
    int numgrp = groups_.size();
    for (int i = 0; i < numgrp; i++) {
      Group grp = groups_.get(i);
      retval.add(grp.getID());
    }
    return (retval);
  }   
  
  /***************************************************************************
  **
  ** Get an iterator over the notes
  **
  */
  
  public Iterator<Note> getNoteIterator() {
    return (notes_.iterator());
  }  

  /***************************************************************************
  **
  ** Get an iterator over the added nodes
  */
  
  public Iterator<AddedNode> getAddedNodeIterator() {
    return (addedNodes_.iterator());
  }
  
  /***************************************************************************
  **
  ** Answer if we have added nodes
  */
  
  public boolean hasAddedNodes() {
    return (addedNodes_.size() > 0);
  }  

  /***************************************************************************
  **
  ** Get the added node
  */
  
  public AddedNode getAddedNode(String nodeID) {
    Iterator<AddedNode> anit = getAddedNodeIterator();
    while (anit.hasNext()) {
      AddedNode an = anit.next();
      if (an.nodeName.equals(nodeID)) {
        return (an);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Answer if added node is already present
  */
  
  public boolean hasAddedNode(String nodeID) {
    Iterator<AddedNode> anit = getAddedNodeIterator();
    while (anit.hasNext()) {
      AddedNode an = anit.next();
      if (an.nodeName.equals(nodeID)) {
        return (true);
      }
    }
    return (false);
  }  
  
  /***************************************************************************
  **
  ** Get group for an added node
  */
  
  public String getGroupForExtraNode(String nodeID) {
    Iterator<AddedNode> anit = getAddedNodeIterator();
    while (anit.hasNext()) {
      AddedNode an = anit.next();
      if (an.nodeName.equals(nodeID)) {
        return (an.groupToUse);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Clear all DynamicGenomeInstances from the cache
  */
  
  public void clearCache() {
    cache_.clear();
    return;
  }

  
  /***************************************************************************
  **
  ** Return some DynamicGenomeInstance
  */
  
  public DynamicGenomeInstance getAnInstance() {
    String dgiKey = (isSingle()) ? (String)getProxiedKeys().iterator().next() 
                                           : getKeyForTime(getMinimumTime(), true);
    return (getProxiedInstance(dgiKey));
  }
  
  /***************************************************************************
  **
  ** Return a DynamicGenomeInstance for the given key.
  */
  
  public DynamicGenomeInstance getProxiedInstance(String key) {
    //
    // See if it is in our cache:
    //
    DynamicGenomeInstance retval = cache_.get(key);
    if (retval != null) {
      return (retval);
    }
    GenomeInstance parent = getVfgParent();  
    //
    // Figure out from the key what time this is for, and build it
    // accordingly.
    //
    String ti = key.substring(key.lastIndexOf(":") + 1);
    if (ti.equals("ALL")) {
      Iterator<Integer> imgKit = imageKeys_.keySet().iterator();
      String imgKey = (imgKit.hasNext()) ? imageKeys_.get(imgKit.next()) : null;
      retval = new DynamicGenomeInstance(appState_, name_, key, parent, id_, imgKey);
      retval.setTime(getSortedTimes());
    } else {
      int time;
      try {
        time = Integer.parseInt(ti);
      } catch (NumberFormatException nfex) {
        System.err.println(key + " " + ti);      
        throw new IllegalArgumentException();
      }
      TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
      boolean namedStages = tad.haveNamedStages();
      String displayUnits = tad.unitDisplayString(); 
      String stageName = (namedStages) ? tad.getNamedStageForIndex(time).name : Integer.toString(time);
      String format = (tad.unitsAreASuffix()) ? "dgi.titleFormat" : "dgi.titleFormatPrefix";
      String dgiTitle = MessageFormat.format(appState_.getRMan().getString(format), 
                                             new Object[] {name_, stageName, displayUnits});
      String imgKey = imageKeys_.get(new Integer(time));                        
      retval = new DynamicGenomeInstance(appState_, dgiTitle, key, parent, id_, imgKey);
      HashSet<Integer> singleton = new HashSet<Integer>();
      singleton.add(new Integer(time));
      retval.setTime(singleton);
    }
    cache_.put(key, retval);
    return (retval);
  } 

  /***************************************************************************
  **
  ** Get the name of a proxied instance
  */
  
  public String getProxiedInstanceName(String key) {
    //
    // Figure out from the key what time this is for, and build the name
    // accordingly.
    //
    String hr = key.substring(key.lastIndexOf(":") + 1);
    if (hr.equals("ALL")) {
      return (name_);
    } else {
      int time;
      try {
        time = Integer.parseInt(hr);
      } catch (NumberFormatException nfex) {
        System.err.println(key + " " + hr);
        throw new IllegalArgumentException();
      }
      TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
      boolean namedStages = tad.haveNamedStages();
      String displayUnits = tad.unitDisplayString(); 
      String stageName = (namedStages) ? tad.getNamedStageForIndex(time).name : Integer.toString(time);
      String format = (tad.unitsAreASuffix()) ? "dgi.nameFormat" : "dgi.nameFormatPrefix";
      String dgiName = MessageFormat.format(appState_.getRMan().getString(format), 
                                             new Object[] {name_, stageName, displayUnits});            
      return (dgiName);
    }
  }  

  /***************************************************************************
  **
  ** Return the proxy name
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Write the DynamicGenomeInstance to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<dynamicProxy ");
    out.print("name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    if (min_ != -1) {
      out.print("\" minHour=\"");
      out.print(min_);
    }
    if (max_ != -1) {
      out.print("\" maxHour=\"");
      out.print(max_);
    }
    if (isSingle_) {
      out.print("\" isSingle=\"true");
    }    
    out.print("\" id=\"");
    out.print(id_);
    out.print("\" vfgParent=\"");
    out.print(vfgParent_);
    out.println("\" >");
    ind.up();
    writeGroups(out, ind);
    writeNotes(out, ind);
    writeImages(out, ind);
    writeAddedNodes(out, ind);
    ovrops_.writeOverlaysToXML(out, ind);    
    ind.down().indent();
    out.println("</dynamicProxy>");    
    return;
  }
  
  /***************************************************************************
  **
  ** Add image using attributes
  **
  */
  
  public void addImage(String elemName, Attributes attrs) throws IOException {
    String imgKey = AttributeExtractor.extractAttribute(elemName, attrs, "dpImage", "img", true);
    Integer time;
    if (!isSingle_) {
      String timeStr = AttributeExtractor.extractAttribute(elemName, attrs, "dpImage", "time", true); 
      try {
        time = Integer.valueOf(timeStr);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
    } else {
      time = new Integer(min_);
    }
    imageKeys_.put(time, imgKey);    
    return;
  } 

  /***************************************************************************
  **
  ** Get the FullKeys for all the modules attached to the given group
  ** for the given group
  */  
  
  public void getModulesAttachedToGroup(String grpID, Set<NetModule.FullModuleKey> attached) {
    ovrops_.getModulesAttachedToGroup(grpID, attached);
    return;
  }  
    
  /***************************************************************************
  **
  ** Get the overlay map (for use by dynamic instances only!)
  **
  */
  
  Map<String, NetworkOverlay> getNetworkOverlayMap() {
    return (ovrops_.getNetworkOverlayMap());
  }  
   
  /***************************************************************************
  **
  ** Add a network module view to the genome
  **
  */
  
  public NetworkOverlayOwnerChange addNetworkOverlay(NetworkOverlay nmView) {
    return (ovrops_.addNetworkOverlay(nmView));
  }
  
  /***************************************************************************
  **
  ** Add a network module view to the genome.  Use for IO
  **
  */
  
  public void addNetworkOverlayAndKey(NetworkOverlay nmView) throws IOException {
    ovrops_.addNetworkOverlayAndKey(nmView);
    return;
  } 

  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome.  Use for IO
  **
  */
  
  public void addNetworkModuleAndKey(String overlayKey, NetModule module) throws IOException {
    ovrops_.addNetworkModuleAndKey(overlayKey, module);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.  Use for IO
  **
  */
  
  public void addNetworkModuleLinkageAndKey(String overlayKey, NetModuleLinkage linkage) throws IOException {
    ovrops_.addNetworkModuleLinkageAndKey(overlayKey, linkage);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.
  **
  */
  
  public NetworkOverlayChange addNetworkModuleLinkage(String overlayKey, NetModuleLinkage linkage) {
    return (ovrops_.addNetworkModuleLinkage(overlayKey, linkage));
  }
  
  /***************************************************************************
  **
  ** Remove a network module linkage from an overlay in this owner.
  **
  */
  
  public NetworkOverlayChange removeNetworkModuleLinkage(String overlayKey, String linkageKey) {  
    return (ovrops_.removeNetworkModuleLinkage(overlayKey, linkageKey));
  }  
 
  /***************************************************************************
  **
  ** Modify a network module linkage in an overlay in this genome.
  **
  */
 
  public NetworkOverlayChange modifyNetModuleLinkage(String overlayKey, String linkKey, int newSign) {  
    return (ovrops_.modifyNetModuleLinkage(overlayKey, linkKey, newSign));
  }    
  
  /***************************************************************************
  **
  ** Remove the network module view from the genome
  **
  */
  
  public NetworkOverlayOwnerChange removeNetworkOverlay(String key) {
    return (ovrops_.removeNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get a network overlay from the genome
  **
  */
  
  public NetworkOverlay getNetworkOverlay(String key) {
    return (ovrops_.getNetworkOverlay(key));
  }
  
  /***************************************************************************
  **
  ** Get an iterator over network overlays
  **
  */
  
  public Iterator<NetworkOverlay> getNetworkOverlayIterator() {
    return (ovrops_.getNetworkOverlayIterator());
  }  
  
  /***************************************************************************
  **
  ** Get the count of network overlays
  **
  */
  
  public int getNetworkOverlayCount() {
    return (ovrops_.getNetworkOverlayCount());
  }
  
  /***************************************************************************
  **
  ** Get the count of network modules
  **
  */
  
  public int getNetworkModuleCount() {
    return (ovrops_.getNetworkModuleCount());
  }      
   
  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome
  **
  */
  
  public NetworkOverlayChange addNetworkModule(String overlayKey, NetModule module) {
    return (ovrops_.addNetworkModule(overlayKey, module));
  }
  
  /***************************************************************************
  **
  ** Remove a network module from an overlay in this genome
  **
  */
  
  public NetworkOverlayChange[] removeNetworkModule(String overlayKey, String moduleKey) {  
    return (ovrops_.removeNetworkModule(overlayKey, moduleKey));    
  }
 
  /***************************************************************************
  **
  ** Add a new member to a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange addMemberToNetworkModule(String overlayKey, NetModule module, String nodeID) {
    return (ovrops_.addMemberToNetworkModule(overlayKey, module, nodeID));
  } 
  
  /***************************************************************************
  **
  ** Remove a member from a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange deleteMemberFromNetworkModule(String overlayKey, NetModule module, String nodeID){
    return (ovrops_.deleteMemberFromNetworkModule(overlayKey, module, nodeID));
  } 

  /***************************************************************************
  **
  ** Return matching Network Modules (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
  
  public Map<String, Set<String>> findMatchingNetworkModules(int searchMode, String key, NameValuePair nvPair) {
    return (ovrops_.findMatchingNetworkModules(searchMode, key, nvPair));
  }
  
  /***************************************************************************
  **
  ** Return network modules that a node belongs to (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
    
  public Map<String, Set<String>> getModuleMembership(String nodeID) {
    return (ovrops_.getModuleMembership(nodeID));
  }
   
  /***************************************************************************
  **
  ** Get the firstView preference
  **
  */   
  
  public String getFirstViewPreference(TaggedSet modChoice, TaggedSet revChoice) {
    return (ovrops_.getFirstViewPreference(modChoice, revChoice));
  }     
  
  /***************************************************************************
  **
  ** Get the overlay mode
  **
  */
  
  public int overlayModeForOwner() {
    return (ovrops_.overlayModeForOwner());
  }    

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void overlayChangeUndo(NetworkOverlayOwnerChange undo) {
    ovrops_.overlayChangeUndo(undo);
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void overlayChangeRedo(NetworkOverlayOwnerChange redo) {
    ovrops_.overlayChangeRedo(redo);
    return;  
  }
  
  /***************************************************************************
  **
  ** Fill in map needed to extract overlay properties for layout extraction
  ** for the given group
  */  
  
  public void fillMapsForGroupExtraction(String grpID, Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap) {
    ovrops_.fillMapsForGroupExtraction(grpID, keyMap);
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Used to display nodes not in a group
  **
  */
  
  public static class AddedNode implements Cloneable {
    public String nodeName;
    public String groupToUse;
    
    public AddedNode(String nodeName, String groupToUse) {
      this.nodeName = nodeName;
      this.groupToUse = groupToUse;
    }
    
    public AddedNode(AddedNode other) {
      this.nodeName = other.nodeName;
      this.groupToUse = other.groupToUse;
    }
     
    public AddedNode clone() { 
      try {
        return ((AddedNode)super.clone());
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }  
  }  
  
  /***************************************************************************
  **
  ** Bundles up all the changes from a property change
  **
  */
  
  public static class ChangeBundle {
    public ProxyChange proxyChange;
    public ImageChange[] imageChanges;
    public UserTreePathChange[] pathChanges; 
    public DatabaseChange dc;
  }    
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answer if the ID is a Dynamic instance
  **
  */
  
  public static boolean isDynamicInstance(String key) {
    return (key.startsWith("{DiP}@"));      
  }  

  /***************************************************************************
  **
  ** Extract the proxy ID from the key
  **
  */
  
  public static String extractProxyID(String key) {
    Pattern p = Pattern.compile(".+-(.+):[^:]+");
    Matcher pm = p.matcher(key);
    if (pm.matches()) {
      return (pm.group(1));
    } else {
      System.err.println("I don't match " + key);
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Extract the time from the key
  **
  */
  
  public static String extractTime(String key) {
    Pattern p = Pattern.compile(".+-(.+):([^:]+)");
    Matcher pm = p.matcher(key);
    if (pm.matches()) {
      return (pm.group(2));
    } else {
      System.err.println("I don't match " + key);
      throw new IllegalArgumentException();
    }
  }  
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("dynamicProxy");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the added node keyword
  **
  */
  
  public static String addedNodeKeyword() {
    return ("addedNode");
  }
  
  /***************************************************************************
  **
  ** Return the added node keyword
  **
  */
  
  public static String imageKeyword() {
    return ("dpImage");
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static AddedNode extractAddedNode(String elemName, 
                                           Attributes attrs) throws IOException {
    String extraName = AttributeExtractor.extractAttribute(
                         elemName, attrs, "addedNode", "ref", true);
    String fromGroup = AttributeExtractor.extractAttribute(
                         elemName, attrs, "addedNode", "fromGroup", true);    
    return (new AddedNode(extraName, fromGroup));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static DynamicInstanceProxy buildFromXML(BTState appState, String elemName, 
                                                  Attributes attrs) throws IOException {
    if (!elemName.equals("dynamicProxy")) {
      return (null);
    }
    String name = null;
    String id = null;
    String parentVfg = null;
    String minHour = null;
    String maxHour = null;
    String isSingle = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("id")) {
          id = val;
        } else if (key.equals("vfgParent")) {
          parentVfg = val;          
        } else if (key.equals("minHour")) {
          minHour = val;          
        } else if (key.equals("maxHour")) {
          maxHour = val;          
        } else if (key.equals("isSingle")) {
          isSingle = val;          
        }
      }
    }
    
    if ((name == null) || (id == null) || (parentVfg == null)) {
      throw new IOException();
    }
    Database db = appState.getDB();
    GenomeInstance vfg = (GenomeInstance)db.getGenome(parentVfg);
    return (new DynamicInstanceProxy(appState, name, id, vfg, isSingle, minHour, maxHour));
  }  

  /***************************************************************************
  **
  ** Gets text notes for the proxy
  */
  
  public XPlatDisplayText getAllDisplayText() {
    XPlatDisplayText retval = new XPlatDisplayText();
    retval.setModelText(null);
    
    ovrops_.getAllDisplayText(retval);
    
    Iterator<Note> noit = notes_.iterator();
    while (noit.hasNext()) {
      Note note = noit.next();
      String nDesc = note.getTextWithBreaksReplaced();     
      if ((nDesc != null) && !nDesc.trim().equals("")) { 
        retval.setNoteText(note.getID(), nDesc);
      }    
    }
    return (retval);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor: do not use
  */

  protected DynamicInstanceProxy() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Write the groups to XML
  **
  */
  
  private void writeGroups(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<dpGroups>");
    Iterator<Group> grit = groups_.iterator();
    ind.up();
    while (grit.hasNext()) {
      Group g = grit.next();
      g.writeXML(out, ind, true);
    }
    ind.down().indent(); 
    out.println("</dpGroups>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the notes to XML
  **
  */
  
  private void writeNotes(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<dpNotes>");
    Iterator<Note> nit = notes_.iterator();
    ind.up();
    while (nit.hasNext()) {
      Note n = nit.next();
      n.writeXML(out, ind, true);
    }
    ind.down().indent(); 
    out.println("</dpNotes>");
    return;
  }  
   
  /***************************************************************************
  **
  ** Write the added nodes to XML
  */
  
  private void writeAddedNodes(PrintWriter out, Indenter ind) {
    Iterator<AddedNode> anit = addedNodes_.iterator();
    while (anit.hasNext()) {
      AddedNode an = anit.next();
      ind.indent();      
      out.print("<addedNode ");
      out.print("ref=\"");
      out.print(an.nodeName);
      out.print("\" fromGroup=\"");
      out.print(an.groupToUse);
      out.println("\" />");
    }          
    return;
  }
  
  /***************************************************************************
  **
  ** Write the image refs to XML
  **
  */
  
  private void writeImages(PrintWriter out, Indenter ind) {
    if (imageKeys_.isEmpty()) {
      return;
    }
    ind.indent();    
    out.println("<dpImages>");
    TreeSet<Integer> sortkeys = new TreeSet<Integer>(imageKeys_.keySet());
    Iterator<Integer> skit = sortkeys.iterator();
    ind.up();
    while (skit.hasNext()) {
      Integer key = skit.next();
      String img = imageKeys_.get(key);
      ind.indent();      
      out.print("<dpImage ");
      out.print("img=\"");
      out.print(img);
      if (!isSingle_) {
        out.print("\" time=\"");
        out.print(key);
      }
      out.println("\" />");
    }
    ind.down().indent(); 
    out.println("</dpImages>");
    return;
  }    
 
  /***************************************************************************
  **
  ** Build sorted times
  */
  
  private TreeSet<Integer> getSortedTimes() {
    //
    // Used to just issue "interesting times".  Now we issue
    // all hours in our bounds.
    //
    if (sortedTimes_ == null) {
      sortedTimes_ = new TreeSet<Integer>();
      for (int i = min_; i <= max_; i++) {
        sortedTimes_.add(new Integer(i));
      }
    }
    return (sortedTimes_);
  }
   
  /*
    if (sortedTimes_ == null) {
      Database db = appState_.getDB();
      TimeCourseData tcd = db.getTimeCourseData();
      Set times = tcd.getInterestingTimes();
      sortedTimes_ = new TreeSet();
      Iterator rtimit = times.iterator();
      while (rtimit.hasNext()) {
        Integer timeObj = (Integer)rtimit.next();
        int time = timeObj.intValue();
        if (((min_ == -1) || (time >= min_)) && 
            ((max_ == -1) || (time <= max_))) {
          sortedTimes_.add(timeObj);
        }
      }
    }
    return (sortedTimes_);
  } 
   
  */  
    
  /***************************************************************************
  **
  ** If modules reference an extra node, it has to be dropped if the
  ** extra node is dropped!
  */  
  
  public NetModuleChange[] dropModuleMembersForExtraNode(String extraID) {  
       
    ArrayList<NetModuleChange> preRetval = new ArrayList<NetModuleChange>();
    Iterator<NetworkOverlay> oit = getNetworkOverlayIterator();
    while (oit.hasNext()) {
      NetworkOverlay no = oit.next();
      String noID = no.getID();
      Iterator<NetModule> nmit = no.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        if (nmod.isAMember(extraID)) {
          NetModuleChange nmc = deleteMemberFromNetworkModule(noID, nmod, extraID);
          if (nmc != null) {
            preRetval.add(nmc);
          }
        }
      }
    }
    return (preRetval.toArray(new NetModuleChange[preRetval.size()]));
  }
}
