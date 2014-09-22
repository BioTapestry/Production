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

import java.util.Comparator;
import java.util.HashSet;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.GenomeSource;

/****************************************************************************
**
** This represents a Genome Instance item in BioTapestry 
*/

public abstract class GenomeItemInstance implements GenomeItem, Cloneable {

  
  public static final int ACTIVITY_NOT_SET = -1;   
  public static final int ACTIVE     = 0;   
  public static final int INACTIVE   = 1;
  public static final int VARIABLE   = 2;  
  protected static final int NUM_BASE_ACTIVITY_LEVELS = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  protected String myItemID_;
  protected String instanceID_;
  protected BTState appState_;
  protected GenomeSource altSrc_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Not Null constructor
  */

  public GenomeItemInstance(BTState appState) {
    appState_ = appState;
  }
  
  /***************************************************************************
  **
  ** Copy constructor
  */

  public GenomeItemInstance(GenomeItemInstance other) {
    appState_ = other.appState_;
    myItemID_ = other.myItemID_;
    instanceID_ = other.instanceID_;
    altSrc_ = other.altSrc_;
  }  
 
  /***************************************************************************
  **
  ** Copy with instance number change
  */

  public GenomeItemInstance(GenomeItemInstance other, int instance) {
    appState_ = other.appState_;
    myItemID_ = other.myItemID_;
    instanceID_ = Integer.toString(instance);
    altSrc_ = other.altSrc_;
  }    

  /***************************************************************************
  **
  ** UI-based creation
  */

  public GenomeItemInstance(BTState appState, DBGenomeItem backing, int instance) {
    appState_ = appState;
    myItemID_ = backing.getID();
    instanceID_ = Integer.toString(instance);
    altSrc_ = null;
  }  
  
  /***************************************************************************
  **
  ** XML-based creation
  */

  public GenomeItemInstance(BTState appState, DBGenomeItem backing, String instance) {
    appState_ = appState;
    myItemID_ = backing.getID();
    instanceID_ = instance;
    altSrc_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the alternate source for the backing object
  ** 
  */
  
  public void setGenomeSource(GenomeSource gSrc) {
    altSrc_ = gSrc;
    return;
  }   
  
 /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  @Override
  public GenomeItemInstance clone() { 
    try {
      return ((GenomeItemInstance)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
   
  /***************************************************************************
  **
  ** Set the name
  ** 
  */
  
  public void setName(String name) {
    getBacking().setName(name);
    return;
  }  
    
  /***************************************************************************
  **
  ** Get the name
  ** 
  */
  
  public String getName() {
    return (getBacking().getName());
  }  
  
  /***************************************************************************
  **
  ** Get the ID
  ** 
  */
  
  public String getID() {
    return (myItemID_ + ":" + instanceID_);
  }

  /***************************************************************************
  **
  ** Get the instance
  ** 
  */
  
  public String getInstance() {
    return (instanceID_);
  }  
   
  /***************************************************************************
  **
  ** Get the Backing item (maybe make this more abstract?)
  ** 
  */
  
  public abstract DBGenomeItem getBacking();
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("GenomeItemInstance: backing = " + myItemID_ + " instance = " + instanceID_);
  }
  
  /***************************************************************************
  **
  ** Get the ID /instance result
  ** 
  */
  
  public static String getCombinedID(String id, String instance) {
    return (id + ":" + instance);
  }
 
  /***************************************************************************
  **
  ** Answer if this is a baseID
  ** 
  */
  
  public static boolean isBaseID(String id) {
    return (id.indexOf(":") == -1);
  }  
  
  /***************************************************************************
  **
  ** Get the baseID
  ** 
  */
  
  public static String getBaseID(String id) {
    int index = id.indexOf(":");
    if (index == -1) {
      return (id);
    }
    return (id.substring(0, index));
  }
  
  /***************************************************************************
  **
  ** Get the instance
  ** 
  */
  
  public static int getInstanceID(String id) {
    String retvalStr = id.substring(id.indexOf(":") + 1);
    try {
      return (Integer.parseInt(retvalStr));
    } catch (NumberFormatException nfe) {
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
  ** A class for dealing with activity state
  */

  public static class ActivityState {
    public int activityState;
    public Double activityLevel;
    
    public ActivityState(int activityState, double activityLevel) {
      this.activityState = activityState;
      this.activityLevel = (activityState == VARIABLE) ? new Double(activityLevel) : null;
    } 
  } 
  
  /***************************************************************************
  **
  ** Used for activity tracking
  */
  
  public static class ActivityTracking {
    public Integer parentActivity;
    public double parentActivityLevel;
    public HashSet<Integer> childActivities;
    public double maxChildLevel;  
  }
  
  /***************************************************************************
  **
  ** Perform consistent ordering of DB and Instance ID orderings.  This is needed
  ** so that e.g. VfG and VfA stacked layouts of the same network look the same.
  ** We frequently do arbitrary but fixed (and reproducable) orderings based on
  ** object ID.  But note that the ordering [3, 381, 708, 88] at the DB level is
  ** not consistent with [381:0, 3:0, 708:0, 88:0] at the instance level: the colon
  ** can mess things up.  So we use this comparator instead!
  */
   
  public static class DBAndInstanceConsistentComparator implements Comparator<String> {

    public int compare(String firstID, String secondID) {
      boolean isFirstBase = isBaseID(firstID);
      boolean isSecondBase = isBaseID(secondID);
      if (isFirstBase && isSecondBase) {
        return (firstID.compareTo(secondID));        
      }
 
      int firstInst = -1000;
      String firstBase;
      if (isFirstBase) {
        firstBase = firstID;
      } else {
        firstBase = GenomeItemInstance.getBaseID(firstID);
        firstInst = getInstanceID(firstID);
      }
      
      int secondInst = -1000;
      String secondBase;
      if (isSecondBase) {
        secondBase = secondID;
      } else {
        secondBase = GenomeItemInstance.getBaseID(secondID);
        secondInst = getInstanceID(secondID);
      }
      
      int baseComp = firstBase.compareTo(secondBase);
      if (baseComp != 0) {
        return (baseComp);
      }
   
      return (secondInst - firstInst);
    }
  }   
}
