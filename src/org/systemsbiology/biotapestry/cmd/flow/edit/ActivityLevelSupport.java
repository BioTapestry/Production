/*
**    Copyright (C) 2003-201 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.edit;

import java.util.Iterator;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Use for validating acitivity level settings in the context of parent and child
*/

public class ActivityLevelSupport {
 
  public enum Results {
    VALID_ACTIVITY,   
    BAD_ACTIVITY_VALUE_VIA_PARENT,
    BAD_ACTIVITY_LEVEL_VIA_PARENT,
    BAD_ACTIVITY_VALUE_VIA_CHILD,
    BAD_ACTIVITY_LEVEL_VIA_CHILD,
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ActivityLevelSupport() { 
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Check if desired activity bounds can be allowed.  MERGE THIS WITH THE METHOD BELOW!!!!
  */
  
  public static Results checkActivityBounds(GenomeItemInstance.ActivityTracking actTrack, GenomeItemInstance.ActivityState nias) {
  
    int newActivity = nias.activityState;
            
    //
    // Make sure we are OK wrt our parent (if we have one):
    //
   
    if (actTrack.parentActivity != null) {
      int parentNodeActivityVal = actTrack.parentActivity.intValue();
      if (!NodeInstance.activityLevelAllowedInChild(parentNodeActivityVal, newActivity)) {
        return (Results.BAD_ACTIVITY_VALUE_VIA_PARENT);
      }
      if ((actTrack.parentActivityLevel != Double.NEGATIVE_INFINITY) && (newActivity == GenomeItemInstance.VARIABLE)) {
        if (nias.activityLevel.doubleValue() > actTrack.parentActivityLevel) {
          return (Results.BAD_ACTIVITY_LEVEL_VIA_PARENT); 
        }
      }
    }
    
    //
    // Make sure we are OK wrt our children:
    //
    
    if (actTrack.childActivities != null) {
      Iterator<Integer> cait = actTrack.childActivities.iterator();
      while (cait.hasNext()) {
        Integer childActivity = cait.next();
        int childActivityVal = childActivity.intValue();        
        if (!NodeInstance.activityLevelAllowedInChild(newActivity, childActivityVal)) {
          return (Results.BAD_ACTIVITY_VALUE_VIA_CHILD); 
        }       
      }
      if ((actTrack.maxChildLevel != Double.NEGATIVE_INFINITY) && (newActivity == GenomeItemInstance.VARIABLE)) {
        if (nias.activityLevel.doubleValue() < actTrack.maxChildLevel) {
          return (Results.BAD_ACTIVITY_LEVEL_VIA_CHILD);
        }
      }     
    }
    
    return (Results.VALID_ACTIVITY);
  }
  
  /***************************************************************************
  **
  ** Check if desired activity bounds can be allowed FOR LINKS
  */
  
  public static Results checkActivityBounds(GenomeItemInstance.ActivityTracking trackInfo, int newActivity, double newLevel) {
    
    //
    // Make sure we are OK wrt our parent (if we have one):
    //
   
    if (trackInfo.parentActivity != null) {
      int parentActivityVal = trackInfo.parentActivity.intValue();
      if (!LinkageInstance.activityLevelAllowedInChild(parentActivityVal, newActivity)) {
        return (Results.BAD_ACTIVITY_VALUE_VIA_PARENT); 
      }
      if ((trackInfo.parentActivityLevel != Double.NEGATIVE_INFINITY) && (newActivity == LinkageInstance.VARIABLE)) {
        if (newLevel > trackInfo.parentActivityLevel) {
          return (Results.BAD_ACTIVITY_LEVEL_VIA_PARENT);              
        }
      }
    }
    
    //
    // Make sure we are OK wrt our children:
    //
    
    if (trackInfo.childActivities != null) {
      Iterator<Integer> cait = trackInfo.childActivities.iterator();
      while (cait.hasNext()) {
        Integer childActivity = cait.next();
        int childActivityVal = childActivity.intValue();
        if (!LinkageInstance.activityLevelAllowedInChild(newActivity, childActivityVal)) {
          return (Results.BAD_ACTIVITY_VALUE_VIA_CHILD);
        }       
      }
      if ((trackInfo.maxChildLevel != Double.NEGATIVE_INFINITY) && (newActivity == LinkageInstance.VARIABLE)) {
        if (newLevel < trackInfo.maxChildLevel) {
          return (Results.BAD_ACTIVITY_LEVEL_VIA_CHILD);         
        }
      }     
    }    
    return (Results.VALID_ACTIVITY);
  }
  
 
  /***************************************************************************
  **
  ** Show error dialog for activity level errors:
  */
  
  public static void showForNode(Results res, BTState appState) {   
    ResourceManager rMan = appState.getRMan();
    switch (res) {
      case VALID_ACTIVITY:
        throw new IllegalArgumentException();
      case BAD_ACTIVITY_VALUE_VIA_PARENT:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityValViaParent"), 
                                      rMan.getString("nodeProp.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        break;
      case BAD_ACTIVITY_LEVEL_VIA_PARENT:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityLevelViaParent"), 
                                      rMan.getString("nodeProp.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        break;
      case BAD_ACTIVITY_VALUE_VIA_CHILD:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityValViaChild"), 
                                      rMan.getString("nodeProp.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);  
        break;
      case BAD_ACTIVITY_LEVEL_VIA_CHILD:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityLevelViaChild"), 
                                      rMan.getString("nodeProp.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE); 
        break;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Show error dialog for activity level errors:
  */
  
  public static void showForLink(Results res, BTState appState) {   
    ResourceManager rMan = appState.getRMan();
    switch (res) {
      case VALID_ACTIVITY:
        throw new IllegalArgumentException();
      case BAD_ACTIVITY_VALUE_VIA_PARENT:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("lprop.badActivityValViaParent"), 
                                      rMan.getString("lprop.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        break;
      case BAD_ACTIVITY_LEVEL_VIA_PARENT:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("lprop.badActivityLevelViaParent"), 
                                      rMan.getString("lprop.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        break;
      case BAD_ACTIVITY_VALUE_VIA_CHILD:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("lprop.badActivityValViaChild"), 
                                      rMan.getString("lprop.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);  
        break;
      case BAD_ACTIVITY_LEVEL_VIA_CHILD:
        JOptionPane.showMessageDialog(appState.getTopFrame(), 
                                      rMan.getString("lprop.badActivityLevelViaChild"), 
                                      rMan.getString("lprop.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE); 
        break;
      default:
        throw new IllegalArgumentException();
    }
    return;
  }
  
  
  /***************************************************************************
  **
  ** Get SUF for activity level errors:
  */
  
  public static SimpleUserFeedback sufForNode(Results res, BTState appState) {   
    ResourceManager rMan = appState.getRMan();
    SimpleUserFeedback suf = null;
    switch (res) {
      case VALID_ACTIVITY:
        throw new IllegalArgumentException();
      case BAD_ACTIVITY_VALUE_VIA_PARENT:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("nodeProp.badActivityValViaParent"), rMan.getString("nodeProp.badActivityTitle"));
        break;
      case BAD_ACTIVITY_LEVEL_VIA_PARENT:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("nodeProp.badActivityLevelViaParent"), rMan.getString("nodeProp.badActivityTitle"));
        break;
      case BAD_ACTIVITY_VALUE_VIA_CHILD:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("nodeProp.badActivityValViaChild"), rMan.getString("nodeProp.badActivityTitle"));
        break;
      case BAD_ACTIVITY_LEVEL_VIA_CHILD:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("nodeProp.badActivityLevelViaChild"), rMan.getString("nodeProp.badActivityTitle"));
        break;
      default:
        throw new IllegalArgumentException();
    }
    return (suf);
  }
  
  /***************************************************************************
  **
  ** Get SUF for activity level errors:
  */
  
  public static SimpleUserFeedback sufForLink(Results res, BTState appState) {  
    ResourceManager rMan = appState.getRMan();
    SimpleUserFeedback suf = null;
    switch (res) {
      case VALID_ACTIVITY:
        throw new IllegalArgumentException();
      case BAD_ACTIVITY_VALUE_VIA_PARENT:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("lprop.badActivityValViaParent"), rMan.getString("lprop.badActivityTitle"));
        break;
      case BAD_ACTIVITY_LEVEL_VIA_PARENT:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("lprop.badActivityLevelViaParent"), rMan.getString("lprop.badActivityTitle"));
        break;
      case BAD_ACTIVITY_VALUE_VIA_CHILD:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("lprop.badActivityValViaChild"), rMan.getString("lprop.badActivityTitle"));
        break;
      case BAD_ACTIVITY_LEVEL_VIA_CHILD:
        suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, rMan.getString("lprop.badActivityLevelViaChild"), rMan.getString("lprop.badActivityTitle"));
        break;
      default:
        throw new IllegalArgumentException();
    }
    return (suf);
  }
}