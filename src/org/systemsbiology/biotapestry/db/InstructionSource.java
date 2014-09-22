/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import java.util.Iterator;
import java.util.List;

import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSet;

public interface InstructionSource {
  
  /***************************************************************************
  **
  ** Add a build instruction
  */
  
  public void addBuildInstruction(BuildInstruction bi);
  
  /***************************************************************************
  **
  ** Get the next instruction label
  */
  
  public String getNextInstructionLabel();
  
  /***************************************************************************
  **
  ** Get an iterator over the build instructions
  */
  
  public Iterator<BuildInstruction> getBuildInstructions();
 
  
  /***************************************************************************
  **
  ** Answer if we have build instructions
  */
  
  public boolean haveBuildInstructions();
 
  /***************************************************************************
  **
  ** Set the build instruction set
  */
  
  public DatabaseChange setBuildInstructions(List<BuildInstruction> inst); 
  
  /***************************************************************************
  **
  ** Get a build instruction
  */
  
  public BuildInstruction getBuildInstruction(String idTag);
   
  /***************************************************************************
  **
  ** Drop the build instruction set
  */
  
  public DatabaseChange dropBuildInstructions();
  
  /***************************************************************************
  **
  ** Add an instance Instruction Set
  */
  
  public void addInstanceInstructionSet(String id, InstanceInstructionSet iis);

  /***************************************************************************
  **
  ** Get the given instance instruction set
  */
  
  public InstanceInstructionSet getInstanceInstructionSet(String key);
 
  /***************************************************************************
  **
  ** Remove the instance instruction set
  */
  
  public DatabaseChange removeInstanceInstructionSet(String key);
 
  /***************************************************************************
  **
  ** Set the instance instruction set
  */
  
  public DatabaseChange setInstanceInstructionSet(String key, InstanceInstructionSet iis);
}
