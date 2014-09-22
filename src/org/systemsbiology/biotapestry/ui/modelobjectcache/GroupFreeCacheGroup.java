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

import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.GenomeItem;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;

public class GroupFreeCacheGroup extends CacheGroup implements ModalShapeContainer, BoundShapeContainer {

	protected ArrayList<ModalShape> bounds_;
	protected ArrayList<CacheGroupItem> children_;
	
	protected ArrayList<CacheGroupItem> toggledItems_;
	protected ArrayList<CacheGroupItem> nonToggledItems_;
	
	private int order_;
	private int layer_;
	
	private double labelCenter_;
	
	private String childId_;
	
	public GroupFreeCacheGroup(String id, String name, String type, int order, int layer, String childId) {
		super(id,  name, type);
		
		order_ = order;
		layer_ = layer;
		
		children_ = new ArrayList<CacheGroupItem>();
		bounds_ = new ArrayList<ModalShape>();		
		
		toggledItems_ = new ArrayList<CacheGroupItem>();
		nonToggledItems_ = new ArrayList<CacheGroupItem>();		
		
		childId_ = childId;
	}
	
	public GroupFreeCacheGroup(GenomeItem item, int order, int layer, String childId) {
		this(item.getID(), item.getName(), "group", order, layer, childId);  
	}
	
	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}
	
	public void addShapeWithToggle(ModalShape shape, Integer majorLayer, Integer minorLayer, boolean isToggled) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
		
		if (isToggled) {
			toggledItems_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
		}
		else {
			nonToggledItems_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));			
		}
	}
	
	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}
	
	public Iterator<CacheGroupItem> iteratorForToggledItems() {
		return toggledItems_.iterator();
	}
	
	public Iterator<CacheGroupItem> iteratorForNonToggledItems() {
		return nonToggledItems_.iterator();
	}	
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
	}	
	
	public int getOrder() {
		return order_;
	}
	
	public int getLayer() {
		return layer_;
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
	
	public String getChildId() {
		return childId_;
	}
}
