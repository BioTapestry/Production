/*
**    Copyright (C) 2003-2013 Institute for Systems Biology
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

package org.systemsbiology.biotapestry.ui.freerender;

import flexjson.BasicType;
import flexjson.TypeContext;
import flexjson.transformer.AbstractTransformer;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.util.Vector2D;

public class LinkSegmentGeometryExportTransformer extends AbstractTransformer {

  private String prefix_ = "";

  public LinkSegmentGeometryExportTransformer() {
    this.prefix_ = "";
  }

  public LinkSegmentGeometryExportTransformer(String prefix) {
    this.prefix_ = prefix;
  }

  public void transform(Object object) {
    boolean setContext = false;

    TypeContext typeContext = getContext().peekTypeContext();
    String propertyName = typeContext != null ? typeContext.getPropertyName() : "";

    // TODO figure out why prefix_ is null at this point
    this.prefix_ = "";
    if (this.prefix_.trim().equals("")) {
      this.prefix_ = propertyName;
    }

    if (typeContext == null || typeContext.getBasicType() != BasicType.OBJECT) {
      typeContext = getContext().writeOpenObject();
      setContext = true;
    }

    LinkSegment seg = (LinkSegment) object;

    if (!typeContext.isFirst()) {
      getContext().writeComma();
    }

    typeContext.setFirst(false);
    getContext().writeName("start");
    getContext().transform(seg.getStart());

    getContext().writeComma();
    getContext().writeName("end");
    getContext().transform(seg.getEnd());

    Vector2D normal = seg.getNormal();
    if (normal != null) {
      getContext().writeComma();
      getContext().writeName("normal");
      getContext().transform(normal);
    }

    Vector2D run = seg.getRun();
    if (run != null) {
      getContext().writeComma();
      getContext().writeName("run");
      getContext().transform(run);
    }

    if (setContext) {
      getContext().writeCloseObject();
    }
  }
}
