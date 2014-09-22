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

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.RemoteRequest;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.cmd.instruct.GeneralBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;
import org.systemsbiology.biotapestry.cmd.instruct.InstructionRegions;
import org.systemsbiology.biotapestry.cmd.instruct.LoneNodeBuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.SignalBuildInstruction;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParamDialogFactory;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngineParams;
import org.systemsbiology.biotapestry.util.ComboBoxEditor;
import org.systemsbiology.biotapestry.util.ComboBoxEditorTracker;
import org.systemsbiology.biotapestry.util.ComboFinishedTracker;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.MultiChoiceEditor;
import org.systemsbiology.biotapestry.util.MultiChoiceEditorTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TextEditor;
import org.systemsbiology.biotapestry.util.TextEditorTracker;
import org.systemsbiology.biotapestry.util.TextFinishedTracker;
import org.systemsbiology.biotapestry.util.UiUtil;


/****************************************************************************
**
** Dialog box for creating networks via interaction lists
*/

public class BuildNetworkDialogFactory extends DialogFactory {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BuildNetworkDialogFactory(ServerControlFlowHarness cfh) {
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
    List<EnumCellSign> signList = buildSignList();
    List<EnumCellGeneralSign> genSignList = buildGeneralSignList();
    List<EnumCellSignalType> sigTypes = buildSignalTypeList();
    Vector<String> showLevs = buildShowLevels(dniba.getGenome(), dniba.parentGenome);
    boolean doRegions = !dniba.getGenome().getID().equals(cfh.getDataAccessContext().getGenomeSource().getGenome().getID());
    Vector<String> compLevs = buildComplexityLevels(doRegions);
    
