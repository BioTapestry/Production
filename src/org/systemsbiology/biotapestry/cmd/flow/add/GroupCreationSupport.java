/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd.flow.add;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNode;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.GroupSettingChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NetOverlayChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PropChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.GroupSettingChange;
import org.systemsbiology.biotapestry.nav.GroupSettings;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.GroupDuplicationDialog;
import org.systemsbiology.biotapestry.ui.dialogs.GroupPositionDialog;
import org.systemsbiology.biotapestry.ui.dialogs.MultiGroupDuplicationDialog;
import org.systemsbiology.biotapestry.util.Bounds;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.Vector2D;

/****************************************************************************
**
** Handler for group creation 
*/

public class GroupCreationSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private boolean forAssignment_;
  private Point2D defaultCenter_;
  private Rectangle approxBounds_;
  private Point2D directCenter_;
  private boolean wasForcedDirect_;
  private Set<String> allGroupIDs_;
  private String srcGroupID_;
  private BTState appState_;
  private DataAccessContext rcxSrc_;
  private DataAccessContext rcxTrg_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public GroupCreationSupport(BTState appState, DataAccessContext rcx,
                              boolean forAssignment,
                              Rectangle approxBounds, Point2D defaultCenter,
                              Point2D directCenter) {
    appState_ = appState;
    forAssignment_ = forAssignment;
    approxBounds_ = approxBounds;
    defaultCenter_ = defaultCenter;
    directCenter_ = directCenter;
    wasForcedDirect_ = false;
    srcGroupID_ = null;
    allGroupIDs_ = null;
    rcxSrc_ = rcx;  
    rcxTrg_ = rcx;
  }
  
  /***************************************************************************
  **
  ** Constructor for group duplication
  */ 
  
  public GroupCreationSupport(BTState appState, DataAccessContext rcxSrc, Set<String> allGroups) {
    appState_ = appState;
    forAssignment_ = true;
    approxBounds_ = null;
    defaultCenter_ = null;
    directCenter_ = null;
    wasForcedDirect_ = false;
    allGroupIDs_ = allGroups;
    if (allGroups.size() == 1) {
      srcGroupID_ = allGroups.iterator().next();
    }
    rcxSrc_ = rcxSrc;
    rcxTrg_ = null;  
  }  

  /***************************************************************************
  **
  ** Constructor for non-interactive placement:
  */ 
  
  public GroupCreationSupport(BTState appState, DataAccessContext rcx, boolean forAssignment) {
    appState_ = appState;
    forAssignment_ = forAssignment;
    approxBounds_ = null;
    defaultCenter_ = null;
    directCenter_ = null;
    wasForcedDirect_ = false;
    srcGroupID_ = null;
    rcxSrc_ = rcx;
    rcxTrg_ = rcx;
  }  
  
  /***************************************************************************
  **
  ** Constructor for non-interactive subset placement:
  */ 
  
  public GroupCreationSupport(BTState appState) {
    appState_ = appState;
  }   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Answers if the user requested a direct placement
  ** 
  */
  
  public boolean wasForcedDirect() {
    return (wasForcedDirect_);
  }
  
  /***************************************************************************
  **
  ** Run the creation loop
  ** 
  */
  
  public Group handleCreation(UndoSupport support) {
    
    String newName = handleCreationPartOne();
    if (newName == null) {
      return (null);
    }
    Point2D groupCenter = handleCreationPartTwo();
    if (groupCenter == null) {
      return (null);
    }   
    String groupKey = rcxSrc_.getNextKey();
    Group newGroup = new Group(rcxSrc_.rMan, groupKey, newName);
    handleCreationPartThreeCore(support, newGroup, groupKey, groupCenter, rcxTrg_.getGenomeAsInstance(), null);
    return (newGroup);
  }
  
  /***************************************************************************
  **
  ** Run the creation loop for copying, possibly into another sibling model 
  */
  
  public List<GroupDuplicationInfo> handleCopyCreation(UndoSupport support) {
    
    if (allGroupIDs_ == null) {
      throw new IllegalStateException();
    }
    
    List<GroupDuplicationInfo> retval;
    if (srcGroupID_ != null) {
      GroupDuplicationInfo gdi = handleSingleGroupCopyCreationPartOne();
      if (gdi == null) {
        return (null);
      }
      retval = new ArrayList<GroupDuplicationInfo>();
      retval.add(gdi);
    } else {
      retval = handleMultiGroupCopyCreationPartOne();
      if (retval == null) {
        return (null);
      }
    }
    
    Point2D groupCenter = handleCreationPartTwo();
    if (groupCenter == null) {
      return (null);
    }
    
    Vector2D commonOffset = new Vector2D(defaultCenter_, groupCenter);
        
    int numRet = retval.size();
    for (int i = 0; i < numRet; i++) {
      GroupDuplicationInfo gdi = retval.get(i);
      String groupKey = ((DBGenome)rcxSrc_.getGenomeSource().getGenome()).getNextKey();
      Group newGroup = new Group(rcxSrc_.rMan, groupKey, gdi.groupName);      
      gdi.bifg.myOffset = commonOffset.clone();
      Point2D myCenter = commonOffset.add(gdi.bifg.defaultCenter);
      UiUtil.forceToGrid(myCenter, UiUtil.GRID_SIZE);
      handleCreationPartThreeCore(support, newGroup, groupKey, myCenter, rcxTrg_.getGenomeAsInstance(), gdi);
      gdi.setGroupAndTargetGroupDuplicationInfo(newGroup, rcxTrg_.getGenomeID());
    }
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Creation part 1
  ** 
  */
  
  public String handleCreationPartOne() {    
    String newName = null;
    String messageKey = (forAssignment_) ? "createGroup.AssignMsg" : "createGroup.FreeMsg";
    String titleKey = (forAssignment_) ? "createGroup.AssignTitle" : "createGroup.FreeTitle";
    
    if (rcxTrg_.getGenomeAsInstance().getVfgParent() != null) {
      throw new IllegalStateException();
    }
    
    while (true) {
      newName = (String)JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                                    rcxSrc_.rMan.getString(messageKey), 
                                                    rcxSrc_.rMan.getString(titleKey),
                                                    JOptionPane.QUESTION_MESSAGE, 
                                                    null, null, rcxTrg_.getGenomeAsInstance().getUniqueGroupName()); 
      if (newName == null) {
        return (null);
      }
      
      newName = newName.trim();
      
      if (rcxTrg_.getGenomeAsInstance().groupNameInUse(newName)) {
        String desc = MessageFormat.format(rcxSrc_.rMan.getString("createGroup.NameInUse"), 
                                           new Object[] {newName});
        JOptionPane.showMessageDialog(appState_.getTopFrame(), desc, 
                                      rcxSrc_.rMan.getString("createGroup.CreationErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
      } else {
        break;
      }
    }

    return (newName);
  } 
  
  /***************************************************************************
  **
  ** Creation part 1
  ** 
  */
  
  public List<GroupDuplicationInfo> handleMultiGroupCopyCreationPartOne() {    
    
    if (rcxSrc_.getGenomeAsInstance().getVfgParent() != null) {
      throw new IllegalStateException();
    }
    
    MultiGroupDuplicationDialog mgdd = new MultiGroupDuplicationDialog(appState_, rcxSrc_.getGenomeID(), allGroupIDs_);
    mgdd.setVisible(true);
    if (!mgdd.haveResult()) {
      return (null);
    }
    
    if (mgdd.doModelChange()) {
      rcxTrg_ = new DataAccessContext(rcxSrc_, mgdd.getTargetModel()); 
    } else {
      rcxTrg_ = rcxSrc_;
    }

    ArrayList<GroupDuplicationInfo> retval = new ArrayList<GroupDuplicationInfo>();
    Set<String> chosenGroups = mgdd.getGroupsToDup();
    Iterator<String> cgit = chosenGroups.iterator();
    while (cgit.hasNext()) {
      String dupID = cgit.next();
      Group toCopy = rcxSrc_.getGenomeAsInstance().getGroup(dupID);
      int lastCopyNum = 0;
      String copyName;
      while (true) {
        String origName = toCopy.getInheritedTrueName(rcxSrc_.getGenomeAsInstance());
        String testName = UiUtil.createCopyName(rcxSrc_.rMan, origName, lastCopyNum++);
        if (!rcxSrc_.getGenomeAsInstance().groupNameInUse(testName)) {
          copyName = testName;
          break;
        }
      }    
      
      GroupDuplicationInfo gdi = new GroupDuplicationInfo(toCopy, copyName);
      getNodesToDuplicate(rcxSrc_.getGenomeAsInstance(), toCopy, gdi.genesToCopy, gdi.nodesToCopy);
      
      // Copy the colors across:
    
      GroupProperties srcProp = rcxSrc_.getLayout().getGroupProperties(dupID);
      gdi.activeColor = srcProp.getColorTag(true);
      gdi.inactiveColor = srcProp.getColorTag(false);
      
      //
      // Get the list of all the links in the source region that we are going to copy
      //

      getLinksToDuplicate(rcxSrc_.getGenomeAsInstance(), toCopy, gdi.internalLinks, gdi.inboundLinks, gdi.outboundLinks, dupID);        
      BoundInfoForGroup bifg = getApproxBounds(gdi.genesToCopy, gdi.nodesToCopy, rcxSrc_, dupID);
      gdi.setBoundInfoForGroup(bifg);
      if (approxBounds_ == null) {
        approxBounds_ = (Rectangle)bifg.approxBounds.clone();
        defaultCenter_ = (Point2D)bifg.defaultCenter.clone();
      } else {
        Bounds.tweakBounds(approxBounds_, bifg.approxBounds);
        defaultCenter_.setLocation(approxBounds_.getCenterX(), approxBounds_.getCenterY());
        UiUtil.forceToGrid(defaultCenter_, UiUtil.GRID_SIZE);
      }
      retval.add(gdi);
    }
    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Creation part 1
  ** 
  */
  
  public GroupDuplicationInfo handleSingleGroupCopyCreationPartOne() {    

    if (rcxSrc_.getGenomeAsInstance().getVfgParent() != null) {
      throw new IllegalStateException();
    }
     
    String origName = rcxSrc_.getGenomeAsInstance().getGroup(srcGroupID_).getInheritedTrueName(rcxSrc_.getGenomeAsInstance());
    String defaultName = null;
    int lastCopyNum = 0;
    while (true) {
      String testName = UiUtil.createCopyName(rcxSrc_.rMan, origName, lastCopyNum++);
      if (!rcxSrc_.getGenomeAsInstance().groupNameInUse(testName)) {
        defaultName = testName;
        break;
      }
    }
    
    GroupDuplicationDialog gdd = new GroupDuplicationDialog(appState_, rcxSrc_.getGenomeID(), srcGroupID_, defaultName);
    gdd.setVisible(true);
    if (!gdd.haveResult()) {
      return (null);
    }
    
    if (gdd.doModelChange()) {
      rcxTrg_ = new DataAccessContext(rcxSrc_, gdd.getTargetModel()); 
    } else {
      rcxTrg_ = rcxSrc_;
    }
    
    //
    // Get the bounds figured out for the new single group:
    //
 
    Group toCopy = rcxSrc_.getGenomeAsInstance().getGroup(srcGroupID_);
    GroupDuplicationInfo gdi = new GroupDuplicationInfo(toCopy, gdd.getName());
    getNodesToDuplicate(rcxSrc_.getGenomeAsInstance(), toCopy, gdi.genesToCopy, gdi.nodesToCopy);
    
    // Copy the colors across:
    
    GroupProperties srcProp = rcxSrc_.getLayout().getGroupProperties(srcGroupID_);
    gdi.activeColor = srcProp.getColorTag(true);
    gdi.inactiveColor = srcProp.getColorTag(false);
       
    //
    // Get the list of all the links in the source region that we are going to copy
    //
    
    getLinksToDuplicate(rcxSrc_.getGenomeAsInstance(), toCopy, gdi.internalLinks, gdi.inboundLinks, gdi.outboundLinks, srcGroupID_);        
    BoundInfoForGroup bifg = getApproxBounds(gdi.genesToCopy, gdi.nodesToCopy, rcxSrc_, srcGroupID_);
    gdi.setBoundInfoForGroup(bifg);
    defaultCenter_ = bifg.defaultCenter;
    approxBounds_ = bifg.approxBounds;
    return (gdi);
  }  
  
  /***************************************************************************
  **
  ** Creation part 2
  ** 
  */
  
  public Point2D handleCreationPartTwo() {    

    // want key to be unique in parent address space!
    // Note added 2/22/05: This fact is crucial to understanding group ID allocation
    // and release.  I remember when it was implemented in 2003, it was a crucial
    // requirement, but WHY was it required?  Is it because we want intersection tests
    // at the root instance level to have unique IDs between groups and nodes, and
    // since node IDs are assigned to be unique at the root model level, group IDs
    // have to be assigned from the same namespace to keep them distinct?  (I think
    // this is the case).  Notes don't suffer this problem because they are not
    // suffixed with an instance label e.g. :0?  FIX ME: Group instance suffixes need
    // to be addressed with more sanity!
    // Also 2-22-07: Note that trGroupMaps and tcGroupMaps use a global namespace for
    // mapping groups to data sources.  If not global, there would be issues.   
    //

    Point2D groupCenter = null;
    if (rcxTrg_.getGenomeAsInstance().hasAGroup() || (defaultCenter_ == null)) {
      GroupPositionDialog gpd = new GroupPositionDialog(appState_, rcxTrg_, approxBounds_, directCenter_);     
      gpd.setVisible(true);
      groupCenter = gpd.getGroupLocation();
      if (groupCenter == null) {
        return (null);
      }
      if ((directCenter_ != null) && groupCenter.equals(directCenter_)) {
        wasForcedDirect_ = true; 
      }
    } else {
      groupCenter = defaultCenter_;
    }
    UiUtil.forceToGrid(groupCenter.getX(), groupCenter.getY(), groupCenter, 10.0);
    return (groupCenter); 
  }    
    
  /***************************************************************************
  **
  ** Creation part 3
  ** 
  */
  
  public void handleCreationPartThreeCore(UndoSupport support, Group newGroup, 
                                          String groupKey, Point2D groupCenter,
                                          GenomeInstance gi, GroupDuplicationInfo gdi) {    
    
    GenomeChange gc = gi.addGroupWithExistingLabel(newGroup); 
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcxTrg_, gc);
      support.addEdit(gcc);
    }
    
    int groupCount = gi.groupCount();
    int order = rcxTrg_.getLayout().getTopGroupOrder() + 1;
    
    GroupProperties newProps;
    if (gdi != null) {
      newProps = new GroupProperties(groupKey, groupCenter, order, gdi.activeColor, gdi.inactiveColor);
    } else {
      newProps = new GroupProperties(groupCount, groupKey, rcxTrg_.getLayout(), groupCenter, order, rcxTrg_.cRes);
    }
    
    Layout.PropChange lpc = rcxTrg_.getLayout().setGroupProperties(groupKey, newProps);
    if (lpc != null) {
      PropChangeCmd pcc = new PropChangeCmd(appState_, rcxTrg_, new Layout.PropChange[] {lpc});
      support.addEdit(pcc);
    }
        
    GroupSettingChange gsc = rcxTrg_.gsm.setGroupVisibility(gi.getID(), groupKey, GroupSettings.Setting.ACTIVE);  
    if (gsc != null) {
      GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState_, rcxTrg_, gsc);
      support.addEdit(gscc);
    }
    
    support.addEvent(new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE));    
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a new group to a VFN model by inheritance.
  */  
 
  public boolean addNewGroupToSubsetInstance(DataAccessContext rcx,
                                             Group useGroup,
                                             UndoSupport support) {
    //
    // Undo/Redo support
    //
    boolean doLocal = false;                                               
    if (support == null) {
      support = new UndoSupport(appState_, "undo.addGroup");
      doLocal = true;
    }
 
    if (!(rcx.getGenome() instanceof GenomeInstance)) {   
      throw new IllegalArgumentException();    
    }
    
    GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }

    Group newGroup = new Group(rcx.rMan, useGroup.getID(), false, null);
    GenomeChange gc = rcx.getGenomeAsInstance().addGroupWithExistingLabel(newGroup);
    if (gc != null) {
      GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcx, gc);
      support.addEdit(gcc);
    }
    
    //
    // If the parent model group has an active subgroup, then we need to pull
    // that model in too to activate.
    //
    
    String active = useGroup.getActiveSubset();
    if (active != null) {
      active = Group.addGeneration(active);
      makeSubGroupActive(rcx, newGroup.getID(), active, support);
    }
 
    // This is done dynamically for proxies
    if (!(rcx.getGenome() instanceof DynamicGenomeInstance)) {      
      GroupSettingChange gsc = rcx.gsm.setGroupVisibility(rcx.getGenomeID(), newGroup.getID(), GroupSettings.Setting.ACTIVE);  
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState_,rcx,  gsc);
        support.addEdit(gscc);
      }
    }
    
    //
    // If we have just added a group to a dynamic instance, we need to make sure
    // that any added nodes in the proxy which belong to the new group are eliminated.
    //
    
    Iterator<DynamicInstanceProxy> dit = rcx.getGenomeSource().getDynamicProxyIterator();
    boolean lostExtra = false;
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteIllegalExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(appState_, rcx, pca[i]));
        lostExtra = true;
      }
    }
    dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteOrphanedExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(appState_, rcx, pca[i]));
        lostExtra = true;
      }
    }
    
    // loss of extra nodes may create empty modules!
    if (lostExtra) {
      RemoveNode.proxyPostExtraNodeDeletionSupport(appState_, rcx, support);
    }
    
    support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE)); 
  
    if (doLocal) {
      support.finish();
    }   
    return (true); 
  }

  /***************************************************************************
  **
  ** Make a subgroup the active group in a subset instance
  */  
 
  public boolean makeSubGroupActive(DataAccessContext rcx,
                                    String parentKey, 
                                    String activeSubset,
                                    UndoSupport support) {
    //
    // Undo/Redo support
    //
    
    boolean doLocal = false;
    if (support == null) {
      doLocal = true;
      support = new UndoSupport(appState_, "undo.makeSubGroupActive");
    }

    if (!(rcx.getGenome() instanceof GenomeInstance)) {   
      throw new IllegalArgumentException();    
    }
    
    GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    
    if (activateSubGroupDetails(rcx, rcx.getGenomeAsInstance(), parentKey, activeSubset, support)) {
 
      //
      // Find child models, and if they include the parent group, we need to add the
      // subgroup too as active (unless it is already active).
      //
      
      String pxid = null;
      if (rcx.getGenomeAsInstance() instanceof DynamicGenomeInstance) {
        pxid = ((DynamicGenomeInstance)rcx.getGenomeAsInstance()).getProxyID();
      }
      
      Iterator<DynamicInstanceProxy> dpit = rcx.getGenomeSource().getDynamicProxyIterator();
      while (dpit.hasNext()) {
        DynamicInstanceProxy dip = dpit.next();
        if ((pxid != null) ? (!pxid.equals(dip.getID()) && dip.proxyIsAncestor(pxid)) : dip.instanceIsAncestor(rcx.getGenomeAsInstance())) {
          // Kinda a hack, going through a proxied dynamic instance!  FIX ME!
          DynamicGenomeInstance kid = dip.getAnInstance();
          String inherit = Group.buildInheritedID(parentKey, kid.getGeneration());
          if (kid.getGroup(inherit) != null) {
            String activeInherit = Group.buildInheritedID(activeSubset, kid.getGeneration());
            activateSubGroupDetails(rcx, kid, inherit, activeInherit, support);            
          }
        }
      }
      Iterator<GenomeInstance> dit = rcx.getGenomeSource().getInstanceIterator();
      while (dit.hasNext()) {
        GenomeInstance kid = dit.next();
        if ((kid != rcx.getGenomeAsInstance()) && rcx.getGenomeAsInstance().isAncestor(kid)) {        
          String inherit = Group.buildInheritedID(parentKey, kid.getGeneration());
          if (kid.getGroup(inherit) != null) {
            String activeInherit = Group.buildInheritedID(activeSubset, kid.getGeneration());
            activateSubGroupDetails(rcx, kid, inherit, activeInherit, support);            
          }
        }
      }
    } 
      
      // This is done dynamically for proxies
    if (!(rcx.getGenomeAsInstance() instanceof DynamicGenomeInstance)) {
      Group parentGroup = rcx.getGenomeAsInstance().getGroup(parentKey);
      String subset = parentGroup.getActiveSubset();
      GroupSettingChange gsc = rcx.gsm.setGroupVisibility(rcx.getGenomeID(), subset, GroupSettings.Setting.ACTIVE);  
      if (gsc != null) {
        GroupSettingChangeCmd gscc = new GroupSettingChangeCmd(appState_, rcx, gsc);
        support.addEdit(gscc);
      }
    }
    
    //
    // If we have just added a group to a dynamic instance, we need to make sure
    // that any added nodes in the proxy which belong to the new group are eliminated.
    //
    
    boolean checkForEmpty = false;
    Iterator<DynamicInstanceProxy> dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteIllegalExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(appState_, rcx, pca[i]));
        checkForEmpty = true;
      }
    }
    dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      ProxyChange[] pca = dip.deleteOrphanedExtraNodes();
      for (int i = 0; i < pca.length; i++) {
        support.addEdit(new ProxyChangeCmd(appState_, rcx, pca[i]));
        checkForEmpty = true;
      }
    }    
 
    dit = rcx.getGenomeSource().getDynamicProxyIterator();
    while (dit.hasNext()) {
      DynamicInstanceProxy dip = dit.next();
      DynamicGenomeInstance kid = dip.getAnInstance();
      NetModuleChange[] nmca = dip.adjustDynamicGroupModuleMembers(kid);
      if (nmca != null) {
        for (int i = 0; i < nmca.length; i++) {
          NetOverlayChangeCmd gcc = new NetOverlayChangeCmd(appState_, rcx, nmca[i]);
          support.addEdit(gcc);
          checkForEmpty = true;
        }
      }
    }
    
    // loss of extra nodes may create empty modules!
    if (checkForEmpty) {
      RemoveNode.proxyPostExtraNodeDeletionSupport(appState_, rcx, support);
    }
       
    if (doLocal) {
      support.finish();
    }
    return (true);
  }  

  /***************************************************************************
  **
  ** INclude a subgroup in a subset instance
  */  
 
  private boolean activateSubGroupDetails(DataAccessContext rcx, GenomeInstance gi,
                                          String parentKey, 
                                          String activeSubset,
                                          UndoSupport support) {
    boolean retval = false;
    GenomeChange[] gc = gi.activateSubGroup(parentKey, activeSubset);
    if (gc != null) {
      for (int i = 0; i < gc.length; i++) {
        if (gc[i] != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, rcx, gc[i]);
          support.addEdit(gcc);
          retval = true;
        }              
      }
    }
    if (retval) {
      ModelChangeEvent mcev = new ModelChangeEvent(gi.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(mcev);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** INclude a subgroup in a subset instance
  */  
 
  public boolean includeSubGroup(DataAccessContext rcx,
                                 String parentKey, 
                                 String newSubgroup) {
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.includeSubGroup"); 
    
    if (!(rcx.getGenome() instanceof GenomeInstance)) {   
      throw new IllegalArgumentException();    
    }
    
    GenomeInstance parent = rcx.getGenomeAsInstance().getVfgParent();
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    
    Group newGroup = new Group(rcx.rMan, newSubgroup, false, null);
    GenomeChange gc = rcx.getGenomeAsInstance().addGroupWithExistingLabel(newGroup);
    if (gc != null) {
      support.addEdit(new GenomeChangeCmd(appState_, rcx, gc));
      support.addEvent(new ModelChangeEvent(rcx.getGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE));
    }     
    
    support.finish();        
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get the list of all the node IDs in the source region that we are going to copy
  */  
 
  private void getNodesToDuplicate(GenomeInstance srcGI, Group toCopy, 
                                   ArrayList<Node> genesToCopy, 
                                   ArrayList<Node> nodesToCopy) {
 
    Iterator<GroupMember> mit = toCopy.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember gm = mit.next();
      String nodeID = gm.getID();
      Node node = srcGI.getNode(nodeID);
      if (node.getNodeType() == Node.GENE) {
        genesToCopy.add(node);
      } else {
        nodesToCopy.add(node);
      }
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Get the list of all the links in the source region that we are going to copy
  */  
 
  private void getLinksToDuplicate(GenomeInstance srcGI, Group toCopy,
                                   ArrayList<String> internalLinks,           
                                   ArrayList<String> inboundLinks, 
                                   ArrayList<String> outboundLinks, String regionID) {
  
    Iterator<Linkage> lit = srcGI.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      GenomeInstance.GroupTuple tuple = srcGI.getRegionTuple(linkID);
      String srcGrp = tuple.getSourceGroup();
      String trgGrp = tuple.getTargetGroup();
      if (srcGrp.equals(regionID)) {
        if (trgGrp.equals(regionID)) {
          internalLinks.add(linkID);
        } else {
          outboundLinks.add(linkID);
        }
      } else if (trgGrp.equals(regionID)) {
        inboundLinks.add(linkID);
      }
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Get bounds
  */  
 
  private BoundInfoForGroup getApproxBounds(ArrayList<Node> genesToCopy, 
                                            ArrayList<Node> nodesToCopy,
                                            DataAccessContext rcx,
                                            String regionID) {
  
    Iterator<Node> git = genesToCopy.iterator();
    Iterator<Node> nit = nodesToCopy.iterator();
    Rectangle approxBounds = rcx.getLayout().getApproxBounds(nit, git, rcx);
    git = genesToCopy.iterator();
    nit = nodesToCopy.iterator();      
    Point2D defaultCenter = rcx.getLayout().getApproxCenter(nit, git);
    if (defaultCenter == null) {  // i.e. no nodes or genes
      GroupProperties gp = rcx.getLayout().getGroupProperties(regionID);
      defaultCenter = gp.getLabelLocation();
      approxBounds.setLocation((int)(approxBounds.getX() + defaultCenter.getX()),
                               (int)(approxBounds.getY() + defaultCenter.getY()));
    }      
              
    return (new BoundInfoForGroup(defaultCenter, approxBounds));  
  }  
  
  /***************************************************************************
  **
  ** Used to pass back group duplication info
  */
  
  public static class GroupDuplicationInfo {
    public Group toCopy;
    public String groupName;
    public String activeColor;
    public String inactiveColor;
    public Group dupGroup;
    public String targetGiID;
    public ArrayList<Node> genesToCopy;        
    public ArrayList<Node> nodesToCopy;
    public ArrayList<String> internalLinks;    
    public ArrayList<String> inboundLinks;    
    public ArrayList<String> outboundLinks;
    public BoundInfoForGroup bifg;
           
    GroupDuplicationInfo(Group toCopy, String groupName) {
      this.toCopy = toCopy;
      this.groupName = groupName;
      this.genesToCopy = new ArrayList<Node>();        
      this.nodesToCopy = new ArrayList<Node>();
      this.internalLinks = new ArrayList<String>();    
      this.inboundLinks = new ArrayList<String>();    
      this.outboundLinks = new ArrayList<String>();
    }    
    
    void setGroupAndTargetGroupDuplicationInfo(Group dupGroup, String targetGiID) {
      this.dupGroup = dupGroup;
      this.targetGiID = targetGiID;
      return;
    }
    
    void setBoundInfoForGroup(BoundInfoForGroup bifg) {
      this.bifg = bifg;
      return;
    }        
  }
  
  /***************************************************************************
  **
  ** Used to hand around group bounds
  */
  
  public static class BoundInfoForGroup { 
    public Point2D defaultCenter;
    public Rectangle approxBounds;
    public Vector2D myOffset;
    
    BoundInfoForGroup(Point2D defaultCenter, Rectangle approxBounds) {
      this.defaultCenter = defaultCenter;
      this.approxBounds = approxBounds;
    }    
  }  

}
