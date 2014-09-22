/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;


/****************************************************************************
**
** Does I/O tasks for a HashMap to lists of strings
*/

public class MapIOSupport {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private String mapTag_;
  private String keyTag_;
  private String keyAttrib_;
  private String elemTag_;
  private String elemAttrib_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public MapIOSupport(String mapTag, String keyTag, String keyAttrib, String elemTag, String elemAttrib) {
    mapTag_ = mapTag;
    keyTag_ = keyTag;
    elemTag_ = elemTag;
    keyAttrib_ = keyAttrib;
    elemAttrib_ = elemAttrib;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Write a map out to XML
  **
  */
  
  public void dumpMapToXML(PrintWriter out, Indenter ind, Map map) { 
    if (!map.isEmpty()) {
      ind.indent();
      out.print("<");
      out.print(mapTag_);
      out.println(">");    
      ind.up();
       
      //
      // Fix the output order:
      //
      
      TreeSet sorted = new TreeSet(map.keySet());
      
      Iterator dpkit = sorted.iterator();
      while (dpkit.hasNext()) {
        String dpkey = (String)dpkit.next();
        ArrayList notes = (ArrayList)map.get(dpkey);
        ind.indent();
        out.print("<");
        out.print(keyTag_);  
        out.print(" ");
        out.print(keyAttrib_);
        out.print("=\"");
        out.print(dpkey);
        out.println("\">");
        ind.up();
        int numN = notes.size();
        for (int i = 0; i < numN; i++) {
          String noteID = (String)notes.get(i);
          ind.indent();
          out.print("<");
          out.print(elemTag_);
          out.print(" ");
          out.print(elemAttrib_);
          out.print("=\"");
          out.print(noteID);
          out.println("\"/>");
        }
        ind.down().indent();
        out.print("</");
        out.print(keyTag_);
        out.println(">");  
      }
      ind.down().indent();
      out.print("</");
      out.print(mapTag_);
      out.println(">");    
    }
    return;
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public class MapWorker extends AbstractFactoryClient {
 
    public MapWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(mapTag_);
      installWorker(new MapKeyWorker(whiteboard), new MyKeyGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(mapTag_)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currentMap = buildFromXML(elemName, attrs);
        retval = board.currentMap;
      }
      return (retval);     
    }  
    
    private Map buildFromXML(String elemName, Attributes attrs) throws IOException {  
      Map retval = new HashMap();
      return (retval);
    }
  }
  
  public static class MyKeyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Map cMap = board.currentMap;
      ArrayList targList = new ArrayList();
      cMap.put(board.currentMapKey, targList);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public class MapKeyWorker extends AbstractFactoryClient {
 
    public MapKeyWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(keyTag_);
      installWorker(new MapElemWorker(whiteboard), new MyElemGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(keyTag_)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currentMapKey = buildFromXML(elemName, attrs);
        retval = board.currentMapKey;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String key = AttributeExtractor.extractAttribute(elemName, attrs, keyTag_, keyAttrib_, true);
      return (key);
    }
  }
  
  public static class MyElemGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      Map<String, List<String>> cMap = board.currentMap;
      ArrayList targList = (ArrayList)cMap.get(board.currentMapKey);
      targList.add(board.currentMapTarget);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public class MapElemWorker extends AbstractFactoryClient {
      
    public MapElemWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(elemTag_);
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(elemTag_)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.currentMapTarget = buildFromXML(elemName, attrs);
        retval = board.currentMapTarget;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String name = AttributeExtractor.extractAttribute(elemName, attrs, elemTag_, elemAttrib_, true);
      return (name);
    }
  }
}
