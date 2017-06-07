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
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;

/****************************************************************************
**
** This represents a linkage instance
*/

public class LinkageInstance extends GenomeItemInstance implements Linkage {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int USE_SOURCE = NUM_BASE_ACTIVITY_LEVELS;
  private static final int NUM_ACTIVITY_LEVELS_ = USE_SOURCE + 1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /**
  *** Activity states
  **/
   
  private static final String INACTIVE_STR_  = "inactive";
  private static final String ACTIVE_STR_    = "active";
  private static final String VARIABLE_STR_ = "variable";    
  private static final String SOURCE_STR_  = "source";    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final int NO_PAD_ = -100000;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  

  private String src_;
  private String targ_;
  private int pad_;
  private int lpad_;
  protected Double activityLevel_;  // will be null unless activity is variable
  protected int activity_;
  protected Double simulationLevel_; // will be null unless simulation level is installed
  
  private String description_;
  private ArrayList<String> urls_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public LinkageInstance(LinkageInstance other) {
    super(other);
    this.src_ = other.src_;
    this.targ_ = other.targ_;
    this.pad_ = other.pad_;
    this.lpad_ = other.lpad_;
    this.activityLevel_ = other.activityLevel_;
    this.activity_ = other.activity_;
    this.simulationLevel_ = other.simulationLevel_;
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);   
  }

  /***************************************************************************
  **
  ** Copy Constructor with new instance numbers
  */

  public LinkageInstance(LinkageInstance other, int newInstance, int srcInstance, int trgInstance) {
    super(other, newInstance);
    this.src_ = getCombinedID(getBaseID(other.src_), Integer.toString(srcInstance));
    this.targ_ = getCombinedID(getBaseID(other.targ_), Integer.toString(trgInstance));
    this.pad_ = other.pad_;
    this.lpad_ = other.lpad_;
    this.activityLevel_ = other.activityLevel_;
    this.activity_ = other.activity_;
    this.simulationLevel_ = other.simulationLevel_;
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);
  } 
  
  /***************************************************************************
  **
  ** Copy Constructor for merged instances
  */

  public LinkageInstance(LinkageInstance other, String newBaseID, int newInstance) {
    super(other, newInstance);
    this.myItemID_ = newBaseID;
    this.src_ = other.src_;
    this.targ_ = other.targ_;
    this.pad_ = other.pad_;
    this.lpad_ = other.lpad_;
    this.activityLevel_ = other.activityLevel_;
    this.activity_ = other.activity_;
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);
  } 
  

  /***************************************************************************
  **
  ** For UI-based creation
  */

  public LinkageInstance(DataAccessContext dacx, DBLinkage backing, int instance, 
                         int srcInstance, int trgInstance) {                           
    super(dacx, backing, instance);
    this.src_ = getCombinedID(backing.getSource(), Integer.toString(srcInstance));
    this.targ_ = getCombinedID(backing.getTarget(), Integer.toString(trgInstance));
    this.pad_ = backing.getLandingPad();
    this.lpad_ = backing.getLaunchPad();
    // Link instances created at VfA level start off active:
    this.activityLevel_ = null;
    this.activity_ = ACTIVE;
    this.simulationLevel_ = null;
    this.description_ = null;
    this.urls_ = new ArrayList<String>();
  }

  /***************************************************************************
  **
  ** Name and id
  */

  public LinkageInstance(DataAccessContext dacx, DBGenomeItem backing, String instance, 
                         String src, String targ, String pad, 
                         String lpad, String activityLevel, 
                         String activityType) throws IOException {
      
    super(dacx, backing, instance);
    src_ = src.trim();
    targ_ = targ.trim();
    
    pad_ = NO_PAD_;
    if (pad != null) {
      try {
        pad_ = Integer.parseInt(pad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    lpad_ = NO_PAD_;
    if (lpad != null) {
      try {
        lpad_ = Integer.parseInt(lpad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    
    try {
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
    
    simulationLevel_ = null;
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
  ** Answers the simulation activity level. May be null.
  ** 
  */
  
  public Double getSimulationLevel() {
    return (simulationLevel_);
  }
  
  /***************************************************************************
  **
  ** Set the simulation activity level. May be null.
  ** 
  */
  
  public void setSimulationLevel(Double level) {
    simulationLevel_ = level;
    return;
  }
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public LinkageInstance clone() {
    LinkageInstance retval = (LinkageInstance)super.clone();
    retval.urls_ = new ArrayList<String>(this.urls_);
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Get the Backing item (maybe make this more abstract?)
  ** 
  */
  
  public DBGenomeItem getBacking() {
    GenomeSource gSrc = (altSrc_ == null) ? dacx_.getGenomeSource() : altSrc_;
    return ((DBGenomeItem)gSrc.getRootDBGenome().getLinkage(myItemID_)); 
  }
  
  /***************************************************************************
  **
  ** Get the source
  ** 
  */
  
  public String getSource() {
    return (src_);
  }  
  
  /***************************************************************************
  **
  ** Set the source
  ** 
  */
  
  public void setSource(String src) {
    src_ = src;
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the target
  ** 
  */
  
  public void setTarget(String targ) {
    targ_ = targ;
    return;
  }    
  
  /***************************************************************************
  **
  ** Get the target
  ** 
  */
  
  public String getTarget() {
    return (targ_);
  }
  
  /***************************************************************************
  **
  ** Get the sign
  ** 
  */
  
  public int getSign() {
    return (((Linkage)getBacking()).getSign());
  }
  
  /***************************************************************************
  **
  ** Get the landing pad
  */
  
  public int getLandingPad() {
    return (pad_);
  }
  
  /***************************************************************************
  **
  ** Get the launch pad
  */
  
  public int getLaunchPad() {
    return (lpad_);
  }
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLandingPad(int landingPad) {
    pad_ = landingPad;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLaunchPad(int launchPad) {
    lpad_ = launchPad;
    return;
  }

  /***************************************************************************
  **
  ** Sync up the pads with the root.  WARNING!  This should only be used
  ** in a context where pad restrictions are maintained by the caller (e.g.
  ** no inbound links sharing a pad with an outbound link.
  */
  
  public void syncLaunchPads(boolean doSource, boolean doTarget) {
    Linkage backing = (Linkage)getBacking();
    if (doTarget) {
      this.pad_ = backing.getLandingPad();
    }
    if (doSource) {
      this.lpad_ = backing.getLaunchPad();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the target experimental verification
  */
  
  public int getTargetLevel() {
    return (((Linkage)getBacking()).getTargetLevel());
  }
  
   /***************************************************************************
  **
  ** Set the link sign
  */
  
  public void setSign(int newSign) {
    ((Linkage)getBacking()).setSign(newSign);    
  }
  
  /***************************************************************************
  **
  ** Set the target experimental verification
  */
  
  public void setTargetLevel(int newTarget) {
    ((Linkage)getBacking()).setTargetLevel(newTarget);    
  }
  
  /***************************************************************************
  **
  ** Answers the node activity class.  Refers back to the source node if
  ** that is the setting.
  */
  
  public int getActivity(GenomeInstance gi) {  
    if (activity_ != USE_SOURCE) {
      return (activity_);
    }      
    NodeInstance ni = (NodeInstance)gi.getNode(src_);
    switch (ni.getActivity()) {
      case NodeInstance.INACTIVE:
        return (INACTIVE);
      case NodeInstance.ACTIVE:
      case NodeInstance.VESTIGIAL:
        return (ACTIVE);
      case NodeInstance.VARIABLE:
        return (VARIABLE);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Figure out required activity bounds based on children and parents
  ** 
  */
  
  public ActivityTracking calcActivityBounds(GenomeInstance gi) {

    ActivityTracking retval = new ActivityTracking();
    
    GenomeSource gs = dacx_.getGenomeSource();
    retval.parentActivity = null;
    retval.parentActivityLevel = Double.NEGATIVE_INFINITY;
    retval.childActivities = null;
    retval.maxChildLevel = Double.NEGATIVE_INFINITY;
  
    // Parent bounds.  We cannot be any more active than our parent.
    //    
    
    GenomeInstance parent = gi.getVfgParent();
    if (parent != null) {
      LinkageInstance parentLink = (LinkageInstance)parent.getLinkage(getID());
      int parentAct = parentLink.getActivity(parent);
      retval.parentActivity = new Integer(parentAct);
      if (parentAct == LinkageInstance.VARIABLE) {
        retval.parentActivityLevel = parentLink.getActivityLevel(parent);
      }
    }
 
    //
    // Child bounds.  We cannot be any inconsistent with any of our children.
    //
   
    Iterator<GenomeInstance> git = gs.getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance giKid = git.next();
      if (giKid == gi) {
        continue;
      }
      if (gi.isAncestor(giKid)) {
        LinkageInstance kidLinkage = (LinkageInstance)giKid.getLinkage(getID());
        if (kidLinkage == null) { // not in child
          continue;
        }
        if (retval.childActivities == null) {
          retval.childActivities = new HashSet<Integer>();
        }
        int kidActivity = kidLinkage.getActivitySetting();
        retval.childActivities.add(new Integer(kidActivity));
        if (kidActivity == LinkageInstance.VARIABLE) {
          double kidActivityLevel = kidLinkage.getActivityLevel(giKid);
          if (kidActivityLevel > retval.maxChildLevel) {
            retval.maxChildLevel = kidActivityLevel;
          }
        }
      }
    }
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Answers the link activity class setting.
  ** 
  */
  
  public int getActivitySetting() {
    return (activity_);
  }

  /***************************************************************************
  **
  ** Set the link activity
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
  ** Answers if we are tracking a variable source level
  ** 
  */
  
  public boolean usingVariableSrc(GenomeInstance gi) { 
    if (activity_ == USE_SOURCE) {
      int srcActivity = getActivity(gi);
      if (srcActivity == VARIABLE) {
        return (true);
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answers the node activity level; only valid for variable (or variable source) activity
  ** 
  */
  
  public double getActivityLevel(GenomeInstance gi) {
    if (activity_ == VARIABLE) {
      return (activityLevel_.doubleValue());
    } else if (activity_ == USE_SOURCE) {
      int srcActivity = getActivity(gi);
      if (srcActivity == VARIABLE) {
        NodeInstance ni = (NodeInstance)gi.getNode(src_);
        return (ni.getActivityLevel());
      }
    }
    throw new IllegalStateException();  
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
  ** Get display string
  */

  public String getDisplayString(Genome genome, boolean typePreface) {
    Node srcNode = genome.getNode(src_);
    Node trgNode = genome.getNode(targ_);
    String format = DBLinkage.mapSignToDisplay(dacx_, getSign());
    String linkMsg = MessageFormat.format(format, new Object[] {srcNode.getDisplayString(genome, typePreface), 
                                                                trgNode.getDisplayString(genome, typePreface)});
    return (linkMsg);
  }
  
  /***************************************************************************
  **
  ** Get display string
  */

  public String getDisplayStringGroupOnly(GenomeInstance gi) {
    NodeInstance srcNode = (NodeInstance)gi.getNode(src_);
    NodeInstance trgNode = (NodeInstance)gi.getNode(targ_);
    String format = DBLinkage.mapSignToDisplay(dacx_, getSign());
    String linkMsg = MessageFormat.format(format, new Object[] {srcNode.getDisplayStringGroupOnly(gi), 
                                                                trgNode.getDisplayStringGroupOnly(gi)});
    return (linkMsg);
  }  

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("LinkageInstance: backing = " + getBacking() + " instance = " + instanceID_
            + " source = " + src_ + " target = " + targ_ + " pad = " + pad_ + " lpad = " + lpad_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();     
    out.print("<linkInstance ");
    out.print("src=\"");
    out.print(src_);
    out.print("\" targ=\"");
    out.print(targ_);
    out.print("\" ref=\"");
    out.print(getBacking().getID());
    out.print("\" targPad=\"");
    out.print(pad_);
    out.print("\" launchPad=\"");
    out.print(lpad_);
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
    
    if (!urls_.isEmpty() || (description_ != null)) {
      out.println("\" >");
      (new CommonGenomeItemCode()).writeDescUrlToXML(out, ind, urls_, description_, "linkInstance");
      ind.indent(); 
      out.println("</linkInstance>");   
    } else {
      out.println("\" />");
    }
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
  ** Add a URL (I/O usage)
  **
  */
  
  public void addUrl(String url) {
    urls_.add(url);
    return;
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
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Linkage creation
  **
  */
  
  public static LinkageInstance buildFromXML(DataAccessContext dacx, DBGenome rootGenome,
                                             Attributes attrs) throws IOException {
    String ref = null;
    String instance = null;
    String src = null;
    String targ = null;
    String pad = null;
    String lpad = null;
    String active = null; // legacy case only
    String activity = null;    
    String activityLevelStr = null;    
    
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
        } else if (key.equals("src")) {
          src = val;
        } else if (key.equals("targ")) {
          targ = val;
        } else if (key.equals("targPad")) {
          pad = val;          
        } else if (key.equals("launchPad")) {
          lpad = val;
        } else if (key.equals("active")) {  // legacy case only
          active = val;
        } else if (key.equals("activity")) {
          activity = val;
        } else if (key.equals("activityLevel")) {
          activityLevelStr = val;
        } 
      }
    }
    
    //
    // Catch legacy case:
    //
    
    if (active != null) {
      boolean legacyActive = Boolean.valueOf(active).booleanValue();
      if (!legacyActive) {
        activity = INACTIVE_STR_;    
      }
    }
    
    DBLinkage backing = (DBLinkage)rootGenome.getLinkage(ref);
    return (new LinkageInstance(dacx, backing, instance, src, targ, pad, lpad, activityLevelStr, activity));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("linkInstance");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the description keyword
  **
  */
  
  public static String descriptionKeyword() {
    return ("linkInstance" + CommonGenomeItemCode.FT_TAG_ROOT);
  }
  
  /***************************************************************************
  **
  ** Return the URL keyword
  **
  */
  
  public static String urlKeyword() {
    return ("linkInstance" + CommonGenomeItemCode.URL_TAG_ROOT);
  }  
  
  /***************************************************************************
  **
  ** Return possible activity choices values
  */
  
  public static Vector<ChoiceContent> getActivityChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_ACTIVITY_LEVELS_; i++) {
      retval.add(activityTypeForCombo(dacx, i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent activityTypeForCombo(DataAccessContext dacx, int activityType) {
    return (new ChoiceContent(dacx.getRMan().getString("lprop." + mapActivityTypes(activityType)), activityType));
  }
  
  /***************************************************************************
  **
  ** Map the activity types
  */

  public static String mapActivityTypes(int activityType) {
    switch (activityType) {
      case INACTIVE:
        return (INACTIVE_STR_);
      case ACTIVE:
        return (ACTIVE_STR_);
      case USE_SOURCE:
        return (SOURCE_STR_);
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

  public static int mapActivityTypeTag(String activityTag) {  
    if (activityTag.equalsIgnoreCase(INACTIVE_STR_)) {
      return (INACTIVE);
    } else if (activityTag.equalsIgnoreCase(ACTIVE_STR_)) {
      return (ACTIVE);
    } else if (activityTag.equalsIgnoreCase(SOURCE_STR_)) {
      return (USE_SOURCE);
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
      case USE_SOURCE:
        return ((childActivity == USE_SOURCE) || (childActivity == INACTIVE));
      default:
        throw new IllegalArgumentException();
    }
  }  
}
