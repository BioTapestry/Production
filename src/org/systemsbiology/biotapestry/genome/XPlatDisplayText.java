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


package org.systemsbiology.biotapestry.genome;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/****************************************************************************
**
** Provides info on the current display text for models, overlays, modules,
** and notes.
*/

public class XPlatDisplayText {

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
 
  private String modelText_;
  private HashMap<String, String> overText_;
  private HashMap<ModuleKey, String> modText_;
  private HashMap<String, String> noteText_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatDisplayText() {
    modelText_ = "";
    overText_ = new HashMap<String, String>();
    modText_ = new HashMap<ModuleKey, String>();
    noteText_ = new HashMap<String, String>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Model text, if any
  */ 
   
  public void setModelText(String display) {
    modelText_ = display;
    return;
  }
  
  /***************************************************************************
  **
  ** Overlay text, if any
  */ 
   
  public void setOverlayText(String key, String display) {
    overText_.put(key, display);
    return;
  }
  
  /***************************************************************************
  **
  ** Module text, if any
  */ 
   
  public void setModuleText(NetModule.FullModuleKey key, String display) {
    modText_.put(new ModuleKey(key.ovrKey, key.modKey), display);
    return;
  }
  
  public Map<ModuleKey,String> getModuleText() {
	  if(this.modText_ == null) {return null;}
	  return Collections.unmodifiableMap(this.modText_);
  }
  
  /***************************************************************************
  **
  ** Note text, if any
  */ 
   
  public void setNoteText(String key, String display) {
    noteText_.put(key, display);
    return;
  }
  
  public Map<String,String> getNoteText() {
	  if(this.noteText_ == null) {return null;}
	  return Collections.unmodifiableMap(this.noteText_);
  }
  
  /***************************************************************************
  **
  ** Get model text
  */ 
   
  public String getModelText() {
    return (modelText_);
  }
  
  /***************************************************************************
  **
  ** Reduced set of FullModuleKey that is appropriate for this application
  */  
      
  public static class ModuleKey implements Cloneable {
    
    public String ovrKey;
    public String modKey;
        
    public ModuleKey(String ovrKey, String modKey) {
      this.ovrKey = ovrKey;
      this.modKey = modKey;
    }

    public String toString() {
    	return this.ovrKey+":"+this.modKey;
    }
    
    @Override
    public int hashCode() {
      return (ovrKey.hashCode() +  modKey.hashCode());
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof ModuleKey)) {
        return (false);
      }
      ModuleKey otherMK = (ModuleKey)other;
      
      if (!this.modKey.equals(otherMK.modKey)) {
        return (false);
      }       
      
      if (!this.ovrKey.equals(otherMK.ovrKey)) {
        return (false);
      }     
      return (true);     
    }
    
    @Override
    public ModuleKey clone() {
      try {
        return ((ModuleKey)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
  }

}
