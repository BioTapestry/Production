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

import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Arc;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Ellipse;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Line;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.MultiLineTextShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PopTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushRotationWithOrigin;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.PushTransformOperation;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.Rectangle;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SegmentedPathShape;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.SetComposite;
import org.systemsbiology.biotapestry.ui.modelobjectcache.ModelObjectCache.TextShape;

public class BoundingShapeJSONizer {

  public static void visit(TextShape text, Map<String, Object> obj) {

  }

  public static void visit(MultiLineTextShape mlt, Map<String, Object> obj) {

  }

  public static void visit(Ellipse shape, Map<String, Object> obj) {
    obj.put("type", "ellipse");
    obj.put("cx", shape.getCenterX());
    obj.put("cy", shape.getCenterY());
    obj.put("r", shape.getRadius());
  }

  public static void visit(Arc shape, Map<String, Object> obj) {
    obj.put("type", "arc");
    obj.put("cx", shape.getCenterX());
    obj.put("cy", shape.getCenterY());
    obj.put("r", shape.getRadius());
    obj.put("start", shape.getStart());
    obj.put("extent", shape.getExtent());
    obj.put("draw_type", shape.getType());
  }

  public static void visit(Rectangle shape, Map<String, Object> obj) {
    obj.put("type", "rect");
    obj.put("x", shape.getX());
    obj.put("y", shape.getY());
    obj.put("w", shape.getWidth());
    obj.put("h", shape.getHeight());

    // Rectangle is the only bounding shape that is used to indicate label bounds
    obj.put("s", shape.getSelectionType());
  }

  public static void visit(Line shape, Map<String, Object> obj) {

  }

  public static void visit(SegmentedPathShape shape, Map<String, Object> obj) {

  }

  public static void visit(PushTransformOperation op, Map<String, Object> obj) {

  }

  public static void visit(PopTransformOperation op, Map<String, Object> obj) {

  }

  public static void visit(PushRotationWithOrigin op, Map<String, Object> obj) {

  }

  public static void visit(SetComposite op, Map<String, Object> obj) {

  }
}
