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


package org.systemsbiology.biotapestry.qpcr;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureProps;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Publishes QPCR table
*/

class QpcrTablePublisher {
   
   static final String STYLE = "style=\"margin:0in;margin-bottom:.0001pt;font-size:11.0pt;\"";
   static final String PSTYLE = "<p " + STYLE + ">";
   static final String PSTYLE_CENTER = "<p " + STYLE + " align=\"center\" >";
  
   static final String STYLEBIG = "style=\"margin:0in;margin-bottom:.0001pt;font-size:30.0pt;\"";
   static final String PSTYLEBIG = "<p " + STYLEBIG + ">"; 
   static final String PSTYLEBIG_CENTER = "<p " + STYLEBIG + " align=\"center\" >";

   static final String BLANK_FOR_CSS = "margin:0in;font-size:1.0pt;";
   static final String BLANK_FOR_CSS_TAG = "<span class=\"Breaker\"> </span>";
   static final String BLANK_FOR_NO_CSS = "<font size=\"1\"> </font>";

   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////   
      
  private PrintWriter out_; 
  private boolean noCss_;
  private boolean bigScreen_;
  private Map<String, String> spanColors_;
  private String scaleKey_;
  private TabPinnedDynamicDataAccessContext ddacx_;
       
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////   
     
  /***************************************************************************
  ** 
  ** For exports
  */

  QpcrTablePublisher(TabPinnedDynamicDataAccessContext ddacx, Map<String, String> colors) {
    ddacx_ = ddacx;
    out_ = null;
    noCss_ = false;
    bigScreen_ = false;
    spanColors_ = colors;
    DisplayOptions dOpt = ddacx_.getDisplayOptsSource().getDisplayOptions();
    scaleKey_ = dOpt.getPerturbDataDisplayScaleKey();
  }
   
  /***************************************************************************
  ** 
  ** For experimental data display
  */

