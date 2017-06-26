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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;

import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.analysis.CycleFinder;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.util.EnumCell;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.ui.dialogs.utils.EditableTable;

/****************************************************************************
**
** Dialog box for specifying region hierarchy during development
*/

public class DevelopmentSpecDialog extends JDialog implements DialogSupport.DialogSupportClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private EditableTable est_;
  private boolean haveResult_;
  private Map<String, String> mapping_;
  private Map<String, Integer> regAndTimes_;
  private ArrayList<String> regionList_;
  private List<EnumCell> parentList_;
  private UIComponentSource uics_;
  private TimeCourseData tcd_;
  
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
  
  public DevelopmentSpecDialog(UIComponentSource uics, TimeCourseData tcd, Map<String, Integer> regAndTimes) {     
    super(uics.getTopFrame(), uics.getRMan().getString("devSpec.title"), true);
    uics_ = uics;
    tcd_ = tcd;
    haveResult_ = false;
    regAndTimes_ = regAndTimes;
    regionList_ =  new ArrayList<String>(regAndTimes.keySet());
    Collections.sort(regionList_, String.CASE_INSENSITIVE_ORDER);
    parentList_ = buildParentEnum(regionList_);
          
    setSize(400, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
    
    //
    // Build the values table tabs.
    //

    est_ = new EditableTable(uics_, new RegionParentTableModel(uics_), uics_.getTopFrame());
    EditableTable.TableParams etp = new EditableTable.TableParams();
    etp.addAlwaysAtEnd = true;
    etp.tableIsUnselectable = true;
    etp.buttons = EditableTable.NO_BUTTONS;
    etp.singleSelectOnly = true;
    etp.perColumnEnums = new HashMap<Integer, EditableTable.EnumCellInfo>();
    etp.perColumnEnums.put(new Integer(RegionParentTableModel.PARENT_), new EditableTable.EnumCellInfo(false, parentList_, EnumCell.class));  
    JPanel tablePan = est_.buildEditableTable(etp);
    // FIX ME, MAKE AVAILABLE FROM SUPERCLASS
    ((DefaultTableCellRenderer)est_.getTable().getDefaultRenderer(String.class)).setHorizontalAlignment(JLabel.CENTER);
    UiUtil.gbcSet(gbc, 0, 0, 1, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(tablePan, gbc);
    
    DialogSupport ds = new DialogSupport(this, uics, gbc);
    ds.buildAndInstallButtonBox(cp, 8, 1, false, true);
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties(tcd_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (stashResults(true)) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    if (stashResults(false)) {
      setVisible(false);
      dispose();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Get the mapping
  ** 
  */
  
  public Map<String, String> getParentMapping() {
    return (mapping_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Used for the data table
  */
  
  class RegionParentTableModel extends EditableTable.TableModel {
    
    private final static int REGION_  = 0;
    final static int PARENT_   = 1;
    private final static int NUM_COL_ = 2;
    
    private static final long serialVersionUID = 1L;
    
    class TableRow {
      String region;
      EnumCell parent;
      
      TableRow() {   
      }
     
      TableRow(int i) {
        region = (String)columns_[REGION_].get(i);
        parent = (EnumCell)columns_[PARENT_].get(i);
      }
      
      void toCols() {
        columns_[REGION_].add(region);  
        columns_[PARENT_].add(parent);
        return;
      }
    }
  
    RegionParentTableModel(UIComponentSource uics) {
      super(uics, NUM_COL_);
      colNames_ = new String[] {"devSpec.region",
                                "devSpec.parent"};
      colClasses_ = new Class[] {String.class,
                                 EnumCell.class};
      canEdit_ = new boolean[] {false,
                                true};
    }
   
    public List<TableRow> getValuesFromTable() {
      ArrayList<TableRow> retval = new ArrayList<TableRow>();
      for (int i = 0; i < rowCount_; i++) {
        TableRow ent = new TableRow(i);
        retval.add(ent);
      }
      return (retval); 
    }
    
    // FIXME! Currently an OBJECT list in super signature!
    public void extractValues(List prsList) {
      super.extractValues(prsList);
      Iterator<TableRow> rit = prsList.iterator();
      while (rit.hasNext()) { 
        TableRow ent = rit.next();
        ent.toCols();
      }
      return;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out hierarchy; apply it
  ** 
  */  
  
  public static boolean assembleAndApplyHierarchy(UIComponentSource uics, TimeCourseData tcd, 
                                                  UndoFactory uFac, DataAccessContext dacx) {

    Map<String, Integer> regAndTimes = tcd.getRegionsWithMinTimes();
    DevelopmentSpecDialog dsd = new DevelopmentSpecDialog(uics, tcd, regAndTimes);
    dsd.setVisible(true);
    if (!dsd.haveResult()) {
      return (false);
    }
    
    //
    // Build data structure to submit:
    //
    
    HashSet<String> regionRoots = new HashSet<String>();
    HashMap<String, String> regionParents = new HashMap<String, String>();
    
    Map<String, String> pMap = dsd.getParentMapping();
    Iterator<String> rit = regAndTimes.keySet().iterator();
    while (rit.hasNext()) {
      String region = rit.next();
      String parentReg = pMap.get(region);
      if (parentReg == null) {
        regionRoots.add(region);
      } else {
        regionParents.put(region, parentReg);
      }
    }
 
    // Submit:
    
    UndoSupport support = uFac.provideUndoSupport("undo.applyRegionHierarchy", dacx);
    TimeCourseChange tcc = tcd.setRegionHierarchy(regionParents, regionRoots, true);
    support.addEdit(new TimeCourseChangeCmd(tcc));
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();          
  
    return (true);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties(TimeCourseData tcd) {
    List<RegionParentTableModel.TableRow> entries = tableEntries(tcd);
    est_.getModel().extractValues(entries);
    return;
  }
   
  /***************************************************************************
  **
  ** Build the Enum of parents
  ** 
  */
  
  private ArrayList<EnumCell> buildParentEnum(List<String> regionList) {
    ArrayList<EnumCell> retval = new ArrayList<EnumCell>();
    ResourceManager rMan = uics_.getRMan();
    String noParent = rMan.getString("devSpec.noParent");
    retval.add(new EnumCell(noParent, null, 0, 0));
    int count = 1;
    
    int numReg = regionList.size();
    for (int i = 0; i < numReg; i++) {
      String regName = regionList.get(i);          
      retval.add(new EnumCell(regName, regName, count, count));
      count++;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      List vals = est_.getModel().getValuesFromTable();
      if (vals.isEmpty()) {
        return (true);
      }     
      if (!checkValues(vals)) {
        haveResult_ = false;
        return (false);
      }
      mapping_ = buildResult(vals);
      haveResult_ = true;
      return (true);
    } else {
      haveResult_ = false;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Build table list
  ** 
  */ 
   
  private List<RegionParentTableModel.TableRow> tableEntries(TimeCourseData tcd) {
    ArrayList<RegionParentTableModel.TableRow> retval = new ArrayList<RegionParentTableModel.TableRow>();
    RegionParentTableModel rpt = (RegionParentTableModel)est_.getModel();  
 
    //
    // The parent list consists of all the regions with a "no parent"
    // option added at the top.
    //

    Iterator<String> rlit = regionList_.iterator();
    int numPar = parentList_.size();
    while (rlit.hasNext()) {
      String entry = rlit.next();
      RegionParentTableModel.TableRow tr = rpt.new TableRow();
      int useIndex = 0;
      tr.region = entry;
      if (!tcd.hierarchyIsSetForRegion(entry)) {
        useIndex = 0;
      } else {
        if (tcd.regionIsRoot(entry)) {
          useIndex = 0;
        } else {
          String parentRegion = tcd.getParentRegion(entry);
          for (int i = 1; i < numPar; i++) {
            EnumCell ecp = parentList_.get(i);
            if (parentRegion.equals(ecp.internal)) {
              useIndex = i;
              break;
            }
          }
          if (useIndex == 0) {
            throw new IllegalStateException();
          }
        }
      }
      tr.parent = new EnumCell(parentList_.get(useIndex));
      retval.add(tr);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Check values for correctness
  ** 
  */
   
  private boolean checkValues(List<RegionParentTableModel.TableRow> vals) {
    //
    // Make sure that there are no loops in the parenting.  Make sure
    // somebody has a null parent.  Kids cannot appear before the parent.
    //

    HashSet<String> nodeSet = new HashSet<String>();
    HashSet<Link> linkSet = new HashSet<Link>();
    boolean atLeastOne = false;
    int num = vals.size();
    for (int i = 0; i < num; i++) {
      RegionParentTableModel.TableRow tr = vals.get(i);
      String region = tr.region;
      EnumCell parent = tr.parent;
      if (parent.internal != null) {
        Integer parentMin = regAndTimes_.get(parent.internal);
        Integer kidMin = regAndTimes_.get(region);
        if (kidMin.intValue() < parentMin.intValue()) {
          ResourceManager rMan = uics_.getRMan();
          JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("devSpec.kidBeforeParent"),
                                        rMan.getString("devSpec.kidBeforeParentTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
        linkSet.add(new Link(parent.internal, region));
      } else {
        atLeastOne = true;
      }
      nodeSet.add(region);
    }

    if (!atLeastOne) {
      ResourceManager rMan = uics_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("devSpec.hasNoRoot"),
                                    rMan.getString("devSpec.HasNoRootTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }

    CycleFinder cf = new CycleFinder(nodeSet, linkSet);
    if (cf.hasACycle()) {
      ResourceManager rMan = uics_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), rMan.getString("devSpec.hasCycle"),
                                    rMan.getString("devSpec.hasCycleTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    return (true);
  }
    
  /***************************************************************************
  **
  ** Build the result 
  ** 
  */ 
     
  private Map<String, String> buildResult(List<RegionParentTableModel.TableRow> vals) {
    HashMap<String, String> retval = new HashMap<String, String>();
    int num = vals.size();
    //
    // Build the map.  No entry for regions without parents
    //
    for (int i = 0; i < num; i++) {
      RegionParentTableModel.TableRow tr = vals.get(i);
      String region = tr.region;
      EnumCell parent = tr.parent;
      if (parent.internal != null) {
        retval.put(region, parent.internal);
      }
    }
    return (retval);
  }    
}
