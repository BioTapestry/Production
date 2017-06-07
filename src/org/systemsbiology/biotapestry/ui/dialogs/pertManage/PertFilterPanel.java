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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertDataPoint;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for setting filters
*/

public class PertFilterPanel extends JPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private PerturbationData pd_;
  private JComboBox srcCombo_;
  private JComboBox pertCombo_;
  private JComboBox targCombo_;
  private JComboBox timeCombo_;
  private JComboBox investCombo_;
  private JComboBox investModeCombo_;
  private JComboBox valCombo_;
  private JLabel srcLabel_;
  private JLabel pertLabel_;
  private JLabel targLabel_;
  private JLabel timeLabel_;
  private JLabel investLabel_;
  private JLabel valLabel_;
  private int[] currIndices_;

  private JCheckBox customFilterInstalled_;
  private PertFilterExpression customFilterExp_;
  private FixedJButton buttonO_;
  private boolean trueBOEnable_;
  private FixedJButton buttonC_;
  private ApplyButtonControl myABC_;
  private PertManageHelper.FilterListRenderer srcRender_;
  private PertManageHelper.FilterListRenderer pertRender_;
  private PertManageHelper.FilterListRenderer targRender_;
  private PertManageHelper.FilterListRenderer timeRender_;
  private PertManageHelper.FilterListRenderer investRender_;
  private PertManageHelper.FilterListRenderer valRender_;
  private Dimension savePref_;
  private Dimension saveMin_;
  private Client myClient_;
  private UIComponentSource uics_;
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
  
  public PertFilterPanel(UIComponentSource uics, DataAccessContext dacx, PerturbationData pd, Client myClient) {
    dacx_ = dacx;
    uics_ = uics;
    pd_ = pd;
    myClient_ = myClient;
    myABC_ = new ApplyButtonControl();
    
    ResourceManager rMan = dacx_.getRMan();    
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();  
    
    JLabel mainLabel = new JLabel(rMan.getString("pertManage.chooseOpFilters"));
    int rowNum = 0;
    int colNum = 0;
    UiUtil.gbcSet(gbc, colNum, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    add(mainLabel, gbc);
     
    // SOURCES ---------------------
  
    srcLabel_ = new JLabel(rMan.getString("pertManage.chooseSource"));
    srcCombo_ = new JComboBox();
    srcRender_ = new PertManageHelper.FilterListRenderer(uics_, srcCombo_.getRenderer());
    srcCombo_.setRenderer(srcRender_);
    srcCombo_.addActionListener(myABC_);
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(srcLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(srcCombo_, gbc);    
 
    // PERTS ---------------------
 
    pertLabel_ = new JLabel(rMan.getString("pertManage.choosePert"));
    pertCombo_ = new JComboBox();
    pertRender_ = new PertManageHelper.FilterListRenderer(uics_, pertCombo_.getRenderer());
    pertCombo_.setRenderer(pertRender_);
    pertCombo_.addActionListener(myABC_);
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(pertLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(pertCombo_, gbc);    
  
    // TARGET ---------------------

    targLabel_ = new JLabel(rMan.getString("pertManage.chooseTarg"));
    targCombo_ = new JComboBox();
    targRender_ = new PertManageHelper.FilterListRenderer(uics_, targCombo_.getRenderer());
    targCombo_.setRenderer(targRender_);
    targCombo_.addActionListener(myABC_);
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(targLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(targCombo_, gbc); 
    
    colNum += 2;
    rowNum = 1;
    
    // TIME ---------------------
 
    timeLabel_ = new JLabel(rMan.getString("pertManage.chooseTime"));
    timeCombo_ = new JComboBox();
    timeRender_ = new PertManageHelper.FilterListRenderer(uics_, timeCombo_.getRenderer());
    timeCombo_.setRenderer(timeRender_);
    timeCombo_.addActionListener(myABC_);
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(timeLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(timeCombo_, gbc);
    
    // INVEST ---------------------
    investLabel_ = new JLabel(rMan.getString("pertManage.chooseInvest"));
    investCombo_ = new JComboBox();
    investRender_ = new PertManageHelper.FilterListRenderer(uics_, investCombo_.getRenderer());
    investCombo_.setRenderer(investRender_);
    investCombo_.addActionListener(myABC_);
    Vector fmo = getInvestOptions();
    investModeCombo_ = new JComboBox(fmo);
    investModeCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          myABC_.enableApply();
          stockInvestigatorChoices();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
 
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(investLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    add(investModeCombo_, gbc);  
    UiUtil.gbcSet(gbc, colNum + 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(investCombo_, gbc);
    
    // VALS or something  ---------------------
 
    valLabel_ = new JLabel(rMan.getString("pertManage.chooseVal"));
    valCombo_ = new JComboBox();
    valRender_ = new PertManageHelper.FilterListRenderer(uics_, valCombo_.getRenderer());
    valCombo_.setRenderer(valRender_);
    valCombo_.addActionListener(myABC_);
    UiUtil.gbcSet(gbc, colNum, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    add(valLabel_, gbc);
    UiUtil.gbcSet(gbc, colNum + 1, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    add(valCombo_, gbc);  
 
    
    // CUSTOM FILTER ---------------------
 
    colNum = 0;
    customFilterInstalled_ = new JCheckBox(rMan.getString("pertManage.customFilter"));
    customFilterInstalled_.setSelected(false);
    customFilterInstalled_.setEnabled(false);
    customFilterInstalled_.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        try {
          boolean amSel = customFilterInstalled_.isSelected();
          enableFilterCombos(!amSel);
          customFilterInstalled_.setEnabled(amSel);
          if (!amSel) {
            myABC_.enableApply();
            customFilterExp_ = null;            
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });    
        
    UiUtil.gbcSet(gbc, colNum, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    add(customFilterInstalled_, gbc);
    
    //
    // Build the button panel:
    //
  
    buttonO_ = new FixedJButton(rMan.getString("pertManage.applyFilter"));
    buttonO_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          PertFilterExpression pfe = buildPertFilterExpr();
          myClient_.installNewFilter(pfe);
          myABC_.disableApply(true);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonO_.setEnabled(false);
    trueBOEnable_ = false;
    
    buttonC_ = new FixedJButton(rMan.getString("pertManage.clearFilter"));
    buttonC_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          clearAllFilters();
          PertFilterExpression pfe = buildPertFilterExpr();
          myClient_.installNewFilter(pfe);
          myABC_.disableApply(true);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO_);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC_);
    buttonPanel.add(Box.createHorizontalGlue()); 

    //
    // Add buttons:
    //
    
    UiUtil.gbcSet(gbc, 0, rowNum, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    add(buttonPanel, gbc);
  }
 
  /***************************************************************************
  **
  ** Clear all filters
  */ 
  
  public void clearAllFilters() {   
    UiUtil.initCombo(pertCombo_);
    UiUtil.initCombo(targCombo_);
    UiUtil.initCombo(srcCombo_);
    UiUtil.initCombo(timeCombo_);
    UiUtil.initCombo(investCombo_);
    UiUtil.initCombo(valCombo_);
    customFilterExp_ = null;
    customFilterInstalled_.setSelected(false);
    customFilterInstalled_.setEnabled(false);
    return;
  }
 
  /***************************************************************************
  **
  ** Handle combo enabling
  */ 
  
  public void enableFilterCombos(boolean enable) {
    srcLabel_.setEnabled(enable);
    pertLabel_.setEnabled(enable);
    targLabel_.setEnabled(enable);
    timeLabel_.setEnabled(enable);
    investLabel_.setEnabled(enable);
    valLabel_.setEnabled(enable);
    
    srcCombo_.setEnabled(enable);
    pertCombo_.setEnabled(enable);
    targCombo_.setEnabled(enable);
    timeCombo_.setEnabled(enable);
    investModeCombo_.setEnabled(enable);
    investCombo_.setEnabled(enable);
    valCombo_.setEnabled(enable);
    return;
  }
    
  /***************************************************************************
  **
  ** Handle top pane enabling
  */ 
  
  public void enableFilters(boolean enable) {  
    enableFilterCombos(enable);
    buttonO_.setEnabled((enable) ? trueBOEnable_ : false);
    buttonC_.setEnabled(enable);
    if (!enable) {
      if (savePref_ == null) savePref_ = getPreferredSize();
      if (saveMin_ == null) saveMin_ = getMinimumSize();
      setPreferredSize(new Dimension(0, 0));
      setMinimumSize(new Dimension(0, 0));
    } else {
      setPreferredSize(savePref_);
      setMinimumSize(saveMin_);
    }
    return;
  }    
  
  /***************************************************************************
  **
  ** Set filters programmatically (use for jump filtering)
  */ 
  
  public void setCurrentSettings(PertFilterExpression pfe) {
    clearAllFilters();
    PertFilterExpression.Op pfeOp = pfe.getOperator();
    if (pfeOp == PertFilterExpression.Op.AND_OP) {
      boolean match = setCurrentSingleSetting((PertFilterExpression)pfe.getTarget1());    
      match = match && setCurrentSingleSetting((PertFilterExpression)pfe.getTarget2());
      if (!match) {
        throw new IllegalArgumentException();
      }     
      customFilterInstalled_.setSelected(false);
      customFilterInstalled_.setEnabled(false);
    } else if (pfeOp != PertFilterExpression.Op.ALWAYS_OP) {
      boolean match = setCurrentSingleSetting(pfe);
      if (!match) {
        customFilterExp_ = pfe.clone();
        customFilterInstalled_.setSelected(true);
        customFilterInstalled_.setEnabled(true);
      }
    }
    myABC_.disableApply(true);
    return;
  }
    
  /***************************************************************************
  **
  ** Stock one guy with current options
  */ 
  
  public boolean setCurrentSingleSetting(PertFilterExpression pfe) {   
    if (pfe.getOperator() != PertFilterExpression.Op.NO_OP) {
      return (false);
    }
    PertFilter filter = (PertFilter)pfe.getTarget1();
    String matchVal = filter.getStringValue();
    PertFilter.Cat filtCat = filter.getCategory();
    if (filter.getMatchType() != PertFilter.Match.STR_EQUALS) {
      throw new IllegalArgumentException();
    }
    JComboBox useCombo;
    if (filtCat == PertFilter.Cat.SOURCE_NAME) {
      useCombo = srcCombo_;
    } else if (filtCat == PertFilter.Cat.TARGET) {
      useCombo = targCombo_;
    } else if (filtCat == PertFilter.Cat.INVEST) {
      useCombo = investCombo_;
    } else if (filtCat == PertFilter.Cat.PERT) {
      useCombo = pertCombo_;  
    } else {
      return (false);
      /*
      case PertFilter.EXP_CONTROL:
      case PertFilter.EXP_CONDITION:
      case PertFilter.MEASURE_SCALE:
      case PertFilter.ANNOTATION:
      case PertFilter.MEASURE_TECH:
      case PertFilter.EXPERIMENT: 
      */
    }
    
    int numSrc = useCombo.getItemCount();
    for (int i = 0; i < numSrc; i++) {
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)useCombo.getItemAt(i);
      if ((tocc.val != null) && tocc.val.equals(matchVal)) {
         useCombo.setSelectedIndex(i);
         break;
      }
    }   
    return (true);
  }  
    
  /***************************************************************************
  **
  ** Stock with current options
  */ 
  
  public void stockFilterPanel() { 
  
    ResourceManager rMan = dacx_.getRMan();    
    TrueObjChoiceContent ncstr = new TrueObjChoiceContent(rMan.getString("pertManage.chooseAll"), null);
    
    // SOURCES ---------------------
    SortedSet<TrueObjChoiceContent> srcCand = pd_.getCandidates(PertFilter.Cat.SOURCE_NAME);  
    srcCombo_.removeAllItems();
    srcCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> scit = srcCand.iterator();
    while (scit.hasNext()) {
      srcCombo_.addItem(scit.next());
    }
    srcCand.add(ncstr);
    srcRender_.setActive(srcCand);   

    // PERTS ---------------------
    SortedSet<TrueObjChoiceContent> pertCand = pd_.getCandidates(PertFilter.Cat.PERT);
    pertCombo_.removeAllItems();
    pertCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> pit = pertCand.iterator();   
    while (pit.hasNext()) {
      pertCombo_.addItem(pit.next());
    }
    pertCand.add(ncstr);
    pertRender_.setActive(pertCand);
 
    // TARGET ---------------------
    SortedSet<TrueObjChoiceContent> targCand = pd_.getCandidates(PertFilter.Cat.TARGET); 
    targCombo_.removeAllItems();
    targCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> tcit = targCand.iterator();
    while (tcit.hasNext()) {
      targCombo_.addItem(tcit.next());
    }
    targCand.add(ncstr);
    targRender_.setActive(targCand);
   
    // TIME ---------------------
    SortedSet<TrueObjChoiceContent> timeCand = pd_.getCandidates(PertFilter.Cat.TIME);
    timeCombo_.removeAllItems();
    timeCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> tmit = timeCand.iterator();
    while (tmit.hasNext()) {
      timeCombo_.addItem(tmit.next());
    }
    timeCand.add(ncstr);
    timeRender_.setActive(timeCand);
   
    // INVEST ---------------------
    stockInvestigatorChoices();
    
    // VALS  ---------------------
    SortedSet<TrueObjChoiceContent> valCand = pd_.getCandidates(PertFilter.Cat.VALUE);
    valCombo_.removeAllItems();
    valCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> vcit = valCand.iterator();
    while (vcit.hasNext()) {
      valCombo_.addItem(vcit.next());
    }
    valCand.add(ncstr);
    valRender_.setActive(valCand);
    
    myABC_.disableApply(true);
    return;
  } 
  
  /***************************************************************************
  **
  ** Stock investigators with current options
  */ 
  
  public void stockInvestigatorChoices() {
    ResourceManager rMan = dacx_.getRMan();    
    TrueObjChoiceContent ncstr = new TrueObjChoiceContent(rMan.getString("pertManage.chooseAll"), null);
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)investModeCombo_.getSelectedItem();
    SortedSet<TrueObjChoiceContent> investCand = pd_.getCandidates((PertFilter.Cat)tocc.val); 
    investCombo_.removeAllItems();
    investCombo_.addItem(ncstr);
    Iterator<TrueObjChoiceContent> icit = investCand.iterator();
    while (icit.hasNext()) {
      investCombo_.addItem(icit.next());
    }
    investCand.add(ncstr);
    investRender_.setActive(investCand);
    return;
  }

  /***************************************************************************
  **
  ** Update the filter renderers
  */ 
  
  public void updateFilterRenderers(List<PertDataPoint> filteredData) {
    ResourceManager rMan = dacx_.getRMan();
    TrueObjChoiceContent ncstr = new TrueObjChoiceContent(rMan.getString("pertManage.chooseAll"), null);

    // SOURCES ---------------------
    SortedSet<TrueObjChoiceContent> srcCand = pd_.getCandidates(filteredData, PertFilter.Cat.SOURCE_NAME);
    srcCand.add(ncstr);
    srcRender_.setActive(srcCand);
    srcCombo_.invalidate();
    // PERTS ---------------------
    SortedSet<TrueObjChoiceContent> pertCand = pd_.getCandidates(filteredData, PertFilter.Cat.PERT);
    pertCand.add(ncstr);
    pertRender_.setActive(pertCand);
    pertCombo_.invalidate();
    // TARGET ---------------------
    SortedSet<TrueObjChoiceContent> targCand = pd_.getCandidates(filteredData, PertFilter.Cat.TARGET);
    targCand.add(ncstr);
    targRender_.setActive(targCand);
    targCombo_.invalidate();
    // TIME ---------------------
    SortedSet<TrueObjChoiceContent> timeCand = pd_.getCandidates(filteredData, PertFilter.Cat.TIME);
    timeCand.add(ncstr);
    timeRender_.setActive(timeCand);
    timeCombo_.invalidate();
    // INVEST ---------------------
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)investModeCombo_.getSelectedItem();
    SortedSet<TrueObjChoiceContent> investCand = pd_.getCandidates(filteredData, (PertFilter.Cat)tocc.val); 
    investCand.add(ncstr);
    investRender_.setActive(investCand);
    investCombo_.invalidate();
    // VALS or something  ---------------------
    SortedSet<TrueObjChoiceContent> valCand = pd_.getCandidates(filteredData, PertFilter.Cat.VALUE);
    valCand.add(ncstr);
    valRender_.setActive(valCand);
    valCombo_.invalidate();
    validate();
    repaint();
    return;
  }

  /***************************************************************************
  **
  ** Build a new pert filter
  */
  
  public PertFilterExpression buildPertFilterExpr() {
    PertFilterExpression retval = null; 
    
    if (customFilterExp_ != null) {
      return (customFilterExp_);
    }    
    
    if (srcCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent useSrc = (TrueObjChoiceContent)srcCombo_.getSelectedItem();
      PertFilter srcFilter = new PertFilter(PertFilter.Cat.SOURCE_NAME, PertFilter.Match.STR_EQUALS, useSrc.val);
      retval = new PertFilterExpression(srcFilter);
    }
    
    if (pertCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent usePert = (TrueObjChoiceContent)pertCombo_.getSelectedItem();
      PertFilter pertFilter = new PertFilter(PertFilter.Cat.PERT, PertFilter.Match.STR_EQUALS, usePert.val);
      if (retval == null) {
        retval = new PertFilterExpression(pertFilter);
      } else {
        retval = new PertFilterExpression(PertFilterExpression.Op.AND_OP, retval, pertFilter);
      }
    }

    if (targCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent useTarg = (TrueObjChoiceContent)targCombo_.getSelectedItem();
      PertFilter targFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, useTarg.val);
      if (retval == null) {
        retval = new PertFilterExpression(targFilter);
      } else {
        retval = new PertFilterExpression(PertFilterExpression.Op.AND_OP, retval, targFilter);
      }
    }
  
    if (timeCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent useTime = (TrueObjChoiceContent)timeCombo_.getSelectedItem();
      PertFilter timeFilter = new PertFilter(PertFilter.Cat.TIME, PertFilter.Match.RANGE_EQUALS, useTime.val);
      if (retval == null) {
        retval = new PertFilterExpression(timeFilter);
      } else {
        retval = new PertFilterExpression(PertFilterExpression.Op.AND_OP, retval, timeFilter);
      }
    }
    
    if (investCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent useInvest = (TrueObjChoiceContent)investCombo_.getSelectedItem();
      TrueObjChoiceContent useInvestMode = (TrueObjChoiceContent)investModeCombo_.getSelectedItem();   
      PertFilter investFilter = new PertFilter((PertFilter.Cat)useInvestMode.val, PertFilter.Match.STR_EQUALS, useInvest.val);
      if (retval == null) {
        retval = new PertFilterExpression(investFilter);
      } else {
        retval = new PertFilterExpression(PertFilterExpression.Op.AND_OP, retval, investFilter);
      }
    }
    
    if (valCombo_.getSelectedIndex() != 0) {
      TrueObjChoiceContent useVal = (TrueObjChoiceContent)valCombo_.getSelectedItem();
      PertFilter valFilter = new PertFilter(PertFilter.Cat.VALUE, (PertFilter.Match)useVal.val, null);
      if (retval == null) {
        retval = new PertFilterExpression(valFilter);
      } else {
        retval = new PertFilterExpression(PertFilterExpression.Op.AND_OP, retval, valFilter);
      }
    }

    if (retval == null) {
      retval = new PertFilterExpression(PertFilterExpression.Op.ALWAYS_OP);
    }
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the choices for controls
  */
  
  public Vector<TrueObjChoiceContent> getInvestOptions() {
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
    retval.add(getMatchOptionsChoice(PertFilter.Cat.INVEST));
    retval.add(getMatchOptionsChoice(PertFilter.Cat.INVEST_LIST));
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the choice
  */
  
  public TrueObjChoiceContent getMatchOptionsChoice(PertFilter.Cat which) {
    return (new TrueObjChoiceContent(dacx_.getRMan().getString("pertFilt." + PertFilter.mapCategory(which)), which));
  } 
  
  /***************************************************************************
  **
  ** Figure out current state
  */
  
  public int[] gatherFilterIndices() {
    int[] retval = new int[7];
    retval[0] = srcCombo_.getSelectedIndex(); 
    retval[1] = pertCombo_.getSelectedIndex();  
    retval[2] = targCombo_.getSelectedIndex();   
    retval[3] = timeCombo_.getSelectedIndex();   
    retval[4] = investCombo_.getSelectedIndex();
    retval[5] = investModeCombo_.getSelectedIndex();
    retval[6] = valCombo_.getSelectedIndex();
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clients implement to use the class
  */

  public interface Client {
    public void installNewFilter(PertFilterExpression pfe);
  }
  
  /***************************************************************************
  ** 
  ** Used to control apply button
  */

  private class ApplyButtonControl implements ActionListener {

    public void actionPerformed(ActionEvent ev) {
      enableApply();
      return;
    }
    
    void enableApply() {
      int[] gfi = gatherFilterIndices();
      buttonO_.setForeground(Color.red);
      trueBOEnable_ = true;
      buttonO_.setEnabled(true);
      validate();
      repaint();
      return;
    }

    void disableApply(boolean doPaint) {
      buttonO_.setForeground(Color.black);
      trueBOEnable_ = false;
      buttonO_.setEnabled(false);
      if (doPaint) {
        validate();
        repaint();
      }
      return;
    }    
  }
}
