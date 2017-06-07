/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;

/****************************************************************************
**
** No longer used; this stub is now just used to recover legacy IO
*/

public class SingleLinkProperties {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class SingleLinkPropertiesWorker extends AbstractFactoryClient {
  
    public SingleLinkPropertiesWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("lprop");
      installWorker(new LinkSegment.LinkSegmentWorker(whiteboard), new MySegmentGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("lprop")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.busProps = buildFromXML(elemName, attrs);
        retval = board.busProps;
      }
      return (retval);     
    }
    
    private BusProperties buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "id", true);
      String color = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "color", true);
      String line = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "line", true);
      String txtX = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "labelX", false);
      String txtY = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "labelY", false);
      String txtDir = AttributeExtractor.extractAttribute(elemName, attrs, "lprop", "labelDir", false);      

      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;      
      Linkage link = board.genome.getLinkage(id);
      if (link == null) {
        System.err.println("Failed to get " + id + " from " + board.genome.getName());      
        throw new IOException();
      }    
      return (new BusProperties(board.genome, color, line, txtX, txtY, txtDir, link));           
    }
  }
  
  public static class MySegmentGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      BusProperties busProps = board.busProps;
      LinkSegment seg = board.linkSegment;
      // Handle legacy IO case:
      busProps.addSegmentInSeries(seg);
      return (null);
    }
  }
}
