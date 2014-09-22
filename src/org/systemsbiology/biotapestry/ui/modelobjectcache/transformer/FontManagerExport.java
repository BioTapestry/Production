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

package org.systemsbiology.biotapestry.ui.modelobjectcache.transformer;

import flexjson.JSON;
import org.systemsbiology.biotapestry.ui.FontManager;

import java.awt.Font;
import java.util.ArrayList;

public class FontManagerExport {
  ArrayList<FontExport> defaults_;
  ArrayList<FontExport> fixed_;

  public FontManagerExport(FontManager fm) {
    defaults_ = new ArrayList<FontExport>();
    fixed_ = new ArrayList<FontExport>();

    for (Font font : fm.getDefaultFonts()) {
      defaults_.add(new FontExport(font));
    }

    for (Font font : fm.getFixedFonts()) {
      fixed_.add(new FontExport(font));
    }
  }

  @JSON
  public ArrayList<FontExport> getDefaults() {
    return defaults_;
  }

  @JSON
  public ArrayList<FontExport> getFixed() {
    return fixed_;
  }
}
