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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** Panel for creating or editing a PertSource
*/

public class PertSourceDefAddOrEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JComboBox srcField_;
  private JComboBox experimentField_; 
  private JComboBox proxyName_;
  private JLabel proxyLab_; 
  private JComboBox proxySignField_;
  private PertSource resultSource_;
  private String currKey_;
  private PerturbationData pd_;
  private PertManageHelper pmh_;
  private EditableTable estAnnot_;
  private ArrayList<EnumCell> annotList_;
  private JButton proxJump_;
  private HashSet allMerge_;
  
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
  
  public PertSourceDefAddOrEditPanel(UIComponentSource uics, DataAccessContext dacx, JFrame parent, PerturbationData pd, PendingEditTracker pet, String myKey) { 
    super(uics, dacx, parent, pet, myKey, 4);
    pd_ = pd;
    pmh_ = new PertManageHelper(uics_, dacx_, parent, pd, rMan_, gbc_, pet_);

    //
    // Source name:
    //
    
    Vector<TrueObjChoiceContent> srcVec = pd_.getSourceNameOptions();
    JLabel srcLabel = new JLabel(rMan_.getString("psdae.src"));    
    srcField_ = new JComboBox(srcVec);
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    add(srcLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, rowNum_, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(srcField_, gbc_);
    
    
    JButton addNewSource = new JButton(rMan_.getString("psdae.manageSources"), pmh_.getJumpIcon());
    addNewSource.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)srcField_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertSrcsAndTargsManagePanel.MANAGER_KEY, 
                                PertSrcsAndTargsManagePanel.SRC_KEY, whichRow); 
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc_, 3, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    add(addNewSource, gbc_);

    //
    // Perturbation type:
    //
    
    JLabel expLabel = new JLabel(rMan_.getString("psdae.experiment"));
    Vector<TrueObjChoiceContent> exps = pd.getPertDictionary().getExperimentTypes();
    experimentField_ = new JComboBox(exps);     
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    add(expLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, rowNum_, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    add(experimentField_, gbc_);
    
    JButton addNewPert = new JButton(rMan_.getString("psdae.managePerts"), pmh_.getJumpIcon());
    addNewPert.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)experimentField_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;         
          pet_.jumpToRemoteEdit(PertPropertiesManagePanel.MANAGER_KEY, 
                                PertPropertiesManagePanel.PERT_KEY, whichRow); 
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc_, 3, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    add(addNewPert, gbc_);
    
    //
    // Handle proxies species (e.g. CAD QCPR represents beta-catenin, with opposite sign)
    //  

    JLabel proxSLab = new JLabel(rMan_.getString("psdae.proxySign"));
    proxyLab_ = new JLabel(rMan_.getString("psdae.proxyFor"));
    Vector<ObjChoiceContent> proxs = new Vector<ObjChoiceContent>(PertSource.getProxySignValues(dacx_));
    proxySignField_ = new JComboBox(proxs);
    proxySignField_.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        try {
          int index = proxySignField_.getSelectedIndex();
          proxyName_.setEnabled(index != PertSource.NO_PROXY_INDEX);
          proxyLab_.setEnabled(index != PertSource.NO_PROXY_INDEX);
          proxJump_.setEnabled(index != PertSource.NO_PROXY_INDEX);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
        
    UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    add(proxSLab, gbc_);  
    UiUtil.gbcSet(gbc_, 1, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.5, 0.0);       
    add(proxySignField_, gbc_);       
    UiUtil.gbcSet(gbc_, 2, rowNum_, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    add(proxyLab_, gbc_);    
    
    proxyName_ = new JComboBox(srcVec);
    proxJump_ = new JButton(rMan_.getString("pdpe.jump"), pmh_.getJumpIcon());
    proxJump_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)proxyName_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertSrcsAndTargsManagePanel.MANAGER_KEY, 
                                PertSrcsAndTargsManagePanel.SRC_KEY, whichRow);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    JPanel combo = pmh_.componentWithJumpButton(proxyName_, proxJump_);
 
    UiUtil.gbcSet(gbc_, 3, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.5, 0.0);       
    add(combo, gbc_);

    //
    // Build the values table tabs.
    //

    annotList_ = new ArrayList<EnumCell>();
    estAnnot_ = new EditableTable(uics_, dacx_, new EditableTable.OneEnumTableModel(uics_, dacx_, "psdae.annot", annotList_), parent_);
    EditableTable.TableParams etp = pmh_.tableParamsForAnnot(annotList_);
    JPanel annotTablePan = estAnnot_.buildEditableTable(etp);
    JPanel annotTableWithButton = pmh_.addEditButton(annotTablePan, "psdae.annotEdit", true, new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          String who = pmh_.getSelectedEnumVal(estAnnot_);        
          pet_.jumpToRemoteEdit(PertAnnotManagePanel.MANAGER_KEY, PertAnnotManagePanel.ANNOT_KEY, who);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    annotTableWithButton.setMinimumSize(new Dimension(400, 100));
   
    //
    // Add the table:
    //
    
    UiUtil.gbcSet(gbc_, 0, rowNum_, 4, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum_ += 2;
    add(annotTableWithButton, gbc_);  
    finishConstruction();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clear out the editor:
  */  
   
  public void closeAction() {
    estAnnot_.stopTheEditing(false);
    super.closeAction();
    return;
  }
 
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public PertSource getResult() {
    return (resultSource_);
  }
  
  /***************************************************************************
  **
  ** Set the new source
  ** 
  */
  
  public void setSource(String srcKey) {
    mode_ = EDIT_MODE;
    currKey_ = srcKey;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new source for duplication
  ** 
  */
  
  public void setDupSourceDef(String origKey) {
    mode_ = DUP_MODE;
    currKey_ = origKey;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the sourceDefs for merging
  ** 
  */
  
  public String setSourceDefsForMerge(List joinKeys) {
    mode_ = MERGE_MODE;
    allMerge_ = new HashSet(joinKeys);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map refCounts = da.getAllSrcDefReferenceCounts();
    currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys);
    displayProperties();
    return (currKey_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  }  
   
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    Vector<TrueObjChoiceContent> srcVec = pd_.getSourceNameOptions();
    Vector<TrueObjChoiceContent> exps = pd_.getPertDictionary().getExperimentTypes();
    UiUtil.replaceComboItems(srcField_, srcVec);  
    UiUtil.replaceComboItems(experimentField_, exps);     
    UiUtil.replaceComboItems(proxyName_, srcVec);     
      
    annotList_ = pmh_.buildAnnotEnum();
    HashMap<Integer, EditableTable.EnumCellInfo> perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, annotList_, EnumCell.class));      
    estAnnot_.refreshEditorsAndRenderers(perColumnEnums);
    ((EditableTable.OneEnumTableModel)estAnnot_.getModel()).setCurrentEnums(annotList_);      
    
    return;
  }
  
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  protected boolean stashResults() {
  
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)srcField_.getSelectedItem();
    if (tocc == null) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.undefinedSrc"),
                                    rMan_.getString("psdae.undefinedSrcTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }   
    String srcKey = (String)tocc.val;
    
    tocc = (TrueObjChoiceContent)experimentField_.getSelectedItem();
    if (tocc == null) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.undefinedExp"),
                                    rMan_.getString("psdae.undefinedExpTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }   
    String expKey = (String)tocc.val;
     
    int index = proxySignField_.getSelectedIndex();
    String pSignResult = PertSource.mapProxySignIndex(index);
    
    
    String proxNameKey = null;
    if (index != PertSource.NO_PROXY_INDEX) {
      tocc = (TrueObjChoiceContent)proxyName_.getSelectedItem();
      if (tocc == null) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.undefinedProx"),
                                      rMan_.getString("psdae.undefinedProxTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }     
      proxNameKey = (String)tocc.val;
    }
    
    //
    // Footnotes:
    //
  
    Iterator tdit = estAnnot_.getModel().getValuesFromTable().iterator();
    ArrayList<String> annotResult = new ArrayList<String>();
    while (tdit.hasNext()) {
      EditableTable.OneEnumTableModel.TableRow ent = (EditableTable.OneEnumTableModel.TableRow)tdit.next();
      EnumCell ec = ent.enumChoice;
      if (ec == null) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.undefinedFoot"),
                                      rMan_.getString("psdae.undefinedFootTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }    
      annotResult.add(ec.internal);
    }
    
    if ((currKey_ == null) || (mode_ == DUP_MODE)) {
      String nextKey = pd_.getNextDataKey();
      resultSource_ = new PertSource(nextKey, srcKey, expKey, annotResult);
      resultSource_.setProxySign(pSignResult); 
    } else {
      resultSource_ = pd_.getSourceDef(currKey_).clone();
      resultSource_.setSourceNameKey(srcKey);
      resultSource_.setExpType(expKey);
      resultSource_.setProxySign(pSignResult);
      resultSource_.setAnnotationIDs(annotResult);
    }
    
    
    Iterator<String> sdkit = pd_.getSourceDefKeys();
    while (sdkit.hasNext()) {
      String sdkey = sdkit.next();
      PertSource pschk =  pd_.getSourceDef(sdkey);
      if (pschk.compareSrcAndType(resultSource_) == 0) {  // just compares source/pert type
        boolean problem = false;
        if (mode_ == MERGE_MODE) {
          problem = !allMerge_.contains(sdkey);
        } else {
          problem = (mode_ == DUP_MODE) || (currKey_ == null) || !currKey_.equals(sdkey);
        }
        if (problem) {         
          JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.duplicatedSrcAndType"),
                                        rMan_.getString("psdae.duplicatedSrcAndTypeTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      }
    } 
    
    if ((proxNameKey != null) && proxNameKey.equals(srcKey)) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("psdae.srcProxiesItself"),
                                    rMan_.getString("psdae.srcProxiesItselfTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
 
    //
    // Null out species if no longer proxied:
    //

    if (index == PertSource.NO_PROXY_INDEX) {
      resultSource_.setProxiedSpeciesKey(null);
    } else {    
      resultSource_.setProxiedSpeciesKey(proxNameKey);
    }
   
    estAnnot_.stopTheEditing(false);
    return (true);
  }    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    
    updateOptions();
     
    if ((currKey_ == null) && (mode_ == EDIT_MODE)) {
      UiUtil.initCombo(srcField_);
      UiUtil.initCombo(experimentField_);
      UiUtil.initCombo(proxySignField_);
      proxyName_.setEnabled(false);
      proxyLab_.setEnabled(false);
      UiUtil.initCombo(proxyName_);
      List annotRows = pmh_.buildAnnotDisplayList(new ArrayList(), estAnnot_, annotList_, false);
      estAnnot_.updateTable(true, annotRows);
      return;
    }
    
    PertSource ps = pd_.getSourceDef(currKey_);
    TrueObjChoiceContent currSrc = pd_.getSourceOrProxyNameChoiceContent(ps.getSourceNameKey());
    srcField_.setSelectedItem(currSrc);
      
    PertProperties currExp = ps.getExpType(pd_.getPertDictionary());
    TrueObjChoiceContent tocc = currExp.getExperimentTypeEntry();
    experimentField_.setSelectedItem(tocc);
    
    String proxSign = ps.getProxySign();
    boolean haveAProx = !proxSign.equals(PertSource.mapProxySignIndex(PertSource.NO_PROXY_INDEX));
    ObjChoiceContent occP = PertSource.getProxySignValue(dacx_, proxSign);
    proxySignField_.setSelectedItem(occP);
    proxyName_.setEnabled(haveAProx);
    proxyLab_.setEnabled(haveAProx);
    if (haveAProx) {
      TrueObjChoiceContent currProx = pd_.getSourceOrProxyNameChoiceContent(ps.getProxiedSpeciesKey());
      proxyName_.setSelectedItem(currProx);
    } else {
      UiUtil.initCombo(proxyName_);
    }
    
    //
    // In a merge situation, we create a super list that combines all merged footnotes:
    //
    
    List anids;
    if (mode_ == MERGE_MODE) {
      HashSet allIDs = new HashSet();
      Iterator amit = allMerge_.iterator();
      while (amit.hasNext()) {
        String nextKey = (String)amit.next();
        PertSource psa = pd_.getSourceDef(nextKey);
        allIDs.addAll(psa.getAnnotationIDs());
      }
      anids = new ArrayList(allIDs);
    } else {
      anids = ps.getAnnotationIDs();
    }
    List annotRows = pmh_.buildAnnotDisplayList(anids, estAnnot_, annotList_, false);
    estAnnot_.updateTable(true, annotRows);
    
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {
    private List estAnnotValues;   
    private TrueObjChoiceContent srcComboTocc;
    private TrueObjChoiceContent expTocc;
    private int proxSignIndex;
    private TrueObjChoiceContent proxNameTocc;
    
    MyFreezeDried() {
      srcComboTocc = (TrueObjChoiceContent)srcField_.getSelectedItem();
      expTocc = (TrueObjChoiceContent)experimentField_.getSelectedItem();
      proxSignIndex = proxySignField_.getSelectedIndex(); 
      proxNameTocc = (TrueObjChoiceContent)proxyName_.getSelectedItem();
      estAnnotValues = estAnnot_.getModel().getValuesFromTable();
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      } 
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        if (pd_.getSourceDef(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {      
      proxySignField_.setSelectedIndex(proxSignIndex); 
      List annotRows = pmh_.buildAnnotDisplayList(estAnnotValues, estAnnot_, annotList_, true);
      estAnnot_.updateTable(true, annotRows);
      
      if (srcComboTocc != null) {
        srcField_.setSelectedItem(pd_.getSourceOrProxyNameChoiceContent((String)srcComboTocc.val)); 
      }
      if (expTocc != null) {
        experimentField_.setSelectedItem(pd_.getPertDictionary().getExperimentTypeChoice((String)expTocc.val));
      }
      if (proxNameTocc != null) {
        proxyName_.setSelectedItem(pd_.getSourceOrProxyNameChoiceContent((String)proxNameTocc.val));
      }     
      return;
    }
  }
}
