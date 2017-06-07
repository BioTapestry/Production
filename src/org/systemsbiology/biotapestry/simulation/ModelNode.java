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

package org.systemsbiology.biotapestry.simulation;

/****************************************************************************
**
** A BioTapestry Node
** 
*/

public class ModelNode implements Cloneable {
  
  public enum Type {BARE, BOX, BUBBLE, GENE, INTERCELL, SLASH, DIAMOND};
    
  private final Type type_;
  private final String name_;
  private final String uniqueName_;
  private final String id_;
  
  public ModelNode(String id, String name, String uniqueName, Type type) {
    if ((name == null) || (id == null) || (type == null)) {
      throw new IllegalArgumentException();
    }
    this.id_ = id;
    this.name_ = name;
    this.uniqueName_ = uniqueName;
    this.type_ = type;
  }

  public ModelNode(ModelNode other) {
    this.name_ = other.name_;
    this.uniqueName_ = other.uniqueName_;
    this.type_ = other.type_;
    this.id_ = other.id_;
  }
  
  @Override
  public ModelNode clone() {
    try {
      return ((ModelNode)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
  
  public String getName() {
    return (name_);
  }
    
  public String getUniqueName() {
    return (uniqueName_);
  }
  
  public Type getType() {
    return (type_);
  }
  
  public String getUniqueInternalID() {
    return (id_);
  } 

  @Override
  public int hashCode() {
    return (name_.hashCode() + type_.hashCode() + id_.hashCode());
  }

  @Override
  public String toString() {
    return ("name = " + name_ + " type = " + type_ + " id = " + id_);
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof ModelNode)) {
      return (false);
    }
    ModelNode otherNode = (ModelNode)other;
    if (!this.name_.equals(otherNode.name_)) {
      return (false);
    }
    
    if (!this.type_.equals(otherNode.type_)) {
      return (false);
    }  
    return (this.id_.equals(otherNode.id_));   
  }
}
