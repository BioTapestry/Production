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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.systemsbiology.biotapestry.app.BTState;

import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Overlay Support
*/

public class OverlayOpsSupport implements Cloneable {   
   
  protected BTState appState_;
  protected Map<String, NetworkOverlay> netOverlays_;
  protected int ownerMode_;
  private String ownerID_;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public OverlayOpsSupport(BTState appState, int overlayOwnerMode, String ownerID) {
    ownerMode_ = overlayOwnerMode;
    appState_ = appState;
    netOverlays_ = new HashMap<String, NetworkOverlay>();
    ownerID_ = ownerID;
  }  

  /***************************************************************************
  **
  ** Copy constructor that is used to create a sibling.  Group IDs are modified
  ** and recorded in the provided map, as for other maps too
  */

  public OverlayOpsSupport(OverlayOpsSupport other) {
    this.appState_ = other.appState_;
 
    this.netOverlays_ = new HashMap<String, NetworkOverlay>();
    Iterator<String> nmit = other.netOverlays_.keySet().iterator();
    while (nmit.hasNext()) {
      String nmID = nmit.next();
      this.netOverlays_.put(nmID, other.netOverlays_.get(nmID).clone());
    }       
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  **
  ** When changing genomeID, this has to happen too:
  ** Part of Issue 195 Fix
  */
  
  public void resetOwner(String newOwner) {
    ownerID_ = newOwner;
    return;
  }
 
  /***************************************************************************
  **
  **
  ** Overlays and modules have globally unique IDs, so generate new IDs and track the mapping.
  **
  */

  public void copyWithMap(OverlayOpsSupport other, DBGenome rootGenome, Map<String, String> groupIDMap,
                          Map<String, String> ovrIDMap, Map<String, String> modIDMap, Map<String, String> modLinkIDMap) {
    // Part of Issue 195 Fix
    // Handled previously in constructor, and ID has actually been switched to new owner:
    // this.ownerMode_ = other.ownerMode_;
    // this.appState_ = other.appState_; 
    // this.ownerID_ = other.ownerID_;
    this.netOverlays_ = new HashMap<String, NetworkOverlay>();
    Iterator<String> noit = other.netOverlays_.keySet().iterator();
   
    while (noit.hasNext()) {
      String noID = noit.next();      
      String newOvID = rootGenome.getNextKey();
      NetworkOverlay nextOver = other.netOverlays_.get(noID);
      ovrIDMap.put(noID, newOvID);
      NetworkOverlay newCopy = new NetworkOverlay(nextOver, newOvID, groupIDMap, null, modIDMap, modLinkIDMap);
      netOverlays_.put(newOvID, newCopy);
    } 
    return;
  }
  
  
  /***************************************************************************
  **
  **  We need to retain all net overlays and modules and links, but we need to
  **  ditch members until they have been (maybe) rebuilt.
  */
  
  public OverlayOpsSupport getStrippedOverlayCopy() {
    
    OverlayOpsSupport retval = new OverlayOpsSupport(this.appState_, this.ownerMode_, this.ownerID_);
    Iterator<String> noit = this.netOverlays_.keySet().iterator();   
    while (noit.hasNext()) {
      String noID = noit.next();      
      NetworkOverlay nextOver = this.netOverlays_.get(noID);
      NetworkOverlay stripped = nextOver.getMemberStrippedOverlay();
      retval.netOverlays_.put(noID, stripped);
    }   
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the overlay map
  **
  */
  
  public Map<String, NetworkOverlay> getNetworkOverlayMap() {
    return (netOverlays_);
  }  
  
  /***************************************************************************
  **
  ** Add a new member to a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange addMemberToNetworkModule(String overlayKey, NetModule module, String nodeID) {
    NetModuleMember nmm = new NetModuleMember(nodeID);
    NetModuleChange nmc = module.addMember(nmm, getID(), ownerMode_, overlayKey);
    return (nmc);    
  }
  
  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome
  **
  */
  
  public NetworkOverlayChange addNetworkModule(String overlayKey, NetModule module) {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IllegalArgumentException();
    }
    NetworkOverlayChange noc = netOv.addNetModule(module, getID(), ownerMode_);
    return (noc);
  }
  
  /***************************************************************************
  **
  ** Recover network overlay module members and module group attachments
  */  
   
  public void recoverMappedModuleMembers(Genome oldGenome, Map<String, String> oldNodeToNew, Map<String, String> oldGroupToNew) {
    Iterator<String> noit = netOverlays_.keySet().iterator();   
    while (noit.hasNext()) {
      String noID = noit.next();      
      NetworkOverlay nextOver = this.netOverlays_.get(noID);
      NetworkOverlay oldOver = oldGenome.getNetworkOverlay(noID);
      nextOver.recoverMappedMembersAndGroups(oldOver, oldNodeToNew, oldGroupToNew);
    }
    return;
  }

  /***************************************************************************
  **
  ** Add a network module view to an overlay in this genome.  Use for IO.
  **
  */
  
  public void addNetworkModuleAndKey( String overlayKey, NetModule module) throws IOException {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IOException();
    }
    try {
      ((DBGenome)appState_.getDB().getGenome()).addKey(module.getID());
    } catch (IllegalStateException isex) {
      throw new IOException();
    }
    netOv.addNetModule(module, getID(), ownerMode_);
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome
  **
  */
  
  public NetworkOverlayChange addNetworkModuleLinkage(String overlayKey, NetModuleLinkage linkage) {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IllegalArgumentException();
    }
    NetworkOverlayChange noc = netOv.addNetModuleLinkage(linkage, getID(), ownerMode_);
    return (noc);
  }
  
