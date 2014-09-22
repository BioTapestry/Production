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

package org.systemsbiology.biotapestry.db;

import java.awt.font.FontRenderContext;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.MinimalDispOptMgr;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** Bundle up all the various common arguments!
*/

public class DataAccessContext {
  private Genome baseGenome_;
  private ArrayList<FontRenderContext> frcStack_;
  private FontRenderContext baseFrc_;
  private GenomeSource baseGSrc_;
  private ArrayList<Boolean> ghostedStack_;
  private boolean baseGhosted_;
  private Layout layout_; 
  
  public boolean showBubbles;
  public boolean forWeb;
  public GroupSettingSource gsm;
  
  public double pixDiam;    
  public FontManager fmgr;
  private MinimalDispOptMgr lbs_;
  public ColorResolver cRes;
  public FullGenomeHierarchyOracle fgho;
  public ResourceManager rMan;
  public WorkspaceSource wSrc;
  public LayoutSource lSrc;
  public OverlayStateOracle oso;
  private ExperimentalDataSource expDatSrc_;
  private InstructionSource iSrc_;
                                 
                                 
  public DataAccessContext(Genome genome, Layout layout, boolean isGhosted, boolean showBubbles, double pixDiam,
                          FontManager fmgr, MinimalDispOptMgr lbs, FontRenderContext frc, GenomeSource gSrc, ColorResolver cRes, 
                          boolean forWeb, FullGenomeHierarchyOracle fgho, ResourceManager rMan, GroupSettingSource gsm, 
                          WorkspaceSource wSrc, LayoutSource lSrc, OverlayStateOracle oso, 
                          ExperimentalDataSource expDatSrc, InstructionSource iSrc                          
  ) {
    
    this.baseGenome_ = genome;
    this.layout_ = layout;
    this.pixDiam = pixDiam;
    this.fmgr = fmgr;
    this.lbs_ = lbs;
    this.baseFrc_ = frc;
    this.frcStack_ = new ArrayList<FontRenderContext>();
    this.baseGSrc_ = gSrc;
    this.cRes = cRes;
    this.fgho = fgho;
    this.rMan = rMan; 
    this.ghostedStack_ = new ArrayList<Boolean>();
    this.baseGhosted_ = isGhosted;
    this.showBubbles = showBubbles;
    this.forWeb = forWeb;
    this.gsm = gsm;
    this.wSrc = wSrc;
    this.lSrc = lSrc;
    this.oso = oso;
    expDatSrc_ = expDatSrc;
    iSrc_ = iSrc;
  }

  public DataAccessContext(BTState appState, GenomeSource gSrc, String genomeID, Layout lo) {
    this(appState, gSrc, (genomeID == null) ? null : gSrc.getGenome(genomeID), lo);
  }
  
  public DataAccessContext(BTState appState, String genomeID, Layout lo) {
    this(appState, appState.getDB(), (genomeID == null) ? null : appState.getDB().getGenome(genomeID), lo);
  }
  
  public DataAccessContext(BTState appState, String genomeID) {
    this(appState, appState.getDB(), (genomeID == null) ? null : appState.getDB().getGenome(genomeID), (genomeID == null) ? null : appState.getLayoutForGenomeKey(genomeID));
  }

  // Rendering context for the root model:
  public DataAccessContext(BTState appState) {
    this(appState, appState.getDB(), 
         appState.getDB().getGenome(), 
         (appState.getDB().getGenome() == null) ? null : appState.getLayoutForGenomeKey(appState.getDB().getGenome() .getID()));
  }
  
  public DataAccessContext(BTState appState, GenomeSource gSrc, Genome genome, Layout lo) {
    this.baseGenome_ = genome;
    this.layout_ = lo;
    // Needed for initialization sequencing:
    ZoomTargetSupport zts = appState.getZoomTarget();
    this.pixDiam = (zts == null) ? 0.0 : zts.currentPixelDiameter();
    this.fmgr = appState.getFontMgr();
    this.lbs_ = appState.getDisplayOptMgr();
    this.frcStack_ = new ArrayList<FontRenderContext>();
    this.baseFrc_ = appState.getFontRenderContext();
    this.baseGSrc_ = gSrc;
    this.cRes = appState.getDB();
    this.fgho = new FullGenomeHierarchyOracle(appState);
    this.rMan = appState.getRMan();   
    this.ghostedStack_ = new ArrayList<Boolean>();
    this.baseGhosted_ = false;;
    this.showBubbles = false;
    this.forWeb = appState.isWebApplication();
    this.gsm = appState.getGroupMgr(); 
    this.wSrc = appState.getDB();
    this.lSrc = appState.getDB();
    this.oso = appState;
    expDatSrc_ = appState.getDB();
    iSrc_ = appState.getDB();
  }
  