  @SuppressWarnings("unused")
  QpcrTablePublisher(TabPinnedDynamicDataAccessContext ddacx, boolean bigScreen, Map<String, String> colors) { 
    ddacx_ = ddacx;
    out_ = null;
    noCss_ = true;
    bigScreen_ = false;
    spanColors_ = colors;
    DisplayOptions dOpt = ddacx_.getDisplayOptsSource().getDisplayOptions();
    scaleKey_ = dOpt.getPerturbDataDisplayScaleKey();
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Set output stream
  */

  void setOutput(PrintWriter out) { 
    out_ = out;
    return;
  }
   
  /***************************************************************************
  ** 
  ** For export
  */

  boolean publish(PrintWriter out, QPCRData qpcr) { 
    out_ = out;
    Indenter ind = new Indenter(out_, Indenter.DEFAULT_INDENT); 
    ind.indent();
    out_.write("<html>\n");
    ind.up().indent();
    out_.write("<body>\n");
    ind.up().indent();
    out_.write("<style type=\"text/css\">\n");
    out_.write("/*<![CDATA[*/\n");
    out_.write("<!--\n");
    out_.write("p.Entry\n");
    out_.write("    {margin:0in;\n");
    out_.write("    margin-bottom:.0001pt;\n");
    out_.write("    font-size:10.0pt;}\n");
    out_.write("span.Breaker\n{");
    out_.write(BLANK_FOR_CSS);
    out_.write("}\n");
    Iterator<String> spit = spanColors_.values().iterator();
    while (spit.hasNext()) {
      String spanCol = spit.next();
      out_.write("span.Col");
      out_.write(spanCol);
      out_.write("\n");
      out_.write("    {margin:0in;color:");
      out_.write(spanCol);
      out_.write(";}\n");
    }
    out_.write("-->\n");
    out_.write("/*]]>*/\n");
    ind.down().indent();    
    out_.write("</style>\n");    
    qpcr.writeHTML(out_, ind, this, ddacx_);
    ind.down().indent();
    out_.write("</body>\n");
    ind.down().indent();
    out_.write("</html>\n");
    out_.flush();
    out_.close();
    return (true);
  } 

  /***************************************************************************
  ** 
  ** Paragraph tag
  */

  void paragraph(boolean centered) {     
    if (noCss_) {
      String use;
      if (bigScreen_) {
        if (centered) {
          use = PSTYLEBIG_CENTER;
        } else {
          use = PSTYLEBIG;
        }
      } else {      
        if (centered) {
          use = PSTYLE_CENTER;
        } else {
          use = PSTYLE;
        }          
      }
      out_.print(use);
    } else {
      if (centered) {
        out_.print("<p class=\"Entry\" align=\"center\">");
      } else {
        out_.print("<p class=\"Entry\">");
      }
    }
    return;
  }
 
  /***************************************************************************
  ** 
  ** Print a breaking space
  */

  void breakSpace() { 
    out_.print((noCss_) ? BLANK_FOR_NO_CSS : BLANK_FOR_CSS_TAG);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Print a color start
  */

  void openColor(String color) {   
    out_.print(openColorStr(color, new StringBuffer()));
    return;
  }
  
  /***************************************************************************
  ** 
  ** Print a color end
  */

  void closeColor() { 
    out_.print(closeColorStr());
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Print a color start
  */

  String openColorStr(String color, StringBuffer buf) {
    buf.setLength(0);
    if (noCss_) {
      buf.append("<font color=\"");
      buf.append(color);     
      buf.append("\">");
    } else {
      buf.append("<span class=\"Col");
      buf.append(color);
      buf.append("\">");
    }
    return (buf.toString());
  }
  
  /***************************************************************************
  ** 
  ** Get a color end
  */

  String closeColorStr() { 
    if (noCss_) {
      return ("</font>");
    } else {
      return ("</span>");
    }
  } 
    
  /***************************************************************************
  ** 
  ** Describe colors and scaling
  */

  void colorsAndScaling() { 
    PerturbationData pd = ddacx_.getExpDataSrc().getPertData();
    MeasureDictionary md = pd.getMeasureDictionary();
    ResourceManager rMan = ddacx_.getRMan();
    String colorNote = rMan.getString("qpcrData.colorKeyFmt"); 
    
    
    String scaleNote = rMan.getString("qpcrData.scaleKeyFmt");
      
    out_.print("<center><p>");  
    String useScale = md.getMeasureScale(scaleKey_).getName();
    String note = MessageFormat.format(scaleNote, new Object[] {useScale});
    out_.print(note);
    out_.println("</p></center>");
    
    Object[] str = new Object[7];
    StringBuffer buf = new StringBuffer();
    
    out_.println("<ul>");
    Iterator<String> cit = spanColors_.keySet().iterator();
    while (cit.hasNext()) {
      out_.print("<li>");
      String measureKey = cit.next();
      MeasureProps mp = md.getMeasureProps(measureKey);
      MeasureScale ms = md.getMeasureScale(mp.getScaleKey());
      String col = spanColors_.get(measureKey);
      str[0] = mp.getName();
      str[1] = openColorStr(col, buf);
      str[2] = col;
      str[3] = closeColorStr();
      str[4] = ms.getName(); 
      str[5] = UiUtil.doubleFormat(mp.getNegThresh().doubleValue(), false);
      str[6] = UiUtil.doubleFormat(mp.getPosThresh().doubleValue(), false);
      String cnote = MessageFormat.format(colorNote, str);
      out_.print(cnote);
      out_.println("</li>");
    }
    out_.println("</ul>");
    return;
  }
  
  
  /***************************************************************************
  **
  ** Do number prefix
  **
  */
  
  void valueSignPrefix(String value) {
    if (scaleKey_.equals("0")) {
      return;
    }
    if (Character.isDigit(value.charAt(0))) {
      out_.print("+");
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Do perturbation header
  **
  */
  
  void writePerturbationHeader(Indenter ind, NullTimeSpan span) {   
    ind.indent();
    out_.println("<p></p>");
    ind.indent();    
    out_.println("<table border=\"0\">");
    ind.up().indent();    
    out_.println("<tr valign=\"top\">");    
    ind.up().indent();    
    out_.println("<td width=\"120\" height=\"22\">");
    ind.up().indent();
    paragraph(false);  
    out_.println("<b>Perturbation:</b></p>");
    ind.down().indent();
    out_.println("</td>");
    ind.indent();  
    out_.println("<td>");
    ind.up().indent();
    ResourceManager rMan = ddacx_.getRMan();
    String tabNote = rMan.getString("qpcrData.nullTableNote");    
    
    MinMax tc = new MinMax(span.getMin(), span.getMax());
    String tdisp = TimeSpan.spanToString(ddacx_, tc);
    
    if (!span.isASpan()) {
      StringBuffer spanBuf = new StringBuffer();
      String subNoteSingle = rMan.getString("qpcrData.nullTableNoteSingle");
      spanBuf.append(subNoteSingle);
      spanBuf.append(" ");      
      spanBuf.append(tdisp);
      tdisp = spanBuf.toString();
    } 
  
    String note = MessageFormat.format(tabNote, new Object[] {tdisp});
    paragraph(false);
    out_.print("<b>");
    out_.print(note);
    out_.println("</b></p>");
    ind.down().indent();
    out_.println("</td>");    
    ind.down().indent();    
    out_.println("</tr>");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
        
}