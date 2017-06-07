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

package org.systemsbiology.biotapestry.ui;

import java.util.Set;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.LayoutSource;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This handles layout creation
*/

public class LayoutFactory extends AbstractFactoryClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String layoutKey_;
  private LayoutMetadata currLmd_;
  private LayoutDerivation currLd_;
  private LayoutDataSource currLds_;
  
  private Set<String> layoutKeys_;
  private String gPropKey_;
  private String ntPropKey_;
  private String dataLocKey_;
  private String metadataKey_;
  private String layoutDeriveKey_;
  private Set<String> layoutDataKeys_; 
  private String layoutDataNodeIDKey_;
  private DataAccessContext dacx_;
  private NodeProperties.NodePropertiesWorker npw_;
  private NetOverlayProperties.NetOverlayPropertiesWorker nopw_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LayoutFactory() {
    super(new FactoryWhiteboard());
    dacx_ = null;
    layoutKeys_ = Layout.keywordsOfInterest();
    gPropKey_ = GroupProperties.keywordOfInterest();
    ntPropKey_ = NoteProperties.keywordOfInterest();
    dataLocKey_ = Layout.getDataLocKeyword();
    metadataKey_ = LayoutMetadata.keywordOfInterest();
    layoutDeriveKey_ = LayoutDerivation.keywordOfInterest();    
    layoutDataKeys_ = LayoutDataSource.keywordsOfInterest();
    layoutDataNodeIDKey_ = LayoutDataSource.nodeIDKeyword(); 
    
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    nopw_ = new NetOverlayProperties.NetOverlayPropertiesWorker(whiteboard);
    installWorker(nopw_, new MyGlue());
    // Note that BusProperties get glued in as terminal bus drops show up!
    installWorker(new BusProperties.BusPropertiesWorker(whiteboard), null);
    npw_ = new NodeProperties.NodePropertiesWorker(whiteboard);
    installWorker(npw_, new MyNodePropsGlue());      
    installWorker(new SingleLinkProperties.SingleLinkPropertiesWorker(whiteboard), new MySinglePropsGlue());    
    
    myKeys_.addAll(layoutKeys_);
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
    npw_.installContext(dacx);
    nopw_.installContext(dacx);
    return;
  }

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {

    if ((attrs == null) || (elemName == null)) {
      return (null);
    }
 
    if (layoutKeys_.contains(elemName)) {
      Layout newLo = Layout.buildFromXML(elemName, attrs);
      if (newLo != null) {
        layoutKey_ = newLo.getID();
        LayoutSource db = dacx_.getLayoutSource();
        db.addLayout(layoutKey_, newLo);
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.layout = newLo;
        board.genome = dacx_.getGenomeSource().getGenome(newLo.getTarget()); 
      }
      return (null);
    }
        
    Layout layout = dacx_.getLayoutSource().getLayout(layoutKey_);
      
    if (elemName.equals(gPropKey_)) {
      GroupProperties gprop = GroupProperties.buildFromXML(attrs);
      layout.setGroupProperties(gprop.getReference(), gprop);

    } else if (elemName.equals(ntPropKey_)) {
      NoteProperties ntprop = NoteProperties.buildFromXML(dacx_, attrs);
      layout.setNoteProperties(ntprop.getReference(), ntprop); 
      
    } else if (elemName.equals(dataLocKey_)) {
      Layout.dataPropFromXML(layout, attrs);
      
    } else if (elemName.equals(metadataKey_)) {
      LayoutMetadata lmd = LayoutMetadata.buildFromXML(attrs);
      if (lmd != null) {
        currLmd_ = lmd;
        layout.setMetadata(currLmd_);
      }
    } else if (elemName.equals(layoutDeriveKey_)) {
      LayoutDerivation ld = LayoutDerivation.buildFromXML(attrs);
      if (ld != null) {
        currLd_ = ld;
        currLmd_.setDerivation(currLd_);
      }
    } else if (layoutDataKeys_.contains(elemName)) {
      LayoutDataSource lds = LayoutDataSource.buildFromXML(elemName, attrs);
      if (lds != null) {
        currLds_ = lds;
        currLd_.addDirective(lds); 
      }
    } else if (elemName.equals(layoutDataNodeIDKey_)) {
      String nodeID = LayoutDataSource.extractNodeID(elemName, attrs);
      currLds_.addNode(nodeID);
    }
    return (null);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      NetOverlayProperties nop = board.netOvrProps;
      layout.setNetOverlayProperties(nop.getReference(), nop);
      return (null);
    }
  }  
  
  public static class MyNodePropsGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      NodeProperties nprop = board.nodeProps;
      layout.setNodeProperties(nprop.getReference(), nprop);
      return (null);
    }
  }

  public static class MySinglePropsGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Layout layout = board.layout;
      BusProperties bprop = board.busProps;
      String targetTag = bprop.getSingleLinkage();
      layout.setLinkProperties(targetTag, bprop);
      return (null);
    }
  }  
}

