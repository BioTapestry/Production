/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Contains the information needed to find out what the current overlay state is
*/

public interface OverlayStateOracle  {

  /***************************************************************************
  **
  ** Get the current overlay key
  ** 
  */
  
  public String getCurrentOverlay();

 /***************************************************************************
  **
  ** Get the modules
  */ 
    
  public TaggedSet getCurrentNetModules();
  
 /***************************************************************************
  **
  ** Get the current settings
  */ 
    
  public NetModuleFree.CurrentSettings getCurrentOverlaySettings();  
  
  /***************************************************************************
  **
  ** Get the set of modules that are showing their contents
  */ 
    
  public TaggedSet getRevealedModules();    
  
  /***************************************************************************
  **
  ** Answer if module components are being displayed
  */ 
    
  public boolean showingModuleComponents();      
 
}
