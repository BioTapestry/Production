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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.ReadOnlyTable;
import org.systemsbiology.biotapestry.ui.dialogs.utils.SelectionOracle;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Helpers for perturbation management IO
*/

public class PertManageHelper  {

  
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
  private ResourceManager rMan_;
  private JFrame parent_;
  private GridBagConstraints gbc_;
  private ImageIcon jump_;
  private UIComponentSource uics_;
  private TimeCourseData tcd_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public PertManageHelper(UIComponentSource uics, JFrame parent, PerturbationData pd, TimeCourseData tcd, ResourceManager rMan, 
                          GridBagConstraints gbc, PendingEditTracker pet) {
    uics_ = uics;
    tcd_ = tcd;
    parent_ = parent;
    pd_ = pd;
    rMan_ = rMan;
    gbc_ = gbc;
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/FastForward16.gif"); 
    jump_ = new ImageIcon(ugif);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Get jump icon
  ** 
  */
  
  public ImageIcon getJumpIcon() {
    return (jump_);
   }
    
  /***************************************************************************
  **
  ** Get a new unique copy name
  ** 
  */
  
  public String getNewUniqueCopyName(Set<String> existingNames, String baseName) {
    int lastCopyNum = 0;
    while (true) {
      String origName = baseName;
      String testName = UiUtil.createCopyName(uics_.getRMan(), origName, lastCopyNum++);
      if (!DataUtil.containsKey(existingNames, testName)) {
        return (testName);
      }
    } 
  }

  /***************************************************************************
  **
  ** return the most used key
  ** 
  */
  
  public String getMostUsedKey(Map<String, Integer> countMap, List<String> usedKeys) {  
    int numJoin = usedKeys.size();
    Integer noCount = new Integer(0);
    int maxRefs = Integer.MIN_VALUE;
    String useKey = null;
    for (int i = 0; i < numJoin; i++) {
      String targKey = usedKeys.get(i);
      Integer refs = countMap.get(targKey);
      if (refs == null) {
        refs = noCount;
      }
      if (refs.intValue() > maxRefs) {
        maxRefs = refs.intValue();
        useKey = targKey;
      }
    }
    return (useKey);
  }
  
  /***************************************************************************
  **
  ** Helper for table selections
  ** 
  */
  
  public int findSelectedRow(ReadOnlyTable rtab, String whichKey, SelectionOracle oracle, String tableID) {  
    int selRow = -1;
    if (whichKey != null) {
      int numElem = rtab.rowElements.size();
      for (int j = 0; j < numElem; j++) {
        Object obj = rtab.rowElements.get(j);
        if (oracle.tableRowMatches(whichKey, obj, tableID)) {
          selRow = rtab.getModel().mapToSelectionIndex(obj, rtab.rowElements);    
          break;
        }
      }
    }
    return (selRow);
  } 

  /***************************************************************************
  **
  ** Helper for table selections
  ** 
  */
  
  public void selectTableRow(ReadOnlyTable rtab, String whichKey, SelectionOracle oracle, String tableID) {  
    int selRow = findSelectedRow(rtab, whichKey, oracle, tableID);
    if (selRow == -1) {
      rtab.clearSelections(false);    
    } else {
      rtab.getTable().getSelectionModel().setSelectionInterval(selRow, selRow);
      rtab.makeSelectionVisible(selRow);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Helper to update and reselect
  ** 
  */
  
  public void updateAndReselect(boolean fireChange, ReadOnlyTable rtab, 
                                SelectionOracle oracle, String tableID, List<String> selKeys) {  
 
    rtab.updateTable(fireChange, ReadOnlyTable.UPDATE_NO_SELECT);
    if ((selKeys != null) && !selKeys.isEmpty()) {
      ListSelectionModel lsm = rtab.getTable().getSelectionModel();
      int numSel = selKeys.size();
      for (int i = 0; i < numSel; i++) {
        String key = selKeys.get(i);
        int selRow = findSelectedRow(rtab, key, oracle, tableID);
        if (selRow != -1) {
          lsm.addSelectionInterval(selRow, selRow);
        }
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Get the internal value out of a OneEnumTableModel
  ** 
  */
  
  public String getSelectedEnumVal(EditableTable et) {  
    int[] sel = et.getSelectedRows();
    if (sel.length != 1) {
      return (null);
    }   
    EditableTable.OneEnumTableModel oetm = (EditableTable.OneEnumTableModel)et.getModel();
    List vft = oetm.getValuesFromTable();
    EditableTable.OneEnumTableModel.TableRow tr = (EditableTable.OneEnumTableModel.TableRow)vft.get(sel[0]);
    return (tr.enumChoice.internal);
  } 

  /***************************************************************************
  **
  ** Handle annot table details
  ** 
  */
  
  public EditableTable.TableParams tableParamsForAnnot(List<EnumCell> annotList) {  
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.tableIsUnselectable = false;
    etp.buttons = EditableTable.ADD_BUTTON | EditableTable.DELETE_BUTTON;
    etp.singleSelectOnly = true;
    etp.buttonsOnSide = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(EditableTable.OneEnumTableModel.ENUM_COL_), new EditableTable.EnumCellInfo(false, annotList, EnumCell.class));  
    return (etp);
  }

  /***************************************************************************
  **
  ** Build the Enum of available annotations
  ** 
  */
  
  public ArrayList<EnumCell> buildAnnotEnum() {
    Vector<TrueObjChoiceContent> annotOps = pd_.getPertAnnotations().getAnnotationOptions();    
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    Iterator<TrueObjChoiceContent> ait = annotOps.iterator();
    int count = 0;
    while (ait.hasNext()) {
      TrueObjChoiceContent tocc = ait.next();
      retval.add(new EnumCell(tocc.name, (String)tocc.val, count, count));
      count++;
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Get the annotation display list
  */
  
  public List<EditableTable.OneEnumTableModel.TableRow> buildAnnotDisplayList(List annotKeys, EditableTable estAnnot, List<EnumCell> annotList, boolean forHotUpdate) {
    ArrayList<EditableTable.OneEnumTableModel.TableRow> retval = new ArrayList<EditableTable.OneEnumTableModel.TableRow>();
    if (annotKeys == null) {
      return (retval);
    }
    EditableTable.OneEnumTableModel rpt = (EditableTable.OneEnumTableModel)estAnnot.getModel();  
 
    Iterator akit = annotKeys.iterator();
    int count = 0;
    int useIndex = -1;
    int numA = annotList.size();
    while (akit.hasNext()) {
      String annotID;
      if (forHotUpdate) {
        annotID = ((EditableTable.OneEnumTableModel.TableRow)akit.next()).enumChoice.internal;
      } else {    
        annotID = (String)akit.next();
      }
      EditableTable.OneEnumTableModel.TableRow tr = rpt.new TableRow();
      tr.origOrder = new Integer(count++);
      for (int i = 0; i < numA; i++) {
        EnumCell ecp = annotList.get(i);
        if (annotID.equals(ecp.internal)) {
          useIndex = i;
          break;
        }
      }
      if (useIndex == -1) {
        if (forHotUpdate) {
          continue;
        } else {
          throw new IllegalStateException();
        }
      }
      tr.enumChoice = new EnumCell(annotList.get(useIndex));
      retval.add(tr);
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Fixup frozen annotations
  ** 
  */
  
  public List<String> fixupFrozenAnnot(List<String> frozenAnnots) { 
    ArrayList<String> currAnnots;
    if (frozenAnnots == null) {
      return (null);
    } else {
      currAnnots = new ArrayList<String>();
      HashSet<String> surviving = new HashSet<String>();
      Vector<TrueObjChoiceContent> annotOps = pd_.getPertAnnotations().getAnnotationOptions();
      Iterator<TrueObjChoiceContent> ait = annotOps.iterator();
      while (ait.hasNext()) {
        TrueObjChoiceContent tocc = ait.next();
        surviving.add((String)tocc.val);
      }
      int numFroz = frozenAnnots.size();
      for (int i = 0; i < numFroz; i++) {
        String testKey = frozenAnnots.get(i);
        if (surviving.contains(testKey)) {
          currAnnots.add(testKey);
        }
      }
    }
    return (currAnnots);
  }  
 
  /***************************************************************************
  **
  ** Fixup frozen region restrictions
  ** 
  */ 
  
  public PerturbationData.RegionRestrict fixupFrozenRegRes(PerturbationData.RegionRestrict frozenRegRes) {  
    PerturbationData.RegionRestrict currRegRes;
    if (frozenRegRes == null) {
      return (null);
    } else {
      ArrayList<String> regList = new ArrayList<String>();
      Set<String> surviving = tcd_.getRegions();
      Iterator<String> rit = frozenRegRes.getRegions();
      while (rit.hasNext()) {
        String region = rit.next();
        if (DataUtil.containsKey(surviving, region)) {
          regList.add(region);
        }
      }
      currRegRes = new PerturbationData.RegionRestrict(regList);      
    }       
    return (currRegRes); 
  }

  /***************************************************************************
  **
  ** Add a jump button
  ** 
  */
  
  public JPanel addEditButton(JPanel tablePane, String label, boolean onBottom, ActionListener alisten) { 
    JPanel tableWithButton = new JPanel();
    tableWithButton.setLayout(new GridBagLayout());
    UiUtil.gbcSet(gbc_, 0, 0, 5, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
    tableWithButton.add(tablePane, gbc_);
    if (onBottom) {
      UiUtil.gbcSet(gbc_, 0, 5, 5, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    } else {
      UiUtil.gbcSet(gbc_, 5, 0, 1, 5, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    }
    JButton jumpButton = new JButton(rMan_.getString(label), jump_);  
    jumpButton.addActionListener(alisten);
    tableWithButton.add(jumpButton, gbc_);
    tableWithButton.setBorder(BorderFactory.createEtchedBorder());
    return (tableWithButton);
  }

  /***************************************************************************
  **
  ** Consolidate
  ** 
  */
  
  public JPanel componentWithJumpButton(JComponent comp, JButton jump) { 
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    UiUtil.gbcSet(gbc_, 0, 0, 4, 1, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);       
    retval.add(comp, gbc_);
    UiUtil.gbcSet(gbc_, 4, 0, 1, 1, UiUtil.NONE, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 0.0, 1.0);
    retval.add(jump, gbc_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Inform the user of data losses
  */ 
  
  public boolean warnAndAsk(DependencyAnalyzer.Dependencies refs) {    
    if (refs.type == DependencyAnalyzer.Dependencies.DepType.DESTROY) {
      return (warnAndAskToDie(refs));
    } else if ((refs.type == DependencyAnalyzer.Dependencies.DepType.MERGE_SOURCE_DEFS) ||
               (refs.type == DependencyAnalyzer.Dependencies.DepType.MERGE_PERT_PROPS) ||
               (refs.type == DependencyAnalyzer.Dependencies.DepType.MERGE_SOURCE_NAMES)) {
      return (warnAndAskToDieOnMerge(refs));      
    } else if ((refs.type == DependencyAnalyzer.Dependencies.DepType.PRUNE_INVEST) ||
               (refs.type == DependencyAnalyzer.Dependencies.DepType.PRUNE_ANNOT) ||
               (refs.type == DependencyAnalyzer.Dependencies.DepType.PRUNE_CONTROL)) {
      return (warnAndAskToPrune(refs));
    } else {
      throw new IllegalStateException();
    }
  }  
    
  /***************************************************************************
  **
  ** Inform the user of data losses
  */ 
  
  public boolean warnAndAskToDie(DependencyAnalyzer.Dependencies refs) {
    
    int numWarns = 0;
    
    String measWarn = null;
    if ((refs.measureProps != null) && (refs.measureProps.size() > 0)) {
      measWarn = MessageFormat.format(rMan_.getString("pertDelete.measurePropFormat"), 
                                        new Object[] {new Integer(refs.measureProps.size())});
      numWarns++;
    }

    String sourceWarn = null;
    if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
      sourceWarn = MessageFormat.format(rMan_.getString("pertDelete.pertSourcesFormat"), 
                                        new Object[] {new Integer(refs.pertSources.size())});
      numWarns++;
    }
    
    String sourceInfoWarn = null;
    if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
      sourceInfoWarn = MessageFormat.format(rMan_.getString("pertDelete.pertSourceInfosFormat"), 
                                            new Object[] {new Integer(refs.experiments.size())});
      numWarns++;
    }
      
    String dataPointWarn = null;
    if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
      dataPointWarn = MessageFormat.format(rMan_.getString("pertDelete.dataPointFormat"), 
                                            new Object[] {new Integer(refs.dataPoints.size())});
      numWarns++;
    }
    
    String timeCourseWarn = null;
    if ((refs.timeCourseRefs != null) && (refs.timeCourseRefs.size() > 0)) {
      timeCourseWarn = MessageFormat.format(rMan_.getString("pertDelete.timeCourseFormat"), 
                                            new Object[] {new Integer(refs.timeCourseRefs.size())});
      numWarns++;
    }
    
    
    
    if (numWarns == 0) {
      return (true);
    }
    
    StringBuffer buf = new StringBuffer();
      
    buf.append(rMan_.getString("pertDelete.haveDependencies"));
    
    if (measWarn != null) {
      buf.append(measWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    if (sourceWarn != null) {
      buf.append(sourceWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    if (sourceInfoWarn != null) {
      buf.append(sourceInfoWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    if (dataPointWarn != null) {
      buf.append(dataPointWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    
    if (timeCourseWarn != null) {
      buf.append(timeCourseWarn);     
    }
    
    buf.append("\n");
    buf.append(rMan_.getString("pertDelete.continueQ"));
    String desc = UiUtil.convertMessageToHtml(buf.toString());
    int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                             rMan_.getString("pertDelete.gottaDeleteTitle"),
                                             JOptionPane.OK_CANCEL_OPTION);
    return (doit == JOptionPane.OK_OPTION);
  }  
  
  /***************************************************************************
  **
  ** Inform the user of data losses
  */ 
  
  public boolean warnAndAskToDieOnMerge(DependencyAnalyzer.Dependencies refs) {
    
    int numWarns = 0;
 
    String timeCourseWarn = null;
    if ((refs.timeCourseMergeRefs != null) && (refs.timeCourseMergeRefs.size() > 0)) {
      timeCourseWarn = MessageFormat.format(rMan_.getString("pertDelete.timeCourseFormat"), 
                                            new Object[] {new Integer(refs.timeCourseMergeRefs.size())});
      numWarns++;
    }
        
    if (numWarns == 0) {
      return (true);
    }
    
    StringBuffer buf = new StringBuffer();
      
    buf.append(rMan_.getString("pertDelete.haveTCMergeDependencies"));
    
    if (timeCourseWarn != null) {
      buf.append(timeCourseWarn);     
    }
    
    buf.append("\n");
    buf.append(rMan_.getString("pertDelete.continueQ"));
    String desc = UiUtil.convertMessageToHtml(buf.toString());
    int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                             rMan_.getString("pertDelete.gottaDeleteTCMergeTitle"),
                                             JOptionPane.OK_CANCEL_OPTION);
    return (doit == JOptionPane.OK_OPTION);
  }  

  /***************************************************************************
  **
  ** Get a unique from a vector of toccs
  */ 
  
  public String getUnique(Vector<TrueObjChoiceContent> sno, String baseName) {
    HashSet<String> existing = new HashSet<String>();
    Iterator<TrueObjChoiceContent> snit = sno.iterator();  
    while (snit.hasNext()) {
      TrueObjChoiceContent tocc = snit.next();
      existing.add(tocc.name);
    }
    String copyName = getNewUniqueCopyName(existing, baseName); 
    return (copyName);          
  }
  
   /***************************************************************************
  **
  ** Inform the user of data losses
  */ 
  
  public boolean warnAndAskToPrune(DependencyAnalyzer.Dependencies refs) {
    
    int numWarns = 0;
    
    String sourceDefWarn = null;
    if ((refs.pertSources != null) && (refs.pertSources.size() > 0)) {
      sourceDefWarn = MessageFormat.format(rMan_.getString("pertPrune.prunePertSourceDefsFormat"), 
                                            new Object[] {new Integer(refs.pertSources.size())});
      numWarns++;
    }
 
    String sourceInfoWarn = null;
    if ((refs.experiments != null) && (refs.experiments.size() > 0)) {
      sourceInfoWarn = MessageFormat.format(rMan_.getString("pertPrune.prunePertSourceInfosFormat"), 
                                            new Object[] {new Integer(refs.experiments.size())});
      numWarns++;
    }
      
    String dataPointWarn = null;
    if ((refs.dataPoints != null) && (refs.dataPoints.size() > 0)) {
      dataPointWarn = MessageFormat.format(rMan_.getString("pertPrune.pruneDataPointFormat"), 
                                            new Object[] {new Integer(refs.dataPoints.size())});
      numWarns++;
    }
    
    if (numWarns == 0) {
      return (true);
    }
    
    StringBuffer buf = new StringBuffer();
      
    buf.append(rMan_.getString("pertPrune.havePruneDependencies"));
    
    if (sourceDefWarn != null) {
      buf.append(sourceDefWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    if (sourceInfoWarn != null) {
      buf.append(sourceInfoWarn);
      if (--numWarns > 0) {
        buf.append("\n");
      }
    }
    if (dataPointWarn != null) {
      buf.append(dataPointWarn);     
    }
    
    buf.append("\n");
    buf.append(rMan_.getString("pertPrune.continueQ"));
    String desc = UiUtil.convertMessageToHtml(buf.toString());
    int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                             rMan_.getString("pertPrune.gottaPruneTitle"),
                                             JOptionPane.OK_CANCEL_OPTION);
    return (doit == JOptionPane.OK_OPTION);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Useful concrete instantiation of the Table Model that shows two columns,
  ** and traffics in augmented TrueObjChoiceContent list elements.
  */

  public static class NameWithHiddenIDAndRefCountModel extends ReadOnlyTable.TableModel {
    public final static int  NAME      = 0;
    public final static int  REF_COUNT = 1;
    private final static int NUM_COL_  = 2; 
    
    private final static int HIDDEN_NAME_ID_ = 0;
    private final static int NUM_HIDDEN_     = 1;
    
    private static final long serialVersionUID = 1L;
 
    public NameWithHiddenIDAndRefCountModel(UIComponentSource uics, String mainColName) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {mainColName,
                               "pertHelper.refCount"};
      colClasses_ = new Class[] {String.class,
                                 Integer.class};
      comparators_ = new Comparator[] {String.CASE_INSENSITIVE_ORDER,
                                       new ReadOnlyTable.IntegerComparator()};   
      addHiddenColumns(NUM_HIDDEN_);
    }    
    
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      hiddenColumns_[HIDDEN_NAME_ID_].clear();
      Iterator iit = prsList.iterator();
      while (iit.hasNext()) { 
        ToccWithRefCount twr = (ToccWithRefCount)iit.next();
        columns_[NAME].add(twr.tocc.name); 
        columns_[REF_COUNT].add(new Integer(twr.refCount)); 
        hiddenColumns_[HIDDEN_NAME_ID_].add(twr.tocc.val);
      }
      return;
    }
    
    public String getSelectedKey(int[] selected) {
      return ((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[0])));
    } 
    
    public List<String> getSelectedKeys(int[] selected) {
      ArrayList<String> retval = new ArrayList<String>();
      for (int i = 0; i < selected.length; i++) {
        retval.add((String)hiddenColumns_[HIDDEN_NAME_ID_].get(mapSelectionIndex(selected[i])));
      }
      return (retval);
    }
  }
  
  public static class ToccWithRefCount {
    public TrueObjChoiceContent tocc;
    public int refCount;
 
    public ToccWithRefCount(TrueObjChoiceContent tocc, int refCount) {
      this.tocc = tocc;
      this.refCount = refCount;
    }    
  }

  /***************************************************************************
  **
  ** Used for the measurement value column
  **
  */
  
  public static class DoubleStrComparator implements Comparator<String> {
    private Pattern pattern_;
    private Matcher matcher_;
        
    public DoubleStrComparator() {
      pattern_ = Pattern.compile("--*");
      matcher_ = pattern_.matcher("");
    } 
    
    public int compare(String o1, String o2) {
      String str1 = o1.trim();
      String str2 = o2.trim();
      boolean str1IsNS = str1.equalsIgnoreCase("NS");
      boolean str2IsNS = str2.equalsIgnoreCase("NS");
      matcher_.reset(str1);
      boolean str1IsDashes = matcher_.matches();
      matcher_.reset(str2);
      boolean str2IsDashes = matcher_.matches();

      if (str1IsDashes || str2IsDashes) {
        if (str1IsDashes && str2IsDashes) {
          return (str1.length() - str2.length());
        } else if (str1IsDashes) {
          return (1);
        } else {
          return (-1);
        }
      }     
      
      Double doub1 = null;
      Double doub2 = null;
      try {
        doub1 = (str1IsNS) ? new Double(-1.0E-10) : new Double(str1);
      } catch (NumberFormatException nfex) {
        return (1);
      }
      try {
        doub2 = (str2IsNS) ? new Double(-1.0E-10) : new Double(str2);
      } catch (NumberFormatException nfex) {
        return (-1);
      }
          
      return (doub1.compareTo(doub2));    
    }
  }
  
  /***************************************************************************
  **
  ** Used to display time ranges
  **
  */
  
  public static class TimeComparator implements Comparator<String> {
    private TimeAxisDefinition tad_;
    private boolean namedStages_;
        
    public TimeComparator(TimeAxisDefinition tad) {
      tad_ = tad;
      namedStages_ = tad_.haveNamedStages();
    }
      
    public int compare(String o1, String o2) {
      String str1 = o1.trim();
      String str2 = o2.trim();
      int dash1 = str1.indexOf(" to ");
      int dash2 = str2.indexOf(" to ");
      
      String str1a = str1;
      String str1b = null;
      if (dash1 != -1) {
        str1a = str1.substring(0, dash1);
        str1b = str1.substring(dash1 + 4);    
      }
      
      String str2a = str2;
      String str2b = null;
      if (dash2 != -1) {
        str2a = str2.substring(0, dash2);
        str2b = str2.substring(dash2 + 4);    
      }
  
      int val1a;
      int val1b = TimeAxisDefinition.INVALID_STAGE_NAME;
      if (namedStages_) {
        val1a = tad_.getIndexForNamedStage(str1a.trim());
        if (str1b != null) {
          val1b = tad_.getIndexForNamedStage(str1b.trim());
        }      
      } else {
        val1a = new Integer(str1a).intValue();
        if (str1b != null) {
          val1b = new Integer(str1b).intValue();
        }            
      }
      
      int val2a;
      int val2b = TimeAxisDefinition.INVALID_STAGE_NAME;
      if (namedStages_) {
        val2a = tad_.getIndexForNamedStage(str2a.trim());
        if (str2b != null) {
          val2b = tad_.getIndexForNamedStage(str2b.trim());
        }      
      } else {
        val2a = new Integer(str2a).intValue();
        if (str2b != null) {
          val2b = new Integer(str2b).intValue();
        }            
      }
      
      //
      // Single time comes before a range if the first times match:
      //
      
      if (val1a != val2a) {
        return (val1a - val2a);
      }
      
      if ((val1b != TimeAxisDefinition.INVALID_STAGE_NAME) || 
          (val2b != TimeAxisDefinition.INVALID_STAGE_NAME)) {      
        if ((val1b != TimeAxisDefinition.INVALID_STAGE_NAME) && 
            (val2b != TimeAxisDefinition.INVALID_STAGE_NAME)) {
          return (val1b - val2b);
        } else if (val1b == TimeAxisDefinition.INVALID_STAGE_NAME) {
          return (-1);
        } else if (val2b == TimeAxisDefinition.INVALID_STAGE_NAME) {
          return (1);
        }
      }    
      return (0);
    }
  }

  /***************************************************************************
  **
  ** Used for the data table renderer
  */
  
  public static class FilterListRenderer extends JLabel implements ListCellRenderer {
    
    private Set activeValues_;
    private ListCellRenderer defaultRenderer_;
    private UIComponentSource uics_;
           
    public FilterListRenderer(UIComponentSource uics, ListCellRenderer defaultRenderer) {
      uics_ = uics;
      defaultRenderer_ = defaultRenderer;
      activeValues_ = new HashSet();
    }
    
    public void setActive(Set values) {
      activeValues_.clear();
      activeValues_.addAll(values);
      return;
    }
  
    public Component getListCellRendererComponent(JList list, 
                                                  Object value,
                                                  int index,
                                                  boolean isSelected, 
                                                  boolean hasFocus) {
      try {        
        setOpaque(isSelected); // MUST be the case, else selection does not color!        
        if (isSelected) {
          //
          // Crazy, huh?  But this seems to be the only way to find out what the final selected background
          // color needs to be for the opaque label.
          Component aComp = defaultRenderer_.getListCellRendererComponent(list, value, index, true, hasFocus);
          setBackground(aComp.getBackground());
        }                 
        if (value == null) {
          return (this);
        }        
        setText(value.toString());
        setEnabled(activeValues_.contains(value));        
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }      
      return (this);             
    }
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
}
