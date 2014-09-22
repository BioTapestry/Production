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

package org.systemsbiology.biotapestry.web.serialization;

import java.util.List;

import flexjson.JSONSerializer;
import org.systemsbiology.biotapestry.ui.LinkSegment;
import org.systemsbiology.biotapestry.ui.freerender.LinkSegmentGeometryExportTransformer;

public class BioTapSerializerFactory {
  private BioTapSerializerFactory() {}

  public static JSONSerializer getBasicTransformer() {
    return new JSONSerializer().exclude("*.class").include("*").transform(new ExcludeTransformer(), void.class);
  }
  
  /**
   * Uses .exclude("*.class") to remove the class property from serialized classes; this is a large
   * savings in terms of serialized overhead.
   * 
   * 
   * @param noClassExceptions List of wildcard-enabled strings which indicate which classes /should/ 
   * retain their class information when serialized
   * @return
   */
  public static JSONSerializer getNoClassTransformer(List<String> noClassExceptions) {
	  JSONSerializer serializer = new JSONSerializer();
	  
	  if(noClassExceptions != null) {
		  for(String exception : noClassExceptions) {
			  serializer = serializer.include(exception);
		  }
	  }
	  
	  return serializer.exclude("*.class").include("*").transform(new ExcludeTransformer(), void.class);  
  }

  public static JSONSerializer getModelMapTransformer() {
    JSONSerializer serializer = getBasicTransformer();

    return serializer.transform(new LinkSegmentGeometryExportTransformer(), LinkSegment.class);
  }
}
