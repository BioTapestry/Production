/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.cmd;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.SUPanel;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.gaggle.GooseAppInterface;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.perturb.PerturbationData;

/****************************************************************************
**
** Use this to centralize decision making on what main commands are active.
** Caches potentially expensive results for reuse.
*/

public class CheckGutsCache {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int NONE    = 0;
  public static final int LAYOUT  = 1;
  public static final int MODEL   = 2;
  public static final int SELECT  = 3;
  public static final int GENERAL = 4;    
  public static final int OVERLAY = 5;       
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private int type_;
  private Genome genome_;
  private BTState appState_;
  private Integer genomeCount_;
  private Boolean gaggleIsActive_;
  private Boolean showingModule_;    
  private Boolean overlayExists_;        
  private Boolean hasSelection_;  
  private Boolean hasNodeSelection_;  
  private Boolean hasMultiSelections_; 
  private Boolean hasAGroup_;        
  private Boolean hasANode_;     
  private Boolean hasALink_;   
  private Boolean hasAModule_;
  private Boolean linksHidden_;

  public CheckGutsCache(BTState appState, Genome genome, int type) {
    appState_ = appState;
    genome_ = genome;
    type_ = type;
    genomeCount_ = null;
    gaggleIsActive_ = null;
    showingModule_ = null;
    overlayExists_ = null;
    hasSelection_ = null;
    hasMultiSelections_ = null;  
    hasAGroup_ = null;
    hasANode_ = null;
    hasALink_ = null;
    hasAModule_ = null;
  }

  int getType() {
    return (type_);
  } 

  public boolean genomeNotNull() {
    return (genome_ != null);
  }

  public boolean genomeNotEmpty() {
    return ((genome_ != null) && !genome_.isEmpty());
  }

  public boolean genomeCanWriteSBML() {
    //
    // Currently only allow SBML on root genome
    //
    if (genome_ == null) {
      return (false);
    } else {
      return (genome_.canWriteSBML());
    }
  }

  public boolean moreThanOneModel() {
    return (loadGenomeCount() > 1);      
  }

  public boolean genomeHasImage() {
    if (genome_ == null) {
      return (false);
    }
    return (genome_.getGenomeImage() != null);
  }

  public boolean haveSubmodelsOrOverlay() {
    if (genome_ == null) {
      return (false);
    }
    if (loadGenomeCount() > 1) {  // Gotta have a submodel OR an overlay!
      return (true);
    }
    if (loadOverlayExists()) {
      return (true);
    }
    return (false);
  }

  public boolean haveASelection() {
    if ((genome_ == null) || genome_.isEmpty()) {
      return (false);
    }
    return (loadHasSelection());   
  }
  
  public boolean haveANodeSelection() {
    if ((genome_ == null) || genome_.isEmpty()) {
      return (false);
    }
    return (loadHasNodeSelection());   
  }

  public boolean hasMultiSelections() {
    if ((genome_ == null) || genome_.isEmpty()) {
      return (false);
    }
    return (loadHasMultiSelections());   
  }
  
  public boolean canAdd() {
    if (genome_ == null) {
      return (false);
    }
    if (genome_ instanceof DBGenome) {
      return (true);
    }
    if (genome_ instanceof DynamicGenomeInstance) {
      return (false);
    }
    return (loadHasAGroup());
  }

  public boolean canAddLink() {
    if (genome_ == null) {
      return (false);
    }
    if (genome_ instanceof DynamicGenomeInstance) {
      return (false);
    }
    if (loadLinksHidden()) {
      return (false);
    }

    return (loadHasANode());
  }
  
  public boolean canLayoutLinks() {
    if (genome_ == null) {
      return (false);
    }
    if (genome_ instanceof DynamicGenomeInstance) {
      return (false);
    }
    if (loadLinksHidden()) {
      return (false);
    }
    return (loadHasALink());
  }
  
