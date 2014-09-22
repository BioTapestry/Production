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

import java.awt.Font;

public class FontExport {
  static class FontType {
    public static final int SANS_SERIF = 0;
    public static final int SERIF = 1;
    public static final int SANS_SERIF_BOLD = 2;
    public static final int SERIF_BOLD = 3;
    public static final int SANS_SERIF_ITALIC = 4;
    public static final int SERIF_ITALIC = 5;
    public static final int SANS_SERIF_BOLD_ITALIC = 6;
    public static final int SERIF_BOLD_ITALIC = 7;
  }

  int size_;
  int type_;
  int style_;

  public FontExport(Font font) {
    size_ = font.getSize();
    style_ = font.getStyle();

    if (font.getFamily().equals(Font.SERIF)) {
      if (!font.isBold() && !font.isItalic()) {
        type_ = FontType.SERIF;
      }
      else if (font.isBold() && !font.isItalic()) {
        type_ = FontType.SERIF_BOLD;
      }
      else if (!font.isBold() && font.isItalic()) {
        type_ = FontType.SERIF_ITALIC;
      }
      else {
        type_ = FontType.SERIF_BOLD_ITALIC;
      }
    }
    else if (font.getFamily().equals(Font.SANS_SERIF)) {
      if (!font.isBold() && !font.isItalic()) {
        type_ = FontType.SANS_SERIF;
      }
      else if (font.isBold() && !font.isItalic()) {
        type_ = FontType.SANS_SERIF_BOLD;
      }
      else if (!font.isBold() && font.isItalic()) {
        type_ = FontType.SANS_SERIF_ITALIC;
      }
      else {
        type_ = FontType.SANS_SERIF_BOLD_ITALIC;
      }
    }
  }

  @JSON
  public int getSize() {
    return size_;
  }

  @JSON
  public int getType() {
    return type_;
  }

  @JSON
  public int getStyle() {
    return style_;
  }
}
