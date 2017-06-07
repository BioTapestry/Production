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

package org.systemsbiology.biotapestry.db;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
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
** Bundle up all the various common arguments!
*/

public abstract class DataAccessContext {
    
  private boolean isForWeb_;
  private double pixDiam_;
  private boolean showBubbles_;
  private ArrayList<FontRenderContext> frcStack_;
  private FontRenderContext baseFrc_;
  private ArrayList<Boolean> ghostedStack_;
  private boolean baseGhosted_;
  protected Metabase mb_;
  
  protected DataAccessContext(Metabase mb, boolean isForWeb, boolean baseGhosted, FontRenderContext frc) {
    mb_ = mb;
    isForWeb_ = isForWeb;
    baseGhosted_ = baseGhosted;
    frcStack_ = new ArrayList<FontRenderContext>();
    ghostedStack_ = new ArrayList<Boolean>();
    baseFrc_ = frc;
    if (frc == null) {
      baseFrc_ = new FontRenderContext(new AffineTransform(), true, true);
      System.out.println("FIXME");
    }
  }

  protected DataAccessContext(DataAccessContext other) {
    this.mb_ = other.mb_;
    this.isForWeb_ = other.isForWeb_;
    this.baseGhosted_ = other.baseGhosted_;
    this.pixDiam_ = other.pixDiam_;
    this.showBubbles_ = other.showBubbles_;
    this.baseFrc_ = other.baseFrc_;
    this.frcStack_ = new ArrayList<FontRenderContext>(other.frcStack_);
    this.ghostedStack_ = new ArrayList<Boolean>(other.ghostedStack_);
  }
  
  public Metabase getMetabase() {
    return (mb_);   
  }
  
  public boolean isForWeb() {
    return (isForWeb_);   
  }
  
  public boolean getShowBubbles() {
    return (showBubbles_); 
  }
  
  public void setShowBubbles(boolean setBubbles) {
    showBubbles_ = setBubbles;
    return;
  }
  
  public void setPixDiam(double pixDiam) {
    pixDiam_ = pixDiam;
    return;
  }
  
  public double getPixDiam() {
    return (pixDiam_);
  }
  
  public NetOverlayOwner getCurrentOverlayOwner() {
    return (getGenomeSource().getOverlayOwnerFromGenomeKey(getCurrentGenomeID()));
  }
  
