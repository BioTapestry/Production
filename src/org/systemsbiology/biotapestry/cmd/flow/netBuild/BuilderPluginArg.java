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

package org.systemsbiology.biotapestry.cmd.flow.netBuild;

import java.io.IOException;
import java.util.Map;

import org.systemsbiology.biotapestry.cmd.flow.AbstractOptArgs;

/***************************************************************************
**
** Control Flow Argument
*/  

public class BuilderPluginArg extends AbstractOptArgs {
  
  private static String[] keys_;
  private static Class<?>[] classes_;
  
  static {
    keys_ = new String[] {"builderName", "builderIndex"};  
    classes_ = new Class<?>[] {Integer.class};  
  }
  
  public String getBuilderName() {
    return (getValue(0));
  }
     
  public int getBuilderIndex() {
    return (Integer.parseInt(getValue(1)));
  }

  public BuilderPluginArg(Map<String, String> argMap) throws IOException {
    super(argMap);
  }

  protected String[] getKeys() {
    return (keys_);
  }
  
  @Override
  protected Class<?>[] getClasses() {
    return (classes_);
  }
  
  public BuilderPluginArg(String builderName, int builderIndex) {
    super();
    setValue(0, builderName);
    setValue(1, Integer.toString(builderIndex));
    bundle();
  }
}  
