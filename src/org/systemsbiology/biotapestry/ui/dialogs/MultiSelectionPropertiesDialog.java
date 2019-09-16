/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;

import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for editing node properties
*/

public class MultiSelectionPropertiesDialog extends JDialog implements DialogSupport.DialogSupportClient {
 
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

 // private String layoutKey_;
 // private String genomeKey_;
  private BTState appState_; 
  private DataAccessContext dacx_;
  
  private MultiNodeTab geneTab_;
  private MultiNodeTab nodeTab_;
  private MultiLinkTab linkTab_;
  
  private ImageIcon warnIcon_;
  private ConsensusNodeProps geneCp_;
  private ConsensusNodeProps nodeCp_;
  private ConsensusLinkProps linkCp_;
  private List<ColorDeletionListener> colorListeners_;
  
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
  
  public MultiSelectionPropertiesDialog(BTState appState, DataAccessContext dacx,
                                        SUPanel sup, Set<String> genes, Set<String> nodes, Set<String> links) {     
    super(appState.getTopFrame(), appState.getRMan().getString("multiSelProps.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    colorListeners_ = new ArrayList<ColorDeletionListener>();
    
    
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/Warn24.gif");  
    warnIcon_ = new ImageIcon(ugif);
 //   layoutKey_ = layoutKey;
  //  genomeKey_ = genomeKey;    
    
  //  Database db = appState_.getDB();
  //  Genome genome = db.getGenome(genomeKey_);
   // Layout layout = db.getLayout(layoutKey_);
 
    boolean haveDynInstance = (dacx.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx.getGenome() instanceof GenomeInstance) && !haveDynInstance;
      
    ResourceManager rMan = appState_.getRMan();
    setSize(850, (haveDynInstance)? 400 : 500);  // Mac needs 850...
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
    int rownum = 0;
    
    //
    // Build the tabs.
    //

    JTabbedPane tabPane = new JTabbedPane();
    if (!genes.isEmpty()) {
      geneTab_ = new MultiNodeTab(appState_, dacx_, genes, true, colorListeners_);
      geneCp_ = new ConsensusNodeProps(dacx.getGenome(), dacx.getLayout(), genes);
      tabPane.addTab(rMan.getString("multiSelProps.geneProp"), geneTab_.buildNodeTab(haveStatInstance, haveDynInstance, geneCp_, warnIcon_));
    }
    
    if (!nodes.isEmpty()) {
      nodeTab_ = new MultiNodeTab(appState_, dacx_, nodes, false, colorListeners_);
      nodeCp_ = new ConsensusNodeProps(dacx.getGenome(), dacx.getLayout(), nodes);
      tabPane.addTab(rMan.getString("multiSelProps.nodeProp"), nodeTab_.buildNodeTab(haveStatInstance, haveDynInstance, nodeCp_, warnIcon_));
    }
    
    if (!links.isEmpty()) {
      linkTab_ = new MultiLinkTab(appState_, dacx_, links, colorListeners_);
      linkCp_ = new ConsensusLinkProps(dacx.getGenome(), dacx.getLayout(), links);
      tabPane.addTab(rMan.getString("multiSelProps.linkProp"), linkTab_.buildLinkTab(haveStatInstance, haveDynInstance, linkCp_, warnIcon_));
    }
    
    UiUtil.gbcSet(gbc, 0, rownum, 11, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rownum += 8;
    cp.add(tabPane, gbc);
  
    //
    // Build the button panel:
    //

    DialogSupport ds = new DialogSupport(this, appState_, gbc);
    ds.buildAndInstallButtonBox(cp, rownum, 11, true, false); 
    setLocationRelativeTo(appState_.getTopFrame());
    displayProperties(geneCp_, nodeCp_, linkCp_);
  }
 
  
  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() {
    applyProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (applyProperties()) {
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
    setVisible(false);
    dispose();
    return;
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
  ** Apply the current node property values to our UI components
  ** 
  */
  
  private void displayProperties(ConsensusNodeProps geneCp, ConsensusNodeProps nodeCp, ConsensusLinkProps linkCp) {
    boolean haveDynInstance = (dacx_.getGenome() instanceof DynamicGenomeInstance);
    boolean haveStatInstance = (dacx_.getGenome() instanceof GenomeInstance) && !haveDynInstance;
   
    
    if (geneTab_ != null) {
      geneTab_.displayForTab(geneCp, haveStatInstance, haveDynInstance);
    }
    if (nodeTab_ != null) {
      nodeTab_.displayForTab(nodeCp, haveStatInstance, haveDynInstance);
    }
    if (linkTab_ != null) {
      linkTab_.displayForTab(linkCp, haveStatInstance, haveDynInstance);
    }  
    return;
  }

  /***************************************************************************
  **
  ** Apply our UI values to the node properties
  ** 
  */
  
  private boolean applyProperties() { 
   
    //
    // Have to error check all activity changes before making the changes:
    //
    
    if (geneTab_ != null) {
      if (!geneTab_.errorCheckForTab()) {
        return (false);
      } 
    }
    if (nodeTab_ != null) {
      if (!nodeTab_.errorCheckForTab()) {
        return (false);
      } 
    }
    if (linkTab_ != null) {
      if (!linkTab_.errorCheckForTab()) {
        return (false);
      } 
    }  
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.multiSelProps");   
    
    boolean doClose = false;
    boolean gotChange = false;
    
    if (geneTab_ != null) {
      gotChange = geneTab_.applyProperties(support);
      doClose |= gotChange;
    }
 
    if (nodeTab_ != null) {
      gotChange = nodeTab_.applyProperties(support);
      doClose |= gotChange;
    }
  
    if (linkTab_ != null) {
      gotChange = linkTab_.applyProperties(support);
      doClose |= gotChange;
    }    
     
    if (doClose) {
      support.finish();
    }
    return (true);
  }
}
