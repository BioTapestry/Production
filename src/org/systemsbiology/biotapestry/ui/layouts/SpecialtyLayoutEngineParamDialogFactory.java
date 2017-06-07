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


package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.Dimension;

import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;

/****************************************************************************
**
** Factory to create dialogs for gathering Specialty Layout Engine Parameters
*/

public class SpecialtyLayoutEngineParamDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SpecialtyLayoutEngineParamDialogFactory(ServerControlFlowHarness cfh) {
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
      return (new DesktopDialog(cfh, dniba.diaTitle, dniba.diaDim, dniba.diaRows, dniba.paramPanel, false));
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Get the stashing version of the dialog
  */  
  
  public SpecialtyLayoutEngineParamDialogFactory.DesktopDialog getStashDialog(DialogBuildArgs ba) {   
    BuildArgs dniba = (BuildArgs)ba;
    return (new DesktopDialog(cfh, dniba.diaTitle, dniba.diaDim, dniba.diaRows, dniba.paramPanel, true));
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
    
    public enum LoType{HALO, DIAGONAL, WORKSHEET, STACKED};
     
    Dimension diaDim;
    int diaRows;
    LoType lotype;
    String diaTitle;
    String selected;
    JPanel paramPanel;
  
    public BuildArgs(UIComponentSource uics, StaticDataAccessContext dacx, Genome genome, LoType lot, boolean forSubset) {
      this(uics, dacx, genome, lot, null, null, forSubset);
      if (lot == LoType.HALO) {
        throw new IllegalArgumentException();
      }
    }
    
    public BuildArgs(UIComponentSource uics, StaticDataAccessContext dacx, Genome genome, LoType lot, String selectedID, HaloLayout halo) {
      this(uics, dacx, genome, lot, selectedID, halo, false);
      if (lot != LoType.HALO) {
        throw new IllegalArgumentException();
      }
    } 
    
    private BuildArgs(UIComponentSource uics, StaticDataAccessContext dacx, Genome genome, LoType lot, String selectedID, HaloLayout halo, boolean forSubset) {
      super(genome);
      lotype = lot;
      LayoutOptionsManager lom = dacx.getLayoutOptMgr();
      
      switch (lotype) {
        case HALO:
          diaDim = new Dimension(600, 250);
          diaRows = 3;
          diaTitle = "haloLayout.title";
          paramPanel = new HaloLayoutSetupPanel(dacx, genome, selectedID, halo, lom.getHaloLayoutParams());
          break;
        case DIAGONAL:
          diaDim = new Dimension(700, 300);
          diaRows = 4;
          diaTitle = "worksheetDiagonalLayout.title";
          paramPanel = new WorksheetLayoutSetupPanel(uics, dacx, genome, forSubset, true, lom.getDiagLayoutParams());
          break;
        case WORKSHEET:
          diaDim = new Dimension(700, 400);
          diaRows = 4;
          diaTitle = "worksheetLayout.title";
          paramPanel = new WorksheetLayoutSetupPanel(uics, dacx, genome, forSubset, false, lom.getWorksheetLayoutParams());
          break;
        case STACKED:
          diaDim = new Dimension(700, 400);
          diaRows = 4;
          diaTitle = "stackedLayout.title";
          paramPanel = new StackedBlockLayoutSetupPanel(uics, dacx, genome, forSubset, lom.getStackedBlockLayoutParams(), !forSubset);
          break;
        default:
          throw new IllegalArgumentException();
      }
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  //
  // Dialog used for Halo Layout
  // 
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
   
    private SpecialtyLayoutSetupPanel paramPanel_;    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, String diaTitle, Dimension diaDim, int diaRows, JPanel paramPanel, boolean forStash) { 
      super(cfh, diaTitle, diaDim, 2, new SpecParamsRequest(), false, forStash);
      paramPanel_ = (SpecialtyLayoutSetupPanel)paramPanel;
      addTable(paramPanel, diaRows);  
      finishConstruction();
      paramPanel_.displayProperties();
    }
 
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
   
    public boolean dialogIsModal() {
      return (true);
    }

    /***************************************************************************
    **
    ** Return results
    ** 
    */
    
    public SpecialtyLayoutEngineParams getParams() {
      SpecParamsRequest crq = (SpecParamsRequest)request_;
      return (crq.params);
    }  
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Stash our results for later interrogation.
    ** 
    */
    
    @Override
    protected boolean stashForOK() {
      if (!paramPanel_.stashResults(true)) {
        return (false);
      }
      
      SpecParamsRequest crq = (SpecParamsRequest)request_;
      crq.params = paramPanel_.getParams();
      crq.haveResult = true;
      return (true);
    } 
  
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) {
      if (!paramPanel_.stashResults(true)) {
        return (false);
      }
      SpecParamsRequest sprq = (SpecParamsRequest)request_;
      sprq.params = paramPanel_.getParams();
      sprq.haveResult = true;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */

  public static class SpecParamsRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;     
    public SpecialtyLayoutEngineParams params;
 
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
      return (false);
    }
  }
}