  public boolean canPropagateDown() {     
    if (genome_ == null) {
      return (false);
    }
    if (genome_ instanceof GenomeInstance) {
      return (false);
    }
    if (loadGenomeCount() < 2) {  // Gotta have a submodel
      return (false);
    }
    //
    // Gotta have a selection
    //

    return (loadHasSelection());
  }

  public boolean genomeIsRoot() {
    if (genome_ == null) {
      return (false);
    }
    if (genome_ instanceof GenomeInstance) {
      return (false);
    }
    return (true);
  }

  public boolean isNonDynamicInstance() {
    if (genome_ == null) {
      return (false);
    } 
    return ((genome_ instanceof GenomeInstance) && !(genome_ instanceof DynamicGenomeInstance));
  } 

  public boolean isDynamicInstance() {
    if (genome_ == null) {
      return (false);
    } 
    return (genome_ instanceof DynamicGenomeInstance);      
  }  

  public boolean isNotDynamicInstance() { // Note not the strict negation of above
    if (genome_ == null) {
      return (false);
    } 
    return (!(genome_ instanceof DynamicGenomeInstance));      
  }    

  public boolean genomeIsRootInstance() {
    if (genome_ == null) {
      return (false);
    } 
    if (!(genome_ instanceof GenomeInstance)) {
      return (false);
    }

    if (genome_ instanceof DynamicGenomeInstance) {
      return (false);
    }
    GenomeInstance gi = (GenomeInstance)genome_;
    return (gi.getVfgParent() == null);
  }

  public boolean isRootOrRootInstance() {
    if (genome_ == null) {
      return (false);
    }

    if (genome_ instanceof DBGenome) {
      return (true);
    }

    GenomeInstance parent = ((GenomeInstance)genome_).getVfgParent();
    return (parent == null);       
  }   

  public boolean haveBuildInstructions() {
    if (genome_ == null) {
      return (false);
    } 
    return (appState_.getDB().haveBuildInstructions());      
  }

  public boolean gooseIsActive() {
    return (loadGooseActive());
  }

  public boolean canShowBubbles() {
    boolean gotModules = loadShowingModule();
    return ((genome_ != null) && (!genome_.isEmpty() || gotModules));    
  }
  
  public boolean canLayoutByOverlay() {
    boolean gotModules = loadShowingModule();
    return ((genome_ != null) && !genome_.isEmpty() && gotModules);    
  }
  
  public boolean haveTimeCourseData() {
    Database db = appState_.getDB();
    TimeCourseData tcd = db.getTimeCourseData();
    return ((tcd != null) && tcd.haveData());
  }

  public boolean timeCourseNotEmpty() { // not exactly the same test as above....
    Database db = appState_.getDB();
    TimeCourseData tcd = db.getTimeCourseData(); 
    return (!tcd.isEmpty());
  }
  
  public boolean timeCourseHasTemplate() {
    Database db = appState_.getDB();
    TimeCourseData tcd = db.getTimeCourseData(); 
    return (tcd.hasGeneTemplate());
  }
  
  public boolean genomeHasModule() {
    return ((genome_ != null) && loadHasAModule()); 
  }

  public boolean canPullDown() {
    if (genome_ == null) {
      return (false);
    }

    if (!(genome_ instanceof GenomeInstance)) {
      return (false);
    }
    GenomeInstance gi = (GenomeInstance)genome_;
    GenomeInstance parent = gi.getVfgParent();
    if (parent == null) {
      return (loadHasAGroup());
    } else {
      return (true);
    }
  }

  public boolean noModel() {
    if ((loadGenomeCount() == 0) || (genome_ == null)) {
      return (true);
    }
    return (false);
  }

  public boolean oneRoot() {
    if ((loadGenomeCount() != 1) || (genome_ == null)) {
      return (false);
    }
    return (true);      
  }      

  public boolean oneEmptyRoot() {
    if ((loadGenomeCount() != 1) || (genome_ == null) || !genome_.isEmpty()) {
      return (false);
    }
    return (true);      
  }

  public boolean hasPerturbationData() {
    Database db = appState_.getDB();
    PerturbationData pd = db.getPertData();
    return (pd.haveData());      
  }
  
