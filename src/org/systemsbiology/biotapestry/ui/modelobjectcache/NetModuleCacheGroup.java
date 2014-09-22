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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree.TaggedShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.GeneCacheGroup.Pad;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;
import org.systemsbiology.biotapestry.util.Vector2D;

public class NetModuleCacheGroup extends CacheGroup implements ModalShapeContainer {
	class Pad {
		double x_;
		double y_;
		
		public Pad(double x, double y) {
			x_ = x;
			y_ = y;
		}
	}
	
	protected ArrayList<CacheGroupItem> children_;	
	ModalShape outerBoundShape_;
	ModalShape nameBoundShape_;
	protected List<TaggedShape> shape_;
	protected List<String> members_;
	
	protected ArrayList<Pad> padCoordinates_;
	
	public NetModuleCacheGroup(String id, String name, String type) {
		super(id,  name, type);

		children_ = new ArrayList<CacheGroupItem>();
		
		outerBoundShape_ = null;
		nameBoundShape_ = null;
		
		shape_ = new ArrayList<TaggedShape>();
		members_ = new ArrayList<String>();
		
		padCoordinates_ = new ArrayList<Pad>();		
	}
	
	public NetModuleCacheGroup(NetModule nm) {
		this(nm.getID(), nm.getName(), "net_module");
	}

	public void addShape(ModalShape shape, Integer majorLayer, Integer minorLayer) {
		children_.add(new CacheGroupItem(shape, majorLayer, minorLayer, false));
	}

	public Iterator<CacheGroupItem> iterator() {
		return children_.iterator();
	}
	
	public ArrayList<CacheGroupItem> getShapes() {
		return children_;
	}
	
	public void accept(CacheGroupVisitor visitor) {
		visitor.visit(this);
	}
	
	public void setOuterBoundShape(ModalShape shape) {
		outerBoundShape_ = shape;
	}
	
	public ModalShape getOuterBoundShape() {
		return outerBoundShape_;
	}
	
	public void setNameBoundShape(ModalShape shape) {
		nameBoundShape_ = shape;
	}
	
	public ModalShape getNameBoundShape() {
		return nameBoundShape_;
	}
	
	public void setShape(List<TaggedShape> shape) {
		shape_ = shape;
	}
	
	public List<TaggedShape> getShape() {
		return shape_;
	}
	
	public void addMember(String memberID) {
		members_.add(memberID);
	}
	
	public List<String> getMembers() {
		return members_;
	}
	
	public void addPad(double x, double y) {
		padCoordinates_.add(new Pad(x, y));
	}
	
	public Iterator<Pad> iteratorForPadCoordiantes() {
		return padCoordinates_.iterator(); 
	}
}
