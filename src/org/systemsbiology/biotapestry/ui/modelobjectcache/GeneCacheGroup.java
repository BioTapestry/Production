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
import org.systemsbiology.biotapestry.util.Vector2D;

public class GeneCacheGroup extends CacheGroup implements ModalShapeContainer, SelectedModalShapeContainer, BoundShapeContainer {
	class Pad {
		double x_;
		double y_;
		
		public Pad(double x, double y) {
			x_ = x;
			y_ = y;
		}
	}
	
	protected ArrayList<ModalShape> bounds_;
	protected ArrayList<CacheGroupItem> children_;
	
	protected ArrayList<CacheGroupItem> padshapes_;
	
	protected Vector2D launchPadOffset_;

	protected ArrayList<Pad> padCoordinates_;
	
	public GeneCacheGroup(String id, String name, String type, Vector2D lpo) {
		super(id, name, type);
		
		children_ = new ArrayList<CacheGroupItem>();
		bounds_ = new ArrayList<ModalShape>();	
		
		padshapes_ = new ArrayList<CacheGroupItem>();
		padCoordinates_ = new ArrayList<Pad>();
		
		launchPadOffset_ = lpo;		
	}
	
	public GeneCacheGroup(GenomeItem item, Vector2D lpo) {
		this(item.getID(), item.getName(), "gene", lpo);
	}
	
	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}
	
	public void addShapeSelected(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, true));
	}	
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
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

	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}
	
	public void addPad(double x, double y) {
		padCoordinates_.add(new Pad(x, y));
	}
	
	public void addPadShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		padshapes_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}
	
	public Iterator<CacheGroupItem> iteratorForPadShapes() {
		return padshapes_.iterator();
	}
	
	public Iterator<Pad> iteratorForPadCoordiantes() {
		return padCoordinates_.iterator(); 
	}
	
	public Vector2D getLaunchPadOffset() {
		return launchPadOffset_;
	}
}
