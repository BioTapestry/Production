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


package org.systemsbiology.biotapestry.nav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/****************************************************************************
**
** Cross-Platform Info for Overlay
*/

public class XPlatOverlayDef {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private ArrayList<ModuleDef> modules_;
  private String id_;
  private String name_;
  private boolean isOpaque_;
  private boolean isStartView_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatOverlayDef(String id, String name, boolean isOpaque, boolean isStartView) {
    modules_ = new ArrayList<ModuleDef>();
    id_ = id;
    name_ = name;
    isOpaque_ = isOpaque;
    isStartView_ = isStartView;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  **
  ** Define a module
  */
  
  public static class ModuleDef {
      
    private String id_;
    private String name_;
    private Boolean shownInit_;
    private Boolean revealInit_;
        
    /***************************************************************************
    **
    ** Constructor
    */

    public ModuleDef(String id, String name, Boolean shownInit, Boolean revealInit) {
      id_ = id;
      name_ = name;
      shownInit_ = shownInit;
      revealInit_ = revealInit;
    }
    
    /***************************************************************************
    **
    ** Get the ID
    */ 
     
    public String getID() {
      return (id_);
    }
    
    /***************************************************************************
    **
    ** Get the Name
    */ 
     
    public String getName() {
      return (name_);
    }
    
    /***************************************************************************
    **
    ** Get if shown on startup. May be null.
    */ 
     
    public Boolean getShownInit() {
      return (shownInit_);
    }
     
    /***************************************************************************
    **
    ** Get if revealed on startup. May be null.
    */ 
     
    public Boolean getRevealInit() {
      return (revealInit_);
    }

    /***************************************************************************
    **
    ** Standard toString
    */ 

    @Override
    public String toString() {
      return ("Module name = " + name_ + " id = " + id_ + " shownInit_ = " + shownInit_ + " revealInit_ = " + revealInit_);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Get the ID
  */ 
   
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Get the Name
  */ 
   
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Get is opaque state
  */ 
   
  public boolean getIsOpaque() {
    return (isOpaque_);
  }
  
  /***************************************************************************
  **
  ** Get is start view
  */ 
   
  public boolean getIsStartView() {
    return (isStartView_);
  }
  
  /***************************************************************************
  **
  ** Add a module
  */ 
   
  public void addAModule(ModuleDef md) {
    modules_.add(md);
    return;
  }

  /***************************************************************************
  **
  ** Get the modules
  */ 
   
  public List<ModuleDef> getModules() {
    if (modules_ == null) {
      return null;
    }
    return (Collections.unmodifiableList(modules_));
  }
  
  /***************************************************************************
  **
  ** Standard string
  */ 
   
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("XPlatOverlayDef: id = ");
    buf.append(id_);
    buf.append(" name = ");
    buf.append(name_);
    buf.append(" isOpaque = ");
    buf.append(isOpaque_); 
    buf.append(" isStartView = ");
    buf.append(isStartView_); 
    
    Iterator<ModuleDef> oit = modules_.iterator();
    while (oit.hasNext()) {
      buf.append("\n");
      buf.append(oit.next().toString());
    }
    return (buf.toString());
  }

}
