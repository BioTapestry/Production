/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.db;

import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.tabs.TabOps;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.StringFromXML;

/****************************************************************************
**
** This holds tab naming data
*/

public class TabNameData implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String title_;
  private String fullTitle_;
  private String desc_; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public TabNameData() {
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public TabNameData(String title, String fullTitle, String desc) {
    title_ = title;
    fullTitle_ = fullTitle;
    desc_ = desc; 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public TabNameData clone() {
    try {
      TabNameData retval = (TabNameData)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  ** 
  ** Get and set tab properties
  */
  
  public String getTitle() {
    return (title_);
  }

  public void setTitle(String title) {
    title_ = title;
    return;
  }
  
  public String getFullTitle() {
    return (fullTitle_);
  }

  public void setFullTitle(String fullTitle) {
    fullTitle_ = fullTitle;
    return;
  }
  
  public String getDesc() {
    return (desc_);
  }

  public void setDesc(String desc) {
    desc_ = desc;
    return;
  }
     
  /***************************************************************************
  **
  ** Write the data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.println("<tabData>");
    ind.up();
    if (title_ != null) {
      ind.indent();
      out.print("<tabTitle>");
      out.print(CharacterEntityMapper.mapEntities(title_, false));
      out.println("</tabTitle>");
    }
    if (fullTitle_ != null) {
      ind.indent();
      out.print("<fullTabTitle>");
      out.print(CharacterEntityMapper.mapEntities(fullTitle_, false));
      out.println("</fullTabTitle>");
    }  
    if (desc_ != null) {
      ind.indent();
      out.print("<tabDesc>");
      out.print(CharacterEntityMapper.mapEntities(desc_, false));
      out.println("</tabDesc>");
    }  
    ind.down().indent();       
    out.println("</tabData>");
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("TabNameData: title = " +  title_ + " full title = " + fullTitle_ + " desc = " +  desc_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class TabNameDataWorker extends AbstractFactoryClient implements StringFromXML.Consumer {
    
    private boolean forAppend_;
    private UIComponentSource uics_;
    private DataAccessContext dacx_;
    private TabSource tSrc_;
    
    public TabNameDataWorker(FactoryWhiteboard whiteboard, boolean forAppend) {
      super(whiteboard);
      forAppend_ = forAppend;
      myKeys_.add("tabData");
      installWorker(new StringFromXML.StringWorker(whiteboard, "tabTitle", this), null);
      installWorker(new StringFromXML.StringWorker(whiteboard, "fullTabTitle", this), null);
      installWorker(new StringFromXML.StringWorker(whiteboard, "tabDesc", this), null);
    }
 
    /***************************************************************************
    **
    ** Set current context
    **
    */
    
    public void setContext(DataAccessContext dacx, UIComponentSource uics, TabSource tSrc) {
      dacx_ = dacx;
      uics_ = uics;
      tSrc_ = tSrc;
      return;
    }

    /***************************************************************************
    **
    ** handle generating a unique tab name for appending cases
    **
    */
    
    public static void tweakForAppendUniqueness(TabSource tSrc, DataAccessContext dacx, TabNameData tnd) {
      String existing = tnd.getFullTitle();
      String unqTabName = TabOps.buildUniqueTabName(tSrc, dacx, existing);
      if (!unqTabName.equals(existing)) {
        tnd.setFullTitle(unqTabName);
        if (tnd.getTitle().equals(existing)) {
          tnd.setTitle(unqTabName);
        }
      }
      return;
    }

   /***************************************************************************
    **
    ** Callback for completion of the element
    **
    */
    
    @Override
    public void localFinishElement(String elemName) {
      if (elemName.equals("tabData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;

        if (forAppend_) {
          tweakForAppendUniqueness(tSrc_, dacx_, board.tnd);
        }     
        dacx_.getGenomeSource().setTabNameData(board.tnd);
        uics_.getCommonView().setTabTitleData(tSrc_.getCurrentTabIndex(), board.tnd);
      }
      return;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("tabData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.tnd = buildFromXML(elemName, attrs);
        retval = board.tnd;
      }
      return (retval);     
    }  
    
    @SuppressWarnings("unused")
    private TabNameData buildFromXML(String elemName, Attributes attrs) throws IOException {
      TabNameData tnd = new TabNameData();
      return (tnd);
    }
    
    public void finishAddAnnotForIO(StringFromXML sfx) {
      FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
      if (sfx.key.equals("tabTitle")) {
        board.tnd.title_ = sfx.value;
      } else if (sfx.key.equals("fullTabTitle")) {
        board.tnd.fullTitle_ = sfx.value;
      } else if (sfx.key.equals("tabDesc")) {
        board.tnd.desc_ = sfx.value;
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
