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

import java.util.Map;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.InstructionSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.LocalDataCopyTarget;
import org.systemsbiology.biotapestry.db.Metabase;
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
** This DataAccessContext can be handed to e.g. UI components that need to
** act on whatever the CURRENT Genome is.
*/

public class DynamicDataAccessContext extends DataAccessContext {
  private BTState appState_;
  private FullGenomeHierarchyOracle fgho_;
//  private Genome baseGenome_;
 // private Layout layout_;
  
  //
  // For instantiations before BTState is ready to be called:
  //
  
  public DynamicDataAccessContext(BTState appState, Metabase mb, boolean isForWeb) {
    super(mb, isForWeb, false, null);
    appState_ = appState;
    fgho_ = new FullGenomeHierarchyOracle(this);
  }
  
  public DynamicDataAccessContext(BTState appState) {
    super(appState.getMetabase(), appState.isWebApplication(), false, appState.getFontRenderContext());
    appState_ = appState;
    fgho_ = new FullGenomeHierarchyOracle(this);
  }
  
  public Map<String, TabPinnedDynamicDataAccessContext> getTabContexts() {
    return (appState_.getTabContexts());
  }
  
  public TabPinnedDynamicDataAccessContext getTabContext(String tabKey) {
    return (appState_.getTabContext(tabKey));
  }


  // ONLY PACKAGE VISIBLE!
 // void setGenome(Genome genome) {
 //   baseGenome_ = genome;
 //   return;
 // }
 // void setLayout(Layout layout) {
  //  layout_ = layout;
  //  return;
 // }
  
  public DBGenome getDBGenome() {
    return (appState_.getDBX().getRootDBGenome());
  }

  public LayoutSource getLayoutSource() {
    return (appState_.getDBX());
  }
   
  public GenomeSource getGenomeSource() {
    return (appState_.getDBX());
  }
   
  public Genome getCurrentGenome() {
    String key = appState_.getGenomeX();
    if (key == null) {
      return (null);
    }
    return (appState_.getDBX().getGenome(key)); 
   // return (baseGenome_);
  }
  
  public GenomeInstance getCurrentGenomeAsInstance() {
    return ((GenomeInstance)getCurrentGenome());
  }
  
  public DynamicGenomeInstance getCurrentGenomeAsDynamicInstance() {
    return ((DynamicGenomeInstance)getCurrentGenome());   
  }

  public DBGenome getCurrentGenomeAsDBGenome() {
    return ((DBGenome)getCurrentGenome());
  }
 
  public String getCurrentGenomeID() {
    Genome g = getCurrentGenome();
    return ((g == null) ? null : g.getID());
  //  return ((baseGenome_ == null) ? null : baseGenome_.getID());
  }
  
  public String getCurrentLayoutID() {
    return (appState_.getLayoutKeyX());
   // return ((layout_ == null) ? null : layout_.getID());
  }
  
  public Layout getCurrentLayout() {
    String lok = appState_.getLayoutKeyX();
    if (lok == null) {
      return (null);
    }
    return (getLayoutSource().getLayout(lok));
   // return (layout_);
  }
  
  public ExperimentalDataSource getExpDataSrc() {
    return (appState_.getDBX());  
  }
  
  public TemporalRangeSource getTemporalRangeSrc() {
    return (appState_.getDBX()); 
  }
  
  public DataMapSource getDataMapSrc() {
    return (appState_.getDBX()); 
  }
  
  public InstructionSource getInstructSrc() {
    return (appState_.getDBX());  
  }
  
  public ModelDataSource getModelDataSource() {
    return (appState_.getDBX());
  }
 
  public ModelBuilder.Source getWorksheetSrc() {
    return (appState_.getDBX()); 
  }
  
  public OverlayStateOracle getOSO() {
    return (appState_.getOSOX());   
  }
  
  public OverlayStateWriter getOverlayWriter() {
    return (appState_.getOverlayWriter());   
  }

  public ColorResolver getColorResolver() {
    return (appState_.getMetabase());
  }
  
  public FontManager getFontManager() {
    return (appState_.getFontMgr());
  }
  
  public MinimalDispOptMgr getDisplayOptsSource() {
    return (appState_.getDisplayOptMgrX());
  }
 
  public WorkspaceSource getWorkspaceSource() {
    return (appState_.getDBX());
  }
   
  public FullGenomeHierarchyOracle getFGHO() {
    return (fgho_);
  }
  
  public GroupSettingSource getGSM() {
    return (appState_.getGroupMgrX()); 
  }
 
  public ZoomTargetSupport getZoomTarget() {
    return (appState_.getZoomTargetX());
  }
  
  public LayoutOptionsManager getLayoutOptMgr() {
    return (appState_.getLayoutOptMgr());
  }
  
  public ResourceManager getRMan() {
    return (appState_.getRManX());
  }
  
  public LocalDataCopyTarget getLocalDataCopyTarget() {
    return (appState_.getDBX());
  }

  /***************************************************************************
  ** 
  ** Get simulator model source
  */
  
  public SimParamSource getSimParamSource() {
    return (appState_.getSimParamSource());  
  }  

  /***************************************************************************
  **
  ** 
  */
  
  public void newModelFollowOn() {
    return;
  }
  
  public void dropFollowOn() {
    return;
  }
  
  public void dropRootNetworkOnlyFollowOn(boolean replace) {
    return;
  }

}