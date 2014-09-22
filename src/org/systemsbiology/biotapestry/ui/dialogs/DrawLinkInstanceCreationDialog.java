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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBLinkage;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for creating links
*/

public class DrawLinkInstanceCreationDialog extends JDialog implements ActionListener {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JComboBox signCombo_;
  private JComboBox importCombo_;  
  private String nameResult_;
  private int signResult_;
  private String idResult_;
  private boolean haveResult_;  
  private JRadioButton drawNew_;
  private JRadioButton drawOld_;
  private JLabel signLabel_;
  private JLabel nameLabel_;
  private JLabel importLabel_;
  private ModelViewPanel msp_;
  private Map<String, Set<String>> okSrcTrgMap_;
  private Genome rootGenome_;
  private GenomeInstance tgi_;
  private boolean immediateAdd_;
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
  
  public DrawLinkInstanceCreationDialog(BTState appState, Genome rootGenome, 
                                        GenomeInstance tgi) {     
    super(appState.getTopFrame(), appState.getRMan().getString("licreate.title"), true);
    appState_ = appState;
    ResourceManager rMan = appState_.getRMan();    
    setSize(700, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    haveResult_ = false;
    signResult_ = Linkage.NONE;
    rootGenome_ = rootGenome;
    tgi_ = tgi;
    immediateAdd_ = false;
    
    JLabel label = new JLabel(rMan.getString("licreate.chooseSource"));

    drawNew_ = new JRadioButton(rMan.getString("licreate.drawNew"), true);
    drawOld_ = new JRadioButton(rMan.getString("licreate.drawOld"), false);
          
    ButtonGroup group = new ButtonGroup();
    group.add(drawNew_);
    group.add(drawOld_);
    
    ButtonTracker bt = new ButtonTracker();
    
    drawNew_.addActionListener(bt);
    drawOld_.addActionListener(bt);
     
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(drawNew_, gbc);
    
    UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(drawOld_, gbc);     
    
    //
    // Build the link type selection
    //
    
    signLabel_ = new JLabel(rMan.getString("lcreate.sign"));
    Vector<ChoiceContent> choices = DBLinkage.getSignChoices(appState_);
    signCombo_ = new JComboBox(choices);
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(signLabel_, gbc);
    
    UiUtil.gbcSet(gbc, 1, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(signCombo_, gbc);
        
    //
    // Build the name panel:
    //

    nameLabel_ = new JLabel(rMan.getString("lcreate.name"));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    cp.add(nameLabel_, gbc);

    UiUtil.gbcSet(gbc, 1, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(nameField_, gbc);    

    //
    // Build the import panel:
    //

    importLabel_ = new JLabel(rMan.getString("licreate.imports"));
    ArrayList<ObjChoiceContent> imports = buildCombo(tgi);
    importCombo_ = new JComboBox(imports.toArray());
    if (imports.size() == 0) {
      drawOld_.setEnabled(false);
    }
    
    UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(importLabel_, gbc);
    
    UiUtil.gbcSet(gbc, 1, 3, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(importCombo_, gbc); 
     
    importCombo_.addActionListener(this);
    
    // Note this is NOT legit, hitting the database from a dialog!
    rcx_ = new DataAccessContext(appState_, (String)null, (Layout)null);
    
    //
    // Build the position panel:
    //

    ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(appState_, null, rcx_);
    msp_ = mvpwz.getModelView();
 
    UiUtil.gbcSet(gbc, 0, 4, 3, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(mvpwz, gbc);
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("lcreate.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            DrawLinkInstanceCreationDialog.this.setVisible(false);
            DrawLinkInstanceCreationDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("lcreate.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);        
          DrawLinkInstanceCreationDialog.this.setVisible(false);
          DrawLinkInstanceCreationDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 7, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
    setActiveFields(true);
  }

  /***************************************************************************
  **
  ** Track list selections
  ** 
  */

  public void actionPerformed(ActionEvent e) {
    try { 
      updateLinkDisplay(false);
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get the result ID (may be null)
  ** 
  */
  
  public String getID() {
    return (idResult_);
  }  

  /***************************************************************************
  **
  ** Get the sign type
  ** 
  */
  
  public int getSign() {
    return (signResult_);
  }  
  
  /***************************************************************************
  **
  ** Get the allowable source/target options
  */
  
  public Map<String, Set<String>> getOkSrcTrgMap() {
    return (okSrcTrgMap_);
  }    
  
  /***************************************************************************
  **
  ** Get the name result.  May be a blank string, or null
  ** 
  */
  
  public String getName() {
    return (nameResult_);
  }

 /***************************************************************************
  **
  ** Answers if we have an immediate add
  ** 
  */
  
  public boolean haveImmediateAdd() {
    return (immediateAdd_);
  }    
  
  /***************************************************************************
  **
  ** Answers if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }    
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private class ButtonTracker implements ActionListener {
    public void actionPerformed(ActionEvent ev) {
      try {
        boolean isDrawNew = drawNew_.isSelected();
        setActiveFields(isDrawNew);
        updateLinkDisplay(isDrawNew);
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

  void setActiveFields(boolean isDrawNew) {
    signLabel_.setEnabled(isDrawNew);
    nameLabel_.setEnabled(isDrawNew);
    importLabel_.setEnabled(!isDrawNew);       
    nameField_.setEnabled(isDrawNew);
    signCombo_.setEnabled(isDrawNew);
    importCombo_.setEnabled(!isDrawNew);
    return;
  }  
  
  /***************************************************************************
  **
  ** Build the combo of available links
  ** 
  */
  
  private ArrayList<ObjChoiceContent> buildCombo(GenomeInstance tgi) {
    TreeMap<String, List<String>> tm = new TreeMap<String, List<String>>();
    HashMap<String, Map<String, List<String>>> instanceMsgs = new HashMap<String, Map<String, List<String>>>();
    
    //
    // Build up the first level (lex. ordered) from top model.  For lower level models: for each of these,
    // get the available instances in the root instance:
    //
    
    Database db = appState_.getDB();
    Genome genome = db.getGenome();
    GenomeInstance rgi = tgi.getVfgParentRoot();
    boolean buildInstances = (rgi != null);
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkMsg = link.getDisplayString(genome, false);
      String linkID = link.getID();
      List<String> perMsg = tm.get(linkMsg);
      if (perMsg == null) {
        perMsg = new ArrayList<String>();
        tm.put(linkMsg, perMsg);
      }
      perMsg.add(linkID);
      if (buildInstances) {
        TreeMap<String, List<String>> perLink = new TreeMap<String, List<String>>();
        instanceMsgs.put(linkID, perLink);
        Iterator<String> rlit = rgi.returnLinkInstanceIDsForBacking(linkID).iterator();
        while (rlit.hasNext()) {
          String rlinkID = rlit.next();
          if (tgi.getLinkage(rlinkID) != null) {
            continue;
          }
          LinkageInstance rlink = (LinkageInstance)rgi.getLinkage(rlinkID);
          String rlinkMsg = rlink.getDisplayStringGroupOnly(rgi);
          // Can you really get multiple copies of the same group tuple for
          // a single instance?
          List<String> perRlinkMsg = perLink.get(rlinkMsg);
          if (perRlinkMsg == null) {
            perRlinkMsg = new ArrayList<String>();
            perLink.put(rlinkMsg, perRlinkMsg);
          }
          perRlinkMsg.add(rlinkID);
        }
      }
    }
    
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    Iterator<String> tmkit = tm.keySet().iterator();
    while (tmkit.hasNext()) {
      String linkMsg = tmkit.next();
      List<String> perMsg = tm.get(linkMsg);
      int pmNum = perMsg.size();
      for (int i = 0; i < pmNum; i++) {
        String linkID = perMsg.get(i);
        retval.add(new ObjChoiceContent(linkMsg, linkID)); 
        if (buildInstances) {
          Map<String, List<String>> perLink = instanceMsgs.get(linkID);
          Iterator<String> plkit = perLink.keySet().iterator();
          while (plkit.hasNext()) {
            String rlinkMsg = plkit.next();
            List<String> perRlinkMsg = perLink.get(rlinkMsg);
            int prlNum = perRlinkMsg.size();
            for (int j = 0; j < prlNum; j++) {
              String rlinkID = perRlinkMsg.get(j);
              retval.add(new ObjChoiceContent("   " + rlinkMsg, rlinkID));
            }
          }
        }        
      }
    }
    
    return (retval);
  }   
  
 
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      if (drawNew_.isSelected()) {
        nameResult_ = nameField_.getText().trim();
        signResult_ = ((ChoiceContent)signCombo_.getSelectedItem()).val;
        idResult_ = null;
        okSrcTrgMap_ = null;
      } else {
        nameResult_ = null;
        idResult_ = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
        signResult_ = Linkage.NONE;
        GenomeInstance rgi = tgi_.getVfgParentRoot();
        boolean immedOK = (rgi != null);
        
        // When installing an instance, it is immediate IF the source and target
        // nodes are both present!
        if (!GenomeItemInstance.getBaseID(idResult_).equals(idResult_)) {
          Linkage oldLink = rgi.getLinkage(idResult_);        
          String src = oldLink.getSource();
          String trg = oldLink.getTarget();
          ResourceManager rMan = appState_.getRMan();
          if ((tgi_.getNode(src) != null) && (tgi_.getNode(trg) != null)) {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("drawNewLink.immediateAdd"),
                                          rMan.getString("drawNewLink.immediateAddTitle"), 
                                          JOptionPane.PLAIN_MESSAGE);
            immediateAdd_ = true;
            haveResult_ = true;
            return (true);
          } else {
            JOptionPane.showMessageDialog(appState_.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("drawNewLink.noTargs")),
                                          rMan.getString("drawNewLink.noTargsTitle"), 
                                          JOptionPane.WARNING_MESSAGE);
            haveResult_ = false;
            return (false);
          }
        }
        
        okSrcTrgMap_ = buildOKSrcTrgMap();
        if (okSrcTrgMap_.isEmpty()) {
          ResourceManager rMan = appState_.getRMan();
          JOptionPane.showMessageDialog(appState_.getTopFrame(), UiUtil.convertMessageToHtml(rMan.getString("drawNewLink.noTargs")),
                                        rMan.getString("drawNewLink.noTargsTitle"), 
                                        JOptionPane.WARNING_MESSAGE);
          haveResult_ = false;
          return (false);
        // Immediate not OK with root instance (no layout)!
        } else if (immedOK && (okSrcTrgMap_.size() == 1)) {
          Set<String> okTrgs = okSrcTrgMap_.values().iterator().next();
          if (okTrgs.size() == 1) {
            Set<String> rgiInst = rgi.returnLinkInstanceIDsForBacking(idResult_);  // idResult_ is a baseID
            Set<String> tgiInst = tgi_.returnLinkInstanceIDsForBacking(idResult_);  // idResult_ is a baseID
            rgiInst.removeAll(tgiInst);
            if (rgiInst.size() == 1) {
              idResult_ = rgiInst.iterator().next();
              ResourceManager rMan = appState_.getRMan();
              JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("drawNewLink.immediateAdd"),
                                            rMan.getString("drawNewLink.immediateAddTitle"), 
                                            JOptionPane.PLAIN_MESSAGE);
              immediateAdd_ = true;
              haveResult_ = true;
              return (true);            
            }
          }
        }
      }
      haveResult_ = true;
    } else {
      nameResult_ = null;
      idResult_ = null;
      signResult_ = Linkage.NONE;
      haveResult_ = false;
      okSrcTrgMap_ = null;
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Update the link display
  ** 
  */

  private void updateLinkDisplay(boolean isDrawNew) { 
        
    if (isDrawNew) {
      rcx_.setGenome(null);
      rcx_.setLayout(null);
      msp_.repaint();
      return;
    }

    String linkID = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
    Genome genome = (GenomeItemInstance.isBaseID(linkID)) ? rootGenome_ : tgi_.getVfgParentRoot();    
    String genomeID = genome.getID();
    rcx_.setGenome(genome);
    rcx_.setLayout(rcx_.lSrc.getLayoutForGenomeKey(genomeID));
    
    Linkage link = genome.getLinkage(linkID);
    Intersection linkInt = Intersection.pathIntersection(link, rcx_);
    ArrayList<Intersection> selections = new ArrayList<Intersection>();
    selections.add(linkInt);
    msp_.selectLinks(selections);
    msp_.repaint();
    return;
  } 
  
  /***************************************************************************
  **
  ** Figure out where we can draw an existing link
  */  
 
  private Map<String, Set<String>> buildOKSrcTrgMap() {
    
    Linkage oldLink = rootGenome_.getLinkage(idResult_);        
    String src = oldLink.getSource();
    String trg = oldLink.getTarget();
    String oldID = oldLink.getID();
    
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    HashSet<String> trgInstances = new HashSet<String>();
    
    //
    // Build up the map of all possibilities, then prune off
    // the ones that already exist.
    //

    Iterator<Node> anit = tgi_.getAllNodeIterator();
    while (anit.hasNext()) {
      Node node = anit.next();
      String nodeID = node.getID();
      String nodeIDBase = GenomeItemInstance.getBaseID(nodeID);
      if (nodeIDBase.equals(src)) {
        retval.put(nodeID, new HashSet<String>());
      }
      if (nodeIDBase.equals(trg)) {
        trgInstances.add(nodeID);
      }      
    }
    
    Iterator<Set<String>> vit = retval.values().iterator();
    while (vit.hasNext()) {
      Set<String> val = vit.next();
      val.addAll(trgInstances);
    }
    
    //
    // Do the pruning:
    //

    Iterator<Linkage> lit = tgi_.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage link = lit.next();
      String linkID = link.getID();
      String linkIDBase = GenomeItemInstance.getBaseID(linkID);
      if (!linkIDBase.equals(oldID)) {
        continue;
      }
      String linkSrc = link.getSource();
      Set<String> targs = retval.get(linkSrc);
      String linkTrg = link.getTarget();
      targs.remove(linkTrg);  
    }    
    
    //
    // Toss sources with no targs:
    //
    
    HashSet<String> origKeys = new HashSet<String>(retval.keySet());
    Iterator<String> oit = origKeys.iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      Set<String> trgSet = retval.get(key);
      if (trgSet.isEmpty()) {
        retval.remove(key);
      }
    }     
    return (retval); 
  }  
}