  /***************************************************************************
  **
  ** Add a network module linkage to an overlay in this genome.  Use for IO.
  **
  */
  
  public void addNetworkModuleLinkageAndKey(String overlayKey, NetModuleLinkage linkage) throws IOException {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IOException();
    }
    try {
      ((DBGenome)appState_.getDB().getGenome()).addKey(linkage.getID());
    } catch (IllegalStateException isex) {
      throw new IOException();
    }
    netOv.addNetModuleLinkage(linkage, getID(), ownerMode_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a network module view to the genome
  **
  */
  
  public NetworkOverlayOwnerChange addNetworkOverlay(NetworkOverlay nmView) {
    String id = nmView.getID();
    if (netOverlays_.get(id) != null) {
      throw new IllegalArgumentException();
    }
    NetworkOverlayOwnerChange retval = new NetworkOverlayOwnerChange();
    netOverlays_.put(id, nmView);
    retval.nmvOrig = null;
    retval.nmvNew = nmView.clone();
    retval.ownerKey = getID();
    retval.ownerMode = ownerMode_;
    return (retval);
  }    
   
  /***************************************************************************
  **
  ** Add a network module view to the genome.  Use for IO
  **
  */
    
  public void addNetworkOverlayAndKey(NetworkOverlay nmView) throws IOException {
    String id = nmView.getID();
    if (netOverlays_.get(id) != null) {
      throw new IOException();
    }
    try {
      ((DBGenome)appState_.getDB().getGenome()).addKey(id);
    } catch (IllegalStateException iaex) {
      throw new IOException();
    }
    netOverlays_.put(id, nmView);
    return;
  }

  /***************************************************************************
  **
  ** Return info on modules that the node belongs to. (Net Overlay keys map to 
  ** sets of matching module keys in return map)
  **
  */

  public Map<String, Set<String>> getModuleMembership(String nodeID) {
    HashMap <String, Set<String>> retval = new HashMap <String, Set<String>>(); 
    Iterator<NetworkOverlay> oit = getNetworkOverlayIterator();
    while (oit.hasNext()) {
      NetworkOverlay no = oit.next();
      String noID = no.getID();
      Iterator<NetModule> nmit = no.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        String nmID = nmod.getID();
        if (nmod.isAMember(nodeID)) {
          Set<String> forOvr = retval.get(noID);
          if (forOvr == null) {
            forOvr = new HashSet<String>();
            retval.put(noID, forOvr);
          }
          forOvr.add(nmID);
        }
      }
    }
    return (retval);
  }
      
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public OverlayOpsSupport clone() {
    try {
      OverlayOpsSupport retval = (OverlayOpsSupport)super.clone();
      retval.netOverlays_ = new HashMap<String, NetworkOverlay>();
      Iterator<String> nmit = this.netOverlays_.keySet().iterator();
      while (nmit.hasNext()) {
        String nmID = nmit.next();
        retval.netOverlays_.put(nmID, this.netOverlays_.get(nmID).clone());
      }      
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    

  /***************************************************************************
  **
  ** Remove a member from a network module of an overlay in this genome
  **
  */
  
  public NetModuleChange deleteMemberFromNetworkModule(String overlayKey, NetModule module, String nodeID) {
    NetModuleChange nmc = module.removeMember(nodeID, getID(), ownerMode_, overlayKey);
    return (nmc);    
  }
 
  
 /***************************************************************************
 **
 ** Return matching Network Modules (Net Overlay keys map to sets of matching module keys in return map)
 **
 */

  public Map<String, Set<String>> findMatchingNetworkModules(int searchMode, String key, NameValuePair nvPair) {
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    
    Iterator<String> nokit = netOverlays_.keySet().iterator();
    while (nokit.hasNext()) {
      String noKey = nokit.next();
      NetworkOverlay novr = netOverlays_.get(noKey);
      HashSet<String> retSet = null;
      Iterator<NetModule> nmit = novr.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        if (searchMode == NetOverlayOwner.MODULE_BY_NAME_VALUE) {
          NameValuePairList nvpl = nmod.getNVPairs();
          Iterator<NameValuePair> nvit = nvpl.getIterator();
          while (nvit.hasNext()) {
            NameValuePair nvp = nvit.next();
            String name = nvp.getName();
            String value = nvp.getValue();
            if (DataUtil.keysEqual(name, nvPair.getName()) && DataUtil.keysEqual(value, nvPair.getValue())) {
              if (retSet == null) {
                retSet = new HashSet<String>();
                retval.put(noKey, retSet);
              }
              retSet.add(nmod.getID());
            }
          }
        } else {
          Iterator<String> ti = nmod.getTagIterator();
          while (ti.hasNext()) {
            String tag = ti.next();
            if (DataUtil.keysEqual(tag, key)) {
              if (retSet == null) {
                retSet = new HashSet<String>();
                retval.put(noKey, retSet);
              }
              retSet.add(nmod.getID());
            }
          }
        }
      }
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the id
  **
  */
  
  public String getID() {
    return (ownerID_);
  }
  
  /***************************************************************************
  **
  ** Get the count of network modules
  **
  */
  
  public int getNetworkModuleCount() {
    int count = 0;
    Iterator<String> nokit = netOverlays_.keySet().iterator();
    while (nokit.hasNext()) {
      String noKey = nokit.next();
      NetworkOverlay novr = netOverlays_.get(noKey);
      Iterator<NetModule> nmit = novr.getModuleIterator();
      while (nmit.hasNext()) {
        nmit.next();
        count++;
      }
    }
    return (count);
  }
    
  /***************************************************************************
  **
  ** Get a network overlay from the genome
  **
  */
  
  public NetworkOverlay getNetworkOverlay(String key) {
    return (netOverlays_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get the count of network overlays
  **
  */
  
  public int getNetworkOverlayCount() {    
    return (netOverlays_.size());
  }

  /***************************************************************************
  **
  ** Get an iterator over network overlays
  **
  */
  
  public Iterator<NetworkOverlay> getNetworkOverlayIterator() {
    return (netOverlays_.values().iterator());
  }

  /***************************************************************************
  **
  ** Fills in the given XPlatTextSummary with overlay and module display text
  */
  
  public void getAllDisplayText(XPlatDisplayText fillIn) { 
    Iterator<String> nokit = netOverlays_.keySet().iterator();
    while (nokit.hasNext()) {
      String noKey = nokit.next();
      NetworkOverlay novr = netOverlays_.get(noKey);
      String desc = novr.getDescription();      
      if ((desc != null) && !desc.trim().equals("")) { 
        fillIn.setOverlayText(noKey, desc);
      }
      Iterator<NetModule> nmit = novr.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();      
        desc = nmod.getDescription();
        if ((desc != null) && !desc.trim().equals("")) { 
          String modID = nmod.getID();
          NetModule.FullModuleKey fullKey = new NetModule.FullModuleKey(ownerMode_, ownerID_, noKey, modID);
          fillIn.setModuleText(fullKey,desc);
        }   
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Modify a network module linkage in an overlay in this genome.
  **
  */
  
  public NetworkOverlayChange modifyNetModuleLinkage(String overlayKey, String linkKey, int newSign) {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IllegalArgumentException();
    }
    NetworkOverlayChange noc = netOv.modifyNetModuleLinkage(linkKey, getID(), ownerMode_, newSign);
    return (noc);
  }  
      
  /***************************************************************************
  **
  ** Redo an overlay change
  **
  */
  
  public void overlayChangeRedo(NetworkOverlayOwnerChange redo) {    
    if ((redo.nmvOrig != null) && (redo.nmvNew != null)) {
      throw new IllegalArgumentException();
    } else if ((redo.nmvOrig == null) && (redo.nmvNew != null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(redo.nmvNew.getID());
      netOverlays_.put(redo.nmvNew.getID(), redo.nmvNew.clone());
    } else if ((redo.nmvOrig != null) && (redo.nmvNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).removeKey(redo.nmvOrig.getID());
      netOverlays_.remove(redo.nmvOrig.getID());
    } else {
   //   if (redo.nameChange) {
    //    name_ = redo.nameNew;
  //    }
    //  if (redo.descChange) {
   //     desc_ = redo.descNew;
  //    }
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Undo an overlay change
  **
  */
  
  public void overlayChangeUndo(NetworkOverlayOwnerChange undo) {
    if ((undo.nmvOrig != null) && (undo.nmvNew != null)) {
      throw new IllegalArgumentException();
    } else if ((undo.nmvOrig == null) && (undo.nmvNew != null)) {     
      ((DBGenome)appState_.getDB().getGenome()).removeKey(undo.nmvNew.getID());
      netOverlays_.remove(undo.nmvNew.getID());
    } else if ((undo.nmvOrig != null) && (undo.nmvNew == null)) {
      ((DBGenome)appState_.getDB().getGenome()).addKey(undo.nmvOrig.getID());
      netOverlays_.put(undo.nmvOrig.getID(), undo.nmvOrig.clone());
    } else { 
    //  if (undo.nameChange) {
    //    name_ = undo.nameOrig;
    //  }
     // if (undo.descChange) {
     //   desc_ = undo.descOrig;
     // }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the overlay mode
  **
  */
  
  public int overlayModeForOwner() {
    return (ownerMode_);
  }
  
  /***************************************************************************
  **
  ** Remove a network module from an overlay in this genome
  **
  */
  
  public NetworkOverlayChange[] removeNetworkModule(String overlayKey, String moduleKey) {  
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IllegalArgumentException();
    }
    ((DBGenome)appState_.getDB().getGenome()).removeKey(moduleKey);
    NetworkOverlayChange[] noc = netOv.removeNetModule(moduleKey, getID(), ownerMode_);
    return (noc);    
  }
  
  /***************************************************************************
  **
  ** Remove a network module linkage from an overlay in this genome.
  **
  */
  
  public NetworkOverlayChange removeNetworkModuleLinkage(String overlayKey, String linkKey) {
    NetworkOverlay netOv = netOverlays_.get(overlayKey);
    if (netOv == null) {
      throw new IllegalArgumentException();
    }
    ((DBGenome)appState_.getDB().getGenome()).removeKey(linkKey);
    NetworkOverlayChange noc = netOv.removeNetModuleLinkage(linkKey, getID(), ownerMode_);
    return (noc);
  }
  
  /***************************************************************************
  **
  ** Remove the network module view from the genome
  **
  */
  
  public NetworkOverlayOwnerChange removeNetworkOverlay(String key) {
    NetworkOverlayOwnerChange retval = new NetworkOverlayOwnerChange();
    retval.nmvOrig = netOverlays_.get(key).clone();
    netOverlays_.remove(key);
    retval.nmvNew = null;
    ((DBGenome)appState_.getDB().getGenome()).removeKey(key);
    retval.ownerKey = getID();
    retval.ownerMode = ownerMode_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Write the network overlays to XML
  **
  */
  
  public void writeOverlaysToXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<netOverlays>"); 
    TreeSet<String> sorted = new TreeSet<String>(this.netOverlays_.keySet());
    Iterator<String> kit = sorted.iterator();
    ind.up();
    while (kit.hasNext()) {
      String key = kit.next();
      NetworkOverlay netOv = netOverlays_.get(key);
      netOv.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</netOverlays>"); 
    return;
  }
  
  /***************************************************************************
  **
  ** Get the firstView preference
  **
  */   
  
  public String getFirstViewPreference(TaggedSet modChoice, TaggedSet revChoice) {
    Iterator<String> nokit = netOverlays_.keySet().iterator();
    while (nokit.hasNext()) {
      String noKey = nokit.next();
      NetworkOverlay no = netOverlays_.get(noKey);
      TaggedSet fvs = new TaggedSet();
      TaggedSet fvr = new TaggedSet();
      boolean isFirst = no.getFirstViewState(fvs, fvr);
      if (isFirst) {
        modChoice.tag = fvs.tag;
        modChoice.set.clear();
        modChoice.set.addAll(fvs.set);
        revChoice.tag = fvr.tag;
        revChoice.set.clear();
        revChoice.set.addAll(fvr.set);        
        return (no.getID());
      }
    }
    return (null);
  }
  
  /***************************************************************************
   **
   ** Fill in map needed to extract overlay properties for layout extraction
   ** for the given group
   */  
   
   public void fillMapsForGroupExtraction(String grpID, Map<NetModule.FullModuleKey, NetModule.FullModuleKey> keyMap) {
     Iterator<String> noit = netOverlays_.keySet().iterator();   
     while (noit.hasNext()) {
       String noID = noit.next();      
       NetworkOverlay nextOver = netOverlays_.get(noID);
       nextOver.fillMapForGroupExtraction(grpID, keyMap, overlayModeForOwner(), getID());
     }
     return;
   }
   
   /***************************************************************************
    **
    ** Get the FullKeys for all the modules attached to the given group
    ** for the given group
    */  
    
    public void getModulesAttachedToGroup(String grpID, Set<NetModule.FullModuleKey> attached) {
      Iterator<String> noit = netOverlays_.keySet().iterator();   
      while (noit.hasNext()) {
        String noID = noit.next();      
        NetworkOverlay nextOver = netOverlays_.get(noID);
        nextOver.getModulesAttachedToGroup(grpID, ownerMode_, getID(), attached);
      }
      return;
    } 
    
  /***************************************************************************
  **
  ** Used by subclasses
  */
     
   public Map<String, Set<String>> findModulesOwnedByGroupGuts(String groupID, Map<String, NetworkOverlay> otherOver) {
     HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();    
     Iterator<String> nokit = otherOver.keySet().iterator();
     while (nokit.hasNext()) {
       String noKey = nokit.next();
       NetworkOverlay novr = otherOver.get(noKey);
       HashSet<String> retSet = null;
       Iterator<NetModule> nmit = novr.getModuleIterator();
       while (nmit.hasNext()) {
         NetModule nmod = nmit.next();
         String grp = nmod.getGroupAttachment();
         if ((grp != null) && grp.equals(groupID)) {
           if (retSet == null) {
             retSet = new HashSet<String>();
             retval.put(noKey, retSet);
           }
           retSet.add(nmod.getID());
         }
       }
     }    
     return (retval);
   }
   
}