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
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Used to define initial view; also gets used for user path injection
*/

public class StartupView implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String modelId_;
  private String nodeId_;
  private NavTree.KidSuperType nodeType_;
  private String regionID_;
  private String ovrKey_;
  private TaggedSet modKeys_;
  private TaggedSet revKeys_;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public StartupView() {
    modelId_ = null;
    nodeId_ = null;
    nodeType_ = null;
    ovrKey_ = null;
    regionID_ = null;
    modKeys_ = new TaggedSet();
    revKeys_ = new TaggedSet();
  }
  
  
  /***************************************************************************
  **
  ** Constructor
  */
  
  public StartupView(String key, String ovrKey, TaggedSet modKeys, TaggedSet revKeys, String regionID, NavTree.KidSuperType nodeType) {
	  this(key, ovrKey, modKeys, revKeys, regionID, nodeType, null);
  }   
  
  
 public StartupView(String key, String ovrKey, TaggedSet modKeys, TaggedSet revKeys, String regionID,
                    NavTree.KidSuperType nodeType, String nodeId) { 
	  modelId_ = key;
	  nodeId_ = nodeId;
	  nodeType_ = nodeType;
    ovrKey_ = ovrKey;
    regionID_ = regionID;
    modKeys_ = (modKeys == null) ? new TaggedSet() : modKeys;
    revKeys_ = (revKeys == null) ? new TaggedSet() : revKeys;    
  }      
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public StartupView clone() {
    try {
      StartupView retval = (StartupView)super.clone();
      retval.modKeys_ = this.modKeys_.clone();
      retval.revKeys_ = this.revKeys_.clone();      
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
 
  /***************************************************************************
  **
  ** Standard equals
  */

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof StartupView)) {
      return (false);
    }
    StartupView otherSV = (StartupView)other;

    if (this.nodeType_ == null) {
    	if (otherSV.nodeType_ != null) {
    		return (false);
    	}
    } else if (!this.nodeType_.equals(otherSV.nodeType_)) {
    	return (false);
    }

    if (this.regionID_ == null) {
      if (otherSV.regionID_ != null) {
        return (false);
      }
    } else if (!this.regionID_.equals(otherSV.regionID_)) {
      return (false);
    }  
    
    if (this.nodeId_ == null) {
        if (otherSV.nodeId_ != null) {
          return (false);
        }
    } else if (!this.nodeId_.equals(otherSV.nodeId_)) {
      return (false);
    }    
    
    if (this.modelId_ == null) {
      if (otherSV.modelId_ != null) {
        return (false);
      }
    } else if (!this.modelId_.equals(otherSV.modelId_)) {
      return (false);
    }

    if (this.ovrKey_ == null) {
      if (otherSV.ovrKey_ != null) {
        return (false);
      }
    } else if (!this.ovrKey_.equals(otherSV.ovrKey_)) {
      return (false);
    }    
    
    if (this.modKeys_ == null) {
      if (otherSV.modKeys_ != null) {
        return (false);
      }
    } else if (!this.modKeys_.equals(otherSV.modKeys_)) {
      return (false);
    }
    
    if (this.revKeys_ == null) {
      if (otherSV.revKeys_ != null) {
        return (false);
      }
    } else if (!this.revKeys_.equals(otherSV.revKeys_)) {
      return (false);
    }    
   
    return (true);
  }  
  

  /***************************************************************************
  **
  ** Return a startup view with a dropped model
  */
  
  public StartupView dropModel() {
    return (new StartupView());  // nothing but the model changes at this time
  }   
  
  /***************************************************************************
  **
  ** Get model
  */
  
  public String getModel() {
    return (modelId_);
  } 
  
  /***************************************************************************
  **
  ** Get overlay key
  */
  
  public String getOverlay() {
    return (ovrKey_);
  } 
  
  /***************************************************************************
  **
  ** Get modules
  */
  
  public TaggedSet getModules() {
    return (modKeys_);
  }  
  
  /***************************************************************************
  **
  ** Get revealed modules
  */
  
  public TaggedSet getRevealedModules() {
    return (revKeys_);
  } 
   
  /***************************************************************************
  **
  ** Get node type
  */
   
  public NavTree.KidSuperType getNodeType() {
	  return (nodeType_);
  }
  
  /***************************************************************************
  **
  ** Get nodeID for group nodes
  */
   
  public String getNodeId() {
	  return (nodeId_);
  }
  
  /***************************************************************************
  **
  ** Get regionID
  */
   
  public String getRegionId() {
    return (regionID_);
  }
  
  /***************************************************************************
  **
  ** Answers if we care about the overlay key
  */
  
  public boolean referencesOverlay(String modelKey, String overlayKey) {
    if ((modelId_ == null) || !modelId_.equals(modelKey)) {
      return (false);
    }
    if (ovrKey_ == null) {
      return (false);
    }
    return (ovrKey_.equals(overlayKey));
  } 
 
  /***************************************************************************
  **
  ** Answers if we care about the module key
  */
  
  public boolean referencesModule(String modelKey, String overlayKey, String modKey) {
    if (!referencesOverlay(modelKey, overlayKey)) {
      return (false);
    }
    return (modKeys_.set.contains(modKey));
  }   
  
  /***************************************************************************
  **
  ** Add the module key
  */
  
  public void addModule(String modelKey, String overlayKey, String modKey) {
    if (!referencesOverlay(modelKey, overlayKey)) {
      throw new IllegalArgumentException();
    }
    modKeys_.set.add(modKey);
    return;
  }   
  
  /***************************************************************************
  **
  ** Delete the module key.  Returns true if it then ends up empty!
  */
  
  public boolean deleteModule(String modelKey, String overlayKey, String modKey) {
    if (!referencesOverlay(modelKey, overlayKey)) {
      throw new IllegalArgumentException();
    }
    modKeys_.set.remove(modKey);
    return (modKeys_.set.isEmpty());
  }  
  
  /***************************************************************************
  **
  ** Add the module keys.  Use for I/O; elements will be added to the given set
  **
  */
  
  public void setModKeys(TaggedSet modKeys) {
    modKeys_ = (modKeys == null) ? new TaggedSet() : modKeys;
    return;
  } 
  
  /***************************************************************************
  **
  ** Add the revealed keys.  Use for I/O; elements will be added to the given set
  **
  */
  
  public void setRevKeys(TaggedSet revKeys) {
    revKeys_ = (revKeys == null) ? new TaggedSet() : revKeys;
    return;
  }   
  
  /***************************************************************************
  **
  ** Write the startup view definition to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {

    ind.indent();     
    out.print("<startupView");
    if ((modelId_ == null) && (nodeId_ == null)) {
      out.println("/>");
      return;
    }
    if (modelId_ != null) {
      out.print(" model=\"");
      out.print(modelId_);
    } else if (nodeId_ != null) {
      out.print(" node=\"");
      out.print(nodeId_);
    }
    
    if (ovrKey_ != null) {
      out.print("\" ovrKey=\"");
      out.print(ovrKey_);
    }  
    
    if (regionID_ != null) {
      out.print("\" region=\"");
      out.print(regionID_);
    }
    
    if (nodeType_ != null) {
      out.print("\" nodeType=\"");
      out.print(nodeType_.getTag());
    }

    if ((modKeys_ == null) || modKeys_.set.isEmpty()) {
      out.println("\" />");          
      return;
    }
    
    out.println("\" >");    
    ind.up(); 
    
    if (modKeys_ != null) {
      ind.indent();
      if (modKeys_.set.isEmpty()) {
        out.println("<startMods/>");
      } else {
        out.println("<startMods>");
        ind.up();
        modKeys_.writeXML(out, ind);
        ind.down().indent();
        out.println("</startMods>");
      }
    }
    
    if (revKeys_ != null) {
      ind.indent();
      if (revKeys_.set.isEmpty()) {
        out.println("<startViz/>");
      } else {
        out.println("<startViz>");
        ind.up();
        revKeys_.writeXML(out, ind);
        ind.down().indent();
        out.println("</startViz>");      
      }
    }
      
    ind.down().indent();
    out.println("</startupView>");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ///////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class StartupViewWorker extends AbstractFactoryClient {
    
    public StartupViewWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("startupView");
      installWorker(new ModWorker(whiteboard), null);
      installWorker(new VizWorker(whiteboard), null);
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("startupView")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.startupView = buildFromXML(elemName, attrs);
        retval = board.startupView;
      }
      return (retval);     
    }
    
    private StartupView buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String modelID = AttributeExtractor.extractAttribute(elemName, attrs, "startupView", "model", false);
      String nodeID = AttributeExtractor.extractAttribute(elemName, attrs, "startupView", "node", false);
      String nodeType = AttributeExtractor.extractAttribute(elemName, attrs, "startupView", "nodeType", false);
      String ovrKey = AttributeExtractor.extractAttribute(elemName, attrs, "startupView", "ovrKey", false); 
      String regionID = AttributeExtractor.extractAttribute(elemName, attrs, "startupView", "region", false);
      NavTree.KidSuperType kst = null;
      if ((nodeType != null) && (nodeType.equals(NavTree.KidSuperType.GROUP.getTag()))) {
        kst = NavTree.KidSuperType.GROUP;   
      } else {
        kst = NavTree.KidSuperType.MODEL;
      }
      return (new StartupView(modelID, ovrKey, null, null, regionID, kst, nodeID));
    }
  }
  
 public static class ModWorker extends AbstractFactoryClient {  
    public ModWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("startMods");
      installWorker(new TaggedSet.TaggedSetWorker(whiteboard), new MyTSMGlue());
    } 
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      return (null);     
    }
  }
  
  public static class VizWorker extends AbstractFactoryClient {   
    public VizWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("startViz");
      installWorker(new TaggedSet.TaggedSetWorker(whiteboard), new MyTSVGlue());
    }  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      return (null);     
    }
  }  

  public static class MyTSMGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      StartupView startView = board.startupView;
      TaggedSet tSet = board.currentTaggedSet;
      startView.setModKeys(tSet);
      return (null);
    }
  }
  
  public static class MyTSVGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      StartupView startView = board.startupView;
      TaggedSet tSet = board.currentTaggedSet;
      startView.setRevKeys(tSet);
      return (null);
    }
  }  
}
