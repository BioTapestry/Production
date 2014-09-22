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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PanelCommands;
import org.systemsbiology.biotapestry.cmd.flow.add.PropagateSupport;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.nav.ZoomTarget;
import org.systemsbiology.biotapestry.ui.BusProperties;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Frame for pulling down elements from the root model
*/

public class PullDownFromRootFrame extends JDialog implements ActionListener, ClickableClient {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox<ObjChoiceContent> regionCombo1_;
  private JComboBox<ObjChoiceContent> regionCombo2s_;
  private JComboBox<ObjChoiceContent> regionCombo2t_;  
  private Vector<ObjChoiceContent> regions_;
  private JLabel regionLabel1_;
  private JLabel regionLabel2s_;
  private JLabel regionLabel2t_;  
  private DBGenome rootGenome_;
  private GenomeInstance rgi_;
  private ClickableModelViewPanel msp_;
  private Set<String> currTargs_;
  private DBGenome reducedLinkGenome_;
//  private LocalGenomeSource lgs_;
  private DataAccessContext rcx_;
 // private Layout tmpLo_; //
  // Add mode cancel options:
  //
  
 // public static final int CANCEL_ADDS_ALL_MODES        = 0x00;
 // public static final int CANCEL_ADDS_SKIP_PULLDOWNS   = 0x01;  
 // public static final int CANCEL_ADDS_SKIP_MODULE_ADDS = 0x02; 

  
  private JRadioButton drawInOne_;
  private JRadioButton drawLinksInTwo_;
  private BTState appState_;
  
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
  
