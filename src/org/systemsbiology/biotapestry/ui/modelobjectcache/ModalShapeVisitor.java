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

public interface ModalShapeVisitor {
	// //////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC INNER CLASSES
	//
	// //////////////////////////////////////////////////////////////////////////
	public interface Visitable {
		public void accept(ModalShapeVisitor visitor);
	}	
	
	void visit(ModelObjectCache.TextShape text);
	void visit(ModelObjectCache.TextShapeForWeb text);
	
	void visit(ModelObjectCache.MultiLineTextShape mlt);
	void visit(ModelObjectCache.MultiLineTextShapeForWeb mlt);
	
	void visit(ModelObjectCache.Ellipse ellipse);
	void visit(ModelObjectCache.Arc arc);
	void visit(ModelObjectCache.Rectangle rect);
	void visit(ModelObjectCache.Line line);
	
	void visit(ModelObjectCache.SegmentedPathShape path);
	
	void visit(ModelObjectCache.PushTransformOperation op);
	void visit(ModelObjectCache.PopTransformOperation op);
	
	void visit(ModelObjectCache.PushRotationWithOrigin op);
	
	void visit(ModelObjectCache.SetComposite op);
}