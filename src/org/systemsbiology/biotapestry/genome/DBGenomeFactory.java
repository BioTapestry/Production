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

package org.systemsbiology.biotapestry.genome;

import java.util.Set;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This builds DBGenomes in BioTapestry
*/

public class DBGenomeFactory extends AbstractFactoryClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private static final int NONE_             = 0;  
  private static final int LONG_NAME_        = 1;
  private static final int DESCRIPTION_      = 2;
  private static final int NODE_DESCRIPTION_ = 3;  
  private static final int NODE_URL_         = 4;
  private static final int NOTE_             = 5;
  private static final int LINK_DESCRIPTION_ = 6;  
  private static final int LINK_URL_         = 7;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private DBGenome currGenome_;
  private DBGene currGene_;
  private DBNode currNode_;  
  private DBLinkage currLink_;  
  private DBInternalLogic currILog_;
  private Note currNote_;
  private int charTarget_;
  
  private Set<String> genomeKeys_;
  private Set<String> geneKeys_;
  private Set<String> nodeKeys_;
  private Set<String> linkKeys_;
  private Set<String> regionKeys_;
  private Set<String> logicKeys_;
  private Set<String> internalNodeKeys_;
  private Set<String> internalLinkKeys_;
  private Set<String> noteKeys_;  
  
  private String simParamKey_;
  private String descripKey_;
  private String nodeDescripKey_;
  private String urlKeyword_;
  private String longNameKey_;
  private String linkDescripKey_;
  private String linkUrlKeyword_;
  
  private StringBuffer descBuf_;
  private StringBuffer urlBuf_;
  
  private NetworkOverlay.NetOverlayWorker now_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for a genome factory
  */

  public DBGenomeFactory() {
    super(new FactoryWhiteboard());
    dacx_ = null;
    genomeKeys_ = DBGenome.keywordsOfInterest();
    geneKeys_ = DBGene.keywordsOfInterest();
    nodeKeys_ = DBNode.keywordsOfInterest();
    linkKeys_ = DBLinkage.keywordsOfInterest();
    regionKeys_ = DBGeneRegion.keywordsOfInterest();
    logicKeys_ = DBInternalLogic.keywordsOfInterest();
    internalNodeKeys_ = InternalFunction.keywordsOfInterest();
    internalLinkKeys_ = InternalLink.keywordsOfInterest();
    noteKeys_ = Note.keywordsOfInterest(false);    
    simParamKey_ = DBInternalLogic.getSimParamKeyword();
    descripKey_ = DBGenome.descriptionKeyword();
    nodeDescripKey_ = DBNode.descriptionKeyword();
    urlKeyword_ = DBNode.urlKeyword();
    longNameKey_ = DBGenome.longNameKeyword();
    linkDescripKey_ = DBLinkage.descriptionKeyword();
    linkUrlKeyword_ = DBLinkage.urlKeyword();
    

    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    whiteboard.genomeType = NetworkOverlay.DB_GENOME;
    now_ = new NetworkOverlay.NetOverlayWorker(whiteboard);
    installWorker(now_, new MyGlue());
    myKeys_.addAll(genomeKeys_);
    
    descBuf_ = new StringBuffer();
    urlBuf_ = new StringBuffer();
  }


  ///////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Set the current context
  */
  
  public void setContext(DataAccessContext dacx) {
    dacx_ = dacx;
    now_.installContext(dacx);
    return;
  }

  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  @Override
  protected void localFinishElement(String elemName) {
    if (charTarget_ == NODE_DESCRIPTION_) {
      if (!elemName.equals(nodeDescripKey_)) {
        throw new IllegalStateException();
      }
      currNode_.appendDescription(descBuf_.toString());
    } else if (charTarget_ == NODE_URL_) {
      if (!elemName.equals(urlKeyword_)) {
        throw new IllegalStateException();
      }
      currNode_.addUrl(urlBuf_.toString());
    } else if (charTarget_ == LINK_DESCRIPTION_) {
      if (!elemName.equals(linkDescripKey_)) {
        throw new IllegalStateException();
      }
      currLink_.appendDescription(descBuf_.toString());
    } else if (charTarget_ == LINK_URL_) {
      if (!elemName.equals(linkUrlKeyword_)) {
        throw new IllegalStateException();
      }
      currLink_.addUrl(urlBuf_.toString());
    }
    charTarget_ = NONE_;
    return;
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  @Override
  protected void localProcessCharacters(char[] chars, int start, int length) {
    String nextString = new String(chars, start, length);
    switch (charTarget_) {
      case NONE_:
        break;
      case LONG_NAME_:
        currGenome_.appendLongName(nextString);
        break;
      case DESCRIPTION_:
        currGenome_.appendDescription(nextString);
        break;
      case NODE_DESCRIPTION_:
      case LINK_DESCRIPTION_:
        descBuf_.append(nextString);
        break;
      case NODE_URL_:
      case LINK_URL_:
        urlBuf_.append(nextString);
        break;
      case NOTE_:
        if (currNote_ != null) {
          currNote_.appendToNote(nextString);
        }
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
    
    if (genomeKeys_.contains(elemName)) {
      DBGenome dbg = DBGenome.buildFromXML(dacx_, elemName, attrs);
      if (dbg != null) {
        dacx_.getGenomeSource().setGenome(dbg);
        currGenome_ = dbg;
        ((FactoryWhiteboard)sharedWhiteboard_).genome = dbg;
        NavTree tree = dacx_.getGenomeSource().getModelHierarchy();
        if (tree.needLegacyGlue()) {
          tree.addNode(NavTree.Kids.ROOT_MODEL, null, null, new NavTree.ModelID(dbg.getID()), null, null, dacx_.getRMan());           
        }
        return (dbg);
      }
      return (null);
    } else if (geneKeys_.contains(elemName)) {
      currGene_ = DBGene.buildFromXML(dacx_, attrs);
      currNode_ = currGene_;
      currGenome_.addGene(currGene_);
    } else if (nodeKeys_.contains(elemName)) {
      currNode_ = DBNode.buildFromXML(dacx_, elemName, attrs);      
      currGenome_.addNode(currNode_);
    } else if (linkKeys_.contains(elemName)) {
      currLink_ = DBLinkage.buildFromXML(dacx_, attrs);
      currGenome_.addLinkage(currLink_);
    } else if (regionKeys_.contains(elemName)) {
      currGene_.addRegion(DBGeneRegion.buildFromXML(attrs));      
    } else if (logicKeys_.contains(elemName)) {
      DBInternalLogic ilog = DBInternalLogic.buildFromXML(elemName, attrs);
      if (ilog != null) {
        currILog_ = ilog;
        currNode_.setInternalLogic(currILog_);
      }
    } else if (internalNodeKeys_.contains(elemName)) {
      InternalFunction ifunc = InternalFunction.buildFromXML(dacx_, attrs);
      currILog_.addFunctionNodeWithExistingLabel(ifunc);
    } else if (internalLinkKeys_.contains(elemName)) {
      InternalLink.buildFromXML(dacx_, attrs);
      InternalLink iLink = InternalLink.buildFromXML(dacx_, attrs);
      currILog_.addFunctionLinkWithExistingLabel(iLink);
    } else if (noteKeys_.contains(elemName)) {
      Note newNote = Note.buildFromXML(elemName, attrs);
      if (newNote != null) {
        charTarget_ = NOTE_;
        currNote_ = newNote;
        currGenome_.addNote(currNote_);
      } 
    } else if (simParamKey_.equals(elemName)) {
      currILog_.addSimParam(elemName, attrs);      
    } else if (descripKey_.equals(elemName)) {
      charTarget_ = DESCRIPTION_;
    } else if (nodeDescripKey_.equals(elemName)) {
      charTarget_ = NODE_DESCRIPTION_;
      descBuf_.setLength(0);
    } else if (urlKeyword_.equals(elemName)) {
      charTarget_ = NODE_URL_;
      urlBuf_.setLength(0);  
    } else if (linkDescripKey_.equals(elemName)) {
      charTarget_ = LINK_DESCRIPTION_;
      descBuf_.setLength(0);
    } else if (linkUrlKeyword_.equals(elemName)) {
      charTarget_ = LINK_URL_;
      urlBuf_.setLength(0);
    } else if (longNameKey_.equals(elemName)) {
      charTarget_ = LONG_NAME_;
    }
    return (null);
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Genome genome = board.genome;
      NetworkOverlay netOvr = board.netOvr;
      if (netOvr != null) {
        genome.addNetworkOverlayAndKey(netOvr);
      }
      return (null);
    }
  }    
}
