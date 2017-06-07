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

package org.systemsbiology.biotapestry.modelBuild;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.systemsbiology.biotapestry.event.ChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.util.Indenter;

/***************************************************************************
**
** Collection of interfaces used for Model Builder 
*/ 

public interface ModelBuilder {

  ////////////////////////////////////////////////////////////////////////////
  //
  // Used to listen for changes
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public interface ChangeListener {
       
    /***************************************************************************
    **
    ** Notify listener of worksheet run change
    */
 
    public void modelBuildHasChanged(MBChangeEvent wrcev);
    
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // Used to signal changes in the interaction analysis
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public interface MBChangeEvent extends ChangeEvent {   
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Interaction analysis UI
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  public interface Dashboard {
  
    /***************************************************************************
    **
    ** Drop the UI
    */ 
    
    public void dropDashboard();
    
    /***************************************************************************
    **
    ** Raise the UI
    */ 
    
    public void pullToFront();
    
    /***************************************************************************
    **
    ** Show the UI
    */ 
    
    public void launch();
  
    /***************************************************************************
    **
    ** Set model requests
    */ 
    
    public void selectModelRequests(List<Request> requestList);
    
    /***************************************************************************
    **
    ** Queue model requests
    */ 
    
    public void queueModelRequests(List<Request> requestList);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // Database
  //
  ////////////////////////////////////////////////////////////////////////////     
  
  public interface Database {
  
    /***************************************************************************
    ** 
    ** Support for database clone
    */
      
    public Database deepCopy(); 
    
    /***************************************************************************
    ** 
    ** Drop everything
    */
  
    public void dropViaDACX();
    
    /***************************************************************************
    **
    ** Dump the database to the given file using XML
    */
    
    public void writeXML(PrintWriter out, Indenter ind);
    
    /***************************************************************************
    **
    ** Handle undo
    */
    
    public void handleWorksheetRunUndo(Undo newData,Undo oldData); 
  
    /***************************************************************************
    **
    ** Handle redo
    */
    
    public void handleWorksheetRunRedo(Undo newData, Undo oldData);
    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Handles reading XML input into Model Builder Plugin
  //
  ////////////////////////////////////////////////////////////////////////////       

  public interface DataLoader extends ParserClient {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Maps node IDs
  //
  ////////////////////////////////////////////////////////////////////////////       

  public interface IDMapper {
  
    /***************************************************************************
    **
    ** Maps node IDs
    */
    
    public String getIDForNode(String id);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // UI for tracking Link drawing operations
  //
  ////////////////////////////////////////////////////////////////////////////     

  public interface LinkDrawingTracker {
  
    /***************************************************************************
    **
    ** Drop the UI
    */ 
    
    public void dropTracker();
    
    /***************************************************************************
    **
    ** Show the UI
    */ 
    
    public void launch();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Listen for Link Changes
  //
  ////////////////////////////////////////////////////////////////////////////     

  public interface LinkMonitor {
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // A model request
  //
  ////////////////////////////////////////////////////////////////////////////     

  public interface Request {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Reques resolver
  //
  ////////////////////////////////////////////////////////////////////////////     

  public interface Resolver {
    public List<Request> resolve(Genome genome, Set<String> linksThru);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // Database Source
  //
  ////////////////////////////////////////////////////////////////////////////     

  /****************************************************************************
  **
  ** Interface for ModelBuilderDatabase Source
  */
  
  public interface Source {
  
    /***************************************************************************
    ** 
    ** Get the modelBuilderDatabase
    */
  
    public Database getMbDB();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Undo support
  //
  ////////////////////////////////////////////////////////////////////////////     

  public interface Undo {
  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // XML Builder Support
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public interface Whiteboard {
  }
 
}
