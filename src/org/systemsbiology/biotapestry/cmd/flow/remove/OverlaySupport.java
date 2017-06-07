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


package org.systemsbiology.biotapestry.cmd.flow.remove;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.AddCommands;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetModuleLinkage;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;
import org.systemsbiology.biotapestry.nav.UserTreePathChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Collection of commands to delete things
*/

public class OverlaySupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor.  Do not use.  This thing is now stateless, so make
  ** everything static for the time being during command refactoring.
  */ 
  
  private OverlaySupport() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  
  /***************************************************************************
  **
  ** Delete the network module linkages in the set
  */  
  
  public static boolean deleteNetworkModuleLinkageSet(DataAccessContext rcx, String ovrKey, Set<String> linkIDs, UndoSupport support) {
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID()); 
    
    boolean retval = false;
    Iterator<String> resit = linkIDs.iterator();
    while (resit.hasNext()) {
      String nextDeadID = resit.next();
      NetworkOverlayChange gc = owner.removeNetworkModuleLinkage(ovrKey, nextDeadID);
      if (gc != null) {
        NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, gc);
        support.addEdit(gcc);
        Layout.PropChange pc = rcx.getCurrentLayout().removeNetModuleLinkageProperties(nextDeadID, ovrKey, rcx);
        if (pc != null) {
          support.addEdit(new PropChangeCmd(rcx, pc));
          retval = true;
        }
      }
    }              
    support.addEvent(new LayoutChangeEvent(rcx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));      
    support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    return (retval);
  }
  
  /***************************************************************************
   **
   ** Delete the specified network module
   */  
  
   public static void deleteNetworkModule(UIComponentSource uics, DataAccessContext rcx, String ovrKey, String modKey, UndoSupport support) {
     NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID());
     
     UserTreePathController utpc = uics.getPathController();
     UserTreePathChange[] chgs = utpc.dropOrChangeStopsOnModules(rcx.getCurrentGenomeID(), ovrKey, modKey);
     for (int i = 0; i < chgs.length; i++) {
       UserTreePathChangeCmd cmd = new UserTreePathChangeCmd(rcx, chgs[i]);
       support.addEdit(cmd);
     }

     Layout.PropChange pc = rcx.getCurrentLayout().removeNetModuleProperties(modKey, ovrKey);
     if (pc != null) {
       support.addEdit(new PropChangeCmd(rcx, pc));
     }  
        
     NetworkOverlayChange[] gc = owner.removeNetworkModule(ovrKey, modKey);
     if ((gc != null) && (gc.length > 0)) {
       for (int i = 0; i < gc.length; i++) {
         NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, gc[i]);
         support.addEdit(gcc);
       }
     }
     
     //
     // Delete all the links to and from the dead module:
     //
     
     NetworkOverlay nov = owner.getNetworkOverlay(ovrKey);
     HashSet<String> linkIDs = new HashSet<String>();
     Iterator<NetModuleLinkage> lit = nov.getLinkageIterator();
     while (lit.hasNext()) {
       NetModuleLinkage nlm = lit.next();
       if (modKey.equals(nlm.getSource()) || modKey.equals(nlm.getTarget())) {
         linkIDs.add(nlm.getID());
       }
     }    
     deleteNetworkModuleLinkageSet(rcx, ovrKey, linkIDs, support);
  
     support.addEvent(new LayoutChangeEvent(rcx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));      
     support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
     return;
   } 
  

 
  /***************************************************************************
  **
  ** Delete the specified network module region
  */  
 
  public static void deleteNetworkModuleRegion(StaticDataAccessContext rcx, String ovrKey, String modKey, 
                                               NetModuleFree.IntersectionExtraInfo iexi, UndoSupport support) {    
    //
    // Removing a region may mess up existing net module linkages.  Record exisiting
    // state before changing anything:
    //

    Layout.PadNeedsForLayout padFixups = rcx.getCurrentLayout().findAllNetModuleLinkPadRequirements(rcx);   
         
    NetOverlayProperties noProps = rcx.getCurrentLayout().getNetOverlayProperties(ovrKey);
    NetModuleProperties nmp = noProps.getNetModuleProperties(modKey);
    NetModuleProperties modProps = nmp.clone();
    if (!modProps.removeBiggestShape(iexi)) {
      return;
    }
   
    Layout.PropChange pc = rcx.getCurrentLayout().replaceNetModuleProperties(modKey, modProps, ovrKey);
    if (pc != null) {
      support.addEdit(new PropChangeCmd(rcx, pc));
    }
    
    //
    // Module link pad fixups:
    //
    
    AddCommands.finishNetModPadFixups(null, null, rcx, padFixups, support);
    
    support.addEvent(new LayoutChangeEvent(rcx.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));      
    support.finish();    
    return;
  }   

  /***************************************************************************
  **
  ** Delete a node from a network module
  */  
 
  public static boolean deleteNodeFromModule(UIComponentSource uics, StaticDataAccessContext rcx, String nodeID, String moduleID, String overlayKey, UndoFactory uFac) {
   
    //
    // Changing module geometry may need module link pad fixups:
    //
    
    Layout.PadNeedsForLayout padFixups = rcx.getCurrentLayout().findAllNetModuleLinkPadRequirements(rcx);    
    NetOverlayOwner owner = rcx.getGenomeSource().getOverlayOwnerFromGenomeKey(rcx.getCurrentGenomeID());
    NetworkOverlay nov = owner.getNetworkOverlay(overlayKey);
    NetModule nmod = nov.getModule(moduleID);    
    
    //
    // Cannot delete the node if the module is auto node and the 
    // node is the last one!
    //
    
    if (nmod.getMemberCount() == 1) {
      NetOverlayProperties noProps = rcx.getCurrentLayout().getNetOverlayProperties(overlayKey);
      NetModuleProperties nmp = noProps.getNetModuleProperties(moduleID);
      if (nmp.getType() == NetModuleProperties.MEMBERS_ONLY) {
        JOptionPane.showMessageDialog(uics.getTopFrame(), rcx.getRMan().getString("deleteNodefromMod.lastOne"), 
                                      rcx.getRMan().getString("deleteNodefromMod.lastOneTitle"),
                                      JOptionPane.ERROR_MESSAGE);

        return (false);
      }
    }
    
    UndoSupport support = uFac.provideUndoSupport("undo.deleteNodeFromNetworkModule", rcx);
    
    
    NetModuleChange nmc = owner.deleteMemberFromNetworkModule(overlayKey, nmod, nodeID);
    if (nmc != null) {
      NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcx, nmc);
      support.addEdit(gcc);
    }    
        
    //
    // Complete the fixups:
    //
    
    AddCommands.finishNetModPadFixups(null, null, rcx, padFixups, support);        
    
    support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));   
    support.finish();    
    return (true);
  }     
}
