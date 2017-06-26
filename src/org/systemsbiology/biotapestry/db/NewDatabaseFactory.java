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

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.tabs.TabOps;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionFactory;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSetFactory;
import org.systemsbiology.biotapestry.genome.DBGenomeFactory;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxyFactory;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.GenomeInstanceFactory;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.nav.UserTreePathFactory;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseFormatFactory;
import org.systemsbiology.biotapestry.ui.LayoutFactory;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This handles database creation from XML files
*/

public class NewDatabaseFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private UIComponentSource uics_;
  private TabSource tSrc_;
  private UndoFactory uFac_;
  private DataAccessContext dacx_;
  private DynamicDataAccessContext ddacx_;
  
  private Set<String> dbHeaderKeys_; 
  private HashSet<String> allKeys_; 
  private ParserClient currClient_;
  private HashMap<String, ParserClient> clients_;
  private DBGenomeFactory dbgf_;
  private GenomeInstanceFactory gif_;
  private DynamicInstanceProxyFactory dipf_;
  private LayoutFactory lof_;
  private boolean isForAppend_;
  private int appendBase_;
  private FactoryWhiteboard sharedBoard_;
  private UndoSupport supportForAppend_;
  private BuildInstructionFactory bif_;
  private UserTreePathFactory utpf_;
  private InstanceInstructionSetFactory iisf_;
  private TimeCourseDataMaps.TimeCourseDataMapsWorker tcdmw_;
  private NavTree.NavTreeWorker ntw_;
  private ModelDataFactory mdf_;
  private TimeCourseFormatFactory tcff_;
  private TimeAxisFactory taxf_; 
  private CopiesPerEmbryoFormatFactory cprff_;
  private PerturbationData.PertDataWorker pdw_;
  private PerturbationDataMaps.DataMapsWorker pdmw_;
  private TemporalInputRangeData.TemporalInputRangeWorker tirw_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public NewDatabaseFactory(FactoryWhiteboard sharedBoard, boolean isViewer, 
                            boolean isForAppend, UndoSupport forAppend, UIComponentSource uics, 
                            TabSource tSrc, UndoFactory uFac, DynamicDataAccessContext ddacx) {
      
    sharedBoard_ = sharedBoard;
    isForAppend_ = isForAppend;
    supportForAppend_ = forAppend;
    uics_ = uics;
    tSrc_ = tSrc;
    uFac_ = uFac;
    ddacx_ = ddacx;
    dbHeaderKeys_ = Database.keywordsOfInterest();  
    allKeys_ = new HashSet<String>();
    allKeys_.addAll(dbHeaderKeys_);
    
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    dbgf_ = new DBGenomeFactory();
    alist.add(dbgf_);
    gif_ = new GenomeInstanceFactory();
    alist.add(gif_);
    lof_ = new LayoutFactory();
    alist.add(lof_);
    mdf_ = new ModelDataFactory(sharedBoard_, isForAppend);   
    alist.add(mdf_);
    bif_ = new BuildInstructionFactory();
    alist.add(bif_);
    iisf_ = new InstanceInstructionSetFactory();
    alist.add(iisf_);
    utpf_ = new UserTreePathFactory();
    alist.add(utpf_);
    tirw_ = new TemporalInputRangeData.TemporalInputRangeWorker(sharedBoard_, false);
    alist.add(tirw_);
    ntw_ = new NavTree.NavTreeWorker(sharedBoard_);
    alist.add(ntw_);  
    dipf_ = new DynamicInstanceProxyFactory();
    alist.add(dipf_);
    tcdmw_ = new TimeCourseDataMaps.TimeCourseDataMapsWorker(sharedBoard_, false);
    alist.add(tcdmw_);
    pdmw_ = new PerturbationDataMaps.DataMapsWorker(sharedBoard_);
    alist.add(pdmw_);
    
    
    //
    // We need to be able to load per-tab data, so this needs to be here as well:
    //
    
    tcff_ = new TimeCourseFormatFactory(false, false, false, false);
    alist.add(tcff_);
    taxf_ = new TimeAxisFactory(sharedBoard_, false);
    alist.add(taxf_);  
    cprff_ = new CopiesPerEmbryoFormatFactory(false, false);
    alist.add(cprff_);
    pdw_ = new PerturbationData.PertDataWorker(false, false, false);
    alist.add(pdw_); 

    
    if (!isViewer) {
      PlugInManager pluginManager = uics_.getPlugInMgr(); 
      Iterator<ModelBuilderPlugIn> mbIterator = pluginManager.getBuilderIterator();
      if (mbIterator.hasNext()) {
         ModelBuilder.DataLoader mbdl = mbIterator.next().getDataLoader();
         alist.add(mbdl);
      }
    }
    Iterator<ParserClient> cit = alist.iterator();
    clients_ = new HashMap<String, ParserClient>();
    while (cit.hasNext()) {
      ParserClient pc = cit.next();
      Set<String> keys = pc.keywordsOfInterest();
      Iterator<String> ki = keys.iterator();
      while (ki.hasNext()) {
        String key = ki.next();
        Object prev = clients_.put(key, pc);
        if (prev != null) {
          throw new IllegalArgumentException();
        }
      }
    }
    currClient_ = null;
    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set data context
  **
  */  

  public void setContext(DataAccessContext dacx) {
    dacx_ = dacx;
    return;
  }
  
  /***************************************************************************
  **
  ** Set data context
  **
  */  

  void setClientContext(TabPinnedDynamicDataAccessContext dacx) {
    dacx_ = dacx;
    dbgf_.setContext(dacx);
    gif_.setContext(dacx);
    dipf_.setContext(dacx);
    lof_.setContext(dacx);
    bif_.setContext(dacx);
    utpf_.setContext(uics_);
    iisf_.setContext(dacx);
    tcdmw_.setContext(dacx);
    pdmw_.setContext(dacx);
    ntw_.setContext(dacx);
    mdf_.setContext(dacx, uics_, tSrc_);
    tcff_.setContext(dacx);
    taxf_.setContext(dacx);  
    cprff_.setContext(dacx);
    pdw_.setContext(dacx);
    tirw_.setContext(dacx);
    return;
  }

  /***************************************************************************
  ** 
  ** Set the container
  */

  public void setContainer(Object container) {
    if (currClient_ != null) {
      currClient_.setContainer(container);
    }    
    return;    
  }
    
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) throws IOException {
    
    if (dbHeaderKeys_.contains(elemName)) { 
      Map<String, String> aikm = sharedBoard_.appendImgKeyMap;
      if (aikm != null) {
        ImageManager.repairMergedRefs(dacx_, aikm);
      }
      Map<String, String> ackm = sharedBoard_.appendColorKeyMap;
      if (ackm != null) {
        ColorGenerator.repairMergedRefs(dacx_, ackm);
      }
    }  
    
    if (currClient_ == null) {
      return (false);
    }

    if (currClient_.finishElement(elemName)) {
      currClient_ = null;
    }
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
    if (currClient_ != null) {
      currClient_.processCharacters(chars, start, length);
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public Set<String> keywordsOfInterest() {
    return (allKeys_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {
   
    if (dbHeaderKeys_.contains(elemName)) {
      int numTab = Database.extractTab(elemName, attrs);
      String id = Database.extractID(elemName, attrs);
      Boolean sharing = Database.extractSharing(elemName, attrs);
      if (!isForAppend_) {
        if (numTab == 0) {
          tSrc_.resetTabForLoad(id, numTab);
          if (!sharing.booleanValue()) {
             dacx_.getLocalDataCopyTarget().installDataSharing(null, ddacx_.getTabContext(id));
          } else {
             dacx_.getLocalDataCopyTarget().installDataSharing(dacx_.getMetabase().getDataSharingPolicy(), ddacx_.getTabContext(id));
          }
        } else {
          TabOps.doNewTabStat(tSrc_, uics_, dacx_, uFac_, true, id, numTab, null, null, null, sharing, null);
        }
      } else { 
        if (numTab == 0) {
          appendBase_ = tSrc_.getNumTab();	
        }
        numTab = appendBase_ + numTab;
        TabOps.doNewTabStat(tSrc_, uics_, dacx_, uFac_, true, null, numTab, null, null, null, sharing, supportForAppend_);
        // Gotta reset the id so that we create the correct tab context below
        id = tSrc_.getDbIdForIndex(numTab);
      }
      setClientContext(ddacx_.getTabContext(id));
      return (null);
    }
    
    if (currClient_ != null) {
      return (currClient_.processElement(elemName, attrs));
    }

    ParserClient pc = clients_.get(elemName);
    if (pc != null) {
      currClient_ = pc; 
      return (currClient_.processElement(elemName, attrs));
    }
    return (null);
  }

}

