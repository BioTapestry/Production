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
import java.util.Iterator;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.ui.Intersection;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for choosing between existing options
*/

public class DrawLinkInstanceExistingOptionsDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private boolean haveResult_;  
  private String idResult_;
  private boolean doDraw_;
  private Genome rootGenome_;
  private GenomeInstance tgi_;
  private JLabel existingLabel_;
  private JLabel existingInstanceLabel_;   
  private JComboBox existingCombo_;
  private JComboBox existingInstanceCombo_;
  private JRadioButton drawNew_;
  private JRadioButton drawOld_;
  private JRadioButton drawOldInstance_;
  private boolean haveOld_;
  private boolean haveOldInstance_;
  private ModelViewPanel msp_;
  private StaticDataAccessContext rcx_; // for local display
  private StaticDataAccessContext dacx_; // the real thing
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
  
  public DrawLinkInstanceExistingOptionsDialog(UIComponentSource uics, StaticDataAccessContext dacx, 
                                               Genome rootGenome, 
                                               GenomeInstance tgi, 
                                               Set<String> existingOptions, 
                                               Set<String> existingInstanceOptions) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("licreateExisting.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    ResourceManager rMan = dacx_.getRMan();    
    setSize(700, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
    haveResult_ = false;
    tgi_ = tgi;
    rootGenome_ = rootGenome;
    addWindowListener(new DialogOpenListener());
           
    JLabel messageLabel = new JLabel(UiUtil.convertMessageToHtml(rMan.getString("licreateExisting.existingMessage")));    

    UiUtil.gbcSet(gbc, 0, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);       
    cp.add(messageLabel, gbc);    
      
    haveOld_ = existingOptions.size() > 0;
    haveOldInstance_ = existingInstanceOptions.size() > 0;
    
    boolean activeNew = !(haveOld_ || haveOldInstance_);
    drawNew_ = new JRadioButton(rMan.getString("licreateExisting.drawNew"), activeNew);
    ButtonGroup group = new ButtonGroup();
    group.add(drawNew_);
    ButtonTracker bt = new ButtonTracker();
    drawNew_.addActionListener(bt);
    UiUtil.gbcSet(gbc, 0, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(drawNew_, gbc);
    int currRow = 2;
   
    //
    // Build the existing link selection
    //        
    
    boolean activeOld = (haveOld_ && !haveOldInstance_);
    if (haveOld_) {     
      drawOld_ = new JRadioButton(rMan.getString("licreateExisting.drawOld"), activeOld);            
      group.add(drawOld_); 
      drawOld_.addActionListener(bt);
      UiUtil.gbcSet(gbc, 0, currRow++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(drawOld_, gbc);             
    
      existingLabel_ = new JLabel(rMan.getString("licreateExisting.existing"));
      ArrayList<ObjChoiceContent>  existing = buildCombo(rootGenome, existingOptions);
      existingCombo_ = new JComboBox(existing.toArray());
      existingCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try { 
            updateLinkDisplay(false, existingCombo_);
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
    
    boolean activeOldInstance = haveOldInstance_;
    if (haveOldInstance_) {      
      drawOldInstance_ = new JRadioButton(rMan.getString("licreateExisting.drawOldInstance"), activeOldInstance);     
      group.add(drawOldInstance_);    
      drawOldInstance_.addActionListener(bt);
      UiUtil.gbcSet(gbc, 0, currRow++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
      cp.add(drawOldInstance_, gbc); 
      
      existingInstanceLabel_ = new JLabel(rMan.getString("licreateExisting.existingInstance"));
      ArrayList<ObjChoiceContent> existing = buildCombo(tgi, existingInstanceOptions);
      existingInstanceCombo_ = new JComboBox(existing.toArray());
      existingInstanceCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try { 
            updateLinkDisplay(false, existingInstanceCombo_);
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

    //
    // Build the position panel:
    //
    
    // Note this is NOT legit, hitting the database from a dialog!
    rcx_ = dacx_.getCustomDACX3();
    
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
            DrawLinkInstanceExistingOptionsDialog.this.setVisible(false);
            DrawLinkInstanceExistingOptionsDialog.this.dispose();
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
          DrawLinkInstanceExistingOptionsDialog.this.setVisible(false);
          DrawLinkInstanceExistingOptionsDialog.this.dispose();
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
    setLocationRelativeTo(uics_.getTopFrame()); 
    // Gotta delay since we need viewport size fixed first:
    //  setActiveFields(activeOld, activeOldInstance);
  }
  
  /***************************************************************************
  **
  ** Get the result ID (may be null)
  ** 
  */
  
  public String getID() {
    return (idResult_);
  }   
  
  /***************************************************************************
  **
  ** Answer if we are to draw:
  ** 
  */
  
  public boolean doDraw() {
    return (doDraw_);
  }   
  
  /***************************************************************************
  **
  ** Answers if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
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
  ** Track the dialog opening
  */
  
  private class DialogOpenListener extends WindowAdapter {
    
    public void windowOpened(WindowEvent e) {
      try {
        boolean activeOld = (haveOld_ && !haveOldInstance_);
        boolean activeOldInstance = haveOldInstance_;
        // This has to happen AFTER the window is up!
        setActiveFields(activeOld, activeOldInstance);
      } catch (Exception ex) {
        uics_.getExceptionHandler().displayException(ex);
      } catch (OutOfMemoryError oom) {
        uics_.getExceptionHandler().displayOutOfMemory(oom);
      }
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
  ** Set what's active
  ** 
  */    
  
  void setActiveFields(boolean isDrawOld, boolean isDrawOldInstance) {    
    if (existingLabel_ != null) existingLabel_.setEnabled(isDrawOld);
    if (existingInstanceLabel_ != null) existingInstanceLabel_.setEnabled(isDrawOldInstance);
    if (existingCombo_ != null) existingCombo_.setEnabled(isDrawOld);
    if (existingInstanceCombo_ != null) existingInstanceCombo_.setEnabled(isDrawOldInstance);
    if (isDrawOld) {
      updateLinkDisplay(false, existingCombo_);
    } else if (isDrawOldInstance) {
      updateLinkDisplay(false, existingInstanceCombo_);
    } else {
      updateLinkDisplay(true, null);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Update the link display
  ** 
  */  
  
  private void updateLinkDisplay(boolean isDrawNew, JComboBox linkCombo) { 
       
    if (isDrawNew) {
      rcx_.setGenome(null);
      rcx_.setLayout(null);
      msp_.repaint();
      return;
    }

    String linkID = ((ObjChoiceContent)linkCombo.getSelectedItem()).val;
    Genome genome = (GenomeItemInstance.isBaseID(linkID)) ? rootGenome_ : tgi_.getVfgParentRoot();    
    String genomeID = genome.getID();
    
    rcx_.setGenome(rcx_.getGenomeSource().getGenome(genomeID));
    rcx_.setLayout(rcx_.getLayoutSource().getLayoutForGenomeKey(genomeID));
    
    Linkage link = genome.getLinkage(linkID); 
    Intersection linkInt = Intersection.pathIntersection(link, rcx_);
    ArrayList<Intersection> selections = new ArrayList<Intersection>();
    selections.add(linkInt);
    msp_.selectLinks(selections);
    msp_.repaint();
    return;
  }   
  
  /***************************************************************************
  **
  ** Build the combo of existing options
  ** 
  */
  
  private ArrayList<ObjChoiceContent> buildCombo(Genome genome, Set<String> existingOptions) {
    ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>();
    if (genome instanceof GenomeInstance) {
      GenomeInstance rootVfg = ((GenomeInstance)genome).getVfgParentRoot();
      genome = (rootVfg == null) ? genome : rootVfg;
    }
    Iterator<String> eoit = existingOptions.iterator();
    while (eoit.hasNext()) {
      String linkID = eoit.next();
      Linkage link = genome.getLinkage(linkID);
      String linkMsg = link.getDisplayString(genome, true);
      retval.add(new ObjChoiceContent(linkMsg, linkID));
    }
    return (retval);
  }
  
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      if (drawNew_.isSelected()) {
        idResult_ = null;
        doDraw_ = true;
        haveResult_ = true;
        return (true);
      } else if ((drawOld_ != null) && drawOld_.isSelected()) {      
        idResult_ = ((ObjChoiceContent)existingCombo_.getSelectedItem()).val;
        doDraw_ = false;
        haveResult_ = true;
        return (true);
      } else if ((drawOldInstance_ != null) && drawOldInstance_.isSelected()) {
        idResult_ = ((ObjChoiceContent)existingInstanceCombo_.getSelectedItem()).val;
        doDraw_ = false;
        haveResult_ = true;
        return (true);
      }
    } else {
      idResult_ = null;
      doDraw_ = false;
      haveResult_ = false;      
    }
    return (true);
  }
}