  public boolean hasPertSources() {
    Database db = appState_.getDB();
    PerturbationData pd = db.getPertData();
    return (pd.havePertSources());      
  }

  public boolean canSquash() {
    if (genome_ == null) {
      return (false);
    }

    boolean genomeOK = (genome_ instanceof DBGenome); 

    if (!genomeOK) {
      GenomeInstance parent = ((GenomeInstance)genome_).getVfgParent();
      genomeOK = (parent == null);
    }

    if (!genomeOK) {
      return (false);
    }

    if (loadShowingModule()) {
      return (true);
    }

    return (!genome_.isEmpty());
  }

  public boolean canStretch() {
    return (canSquash());
  }

  private int loadGenomeCount() {
    if (genomeCount_ == null) {
      Database db = appState_.getDB();
      genomeCount_ = new Integer(db.getGenomeCount());
    }
    return (genomeCount_.intValue());
  } 

  private boolean loadGooseActive() {
    if (gaggleIsActive_ == null) {
      GooseAppInterface goose = appState_.getGooseMgr().getGoose();
      gaggleIsActive_ = new Boolean((goose != null) && goose.isActivated());
    }
    return (gaggleIsActive_.booleanValue());
  }

  private boolean loadShowingModule() {
    if (showingModule_ == null) {    
      showingModule_ = new Boolean((appState_.getCurrentOverlay() != null) && !appState_.getCurrentNetModules().set.isEmpty());
    }
    return (showingModule_.booleanValue());
  }   

  private boolean loadOverlayExists() {
    if (overlayExists_ == null) {  
      overlayExists_ = new Boolean((new FullGenomeHierarchyOracle(appState_)).overlayExists());
    }
    return (overlayExists_.booleanValue());
  }

  private boolean loadHasSelection() {
    if (hasSelection_ == null) { 
      SUPanel sup = appState_.getSUPanel();
      hasSelection_ = new Boolean(sup.hasASelection());
    }
    return (hasSelection_.booleanValue());
  } 
  
  private boolean loadHasNodeSelection() {
    if (hasNodeSelection_ == null) { 
      SUPanel sup = appState_.getSUPanel();
      hasNodeSelection_ = new Boolean(!sup.getSelectedNodes().isEmpty());
    }
    return (hasNodeSelection_.booleanValue());
  } 
  
  
  private boolean loadHasMultiSelections() {
    if (hasMultiSelections_ == null) { 
      hasMultiSelections_ = new Boolean(appState_.getZoomTarget().haveMultipleSelectionsForBounds());
    }
    return (hasMultiSelections_.booleanValue());
  }  
 
  private boolean loadLinksHidden() {
    if (linksHidden_ == null) { 
      SUPanel sup = appState_.getSUPanel();
      DataAccessContext rcx = new DataAccessContext(appState_, appState_.getGenome());
      linksHidden_ = new Boolean(sup.linksAreHidden(rcx));
    }
    return (linksHidden_.booleanValue());
  }          
  
  // Careful!  Check first that it is a genome instance before calling!
  private boolean loadHasAGroup() {
    GenomeInstance gi = (GenomeInstance)genome_;
    if (hasAGroup_ == null) {        
      hasAGroup_ = new Boolean(gi.getGroupIterator().hasNext());
    }
    return (hasAGroup_.booleanValue());
  }    

  private boolean loadHasANode() {
    if (hasANode_ == null) { 
      hasANode_ = new Boolean(genome_.getGeneIterator().hasNext() || genome_.getNodeIterator().hasNext());
    }
    return (hasANode_.booleanValue());
  } 
  
  private boolean loadHasALink() {
    if (hasALink_ == null) { 
      hasALink_ = new Boolean(genome_.getLinkageIterator().hasNext());
    }
    return (hasALink_.booleanValue());
  }  
  
  

  private boolean loadHasAModule() {
    if (hasAModule_ == null) { 
      hasAModule_ = new Boolean(genome_.getNetworkModuleCount() > 0);
    }
    return (hasAModule_.booleanValue());
  }      

}