  public PullDownFromRootFrame(BTState appState, DBGenome rootGenome, GenomeInstance rgi) {
    super(appState.getTopFrame(), appState.getRMan().getString("rootPullDown.title"), false);
    appState_ = appState;
    ResourceManager rMan = appState.getRMan();
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        closeDown();
      }
    });
 
    setSize(800, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
    rgi_ = rgi;
    rootGenome_ = rootGenome;
  //  tmpLo_ = null;
    
    //
    // Operation selection:
    //
    
    JLabel label = new JLabel(rMan.getString("rootPullDown.chooseOperation"));
    drawInOne_ = new JRadioButton(rMan.getString("rootPullDown.drawInOne"), true);
    drawLinksInTwo_ = new JRadioButton(rMan.getString("rootPullDown.drawLinksInTwo"), false);
          
    ButtonGroup group = new ButtonGroup();
    group.add(drawInOne_);
    group.add(drawLinksInTwo_);
    
    ButtonTracker bt = new ButtonTracker();    
    drawInOne_.addActionListener(bt);
    drawLinksInTwo_.addActionListener(bt);

    //
    // Build the region selections:
    //
    
    regions_ = buildCombo();    

    regionLabel1_ = new JLabel(rMan.getString("rootPullDown.region"));
    regionCombo1_ = new JComboBox<ObjChoiceContent>(regions_);
    regionCombo1_.addActionListener(this);    
    regionLabel2s_ = new JLabel(rMan.getString("rootPullDown.sourceRegion"));    
    regionCombo2s_ = new JComboBox<ObjChoiceContent>(regions_);
    regionCombo2s_.addActionListener(this);    
    regionLabel2t_ = new JLabel(rMan.getString("rootPullDown.targetRegion"));    
    regionCombo2t_ = new JComboBox<ObjChoiceContent>(regions_);
    regionCombo2t_.addActionListener(this);
    
    UiUtil.gbcSet(gbc, 0, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 0, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    cp.add(drawInOne_, gbc);
    
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    cp.add(regionLabel1_, gbc);    
    
    UiUtil.gbcSet(gbc, 1, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(regionCombo1_, gbc); 
    
    UiUtil.gbcSet(gbc, 0, 3, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(drawLinksInTwo_, gbc);      
    
    UiUtil.gbcSet(gbc, 0, 4, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    cp.add(regionLabel2s_, gbc);    
    
    UiUtil.gbcSet(gbc, 1, 4, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(regionCombo2s_, gbc);    

    UiUtil.gbcSet(gbc, 2, 4, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    cp.add(regionLabel2t_, gbc);    
    
    UiUtil.gbcSet(gbc, 3, 4, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(regionCombo2t_, gbc);    
    
    //
    // Build the selection panel:
    //
    
    LocalGenomeSource lgs = new LocalGenomeSource();
    rcx_ = new DataAccessContext(null, null, false, false, 0.0,
                                appState_.getFontMgr(), appState_.getDisplayOptMgr(), 
                                appState_.getFontRenderContext(), lgs, appState_.getDB(), false, 
                                new FullGenomeHierarchyOracle(lgs, appState_.getDB()), appState_.getRMan(), 
                                new LocalGroupSettingSource(), appState_.getDB(), appState_.getDB(),
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), 
                                appState_.getDB(), appState_.getDB()
    );    
    
    ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(appState_, this, false, rcx_);
    msp_ = (ClickableModelViewPanel)mvpwz.getModelView();
    lgs.setGenome(rootGenome_);
    
    UiUtil.gbcSet(gbc, 0, 5, 4, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(mvpwz, gbc);

    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.close"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          closeDown();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     

    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);

    //
    // Build the frame
    //
    UiUtil.gbcSet(gbc, 0, 10, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setActiveFields(true); 
    setLocationRelativeTo(appState_.getTopFrame());
  }

 /***************************************************************************
  **
  ** Close down code
  */
  
  public void closeDown() {
    if (appState_.getCommonView().getPullFrame() != null) {
      appState_.getPanelCmds().cancelAddMode(PanelCommands.CANCEL_ADDS_ALL_MODES);
    }
    setVisible(false);
    dispose();    
    return;
  }   
 
  /***************************************************************************
  **
  ** Override show.  Gotta use this tho deprecated!
  */
  
  public void show() {
    super.show();
    //
    // Can't do this before the window opens (get internal error sizing fonts)
    //
    updateNetworkDisplay(true);
    return;
  }  

  /***************************************************************************
  **
  ** Track region selections
  ** 
  */

  public void actionPerformed(ActionEvent e) {
    try { 
      boolean isDrawOne = drawInOne_.isSelected();
      updateNetworkDisplay(isDrawOne);
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private class ButtonTracker implements ActionListener {
    public void actionPerformed(ActionEvent ev) {
      try {
        boolean isDrawOne = drawInOne_.isSelected();
        setActiveFields(isDrawOne); 
        updateNetworkDisplay(isDrawOne);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE VIZ & PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  void setActiveFields(boolean isDrawOne) {
    regionLabel1_.setEnabled(isDrawOne);
    regionCombo1_.setEnabled(isDrawOne);
    regionLabel2s_.setEnabled(!isDrawOne);
    regionCombo2s_.setEnabled(!isDrawOne);
    regionLabel2t_.setEnabled(!isDrawOne); 
    regionCombo2t_.setEnabled(!isDrawOne);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE VIZ & PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Update the network display
  ** 
  */

  private void updateNetworkDisplay(boolean isDrawOne) { 
    
    if (regions_.size() == 0) {
      rcx_.setGenome(null);
      rcx_.setLayout(null);
      msp_.showBackgroundRegion(null, null);
      msp_.repaint();
      return;
    }
    
    String genomeID = rootGenome_.getID();
    rcx_.setGenome(rootGenome_);

    String regionID1 = null;
    String regionID2 = null;    
    if (isDrawOne) {      
      regionID1 = ((ObjChoiceContent)regionCombo1_.getSelectedItem()).val;
    } else {
      regionID1 = ((ObjChoiceContent)regionCombo2s_.getSelectedItem()).val;
      regionID2 = ((ObjChoiceContent)regionCombo2t_.getSelectedItem()).val;    
    }
    
    buildTargetSet(isDrawOne, regionID1, regionID2);
    msp_.setTargets(currTargs_, (reducedLinkGenome_ == null) ? null : reducedLinkGenome_.getID());
    rcx_.setLayout(rcx_.lSrc.getLayoutForGenomeKey(genomeID));          
    msp_.showFullModel();
      
    if (isDrawOne) {
      Layout lo = appState_.getLayoutForGenomeKey(rgi_.getID());
      GroupProperties gp = lo.getGroupProperties(regionID1);
      Rectangle rect = msp_.getCurrentBasicBounds(true, false, ZoomTarget.NO_MODULES);
      Color gCol = gp.getColor(true, appState_.getDB());
      msp_.showBackgroundRegion(gCol, rect);
    } else {
      msp_.showBackgroundRegion(null, null);
    }
    msp_.repaint();
    return;
  }

  /***************************************************************************
  **
  ** Build set of OK targets and an associated layout
  ** 
  */
  
  private void buildTargetSet(boolean isDrawOne, String groupID1, String groupID2) {
    
    currTargs_ = new HashSet<String>();
    Group group = rgi_.getGroup(groupID1);
    Iterator<Node> nit = rootGenome_.getAllNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      if (!group.instanceIsInGroup(nodeID)) {
        currTargs_.add(nodeID);
      }
    }
    
    HashSet<String> useLinks = new HashSet<String>();
    HashSet<String> allLinks = new HashSet<String>();    
    Iterator<Linkage> lit = rootGenome_.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      if (!rgi_.linkInstanceExists(appState_, linkID, groupID1, groupID2)) {
        useLinks.add(linkID);
      } 
      allLinks.add(linkID);  // Everybody needs to be here to get link drawn at all...
    }
    
    reducedLinkGenome_ = rootGenome_.clone();
    reducedLinkGenome_.setID(reducedLinkGenome_.getID() + "__dummy_suffix__");
    lit = rootGenome_.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();    
      String linkID = link.getID();
      if (!useLinks.contains(linkID)) {
        reducedLinkGenome_.removeLinkage(linkID);
      }
    }
    if (!reducedLinkGenome_.getLinkageIterator().hasNext()) {
      ((LocalGenomeSource)rcx_.getGenomeSource()).dropGenome(reducedLinkGenome_.getID());
      reducedLinkGenome_ = null;
    } else {
      ((LocalGenomeSource)rcx_.getGenomeSource()).setGenome(reducedLinkGenome_.getID(), reducedLinkGenome_);
      currTargs_.addAll(allLinks);  // Everybody needs to be here to get link drawn at all...
    }

    return;
  }    
 
  /***************************************************************************
  **
  ** Build the combo of available regions
  ** 
  */
  
  private Vector<ObjChoiceContent> buildCombo() {
    TreeMap<String, String> tm = new TreeMap<String, String>();
    
    //
    // Build up the first level (lex. ordered) from top model.  For lower level models: for each of these,
    // get the available instances in the root instance:
    //
    
    Iterator<Group> git = rgi_.getGroupIterator();
    while (git.hasNext()) {
      Group group = git.next();
      if (group.isASubset(rgi_)) {
        continue;
      }
      String groupMsg = group.getDisplayName();
      String groupID = group.getID();
      tm.put(groupMsg, groupID);
    }
    
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    Iterator<String> tmkit = tm.keySet().iterator();
    while (tmkit.hasNext()) {
      String groupMsg = tmkit.next();
      String groupID = tm.get(groupMsg);
      retval.add(new ObjChoiceContent(groupMsg, groupID)); 
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Pulldown a node
  */
  
  public void nodeIntersected(Intersection intersect) {
    boolean isDrawOne = drawInOne_.isSelected();
    if (!isDrawOne) {
      return;
    }
    String nodeID = intersect.getObjectID();
    if (!currTargs_.contains(nodeID)) {
      return;
    }    
    String regionID = ((ObjChoiceContent)regionCombo1_.getSelectedItem()).val;
    HashSet<String> nodes = new HashSet<String>();
    nodes.add(nodeID);
    PropagateSupport.pullDownsFromFrame(appState_, nodes, new HashSet<String>(), regionID, null);
    buildTargetSet(isDrawOne, regionID, null);
    msp_.setTargets(currTargs_, (reducedLinkGenome_ == null) ? null : reducedLinkGenome_.getID());
    msp_.repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Pulldown a link
  */
  
  public void linkIntersected(Intersection intersect) { 
    boolean isDrawOne = drawInOne_.isSelected();
        
    Layout lo = appState_.getLayoutForGenomeKey(rootGenome_.getID());
    BusProperties bp = lo.getLinkProperties(intersect.getObjectID());
    LinkSegmentID segID = intersect.segmentIDFromIntersect();
    Set<String> linksThru = bp.resolveLinkagesThroughSegment(segID);
    HashSet<String> setIntersect = new HashSet<String>(currTargs_);
    setIntersect.retainAll(linksThru);
    if (setIntersect.isEmpty()) {
      return;
    }
    
    String regionID1 = null;
    String regionID2 = null;    
    if (isDrawOne) {      
      regionID1 = ((ObjChoiceContent)regionCombo1_.getSelectedItem()).val;
    } else {
      regionID1 = ((ObjChoiceContent)regionCombo2s_.getSelectedItem()).val;
      regionID2 = ((ObjChoiceContent)regionCombo2t_.getSelectedItem()).val;    
    }
    PropagateSupport.pullDownsFromFrame(appState_, new HashSet<String>(), linksThru, regionID1, regionID2);
    buildTargetSet(isDrawOne, regionID1, regionID2);
    msp_.setTargets(currTargs_, (reducedLinkGenome_ == null) ? null : reducedLinkGenome_.getID());
    msp_.repaint();
    return;
  }
}
