/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.nav;

import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** This is a stop on a user defined tree path
*/

public class UserTreePathStop implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String genomeID_;
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
  ** Copy Constructor
  */

  public UserTreePathStop(UserTreePathStop other) {  
    this.genomeID_ = other.genomeID_;
    this.ovrKey_ = other.ovrKey_;
    this.modKeys_ = new TaggedSet(other.modKeys_);
    this.revKeys_ = new TaggedSet(other.revKeys_);    
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public UserTreePathStop(String genomeID, String ovrKey, TaggedSet modKeys, TaggedSet revKeys) {  
    genomeID_ = genomeID;
    ovrKey_ = ovrKey;
    modKeys_ = (modKeys == null) ? new TaggedSet() : new TaggedSet(modKeys); 
    revKeys_ = (revKeys == null) ? new TaggedSet() : new TaggedSet(revKeys); 
    
    //
    // We want to guarantee that the reveal keys that we enshrine here are
    // a subset of the mod keys.  Note that this ensures that reasoning about
    // our modules via the mod keys will carry over automatically to reveal
    // key questions.
    //
    revKeys_.set.retainAll(modKeys_.set);
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

  public UserTreePathStop clone() {
    try {
      UserTreePathStop retval = (UserTreePathStop)super.clone();
      retval.modKeys_ = (TaggedSet)this.modKeys_.clone(); 
      retval.revKeys_ = (TaggedSet)this.revKeys_.clone(); 
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();
    }
  }    
  
  /***************************************************************************
  **
  ** Equals
  */

  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof UserTreePathStop)) {
      return (false);
    }
    UserTreePathStop otherPS = (UserTreePathStop)other;

    if (!this.genomeID_.equals(otherPS.genomeID_)) {
      return (false);
    }
    
    if (this.ovrKey_ == null) {
      return (otherPS.ovrKey_ == null); 
    }
   
    if (!this.modKeys_.equals(otherPS.modKeys_)) {
      return (false);
    }
    
    return (this.revKeys_.equals(otherPS.revKeys_));
  }  
  
  /***************************************************************************
  **
  ** Get the genome ID
  **
  */
  
  public String getGenomeID() {
    return (genomeID_);
  }
  
  /***************************************************************************
  **
  ** Set the genome ID
  **
  */
  
  public void setGenomeID(String id) {
    genomeID_ = id;
    return;
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
  ** Get modules
  */
  
  public TaggedSet getRevealed() {
    return (revKeys_);
  }  
  
  /***************************************************************************
  **
  ** Drop the given module key from the stop
  **
  */
  
  public void dropModKey(String dropModKey) {
    modKeys_.set.remove(dropModKey);
    // Keep the two in sync:
    revKeys_.set.remove(dropModKey);
    return;
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
  ** Write the path stop to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {

    ind.indent();     
    out.print("<pathStop id=\""); 
    out.print(genomeID_);
    if (ovrKey_ != null) {
      out.print("\" ovrKey=\"");
      out.print(ovrKey_);
    }       
    if ((ovrKey_ == null) || (modKeys_ == null)) {
      out.println("\" />");          
      return;
    }    
    out.println("\" >");    
    ind.up();
    
    if (modKeys_ != null) {
      ind.indent();
      if (modKeys_.set.isEmpty()) {
        out.println("<stopMods/>");
      } else {
        out.println("<stopMods>");
        ind.up();
        modKeys_.writeXML(out, ind);
        ind.down().indent();
        out.println("</stopMods>");
      }
    }
    
    if (revKeys_ != null) {
      ind.indent();
      if (revKeys_.set.isEmpty()) {
        out.println("<stopViz/>");
      } else {
        out.println("<stopViz>");
        ind.up();
        revKeys_.writeXML(out, ind);
        ind.down().indent();
        out.println("</stopViz>");      
      }
    }
   
    ind.down().indent();
    out.println("</pathStop>");
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
      
  public static class UserTreePathStopWorker extends AbstractFactoryClient {
    
    public UserTreePathStopWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pathStop");
      installWorker(new ModWorker(whiteboard), null);
      installWorker(new VizWorker(whiteboard), null);
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pathStop")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.userTreePathStop = buildFromXML(elemName, attrs);
        retval = board.userTreePathStop;
      }
      return (retval);     
    }

    private UserTreePathStop buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String genomeID = AttributeExtractor.extractAttribute(elemName, attrs, "pathStop", "id", true);
      String ovrKey = AttributeExtractor.extractAttribute(elemName, attrs, "pathStop", "ovrKey", false);      
      return (new UserTreePathStop(genomeID, ovrKey, null, null));
    }
  }
  
  public static class ModWorker extends AbstractFactoryClient {
    
    public ModWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("stopMods");
      installWorker(new TaggedSet.TaggedSetWorker(whiteboard), new MyTSMGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      return (null);     
    }
  }
  
  public static class VizWorker extends AbstractFactoryClient {
    
    public VizWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("stopViz");
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
      UserTreePathStop stop = board.userTreePathStop;
      TaggedSet tSet = board.currentTaggedSet;
      stop.setModKeys(tSet);
      return (null);
    }
  }
  
  public static class MyTSVGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      UserTreePathStop stop = board.userTreePathStop;
      TaggedSet tSet = board.currentTaggedSet;
      stop.setRevKeys(tSet);
      return (null);
    }
  }     
}
