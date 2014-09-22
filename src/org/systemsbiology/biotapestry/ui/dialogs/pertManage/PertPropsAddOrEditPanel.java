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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.NameValuePair;
import org.systemsbiology.biotapestry.util.NameValuePairList;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.NameValuePairTablePanel;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

/****************************************************************************
**
** Panel for creating or editing a Perturbation Properties
*/

public class PertPropsAddOrEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private JTextField typeFieldEdit_; 
  private JComboBox typeCombo_;
  
  private JTextField abbrevFieldEdit_;
  private JComboBox abbrevCombo_;
  
  private JTextField legFieldEdit_;
  private JComboBox legCombo_;
  private JLabel legLabelEdit_;
  private JLabel legLabelMerge_;
  
  private JComboBox relationComboEdit_;
  private JComboBox relationComboMerge_;
  
  private JPanel nvHolderEdit_;
  private JPanel nvHolderMerge_;
  private NameValuePairTablePanel nvptpEdit_;
  private NameValuePairTablePanel nvptpMerge_;
    
  private PerturbationData pd_;
  private String currKey_;
  private PertProperties newProps_; 
  private String copyType_; 
  private String copyAbbrev_;
  private String copyLegAlt_;
  private HashSet allMerge_;
  private PertManageHelper pmh_;
  
  private TreeSet nameOptions_;
  private TreeSet abbrevOptions_; 
  private TreeSet altAbbrevOptions_;
  
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
  
  public PertPropsAddOrEditPanel(BTState appState, DataAccessContext dacx, JFrame parent, PerturbationData pd, PendingEditTracker pet, String myKey) { 
    super(appState, dacx, parent, pet, myKey, 6);
    pd_ = pd;
    pmh_ = new PertManageHelper(appState_, parent, pd, rMan_, gbc_, pet_);
  
    //
    // Edit panel
    //
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout());
    int editRow = 0;  
    typeFieldEdit_ = new JTextField();  
    abbrevFieldEdit_ = new JTextField();
    legFieldEdit_ = new JTextField();
    legLabelEdit_ = new JLabel(rMan_.getString("pertPropEdit.legacy"));
    editRow = ds_.installLabelJCompTriple(editPanel, 
                                          "pertPropEdit.type", typeFieldEdit_, 
                                          "pertPropEdit.abbrev", abbrevFieldEdit_,
                                           legLabelEdit_, legFieldEdit_,
                                           editRow, 6);
  
    relationComboEdit_ = new JComboBox(); 
    editRow = ds_.installLabeledJComp(relationComboEdit_, editPanel, "pertPropEdit.relation", editRow, 6);
    
    JLabel nvLabel = new JLabel(rMan_.getString("pertPropEdit.nvPairsLabel"));
    UiUtil.gbcSet(gbc_, 0, editRow++, 6, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    editPanel.add(nvLabel, gbc_);
    
    // Empty values work for the moment:
    nvptpEdit_ = new NameValuePairTablePanel(appState_, parent_, new NameValuePairList(), 
                                             new HashSet(), new HashSet(), new HashMap(), true);
    nvHolderEdit_ = new JPanel();
    nvHolderEdit_.setLayout(new GridLayout(1, 1));
    nvHolderEdit_.setMinimumSize(new Dimension(200, 100));
    nvHolderEdit_.add(nvptpEdit_);  
    UiUtil.gbcSet(gbc_, 0, editRow, 6, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    editRow += 5;
    editPanel.add(nvHolderEdit_, gbc_);
   
    //
    // Merge panel
    //
    
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout());
    int mergeRow = 0;  
    typeCombo_ = new JComboBox();
    typeCombo_.setEditable(true);
    abbrevCombo_ = new JComboBox();
    abbrevCombo_.setEditable(true);
    legCombo_ = new JComboBox();
    legCombo_.setEditable(true);
    legLabelMerge_ = new JLabel(rMan_.getString("pertPropEdit.legacy"));
    mergeRow = ds_.installLabelJCompTriple(mergePanel, 
                                           "pertPropEdit.type", typeCombo_, 
                                           "pertPropEdit.abbrev", abbrevCombo_,
                                            legLabelMerge_, legCombo_,
                                            mergeRow, 6);
  
    relationComboMerge_ = new JComboBox(); 
    mergeRow = ds_.installLabeledJComp(relationComboMerge_, mergePanel, "pertPropEdit.relation", mergeRow, 6);
    
    JLabel nvLabelToo = new JLabel(rMan_.getString("pertPropEdit.nvPairsLabel"));
    UiUtil.gbcSet(gbc_, 0, mergeRow++, 6, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    mergePanel.add(nvLabelToo, gbc_);
    
    // Empty values work for the moment:
    nvptpMerge_ = new NameValuePairTablePanel(appState_, parent_, new NameValuePairList(), 
                                              new HashSet(), new HashSet(), new HashMap(), true);
    nvHolderMerge_ = new JPanel();
    nvHolderMerge_.setLayout(new GridLayout(1, 1));
    nvHolderMerge_.setMinimumSize(new Dimension(200, 100));
    nvHolderMerge_.add(nvptpMerge_);  
    UiUtil.gbcSet(gbc_, 0, mergeRow, 6, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    mergeRow += 5;
    mergePanel.add(nvHolderMerge_, gbc_);
    
    makeMultiMode(editPanel, mergePanel, 8);
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Set the new pert props
  ** 
  */
  
  public void setPertProps(String newKey) {
    mode_ = EDIT_MODE;
    currKey_ = newKey;
    allMerge_ = null;
    setPertPropsCommon();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new pert props for duplication
  ** 
  */
  
  public void setPertPropsForDup(String dupKey, String copyType, String copyAbbrev, String copyLegAlt) {
    mode_ = DUP_MODE;
    currKey_ = dupKey;
    allMerge_ = null;
    copyType_ = copyType; 
    copyAbbrev_ = copyAbbrev;
    copyLegAlt_ = copyLegAlt;
    setPertPropsCommon();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new pert props for duplication
  ** 
  */
  
  public String setPertPropsForMerge(List joinKeys) {
    mode_ = MERGE_MODE;
    allMerge_ = new HashSet(joinKeys);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map refCounts = da.getAllPertPropReferenceCounts();
    currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys);
    PertDictionary pd = pd_.getPertDictionary();
    nameOptions_ = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    abbrevOptions_ = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    altAbbrevOptions_ = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    int numk = joinKeys.size();     
    for (int i = 0; i < numk; i++) {
      String nextJk = (String)joinKeys.get(i);
      PertProperties pp = pd.getPerturbProps(nextJk);
      nameOptions_.add(pp.getType());
      String abbrev = pp.getAbbrev();
      if (abbrev == null) {
        abbrev = "";
      }
      abbrevOptions_.add(abbrev);
      String lat = pp.getLegacyAlt();
      if (lat == null) {
        lat = "";
      }
      altAbbrevOptions_.add(lat);
    }
    displayProperties();
    setPertPropsCommon();
    return (currKey_);
  }

  /***************************************************************************
  **
  ** common setup
  ** 
  */
  
  private void setPertPropsCommon() {
    PertDictionary pDict = pd_.getPertDictionary();
    HashMap nvPairs = new HashMap(); 
    HashSet allNames = new HashSet();
    HashSet allVals = new HashSet();
    pDict.getNVPairs(nvPairs, allNames, allVals);   
       
    // Easier to just build a new table at the moment:
    NameValuePairList currList = new NameValuePairList();
    currList.addNameValuePair(new NameValuePair("Iam", "bogus"));
    
    NameValuePairTablePanel nvpNew = new NameValuePairTablePanel(appState_, parent_, currList, allNames, allVals, nvPairs, true);
    JPanel useHolder;
    if (mode_ == MERGE_MODE) {
      nvptpMerge_ = nvpNew;
      useHolder = nvHolderMerge_;
    } else {
      nvptpEdit_ = nvpNew;
      useHolder = nvHolderEdit_;
    }
    useHolder.removeAll();
    useHolder.add(nvpNew);
    useHolder.invalidate();
    displayProperties();
    useHolder.validate();
    return;
  }
  
  /***************************************************************************
  **
  ** Get the new pert props
  ** 
  */
  
  public PertProperties getResult() {
    return (newProps_);
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
    Vector<EnumChoiceContent<PertDictionary.PertLinkRelation>> vec = PertDictionary.PertLinkRelation.getLinkRelationshipOptions(appState_);    
    if (mode_ == MERGE_MODE) {
      UiUtil.replaceComboItems(typeCombo_, new Vector(nameOptions_));
      UiUtil.replaceComboItems(abbrevCombo_, new Vector(abbrevOptions_));
      UiUtil.replaceComboItems(legCombo_, new Vector(altAbbrevOptions_));
      UiUtil.replaceComboItems(relationComboMerge_, vec);
    } else {
      UiUtil.replaceComboItems(relationComboEdit_, vec);  
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Install our UI values:
  ** 
  */
  
  protected boolean stashResults() {

    String typeText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(typeCombo_.getEditor().getEditorComponent());
      typeText = jtc.getText().trim();
    } else {
      typeText = typeFieldEdit_.getText().trim();
    }
    if (typeText.equals("")) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pertPropEdit.emptyType"),
                                    rMan_.getString("pertPropEdit.emptyTypeTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }

    String abbrevText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(abbrevCombo_.getEditor().getEditorComponent());
      abbrevText = jtc.getText().trim();
    } else {
      abbrevText = abbrevFieldEdit_.getText().trim();
    }
    if (abbrevText.equals("")) {
      abbrevText = null;
    }
     
    String legacyAltText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(legCombo_.getEditor().getEditorComponent());
      legacyAltText = jtc.getText().trim();
    } else {
      legacyAltText = legFieldEdit_.getText().trim();
    }
    if (legacyAltText.equals("")) {
      legacyAltText = null;
    }    

    //
    // No duplicate names or abbrev allowed
    //
    
    PertDictionary pDict = pd_.getPertDictionary();
    Iterator<String> kit = pDict.getKeys();
   
    while (kit.hasNext()) {
      String key = kit.next();
      PertProperties pp = pDict.getPerturbProps(key);
      String type = pp.getType();
    
      if (((currKey_ == null) || !currKey_.equals(key)) && !((mode_ == MERGE_MODE) && allMerge_.contains(key))) {
        if (DataUtil.keysEqual(type, typeText)) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pertPropEdit.dupType"),
                                        rMan_.getString("pertPropEdit.dupTypeTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
        
        // combine abbrev and legacy abbrev namespaces for uniqueness:
        
        boolean problem = false;        
        String abbrev = pp.getAbbrev();
        String legAlt = pp.getLegacyAlt();
        
        if (abbrevText != null) {
          boolean atProb1 = (abbrev != null) && DataUtil.keysEqual(abbrev, abbrevText);
          boolean atProb2 = (legAlt != null) && DataUtil.keysEqual(legAlt, abbrevText);
          problem = atProb1 || atProb2;
        }
        if (legacyAltText != null) {
          boolean latProb1 = (abbrev != null) && DataUtil.keysEqual(abbrev, legacyAltText);
          boolean latProb2 = (legAlt != null) && DataUtil.keysEqual(legAlt, legacyAltText);
          problem = problem || latProb1 || latProb2;
        }
        if (problem) {
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pertPropEdit.dupAbbrev"),
                                        rMan_.getString("pertPropEdit.dupAbbrevTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      }
    }

    JComboBox useCombo = (mode_ == MERGE_MODE) ? relationComboMerge_ : relationComboEdit_;
    EnumChoiceContent<PertDictionary.PertLinkRelation> ecc = (EnumChoiceContent<PertDictionary.PertLinkRelation>)useCombo.getSelectedItem();
            
    //
    // NV table checks for errors:
    //   
    
    NameValuePairTablePanel useTab = (mode_ == MERGE_MODE) ? nvptpMerge_ : nvptpEdit_;
    NameValuePairList nvPairs = useTab.extractData();  
    if ((currKey_ == null) || (mode_ == DUP_MODE)) {   
      String nextKey = pDict.getNextDataKey();
      newProps_ = new PertProperties(nextKey, typeText, abbrevText, ecc.val);
    } else {
      PertProperties currProps = pDict.getPerturbProps(currKey_);
      boolean legacyAllowed = (currProps.getLegacyAlt() != null) || ((mode_ == MERGE_MODE) && (altAbbrevOptions_.size() > 1));      
      newProps_ = currProps.clone();         
      newProps_.setType(typeText);  
      newProps_.setAbbrev(abbrevText);
      if (legacyAllowed) {
        newProps_.setLegacyAlt(legacyAltText);
      }
      newProps_.setLinkSignRelationship(ecc.val);
      newProps_.clearNameValuePairs();
    }
    
    Iterator<NameValuePair> nvpit = nvPairs.getIterator();
    while (nvpit.hasNext()) {
      NameValuePair nvp = nvpit.next();
      newProps_.setNameValue(nvp);
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
  ** Build NV pairs.  Since it is likely in a merge that we have duplicate
  ** names, prefer the most common one, then collect up other names if we
  ** have them.
  ** 
  */
  
  private NameValuePairList buildNVPairs() {
    NameValuePairList nvPairs = new NameValuePairList();
    HashSet seenKeys = new HashSet();
    PertDictionary pdict = pd_.getPertDictionary();
    PertProperties currProps = pdict.getPerturbProps(currKey_);
    Iterator kit = currProps.getNvpKeys();
    while (kit.hasNext()) {
      String name = (String)kit.next();
      seenKeys.add(name);
      String value = currProps.getValue(name);
      nvPairs.addNameValuePair(new NameValuePair(name, value));
    }  
    if (mode_ == MERGE_MODE) {
      Iterator amit = allMerge_.iterator();
      while (amit.hasNext()) {
        String amkey = (String)amit.next();
        if (amkey.equals(currKey_)) {
          continue;
        }
        PertProperties otherProps = pdict.getPerturbProps(amkey);
        Iterator okit = otherProps.getNvpKeys();
        while (okit.hasNext()) {
          String name = (String)okit.next();
          if (!seenKeys.contains(name)) {
            seenKeys.add(name);
            String value = otherProps.getValue(name);
            nvPairs.addNameValuePair(new NameValuePair(name, value));
          }
        }
      }      
    }
    return (nvPairs); 
  }
    
  /***************************************************************************
  **
  ** Apply the current property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    
    updateOptions();
 
    if (currKey_ == null) {
      if (mode_ != EDIT_MODE) {
        throw new IllegalStateException();
      }
      String empty = "";
      typeFieldEdit_.setText(empty);
      abbrevFieldEdit_.setText(empty);
      legLabelEdit_.setEnabled(false);
      legFieldEdit_.setEditable(false);
      legFieldEdit_.setText(empty);  
      UiUtil.initCombo(relationComboEdit_);
      nvptpEdit_.resetNVPList(new NameValuePairList());
      cardLayout_.show(myCard_, EDIT_CARD);
      return;
    }
    
    PertDictionary pdict = pd_.getPertDictionary();
    PertProperties currProps = pdict.getPerturbProps(currKey_);   
    String currType = currProps.getType();
    String currAbbrev = currProps.getAbbrev();
    String legacy = currProps.getLegacyAlt();
    PertDictionary.PertLinkRelation lsr = currProps.getLinkSignRelationship();
    NameValuePairList nvPairs = buildNVPairs();
 
    switch (mode_) {
      case EDIT_MODE: 
      case DUP_MODE:
        nvptpEdit_.resetNVPList(nvPairs);
        typeFieldEdit_.setText((mode_ == EDIT_MODE) ? currType : copyType_);
        String useAbbrev = (mode_ == EDIT_MODE) ? currAbbrev : copyAbbrev_;
        abbrevFieldEdit_.setText((useAbbrev == null) ? "" : useAbbrev);     
        String useLeg = (mode_ == EDIT_MODE) ? legacy : copyLegAlt_;
        legFieldEdit_.setText((useLeg == null) ? "" : useLeg);       
        legLabelEdit_.setEnabled(useLeg != null);
        legFieldEdit_.setEditable(useLeg != null);
        relationComboEdit_.setSelectedItem(lsr.generateCombo(appState_));
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        nvptpMerge_.resetNVPList(nvPairs);    
        JTextComponent jtc = (JTextComponent)(typeCombo_.getEditor().getEditorComponent());
        jtc.setText(currType);
        jtc = (JTextComponent)(abbrevCombo_.getEditor().getEditorComponent());
        jtc.setText((currAbbrev == null) ? "" : currAbbrev);
        jtc = (JTextComponent)(legCombo_.getEditor().getEditorComponent());
        jtc.setText((legacy == null) ? "" : legacy);
        boolean legEnable = (altAbbrevOptions_.size() > 1) || (legacy != null);
        legLabelMerge_.setEnabled(legEnable);
        legCombo_.setEnabled(legEnable);
        relationComboMerge_.setSelectedItem(lsr.generateCombo(appState_));
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
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
    private String typeFieldText;
    private String abbrevFieldText;
    private String legFieldText;
    private List nvpValues;
    private EnumChoiceContent relationComboEcc;
    
    MyFreezeDried() {
         if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(typeCombo_.getEditor().getEditorComponent());
        typeFieldText = jtc.getText().trim();
        jtc = (JTextComponent)(abbrevCombo_.getEditor().getEditorComponent());
        abbrevFieldText = jtc.getText().trim();
        jtc = (JTextComponent)(legCombo_.getEditor().getEditorComponent());
        legFieldText = jtc.getText().trim();
        nvpValues = nvptpMerge_.getTable().getModel().getValuesFromTable();
        relationComboEcc = (EnumChoiceContent)relationComboMerge_.getSelectedItem();
      } else {
        typeFieldText = typeFieldEdit_.getText();
        abbrevFieldText = abbrevFieldEdit_.getText();
        legFieldText = legFieldEdit_.getText();  
        nvpValues = nvptpEdit_.getTable().getModel().getValuesFromTable();
        relationComboEcc = (EnumChoiceContent)relationComboEdit_.getSelectedItem();
      }
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        PertDictionary pdict = pd_.getPertDictionary();
        if (pdict.getPerturbProps(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(typeCombo_.getEditor().getEditorComponent());
        jtc.setText(typeFieldText);
        jtc = (JTextComponent)(abbrevCombo_.getEditor().getEditorComponent());
        jtc.setText(abbrevFieldText);
        jtc = (JTextComponent)(legCombo_.getEditor().getEditorComponent());
        jtc.setText(legFieldText);
        relationComboMerge_.setSelectedItem(relationComboEcc);
        nvptpMerge_.getTable().updateTable(true, nvpValues);
      } else { 
        typeFieldEdit_.setText(typeFieldText);
        abbrevFieldEdit_.setText(abbrevFieldText);
        legFieldEdit_.setText(legFieldText);  
        relationComboEdit_.setSelectedItem(relationComboEcc);
        nvptpEdit_.getTable().updateTable(true, nvpValues);
      }
      return;
    }
  }
}
