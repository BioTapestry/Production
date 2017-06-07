/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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


/****************************************************************************
**
** Generic tuple
*/

public class Tuple<T extends Comparable<T>, S extends Comparable<S>> implements Comparable<Tuple<T, S>>, Cloneable {

  private final T obj1_;
  private final S obj2_;
  
  public Tuple(T o1, S o2) {
    if ((o1 == null) || (o2 == null)) {
      throw new IllegalArgumentException();
    }
    obj1_ = o1;
    obj2_ = o2;
  }
  
  @Override
  public Tuple<T, S> clone() {
    try {
      @SuppressWarnings("unchecked")
      Tuple<T, S> retval = (Tuple<T, S>)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  public int compareTo(Tuple<T, S> other) {
    int compare1 = this.obj1_.compareTo(other.obj1_);
    if (compare1 != 0) {
      return (compare1);
    }
    return (this.obj2_.compareTo(other.obj2_));
  }
 
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof Tuple)) {
      return (false);
    }
    Tuple<?, ?> occ = (Tuple<?, ?>)other;
    if (!this.obj1_.equals(occ.obj1_)) {
      return (false);
    }
    return (this.obj2_.equals(occ.obj2_));
  }
  
  public T getFirst() {
    return (obj1_);
  }
  
  public S getSecond() {
    return (obj2_);
  } 
  
  public int hashCode() {
    return ((obj1_.hashCode() * 3) + obj2_.hashCode());
  }
  
  public String toString() {
    return ("Tuple : " + obj1_ + " -> " + obj2_);
  }
}
