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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Factory to create dialogs for compression/expansion input
*/

public class CompressExpandDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public CompressExpandDialogFactory(ServerControlFlowHarness cfh) {
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
      return (new DesktopDialog(cfh, dniba.doCompress, dniba.showOverlayOptions, dniba.cpexlayoutChoices, dniba.selectedStart));
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
    
    boolean doCompress;
    boolean showOverlayOptions;
    Vector<ChoiceContent> cpexlayoutChoices;
    ChoiceContent selectedStart;
 
    public BuildArgs(boolean doCompress, boolean showOverlayOptions, DataAccessContext dacx) {
      super(null);
      this.doCompress = doCompress;
      this.showOverlayOptions = showOverlayOptions;
      
      if (showOverlayOptions) {
        cpexlayoutChoices = NetOverlayProperties.getCompressExpandLayoutOptions(dacx);
        selectedStart = NetOverlayProperties.cpexLayoutForCombo(dacx, NetOverlayProperties.CPEX_LAYOUT_APPLY_ALGORITHM);
      }
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
    
    private JSlider percentVertical_;
    private JSlider percentHorizontal_;
    private JComboBox overlayOptionCombo_;
     
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, boolean doCompress, boolean showOverlayOptions,  
                         Vector<ChoiceContent> cpexlayoutChoices, ChoiceContent selectedStart) { 
      super(cfh, (doCompress) ? "compressPercent.title" : "expandPercent.title", new Dimension(700, 500), 2, new PercentsRequest(), false);
      
      //
      // Build a slider for each parameter:
      //
  
      percentVertical_ = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
      percentVertical_.setMinorTickSpacing(5);
      percentVertical_.setMajorTickSpacing(10);
      percentVertical_.setLabelTable(percentVertical_.createStandardLabels(20));
      percentVertical_.setPaintTicks(true);
      percentVertical_.setPaintLabels(true);
      percentVertical_.setSnapToTicks(true);
      
      addWidgetFullRow(percentVertical_, false);
      JLabel lab = new JLabel(rMan_.getString((doCompress) ? "compressPercent.vertical" : "expandPercent.vertical"), JLabel.CENTER);     
      addWidgetFullRowWithInsets(lab, true, 0, 0, 25, 0);
      
      percentHorizontal_ = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
      percentHorizontal_.setMinorTickSpacing(5);
      percentHorizontal_.setMajorTickSpacing(10);
      percentHorizontal_.setLabelTable(percentHorizontal_.createStandardLabels(20));
      percentHorizontal_.setPaintTicks(true);
      percentHorizontal_.setPaintLabels(true);
      percentHorizontal_.setSnapToTicks(true);
      
      addWidgetFullRow(percentHorizontal_, false);
      lab = new JLabel(rMan_.getString((doCompress) ? "compressPercent.horizontal" : "expandPercent.horizontal"), JLabel.CENTER);
      addWidgetFullRowWithInsets(lab, true, 0, 0, 25, 0);
         
      if (showOverlayOptions) {
        JLabel overlayLabel = new JLabel(rMan_.getString("layoutParam.cpexOverlayOptions"));
        overlayOptionCombo_ = new JComboBox(cpexlayoutChoices);
        overlayOptionCombo_.setSelectedItem(selectedStart);    
  
        UiUtil.gbcSet(gbc_, 0, rowNum_, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 25, 25, UiUtil.CEN, 1.0, 0.0);    
        cp_.add(overlayLabel, gbc_);
        UiUtil.gbcSet(gbc_, 1, rowNum_++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 25, 25, UiUtil.CEN, 1.0, 0.0);    
        cp_.add(overlayOptionCombo_, gbc_);    
      }
      
      finishConstruction();
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
      PercentsRequest pcq = (PercentsRequest)request_;
      pcq.retvalV  = percentVertical_.getValue() / 100.0;
      pcq.retvalH  = percentHorizontal_.getValue() / 100.0;
      pcq.overlayOption = (overlayOptionCombo_ != null) ? ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val 
                                                        : NetOverlayProperties.NO_CPEX_LAYOUT_OPTION;
      pcq.haveResult = true;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class PercentsRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;     
    public double retvalH;
    public double retvalV;
    public int overlayOption;
    
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
