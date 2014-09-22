/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;


/****************************************************************************
**
** This class contains directives for deriving a layout from others.  Used
** for upward layout synching of the root from the top-level instance models.
*/

public class LayoutDerivation implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
     
  private ArrayList<LayoutDataSource> ldsList_;
  private boolean swapPads_;
  private boolean forceUniquePads_;
  private int overlayOption_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** UI-based construction
  */   
  
  public LayoutDerivation() {
    ldsList_ = new ArrayList<LayoutDataSource>();
    swapPads_ = true;
    forceUniquePads_ = true;
    overlayOption_ = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
  }  
  
  /***************************************************************************
  **
  ** UI-based construction
  */   
  
  public LayoutDerivation(boolean swapPads, boolean forceUniquePads, int overlayOption) {
    ldsList_ = new ArrayList();
    swapPads_ = swapPads;
    forceUniquePads_ = forceUniquePads;
    overlayOption_ = overlayOption;
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

  @Override
  public LayoutDerivation clone() {
    try {
      LayoutDerivation retval = (LayoutDerivation)super.clone();
      int num = this.ldsList_.size();
      retval.ldsList_ = new ArrayList();
      for (int i = 0; i < num; i++) {
        LayoutDataSource lds = this.ldsList_.get(i);
        retval.ldsList_.add(lds.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Answer if we have a model dependency
  **
  */
  
  public boolean haveModelDependency(String modelID) {
    int num = ldsList_.size();
    for (int i = 0; i < num; i++) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.haveModelDependency(modelID)) {
        return (true);
      }
    }    
    return (false);
  }      
  
  /***************************************************************************
  **
  ** Remove a model dependency
  **
  */
  
  public void removeModelDependency(String modelID) {
    int i = 0;
    while (i < ldsList_.size()) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.haveModelDependency(modelID)) {
        ldsList_.remove(i);
      } else {
        i++;
      }
    }    
    return;
  }    
  
  /***************************************************************************
  **
  ** Get model dependencies
  **
  */
  
  public Set<String> getModelDependencies() {
    HashSet<String> retval = new HashSet<String>();
    int num = ldsList_.size();
    for (int i = 0; i < num; i++) {
      LayoutDataSource lds = ldsList_.get(i);
      retval.add(lds.getModelID());
    }    
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Answer if we have a group dependency
  **
  */
  
  public boolean haveGroupDependency(String modelID, String groupID) {
    int num = ldsList_.size();
    for (int i = 0; i < num; i++) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.haveGroupDependency(modelID, groupID)) {
        return (true);
      }
    }    
    return (false);
  }      
  
  /***************************************************************************
  **
  ** Remove a group dependency
  **
  */
  
  public void removeGroupDependency(String modelID, String groupID) {
    int i = 0;
    while (i < ldsList_.size()) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.haveGroupDependency(modelID, groupID)) {
        ldsList_.remove(i);
      } else {
        i++;
      }
    }    
    return;
  }    
  
  /***************************************************************************
  **
  ** Answer if we have a node dependency
  **
  */
  
  public boolean haveNodeDependency(String modelID, String nodeID) {
    int num = ldsList_.size();
    for (int i = 0; i < num; i++) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.haveNodeDependency(modelID, nodeID)) {
        return (true);
      }
    }    
    return (false);
  }      
  
  /***************************************************************************
  **
  ** Remove a node dependency
  **
  */
  
  public void removeNodeDependency(String modelID, String nodeID) {
    int i = 0;
    while (i < ldsList_.size()) {
      LayoutDataSource lds = ldsList_.get(i);
      if (lds.removeNodeDependency(modelID, nodeID)) {
        ldsList_.remove(i);
      } else {
        i++;
      }
    }    
    return;
  }      
  
  /***************************************************************************
  **
  ** Answer directives count
  **
  */
  
  public int numDirectives() {
    return (ldsList_.size());
  }    
 
  /***************************************************************************
  **
  ** Set the directives list
  **
  */
  
  public void setDirectives(List<LayoutDataSource> ldsList) {
    ldsList_.clear();
    int num = ldsList.size();
    for (int i = 0; i < num; i++) {
      ldsList_.add(ldsList.get(i).clone());
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Add a directive to the end
  **
  */
  
  public void addDirective(LayoutDataSource lds) {
    ldsList_.add(lds);
    return;
  }     
  
  /***************************************************************************
  **
  ** Remove directive at index
  **
  */
  
  public void removeDirective(int i) {
    ldsList_.remove(i);
    return;
  } 
  
  /***************************************************************************
  **
  ** Replace directive at index
  **
  */
  
  public void replaceDirective(int i, LayoutDataSource lds) {
    ldsList_.remove(i);
    ldsList_.add(i, lds);
    return;
  }   

  /***************************************************************************
  **
  ** Get the ith directive
  **
  */
  
  public LayoutDataSource getDirective(int i) {
    return (ldsList_.get(i));
  } 
  
  /***************************************************************************
  **
  ** Bump up the ith directive
  **
  */
  
  public void bumpDirectiveUp(int i) {
    LayoutDataSource selObj = ldsList_.remove(i);   
    ldsList_.add(i - 1, selObj);  
    return;
  }

  /***************************************************************************
  **
  ** Bump down the ith directive
  **
  */
  
  public void bumpDirectiveDown(int i) {
    LayoutDataSource selObj = ldsList_.remove(i);   
    ldsList_.add(i + 1, selObj);  
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we already have it
  **
  */
  
  public boolean containsExactDirective(LayoutDataSource lds) {
    return (ldsList_.contains(lds));
  } 
  
  /***************************************************************************
  **
  ** Answer if we already have it, ignoring transform info
  **
  */
  
  public boolean containsDirectiveModuloTransforms(LayoutDataSource lds) {
    int num = ldsList_.size();
    for (int i = 0; i < num; i++) {
      LayoutDataSource chkLds = ldsList_.get(i);
      if (chkLds.equalsExceptTransforms(lds)) {
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Answer if we are switching pads
  ** 
  */
  
  public boolean getSwitchPads() {
    return (swapPads_);
  }  
  
  /***************************************************************************
  **
  ** Answer if we are doing unique pads
  ** 
  */
  
  public boolean getForceUnique() {  
    return (forceUniquePads_);
  }  
  
  /***************************************************************************
  **
  ** Get the overlayOption
  ** 
  */
  
  public int getOverlayOption() {  
    return (overlayOption_);
  }    
  
  /***************************************************************************
  **
  ** Set if we are switching pads
  ** 
  */
  
  public void setSwitchPads(boolean swapPads) {
    swapPads_ = swapPads;
    return;
  }  
  
  /***************************************************************************
  **
  ** Set if we are doing unique pads
  ** 
  */
  
  public void setForceUnique(boolean forceUniquePads) {
    forceUniquePads_ = forceUniquePads;
    return;  
  }
  
  /***************************************************************************
  **
  ** Set the overlayOption
  ** 
  */
  
  public void setOverlayOption(int overlayOption) {  
    overlayOption_ = overlayOption;
    return;
  }  
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {

    ind.indent();
    out.print("<layoutDerivation");
    if (!swapPads_) {
      out.print(" swap=\"false\"");
    }
    if (!forceUniquePads_) {
      out.print(" unique=\"false\"");
    }
    out.print(" overlay=\"");
    out.print(NetOverlayProperties.mapToRelayoutTag(overlayOption_));
    out.print("\"");
    
    if (ldsList_.isEmpty()) {
      out.println("/>");
      return;
    } else {
      out.println(">");
    }
    ind.up();
    Iterator ldsit = ldsList_.iterator();
    while (ldsit.hasNext()) {
      LayoutDataSource lds = (LayoutDataSource)ldsit.next();
      lds.writeXML(out, ind);
    }

    ind.down().indent();
    out.println("</layoutDerivation>");
    return;
  }
  
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("layoutDerivation");
  }   
  
  /***************************************************************************
  **
  ** Handle creation from XML
  **
  */
  
  public static LayoutDerivation buildFromXML(Attributes attrs) throws IOException { 

    String swapPadsStr = "true";
    String forceUniquePadsStr = "true";
    String overlayStr = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("swap")) {
          swapPadsStr = val;
        } else if (key.equals("unique")) {
          forceUniquePadsStr = val;
        } else if (key.equals("overlay")) {
          overlayStr = val;
        } 
      }
    }
    
    boolean doSwap = Boolean.valueOf(swapPadsStr).booleanValue();
    boolean doUnique = Boolean.valueOf(swapPadsStr).booleanValue(); 
    
    // Legacy loads:
    int overOpt = NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES;
    if (overlayStr != null) {
      try { 
        overOpt = NetOverlayProperties.mapFromRelayoutTag(overlayStr);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
    LayoutDerivation ld = new LayoutDerivation(doSwap, doUnique, overOpt);
    return (ld);
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
