/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.io.IOException;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.xml.sax.Attributes;

/***************************************************************************
**
** For XML input of general string elements within our framework
*/  
     
public class StringFromXML {
  public String key;
  public String value;
  
  
  public StringFromXML(String key) {
    this.key = key;
  }

  public interface Consumer {
    public void finishAddAnnotForIO(StringFromXML sfx);  
  }

  public static class StringWorker extends AbstractFactoryClient {
    
    private String elemTag_;
    private StringBuffer buf_;
    private StringFromXML currStr_;
    private Consumer cons_;
 
    public StringWorker(FactoryWhiteboard whiteboard, String elemTag, Consumer cons) {
      super(whiteboard);
      buf_ = new StringBuffer();
      elemTag_ = elemTag;
      myKeys_.add(elemTag_);
      cons_ = cons;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(elemTag_)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.sfxml = buildFromXML(elemName, attrs);
        retval = board.sfxml;
      }
      return (retval);     
    }
  
    @Override
    protected void localFinishElement(String elemName) throws IOException {
      currStr_.value = CharacterEntityMapper.unmapEntities(buf_.toString(), false);
      cons_.finishAddAnnotForIO(currStr_);    
      return;
    }
 
    @Override
    protected void localProcessCharacters(char[] chars, int start, int length) {
      String nextString = new String(chars, start, length);
      buf_.append(nextString);
      return;
    }  
    
    @SuppressWarnings("unused")
    private StringFromXML buildFromXML(String elemName, Attributes attrs) throws IOException {
      buf_.setLength(0);
      currStr_ = new StringFromXML(elemTag_);
      return (currStr_);
    }
  }
}