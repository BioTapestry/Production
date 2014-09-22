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

package org.systemsbiology.biotapestry.event;

import java.util.HashSet;
import java.util.Iterator;

/****************************************************************************
**
** Event Manager.  Not currently thread-safe.
*/

public class EventManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashSet<LayoutChangeListener> layoutListeners_;
  private HashSet<ModelChangeListener> modelListeners_;
  private HashSet<SelectionChangeListener> selectListeners_;
  private HashSet<GeneralChangeListener> generalListeners_;
  private HashSet<OverlayDisplayChangeListener> overlayListeners_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public EventManager() {
     layoutListeners_ = new HashSet<LayoutChangeListener>();
     modelListeners_ = new HashSet<ModelChangeListener>();
     selectListeners_ = new HashSet<SelectionChangeListener>();
     generalListeners_ = new HashSet<GeneralChangeListener>();    
     overlayListeners_ = new HashSet<OverlayDisplayChangeListener>(); 
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

 
  /***************************************************************************
  ** 
  ** Add an event listener
  */

  public void addLayoutChangeListener(LayoutChangeListener lcl) {
    layoutListeners_.add(lcl);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Remove an event listener
  */

  public void removeLayoutChangeListener(LayoutChangeListener lcl) {
    layoutListeners_.remove(lcl);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendLayoutChangeEvent(LayoutChangeEvent lcev) {
    Iterator<LayoutChangeListener> lclit = layoutListeners_.iterator();
    while (lclit.hasNext()) {
      LayoutChangeListener lcl = lclit.next();
      lcl.layoutHasChanged(lcev);
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** Add an event listener
  */

  public void addGeneralChangeListener(GeneralChangeListener gcl) {
    generalListeners_.add(gcl);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Remove an event listener
  */

  public void removeGeneralChangeListener(GeneralChangeListener gcl) {
    generalListeners_.remove(gcl);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendGeneralChangeEvent(GeneralChangeEvent gcev) {
    Iterator<GeneralChangeListener> gclit = generalListeners_.iterator();
    while (gclit.hasNext()) {
      GeneralChangeListener gcl = gclit.next();
      gcl.generalChangeOccurred(gcev);
    }
    return;
  }  

  /***************************************************************************
  ** 
  ** Add an event listener
  */

  public void addModelChangeListener(ModelChangeListener mcl) {
    modelListeners_.add(mcl);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Remove an event listener
  */

  public void removeModelChangeListener(ModelChangeListener mcl) {
    modelListeners_.remove(mcl);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendModelChangeEvent(ModelChangeEvent mcev) {
    Iterator<ModelChangeListener> mclit = modelListeners_.iterator();
    while (mclit.hasNext()) {
      ModelChangeListener mcl = mclit.next();
      mcl.modelHasChanged(mcev);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendModelChangeEvent(ModelChangeEvent mcev, int remaining) {
    Iterator<ModelChangeListener> mclit = modelListeners_.iterator();
    while (mclit.hasNext()) {
      ModelChangeListener mcl = mclit.next();
      mcl.modelHasChanged(mcev, remaining);
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Add an event listener
  */

  public void addSelectionChangeListener(SelectionChangeListener scl) {
    selectListeners_.add(scl);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Remove an event listener
  */

  public void removeSelectionChangeListener(SelectionChangeListener scl) {
    selectListeners_.remove(scl);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendSelectionChangeEvent(SelectionChangeEvent scev) {
    Iterator<SelectionChangeListener> sclit = selectListeners_.iterator();
    while (sclit.hasNext()) {
      SelectionChangeListener scl = sclit.next();
      scl.selectionHasChanged(scev);
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Add an event listener
  */

  public void addOverlayDisplayChangeListener(OverlayDisplayChangeListener odcl) {
    overlayListeners_.add(odcl);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Remove an event listener
  */

  public void removeOverlayDisplayChangeListener(OverlayDisplayChangeListener odcl) {
    overlayListeners_.remove(odcl);
    return;
  }  
 
  /***************************************************************************
  ** 
  ** Ask for an event to be sent.
  */

  public void sendOverlayDisplayChangeEvent(OverlayDisplayChangeEvent odcev) {
    Iterator<OverlayDisplayChangeListener> odclit = overlayListeners_.iterator();
    while (odclit.hasNext()) {
      OverlayDisplayChangeListener odcl = odclit.next();
      odcl.overlayDisplayChangeOccurred(odcev);
    }
    return;
  }  
  
  
}
