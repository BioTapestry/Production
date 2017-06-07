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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.ModificationCommands;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy.AddedNode;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMembership;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleMember;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Dialog box for adding module members to a module.  Pretty necessary in
** Dynamic Instances, where we don't have the stuff to click on.
*/

public class EditModuleMemberDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private CheckBoxList nodesToChoose_;
  private String ovrID_; 
  private String moduleID_;
  private StaticDataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditModuleMemberDialog(UIComponentSource uics, StaticDataAccessContext dacx, 
                                String genomeID, String sourceGenomeID, String ovrID, String moduleID, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("editModMemb.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    ovrID_ = ovrID;
    uFac_ = uFac;
    moduleID_ = moduleID;
    ResourceManager rMan = dacx_.getRMan();    
    setSize(500, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
       
    //
    // Figure out current members
    //
    
    GenomeSource db = dacx_.getGenomeSource(); 
       
    HashSet<String> currentMembers = new HashSet<String>();
    NetworkOverlay nov = db.getOverlayOwnerFromGenomeKey(genomeID).getNetworkOverlay(ovrID);     
    NetModule nmod = nov.getModule(moduleID);

    Iterator<NetModuleMember> memit = nmod.getMemberIterator();
    while (memit.hasNext()) {
      NetModuleMember nmm = memit.next();
      String nodeID = nmm.getID();
      currentMembers.add(nodeID);
    }
    
    //
    // Figure out which groups we can offer for members.  If tne module is attached to
    // a group, there is only one.  Else it is limited to the groups present in the model.
    //
    
    GenomeInstance srcGen = (GenomeInstance)db.getGenome(sourceGenomeID);
    DynamicGenomeInstance gi = (DynamicGenomeInstance)db.getGenome(genomeID);
    DynamicInstanceProxy dip = db.getDynamicProxy(gi.getProxyID());
    
      
    HashSet<String> allowedMainGroups = new HashSet<String>();
    HashSet<String> allowedActiveSubGroups = new HashSet<String>();    
    String grpAttach = nmod.getGroupAttachment();
    if (grpAttach != null) {
      Group grp = gi.getGroup(grpAttach);
      String baseAttach = Group.getBaseID(grpAttach);
      String active = grp.getActiveSubset();
      if (active != null) {
        baseAttach = Group.getBaseID(active);
        allowedActiveSubGroups.add(baseAttach);
      } else {
        allowedMainGroups.add(baseAttach);
      }
    } else {
      Iterator<Group> git = gi.getGroupIterator();
      while (git.hasNext()) {
        Group grp = git.next();
        if (grp.isASubset(srcGen)) {
          continue;
        }
        String baseAttach = Group.getBaseID(grp.getID());
        String active = grp.getActiveSubset();
        if (active != null) {
          baseAttach = Group.getBaseID(active);
          allowedActiveSubGroups.add(baseAttach);
        } else {
          allowedMainGroups.add(baseAttach);
        }
      }
    }
 
    HashSet<String> added = new HashSet<String>();
    if (grpAttach == null) {
      Iterator<AddedNode> anit = dip.getAddedNodeIterator();
      while (anit.hasNext()) {
        AddedNode an = anit.next();
        added.add(an.nodeName);
      } 
    }
    
    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan.getString("editModMemb.selectMembers"));
    nodesToChoose_ = new CheckBoxList(buildMemberChoices(sourceGenomeID, currentMembers, allowedMainGroups, allowedActiveSubGroups, added), 
                                      uics_.getHandlerAndManagerSource());
    UiUtil.gbcSet(gbc, 0, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(label, gbc);

    JScrollPane jsp = new JScrollPane(nodesToChoose_);
    UiUtil.gbcSet(gbc, 0, 1, 3, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(jsp, gbc);         
    
    //
    // Build the button panel:
    //
    
    FixedJButton buttonA = new FixedJButton(rMan.getString("dialogs.apply"));
    buttonA.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          applyProperties();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (applyProperties()) {
            EditModuleMemberDialog.this.setVisible(false);
            EditModuleMemberDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {      
          EditModuleMemberDialog.this.setVisible(false);
          EditModuleMemberDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonA);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);
 
    //
    // Build the dialog:
    //
    
    UiUtil.gbcSet(gbc, 0, 7, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the list of nodes to choose from: 
  */
  
  private Vector<CheckBoxList.ListChoice> buildMemberChoices(String genomeID, Set<String> modMembers, 
                                                             Set<String> mainGroupsToShow, Set<String> activeSubGroupsToShow, 
                                                             Set<String> added) {
       
    GenomeSource db = dacx_.getGenomeSource();
    ColorResolver cres = dacx_.getColorResolver();
    GenomeInstance gi = (GenomeInstance)db.getGenome(genomeID);
    Layout lo = dacx_.getLayoutSource().getLayoutForGenomeKey(genomeID);
    ArrayList<ObjChoiceContent> nodeChoices = buildCombo(gi);
    int count = nodeChoices.size();      
    Vector<CheckBoxList.ListChoice> retval = new Vector<CheckBoxList.ListChoice>();
    for (int i = 0; i < count; i++) {
      ObjChoiceContent occ = nodeChoices.get(i);
      GroupMembership gm = gi.getNodeGroupMembership(gi.getNode(occ.val));    
      Color gCol = Color.white;
      if (added.contains(occ.val)) {
        gCol = Color.white;
        boolean isAMem = modMembers.contains(occ.val);
        retval.add(new CheckBoxList.ListChoice(occ.val, occ.name, gCol, isAMem));
      } else {
        String grpMatch = gm.gotAGroupMatch(mainGroupsToShow, activeSubGroupsToShow);
        if (grpMatch != null) {
          GroupProperties gp = lo.getGroupProperties(grpMatch);
          gCol = gp.getColor(true, cres);
          boolean isAMem = modMembers.contains(occ.val);
          retval.add(new CheckBoxList.ListChoice(occ.val, occ.name, gCol, isAMem));
        }
      }
    }

    return (retval);    
  }
  
  /***************************************************************************
  **
  ** Build the combo of available nodes
  ** 
  */
  
  private ArrayList<ObjChoiceContent> buildCombo(GenomeInstance tgi) {
    TreeMap<String, List<String>> tm = new TreeMap<String, List<String>>();
    
    Iterator<Node> nit = tgi.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeMsg = node.getDisplayString(tgi, true);
      String nodeID = node.getID();
      List<String> perMsg = tm.get(nodeMsg);
      if (perMsg == null) {
        perMsg = new ArrayList<String>();
        tm.put(nodeMsg, perMsg);
      }
      perMsg.add(nodeID);
    }    
   
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    Iterator<String> tmkit = tm.keySet().iterator();
    while (tmkit.hasNext()) {
      String nodeMsg = tmkit.next();
      List<String> perMsg = tm.get(nodeMsg);
      int pmNum = perMsg.size();
      for (int i = 0; i < pmNum; i++) {
        String nodeID = perMsg.get(i);
        retval.add(new ObjChoiceContent(nodeMsg, nodeID)); 
      }
    }   
    return (retval);
  }  

  /***************************************************************************
  **
  ** Apply our UI values to the model
  ** 
  */
  
  private boolean applyProperties() {
    
    HashSet<String> modMembers = new HashSet<String>();
    ListModel myModel = nodesToChoose_.getModel();
    int numElem = myModel.getSize();
    for (int i = 0; i < numElem; i++) {
      CheckBoxList.ListChoice choice = (CheckBoxList.ListChoice)myModel.getElementAt(i);
      if (choice.isSelected) {
        modMembers.add((String)choice.getObject());
      }
    } 
    return (ModificationCommands.resetModuleMembers(uics_, modMembers, dacx_, moduleID_, ovrID_, uFac_));
  }  
}
