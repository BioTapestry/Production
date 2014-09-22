/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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
import java.awt.Color;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
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
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public LayoutFactory(BTState appState) {
    super(new FactoryWhiteboard());
    appState_ = appState;
    layoutKeys_ = Layout.keywordsOfInterest();
    gPropKey_ = GroupProperties.keywordOfInterest();
    ntPropKey_ = NoteProperties.keywordOfInterest();
    dataLocKey_ = Layout.getDataLocKeyword();
    metadataKey_ = LayoutMetadata.keywordOfInterest();
    layoutDeriveKey_ = LayoutDerivation.keywordOfInterest();    
    layoutDataKeys_ = LayoutDataSource.keywordsOfInterest();
    layoutDataNodeIDKey_ = LayoutDataSource.nodeIDKeyword(); 
    
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    installWorker(new NetOverlayProperties.NetOverlayPropertiesWorker(appState_, whiteboard), new MyGlue());
    // Note that BusProperties get glued in as terminal bus drops show up!
    installWorker(new BusProperties.BusPropertiesWorker(whiteboard), null);  
    installWorker(new NodeProperties.NodePropertiesWorker(appState_.getDB(), whiteboard), new MyNodePropsGlue());      
    installWorker(new SingleLinkProperties.SingleLinkPropertiesWorker(whiteboard), new MySinglePropsGlue());    
    

    myKeys_.addAll(layoutKeys_);
    myKeys_.add("color");
 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {

    if ((attrs == null) || (elemName == null)) {
      return (null);
    }
    
    if (elemName.equals("color")) {  // FIX ME!  Refactor this out
      createColor(attrs);
      return (null);
    }
   
    if (layoutKeys_.contains(elemName)) {
      Layout newLo = Layout.buildFromXML(appState_, elemName, attrs);
      if (newLo != null) {
        layoutKey_ = newLo.getID();
        Database db = appState_.getDB();
        db.addLayout(layoutKey_, newLo);
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.layout = newLo;
        board.genome = db.getGenome(newLo.getTarget()); 
      }
      return (null);
    }
        
    Layout layout = appState_.getDB().getLayout(layoutKey_);
      
    if (elemName.equals(gPropKey_)) {
      GroupProperties gprop = GroupProperties.buildFromXML(appState_, layout, attrs);
      layout.setGroupProperties(gprop.getReference(), gprop);

    } else if (elemName.equals(ntPropKey_)) {
      NoteProperties ntprop = NoteProperties.buildFromXML(appState_, layout, attrs);
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

  /***************************************************************************
  **
  ** Handle color creation.  FIX ME!  Belongs in a DBFactory or something!
  **
  */
  
  private void createColor(Attributes attrs) throws IOException {
    String color = null;
    String name = null;
    String r = null;
    String g = null;
    String b = null;    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("color")) {
          color = val;
        } else if (key.equals("name")) {
          name = val;          
        } else if (key.equals("r")) {
          r = val;
        } else if (key.equals("g")) {
          g = val;
        } else if (key.equals("b")) {
          b = val;
        }        
      }
    }
    
    if ((color == null) || (r == null) || (g == null) || (b == null)) {
      throw new IOException();
    }
    color = color.trim();
    if (color.equals("")) {
      throw new IOException();
    }
    
    if (name == null) {
      name = color;
    }
    
    int red = -1;
    int green = -1;
    int blue = -1;
    try {
      red = Integer.parseInt(r);
      green = Integer.parseInt(g);    
      blue = Integer.parseInt(b); 
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    if ((red < 0) || (green < 0) || (blue < 0) ||
        (red > 255) || (green > 255) || (blue > 255)) {
      throw new IOException();
    }
    
    appState_.getDB().setColor(color, new NamedColor(color, new Color(red, green, blue), name));
    return;
  }
  
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

