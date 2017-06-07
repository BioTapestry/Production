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

package org.systemsbiology.biotapestry.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.LocalWorkspaceSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.dialogs.ModelViewPanel;
import org.systemsbiology.biotapestry.ui.dialogs.ModelViewPanelWithZoom;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UnselectableJTable;

/****************************************************************************
**
** The multi strip chart
*/

public class ChartStackableModelView extends JPanel {

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
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private ModelViewPanel msp_;
  private CSTipGen tipGen_;
  private JComboBox batchCombo_;
  private boolean managingBatches_;
  private BatchTableModel btm_;   
  private UnselectableJTable ujt_;
  // STILL trying to track down the Object types here:
  private Map<Link, Map<String, List<Object>>> currBatchVals_;
  private Genome currGenome_;
  private StaticDataAccessContext rcx_;
  private UIComponentSource uics_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Null constructor
  */
  
  public ChartStackableModelView(UIComponentSource uics, DataAccessContext dacx) {
    managingBatches_ = false;
    uics_ = uics;
    setLayout(new GridBagLayout());    
    GridBagConstraints gbc = new GridBagConstraints();
    int rowNum = 0;

    //
    // Build the network view
    //
    
    // Note this is NOT legit, hitting the database from a dialog under the covers
    rcx_ = StaticDataAccessContext.getCustomDACX8(dacx);
    ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(uics, null, rcx_);
    msp_ = mvpwz.getModelView();
    tipGen_ = new CSTipGen();
    msp_.setToolTipGenerator(tipGen_);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 6, UiUtil.BO, 0, 0, 0, 5, 5, 5, UiUtil.CEN, 1.0, 0.5);    
    add(mvpwz, gbc);
    rowNum += 6;    

    //
    // Build the dropdown
    //
    
