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

package org.systemsbiology.biotapestry.ui.modelobjectcache;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;

public class CommonCacheGroup extends CacheGroup implements ModalShapeContainer, SelectedModalShapeContainer, BoundShapeContainer {

	protected ArrayList<CacheGroupItem> children_;
	protected AffineTransform trans_;
	protected ArrayList<ModalShape> bounds_;

	public CommonCacheGroup(String id, String name, String type, AffineTransform trans) {
		super(id, name, type);
		
		trans_ = trans;
		children_ = new ArrayList<CacheGroupItem>();
		bounds_ = new ArrayList<ModalShape>();
	}
	
	public CommonCacheGroup(String id, String name, String type) {
		this(id, name, type, new AffineTransform());
	}

	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}

	public void addShapeSelected(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, true));
	}

	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}

	public void addBoundsShape(ModalShape shapeObj) {
		bounds_.add(shapeObj);
	}
	
	public ArrayList<ModalShape> getBounds() {
		return bounds_;
	}
	
	public Iterator<ModalShape> boundsIterator() {
		return bounds_.iterator();
	}
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
	}	
}