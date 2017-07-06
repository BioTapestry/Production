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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Support for removal operations
*/

public class RemoveSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

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
  
  private RemoveSupport() {
  }

  /***************************************************************************
  **
  ** Helper for delete warnings
  */
  
  public static SimpleUserFeedback deleteWarningHelperNew(StaticDataAccessContext rcx) {
    boolean gotRootInstructions = rcx.getInstructSrc().haveBuildInstructions();
    boolean isRoot = rcx.currentGenomeIsRootDBGenome();
    boolean showRootDialog = false;
    boolean showNonInstructDialog = false;
    boolean showInstructDialog = false;
    ResourceManager rMan = rcx.getRMan();
    if (isRoot) {
      showRootDialog = gotRootInstructions;
    } else if (gotRootInstructions) {
      InstanceInstructionSet iis = rcx.getInstructSrc().getInstanceInstructionSet(rcx.getCurrentGenomeID());
      if (iis == null) {
        showNonInstructDialog = true;
      } else {
        showInstructDialog = true;
      }
    }
    if (showRootDialog || showInstructDialog) {
      return (new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, rMan.getString("instructWarning.deleteMessageI"), rMan.getString("instructWarning.deleteTitle")));        
    } else if (showNonInstructDialog) { 
      return (new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, rMan.getString("instructWarning.deleteMessageNI"), rMan.getString("instructWarning.deleteTitle")));     
    }
    return (null);
  }
    
    
  /***************************************************************************
  **
  ** Helper for delete warnings
  */
  
  public static void deleteWarningHelper(UIComponentSource uics, StaticDataAccessContext rcx) {
    boolean gotRootInstructions = rcx.getInstructSrc().haveBuildInstructions();
    boolean isRoot = rcx.currentGenomeIsRootDBGenome();
    boolean showRootDialog = false;
    boolean showNonInstructDialog = false;
    boolean showInstructDialog = false;
    if (isRoot) {
      showRootDialog = gotRootInstructions;
    } else if (gotRootInstructions) {
      InstanceInstructionSet iis = rcx.getInstructSrc().getInstanceInstructionSet(rcx.getCurrentGenomeID());
      if (iis == null) {
        showNonInstructDialog = true;
      } else {
        showInstructDialog = true;
      }
    }
    ResourceManager rMan = rcx.getRMan();
    if (showRootDialog || showInstructDialog) {
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("instructWarning.deleteMessageI"), 
                                    rMan.getString("instructWarning.deleteTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    } else if (showNonInstructDialog) { 
      JOptionPane.showMessageDialog(uics.getTopFrame(), 
                                    rMan.getString("instructWarning.deleteMessageNI"), 
                                    rMan.getString("instructWarning.deleteTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    return;
  }
   
  /***************************************************************************
  **
  ** User data choice
  */
        
  public static class UserDataChoice  {
    
    public enum Delete {
      SINGLE_DELETE,
      ASK_FOR_ALL,
      PREVIOUS_NO,
      PREVIOUS_YES,
      OFFLINE_NO;
    }
   
    public enum Decision {
      NO_DELETION,
      NO_DECISION, 
      YES_FOR_ALL,  
      NO_FOR_ALL;  
    }
 
    public Decision decide;  
    public boolean deleteUnderlyingTables;
     
    UserDataChoice(Decision decide, boolean deleteUnderlyingTables) {
      this.decide = decide;
      this.deleteUnderlyingTables = deleteUnderlyingTables;
    }   
  } 

  /***************************************************************************
  **
  ** Used to return link usage results
  */
  
  public static class DataDeleteQueryState { 
    
    public enum NextStep {GO_DELETE,
                          CHECK_DATA_DELETE,
                          REGISTER_DATA_DELETE,
                         };
   
    public UserDataChoice.Delete multiDelete;
    public Map<String, Boolean> dataDelete;
    public int deadCheck;
    public NextStep nextStep;
    public List<String> deadList;
    public DialogAndInProcessCmd retval;
   
    public DataDeleteQueryState(List<String> deadList, DialogAndInProcessCmd daipc) {
      this.multiDelete = (deadList.size() == 1) ? UserDataChoice.Delete.SINGLE_DELETE : UserDataChoice.Delete.ASK_FOR_ALL;
      this.dataDelete = new HashMap<String, Boolean>();
      this.deadCheck = 0;
      this.nextStep = NextStep.CHECK_DATA_DELETE;
      this.deadList = new ArrayList<String>(deadList);
      this.retval = daipc;
    }
     
    public DataDeleteQueryState(List<String> deadList, DialogAndInProcessCmd daipc, boolean isRoot) {
      this.multiDelete = null;
      this.dataDelete = null;
      this.deadCheck = 0;
      this.nextStep = NextStep.GO_DELETE;
      this.deadList = new ArrayList<String>(deadList);
      this.retval = daipc;
    }
  }

  /***************************************************************************
  **
  ** Do the step
  */ 
     
  public static void stepToCheckDataDelete(StaticDataAccessContext rcx, 
                                           DataDeleteQueryState ddqs, DialogAndInProcessCmd.CmdState cms) {

    String deadID = ddqs.deadList.get(ddqs.deadCheck);
    String deadName = rcx.getCurrentGenome().getNode(deadID).getRootName();

    //
    // Under all circumstances, we delete associated mappings to data.
    // We also give the user the option to delete the underlying data
    // tables, unless previously specified
    //
  
    TimeCourseData tcd = rcx.getExpDataSrc().getTimeCourseData();
    TemporalInputRangeData tird = rcx.getTemporalRangeSrc().getTemporalInputRangeData();
    ResourceManager rMan = rcx.getRMan();
    
    if (((tcd != null) && tcd.haveDataForNodeOrName(deadID, deadName, rcx.getDataMapSrc())) ||
        ((tird != null) && tird.haveDataForNodeOrName(deadID, deadName))) {    
      if (ddqs.multiDelete == UserDataChoice.Delete.PREVIOUS_YES) {
        ddqs.dataDelete.put(deadID, Boolean.valueOf(true));
        ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, cms); // Keep going
        ddqs.deadCheck++;
        ddqs.nextStep = (ddqs.deadCheck >= ddqs.deadList.size()) ? DataDeleteQueryState.NextStep.GO_DELETE 
                                                                 : DataDeleteQueryState.NextStep.CHECK_DATA_DELETE;
        return;
      } else if (ddqs.multiDelete == UserDataChoice.Delete.PREVIOUS_NO) {
        ddqs.dataDelete.put(deadID, Boolean.valueOf(false));
        ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, cms); // Keep going
        ddqs.deadCheck++;
        ddqs.nextStep = (ddqs.deadCheck >= ddqs.deadList.size()) ? DataDeleteQueryState.NextStep.GO_DELETE 
                                                                 : DataDeleteQueryState.NextStep.CHECK_DATA_DELETE;
        return;
      } else if (ddqs.multiDelete == UserDataChoice.Delete.SINGLE_DELETE) {
        String daString = MessageFormat.format(rMan.getString("nodeDelete.doDataMessage"), new Object[] {deadName});
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_CANCEL_OPTION, daString, rMan.getString("nodeDelete.messageTitle"));
        ddqs.retval = new DialogAndInProcessCmd(suf, cms);     
        ddqs.nextStep = DataDeleteQueryState.NextStep.REGISTER_DATA_DELETE;   
        return;  
      } else if (ddqs.multiDelete == UserDataChoice.Delete.ASK_FOR_ALL) {       
        String doData = rMan.getString("nodeDelete.doDataMessage");
        String msg = MessageFormat.format(doData, new Object[] {deadName});
        Object[] args = new Object[] {
                                      rMan.getString("dialogs.yesToAll"),
                                      rMan.getString("dialogs.noToAll"),
                                      rMan.getString("dialogs.yes"),
                                      rMan.getString("dialogs.no"),
                                      rMan.getString("dialogs.cancel")
                                     };
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.OPTION_OPTION, 
                                                        msg, 
                                                        rMan.getString("nodeDelete.messageTitle"),
                                                        args, rMan.getString("dialogs.no"));
        ddqs.retval = new DialogAndInProcessCmd(suf, cms);     
        ddqs.nextStep = DataDeleteQueryState.NextStep.REGISTER_DATA_DELETE;   
        return;  
      }          
    }
    ddqs.deadCheck++;
    ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, cms); // Keep going
    ddqs.nextStep = (ddqs.deadCheck >= ddqs.deadList.size()) ? DataDeleteQueryState.NextStep.GO_DELETE 
                                                             : DataDeleteQueryState.NextStep.CHECK_DATA_DELETE;
    return;
  } 

  /***************************************************************************
  **
  ** Do the step
  */ 
     
  public static void registerCheckDataDelete(DialogAndInProcessCmd daipc,
                                             DataDeleteQueryState ddqs, DialogAndInProcessCmd.CmdState cms) {
    if (ddqs.multiDelete == UserDataChoice.Delete.PREVIOUS_YES) {
      throw new IllegalStateException();    
    } else if (ddqs.multiDelete == UserDataChoice.Delete.PREVIOUS_NO) {
      throw new IllegalStateException();
    } else if (ddqs.multiDelete == UserDataChoice.Delete.SINGLE_DELETE) {
      int result = daipc.suf.getIntegerResult();
      if (result == SimpleUserFeedback.CANCEL) {
        ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, cms);
        return;
      }     
      ddqs.dataDelete.put(ddqs.deadList.get(ddqs.deadCheck), Boolean.valueOf(result == SimpleUserFeedback.YES));
      ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, cms); // Keep going
      ddqs.nextStep = DataDeleteQueryState.NextStep.GO_DELETE;
      return;  
    } else if (ddqs.multiDelete == UserDataChoice.Delete.ASK_FOR_ALL) {
      int result = daipc.suf.getIntegerResult();
      if ((result == 4) || (result == SimpleUserFeedback.CANCEL)) {
        ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, cms);
        return;
      }  
      ddqs.dataDelete.put(ddqs.deadList.get(ddqs.deadCheck), Boolean.valueOf((result == 0) || (result == 2)));
      if (result == 0) {
        ddqs.multiDelete = UserDataChoice.Delete.PREVIOUS_YES;
      } else if (result == 1) {
        ddqs.multiDelete = UserDataChoice.Delete.PREVIOUS_NO;
      }
    } else {
      throw new IllegalStateException();
    }
    ddqs.retval = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.KEEP_PROCESSING, cms); // Keep going
    ddqs.deadCheck++;
    ddqs.nextStep = (ddqs.deadCheck >= ddqs.deadList.size()) ? DataDeleteQueryState.NextStep.GO_DELETE 
                                                             : DataDeleteQueryState.NextStep.CHECK_DATA_DELETE;    
    return;
  } 

  /***************************************************************************
  **
  ** Go through all subsets and delete all nodes and links not present in their
  ** parents anymore.  Note that these guys are not coming back, so there is
  ** no reason to keep empty-member only modules
  */  
 
  public static void fixupDeletionsInNonInstructionModels(UIComponentSource uics, TabSource tSrc, StaticDataAccessContext rcx, 
                                                          UndoSupport support, UndoFactory uFac) {  
    //
    // Make a list of subsets needing fixup, and a done list consisting
    // of all root instances.  Only process subsets if parent is on the
    // done list.  Continue until pending list is empty.
    //
    
    HashSet<String> pending = new HashSet<String>();
    HashSet<String> done = new HashSet<String>();
    HashSet<String> dynamics = new HashSet<String>();
    Iterator<GenomeInstance> git = rcx.getGenomeSource().getInstanceIterator();
    while (git.hasNext()) {
      GenomeInstance gi = git.next();
      String giID = gi.getID();
      InstanceInstructionSet iis = rcx.getInstructSrc().getInstanceInstructionSet(giID);
      if (gi.getVfgParent() == null) {
        done.add(giID);
      } else if (iis != null) {
        done.add(giID);
      } else {
        pending.add(giID);
        if (gi instanceof DynamicGenomeInstance) {
          dynamics.add(giID);
        }
      }
    }
    
    while (pending.size() > 0) {
      Iterator<String> pit = pending.iterator();
      while (pit.hasNext()) {
        String pendID = pit.next();
        GenomeInstance gi = (GenomeInstance)rcx.getGenomeSource().getGenome(pendID);
        GenomeInstance parent = gi.getVfgParent();
        if (parent == null) {
          continue;
        }
        StaticDataAccessContext rcxP = new StaticDataAccessContext(rcx, gi);
        if (done.contains(parent.getID())) {
          fixupDeletionsForSubsetModel(uics, tSrc, rcxP, parent, support, false, uFac);  
          pending.remove(pendID);
          done.add(pendID);
          break;
        }
      }  
    }
    
    ArrayList<String> deadGroups = new ArrayList<String>();
    Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dpit.hasNext()) {
      DynamicInstanceProxy dip = dpit.next();
      StaticDataAccessContext rcxDP = new StaticDataAccessContext(rcx, dip.getFirstProxiedKey());
      GenomeInstance lowestStatic = dip.getStaticVfgParent();
      Iterator<Group> grit = dip.getGroupIterator();
      deadGroups.clear();
      while (grit.hasNext()) {
        Group kgrp = grit.next();
        String inherit = Group.buildInheritedID(kgrp.getID(), lowestStatic.getGeneration());
        if (lowestStatic.getGroup(inherit) == null) {
          deadGroups.add(kgrp.getID());
        } 
      }
      Iterator<String> dgit = deadGroups.iterator();
      while (dgit.hasNext()) {
        String dgid = dgit.next();
        ProxyChange pxc = dip.removeGroupNoChecks(dgid);
        support.addEdit(new ProxyChangeCmd(rcxDP, pxc));    
        ProxyChange[] pca = dip.deleteExtraNodesForGroup(Group.getBaseID(dgid));       
        for (int i = 0; i < pca.length; i++) {
          support.addEdit(new ProxyChangeCmd(rcxDP, pca[i]));
        }
      }
      fixupAllDeletionsForDynamicProxy(rcxDP, dip, lowestStatic, support);      
    }
 
    // DIP extra deletion does not take care of emptied mem-only modules.  And neither does
    // all deletion code.  Do it now.
    RemoveNode.proxyPostExtraNodeDeletionSupport(uics, rcx, support);
 
    return;
  }

  /***************************************************************************
  **
  ** Go through a subset and delete all nodes and links not present in its
  ** parent anymore.
  */  
 
   private static void fixupDeletionsForSubsetModel(UIComponentSource uics, TabSource tSrc, StaticDataAccessContext rcx, GenomeInstance parent, 
                                                    UndoSupport support, boolean keepEmptyMemOnly, UndoFactory uFac) {  

     HashSet<String> deadLinks = new HashSet<String>();
     HashSet<String> deadNodes = new HashSet<String>();
     
     Iterator<Linkage> lit = rcx.getCurrentGenome().getLinkageIterator();
     while (lit.hasNext()) {
       Linkage link = lit.next();
       String lid = link.getID();
       if (parent.getLinkage(lid) == null) {
         deadLinks.add(lid);
       }
     }
     
     Iterator<Node> nit = rcx.getCurrentGenome().getAllNodeIterator();
     while (nit.hasNext()) {
       Node node = nit.next();
       String nid = node.getID();
       if (parent.getNode(nid) == null) {
         deadNodes.add(nid);
       }
     }
     
     deleteNodesAndLinksFromModel(uics, tSrc, deadNodes, deadLinks, rcx, support, null, keepEmptyMemOnly, uFac); 
     
     Iterator<Group> git = rcx.getCurrentGenomeAsInstance().getGroupIterator();
     boolean dropped = false;
     while (git.hasNext()) {
       Group grp = git.next();
       String inherit = Group.buildInheritedID(grp.getID(), parent.getGeneration());
       if (parent.getGroup(inherit) == null) {
         GenomeChange gc = rcx.getCurrentGenomeAsInstance().removeGroupNoChecks(grp.getID());
         GenomeChangeCmd gcc = new GenomeChangeCmd(gc);
         support.addEdit(gcc);    
    
         GroupSettingSource gsm = rcx.getGSM();          
         GroupSettingChange gsc = gsm.dropGroupVisibility(rcx.getCurrentGenomeID(), grp.getID());  
         if (gsc != null) {
           GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(rcx, gsc);
           support.addEdit(gscc);
         }
         dropped = true;
       } 
     }
     
     if (dropped) {
       support.addEvent(new ModelChangeEvent(rcx.getGenomeSource().getID(), rcx.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
     }       
     return;
   }
   
  /***************************************************************************
  **
  ** Go through a subset and delete all nodes and links not present in its
  ** parent anymore.
  */  
 
  private static boolean fixupAllDeletionsForDynamicProxy(StaticDataAccessContext rcxDP, 
                                                          DynamicInstanceProxy dip, GenomeInstance staticParent, 
                                                          UndoSupport support) {  

    HashSet<String> deadNodes = new HashSet<String>(); 
   
    boolean retval = false;
    Iterator<NetworkOverlay> oit = dip.getNetworkOverlayIterator();
    while (oit.hasNext()) {
      NetworkOverlay no = oit.next();
      String noID = no.getID();
      deadNodes.clear();
      Iterator<NetModule> nmit = no.getModuleIterator();
      while (nmit.hasNext()) {
        NetModule nmod = nmit.next();
        Iterator<NetModuleMember> mit = nmod.getMemberIterator();
        while (mit.hasNext()) {
          NetModuleMember nmm = mit.next();
          String nodeID = nmm.getID();
          if (staticParent.getNode(nodeID) == null) {
            deadNodes.add(nodeID);  
          }
        }
        Iterator<String> dnit = deadNodes.iterator();
        while (dnit.hasNext()) {
          String nodeID = dnit.next();
          NetModuleChange nmc = dip.deleteMemberFromNetworkModule(noID, nmod, nodeID);
          if (nmc != null) {
            NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(rcxDP, nmc);
            support.addEdit(gcc);
            retval = true;
          }    
        }
      }
    }
    return (retval);
  }   
 
  /***************************************************************************
  **
  ** Delete a set of nodes and links from the model
  */  
 
  public static void deleteNodesAndLinksFromModel(UIComponentSource uics, TabSource tSrc, Set<String> nodes, Set<String> links, StaticDataAccessContext rcx,
                                                  UndoSupport support, Map<String, Boolean> dataDelete, 
                                                  boolean keepEmptyMemOnly, UndoFactory uFac) {  
                                            
    RemoveLinkage.deleteLinkSetFromModel(links, rcx, support);    
 
    Iterator<String> nit = nodes.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      RemoveNode.deleteNodeFromModelCore(uics, tSrc, nodeID, rcx, support, dataDelete, keepEmptyMemOnly, uFac);
    }
    return;
  }
}
