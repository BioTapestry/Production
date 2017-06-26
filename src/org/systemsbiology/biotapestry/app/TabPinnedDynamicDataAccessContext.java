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

package org.systemsbiology.biotapestry.app;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.InstructionSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.LocalDataCopyTarget;
import org.systemsbiology.biotapestry.db.ModelDataSource;
import org.systemsbiology.biotapestry.db.SimParamSource;
import org.systemsbiology.biotapestry.db.TemporalRangeSource;
import org.systemsbiology.biotapestry.db.WorkspaceSource;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.MinimalDispOptMgr;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.OverlayStateWriter;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** This is a dynamic data access context, but the various data sources
** are pinned to come from a specific tab. 
*/

public class TabPinnedDynamicDataAccessContext extends DataAccessContext {
    
  private String myTab_;
  private BTState appState_;
  private Genome baseGenome_;
  private FullGenomeHierarchyOracle fgho_;
  private Layout layout_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  
  /***************************************************************************
  **
  ** A data access context that has data sources pinned to a specific tag. It is kept
  ** current for genome and layout through automatic BTState updates, but reflects the
  ** state of assigned tab.
  **
  ** NOTE: at first, this was fixed at constructor time. But now that we want to provide it
  ** to the database at database construction time, we have to be lazy at getting the data.
  */
   
  // Was default, now public, since ExportPerturb needs it.

  public TabPinnedDynamicDataAccessContext(BTState appState, String currTab) {
    super(appState.getMetabase(), appState.isWebApplication(), false, appState.getFontRenderContext());
    this.appState_ = appState;
    this.myTab_ = currTab;
    this.baseGenome_ = null;
    this.layout_ = null;
    // Needed for initialization sequencing:
    ZoomTargetSupport zts = appState.getZoomTargetX(myTab_);
    this.setPixDiam((zts == null) ? 0.0 : zts.currentPixelDiameter());
    this.fgho_ = new FullGenomeHierarchyOracle(this);
    this.setFrc(appState_.getFontRenderContext());
  }
  
  /***************************************************************************
  **
  ** Use if the current tab is what you want to pin this to. 
  */  
  
  TabPinnedDynamicDataAccessContext(BTState appState) {
    this(appState, appState.getTabSource().getCurrentTab());
  }
  
  /***************************************************************************
  **
  ** Use to pin to a particular tab if we are handed a fully Dynamic context:
  */  
  
  public TabPinnedDynamicDataAccessContext(DynamicDataAccessContext ddacx, String currTab) {
    // Hokey, but works for the moment:
    this(ddacx.getMetabase().getAppState(), currTab);
  }
 
  /***************************************************************************
  **
  ** Use to pin to a particular tab if we are handed a fully Dynamic context:
  */  
  
  public TabPinnedDynamicDataAccessContext(DynamicDataAccessContext ddacx) {
    // Hokey, but works for the moment:
    this(ddacx.getMetabase().getAppState(), ddacx.getMetabase().getAppState().getTabSource().getCurrentTab());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////                           
 
  // ONLY PACKAGE VISIBLE!
  
  void setGenome(Genome genome) {
    baseGenome_ = genome;
    return;
  }
  
  void setLayout(Layout layout) {
    layout_ = layout;
    return;
  }
 
  
  /***************************************************************************
  **
  ** 
  */
  
  public String getTabID() {
    return (myTab_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // STATIC-DAC-ONLY OPERATIONS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** 
  */
  
  public void newModelFollowOn() {
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public void dropFollowOn() {
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public void dropRootNetworkOnlyFollowOn(boolean replace) {
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // ALL DAC OPERATION IMPLEMENTATIONS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public GenomeSource getGenomeSource() {
    return (appState_.getDBX(myTab_));
  }
  
  public LayoutSource getLayoutSource() {
    return (appState_.getDBX(myTab_));
  }

  public Genome getCurrentGenome() {
    return (baseGenome_);
  }
  
  public GenomeInstance getCurrentGenomeAsInstance() {
    return ((GenomeInstance)baseGenome_);
  }
  
  public DynamicGenomeInstance getCurrentGenomeAsDynamicInstance() {
    return ((DynamicGenomeInstance)baseGenome_);   
  }

  public DBGenome getCurrentGenomeAsDBGenome() {
    return ((DBGenome)baseGenome_);
  }
    
  public String getCurrentGenomeID() {
    return ((baseGenome_ == null) ? null : baseGenome_.getID());
  }
  
  public String getCurrentLayoutID() {
    return ((layout_ == null) ? null : layout_.getID());
  }
  
  public Layout getCurrentLayout() {
    return (layout_);
  }
 
  public ExperimentalDataSource getExpDataSrc() {
    return (appState_.getDBX(myTab_));
  }
  
  public TemporalRangeSource getTemporalRangeSrc() {
    return (appState_.getDBX(myTab_));
  }
  
  public DataMapSource getDataMapSrc() {
    return (appState_.getDBX(myTab_));
  }
  
  public InstructionSource getInstructSrc() {
    return (appState_.getDBX(myTab_));
  }

  public ModelBuilder.Source getWorksheetSrc() {
    return (appState_.getDBX(myTab_));
  }

  public MinimalDispOptMgr getDisplayOptsSource() {
    return (appState_.getDisplayOptMgrX());
  }
  
  public DBGenome getDBGenome() {
    return (getGenomeSource().getRootDBGenome());
  }
 
  public ModelDataSource getModelDataSource() {
    return (appState_.getDBX(myTab_));
  }
 
  public OverlayStateOracle getOSO() {
    return (appState_.getOSOX(myTab_));   
  }
  
  public OverlayStateWriter getOverlayWriter() {
    return (appState_.getOverlayWriterX(myTab_));   
  }
  
  public ColorResolver getColorResolver() {
    return (appState_.getMetabase());
  }
  
  public FontManager getFontManager() {
    return (appState_.getFontMgr());
  }
 
  public WorkspaceSource getWorkspaceSource() {
    return (appState_.getDBX(myTab_));
  }
   
  public FullGenomeHierarchyOracle getFGHO() {
    return (fgho_);
  }
  
  public ZoomTargetSupport getZoomTarget() {
    return (appState_.getZoomTargetX(myTab_));
  }
  
  public LayoutOptionsManager getLayoutOptMgr() {
    return (appState_.getLayoutOptMgr());
  }
  
  public ResourceManager getRMan() {
    return (appState_.getRManX());
  }
  
  public GroupSettingSource getGSM() {
    return (appState_.getGroupMgrX(myTab_));
  }
  
  public LocalDataCopyTarget getLocalDataCopyTarget() {
    return (appState_.getDBX(myTab_));
  }

  /***************************************************************************
  ** 
  ** Get simulator model source
  */
  
  public SimParamSource getSimParamSource() {
    return (appState_.getSimParamSource());  
  }  

}