    batchCombo_ = new JComboBox();
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 0, 5, 5, 5, UiUtil.E, 0.0, 0.0);    
    add(batchCombo_, gbc);
    rowNum += 1;
    batchCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          setTheBatch();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    
    //
    // Build the table:
    //

    JPanel tab = buildBatchTable();   
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 6, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 0.5);    
    add(tab, gbc);
    rowNum += 6;
 
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the tool tip
  */
  
  public JComponent getToolTipTarget() {
    return (msp_);
  } 

 /***************************************************************************
 **
 ** Set the genome source
 */
  
  public void setSources(GenomeSource src, LayoutSource layoutSrc, Map<Link, Map<String, List<Object>>> batchData) {
    if (src == null) {
      rcx_.setGenome(null);
      rcx_.setLayout(null);
      tipGen_.setBatchSource(null);
      currBatchVals_ = null;
      installBatchSelections(new ArrayList<TrueObjChoiceContent>());
      msp_.repaint();
      return; 
    }
    
    currBatchVals_ = batchData;
    
    List<TrueObjChoiceContent> bSel = getBatchSelections(batchData);
    installBatchSelections(bSel);
    rcx_.setGenomeSource(src);
    currGenome_ = src.getRootDBGenome();
    String genomeID = currGenome_.getID();
    rcx_.setGenome(currGenome_);
    rcx_.setLayoutSource(layoutSrc);
    rcx_.setLayout(rcx_.getLayoutSource().getLayoutForGenomeKey(genomeID));
    rcx_.setFGHO(new FullGenomeHierarchyOracle(rcx_));
    rcx_.setWorkspaceSource(new LocalWorkspaceSource());
    tipGen_.setBatchSource(batchData);
    msp_.fixCenterPoint(false, null, false);
   // msp_.showFullModel();
  }
    
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMinimumSize() {
    return (getPreferredSize());
  }  
  
  /***************************************************************************
  **
  ** Get the minimum size
  */
  
  public Dimension getMaximumSize() {
    return (getPreferredSize());
  }    
 
  /***************************************************************************
  **
  ** Get the preferred size
  */
  
  public Dimension getPreferredSize() {
    return (new Dimension(400, 400));
  }
  
  /***************************************************************************
  **
  ** Set the batch
  */
  
  public void setTheBatch() {
    if (managingBatches_) {
      return;
    }

    TrueObjChoiceContent tocc = (TrueObjChoiceContent)batchCombo_.getSelectedItem();
    String batchID;
    if (tocc.val instanceof Integer) {
      batchID = null;
    } else {
      batchID = (String)tocc.val;
    }
    btm_.extractValues(currBatchVals_, batchID);
    ujt_.invalidate();
    validate();
    repaint();
    return;
  } 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Used to gather up data 
  */
  
  public static class CSTipGen implements ModelViewPanel.TipGenerator { 
    
    private Map<Link, Map<String, List<Object>>> batchSource_;
        
    public String generateTip(Genome genome, Layout layout, Intersection intersected) {
      String objID = intersected.getObjectID();
      Linkage link = genome.getLinkage(objID);  // This could be ANY link through a bus segment
      if (link == null) {
        return (null);
      }
      BusProperties bp = layout.getLinkProperties(objID);
      LinkSegmentID segID = intersected.segmentIDFromIntersect();
      Set<String> linksThru = bp.resolveLinkagesThroughSegment(segID);
      if (linksThru.size() != 1) {
        return (null);
      }
      String linkID = linksThru.iterator().next();
      link = genome.getLinkage(linkID);
      Link batchLink = new Link(link.getSource(), link.getTarget());
      Map<String, List<Object>> batchData = batchSource_.get(batchLink);
      if (batchData == null) {
        return (batchLink.toString());
      } else {
        return (batchData.toString());
      }
    }
    
    public void setBatchSource(Map<Link, Map<String, List<Object>>> batchSource) {
      batchSource_ = batchSource;
      return;
    }
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build the batch table
  */ 
  
  private JPanel buildBatchTable() {       
    GridBagConstraints gbc = new GridBagConstraints();
       
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    
    btm_ = new BatchTableModel();   
    ujt_ = new UnselectableJTable(btm_);
    JTableHeader th = ujt_.getTableHeader();
    th.setReorderingAllowed(false);
    
    JScrollPane jsp = new JScrollPane(ujt_);
    UiUtil.gbcSet(gbc, 0, 1, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(jsp, gbc);
        
    return (retval);
  } 
  
  
  /***************************************************************************
  **
  ** Get the batch selections. 
  */ 
  
  private List<TrueObjChoiceContent> getBatchSelections(Map<Link, Map<String, List<Object>>> batchMap) {
    ArrayList<TrueObjChoiceContent> retval = new ArrayList<TrueObjChoiceContent>();
    ResourceManager rMan = rcx_.getRMan();
    TreeSet<String> batchIDs = new TreeSet<String>();
    Iterator<Map<String, List<Object>>> vit = batchMap.values().iterator();
    while (vit.hasNext()) {
      Map<String, List<Object>> perLink = vit.next();
      Iterator<String> bit = perLink.keySet().iterator();
      while (bit.hasNext()) {
        String bID = bit.next();
        batchIDs.add(bID);
      }
    }
    retval.add(new TrueObjChoiceContent(rMan.getString("batchOpt.avg"), new Integer(0)));      
    retval.add(new TrueObjChoiceContent(rMan.getString("batchOpt.max"), new Integer(1)));
    Iterator<String> bidit = batchIDs.iterator();
    while (bidit.hasNext()) {
      String batchID = bidit.next();
      retval.add(new TrueObjChoiceContent(batchID, batchID));      
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Stock the batch selector
  */ 
  
  private void installBatchSelections(List<TrueObjChoiceContent> install) {
    managingBatches_ = true;
    batchCombo_.removeAllItems();
    int iNum = install.size();
    for (int i = 0; i < iNum; i++) {
      TrueObjChoiceContent tocc = install.get(i);
      batchCombo_.addItem(tocc);
    }
    managingBatches_ = false;
    return;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used for the batch table
  */
  
  class BatchTableModel extends AbstractTableModel {
   
    private ArrayList<String> srcColumn_;
    private ArrayList<String> targColumn_;
    private ArrayList<String> valColumn_;
    protected int rowCount_;
    private static final long serialVersionUID = 1L;

    BatchTableModel() {
    }
    
    public int getRowCount() {
      return (rowCount_); 
    }  
    
    public void setValueAt(Object value, int r, int c) {
      throw new IllegalStateException();
    }
    
    @Override
    public boolean isCellEditable(int r, int c) {
      return (false);
    }        
    
    public Object getValueAt(int r, int c) {
      try {
        switch (c) {
          case 0:
            return (srcColumn_.get(r));            
          case 1:
            return (targColumn_.get(r));
          case 2:
            return (valColumn_.get(r));
          default:
            throw new IllegalArgumentException();
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }
    
    public int getColumnCount() {
      return (3);
    }
    
    public Class getColumnClass(int c) {
      try {
        switch (c) {
          case 0:
          case 1:
          case 2:
            return (String.class);
          default:
            throw new IllegalArgumentException();
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }    

    public String getColumnName(int c) {
      try {
        ResourceManager rMan = rcx_.getRMan();
        switch (c) {
          case 0:
            return (rMan.getString("batchTab.source"));
          case 1:
            return (rMan.getString("batchTab.target"));
          case 2:
            return (rMan.getString("batchTab.value"));     
          default:
            throw new IllegalArgumentException();
        }
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      }
      return (null);
    }
   
    void extractValues(Map<Link, Map<String, List<Object>>> batchMap, String batchID) {
      srcColumn_ = new ArrayList<String>();
      targColumn_ = new ArrayList<String>();
      valColumn_ = new ArrayList<String>();
      rowCount_ = 0;
      if (batchID == null) {
        return;
      }
      Iterator<Link> kit = batchMap.keySet().iterator();
      while (kit.hasNext()) {
        Link link = kit.next();
        Map<String, List<Object>> perLink = batchMap.get(link);
        Iterator<String> bit = perLink.keySet().iterator();
        while (bit.hasNext()) {
          String bID = bit.next();
          if (bID.equals(batchID)) {
            String srcName = currGenome_.getNode(link.getSrc()).getName();       
            String trgName = currGenome_.getNode(link.getTrg()).getName();       
            srcColumn_.add(srcName);
            targColumn_.add(trgName);
            List<Object> perBatch = perLink.get(bID);
            valColumn_.add(perBatch.toString());
            rowCount_++;
          }
        }
      }
      return;
    }
  }
}