  public DataAccessContext(BTState appState, Genome genome, Layout lo) {
    this(appState, appState.getDB(), genome, lo);
  }
  
  public DataAccessContext(DataAccessContext other) {
    this.baseGenome_ = other.baseGenome_;
    this.layout_ = other.layout_;
    this.pixDiam = other.pixDiam;
    this.fmgr = other.fmgr;
    this.lbs_ = other.lbs_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>(other.frcStack_);
    this.baseGSrc_ = other.baseGSrc_;
    this.cRes = other.cRes;
    this.fgho = other.fgho;
    this.rMan = other.rMan;
    this.baseGhosted_ = other.baseGhosted_;
    this.ghostedStack_ = new ArrayList<Boolean>(other.ghostedStack_);
    this.showBubbles = other.showBubbles;
    this.forWeb = other.forWeb;
    this.gsm = other.gsm;
    this.wSrc = other.wSrc;
    this.lSrc = other.lSrc;
    this.oso = other.oso;
    this.expDatSrc_ = other.expDatSrc_;
    this.iSrc_ = other.iSrc_;
  } 
  
  public DataAccessContext(DataAccessContext other, String genomeID, String layoutID) {
    this.baseGenome_ = (genomeID == null) ? null : other.getGenomeSource().getGenome(genomeID);
    this.layout_ = (layoutID == null) ? null : other.lSrc.getLayout(layoutID);
    this.pixDiam = other.pixDiam;
    this.fmgr = other.fmgr;
    this.lbs_ = other.lbs_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>(other.frcStack_);
    this.baseGSrc_ = other.baseGSrc_;
    this.cRes = other.cRes;
    this.fgho = other.fgho;
    this.rMan = other.rMan;
    this.baseGhosted_ = other.baseGhosted_;
    this.ghostedStack_ = new ArrayList<Boolean>(other.ghostedStack_);
    this.showBubbles = other.showBubbles;
    this.forWeb = other.forWeb;
    this.gsm = other.gsm;
    this.wSrc = other.wSrc;
    this.lSrc = other.lSrc;
    this.oso = other.oso;
    this.expDatSrc_ = other.expDatSrc_;
    this.iSrc_ = other.iSrc_;
  }
  
  public DataAccessContext(DataAccessContext other, String genomeID) {
    this(other, genomeID, other.lSrc.mapGenomeKeyToLayoutKey(genomeID));
  }
  
  public DataAccessContext(DataAccessContext other, Genome genome) {
    this.baseGenome_ = genome;
    this.layout_ = other.lSrc.getLayoutForGenomeKey(genome.getID());
    //
    // Local layout source actually does this call correctly now. Handle the
    // erroneous legacy cases where it would return the only layout it had
    // regardless if it was the correct one:
    //
    if ((this.layout_ == null) && other.lSrc instanceof LocalLayoutSource) {
      System.err.println("Bad Layout Ref in DAC Constructor " + genome.getID());
      this.layout_ = ((LocalLayoutSource)other.lSrc).getLayoutIterator().next();
    }
    this.pixDiam = other.pixDiam;
    this.fmgr = other.fmgr;
    this.lbs_ = other.lbs_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>(other.frcStack_);
    this.baseGSrc_ = other.baseGSrc_;
    this.cRes = other.cRes;
    this.fgho = other.fgho;
    this.rMan = other.rMan;
    this.baseGhosted_ = other.baseGhosted_;
    this.ghostedStack_ = new ArrayList<Boolean>(other.ghostedStack_);
    this.showBubbles = other.showBubbles;
    this.forWeb = other.forWeb;
    this.gsm = other.gsm;
    this.wSrc = other.wSrc;
    this.lSrc = other.lSrc;
    this.oso = other.oso;
    this.expDatSrc_ = other.expDatSrc_;
    this.iSrc_ = other.iSrc_;
  }
  