  protected void clearStacksAndGhosting(boolean isGhosted) {
    this.frcStack_.clear();
    this.baseGhosted_ = isGhosted;
    this.ghostedStack_.clear();
    return;
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
    return (getLayoutSource().getLayoutForGenomeKey(getGenomeSource().getRootDBGenome().getID()));
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
    if (baseFrc_ == null) {
      System.out.println("getfrc " + baseFrc_ + " " + frcStack_.size());
    }
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
  
  /***************************************************************************
  **
  ** 
  */
  
  public void newModel() {
    if (!(getGenomeSource() instanceof Database)) {
      throw new IllegalStateException();
    }
    mb_.newModelViaDACX();
    // FIXME: This is pretty horrible:
    Database db = (Database)getGenomeSource();
    db.newModelViaDACX(new DynamicDataAccessContext(mb_.getAppState()).getTabContext(db.getID()));
    newModelFollowOn();
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public void drop() {
    if (!(getGenomeSource() instanceof Database)) {
      throw new IllegalStateException();
    }
    mb_.dropViaDACX();
    // FIXME: This is pretty horrible:
    Database db = (Database)getGenomeSource();
    db.dropViaDACX(new DynamicDataAccessContext(mb_.getAppState()).getTabContext(db.getID()));
    dropFollowOn();
    return;
  }

  /***************************************************************************
  **
  ** 
  */
  
  public DatabaseChange dropRootNetworkOnly() {
    boolean replace = currentGenomeIsRootDBGenome();
    DatabaseChange retval = getGenomeSource().dropRootNetworkOnly();
    dropRootNetworkOnlyFollowOn(replace);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** 
  */
  
  public abstract void newModelFollowOn();
 
  public abstract void dropFollowOn();
  
  public abstract void dropRootNetworkOnlyFollowOn(boolean replace);
  
  /***************************************************************************
  **
  ** 
  */
  
  public abstract ResourceManager getRMan();
  
  public abstract LayoutOptionsManager getLayoutOptMgr();
   
  public abstract ExperimentalDataSource getExpDataSrc();
  
  public abstract TemporalRangeSource getTemporalRangeSrc();
  
  public abstract DataMapSource getDataMapSrc();
  
  public abstract InstructionSource getInstructSrc();
  
  public abstract ModelDataSource getModelDataSource();
 
  public abstract ModelBuilder.Source getWorksheetSrc(); 
  
  public abstract OverlayStateOracle getOSO();
  
  public abstract OverlayStateWriter getOverlayWriter();
 
  public abstract ColorResolver getColorResolver();
  
  public abstract FontManager getFontManager();
  
  public abstract DBGenome getDBGenome();
  
  public abstract LayoutSource getLayoutSource();

  public abstract GenomeSource getGenomeSource();
  
  public abstract WorkspaceSource getWorkspaceSource();

  public abstract Genome getCurrentGenome();

  public abstract GenomeInstance getCurrentGenomeAsInstance();
  
  public abstract DynamicGenomeInstance getCurrentGenomeAsDynamicInstance();
 
  public abstract DBGenome getCurrentGenomeAsDBGenome();
  
  public abstract String getCurrentGenomeID();

  public abstract String getCurrentLayoutID();
 
  public abstract Layout getCurrentLayout();
  
  public abstract FullGenomeHierarchyOracle getFGHO();
  
  public abstract GroupSettingSource getGSM();
 
  public abstract ZoomTargetSupport getZoomTarget();
  
  public abstract MinimalDispOptMgr getDisplayOptsSource();
  
  public abstract SimParamSource getSimParamSource();
  
  public abstract LocalDataCopyTarget getLocalDataCopyTarget();
 
  /***************************************************************************
  **
  ** Answer if resident is an Instance
  */
  
  public boolean currentGenomeIsAnInstance() {
    return (!(getCurrentGenome() instanceof DBGenome));
  }
  
  /***************************************************************************
  **
  ** Answer if resident is a dynamic instance
  */
  
  public boolean currentGenomeIsADynamicInstance() {
    return (getCurrentGenome() instanceof DynamicGenomeInstance);
  }
  
  /***************************************************************************
  **
  ** Answer if resident is the Root Instance
  */
  
  public boolean currentGenomeIsRootInstance() {
    if (getCurrentGenome() instanceof DBGenome) {
      return (false);
    }
    return (getCurrentGenomeAsInstance().isRootInstance());
  }
  
  /***************************************************************************
  **
  ** Answer if we are outside bounds
  */
  
  public boolean modelIsOutsideWorkspaceBounds() {
    return (!getWorkspaceSource().getWorkspace().contains(getZoomTarget().getAllModelBounds()));
  }

  /***************************************************************************
  **
  ** Root Instance
  */
  
  public GenomeInstance getRootInstanceFOrCurrentInstance() {
    GenomeInstance gen = getCurrentGenomeAsInstance();
    return ((gen.isRootInstance()) ? gen : gen.getVfgParentRoot());
  }
 

  /***************************************************************************
  **
  ** Answer if current genome is the root genome
  */
  
  public boolean currentGenomeIsRootDBGenome() {
    Genome gen = getCurrentGenome();
    return (gen instanceof DBGenome);
  }
  
  /***************************************************************************
  **
  ** Root Genome ID
  */
  
  public String getDBGenomeID() {
    Genome root = getGenomeSource().getRootDBGenome();
    return ((root == null) ? null : root.getID());
  }
}