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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This represents an owner of network overlays in BioTapestry
*/

public interface NetOverlayOwner {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Module Search modes:
  **
  */
  
  public static final int MODULE_BY_KEY        = 0;
  public static final int MODULE_BY_NAME_VALUE = 1;    
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the id
  **
  */
  
  public String getID(); 
  
 
  /***************************************************************************
  **
  ** Get the overlay mode
  **
  */
  
  public int overlayModeForOwner();
  
   
  /***************************************************************************
  **
  ** Add a network overlay to the owner
  **
  */
  
  public NetworkOverlayOwnerChange addNetworkOverlay(NetworkOverlay nmView);
  
  /***************************************************************************
  **
  ** Add a network overlay to the owner.  Use for IO.
  **
  */
  
  public void addNetworkOverlayAndKey(NetworkOverlay nmView) throws IOException;  
  
  /***************************************************************************
  **
  ** Remove a network overlay from the genome
  **
  */
  
  public NetworkOverlayOwnerChange removeNetworkOverlay(String key);

  /***************************************************************************
  **
  ** Get a network overlay from the genome
  **
  */
  
  public NetworkOverlay getNetworkOverlay(String key);
  
  /***************************************************************************
  **
  ** Get an iterator over network overlays
  **
  */
  
  public Iterator<NetworkOverlay> getNetworkOverlayIterator();  
  
  /***************************************************************************
  **
  ** Get the count of network overlays
  **
  */
  
  public int getNetworkOverlayCount();   
  
  /***************************************************************************
  **
  ** Get the count of network modules
  **
  */
  
  public int getNetworkModuleCount();     
    
  /***************************************************************************
  **
  ** Add a network module to an overlay in this owner
  **
  */
  
  public NetworkOverlayChange addNetworkModule(String overlayKey, NetModule module);
  
  /***************************************************************************
  **
  ** Add a network module to an overlay in this owner.  Use for IO
  **
  */
  
  public void addNetworkModuleAndKey(String overlayKey, NetModule module) throws IOException;  
  

  /***************************************************************************
  **
  ** Remove a network module from an overlay in this owner
  **
  */
  
  public NetworkOverlayChange[] removeNetworkModule(String overlayKey, String moduleKey);
 
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this owner.  Use for IO
  **
  */
  
  public void addNetworkModuleLinkageAndKey(String overlayKey, NetModuleLinkage linkage) throws IOException;  
    
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this owner.
  **
  */
  
  public NetworkOverlayChange addNetworkModuleLinkage(String overlayKey, NetModuleLinkage linkage);  
 
  /***************************************************************************
  **
  ** Remove a network module linkage from an overlay in this owner.
  **
  */
  
  public NetworkOverlayChange removeNetworkModuleLinkage(String overlayKey, String linkageKey);  
  
  /***************************************************************************
  **
  ** Modify a network module linkage in an overlay in this genome.
  **
  */
  
  public NetworkOverlayChange modifyNetModuleLinkage(String overlayKey, String linkKey, int newSign);
  
  /***************************************************************************
  **
  ** Add a new member to a network module of an overlay in this owner
  **
  */
  
  public NetModuleChange addMemberToNetworkModule(String overlayKey, NetModule module, String nodeID);  
  
  /***************************************************************************
  **
  ** Remove a member from a network module of an overlay in this owner
  **
  */
  
  public NetModuleChange deleteMemberFromNetworkModule(String overlayKey, NetModule module, String nodeID);  
 
  /***************************************************************************
  **
  ** Return matching Network Modules (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
  
  public Map<String, Set<String>> findMatchingNetworkModules(int searchMode, String key, NameValuePair nvPair);  
 
  
  /***************************************************************************
  **
  ** Return network modules that a node belongs to (Net Overlay keys map to sets of matching module keys in return map)
  **
  */
    
  public Map<String, Set<String>> getModuleMembership(String nodeID);
 
  /***************************************************************************
  **
  ** Get the firstView preference
  **
  */   
  
  public String getFirstViewPreference(TaggedSet modChoice, TaggedSet revChoice);
  
  /***************************************************************************
  **
  ** Need to know groups for submodel rendering exclusion
  **
  */   
  
  public Set<String> getGroupsForOverlayRendering();
    
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void overlayChangeUndo(NetworkOverlayOwnerChange undo);
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void overlayChangeRedo(NetworkOverlayOwnerChange undo);
  
}
