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


package org.systemsbiology.biotapestry.ui.dialogs;


import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.export.ExportExpression;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** Dialog box for choosing Time Course CSV export options
*/

public class TimeCourseCSVExportOptionsDialogFactory extends DialogFactory {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TimeCourseCSVExportOptionsDialogFactory(ServerControlFlowHarness cfh) {
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
   
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, buildChoices(appState.getRMan())));
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
  ** Build choices
  ** 
  */
  
  private Vector<TrueObjChoiceContent> buildChoices(ResourceManager rMan) {
    Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();           
    retval.add(new TrueObjChoiceContent(rMan.getString("tCourseCSVOptions.groupTimes"), new Boolean(false)));
    retval.add(new TrueObjChoiceContent(rMan.getString("tCourseCSVOptions.groupRegions"), new Boolean(true)));
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  public class DesktopDialog extends BTTransmitResultsDialog {
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JCheckBox embedConfidence_;
    private JCheckBox skipInterals_;
    private JComboBox orderingOptionCombo_;
     
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
    
    private DesktopDialog(ServerControlFlowHarness cfh, Vector<TrueObjChoiceContent> tocc) {     
      super(cfh, "tCourseCSVOptions.title", new Dimension(400, 250), 4, new ExportExpression.TimeCourseExportSettings(), false);
    
      JLabel orderingLabel = new JLabel(rMan_.getString("tCourseCSVOptions.orderingOptions"));
      orderingOptionCombo_ = new JComboBox(tocc);
      embedConfidence_ = new JCheckBox(rMan_.getString("tCourseCSVOptions.embedConfidence"), true);   
      skipInterals_ = new JCheckBox(rMan_.getString("tCourseCSVOptions.skipInternals"), true);
    
      addLabeledWidget(orderingLabel, orderingOptionCombo_, false, true);
      addWidgetFullRow(embedConfidence_, false);
      addWidgetFullRow(skipInterals_, false);
      
      finishConstruction();
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED/PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Do the bundle 
    ** 
    */    
    protected boolean bundleForExit(boolean forApply) {
      ExportExpression.TimeCourseExportSettings export = (ExportExpression.TimeCourseExportSettings)request_;
      export.embedConf = embedConfidence_.isSelected();
      export.skipInt = skipInterals_.isSelected();
      export.ordByReg = ((Boolean)((TrueObjChoiceContent)orderingOptionCombo_.getSelectedItem()).val).booleanValue();
      return (true);
    }
  }
}
