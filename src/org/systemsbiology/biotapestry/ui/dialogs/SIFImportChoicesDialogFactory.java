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

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.ui.layouts.SpecialtyLayoutEngine;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

/****************************************************************************
**
** Dialog box for choosing SIF import mode
*/

public class SIFImportChoicesDialogFactory extends DialogFactory {
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  public enum LayoutModes {INCREMENTAL, REPLACEMENT};
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SIFImportChoicesDialogFactory(ServerControlFlowHarness cfh) {
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
 
    switch(platform.getPlatform()) {
      case DESKTOP:
        return (new DesktopDialog(cfh, dniba.forSIF));  
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

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class BuildArgs extends DialogBuildArgs { 
    
    boolean forSIF;
          
    public BuildArgs(boolean forSIF) {
      super(null);
      this.forSIF = forSIF; 
    }
  }

  public static class DesktopDialog extends BTTransmitResultsDialog {
  
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JRadioButton incremental_;
    private JRadioButton replace_;
    private JCheckBox optimize_;
    private JCheckBox squash_;
    private JComboBox<ChoiceContent> overlayOptionCombo_;
    private JComboBox layoutChoiceCombo_;
    private JCheckBox setLayoutOptions_;
    private JLabel layoutChoiceLabel_;
    private JLabel overlayLabel_;
   
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, boolean forSIF) { 
      super(cfh, (forSIF) ? "chooseSIFImport.title" : "chooseSIFImport.CSVtitle", new Dimension(700, 400), 2,  new SIFImportRequest(), false);
         
      //
      // For SIF, all submodels are scrubbed of contents.  About the only thing worth doing with
      // overlays is recentering them; anything fancier is a no-op.
      //
      
      boolean showOverlayOptions = (!forSIF && cfh.getDataAccessContext().fgho.overlayExists());
      
      //
      // Build the option selection buttons
      //
      
      replace_ = new JRadioButton(rMan_.getString("chooseSIFImport.replacement"), true);
      incremental_ = new JRadioButton(rMan_.getString("chooseSIFImport.incremental"), false);
      incremental_.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          try {
            boolean incSel = incremental_.isSelected();
            optimize_.setEnabled(incSel);
            layoutChoiceLabel_.setEnabled(!incSel); 
            layoutChoiceCombo_.setEnabled(!incSel);
            setLayoutOptions_.setEnabled(!incSel);
            if (overlayOptionCombo_ != null) {
              overlayLabel_.setEnabled(incSel);
              overlayOptionCombo_.setEnabled(incSel);
            }
            invalidate();
            validate();
            repaint();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      }); 
  
      optimize_ = new JCheckBox(rMan_.getString("chooseSIFImport.optimize"), false);
      optimize_.setEnabled(false);
      if (forSIF) {
        squash_ = null;
      } else {
        squash_ = new JCheckBox(rMan_.getString("chooseSIFImport.squash"), false);
      }
     
      layoutChoiceLabel_ = new JLabel(rMan_.getString("chooseSIFImport.layout"));
      
      Vector<EnumChoiceContent<SpecialtyLayoutEngine.SpecialtyType>> layoutChoices = SpecialtyLayoutEngine.SpecialtyType.getChoices(appState_, true);    
      layoutChoiceCombo_ = new JComboBox(layoutChoices); 
      setLayoutOptions_ = new JCheckBox(rMan_.getString("chooseSIFImport.setOptions"), true);
      
      if (showOverlayOptions) {
        // Note that these values are used when the layout is retained, and we need to
        // shove GenomeInstance groups around to fit stuff in.  In those cases, the
        // overlay modules may need to be tweaked:
        overlayLabel_ = new JLabel(rMan_.getString("layoutParam.overlayOptions"));
        Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(appState_);
        overlayOptionCombo_ = new JComboBox<ChoiceContent>(relayoutChoices);
        overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(appState_, NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES));
        overlayLabel_.setEnabled(false);
        overlayOptionCombo_.setEnabled(false);
      }    
  
      if (cfh.getDataAccessContext().getGenome().isEmpty()) {
        replace_.setEnabled(false);
        incremental_.setEnabled(false);
      } else {
        ButtonGroup group = new ButtonGroup();
        group.add(incremental_);
        group.add(replace_);    
      }   
      
      
      addWidgetFullRow(replace_, false); 
      addLabeledWidgetWithInsets(layoutChoiceLabel_, layoutChoiceCombo_, false, true, 5, 30, 5, 5); 
      addWidgetFullRowWithInsets(setLayoutOptions_, false, 5, 30, 25, 5);
          
      addWidgetFullRow(incremental_, false);   
      addWidgetFullRowWithInsets(optimize_, false, 5, 30, 25, 5);
      
      if (!forSIF) {    
        addWidgetFullRow(squash_, false);
      }
           
      if (showOverlayOptions) {
        addLabeledWidget(overlayLabel_, overlayOptionCombo_, false, true);
      }
      
      finishConstruction();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    //////////////////////////////////////////////////////////////////////////// 

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
  
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      SIFImportRequest crq = (SIFImportRequest)request_;
      crq.mode = incremental_.isSelected() ? LayoutModes.INCREMENTAL : LayoutModes.REPLACEMENT;
      crq.doOptimize = (crq.mode == LayoutModes.INCREMENTAL) ? optimize_.isSelected() : false;
      crq.doSquash = (squash_ == null) ? false : squash_.isSelected();
      crq.overlayOption = (overlayOptionCombo_ != null) ? ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val 
                                                     : NetOverlayProperties.NO_RELAYOUT_OPTION;
      crq.showOptionDialog = (crq.mode == LayoutModes.REPLACEMENT) ? setLayoutOptions_.isSelected() : false;
      crq.layoutType = (crq.mode == LayoutModes.REPLACEMENT) ? ((EnumChoiceContent<SpecialtyLayoutEngine.SpecialtyType>)layoutChoiceCombo_.getSelectedItem()).val : null;     
      crq.haveResult = true;
      return (true);
    }
  }

  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class SIFImportRequest implements ServerControlFlowHarness.UserInputs {
    public boolean haveResult;
    public LayoutModes mode;
    public boolean doOptimize;  
    public boolean doSquash;
    public int overlayOption;
    public boolean showOptionDialog;
    public SpecialtyLayoutEngine.SpecialtyType layoutType;
      
    public void clearHaveResults() {
      haveResult = false;
      return;
    }    
    public boolean haveResults() {
      return (haveResult);
    }
    public boolean isForApply() {
      return (false);
    }
  } 
}
