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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGeneRegion;
import org.systemsbiology.biotapestry.genome.Gene;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** Factory for dialog boxes that edit cis-reg gene modules
*/

public class CisModuleEditDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CisModuleEditDialogFactory(ServerControlFlowHarness cfh) {
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
   
    CisModBuildArgs dniba = (CisModBuildArgs)ba;
    
    ArrayList<DBGeneRegion> goodCopy = new ArrayList<DBGeneRegion>(dniba.regList);
    Collections.sort(goodCopy);
     
    if (!DBGeneRegion.validOrder(goodCopy, dniba.minPad, dniba.maxPad)) {
      throw new IllegalStateException();
    }
      
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.geneID, dniba.regList, dniba.minPad, dniba.maxPad, 
                                dniba.resize, dniba.moveRegions, dniba.lhr, dniba.firstN, dniba.lastN));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class CisModBuildArgs extends DialogBuildArgs { 
    
    String geneID;
    Gene gene;
    List<DBGeneRegion> regList;
    Map<Integer, ResizeData> resize;
    int padCount ;
    int minPad;
    int maxPad;
    int firstN;
    int lastN;
    Map<Integer, Vector<TrueObjChoiceContent>> moveRegions;
    Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr;
       
    public CisModBuildArgs(String geneID, Gene gene, List<DBGeneRegion> regList, ResourceManager rMan, 
                           Map<String, Map<String, DBGeneRegion.LinkAnalysis>> gla, Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr) {
      super(null);
      this.gene = gene;
      this.geneID = geneID;
      this.regList = regList;
      this.padCount = gene.getPadCount();
      this.minPad = DBGene.DEFAULT_PAD_COUNT - padCount;
      this.maxPad = DBGene.DEFAULT_PAD_COUNT - 1;
      this.lhr = lhr;
      
      this.firstN = DBGeneRegion.firstNonHolderIndex(regList);
      this.lastN = DBGeneRegion.lastNonHolderIndex(regList);
      
      this.resize = new HashMap<Integer, ResizeData>();
      this.moveRegions = new HashMap<Integer, Vector<TrueObjChoiceContent>>();
      int num = this.regList.size();
      for (int i = 0; i < num; i++) {
        DBGeneRegion orig = this.regList.get(i);
        if (!orig.isHolder()) {
          resize.put(Integer.valueOf(i), new ResizeData(i, this.regList, minPad, maxPad, firstN, lastN, lhr));
          moveRegions.put(Integer.valueOf(i), buildMoveRegionsChoices(i, this.regList, firstN, lastN, rMan));
        }
      }

    } 
    
    /***************************************************************************
    **
    ** Calculate move possibilities
    */
      
    private Vector<TrueObjChoiceContent> buildMoveRegionsChoices(int currPos, List<DBGeneRegion> regList, int firstN, int lastN, ResourceManager rMan) {    
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      int num = regList.size();

      if (firstN == lastN) {
        choices.add(new TrueObjChoiceContent(rMan.getString("cmodedit.noMove"), null));
        return (choices);
      }  
      
      DBGeneRegion lastNonHolder = null;

      //
      // If we start with a non-holder, we need to slot in the left position
      // as long as it is not the current position:
      //
      
      if ((currPos != 0) && (firstN == 0)) {
        choices.add(getTOCCForTwo(null, regList.get(firstN), rMan));
      }
  
      for (int i = 0; i < (num - 1); i++) {
        if ((i == currPos) || (i == (currPos - 1))) { // Can't move by staying in place
          lastNonHolder = null;
          continue;
        }
        DBGeneRegion left = regList.get(i);
        DBGeneRegion right = regList.get(i + 1);
        
        boolean leftIsHolder = left.isHolder();
        boolean rightIsHolder = right.isHolder();
        if (leftIsHolder && rightIsHolder) {
          throw new IllegalStateException();
        }
        if (rightIsHolder && (i == (currPos - 2))) { // Blank next to currPos is skipped too
          continue;
        }
        if (leftIsHolder && (i == (currPos + 1))) { // Blank next to currPos is skipped too
          continue;
        }
        if (rightIsHolder) {
          if (i == (num - 2)) {
            choices.add(getTOCCForTwo(left, right, rMan));
            lastNonHolder = null;
          } else {
            lastNonHolder = left;
          }
        } else if (lastNonHolder != null) {
          choices.add(getTOCCForTwo(lastNonHolder, right, rMan));
          lastNonHolder = null;
        } else {
          choices.add(getTOCCForTwo(left, right, rMan));
        }
      }

      //
      // If we end with a non-holder, we need to slot in the right position
      // as long as it is not the current position:
      //
      
      if ((currPos != (num - 1)) && (lastN == (num - 1))) {
        choices.add(getTOCCForTwo(regList.get(lastN), null, rMan));
      }
      
      return (choices);
    }
    
    /***************************************************************************
    **
    ** Build between-region TOCCs
    */
      
    private TrueObjChoiceContent getTOCCForTwo(DBGeneRegion left, DBGeneRegion right, ResourceManager rMan) {
      String desc;
      ArrayList<DBGeneRegion.DBRegKey> pair = new ArrayList<DBGeneRegion.DBRegKey>();
      if ((left == null) || left.isHolder() || left.isLinkHolder()) {
        String format = rMan.getString("cmodedit.moveToLeftEnd");
        desc = MessageFormat.format(format, new Object[] {right.getName()});
        if (left != null) {
          pair.add(left.getKey());           
        }
        pair.add(right.getKey());
      } else if ((right == null) || right.isHolder() || right.isLinkHolder()) {
        String format = rMan.getString("cmodedit.moveToRightEnd");
        desc = MessageFormat.format(format, new Object[] {left.getName()});
        pair.add(left.getKey());
        if (right != null) {
          pair.add(right.getKey());           
        }
      } else {
        String format = rMan.getString("cmodedit.moveBetween");
        desc = MessageFormat.format(format, new Object[] {left.getName(), right.getName()});
        pair.add(left.getKey());
        pair.add(right.getKey());
      }
      return (new TrueObjChoiceContent(desc, pair));
    }
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private String geneID_;
    private List<DBGeneRegion> regList_;
    private Map<Integer, ResizeData> resize_;
    private int firstN_;
    private int lastN_;
    private Map<Integer, Vector<TrueObjChoiceContent>> moveRegions_;
    private Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr_;
    
    private JComboBox chooseRegCombo_;
    private JComboBox chooseOpCombo_;
    private CardLayout myCard_;
    private JPanel cardPanel_;
    private DBGeneRegion currReg_;
    private int minPad_;
    private int maxPad_;
    private ChoiceListener listenLeft_;
    private ChoiceListener listenRight_;
    private String currOp_;
       
    // DELETE CARD CONTROLS
    private JRadioButton blank_;
    private JRadioButton right_;
    private JRadioButton left_;
    private JRadioButton equal_;       
    private JLabel delLabel_;
    private JComboBox destRegCombo_; 
    
    // RESIZE CARD CONTROLS
    private JRadioButton shiftRightOnMove_;
    private JRadioButton shrinkRightOnMove_;
    private JRadioButton shiftLeftOnMove_;
    private JRadioButton shrinkLeftOnMove_;
    
    private JLabel resizeLeftLabel_;
    private JComboBox resizeLeftChoose_;
    private JLabel resizeRightLabel_;
    private JComboBox resizeRightChoose_;
    private BoundsChecker leftChecker_;
    private BoundsChecker rightChecker_;
    private JLabel sizeLab_;
    
    // SLIDE CONTROLS
    
    private JRadioButton pullTrailingOnSlide_;
    private JRadioButton gapTrailingOnSlide_;
    private JComboBox slideAmount_;
    
    // RENAME CONTROLS
    
    private JTextField nameField_;
    private JComboBox evidenceOptions_;
    
    // MOVE CONTROLS
    
    private JComboBox moveOptions_;
     
    // Class statics
    
    private static final long serialVersionUID = 1L;
    
    private static final String DEL_CARD_ = "Delete";
    private static final String SIZE_CARD_ =  "Resize"; 
    private static final String MOV_CARD_ =  "Move";
    private static final String NAME_EVIDENCE_CARD_ = "NameEvidence";
    private static final String SLIDE_CARD_ =  "Slide";
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////    
  
    /***************************************************************************
    **
    ** Constructor 
    */ 
     
    public DesktopDialog(ServerControlFlowHarness cfh, String geneID, List<DBGeneRegion> regList, 
                         int minPad, int maxPad, Map<Integer, ResizeData> resize, 
                         Map<Integer, Vector<TrueObjChoiceContent>> moveRegions, 
                         Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr,
                         int firstN, int lastN) {
      super(cfh, "cmodedit.title", new Dimension(500, 500), 2, new CisModRequest(), false);
      geneID_ = geneID;
      regList_ = new ArrayList<DBGeneRegion>(regList);
      minPad_ = minPad;
      maxPad_ = maxPad;
      currReg_ = DBGeneRegion.firstNonHolder(regList);
      listenLeft_ = new ChoiceListener(true);
      listenRight_ = new ChoiceListener(false);
      currOp_ = DEL_CARD_;
      leftChecker_ = new BoundsChecker(true);
      rightChecker_ = new BoundsChecker(false);
      resize_ = resize;
      firstN_ = firstN;
      lastN_ = lastN;
      moveRegions_ = moveRegions;
      lhr_ = lhr;
    
      //
      // Build the module selection
      //
      
      JLabel label = new JLabel(rMan_.getString("cmodedit.module"));
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      for (DBGeneRegion reg : regList_) {
        if (reg.isHolder() || reg.isLinkHolder()) {
          continue;
        }
        choices.add(new TrueObjChoiceContent(reg.getName(), reg));
      }
      chooseRegCombo_ = new JComboBox(choices);
      chooseRegCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            currReg_ = (DBGeneRegion)((TrueObjChoiceContent)chooseRegCombo_.getSelectedItem()).val;
            setUIForModule();
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      }); 
      addLabeledWidget(label, chooseRegCombo_, true, true);
      
      label = new JLabel(rMan_.getString("cmodedit.operation"));
      Vector<TrueObjChoiceContent> opChoices = new Vector<TrueObjChoiceContent>(); 
      opChoices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.resize"), SIZE_CARD_));
      opChoices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.move"), MOV_CARD_));
      opChoices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.nameAndEvidence"), NAME_EVIDENCE_CARD_));
      opChoices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.delete"), DEL_CARD_));
      opChoices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.slide"), SLIDE_CARD_));
        
      chooseOpCombo_ = new JComboBox(opChoices);
      chooseOpCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            currOp_ = (String)((TrueObjChoiceContent)chooseOpCombo_.getSelectedItem()).val;
            setUIForCommand(currOp_);
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      }); 
      addLabeledWidget(label, chooseOpCombo_, true, true); 
      
      cardPanel_ = new JPanel();     
      myCard_ = new CardLayout();
      cardPanel_.setLayout(myCard_);
      
      JPanel deletePanel = buildDeleteCard();
      cardPanel_.add(deletePanel, DEL_CARD_);
      JPanel resizePanel = buildResizeCard();
      cardPanel_.add(resizePanel, SIZE_CARD_);
      JPanel movePanel = buildMoveCard();
      cardPanel_.add(movePanel, MOV_CARD_);
      JPanel renamePanel = buildNameEvidenceCard();
      cardPanel_.add(renamePanel, NAME_EVIDENCE_CARD_);
      JPanel slidePanel = buildSlideCard();
      cardPanel_.add(slidePanel, SLIDE_CARD_);
 
      addTable(cardPanel_, 6);
      currOp_ = (String)((TrueObjChoiceContent)chooseOpCombo_.getSelectedItem()).val;
      setUIForCommand(currOp_);

      finishConstruction();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
    **
    ** Gotta say
    */
    
    public boolean dialogIsModal() {
      return true;
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /***************************************************************************
    **
    ** Handle UI change for current module choice.
    */
      
    private void setUIForModule() {
      updateDeleteTab();
      enableResizeCard();
      enableSlideCard();
      initRenameCard();
      installMoveRegions();
      return;
    } 
    
    /***************************************************************************
    **
    ** Handle UI change for current module choice.
    */
      
    private void setUIForCommand(String key) {
      setUIForModule();
      myCard_.show(cardPanel_, key);
      return;
    } 
    
    /***************************************************************************
    **
    ** Build the delete card
    */
      
    private JPanel buildDeleteCard() {
      JPanel retval = new JPanel();
      int rowNum = 0;
      retval.setLayout(new GridBagLayout());
      ButtonGroup group = new ButtonGroup();
      blank_ = new JRadioButton(rMan_.getString("cmodedit.leaveOtherRegionsUnchanged"));
      group.add(blank_);
      rowNum = ds_.addWidgetFullRow(retval, blank_, true, true, rowNum, 2);
      right_ = new JRadioButton(rMan_.getString("cmodedit.slideRegionsOnRight"));
      group.add(right_);
      rowNum = ds_.addWidgetFullRow(retval, right_, true, true, rowNum, 2);
      left_ = new JRadioButton(rMan_.getString("cmodedit.slideRegionsOnLeft"));
      group.add(left_);
      rowNum = ds_.addWidgetFullRow(retval, left_, true, true, rowNum, 2);
      equal_ = new JRadioButton(rMan_.getString("cmodedit.slideRegionsEqual"));
      group.add(equal_);
      rowNum = ds_.addWidgetFullRow(retval, equal_, true, true, rowNum, 2);
      
      delLabel_ = new JLabel(rMan_.getString("cmodedit.moveLinksTo"));
    
      destRegCombo_ = new JComboBox();
      rowNum = ds_.addLabeledWidget(retval, delLabel_, destRegCombo_, true, true, rowNum, 2);
      blank_.setSelected(true);
      installDestRegions();
   
      return (retval);
    }
    
    
    /***************************************************************************
    **
    ** Build the move card
    */
      
    private JPanel buildMoveCard() {
      JPanel retval = new JPanel();
      int rowNum = 0;
      retval.setLayout(new GridBagLayout());
        
      JLabel lab = new JLabel(rMan_.getString("cmodedit.moveTo"));
    
      moveOptions_ = new JComboBox();
      rowNum = ds_.addLabeledWidget(retval, lab, moveOptions_, true, true, rowNum, 2);
      installMoveRegions();
      if (firstN_ == lastN_) {     
        moveOptions_.setEnabled(false);
      }
      
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Build the rename card
    */
      
    private JPanel buildNameEvidenceCard() {
      JPanel retval = new JPanel();
      int rowNum = 0;
      retval.setLayout(new GridBagLayout()); 
      JLabel lab = new JLabel(rMan_.getString("cmodedit.regionName"));
      nameField_ = new JTextField();
      rowNum = ds_.addLabeledWidget(retval, lab, nameField_, true, true, rowNum, 2);
            
      JLabel lab2 = new JLabel(rMan_.getString("cmodedit.regionEvidence"));
      evidenceOptions_ = new JComboBox(buildEvidenceCombo());
      rowNum = ds_.addLabeledWidget(retval, lab2, evidenceOptions_, true, true, rowNum, 2);
         
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Evidence combos
    */
   
    private Vector<TrueObjChoiceContent> buildEvidenceCombo() {  
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      StringBuffer buf = new StringBuffer();
      Set<String> evals = DBGene.evidenceLevels();
      Iterator<String> evit = evals.iterator();
      while (evit.hasNext()) {
        String elev = evit.next();
        buf.setLength(0);
        buf.append("nprop.");
        buf.append(elev);        
        String desc = rMan_.getString(buf.toString()); 
        choices.add(new TrueObjChoiceContent(desc, Integer.valueOf(DBGene.mapFromEvidenceTag(elev))));
      }
      return (choices);
    }
    
    /***************************************************************************
    **
    ** Get the combo for the desired level
    */
   
    private void setEvidenceForLevel(int matchEv) {
      Integer currSet = Integer.valueOf(matchEv);
      ComboBoxModel mod = evidenceOptions_.getModel();
      int numMod = mod.getSize();
      for (int i = 0; i < numMod; i++) {
        TrueObjChoiceContent cc = (TrueObjChoiceContent)mod.getElementAt(i);
        if (cc.val.equals(currSet)) {
          evidenceOptions_.setSelectedIndex(i);
          return;  
        }
      }
      throw new IllegalArgumentException();
    }
    
   /***************************************************************************
    **
    ** Build the resize
    */
      
    private JPanel buildResizeCard() {
      JPanel retval = new JPanel();
      int rowNum = 0;
      retval.setLayout(new GridBagLayout());
      
      sizeLab_ = new JLabel(" ", JLabel.CENTER);
      sizeLab_.setFont(sizeLab_.getFont().deriveFont(Font.BOLD));
      rowNum = ds_.addWidgetFullRow(retval, sizeLab_, false, false, rowNum, 2);
      
      ButtonGroup leftGroup = new ButtonGroup();
      
      shiftLeftOnMove_ = new JRadioButton(rMan_.getString("cmodedit.shiftLeftOnMove"));
      shiftLeftOnMove_.addActionListener(listenLeft_);   
      leftGroup.add(shiftLeftOnMove_);
      rowNum = ds_.addWidgetFullRow(retval, shiftLeftOnMove_, true, true, rowNum, 2);
      
      shrinkLeftOnMove_ = new JRadioButton(rMan_.getString("cmodedit.shrinkLeftOnMove"));
      shrinkLeftOnMove_.addActionListener(listenLeft_);
      leftGroup.add(shrinkLeftOnMove_);
      rowNum = ds_.addWidgetFullRow(retval, shrinkLeftOnMove_, true, true, rowNum, 2);
      shiftLeftOnMove_.setSelected(true);
            
      resizeLeftLabel_ = new JLabel(rMan_.getString("cmodedit.leftRegionTreatment"));
      resizeLeftChoose_ = new JComboBox();
      resizeLeftChoose_.addActionListener(leftChecker_);
      rowNum = ds_.addLabeledWidget(retval, resizeLeftLabel_, resizeLeftChoose_, true, true, rowNum, 2);

      rowNum = ds_.addWidgetFullRow(retval, new JLabel(" "), true, true, rowNum, 2);
      rowNum = ds_.addWidgetFullRow(retval, new JLabel(" "), true, true, rowNum, 2);
    
      ButtonGroup rightGroup = new ButtonGroup();
      
      shiftRightOnMove_ = new JRadioButton(rMan_.getString("cmodedit.shiftRightOnMove"));
      shiftRightOnMove_.addActionListener(listenRight_);
      rightGroup.add(shiftRightOnMove_);
      rowNum = ds_.addWidgetFullRow(retval, shiftRightOnMove_, true, true, rowNum, 2);
      
      shrinkRightOnMove_ = new JRadioButton(rMan_.getString("cmodedit.shrinkRightOnMove"));
      shrinkRightOnMove_.addActionListener(listenRight_);
      rightGroup.add(shrinkRightOnMove_);
      rowNum = ds_.addWidgetFullRow(retval, shrinkRightOnMove_, true, true, rowNum, 2);
      shiftRightOnMove_.setSelected(true);
       
      resizeRightLabel_ = new JLabel(rMan_.getString("cmodedit.rightRegionTreatment"));
      resizeRightChoose_ = new JComboBox();
      resizeRightChoose_.addActionListener(rightChecker_);
      rowNum = ds_.addLabeledWidget(retval, resizeRightLabel_, resizeRightChoose_, true, true, rowNum, 2);
    
      return (retval);
    }
  
    /***************************************************************************
    **
    ** Handle building the slide tab
    */
      
    private JPanel buildSlideCard() {
      JPanel retval = new JPanel();
      int rowNum = 0;
      retval.setLayout(new GridBagLayout());
       
      ButtonGroup leftGroup = new ButtonGroup();
      
      pullTrailingOnSlide_ = new JRadioButton(rMan_.getString("cmodedit.pullOnSlide")); 
      leftGroup.add(pullTrailingOnSlide_);
      rowNum = ds_.addWidgetFullRow(retval, pullTrailingOnSlide_, true, true, rowNum, 2);
    
      gapTrailingOnSlide_ = new JRadioButton(rMan_.getString("cmodedit.gapOnSlide")); 
      leftGroup.add(gapTrailingOnSlide_);
      rowNum = ds_.addWidgetFullRow(retval, gapTrailingOnSlide_, true, true, rowNum, 2);
            
      JLabel slideLabel = new JLabel(rMan_.getString("cmodedit.slideAmount"));
      slideAmount_ = new JComboBox();
      rowNum = ds_.addLabeledWidget(retval, slideLabel, slideAmount_, true, true, rowNum, 2);
    
      return (retval);
    }
     
    /***************************************************************************
    **
    ** Install slide amounts
    */
      
    private int updateSlideCombo(int leftWiggle, int rightWiggle) {   
    
      // If we are shifting all modules, the amount of wiggle room is the space in the gap immediately to the side,
      // plus the space on the end.
  
      String lFormat = rMan_.getString("cmodedit.leftMoveFormat");
      String rFormat = rMan_.getString("cmodedit.rightMoveFormat");
      String nFormat = rMan_.getString("cmodedit.noMoveFormat");
      int retval = Integer.MIN_VALUE;
      
      int count = 0;
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      for (int i = -leftWiggle; i <= rightWiggle; i++) {
        String use;
        int show;
        if (i < 0) {
          use = lFormat;
          show = -i;
        } else if (i > 0) {
          use = rFormat;
          show = i;
        } else {
          use = nFormat;
          show = 0;
          retval = count;
        }
        count++;
        String desc = MessageFormat.format(use, new Object[] {Integer.valueOf(show)});
        choices.add(new TrueObjChoiceContent(desc, Integer.valueOf(i)));
      }
      // NO WIGGLE ROOM!
      if (choices.size() == 0) {
        String desc = MessageFormat.format(nFormat, new Object[] {Integer.valueOf(0)});
        choices.add(new TrueObjChoiceContent(desc, Integer.valueOf(0)));
        retval = 0;
      }    
      UiUtil.replaceComboItems(slideAmount_, choices);
      return (retval);    
    }
 
    /***************************************************************************
    **
    ** Install move amounts
    */
      
    private int buildMoveCombo(JComboBox combo, int bot, int top) {
      String lFormat = rMan_.getString("cmodedit.leftMoveFormat");
      String rFormat = rMan_.getString("cmodedit.rightMoveFormat");
      String nFormat = rMan_.getString("cmodedit.noMoveFormat");
      int retval = Integer.MIN_VALUE;
      
      int count = 0;
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      for (int i = bot; i <= top; i++) {
        String use;
        int show;
        if (i < 0) {
          use = lFormat;
          show = -i;
        } else if (i > 0) {
          use = rFormat;
          show = i;
        } else {
          use = nFormat;
          show = 0;
          retval = count;
        }
        count++;
        String desc = MessageFormat.format(use, new Object[] {Integer.valueOf(show)});
        choices.add(new TrueObjChoiceContent(desc, Integer.valueOf(i)));
      }
  
      UiUtil.replaceComboItems(combo, choices);
      return (retval);
    }

    /***************************************************************************
    **
    ** Install move amounts
    */
      
    private void enableResizeCard() {
      
      listenLeft_.setIgnore(true);
      listenRight_.setIgnore(true);
 
      if (firstN_ == lastN_) {
        shiftLeftOnMove_.setEnabled(false);
        shrinkLeftOnMove_.setEnabled(false);
        shiftRightOnMove_.setEnabled(false);
        shrinkRightOnMove_.setEnabled(false);
      } else {
        int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
        if (currPos == firstN_) {
          shiftLeftOnMove_.setEnabled(false);
          shrinkLeftOnMove_.setEnabled(false);
          shiftRightOnMove_.setEnabled(true);
          shrinkRightOnMove_.setEnabled(true);      
        } else if (currPos == lastN_) {
          shiftLeftOnMove_.setEnabled(true);
          shrinkLeftOnMove_.setEnabled(true);
          shiftRightOnMove_.setEnabled(false);
          shrinkRightOnMove_.setEnabled(false);
        } else {
          shiftLeftOnMove_.setEnabled(true);
          shrinkLeftOnMove_.setEnabled(true);
          shiftRightOnMove_.setEnabled(true);
          shrinkRightOnMove_.setEnabled(true);
        }
      }
      listenLeft_.setIgnore(false);
      listenRight_.setIgnore(false);
      updateResizeCombos(true);
      
      return;
    }     
    
    /***************************************************************************
    **
    ** Install current name
    */
      
    private void initRenameCard() {    
      nameField_.setText(currReg_.getName());  
      setEvidenceForLevel(currReg_.getEvidenceLevel());
      return;
    } 
     
    /***************************************************************************
    **
    ** Install slide amounts
    */
      
    private void enableSlideCard() {
      
      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      int leftWiggle = DBGeneRegion.slideSpace(regList_, currPos, true, lhr_);
      int rightWiggle = DBGeneRegion.slideSpace(regList_, currPos, false, lhr_);
      //
      // To be active, there has to be somebody behind me in the directions I can move.
      //
      boolean canPullLeftNeighbors = (rightWiggle > 0) && (currPos > firstN_);
      boolean canPullRightNeighbors = (leftWiggle > 0) && (currPos < lastN_);
      boolean canPull = canPullRightNeighbors || canPullLeftNeighbors;
  
      pullTrailingOnSlide_.setEnabled(canPull);
      gapTrailingOnSlide_.setEnabled(canPull);
     
      int zero = updateSlideCombo(leftWiggle, rightWiggle);
      slideAmount_.setSelectedIndex(zero);
      
      pullTrailingOnSlide_.setSelected(true);    
      return;
    }

    /***************************************************************************
    **
    ** If e.g. left bounds are moved right, and right bounds are moved left, we
    ** can get a negative sized region. Need to keep the two choice options in
    ** sync so this does not happen
    */
      
    private void trimResizeCombos(boolean isLeft) {
      
      //
      // "IS LEFT" means the left margin combo has fired a change!
      //
      int startWidth = currReg_.getWidth();
      
      SortedSet<Integer> pads = lhr_.get(currReg_.getKey());
      int links = (pads == null) ? 0 : pads.size();
      int origAllowedReduction = currReg_.getWidth() - Math.max(1, links);
      if (origAllowedReduction < 0) {
        throw new IllegalStateException();
      }
        
      int currSP = currReg_.getStartPad();
      if (isLeft) {
        int leftMove = ((Integer)(((TrueObjChoiceContent)resizeLeftChoose_.getSelectedItem()).val)).intValue();
        if (leftMove > 0) {
          currSP += leftMove;
        }
      }
      
      int currEP = currReg_.getEndPad();
      if (!isLeft) {
        int rightMove = ((Integer)(((TrueObjChoiceContent)resizeRightChoose_.getSelectedItem()).val)).intValue();
        if (rightMove < 0) {
          currEP += rightMove;
        }
      }     
      // This is what the current settings are giving for a width:
      int currSettingsWidth = currEP - currSP + 1;     
      int consumedReduction = startWidth - currSettingsWidth;  
      int remainingWiggle = origAllowedReduction - consumedReduction;

      //
      // If the last change came from the left, we limit the options on the right. 
      //
      // If we are called after a left boundary shift, we only modify the right boundary
      // choice to keep the minimum size in bounds.
      //
      // No matter what, the minimum size of a module is one pad (or the number of link pads it is using. That restricts the number of
      // pads you can move the left bound right, and right bound left:
      //
      
      // This is the rightward maximum (i.e. highest value) of the left bounds JCombo:
      int maxLeftBoundRight = (isLeft) ? origAllowedReduction : remainingWiggle;
      // This is the leftward maximum (i.e. lowest value) of the right bounds JCombo:
      int maxRightBoundLeft = (isLeft) ? -remainingWiggle : -origAllowedReduction;
      
      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      ResizeData resize = this.resize_.get(Integer.valueOf(currPos));
      // This is the leftward maximum (i.e. lowest value) of the left bounds JCombo:
      int useForLeft = (shiftLeftOnMove_.isSelected()) ? resize.useForLeftOnShift : resize.useForLeftOnShrink;
       // This is the rightward maximum (i.e. highest value) of the right bounds JCombo:
      int useForRight = (shiftRightOnMove_.isSelected()) ? resize.useForRightOnShift : resize.useForRightOnShrink;
   
      if (isLeft) {
        this.rightChecker_.setIgnore(true);
        TrueObjChoiceContent cval = (TrueObjChoiceContent)resizeRightChoose_.getSelectedItem();
        buildMoveCombo(resizeRightChoose_, maxRightBoundLeft, useForRight);
        setToClosest(cval, resizeRightChoose_, false);
        this.rightChecker_.setIgnore(false);
      } else {
        this.leftChecker_.setIgnore(true);
        TrueObjChoiceContent cval = (TrueObjChoiceContent)resizeLeftChoose_.getSelectedItem();
        buildMoveCombo(resizeLeftChoose_, useForLeft, maxLeftBoundRight);
        setToClosest(cval, resizeLeftChoose_, true);
        this.leftChecker_.setIgnore(false); 
      }      
      return;
    }
    
    /***************************************************************************
    **
    ** Install resize values
    */
      
    private void updateResizeCombos(boolean init) {

      SortedSet<Integer> pads = lhr_.get(currReg_.getKey());
      int links = (pads == null) ? 0 : pads.size();
      int allowed = currReg_.getWidth() - Math.max(1, links);
     
      //
      // No matter what, the minimum size of an empty NON-HOLDER module is one pad, or the width of the contained links.
      // Minimum size of holder is zero pads, or the width of contained links.
      // That restricts the number of pads you can move the left bound right, and right bound left.
      //
      
      int maxLeftBoundRight = allowed;
      int maxRightBoundLeft = -allowed;
        
      //
      // If we are eating into the neighboring region, then we are limited by its size. If we are right next to a holder, then
      // we can add that entire width onto the amount, unless it has links into it!
      //
      
      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      ResizeData resize = this.resize_.get(Integer.valueOf(currPos)); 
      int useForLeft = (shiftLeftOnMove_.isSelected()) ? resize.useForLeftOnShift : resize.useForLeftOnShrink;
      int useForRight = (shiftRightOnMove_.isSelected()) ? resize.useForRightOnShift : resize.useForRightOnShrink;
      leftChecker_.setIgnore(true);
      rightChecker_.setIgnore(true);
      TrueObjChoiceContent cvar = (TrueObjChoiceContent)resizeRightChoose_.getSelectedItem();
      TrueObjChoiceContent cval = (TrueObjChoiceContent)resizeLeftChoose_.getSelectedItem();
      int leftNC = buildMoveCombo(resizeLeftChoose_, useForLeft, maxLeftBoundRight);
      int rightNC = buildMoveCombo(resizeRightChoose_, maxRightBoundLeft, useForRight);
      if (init) {
        resizeLeftChoose_.setSelectedIndex(leftNC);
        resizeRightChoose_.setSelectedIndex(rightNC);    
      } else {
        setToClosest(cval, resizeLeftChoose_, true);
        setToClosest(cvar, resizeRightChoose_, false);     
      }
      rightChecker_.setIgnore(false);
      leftChecker_.setIgnore(false);
      updateSizeLabel();

      return;
    }
    
    /***************************************************************************
    **
    ** Get the choice to the closest possible to the current setting.
    */
      
    private void setToClosest(TrueObjChoiceContent cval, JComboBox combo, boolean isLeft) {
      Integer currSet = (Integer)cval.val;
      ComboBoxModel mod = combo.getModel();
      int numMod = mod.getSize();
      for (int i = 0; i < numMod; i++) {
        TrueObjChoiceContent cc = (TrueObjChoiceContent)mod.getElementAt(i);
        if (cc.val.equals(currSet) || 
            (isLeft && (((Integer)cc.val).intValue() > currSet.intValue())) || 
            (!isLeft && (i == numMod - 1))) {
          combo.setSelectedIndex(i);
          return;  
        }
      }
      UiUtil.fixMePrintout("Keep or restore?");
      Thread.dumpStack();
      //throw new IllegalStateException();
    } 

    /***************************************************************************
    **
    ** Install move amounts
    */
      
    private void updateSizeLabel() {     
      int size = currReg_.getEndPad() - currReg_.getStartPad() + 1;
      int leftMove = ((Integer)(((TrueObjChoiceContent)resizeLeftChoose_.getSelectedItem()).val)).intValue();
      int rightMove = ((Integer)(((TrueObjChoiceContent)resizeRightChoose_.getSelectedItem()).val)).intValue();
      int newSize = size + (-leftMove) + rightMove;
      String format = rMan_.getString("cmodedit.sizeFormat");
      String desc = MessageFormat.format(format, new Object[] {Integer.valueOf(size), Integer.valueOf(newSize)});
      sizeLab_.setText(desc);
      sizeLab_.invalidate();
      validate();
      return;
    }
  
    /***************************************************************************
    **
    ** Install link destination regions
    */
      
    private void installDestRegions() {
      DBGeneRegion.DBRegKey drk = currReg_.getKey();
      Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
      choices.add(new TrueObjChoiceContent(rMan_.getString("cmodedit.doNotTransfer"), null));
      for (DBGeneRegion reg : regList_) {
        if (reg.isHolder() || reg.isLinkHolder()) {
          continue;
        }
        if (!drk.equals(reg.getKey())) {
          choices.add(new TrueObjChoiceContent(reg.getName(), reg));
        }
      }
      UiUtil.replaceComboItems(destRegCombo_, choices);
      return;
    }
    
    /***************************************************************************
    **
    ** Install move possibilities
    */
      
    private void installMoveRegions() {
      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      Vector<TrueObjChoiceContent> choices = moveRegions_.get(Integer.valueOf(currPos));
      UiUtil.replaceComboItems(moveOptions_, choices);
      return;
    }  
 
    /***************************************************************************
    **
    ** Handle delete tab changes for current module choice.
    */
      
    private void updateDeleteTab() {
  
      //
      // If there is only one region, there is nothing to do. When it disappears, everything will
      // remain as it is.
      //
  
      if (firstN_ == lastN_) {
        blank_.setSelected(true);
        blank_.setEnabled(false); 
        right_.setEnabled(false); 
        left_.setEnabled(false); 
        equal_.setEnabled(false);   
        delLabel_.setEnabled(false);
        destRegCombo_.setEnabled(false);
        installDestRegions();
        return;
      }
      
      //
      // If the region is on the left end, we can choose to slide regions on the right over to 
      // the left to replace it, or we can do nothing. Do need to have the user say what to do 
      // about the links. 
      //

      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      if (currPos == firstN_) {
        blank_.setEnabled(true); 
        right_.setEnabled(true); 
        left_.setEnabled(false); 
        equal_.setEnabled(false);   
        delLabel_.setEnabled(true);
        destRegCombo_.setEnabled(true);
      } else if (currPos == lastN_) {
        blank_.setEnabled(true); 
        right_.setEnabled(false); 
        left_.setEnabled(true); 
        equal_.setEnabled(false);   
        delLabel_.setEnabled(true);
        destRegCombo_.setEnabled(true);
      } else {
        blank_.setEnabled(true); 
        right_.setEnabled(true); 
        left_.setEnabled(true); 
        equal_.setEnabled(true);   
        delLabel_.setEnabled(true);
        destRegCombo_.setEnabled(true);
      }
      installDestRegions();
      return;
    }

    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      CisModRequest crq = (CisModRequest)request_;
      boolean ok = false;
      if (currOp_.equals(SIZE_CARD_)) {
        ok = bundleForSize(crq);
      } else if (currOp_.equals(MOV_CARD_)) {
        ok = bundleForMove(crq);
      } else if (currOp_.equals(NAME_EVIDENCE_CARD_)) {
        ok = bundleForNameEvidence(crq);       
      } else if (currOp_.equals(DEL_CARD_)) {
        ok = bundleForDelete(crq);
      } else if (currOp_.equals(SLIDE_CARD_)) {
        ok = bundleForSlide(crq);
      } else {
        throw new IllegalStateException();
      }
  
      if (!ok) {
        return (false);
      }
      crq.haveResult = true;
      return (true);
    } 
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private boolean bundleForSize(CisModRequest crq) {  
      
      crq.sizeMode = CisModRequest.SizeMode.DO_SIZE;
      crq.selectedKey = currReg_.getKey();
      //
      // These are the shifts to apply to the boundaries of the specified current region:
      //
      crq.resizeLeftAmt = ((Integer)(((TrueObjChoiceContent)resizeLeftChoose_.getSelectedItem()).val)).intValue();
      crq.resizeRightAmt = ((Integer)(((TrueObjChoiceContent)resizeRightChoose_.getSelectedItem()).val)).intValue();     
      
      crq.shiftLeftOnMove = shiftLeftOnMove_.isSelected();
      crq.shrinkLeftOnMove = shrinkLeftOnMove_.isSelected();
      crq.shiftRightOnMove = shiftRightOnMove_.isSelected();
      crq.shrinkRightOnMove = shrinkRightOnMove_.isSelected();
      
      if (crq.shiftLeftOnMove && crq.shrinkLeftOnMove) {
        throw new IllegalStateException();   
      }
      if (crq.shiftRightOnMove && crq.shrinkRightOnMove) {
        throw new IllegalStateException();   
      }
 
      int size = currReg_.getWidth();
      if ((size - crq.resizeLeftAmt +  crq.resizeRightAmt) < 1) {
        throw new IllegalStateException();
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private boolean bundleForMove(CisModRequest crq) {
      if (firstN_ == lastN_) {
        return (false);
      }
      crq.selectedKey = currReg_.getKey();
      TrueObjChoiceContent tocc = (TrueObjChoiceContent)moveOptions_.getSelectedItem();
      crq.movePair = (List<DBGeneRegion.DBRegKey>)tocc.val;
      crq.moveMode = CisModRequest.MoveMode.DO_MOVE;
      return (true);
    }

    /***************************************************************************
    **
    ** Do the bundle for deletions
    */
      
    private boolean bundleForDelete(CisModRequest crq) { 
    
      crq.selectedKey = currReg_.getKey();
      if (blank_.isSelected()) {
        crq.delMode = CisModRequest.DeleteMode.DELETE_BLANK;
      } else if (right_.isSelected()) {
        crq.delMode = CisModRequest.DeleteMode.DELETE_RIGHT;
      } else if (left_.isSelected()) {
        crq.delMode = CisModRequest.DeleteMode.DELETE_LEFT;
      } else if (equal_.isSelected()) {
        crq.delMode = CisModRequest.DeleteMode.DELETE_EQUAL;
      } else {
        throw new IllegalStateException();
      }

      DBGeneRegion delTarg = ((DBGeneRegion)((TrueObjChoiceContent)this.destRegCombo_.getSelectedItem()).val);
      crq.deleteLinkTarget = (delTarg == null) ? null : delTarg.getKey();        
      return (true);
    }

    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    private boolean bundleForSlide(CisModRequest crq) {
      crq.selectedKey = currReg_.getKey();
      if (pullTrailingOnSlide_.isSelected()) {
        crq.slideMode = CisModRequest.SlideMode.SLIDE_PULL;
      } else if (gapTrailingOnSlide_.isSelected()) {
        crq.slideMode = CisModRequest.SlideMode.SLIDE_GAP;
      } else {
        throw new IllegalArgumentException();
      }
      crq.slideAmt = ((Integer)(((TrueObjChoiceContent)slideAmount_.getSelectedItem()).val)).intValue();
      return (true);
    }
    
    /***************************************************************************
    **
    ** Bundle for rename
    */
      
    private boolean bundleForNameEvidence(CisModRequest crq) {
      
      crq.nameMode = CisModRequest.NameMode.DO_NAME_EVIDENCE;
      crq.selectedKey = currReg_.getKey();
      
      String newName = nameField_.getText().trim();
      if (newName.equals("")) {   
        String msg = rMan_.getString("cisedit.blankName");
        String title = rMan_.getString("cisedit.blankNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
 
      int currPos = DBGeneRegion.regionIndex(regList_, currReg_.getKey());
      crq.newEvidence = ((Integer)(((TrueObjChoiceContent)this.evidenceOptions_.getSelectedItem()).val)).intValue();
     
      int num = regList_.size();
      for (int i = 0; i < num; i++) {
        DBGeneRegion orig = regList_.get(i);
        if (i != currPos) {
          if (DataUtil.normKeyWithGreek(orig.getName()).equals(DataUtil.normKeyWithGreek(newName))) {
            String msg = rMan_.getString("cisedit.dupName");
            String title = rMan_.getString("cisedit.dupNameTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
            cfh_.showSimpleUserFeedback(suf);
            return (false);
          }
        }
      }
      crq.newName = newName;
      return (true);
    } 

    /***************************************************************************
    **
    ** Common listener
    ** 
    */
   
    private class ChoiceListener implements ActionListener {
     
      private boolean ignore_;
      private boolean isLeft_;
      
      ChoiceListener(boolean isLeft) {
        isLeft_ = isLeft;
      }
        
      void setIgnore(boolean ignore) {
        ignore_ = ignore;
        return;
      }      
      
      public void actionPerformed(ActionEvent ev) {
        try {
          if (!ignore_) {
            //
            // When we change the radio buttons for e.g. the left choice, we need to modify the
            // *left* choices, not the right. So the trim operation needs to occur on the left,
            // i.e. the reverse of the normal usage:
            //
            trimResizeCombos(!isLeft_);
            updateSizeLabel();
          }

        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }
    
    /***************************************************************************
    **
    ** Common listener
    ** 
    */
   
    private class BoundsChecker implements ActionListener {
     
      private boolean ignore_;
      private boolean isLeft_;
      
      BoundsChecker(boolean isLeft) {
        isLeft_ = isLeft;
      }
      
      
      void setIgnore(boolean ignore) {
        ignore_ = ignore;
        return;
      }      
      
      public void actionPerformed(ActionEvent ev) {
        try {
          if (!ignore_) {
            trimResizeCombos(isLeft_);
            updateSizeLabel();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }
 
  }

   /***************************************************************************
   **
   ** Return Results
   ** 
   */
   
   public static class CisModRequest implements ServerControlFlowHarness.UserInputs {
     
     public enum DeleteMode {DELETE_BLANK, DELETE_RIGHT, DELETE_LEFT, DELETE_EQUAL}; 
     public enum SlideMode {SLIDE_PULL, SLIDE_GAP};
     public enum MoveMode {DO_MOVE};
     public enum SizeMode {DO_SIZE};
     public enum NameMode {DO_NAME_EVIDENCE};

     private boolean haveResult;
     public DeleteMode delMode;
     public SlideMode slideMode;
     public MoveMode moveMode;
     public SizeMode sizeMode;
     public NameMode nameMode;
     
     // Current region
     public DBGeneRegion.DBRegKey selectedKey;
     // Name/Evidence:
     public String newName;
     public int newEvidence;
     // Sliding:
     public int slideAmt;
     // Moving
     public List<DBGeneRegion.DBRegKey> movePair;
     // Resizing
     public boolean shiftRightOnMove;
     public boolean shrinkRightOnMove;
     public boolean shiftLeftOnMove;
     public boolean shrinkLeftOnMove;
     public int resizeLeftAmt;
     public int resizeRightAmt;
     // Deleting
     public DBGeneRegion.DBRegKey deleteLinkTarget;
     
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
   
  /***************************************************************************
  **
  ** Hold parameters for resizing options
  ** 
  */
 
  public static class ResizeData {
  
    int currPos;
    List<Integer> resizeBounds;
    
    int useForLeftOnShift;
    int useForLeftOnShrink;
    int useForRightOnShift;
    int useForRightOnShrink;
   
    ResizeData(int currPos, List<DBGeneRegion> regList, int minPad, int maxPad, int firstN,  int lastN, Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr) {
      this.currPos = currPos;
      this.resizeBounds = resizeBounds(currPos, regList, lhr);
      
      //
      // If we are shifting all modules, the amount of wiggle room is the space between left
      // end of gene and start pad of left non-holder region, MINUS SPACE NEEDED FOR LINKS. Similar idea on right end. Note this gives the
      // space for the singleton region as well. 
      //
      // If we are eating into the neighboring region, then we are limited by its size. If we are right next to a holder, then
      // we can add that entire width onto the amount, UNLESS IT CONTAINS LINKS. Note the region list this routine has been given considers
      // holder regions with links to NOT be called holders; they are "link holders"
      //
      
      DBGeneRegion leftmost = regList.get(firstN);
      DBGeneRegion rightmost = regList.get(lastN);
      
      //
      // Shoulders can be ditched, unless they have links into them:
      //
      
      int leftLinkShoulderNeeds = 0;
      if (firstN != 0) {
        SortedSet<Integer> needs = lhr.get(regList.get(0).getKey());
        leftLinkShoulderNeeds = (needs == null) ? 0 : needs.size();       
      }
      
      int rightLinkShoulderNeeds = 0;
      int num = regList.size();
      if (lastN != (num - 1)) {
        SortedSet<Integer> needs = lhr.get(regList.get(num - 1).getKey());
        rightLinkShoulderNeeds = (needs == null) ? 0 : needs.size();       
      }

      int leftWiggle = minPad - leftmost.getStartPad() + leftLinkShoulderNeeds; // Want negative sign here
      int rightWiggle = maxPad - rightmost.getEndPad() - rightLinkShoulderNeeds;
      
      useForLeftOnShift = calcConstrainedUseForLeft(firstN, lastN, true, leftWiggle, resizeBounds.get(0), currPos);
      useForLeftOnShrink = calcConstrainedUseForLeft(firstN, lastN, false, leftWiggle, resizeBounds.get(0), currPos);
      useForRightOnShift = calcConstrainedUseForRight(firstN, lastN, true, rightWiggle, resizeBounds.get(1), currPos);
      useForRightOnShrink = calcConstrainedUseForRight(firstN, lastN, false, rightWiggle, resizeBounds.get(1), currPos);
    }
   
    //
    // The idea here is that we can blow empty holders away and eat into the adjacent module. Used to be we only required one
    // pad to remain if links inbound. But now we need to keep all the pads distinct.
    //
    
    private List<Integer> resizeBounds(int currPos, List<DBGeneRegion> regList, Map<DBGeneRegion.DBRegKey, SortedSet<Integer>> lhr) {
      ArrayList<Integer> retval = new ArrayList<Integer>();
      
      DBGeneRegion justLeft = (currPos == 0) ? null : regList.get(currPos - 1);
      DBGeneRegion holderLeft = null;
      // NOTE: For this analysis, holders with with links are *not* "holders", but "link holders"
      if ((justLeft != null) && justLeft.isHolder()) {
        holderLeft = justLeft;
        justLeft = (currPos == 1) ? null : regList.get(currPos - 2);
      }
      
      SortedSet<Integer> justLeftLinks = (justLeft == null) ? null : lhr.get(justLeft.getKey());
      int justLeftNeeds = (justLeftLinks == null) ? 1 : justLeftLinks.size();      
      
      DBGeneRegion justRight = (currPos == (regList.size() - 1)) ? null : regList.get(currPos + 1);
      DBGeneRegion holderRight = null;
      if ((justRight != null) && justRight.isHolder()) {
        holderRight = justRight;
        justRight = (currPos == (regList.size() - 2)) ? null : regList.get(currPos + 2);
      }
     
      SortedSet<Integer> justRightLinks = (justRight == null) ? null : lhr.get(justRight.getKey());
      int justRightNeeds = (justRightLinks == null) ? 1 : justRightLinks.size();   
      
      Integer eatLeft = (holderLeft != null) ? Integer.valueOf(holderLeft.getWidth()) : Integer.valueOf(0);
      if (justLeft != null) {
        eatLeft = Integer.valueOf(eatLeft.intValue() + justLeft.getWidth() - justLeftNeeds);
      }
      retval.add(eatLeft);
      
      Integer eatRight = (holderRight != null) ? Integer.valueOf(holderRight.getWidth()) : Integer.valueOf(0);
      if (justRight != null) {
        eatRight = Integer.valueOf(eatRight.intValue() + justRight.getWidth() - justRightNeeds);
      }
      retval.add(eatRight);
      return (retval);   
    }
    
    private int calcConstrainedUseForLeft(int firstN, int lastN, boolean shiftLeft, int leftWiggle, Integer eatLeft, int currPos) {
      int useForLeft;
      if (firstN == lastN) {
        useForLeft = leftWiggle;
       } else { 
        if (currPos == firstN) {
          useForLeft = leftWiggle;
        } else if (currPos == lastN) {
          useForLeft = (shiftLeft) ? leftWiggle : -eatLeft.intValue();   
        } else {
          useForLeft = (shiftLeft) ? leftWiggle : -eatLeft.intValue();
        }
      }
      return (useForLeft);
    }
        
    private int calcConstrainedUseForRight(int firstN, int lastN, boolean shiftRight, int rightWiggle, Integer eatRight, int currPos) {
      int useForRight;
       if (firstN == lastN) {
        useForRight = rightWiggle;
      } else { 
        if (currPos == firstN) {
          useForRight = (shiftRight) ? rightWiggle : eatRight.intValue();   
        } else if (currPos == lastN) {
          useForRight = rightWiggle;
        } else {
          useForRight = (shiftRight) ? rightWiggle : eatRight.intValue();
        }
      }
      return (useForRight);
    }
  }
}
