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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Factory to create dialogs for specifying data sharing for new model tabs
*/

public class SetDataSharingDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SetDataSharingDialogFactory(ServerControlFlowHarness cfh) {
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
      return (new DesktopDialog(cfh, (BuildArgs)ba));
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
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    boolean areSharing;
    List<TabSource.AnnotatedTabData> choices;
    
    public BuildArgs(boolean areSharing, List<TabSource.AnnotatedTabData> choices) {
      super(null);
      this.areSharing = areSharing;
      this.choices = choices;
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
    // PUBLIC CONSTANTS
    //
    ////////////////////////////////////////////////////////////////////////////   
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JComboBox otherTabs_;
    private boolean areSharing_;
    private List<TabSource.AnnotatedTabData> choices_;
   
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, BuildArgs ba) { 
      super(cfh, "dataSharingPolicy.title", new Dimension(650, 200), 4, new DataSharingRequest(), false);
      areSharing_ = ba.areSharing;
      choices_ = ba.choices;
      
      JLabel topLabel = new JLabel(UiUtil.convertMessageToHtml(rMan_.getString("dataSharingPolicy.shareTimeUnitInfo")));
      addWidgetFullRow(topLabel, false, false); 
    
      String instruct =  rMan_.getString("dataSharingPolicy.selectWhich");
      
      JLabel chooseLabel = new JLabel(instruct);
      addWidgetFullRow(chooseLabel, false);  
      
      //
      // Chooose either to share, or not:
      //
      
      Vector<TrueObjChoiceContent> otherTabs = buildOptions(choices_, uics_.getRMan());
      JLabel tabLabel = new JLabel(rMan_.getString("dataSharingPolicy.withWho"));
      otherTabs_ = new JComboBox(otherTabs);
      addLabeledWidget(tabLabel, otherTabs_, false, false);    
      
      finishConstruction(); 
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    ////////////////////////////////////////////////////////////////////////////   
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
      
    public boolean dialogIsModal() {
      return (true);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
   
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      DataSharingRequest crq = (DataSharingRequest)request_;
      crq.setForApply(false);
      
      if ((otherTabs_ != null) && otherTabs_.isEnabled()) {
        crq.startSharingWith = (String)((TrueObjChoiceContent)otherTabs_.getSelectedItem()).val;
      }
      crq.haveResult = true;
      return (true);
    }
    
    /***************************************************************************
    **
    ** Do the bundle 
    */

    protected Vector<TrueObjChoiceContent> buildOptions(List<TabSource.AnnotatedTabData> tabs, ResourceManager rMan) {     
      Vector<TrueObjChoiceContent> retval = new Vector<TrueObjChoiceContent>();
      TrueObjChoiceContent noShare = new TrueObjChoiceContent(rMan.getString("dataSharingPolicy.noNotShare"), null);
      retval.add(noShare);
      for (TabSource.AnnotatedTabData atd : tabs) {
        TrueObjChoiceContent cc = new TrueObjChoiceContent(atd.tnd.getTitle(), atd.dbID);
        retval.add(cc);
      }
      return (retval);
    }  
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class DataSharingRequest implements ServerControlFlowHarness.UserInputs {

    private boolean haveResult;
    public String startSharingWith;
    private boolean forApply_;
    
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
    
    public void setForApply(boolean forApply) {
      forApply_ = forApply;
      return;
    }
   
    public boolean isForApply() {
      return (forApply_);
    }
  } 
}
