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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.nav.ImageManager;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.qpcr.QpcrXmlFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseFormatFactory;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This handles metabase creation from XML files
*/

public class MetabaseFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashSet<String> allKeys_; 
  private ParserClient currClient_;
  private HashMap<String, ParserClient> clients_;
  private FactoryWhiteboard sharedBoard_;
  private ImageManager.ImageManagerWorker imw_;
  
  private UIComponentSource uics_;
  private TabSource tSrc_;
  private UndoFactory uFac_;
  private DynamicDataAccessContext ddacx_;
  
  private NewDatabaseFactory ndf_;
  private FontManager.FontManagerWorker fmw_;
  private TimeAxisFactory taf_;   
  private DisplayOptions.DisplayOptionsWorker dow_;   
  private QpcrXmlFormatFactory qxff_;
  private TimeCourseFormatFactory tcff_;
  private CopiesPerEmbryoFormatFactory ceff_;
  private ColorGenerator.ColorGeneratorWorker cge_;
  private PerturbationData.PertDataWorker pdw_;
  private Metabase.PolicyWorker pw_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public MetabaseFactory(boolean isViewer, boolean isForAppend, UndoSupport forAppend,                         
                         DynamicDataAccessContext ddacx, UIComponentSource uics, 
                         TabSource tSrc, UndoFactory uFac) {
    
    uics_ = uics;
    tSrc_ = tSrc;
    uFac_ = uFac;
    ddacx_ = ddacx;
 
    allKeys_ = new HashSet<String>();
    sharedBoard_ = new FactoryWhiteboard();
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();

    ndf_ = new NewDatabaseFactory(sharedBoard_, isViewer, isForAppend, forAppend, uics_, tSrc_, uFac_, ddacx_);
    alist.add(ndf_);
    dow_ = new DisplayOptions.DisplayOptionsWorker(sharedBoard_, isForAppend);
    alist.add(dow_);
    fmw_ = new FontManager.FontManagerWorker(sharedBoard_, isForAppend);
    alist.add(fmw_);
    imw_ = new ImageManager.ImageManagerWorker(sharedBoard_, isForAppend);
    alist.add(imw_);
    qxff_ = new QpcrXmlFormatFactory(false, false);
    alist.add(qxff_);
    tcff_ = new TimeCourseFormatFactory(false, false, false, true);
    alist.add(tcff_);
    taf_ = new TimeAxisFactory(sharedBoard_, true);
    alist.add(taf_);
    pw_ = new Metabase.PolicyWorker(isForAppend);
    alist.add(pw_);   
    ceff_ = new CopiesPerEmbryoFormatFactory(false, true);
    alist.add(ceff_);
    pdw_ = new PerturbationData.PertDataWorker(false, false, true);
    alist.add(pdw_); 
    cge_ = new ColorGenerator.ColorGeneratorWorker(sharedBoard_, isForAppend);
    alist.add(cge_);
 
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
  ** Set client contexts
  **
  */
  
  public void setContext() {
    ndf_.setContext(ddacx_);
    imw_.setContext(uics_);
    fmw_.setContext(ddacx_);
    taf_.setContext(ddacx_); 
    dow_.setContext(ddacx_);  
    qxff_.setContext(ddacx_);
    tcff_.setContext(ddacx_);
    ceff_.setContext(ddacx_);
    cge_.setContext(ddacx_);
    pdw_.setContext(new TabPinnedDynamicDataAccessContext(ddacx_));
    pw_.setContext(ddacx_);
    return;
  }

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {
     
    if (currClient_ != null) {
      return (currClient_.processElement(elemName, attrs));
    }
    
    setContext();
    
    ParserClient pc = clients_.get(elemName);
    if (pc != null) {
      currClient_ = pc; 
      return (currClient_.processElement(elemName, attrs));
    }
    return (null);
  }
}

