/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.ui.xplat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XPlatStackPage implements XPlatResponse {

	private String containerId_;
	private String pageId_;
	private Map<String,Map<String,Object>> widgetSettings_;
	private Map<String,Map<String,Object>> elementConditions_;

	public XPlatStackPage(String contId,String pgId) {
		this.containerId_ = contId;
		this.pageId_ = pgId;
	}
	
	public XPlatStackPage(String id) {
		this.pageId_ = id;
	}
	
	public XPlatStackPage() {
		
	}
	
	public XPlatResponseType getXplatResponseType() {
		return XPlatResponseType.STACK_PAGE;
	}

	public String getContainerId() {
		return this.containerId_;
	}

	public void setContainerId(String id) {
		this.containerId_ = id;
	}
	
	public String getPageId() {
		return this.pageId_;
	}

	public void setPageId(String id) {
		this.pageId_ = id;
	}

	public Map<String, Map<String,Object>> getWidgetSettings() {
		if(this.widgetSettings_ != null) {
			return Collections.unmodifiableMap(widgetSettings_);	
		}
		return null;
	}

	public void setWidgetSettings(Map<String,Map<String,Object>> widgetSettings) {
		this.widgetSettings_ = widgetSettings;
	}
	
	public void addWidgetSettings(String widget,Map<String,Object> settings) {
		if(this.widgetSettings_ == null) {
			this.widgetSettings_ = new HashMap<String,Map<String,Object>>();
		}
		this.widgetSettings_.put(widget, settings);
	}
	
	public void addWidgetSetting(String widget,String setting,Object value) {
		if(this.widgetSettings_ == null) {
			this.widgetSettings_ = new HashMap<String,Map<String,Object>>();
		}
		Map<String,Object> widgSettings = this.widgetSettings_.get(widget);
		if(widgSettings == null) {
			widgSettings = new HashMap<String,Object>();
			this.widgetSettings_.put(widget, widgSettings);
		}
		widgSettings.put(setting,value);
	}

	public Map<String, Map<String,Object>> getElementConditions() {
		if(this.elementConditions_ != null) {
			return Collections.unmodifiableMap(elementConditions_);
		}
		return null;
	}
	
	public void setElementConditions(Map<String, Map<String,Object>> elementConditions) {
		this.elementConditions_ = elementConditions;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
