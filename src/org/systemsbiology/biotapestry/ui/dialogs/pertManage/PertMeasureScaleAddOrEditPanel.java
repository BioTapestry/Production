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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for creating or editing a perturbation measurement scale
*/

public class PertMeasureScaleAddOrEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private EditOrMergeComps editComps_;
  private EditOrMergeComps mergeComps_;
   
  private PerturbationData pd_;
  private String currKey_;
  private String dupName_;
  private HashSet allMerge_;
  private MeasureScale newScale_;
  private PertManageHelper pmh_;
  
  private TreeSet nameOptions_;
  private TreeSet foldOptions_;
  private TreeSet minOptions_;
  private TreeSet maxOptions_;
  private TreeSet uchOptions_;
  
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
  
  public PertMeasureScaleAddOrEditPanel(UIComponentSource uics,
                                        JFrame parent, PerturbationData pd, TimeCourseData tcd, 
                                        PendingEditTracker pet, String myKey) { 
    super(uics, parent, pet, myKey, 6);
    pd_ = pd;
    pmh_ = new PertManageHelper(uics_, parent, pd, tcd, rMan_, gbc_, pet_);

    //
    // Edit Panel
    //
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout());
  
    editComps_ = new EditOrMergeComps();
    editComps_.compMode = EDIT_MODE;
    editComps_.nameField = new JTextField(); 
    editComps_.foldCheck = new JCheckBox(rMan_.getString("pmsaep.doFoldConvert"));
    editComps_.foldCombo = new JComboBox(); 
    editComps_.foldLabel = new JLabel(rMan_.getString("pmsaep.toFoldType"));
    editComps_.foldField = new JTextField();
    editComps_.foldFieldLabel = new JLabel(rMan_.getString("pmsaep.toFoldVal"));    
    editComps_.illegalCheck = new JCheckBox(rMan_.getString("pmsaep.setIllegalBounds"));
    editComps_.minCombo = new JComboBox();      
    editComps_.minLabel = new JLabel(rMan_.getString("pmsaep.illegalMin"));
    editComps_.maxCombo = new JComboBox();
    editComps_.maxLabel = new JLabel(rMan_.getString("pmsaep.illegalMax"));
    editComps_.minField = new JTextField();
    editComps_.minValLabel = new JLabel(rMan_.getString("pmsaep.illegalMinVal"));
    editComps_.maxField = new JTextField();
    editComps_.maxValLabel = new JLabel(rMan_.getString("pmsaep.illegalMaxVal"));
    editComps_.unchangedField = new JTextField();
    
    commonConstruct(editPanel, editComps_);
 
    //
    // Merge panel
    //
    
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout());
    
    mergeComps_ = new EditOrMergeComps();
    mergeComps_.compMode = MERGE_MODE;
    mergeComps_.nameField = new JComboBox();
    ((JComboBox)mergeComps_.nameField).setEditable(true);
    mergeComps_.foldCheck = new JCheckBox(rMan_.getString("pmsaep.doFoldConvert"));
    mergeComps_.foldCombo = new JComboBox(); 
    mergeComps_.foldLabel = new JLabel(rMan_.getString("pmsaep.toFoldType"));
    mergeComps_.foldField = new JComboBox();
    ((JComboBox)mergeComps_.foldField).setEditable(true);
    mergeComps_.foldFieldLabel = new JLabel(rMan_.getString("pmsaep.toFoldVal"));    
    mergeComps_.illegalCheck = new JCheckBox(rMan_.getString("pmsaep.setIllegalBounds"));
    mergeComps_.minCombo = new JComboBox();      
    mergeComps_.minLabel = new JLabel(rMan_.getString("pmsaep.illegalMin"));
    mergeComps_.maxCombo = new JComboBox();
    mergeComps_.maxLabel = new JLabel(rMan_.getString("pmsaep.illegalMax"));
    mergeComps_.minField = new JComboBox();
    ((JComboBox)mergeComps_.minField).setEditable(true);
    mergeComps_.minValLabel = new JLabel(rMan_.getString("pmsaep.illegalMinVal"));
    mergeComps_.maxField = new JComboBox();
    ((JComboBox)mergeComps_.maxField).setEditable(true);
    mergeComps_.maxValLabel = new JLabel(rMan_.getString("pmsaep.illegalMaxVal"));
    mergeComps_.unchangedField = new JComboBox();
    ((JComboBox)mergeComps_.unchangedField).setEditable(true);
    
    commonConstruct(mergePanel, mergeComps_);
    
    makeMultiMode(editPanel, mergePanel, 4);
    finishConstruction();
  }
  
  
  /***************************************************************************
  **
  ** Set the new measurement scale key
  ** 
  */
  
  private int commonConstruct(JPanel panel, EditOrMergeComps comps) {
  
    int currRow = 0;
    
    currRow = ds_.installLabelJCompPair(panel, 
                                       "pmsaep.name", comps.nameField,  
                                       "pmsaep.unchanged", comps.unchangedField,  
                                        currRow, 6, false);
    
    comps.foldCheck.addActionListener(new ToFoldCheckListener(comps, uics_.getHandlerAndManagerSource()));    
    comps.foldCombo.addActionListener(new ToFoldComboListener(comps, uics_.getHandlerAndManagerSource()));
    currRow = ds_.installLabelJCompTriple(panel, 
                                          null, comps.foldCheck, 
                                          comps.foldLabel, comps.foldCombo, 
                                          comps.foldFieldLabel, comps.foldField, 
                                          currRow, 6);
     
    comps.illegalCheck.addActionListener(new IllegalCheckListener(comps, uics_.getHandlerAndManagerSource()));
    comps.minCombo.addActionListener(new IllegalComboListener(comps, true, uics_.getHandlerAndManagerSource()));
    currRow = ds_.installLabelJCompTriple(panel, 
                                          null, comps.illegalCheck, 
                                          comps.minLabel, comps.minCombo, 
                                          comps.minValLabel, comps.minField, 
                                          currRow, 6);   
 
    comps.maxCombo.addActionListener(new IllegalComboListener(comps, false, uics_.getHandlerAndManagerSource()));
    currRow = ds_.installLabelJCompTriple(panel, 
                                          null, new JLabel(""), 
                                          comps.maxLabel, comps.maxCombo, 
                                          comps.maxValLabel, comps.maxField, 
                                          currRow, 6);
    
    comps.foldCombo.setEnabled(false);
    comps.foldLabel.setEnabled(false);
    comps.minCombo.setEnabled(false);
    comps.maxCombo.setEnabled(false);
    comps.minLabel.setEnabled(false);
    comps.maxLabel.setEnabled(false);
    comps.minField.setEnabled(false);
    comps.minValLabel.setEnabled(false);
    comps.maxField.setEnabled(false);
    comps.maxValLabel.setEnabled(false);
    return (currRow); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Set the new measurement scale key
  ** 
  */
  
  public void setEditMeasureScale(String currKey) {
    mode_ = EDIT_MODE;
    currKey_ = currKey;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new measurement scale key
  ** 
  */
  
  public void setMeasureScaleForDup(String origKey) {
    mode_ = DUP_MODE;
    currKey_ = origKey;
    MeasureDictionary md = pd_.getMeasureDictionary();
    dupName_ = pmh_.getUnique(md.getScaleOptions(), md.getMeasureScale(origKey).getName());  
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new measurement scale key
  ** 
  */
  
  public String setMeasureScaleForMerge(List joinKeys) {
    mode_ = MERGE_MODE;
    MeasureDictionary md = pd_.getMeasureDictionary();
    allMerge_ = new HashSet(joinKeys);
    currKey_ = null;
    String[] stdkeys = md.getStandardScaleKeys();
    for (int i = 0; i < stdkeys.length; i++) {
      if (joinKeys.contains(stdkeys[i])) {
        if (currKey_ != null) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.doubleDefaultMerge"),
                                        rMan_.getString("pmsaep.doubleDefaultMergeTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (null);
        }
        currKey_ = stdkeys[i];
      }
    }
    if (currKey_ == null) {
      DependencyAnalyzer da = pd_.getDependencyAnalyzer();
      Map refCounts = da.getAllMeasureScaleReferenceCounts(true);
      currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys); 
    }
    nameOptions_ = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    foldOptions_ = new TreeSet();
    minOptions_ = new TreeSet();
    maxOptions_ = new TreeSet();
    uchOptions_ = new TreeSet();
    int numk = joinKeys.size();     
    for (int i = 0; i < numk; i++) {
      String nextJk = (String)joinKeys.get(i);
      MeasureScale mp = md.getMeasureScale(nextJk);
      nameOptions_.add(mp.getName());
      Double uch = mp.getUnchanged();
      if (uch != null) {
        uchOptions_.add(uch.toString());
      }
      MeasureScale.Conversion msc = mp.getConvToFold();
      if (msc != null) {
        if (msc.type == MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE) {
          foldOptions_.add(msc.factor.toString());
        }
      }
      BoundedDoubMinMax bdmm = mp.getIllegalRange();
      if (bdmm != null) {
        if (bdmm.min != Double.NEGATIVE_INFINITY) {
          minOptions_.add(Double.toString(bdmm.min));
        }
         if (bdmm.max != Double.POSITIVE_INFINITY) {
          maxOptions_.add(Double.toString(bdmm.max));
        }   
      }
    }
    displayProperties();
    return (currKey_);
  }
  
  /***************************************************************************
  **
  ** Get the new measurement scale
  ** 
  */
  
  public MeasureScale getResult() {
    return (newScale_);
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
    
    if (mode_ == MERGE_MODE) {
      UiUtil.replaceComboItems((JComboBox)mergeComps_.nameField, new Vector(nameOptions_));
      UiUtil.replaceComboItems((JComboBox)mergeComps_.foldField, new Vector(foldOptions_));
      UiUtil.replaceComboItems((JComboBox)mergeComps_.minField, new Vector(minOptions_));
      UiUtil.replaceComboItems((JComboBox)mergeComps_.maxField, new Vector(maxOptions_));
      UiUtil.replaceComboItems((JComboBox)mergeComps_.unchangedField, new Vector(uchOptions_));
      
      Vector convertTypes = MeasureScale.Conversion.getConvertChoices(uics_);
      UiUtil.replaceComboItems(mergeComps_.foldCombo, convertTypes);    
      Vector illegalNeg = MeasureScale.getIllegalChoices(uics_, true);
      UiUtil.replaceComboItems(mergeComps_.minCombo, illegalNeg);    
      Vector illegalPos = MeasureScale.getIllegalChoices(uics_, false);
      UiUtil.replaceComboItems(mergeComps_.maxCombo, illegalPos);         
    } else {
      Vector convertTypes = MeasureScale.Conversion.getConvertChoices(uics_);
      UiUtil.replaceComboItems(editComps_.foldCombo, convertTypes);    
      Vector illegalNeg = MeasureScale.getIllegalChoices(uics_, true);
      UiUtil.replaceComboItems(editComps_.minCombo, illegalNeg);    
      Vector illegalPos = MeasureScale.getIllegalChoices(uics_, false);
      UiUtil.replaceComboItems(editComps_.maxCombo, illegalPos);         
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Install our UI values:
  ** 
  */
  
  protected boolean stashResults() {
 
    EditOrMergeComps useComps = (mode_ == MERGE_MODE) ? mergeComps_ : editComps_;
    
    MeasureScale.Conversion conv = null;  
    try {
      conv = buildToFoldResults(useComps);
    } catch (IOException ioex) {
      return (false);
    }
     
    BoundedDoubMinMax illegal = null;
    try {
      illegal = buildRangeResults(useComps);
    } catch (IOException ioex) {
      return (false);
    }
    
    String nameText = useComps.getText(useComps.nameField);
  
    if (nameText.equals("")) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.emptyName"),
                                    rMan_.getString("pmsaep.emptyNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
   
    Double unchanged = null;
    String uchText = useComps.getText(useComps.unchangedField);
    if (!uchText.equals("")) {
      try {        
        unchanged = Double.valueOf(uchText);
      } catch (NumberFormatException nfex) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.badNumber"),
                                      rMan_.getString("pmsaep.badNumberTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }     
    }  
    
    MeasureDictionary md = pd_.getMeasureDictionary();
    
    //
    // No duplicate names allowed
    //

    Iterator kit = md.getScaleKeys();
    while (kit.hasNext()) {
      String key = (String)kit.next();
      MeasureScale ms = md.getMeasureScale(key);
      String name = ms.getName();
      if (DataUtil.keysEqual(name, nameText)) {
        boolean problem = false;
        if (mode_ == MERGE_MODE) {
          problem = !allMerge_.contains(key);
        } else {
          problem = (mode_ == DUP_MODE) || (currKey_ == null) || !currKey_.equals(key);
        }
        if (problem) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.dupName"),
                                        rMan_.getString("pmsaep.dupNameTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      }
    }

    if ((currKey_ == null) || (mode_ == DUP_MODE)) {
      String nextKey = md.getNextDataKey();  
      newScale_ = new MeasureScale(nextKey, nameText, conv, illegal, unchanged);
    } else {
      MeasureScale currScale = md.getMeasureScale(currKey_);
      newScale_ = (MeasureScale)currScale.clone();     
      newScale_.setName(nameText);
      newScale_.setConvToFold(conv);  
      newScale_.setIllegalRange(illegal); 
      newScale_.setUnchanged(unchanged);
    }
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
   
    MeasureDictionary md = pd_.getMeasureDictionary();
    switch (mode_) {
      case EDIT_MODE:
        if (currKey_ == null) {
          stockFields(editComps_, null, "");
        } else {
          MeasureScale currScale = md.getMeasureScale(currKey_);
          stockFields(editComps_, currScale, null);
        }
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case DUP_MODE:
        MeasureScale currScale = md.getMeasureScale(currKey_);
        stockFields(editComps_, currScale, dupName_);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        currScale = md.getMeasureScale(currKey_);
        stockFields(mergeComps_, currScale, null);
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
    
    return;
  }
  
  /***************************************************************************
  **
  ** Stock fields
  ** 
  */
  
  private void stockFields(EditOrMergeComps comps, MeasureScale currScale, String nameOverride) { 
    String currName = (nameOverride == null) ? currScale.getName() : nameOverride;
    comps.setText(comps.nameField, currName);
    
    Double unchObj = (currScale == null) ? null : currScale.getUnchanged();
    comps.setText(comps.unchangedField, (unchObj == null) ? "" : unchObj.toString());
    
    MeasureScale.Conversion msc = (currScale == null) ? null : currScale.getConvToFold();
    comps.foldCheck.setSelected(msc != null);
   
    if (msc != null) {
      comps.foldCombo.setSelectedItem(MeasureScale.Conversion.convertTypeForCombo(uics_, msc.type)); 
      comps.setText(comps.foldField, (msc.factor != null) ? msc.factor.toString() : "");
    } else {
      comps.foldCombo.setSelectedIndex(0);
      comps.setText(comps.foldField, "");
    }
      
    BoundedDoubMinMax bdmm = (currScale == null) ? null : currScale.getIllegalRange();
    comps.illegalCheck.setSelected(bdmm != null);
 
    if (bdmm != null) {
      int negType = MeasureScale.rangeToType(bdmm, true);
      comps.minCombo.setSelectedItem(MeasureScale.convertTypeForCombo(uics_, negType, true));
      comps.setText(comps.minField, (negType != MeasureScale.INFINITY) ? Double.toString(bdmm.min) : "");
      int posType = MeasureScale.rangeToType(bdmm, false);
      comps.maxCombo.setSelectedItem(MeasureScale.convertTypeForCombo(uics_, posType, false)); 
      comps.setText(comps.maxField, (posType != MeasureScale.INFINITY) ? Double.toString(bdmm.max) : "");
    } else {
      comps.minCombo.setSelectedIndex(0);
      comps.setText(comps.minField, "");
      comps.maxCombo.setSelectedIndex(0);
      comps.setText(comps.maxField, "");
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Build results for toFold
  */
  
  private MeasureScale.Conversion buildToFoldResults(EditOrMergeComps comps) throws IOException { 
  
    if (!comps.foldCheck.isSelected()) {
      return (null);
    }
     
    Double toFold = null;
    
    int convertType = ((ChoiceContent)comps.foldCombo.getSelectedItem()).val;
    if (convertType == MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE) {     
      boolean badNum = false;
      String foldText = comps.getText(comps.foldField); 
      if (!foldText.equals("")) {
        try {        
          toFold = Double.valueOf(foldText);
        } catch (NumberFormatException nfex) {
          badNum = true;
        }
      } else {
        badNum = true;
      }
      if (badNum) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.badNumber"),
                                      rMan_.getString("pmsaep.badNumberTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        throw (new IOException());
      }     
    }  
    return (new MeasureScale.Conversion(convertType, toFold));   
  }

  /***************************************************************************
  **
  ** Build results for illegal range
  */
  
  private BoundedDoubMinMax buildRangeResults(EditOrMergeComps comps) throws IOException { 
  
    if (!comps.illegalCheck.isSelected()) {
      return (null);
    }

    double minVal = Double.NEGATIVE_INFINITY;
    boolean minInclude = true;
    double maxVal = Double.POSITIVE_INFINITY;
    boolean maxInclude = true;
    boolean badMin = false;
    boolean badMax = false;
    
    int minSpec = ((ChoiceContent)comps.minCombo.getSelectedItem()).val;
    if (minSpec != MeasureScale.INFINITY) {
      String minText = comps.getText(comps.minField);
      if (!minText.equals("")) {
        try {        
          minVal = Double.valueOf(minText).doubleValue();
        } catch (NumberFormatException nfex) {
          badMin = true;
        }
      } else {
        badMin = true;
      }
      minInclude = (minSpec == MeasureScale.VALUE_INCLUSIVE);
    } 
    
    int maxSpec = ((ChoiceContent)comps.maxCombo.getSelectedItem()).val;
    if (maxSpec != MeasureScale.INFINITY) {
      String maxText = comps.getText(comps.maxField);
      if (!maxText.equals("")) {
        try {        
          maxVal = Double.valueOf(maxText).doubleValue();
        } catch (NumberFormatException nfex) {
          badMax = true;
        }
      } else {
        badMax = true;
      }
      maxInclude = (maxSpec == MeasureScale.VALUE_INCLUSIVE);
    }

    if (badMin || badMax) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pmsaep.badNumber"),
                                    rMan_.getString("pmsaep.badNumberTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      throw (new IOException());
    } 
    
    return (new BoundedDoubMinMax(minVal, maxVal, minInclude, maxInclude));   
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Holds components for edit or merge panels
  */

  static class EditOrMergeComps {
    int compMode; 
    JComponent nameField;
    JCheckBox foldCheck; 
    JComboBox foldCombo;
    JLabel foldLabel; 
    JComponent foldField;
    JLabel foldFieldLabel;     
    JCheckBox illegalCheck; 
    JComboBox minCombo;
    JLabel minLabel;
    JComboBox maxCombo; 
    JLabel maxLabel;
    JComponent minField;
    JLabel minValLabel; 
    JComponent maxField; 
    JLabel maxValLabel;
    JComponent unchangedField;
    
    public String getText(JComponent comp) {
      if (compMode == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)((JComboBox)comp).getEditor().getEditorComponent();
        return (jtc.getText().trim());
      } else {
        return (((JTextField)comp).getText().trim());
      }
    }
    
    public void setText(JComponent comp, String text) {
      if (compMode == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)((JComboBox)comp).getEditor().getEditorComponent();
        jtc.setText(text);
      } else {
        ((JTextField)comp).setText(text);
      }
      return;
    }
  }  

  /***************************************************************************
  **
  ** Various listeners
  */

  static class IllegalComboListener implements ActionListener {
  
    private EditOrMergeComps eomc_;
    private boolean forMin_;
    private HandlerAndManagerSource hams_;
    
    IllegalComboListener(EditOrMergeComps eomc, boolean forMin, HandlerAndManagerSource hams) {
      eomc_ = eomc;
      forMin_ = forMin;
      hams_ = hams;
    }
       
    public void actionPerformed(ActionEvent ev) {
      try {
        JComboBox combo = (forMin_) ? eomc_.minCombo : eomc_.maxCombo;
        ChoiceContent cc = (ChoiceContent)combo.getSelectedItem();
        boolean doFact = (cc == null) ? false : (cc.val != MeasureScale.INFINITY);
        JComponent field = (forMin_) ? eomc_.minField : eomc_.maxField;
        field.setEnabled(doFact);
        JLabel label = (forMin_) ? eomc_.minValLabel : eomc_.maxValLabel;
        label.setEnabled(doFact);
        return;
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }
    }
  }  
  
  /***************************************************************************
  **
  ** Various listeners
  */

  static class ToFoldComboListener implements ActionListener {
  
    private EditOrMergeComps eomc_;
    private HandlerAndManagerSource hams_;
    
    ToFoldComboListener(EditOrMergeComps eomc, HandlerAndManagerSource hams) {
      eomc_ = eomc;
      hams_ = hams;
    }
       
    public void actionPerformed(ActionEvent ev) {
      try {
        ChoiceContent cc = (ChoiceContent)eomc_.foldCombo.getSelectedItem();
        boolean doFact = (cc == null) ? false : (cc.val == MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE);
        eomc_.foldField.setEnabled(doFact);
        eomc_.foldFieldLabel.setEnabled(doFact);
        return;
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }
    }
  }
  
  /***************************************************************************
  **
  ** Various listeners
  */

  static class IllegalCheckListener implements ActionListener {
  
    private EditOrMergeComps eomc_;
    private HandlerAndManagerSource hams_;
    
    IllegalCheckListener(EditOrMergeComps eomc, HandlerAndManagerSource hams) {
      eomc_ = eomc;
      hams_ = hams;
    }
    
    public void actionPerformed(ActionEvent ev) {
      try {
        boolean doIll = eomc_.illegalCheck.isSelected();
        eomc_.minCombo.setEnabled(doIll);
        eomc_.maxCombo.setEnabled(doIll);
        eomc_.minLabel.setEnabled(doIll);
        eomc_.maxLabel.setEnabled(doIll);

        ChoiceContent cc = (ChoiceContent)eomc_.minCombo.getSelectedItem();
        boolean doFact = (cc == null) ? false : (cc.val != MeasureScale.INFINITY);
        eomc_.minField.setEnabled(doIll && doFact);
        eomc_.minValLabel.setEnabled(doIll && doFact);

        cc = (ChoiceContent)eomc_.maxCombo.getSelectedItem();
        doFact = (cc == null) ? false : (cc.val != MeasureScale.INFINITY);
        eomc_.maxField.setEnabled(doIll && doFact);
        eomc_.maxValLabel.setEnabled(doIll && doFact);

      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }
    }
  }
  
  /***************************************************************************
  **
  ** Various listeners
  */

  static class ToFoldCheckListener implements ActionListener {
  
    private EditOrMergeComps eomc_;
    private HandlerAndManagerSource hams_;
    
    ToFoldCheckListener(EditOrMergeComps eomc, HandlerAndManagerSource hams) {
      eomc_ = eomc;
      hams_ = hams;
    }
  
    public void actionPerformed(ActionEvent ev) {
      try {
        boolean doFold = eomc_.foldCheck.isSelected();
        eomc_.foldCombo.setEnabled(doFold);
        eomc_.foldLabel.setEnabled(doFold);
        ChoiceContent cc = (ChoiceContent)eomc_.foldCombo.getSelectedItem();
        boolean doFact = (cc == null) ? false : (cc.val == MeasureScale.Conversion.FACTOR_IS_EXPONENT_BASE);
        eomc_.foldField.setEnabled(doFold && doFact);
        eomc_.foldFieldLabel.setEnabled(doFold && doFact);
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }
    }
  }
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {
    private String nameFieldText;
    private String toFoldText;
    private String minValText;
    private String maxValText;
    private String uchText;
    private boolean toFold;
    private boolean illegal;
    private ChoiceContent foldComboCc;
    private ChoiceContent minComboCc;
    private ChoiceContent maxComboCc;

    MyFreezeDried() {  
      
      EditOrMergeComps useComps = (mode_ == MERGE_MODE) ? mergeComps_ : editComps_;
      
      nameFieldText = useComps.getText(useComps.nameField);
      toFoldText = useComps.getText(useComps.foldField);
      minValText = useComps.getText(useComps.minField);
      maxValText = useComps.getText(useComps.maxField);
      uchText = useComps.getText(useComps.unchangedField);
      toFold = useComps.foldCheck.isSelected();
      illegal = useComps.illegalCheck.isSelected();
      foldComboCc = (ChoiceContent)useComps.foldCombo.getSelectedItem();
      minComboCc = (ChoiceContent)useComps.minCombo.getSelectedItem();
      maxComboCc = (ChoiceContent)useComps.maxCombo.getSelectedItem();
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        if (pd_.getMeasureDictionary().getMeasureScale(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      
      EditOrMergeComps useComps = (mode_ == MERGE_MODE) ? mergeComps_ : editComps_;
      
      useComps.setText(useComps.nameField, nameFieldText);
      useComps.setText(useComps.foldField, toFoldText);
      useComps.setText(useComps.minField, minValText );
      useComps.setText(useComps.maxField, maxValText);
      useComps.setText(useComps.unchangedField, uchText);
      useComps.foldCheck.setSelected(toFold);
      useComps.illegalCheck.setSelected(illegal);
      useComps.foldCombo.setSelectedItem(foldComboCc);
      useComps.minCombo.setSelectedItem(minComboCc);
      useComps.maxCombo.setSelectedItem(maxComboCc);
      return;
    }
  }
}
