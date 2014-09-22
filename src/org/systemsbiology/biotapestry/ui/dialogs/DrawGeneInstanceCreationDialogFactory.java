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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.genome.GeneInstance;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogModelDisplay;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatLayoutFactory.RegionType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUICollectionElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement.XPlatUIElementType;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIEvent;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIPrimitiveElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for creating gene instances via drawing
*/

public class DrawGeneInstanceCreationDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DrawGeneInstanceCreationDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    BuildArgs dniba = (BuildArgs)ba;
    List<ObjChoiceContent> occ = buildCombo(dniba.rootGenome, dniba.tgi);
    
    String rootGenomeID = dniba.rootGenome.getID();
    String genomeInstanceID = dniba.tgi.getID();
    String rootLayoutID = appState.getLayoutMgr().getLayout(rootGenomeID);
    String instanceLayoutID = appState.getLayoutMgr().getLayout(genomeInstanceID);
    
    switch(platform.getPlatform()) {
    	case DESKTOP:
	      return (new DesktopDialog(cfh, dniba.defaultName, dniba.overlayKey, dniba.modKeys, dniba.tgi, occ, 
                                  rootGenomeID, genomeInstanceID, rootLayoutID, instanceLayoutID));    		
    	case WEB:
  	      return (new SerializableDialog(cfh, dniba.defaultName,occ,dniba.tgi,dniba.overlayKey,dniba.modKeys,
    		  rootGenomeID, genomeInstanceID, rootLayoutID, instanceLayoutID));
    	default:
    		throw new IllegalArgumentException("The platform is not supported: " + platform.getPlatform());
    }

    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  /***************************************************************************
  **
  ** Build the combo of available genes
  ** 
  */
  
  private List<ObjChoiceContent> buildCombo(DBGenome rootGenome, GenomeInstance tgi) {
    TreeMap<String, String> tm = new TreeMap<String, String>();
    HashMap<String, Map<String, String>> instanceMsgs = new HashMap<String, Map<String, String>>();
    
    //
    // Build up the first level (lex. ordered) from top model.  For lower level models: for each of these,
    // get the available instances in the root instance:
    //
    
    GenomeInstance rgi = tgi.getVfgParentRoot();
    boolean buildInstances = (rgi != null);
    Iterator<Gene> git = rootGenome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String geneMsg = gene.getDisplayString(rootGenome, false);
      String geneID = gene.getID();
      tm.put(geneMsg, geneID);
      if (buildInstances) {
        TreeMap<String, String> perGene = new TreeMap<String, String>();
        instanceMsgs.put(geneID, perGene);
        Iterator<String> rgit = rgi.getNodeInstances(geneID).iterator();
        while (rgit.hasNext()) {
          String rgeneID = rgit.next();
          if (tgi.getNode(rgeneID) != null) {
            continue;
          }
          GeneInstance rgene = (GeneInstance)rgi.getGene(rgeneID);
          String rgeneMsg = rgene.getDisplayStringGroupOnly(rgi);
          perGene.put(rgeneMsg, rgeneID);
        }
      }
    }
    
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    Iterator<String> tmkit = tm.keySet().iterator();
    while (tmkit.hasNext()) {
      String geneMsg = tmkit.next();
      String geneID = tm.get(geneMsg);
      retval.add(new ObjChoiceContent(geneMsg, geneID)); 
      if (buildInstances) {
        Map<String, String> perGene = instanceMsgs.get(geneID);
        Iterator<String> pgkit = perGene.keySet().iterator();
        while (pgkit.hasNext()) {
          String rgeneMsg = pgkit.next();
          String rgeneID = perGene.get(rgeneMsg);
          retval.add(new ObjChoiceContent("   " + rgeneMsg, rgeneID));
        }
      }        
    }
    
    return (retval);
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    DBGenome rootGenome; 
    GenomeInstance tgi;
    String defaultName;
    String overlayKey; 
    Set<String> modKeys;
          
    public BuildArgs(DBGenome rootGenome, GenomeInstance tgi, String defaultName, String overlayKey, Set<String> modKeys) {
      super(tgi);
      this.rootGenome = rootGenome;
      this.tgi = tgi;
      this.defaultName = defaultName;
      this.overlayKey = overlayKey;
      this.modKeys = modKeys;    
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  
  public static class DesktopDialog extends JDialog implements DesktopDialogPlatform.Dialog, ActionListener {
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JTextField nameField_;
    private JFrame parent_;
    private JComboBox importCombo_;
    private List<ObjChoiceContent> imports_;
    private JRadioButton drawNew_;
    private JRadioButton drawOld_;
    private JLabel nameLabel_;
    private JLabel importLabel_;
    private JCheckBox addToModule_;
    
    private String rootGenomeID_;
    private String genomeInstanceID_;
    private String rootInstanceID_;
    private String rootLayoutID_;
    private String instanceLayoutID_;
    private DataAccessContext rcx_;
    
    private DialogModelDisplay msp_;
    private ClientControlFlowHarness cfh_;
    private DrawRequest request_;
    private BTState appState_;
    
    private static final long serialVersionUID = 1L;
        
    ////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
    
    DesktopDialog(ServerControlFlowHarness cfh, String defaultName, String overlayKey, 
                  Set<String> modKeys, GenomeInstance tgi, List<ObjChoiceContent> occ, 
                  String rootGenomeID, String genomeInstanceID, 
                  String rootLayoutID, String instanceLayoutID) {
    
      super(cfh.getBTState().getTopFrame(), cfh.getBTState().getRMan().getString("gincreate.title"), true);
      appState_ = cfh.getBTState();
      parent_ = appState_.getTopFrame();
      ResourceManager rMan = appState_.getRMan();
      setSize(700, 500);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      request_ = new DrawRequest();
      request_.haveResult = false;
      request_.immediateAdd = false;
      cfh_ = cfh.getClientHarness();
      
      rootGenomeID_ = rootGenomeID;
      genomeInstanceID_ = genomeInstanceID;
      
      GenomeInstance rgi = tgi.getVfgParentRoot();
      this.rootInstanceID_ = (rgi != null) ? rgi.getID() : tgi.getID();
      
      rootLayoutID_ = rootLayoutID;
      instanceLayoutID_ = instanceLayoutID;
         
      JLabel label = new JLabel(rMan.getString("gincreate.chooseSource"));
  
      drawNew_ = new JRadioButton(rMan.getString("gincreate.drawNew"), true);
      drawOld_ = new JRadioButton(rMan.getString("gincreate.drawOld"), false);
            
      ButtonGroup group = new ButtonGroup();
      group.add(drawNew_);
      group.add(drawOld_);
      
      ButtonTracker bt = new ButtonTracker();
      
      drawNew_.addActionListener(bt);
      drawOld_.addActionListener(bt);
       
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      cp.add(label, gbc);
      
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(drawNew_, gbc);
      
      UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(drawOld_, gbc);             
      
      //
      // Build the name panel:
      //
  
      nameLabel_ = new JLabel(rMan.getString("gincreate.name"));
      nameField_ = new JTextField(defaultName);
      UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      cp.add(nameLabel_, gbc);
  
      UiUtil.gbcSet(gbc, 1, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(nameField_, gbc);
      nameField_.selectAll();
      
      addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          nameField_.requestFocus();
        }
      });
  
      //
      // Build the import panel:
      //
  
      importLabel_ = new JLabel(rMan.getString("gincreate.imports"));
      imports_ = occ;
      importCombo_ = new JComboBox(imports_.toArray());
      if (imports_.size() == 0) {
        drawOld_.setEnabled(false);
      }
         
      UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      cp.add(importLabel_, gbc);
      
      UiUtil.gbcSet(gbc, 1, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(importCombo_, gbc); 
      
      importCombo_.addActionListener(this); 
      
      //
      // Build overlay module inclusion option, if appropriate:
      //
      
      int rowNum = 3;
      if ((overlayKey != null) && (modKeys.size() > 0)) {
        addToModule_ = new JCheckBox(rMan.getString("gincreate.addToModule"));
        addToModule_.setSelected(true);
        UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
        cp.add(addToModule_, gbc);
      }
     
      //
      // Build the position panel:
      //
  
      // Note this is NOT legit, hitting the database from a dialog!
      Database db = appState_.getDB();
      rcx_ = new DataAccessContext(null, null, false, false, 0.0,
                                  appState_.getFontMgr(), appState_.getDisplayOptMgr(), 
                                  appState_.getFontRenderContext(), db, db, false, 
                                  new FullGenomeHierarchyOracle(db, db), appState_.getRMan(), 
                                  new LocalGroupSettingSource(), db, db,
                                  new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), db, db
      );
      
      ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(appState_, null, rcx_);
      msp_ = mvpwz.getModelView();
   
      UiUtil.gbcSet(gbc, 0, rowNum, 4, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(mvpwz, gbc);
      rowNum += 5;
      
      //
      // Build the button panel:
      //
    
      FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
      buttonO.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (stashResults(true)) {
              cfh_.sendUserInputs(request_);
              DesktopDialog.this.setVisible(false);
              DesktopDialog.this.dispose();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });     
      FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
      buttonC.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stashResults(false);
            cfh_.sendUserInputs(request_);
            DesktopDialog.this.setVisible(false);
            DesktopDialog.this.dispose();
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
      UiUtil.gbcSet(gbc, 0, rowNum, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      setLocationRelativeTo(parent_);
      setActiveFields(true);
    }
  
    /***************************************************************************
    **
    ** Track node selections
    ** 
    */
  
    public void actionPerformed(ActionEvent e) {
      try { 
        updateNodeDisplay(false);
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }      
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    //////////////////////////////////////////////////////////////////////////// 
    
    private class ButtonTracker implements ActionListener {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean isDrawNew = drawNew_.isSelected();
          setActiveFields(isDrawNew);
          updateNodeDisplay(isDrawNew);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE VIZ & PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Enable/disable controls
    ** 
    */  
    
    void setActiveFields(boolean isDrawNew) {
      nameLabel_.setEnabled(isDrawNew);
      importLabel_.setEnabled(!isDrawNew);       
      nameField_.setEnabled(isDrawNew);
      importCombo_.setEnabled(!isDrawNew);
      return;
    }
    
    /***************************************************************************
    **
    ** Update the node display
    ** 
    */
  
    private void updateNodeDisplay(boolean isDrawNew) { 
      
      if (isDrawNew) {
        rcx_.setGenome(null);
        rcx_.setLayout(null);
        msp_.repaint();
        return;
      }
  
      String nodeID = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
      boolean isRoot = GenomeItemInstance.isBaseID(nodeID);
      String genomeID = (isRoot) ? rootGenomeID_ : rootInstanceID_;
      String loKey = (isRoot) ? rootLayoutID_ : instanceLayoutID_;
      rcx_.setGenome(rcx_.getGenomeSource().getGenome(genomeID));
      rcx_.setLayout(rcx_.lSrc.getLayout(loKey));
  
      HashSet<String> nodes = new HashSet<String>();
      nodes.add(nodeID);
      msp_.selectNodes(nodes);
      msp_.repaint();
      return;
    }  
   
    /***************************************************************************
    **
    ** Stash our results for later interrogation
    ** 
    */
    
    private boolean stashResults(boolean ok) {
      if (ok) {
        ResourceManager rMan = appState_.getRMan();
        request_.idResult = null;
        if (drawNew_.isSelected()) {
          request_.nameResult = nameField_.getText().trim();
          if (request_.nameResult.equals("")) {
            String message = rMan.getString("addGene.EmptyName");
            String title = rMan.getString("addGene.CreationErrorTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
            cfh_.showSimpleUserFeedback(suf);
            return (false);
          }
          //
          // If the name matches an existing gene, we report it as being an import.
          // If it does not, we need to insure it does not conflict with another
          // node name
          //
          int numImp = imports_.size();
          for (int i = 0; i < numImp; i++) {
            ObjChoiceContent occ = imports_.get(i);
            if (DataUtil.keysEqual(occ.name, request_.nameResult)) {
              request_.nameResult = null;  // Make it like a existing gene...
              request_.idResult = occ.val;
              String message = rMan.getString("drawNewGene.notDraw");
              String title = rMan.getString("drawNewGene.notDrawTitle");
              SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
              cfh_.showSimpleUserFeedback(suf);
              break;
            }
          }
          //
          // If we have a null result, then we need to insure that we do not match any
          // other gene or node name (globally):
          //      
          if (request_.idResult == null) {
            if (!queBombForName(request_.nameResult)) {
              return (false);
            }
          }
     
          //
          // At this point, we may be into an existing gene if there was a name match.  If
          // not, we are done:
          //
  
          if (request_.idResult == null) {
            request_.haveResult = true;
            request_.doModuleAdd = (addToModule_ != null) ? addToModule_.isSelected() : false;
            return (true);
          }
          
        } else { // user chose existing gene from the start:
          request_.idResult = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
          request_.nameResult = null;                
        }
        
        boolean retval = handleExistingNode(request_.idResult);
        request_.doModuleAdd = (retval && (addToModule_ != null)) ? addToModule_.isSelected() : false;
        return (retval);
  
      } else {
        request_.nameResult = null;
        request_.idResult = null;
        request_.doModuleAdd = false;
        request_.haveResult = false;
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
    
    private boolean queBombForName(String nameResult) {
      RemoteRequest daBomb = new RemoteRequest("queBombNameMatch");
      daBomb.setStringArg("nameResult", nameResult); 
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      SimpleUserFeedback suf = dbres.getSimpleUserFeedback();
      if (suf != null) {
        cfh_.showSimpleUserFeedback(suf);
      }
      if (dbres.keepGoing() == RemoteRequest.Progress.STOP) {
        return (false);
      } else {
        return (true);
      }
    }
    
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
    
    private boolean handleExistingNode(String idResult) {
      request_.idResult = idResult;
      request_.haveResult = false;
      request_.immediateAdd = false;  
     
      String[] queBombs = {"queBombCheckInstanceToSubset",
                           "queBombCheckTargetGroups",
                           "topOnSubset"};
      
      int count = 0;
      RemoteRequest daBomb = new RemoteRequest(queBombs[count++]);
      daBomb.setBooleanArg("haveResult", request_.haveResult);
      daBomb.setBooleanArg("immediateAdd", request_.immediateAdd);
      daBomb.setStringArg("idResult", request_.idResult); 
        
      while (count <= queBombs.length) {
        RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
        SimpleUserFeedback suf = dbres.getSimpleUserFeedback();
        if (suf != null) {
          cfh_.showSimpleUserFeedback(suf);
        }
        request_.haveResult = dbres.getBooleanAnswer("haveResult");
        request_.immediateAdd = dbres.getBooleanAnswer("immediateAdd");
        request_.idResult = dbres.getStringAnswer("idResult");
        if (dbres.keepGoing() == RemoteRequest.Progress.DONE) { 
          return (true);    
        } else if (dbres.keepGoing() == RemoteRequest.Progress.STOP) {
          return (false);
        }
        // I.e. we require exit before we hit the end!
        if (count >= queBombs.length) {
          break;
        }
        daBomb = new RemoteRequest(queBombs[count++], daBomb);
      }
      throw new IllegalStateException();
    }
  }
  
  
  
  /**
   * Wrapper for an XPlat implementation of of a dialog.
   * 
   * 
   * 
   * 
   * 
   * 
   *
   */
  public static class SerializableDialog implements SerializableDialogPlatform.Dialog {
	  private XPlatUIDialog xplatDialog_;
	  private ServerControlFlowHarness scfh_;
	  private List<ObjChoiceContent> occ_;
	  private XPlatPrimitiveElementFactory primElemFac_; 
	  private ResourceManager rMan_;
	  private String defaultGeneName_;
	  private Set<String> modKeys_;
	  private String overlayKey_;
	  private String rootGenomeID_;
	  private String rootInstanceID_;
	  private String genomeInstanceID_;
	  private String rootLayoutID_;
	  private String instanceLayoutID_;
	  
	  public SerializableDialog(
		  ServerControlFlowHarness cfh, 
		  String defaultGeneName,
		  List<ObjChoiceContent> occ,
		  GenomeInstance tgi,
		  String overlayKey,
		  Set<String> modKeys,
		  String rootGenomeID, 
		  String genomeInstanceID, 
		  String rootLayoutID, 
		  String instanceLayoutID
      ){
		  this.scfh_ = cfh;
		  this.occ_ = occ;
		  this.rMan_ = scfh_.getBTState().getRMan();
		  this.primElemFac_ = new XPlatPrimitiveElementFactory(rMan_);
		  this.defaultGeneName_ = defaultGeneName;
		  this.overlayKey_ = overlayKey;
		  this.modKeys_ = modKeys;
		  this.rootGenomeID_ = rootGenomeID;
		  this.genomeInstanceID_ = genomeInstanceID;
		  this.rootLayoutID_ = rootLayoutID;
		  this.instanceLayoutID_ = instanceLayoutID;	
		  
	      GenomeInstance rgi = tgi.getVfgParentRoot();
	      this.rootInstanceID_ = (rgi != null) ? rgi.getID() : tgi.getID();
	  }
	  
	  public boolean isModal() {
      return (true);
    }

	  private void buildDialog(String title, String defaultGeneName,int height,int width,FlowKey okayKey) {
		  Map<String,String> clickActions = new HashMap<String,String>();		  
		  clickActions.put("cancel", FlowMeister.MainFlow.CANCEL_ADD_MODE.toString());
		  clickActions.put("ok", okayKey.toString());
		  
		  this.xplatDialog_ = new XPlatUIDialog(title,height,width,"");
		  
		  this.xplatDialog_.setParameter("id", defaultGeneName.replaceAll("\\s+", "_").toLowerCase());
		  
		  // Set up the main dialog layout
		  
		  XPlatUICollectionElement layoutCollection = this.xplatDialog_.createCollection("main", XPlatUIElementType.LAYOUT_CONTAINER);
		  layoutCollection.setParameter("style", "height: " + height + "px; width: " + width + "px;");
		  layoutCollection.setParameter("gutters", "false");
		  
		  this.xplatDialog_.createCollectionList("main", "center");
		  this.xplatDialog_.createCollectionList("main", "top");
		  this.xplatDialog_.createCollectionList("main", "bottom");
		  
		  XPlatUICollectionElement rightJustifiedBtnsContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  rightJustifiedBtnsContainer.setParameter("id", this.primElemFac_.generateId("rightJustifiedBtns", XPlatUIElementType.LAYOUT_CONTAINER));
		  rightJustifiedBtnsContainer.setParameter("style", "height: 50px");
		  rightJustifiedBtnsContainer.createList("center");
		  rightJustifiedBtnsContainer.createList("left");
		  rightJustifiedBtnsContainer.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.BOTTOM, 2));
		  this.xplatDialog_.addElementToCollection("main","bottom",rightJustifiedBtnsContainer);
		  
		  XPlatUICollectionElement drawingAreaContainer = new XPlatUICollectionElement(XPlatUIElementType.LAYOUT_CONTAINER);
		  drawingAreaContainer.setParameter("id", this.primElemFac_.generateId("drawingAreaContainer", XPlatUIElementType.LAYOUT_CONTAINER));
		  drawingAreaContainer.createList("center");
		  drawingAreaContainer.createList("bottom");
		  drawingAreaContainer.setLayout(XPlatLayoutFactory.makeRegionalLayout(RegionType.CENTER, 2));
		  this.xplatDialog_.addElementToCollection("main","center",drawingAreaContainer);
		  
		  // Fill in the 'top' region
		  XPlatUIPrimitiveElement drawWhichSelexGroup = this.primElemFac_.makeSelectionGroup(
			  "drawWhich", "drawNew", null, false, this.rMan_.getString("gincreate.chooseSource"),null
		  );
		  
		  drawWhichSelexGroup.addAvailVal("drawNew", this.rMan_.getString("gincreate.drawNew"));
		  drawWhichSelexGroup.addAvailVal("drawOld", this.rMan_.getString("gincreate.drawOld"));
		  
		  Map<String,Object> onChangeParams = new HashMap<String,Object>();
		  onChangeParams.put("conditionValueLoc", "ELEMENT_VALUES");
		  
		  drawWhichSelexGroup.setEvent("change", new XPlatUIEvent("change","CLIENT_SET_ELEMENT_CONDITION",onChangeParams));
		  
		  XPlatUIPrimitiveElement chooseOldGene = this.primElemFac_.makeTxtComboBox(
			  "chooseOldGene",occ_.get(0).val,null,true,this.rMan_.getString("gincreate.imports"),null
		  );

		  chooseOldGene.setParameter("class", "PreserveWhitespace");
		  chooseOldGene.setParameter("formattedValues", true);
		  	  
		  for(ObjChoiceContent occ : occ_) {
		      Map<String,String> vals = new HashMap<String,String>();
		      vals.put("label",occ.name);
		      vals.put("modelId",((GenomeItemInstance.isBaseID(occ.val)) ? this.rootGenomeID_ : this.rootInstanceID_));
			  chooseOldGene.addAvailVal(occ.val,vals);
		  }
		  
		  chooseOldGene.setEvent("change", new XPlatUIEvent("change","CLIENT_SELECT_AND_ZOOM"));
		  
		  XPlatUIPrimitiveElement newGeneName = this.primElemFac_.makeTextBox("gene_name", false, defaultGeneName, null, true,this.rMan_.getString("gincreate.name"),false);
		  		  
		  chooseOldGene.setValidity("drawOld", "true");
		  chooseOldGene.setParameter("bundleAs", "idResult");
		  
		  newGeneName.setValidity("drawNew","true");		  
		  newGeneName.setParameter("bundleAs","nameResult");
		  
		  this.xplatDialog_.addElementToCollection("main","top",drawWhichSelexGroup);
		  this.xplatDialog_.addElementToCollection("main","top",newGeneName);
		  this.xplatDialog_.addElementToCollection("main","top",chooseOldGene);	
		  
		  // Fill in the 'center' region
		  
		  XPlatUIPrimitiveElement modelDrawingArea = this.primElemFac_.makeDrawingArea(300,300);
		  modelDrawingArea.setValidity("drawOld", "true");
		  modelDrawingArea.setParameter("drawWorkspace", "true");
		  		    
		  XPlatUIPrimitiveElement zoomIn = this.primElemFac_.makeBasicButton(rMan_.getString("mvpwz.zoomIn"), null, "MAIN_ZOOM_IN", null);
		  XPlatUIPrimitiveElement zoomOut = this.primElemFac_.makeBasicButton(rMan_.getString("mvpwz.zoomOut"), null, "MAIN_ZOOM_OUT", null);
		  XPlatUIPrimitiveElement zoomToSelected = this.primElemFac_.makeBasicButton(rMan_.getString("mvpwz.zoomToSelected"), null, "MAIN_ZOOM_TO_ALL_SELECTED", null);
		  
		  zoomIn.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  zoomOut.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  zoomToSelected.getEvent("click").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  chooseOldGene.getEvent("change").addParameter("drawingAreaId", modelDrawingArea.getParameter("id"));
		  
		  zoomIn.setValidity("drawOld", "true");
		  zoomOut.setValidity("drawOld", "true");
		  zoomToSelected.setValidity("drawOld", "true");

		  drawingAreaContainer.addElement("center", modelDrawingArea);
		  drawingAreaContainer.addElement("bottom", zoomIn);
		  drawingAreaContainer.addElement("bottom", zoomOut);
		  drawingAreaContainer.addElement("bottom", zoomToSelected);		  
		  
		  // Fill in the 'bottom' region
		  XPlatUIPrimitiveElement emptyPane = new XPlatUIPrimitiveElement(XPlatUIElementType.PANE);
		  // TODO: This is a hardcorded size, and probably needs to be relative to the count of buttons
		  // eg. emptyPaneWidth = dialogWidth - (buttonCount * 50) - right-side-padding 
		  emptyPane.setParameter("style", "width: 515px;");
		  rightJustifiedBtnsContainer.addElement("left", emptyPane);
		  rightJustifiedBtnsContainer.addElements("center", this.primElemFac_.makeOkCancelButtons(clickActions.get("ok"),clickActions.get("cancel"),false));
		  		  
		  this.xplatDialog_.setCancel(clickActions.get("cancel"));
		  this.xplatDialog_.addDefaultState_("drawNew", "true");
		  this.xplatDialog_.addDefaultState_("drawOld", "false");
		  
		  this.xplatDialog_.setUserInputs(new DrawRequest(true));
		  
	      if ((this.overlayKey_ != null) && (this.modKeys_.size() > 0)) {
	    	  XPlatUIElement addToModule = this.primElemFac_.makeCheckbox(this.rMan_.getString("gincreate.addToModule"), null, "addToMod",true);
	    	  addToModule.setParameter("bundleAs", "doModuleAdd");
	    	  this.xplatDialog_.addElementToCollection("main", "top", addToModule);
	      }
		  
	  }

	  public XPlatUIDialog getDialog() {
		  buildDialog("Draw a Gene",this.defaultGeneName_,500,700,null);		  
		  return xplatDialog_;
	  }	  
	  
	  
	  public XPlatUIDialog getDialog(FlowKey okayKey) {
		  buildDialog("Draw a Gene",this.defaultGeneName_,500,700,okayKey);		  
		  return xplatDialog_;
	  }
	  
	  /***************************************************************************
	   **
	   ** Return the parameters we are interested in:
	   */
	    
	  public Set<String> getRequiredParameters() {
		  HashSet<String> retval = new HashSet<String>();
	      retval.add("gene");
	      retval.add("drawWhich");
	      if((this.overlayKey_ != null) && (this.modKeys_.size() > 0)) {
	    	  retval.add("addToModule");
	      }
	      return (retval);
	  }   
	      
	  /***************************************************************************
	   **
	   ** Talk to the expert!
	   ** 
	   */	  
	    public SimpleUserFeedback checkForErrors(UserInputs ui) {
	    	if(((DrawRequest) ui).nameResult != null) {
		    	RemoteRequest daBomb = new RemoteRequest("queBombNameMatchForGeneCreate");
		    	daBomb.setStringArg("nameResult", ((DrawRequest)ui).nameResult); 
		    	RemoteRequest.Result dbres = scfh_.receiveRemoteRequest(daBomb);
		    	return (dbres.getSimpleUserFeedback());	    		
	    	}
	    	return null;

	    }
	  
	  
	 
	  /***************************************************************************
	   **
	   ** Do the bundle 
	   */ 
	  public ServerControlFlowHarness.UserInputs bundleForExit(Map<String, String> params) {
		  return null;
	  }
  }  // SerializableDialog
  
  /***************************************************************************
   **
   ** User input data
   ** 
   */

   public static class DrawRequest implements ServerControlFlowHarness.UserInputs {
     public String nameResult;
     public String idResult;
     
     private boolean haveResult;
     public HashSet<String> idOptions;
     public boolean immediateAdd;
     public boolean doModuleAdd;
     
   public DrawRequest() {
	   this.idResult = null;
	   this.nameResult = null;
	   this.haveResult = true;
	   this.doModuleAdd = false;
   }

   public DrawRequest(boolean forTransit) {
	   this.idResult = "";
	   this.nameResult = "";
	   this.haveResult = true;
	   this.doModuleAdd = false;
   }
   
     public void clearHaveResults() {
       haveResult = false;
       return;
     }   
     public boolean haveResults() {
       return (haveResult);
     }  
     public boolean isForApply() {
       return (false);
     } 
   }  
}
