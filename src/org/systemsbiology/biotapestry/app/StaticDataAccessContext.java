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

import java.awt.font.FontRenderContext;

import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.db.GenomeSource;
import org.systemsbiology.biotapestry.db.InstructionSource;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.db.LocalDataCopyTarget;
import org.systemsbiology.biotapestry.db.LocalGenomeSource;
import org.systemsbiology.biotapestry.db.LocalLayoutSource;
import org.systemsbiology.biotapestry.db.LocalWorkspaceSource;
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
import org.systemsbiology.biotapestry.nav.LocalGroupSettingSource;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.FreezeDriedOverlayOracle;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.LocalDispOptMgr;
import org.systemsbiology.biotapestry.ui.MinimalDispOptMgr;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.OverlayStateWriter;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/***************************************************************************
**
** Bundle up all the various common arguments!
*/

public class StaticDataAccessContext extends DataAccessContext {
    
  private Genome baseGenome_;
  private GenomeSource baseGSrc_;
  private ColorResolver cRes_;  
  private DataMapSource dms_;  
  private ExperimentalDataSource expDatSrc_;
  private FullGenomeHierarchyOracle fgho_;
  private FontManager fmgr_;
  private GroupSettingSource gsm_;
  private InstructionSource iSrc_;
  private Layout layout_;
  private MinimalDispOptMgr lbs_;
  private LocalDataCopyTarget ldct_;
  private LayoutOptionsManager lom_;
  private LayoutSource lSrcx_;
  private ModelDataSource mds_;
  private OverlayStateOracle oso_;
  private OverlayStateWriter osw_; 
  private ResourceManager rMan_;
  private SimParamSource sps_;
  private TemporalRangeSource trs_; 
  private ModelBuilder.Source wkSrc_;
  private WorkspaceSource wSrc_;
  private ZoomTargetSupport zts_;

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Build static DAC with given pretty much the full kitchen sink of options:
  */
  
  public StaticDataAccessContext(Genome genome, Layout layout, boolean isGhosted, boolean showBubbles, double pixDiam,
                                 FontManager fmgr, MinimalDispOptMgr lbs, FontRenderContext frc, GenomeSource gSrc, ColorResolver cRes, 
                                 boolean forWeb, ResourceManager rMan, GroupSettingSource gsm, 
                                 WorkspaceSource wSrc, LayoutSource lSrc, OverlayStateOracle oso, OverlayStateWriter osw, 
                                 ExperimentalDataSource expDatSrc, InstructionSource iSrc, DataMapSource dms, TemporalRangeSource trs,                         
                                 ModelBuilder.Source wkSrc, ModelDataSource mds, ZoomTargetSupport zts, LayoutOptionsManager lom, 
                                 Metabase mb, SimParamSource sps, LocalDataCopyTarget ldct) {
    super(mb, forWeb, isGhosted, frc);
    this.baseGenome_ = genome;
    this.layout_ = layout;
    this.setPixDiam(pixDiam);
    this.fmgr_ = fmgr;
    this.lbs_ = lbs;
    this.baseGSrc_ = gSrc;
    this.cRes_ = cRes;
    this.fgho_ = new FullGenomeHierarchyOracle(this);
    this.rMan_ = rMan; 
    this.setShowBubbles(showBubbles);
    this.gsm_ = gsm;
    this.wSrc_ = wSrc;
    this.ldct_ = ldct;
    this.lSrcx_ = lSrc;
    this.oso_ = oso;
    this.osw_ = osw;
    this.expDatSrc_ = expDatSrc;
    this.dms_ = dms;
    this.sps_ = sps;
    this.trs_ = trs;
    this.iSrc_ = iSrc;
    this.wkSrc_ = wkSrc;
    this.mb_ = mb;
    this.mds_ = mds;    
    this.zts_ = zts;
    this.lom_ = lom;
  }

