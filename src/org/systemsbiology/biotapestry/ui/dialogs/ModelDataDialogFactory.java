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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.ModelData;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;

/****************************************************************************
**
** Factory to create dialogs for editing model data
*/

public class ModelDataDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ModelDataDialogFactory(ServerControlFlowHarness cfh) {
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
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.mData));
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
    
    ModelData mData; 
 
    public BuildArgs(ModelData md) {
      super(null);
      this.mData = md;
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
  
    private JTextField dateField_;
    private JTextField attribField_;
    private JTextArea keyField_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, ModelData mData) { 
      super(cfh, "mdata.title", new Dimension(700, 500), 6, new ModelDataRequest(), true);

      //
      // Build the date panel:
      //
  
      JLabel label = new JLabel(rMan_.getString("mdata.date"));
      dateField_ = new JTextField();
      addLabeledWidget(label, dateField_, false, true);
      
      
      //
      // Build the attribution panel:
      //
  
      label = new JLabel(rMan_.getString("mdata.attribution"));
      attribField_ = new JTextField();
      addLabeledWidget(label, attribField_, false, true);
      
      //
      // Build the model key text field
      //
  
      keyField_ = new JTextArea();
      keyField_.setEditable(true);
      addLabeledScrolledWidget("mdata.key", keyField_, 6);

      finishConstruction();
      displayProperties(mData);
    }

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
      ModelDataRequest crq = (ModelDataRequest)request_;
      crq.setForApply(forApply);
      crq.dateResult = dateField_.getText().trim(); 
      crq.attribResult = attribField_.getText().trim(); 
      crq.keyResult = keyField_.getText().trim();
      crq.haveResult = true;
      return (true);
    }
     
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Apply the current model data values to our UI components
    ** 
    */
    
    private void displayProperties(ModelData mdat) {
    
      dateField_.setText((mdat == null) ? "" : mdat.getDate());
      attribField_.setText((mdat == null) ? "" : mdat.getAttribution());
      int kSize = (mdat == null) ? 0 : mdat.getKeySize();
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < kSize; i++) {
        buf.append(mdat.getKey(i));
        buf.append("\n");
      }
      keyField_.setText(buf.toString());
      keyField_.setCaretPosition(0);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class ModelDataRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public String dateResult;
    public String attribResult;
    public String keyResult;
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