  public DataAccessContext(DataAccessContext other, Genome genome, Layout layout) {
    this.baseGenome_ = genome;
    this.layout_ = layout;
    this.pixDiam = other.pixDiam;
    this.fmgr = other.fmgr;
    this.lbs_ = other.lbs_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>(other.frcStack_);
    this.baseGSrc_ = other.baseGSrc_;
    this.cRes = other.cRes;
    this.fgho = other.fgho;
    this.rMan = other.rMan;
    this.baseGhosted_ = other.baseGhosted_;
    this.ghostedStack_ = new ArrayList<Boolean>(other.ghostedStack_);
    this.showBubbles = other.showBubbles;
    this.forWeb = other.forWeb;
    this.gsm = other.gsm;
    this.wSrc = other.wSrc;
    this.lSrc = other.lSrc;
    this.oso = other.oso;
    this.expDatSrc_ = other.expDatSrc_;
    this.iSrc_ = other.iSrc_;
  }
  /***************************************************************************
  **
  ** Copy with empty stacks and specified values:
  */
  
  public DataAccessContext(DataAccessContext other, Genome genome, boolean isGhosted) {
    this.baseGenome_ = genome;
    this.layout_ = other.layout_;
    this.pixDiam = other.pixDiam;
    this.fmgr = other.fmgr;
    this.lbs_ = other.lbs_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>();
    this.baseGSrc_ = other.baseGSrc_;
    this.cRes = other.cRes;
    this.fgho = other.fgho;
    this.rMan = other.rMan;
    this.baseGhosted_ = isGhosted;
    this.ghostedStack_ = new ArrayList<Boolean>();
    this.showBubbles = other.showBubbles;
    this.forWeb = other.forWeb;
    this.gsm = other.gsm;
    this.wSrc = other.wSrc;
    this.lSrc = other.lSrc;
    this.oso = other.oso;
    this.expDatSrc_ = other.expDatSrc_;
    this.iSrc_ = other.iSrc_;
  } 

  public NetOverlayOwner getCurrentOverlayOwner() {
    return (getGenomeSource().getOverlayOwnerFromGenomeKey(getGenomeID()));
  }

  public DataAccessContext getContextForRoot() {
    DataAccessContext retval = new DataAccessContext(this);
    retval.baseGenome_ = getGenomeSource().getGenome();
    retval.layout_ = retval.lSrc.getLayoutForGenomeKey(retval.baseGenome_.getID());
    return (retval);
  }
  
  public void setGenomeSource(GenomeSource gSrc) {
     baseGSrc_ = gSrc;
    return;
  }
 
  public GenomeSource getGenomeSource() {
    return (baseGSrc_);
  }

  public void setGenome(Genome genome) {
    baseGenome_ = genome;
    return;
  }

  public Genome getGenome() {
    return (baseGenome_);
  }
  
  public GenomeInstance getGenomeAsInstance() {
    return ((GenomeInstance)baseGenome_);
  }
  
  public DBGenome getGenomeAsDBGenome() {
    return ((DBGenome)baseGenome_);
  }
    
  public String getGenomeID() {
    return ((baseGenome_ == null) ? null : baseGenome_.getID());
  }
  
  public String getLayoutID() {
    return ((layout_ == null) ? null : layout_.getID());
  }
  
  public Layout getLayout() {
    return (layout_);
  }
  
  public void setLayout(Layout layout) {
    layout_ = layout;
    return;
  }
  
  //
  // Replace the guts of the layout with given ID in the layout source, then
  // make that the current layout. Used inside a layoutUndoTransaction.
  //
  
  public void fullLayoutReplacement(String loID, Layout lo) {
    lSrc.getLayout(loID).replaceContents(lo);
    setLayout(lSrc.getLayout(loID));
    return;
  }
  
  public ExperimentalDataSource getExpDataSrc() {
    return (expDatSrc_);
  }
  
