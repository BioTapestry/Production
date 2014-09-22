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

package org.systemsbiology.biotapestry.cmd;

import java.util.HashSet;
import java.util.Iterator;

/***************************************************************************
**
** Constrains pads for new links
*/
  
public class PadConstraints {
  
  private boolean areForced_;
  private int forcedLaunch_;
  private int forcedLanding_;
  private HashSet<Integer> usedLaunches_;
  private HashSet<Integer> usedLandings_;
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public PadConstraints() {
  }  
   
  /***************************************************************************
  **
  ** Answer if the pads are forced.
  */
  
  public boolean arePadsForced() {
    return (areForced_);
  }

  /***************************************************************************
  **
  ** Get the forced launch pad
  */
  
  public int getForcedLaunch() {
    if (!areForced_) {
      throw new IllegalStateException();
    }
    return (forcedLaunch_);
  }
  
  /***************************************************************************
  **
  ** Get the forced landing pad
  */
  
  public int getForcedLanding() {
    if (!areForced_) {
      throw new IllegalStateException();
    }
    return (forcedLanding_);
  }
  
  /***************************************************************************
  **
  ** Set the forced pads
  */
  
  public void setForcedPads(int launchPad, int landingPad) {
    if ((usedLaunches_ != null) || (usedLandings_ != null)) {
      throw new IllegalStateException();
    }
    forcedLaunch_ = launchPad;
    forcedLanding_ = landingPad;
    areForced_ = true;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the used landing pads.  May be null!
  */
  
  public Iterator<Integer> getUsedLandingPads() {
    if (areForced_) {
      throw new IllegalStateException();
    }
    if (usedLandings_ == null) {
      return (null);
    }
    return (usedLandings_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the used launch pads.  May be null!
  */
  
  public Iterator<Integer> getUsedLaunchPads() {
    if (areForced_) {
      throw new IllegalStateException();
    }
    if (usedLaunches_ == null) {
      return (null);
    }
    return (usedLaunches_.iterator());
  }  

  /***************************************************************************
  **
  ** Add a used landing pad.
  */
  
  public void addUsedLandingPad(int pad) {
    if (areForced_) {
      throw new IllegalStateException();
    }
    if (usedLandings_ == null) {
      usedLandings_ = new HashSet<Integer>();
    }
    usedLandings_.add(new Integer(pad));
    return;
  }
  
  /***************************************************************************
  **
  ** Add a used launch pad.
  */
  
  public void addUsedLaunchPad(int pad) {
    if (areForced_) {
      throw new IllegalStateException();
    }
    Integer newPad = new Integer(pad);
    if (usedLaunches_ == null) {
      usedLaunches_ = new HashSet<Integer>();
      usedLaunches_.add(newPad);
    } else if (!usedLaunches_.contains(newPad)) {
      throw new IllegalStateException();
    }
    return;
  }
}
