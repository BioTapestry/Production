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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Support for sync layout processing
*/

public class SyncSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////    
   
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor:
  */ 
  
  public SyncSupport(DataAccessContext dacx) {
    dacx_ = dacx;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Synchronize all link pads.
  */  
 
  public void syncAllLinkPads(UndoSupport support, Map<String, Set<String>> groupsForModel) {
    Set<String> models = (groupsForModel == null) ? null : groupsForModel.keySet();
    Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance gi = iit.next();
      Genome rootGi = gi.getVfgParentRoot();
      Set<String> useGroups = null;
      if (rootGi == null) {
        rootGi = gi;
        useGroups = (groupsForModel == null) ? null : groupsForModel.get(rootGi.getID());
      }  
      if ((models == null) || models.contains(rootGi.getID())) {
        // Is we are working with groups, we need to generate correct IDs for child models:
        if ((models != null) && (useGroups == null)) {
          Set<String> needGroups = groupsForModel.get(rootGi.getID());
          int genCount = gi.getGeneration();
          useGroups = new HashSet<String>();
          Iterator<String> grit = needGroups.iterator();
          while (grit.hasNext()) {
            String groupID = grit.next();
            String baseGrpID = Group.getBaseID(groupID);   
            String inherit = Group.buildInheritedID(baseGrpID, genCount);
            useGroups.add(inherit);
          }
        }
        GenomeChange[] changes = gi.syncAllLinkagePads(useGroups);
        int numCh = changes.length;
        for (int i = 0; i < numCh; i++) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(changes[i]);
          support.addEdit(gcc); 
        }
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }
    }
    //
    // Ditch all the dynamic guys, since we have not modified them:
    //
    dacx_.getGenomeSource().clearAllDynamicProxyCaches();
    return;
  }

  /***************************************************************************
  **
  ** Synchronize root links pads from children
  */  
 
  public void upwardSyncAllLinkPads(DBGenome dbg, PadCalculatorToo.UpwardPadSyncData upsd, 
                                    UndoSupport support, boolean forcePads, Set<String> orphanedLinks) {
   
    PadCalculatorToo pcalc = new PadCalculatorToo();
    HashMap<String, Integer> launchDirections = new HashMap<String, Integer>();
    HashMap<String, Integer> landDirections = new HashMap<String, Integer>();
    pcalc.getDirections(upsd, dbg, launchDirections, landDirections, forcePads, orphanedLinks);
    
    GenomeChange[] gca = dbg.installPadChanges(launchDirections, landDirections);
    if ((support != null) && (gca != null)) {
      boolean doEvent = false;
      for (int i = 0; i < gca.length; i++) {
        if (gca[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(gca[i]);
          support.addEdit(gcc);
          doEvent = true;
        }              
      }
      if (doEvent) {
        support.addEvent(new ModelChangeEvent(dacx_.getGenomeSource().getID(), dbg.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
      }
    }
    return;
  } 
}