  public InstructionSource getInstructSrc() {
    return (iSrc_);
  }
  
  
  public void newModel() {
    if (!(getGenomeSource() instanceof Database)) {
      throw new IllegalStateException();
    }
    ((Database)getGenomeSource()).newModelViaDACX();
    baseGenome_ = getGenomeSource().getGenome();
    layout_ = lSrc.getLayoutForGenomeKey(baseGenome_.getID());
    return;
  }
  
  public void drop() {
    if (!(getGenomeSource() instanceof Database)) {
      throw new IllegalStateException();
    }
    ((Database)getGenomeSource()).dropViaDACX();
    baseGenome_ = getGenomeSource().getGenome();
    layout_ = null;
    return;
  }
  
  public DatabaseChange dropRootNetworkOnly() {
    boolean replace = genomeIsRootGenome();
    DatabaseChange retval = getGenomeSource().dropRootNetworkOnly();
    if (replace) {
      baseGenome_ = getGenomeSource().getGenome();
      layout_ = lSrc.getRootLayout();
    }
    return (retval);
  }
  
  public MinimalDispOptMgr getDisplayOptsSource() {
    return (lbs_);
  }

  /***************************************************************************
  **
  ** Root Instance
  */
  
  public GenomeInstance getRootInstance() {
    GenomeInstance gen = getGenomeAsInstance();
    return ((gen.isRootInstance()) ? gen : gen.getVfgParentRoot());
  }
 
  /***************************************************************************
  **
  ** Answer if resident is an Instance
  */
  
  public boolean genomeIsAnInstance() {
    return (!(baseGenome_ instanceof DBGenome));
  }

  /***************************************************************************
  **
  ** Answer if resident is the Root Instance
  */
  
  public boolean genomeIsRootInstance() {
    if (baseGenome_ instanceof DBGenome) {
      return (false);
    }
    return (getGenomeAsInstance().isRootInstance());
  }
  
  /***************************************************************************
  **
  ** Answer if resident genome is the root genome
  */
  
  public boolean genomeIsRootGenome() {
    Genome gen = getGenome();
    return (gen.getID().equals(getDBGenome().getID()));
  }
 
  /***************************************************************************
  **
  ** Root Genome
  */
  
  public DBGenome getDBGenome() {
    return ((DBGenome)getGenomeSource().getGenome());
  }
  
  /***************************************************************************
  **
  ** Root Genome ID
  */
  
  public String getDBGenomeID() {
    Genome root = getGenomeSource().getGenome();
    return ((root == null) ? null : root.getID());
  }
   
  /***************************************************************************
  **
  ** New Key
  */
  
  public String getNextKey() {
    return (getDBGenome().getNextKey());
  }

  /***************************************************************************
  **
  ** Root layout
  */
  
  public Layout getRootLayout() {
    return (lSrc.getLayoutForGenomeKey(getGenomeSource().getGenome().getID()));
  }
  
  public void pushFrc(FontRenderContext frc) {
    frcStack_.add(frc);
    return;
  }
  
  public void popFrc() {
    if (frcStack_.isEmpty()) {
      throw new IllegalStateException();
    }
    frcStack_.remove(frcStack_.size() - 1);
    return;
  }
  
  public FontRenderContext getFrc() {
     if (frcStack_.isEmpty()) {
      return (baseFrc_);
    } else {
      return (frcStack_.get(frcStack_.size() - 1));
    }
  }  
  
  public void setFrc(FontRenderContext frc) {
    if (!frcStack_.isEmpty()) {
      throw new IllegalStateException();
    }
    baseFrc_ = frc;
    return;
  }

  public void pushGhosted(boolean localGhost) {
    ghostedStack_.add(Boolean.valueOf(localGhost));
    return;
  }

  public void popGhosted() {
    if (ghostedStack_.isEmpty()) {
      throw new IllegalStateException();
    }
    ghostedStack_.remove(ghostedStack_.size() - 1);
    return;
  }

  public boolean isGhosted() {
    if (ghostedStack_.isEmpty()) {
      return (baseGhosted_);
    } else {
      return (ghostedStack_.get(ghostedStack_.size() - 1).booleanValue());
    }
  }  
}