    switch(platform.getPlatform()) {
      case DESKTOP:
        return (new DesktopDialog(cfh, dniba.getGenome(), dniba.parentGenome, dniba.workingRegions, dniba.instruct, 
                                  signList, genSignList, sigTypes, showLevs, compLevs, doRegions));  
      case WEB:
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
  ** Build signal type list
  */

  private List<EnumCellSignalType> buildSignalTypeList() {    
    ResourceManager rMan = appState.getRMan();
    ArrayList<EnumCellSignalType> retval = new ArrayList<EnumCellSignalType>();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < SignalBuildInstruction.NUM_SIGNAL_TYPES; i++) {
      buf.setLength(0);
      buf.append("buildInstruction.");
      buf.append(SignalBuildInstruction.mapToSignalTypeTag(i));
      String display = rMan.getString(buf.toString());
      retval.add(new EnumCellSignalType(display, Integer.toString(i), i));
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Build link sign list
  */
   
  private List<EnumCellSign> buildSignList() {
    ResourceManager rMan = appState.getRMan();
    ArrayList<EnumCellSign> retval = new ArrayList<EnumCellSign>();
    StringBuffer buf = new StringBuffer();
    int index = 0;
    for (int i = 0; i < GeneralBuildInstruction.NUM_SIGN_TYPES; i++) {
      if (i != GeneralBuildInstruction.NEUTRAL) {
        buf.setLength(0);
        buf.append("buildInstruction.");
        buf.append(GeneralBuildInstruction.mapToSignTag(i));
        String display = rMan.getString(buf.toString());
        retval.add(new EnumCellSign(display, Integer.toString(i), index++));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build general link sign list
  */
   
  private List<EnumCellGeneralSign> buildGeneralSignList() {
    ResourceManager rMan = appState.getRMan();
    ArrayList<EnumCellGeneralSign> retval = new ArrayList<EnumCellGeneralSign>();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < GeneralBuildInstruction.NUM_SIGN_TYPES; i++) {
      buf.setLength(0);
      buf.append("buildInstruction.general");
      buf.append(GeneralBuildInstruction.mapToSignTag(i));
      String display = rMan.getString(buf.toString());
      retval.add(new EnumCellGeneralSign(display, Integer.toString(i), i));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the show level combo
  ** 
  */
 
  private Vector<String> buildShowLevels(Genome genome, Genome vfgParent) {  
    ResourceManager rMan = appState.getRMan();
    Vector<String> levels = new Vector<String>();
    levels.add(rMan.getString("buildNetwork.showLevelShowAll"));
    levels.add(rMan.getString("buildNetwork.showLevelShowSelected"));
    levels.add(rMan.getString("buildNetwork.showLevelShowNew")); 
    if (vfgParent != null) {
      levels.add(rMan.getString("buildNetwork.showLevelShowParents"));        
    }      
    return (levels);
  }
  
  /***************************************************************************
  **
  ** Build the complexity combo
  ** 
  */
 
  private Vector<String> buildComplexityLevels(boolean doRegions) {
    ResourceManager rMan = appState.getRMan();
    Vector<String> levels = new Vector<String>();
    levels.add(rMan.getString((doRegions) ? "buildNetwork.chooseSimple" : "buildNetwork.chooseSimpleRoot"));
    levels.add(rMan.getString((doRegions) ? "buildNetwork.chooseMedium" : "buildNetwork.chooseMediumRoot"));
    if (doRegions) {
      levels.add(rMan.getString("buildNetwork.chooseComplex"));
    }
    return (levels);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    Genome parentGenome;
    List<InstanceInstructionSet.RegionInfo> workingRegions; 
    List<BuildInstruction> instruct;
          
    public BuildArgs(Genome genome, Genome parentGenome, 
                     List<InstanceInstructionSet.RegionInfo> workingRegions, List<BuildInstruction> instruct) {
      super(genome);
      this.parentGenome = parentGenome;
      this.workingRegions = workingRegions; 
      this.instruct = instruct;
    }

    public Genome getGenome() {
      return (genome);
    }
  }
   
  /****************************************************************************
  **
  ** Dialog box for dialog-driven network building
  */
  
  public static class DesktopDialog extends JDialog implements DesktopDialogPlatform.Dialog {
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE CONSTANTS
    //
    //////////////////////////////////////////////////////////////////////////// 
  
    //
    // Different levels of display:
    //
  
    private static final int SHOW_ALL_        = 0;
    private static final int SHOW_SELECTED_   = 1;
    private static final int SHOW_NEW_        = 2;
    private static final int SHOW_PARENTS_    = 3;  // only for subset models  
  
    //
    // Complexity levels:
    //
    
    private final static int ONLY_GENES_INTRA_REGION_ = 0;
    private final static int ALL_TYPES_INTRA_REGION_  = 1;
    private final static int ALL_TYPES_INTER_REGION_  = 2;    
     
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private List<InstanceInstructionSet.RegionInfo> workingRegions_;
    private List<EnumCellSign> signList_;
    private List<EnumCellGeneralSign> generalSignList_;  
    private List<EnumCellNodeType> nodeTypeList_;
    private int geneTypeIndex_;
    private List<EnumCellSignalType> signalTypeList_;
    private List<EnumCellRegionType> regionList_;
    private MultiChoiceEditor.ChoiceEntrySet regionSet_;
    private MultiChoiceEditorTracker mceTracker_;
    private MultiChoiceEditorTracker mceTrackerMed_;  
    private MultiChoiceEditorTracker mceTrackerLn_;
    private TabData td_;
    private TabData mtd_; 
    private TabData ctd_;
    private TabData currTd_;
    private TabData std_;
    private TabData ltd_;
    private TabData[] allCurrTabData_;  
    private List<EnumCellNodeType> dummyList_;
    private FixedJButton buttonA_; 
    private FixedJButton buttonO_;
    private FixedJButton buttonC_;
    private FixedJButton buttonParam_;
    private JComboBox showLevelCombo_;
    private JComboBox complexityCombo_;
    private int complexityLevel_;
    private int showLevel_;
    private JTabbedPane tabPane_;
    private JPanel simplePanel_;
    private JPanel mediumPanel_;
    private JPanel complexPanel_;
    private JComboBox layoutCombo_;
    private JComboBox layoutChoiceCombo_;
    private boolean doRegions_;
    private String parentID_;  
    private String genomeID_;
    private HashMap<String, EnumCellNodeType> typeTracker_;
    private EnumCellNodeType geneEnumType_;
    private BTState appState_;
    private DataAccessContext dacx_;
    private JLabel lccLabel_;
    private SpecialtyLayoutEngineParams holdParams_;
    
    private ServerControlFlowHarness scfh_;
    private ClientControlFlowHarness cfh_;
    private RegionInfoRequest request_;
    
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
    
     DesktopDialog(ServerControlFlowHarness cfh, Genome genome, Genome parentGenome,
                   List<InstanceInstructionSet.RegionInfo> workingRegions, List<BuildInstruction> instruct,  
                   List<EnumCellSign> signList, List<EnumCellGeneralSign> genSignList, 
                   List<EnumCellSignalType> sigTypes, Vector<String> showLevs, Vector<String> compLevs, 
                   boolean doRegions) {
          
      super(cfh.getBTState().getTopFrame(), cfh.getBTState().getRMan().getString("buildNetwork.title"), true);
      scfh_ = cfh;
      cfh_ = cfh.getClientHarness();
      appState_ = cfh.getBTState();
      dacx_ = cfh.getDataAccessContext();
      ResourceManager rMan = appState_.getRMan();
      typeTracker_ = new HashMap<String, EnumCellNodeType>();
      genomeID_ = genome.getID();
      doRegions_ = doRegions;
      showLevel_ = (doRegions_) ? SHOW_ALL_ : SHOW_SELECTED_;
      workingRegions_ = workingRegions;
      
      request_ = new RegionInfoRequest();
      request_.haveResult = false;
  
      setSize(1050, 500);
      JPanel cp = (JPanel)getContentPane();
      cp.setBorder(new EmptyBorder(20, 20, 20, 20));
      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      
      signList_ = signList;
      generalSignList_ = genSignList;
      nodeTypeList_ = new ArrayList<EnumCellNodeType>();
      geneTypeIndex_ = buildNodeTypeList(nodeTypeList_);
      geneEnumType_ = nodeTypeList_.get(geneTypeIndex_);
      signalTypeList_ = sigTypes;
      dummyList_ = buildDummyList(nodeTypeList_);
      if (doRegions_) {
        regionSet_ = buildRegionSet(workingRegions_);
        regionList_ = buildRegionList(workingRegions_);
        parentID_ = (parentGenome == null) ? null : parentGenome.getID();
      }
    
      int rowNum = 0;
  
      
      //
      // Let the user know what is going on:
      //
      
      String form = (!doRegions_) ? rMan.getString("buildNetwork.rootModel")
                                  : rMan.getString("buildNetwork.subModel");
      String desc = MessageFormat.format(form, new Object[] {genome.getName()});
      JLabel modelLabel = new JLabel(desc, JLabel.CENTER);
      modelLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
      UiUtil.gbcSet(gbc, 0, rowNum++, 10, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      cp.add(modelLabel, gbc); 
      
      //
      // Build the regionEdit button and the root include button:
      //
      
      if (doRegions_) {
        FixedJButton regionButton = new FixedJButton(rMan.getString("buildNetwork.editRegions"));
        regionButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing();
              boolean doInherit = false;
              ArrayList<InstanceInstructionSet.RegionInfo> parentList = null;
              if (parentID_ != null) {
                Database db = appState_.getDB();
                InstanceInstructionSet piis = db.getInstanceInstructionSet(parentID_);
                parentList = new ArrayList<InstanceInstructionSet.RegionInfo>();
                if (piis != null) {
                  Iterator<InstanceInstructionSet.RegionInfo> prit = piis.getRegionIterator();  
                  while (prit.hasNext()) {
                    parentList.add(prit.next());
                  }
                }
                if (!parentList.isEmpty()) {
                  ResourceManager rMan = appState_.getRMan();
                  int result = JOptionPane.showOptionDialog(appState_.getTopFrame(), rMan.getString("buildNetwork.chooseRegEdit"),
                                                                     rMan.getString("buildNetwork.chooseRegEditTitle"),
                                                                     JOptionPane.DEFAULT_OPTION, 
                                                                     JOptionPane.QUESTION_MESSAGE, 
                                                                     null, new Object[] {
                                                                       rMan.getString("buildNetwork.doInherit"),
                                                                       rMan.getString("buildNetwork.doNew"),
                                                                       rMan.getString("dialogs.cancel"),
                                                                     }, rMan.getString("buildNetwork.doInherit"));
                  if (result == 2) {
                    return;
                  }
                  doInherit = (result == 0);
                }
              }
              
              if (doInherit) {
                RegionInheritDialogFactory.BuildArgs ba = 
                  new RegionInheritDialogFactory.BuildArgs(appState_.getDB().getGenome(genomeID_), parentList, workingRegions_);
                RegionInheritDialogFactory mddf = new RegionInheritDialogFactory(scfh_);
                RegionInheritDialogFactory.DesktopDialog rsd = mddf.getStashDialog(ba);
                rsd.setVisible(true);
                if (!rsd.haveResult()) {
                  return;
                }
                updateRegions(rsd.getRegions(), true);
              } else {
                RegionSetupDialogFactory.BuildArgs ba = 
                  new RegionSetupDialogFactory.BuildArgs(appState_.getDB().getGenome(genomeID_), workingRegions_);
                RegionSetupDialogFactory mddf = new RegionSetupDialogFactory(scfh_);
                RegionSetupDialogFactory.DesktopDialog rsd = mddf.getStashDialog(ba);
                rsd.setVisible(true);
                if (!rsd.haveResult()) {
                  return;
                }
                updateRegions(rsd.getRegions(), false);
              }
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
          }
        });
        
        UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);    
        cp.add(regionButton, gbc);
  
        //
        // Build the combo for the show level:
        //
  
        JLabel showLabel = new JLabel(rMan.getString("buildNetwork.showLevels"), JLabel.RIGHT);
        showLevelCombo_ = new JComboBox(showLevs);
        showLevelCombo_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              stopTheEditing();
              setShowLevel(showLevelCombo_.getSelectedIndex());
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
          }
        });
        
  
        UiUtil.gbcSet(gbc, 1, rowNum, 8, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);    
        cp.add(showLabel, gbc);    
        UiUtil.gbcSet(gbc, 9, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);    
        cp.add(showLevelCombo_, gbc);
      } else {
        rowNum++;
      }
      
      
      JPanel tablePanel = new JPanel();
      tablePanel.setBorder(new EtchedBorder());
      tablePanel.setLayout(new GridBagLayout());
      
      //
      // Build the complexity combo:
      //
      
      complexityLevel_ = requiredComplexity(instruct);
  
      JLabel complexLabel = new JLabel(rMan.getString("buildNetwork.chooseComplexity"), JLabel.RIGHT);
     
      complexityCombo_ = new JComboBox(compLevs);
      complexityCombo_.setSelectedIndex(complexityLevel_);
      complexityCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
            changeComplexity(complexityCombo_.getSelectedIndex());
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });

      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);    
      tablePanel.add(complexLabel, gbc);    
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);    
      tablePanel.add(complexityCombo_, gbc);    
      
      //
      // Build the tabs.
      //
         
      td_ = new TabData();
      mtd_ = new TabData();
      ctd_ = new TabData();
       
      tabPane_ = new JTabbedPane();    
      if (complexityLevel_ == ONLY_GENES_INTRA_REGION_) {
        simplePanel_ = buildSimpleTab(appState_.getTopFrame(), doRegions_, instruct);
        tabPane_.addTab(rMan.getString("buildNetwork.standard"), simplePanel_);
        currTd_ = td_;
      } else if (complexityLevel_ == ALL_TYPES_INTRA_REGION_) {
        mediumPanel_ = buildMediumTab(appState_.getTopFrame(), doRegions_, instruct);
        tabPane_.addTab(rMan.getString("buildNetwork.standard"), mediumPanel_);
        currTd_ = mtd_;
      } else {
        complexPanel_ = buildComplexTab(appState_.getTopFrame(), doRegions_, instruct);
        tabPane_.addTab(rMan.getString("buildNetwork.standard"), complexPanel_);
        currTd_ = ctd_;
      }
      
      allCurrTabData_ = new TabData[3];
      allCurrTabData_[0] = currTd_;
      
      std_ = new TabData();
      ltd_ = new TabData();
      allCurrTabData_[1] = std_;
      allCurrTabData_[2] = ltd_;
  
      tabPane_.addTab(rMan.getString("buildNetwork.signals"), buildSignalTab(appState_.getTopFrame(), doRegions_, instruct));
      tabPane_.addTab(rMan.getString("buildNetwork.loneNodes"), buildLoneNodeTab(appState_.getTopFrame(), doRegions_, instruct));
  
      UiUtil.gbcSet(gbc, 0, 1, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      tablePanel.add(tabPane_, gbc);
      rowNum += 8;
      
      UiUtil.gbcSet(gbc, 0, rowNum, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
      cp.add(tablePanel, gbc);
      rowNum += 8;
      
      tabPane_.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {
            stopTheEditing();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });         
  
      //
      // Build the layout option:
      //
  
      JPanel layoutOptions = new JPanel();
      layoutOptions.setLayout(new GridBagLayout());   
      JLabel label = new JLabel(rMan.getString("buildNetwork.layoutOption"));
      Vector<String> layouts = new Vector<String>();
      layouts.add(rMan.getString("buildNetwork.keepLayout"));
      layouts.add(rMan.getString("buildNetwork.freshLayout"));
      layoutCombo_ = new JComboBox(layouts);
      
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);    
      layoutOptions.add(label, gbc);    
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);    
      layoutOptions.add(layoutCombo_, gbc);
      layoutCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
            boolean enableOps = (layoutCombo_.getSelectedIndex() != 0);
            layoutChoiceCombo_.setEnabled(enableOps);
            lccLabel_.setEnabled(enableOps);
            buttonParam_.setEnabled(enableOps);
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      }); 
      
      Vector<EnumChoiceContent<SpecialtyLayoutEngine.SpecialtyType>> layoutChoices = SpecialtyLayoutEngine.SpecialtyType.getChoices(appState_, true);    
      layoutChoiceCombo_ = new JComboBox(layoutChoices);        
      lccLabel_ = new JLabel(rMan.getString("buildNetwork.layoutChoice"));     
      UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);    
      layoutOptions.add(lccLabel_, gbc);    
      UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);    
      layoutOptions.add(layoutChoiceCombo_, gbc);
      layoutChoiceCombo_.setEnabled(false);
      lccLabel_.setEnabled(false);
      layoutChoiceCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      }); 
   
      //
      // Build the layout parameters button:
      //
      
      buttonParam_ = new FixedJButton(rMan.getString("buildNetwork.getParams"));
      buttonParam_.setEnabled(false);
      buttonParam_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
            getLOParamsFromUser();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      UiUtil.gbcSet(gbc, 4, 0, 6, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);    
      layoutOptions.add(buttonParam_, gbc); 
      UiUtil.gbcSet(gbc, 0, rowNum++, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
      cp.add(layoutOptions, gbc);    
  
      //
      // Button Panel:
      //
     
      buttonA_ = new FixedJButton(rMan.getString("dialogs.apply"));
      buttonA_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
            if (stashResults(true, true)) {
              disableControls();
              cfh_.sendUserInputs(request_);
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });     
      buttonO_ = new FixedJButton(rMan.getString("dialogs.ok"));
      buttonO_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stopTheEditing();
            if (stashResults(true, false)) {
              disableControls();
              cfh_.sendUserInputs(request_);
              DesktopDialog.this.setVisible(false);
              DesktopDialog.this.dispose();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });     
      buttonC_ = new FixedJButton(rMan.getString("dialogs.close"));
      buttonC_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            stashResults(false, false);
            DesktopDialog.this.setVisible(false);
            DesktopDialog.this.dispose();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      Box buttonPanel = Box.createHorizontalBox();    
      buttonPanel.add(Box.createHorizontalGlue()); 
      buttonPanel.add(buttonA_);
      buttonPanel.add(Box.createHorizontalStrut(10));    
      buttonPanel.add(buttonO_);
      buttonPanel.add(Box.createHorizontalStrut(10));    
      buttonPanel.add(buttonC_);
   
      //
      // Build the dialog:
      //
      UiUtil.gbcSet(gbc, 0, rowNum, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      cp.add(buttonPanel, gbc);
      setLocationRelativeTo(appState_.getTopFrame());
      // This is already done in the building process
      //displayProperties(doRegions_, instruct);
        
      //
      // Start display of an inhabited table, if there is one:
      
      int selIndex = 0;
      for (int i = 0; i < 3; i++) {
        if (allCurrTabData_[i].tm.getRowCount() > 0) {
          selIndex = i;
          break;
        }
      }
      tabPane_.setSelectedIndex(selIndex);    
    }
    
    /***************************************************************************
    **
    ** Disable the dialog controls
    */ 
    
    public void disableControls() {       
      if (showLevelCombo_ != null) {
        showLevelCombo_.setEnabled(false);
      }
      complexityCombo_.setEnabled(false);
      layoutCombo_.setEnabled(false);
      buttonA_.setEnabled(false);
      buttonO_.setEnabled(false);
      buttonC_.setEnabled(false);
      buttonParam_.setEnabled(false);
      for (int i = 0; i < 3; i++) {
        disableTabControls(allCurrTabData_[i]);
      }
      getContentPane().validate();
      return;
    }  
    
    /***************************************************************************
    **
    ** Reenable the controls
    */ 
    
    public void enableControls() {
      if (showLevelCombo_ != null) {
        showLevelCombo_.setEnabled(true);
      }
      complexityCombo_.setEnabled(true);    
      layoutCombo_.setEnabled(true);
      buttonA_.setEnabled(true);
      buttonO_.setEnabled(true);
      buttonC_.setEnabled(true);
      buttonParam_.setEnabled(true);
      for (int i = 0; i < 3; i++) {
        enableTabControls(allCurrTabData_[i]);
      }
      getContentPane().validate();
      return;
    }
    
    /***************************************************************************
    **
    ** Disable the dialog controls
    */ 
    
    private void disableTabControls(TabData td) {
      td.jt.setEnabled(false);
      td.baPush = td.tableButtonA.isEnabled();
      td.bdPush = td.tableButtonD.isEnabled();
      td.brPush = td.tableButtonR.isEnabled();
      td.blPush = td.tableButtonL.isEnabled();
      td.tableButtonA.setEnabled(false);    
      td.tableButtonD.setEnabled(false);
      td.tableButtonR.setEnabled(false);
      td.tableButtonL.setEnabled(false);    
      return;
    }  
    
    /***************************************************************************
    **
    ** Reenable the controls
    */ 
    
    private void enableTabControls(TabData td) {
      td.jt.setEnabled(true);
      td.tableButtonA.setEnabled(td.baPush);    
      td.tableButtonD.setEnabled(td.bdPush);
      td.tableButtonR.setEnabled(td.brPush);
      td.tableButtonL.setEnabled(td.blPush);  
      return;
    }  
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Get Layout params via quebomb
    ** 
    */
    
    private void getLOParamsFromUser() {
  
      SpecialtyLayoutEngine.SpecialtyType layoutType = ((EnumChoiceContent<SpecialtyLayoutEngine.SpecialtyType>)layoutChoiceCombo_.getSelectedItem()).val;     
      RemoteRequest daBomb = new RemoteRequest("queBombLayoutValues"); 
      daBomb.setObjectArg("strategy", layoutType);
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      SpecialtyLayoutEngineParamDialogFactory.BuildArgs ba = (SpecialtyLayoutEngineParamDialogFactory.BuildArgs)dbres.getObjAnswer("specLoParams");
      SpecialtyLayoutEngineParamDialogFactory cedf = new SpecialtyLayoutEngineParamDialogFactory(scfh_);
      SpecialtyLayoutEngineParamDialogFactory.DesktopDialog dd = cedf.getStashDialog(ba); 
      dd.setVisible(true);
      if (dd.haveResult()) {
        holdParams_ = dd.getParams();
      }
      return;     
    }
    
    /***************************************************************************
    **
    ** Change the table complexity setting.
    ** 
    */
    
    private boolean changeComplexity(int desiredLevel) {
      
      if (desiredLevel == this.complexityLevel_) {
        return (true);
      }
      
      //
      // If the instructions we want to shift have errors, we cannot continue:
      //
      
      
      HashMap<String, EnumCellNodeType> types = new HashMap<String, EnumCellNodeType>();
      if (!currTd_.tm.checkValues(types, true)) {
        return (false);
      }
      
      //
      // If we get instance mismatches, we cannot continue:
      //
      
      List<BuildInstruction> buildCmds = currTd_.tm.applyValues(true);
      RemoteRequest daBomb = new RemoteRequest("queBombInstanceMismatch");
      daBomb.setObjectArg("buildCmds", buildCmds);
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      List<BuildInstructionProcessor.MatchChecker> mismatch = (List<BuildInstructionProcessor.MatchChecker>)dbres.getObjAnswer("instMismatch");
      if (mismatch.size() > 0) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("buildNetwork.cannotReduceComplexity");
        String title = rMan.getString("buildNetwork.cannotReduceComplexityTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
  
      //
      // If the complexity we are heading for is too low, we cannot continue:
      //
      
      int needComplexity = requiredComplexity(buildCmds);
      if (desiredLevel < needComplexity) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("buildNetwork.cannotReduceComplexity");
        String title = rMan.getString("buildNetwork.cannotReduceComplexityTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
      
      //
      // Clean up before reset:
      //
   
      if (complexityLevel_ == ONLY_GENES_INTRA_REGION_) {
        finishSimpleTabEditing(false);
      } else if (complexityLevel_ == ALL_TYPES_INTRA_REGION_) {
        finishMediumTabEditing(false);
      } else {
        finishComplexTabEditing(false);
      }
      // Dump instructions:
      currTd_.tm.extractValues(doRegions_, new ArrayList<BuildInstruction>());
  
      //
      // We are good to go.  Create the target panel if we need to; else
      // install the new instructions.
      //
      
      JPanel desiredPanel;
      if (desiredLevel == ONLY_GENES_INTRA_REGION_) {
        if (simplePanel_ == null) {
          simplePanel_ = buildSimpleTab(appState_.getTopFrame(), doRegions_, buildCmds);
        } else {
          td_.tm.extractValues(doRegions_, buildCmds);
        }
        if (doRegions_) {
          td_.tm.setShowLevel(showLevel_);
        }
        desiredPanel = simplePanel_;
        currTd_ = td_;
      } else if (desiredLevel == ALL_TYPES_INTRA_REGION_) {
        if (mediumPanel_ == null) {
          mediumPanel_ = buildMediumTab(appState_.getTopFrame(), doRegions_, buildCmds);
        } else {
          mtd_.tm.extractValues(doRegions_, buildCmds);
        }
        if (doRegions_) {
          mtd_.tm.setShowLevel(showLevel_);
        }
        desiredPanel = mediumPanel_;
        currTd_ = mtd_;
      } else {
        if (complexPanel_ == null) {
          complexPanel_ = buildComplexTab(appState_.getTopFrame(), doRegions_, buildCmds);
        } else {
          ctd_.tm.extractValues(doRegions_, buildCmds);
        }
        if (doRegions_) {
          ctd_.tm.setShowLevel(showLevel_);
        }
        desiredPanel = complexPanel_;
        currTd_ = ctd_;
      }  
  
      
      int selIndex = tabPane_.getSelectedIndex();
      allCurrTabData_[0] = currTd_;
      complexityLevel_ = desiredLevel;
      ResourceManager rMan = appState_.getRMan();
      int index = tabPane_.indexOfTab(rMan.getString("buildNetwork.standard"));
      tabPane_.removeTabAt(index);    
      tabPane_.insertTab(rMan.getString("buildNetwork.standard"), null, desiredPanel, null, index);
      tabPane_.setSelectedIndex(selIndex);
      currTd_.jt.revalidate(); // needed??
      tabPane_.revalidate();
      currTd_.jt.repaint(); // needed??
      tabPane_.repaint();
      return (true);
    }
    
    /***************************************************************************
    **
    ** Build the simple tab
    */ 
    
    private JPanel buildSimpleTab(JFrame parent, boolean doRegions, List<BuildInstruction> instruct) {       
      ResourceManager rMan = appState_.getRMan();
      GridBagConstraints gbc = new GridBagConstraints();
         
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
  
      td_.tm = new GeneTableModel(parent, doRegions, instruct, typeTracker_);   
      td_.jt = new JTable(td_.tm);
      td_.jt.getTableHeader().setReorderingAllowed(false);    
      td_.selectedRows = new int[0];
      
      td_.tracker = new SelectionTracker(td_);
      ListSelectionModel lsm = td_.jt.getSelectionModel();
      lsm.addListSelectionListener(td_.tracker);
      
      //
      // Set the specialty editors and renderers:
      //
          
      TextEditor textEdit = new TextEditor(appState_);
      new TextEditorTracker(textEdit, td_.tm, td_.tm, appState_);
      td_.jt.setDefaultEditor(String.class, textEdit);
      UiUtil.installDefaultCellRendererForPlatform(td_.jt, String.class, true, appState_);
      
      SignEditor signEdit = new SignEditor(signList_);
      new ComboBoxEditorTracker(signEdit, td_.tm, appState_);
      td_.jt.setDefaultEditor(EnumCellSign.class, signEdit); 
      td_.jt.setDefaultRenderer(EnumCellSign.class, new NoEditRenderer(signList_));
      
      if (doRegions) {
        MultiChoiceEditor regionEdit = new MultiChoiceEditor(regionSet_, appState_);
        mceTracker_ = new MultiChoiceEditorTracker(regionEdit, td_.tm, appState_);
        td_.jt.setDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class, regionEdit); 
        td_.jt.setDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class, new MultiChoiceRenderer(regionSet_));   
      }        
      
      UiUtil.platformTableRowHeight(td_.jt, true);
      JScrollPane jsp = new JScrollPane(td_.jt);
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
      
      //
      // Build the buttons:
      //
      
      td_.tableButtonA = new FixedJButton(rMan.getString("dialogs.addEntry"));
  
      td_.tableButtonA.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSimpleTabEditing(false);
            if (td_.tm.addRow()) {
              td_.jt.revalidate();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      td_.tableButtonD = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
      td_.tableButtonD.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (doRegions_ && losingAnInstance(td_, td_.selectedRows, allCurrTabData_) && !confirmRowDeletion()) {
              return;
            }
            finishSimpleTabEditing(true);
            td_.tm.deleteRows(td_.selectedRows);
            if ((td_.selectedRows != null) && 
                (td_.selectedRows[td_.selectedRows.length - 1] >= td_.jt.getRowCount())) {
              td_.jt.clearSelection();  // needed to deactivate delete button
            }
            td_.tracker.setButtonState();
            td_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      td_.tableButtonD.setEnabled(false);
      
      td_.tableButtonL = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
      td_.tableButtonL.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSimpleTabEditing(false);
            td_.tm.bumpRowDown(td_.selectedRows);
            td_.selectedRows[0]++;
            td_.jt.getSelectionModel().setSelectionInterval(td_.selectedRows[0], td_.selectedRows[0]);
            td_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      td_.tableButtonL.setEnabled(false);    
              
      td_.tableButtonR = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
      td_.tableButtonR.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSimpleTabEditing(false);   
            td_.tm.bumpRowUp(td_.selectedRows);
            td_.selectedRows[0]--;
            td_.jt.getSelectionModel().setSelectionInterval(td_.selectedRows[0], td_.selectedRows[0]);
            td_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      td_.tableButtonR.setEnabled(false);    
   
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      tableButtonPanel.add(td_.tableButtonA);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(td_.tableButtonD);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(td_.tableButtonR);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(td_.tableButtonL);    
      tableButtonPanel.add(Box.createHorizontalGlue());
  
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 10, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
      
      return (retval);
    }
   
    /***************************************************************************
    **
    ** Stop all gene tab editing
    */ 
    
    private void finishSimpleTabEditing(boolean doCancel) {       
      if (td_.jt == null) { // Not built yet
        return;
      }
      if (doCancel) {
        ((TextEditor)td_.jt.getDefaultEditor(String.class)).cancelCellEditing();
        ((SignEditor)td_.jt.getDefaultEditor(EnumCellSign.class)).cancelCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)td_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).cancelCellEditing();
        }
      } else {
        ((TextEditor)td_.jt.getDefaultEditor(String.class)).stopCellEditing();
        ((SignEditor)td_.jt.getDefaultEditor(EnumCellSign.class)).stopCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)td_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).stopCellEditing();
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Handle changes in regions
    ** 
    */
    
    private void updateSimpleRegions(int showChoice) {
      if (td_.jt == null) { // Not built yet
        return;
      }
      finishSimpleTabEditing(false);   
      td_.jt.getSelectionModel().clearSelection();      
      MultiChoiceEditor mce = (MultiChoiceEditor)td_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class);  
      mce.replaceValues(regionSet_);
      mceTracker_.updateListenerStatus();
      ((MultiChoiceRenderer)td_.jt.getDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class)).replaceValues(regionSet_);       
      td_.tm.dropObsoleteRegions();
      //
      // Region change can affect displayed instructions:
      //
      if (currTd_ == td_) {
        td_.tm.setShowLevel(showChoice);     
        td_.jt.revalidate();
        td_.jt.repaint();
      }
      return;
    }  
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setSimpleShowLevel(int showChoice) {
      if (td_.jt == null) { // Not built yet
        return;
      }
      finishSimpleTabEditing(false);
      td_.jt.getSelectionModel().clearSelection();   
      boolean canAdd = (showChoice == SHOW_ALL_) || (showChoice == SHOW_NEW_);
      td_.tableButtonA.setEnabled(canAdd); 
      // show or hide root instructions
      if (currTd_ == td_) {
        td_.tm.setShowLevel(showChoice);
        td_.jt.revalidate();
        td_.jt.repaint();
      }
      return;
    }    
   
    /***************************************************************************
    **
    ** Build the medium complexity tab
    */ 
    
    private JPanel buildMediumTab(JFrame parent, boolean doRegions, List<BuildInstruction> instruct) {       
      ResourceManager rMan = appState_.getRMan();
      GridBagConstraints gbc = new GridBagConstraints();
         
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
  
      mtd_.tm = new MediumTableModel(parent, doRegions, instruct, typeTracker_);   
      mtd_.jt = new JTable(mtd_.tm);
      mtd_.jt.getTableHeader().setReorderingAllowed(false);    
      mtd_.selectedRows = new int[0];
      
      mtd_.tracker = new SelectionTracker(mtd_);
      ListSelectionModel lsm = mtd_.jt.getSelectionModel();
      lsm.addListSelectionListener(mtd_.tracker);
      
      //
      // Set the specialty editors and renderers:
      //
      
      NodeTypeEditor nodeTypeEdit = new NodeTypeEditor(nodeTypeList_);
      new ComboBoxEditorTracker(nodeTypeEdit, mtd_.tm, (ComboFinishedTracker)mtd_.tm, appState_);
      mtd_.jt.setDefaultEditor(EnumCellNodeType.class, nodeTypeEdit); 
      mtd_.jt.setDefaultRenderer(EnumCellNodeType.class, new NoEditRenderer(nodeTypeList_));
      
      TextEditor textEdit = new TextEditor(appState_);
      new TextEditorTracker(textEdit, mtd_.tm, mtd_.tm, appState_);
      mtd_.jt.setDefaultEditor(String.class, textEdit);
      UiUtil.installDefaultCellRendererForPlatform(mtd_.jt, String.class, true, appState_);
      
      GeneralSignEditor signEdit = new GeneralSignEditor(generalSignList_);
      new ComboBoxEditorTracker(signEdit, mtd_.tm, appState_);
      mtd_.jt.setDefaultEditor(EnumCellGeneralSign.class, signEdit); 
      mtd_.jt.setDefaultRenderer(EnumCellGeneralSign.class, new NoEditRenderer(generalSignList_));    
      
      if (doRegions) {
        MultiChoiceEditor regionEdit = new MultiChoiceEditor(regionSet_, appState_);
        mceTrackerMed_ = new MultiChoiceEditorTracker(regionEdit, mtd_.tm, appState_);
        mtd_.jt.setDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class, regionEdit); 
        mtd_.jt.setDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class, new MultiChoiceRenderer(regionSet_));   
      }        
      
      UiUtil.platformTableRowHeight(mtd_.jt, true);
      JScrollPane jsp = new JScrollPane(mtd_.jt);
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
      
      //
      // Build the buttons:
      //
      
      mtd_.tableButtonA = new FixedJButton(rMan.getString("dialogs.addEntry"));
  
      mtd_.tableButtonA.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishMediumTabEditing(false);
            if (mtd_.tm.addRow()) {
              mtd_.jt.revalidate();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      mtd_.tableButtonD = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
      mtd_.tableButtonD.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (doRegions_ && losingAnInstance(mtd_, mtd_.selectedRows, allCurrTabData_) && !confirmRowDeletion()) {
              return;
            }
            finishMediumTabEditing(true);
            mtd_.tm.deleteRows(mtd_.selectedRows);
            if ((mtd_.selectedRows != null) && 
                (mtd_.selectedRows[mtd_.selectedRows.length - 1] >= mtd_.jt.getRowCount())) {
              mtd_.jt.clearSelection();  // needed to deactivate delete button
            }
            mtd_.tracker.setButtonState();
            mtd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      mtd_.tableButtonD.setEnabled(false);
      
      mtd_.tableButtonL = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
      mtd_.tableButtonL.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishMediumTabEditing(false);
            mtd_.tm.bumpRowDown(mtd_.selectedRows);
            mtd_.selectedRows[0]++;
            mtd_.jt.getSelectionModel().setSelectionInterval(mtd_.selectedRows[0], mtd_.selectedRows[0]);
            mtd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      mtd_.tableButtonL.setEnabled(false);    
              
      mtd_.tableButtonR = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
      mtd_.tableButtonR.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishMediumTabEditing(false);   
            mtd_.tm.bumpRowUp(mtd_.selectedRows);
            mtd_.selectedRows[0]--;
            mtd_.jt.getSelectionModel().setSelectionInterval(mtd_.selectedRows[0], mtd_.selectedRows[0]);
            mtd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      mtd_.tableButtonR.setEnabled(false);    
   
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      tableButtonPanel.add(mtd_.tableButtonA);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(mtd_.tableButtonD);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(mtd_.tableButtonR);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(mtd_.tableButtonL);    
      tableButtonPanel.add(Box.createHorizontalGlue());
  
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 10, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
      
      return (retval);
    }
  
    /***************************************************************************
    **
    ** Handle changes in regions
    ** 
    */
    
    private void updateMediumRegions(int showChoice) {
      if (mtd_.jt == null) { // Not built yet
        return;
      }    
      finishMediumTabEditing(false);
      mtd_.jt.getSelectionModel().clearSelection();           
      MultiChoiceEditor mce = (MultiChoiceEditor)mtd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class);               
      mce.replaceValues(regionSet_);
      mceTrackerMed_.updateListenerStatus();
      ((MultiChoiceRenderer)mtd_.jt.getDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class)).replaceValues(regionSet_);   
      mtd_.tm.dropObsoleteRegions();
      //
      // Region change can affect displayed instructions:
      //
      if (currTd_ == mtd_) {
        mtd_.tm.setShowLevel(showChoice);
        mtd_.jt.revalidate();
        mtd_.jt.repaint();
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setMediumShowLevel(int showChoice) {
      if (mtd_.jt == null) { // Not built yet
        return;
      }
      finishMediumTabEditing(false);
      mtd_.jt.getSelectionModel().clearSelection();   
      boolean canAdd = (showChoice == SHOW_ALL_) || (showChoice == SHOW_NEW_);
      mtd_.tableButtonA.setEnabled(canAdd); 
      // show or hide root instructions
      if (currTd_ == mtd_) {
        mtd_.tm.setShowLevel(showChoice);
        mtd_.jt.revalidate();
        mtd_.jt.repaint();
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Stop all medium tab editing
    */ 
    
    private void finishMediumTabEditing(boolean doCancel) {
      if (mtd_.jt == null) { // Not built yet
        return;
      }
      if (doCancel) {
        ((TextEditor)mtd_.jt.getDefaultEditor(String.class)).cancelCellEditing();
        ((GeneralSignEditor)mtd_.jt.getDefaultEditor(EnumCellGeneralSign.class)).cancelCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)mtd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).cancelCellEditing();
        }
      } else {
        ((TextEditor)mtd_.jt.getDefaultEditor(String.class)).stopCellEditing();
        ((GeneralSignEditor)mtd_.jt.getDefaultEditor(EnumCellGeneralSign.class)).stopCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)mtd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).stopCellEditing();
        }
      }
      return;
    }  
  
    /***************************************************************************
    **
    ** Build the signal tab
    */ 
    
    private JPanel buildSignalTab(JFrame parent, boolean doRegions, List<BuildInstruction> instruct) {       
      ResourceManager rMan = appState_.getRMan(); 
      GridBagConstraints gbc = new GridBagConstraints();
      
      //
      // Build the values table.
      //
         
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
      
      std_.tm = new SignalTableModel(parent, doRegions, instruct, typeTracker_);   
      std_.jt = new JTable(std_.tm);
      std_.jt.getTableHeader().setReorderingAllowed(false);
      int viewColumn = std_.jt.convertColumnIndexToView(1);
      TableColumn tfCol = std_.jt.getColumnModel().getColumn(viewColumn);
      viewColumn = std_.jt.convertColumnIndexToView(3);
      TableColumn tyCol = std_.jt.getColumnModel().getColumn(viewColumn);    
      tfCol.setMinWidth(200);
      tfCol.setPreferredWidth(200);
      tfCol.setMaxWidth(500);
      tyCol.setMinWidth(230);
      tyCol.setPreferredWidth(230);
      tyCol.setMaxWidth(500);    
      std_.tracker = new SelectionTracker(std_);
      ListSelectionModel lsm = std_.jt.getSelectionModel();
      lsm.addListSelectionListener(std_.tracker);
      
      //
      // Set the specialty editors and renderers:
      //
         
      TextEditor textEdit = new TextEditor(appState_);
      new TextEditorTracker(textEdit, std_.tm, std_.tm, appState_);
      std_.jt.setDefaultEditor(String.class, textEdit);
      UiUtil.installDefaultCellRendererForPlatform(std_.jt, String.class, true, appState_);
      
      SignalTypeEditor signalEdit = new SignalTypeEditor(signalTypeList_);
      new ComboBoxEditorTracker(signalEdit, std_.tm, appState_);
      std_.jt.setDefaultEditor(EnumCellSignalType.class, signalEdit); 
      std_.jt.setDefaultRenderer(EnumCellSignalType.class, new NoEditRenderer(signalTypeList_)); 
      
      if (doRegions) {
        RegionEditor regionEdit = new RegionEditor(regionList_);
        new ComboBoxEditorTracker(regionEdit, std_.tm, appState_);
        std_.jt.setDefaultEditor(EnumCellRegionType.class, regionEdit); 
        std_.jt.setDefaultRenderer(EnumCellRegionType.class, new NoEditRenderer(regionList_));    
      }
      
      UiUtil.platformTableRowHeight(std_.jt, true);
      JScrollPane jsp = new JScrollPane(std_.jt);
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
      
      //
      // Build the buttons:
      //
      
      std_.tableButtonA = new FixedJButton(rMan.getString("dialogs.addEntry"));
      std_.tableButtonA.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSignalTabEditing(false);
            if (std_.tm.addRow()) {
              std_.jt.revalidate();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      std_.tableButtonD = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
      std_.tableButtonD.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (doRegions_ && losingAnInstance(std_, std_.selectedRows, allCurrTabData_) && !confirmRowDeletion()) {
              return;
            }  
            finishSignalTabEditing(true);
            std_.tm.deleteRows(std_.selectedRows);
            if ((std_.selectedRows != null) && 
                (std_.selectedRows[std_.selectedRows.length - 1] >= std_.jt.getRowCount())) {
              std_.jt.clearSelection();  // needed to deactivate delete button
            }
            std_.tracker.setButtonState();
            std_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      std_.tableButtonD.setEnabled(false);
      
      std_.tableButtonL = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
      std_.tableButtonL.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSignalTabEditing(false);
            std_.tm.bumpRowDown(std_.selectedRows);
            std_.selectedRows[0]++;
            std_.jt.getSelectionModel().setSelectionInterval(std_.selectedRows[0], std_.selectedRows[0]);
            std_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      std_.tableButtonL.setEnabled(false);    
              
      std_.tableButtonR = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
      std_.tableButtonR.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishSignalTabEditing(false);
            std_.tm.bumpRowUp(std_.selectedRows);
            std_.selectedRows[0]--;
            std_.jt.getSelectionModel().setSelectionInterval(std_.selectedRows[0], std_.selectedRows[0]);
            std_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      std_.tableButtonR.setEnabled(false);    
  
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      tableButtonPanel.add(std_.tableButtonA);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(std_.tableButtonD);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(std_.tableButtonR);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(std_.tableButtonL);        
      tableButtonPanel.add(Box.createHorizontalGlue());
  
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 10, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
      
      return (retval);
    } 
  
    /***************************************************************************
    **
    ** Stop signal tab editing
    */ 
    
    private void finishSignalTabEditing(boolean doCancel) {
      
      if (doCancel) {
        ((TextEditor)std_.jt.getDefaultEditor(String.class)).cancelCellEditing();
        ((SignalTypeEditor)std_.jt.getDefaultEditor(EnumCellSignalType.class)).cancelCellEditing();
        if (doRegions_) {
          ((RegionEditor)std_.jt.getDefaultEditor(EnumCellRegionType.class)).cancelCellEditing();
        }
      } else {
        ((TextEditor)std_.jt.getDefaultEditor(String.class)).stopCellEditing();
        ((SignalTypeEditor)std_.jt.getDefaultEditor(EnumCellSignalType.class)).stopCellEditing();
        if (doRegions_) {
          ((RegionEditor)std_.jt.getDefaultEditor(EnumCellRegionType.class)).stopCellEditing();
        }
      } 
      return;
    } 
    
    /***************************************************************************
    **
    ** Handle changes in regions
    ** 
    */
    
    private void updateSignalRegions(int showChoice) {
      finishSignalTabEditing(false);
      std_.jt.getSelectionModel().clearSelection();
      RegionEditor res = (RegionEditor)std_.jt.getDefaultEditor(EnumCellRegionType.class);    
      res.replaceValues(regionList_);      
      ((NoEditRenderer)std_.jt.getDefaultRenderer(EnumCellRegionType.class)).replaceValues(regionList_);
      std_.tm.dropObsoleteRegions();
      //
      // Region change can affect displayed instructions:
      //
      std_.tm.setShowLevel(showChoice);
      std_.jt.revalidate();
      std_.jt.repaint();
      return;
    }  
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setSignalShowLevel(int showChoice) {
      finishSignalTabEditing(false);
      std_.jt.getSelectionModel().clearSelection();
      boolean canAdd = (showChoice == SHOW_ALL_) || (showChoice == SHOW_NEW_);
      std_.tableButtonA.setEnabled(canAdd);  
      // show or hide root instructions
      std_.tm.setShowLevel(showChoice);
      std_.jt.revalidate();
      std_.jt.repaint();
      return;
    }    
  
    /***************************************************************************
    **
    ** Build the complex tab
    */ 
    
    private JPanel buildComplexTab(JFrame parent, boolean doRegions, List<BuildInstruction> instruct) {       
      ResourceManager rMan = appState_.getRMan();
      GridBagConstraints gbc = new GridBagConstraints();
      
      //
      // Build the values table.
      //
         
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
  
      ctd_.tm = new GeneralTableModel(parent, doRegions, instruct, typeTracker_);   
      ctd_.jt = new JTable(ctd_.tm);
      ctd_.jt.getTableHeader().setReorderingAllowed(false);    
      ctd_.tracker = new SelectionTracker(ctd_);
      ListSelectionModel lsm = ctd_.jt.getSelectionModel();
      lsm.addListSelectionListener(ctd_.tracker);
  
      //
      // Set the specialty editors and renderers:
      //
      
      NodeTypeEditor nodeTypeEdit = new NodeTypeEditor(nodeTypeList_);
      new ComboBoxEditorTracker(nodeTypeEdit, ctd_.tm, (ComboFinishedTracker)ctd_.tm, appState_);
      ctd_.jt.setDefaultEditor(EnumCellNodeType.class, nodeTypeEdit); 
      ctd_.jt.setDefaultRenderer(EnumCellNodeType.class, new NoEditRenderer(nodeTypeList_));
      
      TextEditor textEdit = new TextEditor(appState_);
      new TextEditorTracker(textEdit, ctd_.tm, ctd_.tm, appState_);
      ctd_.jt.setDefaultEditor(String.class, textEdit);
      UiUtil.installDefaultCellRendererForPlatform(ctd_.jt, String.class, true, appState_);
      
      GeneralSignEditor signEdit = new GeneralSignEditor(generalSignList_);
      new ComboBoxEditorTracker(signEdit, ctd_.tm, appState_);
      ctd_.jt.setDefaultEditor(EnumCellGeneralSign.class, signEdit); 
      ctd_.jt.setDefaultRenderer(EnumCellGeneralSign.class, new NoEditRenderer(generalSignList_));
      
      if (doRegions) {
        RegionEditor regionEdit = new RegionEditor(regionList_);
        new ComboBoxEditorTracker(regionEdit, ctd_.tm, appState_);
        ctd_.jt.setDefaultEditor(EnumCellRegionType.class, regionEdit); 
        ctd_.jt.setDefaultRenderer(EnumCellRegionType.class, new NoEditRenderer(regionList_));    
      }    
        
      UiUtil.platformTableRowHeight(ctd_.jt, true);
      JScrollPane jsp = new JScrollPane(ctd_.jt);
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
      
      //
      // Build the buttons:
      //
      
      ctd_.tableButtonA = new FixedJButton(rMan.getString("dialogs.addEntry"));
      ctd_.tableButtonA.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishComplexTabEditing(false);
            if (ctd_.tm.addRow()) {
              ctd_.jt.revalidate();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      ctd_.tableButtonD = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
      ctd_.tableButtonD.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (doRegions_ && losingAnInstance(ctd_, ctd_.selectedRows, allCurrTabData_) && !confirmRowDeletion()) {
              return;
            }   
            finishComplexTabEditing(true);
            ctd_.tm.deleteRows(ctd_.selectedRows);
            if ((ctd_.selectedRows != null) && 
                (ctd_.selectedRows[ctd_.selectedRows.length - 1] >= ctd_.jt.getRowCount())) {
              ctd_.jt.clearSelection();  // needed to deactivate delete button
            }
            ctd_.tracker.setButtonState();
            ctd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ctd_.tableButtonD.setEnabled(false);
      
      ctd_.tableButtonL = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
      ctd_.tableButtonL.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishComplexTabEditing(false);
            ctd_.tm.bumpRowDown(ctd_.selectedRows);
            ctd_.selectedRows[0]++;
            ctd_.jt.getSelectionModel().setSelectionInterval(ctd_.selectedRows[0], ctd_.selectedRows[0]);
            ctd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ctd_.tableButtonL.setEnabled(false);    
              
      ctd_.tableButtonR = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
      ctd_.tableButtonR.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishComplexTabEditing(false);
            ctd_.tm.bumpRowUp(ctd_.selectedRows);
            ctd_.selectedRows[0]--;
            ctd_.jt.getSelectionModel().setSelectionInterval(ctd_.selectedRows[0], ctd_.selectedRows[0]);
            ctd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ctd_.tableButtonR.setEnabled(false);    
  
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      tableButtonPanel.add(ctd_.tableButtonA);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ctd_.tableButtonD);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ctd_.tableButtonR);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ctd_.tableButtonL);        
      tableButtonPanel.add(Box.createHorizontalGlue());
  
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 10, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
      
      return (retval);
    } 
  
    /***************************************************************************
    **
    ** Handle changes in regions
    ** 
    */
    
    private void updateComplexRegions(int showChoice) {
      if (ctd_.jt == null) { // Not built yet
        return;
      }    
      finishComplexTabEditing(false);
      ctd_.jt.getSelectionModel().clearSelection();
      RegionEditor reg = (RegionEditor)ctd_.jt.getDefaultEditor(EnumCellRegionType.class);    
      reg.replaceValues(regionList_);
      ((NoEditRenderer)ctd_.jt.getDefaultRenderer(EnumCellRegionType.class)).replaceValues(regionList_);    
      ctd_.tm.dropObsoleteRegions();
      // Region change can affect displayed instructions:
      if (currTd_ == ctd_) {
        ctd_.tm.setShowLevel(showChoice);
        ctd_.jt.revalidate();
        ctd_.jt.repaint();  
      }
      return;
    }  
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setComplexShowLevel(int showChoice) {
      if (ctd_.jt == null) { // Not built yet
        return;
      }    
      finishComplexTabEditing(false);
      ctd_.jt.getSelectionModel().clearSelection(); 
      boolean canAdd = (showChoice == SHOW_ALL_) || (showChoice == SHOW_NEW_); 
      ctd_.tableButtonA.setEnabled(canAdd);
      // show or hide root instructions
      if (currTd_ == ctd_) {
        ctd_.tm.setShowLevel(showChoice);
        ctd_.jt.revalidate();
        ctd_.jt.repaint();
      }
      return;
    }    
  
    /***************************************************************************
    **
    ** Stop complex tab editing
    */ 
    
    private void finishComplexTabEditing(boolean doCancel) {
      if (ctd_.jt == null) { // Not built yet
        return;
      }    
      if (doCancel) {
        ((TextEditor)ctd_.jt.getDefaultEditor(String.class)).cancelCellEditing();
        ((GeneralSignEditor)ctd_.jt.getDefaultEditor(EnumCellGeneralSign.class)).cancelCellEditing();   
        ((NodeTypeEditor)ctd_.jt.getDefaultEditor(EnumCellNodeType.class)).cancelCellEditing();
        if (doRegions_) {
          ((RegionEditor)ctd_.jt.getDefaultEditor(EnumCellRegionType.class)).cancelCellEditing();
        }
      } else {
        ((TextEditor)ctd_.jt.getDefaultEditor(String.class)).stopCellEditing();
        ((GeneralSignEditor)ctd_.jt.getDefaultEditor(EnumCellGeneralSign.class)).stopCellEditing();   
        ((NodeTypeEditor)ctd_.jt.getDefaultEditor(EnumCellNodeType.class)).stopCellEditing();
        if (doRegions_) {
          ((RegionEditor)ctd_.jt.getDefaultEditor(EnumCellRegionType.class)).stopCellEditing();
        }
      }
      return;
    }  
    
    /***************************************************************************
    **
    ** Build the lone node tab
    */ 
    
    private JPanel buildLoneNodeTab(JFrame parent, boolean doRegions, List<BuildInstruction> instruct) {       
      ResourceManager rMan = appState_.getRMan();
      GridBagConstraints gbc = new GridBagConstraints();
      
      //
      // Build the values table.
      //
         
      JPanel retval = new JPanel();
      retval.setLayout(new GridBagLayout());
  
      ltd_.tm = new LoneNodeTableModel(parent, doRegions, instruct, typeTracker_);   
      ltd_.jt = new JTable(ltd_.tm);
      ltd_.jt.getTableHeader().setReorderingAllowed(false);    
      ltd_.tracker = new SelectionTracker(ltd_);
      ListSelectionModel lsm = ltd_.jt.getSelectionModel();
      lsm.addListSelectionListener(ltd_.tracker);
  
      //
      // Set the specialty editors and renderers:
      //
      
      NodeTypeEditor nodeTypeEdit = new NodeTypeEditor(nodeTypeList_);
      new ComboBoxEditorTracker(nodeTypeEdit, ltd_.tm, (ComboFinishedTracker)ltd_.tm, appState_);
      ltd_.jt.setDefaultEditor(EnumCellNodeType.class, nodeTypeEdit); 
      ltd_.jt.setDefaultRenderer(EnumCellNodeType.class, new NoEditRenderer(nodeTypeList_));
      
      TextEditor textEdit = new TextEditor(appState_);
      new TextEditorTracker(textEdit, ltd_.tm, ltd_.tm, appState_);
      ltd_.jt.setDefaultEditor(String.class, textEdit);
      UiUtil.installDefaultCellRendererForPlatform(ltd_.jt, String.class, true, appState_);
  
      if (doRegions) {
        MultiChoiceEditor regionEdit = new MultiChoiceEditor(regionSet_, appState_);
        mceTrackerLn_ = new MultiChoiceEditorTracker(regionEdit, ltd_.tm, appState_);
        ltd_.jt.setDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class, regionEdit); 
        ltd_.jt.setDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class, new MultiChoiceRenderer(regionSet_));   
      }         
        
      UiUtil.platformTableRowHeight(ltd_.jt, true);
      JScrollPane jsp = new JScrollPane(ltd_.jt);
      UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      retval.add(jsp, gbc);
      
      //
      // Build the buttons:
      //
      
      ltd_.tableButtonA = new FixedJButton(rMan.getString("dialogs.addEntry"));
      ltd_.tableButtonA.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishLoneNodeTabEditing(false);
            if (ltd_.tm.addRow()) {
              ltd_.jt.revalidate();
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      
      ltd_.tableButtonD = new FixedJButton(rMan.getString("dialogs.deleteEntry"));
      ltd_.tableButtonD.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (doRegions_ && losingAnInstance(ltd_, ltd_.selectedRows, allCurrTabData_) && !confirmRowDeletion()) {
              return;
            }   
            finishLoneNodeTabEditing(true);
            ltd_.tm.deleteRows(ltd_.selectedRows);
            if ((ltd_.selectedRows != null) && 
                (ltd_.selectedRows[ltd_.selectedRows.length - 1] >= ltd_.jt.getRowCount())) {
              ltd_.jt.clearSelection();  // needed to deactivate delete button
            }
            ltd_.tracker.setButtonState();
            ltd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ltd_.tableButtonD.setEnabled(false);
      
      ltd_.tableButtonL = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
      ltd_.tableButtonL.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishLoneNodeTabEditing(false);
            ltd_.tm.bumpRowDown(ltd_.selectedRows);
            ltd_.selectedRows[0]++;
            ltd_.jt.getSelectionModel().setSelectionInterval(ltd_.selectedRows[0], ltd_.selectedRows[0]);
            ltd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ltd_.tableButtonL.setEnabled(false);    
              
      ltd_.tableButtonR = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
      ltd_.tableButtonR.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            finishLoneNodeTabEditing(false);
            ltd_.tm.bumpRowUp(ltd_.selectedRows);
            ltd_.selectedRows[0]--;
            ltd_.jt.getSelectionModel().setSelectionInterval(ltd_.selectedRows[0], ltd_.selectedRows[0]);
            ltd_.jt.revalidate();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
      ltd_.tableButtonR.setEnabled(false);    
  
      Box tableButtonPanel = Box.createHorizontalBox();
      tableButtonPanel.add(Box.createHorizontalGlue()); 
      tableButtonPanel.add(ltd_.tableButtonA);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ltd_.tableButtonD);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ltd_.tableButtonR);
      tableButtonPanel.add(Box.createHorizontalStrut(10));    
      tableButtonPanel.add(ltd_.tableButtonL);        
      tableButtonPanel.add(Box.createHorizontalGlue());
  
      //
      // Add the add/delete buttons
      //
      
      UiUtil.gbcSet(gbc, 0, 10, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
      retval.add(tableButtonPanel, gbc);
      
      return (retval);
    } 
  
    /***************************************************************************
    **
    ** Handle changes in regions
    ** 
    */
    
    private void updateLoneNodeRegions(int showChoice) {
      finishLoneNodeTabEditing(false);    
      ltd_.jt.getSelectionModel().clearSelection();           
      MultiChoiceEditor mceLn = (MultiChoiceEditor)ltd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class);
      mceLn.replaceValues(regionSet_);    
      mceTrackerLn_.updateListenerStatus();
      ((MultiChoiceRenderer)ltd_.jt.getDefaultRenderer(MultiChoiceEditor.ChoiceEntrySet.class)).replaceValues(regionSet_);
      ltd_.tm.dropObsoleteRegions();       
      //
      // Region change can affect displayed instructions:
      //
      ltd_.tm.setShowLevel(showChoice);    
      ltd_.jt.revalidate();    
      ltd_.jt.repaint();   
      return;
    }
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setLoneNodeShowLevel(int showChoice) {
      finishSimpleTabEditing(false);
      ltd_.jt.getSelectionModel().clearSelection();   
      boolean canAdd = (showChoice == SHOW_ALL_) || (showChoice == SHOW_NEW_);
      ltd_.tableButtonA.setEnabled(canAdd); 
      // show or hide root instructions
      ltd_.tm.setShowLevel(showChoice);
      ltd_.jt.revalidate();
      ltd_.jt.repaint();
      return;
    }
    
    /***************************************************************************
    **
    ** Stop lone node tab editing
    */ 
    
    private void finishLoneNodeTabEditing(boolean doCancel) {
      if (doCancel) {
        ((TextEditor)ltd_.jt.getDefaultEditor(String.class)).cancelCellEditing();
        ((NodeTypeEditor)ltd_.jt.getDefaultEditor(EnumCellNodeType.class)).cancelCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)ltd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).cancelCellEditing();
        }      
      } else {
        ((TextEditor)ltd_.jt.getDefaultEditor(String.class)).stopCellEditing();
        ((NodeTypeEditor)ltd_.jt.getDefaultEditor(EnumCellNodeType.class)).stopCellEditing();
        if (doRegions_) {
          ((MultiChoiceEditor)ltd_.jt.getDefaultEditor(MultiChoiceEditor.ChoiceEntrySet.class)).stopCellEditing();
        }      
      }
      return;
    }  
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Used for tracking tabs
    */
  
    class TabData {
      BuildTableModel tm;
      JTable jt;
      int[] selectedRows;
      FixedJButton tableButtonA;
      FixedJButton tableButtonD;
      FixedJButton tableButtonR;
      FixedJButton tableButtonL;
      boolean baPush;
      boolean bdPush;
      boolean brPush;
      boolean blPush;
      SelectionTracker tracker;
    }
    
   /***************************************************************************
    **
    ** Abstract base class for tables
    */
    
    abstract class BuildTableModel extends AbstractTableModel implements TextFinishedTracker {
      private static final long serialVersionUID = 1L;  
      protected ArrayList<Integer> rowMapping_;
      protected int rowCount_;
      protected ArrayList<String> hiddenIDColumn_;      
       
      abstract void deleteRows(int[] rows);
      abstract boolean addRow();
  
      //
      // Return the instances that are completely removed by removing the given rows
      //
      Set<String> removedInstances(int[] rows) {
        
        Set<String> retval = existingInstances();
        
        HashSet<Integer> keptRows = new HashSet<Integer>();
        int num = hiddenIDColumn_.size();
        for (int i = 0; i < num; i++) {
          keptRows.add(new Integer(i));
        }
  
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.get(rows[i]).intValue();
          keptRows.remove(new Integer(mapRow));
        }
        
        Set<String> remaining = new HashSet<String>();
        Iterator<Integer> krit = keptRows.iterator();
        while (krit.hasNext()) {
          Integer keptRow = krit.next();
          String hiddenID = hiddenIDColumn_.get(keptRow.intValue());
          if ((hiddenID != null) && (!hiddenID.trim().equals(""))) {
            remaining.add(hiddenID);
          }
        }
        
        retval.removeAll(remaining);
        return (retval);
      }
      
      Set<String> existingInstances() {
        //
        // See if these row removals completely remove an existing instance from the table
        //
        HashSet<String> retval = new HashSet<String>();
        int num = hiddenIDColumn_.size();
        
        for (int i = 0; i < num; i++) {
          String hiddenID = hiddenIDColumn_.get(i);
          if ((hiddenID != null) && (!hiddenID.trim().equals(""))) {
            retval.add(hiddenID);
          }
        }
        return (retval);
      }    
      
      void bumpRowUp(int[] rows) {
        if ((rows.length != 1) || (rows[0] == 0)) {
          throw new IllegalStateException();
        }
        
        int mappedTopIndex = rowMapping_.get(rows[0] - 1).intValue();
        int mappedBottomIndex = rowMapping_.get(rows[0]).intValue();
        
        swapRows(mappedTopIndex, mappedBottomIndex);           
        return;
      }
      
      void bumpRowDown(int[] rows) {
        if ((rows.length != 1) || (rows[0] == (rowMapping_.size() - 1))) {
          throw new IllegalStateException();
        }
        
        int mappedTopIndex = rowMapping_.get(rows[0]).intValue();
        int mappedBottomIndex = rowMapping_.get(rows[0] + 1).intValue();      
  
        swapRows(mappedTopIndex, mappedBottomIndex);
        return;
      }
      
      void setShowLevel(int level) {
        //
        // Turning root on->mapping is identity matrix.  Turning off, hide instructions
        // with an ID and no region choices:
        //
  
        if (level == SHOW_ALL_) {
          rowMapping_.clear();
          int size = hiddenIDColumn_.size();
          for (int i = 0; i < size; i++) {
            rowMapping_.add(new Integer(i));
          }
          rowCount_ = size;
        } else if (level == SHOW_NEW_) {
          rowMapping_.clear();
          int size = hiddenIDColumn_.size();
          for (int i = 0; i < size; i++) {
            String hiddenID = hiddenIDColumn_.get(i);
            if ((hiddenID == null) || hiddenID.trim().equals("")) {
              rowMapping_.add(new Integer(i));
            }
          }
          rowCount_ = rowMapping_.size();
        } else if (level == SHOW_PARENTS_) {          
          GenomeInstance myGenome = (GenomeInstance)appState_.getDB().getGenome(genomeID_);
          GenomeInstance vfgParent = myGenome.getVfgParent();
          if (vfgParent == null) {
            throw new IllegalStateException();
          } 
          BuildInstructionProcessor bip = new BuildInstructionProcessor(appState_);
          DataAccessContext dacxI = new DataAccessContext(dacx_, vfgParent);
          List<BuildInstruction> parentList = bip.getInstructions(dacxI);
          int plSize = parentList.size();
          rowMapping_.clear();
          int size = hiddenIDColumn_.size();
          for (int i = 0; i < size; i++) {
            String hiddenID = hiddenIDColumn_.get(i);
            for (int j = 0; j < plSize; j++) {
              BuildInstruction bi = parentList.get(j);
              if ((hiddenID != null) && hiddenID.equals(bi.getID()) && isInheritable(bi)) {
                rowMapping_.add(new Integer(i));
                break;
              }
            }
          }
          rowCount_ = rowMapping_.size();
        } else {
          if (level != SHOW_SELECTED_) {
            throw new IllegalStateException();
          }
          rowMapping_.clear();
          int size = hiddenIDColumn_.size();
          for (int i = 0; i < size; i++) {
            if (rowHasSelectedRegion(i)) {
              rowMapping_.add(new Integer(i));
            }
          }
          rowCount_ = rowMapping_.size();
        }   
        return;
      }    
  
      abstract boolean checkValues(Map<String, EnumCellNodeType> types, boolean allowDoubleBlanks);    
      abstract void extractValues(boolean doRegions, List<BuildInstruction> instruct);    
      abstract List<BuildInstruction> applyValues(boolean dumpBlanks);
      abstract boolean myBuildInstruction(BuildInstruction bi);
      abstract void dropObsoleteRegions();
      abstract boolean rowHasSelectedRegion(int row);
      abstract void swapRows(int mappedTopIndex, int mappedBottomIndex);
      abstract public boolean textEditingDone(int col, int row, Object val);
      abstract int hasNameInRows(String nodeName);
      abstract boolean canChangeType(String nodeName, EnumCellNodeType newType);
      abstract void batchTypeChange(String nodeName, EnumCellNodeType newType);  
    }
    
 
    /***************************************************************************
    **
    ** Used for the gene table
    */
    
    class GeneTableModel extends BuildTableModel {
      private static final long serialVersionUID = 1L;
      private int colCount_;
      private ArrayList<String> inputColumn_;
      private ArrayList<EnumCellSign> signColumn_;  
      private ArrayList<String> targetColumn_; 
      private ArrayList<MultiChoiceEditor.ChoiceEntrySet> regionColumn_;
      private Map<String, EnumCellNodeType> myTypeTracker_;
  
      private JFrame parent_;
  
      GeneTableModel(JFrame parent, boolean doRegions, List<BuildInstruction> instruct, Map<String, EnumCellNodeType> nodeTypeTracker) {
        parent_ = parent;
        myTypeTracker_ = nodeTypeTracker;
        extractValues(doRegions, instruct); 
      }
  
      public boolean textEditingDone(int col, int row, Object val) {
        if ((val == null) || ((String)val).trim().equals("")) {
          return (true);
        }
        EnumCellNodeType existing = findExistingType((String)val, myTypeTracker_);
        if (existing == null) {
          myTypeTracker_.put(DataUtil.normKey((String)val), new EnumCellNodeType(geneEnumType_));
          
        } else if (!existing.internal.equals(Integer.toString(Node.GENE))) {
          cannotUseEnteredName((String)val, existing);
          return (false);
        }
        return (true);
      }
  
      public int hasNameInRows(String nodeName) {
        int retval = 0;
        int size = inputColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String sname = inputColumn_.get(i);
          String tname = targetColumn_.get(i);        
          if (normName.equals(DataUtil.normKey(sname)) || normName.equals(DataUtil.normKey(tname))) {
            retval++;
          }
        }      
        return (retval);
      }
      
      public boolean canChangeType(String nodeName, EnumCellNodeType newType) {
        if (hasNameInRows(nodeName) == 0) {
          return (true);
        }
        return (newType.value == Node.GENE);
      }
  
      public void batchTypeChange(String nodeName, EnumCellNodeType newType) {
        if (!canChangeType(nodeName, newType)) {
          throw new IllegalStateException();
        }
        // Nothing to do...
        return;
      }           
      
      void deleteRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.remove(rows[i]).intValue();
          hiddenIDColumn_.remove(mapRow);        
          inputColumn_.remove(mapRow);
          signColumn_.remove(mapRow);
          targetColumn_.remove(mapRow);
          if (regionColumn_ != null) {
            regionColumn_.remove(mapRow);
          }
  
          for (int j = i + 1; j < rows.length; j++) {
            if (rows[j] > rows[i]) {
              rows[j]--;
            }
          }
          rowCount_--;
          
          for (int j = rows[i]; j < rowCount_; j++) {
            int currVal = rowMapping_.get(j).intValue();
            rowMapping_.set(j, new Integer(currVal - 1));
          }           
        }
        return; 
      } 
      
      boolean addRow() {
        rowMapping_.add(rowCount_, new Integer(hiddenIDColumn_.size()));      
        hiddenIDColumn_.add(""); // IMPORTANT! must be set by instruction processor!        
        inputColumn_.add("");
        signColumn_.add(signList_.get(0));
        targetColumn_.add("");
        if (regionColumn_ != null) {
          regionColumn_.add(new MultiChoiceEditor.ChoiceEntrySet(regionSet_));
        }
        rowCount_++;
        return (true);
      }
      
      void swapRows(int mappedTopIndex, int mappedBottomIndex) {
        
        String remObj = hiddenIDColumn_.get(mappedTopIndex);
        hiddenIDColumn_.set(mappedTopIndex, hiddenIDColumn_.get(mappedBottomIndex));
        hiddenIDColumn_.set(mappedBottomIndex, remObj);
  
        remObj = inputColumn_.get(mappedTopIndex);
        inputColumn_.set(mappedTopIndex, inputColumn_.get(mappedBottomIndex));
        inputColumn_.set(mappedBottomIndex, remObj);
        
        EnumCellSign remObjECS = signColumn_.get(mappedTopIndex);
        signColumn_.set(mappedTopIndex, signColumn_.get(mappedBottomIndex));
        signColumn_.set(mappedBottomIndex, remObjECS);
        
        remObj = targetColumn_.get(mappedTopIndex);
        targetColumn_.set(mappedTopIndex, targetColumn_.get(mappedBottomIndex));
        targetColumn_.set(mappedBottomIndex, remObj);
  
        if (regionColumn_ != null) {
          MultiChoiceEditor.ChoiceEntrySet remObjCES = regionColumn_.get(mappedTopIndex);
          regionColumn_.set(mappedTopIndex, regionColumn_.get(mappedBottomIndex));
          regionColumn_.set(mappedBottomIndex, remObjCES);      
        }
        
        return;
      }    
  
      public int getRowCount() {
        return (rowCount_); 
      }  
      
      public int getColumnCount() {
        return (colCount_);
      }
      
      public String getColumnName(int c) {
        try {
          ResourceManager rMan = appState_.getRMan();
          switch (c) {
            case 0:
              return (rMan.getString("buildNetwork.inputGene"));
            case 1:
              return (rMan.getString("buildNetwork.sign"));
            case 2:
              return (rMan.getString("buildNetwork.targetGene"));
            case 3:
              if (regionColumn_ != null) {
                return (rMan.getString("buildNetwork.regionChoice"));            
              } // want drop thru
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
  
      public Class<? extends Object> getColumnClass(int c) {
        try {
          switch (c) {
            case 0:
              return (String.class);            
            case 1:
              return (EnumCellSign.class);
            case 2:
              return (String.class);
            case 3:
              if (regionColumn_ != null) {
                return (MultiChoiceEditor.ChoiceEntrySet.class);            
              } // want drop thru            
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public Object getValueAt(int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {
            case 0:
              return (inputColumn_.get(mapRow));            
            case 1:
              return (signColumn_.get(mapRow));
            case 2:
              return (targetColumn_.get(mapRow));
            case 3:
              if (regionColumn_ != null) {
                return (regionColumn_.get(mapRow));            
              } // want drop thru              
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public void setValueAt(Object value, int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) { 
            case 0:          
              String currInp = inputColumn_.get(mapRow);
              if ((currInp == null) || (!currInp.equals(value))) {
                inputColumn_.set(mapRow, (String)value);
              }  
              break;          
            case 1:
              EnumCell currSign = signColumn_.get(mapRow);
              if (!currSign.internal.equals(((EnumCellSign)value).internal)) {
                signColumn_.set(mapRow, (EnumCellSign)value);
              }  
              break;
            case 2:
              String currTarg = targetColumn_.get(mapRow);
              if ((currTarg == null) || (!currTarg.equals(value))) {
                targetColumn_.set(mapRow, (String)value);
              }  
              break;
            case 3:
              if (regionColumn_ != null) {
                MultiChoiceEditor.ChoiceEntrySet currReg = regionColumn_.get(mapRow);
                if ((currReg == null) || (!currReg.equals(value))) {
                  regionColumn_.set(mapRow, (MultiChoiceEditor.ChoiceEntrySet)value);
                }
              } else {
                throw new IllegalArgumentException();
              }
              break;                   
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      } 
      
      public boolean isCellEditable(int r, int c) {
        return (true);
      }
      
      boolean checkValues(Map<String, EnumCellNodeType> nodeTypes, boolean allowDoubleBlanks) {
        //
        // The source and target nodes must be named, and
        // all instances of a node must have the same node type:
        //
        
        int size = inputColumn_.size();
        for (int i = 0; i < size; i++) {        
          String ecs = inputColumn_.get(i);
          String ect = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));                
          boolean srcBlank = ((ecs == null) || ecs.trim().equals(""));
          boolean trgBlank = ((ect == null) || ect.trim().equals(""));
          if (allowDoubleBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }
          if (srcBlank || trgBlank) {          
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, 
                                          rMan.getString("buildNetwork.cannotBeBlank"), 
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          String inconDesc = checkMismatch(ecs, dummyList_, 0, nodeTypes);
          if (inconDesc == null) {
            inconDesc = checkMismatch(ect, dummyList_, 0, nodeTypes);
          }
          if (inconDesc != null) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, inconDesc,
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
        }
        return (true);
      }
  
      boolean myBuildInstruction(BuildInstruction bi) {
        if (!(bi instanceof GeneralBuildInstruction)) {
          return (false);
        }
        GeneralBuildInstruction gbi = (GeneralBuildInstruction)bi;
        if (!((gbi.getSourceType() == Node.GENE) && 
              (gbi.getTargType() == Node.GENE) &&
              (gbi.getLinkSign() != GeneralBuildInstruction.NEUTRAL))) {
          return (false);
        }
        
        if (!gbi.hasRegions()) {
          return (true);
        }
        
        Iterator<InstructionRegions.RegionTuple> tupit = gbi.getRegions().getRegionTuples();
        while (tupit.hasNext()) {
          InstructionRegions.RegionTuple tuple = tupit.next();             
          if (!tuple.sourceRegion.equals(tuple.targetRegion)) {
            return (false);
          }   
        }
        return (true);
      }
      
      void dropObsoleteRegions() {
        int rcolSize = regionColumn_.size();
        for (int i = 0; i < rcolSize; i++) {
          MultiChoiceEditor.ChoiceEntrySet oldCes = regionColumn_.get(i);
          MultiChoiceEditor.ChoiceEntrySet newCes = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
          regionColumn_.set(i, newCes);
          int oldCSize = oldCes.values.length;
          int newCSize = newCes.values.length;        
          for (int j = 0; j < newCSize; j++) {
            MultiChoiceEditor.ChoiceEntry newCe = newCes.values[j];
            for (int k = 0; k < oldCSize; k++) {
              MultiChoiceEditor.ChoiceEntry oldCe = oldCes.values[k];
              if (oldCe.internal.equals(newCe.internal)) {
                if (oldCe.selected) {
                  newCe.selected = true;
                }
                break;
              }
            }
          }
        }
        return;
      }
      
      boolean rowHasSelectedRegion(int row) {
        MultiChoiceEditor.ChoiceEntrySet ces = regionColumn_.get(row);
        int cSize = ces.values.length;
        for (int j = 0; j < cSize; j++) {
          MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
          if (ce.selected) {
            return (true);
          }
        }
        return (false);
      }
     
      void extractValues(boolean doRegions, List<BuildInstruction> instruct) {
        rowMapping_ = new ArrayList<Integer>();
        hiddenIDColumn_ = new ArrayList<String>();
        inputColumn_ = new ArrayList<String>();
        signColumn_ = new ArrayList<EnumCellSign>();
        targetColumn_ = new ArrayList<String>();
        if (doRegions) {
          regionColumn_ = new ArrayList<MultiChoiceEditor.ChoiceEntrySet>();
          colCount_ = 4;
        } else {
          colCount_ = 3;
        }
        rowCount_ = 0;   
        
        Iterator<BuildInstruction> biit = instruct.iterator();
        while (biit.hasNext()) { 
          BuildInstruction bi = biit.next();
          if (!myBuildInstruction(bi)) {
            continue;
          }
          GeneralBuildInstruction gbi = (GeneralBuildInstruction)bi;
          // Hidden column
          hiddenIDColumn_.add(bi.getID());
          // Col 0
          inputColumn_.add(gbi.getSourceName());
          // Col 1
          int ssize = signList_.size();
          for (int i = 0; i < ssize; i++) {
            EnumCellSign ecs = signList_.get(i);
            if (ecs.internal.equals(Integer.toString(gbi.getLinkSign()))) {
              signColumn_.add(new EnumCellSign(ecs));
              break;
            }
          }        
          // Col 2
          targetColumn_.add(gbi.getTargName());
          
          // Col 3
          
          if (regionColumn_ != null) {
            MultiChoiceEditor.ChoiceEntrySet ces = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
            int cSize = ces.values.length; 
            Iterator<InstructionRegions.RegionTuple> tupit = gbi.getRegions().getRegionTuples();
            while (tupit.hasNext()) {
              InstructionRegions.RegionTuple tuple = tupit.next();             
              for (int j = 0; j < cSize; j++) {
                MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
                if (ce.internal.equals(tuple.sourceRegion) &&
                    ce.internal.equals(tuple.targetRegion)) {
                  ce.selected = true;
                  break;
                }
              }
            } 
            regionColumn_.add(ces);
          }
          
          initTypeMap(myTypeTracker_, gbi.getSourceName(), gbi.getSourceType());
          initTypeMap(myTypeTracker_, gbi.getTargName(), gbi.getTargType());        
          
          rowMapping_.add(rowCount_, new Integer(rowCount_));
          
          rowCount_++;
        }
        return;
      }
      
      List<BuildInstruction> applyValues(boolean dumpBlanks) {
        ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
  
        int size = inputColumn_.size();      
        for (int i = 0; i < size; i++) {
          String sourceName = inputColumn_.get(i);
          String targName = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));          
          boolean srcBlank = ((sourceName == null) || sourceName.trim().equals(""));
          boolean trgBlank = ((targName == null) || targName.trim().equals(""));
          if (dumpBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }        
          int linkSign = extractIntValue(((EnumCell)signColumn_.get(i)).internal).intValue();        
          
          GeneralBuildInstruction gbi = 
            new GeneralBuildInstruction(id, Node.GENE, sourceName, linkSign, Node.GENE, targName);
          if (regionColumn_ != null) {
            InstructionRegions ir = new InstructionRegions();
            MultiChoiceEditor.ChoiceEntrySet ces = regionColumn_.get(i);    
            int cSize = ces.values.length;  
            for (int j = 0; j < cSize; j++) {
              MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
              if (ce.selected) {
                InstructionRegions.RegionTuple tup = 
                  new InstructionRegions.RegionTuple(ce.internal, ce.internal);              
                ir.addRegionTuple(tup);
              }
            }
            gbi.setRegions(ir);
          }
          retval.add(gbi);
        }
        return (retval);
      }
    }
  
    /***************************************************************************
    **
    ** Used for the gene table
    */
    
    class MediumTableModel extends BuildTableModel implements ComboFinishedTracker {
      
      private static final long serialVersionUID = 1L;
      
      private int colCount_;
      private ArrayList<EnumCellNodeType> srcTypeColumn_;    
      private ArrayList<String> sourceColumn_;
      private ArrayList<EnumCellGeneralSign> linkSignColumn_;
      private ArrayList<EnumCellNodeType> trgTypeColumn_;    
      private ArrayList<String> targetColumn_;
      private ArrayList<MultiChoiceEditor.ChoiceEntrySet> regionColumn_;
      private Map<String, EnumCellNodeType> myTypeTracker_;
  
      private JFrame parent_;
  
      MediumTableModel(JFrame parent, boolean doRegions, List<BuildInstruction> instruct, Map<String, EnumCellNodeType> nodeTypeTracker) {
        parent_ = parent;
        myTypeTracker_ = nodeTypeTracker;
        extractValues(doRegions, instruct); 
      }
      
      public boolean comboEditingDone(int col, int row, Object val) {
        int mapRow = rowMapping_.get(row).intValue();
        EnumCellNodeType newType = (EnumCellNodeType)val;
        List<EnumCellNodeType> typeList; 
        List<String> nameList;       
        if (col == 1) {
          typeList = srcTypeColumn_;
          nameList = sourceColumn_;
        } else if (col == 4) {
          typeList = trgTypeColumn_;
          nameList = targetColumn_;
        } else {
          throw new IllegalArgumentException();
        }
        return (comboEditingDoneSupport(nameList, typeList, mapRow, myTypeTracker_, newType));    
      }    
  
      public boolean textEditingDone(int col, int row, Object val) {
        if ((val == null) || ((String)val).trim().equals("")) {
          return (true);
        }
        EnumCellNodeType existing = findExistingType((String)val, myTypeTracker_);
        int mapRow = rowMapping_.get(row).intValue();
        ArrayList<EnumCellNodeType> useList;
        if (col == 0) {
          useList = srcTypeColumn_;
        } else if (col == 3) {
          useList = trgTypeColumn_;
        } else {
          throw new IllegalArgumentException();
        }
        if (existing == null) {
          EnumCellNodeType newType = useList.get(mapRow);
          myTypeTracker_.put(DataUtil.normKey((String)val), newType);
        } else {
          EnumCell currStype = useList.get(mapRow);
          if (!currStype.internal.equals(existing.internal)) {
            useList.set(mapRow, existing);
            mtd_.jt.revalidate();
            mtd_.jt.repaint();
          }
        }
        return (true);
      }
  
      public int hasNameInRows(String nodeName) {
        int retval = 0;
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String sname = sourceColumn_.get(i);
          String tname = targetColumn_.get(i);
          if (normName.equals(DataUtil.normKey(sname)) || normName.equals(DataUtil.normKey(tname))) {
            retval++;
          }
        }      
        return (retval);
      }
      
      public boolean canChangeType(String nodeName, EnumCellNodeType newType) {
        // We can always change types
        return (true);
      }
  
      public void batchTypeChange(String nodeName, EnumCellNodeType newType) {
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ecs))) {
            srcTypeColumn_.set(i, new EnumCellNodeType(newType));
          }
          String ect = targetColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ect))) {
            trgTypeColumn_.set(i, new EnumCellNodeType(newType));
          }
        }      
        return;
      }        
      
      
      void deleteRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.remove(rows[i]).intValue();
          hiddenIDColumn_.remove(mapRow);        
          srcTypeColumn_.remove(mapRow);
          sourceColumn_.remove(mapRow);
          linkSignColumn_.remove(mapRow);
          trgTypeColumn_.remove(mapRow);    
          targetColumn_.remove(mapRow);
          if (regionColumn_ != null) {
            regionColumn_.remove(mapRow);
          }
  
          for (int j = i + 1; j < rows.length; j++) {
            if (rows[j] > rows[i]) {
              rows[j]--;
            }
          }
          rowCount_--;
          
          for (int j = rows[i]; j < rowCount_; j++) {
            int currVal = rowMapping_.get(j).intValue();
            rowMapping_.set(j, new Integer(currVal - 1));
          }           
        }
        return; 
      } 
      
      boolean addRow() {
        rowMapping_.add(rowCount_, new Integer(hiddenIDColumn_.size()));      
        hiddenIDColumn_.add(""); // IMPORTANT! must be set by instruction processor!        
        srcTypeColumn_.add(nodeTypeList_.get(geneTypeIndex_));
        sourceColumn_.add("");
        linkSignColumn_.add(generalSignList_.get(0));
        trgTypeColumn_.add(nodeTypeList_.get(geneTypeIndex_));
        targetColumn_.add("");
        if (regionColumn_ != null) {
          regionColumn_.add(new MultiChoiceEditor.ChoiceEntrySet(regionSet_));
        }
        rowCount_++;
        return (true);
      }
      
      void swapRows(int mappedTopIndex, int mappedBottomIndex) {
        
        String remObj = hiddenIDColumn_.get(mappedTopIndex);
        hiddenIDColumn_.set(mappedTopIndex, hiddenIDColumn_.get(mappedBottomIndex));
        hiddenIDColumn_.set(mappedBottomIndex, remObj);
  
        EnumCellNodeType remObjST = srcTypeColumn_.get(mappedTopIndex);
        srcTypeColumn_.set(mappedTopIndex, srcTypeColumn_.get(mappedBottomIndex));
        srcTypeColumn_.set(mappedBottomIndex, remObjST);
        
        String remObjS = sourceColumn_.get(mappedTopIndex);
        sourceColumn_.set(mappedTopIndex, sourceColumn_.get(mappedBottomIndex));
        sourceColumn_.set(mappedBottomIndex, remObjS);
        
        EnumCellGeneralSign remObjECS = linkSignColumn_.get(mappedTopIndex);
        linkSignColumn_.set(mappedTopIndex, linkSignColumn_.get(mappedBottomIndex));
        linkSignColumn_.set(mappedBottomIndex, remObjECS);
        
        EnumCellNodeType remObjTT = trgTypeColumn_.get(mappedTopIndex);
        trgTypeColumn_.set(mappedTopIndex, trgTypeColumn_.get(mappedBottomIndex));
        trgTypeColumn_.set(mappedBottomIndex, remObjTT);
        
        String remObjT = targetColumn_.get(mappedTopIndex);
        targetColumn_.set(mappedTopIndex, targetColumn_.get(mappedBottomIndex));
        targetColumn_.set(mappedBottomIndex, remObjT);      
  
        if (regionColumn_ != null) {
          MultiChoiceEditor.ChoiceEntrySet remObjCES = regionColumn_.get(mappedTopIndex);
          regionColumn_.set(mappedTopIndex, regionColumn_.get(mappedBottomIndex));
          regionColumn_.set(mappedBottomIndex, remObjCES);      
        }
        
        return;
      }    
  
      public int getRowCount() {
        return (rowCount_); 
      }  
      
      public int getColumnCount() {
        return (colCount_);
      }
      
      public String getColumnName(int c) {
        try {
          ResourceManager rMan = appState_.getRMan();
          switch (c) {
            case 0:
              return (rMan.getString("buildNetwork.input"));
            case 1:
              return (rMan.getString("buildNetwork.srcType")); 
            case 2:
              return (rMan.getString("buildNetwork.generalSign"));
            case 3:
              return (rMan.getString("buildNetwork.target"));
            case 4:
              return (rMan.getString("buildNetwork.trgType"));            
            case 5:
              if (regionColumn_ != null) {
                return (rMan.getString("buildNetwork.regionChoice"));            
              } // want drop thru
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
  
      public Class<? extends Object> getColumnClass(int c) {
        try {
          switch (c) {
            case 0:
              return (String.class);
            case 1:
              return (EnumCellNodeType.class);            
            case 2:
              return (EnumCellGeneralSign.class);
            case 3:
              return (String.class);
            case 4:
              return (EnumCellNodeType.class);               
            case 5:
              if (regionColumn_ != null) {
                return (MultiChoiceEditor.ChoiceEntrySet.class);            
              } // want drop thru            
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public Object getValueAt(int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {
            case 0:
              return (sourceColumn_.get(mapRow));
            case 1:
              return (srcTypeColumn_.get(mapRow));            
            case 2:
              return (linkSignColumn_.get(mapRow));
            case 3:
              return (targetColumn_.get(mapRow));
            case 4:
              return (trgTypeColumn_.get(mapRow));             
            case 5:
              if (regionColumn_ != null) {
                return (regionColumn_.get(mapRow));            
              } // want drop thru              
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public void setValueAt(Object value, int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) { 
            case 0:          
              String currSrc = sourceColumn_.get(mapRow);
              if ((currSrc == null) || (!currSrc.equals(value))) {
                sourceColumn_.set(mapRow, (String)value);
              }  
              break;
            case 1:          
              EnumCell currStype = srcTypeColumn_.get(mapRow);
              EnumCellNodeType valueT = (EnumCellNodeType)value;
              if (!currStype.internal.equals(valueT.internal)) {
                srcTypeColumn_.set(mapRow, valueT);
              }
              break;               
            case 2:
              EnumCell currSign = linkSignColumn_.get(mapRow);
              EnumCellGeneralSign valueTS = (EnumCellGeneralSign)value;
              if (!currSign.internal.equals(valueTS.internal)) {
                linkSignColumn_.set(mapRow, valueTS);
              }  
              break;
            case 3:
              String currTarg = targetColumn_.get(mapRow);
              if ((currTarg == null) || (!currTarg.equals(value))) {
                targetColumn_.set(mapRow, (String)value);
              }  
              break;
            case 4:
              EnumCell currTtype = trgTypeColumn_.get(mapRow);
              EnumCellNodeType valueTT = (EnumCellNodeType)value;
              if (!currTtype.internal.equals(valueTT.internal)) {
                trgTypeColumn_.set(mapRow, valueTT);
              }
              break;             
            case 5:
              if (regionColumn_ != null) {
                MultiChoiceEditor.ChoiceEntrySet currReg = regionColumn_.get(mapRow);
                if ((currReg == null) || (!currReg.equals(value))) {
                  regionColumn_.set(mapRow, (MultiChoiceEditor.ChoiceEntrySet)value);
                }
              } else {
                throw new IllegalArgumentException();
              }
              break;                   
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      } 
      
      public boolean isCellEditable(int r, int c) {
        return (true);
      }
      
      boolean checkValues(Map<String, EnumCellNodeType> nodeTypes, boolean allowDoubleBlanks) {
        //
        // The source and target nodes must be named, and
        // all instances of a node must have the same node type:
        //
        
        int size = sourceColumn_.size();
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          String ect = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));                
          boolean srcBlank = ((ecs == null) || ecs.trim().equals(""));
          boolean trgBlank = ((ect == null) || ect.trim().equals(""));
          if (allowDoubleBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }
          if (srcBlank || trgBlank) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, 
                                          rMan.getString("buildNetwork.cannotBeBlank"), 
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          String inconDesc = checkMismatch(ecs, srcTypeColumn_, i, nodeTypes);
          if (inconDesc == null) {
            inconDesc = checkMismatch(ect, trgTypeColumn_, i, nodeTypes);
          }
          if (inconDesc != null) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, inconDesc,
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
        }
        return (true);
      }
  
      boolean myBuildInstruction(BuildInstruction bi) {
        return (bi instanceof GeneralBuildInstruction);
      }
      
      void dropObsoleteRegions() {
        int rcolSize = regionColumn_.size();
        for (int i = 0; i < rcolSize; i++) {
          MultiChoiceEditor.ChoiceEntrySet oldCes = regionColumn_.get(i);
          MultiChoiceEditor.ChoiceEntrySet newCes = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
          regionColumn_.set(i, newCes);
          int oldCSize = oldCes.values.length;
          int newCSize = newCes.values.length;        
          for (int j = 0; j < newCSize; j++) {
            MultiChoiceEditor.ChoiceEntry newCe = newCes.values[j];
            for (int k = 0; k < oldCSize; k++) {
              MultiChoiceEditor.ChoiceEntry oldCe = oldCes.values[k];
              if (oldCe.internal.equals(newCe.internal)) {
                if (oldCe.selected) {
                  newCe.selected = true;
                }
                break;
              }
            }
          }
        }
        return;
      }
      
      boolean rowHasSelectedRegion(int row) {
        MultiChoiceEditor.ChoiceEntrySet ces = regionColumn_.get(row);
        int cSize = ces.values.length;
        for (int j = 0; j < cSize; j++) {
          MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
          if (ce.selected) {
            return (true);
          }
        }
        return (false);
      }
     
      void extractValues(boolean doRegions, List<BuildInstruction> instruct) {
        rowMapping_ = new ArrayList<Integer>();
        hiddenIDColumn_ = new ArrayList<String>();
        srcTypeColumn_ = new ArrayList<EnumCellNodeType>();      
        sourceColumn_ = new ArrayList<String>();
        linkSignColumn_ = new ArrayList<EnumCellGeneralSign>();
        trgTypeColumn_ = new ArrayList<EnumCellNodeType>();      
        targetColumn_ = new ArrayList<String>();
        if (doRegions) {
          regionColumn_ = new ArrayList<MultiChoiceEditor.ChoiceEntrySet>();
          colCount_ = 6;
        } else {
          colCount_ = 5;
        }
        rowCount_ = 0;   
        
        Iterator<BuildInstruction> biit = instruct.iterator();
        while (biit.hasNext()) { 
          BuildInstruction bi = biit.next();
          if (!myBuildInstruction(bi)) {
            continue;
          }
          GeneralBuildInstruction gbi = (GeneralBuildInstruction)bi;
          // Hidden column
          hiddenIDColumn_.add(bi.getID());
          // This is hacky; use a reverse mapping instead?
          // Col 1
          int nsize = nodeTypeList_.size();
          for (int i = 0; i < nsize; i++) {
            EnumCellNodeType ecnt = nodeTypeList_.get(i);
            if (ecnt.internal.equals(Integer.toString(gbi.getSourceType()))) {
              srcTypeColumn_.add(new EnumCellNodeType(ecnt));
              break;
            }
          }
          // Col 0
          sourceColumn_.add(bi.getSourceName());
          // Col 2
          int ssize = generalSignList_.size();
          for (int i = 0; i < ssize; i++) {
            EnumCellGeneralSign ecs = generalSignList_.get(i);
            if (ecs.internal.equals(Integer.toString(gbi.getLinkSign()))) {
              linkSignColumn_.add(new EnumCellGeneralSign(ecs));
              break;
            }
          }        
          // Col 4
          for (int i = 0; i < nsize; i++) {
            EnumCellNodeType ecnt = nodeTypeList_.get(i);
            if (ecnt.internal.equals(Integer.toString(gbi.getTargType()))) {
              trgTypeColumn_.add(new EnumCellNodeType(ecnt));
              break;
            }
          }
          // Col 3
          targetColumn_.add(gbi.getTargName());
          
          // Col 5       
          if (regionColumn_ != null) {
            MultiChoiceEditor.ChoiceEntrySet ces = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
            int cSize = ces.values.length; 
            Iterator<InstructionRegions.RegionTuple> tupit = gbi.getRegions().getRegionTuples();
            while (tupit.hasNext()) {
              InstructionRegions.RegionTuple tuple = tupit.next();             
              for (int j = 0; j < cSize; j++) {
                MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
                if (ce.internal.equals(tuple.sourceRegion) &&
                    ce.internal.equals(tuple.targetRegion)) {
                  ce.selected = true;
                  break;
                }
              }
            } 
            regionColumn_.add(ces);
          }
          
          initTypeMap(myTypeTracker_, gbi.getSourceName(), gbi.getSourceType());
          initTypeMap(myTypeTracker_, gbi.getTargName(), gbi.getTargType());   
                 
          rowMapping_.add(rowCount_, new Integer(rowCount_));
          
          rowCount_++;
        }
        return;
      }
      
      List<BuildInstruction> applyValues(boolean dumpBlanks) {
        ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
  
        int size = sourceColumn_.size();      
        for (int i = 0; i < size; i++) {
          String sourceName = sourceColumn_.get(i);
          String targName = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);        
          boolean srcBlank = ((sourceName == null) || sourceName.trim().equals(""));
          boolean trgBlank = ((targName == null) || targName.trim().equals(""));
          boolean idBlank = ((id == null) || id.trim().equals(""));        
          if (dumpBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }        
          int sourceType = extractIntValue(srcTypeColumn_.get(i).internal).intValue();
          int linkSign = extractIntValue(linkSignColumn_.get(i).internal).intValue();
          int targType = extractIntValue(trgTypeColumn_.get(i).internal).intValue();
  
          GeneralBuildInstruction gbi = 
            new GeneralBuildInstruction(id, sourceType, sourceName, linkSign, targType, targName);
          if (regionColumn_ != null) {
            InstructionRegions ir = new InstructionRegions();
            MultiChoiceEditor.ChoiceEntrySet ces = regionColumn_.get(i);    
            int cSize = ces.values.length;  
            for (int j = 0; j < cSize; j++) {
              MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
              if (ce.selected) {
                InstructionRegions.RegionTuple tup = 
                  new InstructionRegions.RegionTuple(ce.internal, ce.internal);              
                ir.addRegionTuple(tup);
              }
            }
            gbi.setRegions(ir);
          }
          retval.add(gbi);
        }
        return (retval);
      }
    }  
  
    /***************************************************************************
    **
    ** Used for the signal table
    */
    
    class SignalTableModel extends BuildTableModel {
      
      private static final long serialVersionUID = 1L;
      
      private int colCount_;
      private ArrayList<String> srcColumn_;    
      private ArrayList<String> trFacColumn_;    
      private ArrayList<String> targetColumn_;
      private ArrayList<EnumCellSignalType> signalTypeColumn_;
      private ArrayList<EnumCellRegionType> srcRegionColumn_;
      private ArrayList<EnumCellRegionType> trgRegionColumn_;  
      private JFrame parent_;
      private Map<String, EnumCellNodeType> myTypeTracker_;    
  
      SignalTableModel(JFrame parent, boolean doRegions, List<BuildInstruction> instruct, Map<String, EnumCellNodeType> nodeTypeTracker) {
        parent_ = parent;
        myTypeTracker_ = nodeTypeTracker;
        extractValues(doRegions, instruct); 
      }
  
      public boolean textEditingDone(int col, int row, Object val) {
        if ((val == null) || ((String)val).trim().equals("")) {
          return (true);
        }
        EnumCellNodeType existing = findExistingType((String)val, myTypeTracker_);
        if (existing == null) {
          myTypeTracker_.put(DataUtil.normKey((String)val), new EnumCellNodeType(geneEnumType_));      
        } else if (!existing.internal.equals(Integer.toString(Node.GENE))) { 
          cannotUseEnteredName((String)val, existing);
          return (false);
        }
        return (true);
      }
  
      public int hasNameInRows(String nodeName) {
        int retval = 0;
        int size = srcColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String sname = srcColumn_.get(i);
          String tfname = trFacColumn_.get(i);
          String tname = targetColumn_.get(i);       
          if (normName.equals(DataUtil.normKey(sname)) || 
              normName.equals(DataUtil.normKey(tfname)) || 
              normName.equals(DataUtil.normKey(tname)) ) {
            retval++;
          }
        }      
        return (retval);
      }
   
      public boolean canChangeType(String nodeName, EnumCellNodeType newType) {
        if (hasNameInRows(nodeName) == 0) {
          return (true);
        }
        return (newType.value == Node.GENE);
      }
  
      public void batchTypeChange(String nodeName, EnumCellNodeType newType) {
        if (!canChangeType(nodeName, newType)) {
          throw new IllegalStateException();
        }
        // Nothing to do...
        return;
      }              
   
      void deleteRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.remove(rows[i]).intValue();        
          hiddenIDColumn_.remove(mapRow);         
          srcColumn_.remove(mapRow);        
          trFacColumn_.remove(mapRow);
          targetColumn_.remove(mapRow);
          signalTypeColumn_.remove(mapRow);
          if (srcRegionColumn_ != null) {
            srcRegionColumn_.remove(mapRow);
            trgRegionColumn_.remove(mapRow);          
          }        
          
          for (int j = i + 1; j < rows.length; j++) {
            if (rows[j] > rows[i]) {
              rows[j]--;
            }
          }
          rowCount_--;
          
          for (int j = rows[i]; j < rowCount_; j++) {
            int currVal = rowMapping_.get(j).intValue();
            rowMapping_.set(j, new Integer(currVal - 1));
          }
        }
        return; 
      } 
      
      boolean addRow() {
        rowMapping_.add(rowCount_, new Integer(hiddenIDColumn_.size()));    
        hiddenIDColumn_.add("");  // IMPORTANT! must be set by instruction processor!      
        srcColumn_.add("");
        trFacColumn_.add("");
        targetColumn_.add("");
        signalTypeColumn_.add(signalTypeList_.get(0)); 
        if (srcRegionColumn_ != null) {
          srcRegionColumn_.add(regionList_.get(0));
          trgRegionColumn_.add(regionList_.get(0));         
        }      
        rowCount_++;
        return (true);
      }
          
      void swapRows(int mappedTopIndex, int mappedBottomIndex) {
        
        String remObj = hiddenIDColumn_.get(mappedTopIndex);
        hiddenIDColumn_.set(mappedTopIndex, hiddenIDColumn_.get(mappedBottomIndex));
        hiddenIDColumn_.set(mappedBottomIndex, remObj);
  
        remObj = srcColumn_.get(mappedTopIndex);
        srcColumn_.set(mappedTopIndex, srcColumn_.get(mappedBottomIndex));
        srcColumn_.set(mappedBottomIndex, remObj);
        
        remObj = trFacColumn_.get(mappedTopIndex);
        trFacColumn_.set(mappedTopIndex, trFacColumn_.get(mappedBottomIndex));
        trFacColumn_.set(mappedBottomIndex, remObj);
        
        remObj = targetColumn_.get(mappedTopIndex);
        targetColumn_.set(mappedTopIndex, targetColumn_.get(mappedBottomIndex));
        targetColumn_.set(mappedBottomIndex, remObj);
        
        EnumCellSignalType remObjST = signalTypeColumn_.get(mappedTopIndex);
        signalTypeColumn_.set(mappedTopIndex, signalTypeColumn_.get(mappedBottomIndex));
        signalTypeColumn_.set(mappedBottomIndex, remObjST);      
  
        if (srcRegionColumn_ != null) {
          EnumCellRegionType remObjRT = srcRegionColumn_.get(mappedTopIndex);
          srcRegionColumn_.set(mappedTopIndex, srcRegionColumn_.get(mappedBottomIndex));
          srcRegionColumn_.set(mappedBottomIndex, remObjRT);
          
          remObjRT = trgRegionColumn_.get(mappedTopIndex);
          trgRegionColumn_.set(mappedTopIndex, trgRegionColumn_.get(mappedBottomIndex));
          trgRegionColumn_.set(mappedBottomIndex, remObjRT);
        }
        
        return;
      }        
  
      public int getRowCount() {
        return (rowCount_); 
      }  
      
      public int getColumnCount() {
        return (colCount_);
      }
      
      public String getColumnName(int c) {
        try {
          ResourceManager rMan = appState_.getRMan();
          switch (c) {
            case 0:
              return (rMan.getString("buildNetwork.signalInputGene"));          
            case 1:
              return (rMan.getString("buildNetwork.trFac"));
            case 2:
              return (rMan.getString("buildNetwork.targetGene"));  
            case 3:
              return (rMan.getString("buildNetwork.signalType"));
            case 4:
              if (srcRegionColumn_ != null) {
                return (rMan.getString("buildNetwork.srcRegion"));
              }
              throw new IllegalArgumentException();
            case 5:
              if (trgRegionColumn_ != null) {
                return (rMan.getString("buildNetwork.trgRegion"));
              }
              throw new IllegalArgumentException();
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
  
      public Class<? extends Object> getColumnClass(int c) {
        try {
          switch (c) {
            case 0:
              return (String.class);                   
            case 1:
              return (String.class);
            case 2:
              return (String.class);            
            case 3:
              return (EnumCellSignalType.class);
            case 4:
            case 5:
              if (srcRegionColumn_ != null) {
                return (EnumCellRegionType.class);
              }
              throw new IllegalArgumentException();             
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public Object getValueAt(int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {
            case 0:
              return (srcColumn_.get(mapRow));
            case 1:
              return (trFacColumn_.get(mapRow));            
            case 2:
              return (targetColumn_.get(mapRow));
            case 3:
              return (signalTypeColumn_.get(mapRow));
            case 4:
              if (srcRegionColumn_ != null) {
                return (srcRegionColumn_.get(mapRow));
              }
              throw new IllegalArgumentException();
            case 5:
              if (trgRegionColumn_ != null) {
                return (trgRegionColumn_.get(mapRow));
              } 
              throw new IllegalArgumentException();            
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public void setValueAt(Object value, int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {       
            case 0:          
              String currInp = srcColumn_.get(mapRow);
              if ((currInp == null) || (!currInp.equals(value))) {
                srcColumn_.set(mapRow, (String)value);
              }  
              break;          
            case 1:
              String currTrf = trFacColumn_.get(mapRow);
              if ((currTrf == null) || (!currTrf.equals(value))) {
                trFacColumn_.set(mapRow, (String)value);
              }  
              break;  
            case 2:
              String currTarg = targetColumn_.get(mapRow);
              if ((currTarg == null) || (!currTarg.equals(value))) {
                targetColumn_.set(mapRow, (String)value);
              }  
              break;              
            case 3:
              EnumCellSignalType currStype = signalTypeColumn_.get(mapRow);
              EnumCellSignalType valueST = (EnumCellSignalType)value;
              if (!currStype.internal.equals(valueST.internal)) {
                signalTypeColumn_.set(mapRow, valueST);
              }             
              break;
            case 4:
              if (srcRegionColumn_ == null) {
                throw new IllegalArgumentException();
              }
              EnumCellRegionType valueRT = (EnumCellRegionType)value;
              
              if (srcRegionColumn_.get(mapRow).doReplace(valueRT)) {
                srcRegionColumn_.set(mapRow, valueRT);
              }
              break;
            case 5:
              if (trgRegionColumn_ == null) {
                throw new IllegalArgumentException();
              }
              EnumCellRegionType valueRTT = (EnumCellRegionType)value;
              if (trgRegionColumn_.get(mapRow).doReplace(valueRTT)) {
                trgRegionColumn_.set(mapRow, valueRTT);
              }
              break;            
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      } 
      
      public boolean isCellEditable(int r, int c) {
        return (true);
      }
      
      boolean checkValues(Map<String, EnumCellNodeType> nodeTypes, boolean allowDoubleBlanks) {
        //
        // The source and target nodes must be named, and
        // all instances of a node must have the same node type:
        //
        
        int size = srcColumn_.size();
        for (int i = 0; i < size; i++) {        
          String ecs = srcColumn_.get(i);
          String ect = targetColumn_.get(i);        
          String ectr = trFacColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));                
          boolean srcBlank = ((ecs == null) || ecs.trim().equals(""));
          boolean trgBlank = ((ect == null) || ect.trim().equals(""));
          boolean trfBlank = ((ectr == null) || ectr.trim().equals(""));        
          if (allowDoubleBlanks && srcBlank && trgBlank && trfBlank && idBlank) {
            continue;
          }
          if (srcBlank || trgBlank || trfBlank) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, 
                                          rMan.getString("buildNetwork.cannotBeBlank"), 
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          String inconDesc = checkMismatch(ecs, dummyList_, 0, nodeTypes);
          if (inconDesc == null) {
            inconDesc = checkMismatch(ect, dummyList_, 0, nodeTypes);
          }
          if (inconDesc == null) {
            inconDesc = checkMismatch(ectr, dummyList_, 0, nodeTypes);
          }        
          if (inconDesc != null) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, inconDesc,
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          
          if (srcRegionColumn_ != null) {
            EnumCellRegionType src = srcRegionColumn_.get(i);
            EnumCellRegionType trg = trgRegionColumn_.get(i);          
            if (((src.value != 0) && (trg.value == 0)) ||
                ((src.value == 0) && (trg.value != 0))) {
              ResourceManager rMan = appState_.getRMan();
              JOptionPane.showMessageDialog(parent_, rMan.getString("buildNetwork.missingSrcTrgRegion"),
                                            rMan.getString("buildNetwork.errorTitle"),
                                            JOptionPane.ERROR_MESSAGE);
              return (false);
            }
          }        
        }
        return (true);
      }
      
      boolean myBuildInstruction(BuildInstruction bi) {
        return (bi instanceof SignalBuildInstruction);
      }
      
      boolean rowHasSelectedRegion(int row) {
        EnumCellRegionType sEcrt = srcRegionColumn_.get(row);
        EnumCellRegionType tEcrt = trgRegionColumn_.get(row);          
        return ((sEcrt.internal != null) || (tEcrt.internal != null));
      }
          
      void dropObsoleteRegions() {
        dropObsoleteRegionHelper(srcRegionColumn_);
        dropObsoleteRegionHelper(trgRegionColumn_);
        return;
      }
      
      void extractValues(boolean doRegions, List<BuildInstruction> instruct) {
        rowMapping_ = new ArrayList<Integer>();
        hiddenIDColumn_ = new ArrayList<String>();      
        srcColumn_ = new ArrayList<String>();
        trFacColumn_ = new ArrayList<String>();    
        targetColumn_ = new ArrayList<String>();
        signalTypeColumn_ = new ArrayList<EnumCellSignalType>();
        if (doRegions) {
          srcRegionColumn_ = new ArrayList<EnumCellRegionType>();
          trgRegionColumn_ = new ArrayList<EnumCellRegionType>();        
        }
        colCount_ = (doRegions) ? 6 : 4;      
        rowCount_ = 0;   
        
        Iterator<BuildInstruction> biit = instruct.iterator();
        while (biit.hasNext()) { 
          BuildInstruction bi = biit.next();
          if (!myBuildInstruction(bi)) {
            continue;
          }
          SignalBuildInstruction sbi = (SignalBuildInstruction)bi;
          int instrCount;
          Iterator<InstructionRegions.RegionTuple> tupit = null;
          if (doRegions) {
            if (!sbi.hasRegions()) {
              throw new IllegalStateException();
            }
            instrCount = sbi.getRegions().getNumTuples();
            if (instrCount == 0) {
              instrCount = 1;
            } else {
              tupit = sbi.getRegions().getRegionTuples();
            }
          } else {
            instrCount = 1;
          }
  
          for (int r = 0; r < instrCount; r++) {
            // Hidden column
            hiddenIDColumn_.add(bi.getID());
            // Col 0
            srcColumn_.add(sbi.getSourceName());
            // Col 1
            trFacColumn_.add(sbi.getSignalFactorName());
            // Col 2
            targetColumn_.add(sbi.getTargName());
            // Col 3
            int nsize = signalTypeList_.size();
            for (int i = 0; i < nsize; i++) {
              EnumCellSignalType ecst = signalTypeList_.get(i);
              if (ecst.internal.equals(Integer.toString(sbi.getSignalType()))) {
                signalTypeColumn_.add(new EnumCellSignalType(ecst));
                break;
              }
            }
  
            if (doRegions) {
              if (tupit != null) {
                int rsize = regionList_.size();
                InstructionRegions.RegionTuple tuple = tupit.next();
  
                // Col 5
                for (int i = 0; i < rsize; i++) {
                  EnumCellRegionType ecrt = regionList_.get(i);
                  if (ecrt.internalMatch(tuple.sourceRegion)) {
                    srcRegionColumn_.add(new EnumCellRegionType(ecrt));
                    break;
                  }
                }
                // Col 6
                for (int i = 0; i < rsize; i++) {
                  EnumCellRegionType ecrt = regionList_.get(i);
                  if (ecrt.internalMatch(tuple.targetRegion)) {
                    trgRegionColumn_.add(new EnumCellRegionType(ecrt));
                    break;
                  }
                }
              } else {
                EnumCellRegionType ecrt = regionList_.get(0);
                srcRegionColumn_.add(new EnumCellRegionType(ecrt));
                trgRegionColumn_.add(new EnumCellRegionType(ecrt));
              }
            }
                      
            initTypeMap(myTypeTracker_, sbi.getSourceName(), sbi.getSourceType());
            initTypeMap(myTypeTracker_, sbi.getSignalFactorName(), sbi.getSignalFactorType());          
            initTypeMap(myTypeTracker_, sbi.getTargName(), sbi.getTargType());        
                                   
            rowMapping_.add(rowCount_, new Integer(rowCount_));
          
            rowCount_++;
          }
        }
        return;
      }
      
      List<BuildInstruction> applyValues(boolean dumpBlanks) {
        ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
  
        int size = srcColumn_.size();      
        for (int i = 0; i < size; i++) {
          String sourceName = srcColumn_.get(i);
          String targName = targetColumn_.get(i);
          String trFacName = trFacColumn_.get(i);
          String id = hiddenIDColumn_.get(i);        
          boolean idBlank = ((id == null) || id.trim().equals(""));   
          boolean srcBlank = ((sourceName == null) || sourceName.trim().equals(""));
          boolean trgBlank = ((targName == null) || targName.trim().equals(""));
          boolean trfBlank = ((trFacName == null) || trFacName.trim().equals(""));        
          if (dumpBlanks && srcBlank && trgBlank && trfBlank && idBlank) {
            continue;
          }
          int sigType = extractIntValue(((EnumCell)signalTypeColumn_.get(i)).internal).intValue();
          
          SignalBuildInstruction sbi = new SignalBuildInstruction(id, Node.GENE, sourceName, 
                                                                  Node.GENE, targName,
                                                                  Node.GENE, trFacName, sigType);
          if (srcRegionColumn_ != null) {
            InstructionRegions ir = new InstructionRegions();
            EnumCellRegionType src = srcRegionColumn_.get(i);
            EnumCellRegionType trg = trgRegionColumn_.get(i);          
            if ((src.value != 0) && (trg.value != 0)) {
              InstructionRegions.RegionTuple tup = 
                new InstructionRegions.RegionTuple(src.internal, trg.internal);              
              ir.addRegionTuple(tup);
            }
            sbi.setRegions(ir);
          }
          retval.add(sbi);
        }
        return (retval);
      }
    }  
    
   
    /***************************************************************************
    **
    ** Used for the general table
    */
    
    class GeneralTableModel extends BuildTableModel implements ComboFinishedTracker {
      private static final long serialVersionUID = 1L;
      private int colCount_;
      private ArrayList<EnumCellNodeType> srcTypeColumn_;    
      private ArrayList<String> sourceColumn_;
      private ArrayList<EnumCellGeneralSign> linkSignColumn_;
      private ArrayList<EnumCellNodeType> trgTypeColumn_;    
      private ArrayList<String> targetColumn_;
      private ArrayList<EnumCellRegionType> srcRegionColumn_;
      private ArrayList<EnumCellRegionType> trgRegionColumn_;
      private JFrame parent_;
      private Map<String, EnumCellNodeType> myTypeTracker_;
  
      GeneralTableModel(JFrame parent, boolean doRegions, List<BuildInstruction> instruct, Map<String, EnumCellNodeType> nodeTypeTracker) {
        parent_ = parent;
        myTypeTracker_ = nodeTypeTracker;
        extractValues(doRegions, instruct); 
      }
  
      
     public boolean comboEditingDone(int col, int row, Object val) {
        int mapRow = rowMapping_.get(row).intValue();
        EnumCellNodeType newType = (EnumCellNodeType)val;
        List<EnumCellNodeType> typeList; 
        List<String> nameList;       
        if (col == 1) {
          typeList = srcTypeColumn_;
          nameList = sourceColumn_;
        } else if (col == 4) {
          typeList = trgTypeColumn_;
          nameList = targetColumn_;
        } else {
          throw new IllegalArgumentException();
        }
        return (comboEditingDoneSupport(nameList, typeList, mapRow, 
                                        myTypeTracker_, newType));    
      }        
  
      public boolean textEditingDone(int col, int row, Object val) {
        if ((val == null) || ((String)val).trim().equals("")) {
          return (true);
        }
        EnumCellNodeType existing = findExistingType((String)val, myTypeTracker_);
        int mapRow = rowMapping_.get(row).intValue();
        ArrayList<EnumCellNodeType> useList;
        if (col == 0) {
          useList = srcTypeColumn_;
        } else if (col == 3) {
          useList = trgTypeColumn_;
        } else {
          throw new IllegalArgumentException();
        }
        if (existing == null) {
          EnumCellNodeType newType = useList.get(mapRow);
          myTypeTracker_.put(DataUtil.normKey((String)val), newType);
        } else {
          EnumCell currStype = useList.get(mapRow);
          if (!currStype.internal.equals(existing.internal)) {
            useList.set(mapRow, existing);
            ctd_.jt.revalidate();
            ctd_.jt.repaint();
          }
        }
        return (true); 
      }
  
      public int hasNameInRows(String nodeName) {
        int retval = 0;
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String sname = sourceColumn_.get(i);
          String tname = targetColumn_.get(i);
          if (normName.equals(DataUtil.normKey(sname)) || normName.equals(DataUtil.normKey(tname))) {
            retval++;
          }
        }      
        return (retval);
      }
      
      public boolean canChangeType(String nodeName, EnumCellNodeType newType) {
        // We can always change types
        return (true);
      }
  
      public void batchTypeChange(String nodeName, EnumCellNodeType newType) {
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ecs))) {
            srcTypeColumn_.set(i, new EnumCellNodeType(newType));
          }
          String ect = targetColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ect))) {
            trgTypeColumn_.set(i, new EnumCellNodeType(newType));
          }
        }      
        return;
      } 
      
      void deleteRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.remove(rows[i]).intValue();
          hiddenIDColumn_.remove(mapRow);          
          srcTypeColumn_.remove(mapRow);        
          sourceColumn_.remove(mapRow);
          linkSignColumn_.remove(mapRow);
          trgTypeColumn_.remove(mapRow);        
          targetColumn_.remove(mapRow);
          if (srcRegionColumn_ != null) {
            srcRegionColumn_.remove(mapRow);
            trgRegionColumn_.remove(mapRow);          
          }
          
          for (int j = i + 1; j < rows.length; j++) {
            if (rows[j] > rows[i]) {
              rows[j]--;
            }
          }
          rowCount_--;
          
          for (int j = rows[i]; j < rowCount_; j++) {
            int currVal = rowMapping_.get(j).intValue();
            rowMapping_.set(j, new Integer(currVal - 1));
          }
        }
        return; 
      } 
      
      boolean addRow() {
        rowMapping_.add(rowCount_, new Integer(hiddenIDColumn_.size()));      
        hiddenIDColumn_.add("");  // IMPORTANT! must be set by instruction processor!   
        srcTypeColumn_.add(nodeTypeList_.get(geneTypeIndex_));   
        sourceColumn_.add("");
        linkSignColumn_.add(generalSignList_.get(0));
        trgTypeColumn_.add(nodeTypeList_.get(geneTypeIndex_));
        targetColumn_.add("");
        if (srcRegionColumn_ != null) {
          srcRegionColumn_.add(regionList_.get(0));
          trgRegionColumn_.add(regionList_.get(0));         
        }
        rowCount_++;
        return (true);
      } 
      
      void swapRows(int mappedTopIndex, int mappedBottomIndex) {
        
        String remObj = hiddenIDColumn_.get(mappedTopIndex);
        hiddenIDColumn_.set(mappedTopIndex, hiddenIDColumn_.get(mappedBottomIndex));
        hiddenIDColumn_.set(mappedBottomIndex, remObj);
  
        EnumCellNodeType remObjNT = srcTypeColumn_.get(mappedTopIndex);
        srcTypeColumn_.set(mappedTopIndex, srcTypeColumn_.get(mappedBottomIndex));
        srcTypeColumn_.set(mappedBottomIndex, remObjNT);
        
        remObj = sourceColumn_.get(mappedTopIndex);
        sourceColumn_.set(mappedTopIndex, sourceColumn_.get(mappedBottomIndex));
        sourceColumn_.set(mappedBottomIndex, remObj);
        
        EnumCellGeneralSign remObjGS = linkSignColumn_.get(mappedTopIndex);
        linkSignColumn_.set(mappedTopIndex, linkSignColumn_.get(mappedBottomIndex));
        linkSignColumn_.set(mappedBottomIndex, remObjGS);
        
        remObjNT = trgTypeColumn_.get(mappedTopIndex);
        trgTypeColumn_.set(mappedTopIndex, trgTypeColumn_.get(mappedBottomIndex));
        trgTypeColumn_.set(mappedBottomIndex, remObjNT);
        
        remObj = targetColumn_.get(mappedTopIndex);
        targetColumn_.set(mappedTopIndex, targetColumn_.get(mappedBottomIndex));
        targetColumn_.set(mappedBottomIndex, remObj);      
  
        if (srcRegionColumn_ != null) {
          EnumCellRegionType remObjRT = srcRegionColumn_.get(mappedTopIndex);
          srcRegionColumn_.set(mappedTopIndex, srcRegionColumn_.get(mappedBottomIndex));
          srcRegionColumn_.set(mappedBottomIndex, remObjRT);
          
          remObjRT = trgRegionColumn_.get(mappedTopIndex);
          trgRegionColumn_.set(mappedTopIndex, trgRegionColumn_.get(mappedBottomIndex));
          trgRegionColumn_.set(mappedBottomIndex, remObjRT);
        }
        
        return;
      }        
   
      public int getRowCount() {
        return (rowCount_); 
      }  
      
      public int getColumnCount() {
        return (colCount_);
      }
      
      public String getColumnName(int c) {
        try {
          ResourceManager rMan = appState_.getRMan();
          switch (c) {
            case 0:
              return (rMan.getString("buildNetwork.input"));
            case 1:
              return (rMan.getString("buildNetwork.srcType"));             
            case 2:
              return (rMan.getString("buildNetwork.generalSign"));
            case 3:
              return (rMan.getString("buildNetwork.target"));
            case 4:
              return (rMan.getString("buildNetwork.trgType"));               
            case 5:
              if (srcRegionColumn_ != null) {
                return (rMan.getString("buildNetwork.srcRegion"));
              }
              throw new IllegalArgumentException();
            case 6:
              if (trgRegionColumn_ != null) {
                return (rMan.getString("buildNetwork.trgRegion"));
              }
              throw new IllegalArgumentException();
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
  
      public Class<? extends Object> getColumnClass(int c) {
        try {
          switch (c) {
            case 0:
              return (String.class);
            case 1:
              return (EnumCellNodeType.class);            
            case 2:
              return (EnumCellGeneralSign.class);
            case 3:
              return (String.class);
            case 4:
              return (EnumCellNodeType.class);             
            case 5:
            case 6:
              if (srcRegionColumn_ != null) {
                return (EnumCellRegionType.class);
              }
              throw new IllegalArgumentException();        
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public Object getValueAt(int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {
            case 0:
              return (sourceColumn_.get(mapRow));
            case 1:
              return (srcTypeColumn_.get(mapRow));            
            case 2:
              return (linkSignColumn_.get(mapRow));
            case 3:
              return (targetColumn_.get(mapRow));
            case 4:
              return (trgTypeColumn_.get(mapRow));            
            case 5:
              if (srcRegionColumn_ != null) {
                return (srcRegionColumn_.get(mapRow));
              }
              throw new IllegalArgumentException();
            case 6:
              if (trgRegionColumn_ != null) {
                return (trgRegionColumn_.get(mapRow));
              } 
              throw new IllegalArgumentException();         
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public void setValueAt(Object value, int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();        
          switch (c) {
            case 0:          
              String currSrc = sourceColumn_.get(mapRow);
              if ((currSrc == null) || (!currSrc.equals(value))) {
                sourceColumn_.set(mapRow, (String)value);
              }  
              break;
            case 1:          
              EnumCellNodeType currStype = srcTypeColumn_.get(mapRow);
              EnumCellNodeType valueNS = (EnumCellNodeType)value; 
              if (!currStype.internal.equals(valueNS.internal)) {
                srcTypeColumn_.set(mapRow, valueNS);
              }            
              break;                
            case 2:
              EnumCellGeneralSign currSign = linkSignColumn_.get(mapRow);
              EnumCellGeneralSign valueGS = (EnumCellGeneralSign)value;
              if (!currSign.internal.equals(valueGS.internal)) {
                linkSignColumn_.set(mapRow, valueGS);
              }  
              break;
            case 3:
              String currTarg = targetColumn_.get(mapRow);
              if ((currTarg == null) || (!currTarg.equals(value))) {
                targetColumn_.set(mapRow, (String)value);
              }  
              break;
            case 4:
              EnumCellNodeType currTtype = trgTypeColumn_.get(mapRow);
              EnumCellNodeType valueNT = (EnumCellNodeType)value; 
              if (!currTtype.internal.equals(valueNT.internal)) {
                trgTypeColumn_.set(mapRow, valueNT);
              }
              break;                  
            case 5:
              if (srcRegionColumn_ == null) {
                throw new IllegalArgumentException();
              } 
              EnumCellRegionType valueRT = (EnumCellRegionType)value;
              if (srcRegionColumn_.get(mapRow).doReplace(valueRT)) {
                srcRegionColumn_.set(mapRow, valueRT);
              }  
              break;
            case 6:
              if (trgRegionColumn_ == null) {
                throw new IllegalArgumentException();
              }
              EnumCellRegionType valueRTT = (EnumCellRegionType)value;
              if (trgRegionColumn_.get(mapRow).doReplace(valueRTT)) {
                trgRegionColumn_.set(mapRow, valueRTT);
              }
              break;               
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      } 
      
      public boolean isCellEditable(int r, int c) {
        return (true);
      }
      
      boolean checkValues(Map<String, EnumCellNodeType> nodeTypes, boolean allowDoubleBlanks) {
        //
        // The source and target nodes must be named, and
        // all instances of a node must have the same node type:
        //
        
        int size = sourceColumn_.size();
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          String ect = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));                
          boolean srcBlank = ((ecs == null) || ecs.trim().equals(""));
          boolean trgBlank = ((ect == null) || ect.trim().equals(""));
          if (allowDoubleBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }
          if (srcBlank || trgBlank) {          
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, 
                                          rMan.getString("buildNetwork.cannotBeBlank"), 
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          String inconDesc = checkMismatch(ecs, srcTypeColumn_, i, nodeTypes);
          if (inconDesc == null) {
            inconDesc = checkMismatch(ect, trgTypeColumn_, i, nodeTypes);
          }
          if (inconDesc != null) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, inconDesc,
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          
          if (srcRegionColumn_ != null) {
            EnumCellRegionType src = srcRegionColumn_.get(i);
            EnumCellRegionType trg = trgRegionColumn_.get(i);          
            if (((src.value != 0) && (trg.value == 0)) ||
                ((src.value == 0) && (trg.value != 0))) {
              ResourceManager rMan = appState_.getRMan();
              JOptionPane.showMessageDialog(parent_, rMan.getString("buildNetwork.missingSrcTrgRegion"),
                                            rMan.getString("buildNetwork.errorTitle"),
                                            JOptionPane.ERROR_MESSAGE);
              return (false);
            }
          }
        }
        return (true);
      }
  
      boolean myBuildInstruction(BuildInstruction bi) {
        return (bi instanceof GeneralBuildInstruction);
      }
  
      void extractValues(boolean doRegions, List<BuildInstruction> instruct) {
        rowMapping_ = new ArrayList<Integer>();      
        hiddenIDColumn_ = new ArrayList<String>();
        srcTypeColumn_ = new ArrayList<EnumCellNodeType>();      
        sourceColumn_ = new ArrayList<String>();
        linkSignColumn_ = new ArrayList<EnumCellGeneralSign>();
        trgTypeColumn_ = new ArrayList<EnumCellNodeType>();      
        targetColumn_ = new ArrayList<String>();
        if (doRegions) {
          srcRegionColumn_ = new ArrayList<EnumCellRegionType>();
          trgRegionColumn_ = new ArrayList<EnumCellRegionType>();        
        }
        colCount_ = (doRegions) ? 7 : 5;      
        rowCount_ = 0;   
        
        Iterator<BuildInstruction> biit = instruct.iterator();
        while (biit.hasNext()) { 
          BuildInstruction bi = biit.next();
          if (!myBuildInstruction(bi)) {
            continue;
          }
          GeneralBuildInstruction gbi = (GeneralBuildInstruction)bi;
          int instrCount;
          Iterator<InstructionRegions.RegionTuple> tupit = null;
          if (doRegions) {
            if (!gbi.hasRegions()) {
              throw new IllegalStateException();
            }
            instrCount = gbi.getRegions().getNumTuples();
            if (instrCount == 0) {
              instrCount = 1;
            } else {
              tupit = gbi.getRegions().getRegionTuples();
            }
          } else {
            instrCount = 1;
          }
   
          for (int r = 0; r < instrCount; r++) {
            // Hidden column
            hiddenIDColumn_.add(bi.getID());
  
            // This is hacky; use a reverse mapping instead?
            // Col 1
            int nsize = nodeTypeList_.size();
            for (int i = 0; i < nsize; i++) {
              EnumCellNodeType ecnt = nodeTypeList_.get(i);
              if (ecnt.internal.equals(Integer.toString(gbi.getSourceType()))) {
                srcTypeColumn_.add(new EnumCellNodeType(ecnt));
                break;
              }
            }
            // Col 0
            sourceColumn_.add(bi.getSourceName());
            // Col 2
            int ssize = generalSignList_.size();
            for (int i = 0; i < ssize; i++) {
              EnumCellGeneralSign ecs = generalSignList_.get(i);
              if (ecs.internal.equals(Integer.toString(gbi.getLinkSign()))) {
                linkSignColumn_.add(new EnumCellGeneralSign(ecs));
                break;
              }
            }        
            // Col 4
            for (int i = 0; i < nsize; i++) {
              EnumCellNodeType ecnt = nodeTypeList_.get(i);
              if (ecnt.internal.equals(Integer.toString(gbi.getTargType()))) {
                trgTypeColumn_.add(new EnumCellNodeType(ecnt));
                break;
              }
            }
            // Col 3
            targetColumn_.add(gbi.getTargName());
  
            if (doRegions) {
              if (tupit != null) {
                int rsize = regionList_.size();
                InstructionRegions.RegionTuple tuple = tupit.next();
  
                // Col 5
                for (int i = 0; i < rsize; i++) {
                  EnumCellRegionType ecrt = regionList_.get(i);
                  if (ecrt.internalMatch(tuple.sourceRegion)) {
                    srcRegionColumn_.add(new EnumCellRegionType(ecrt));
                    break;
                  }
                }
                // Col 6
                for (int i = 0; i < rsize; i++) {
                  EnumCellRegionType ecrt = regionList_.get(i);
                  if (ecrt.internalMatch(tuple.targetRegion)) {
                    trgRegionColumn_.add(new EnumCellRegionType(ecrt));
                    break;
                  }
                }
              } else {
                EnumCellRegionType ecrt = regionList_.get(0);
                srcRegionColumn_.add(new EnumCellRegionType(ecrt));
                trgRegionColumn_.add(new EnumCellRegionType(ecrt));
              }
            }
            rowMapping_.add(rowCount_, new Integer(rowCount_));
            initTypeMap(myTypeTracker_, gbi.getSourceName(), gbi.getSourceType());       
            initTypeMap(myTypeTracker_, gbi.getTargName(), gbi.getTargType());
            rowCount_++;
          }
        }
        return;
      }
      
      boolean rowHasSelectedRegion(int row) {
        EnumCellRegionType sEcrt = srcRegionColumn_.get(row);
        EnumCellRegionType tEcrt = trgRegionColumn_.get(row);          
        return ((sEcrt.internal != null) || (tEcrt.internal != null));
      } 
  
      void dropObsoleteRegions() {
        dropObsoleteRegionHelper(srcRegionColumn_);
        dropObsoleteRegionHelper(trgRegionColumn_);
        return;
      }    
      
      List<BuildInstruction> applyValues(boolean dumpBlanks) {
        ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
  
        int size = sourceColumn_.size();      
        for (int i = 0; i < size; i++) {
          String sourceName = sourceColumn_.get(i);
          String targName = targetColumn_.get(i);
          String id = hiddenIDColumn_.get(i);        
          boolean idBlank = ((id == null) || id.trim().equals(""));   
          boolean srcBlank = ((sourceName == null) || sourceName.trim().equals(""));
          boolean trgBlank = ((targName == null) || targName.trim().equals(""));
          if (dumpBlanks && srcBlank && trgBlank && idBlank) {
            continue;
          }
          int sourceType = extractIntValue(((EnumCell)srcTypeColumn_.get(i)).internal).intValue();
          int linkSign = extractIntValue(((EnumCell)linkSignColumn_.get(i)).internal).intValue();
          int targType = extractIntValue(((EnumCell)trgTypeColumn_.get(i)).internal).intValue();        
          
          GeneralBuildInstruction gbi = 
            new GeneralBuildInstruction(id, sourceType, sourceName, linkSign, targType, targName);
          if (srcRegionColumn_ != null) {
            InstructionRegions ir = new InstructionRegions();
            EnumCellRegionType src = srcRegionColumn_.get(i);
            EnumCellRegionType trg = trgRegionColumn_.get(i);          
            if ((src.value != 0) && (trg.value != 0)) {
              InstructionRegions.RegionTuple tup = 
                new InstructionRegions.RegionTuple(src.internal, trg.internal);              
              ir.addRegionTuple(tup);
            }
            gbi.setRegions(ir);
          }
          retval.add(gbi);
        }
        return (retval);
      }
    }  
  
    /***************************************************************************
    **
    ** Used for the lone node table
    */
    
    class LoneNodeTableModel extends BuildTableModel implements ComboFinishedTracker {
      private static final long serialVersionUID = 1L;
      private int colCount_;
      private ArrayList<EnumCellNodeType> srcTypeColumn_;    
      private ArrayList<String> sourceColumn_;
      private ArrayList<MultiChoiceEditor.ChoiceEntrySet> srcRegionColumn_;
      private JFrame parent_;
      private Map<String, EnumCellNodeType> myTypeTracker_;
  
      LoneNodeTableModel(JFrame parent, boolean doRegions, List<BuildInstruction> instruct, Map<String, EnumCellNodeType> nodeTypeTracker) {
        parent_ = parent;
        myTypeTracker_ = nodeTypeTracker;
        extractValues(doRegions, instruct); 
      }
      
      public boolean comboEditingDone(int col, int row, Object val) {
        int mapRow = rowMapping_.get(row).intValue();
        EnumCellNodeType newType = (EnumCellNodeType)val;
        return (comboEditingDoneSupport(sourceColumn_, srcTypeColumn_, mapRow, 
                                        myTypeTracker_, newType));    
      }
      
      public boolean textEditingDone(int col, int row, Object val) {
        if ((val == null) || ((String)val).trim().equals("")) {
          return (true);
        }
        EnumCellNodeType existing = findExistingType((String)val, myTypeTracker_);
        int mapRow = rowMapping_.get(row).intValue();
        if (existing == null) {
          EnumCellNodeType newType = srcTypeColumn_.get(mapRow);
          myTypeTracker_.put(DataUtil.normKey((String)val), newType);     
        } else {
          EnumCell currStype = srcTypeColumn_.get(mapRow);
          if (!currStype.internal.equals(((EnumCell)existing).internal)) {
            srcTypeColumn_.set(mapRow, existing);
            ltd_.jt.revalidate();
            ltd_.jt.repaint();
          }
        }
        return (true);
      }
  
      public int hasNameInRows(String nodeName) {
        int retval = 0;
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ecs))) {
            retval++;
          }
        }      
        return (retval);
      }
      
      public boolean canChangeType(String nodeName, EnumCellNodeType newType) {
        // We can always change types
        return (true);
      }
  
      public void batchTypeChange(String nodeName, EnumCellNodeType newType) {
        int size = sourceColumn_.size();
        String normName = DataUtil.normKey(nodeName);
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          if (normName.equals(DataUtil.normKey(ecs))) {
            srcTypeColumn_.set(i, new EnumCellNodeType(newType));
          }
        }      
        return;
      }    
  
      void deleteRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
          int mapRow = rowMapping_.remove(rows[i]).intValue();
          hiddenIDColumn_.remove(mapRow);          
          srcTypeColumn_.remove(mapRow);        
          sourceColumn_.remove(mapRow);
          if (srcRegionColumn_ != null) {
            srcRegionColumn_.remove(mapRow);
          }
          
          for (int j = i + 1; j < rows.length; j++) {
            if (rows[j] > rows[i]) {
              rows[j]--;
            }
          }
          rowCount_--;
          
          for (int j = rows[i]; j < rowCount_; j++) {
            int currVal = rowMapping_.get(j).intValue();
            rowMapping_.set(j, new Integer(currVal - 1));
          }
        }
        return; 
      } 
      
      boolean addRow() {
        rowMapping_.add(rowCount_, new Integer(hiddenIDColumn_.size()));      
        hiddenIDColumn_.add("");  // IMPORTANT! must be set by instruction processor!   
        srcTypeColumn_.add(nodeTypeList_.get(geneTypeIndex_));   
        sourceColumn_.add("");
        if (srcRegionColumn_ != null) {
          srcRegionColumn_.add(new MultiChoiceEditor.ChoiceEntrySet(regionSet_));
        }
        rowCount_++;
        return (true);
      } 
      
      void swapRows(int mappedTopIndex, int mappedBottomIndex) {
        
        String remObj = hiddenIDColumn_.get(mappedTopIndex);
        hiddenIDColumn_.set(mappedTopIndex, hiddenIDColumn_.get(mappedBottomIndex));
        hiddenIDColumn_.set(mappedBottomIndex, remObj);
  
        EnumCellNodeType remObjNT = srcTypeColumn_.get(mappedTopIndex);
        srcTypeColumn_.set(mappedTopIndex, srcTypeColumn_.get(mappedBottomIndex));
        srcTypeColumn_.set(mappedBottomIndex, remObjNT);
        
        remObj = sourceColumn_.get(mappedTopIndex);
        sourceColumn_.set(mappedTopIndex, sourceColumn_.get(mappedBottomIndex));
        sourceColumn_.set(mappedBottomIndex, remObj);
        
        if (srcRegionColumn_ != null) {
          MultiChoiceEditor.ChoiceEntrySet remObjCES = srcRegionColumn_.get(mappedTopIndex);
          srcRegionColumn_.set(mappedTopIndex, srcRegionColumn_.get(mappedBottomIndex));
          srcRegionColumn_.set(mappedBottomIndex, remObjCES);
        }
        
        return;
      }        
   
      public int getRowCount() {
        return (rowCount_); 
      }  
      
      public int getColumnCount() {
        return (colCount_);
      }
      public String getColumnName(int c) {
        try {
          ResourceManager rMan = appState_.getRMan();
          switch (c) {
            case 0:
              return (rMan.getString("buildNetwork.loneName"));
            case 1:
              return (rMan.getString("buildNetwork.loneType"));                
            case 2:
              if (srcRegionColumn_ != null) {
                return (rMan.getString("buildNetwork.loneRegion"));
              }
              throw new IllegalArgumentException();
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
  
      public Class<? extends Object> getColumnClass(int c) {
        try {
          switch (c) {
            case 0:
              return (String.class);
            case 1:
              return (EnumCellNodeType.class);            
            case 2:
              if (srcRegionColumn_ != null) {
                return (MultiChoiceEditor.ChoiceEntrySet.class);
              }
              throw new IllegalArgumentException();        
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public Object getValueAt(int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();
          switch (c) {
            case 0:
              return (sourceColumn_.get(mapRow));
            case 1:
              return (srcTypeColumn_.get(mapRow));            
            case 2:
              if (srcRegionColumn_ != null) {
                return (srcRegionColumn_.get(mapRow));
              }
              throw new IllegalArgumentException();
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (null);
      }
      
      public void setValueAt(Object value, int r, int c) {
        try {
          int mapRow = rowMapping_.get(r).intValue();        
          switch (c) {         
            case 0:          
              String currSrc = sourceColumn_.get(mapRow);
              if ((currSrc == null) || (!currSrc.equals(value))) {
                sourceColumn_.set(mapRow, (String)value);
              }  
              break;
            case 1:
              EnumCellNodeType currStype = srcTypeColumn_.get(mapRow);
              EnumCellNodeType valueNT = (EnumCellNodeType)value;
              if (!currStype.internal.equals(valueNT.internal)) {
                srcTypeColumn_.set(mapRow, valueNT);
              }  
              break;                  
            case 2:
              if (srcRegionColumn_ == null) {
                throw new IllegalArgumentException();
              }
              MultiChoiceEditor.ChoiceEntrySet currReg = srcRegionColumn_.get(mapRow);
              MultiChoiceEditor.ChoiceEntrySet valueCES = (MultiChoiceEditor.ChoiceEntrySet)value;
              if ((currReg == null) || (!currReg.equals(valueCES))) {
                srcRegionColumn_.set(mapRow, valueCES);
              }     
              break;
            default:
              throw new IllegalArgumentException();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      } 
      
      public boolean isCellEditable(int r, int c) {
        return (true);
      }
      
      boolean checkValues(Map<String, EnumCellNodeType> nodeTypes, boolean allowDoubleBlanks) {
        //
        // The node must be named, and
        // all instances of a node must have the same node type:
        //
        
        int size = sourceColumn_.size();
        for (int i = 0; i < size; i++) {        
          String ecs = sourceColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));        
          boolean srcBlank = ((ecs == null) || ecs.trim().equals(""));
          if (allowDoubleBlanks && srcBlank && idBlank) {
            continue;
          }
          if (srcBlank) { 
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, 
                                          rMan.getString("buildNetwork.cannotBeBlank"), 
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
          String inconDesc = checkMismatch(ecs, srcTypeColumn_, i, nodeTypes);
          if (inconDesc != null) {
            ResourceManager rMan = appState_.getRMan();
            JOptionPane.showMessageDialog(parent_, inconDesc,
                                          rMan.getString("buildNetwork.errorTitle"),
                                          JOptionPane.ERROR_MESSAGE);
            return (false);
          }
        }
       
        return (true);
      }
  
      boolean myBuildInstruction(BuildInstruction bi) {
        return (bi instanceof LoneNodeBuildInstruction);
      }
  
      void extractValues(boolean doRegions, List<BuildInstruction> instruct) {
        rowMapping_ = new ArrayList<Integer>();      
        hiddenIDColumn_ = new ArrayList<String>();
        srcTypeColumn_ = new ArrayList<EnumCellNodeType>();      
        sourceColumn_ = new ArrayList<String>();
        if (doRegions) {
          srcRegionColumn_ = new ArrayList<MultiChoiceEditor.ChoiceEntrySet>();
        }
        colCount_ = (doRegions) ? 3 : 2;      
        rowCount_ = 0;   
        
        Iterator<BuildInstruction> biit = instruct.iterator();
        while (biit.hasNext()) { 
          BuildInstruction bi = biit.next();
          if (!myBuildInstruction(bi)) {
            continue;
          }
          LoneNodeBuildInstruction lbi = (LoneNodeBuildInstruction)bi;
          // Hidden column
          hiddenIDColumn_.add(bi.getID());
  
          // This is hacky; use a reverse mapping instead?
          // Col 1
          int nsize = nodeTypeList_.size();
          for (int i = 0; i < nsize; i++) {
            EnumCellNodeType ecnt = nodeTypeList_.get(i);
            if (ecnt.internal.equals(Integer.toString(lbi.getSourceType()))) {
              srcTypeColumn_.add(new EnumCellNodeType(ecnt));
              break;
            }
          }
          // Col 0
          sourceColumn_.add(bi.getSourceName());
  
          // Col 2     
  
          if (doRegions) {
            MultiChoiceEditor.ChoiceEntrySet ces = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
            int cSize = ces.values.length; 
            Iterator<InstructionRegions.RegionTuple> tupit = lbi.getRegions().getRegionTuples();
            while (tupit.hasNext()) {
              InstructionRegions.RegionTuple tuple = tupit.next();             
              for (int j = 0; j < cSize; j++) {
                MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
                if (ce.internal.equals(tuple.sourceRegion)) {
                  ce.selected = true;
                  break;
                }
              }
            } 
            srcRegionColumn_.add(ces);
          }
          initTypeMap(myTypeTracker_, lbi.getSourceName(), lbi.getSourceType());
          rowMapping_.add(rowCount_, new Integer(rowCount_));        
          rowCount_++;
        }
        return;
      }
  
      void dropObsoleteRegions() {
        int rcolSize = srcRegionColumn_.size();
        for (int i = 0; i < rcolSize; i++) {
          MultiChoiceEditor.ChoiceEntrySet oldCes = srcRegionColumn_.get(i);
          MultiChoiceEditor.ChoiceEntrySet newCes = new MultiChoiceEditor.ChoiceEntrySet(regionSet_);
          srcRegionColumn_.set(i, newCes);
          int oldCSize = oldCes.values.length;
          int newCSize = newCes.values.length;        
          for (int j = 0; j < newCSize; j++) {
            MultiChoiceEditor.ChoiceEntry newCe = newCes.values[j];
            for (int k = 0; k < oldCSize; k++) {
              MultiChoiceEditor.ChoiceEntry oldCe = oldCes.values[k];
              if (oldCe.internal.equals(newCe.internal)) {
                if (oldCe.selected) {
                  newCe.selected = true;
                }
                break;
              }
            }
          }
        }
        return;
      }
      
      boolean rowHasSelectedRegion(int row) {
        MultiChoiceEditor.ChoiceEntrySet ces = srcRegionColumn_.get(row);
        int cSize = ces.values.length;
        for (int j = 0; j < cSize; j++) {
          MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
          if (ce.selected) {
            return (true);
          }
        }
        return (false);
      }   
  
      List<BuildInstruction> applyValues(boolean dumpBlanks) {
        ArrayList<BuildInstruction> retval = new ArrayList<BuildInstruction>();
  
        int size = sourceColumn_.size();      
        for (int i = 0; i < size; i++) {
          String sourceName = sourceColumn_.get(i);
          String id = hiddenIDColumn_.get(i);
          boolean idBlank = ((id == null) || id.trim().equals(""));          
          boolean srcBlank = ((sourceName == null) || sourceName.trim().equals(""));
          if (dumpBlanks && srcBlank && idBlank) {
            continue;
          }
          int sourceType = extractIntValue(srcTypeColumn_.get(i).internal).intValue();        
          LoneNodeBuildInstruction lnbi = new LoneNodeBuildInstruction(id, sourceType, sourceName);
          if (srcRegionColumn_ != null) {
            InstructionRegions ir = new InstructionRegions();
            MultiChoiceEditor.ChoiceEntrySet ces = srcRegionColumn_.get(i);    
            int cSize = ces.values.length;  
            for (int j = 0; j < cSize; j++) {
              MultiChoiceEditor.ChoiceEntry ce = ces.values[j];
              if (ce.selected) {
                InstructionRegions.RegionTuple tup = 
                  new InstructionRegions.RegionTuple(ce.internal, null);              
                ir.addRegionTuple(tup);
              }
            }
            lnbi.setRegions(ir);
          }          
          retval.add(lnbi);
        }
        return (retval);
      }
    }  
  
    /***************************************************************************
    **
    ** Used for a data table renderer
    */
    
    class MultiChoiceRenderer extends JPanel implements TableCellRenderer {
      private static final long serialVersionUID = 1L;
      private JCheckBox[] boxes_;
        
      MultiChoiceRenderer(MultiChoiceEditor.ChoiceEntrySet entry) {
        super();
        valueFill(entry);
      }
  
      void replaceValues(MultiChoiceEditor.ChoiceEntrySet ces) {
        removeAll();
        valueFill(ces);
        return;
      }    
      
      private void valueFill(MultiChoiceEditor.ChoiceEntrySet ces) {
        int size = ces.values.length;
        boxes_ = new JCheckBox[size]; 
        setLayout(new GridLayout(1, size));
  
        for (int i = 0; i < size; i++) {
          MultiChoiceEditor.ChoiceEntry ce = ces.values[i];
          boxes_[i] = new JCheckBox(ce.display, ce.selected);
          add(boxes_[i]);
        }
        return;
      }
    
      private void toBoxes(MultiChoiceEditor.ChoiceEntrySet ces) {     
        int size = ces.values.length;  
        for (int i = 0; i < size; i++) {
          MultiChoiceEditor.ChoiceEntry ce = ces.values[i];
          boxes_[i].setSelected(ce.selected);
        }    
        return;
      }  
          
      public Component getTableCellRendererComponent(JTable table, Object value, 
                                                     boolean isSelected, boolean hasFocus, 
                                                     int row, int column) {
        try {                                               
          if (value == null) {
            return (this);
          }
          MultiChoiceEditor.ChoiceEntrySet ces = (MultiChoiceEditor.ChoiceEntrySet)value;  
          toBoxes(ces);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (this);             
      }
    }  
    
    /***************************************************************************
    **
    ** Used for a data table renderer
    */
    
    class NoEditRenderer extends JComboBox implements TableCellRenderer {
      private static final long serialVersionUID = 1L;
      NoEditRenderer(List<? extends EnumCell> items) {
        super();
        valueFill(items);
      }
      
      void valueFill(List<? extends EnumCell> items) {
        Iterator<? extends EnumCell> iit = items.iterator();
        while (iit.hasNext()) {
          EnumCell cell = iit.next(); 
          this.addItem(cell.display);
        }
        return;
      }
      
      void replaceValues(List<? extends EnumCell> values) {
        removeAllItems();
        valueFill(values);
        return;
      }
      
      public Component getTableCellRendererComponent(JTable table, Object value, 
                                                     boolean isSelected, boolean hasFocus, 
                                                     int row, int column) {
        try {                                               
          if (value == null) {
            return (this);
          }
          setSelectedIndex(((EnumCell)value).value);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return (this);             
      }
    }
  
    
    /***************************************************************************
    **
    ** Used for editors
    */
    
    abstract class BackedEditor extends ComboBoxEditor {
      
      private static final long serialVersionUID = 1L;
      
      protected List<EnumCell> backing_;
      
      BackedEditor(List<? extends EnumCell> values) {
        super(values, appState_);
      }
          
      public void valueFill(Object valueObject) {
        List<EnumCell> list = (List<EnumCell>)valueObject;
        backing_ = new ArrayList<EnumCell>();
        for (int i = 0; i < list.size(); i++) {
          String display = list.get(i).display; 
          this.addItem(display);
          backing_.add(list.get(i));
        }
      }
      
      public int valueToIndex(Object value) {
        return (((EnumCell)value).value);
      }
      
    }
    
    /***************************************************************************
    **
    ** Used for the link sign editor
    */
    
    class SignEditor extends BackedEditor { 
      private static final long serialVersionUID = 1L;
      
      SignEditor(List<? extends EnumCell> values) {
        super(values);
      }   
      public Object indexToValue(int index) {
        return (new EnumCellSign((EnumCellSign)backing_.get(index)));
      }    
    }
    
    /***************************************************************************
    **
    ** Used for the general link sign editor
    */
    
    class GeneralSignEditor extends BackedEditor {
      private static final long serialVersionUID = 1L;
      
      GeneralSignEditor(List<? extends EnumCell> values) {
        super(values);
      }   
      public Object indexToValue(int index) {
        return (new EnumCellGeneralSign((EnumCellGeneralSign)backing_.get(index)));
      }    
    }  
     
    /***************************************************************************
    **
    ** Used for the node type editor
    */
    
    class NodeTypeEditor extends BackedEditor {
      private static final long serialVersionUID = 1L;
      
      NodeTypeEditor(List<? extends EnumCell> values) {
        super(values);
      }   
      public Object indexToValue(int index) {
        return (new EnumCellNodeType((EnumCellNodeType)backing_.get(index)));
      }    
    }
  
    /***************************************************************************
    **
    ** Used for the region editor
    */
    
    class RegionEditor extends BackedEditor {  
      private static final long serialVersionUID = 1L;
      
      RegionEditor(List<? extends EnumCell> values) {
        super(values);
      }   
      public Object indexToValue(int index) {
        return (new EnumCellRegionType((EnumCellRegionType)backing_.get(index)));
      }
      
      void replaceValues(List<? extends EnumCell> values) {
        removeAllItems();
        valueFill(values);
        return;
      }
    }  
    
    /***************************************************************************
    **
    ** Used for the signal type editor
    */
    
    class SignalTypeEditor extends BackedEditor { 
      private static final long serialVersionUID = 1L;
      
      SignalTypeEditor(List<? extends EnumCell> values) {
        super(values);
      }   
      public Object indexToValue(int index) {
        return (new EnumCellSignalType((EnumCellSignalType)backing_.get(index)));
      }    
    }
    
    /***************************************************************************
    **
    ** Used to track selections
    */
    
    class SelectionTracker implements ListSelectionListener {
      
      private TabData td_;
   
      SelectionTracker(TabData td) {
        td_ = td;
      }
      
      public void valueChanged(ListSelectionEvent lse) {
        try {
          if (lse.getValueIsAdjusting()) {
            return;
          }
          setButtonState();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;             
      }
      
      public void setButtonState() {
        td_.selectedRows = td_.jt.getSelectedRows();
        boolean haveARow = (td_.selectedRows.length > 0);
        td_.tableButtonD.setEnabled(haveARow);
        td_.tableButtonD.revalidate();       
        boolean canLower = ((td_.selectedRows.length == 1) && 
                            (td_.selectedRows[0] < td_.jt.getRowCount() - 1));
        td_.tableButtonL.setEnabled(canLower);
        td_.tableButtonL.revalidate();  
        boolean canRaise = ((td_.selectedRows.length == 1) && (td_.selectedRows[0] != 0));
        td_.tableButtonR.setEnabled(canRaise);
        td_.tableButtonR.revalidate();                  
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
    ** Apply our UI values
    ** 
    */
    
    private boolean stashResults(boolean ok, boolean forApply) {
      if (ok) {
        //
        // Check for errors before actually doing the work:
        //
        
        HashMap<String, EnumCellNodeType> types = new HashMap<String, EnumCellNodeType>();
        if (!currTd_.tm.checkValues(types, false) || 
            !std_.tm.checkValues(types, false) || 
            !ltd_.tm.checkValues(types, false)) {  
          return (false);
        }
   
        //
        // Collect the build commands.  Signals are most important.  FIX ME! Inter-region stuff 
        // should go dead last?
        //
        
        request_.buildCmds = std_.tm.applyValues(false);
        request_.buildCmds.addAll(currTd_.tm.applyValues(false));
        request_.buildCmds.addAll(ltd_.tm.applyValues(false));    
    
        request_.keepLayout = (layoutCombo_.getSelectedIndex() == 0);
    
        if (!checkInstructionMismatchs(request_.buildCmds)) {
          return (false);
        }
     
        if (!checkPreProcess(request_)) {
          return (false);
        }
        
        request_.workingRegions = this.workingRegions_;
        request_.strat = ((EnumChoiceContent<SpecialtyLayoutEngine.SpecialtyType>)layoutChoiceCombo_.getSelectedItem()).val;
        request_.params = holdParams_; // Will only hold parameters IF the user chose to override them
        request_.forApply_ = forApply;
        //
        // Still need to come up with the appropriate approach for dealing with the APPLY_ON_THREAD case. In the interim,
        // provide this dialog to the control flow so it can be re-enabled following the thread completion. (GAG!)
        //
        request_.horribleHack = this;
        request_.haveResult = true;
      } else {
        request_.workingRegions = null;
        request_.keepLayout = false; 
        request_.globalDelete = false;
        request_.strat = null;
        request_.params = null;
        request_.buildCmds = null;
        request_.forApply_ = false;
        request_.horribleHack = null;
        request_.haveResult = false;
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
    
    private boolean checkInstructionMismatchs(List<BuildInstruction> buildCmds) {
         
      //
      // We need to find instruction mismatches.  If the user changes a pre-existing interaction
      // to something new, and some other references to that interaction have not been changed, we
      // need to tell the user and allow them to bring the other references into line, or else
      // split the original instruction into two separate instructions:
      //
  
      RemoteRequest daBomb = new RemoteRequest("queBombInstanceMismatch");
      daBomb.setObjectArg("buildCmds", buildCmds);
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      List<BuildInstructionProcessor.MatchChecker> mismatch = (List<BuildInstructionProcessor.MatchChecker>)dbres.getObjAnswer("instMismatch");
      int numMis = mismatch.size();
      
      for (int i = 0; i < numMis; i++) {
        BuildInstructionProcessor.MatchChecker mc = mismatch.get(i);
        InstructionMismatchChoicesDialog imcd = new InstructionMismatchChoicesDialog(appState_, appState_.getTopFrame(), mc);
        imcd.setVisible(true);
        if (!imcd.haveResult()) {
          return (false);
        }
        if (imcd.doSplit()) {
          splitMismatchedInstruct(buildCmds, mc);
        } else {
          changeOtherMismatchedInstruct(buildCmds, mc);        
        }
      }
      return (true);
    }
  
    /***************************************************************************
    **
    ** Talk to the expert!
    ** 
    */
    
    private boolean checkPreProcess(RegionInfoRequest br) {  
    
      //
      // Need to do a high-level analysis and look for instruction duplications,
      // and deletion info, and ask the user what they want to do.
      //
      
      RemoteRequest daBomb = new RemoteRequest("queBombPreProcess");
      daBomb.setObjectArg("buildCmds", br.buildCmds);
      RemoteRequest.Result dbres = cfh_.routeRemoteRequest(daBomb);
      BuildInstructionProcessor.BuildChanges bChanges = (BuildInstructionProcessor.BuildChanges)dbres.getObjAnswer("bChanges");
    
      //
      // Catch (NEW) duplicate interactions and allow the user to punt:
      //
      
      if (bChanges.haveNewDuplicates) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("buildNetwork.duplicateWarning");
        String title = rMan.getString("buildNetwork.duplicateWarningTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
        cfh_.showSimpleUserFeedback(suf);
        if (suf.getIntegerResult() != SimpleUserFeedback.YES) {
          return (false);
        }
      }
    
      //
      // Catch (NEW) duplicate lone nodes interactions and allow the user to punt:
      //
      
      if (bChanges.haveNewLoneDuplicates) {
        ResourceManager rMan = appState_.getRMan();
        String message = rMan.getString("buildNetwork.loneDuplicateWarning");
        String title = rMan.getString("buildNetwork.loneDuplicateWarningTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.YES_NO_OPTION, message, title);
        cfh_.showSimpleUserFeedback(suf);
        if (suf.getIntegerResult() != SimpleUserFeedback.YES) {
          return (false);
        }
      }    
      
      //
      // Catch low-level deletions and ask if the user want to apply them locally or globally:
      //
      
      br.globalDelete = false;
      if (bChanges.needDeletionType) {
        ResourceManager rMan = appState_.getRMan();
        int result = JOptionPane.showOptionDialog(appState_.getTopFrame(), rMan.getString("buildNetwork.deleteTypeMessage"),
                                                  rMan.getString("buildNetwork.deleteTypeMessageTitle"),
                                                  JOptionPane.DEFAULT_OPTION, 
                                                  JOptionPane.QUESTION_MESSAGE, 
                                                  null, new Object[] {
                                                                      rMan.getString("buildNetwork.global"),
                                                                      rMan.getString("buildNetwork.local"),
                                                                      rMan.getString("dialogs.cancel"),
                                                                     }, rMan.getString("buildNetwork.local"));
        if (result == 2) {
          return (false);
        }
        br.globalDelete = (result == 0);                                                                                             
      }
      
      //
      // If other models are being affected, let the user know:
      //
      
      if (!issueChangeMessage(bChanges)) {
        return (false);
      }
      return (true);
    }
    
    /***************************************************************************
    **
    ** Handle a core mismatch by splitting instructions
    ** 
    */
    
    private void splitMismatchedInstruct(List<BuildInstruction> buildCmds, BuildInstructionProcessor.MatchChecker checker) {
      //
      // Splitting should work by just blanking out the ID of the new instructions:
      //   
      int dcNum = checker.differentCores.size();
      for (int i = 0; i < dcNum; i++) {
        BuildInstructionProcessor.CoreInfo ci = checker.differentCores.get(i);
        int inNum = ci.indices.size();
        for (int j = 0; j < inNum; j++) {
          Integer index = ci.indices.get(j);
          BuildInstruction bi = buildCmds.get(index.intValue());
          bi.setID("");
        }
      }
      
      return;
    }
    
   /***************************************************************************
    **
    ** Handle a core mismatch by changing other mismatched instructions
    ** 
    */
    
    private void changeOtherMismatchedInstruct(List<BuildInstruction> buildCmds, BuildInstructionProcessor.MatchChecker checker) {
      //
      // To change, we need to modify the cores 
      //
      
      int dcNum = checker.differentCores.size();
      if (dcNum != 1) {
        throw new IllegalArgumentException();
      }
      BuildInstructionProcessor.CoreInfo ci = checker.differentCores.get(0);
      BuildInstruction newCore = ci.core;
      int scNum = checker.sameCores.size();
      for (int j = 0; j < scNum; j++) {
        Integer index = checker.sameCores.get(j);
        BuildInstruction bi = buildCmds.get(index.intValue());
        BuildInstruction newBi = newCore.clone();
        newBi.setRegions(bi.getRegions());
        newBi.setID(bi.getID());
        buildCmds.set(index.intValue(), newBi);
      }
      return;
    }
  
    /***************************************************************************
    **
    ** Issue a change message, if needed
    ** 
    */
    
    private boolean issueChangeMessage(BuildInstructionProcessor.BuildChanges bChanges) {
      
  
      ResourceManager rMan = appState_.getRMan();
      Database db = appState_.getDB();    
  
      String message = rMan.getString("buildNetwork.baseChange");
      Set<String> cpi = bChanges.changedParentInstances;
      Set<String> cci = bChanges.changedChildInstances;
      Set<String> csci = bChanges.changedSiblingCousinInstances;
      message = "<html><center>" + message;
      
      boolean doit = false;
      
      if ((cpi != null) && !cpi.isEmpty()) {
        int size = cpi.size();
        String firstID = cpi.iterator().next();
        Genome gi = db.getGenome(firstID);
        String name = gi.getName();
        String form = (size == 1) ? rMan.getString("buildNetwork.oneParentChange")
                                  : rMan.getString("buildNetwork.multiParentChange");
        String desc = MessageFormat.format(form, new Object[] {name});
        message = message + "<br><br>" + desc;
        doit = true;
      }
  
      if ((cci != null) && !cci.isEmpty()) {
        int size = cci.size();
        String firstID = cci.iterator().next();
        Genome gi = db.getGenome(firstID);
        String name = gi.getName();
        String form = (size == 1) ? rMan.getString("buildNetwork.oneChildChange")
                                  : rMan.getString("buildNetwork.multiChildChange");
        String desc = MessageFormat.format(form, new Object[] {name});
        message = message + "<br><br>" + desc;
        doit = true;
      }
      
      if ((csci != null) && !csci.isEmpty()) {
        int size = csci.size();
        String firstID = csci.iterator().next();
        Genome gi = db.getGenome(firstID);
        String name = gi.getName();
        String form = (size == 1) ? rMan.getString("buildNetwork.oneCousinSiblingChange")
                                  : rMan.getString("buildNetwork.multiCousinSiblingChange");
        String desc = MessageFormat.format(form, new Object[] {name});
        message = message + "<br><br>" + desc;
        doit = true;
      }    
  
      if (doit) {
        message = message + "<br><br>" + rMan.getString("buildNetwork.changeSuffix") + "</center></html>";
        int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), message, 
                                               rMan.getString("buildNetwork.changeWarningTitle"),
                                               JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return (false);
        }
      }
  
      return (true);    
    }
    
    /***************************************************************************
    **
    ** Extract integer while hiding exception.
    */
     
    private Integer extractIntValue(String intString) {
      try {
        return (Integer.valueOf(intString));
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException();
      }
    }
    
    /***************************************************************************
    **
    ** Type mismatch checker
    */
     
    private String checkMismatch(String nodeName, List<EnumCellNodeType> typeCol, int row, Map<String, EnumCellNodeType> oldTypes) {   
      String norm = DataUtil.normKey(nodeName);
      EnumCellNodeType newType = typeCol.get(row); 
      Integer nType = extractIntValue(newType.internal);
      EnumCellNodeType oldType = oldTypes.get(norm);   
      if (oldType == null) {
        oldTypes.put(norm, newType);
      } else if (!nType.equals(extractIntValue(oldType.internal))) {
        ResourceManager rMan = appState_.getRMan();
        String desc = MessageFormat.format(rMan.getString("buildNetwork.inconsistentTypes"), 
                                           new Object[] {nodeName, oldType.display, newType.display}); 
        return (desc);
      }
      return (null);
    }
    
    /***************************************************************************
    **
    ** Fill in the type map
    */
     
    private void initTypeMap(Map<String, EnumCellNodeType> typeMap, String nodeName, int nodeType) { 
      if ((nodeTypeList_ == null) || (nodeTypeList_.size() == 0)) {
        throw new IllegalStateException();
      }
      int nsize = nodeTypeList_.size();
      for (int i = 0; i < nsize; i++) {
        EnumCellNodeType ecnt = nodeTypeList_.get(i);
        if (ecnt.internal.equals(Integer.toString(nodeType))) {
          String nameKey = DataUtil.normKey(nodeName);
          EnumCellNodeType existing = typeMap.get(nameKey);
          //
          // FIXME: A crash has been seen here after wkst net build - mixed node types
          //
          if ((existing != null) && (!existing.internal.equals(Integer.toString(nodeType)))) {
            throw new IllegalStateException();
          }
          typeMap.put(nameKey, ecnt);
          return;
        }
      }
      throw new IllegalStateException();
    }
  
    /***************************************************************************
    **
    ** Return existing type
    */
     
    private EnumCellNodeType findExistingType(String nodeName, Map<String, EnumCellNodeType> oldTypes) {
      String norm = DataUtil.normKey(nodeName);
      EnumCellNodeType oldType = oldTypes.get(norm);
      return (oldType);
    }  
  
    /***************************************************************************
    **
    ** Figure out the complexity we need:
    */
     
    private int requiredComplexity(List<BuildInstruction> instructions) {    
      //
      // Assume the simplest:
      //    
      int retval = ONLY_GENES_INTRA_REGION_;    
      int numInt = instructions.size();
      for (int i = 0; i < numInt; i++) {
        BuildInstruction bi = instructions.get(i);
        int complex = instructionComplexity(bi);
        if (complex > retval) {
          retval = complex;
        }
      }
      return (retval);
    }  
  
    /***************************************************************************
    **
    ** Answer if we can accomodate the instruction with a non-advanced
    ** format:
    */
    
    private int instructionComplexity(BuildInstruction bi) {     
      if (!(bi instanceof GeneralBuildInstruction)) {
        return (ONLY_GENES_INTRA_REGION_);
      }
      
      boolean mixedTypes = false;
      GeneralBuildInstruction gbi = (GeneralBuildInstruction)bi;
      if (!((gbi.getSourceType() == Node.GENE) && 
            (gbi.getTargType() == Node.GENE) &&
            (gbi.getLinkSign() != GeneralBuildInstruction.NEUTRAL))) {
        mixedTypes = true;
      }
  
      boolean simpleRegions = true;          
      if (gbi.hasRegions()) {
        Iterator<InstructionRegions.RegionTuple> tupit = gbi.getRegions().getRegionTuples();
        while (tupit.hasNext()) {
          InstructionRegions.RegionTuple tuple = tupit.next();             
          if (!tuple.sourceRegion.equals(tuple.targetRegion)) {
            simpleRegions = false;
            break;
          }   
        }  
      }
      
      if (!mixedTypes && simpleRegions) {
        return (ONLY_GENES_INTRA_REGION_);
      } else if (simpleRegions) {
        return (ALL_TYPES_INTRA_REGION_);
      } else {
        return (ALL_TYPES_INTER_REGION_);
      }
    }  

 
  
    /***************************************************************************
    **
    ** Build node type list.  Return the index of the gene type.
    */
    
    private int buildNodeTypeList(List<EnumCellNodeType> fillIt) {    
      ResourceManager rMan = appState_.getRMan();
      StringBuffer buf = new StringBuffer();
      int index = 0;
      int retval = -1;
      for (int i = BuildInstruction.MIN_NODE_TYPE; i <= BuildInstruction.MAX_NODE_TYPE; i++) {
        if (i == Node.GENE) {
          retval = index;
        }
        buf.setLength(0);
        buf.append("buildInstruction.");
        buf.append(BuildInstruction.mapToNodeTypeTag(i));
        String display = rMan.getString(buf.toString());
        fillIt.add(new EnumCellNodeType(display, Integer.toString(i), index++));
      }
      if (retval == -1) {
        throw new IllegalStateException();
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Build dummy Node list
    */
    
    private List<EnumCellNodeType> buildDummyList(List<EnumCellNodeType> typeList) {
      List<EnumCellNodeType> retval = new ArrayList<EnumCellNodeType>();
      int nsize = typeList.size();
      for (int i = 0; i < nsize; i++) {
        EnumCellNodeType ecnt = typeList.get(i);
        if (ecnt.internal.equals(Integer.toString(Node.GENE))) {
          retval.add(new EnumCellNodeType(ecnt));
          break;
        }
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Build region set
    */
     
    private MultiChoiceEditor.ChoiceEntrySet buildRegionSet(List<InstanceInstructionSet.RegionInfo> regions) {
      int size = regions.size();    
      MultiChoiceEditor.ChoiceEntry[] entries = new MultiChoiceEditor.ChoiceEntry[size];
      for (int i = 0; i < size; i++) {
        InstanceInstructionSet.RegionInfo reg = regions.get(i);
        entries[i] = new MultiChoiceEditor.ChoiceEntry(reg.abbrev, reg.abbrev, false);
      }
      return (new MultiChoiceEditor.ChoiceEntrySet(entries));
    }
  
    /***************************************************************************
    **
    ** Build region list
    */
    
    private List<EnumCellRegionType> buildRegionList(List<InstanceInstructionSet.RegionInfo> regions) {    
      ResourceManager rMan = appState_.getRMan();
      ArrayList<EnumCellRegionType> retval = new ArrayList<EnumCellRegionType>();
      int index = 0;
      String display = rMan.getString("buildInstruction.noRegionChosen");
      retval.add(new EnumCellRegionType(display, null, index++));
      int size = regions.size();
      for (int i = 0; i < size; i++) {
        InstanceInstructionSet.RegionInfo reg = regions.get(i);
        retval.add(new EnumCellRegionType(reg.name, reg.abbrev, index++));
      }
      return (retval);
    }
    
    /***************************************************************************
    **
    ** Handle changes in regions.  V7: We add new regions to the list if it via inheritance!
    ** 
    */
    
    private void updateRegions(List<InstanceInstructionSet.RegionInfo> newRegions, boolean fromInherit) {   
      
      if (fromInherit) {
        Iterator<InstanceInstructionSet.RegionInfo> nrit = newRegions.iterator();
        while (nrit.hasNext()) {
          InstanceInstructionSet.RegionInfo nextReg = nrit.next();
          if (!workingRegions_.contains(nextReg)) {
            workingRegions_.add(nextReg);
          }
        }
      } else { 
        workingRegions_ = newRegions;
      }
      regionSet_ = buildRegionSet(workingRegions_);
      regionList_ = buildRegionList(workingRegions_);
      
      int showChoice = showLevelCombo_.getSelectedIndex();
      updateSimpleRegions(showChoice);
      updateMediumRegions(showChoice);
      updateComplexRegions(showChoice);
      updateSignalRegions(showChoice);    
      updateLoneNodeRegions(showChoice);
      return;
    }
    
    /***************************************************************************
    **
    ** Handle setting the types of instructions shown
    ** 
    */
    
    private void setShowLevel(int showChoice) {
      showLevel_ = showChoice;        
      setSimpleShowLevel(showChoice);
      setMediumShowLevel(showChoice);
      setComplexShowLevel(showChoice);
      setSignalShowLevel(showChoice);
      setLoneNodeShowLevel(showChoice);
      return;
    }  
  
    /***************************************************************************
    **
    ** Helper
    ** 
    */  
    
    private void dropObsoleteRegionHelper(ArrayList<EnumCellRegionType> dataCol) {
      int rsize = regionList_.size();
      int dcSize = dataCol.size();
      for (int i = 0; i < dcSize; i++) {
        EnumCellRegionType src = dataCol.get(i);
        boolean retain = false;
        for (int j = 0; j < rsize; j++) {
          EnumCellRegionType ecrt = regionList_.get(j);
          if (ecrt.internalMatch(src.internal)) {
            retain = true;
            src.value = j;
            break;
          }
        }
        if (!retain) {
          dataCol.set(i, new EnumCellRegionType(regionList_.get(0)));  
        }
      }
      return;
    }
    
    /***************************************************************************
    **
    ** Answer if the given parent model instruction can be inherited:
    */  
  
    boolean isInheritable(BuildInstruction parentBi) {
      //
      // To be inheritable, we need to have both the regions present in at least one tuple.
      //
      InstructionRegions ir = parentBi.getRegions();
      Iterator<InstructionRegions.RegionTuple> rtit = ir.getRegionTuples();
      int wSize = workingRegions_.size();
      while (rtit.hasNext()) {
        InstructionRegions.RegionTuple tup = rtit.next();
        boolean haveSrc = false;
        boolean haveTrg = false;
        for (int i = 0; i < wSize; i++) {
          InstanceInstructionSet.RegionInfo reg = workingRegions_.get(i);
          if (reg.abbrev.equals(tup.sourceRegion)) {
            haveSrc = true;
          }
          if (reg.abbrev.equals(tup.targetRegion)) {
            haveTrg = true;
          }
          if (haveSrc && haveTrg) {
            return (true);
          }
        }
      }
      return (false);
    }
    
    /***************************************************************************
    **
    ** Get a row deletion confirmation
    */  
  
    private boolean confirmRowDeletion() {
      ResourceManager rMan = appState_.getRMan();
      String message = rMan.getString("buildNetwork.confirmRowDelete");
      // FYI: the last string is the client key for tracking answer, not a rMan key!
      return (YesNoShutupDialog.launchIfNeeded(appState_, appState_.getTopFrame(), "buildNetwork.confirmDeleteTitle", 
                                               message, "BuildNetworkDialog.rowDelete"));
    }
    
    /***************************************************************************
    **
    ** Decide if we are losing an instance from a rows deletion
    */  
  
    private boolean losingAnInstance(TabData losing, int[] rows, TabData[] currTd) {
      
      //
      // We are only losing if an element from the lost set is not present in
      // any other set;
      //
  
      HashSet<String> existing = new HashSet<String>();
      for (int i = 0; i < currTd.length; i++) {
        if (currTd[i] != losing) {
          existing.addAll(currTd[i].tm.existingInstances());
        }
      }
       
      Set<String> losingInstances = losing.tm.removedInstances(rows);
      if (losingInstances.isEmpty()) {
        return (false);
      }
      losingInstances.removeAll(existing);
      
      return (!losingInstances.isEmpty());
    }
    
    /***************************************************************************
    **
    ** Signal active editing to stop
    ** 
    */
    
    private void stopTheEditing() {
      if (complexityLevel_ == ONLY_GENES_INTRA_REGION_) {
        finishSimpleTabEditing(false);
      } else if (complexityLevel_ == ALL_TYPES_INTRA_REGION_) {
        finishMediumTabEditing(false);
      } else {
        finishComplexTabEditing(false);
      }
      finishLoneNodeTabEditing(false);
      finishSignalTabEditing(false);
      tabPane_.revalidate();
      tabPane_.repaint();
      return;
    }
    
    
    /***************************************************************************
    **
    ** Ask if we need to change all node types:
    ** 
    */
    
    private boolean askToChangeNodeType(String nodeName, EnumCellNodeType newType) {
      int nameCount = 0;
      for (int i = 0; i < 3; i++) {
        nameCount += allCurrTabData_[i].tm.hasNameInRows(nodeName);
      }
      if (nameCount < 2) {
        return (true);
      }
     
      ResourceManager rMan = appState_.getRMan();
      String form = rMan.getString("buildNetwork.chooseTypeChange");                               
      String desc = MessageFormat.format(form, new Object[] {nodeName, newType.display});        
      
      int ok = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                             desc,
                                             rMan.getString("buildNetwork.chooseTypeChangeTitle"),
                                             JOptionPane.YES_NO_OPTION);
      if (ok != JOptionPane.YES_OPTION) {
        return (false);
      }    
      
  
      return (true);                                            
    }
    
    /***************************************************************************
    **
    ** Support for changing types:
    ** 
    */
    
    private boolean comboEditingDoneSupport(List<String> nameList, List<EnumCellNodeType> typeList, int rowNum, 
                                            Map<String, EnumCellNodeType> typeTracker, EnumCellNodeType newType) {
      EnumCellNodeType currtype = typeList.get(rowNum);
      if (currtype.internal.equals(newType.internal)) {
        return (true);
      }  
      String nameVal = nameList.get(rowNum);
      if ((nameVal == null) || nameVal.trim().equals("")) {
        return (true);
      }
      nameVal = nameVal.trim();
      if (canChangeNodeType(nameVal, newType) && askToChangeNodeType(nameVal, newType)) { 
        typeTracker.put(DataUtil.normKey(nameVal), newType);
        for (int i = 0; i < 3; i++) {
          allCurrTabData_[i].tm.batchTypeChange(nameVal, newType);
        }
        for (int i = 0; i < 3; i++) {
          allCurrTabData_[i].jt.revalidate();
          allCurrTabData_[i].jt.repaint();
        }
        return (true);
      }
      return (false);
    }
  
    /***************************************************************************
    **
    ** If we are trying to change a node type, and it is fixed, we will balk.
    ** 
    */
    
    private boolean canChangeNodeType(String nodeName, EnumCellNodeType newType) {
      int nameCount = 0;
      for (int i = 0; i < 3; i++) {
        nameCount += allCurrTabData_[i].tm.hasNameInRows(nodeName);
      }
      if (nameCount < 2) {
        return (true);
      }
      boolean canChangeType = true;
      for (int i = 0; i < 3; i++) {
        canChangeType = canChangeType && allCurrTabData_[i].tm.canChangeType(nodeName, newType);
      }
      if (!canChangeType) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("buildNetwork.cannotChangeType"), 
                                      rMan.getString("buildNetwork.cannotChangeTypeTitle"),
                                      JOptionPane.ERROR_MESSAGE);
         return (false);
      }
      return (true);                                            
    }  
    
    /***************************************************************************
    **
    ** Cannot use a name for a fixed type if it is used elsewhere
    ** 
    */
    
    private void cannotUseEnteredName(String nodeName, EnumCellNodeType newType) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("buildNetwork.cannotUseNameTypeMismatch"), 
                                    rMan.getString("buildNetwork.cannotUseNameTypeMismatchTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return;                                            
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  abstract static class EnumCell {
    int value;
    String display;
    String internal;
    
    EnumCell(String display, String internal, int val) {
      this.display = display;
      this.internal = internal;
      this.value = val;      
    }
    
    EnumCell(EnumCell other) {
      this.display = other.display;
      this.internal = other.internal;
      this.value = other.value;      
    }
       
    boolean doReplace(EnumCell other) {
      if (this.internal == null) {
        return (other.internal != null);
      }
      return (!this.internal.equals(other.internal));
    }
    
    boolean internalMatch(Object other) {
      if (this.internal == null) {
        return (other == null);
      }
      return (this.internal.equals(other));
    }
  }
  
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  static class EnumCellSign extends EnumCell {
    EnumCellSign(String display, String internal, int val) {
      super(display, internal, val);
    }
    EnumCellSign(EnumCellSign other) {
      super(other);
    }    
  }
  
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  static class EnumCellGeneralSign extends EnumCell {
    EnumCellGeneralSign(String display, String internal, int val) {
      super(display, internal, val);
    }
    EnumCellGeneralSign(EnumCellGeneralSign other) {
      super(other);
    }    
  }  
   
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  static class EnumCellNodeType extends EnumCell {
    EnumCellNodeType(String display, String internal, int val) {
      super(display, internal, val);
    }
    EnumCellNodeType(EnumCellNodeType other) {
      super(other);
    }    
  }
  
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  static class EnumCellSignalType extends EnumCell {
    EnumCellSignalType(String display, String internal, int val) {
      super(display, internal, val);
    }
    EnumCellSignalType(EnumCellSignalType other) {
      super(other);
    }    
  }
  
  /***************************************************************************
  **
  ** Used for the combo entries
  */
  
  static class EnumCellRegionType extends EnumCell {
    EnumCellRegionType(String display, String internal, int val) {
      super(display, internal, val);
    }
    EnumCellRegionType(EnumCellRegionType other) {
      super(other);
    }    
  }  

 /***************************************************************************
 **
 ** User input data
 ** 
 */

  public static class RegionInfoRequest implements ServerControlFlowHarness.UserInputs {
   
    private boolean haveResult; 
    public boolean forApply_;
    public List<BuildInstruction> buildCmds;
    public List<InstanceInstructionSet.RegionInfo> workingRegions;
    public boolean keepLayout; 
    public boolean globalDelete;
    public SpecialtyLayoutEngine.SpecialtyType strat;
    public SpecialtyLayoutEngineParams params;
    public DesktopDialog horribleHack;
 
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }  
    public boolean isForApply() {
      return (forApply_);
    } 
  } 
}
