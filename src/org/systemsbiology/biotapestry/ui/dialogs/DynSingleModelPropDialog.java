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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.SliderChange;
import org.systemsbiology.biotapestry.cmd.flow.remove.RemoveNode;
import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ImageChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.NavTreeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.ProxyChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.SliderStateChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.UserTreePathControllerChangeCmd;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.ProxyChange;
import org.systemsbiology.biotapestry.nav.ImageChange;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.NavTreeChange;
import org.systemsbiology.biotapestry.nav.UserTreePathChange;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing single model property data
*/

public class DynSingleModelPropDialog extends JDialog implements ListWidgetClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox typeCombo_;
  private JTextField minField_;
  private JTextField maxField_;
  private JTextField nameField_;
  private DynamicInstanceProxy proxy_;
  private DynamicInstanceProxy parentProx_;
  private List proxyChildren_;
  private int minChildTime_;
  private int maxChildTime_;
  private int minParentTime_;
  private int maxParentTime_;
  private ListWidget lw_;
  private List<ObjChoiceContent> extras_;
  private HashMap<String, String> extraGroups_;
  private NavTree nt_;
  private TreeNode popupNode_;
  private TimeAxisDefinition tad_;
  private boolean namedStages_;  
  
  private BTState appState_;
  private DataAccessContext dacx_;
  
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
  
  public DynSingleModelPropDialog(BTState appState, DataAccessContext dacx, DynamicInstanceProxy proxy,
                                  DynamicInstanceProxy parentProx, 
                                  List proxyChildren, NavTree nt, TreeNode popupNode) {     
    super(appState.getTopFrame(), appState.getRMan().getString("dsmprop.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    proxy_ = proxy;
    parentProx_ = parentProx;
    proxyChildren_ = proxyChildren;
    nt_ = nt;
    popupNode_ = popupNode;
    calcTimeBounds();    
   
    ResourceManager rMan = appState_.getRMan();
    setSize(700, proxy_.hasAddedNodes() ? 300 : 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    
    tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
    namedStages_ = tad_.haveNamedStages();
    String displayUnits = tad_.unitDisplayString();    
    
    //
    // Build the timely choice:
    //
    
    JLabel label = new JLabel(rMan.getString("dsmprop.type"));
    Vector<String> choices = new Vector<String>();
    String perTime = MessageFormat.format(rMan.getString("dicreate.perTimeChoice"), new Object[] {displayUnits});
    choices.add(perTime);    
    choices.add(rMan.getString("dicreate.sumChoice")); 
    typeCombo_ = new JComboBox(choices);

    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(typeCombo_, gbc);
    
    //
    // Build the minimum time:
    //

    String minLab = MessageFormat.format(rMan.getString("dsmprop.minTime"), new Object[] {displayUnits});    
    label = new JLabel(minLab);
    minField_ = new JTextField();
    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 4, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(minField_, gbc);
    
    //
    // Build the maximum time:
    //

    String maxLab = MessageFormat.format(rMan.getString("dsmprop.maxTime"), new Object[] {displayUnits});    
    label = new JLabel(maxLab);
    maxField_ = new JTextField();
    UiUtil.gbcSet(gbc, 5, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 6, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(maxField_, gbc);
    
    //
    // Build the name panel:
    //

    label = new JLabel(rMan.getString("dsmprop.name"));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 1, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(nameField_, gbc);
    
    //
    // Build the extra added node list:
    //
    
    int row = 2;
    if (proxy_.hasAddedNodes()) {
      label = new JLabel(rMan.getString("dsmprop.extra"));
      UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 1.0);
      cp.add(label, gbc);      
      generateAddedNodes();
      lw_ = new ListWidget(appState_, extras_, this, (parentProx_ == null) ? 
                                             ListWidget.DELETE_EDIT : 
                                             ListWidget.DELETE_ONLY); 
      UiUtil.gbcSet(gbc, 1, row, 6, 2, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
      cp.add(lw_, gbc);
      row += 2;
    }
                
    //
    // Build the button panel:
    //
 
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (applyProperties()) {
            DynSingleModelPropDialog.this.setVisible(false);
            DynSingleModelPropDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          DynSingleModelPropDialog.this.setVisible(false);
          DynSingleModelPropDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, row, 7, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Add a row to the list
  */
  
  public List addRow(ListWidget widget) {
    // not used
    return (null);
  }
  
  /***************************************************************************
  **
  ** Delete rows from the ORDERED list
  */
  
  public List deleteRows(ListWidget widget, int[] rows) {
    for (int i = 0; i < rows.length; i++) {
      extras_.remove(rows[i]);
      for (int j = i + 1; j < rows.length; j++) {
        if (rows[j] > rows[i]) {
          rows[j]--;
        }
      }
    }
    return (extras_);   
  }

  /***************************************************************************
  **
  ** Edit a row from the list
  */
  
  public List editRow(ListWidget widget, int[] selectedRows) {
    if (selectedRows.length != 1) {
      throw new IllegalArgumentException();
    }

    //
    // Editing a row means popping up a dialog to let the user change
    // the region binding.  Only applicable for top-level dynamic instances.
    //

    ObjChoiceContent occ = extras_.get(selectedRows[0]);
    String nodeName = occ.val;
    String groupToUse = extraGroups_.get(nodeName);

    GenomeInstance gi = proxy_.getAnInstance();
    GenomeInstance parent = (gi).getVfgParent();         
 
    ArrayList<ObjChoiceContent> choices = new ArrayList<ObjChoiceContent>();
    ObjChoiceContent defObj = null;
    Iterator<Group> grit = parent.getGroupIterator();
    while (grit.hasNext()) {
      Group group = grit.next();
      String groupID = group.getID();
      ObjChoiceContent ccont = new ObjChoiceContent(group.getDisplayName(), 
                                                    Group.getBaseID(groupID));
      choices.add(ccont);
      if (groupID.equals(groupToUse)) {
        defObj = ccont;
      } 
    }

    Object[] objs = choices.toArray();
    ObjChoiceContent groupChoice = 
      (ObjChoiceContent)JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                                    dacx_.rMan.getString("dsmprop.extraGroup"), 
                                                    dacx_.rMan.getString("dsmprop.extraGroupTitle"),     
                                                    JOptionPane.QUESTION_MESSAGE, null, 
                                                    objs, defObj);
    if (groupChoice != null) { 
      if (!groupChoice.val.equals(defObj.val)) {
        extraGroups_.put(nodeName, groupChoice.val);
      }
    }
    
    return (extras_);
  }
  
  /***************************************************************************
  **
  ** Merge rows from the list
  */
  
  public List combineRows(ListWidget widget, int[] selectedRows) {
    // not used
    return (null);
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
  ** Generate the list of added nodes
  ** 
  */
  
  private void generateAddedNodes() {
    ResourceManager rMan = appState_.getRMan();
    extras_ = new ArrayList<ObjChoiceContent>();
    extraGroups_ = new HashMap<String, String>();
   
    String format = rMan.getString("addedNode.nameFormat");
    String multi = rMan.getString("addedNode.multiSuffix");
    String noName = rMan.getString("tirmd.noName");
    GenomeInstance gi = proxy_.getAnInstance();
    GenomeInstance giRoot = gi.getVfgParentRoot();
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    
    Iterator<DynamicInstanceProxy.AddedNode> anit = proxy_.getAddedNodeIterator();
    while (anit.hasNext()) {
      DynamicInstanceProxy.AddedNode an = anit.next();
      Node node = giRoot.getNode(an.nodeName);
      String nodeName = node.getName();
      if ((nodeName == null) || (nodeName.trim().equals(""))) {
        nodeName = noName;
      }
      List<TimeCourseData.TCMapping> tcms = tcd.getTimeCourseTCMDataKeysWithDefault(GenomeItemInstance.getBaseID(an.nodeName));
      ObjChoiceContent occ;
      if ((tcms == null) || (tcms.size() == 0)) {
        occ = new ObjChoiceContent(nodeName, an.nodeName);
      } else if (tcms.size() == 1) {
        TimeCourseData.TCMapping tcm = tcms.get(0);
        String desc = MessageFormat.format(format, new Object[] {nodeName, tcm.name});         
        occ = new ObjChoiceContent(desc, an.nodeName);    
      } else {
        String multiName = nodeName + multi;
        String desc = MessageFormat.format(format, new Object[] {nodeName, multiName});         
        occ = new ObjChoiceContent(desc, an.nodeName);
      }
      extras_.add(occ);
      extraGroups_.put(an.nodeName, an.groupToUse);
    }
    
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply the current model property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    
    int index = (proxy_.isSingle()) ? 1 : 0;   
    typeCombo_.setSelectedIndex(index);
    if (!proxyChildren_.isEmpty()) {
      typeCombo_.setEnabled(false);
    }
    
    int minTime = proxy_.getMinimumTime();    
    int maxTime = proxy_.getMaximumTime();    
    String initMin = (namedStages_) ? tad_.getNamedStageForIndex(minTime).name : Integer.toString(minTime);
    String initMax = (namedStages_) ? tad_.getNamedStageForIndex(maxTime).name : Integer.toString(maxTime);

    minField_.setText(initMin); 
    maxField_.setText(initMax);    
    nameField_.setText(proxy_.getName());  
    return;
  }
  
  /***************************************************************************
  **
  ** Figure out required time ranges given proxies
  ** 
  */
  
  private void calcTimeBounds() {
    
    //
    // No bigger than parent:
    //
    
    minParentTime_ = Integer.MIN_VALUE;
    maxParentTime_ = Integer.MAX_VALUE;    
    if (parentProx_ != null) {
      minParentTime_ = parentProx_.getMinimumTime();
      maxParentTime_ = parentProx_.getMaximumTime();      
    }
    
    //
    // No smaller than children:
    //
    
    int size = proxyChildren_.size();
    minChildTime_ = Integer.MAX_VALUE;
    maxChildTime_ = Integer.MIN_VALUE;
    for (int i = 0; i < size; i++) {
      DynamicInstanceProxy dip = (DynamicInstanceProxy)proxyChildren_.get(i);
      int min = dip.getMinimumTime();
      if (min < minChildTime_) {
        minChildTime_ = min;
      }
      int max = dip.getMaximumTime();
      if (max > maxChildTime_) {
        maxChildTime_ = max;
      }      
    }  
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the model
  ** 
  */
  
  private boolean applyProperties() {

    DataAccessContext rcx = new DataAccessContext(dacx_, proxy_.getFirstProxiedKey());
    
    //
    // Change rules:
    //
    // If we have children, we cannot change to per-time
    // If we have children, our time bounds must span the child
    // time bounds.
    // Our time bounds must also be within the time bounds of
    // our parent.
    //
    

    int minResult;
    int maxResult;
    
    if (namedStages_) {
      minResult = tad_.getIndexForNamedStage(minField_.getText());
      maxResult = tad_.getIndexForNamedStage(maxField_.getText());          
      if ((minResult == TimeAxisDefinition.INVALID_STAGE_NAME) ||
          (maxResult == TimeAxisDefinition.INVALID_STAGE_NAME)) {
        ResourceManager rMan = appState_.getRMan(); 
        JOptionPane.showMessageDialog(this, rMan.getString("dsmprop.badStageName"),
                                      rMan.getString("dsmprop.badStageNameTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);          
      }
    } else {      
      try {
        minResult = Integer.parseInt(minField_.getText());
        maxResult = Integer.parseInt(maxField_.getText());
      } catch (NumberFormatException nfex) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(this, rMan.getString("dsmprop.badNumber"),
                                      rMan.getString("dsmprop.badNumberTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }    

    if ((minResult < minParentTime_) || (maxResult > maxParentTime_) ||
        (minResult > minChildTime_) || (maxResult < maxChildTime_)) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(this, rMan.getString("dsmprop.badBounds"),
                                    rMan.getString("dsmprop.badBoundsTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);      
    }

    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.dsmprop");     
    
    String nameResult = nameField_.getText().trim();
    boolean isPerTimeResult = (typeCombo_.getSelectedIndex() == 0);
    boolean wasPerTimeResult = !proxy_.isSingle();
    
    List<String> preChangeKeys = proxy_.getProxiedKeys();
    String preChangeKey = preChangeKeys.get(0); 
    
    UserTreePathController utpctrl = appState_.getPathController();
    support.addEdit(new UserTreePathControllerChangeCmd(appState_, rcx, utpctrl.recordStateForUndo(true)));

    DynamicInstanceProxy.ChangeBundle pc = proxy_.changeProperties(appState_, nameResult, isPerTimeResult, 
                                                                   minResult, maxResult);
    
    for (int i = 0; i < pc.imageChanges.length; i++) {
      ImageChange ic = pc.imageChanges[i];
      if (ic != null) {
        support.addEdit(new ImageChangeCmd(appState_, rcx, ic));
      }
    }
    for (int i = 0; i < pc.pathChanges.length; i++) {
      UserTreePathChange utpc = pc.pathChanges[i];
      if (utpc != null) {
        support.addEdit(new UserTreePathChangeCmd(appState_, rcx, utpc));
      }
    } 
    if (pc.dc != null) {
      support.addEdit(new DatabaseChangeCmd(appState_, rcx, pc.dc));      
    }
 
    if (pc.proxyChange != null) {
      ProxyChangeCmd cmd = new ProxyChangeCmd(appState_, rcx, pc.proxyChange);
      support.addEdit(cmd);
    }
    
    List<String> postChangeKeys = proxy_.getProxiedKeys();
    String postChangeKey = postChangeKeys.get(0); 
 
    applyNavTreeChange(support, rcx, isPerTimeResult, wasPerTimeResult);

    applyExtraNodeChanges(support, rcx);
    
    applySliderChange(support, rcx);
    
    support.addEdit(new UserTreePathControllerChangeCmd(appState_, rcx, utpctrl.recordStateForUndo(false)));
    
    issueEvents(support, isPerTimeResult, wasPerTimeResult, preChangeKey, postChangeKey);
    
    support.finish();         
    return (true);
  }
  
  /***************************************************************************
  **
  ** Apply extra node changes
  ** 
  */
  
  private void applyExtraNodeChanges(UndoSupport support, DataAccessContext rcx) {
     
    //
    // Crank through the extra nodes.  If any are missing from our local
    // list, we delete them.  If any have different group bindings, we
    // change them.
    //
    
    boolean needGeneral = false;
    String proxID = proxy_.getID();
        
    Iterator<DynamicInstanceProxy.AddedNode> anit = proxy_.getAddedNodeIterator();
    ArrayList<String> deadList = new ArrayList<String>();
    while (anit.hasNext()) {
      DynamicInstanceProxy.AddedNode an = anit.next();
      boolean match = false;
      for (int i = 0; i < extras_.size(); i++) {
        ObjChoiceContent occ = extras_.get(i);
        if (occ.val.equals(an.nodeName)) {
          match = true;
          String groupToUse = extraGroups_.get(occ.val);
          if (!groupToUse.equals(an.groupToUse)) {
            ProxyChange pc = proxy_.changeExtraNodeGroupBinding(an.nodeName, groupToUse);
            if (pc != null) {
              ProxyChangeCmd cmd = new ProxyChangeCmd(appState_, rcx, pc);
              support.addEdit(cmd);
              //
              // We also need to handle nodes underneath
              //   
              Iterator<DynamicInstanceProxy> pxit = rcx.getGenomeSource().getDynamicProxyIterator();        
              while (pxit.hasNext()) {
                DynamicInstanceProxy dip = pxit.next();
                if ((!dip.getID().equals(proxID)) && dip.proxyIsAncestor(proxID) && dip.hasAddedNode(an.nodeName)) {
                  pc = dip.changeExtraNodeGroupBinding(an.nodeName, groupToUse);
                  if (pc != null) {
                    support.addEdit(new ProxyChangeCmd(appState_, rcx, pc));
                    // Don't want to enumerate all those dynamic instances for eventing.
                    // This is a stand in that forces update of all dynamic models:
                    needGeneral = true;
                  }
                }
              }
            }
          }
          break;
        }
      }
      if (!match) {
        deadList.add(an.nodeName);
      }
    }
      
    Iterator<String> dlit = deadList.iterator();
    while (dlit.hasNext()) {
      String delNode = dlit.next();
      ProxyChange pc = proxy_.deleteExtraNode(delNode);
      if (pc != null) {
        ProxyChangeCmd cmd = new ProxyChangeCmd(appState_, rcx, pc);
        support.addEdit(cmd);
        Iterator<DynamicInstanceProxy> pxit = rcx.getGenomeSource().getDynamicProxyIterator();        
        while (pxit.hasNext()) {
          DynamicInstanceProxy dip = pxit.next();
          if ((!dip.getID().equals(proxID)) && dip.proxyIsAncestor(proxID) && dip.hasAddedNode(delNode)) {
            pc = dip.deleteExtraNode(delNode);
            if (pc != null) {
              cmd = new ProxyChangeCmd(appState_, rcx, pc);
              support.addEdit(cmd);
              // Don't want to enumerate all those dynamic instances for eventing.
              // This is a stand in that forces update of all dynamic models:
              needGeneral = true;
            }
          }
        }
      }
    }
    
    if (!deadList.isEmpty()) {
      RemoveNode.proxyPostExtraNodeDeletionSupport(appState_, rcx, support);
    }    

    if (needGeneral) {
      GeneralChangeEvent ev = new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE);
      support.addEvent(ev);
    }
    return;
  }

  /***************************************************************************
  **
  ** Apply change to navigation tree
  ** 
  */
  
  private NavTreeChange applyNavTreeChange(UndoSupport support, DataAccessContext rcx, 
                                           boolean isPerTimeResult, boolean wasPerTimeResult) {
    //
    // If we go from per time to summation, the contents of the navigation
    // tree change too, so this handles that.
    //
    if (isPerTimeResult == wasPerTimeResult) {
      return (null);
    }
    
    NavTreeChange ntc;
    if (isPerTimeResult) {
      ntc = nt_.changeToProxy((DefaultMutableTreeNode)popupNode_, proxy_.getID());                                                          
    } else {
      String id = proxy_.getProxiedKeys().get(0);
      ntc = nt_.changeFromProxy((DefaultMutableTreeNode)popupNode_, id);      
    }
    if (ntc != null) {
      support.addEdit(new NavTreeChangeCmd(appState_, rcx, ntc));
    }
    return (ntc);
  }
  
  /***************************************************************************
  **
  ** Apply change to slider bounds
  ** 
  */
  
  private void applySliderChange(UndoSupport support, DataAccessContext dacx) {
    SliderChange sc = appState_.getVTSlider().recordSliderStateForUndo();
    if (sc != null) {
      support.addEdit(new SliderStateChangeCmd(appState_, dacx, sc));
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Apply change to navigation tree
  ** 
  */
  
  private void issueEvents(UndoSupport support, 
                           boolean isPerTimeResult, boolean wasPerTimeResult,
                           String preChangeKey, String postChangeKey) {
                                                  
    if (isPerTimeResult == wasPerTimeResult) {
      if (proxy_.isSingle()) {
        String id = postChangeKey;
        ModelChangeEvent mcev = new ModelChangeEvent(id, ModelChangeEvent.UNSPECIFIED_CHANGE, false);    
        support.addEvent(mcev);    
        mcev = new ModelChangeEvent(id, ModelChangeEvent.PROPERTY_CHANGE, false);     
        support.addEvent(mcev);
      } else {
        ModelChangeEvent mcev = new ModelChangeEvent(proxy_.getID(), ModelChangeEvent.UNSPECIFIED_CHANGE, true);    
        support.addEvent(mcev);    
        mcev = new ModelChangeEvent(proxy_.getID(), ModelChangeEvent.PROPERTY_CHANGE, true);     
        support.addEvent(mcev);
      }
    } else {                    
      //
      // Note: events have to be different depending on direction we are going:
      //
      int uc = ModelChangeEvent.UNSPECIFIED_CHANGE;
      int pc = ModelChangeEvent.PROPERTY_CHANGE;      
      if (proxy_.isSingle()) { // going from per time to single (pre events for reverse):
        support.addPreEvent(new ModelChangeEvent(proxy_.getID(), uc, true, postChangeKey, false));    
        support.addPreEvent(new ModelChangeEvent(proxy_.getID(), pc, true, postChangeKey, false));     
        support.addPostEvent(new ModelChangeEvent(postChangeKey, uc, false, proxy_.getID(), true));
        support.addPostEvent(new ModelChangeEvent(postChangeKey, pc, false, proxy_.getID(), true));      
      } else {  // going from single to per time (pre events for reverse):
        support.addPreEvent(new ModelChangeEvent(preChangeKey, uc, false, proxy_.getID(), true));    
        support.addPreEvent(new ModelChangeEvent(preChangeKey, pc, false, proxy_.getID(), true));     
        support.addPostEvent(new ModelChangeEvent(proxy_.getID(), uc, true, preChangeKey, false));
        support.addPostEvent(new ModelChangeEvent(proxy_.getID(), pc, true, preChangeKey, false));
      } 
    }
    return;
  }
}
