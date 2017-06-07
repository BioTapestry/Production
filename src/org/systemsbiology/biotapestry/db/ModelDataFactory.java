/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This builds a ModelData, AND NOW also time axis and workspace and start view, 
** other database-direct-stored items!
*/

public class ModelDataFactory extends AbstractFactoryClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private static final int NONE_        = 0;  
  private static final int DATE_        = 1;
  private static final int ATTRIBUTION_ = 2;
  private static final int KEY_         = 3;  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private DataAccessContext dacx_;
  private MyWorkspaceGlue mwg_;
  private MyStartupViewGlue svg_;
  private TabNameData.TabNameDataWorker tndw_;
  
  private int charTarget_;
   
  private Set<String> modelDataKeys_;
  private String attribKey_;
  private String dateKey_;
  private String keyKey_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for the model data factory
  */

  public ModelDataFactory(FactoryWhiteboard whiteboard, boolean forAppend) {
    super(whiteboard);
 
    modelDataKeys_ = ModelData.keywordsOfInterest();
    attribKey_ = ModelData.attributionKeyword();
    dateKey_ = ModelData.dateKeyword();
    keyKey_ = ModelData.keyEntryKeyword();
    
    AbstractFactoryClient wfc = new Workspace.WorkspaceWorker(whiteboard);
    mwg_ = new MyWorkspaceGlue();
    installWorker(wfc, mwg_);
    AbstractFactoryClient svfc = new StartupView.StartupViewWorker(whiteboard);
    svg_ = new MyStartupViewGlue();
    installWorker(svfc, svg_);
    tndw_ = new TabNameData.TabNameDataWorker(whiteboard, forAppend);
    installWorker(tndw_, null);
        
    myKeys_.addAll(wfc.keywordsOfInterest());
    myKeys_.addAll(svfc.keywordsOfInterest());
    myKeys_.addAll(tndw_.keywordsOfInterest());
    myKeys_.addAll(modelDataKeys_);
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

  public void setContext(DataAccessContext dacx, UIComponentSource uics, TabSource tSrc) {
    dacx_ = dacx;
    mwg_.setContext(dacx);
    svg_.setContext(dacx);
    tndw_.setContext(dacx, uics, tSrc);
    return;
  }

  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  @Override
  public void localFinishElement(String elemName) {
    charTarget_ = NONE_;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  @Override
  public void localProcessCharacters(char[] chars, int start, int length) {
    String nextString = new String(chars, start, length);
    ModelData md = null;
    switch (charTarget_) {
      case NONE_:
        break;
      case DATE_:
        md = (ModelData)container_;
        md.appendDate(nextString);
        break;
      case ATTRIBUTION_:
        md = (ModelData)container_;
        md.appendAttribution(nextString);
        break;
      case KEY_:
        md = (ModelData)container_;
        md.appendKey(nextString);
        break;        
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object localProcessElement(String elemName, Attributes attrs) throws IOException {
    if (modelDataKeys_.contains(elemName)) {
      ModelData md = ModelData.buildFromXML(elemName, attrs);
      if (md != null) {
        dacx_.getModelDataSource().setModelData(md);
        return (md);
      }
      return (null);
    } else if (dateKey_.equals(elemName)) {
      charTarget_ = DATE_;
    } else if (attribKey_.equals(elemName)) {
      charTarget_ = ATTRIBUTION_;            
    } else if (keyKey_.equals(elemName)) {
      ModelData md = (ModelData)container_;
      md.startKey("");
      charTarget_ = KEY_;
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Adds workspace to database
  **
  */  
  
  public static class MyWorkspaceGlue implements GlueStick {
    
    private DataAccessContext dacx_;
    
    public MyWorkspaceGlue() {
    }
     
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }
    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      dacx_.getWorkspaceSource().setWorkspace(board.workspace);
      return (null);
    }
  } 
  
  /***************************************************************************
  **
  ** Adds startup view to database
  **
  */  
  
  public static class MyStartupViewGlue implements GlueStick {
    
    private DataAccessContext dacx_;
    
    public MyStartupViewGlue() {
    }   
    
    public void setContext(DataAccessContext dacx) {
      dacx_ = dacx;
      return;
    }

    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      dacx_.getGenomeSource().setStartupView(board.startupView);
      return (null);
    }
  } 
}
