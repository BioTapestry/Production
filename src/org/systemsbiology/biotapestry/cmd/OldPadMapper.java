/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/***************************************************************************
**
** Holds information on old pad for a node and link
*/
  
public class OldPadMapper {
  
  private HashMap<String, PadPair> padsPerLink_;
  private HashMap<String, Integer> launchPerNode_;
  private HashMap<String, Set<Integer>> landingsPerNode_;  
  
  /***************************************************************************
  **
  ** Null Constructor
  **
  */
  
  public OldPadMapper() {
    padsPerLink_ = new HashMap<String, PadPair>();
    launchPerNode_ = new HashMap<String, Integer>();
    landingsPerNode_ = new HashMap<String, Set<Integer>>();      
  }  
   
  /***************************************************************************
  **
  ** Set the pad pair for the given node and link
  */
  
  public void setPadPair(String srcNodeID, String targNodeID, String linkID, int launch, int landing) {
    padsPerLink_.put(linkID, new PadPair(launch, landing));
    launchPerNode_.put(srcNodeID, new Integer(launch));
    Set<Integer> lands = landingsPerNode_.get(targNodeID);
    if (lands == null) {
      lands = new HashSet<Integer>();
      landingsPerNode_.put(targNodeID, lands);
    }
    lands.add(new Integer(landing));
    return;
  }
  
  /***************************************************************************
  **
  ** Get the launch pad for the given link
  */
  
  public int getLaunch(String linkID) {
    PadPair pair = padsPerLink_.get(linkID);
    if (pair == null) {
      throw new IllegalStateException();
    }
    return (pair.launchPad);
  }

  /***************************************************************************
  **
  ** Get the landing pad for the given link
  */
  
  public int getLanding(String linkID) {
    PadPair pair = padsPerLink_.get(linkID);
    if (pair == null) {
      throw new IllegalStateException();
    }
    return (pair.landingPad);
  }

  /***************************************************************************
  **
  ** Get the launch pad for the given node
  */
      
  public Integer getNodeLaunch(String nodeID) {
    return (launchPerNode_.get(nodeID));
  }
    
  /***************************************************************************
  **
  ** Get the used landing pads for the given node
  */    

  public Iterator<Integer> getNodeLandings(String nodeID) {
    Set<Integer> lands = landingsPerNode_.get(nodeID);
    if (lands == null) {
      return (null);
    } else {
      return (lands.iterator());
    }
  }
          
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Represents a launch/landing pad pair
  */
  
  private static class PadPair {

    public int launchPad;
    public int landingPad;

    public PadPair(int launchPad, int landingPad) {
      this.launchPad = launchPad;
      this.landingPad = landingPad;
    }
  } 
}
