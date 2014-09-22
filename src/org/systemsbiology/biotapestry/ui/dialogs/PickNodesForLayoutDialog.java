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


package org.systemsbiology.biotapestry.ui.dialogs;

import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.GroupMember;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.nav.LayoutManager;
import org.systemsbiology.biotapestry.ui.LayoutDataSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;

/****************************************************************************
**
** Dialog box for picking nodes for layout
*/

public class PickNodesForLayoutDialog extends BTStashResultsDialog implements ListSelectionListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ArrayList<ObjChoiceContent> selected_;
  private JList jlist_;
  private LayoutDataSource masterLds_;
  private LayoutDataSource nodedLds_;
  private ModelViewPanel msp_;
  private BTState appState_;
  private DataAccessContext rcx_;
  
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
  
  public PickNodesForLayoutDialog(BTState appState, LayoutDataSource lds, DataAccessContext rcx) {
    super(appState, "pnfl.title", new Dimension(600, 500), 1);
    appState_ = appState;
    selected_ = new ArrayList<ObjChoiceContent>();
    nodedLds_ = null;
    masterLds_ = lds;
  
    JLabel lab = new JLabel(rMan_.getString("pnfl.pick"));
    addWidgetFullRow(lab, true, true);
 
    ArrayList<ObjChoiceContent> selection = buildSelections(lds);
    jlist_ = new JList(selection.toArray());
    jlist_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    jlist_.addListSelectionListener(this);
    JScrollPane jsp = new JScrollPane(jlist_);
    addTable(jsp, 20);
  
    //
    // Build the selection panel:
    //

    rcx_ = new DataAccessContext(rcx);
    ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(appState, null, rcx_);
    msp_ = mvpwz.getModelView();
    addTable(mvpwz, 10);
    
    finishConstruction();
    
    addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        initSelections(masterLds_, buildSelections(masterLds_));
      }
    });
    
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the result.
  ** 
  */
  
  public LayoutDataSource getLDSWithNodes() {
    return (nodedLds_);
  }
 
  /***************************************************************************
  **
  ** Handle selections
  */
  
  public void valueChanged(ListSelectionEvent lse) {
    try {
      if (lse.getValueIsAdjusting()) {
        return;
      }
      selected_.clear();
      int[] sel = jlist_.getSelectedIndices();
      if ((sel == null) || (sel.length == 0)) {
        updateNodeDisplay(true);
        return;
      } else {
        ListModel jlm = jlist_.getModel();
        for (int i = 0; i < sel.length; i++) {
          selected_.add((ObjChoiceContent)jlm.getElementAt(sel[i]));
        }
        updateNodeDisplay(false);
      }
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;             
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build the list of available nodes
  ** 
  */
  
  private ArrayList<ObjChoiceContent> buildSelections(LayoutDataSource lds) {
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    Database db = appState_.getDB();
    Genome rootGenome = db.getGenome();
    GenomeInstance gi = (GenomeInstance)db.getGenome(lds.getModelID());
    Group useGroup = gi.getGroup(lds.getGroupID());
    Iterator<GroupMember> mit = useGroup.getMemberIterator();
    while (mit.hasNext()) {
      GroupMember meb = mit.next();
      String nodeID = meb.getID();
      NodeInstance node = (NodeInstance)gi.getNode(nodeID);
      Node rootNode = (Node)node.getBacking();
      String display = rootNode.getDisplayString(rootGenome, false);
      retval.add(new ObjChoiceContent(display, nodeID)); 
    }
    Collections.sort(retval);  
    return (retval);
  }

  /***************************************************************************
  **
  ** init the list of available nodes
  ** 
  */
  
  private void initSelections(LayoutDataSource lds, ArrayList<ObjChoiceContent> nodeList) {  
  
    if (!masterLds_.isNodeSpecific()) {
      return;
    } 
    
    HashSet<String> initNodes = new HashSet<String>();
    Iterator<String> mnit = lds.getNodes();
    while (mnit.hasNext()) {
      String nodeID = mnit.next();
      initNodes.add(nodeID);
    }
    
    ListSelectionModel lsm = jlist_.getSelectionModel();
    lsm.clearSelection();
    int numnl = nodeList.size();
    for (int i = 0; i < numnl; i++) {
      ObjChoiceContent occ = nodeList.get(i);
      String nodeID = occ.val;
      if (initNodes.contains(nodeID)) {
        lsm.addSelectionInterval(i, i);
      }        
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Update the node display
  ** 
  */

  private void updateNodeDisplay(boolean doClear) { 
    
    if (doClear) {
      rcx_.setGenome(null);
      rcx_.setLayout(null);
      msp_.repaint();
      return;
    }
    
    String gid = masterLds_.getModelID();
    rcx_.setGenome(rcx_.getGenomeSource().getGenome(gid));
    rcx_.setLayout(rcx_.lSrc.getLayoutForGenomeKey(gid));

    HashSet<String> nodes = new HashSet<String>();
    int numNodes = selected_.size();
    for (int i = 0; i < numNodes; i++) {
      ObjChoiceContent occ = selected_.get(i);
      nodes.add(occ.val);
    }
    msp_.selectNodes(nodes);
    msp_.repaint();
    return;
  }  
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() {
    int numSel = selected_.size();
    if (numSel == 0) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("pnfl.noSelection"), 
                                    rMan.getString("pnfl.noSelectionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      nodedLds_ = null;
      return (false);
    }

    nodedLds_ = masterLds_.clone();
    if (nodedLds_.isNodeSpecific()) {
      nodedLds_.dropAllNodes();
    }

    for (int i = 0; i < numSel; i++) {
      ObjChoiceContent occ = selected_.get(i);
      nodedLds_.addNode(occ.val);
    }
    return (true);
  }
}
