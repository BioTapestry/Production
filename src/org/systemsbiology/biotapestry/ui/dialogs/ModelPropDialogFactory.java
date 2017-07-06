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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.EditModelProps.ModelProperties;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.ui.DataLocator;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.TimeAxisHelper;
import org.systemsbiology.biotapestry.ui.xplat.XPlatPrimitiveElementFactory;
import org.systemsbiology.biotapestry.ui.xplat.XPlatUIElement;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;

/*************************
 * ModelPropDialogFactory
 *************************
 *
 * Factory which produces Dialogs that allow you to edit model properties:
 * 
 * 	SingleInstanceModelPropDialog
 * 	SingleModelPropDialog
 * 	DynSingleModelPropDialog
 * 
 *
 */

public class ModelPropDialogFactory extends DialogFactory {



	public ModelPropDialogFactory(ServerControlFlowHarness cfh) {
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

		ModelPropsBuildArgs mpba = (ModelPropsBuildArgs)ba;

		if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
			return (new DesktopDialog(cfh, mpba.props));
		} else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
			return null; //(new SerializableDialog(cfh, mpba.props));   
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
	
	public static class ModelPropsBuildArgs extends DialogBuildArgs { 

		String modelID;
		ModelProperties props;

		public ModelPropsBuildArgs(ModelProperties props) {
			super(null);
			this.modelID = props.modelID;
			this.props = props;
		} 
	}

	//////////////////
	// Dialog Types
	//////////////////
	//
	//
	public enum ModelPropType {
		SINGLE_INSTANCE,
		DYNAMIC_SINGLE_MODEL,
		SINGLE_MODEL
	};
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// DIALOG CLASSES
	//
	////////////////////////////////////////////////////////////////////////////    


	/****************************************************************************
	 **
	 ** Desktop Platform dialog box for editing a single model's properties
	 */
	
	public class DesktopDialog extends BTTransmitResultsDialog implements DesktopDialogPlatform.Dialog, ListWidgetClient {


		////////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INSTANCE MEMBERS
		//
		////////////////////////////////////////////////////////////////////////////  
		
		private ModelPropType type_;
		private ModelProperties props_;

		// Properties for all types
		private JTextField longNameField_;
		private JTextField descripField_;
		
		// SingleInstanceModel & DynamicSingle
		private JTextField modelNameField_;
		private int minParentTime_;
		private int maxParentTime_;
		private int minChildTime_;
		private int maxChildTime_;				
		private JTextField minField_;
		private JTextField maxField_;  
		
		// SingleInstanceModel
		private JLabel minFieldLabel_;
		private JLabel maxFieldLabel_;  
		private JCheckBox timeBoundsBox_;
		private boolean lineageIsBounded_;
		private TimeAxisHelper timeAxisHelper_; 
		
		// DynamicSingleModel
		private JComboBox typeCombo_;
		private JComboBox skeyCombo_;
		private JCheckBox showDiffBox_;
		private JLabel skLabel_;
		private DynamicInstanceProxy proxy_;
		private DynamicInstanceProxy parentProx_;
		private List proxyChildren_;
		private ListWidget lw_;
		private List<ObjChoiceContent> extras_;
		private HashMap<String, String> extraGroups_;
		private NavTree nt_;
		private TreeNode popupNode_;
		private TimeAxisDefinition tad_;
		private boolean namedStages_;  
		
		private UndoFactory uFac_;
		private DataAccessContext dacx_;
		
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

