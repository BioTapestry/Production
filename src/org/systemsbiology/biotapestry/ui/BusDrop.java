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

package org.systemsbiology.biotapestry.ui;

import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** Describes terminal connections to link buses
*/

public class BusDrop extends LinkBusDrop {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public BusDrop(BusDrop other) {
    super(other);
  }
   
  /***************************************************************************
  **
  ** Constructor
  */

  public BusDrop(Layout layout, Genome genome, String startID, Attributes attrs) throws IOException {   
    super(layout, genome, startID, attrs); 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** To string
  */
  
  public String toString() {
    return ("BusDrop: " + super.toString());
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE VISIBLE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Package visible:
  */

  BusDrop(String targetRef, String ourConnection, int dropType, int connectionEnd) {
    super(targetRef, ourConnection, dropType, connectionEnd);
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class BusDropWorker extends AbstractFactoryClient {
     
    public BusDropWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(XML_TAG);
      installWorker(new PerLinkDrawStyle.PerLinkDrawStyleWorker(whiteboard), new MyPerLinkGlue());
      installWorker(new SuggestedDrawStyle.SuggestedDrawStyleWorker(whiteboard), new MyStyleGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(XML_TAG)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.busDrop = buildFromXML(elemName, attrs);
        retval = board.busDrop;
      }
      return (retval);     
    }
    
    private BusDrop buildFromXML(String elemName, Attributes attrs) throws IOException { 
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;      
      BusProperties bus = board.busProps;
      String startID = bus.getSourceTag();
      // FIX ME: Do this like BusProperties
      return (new BusDrop(board.layout, board.genome, startID, attrs));    
    }
  }
  
  public static class MyPerLinkGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      BusDrop drop = board.busDrop;
      PerLinkDrawStyle plds = board.perLinkSty;
      drop.setDrawStyleForLink(plds);
      return (null);
    }
  } 
  
  public static class MyStyleGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      BusDrop drop = board.busDrop;
      SuggestedDrawStyle suggSty = board.suggSty;
      drop.setDrawStyleForDrop(suggSty);
      return (null);
    }
  } 
}
