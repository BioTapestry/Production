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

import java.util.Iterator;

public abstract class CacheGroup {
	protected String id_;
	protected String name_;
	protected String type_;
	
	public CacheGroup(String id, String name, String type) {
		id_ = id;
		name_ = name;
		type_ = type;
	}
	
	public String getId() {
		return id_;
	}

	public String getName() {
		return name_;
	}

	public String getType() {
		return type_;
	}
	
	public abstract void accept(CacheGroupVisitor visitor);
	
	public abstract Iterator<CacheGroupItem> iterator();	
}
