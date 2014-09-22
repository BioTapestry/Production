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

import java.util.Map;

import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.ModalShape;

import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Ellipse;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShapeForWeb;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PopTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushRotationWithOrigin;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Rectangle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SegmentedPathShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SetComposite;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShapeForWeb;

public class BoundingShapeVisitor implements ModalShapeVisitor {
  private Map<String, Object> currentObject_;

  public BoundingShapeVisitor () {

  }

  public void setCurrentObject(Map<String, Object> targetObject_) {
    currentObject_ = targetObject_;
  }

  public void visit(TextShape text) {

  }
  
  public void visit(TextShapeForWeb text) {

  }  

  public void visit(MultiLineTextShape mlt) {

  }
  
  public void visit(MultiLineTextShapeForWeb mlt) {

  }  

  public void visit(Ellipse shape) {
    BoundingShapeJSONizer.visit(shape, currentObject_);
  }

  public void visit(Arc shape) {
    BoundingShapeJSONizer.visit(shape, currentObject_);
  }

  public void visit(Rectangle shape) {
    BoundingShapeJSONizer.visit(shape, currentObject_);
  }

  public void visit(Line shape) {
    BoundingShapeJSONizer.visit(shape, currentObject_);
  }

  public void visit(SegmentedPathShape shape) {

  }

  public void visit(PushTransformOperation op) {

  }

  public void visit(PopTransformOperation op) {

  }

  public void visit(PushRotationWithOrigin op) {

  }

  public void visit(SetComposite op) {

  }
}