  /***************************************************************************
  **
  ** Chicken and egg. Things like genomePresentation needs the DCX to build, and we need
  ** it to build the zoom target support:
  */
  
  public void setZoomTargetSupport(ZoomTargetSupport zts) {
    zts_ = zts;
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Build static DAC using the current database as the genome source, and set the current
  ** genome by pulling it out of the source using the ID. Layout can be anything you want. The
  ** genomeID and Layout can both be null.
  */
  
  public StaticDataAccessContext(BTState appState, String genomeID, Layout lo) {
    this(appState, appState.getDBX(), (genomeID == null) ? null : appState.getDBX().getGenome(genomeID), lo);
  }
  
  /***************************************************************************
  **
  ** Return a new StaticDataAccessContext with the given layout as the current immediate layout. WARNING!
  ** This could be a layout NOT in the layout source, i.e. dacx.getLayoutSource().getLayout(lo.getID()) != lo
  */
  
  public StaticDataAccessContext replaceImmediateLayout(Layout lo) {
    StaticDataAccessContext retval = new StaticDataAccessContext(this);
    retval.layout_ = lo;
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Like the above, but we hand this an actual current genome:
  */
  
  public StaticDataAccessContext(BTState appState, Genome genome, Layout lo) {
    this(appState, appState.getDBX(), genome, lo);
  }
   
  /***************************************************************************
  **
  ** Use this guy to do the usual heavy lifting:
  */
  
  public StaticDataAccessContext(BTState appState, String genomeID) {
    this(appState, appState.getDBX(), (genomeID == null) ? null : appState.getDBX().getGenome(genomeID), 
        (genomeID == null) ? null : appState.getDBX().getLayoutForGenomeKey(genomeID));
  }

  /***************************************************************************
  **
  ** Build static DAC that has the CURRENT genome set.
  */
  
  public StaticDataAccessContext(BTState appState) {
    this(appState, appState.getGenomeX());
  }
  
  /***************************************************************************
  **
  ** Build static DAC with given genome and layout, plus a custom genome source. Otherwise the current DB provides the
  ** answers. Used internally by other constructors:
  */
   
  private StaticDataAccessContext(BTState appState, GenomeSource gSrc, Genome genome, Layout lo) {
    super(appState.getMetabase(), appState.isWebApplication(), false, appState.getFontRenderContext());
    this.baseGenome_ = genome;
    this.layout_ = lo;
    // Needed for initialization sequencing:
    ZoomTargetSupport zts = appState.getZoomTargetX();
    this.setPixDiam((zts == null) ? 0.0 : zts.currentPixelDiameter());
    this.fmgr_ = appState.getFontMgr();
    this.lbs_ = appState.getDisplayOptMgrX();
    this.baseGSrc_ = gSrc;
    this.cRes_ = appState.getMetabase();
    this.fgho_ = new FullGenomeHierarchyOracle(this);
    this.rMan_ = appState.getRManX();
    this.gsm_ = appState.getGroupMgrX(); 
    this.wSrc_ = appState.getDBX();
    this.ldct_ = appState.getDBX();
    this.lSrcx_ = appState.getDBX();
    this.lom_ = appState.getLayoutOptMgr();
    this.oso_ = appState.getOSOX();
    this.osw_ = appState.getOverlayWriter();
    this.expDatSrc_ = appState.getDBX();
    this.dms_ = appState.getDBX();
    this.sps_ = appState.getSimParamSource();
    this.trs_ = appState.getDBX();
    this.iSrc_ = appState.getDBX();
    this.wkSrc_ = appState.getDBX();
    this.mds_ = appState.getDBX();
    this.zts_  = appState.getZoomTargetX();
    this.setFrc(appState.getFontRenderContext());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  
  /***************************************************************************
  **
  ** Copy constructor. Only usable for another static DAC:
  */  

  public StaticDataAccessContext(StaticDataAccessContext other) {
    super(other);
    copyAcross(other);
  } 
 
  /***************************************************************************
  **
  ** Build static DAC from another, set the current by pulling it out of the source 
  *  using the ID. Layout can be anything you want. The genomeID and Layout can both be null.
  */
  
  public StaticDataAccessContext(DataAccessContext other, String genomeID, Layout lo) {
    super(other);
    copyAcross(other);
    this.baseGenome_ = (genomeID == null) ? null : other.getGenomeSource().getGenome(genomeID);
    this.baseGSrc_ = other.getGenomeSource();
    this.layout_ = lo;  
  }
  
  
  public StaticDataAccessContext(DataAccessContext other, String genomeID, String layoutID) {
    super(other);
    copyAcross(other);
    this.baseGenome_ = (genomeID == null) ? null : other.getGenomeSource().getGenome(genomeID);
    this.layout_ = (layoutID == null) ? null : other.getLayoutSource().getLayout(layoutID);  
  }
  
  public StaticDataAccessContext(DataAccessContext other, String genomeID) {
    this(other, genomeID, other.getLayoutSource().mapGenomeKeyToLayoutKey(genomeID));
  }
   
  public StaticDataAccessContext(DataAccessContext other, GenomeSource gSrc, Genome genome, Layout lo) {
    super(other);
    copyAcross(other);
    this.baseGenome_ = genome;
    this.baseGSrc_ = gSrc;
    this.layout_ = lo;  
  }
  
   //
   // TO freeze-dry the current dymaic state:
   //
  
   public StaticDataAccessContext(DataAccessContext dacx) {
     super(dacx);
     copyAcross(dacx);
   }
  
  public StaticDataAccessContext(DataAccessContext other, Genome genome) {
    super(other);
    copyAcross(other);
    this.baseGenome_ = genome;
    this.layout_ = (genome != null) ? other.getLayoutSource().getLayoutForGenomeKey(genome.getID()) : null;  
    //
    // Local layout source actually does this call correctly now. Handle the
    // erroneous legacy cases where it would return the only layout it had
    // regardless if it was the correct one:
    //
    if ((this.layout_ == null) && other.getLayoutSource() instanceof LocalLayoutSource) {
      System.err.println("Bad Layout Ref in DAC Constructor " + genome.getID());
      this.layout_ = ((LocalLayoutSource)other.getLayoutSource()).getLayoutIterator().next();
    }
  }
  
  /***************************************************************************
  **
  ** Copy from another DAC, but set genome and layout:
  */
  
  public StaticDataAccessContext(DataAccessContext other, Genome genome, Layout layout) {
    super(other);
    copyAcross(other);
    this.baseGenome_ = genome;
    this.layout_ = layout;
  }
  
  /***************************************************************************
  **
  ** Sorta-constructor 
  */
  
  public StaticDataAccessContext getContextForRoot() {
    StaticDataAccessContext retval = new StaticDataAccessContext(this);
    retval.baseGenome_ = getGenomeSource().getRootDBGenome();
    retval.layout_ = (retval.baseGenome_ == null) ? null :retval.getLayoutSource().getLayoutForGenomeKey(retval.baseGenome_.getID());
    return (retval);
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE HELPER
  //
  ////////////////////////////////////////////////////////////////////////////  

  private void copyAcross(DataAccessContext other) { 

    this.baseGenome_ = other.getCurrentGenome();
    this.baseGSrc_ = other.getGenomeSource();
    this.cRes_ = other.getColorResolver();
    this.dms_ = other.getDataMapSrc();
    this.expDatSrc_ = other.getExpDataSrc();
    this.fgho_ = other.getFGHO();
    this.fmgr_ = other.getFontManager();
    this.gsm_ = other.getGSM();
    this.iSrc_ = other.getInstructSrc();
    this.layout_ = other.getCurrentLayout(); 
    this.lbs_ = other.getDisplayOptsSource();
    this.lom_ = other.getLayoutOptMgr();
    this.ldct_ = other.getLocalDataCopyTarget();
    this.lSrcx_ = other.getLayoutSource();
    this.mds_ = other.getModelDataSource();
    this.oso_ = other.getOSO();
    this.osw_ = other.getOverlayWriter();
    this.rMan_ = other.getRMan();
    this.sps_ = other.getSimParamSource();
    this.trs_ = other.getTemporalRangeSrc();
    this.wkSrc_ = other.getWorksheetSrc();
    this.wSrc_ = other.getWorkspaceSource();
    this.zts_  = other.getZoomTarget();
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // STATIC-DAC-ONLY OPERATIONS
  //
  //////////////////////////////////////////////////////////////////////////// 


  public void setLayout(Layout layout) {
    layout_ = layout;
    return;
  }
  
  public void setOSO(OverlayStateOracle oso) {
    oso_ = oso;
    return;
  }
  
  public void setFGHO(FullGenomeHierarchyOracle fgho) {
    fgho_ = fgho;
    return;
  }
  
  public void setGenomeSource(GenomeSource gSrc) {
     baseGSrc_ = gSrc;
    return;
  }
  
  public void setWorkspaceSource(WorkspaceSource wSrc) {
     wSrc_ = wSrc;
    return;
  }
  
  public void setLayoutSource(LayoutSource lSrc) {
    lSrcx_ = lSrc;
    return;
  }
 
  public void setGenome(Genome genome) {
    baseGenome_ = genome;
    return;
  }

  /***************************************************************************
  **
  ** 
  */
 
  public void newModelFollowOn() {
    baseGenome_ = getGenomeSource().getRootDBGenome();
    layout_ = getLayoutSource().getLayoutForGenomeKey(baseGenome_.getID());
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public void dropFollowOn() {
    baseGenome_ = getGenomeSource().getRootDBGenome();
    layout_ = null;
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public void dropRootNetworkOnlyFollowOn(boolean replace) {
    if (replace) {
      baseGenome_ = getGenomeSource().getRootDBGenome();
      layout_ = getLayoutSource().getRootLayout();
    }
    return;
  }

  /***************************************************************************
  **
  ** Replace the guts of the layout with given ID in the layout source, then
  ** make that the current layout. Used inside a layoutUndoTransaction. 
  */  
  
  public void fullLayoutReplacement(String loID, Layout lo) {
    getLayoutSource().getLayout(loID).replaceContents(lo);
    setLayout(getLayoutSource().getLayout(loID));
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // ALL DAC OPERATION IMPLEMENTATIONS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  
  public GenomeSource getGenomeSource() {
    return (baseGSrc_);
  }
  
  public LayoutSource getLayoutSource() {
    return (lSrcx_);
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
    return (expDatSrc_);
  }
  
  public TemporalRangeSource getTemporalRangeSrc() {
    return (trs_);
  }
  
  public DataMapSource getDataMapSrc() {
    return (dms_);
  }
  
  public InstructionSource getInstructSrc() {
    return (iSrc_);
  }

  public ModelBuilder.Source getWorksheetSrc() {
    return (wkSrc_);
  }

  public MinimalDispOptMgr getDisplayOptsSource() {
    return (lbs_);
  }
  
  public DBGenome getDBGenome() {
    return (getGenomeSource().getRootDBGenome());
  }
 
  public ModelDataSource getModelDataSource() {
    return (this.mds_);
  }
 
  public OverlayStateOracle getOSO() {
    UiUtil.fixMePrintout("For root context, does this need to be freeze-dried??");
    return (this.oso_);   
  }
  
  public OverlayStateWriter getOverlayWriter() {
    return (this.osw_);   
  }

  public ColorResolver getColorResolver() {
    return (this.cRes_);
  }
  
  public FontManager getFontManager() {
    return (this.fmgr_);
  }
 
  public WorkspaceSource getWorkspaceSource() {
    return (this.wSrc_);
  }
   
  public FullGenomeHierarchyOracle getFGHO() {
    return (fgho_);
  }
  
  public ZoomTargetSupport getZoomTarget() {
    return (this.zts_);
  }
  
  public LayoutOptionsManager getLayoutOptMgr() {
    return (this.lom_);
  }
  
  public ResourceManager getRMan() {
    return (this.rMan_);
  }
  
  public GroupSettingSource getGSM() {
    return (this.gsm_);
  }
  
  public LocalDataCopyTarget getLocalDataCopyTarget() {
    return (this.ldct_);
  }
 
  /***************************************************************************
  ** 
  ** Get simulator model source
  */
  
  public SimParamSource getSimParamSource() {
    return (this.sps_);  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // HACKTASTIC GENERATORS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  ** 
  ** There were (are) places in the code where crazy-customized DACXs were being built.
  ** This is gathering these all together so they can be cleaned up and rationalized at some point.
  */
  
  public StaticDataAccessContext getCustomDACX1() {
    BTState appState = mb_.getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext retval = new StaticDataAccessContext(null, null, false, false, 0.0,
          appState.getFontMgr(), appState.getDisplayOptMgrX(), 
          appState.getFontRenderContext(), db, appState.getMetabase(), false, 
          appState.getRManX(), 
          new LocalGroupSettingSource(), db, db,
          new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
          db, db, db, db, db, db, null, //<-Gotta Fill in; does this in MVPWZ constructor below 
          appState.getLayoutOptMgr(), appState.getMetabase(), null, db); 
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Another
  */
  
   public StaticDataAccessContext getCustomDACX2() {
     BTState appState = mb_.getAppState();
     return (new StaticDataAccessContext(appState, (String)null, (Layout)null));
   }

  /***************************************************************************
  ** 
  ** Another
  */
  
  public StaticDataAccessContext getCustomDACX3() {
    BTState appState = mb_.getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext retval = new StaticDataAccessContext(null, null, false, false, 0.0,
                                appState.getFontMgr(), appState.getDisplayOptMgrX(), 
                                appState.getFontRenderContext(), db, appState.getMetabase(), false, 
                                appState.getRManX(), 
                                new LocalGroupSettingSource(), db, db,
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null,
                                db, db, db, db, db, db, null, //<-Gotta Fill in; does this in MVPWZ constructor below 
                                appState.getLayoutOptMgr(), appState.getMetabase(), null, db);   
    return (retval);
  }
  
 
  /***************************************************************************
  ** 
  ** Another
  */
  
  public StaticDataAccessContext getCustomDACX4(LocalGenomeSource lgs) {
    BTState appState = mb_.getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext retval = new StaticDataAccessContext(null, null, false, false, 0.0,
                                appState.getFontMgr(), appState.getDisplayOptMgrX(), 
                                appState.getFontRenderContext(), lgs, mb_, false, 
                                appState.getRManX(), 
                                new LocalGroupSettingSource(), db, db,
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                db, db, db, db, db, db, null, //<-Gotta Fill in; does this in MVPWZ constructor below 
                                appState.getLayoutOptMgr(), mb_, null, db); 
    return (retval); 
  }
  
  /***************************************************************************
  ** 
  ** Another
  */
  
  public StaticDataAccessContext getCustomDACX5(LocalGenomeSource currGSrc, LocalLayoutSource lls, LocalWorkspaceSource lws, LocalDispOptMgr llbs) {
    BTState appState = mb_.getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext retval = new StaticDataAccessContext(null, null, false, false, 0.0,
                                  appState.getFontMgr(), llbs, 
                                  appState.getFontRenderContext(), currGSrc, mb_, this.isForWeb(), this.rMan_, 
                                  new LocalGroupSettingSource(), lws, lls, 
                                  new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                  db, db, db, db, db, db, null, //<-Gotta Fill in! 
                                  appState.getLayoutOptMgr(), mb_, null, db);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Another
  */
  
  public StaticDataAccessContext getCustomDACX6(Genome useGenome, Layout retval, LocalLayoutSource lls, LocalGenomeSource fillIn, LocalDispOptMgr llbs) {
    BTState appState = mb_.getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext rcx = new StaticDataAccessContext(useGenome, retval, false, false, 0.0,
                                                          appState.getFontMgr(), llbs, 
                                                          appState.getFontRenderContext(), fillIn, appState.getMetabase(), this.isForWeb(), 
                                                          appState.getRManX(), 
                                                          new LocalGroupSettingSource(), appState.getDBX(), lls, 
                                                          new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                                          db, db, db, db, db, db, appState.getZoomTargetX(), //<- Incorrect Zoom support, but used in UI-free context  
                                                          appState.getLayoutOptMgr(), appState.getMetabase(), null, db);   
  
    return (rcx);
  }
 
  /***************************************************************************
  ** 
  ** Another
  */
  
  public static StaticDataAccessContext getCustomDACX7(LocalGenomeSource fgs, LocalLayoutSource lls, DataAccessContext dacx) { 
    BTState appState = dacx.getMetabase().getAppState();
    StaticDataAccessContext rcx = new StaticDataAccessContext(null, null, false, false, 0.0,
                                 appState.getFontMgr(), appState.getDisplayOptMgrX(), 
                                 appState.getFontRenderContext(), fgs, dacx.getColorResolver(), false, 
                                 dacx.getRMan(), 
                                 new LocalGroupSettingSource(), dacx.getWorkspaceSource(), lls,
                                 new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                 dacx.getExpDataSrc(), dacx.getInstructSrc(), dacx.getDataMapSrc(), 
                                 dacx.getTemporalRangeSrc(), dacx.getWorksheetSrc(), dacx.getModelDataSource(), 
                                 dacx.getZoomTarget(), dacx.getLayoutOptMgr(), appState.getMetabase(), null, appState.getDBX()); 
    return (rcx);
  } 

  /***************************************************************************
  ** 
  ** Another
  */

  public static StaticDataAccessContext getCustomDACX8(DataAccessContext dacx) { 
    BTState appState = dacx.getMetabase().getAppState();
    Database db = appState.getDBX();
    StaticDataAccessContext rcx = new StaticDataAccessContext(null, null, false, false, 0.0,
                                appState.getFontMgr(), appState.getDisplayOptMgrX(), 
                                appState.getFontRenderContext(), db, appState.getMetabase(), false, 
                                appState.getRManX(), 
                                new LocalGroupSettingSource(), db, db,
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                db, db, db, db, db, db, null, //<-Gotta Fill in; does this in MVPWZ constructor below 
                                appState.getLayoutOptMgr(), appState.getMetabase(), null, db);   
  
    return (rcx);
  }
  

  /***************************************************************************
  ** 
  ** Another
  */

  public static StaticDataAccessContext getCustomDACX9(DataAccessContext dacx, LocalGenomeSource currGSrc, 
                                                       LocalLayoutSource lls, LocalWorkspaceSource lws, FontRenderContext frci) { 
    BTState appState = dacx.getMetabase().getAppState();
    Database db = appState.getDBX(); 
    StaticDataAccessContext rcx = new StaticDataAccessContext(null, null, false, false, 0.0,
                                appState.getFontMgr(), new LocalDispOptMgr(appState.getDisplayOptMgrX().getDisplayOptions().clone()), 
                                frci, currGSrc, appState.getMetabase(), false, 
                                appState.getRManX(), 
                                new LocalGroupSettingSource(), lws, lls,
                                new FreezeDriedOverlayOracle(null, null, NetModuleFree.CurrentSettings.NOTHING_MASKED, null), null, 
                                db, db, db, db, db, db, null, //<-Gotta Fill in; does this in MVPWZ constructor below 
                                appState.getLayoutOptMgr(), appState.getMetabase(), null, db);  
  
    return (rcx);  
  }  
}