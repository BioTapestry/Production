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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogModelDisplay;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for choosing between existing options
*/

public class DrawNodeInstanceExistingOptionsDialogFactory extends DialogFactory {
 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DrawNodeInstanceExistingOptionsDialogFactory(ServerControlFlowHarness cfh) {
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
 
    List<ObjChoiceContent> occRoo = null;
    if (dniba.existingOptions.size() > 0) {   
      occRoo = buildCombo(dniba.rootGenome, dniba.existingOptions);
    }
        
    List<ObjChoiceContent> occIns = null;
    if (dniba.existingInstanceOptions.size() > 0) {
      occIns = buildCombo(dniba.tgi, dniba.existingInstanceOptions);
    }
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, occRoo, occIns, dniba.rootGenomeID, dniba.genomeInstanceID, dniba.rootLayoutID, dniba.instanceLayoutID));
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
  ** Build the combo of existing options
  ** 
  */
  
  private List<ObjChoiceContent> buildCombo(Genome genome, Set<String> existingOptions) {
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    if (genome instanceof GenomeInstance) {
      GenomeInstance rootVfg = ((GenomeInstance)genome).getVfgParentRoot();
      genome = (rootVfg == null) ? genome : rootVfg;
    }
    Iterator<String> nit = existingOptions.iterator();
    while (nit.hasNext()) {
      String nodeID = nit.next();
      Node node = genome.getNode(nodeID);
      String nodeMsg = node.getDisplayString(genome, true);
      retval.add(new ObjChoiceContent(nodeMsg, nodeID));
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
    Set<String> existingOptions;
    Set<String> existingInstanceOptions;
    
    String rootGenomeID; 
    String genomeInstanceID;
    String rootLayoutID;
    String instanceLayoutID;
          
    public BuildArgs(DataAccessContext dacx, Set<String> existingOptions, Set<String> existingInstanceOptions) {
      super(dacx.getCurrentGenomeAsInstance());
      this.rootGenome = dacx.getDBGenome();
      this.tgi = dacx.getCurrentGenomeAsInstance();
      this.existingOptions = existingOptions;
      this.existingInstanceOptions = existingInstanceOptions;
      
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
  
  
  public static class DesktopDialog extends JDialog implements DesktopDialogPlatform.Dialog {  
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JFrame parent_;  
    private JLabel existingLabel_;
    private JLabel existingInstanceLabel_;   
    private JComboBox existingCombo_;
    private JComboBox existingInstanceCombo_;
    private JRadioButton drawNew_;
    private JRadioButton drawOld_;
    private JRadioButton drawOldInstance_;
    private boolean haveOld_;
    private boolean haveOldInstance_;
    
    private String rootGenomeID_;
    private String genomeInstanceID_;
    private String rootLayoutID_;
    private String instanceLayoutID_;
        
    private DialogModelDisplay msp_;
    private ExistingDrawRequest request_;
    private ClientControlFlowHarness cfh_;
    private StaticDataAccessContext rcx_;
    private DataAccessContext dacx_;
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
    
    DesktopDialog(ServerControlFlowHarness cfh, List<ObjChoiceContent> occRoo, List<ObjChoiceContent> occIns, 
                  String rootGenomeID, String genomeInstanceID, 
                  String rootLayoutID, String instanceLayoutID) {

      super(cfh.getUI().getTopFrame(), cfh.getDataAccessContext().getRMan().getString("nicreateExisting.title"), true);
      uics_ = cfh.getUI();
      dacx_ = cfh.getDataAccessContext();
      parent_ = uics_.getTopFrame();
      ResourceManager rMan = dacx_.getRMan();

      setSize(600, 700);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      request_ = new ExistingDrawRequest();
      request_.haveResult = false;
      cfh_ = cfh.getClientHarness();
      
      rootGenomeID_ = rootGenomeID;
      genomeInstanceID_ = genomeInstanceID;
      rootLayoutID_ = rootLayoutID;
      instanceLayoutID_ = instanceLayoutID;      
             
      JLabel messageLabel = new JLabel(UiUtil.convertMessageToHtml(rMan.getString("nicreateExisting.existingMessage")), JLabel.CENTER);    
  
      UiUtil.gbcSet(gbc, 0, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
      cp.add(messageLabel, gbc);    
        
      haveOld_ = (occRoo != null);
      haveOldInstance_ = (occIns != null);
      
      drawNew_ = new JRadioButton(rMan.getString("nicreateExisting.drawNew"), true);
      ButtonGroup group = new ButtonGroup();
      group.add(drawNew_);
      ButtonTracker bt = new ButtonTracker();
      drawNew_.addActionListener(bt);
      UiUtil.gbcSet(gbc, 0, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(drawNew_, gbc);
      int currRow = 2;
     
      //
      // Build the existing node selection
      //        
      
      if (haveOld_) {
        String dol = rMan.getString("nicreateExisting.drawOld");
        String buttonLabel;
        if (!haveOldInstance_) {
          buttonLabel = MessageFormat.format(rMan.getString("nicreateExisting.recommendPattern"), new Object[] {dol});
        } else {
          buttonLabel = dol;
        }
        drawOld_ = new JRadioButton(buttonLabel, false);            
        group.add(drawOld_); 
        drawOld_.addActionListener(bt);
        UiUtil.gbcSet(gbc, 0, currRow++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(drawOld_, gbc);             
      
        existingLabel_ = new JLabel(rMan.getString("nicreateExisting.existing"));
        List<ObjChoiceContent> existing = occRoo;
        existingCombo_ = new JComboBox(existing.toArray());
        existingCombo_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try { 
              updateNodeDisplay(false, existingCombo_);
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
            return;
          }
        });
  
        UiUtil.gbcSet(gbc, 1, currRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
        cp.add(existingLabel_, gbc);
  
        UiUtil.gbcSet(gbc, 2, currRow++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(existingCombo_, gbc);
      }
      
      //
      // Build the existing instance node selection
      //
      
      if (haveOldInstance_) {
        String doi = rMan.getString("nicreateExisting.drawOldInstance");
        String buttonLabel = MessageFormat.format(rMan.getString("nicreateExisting.recommendPattern"), new Object[] {doi});
        drawOldInstance_ = new JRadioButton(buttonLabel, false);     
        group.add(drawOldInstance_);    
        drawOldInstance_.addActionListener(bt);
        UiUtil.gbcSet(gbc, 0, currRow++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(drawOldInstance_, gbc); 
        
        existingInstanceLabel_ = new JLabel(rMan.getString("nicreateExisting.existingInstance"));
        List<ObjChoiceContent> existing = occIns;
        existingInstanceCombo_ = new JComboBox(existing.toArray());
        existingInstanceCombo_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try { 
              updateNodeDisplay(false, existingInstanceCombo_);
            } catch (Exception ex) {
              uics_.getExceptionHandler().displayException(ex);
            }
            return;
          }
        });
  
        UiUtil.gbcSet(gbc, 1, currRow, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
        cp.add(existingInstanceLabel_, gbc);
  
        UiUtil.gbcSet(gbc, 2, currRow++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(existingInstanceCombo_, gbc);
      }
  
      // Note this is NOT legit, hitting the database from a dialog!
      rcx_ = new StaticDataAccessContext(dacx_, (String)null, (Layout)null);
            
      //
      // Build the position panel:
      //
  
      ModelViewPanelWithZoom mvpwz = new ModelViewPanelWithZoom(uics_, null, rcx_);
      msp_ = mvpwz.getModelView();
      UiUtil.gbcSet(gbc, 0, currRow, 4, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(mvpwz, gbc);
      currRow += 5;
  
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
      UiUtil.gbcSet(gbc, 0, currRow, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      setLocationRelativeTo(parent_);
      addWindowListener(new OpenListener());
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
          boolean isDrawOld = (drawOld_ == null) ? false : drawOld_.isSelected();
          boolean isDrawOldInstance = (drawOldInstance_ == null) ? false : drawOldInstance_.isSelected();
          setActiveFields(isDrawOld, isDrawOldInstance);   
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }
    
    /***************************************************************************
    **
    ** User input data
    ** 
    */
  
    private class OpenListener extends WindowAdapter {
      public void windowOpened(WindowEvent e) {
        setDefaultRadio();
        setActiveFields((drawOld_ != null) && drawOld_.isSelected(), (drawOldInstance_ != null) && drawOldInstance_.isSelected());
        return;
      }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE VIZ & PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
   /***************************************************************************
   **
   ** Set default choice
   ** 
   */    
    
    void setDefaultRadio() {       
      if (haveOldInstance_) {
        drawOldInstance_.setSelected(true);    
      } else if (haveOld_) {
        drawOld_.setSelected(true); 
      } else {
        drawNew_.setSelected(true);
      }
      return;
    }
    
   /***************************************************************************
    **
    ** Set what's active
    ** 
    */    
    
    void setActiveFields(boolean isDrawOld, boolean isDrawOldInstance) {    
      if (existingLabel_ != null) existingLabel_.setEnabled(isDrawOld);
      if (existingInstanceLabel_ != null) existingInstanceLabel_.setEnabled(isDrawOldInstance);
      if (existingCombo_ != null) existingCombo_.setEnabled(isDrawOld);
      if (existingInstanceCombo_ != null) existingInstanceCombo_.setEnabled(isDrawOldInstance);
      if (isDrawOld) {
        updateNodeDisplay(false, existingCombo_);
      } else if (isDrawOldInstance) {
        updateNodeDisplay(false, existingInstanceCombo_);
      } else {
        updateNodeDisplay(true, null);
      }
      return;
    }  
    
    /***************************************************************************
    **
    ** Update the node display
    ** 
    */
  
    private void updateNodeDisplay(boolean isDrawNew, JComboBox nodeCombo) { 
      
      if (isDrawNew) {
        rcx_.setGenome(null);
        rcx_.setLayout(null);
        msp_.repaint();
        return;
      }
      
      String nodeID = ((ObjChoiceContent)nodeCombo.getSelectedItem()).val;
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
        if (drawNew_.isSelected()) {
          request_.idResult = null;
          request_.doDraw = true;
          request_.haveResult = true;
          return (true);
        } else if ((drawOld_ != null) && drawOld_.isSelected()) {      
          request_.idResult = ((ObjChoiceContent)existingCombo_.getSelectedItem()).val;
          request_.doDraw = false;
          request_.haveResult = true;
          return (true);
        } else if ((drawOldInstance_ != null) && drawOldInstance_.isSelected()) {
          request_.idResult = ((ObjChoiceContent)existingInstanceCombo_.getSelectedItem()).val;
          request_.doDraw = false;
          request_.haveResult = true;
          return (true);
        }
      } else {
        request_.idResult = null;
        request_.doDraw = false;
        request_.haveResult = false;      
      }
      return (true);
    }
  }
 
  /***************************************************************************
   **
   ** User input data
   ** 
   */

   public static class ExistingDrawRequest implements ServerControlFlowHarness.UserInputs {
     private boolean haveResult; 
     public String idResult;
     public boolean doDraw;
     
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
