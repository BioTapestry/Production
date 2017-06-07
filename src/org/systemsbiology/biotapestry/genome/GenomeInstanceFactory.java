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

import javax.swing.tree.TreeNode;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This builds GenomeInstances in BioTapestry
*/

public class GenomeInstanceFactory extends AbstractFactoryClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private static final int NONE_             = 0;  
  private static final int LONG_NAME_        = 1;
  private static final int DESCRIPTION_      = 2;
  private static final int NOTE_             = 3;
  private static final int NODE_DESCRIPTION_ = 4;  
  private static final int NODE_URL_         = 5;
  private static final int LINK_DESCRIPTION_ = 6;  
  private static final int LINK_URL_         = 7;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private DataAccessContext dacx_;
  private GenomeInstance currGI_;
  private Group currGroup_;
  private Note currNote_;
  private NodeInstance currNode_;
  private LinkageInstance currLink_;
  private int charTarget_;
   
  private Set<String> genomeKeys_;
  private Set<String> geneKeys_;
  private Set<String> nodeKeys_;
  private Set<String> linkKeys_;
  private Set<String> groupKeys_;
  private Set<String> noteKeys_;  
  private Set<String> groupMemberKeys_;
  
  private String descripKey_;
  private String longNameKey_;
 
  private String nodeDescripKey_;
  private String nodeUrlKeyword_;
 
  private String linkDescripKey_;
  private String linkUrlKeyword_;
  
  private StringBuffer descBuf_;
  private StringBuffer urlBuf_;
  private NetworkOverlay.NetOverlayWorker now_;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for a genome instance factory
  */

  public GenomeInstanceFactory() {
    super(new FactoryWhiteboard());
    dacx_ = null;
    genomeKeys_ = GenomeInstance.keywordsOfInterest();
    geneKeys_ = GeneInstance.keywordsOfInterest();
    nodeKeys_ = NodeInstance.keywordsOfInterest();
    linkKeys_ = LinkageInstance.keywordsOfInterest();
    groupKeys_ = Group.keywordsOfInterest(false);
    noteKeys_ = Note.keywordsOfInterest(false);    
    groupMemberKeys_ = GroupMember.keywordsOfInterest();
    descripKey_ = GenomeInstance.descriptionKeyword();
    longNameKey_ = GenomeInstance.longNameKeyword();
    linkDescripKey_ = LinkageInstance.descriptionKeyword();
    linkUrlKeyword_ = LinkageInstance.urlKeyword();
    nodeDescripKey_ = NodeInstance.descriptionKeyword();
    nodeUrlKeyword_ = NodeInstance.urlKeyword();
   
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    whiteboard.genomeType = NetworkOverlay.GENOME_INSTANCE;
    now_ = new NetworkOverlay.NetOverlayWorker(whiteboard);
    installWorker(now_, new MyGlue());
    myKeys_.addAll(genomeKeys_);
    
    descBuf_ = new StringBuffer();
    urlBuf_ = new StringBuffer();    
  }

  ////////////////////////////////////////////////////////////////////////////
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
      if (!elemName.equals(nodeUrlKeyword_)) {
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
        currGI_.appendLongName(nextString);
        break;
      case DESCRIPTION_:
        currGI_.appendDescription(nextString);
        break;
      case NOTE_:
        currNote_.appendToNote(nextString);
        break;
      case LINK_DESCRIPTION_:
      case NODE_DESCRIPTION_:
        descBuf_.append(nextString);
        break;
      case LINK_URL_:
      case NODE_URL_:
        urlBuf_.append(nextString);
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
      GenomeInstance gi = GenomeInstance.buildFromXML(dacx_, elemName, attrs);
      if (gi != null) {        
        dacx_.getGenomeSource().addGenomeInstance(gi.getID(), gi);
        currGI_ = gi;
        ((FactoryWhiteboard)sharedWhiteboard_).genome = gi;
        NavTree tree = dacx_.getGenomeSource().getModelHierarchy();
        if (tree.needLegacyGlue()) {
          GenomeInstance parent = gi.getVfgParent();
          NavTree.Kids nodeType = (parent == null) ? NavTree.Kids.ROOT_INSTANCE : NavTree.Kids.STATIC_CHILD_INSTANCE;
          TreeNode parNode = tree.nodeForModel(dacx_.getGenomeSource().getRootDBGenome().getID());
          tree.addNode(nodeType, gi.getName(), parNode, new NavTree.ModelID(gi.getID()), null, null, dacx_);
        }
        return (gi);
      }
      return (null);
    } else if (descripKey_.equals(elemName)) {
      charTarget_ = DESCRIPTION_;
    } else if (longNameKey_.equals(elemName)) {
      charTarget_ = LONG_NAME_;            
    } else if (geneKeys_.contains(elemName)) {
      currNode_ = GeneInstance.buildFromXML(dacx_, (DBGenome)dacx_.getGenomeSource().getRootDBGenome(), attrs);
      currGI_.addGene((GeneInstance)currNode_);
    } else if (nodeKeys_.contains(elemName)) {
      currNode_ = NodeInstance.buildFromXML(dacx_, dacx_.getGenomeSource().getRootDBGenome(), elemName, attrs);
      currGI_.addNode(currNode_);
    } else if (linkKeys_.contains(elemName)) {
      currLink_ = LinkageInstance.buildFromXML(dacx_, (DBGenome)dacx_.getGenomeSource().getRootDBGenome(), attrs);
      currGI_.addLinkage(currLink_); 
    } else if (linkDescripKey_.equals(elemName)) {
      charTarget_ = LINK_DESCRIPTION_;
      descBuf_.setLength(0);
    } else if (linkUrlKeyword_.equals(elemName)) {
      charTarget_ = LINK_URL_;
      urlBuf_.setLength(0);
    } else if (nodeDescripKey_.equals(elemName)) {
      charTarget_ = NODE_DESCRIPTION_;
      descBuf_.setLength(0);
    } else if (nodeUrlKeyword_.equals(elemName)) {
      charTarget_ = NODE_URL_;
      urlBuf_.setLength(0);          
    } else if (groupKeys_.contains(elemName)) {
      Group newGroup = Group.buildFromXML(dacx_.getRMan(), elemName, attrs);
      if (newGroup != null) {        
        currGroup_ = newGroup;
        currGI_.addGroup(currGroup_);
      }
    } else if (noteKeys_.contains(elemName)) {
      Note newNote = Note.buildFromXML(elemName, attrs);
      if (newNote != null) {
        charTarget_ = NOTE_;
        currNote_ = newNote;
        currGI_.addNote(currNote_);
      }      
    } else if (groupMemberKeys_.contains(elemName)) {
      GroupMember newMember = GroupMember.buildFromXML(attrs);
      currGroup_.addMember(newMember, currGI_.getID());
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
