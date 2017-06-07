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
import java.util.Vector;

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

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogModelDisplay;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for creating non-gene nodes
*/

public class DrawNodeInstanceCreationDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DrawNodeInstanceCreationDialogFactory(ServerControlFlowHarness cfh) {
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
    
    HashMap<String, String>toName = new HashMap<String, String>();
    HashMap<String, Integer>toType = new HashMap<String, Integer>();
    
    buildImportMaps(dniba.rootGenome, occ, toName, toType);
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.defaultName, dniba.overlayKey, dniba.modKeys, occ, 
                                dniba.rootGenomeID, dniba.genomeInstanceID, 
                                dniba.rootLayoutID, dniba.instanceLayoutID, toName, toType));
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  **
  ** Build the combo of available nodes
  ** 
  */
   
  private void buildImportMaps(DBGenome rootGenome, List<ObjChoiceContent> occl, Map<String, String> toName, Map<String, Integer> toType) {  
    int numImp = occl.size();
    for (int i = 0; i < numImp; i++) {
      ObjChoiceContent occ = occl.get(i);
      Node node = rootGenome.getNode(occ.val);
      if (node == null) {  // Should not happen with root?
        continue;
      }
      String name = node.getName();
      int type = node.getNodeType();
      toName.put(occ.val, name);
      toType.put(occ.val, new Integer(type));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Build the combo of available nodes
  ** 
  */
  
  private List<ObjChoiceContent> buildCombo(DBGenome rootGenome, GenomeInstance tgi) {
    TreeMap<String, List<String>> tm = new TreeMap<String, List<String>>();
    HashMap<String, Map<String, String>> instanceMsgs = new HashMap<String, Map<String, String>>();
    
    //
    // Build up the first level (lex. ordered) from top model.  For lower level models: for each of these,
    // get the available instances in the root instance:
    //
    
    GenomeInstance rgi = tgi.getVfgParentRoot();
    boolean buildInstances = (rgi != null);
    Iterator<Node> nit = rootGenome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeMsg = node.getDisplayString(rootGenome, false);
      String nodeID = node.getID();
      List<String> perMsg = tm.get(nodeMsg);
      if (perMsg == null) {
        perMsg = new ArrayList<String>();
        tm.put(nodeMsg, perMsg);
      }
      perMsg.add(nodeID);
      if (buildInstances) {
        TreeMap<String, String> perNode = new TreeMap<String, String>();
        instanceMsgs.put(nodeID, perNode);
        Iterator<String> rnit = rgi.getNodeInstances(nodeID).iterator();
        while (rnit.hasNext()) {
          String rnodeID = rnit.next();
          if (tgi.getNode(rnodeID) != null) {
            continue;
          }
          NodeInstance rnode = (NodeInstance)rgi.getNode(rnodeID);
          String rnodeMsg = rnode.getDisplayStringGroupOnly(rgi);        
          perNode.put(rnodeMsg, rnodeID);
        }
      }
    }
    
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    Iterator<String> tmkit = tm.keySet().iterator();
    while (tmkit.hasNext()) {
      String nodeMsg = tmkit.next();
      List<String> perMsg = tm.get(nodeMsg);
      int pmNum = perMsg.size();
      for (int i = 0; i < pmNum; i++) {
        String nodeID = perMsg.get(i);
        retval.add(new ObjChoiceContent(nodeMsg, nodeID)); 
        if (buildInstances) {
          Map<String, String> perNode = instanceMsgs.get(nodeID);
          Iterator<String> pnkit = perNode.keySet().iterator();
          while (pnkit.hasNext()) {
            String rnodeMsg = pnkit.next();
            String rnodeID = perNode.get(rnodeMsg);
            retval.add(new ObjChoiceContent("   " + rnodeMsg, rnodeID));
          }
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
    String rootGenomeID; 
    String genomeInstanceID;
    String rootLayoutID;
    String instanceLayoutID;
 
    public BuildArgs(DataAccessContext dacx, String overlayKey, Set<String> modKeys) {
      super(dacx.getCurrentGenomeAsInstance());
      this.rootGenome = dacx.getDBGenome();
      this.tgi = dacx.getCurrentGenomeAsInstance();
      this.defaultName = rootGenome.getUniqueNodeName();
      this.overlayKey = overlayKey;
      this.modKeys = modKeys;  
      
      this.rootGenomeID = rootGenome.getID();
      this.genomeInstanceID = tgi.getID();
      this.rootLayoutID = dacx.getLayoutSource().mapGenomeKeyToLayoutKey(rootGenomeID);
      this.instanceLayoutID = dacx.getLayoutSource().mapGenomeKeyToLayoutKey(genomeInstanceID);
   
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
    private JComboBox typeCombo_;
    private JComboBox importCombo_;
    private List<ObjChoiceContent> imports_; 
    private JFrame parent_;
    private JRadioButton drawNew_;
    private JRadioButton drawOld_;
    private JLabel typeLabel_;
    private JLabel nameLabel_;
    private JLabel importLabel_;
    private JCheckBox addToModule_;
    
    private String rootGenomeID_;
    private String genomeInstanceID_;
    private String rootLayoutID_;
    private String instanceLayoutID_;
    
    private DialogModelDisplay msp_;
    private ClientControlFlowHarness cfh_;
    private DrawRequest request_;
    
    private Map<String, String> toName_;
    private Map<String, Integer> toType_;
    private DataAccessContext dacx_;
    private StaticDataAccessContext rcx_;
    private UIComponentSource uics_;
      
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
    
    DesktopDialog(ServerControlFlowHarness cfh, String defaultName, String overlayKey, 
                  Set<String> modKeys, List<ObjChoiceContent> occ, 
                  String rootGenomeID, String genomeInstanceID, 
                  String rootLayoutID, String instanceLayoutID, 
                  Map<String, String> toName, Map<String, Integer> toType) {
      
      super(cfh.getUI().getTopFrame(), cfh.getDataAccessContext().getRMan().getString("nicreate.title"), true);
      uics_ = cfh.getUI();
      dacx_ = cfh.getDataAccessContext();
      parent_ = uics_.getTopFrame();
      ResourceManager rMan = dacx_.getRMan();
      setSize(800, 500);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      request_ = new DrawRequest();
      request_.haveResult = false;
      request_.typeResult = Node.NO_NODE_TYPE;
      request_.immediateAdd = false;
      cfh_ = cfh.getClientHarness();
      toName_ = toName;
      toType_ = toType;
      
      rootGenomeID_ = rootGenomeID;
      genomeInstanceID_ = genomeInstanceID;
      rootLayoutID_ = rootLayoutID;
      instanceLayoutID_ = instanceLayoutID;
         
      JLabel label = new JLabel(rMan.getString("nicreate.chooseSource"));
  
      drawNew_ = new JRadioButton(rMan.getString("nicreate.drawNew"), true);
      drawOld_ = new JRadioButton(rMan.getString("nicreate.drawOld"), false);
            
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
      // Build the node type selection
      //
      
      typeLabel_ = new JLabel(rMan.getString("nicreate.type"));
      Vector<ChoiceContent> choices = DBNode.getTypeChoices(rMan, false);
      typeCombo_ = new JComboBox(choices);
  
      UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      cp.add(typeLabel_, gbc);
      
      UiUtil.gbcSet(gbc, 1, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(typeCombo_, gbc);
          
      //
      // Build the name panel:
      //
  
      nameLabel_ = new JLabel(rMan.getString("nicreate.name"));
      nameField_ = new JTextField(defaultName);
      UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
      cp.add(nameLabel_, gbc);
  
      UiUtil.gbcSet(gbc, 1, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(nameField_, gbc);
      
      nameField_.selectAll();
      
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
          nameField_.requestFocus();
        }
      });    
      
      //
      // Build the import panel:
      //
  
      importLabel_ = new JLabel(rMan.getString("nicreate.imports"));
      imports_ = occ;
      importCombo_ = new JComboBox(imports_.toArray());
      if (imports_.size() == 0) {
        drawOld_.setEnabled(false);
      }    
      
      
      UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
      cp.add(importLabel_, gbc);
      
      UiUtil.gbcSet(gbc, 1, 3, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(importCombo_, gbc); 
      
      importCombo_.addActionListener(this);    
      
      //
      // Build overlay module inclusion option, if appropriate:
      //
      
      int rowNum = 4;
      if ((overlayKey != null) && (modKeys.size() > 0)) {
        addToModule_ = new JCheckBox(rMan.getString("gincreate.addToModule"));
        addToModule_.setSelected(true);
        UiUtil.gbcSet(gbc, 0, rowNum++, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
        cp.add(addToModule_, gbc);
      }    
  
      // Note this is NOT legit, hitting the database from a dialog!
      rcx_ = new StaticDataAccessContext(dacx_, (String)null, (Layout)null);
            
      //
      // Build the position panel:
      //
  
      ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(uics_, null, rcx_);
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
            uics_.getExceptionHandler().displayException(ex);
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
            uics_.getExceptionHandler().displayException(ex);
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
        uics_.getExceptionHandler().displayException(ex);
      }
      return;
    }    
    
    public boolean dialogIsModal() {
      return (true);
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
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE VIZ & PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    void setActiveFields(boolean isDrawNew) {
      typeLabel_.setEnabled(isDrawNew);
      nameLabel_.setEnabled(isDrawNew);
      importLabel_.setEnabled(!isDrawNew);       
      nameField_.setEnabled(isDrawNew);
      typeCombo_.setEnabled(isDrawNew);
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
      String genomeID = (isRoot) ? rootGenomeID_ : genomeInstanceID_;    
      String loKey = (isRoot) ? rootLayoutID_ : instanceLayoutID_;    
      rcx_.setGenome(rcx_.getGenomeSource().getGenome(genomeID));
      rcx_.setLayout(rcx_.getLayoutSource().getLayout(loKey));
  
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
        request_.idOptions = null;
        if (drawNew_.isSelected()) {
          request_.nameResult = nameField_.getText().trim();
          //
          // Even though we do not restrict non-gene node names, they cannot share
          // names with genes.
          // 
          if (!queBombForName(request_.nameResult)) {
            return (false);
          }
          request_.typeResult = ((ChoiceContent)typeCombo_.getSelectedItem()).val;
          request_.idResult = null;        
          //
          // If the name matches an existing node with the same type, it _may_ be 
          // an import depending on where we try to place it.  In a region where it does 
          // not exist, it can be an import, and which it is depends on where the user
          // ultimately tries to place it.
          //
          int numImp = imports_.size();
          for (int i = 0; i < numImp; i++) {
            ObjChoiceContent occ = imports_.get(i);
            String name = toName_.get(occ.val);
            if (name == null) {
              continue;
            }
            if (DataUtil.keysEqual(name, request_.nameResult)) {
              int type = toType_.get(occ.val).intValue();
              if (type == request_.typeResult) {
                if (request_.idOptions == null) {
                  request_.idOptions = new HashSet<String>();
                }
                request_.idOptions.add(occ.val);
              }
            }
          }
          
          //
          // At this point, we are done, since we don't have enough info
          // to get a single idResult:
          //
  
          request_.haveResult = true;
          request_.doModuleAdd = (addToModule_ != null) ? addToModule_.isSelected() : false;
          return (true);        
   
        } 
        
        String idResult = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
        request_.typeResult = Node.NO_NODE_TYPE;
        request_.nameResult = null;                     
        boolean retval = handleExistingNode(idResult);
        request_.doModuleAdd = (retval && (addToModule_ != null)) ? addToModule_.isSelected() : false;
        return (retval);
      } else {
        request_.nameResult = null;
        request_.typeResult = Node.NO_NODE_TYPE;
        request_.idResult = null;
        request_.doModuleAdd = false;
        request_.idOptions = null;
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
  
  /***************************************************************************
   **
   ** User input data
   ** 
   */

   public static class DrawRequest implements ServerControlFlowHarness.UserInputs {
     public String nameResult;
     private boolean haveResult;
     public int typeResult;
     public String idResult;
     public HashSet<String> idOptions;
     public boolean immediateAdd;
     public boolean doModuleAdd;
     
     public void clearHaveResults() {
       haveResult = false;
       return;
     }   
     public boolean haveResults() {
       return (haveResult);
     }  
 	public void setHasResults() {
		this.haveResult = true;
		return;
	}  
     
     public boolean isForApply() {
       return (false);
     } 
   }  
}