		public DesktopDialog(ServerControlFlowHarness cfh, ModelProperties props) {     
			super(cfh, "smprop.title", new Dimension(700,200), 1, new ModelRequest(), true);
			props_ = props;
			uFac_ = cfh.getUndoFactory();
			dacx_ = cfh.getDataAccessContext();
			
			if (props.popupModel instanceof DynamicGenomeInstance) {
				type_ = ModelPropType.DYNAMIC_SINGLE_MODEL;
			} else if(props.popupModel instanceof DBGenome) {
				type_ = ModelPropType.SINGLE_MODEL;
			} else {
				type_ = ModelPropType.SINGLE_INSTANCE;
			}
			
			ResourceManager rMan = uics_.getRMan();    
			setSize(700, 200);
			JPanel cp = (JPanel)getContentPane();
			cp.setBorder(new EmptyBorder(20, 20, 20, 20));
			cp.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			int rowNum = 0;
			
			if(type_ != ModelPropType.DYNAMIC_SINGLE_MODEL) {
				rowNum = longNameDescFields(cp,rowNum,gbc,rMan);
			}
			
			if(type_ == ModelPropType.SINGLE_INSTANCE) {
				rowNum = modelNameField(cp,rowNum,gbc,rMan);
				rowNum = singleInstTimeFields(cp,rowNum,gbc,rMan);
			}
			
			if(type_ == ModelPropType.DYNAMIC_SINGLE_MODEL) {
				rowNum = dynInstTimeFields(cp,rowNum,gbc,rMan);
				rowNum = modelNameField(cp,rowNum,gbc,rMan);
				rowNum = dynInstLowerFields(cp,rowNum,gbc,rMan);
			}

			

			//
			// Build the button panel:
			//

			FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
			buttonO.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						okAction();
					} catch (Exception ex) {
						uics_.getExceptionHandler().displayException(ex);
					}
				}
			});     
			FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
			buttonC.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						closeAction();
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
			UiUtil.gbcSet(gbc, 0, rowNum, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
			cp.add(buttonPanel, gbc);
			setLocationRelativeTo(uics_.getTopFrame());
			displayProperties();
		}

		public boolean dialogIsModal() {
      return (true);
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
		
		private int modelNameField(JPanel cp, int rowNum, GridBagConstraints gbc, ResourceManager rMan) {
			
		    JLabel label = new JLabel(rMan.getString("simprop.name"));
		    modelNameField_ = new JTextField();
		    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
		    cp.add(label, gbc);

		    UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
		    cp.add(modelNameField_, gbc);
		    
		    return rowNum;
		}
		
		private int longNameDescFields(JPanel cp, int rowNum, GridBagConstraints gbc, ResourceManager rMan) {	    
			
			//
			// Build the long name panel:
			//

			JLabel label = new JLabel(rMan.getString("smprop.longName"));
			longNameField_ = new JTextField();
			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
			cp.add(label, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
			cp.add(longNameField_, gbc);

			//
			// Build the description panel:
			//

			label = new JLabel(rMan.getString("smprop.descrip"));
			descripField_ = new JTextField();
			UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
			cp.add(label, gbc);

			UiUtil.gbcSet(gbc, 1, rowNum++, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
			cp.add(descripField_, gbc);
			
			return rowNum;			
		}
		
		
		private int singleInstTimeFields(JPanel cp, int rowNum, GridBagConstraints gbc, ResourceManager rMan) {
			
		    //
		    // Time bounds box and times.  We cannot change our bounding state unless we are the root instance.
		    //
		    
		    timeBoundsBox_ = new JCheckBox(rMan.getString("simprop.timeBounds"));
		    if (!dacx_.currentGenomeIsRootInstance()) {
		      timeBoundsBox_.setEnabled(false);
		    }    

		    UiUtil.gbcSet(gbc, 0, rowNum++, 6, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
		    cp.add(timeBoundsBox_, gbc);
		    timeBoundsBox_.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ev) {
		        try {
		          boolean enabled = timeBoundsBox_.isSelected();
		          if (enabled && !timeAxisHelper_.establishTimeAxis(dacx_.getExpDataSrc(), dacx_)) {          
		            timeBoundsBox_.setSelected(false);  // gonna cause a callback!
		            return;
		          }
		          minField_.setEnabled(enabled);
		          minFieldLabel_.setEnabled(enabled);
		          maxField_.setEnabled(enabled);
		          maxFieldLabel_.setEnabled(enabled);
		        } catch (Exception ex) {
		          uics_.getExceptionHandler().displayException(ex);
		        }
		      }
		    });

		    //
		    // Build the minimum and maxiumum times:
		    //

		    minFieldLabel_ = new JLabel("");
		    maxFieldLabel_ = new JLabel(""); 
		    
		    timeAxisHelper_ = new TimeAxisHelper(uics_, this, minFieldLabel_, maxFieldLabel_, cfh.getTabSource(), uFac_);    
		    timeAxisHelper_.fixMinMaxLabels(false, dacx_.getExpDataSrc().getTimeAxisDefinition());

		    minField_ = new JTextField();
		    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
		    cp.add(minFieldLabel_, gbc);

		    UiUtil.gbcSet(gbc, 1, rowNum, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
		    cp.add(minField_, gbc);
		    
		    maxField_ = new JTextField();     
		    UiUtil.gbcSet(gbc, 3, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
		    cp.add(maxFieldLabel_, gbc);
		    
		    UiUtil.gbcSet(gbc, 4, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
		    cp.add(maxField_, gbc);
			
			return rowNum;
		}
		
		private int dynInstTimeFields(JPanel cp, int rowNum, GridBagConstraints gbc, ResourceManager rMan) {
			
			 tad_ = dacx_.getExpDataSrc().getTimeAxisDefinition();
			    namedStages_ = tad_.haveNamedStages();
			    String displayUnits = tad_.unitDisplayString();    
			    
			    //
			    // Build the timely choice:
			    //
			    
			    JLabel label = new JLabel(rMan.getString("dsmprop.type"));
			    Vector<String> choices = new Vector<String>();
			    String perTime = MessageFormat.format(rMan.getString("dicreate.perTimeChoice"), new Object[] {displayUnits});
			    choices.add(perTime);    
			    choices.add(rMan.getString("dicreate.sumChoice")); 
			    typeCombo_ = new JComboBox(choices);

			    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
			    cp.add(label, gbc);
			    
			    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
			    cp.add(typeCombo_, gbc);
			    
			    //
			    // Build the minimum time:
			    //

			    String minLab = MessageFormat.format(rMan.getString("dsmprop.minTime"), new Object[] {displayUnits});    
			    label = new JLabel(minLab);
			    minField_ = new JTextField();
			    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
			    cp.add(label, gbc);

			    UiUtil.gbcSet(gbc, 4, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
			    cp.add(minField_, gbc);
			    
			    //
			    // Build the maximum time:
			    //

			    String maxLab = MessageFormat.format(rMan.getString("dsmprop.maxTime"), new Object[] {displayUnits});    
			    label = new JLabel(maxLab);
			    maxField_ = new JTextField();
			    UiUtil.gbcSet(gbc, 5, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
			    cp.add(label, gbc);

			    UiUtil.gbcSet(gbc, 6, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
			    cp.add(maxField_, gbc);
			
			return rowNum;
		}
		
		private int dynInstLowerFields(JPanel cp, int rowNum, GridBagConstraints gbc, ResourceManager rMan) {
			
		    JLabel label = new JLabel(rMan.getString("dsmprop.showAsDiff"));
		    showDiffBox_ = new JCheckBox();
		   
		    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
		    cp.add(label, gbc);
		    
		    UiUtil.gbcSet(gbc, 1, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
		    cp.add(showDiffBox_, gbc);
		 
		    skLabel_ = new JLabel(rMan.getString("dsmprop.simKeyOptions"));
		    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
		    Vector<ObjChoiceContent> skeys = tcd.buildSimKeyCombo();
		    skeyCombo_ = new JComboBox(skeys);
		    UiUtil.gbcSet(gbc, 2, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
		    cp.add(skLabel_, gbc);
		    
		    UiUtil.gbcSet(gbc, 3, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
		    cp.add(skeyCombo_, gbc);
		    skLabel_.setEnabled(false);
		    skeyCombo_.setEnabled(false);
		    
		    
		    showDiffBox_.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ev) {
		        try {
		          boolean isSel = showDiffBox_.isSelected();
		          skLabel_.setEnabled(isSel);
		          skeyCombo_.setEnabled(isSel);
		        } catch (Exception ex) {
		          uics_.getExceptionHandler().displayException(ex);
		        }
		      }
		    });      

		    //
		    // Build the extra added node list:
		    //
		    
		    int row = 3;
		    if (proxy_.hasAddedNodes()) {
		      label = new JLabel(rMan.getString("dsmprop.extra"));
		      UiUtil.gbcSet(gbc, 0, row, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 1.0);
		      cp.add(label, gbc);      
		      generateAddedNodes();
		      lw_ = new ListWidget(uics_.getHandlerAndManagerSource(), extras_, this, (parentProx_ == null) ? ListWidget.DELETE_EDIT : ListWidget.DELETE_ONLY); 
		      UiUtil.gbcSet(gbc, 1, row, 6, 2, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
		      cp.add(lw_, gbc);
		      row += 2;
		    }
			
			return rowNum;
		}
		
		
		  /***************************************************************************
		  **
		  ** Generate the list of added nodes
		  ** 
		  */
		  
		  private void generateAddedNodes() {
		    ResourceManager rMan = dacx_.getRMan();
		    extras_ = new ArrayList<ObjChoiceContent>();
		    extraGroups_ = new HashMap<String, String>();
		   
		    String format = rMan.getString("addedNode.nameFormat");
		    String multi = rMan.getString("addedNode.multiSuffix");
		    String noName = rMan.getString("tirmd.noName");
		    GenomeInstance gi = proxy_.getAnInstance();
		    GenomeInstance giRoot = gi.getVfgParentRoot();
		    GenomeSource gsrc = dacx_.getGenomeSource();
		    TimeCourseDataMaps tcdm = dacx_.getDataMapSrc().getTimeCourseDataMaps();
		    
		    Iterator<DynamicInstanceProxy.AddedNode> anit = proxy_.getAddedNodeIterator();
		    while (anit.hasNext()) {
		      DynamicInstanceProxy.AddedNode an = anit.next();
		      Node node = giRoot.getNode(an.nodeName);
		      String nodeName = node.getName();
		      if ((nodeName == null) || (nodeName.trim().equals(""))) {
		        nodeName = noName;
		      }
		      List<TimeCourseDataMaps.TCMapping> tcms = tcdm.getTimeCourseTCMDataKeysWithDefault(GenomeItemInstance.getBaseID(an.nodeName), gsrc);
		      ObjChoiceContent occ;
		      if ((tcms == null) || (tcms.size() == 0)) {
		        occ = new ObjChoiceContent(nodeName, an.nodeName);
		      } else if (tcms.size() == 1) {
		        TimeCourseDataMaps.TCMapping tcm = tcms.get(0);
		        String desc = MessageFormat.format(format, new Object[] {nodeName, tcm.name});         
		        occ = new ObjChoiceContent(desc, an.nodeName);    
		      } else {
		        String multiName = nodeName + multi;
		        String desc = MessageFormat.format(format, new Object[] {nodeName, multiName});         
		        occ = new ObjChoiceContent(desc, an.nodeName);
		      }
		      extras_.add(occ);
		      extraGroups_.put(an.nodeName, an.groupToUse);
		    }
		    
		    return;
		  }  
		

		/***************************************************************************
		 **
		 ** Apply the current model property values to our UI components
		 ** 
		 */

		private void displayProperties() {
			
			Genome theGenome = dacx_.getCurrentGenome();
			
			if(type_ != ModelPropType.DYNAMIC_SINGLE_MODEL) {
				longNameField_.setText(props_.longName);
				descripField_.setText(props_.description);				
			}

			if(type_ != ModelPropType.SINGLE_MODEL) {
				modelNameField_.setText(props_.modelName);
			}

			if(type_ == ModelPropType.SINGLE_INSTANCE) {
			    boolean hasTimeBounds = ((GenomeInstance)theGenome).hasTimeBounds();
			    timeBoundsBox_.setSelected(hasTimeBounds);
			    minFieldLabel_.setEnabled(hasTimeBounds);
			    minField_.setEnabled(hasTimeBounds);
			    String minText = (hasTimeBounds) ? timeAxisHelper_.timeValToDisplay(((GenomeInstance)theGenome).getMinTime(),
                                                                              dacx_.getExpDataSrc().getTimeAxisDefinition()) : "";
			    minField_.setText(minText);
			    maxFieldLabel_.setEnabled(hasTimeBounds);    
			    maxField_.setEnabled(hasTimeBounds);
			    String maxText = (hasTimeBounds) ? timeAxisHelper_.timeValToDisplay(((GenomeInstance)theGenome).getMaxTime(), 
			                                                                        dacx_.getExpDataSrc().getTimeAxisDefinition()) : "";
			    maxField_.setText(maxText); 
			}
			
			if(type_ == ModelPropType.DYNAMIC_SINGLE_MODEL) {
			    int index = (proxy_.isSingle()) ? 1 : 0;   
			    typeCombo_.setSelectedIndex(index);
			    if (!proxyChildren_.isEmpty()) {
			      typeCombo_.setEnabled(false);
			    }
			    
			    int minTime = proxy_.getMinimumTime();    
			    int maxTime = proxy_.getMaximumTime();    
			    String initMin = (namedStages_) ? tad_.getNamedStageForIndex(minTime).name : Integer.toString(minTime);
			    String initMax = (namedStages_) ? tad_.getNamedStageForIndex(maxTime).name : Integer.toString(maxTime);

			    minField_.setText(initMin); 
			    maxField_.setText(initMax);    
			    modelNameField_.setText(proxy_.getName());
			    
			    showDiffBox_.setSelected(proxy_.showAsSimDiff());
			    if (proxy_.showAsSimDiff()) {
			      skeyCombo_.setSelectedItem(new ObjChoiceContent(proxy_.getSimKey(), proxy_.getSimKey()));
			      skLabel_.setEnabled(true);
			      skeyCombo_.setEnabled(true);
			    } 
			}
			
			
			return;
		}

		/***************************************************************************
		 **
		 ** Apply our UI values to the model
		 ** 
		 */

		private void applyProperties() {

			//
			// Undo/Redo support
			//

			UndoSupport support = uFac_.provideUndoSupport("undo.smprop", dacx_);  
			String longName = longNameField_.getText();
			GenomeChange gc = dacx_.getCurrentGenomeAsDBGenome().setProperties(dacx_.getCurrentGenome().getName(),  
					longName, 
					descripField_.getText());
			GenomeChangeCmd cmd = new GenomeChangeCmd(gc);
			support.addEdit(cmd);

			if (!longName.trim().equals("")) {
				new DataLocator(uics_.getGenomePresentation(), dacx_).setTitleLocation(support, dacx_.getCurrentGenomeID(), longName);
			}

			ModelChangeEvent mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.UNSPECIFIED_CHANGE);
			support.addEvent(mcev);    
			mcev = new ModelChangeEvent(dacx_.getGenomeSource().getID(), dacx_.getCurrentGenomeID(), ModelChangeEvent.PROPERTY_CHANGE);
			support.addEvent(mcev);

			support.finish();    
			return;
		}

		@Override
		protected boolean bundleForExit(boolean forApply) {
			// TODO Auto-generated method stub
			return false;
		}
		
		
		  /***************************************************************************
		  **
		  ** Add a row to the list
		  */
		  
		  public List addRow(ListWidget widget) {
		    // not used
		    return (null);
		  }
		  
		  /***************************************************************************
		  **
		  ** Delete rows from the ORDERED list
		  */
		  
		  public List deleteRows(ListWidget widget, int[] rows) {
		    for (int i = 0; i < rows.length; i++) {
		      extras_.remove(rows[i]);
		      for (int j = i + 1; j < rows.length; j++) {
		        if (rows[j] > rows[i]) {
		          rows[j]--;
		        }
		      }
		    }
		    return (extras_);   
		  }

		  /***************************************************************************
		  **
		  ** Edit a row from the list
		  */
		  
		  public List editRow(ListWidget widget, int[] selectedRows) {
		    if (selectedRows.length != 1) {
		      throw new IllegalArgumentException();
		    }

		    //
		    // Editing a row means popping up a dialog to let the user change
		    // the region binding.  Only applicable for top-level dynamic instances.
		    //

		    ObjChoiceContent occ = extras_.get(selectedRows[0]);
		    String nodeName = occ.val;
		    String groupToUse = extraGroups_.get(nodeName);

		    GenomeInstance gi = proxy_.getAnInstance();
		    GenomeInstance parent = (gi).getVfgParent();         
		 
		    ArrayList<ObjChoiceContent> choices = new ArrayList<ObjChoiceContent>();
		    ObjChoiceContent defObj = null;
		    Iterator<Group> grit = parent.getGroupIterator();
		    while (grit.hasNext()) {
		      Group group = grit.next();
		      String groupID = group.getID();
		      ObjChoiceContent ccont = new ObjChoiceContent(group.getDisplayName(), 
		                                                    Group.getBaseID(groupID));
		      choices.add(ccont);
		      if (groupID.equals(groupToUse)) {
		        defObj = ccont;
		      } 
		    }

		    Object[] objs = choices.toArray();
		    ObjChoiceContent groupChoice = 
		      (ObjChoiceContent)JOptionPane.showInputDialog(uics_.getTopFrame(), 
		                                                    dacx_.getRMan().getString("dsmprop.extraGroup"), 
		                                                    dacx_.getRMan().getString("dsmprop.extraGroupTitle"),     
		                                                    JOptionPane.QUESTION_MESSAGE, null, 
		                                                    objs, defObj);
		    if (groupChoice != null) { 
		      if (!groupChoice.val.equals(defObj.val)) {
		        extraGroups_.put(nodeName, groupChoice.val);
		      }
		    }
		    
		    return (extras_);
		  }
		  
		  /***************************************************************************
		  **
		  ** Merge rows from the list
		  */
		  
		  public List combineRows(ListWidget widget, int[] selectedRows) {
		    // not used
		    return (null);
		  }

		
		
		
	} // DesktopDialog
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// WEB DIALOG CLASS
	//
	////////////////////////////////////////////////////////////////////////////    

	public static class SerializableDialog implements SerializableDialogPlatform.Dialog { 

		////////////////////////////////////////////////////////////////////////////
		//
		// PRIVATE INSTANCE MEMBERS
		//
		////////////////////////////////////////////////////////////////////////////  

		// The dialog box
		private XPlatUIDialog xplatDialog_;

		private ServerControlFlowHarness scfh_;
		private ResourceManager rMan_; 
		private XPlatPrimitiveElementFactory primElemFac_;

		////////////////////////////////////////////////////////////////////////////
		//
		// PUBLIC CONSTRUCTORS
		//
		////////////////////////////////////////////////////////////////////////////    

		/***************************************************************************
		 **
		 ** Constructor 
		 */ 

		public SerializableDialog(ServerControlFlowHarness cfh, String modelID) { 
			this.scfh_ = cfh;
			this.rMan_ = scfh_.getDataAccessContext().getRMan();
			this.primElemFac_ = new XPlatPrimitiveElementFactory(this.rMan_);

			buildDialog(this.rMan_.getString("groupprop.title"), 400, 650,modelID);
		}
		

		private void buildDialog(String title, int height, int width,String modelID) {

			this.xplatDialog_ = new XPlatUIDialog(title,height,width);
	    	
			
			this.xplatDialog_.setUserInputs(new ModelRequest(true));
		}



		////////////////////////////////////////////////////////////////////////////
		//
		// PUBLIC METHODS
		//
		////////////////////////////////////////////////////////////////////////////


		///////////////
		// getDialog
		//////////////
		//
		//
		public XPlatUIDialog getDialog() {

			Map<String,String> clickActions = new HashMap<String,String>();
			clickActions.put("close", "CLIENT_CANCEL_COMMAND");
			clickActions.put("ok", "DO_NOTHING");
			clickActions.put("apply", "DO_NOTHING");

			List<XPlatUIElement> btns = this.primElemFac_.makeApplyOkCloseButtons(clickActions.get("ok"),clickActions.get("apply"),clickActions.get("close")); 
			

			this.xplatDialog_.setCancel(clickActions.get("cancel"));

			this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);

			return xplatDialog_;
		}	  

		////////////////
		// getDialog
		///////////////
		//
		//
		public XPlatUIDialog getDialog(FlowKey okKeyVal) {

			Map<String,String> clickActions = new HashMap<String,String>();
			clickActions.put("close", "CLIENT_CANCEL_COMMAND");
			clickActions.put("ok", okKeyVal.toString());
			clickActions.put("apply", okKeyVal.toString());

			List<XPlatUIElement> btns = this.primElemFac_.makeApplyOkCloseButtons(clickActions.get("ok"),clickActions.get("apply"),clickActions.get("close")); 

			this.xplatDialog_.setCancel(clickActions.get("cancel"));

			this.xplatDialog_.replaceElementsInCollection("main","bottom",btns);

			return xplatDialog_;
		}

		///////////////
		// isModal()
		//////////////
		//
		//
		public boolean dialogIsModal() {
      return (true);
    }

		// Stub to satisfy interface
		public String getHTML(String hiddenForm) {
			return null;
		}   

		///////////////////
		// checkForErrors
		///////////////////
		//
		//
		public SimpleUserFeedback checkForErrors(UserInputs ui) { 

	    	return null;
		}

		// Stub to satisfy interface
		public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc) {
			// The only SUF to go out will be an error SUF so it doesn't
			// need to be handled
			return null;
		}
	}
	// SerializableDialog
	

	//////////////////
	// ModelRequest
	//////////////////
	//
	//
	public static class ModelRequest implements ServerControlFlowHarness.UserInputs {

		public String nameResult;

		private boolean haveResult;
		private boolean forApply_;

		public ModelRequest() {

		}

		// For serialization when null fields are excluded
		public ModelRequest(boolean forTransit) {
			haveResult = true;
			forApply_ = false;
			
			nameResult = "";
		}

		public void clearHaveResults() {
			haveResult = false;
			return;
		}

		public void setHasResults() {
			this.haveResult = true;
			return;
		}  

		public boolean haveResults() {
			return (haveResult);
		}  

		public boolean isForApply() {
			return (forApply_);
		}

		// setters/getters for serialization

		public void setForApply(boolean forApply) {
			forApply_ = forApply;
			return;
		}

		public boolean getForApply() {
			return this.forApply_;
		}

		public boolean getHaveResult() {
			return haveResult;
		}

		public void setHasResults(boolean haveResult) {
			this.haveResult = haveResult;
		}
	} // ModelRequest
	
}
