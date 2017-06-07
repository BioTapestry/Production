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
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.tabs.TabOps;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionFactory;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSetFactory;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.DBGenomeFactory;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxyFactory;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.GenomeInstanceFactory;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.nav.UserTreePathFactory;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.plugin.ModelBuilderPlugIn;
import org.systemsbiology.biotapestry.plugin.PlugInManager;
import org.systemsbiology.biotapestry.qpcr.QpcrXmlFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseFormatFactory;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.LayoutFactory;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This handles database creation from XML files
*/

public class DatabaseFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private UIComponentSource uics_;
  private TabSource tSrc_;
  private UndoFactory uFac_;
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
  private FactoryWhiteboard sharedBoard_;
  private boolean isFirstElem_;
  private UndoSupport supportForAppend_;
  private BuildInstructionFactory bif_;
  private TemporalInputRangeData.TemporalInputRangeWorker tirw_;
  private FontManager.FontManagerWorker fmw_;
  private UserTreePathFactory utpf_;
  private InstanceInstructionSetFactory iisf_;
  private ImageManager.ImageManagerWorker imw_;
  private ModelDataFactory mdf_;  
  private TimeAxisFactory taf_;   
  private DisplayOptions.DisplayOptionsWorker dow_;   
  private QpcrXmlFormatFactory qxff_;
  private TimeCourseFormatFactory tcff_;
  private CopiesPerEmbryoFormatFactory ceff_;
  private ColorGenerator.ColorGeneratorWorker cge_;
  private PerturbationData.PertDataWorker pdw_;
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DatabaseFactory(boolean isViewer, 
                         boolean isForAppend, UndoSupport forAppend, 
                         DynamicDataAccessContext ddacx, UIComponentSource uics, 
                         TabSource tSrc, UndoFactory uFac) {
    
    uics_ = uics;
    tSrc_ = tSrc;
    uFac_ = uFac;
    ddacx_ = ddacx;
    isFirstElem_ = true;
    supportForAppend_ = forAppend;
    allKeys_ = new HashSet<String>();
    isForAppend_ = isForAppend;
    dbHeaderKeys_ = DBGenome.keywordsOfInterest();  
    sharedBoard_ = new FactoryWhiteboard();
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    dbgf_ = new DBGenomeFactory();
    alist.add(dbgf_);
    gif_ = new GenomeInstanceFactory();
    alist.add(gif_); 
    lof_ = new LayoutFactory();
    alist.add(lof_); 
    mdf_ = new ModelDataFactory(sharedBoard_, isForAppend);
    alist.add(mdf_);
    taf_ = new TimeAxisFactory(sharedBoard_, false);
    alist.add(taf_);
    bif_ = new BuildInstructionFactory();
    alist.add(bif_);
    iisf_ = new InstanceInstructionSetFactory();
    alist.add(iisf_);
    dow_ = new DisplayOptions.DisplayOptionsWorker(sharedBoard_, isForAppend);
    alist.add(dow_);
    fmw_ = new FontManager.FontManagerWorker(sharedBoard_, isForAppend);
    alist.add(fmw_);
    imw_ = new ImageManager.ImageManagerWorker(sharedBoard_, isForAppend);    
    alist.add(imw_);
    utpf_ = new UserTreePathFactory();
    alist.add(utpf_);
    qxff_ = new QpcrXmlFormatFactory(false, false);
    alist.add(qxff_);
    tcff_ = new TimeCourseFormatFactory(false, false, true, false);
    alist.add(tcff_);
    ceff_ = new CopiesPerEmbryoFormatFactory(false, false);
    alist.add(ceff_);
    tirw_ = new TemporalInputRangeData.TemporalInputRangeWorker(sharedBoard_, false);
    alist.add(tirw_);
    cge_ = new ColorGenerator.ColorGeneratorWorker(sharedBoard_, isForAppend);
    alist.add(cge_);
    dipf_ = new DynamicInstanceProxyFactory();
    alist.add(dipf_);
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
        allKeys_.add(key);
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
  ** Get merge issue list
  */

  public List<String> getMergeErrors() { 
    return (sharedBoard_.mergeIssues);    
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
    if (elemName.equals("BioTapestry")) {
      Map<String, String> aikm = sharedBoard_.appendImgKeyMap;
      if (aikm != null) {
        ImageManager.repairMergedRefs(ddacx_, aikm);
      }
      Map<String, String> ackm = sharedBoard_.appendColorKeyMap;
      if (ackm != null) {
        ColorGenerator.repairMergedRefs(ddacx_, ackm);
      }

      TabNameData tnd = ddacx_.getGenomeSource().getTabNameData();
      if (isForAppend_) {
        TabNameData.TabNameDataWorker.tweakForAppendUniqueness(tSrc_, ddacx_, tnd);
        ddacx_.getGenomeSource().setTabNameData(tnd);
      }    
      uics_.getCommonView().setTabTitleData(tSrc_.getCurrentTabIndex(), tnd);  
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
    
    //
    // We need to get the tab stuff sorted out ASAP so legacy BTP loads can be successfully
    // appended as new tabs. So the first time any tab we are interested in shows up through
    // the door, we deal with it.
    //
    
    if (allKeys_.contains(elemName) && isFirstElem_) {
      isFirstElem_ = false;
      if (!isForAppend_) {
        tSrc_.resetTabForLoad(ddacx_.getMetabase().legacyDBID(), 0);
      } else { 
        TabOps.doNewTabStat(tSrc_, uics_, ddacx_, uFac_, true, null, tSrc_.getNumTab(), null, null, null, null, supportForAppend_);
      }
      dbgf_.setContext(ddacx_);
      gif_.setContext(ddacx_);
      dipf_.setContext(ddacx_);
      lof_.setContext(ddacx_);
      bif_.setContext(ddacx_);
      tirw_.setContext(ddacx_);
      fmw_.setContext(ddacx_);
      utpf_.setContext(uics_);
      iisf_.setContext(ddacx_);
      imw_.setContext(uics_);
      mdf_.setContext(ddacx_, uics_, tSrc_);  
      taf_.setContext(ddacx_);
      dow_.setContext(ddacx_);
      qxff_.setContext(ddacx_);
      tcff_.setContext(ddacx_);
      ceff_.setContext(ddacx_);
      cge_.setContext(ddacx_);
      pdw_.setContext(new TabPinnedDynamicDataAccessContext(ddacx_));  
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

