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

package org.systemsbiology.biotapestry.embedded;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** A wrapper demonstrating how to create an embedded viewer
*/

public class EmbeddedViewerPanelTestWrapper extends JFrame implements ExternalSelectionChangeListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private EmbeddedBioTapestryViewer evp_;
  private EmbeddedViewerInventory evi_;
  private JComboBox modelSelector_;
  private JList nodeSelectionList_;
  private JList linkSelectionList_;
  private JLabel selectedLabel_;
  private String currModelID_;
  private Stack<Boolean> skipIt_;
  private BTState appState_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    EmbeddedViewerPanelTestWrapper cc = new EmbeddedViewerPanelTestWrapper();
    cc.setVisible(true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
  public EmbeddedViewerPanelTestWrapper() {
    super("Embedded BioTapestry Viewer Example");
    appState_ = new BTState("WJRL", new HashMap<String, Object>(), false, false);
    setSize(1000, 1000);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());    
    GridBagConstraints gbc = new GridBagConstraints();
   
    JPanel pTop = new JPanel();
    pTop.setLayout(new GridBagLayout());
    skipIt_ = new Stack<Boolean>();
    skipIt_.push(Boolean.valueOf(false));
       
    //
    // Button for loading BioTapestry file:
    //
    
    FixedJButton button1 = new FixedJButton("Load BioTapestry File");
    button1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          loadAFile();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    UiUtil.gbcSet(gbc, 0, 0, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    pTop.add(button1, gbc);

    //
    // Model selection:
    //
    
    modelSelector_ = new JComboBox();
    modelSelector_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (skipIt_.peek().booleanValue()) {
            return;
          }
          selectAModel();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    UiUtil.gbcSet(gbc, 0, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    pTop.add(modelSelector_, gbc);
    
    //
    // No selections warning:
    //
    
    selectedLabel_ = new JLabel(""); 
 
    UiUtil.gbcSet(gbc, 0, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    pTop.add(selectedLabel_, gbc);
 
    //
    // Node selection:
    //

    nodeSelectionList_ = new JList(new DefaultListModel());
    nodeSelectionList_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    JScrollPane scrollPane = new JScrollPane(nodeSelectionList_);
    nodeSelectionList_.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent ev) {
        try {
          if (ev.getValueIsAdjusting()) {
            return;
          }
          if (skipIt_.peek().booleanValue()) {
            return;
          }
          selectNodesAndLinks();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc, 0, 3, 1, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    pTop.add(scrollPane, gbc);
        
    //
    // Link selection:
    //

    linkSelectionList_ = new JList(new DefaultListModel());
    linkSelectionList_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    scrollPane = new JScrollPane(linkSelectionList_);
    linkSelectionList_.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent ev) {
        try {
          if (ev.getValueIsAdjusting()) {
            return;
          }
          if (skipIt_.peek().booleanValue()) {
            return;
          }
          selectNodesAndLinks();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
 
    UiUtil.gbcSet(gbc, 1, 3, 1, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    pTop.add(scrollPane, gbc);
  
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(pTop, gbc);
    
    //
    // Embedded BioTapestry Viewer:
    //
    
    EmbeddedViewerPanel evp = new EmbeddedViewerPanel(this, appState_);
    evp.setBorder(BorderFactory.createTitledBorder("BioTapestry Network View"));
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(evp, gbc);
    evp_ = evp;
       
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); 
    addWindowListener(new WrapperListener());      
    evp_.addSelectionChangeListener(this);    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Handle selection change
  */ 
  
  public void selectionHasChanged(ExternalSelectionChangeEvent escev) {    
    changeNodeSelections(escev.getNodeSelections());
    changeLinkSelections(escev.getLinkSelections()); 
    return;
  }
  
  /***************************************************************************
  **
  ** Handle model selection change
  */ 
  
  public void selectedModelHasChanged(ExternalSelectionChangeEvent escev) {
    String modelID = escev.getSelectedModel();
    ComboBoxModel cbm = modelSelector_.getModel();
    int num = cbm.getSize();
    for (int i = 0; i < num; i++) {
      ObjChoiceContent selected = (ObjChoiceContent)cbm.getElementAt(i);
      if (selected.val.equals(modelID)) {
        skipIt_.push(Boolean.valueOf(true));
        modelSelector_.setSelectedItem(selected);
        skipIt_.pop();
        currModelID_ = selected.val;
        loadSelectionsForModel();
        break;
      }
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VIZ METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Handle model selection
  */   
  
  void selectAModel() { 
    ObjChoiceContent selected = (ObjChoiceContent)modelSelector_.getSelectedItem();
    currModelID_ = selected.val;
    loadSelectionsForModel();
    HashSet nodeIDs = new HashSet(); 
    HashSet linkIDs = new HashSet();
    evp_.goToModelAndSelect(currModelID_, nodeIDs, linkIDs); 
    return;
  }

  /***************************************************************************
  **
  ** Load in a new btp file
  */   
  
  void loadAFile() throws IOException { 
    File file = null;
    while (file == null) {
      JFileChooser chooser = new JFileChooser(); 
      int option = chooser.showOpenDialog(this);
      if (option != JFileChooser.APPROVE_OPTION) {
        return;
      }
      file = chooser.getSelectedFile();
      if (file == null) {
        continue;
      }
      try {
        evp_.loadBtp(new FileInputStream(file));
      } catch (EmbeddedException ge) {
        throw new IOException();
      } catch (IOException ioex) {
        throw new IOException();      
      }
      evi_ = evp_.getElementInventory();
      
      skipIt_.push(Boolean.valueOf(true));
      currModelID_ = null;
      Vector modelGuys = new Vector();
      Iterator omidit = evi_.getOrderedModelIDs();
      while (omidit.hasNext()) {
        String omid = (String)omidit.next();
        ExternalModelInfo exmi = evi_.getModelInfo(omid);
        String[] path = exmi.getModelNameChain();
        modelGuys.add(new ObjChoiceContent(pathToString(path), omid));
        if (currModelID_ == null) {
          currModelID_ = omid;
        }
      }      
      UiUtil.replaceComboItems(modelSelector_, modelGuys);
      skipIt_.pop();
      loadSelectionsForModel();
      clearEmbeddedSelections();
      return;
    }
  }
     
  /***************************************************************************
  **
  ** Select elements
  */   
  
  void selectNodesAndLinks() {
    skipIt_.push(Boolean.valueOf(true));
    DefaultListModel dlm = (DefaultListModel)nodeSelectionList_.getModel();
    HashSet nodeIDs = new HashSet();
    int[] indices = nodeSelectionList_.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      ObjChoiceContent occ = (ObjChoiceContent)dlm.getElementAt(indices[i]);
      nodeIDs.add(occ.val);
    }
    dlm = (DefaultListModel)linkSelectionList_.getModel();
    HashSet linkIDs = new HashSet();
    indices = linkSelectionList_.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      ObjChoiceContent occ = (ObjChoiceContent)dlm.getElementAt(indices[i]);
      linkIDs.add(occ.val);
    }
    boolean success = evp_.goToModelAndSelect(currModelID_, nodeIDs, linkIDs);
    if (!success) {
      evp_.goToModelAndSelect(currModelID_, new HashSet(), new HashSet());
    }
    skipIt_.pop();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Load in selections for newly selected model
  */   
  
  private void loadSelectionsForModel() {
    skipIt_.push(Boolean.valueOf(true));
    DefaultListModel dlm = (DefaultListModel)nodeSelectionList_.getModel();
    List nodes = evi_.getNodes(currModelID_);
    int numNodes = nodes.size();
   
    TreeSet sorted = new TreeSet();
    for (int i = 0; i < numNodes; i++) {
      ExternalInventoryNode ens = (ExternalInventoryNode)nodes.get(i);
      sorted.add(new ObjChoiceContent(ens.toString(), ens.getInternalID()));
    }
    
    dlm.clear();
    Iterator sit = sorted.iterator();
    while (sit.hasNext()) {
      dlm.addElement(sit.next());
    }   
    
    dlm = (DefaultListModel)linkSelectionList_.getModel();
    List links = evi_.getLinks(currModelID_);
    int numLinks = links.size();
    
    sorted = new TreeSet();
    for (int i = 0; i < numLinks; i++) {
      ExternalInventoryLink ens = (ExternalInventoryLink)links.get(i);
      sorted.add(new ObjChoiceContent(ens.toString(), ens.getInternalID()));
    }
    
    dlm.clear();
    sit = sorted.iterator();
    while (sit.hasNext()) {
      dlm.addElement(sit.next());
    }   
    
    skipIt_.pop();
    
    ExternalModelInfo emi = evi_.getModelInfo(currModelID_);
    boolean selSup = emi.supportsSelections();
    linkSelectionList_.setEnabled(selSup);
    nodeSelectionList_.setEnabled(selSup);
    selectedLabel_.setText((selSup) ? "" : "Selections not supported for dynamic submodels");
    selectedLabel_.invalidate();
    validate();
    return;
  }

  /***************************************************************************
  **
  ** Clear selections
  */   
  
  private void clearEmbeddedSelections() {
    skipIt_.push(Boolean.valueOf(true));
    if (currModelID_ != null) {
      HashSet nodeIDs = new HashSet(); 
      HashSet linkIDs = new HashSet();
      evp_.goToModelAndSelect(currModelID_, nodeIDs, linkIDs);
    }
    skipIt_.pop();
    return;
  }
    
  /***************************************************************************
  **
  ** Model name generator:
  */   
  
  private String pathToString(String[] path) {
    StringBuffer buf = new StringBuffer();
    int skipit = path.length - 1;
    for (int i = 0; i < path.length; i++) {
      String mName = path[i];
      buf.append(mName);
      if (i < skipit) {
        buf.append("::");
      }
    }
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Handle node selection changes
  */ 
  
  private void changeNodeSelections(Set nodes) {
    skipIt_.push(Boolean.valueOf(true));
    DefaultListModel dlm = (DefaultListModel)nodeSelectionList_.getModel();
    int numSel = nodes.size();
    if (numSel == 0) {
      nodeSelectionList_.clearSelection();
    } else {    
      HashSet ns = new HashSet(nodes);
      int[] indices = new int[numSel];
      int count = 0;
      int num = dlm.getSize();
      for (int i = 0; i < num; i++) {
        ObjChoiceContent selected = (ObjChoiceContent)dlm.getElementAt(i);
        if (ns.contains(selected.val)) {
          indices[count++] = i;
        }
      }      
      nodeSelectionList_.setSelectedIndices(indices);
      nodeSelectionList_.ensureIndexIsVisible(indices[0]);
    }
    skipIt_.pop();
    return;
  }
   
  /***************************************************************************
  **
  ** Handle link selection changes
  */ 
  
  private void changeLinkSelections(Set links) {
    skipIt_.push(Boolean.valueOf(true));
    DefaultListModel dlm = (DefaultListModel)linkSelectionList_.getModel();
   
    int numSel = links.size();
    if (numSel == 0) {
      linkSelectionList_.clearSelection();
    } else {
      HashSet ns = new HashSet(links);
      int[] indices = new int[numSel];
      int count = 0;
      int num = dlm.getSize();
      for (int i = 0; i < num; i++) {
        ObjChoiceContent selected = (ObjChoiceContent)dlm.getElementAt(i);
        if (ns.contains(selected.val)) {
          indices[count++] = i;
        }
      }      
      linkSelectionList_.setSelectedIndices(indices);
      linkSelectionList_.ensureIndexIsVisible(indices[0]);
    }
    skipIt_.pop();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Handle Window open and close
  */
  
  private class WrapperListener extends WindowAdapter {
    
    public void windowClosing(WindowEvent e) {
      evp_.callForShutdown();
      dispose();
      System.exit(0);
    }
  }
}