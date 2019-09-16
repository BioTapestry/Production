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

import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This represents a node instance in BioTapestry
*/

public class NodeInstance extends GenomeItemInstance implements Node {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int VESTIGIAL = NUM_BASE_ACTIVITY_LEVELS;
  private static final int NUM_ACTIVITY_LEVELS_ = VESTIGIAL + 1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String BARE_TAG_      = "bareInstance";
  private static final String BOX_TAG_       = "boxInstance";
  private static final String BUBBLE_TAG_    = "bubbleInstance";
  private static final String INTERCELL_TAG_ = "intercelInstance";  
  private static final String SLASH_TAG_     = "slashInstance";
  private static final String DIAMOND_TAG_   = "diamondInstance";  

  /**
  *** Inactivity states
  **/ 
    
  private static final String INACTIVE_STR_  = "inactive";
  private static final String ACTIVE_STR_    = "active";
  private static final String VESTIGIAL_STR_ = "vestigial";    
  private static final String VARIABLE_STR_  = "variable";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected int nodeType_;
  protected Double activityLevel_;  // will be null unless activity is variable
  protected int activity_;
  protected String nameOverride_;
  protected String description_;
  protected ArrayList<String> urls_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy constructor
  */

  public NodeInstance(NodeInstance other) {
    super(other);
    this.nodeType_ = other.nodeType_;
    this.activityLevel_ = other.activityLevel_;
    this.activity_ = other.activity_;    
    this.nameOverride_ = other.nameOverride_;
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);
  }

  /***************************************************************************
  **
  ** Copy with instance number change
  */

  public NodeInstance(NodeInstance other, int instance) {
    super(other, instance);
    this.nodeType_ = other.nodeType_;
    this.activityLevel_ = other.activityLevel_;
    this.activity_ = other.activity_;
    this.nameOverride_ = other.nameOverride_;
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);
  }    

  /***************************************************************************
  **
  ** For UI-based creation
  */

  public NodeInstance(BTState appState, DBGenomeItem backing, int nodeType, 
                      int instance, Double activityLevel, int activity) {
    super(appState, backing, instance);
    nodeType_ = nodeType;
    activityLevel_ = activityLevel;
    activity_ = activity;
    nameOverride_ = null;  // FIX ME?
    description_ = null;
    urls_ = new ArrayList<String>();
  }  
   
  /***************************************************************************
  **
  ** For XML-based creation
  */

  public NodeInstance(BTState appState, DBGenomeItem backing, String elemName, 
                      String instance, String activityLevel, 
                      String activityType, String nameOverride) throws IOException {
    super(appState, backing, instance);
    nameOverride_ = nameOverride;
    try {
      // Type may be set later (when used as super() by gene)
      if (elemName != null) {
        nodeType_ = mapFromElem(elemName);
      }
      if (activityType != null) {
        activity_ = mapActivityTypeTag(activityType);
      } else {
        activity_ = ACTIVE;
      }
    } catch (IllegalArgumentException ex) {
      throw new IOException();
    }

    if (activity_ == VARIABLE) {
      if (activityLevel == null) {
        throw new IOException();
      }
      try {
        activityLevel_ = new Double(activityLevel); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }     
    } else {
      activityLevel_ = null;
    }
    
    description_ = null;
    urls_ = new ArrayList<String>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Fixup activity bounds on legacy IO.  This needs to be called top-down, since
  ** it make us consistent wrt our parent only.
  ** 
  */
  
  public boolean fixupLegacyIOActivityBounds(GenomeInstance gi) {

    ActivityTracking nat = calcActivityBounds(gi);
    boolean retval = false;
    
    if (nat.parentActivity != null) {
      int parentNodeActivityVal = nat.parentActivity.intValue();
      if (!activityLevelAllowedInChild(parentNodeActivityVal, getActivity())) {
        setActivity(parentNodeActivityVal);
        retval = true;
      }
      if ((getActivity() == NodeInstance.VARIABLE) &&
          (nat.parentActivityLevel != Double.NEGATIVE_INFINITY) && 
          (getActivityLevel() > nat.parentActivityLevel)) {
        setActivityLevel(nat.parentActivityLevel);     
        retval = true;
      }
    }
    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Figure out required activity bounds based on children and parents
  ** Refactor into parent class (identical to link calcs?)
  ** 
  */
  
  public ActivityTracking calcActivityBounds(GenomeInstance gi) {

    Database db = appState_.getDB();    
    ActivityTracking retval = new ActivityTracking();
    
    retval.parentActivity = null;
    retval.parentActivityLevel = Double.NEGATIVE_INFINITY;
    retval.childActivities = null;
    retval.maxChildLevel = Double.NEGATIVE_INFINITY;
    
    //
    // Parent bounds.  We cannot be any more active than our parent.
    //    
    
    GenomeInstance parent = gi.getVfgParent();
    if (parent != null) {
      NodeInstance parentNode = (NodeInstance)parent.getNode(getID());
      int parentAct = parentNode.getActivity();
      retval.parentActivity = new Integer(parentAct);
      if (parentAct == NodeInstance.VARIABLE) {
        retval.parentActivityLevel = parentNode.getActivityLevel();
      }
    }
 
    //
    // Child bounds.  We cannot be any inconsistent with any of our children.
    //
   
    Iterator<GenomeInstance> git = db.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance giKid = git.next();
      if (giKid == gi) {
        continue;
      }
      if (gi.isAncestor(giKid)) {
        NodeInstance kidNode = (NodeInstance)giKid.getNode(getID());
        if (kidNode == null) { // not in child
          continue;
        }
        if (retval.childActivities == null) {
          retval.childActivities = new HashSet<Integer>();
        }
        int kidNodeActivity = kidNode.getActivity();
        retval.childActivities.add(new Integer(kidNodeActivity));
        if (kidNodeActivity == NodeInstance.VARIABLE) {
          double kidNodeActivityLevel = kidNode.getActivityLevel();
          if (kidNodeActivityLevel > retval.maxChildLevel) {
            retval.maxChildLevel = kidNodeActivityLevel;
          }
        }
      }
    }
    
    return (retval);
  }
  
 
  /***************************************************************************
  **
  ** Clone
  */

  public NodeInstance clone() {
    NodeInstance retval = (NodeInstance)super.clone();
    retval.urls_ = new ArrayList<String>(this.urls_);
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the Backing item (maybe make this more abstract?)
  ** 
  */
  
  public DBGenomeItem getBacking() {
    GenomeSource gSrc = (altSrc_ == null) ? appState_.getDB() : altSrc_;
    DBGenomeItem retval = (DBGenomeItem)gSrc.getGenome().getNode(myItemID_);
    if (retval == null) {
      throw new IllegalStateException();
      // May just be held in temporary holding storage:
      //retval = (DBGenomeItem)appState_.getDB().getHoldingGenome().getGene(myItemID_); 
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the node name
  */

  public String getName() {
    if (nameOverride_ == null) {
      return (super.getName());
    } else {
      return (nameOverride_);
    }
  }  

  /***************************************************************************
  **
  ** Set the node type.  Backing must be set first.
  */

  public void setNodeType(int type) {
    if (((DBNode)getBacking()).getNodeType() != type) {
      throw new IllegalStateException();
    }
    nodeType_ = type;
    return;
  }

  /***************************************************************************
  **
  ** Get the root name
  */

  public String getRootName() {
    return (getBacking().getName());
  }
  
  /***************************************************************************
  **
  ** Get the override name
  */

  public String getOverrideName() {
    return (nameOverride_);
  }  
 
  /***************************************************************************
  **
  ** Override the node name
  */

  public void overrideName(String name) {
    nameOverride_ = name;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the node type
  */

  public int getNodeType() {
    return (nodeType_);
  }

  /***************************************************************************
  **
  ** Answers the node activity class
  ** 
  */
  
  public int getActivity() {
    return (activity_);
  }

  /***************************************************************************
  **
  ** Set the node activity
  ** 
  */
  
  public void setActivity(int activity) {
    activity_ = activity;
    if (activity_ != VARIABLE) {
      activityLevel_ = null;
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Answers the node activity level; only valid for variable activity
  ** 
  */
  
  public double getActivityLevel() {
    if ((activity_ != VARIABLE) || (activityLevel_ == null)) {
      throw new IllegalStateException();
    }
    return (activityLevel_.doubleValue());
  }

  /***************************************************************************
  **
  ** Set the node activity state
  ** 
  */
  
  public void setActivityLevel(double activity) {
    if (activity_ != VARIABLE) {
      throw new IllegalStateException();
    }
    if ((activity < 0.0) || (activity > 1.0)) {
      throw new IllegalArgumentException();
    }
    activityLevel_ = new Double(activity);
    return;
  }  
   
  /***************************************************************************
  **
  ** Get the url iterator
  **
  */
  
  public Iterator<String> getURLs() {
    return (urls_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the url count
  **
  */
  
  public int getURLCount() {
    return (urls_.size());
  }      
  
  /***************************************************************************
  **
  ** Set all URLs
  **
  */
  
  public void setAllUrls(List<String> urls) {
    urls_.clear();
    urls_.addAll(urls);
    return;
  }      
  
  /***************************************************************************
  **
  ** Get the description
  **
  */
  
  public String getDescription() {
    return (description_);
  }
   
  /***************************************************************************
  **
  ** Set the description
  **
  */
  
  public void setDescription(String description) {
    description_ = description;    
    return;
  }  
  
  /***************************************************************************
  **
  ** Get display string.  Always show the type
  */

  public String getDisplayString(Genome genome, boolean typePreface) {
    GenomeInstance gi = (GenomeInstance)genome;
    ResourceManager rMan = appState_.getRMan();
    GroupMembership member = gi.getNodeGroupMembership(this);
    String groupID = (member.mainGroups.isEmpty()) ? null : (String)member.mainGroups.iterator().next();
    String groupName = (groupID == null) ? "" : gi.getGroup(groupID).getInheritedDisplayName(gi);    
    String format = rMan.getString("ncreate.importFormatForInstance");
    String typeDisplay = DBNode.mapTypeToDisplay(rMan, nodeType_);
    String nodeMsg = MessageFormat.format(format, new Object[] {typeDisplay, getName(), groupName});    
    return (nodeMsg);
  }  
  
  /***************************************************************************
  **
  ** Get display string for just groups.
  */

  public String getDisplayStringGroupOnly(GenomeInstance gi) {
    GroupMembership member = gi.getNodeGroupMembership(this);
    String groupID = (member.mainGroups.isEmpty()) ? null : (String)member.mainGroups.iterator().next();
    String groupName = (groupID == null) ? "" : gi.getGroup(groupID).getInheritedDisplayName(gi);    
    String format = appState_.getRMan().getString("ncreate.importFormatForInstanceGroup");
    String nodeMsg = MessageFormat.format(format, new Object[] {groupName});    
    return (nodeMsg);
  }  

  /***************************************************************************
  **
  ** Get the pad count
  */
  
  public int getPadCount() {    
    return (((DBNode)getBacking()).getPadCount());
  } 
  
  /***************************************************************************
  **
  ** Check for extra pads:
  */
  
  public boolean haveExtraPads() {  
    return (getPadCount() > DBNode.getDefaultPadCount(nodeType_));
  } 

  /***************************************************************************
  **
  ** Add a URL (I/O usage)
  **
  */
  
  public void addUrl(String url) {
    urls_.add(url);
    return;
  }    
  
  /***************************************************************************
  **
  ** Append to the description
  **
  */
  
  public void appendDescription(String description) {
    if (description_ == null) {
      description_ = description;
    } else {
      description_ = description_.concat(description);
    }
    description_ = CharacterEntityMapper.unmapEntities(description_, false);    
    return;
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("NodeInstance: type = " + mapToElem(nodeType_) + " backing = " + getBacking() +
            " instance = " + instanceID_ + " activity = " + activityLevel_ + " activity = " + activity_+
            " override = " + nameOverride_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(mapToElem(nodeType_));
    out.print(" ref=\"");
    out.print(getBacking().getID());
    out.print("\" instance=\"");
    out.print(instanceID_);
    if (activity_ != ACTIVE) {
      out.print("\" activity=\"");
      out.print(mapActivityTypes(activity_));
    }
    if (activityLevel_ != null) {
      out.print("\" activityLevel=\"");
      out.print(activityLevel_);
    }
    if (nameOverride_ != null) {
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(nameOverride_, false));
    }
    
    if (!urls_.isEmpty() || (description_ != null)) {
      out.println("\" >");
      (new CommonGenomeItemCode()).writeDescUrlToXML(out, ind, urls_, description_, "nodeInstance");
      ind.indent(); 
      out.print("</");
      out.print(mapToElem(nodeType_));
      out.println(">");
    } else {
      out.println("\" />");
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
  ** Handle Node Instance creation
  **
  */
  
  public static NodeInstance buildFromXML(BTState appState, Genome rootGenome, Genome genome, String elemName,
                                          Attributes attrs) throws IOException {

    String ref = null;
    String instance = null;
    String activity = null;  
    String activityLevelStr = null;
     
    String name = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("ref")) {
          ref = val;
        } else if (key.equals("instance")) {
          instance = val;
        } else if (key.equals("activity")) {
          activity = val;
        } else if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("activityLevel")) {
          activityLevelStr = val;
        } 
      }
    }
    
    DBNode backing = (DBNode)rootGenome.getNode(ref);
    return (new NodeInstance(appState, backing, elemName, instance, activityLevelStr, activity, name));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(BARE_TAG_);
    retval.add(BOX_TAG_);
    retval.add(BUBBLE_TAG_);
    retval.add(INTERCELL_TAG_);
    retval.add(SLASH_TAG_); 
    retval.add(DIAMOND_TAG_);     
    return (retval);
  }

  /***************************************************************************
  **
  ** Return the description keyword
  **
  */
  
  public static String descriptionKeyword() {
    return ("nodeInstance" + CommonGenomeItemCode.FT_TAG_ROOT);
  }
  
  /***************************************************************************
  **
  ** Return the URL keyword
  **
  */
  
  public static String urlKeyword() {
    return ("nodeInstance" + CommonGenomeItemCode.URL_TAG_ROOT);
  }  
   
  /***************************************************************************
  **
  ** Return possible activity choices values
  */
  
  public static Vector<ChoiceContent> getActivityChoices(BTState appState) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_ACTIVITY_LEVELS_; i++) {
      retval.add(activityTypeForCombo(appState, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent activityTypeForCombo(BTState appState, int activityType) {
    return (new ChoiceContent(appState.getRMan().getString("nprop." + mapActivityTypes(activityType)), activityType));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Map the activity types
  */

  protected static String mapActivityTypes(int activityType) {
    switch (activityType) {
      case INACTIVE:
        return (INACTIVE_STR_);
      case ACTIVE:
        return (ACTIVE_STR_);
      case VESTIGIAL:
        return (VESTIGIAL_STR_);
      case VARIABLE:
        return (VARIABLE_STR_);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** map the activity type tags
  */

  protected static int mapActivityTypeTag(String activityTag) {  
    if (activityTag.equalsIgnoreCase(INACTIVE_STR_)) {
      return (INACTIVE);
    } else if (activityTag.equalsIgnoreCase(ACTIVE_STR_)) {
      return (ACTIVE);
    } else if (activityTag.equalsIgnoreCase(VESTIGIAL_STR_)) {
      return (VESTIGIAL);
    } else if (activityTag.equalsIgnoreCase(VARIABLE_STR_)) {
      return (VARIABLE);      
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Say what activity levels are appropriate in a parent-child model relationship.
  */

  public static boolean activityLevelAllowedInChild(int parentActivity, int childActivity) {  
    switch (parentActivity) {
      case ACTIVE:
        return (true);
      case INACTIVE:
        return (childActivity == INACTIVE);
      case VARIABLE:
        return ((childActivity == VARIABLE) || (childActivity == INACTIVE));
      case VESTIGIAL:
         return ((childActivity == VESTIGIAL) || (childActivity == INACTIVE));
      default:
        throw new IllegalArgumentException();
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** map XML tags to values
  */

  private static int mapFromElem(String elemTag) {    
    if (elemTag == null) {
      throw new IllegalArgumentException();
    }
    if (elemTag.equals(BARE_TAG_)) {
      return (BARE);
    } else if (elemTag.equals(BOX_TAG_)) {
      return (BOX);
    } else if (elemTag.equals(BUBBLE_TAG_)) {
      return (BUBBLE);
    } else if (elemTag.equals(INTERCELL_TAG_)) {
      return (INTERCELL);
    } else if (elemTag.equals(SLASH_TAG_)) {
      return (SLASH);
    } else if (elemTag.equals(DIAMOND_TAG_)) {
      return (DIAMOND);        
    } else {
      throw new IllegalArgumentException();
    }
  }  

  /***************************************************************************
  **
  ** Get XML tag from value
  */

  private static String mapToElem(int nodeType) {
    switch (nodeType) {
      case BARE:
        return (BARE_TAG_);
      case BOX:
        return (BOX_TAG_);
      case BUBBLE:
        return (BUBBLE_TAG_);
      case INTERCELL:
        return (INTERCELL_TAG_);
      case SLASH:
        return (SLASH_TAG_);
      case DIAMOND:
        return (DIAMOND_TAG_);        
      default:
        throw new IllegalStateException();
    }
  }
}
