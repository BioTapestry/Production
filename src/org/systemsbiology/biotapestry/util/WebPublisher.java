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


package org.systemsbiology.biotapestry.util;

import java.util.Map;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.awt.Rectangle;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.systemsbiology.biotapestry.nav.NavTree;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Note;

/****************************************************************************
**
** Used to publish to the web
*/

public class WebPublisher {

  private DataAccessContext dacx_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public WebPublisher(DataAccessContext dacx)  {
    dacx_ = dacx;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Generate HTML pages
  */
  
  public void printHTML(NamedOutputStreamSource streamSrc, Map<ModelScale, Map<String, Rectangle>> boundsMap, boolean skipRoot, String topID) throws IOException {
    // Launcher file:
    OutputStream stream = streamSrc.getNamedStream("BTLauncher.html");
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    out.println("<html>");
    out.println("<head>");
    out.println("<title>BioTapestry Browser</title>");
    out.println("<script language=\"JavaScript\">");
    out.println(" function getHeight() {");
    out.println("   return window.screen.availHeight * .90;");
    out.println("  }");
    out.println("  function getWidth() {");
    out.println("     return window.screen.availWidth * .95;");
    out.println("   }");
    out.println("  </script>");
    out.println(" </head>");
    out.println(" <body>");
    out.println(" <a href=\"\" onclick=\"brwin = window.open('suIndex.html', 'urchinHtmlWin','width=' + getWidth() + ',height=' + getHeight() + ',resizable=yes,scrollbars=no'); brwin.focus(); return false;\">Browse BioTapestry Network</a>");
    out.println("  </body>");
    out.println("</html>");
    out.close();
    // Browser shell:
    stream = streamSrc.getNamedStream("suIndex.html");
    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>BioTapestry Browser</title>");
    out.println("  </head>");
    out.println("  <frameset rows=\"38,*,67\">");
    out.println("    <frame src=\"toolbar.html\" scrolling=\"no\" noresize=\"1\" frameborder=\"0\" />");    
    out.println("    <frameset cols=\"280, *\">");
    out.println("      <frame src=\"suTreeSmall.html\" name=\"tree_frame\" scrolling=\"yes\" frameborder=\"1\" />");
    out.print("      <frame src=\"");
    out.print(topID);
    out.println("Small.png\" name=\"view_frame\" scrolling=\"yes\" frameborder=\"1\" />");
    out.println("    </frameset>");    
    out.print("    <frame src=\"");
    out.print(topID);
    out.println("Message.html\" name=\"msgFrame\" scrolling=\"no\" noresize=\"1\" frameborder=\"0\" />"); 
    out.println("  </frameset>");
    out.println("</html>");
    out.close();
    writeTreeHTML(streamSrc, ModelScale.SMALL, skipRoot);
    writeTreeHTML(streamSrc, ModelScale.MEDIUM, skipRoot);
    writeTreeHTML(streamSrc, ModelScale.LARGE, skipRoot);
    writeTreeHTML(streamSrc, ModelScale.JUMBO, skipRoot);    
    writeMessageHTML(streamSrc, skipRoot);
    writeImageWrapperHTML(streamSrc, boundsMap, skipRoot);
    writeToolbarHTML(streamSrc, topID);
    writeNotesHTML(streamSrc, skipRoot);
    return;
  }

  /****************************************************************************
  **
  ** Support class used to identify model/image size combination.  Used for keys.
  */

  public static class ModelScale implements Cloneable {
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTANTS
    //
    ////////////////////////////////////////////////////////////////////////////    
    
    public static final int SMALL  = 0; 
    public static final int MEDIUM = 1; 
    public static final int LARGE  = 2; 
    public static final int JUMBO  = 3;
    public static final int NUM_SIZES = 4;
       
    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE VARIABLES
    //
    ////////////////////////////////////////////////////////////////////////////

    private String modelID_;
    private int size_;

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC CONSTRUCTORS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
    **
    ** Constructor
    */

    public ModelScale(String modelID, int size) {
      modelID_ = modelID;
      size_ = size;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC METHODS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
    ** 
    ** Get model ID
    */

    public String getModelID() {
      return (modelID_);
    } 

    /***************************************************************************
    ** 
    ** Get the size
    */

    public int getSize() {
      return (size_);
    } 

    /***************************************************************************
    ** 
    ** Clone support
    */

    @Override
    public ModelScale clone() {
      try {
        // Strings don't need cloning:
        ModelScale retval = (ModelScale)super.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }  

    /***************************************************************************
    ** 
    ** Get the hashCode
    */
    
    @Override
    public int hashCode() {
      return (modelID_.hashCode() + size_);
    }

    /***************************************************************************
    ** 
    ** Get the to string
    */

    @Override
    public String toString() {
      return ("ModelScale: modelID = " + modelID_ + " size = " + size_);
    }

    /***************************************************************************
    ** 
    ** Standard equals
    */

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof ModelScale)) {
        return (false);
      }
      ModelScale otherMS = (ModelScale)other;

      if (this.size_ != otherMS.size_) {
        return (false);
      }
      return (this.modelID_.equals(otherMS.modelID_));
    }
    
    /***************************************************************************
    ** 
    ** Get file name
    */

    public String getFileName() {
      return (modelID_ + getSizeTag(size_) + ".png");
    } 
    
    /***************************************************************************
    ** 
    ** Get size tags
    */

    public static String getSizeTag(int size) {
      switch (size) {
        case SMALL:
          return ("Small");      
        case MEDIUM:
          return ("Medium");
        case LARGE:
          return ("Large");
        case JUMBO:
          return ("Jumbo");        
        default:
          throw new IllegalArgumentException();
      }
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Generate the tree HTML
  */
  
  private void writeTreeHTML(NamedOutputStreamSource streamSrc, int size, boolean skipRoot) throws IOException {  
     
    OutputStream stream = streamSrc.getNamedStream("suTree" + ModelScale.getSizeTag(size) + ".html");
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    out.println("<html>");
    out.println("  <head>");    
    out.println("    <script Language=\"JavaScript\">");
    out.println("      function getPathRoot() {");
    out.println("        var fullpath = window.location.pathname;");
    out.println("        var pos = fullpath.lastIndexOf('/');");
    out.println("        if (pos < 0) {");
    out.println("          return ('');");
    out.println("        } else {");
    out.println("          return (fullpath.substr(0, pos + 1));");
    out.println("        }");
    out.println("      }");
    out.println("    </script>");   
    out.println("  </head>");      
    out.println("  <body>");
    out.println("    <font face=\"Arial, Helvetica, sans-serif\" size=\"2\">");
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    List<String> ordered = navTree.getPreorderListing(skipRoot);
    Iterator<String> oit = ordered.iterator();
    boolean first = true;
    while (oit.hasNext()) {
      String gkey = oit.next();
      Genome currGenome = dacx_.getGenomeSource().getGenome(gkey);
      if (first) {
        first = false;
        out.print("<nobr>&nbsp;&nbsp;");
      } else {
        out.print("<br/><nobr>&nbsp;&nbsp;");
      }
      out.print("<a href=\"");
      out.print(gkey);
      out.print(ModelScale.getSizeTag(size));
      out.print("Image.html\" target=\"view_frame\" ");
      out.print("onclick=\"top.msgFrame.location.pathname = getPathRoot() + '");
      out.print(gkey);
      out.print("Message.html'; ");
      out.print("top.frames[0].currentView='");
      out.print(gkey);
      out.print("'; return true;\">");      
      out.print(CharacterEntityMapper.mapEntities(currGenome.getName(), false));
      out.print("</a>");
      out.println("</nobr>");
    }
    out.println("    </font>");
    out.println("  </body>");
    out.println("</html>");    
    out.close();    
    return;
  }
  
  /***************************************************************************
  **
  ** Generate message pages
  */
  
  private void writeMessageHTML(NamedOutputStreamSource streamSrc, boolean skipRoot) throws IOException {  
  
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    List<String> ordered = navTree.getPreorderListing(skipRoot);
    Iterator<String> oit = ordered.iterator();    
    while (oit.hasNext()) {
      String gkey = oit.next();
      Genome currGenome = dacx_.getGenomeSource().getGenome(gkey);
      String description = currGenome.getDescription();    
      // FIX ME use Message Format!
      String show = (description == null) ? "" : description;      
      OutputStream stream = streamSrc.getNamedStream( gkey + "Message.html");
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
      out.println("<html>");
      out.println("  <body>");
      out.println(show);        
      out.println("  </body>");
      out.println("</html>");    
      out.close();    
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Generate image wrapper pages
  */
  
  private void writeImageWrapperHTML(NamedOutputStreamSource streamSrc, Map<ModelScale, Map<String, Rectangle>> boundsMap, boolean skipRoot) throws IOException {  
  
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    List<String> ordered = navTree.getPreorderListing(skipRoot);
    Iterator<String> oit = ordered.iterator();    
    while (oit.hasNext()) {
      String gkey = oit.next();
      for (int i = 0; i < ModelScale.NUM_SIZES; i++) {
        ModelScale key = new ModelScale(gkey, i);
        Map<String, Rectangle> bounds = boundsMap.get(key);
        OutputStream stream = streamSrc.getNamedStream(gkey + ModelScale.getSizeTag(i) + "Image.html");
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
        out.println("<html>");
        out.println("  <head>");    
        out.println("    <script Language=\"JavaScript\">");
        out.println("      function getPathRoot() {");
        out.println("        var fullpath = window.location.pathname;");
        out.println("        var pos = fullpath.lastIndexOf('/');");
        out.println("        if (pos < 0) {");
        out.println("          return ('');");
        out.println("        } else {");
        out.println("          return (fullpath.substr(0, pos + 1));");
        out.println("        }");
        out.println("      }");
        out.println("    </script>");   
        out.println("  </head>");         
        out.println("  <body>");
        out.print("    <img src=\"");
        out.print(key.getFileName());
        out.println("\" usemap=\"#notesMap\" border=0 />");
        out.println("    <map name=\"notesMap\">");
        Iterator<String> noit = bounds.keySet().iterator();    
        while (noit.hasNext()) {
          String name = noit.next();
          Rectangle rect = bounds.get(name);
          int maxX = rect.x + rect.width;
          int maxY = rect.y + rect.height;
          out.print("      <area shape=rect coords=\"");
          out.print(rect.x);// * factors[i]);
          out.print(",");
          out.print(rect.y); // * factors[i]);
          out.print(",");
          out.print(maxX); // * factors[i]);
          out.print(",");
          out.print(maxY); // * factors[i]);
          out.print("\" ");
          out.print("onclick=\"top.msgFrame.location.pathname = getPathRoot() + '");
          out.print(gkey);
          out.print("Note");
          out.print(name);
          out.print(".html'; return true;\">");
        }
        out.println("    </map>");          
        out.println("  </body>");
        out.println("</html>");    
        out.close();    
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Generate toolbar page
  */
  
  private void writeToolbarHTML(NamedOutputStreamSource streamSrc, String topID) throws IOException { 
    OutputStream stream = streamSrc.getNamedStream("toolbar.html");
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    out.println("<html>");
    out.println("  <head>");    
    out.println("    <script Language=\"JavaScript\">");
    out.println("      function getPathRoot() {");
    out.println("        var fullpath = window.location.pathname;");
    out.println("        var pos = fullpath.lastIndexOf('/');");
    out.println("        if (pos < 0) {");
    out.println("          return ('');");
    out.println("        } else {");
    out.println("          return (fullpath.substr(0, pos + 1));");
    out.println("        }");
    out.println("      }");      
    out.println();  
    out.print("      currentView = \"");
    out.print(topID);
    out.println("\";");
    out.println();  
    out.println("      function toSmall() {");
    out.println("        top.frames[3].location.pathname = getPathRoot() + currentView + 'Message.html';");
    out.println("        top.frames[1].location.pathname = getPathRoot() + 'suTreeSmall.html';");
    out.println("        top.frames[2].location.pathname = getPathRoot() + currentView + 'SmallImage.html';");
    out.println("        return true;");
    out.println("      }");
    out.println("      function toMedium() {");
    out.println("        top.frames[3].location.pathname = getPathRoot() + currentView + 'Message.html';");
    out.println("        top.frames[1].location.pathname = getPathRoot() + 'suTreeMedium.html';");
    out.println("        top.frames[2].location.pathname = getPathRoot() + currentView + 'MediumImage.html';");
    out.println("        return true;");
    out.println("      }");
    out.println("      function toLarge() {");
    out.println("        top.frames[3].location.pathname = getPathRoot() + currentView + 'Message.html';");
    out.println("        top.frames[1].location.pathname = getPathRoot() + 'suTreeLarge.html';");
    out.println("        top.frames[2].location.pathname = getPathRoot() + currentView + 'LargeImage.html';");
    out.println("        return true;");
    out.println("      }");
    out.println("      function toJumbo() {");
    out.println("        top.frames[3].location.pathname = getPathRoot() + currentView + 'Message.html';");
    out.println("        top.frames[1].location.pathname = getPathRoot() + 'suTreeJumbo.html';");
    out.println("        top.frames[2].location.pathname = getPathRoot() + currentView + 'JumboImage.html';");
    out.println("        return true;");
    out.println("      }");      
    out.println("    </script>");
    out.println("  </head>");
    out.println("  <body bgcolor=\"#CCCCCC\" >");
    out.println("    <form>");
    out.println("     <input type=\"button\" value=\"Small\" onclick=\"toSmall();\" />");
    out.println("     <input type=\"button\" value=\"Medium\"  onclick=\"toMedium();\" />");
    out.println("     <input type=\"button\" value=\"Large\"  onclick=\"toLarge();\" />");
    out.println("     <input type=\"button\" value=\"Extra Large\"  onclick=\"toJumbo();\" />");      
    out.println("   </form>");
    out.println("  </body>");
    out.println("</html>");
    out.close();
    return;
  }
  
  /***************************************************************************
  **
  ** Generate image wrapper pages
  */
  
  private void writeNotesHTML(NamedOutputStreamSource streamSrc, boolean skipRoot) throws IOException {  
 
    NavTree navTree = dacx_.getGenomeSource().getModelHierarchy();
    List<String> ordered = navTree.getPreorderListing(skipRoot);
    Iterator<String> oit = ordered.iterator();    
    while (oit.hasNext()) {
      String gkey = oit.next();
      Genome currGenome = dacx_.getGenomeSource().getGenome(gkey);
      if (currGenome instanceof GenomeInstance) {
        GenomeInstance gi = (GenomeInstance)currGenome;
        Iterator<Note> noit = gi.getNoteIterator();
        while (noit.hasNext()) {
          Note note = noit.next();
          if (note.isInteractive()) {
            OutputStream stream = streamSrc.getNamedStream(gkey + "Note" + note.getID() + ".html");
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));  
            out.println("<html>");
            out.println("  <body>");
            //out.println("    <font face=\"Arial, Helvetica, sans-serif\" >");
            out.println(CharacterEntityMapper.mapEntities(note.getTextWithBreaksReplaced(), false));
            //out.println("    </font>");
            out.println("  </body>");
            out.println("</html>");    
            out.close();
          }
        }
      }
    }
    return;
  }
}
