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

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DynamicGenomeInstance;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Cross-Platform Model Tree Node
*/

public class XPlatModelNode {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  public enum ModelType {SUPER_ROOT, DB_GENOME, GENOME_INSTANCE, DYNAMIC_INSTANCE, DYNAMIC_PROXY};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public ArrayList<XPlatModelNode> children_;
  private String id_;
  private String name_;
  private ModelType modType_;
  private MinMax range_;
  private boolean hasImage_; 
  private boolean hasOverlays_;
  private ArrayList<XPlatOverlayDef> overInfo_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatModelNode(String id, String name, ModelType modType, boolean hasImage, boolean hasOverlays) {
    if (modType == ModelType.DYNAMIC_PROXY) {
      throw new IllegalArgumentException();
    }
    children_ = new ArrayList<XPlatModelNode>();
    overInfo_ = new ArrayList<XPlatOverlayDef>();
    id_ = id;
    name_ = name;
    modType_ = modType;
    range_ = null;
    hasImage_ = hasImage;
    hasOverlays_ = hasOverlays;
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatModelNode(String id, String name, ModelType modType, MinMax timeRange, boolean hasImage, boolean hasOverlays) {
    if (modType != ModelType.DYNAMIC_PROXY) {
      throw new IllegalArgumentException();
    }
    children_ = new ArrayList<XPlatModelNode>();
    overInfo_ = new ArrayList<XPlatOverlayDef>();
    id_ = id;
    name_ = name;
    modType_ = modType;
    range_ = timeRange;
    hasImage_ = hasImage;
    hasOverlays_ = hasOverlays;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////      
  
  /***************************************************************************
  **
  ** Used as keys for identifying model tree nodes
  */
  
  public static class NodeKey {
      
    public String id;
    public ModelType modType;
    
    /***************************************************************************
    **
    ** Constructor
    */

    public NodeKey(String id, ModelType modType) {
      this.id = id;
      this.modType = modType;
    }

    public Genome getPopTarget(DataAccessContext dacx) {
      if (modType == XPlatModelNode.ModelType.DYNAMIC_PROXY) {
        return (dacx.getGenomeSource().getDynamicProxy(id).getAnInstance());
      } else {
        return (dacx.getGenomeSource().getGenome(id));
      }
    }
    
    public String toString() {
    	return "{\"id\":\"" + this.id + "\",\"type\":\"" + this.modType.toString() + "\"}";
    }
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ModelType getModType() {
		return modType;
	}

	public void setModType(ModelType modType) {
		this.modType = modType;
	}



	public NetOverlayOwner getOverlayOwner(DataAccessContext dacx) {
      if (id == null) {
        return (null);
      } else if (modType == XPlatModelNode.ModelType.DYNAMIC_PROXY) {
        return (dacx.getGenomeSource().getDynamicProxy(id));
      } else {
        return (dacx.getGenomeSource().getGenome(id));
      }
    }
    
    public boolean currentModelMatch(DataAccessContext dacx) {
      if (modType == XPlatModelNode.ModelType.DYNAMIC_PROXY) {
        Genome currGenome = dacx.getGenome();
        if (currGenome == null) {
          return (false);
        }
        if (currGenome instanceof DynamicGenomeInstance) {
        return (DynamicInstanceProxy.extractProxyID(currGenome.getID()).equals(id));
        } else {
          return (false);
        }
      } else {
        return (id.equals(dacx.getGenomeID()));
      }
    }
    
    @Override
    public int hashCode() {
      return (((id == null) ? 0 : id.hashCode()) + modType.hashCode());
    }  
  
    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof NodeKey)) {
        return (false);
      }
      NodeKey otherNK = (NodeKey)other;
      if (this.modType != otherNK.modType) {
        return (false);
      }
      if (this.id != null) {
        return (this.id.equals(otherNK.id));
      }
      return (otherNK.id == null);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get overlay information for a node
  */

  public void fillOverlayInfoForNode(DataAccessContext dacx) {     
     
    XPlatModelNode.NodeKey nkey = getNodeKey();
    if (nkey == null) {
      return;
    }
    
    NetOverlayOwner noo = nkey.getOverlayOwner(dacx);
    if (noo == null) {
      return;
    }
  
    Iterator<NetworkOverlay> noit = noo.getNetworkOverlayIterator();    
    while (noit.hasNext()) {
      NetworkOverlay ovr = noit.next();
      TaggedSet fvs = new TaggedSet();
      TaggedSet fvr = new TaggedSet();
      boolean isFirst = ovr.getFirstViewState(fvs, fvr);
      NetOverlayProperties noProps = dacx.getLayout().getNetOverlayProperties(ovr.getID());
      boolean isOpaque = (noProps.getType() == NetOverlayProperties.OvrType.OPAQUE);
      XPlatOverlayDef ovdef = new XPlatOverlayDef(ovr.getID(), ovr.getName(), isOpaque, isFirst);
      Iterator<NetModule> nmit = ovr.getModuleIterator();       
      while (nmit.hasNext()) {
        NetModule mod = nmit.next();
        String modID = mod.getID();
        Boolean modInFirst = (isFirst) ? fvs.set.contains(modID) : null;
        Boolean modRevealedFirst = (isFirst && isOpaque) ? fvr.set.contains(modID) : null;
        ovdef.addAModule(new XPlatOverlayDef.ModuleDef(mod.getName(), mod.getID(), modInFirst, modRevealedFirst));
      }
      overInfo_.add(ovdef);
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Get the node Key
  */ 
   
  public NodeKey getNodeKey() {
    return (new NodeKey(id_, modType_));
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
  ** Get the ModelType
  */ 
   
  public ModelType getModelType() {
    return (modType_);
  }
 
  /***************************************************************************
  **
  ** Get the time range
  */ 
   
  public MinMax getTimeRange() {
    return (range_);
  }
 
  /***************************************************************************
  **
  ** Get if node has associated image(s)
  */ 
   
  public boolean getHasImage() {
    return (hasImage_);
  }
  
  /***************************************************************************
  **
  ** Get if node has associated overlays
  */ 
   
  public boolean getHasOverlays() {
    return (hasOverlays_);
  }

  /***************************************************************************
  **
  ** Add a child
  */ 
   
  public void addChild(XPlatModelNode xpmn) {
    children_.add(xpmn);
    return;
  }
  
  /***************************************************************************
  **
  ** Menu Iterator
  */ 
   
  public Iterator<XPlatModelNode> getChildren() {
    return (children_.iterator());
  }
  
  /***************************************************************************
  **
  ** Answer if we or kids have an image. Recursive with short-circuit answer
  */ 
   
  public boolean iOrKidsHaveImage() {  
  
    if (hasImage_) {
      return (true);
    }
    
    Iterator<XPlatModelNode> cit = children_.iterator();
    while (cit.hasNext()) {
      XPlatModelNode child = cit.next();
      if (child.iOrKidsHaveImage()) {
        return (true);
      }
    }
  
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if we or kids have overlays. Recursive with short-circuit answer
  */ 
   
  public boolean iOrKidsHaveOverlays() {  
  
    if (hasOverlays_) {
      return (true);
    }
    
    Iterator<XPlatModelNode> cit = children_.iterator();
    while (cit.hasNext()) {
      XPlatModelNode child = cit.next();
      if (child.iOrKidsHaveOverlays()) {
        return (true);
      }
    }
  
    return (false);
  }
  
  
  /***************************************************************************
  **
  ** Get the child nodes
  */ 
   
  public List<XPlatModelNode> getChildNodes() {
    if (children_ == null) {
      return null;
    }
    return (Collections.unmodifiableList(children_));
  }
  
  
  /***************************************************************************
  **
  ** Get the overlay definitions
  */ 
   
  public List<XPlatOverlayDef> getOverlayDefs() {
    if (overInfo_ == null) {
      return null;
    }
    return (Collections.unmodifiableList(overInfo_));
  }
  
  /***************************************************************************
  **
  ** Standard string
  */ 
   
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("XPlatModelNode: id = ");
    buf.append(id_);
    buf.append(" name = ");
    buf.append(name_);
    buf.append(" model type = ");
    buf.append(modType_);
    buf.append(" range = ");
    buf.append(range_);
    Iterator<XPlatOverlayDef> oit = overInfo_.iterator();
    while (oit.hasNext()) {
      buf.append("\n");
      buf.append(oit.next().toString());
    }

    Iterator<XPlatModelNode> cit = children_.iterator();
    while (cit.hasNext()) {
      buf.append("\n");
      buf.append(cit.next().toString());
    }

    return (buf.toString());
  }
}
