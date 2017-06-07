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

package org.systemsbiology.biotapestry.genome;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;

/****************************************************************************
**
** This represents a linkage in BioTapestry
*/

public class DBLinkage extends DBGenomeItem implements Linkage {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String src_;
  private String targ_;
  private int sign_;
  private int pad_;
  private String signTag_;
  private int lpad_;
  private int tnote_;
  private String starg_;
  
  private String description_;
  private ArrayList<String> urls_;
          
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public DBLinkage(DBLinkage other) {
    super(other);
    this.signTag_ = other.signTag_;
    this.sign_ = other.sign_;
    this.tnote_ = other.tnote_;
    this.starg_ = other.starg_;
    this.src_ = other.src_;
    this.targ_ = other.targ_;
    this.pad_ = other.pad_;
    this.lpad_ = other.lpad_;   
    this.description_ = other.description_;
    this.urls_ = new ArrayList<String>(other.urls_);
  }
  
  /***************************************************************************
  **
  ** UI-based contructor
  */

  public DBLinkage(DataAccessContext dacx, String name, String id, String src, String targ, 
                   int sign, int pad, int lpad) {   
    super(dacx, name, id);
    if ((src == null) || (targ == null)) {
      throw new IllegalArgumentException();
    }
    src_ = src;
    targ_ = targ;
    sign_ = sign;
    if (sign_ == NEGATIVE) {
      signTag_ = "-";
    } else if (sign_ == POSITIVE) {
      signTag_ = "+";
    } else {
      signTag_ = null;
    }
    pad_ = pad;
    lpad_ = lpad;
    tnote_ = LEVEL_NONE;
    starg_ = null;
    
    description_ = null;
    urls_ = new ArrayList<String>();
  }
  
  /***************************************************************************
  **
  ** XML-based contructor
  */

  public DBLinkage(DataAccessContext dacx, String name, String id, String src, String targ, 
                   String sign, String pad, String lpad, String tnote, 
                   String starg) throws IOException {   
    super(dacx, name, id);
    if ((src == null) || (targ == null)) {
      throw new IOException();
    }
    src_ = src.trim();
    targ_ = targ.trim();
    if (sign != null) {
      sign = sign.trim();
      if (sign.equals("+")) {
        sign_ = POSITIVE;
      } else if (sign.equals("-")) {
        sign_ = NEGATIVE;
      } else if (sign.equals("")) {
        sign_ = NONE;
      } else {
        throw new IOException();
      }
    } else {
      sign_ = NONE;
    }
    signTag_ = sign;
    
    pad_ = 0;
    if (pad != null) {
      try {
        pad_ = Integer.parseInt(pad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    lpad_ = 0;
    if (lpad != null) {
      try {
        lpad_ = Integer.parseInt(lpad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    
    tnote_ = LEVEL_NONE;
    if (tnote != null) {
      try {
        tnote_ = Integer.parseInt(tnote);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
      if (tnote_ > MAX_LEVEL) {
         throw new IOException();
      }
    }
    
    starg_ = starg;
    
    description_ = null;
    urls_ = new ArrayList<String>();
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

  public DBLinkage clone() {
    DBLinkage retval = (DBLinkage)super.clone();
    retval.urls_ = new ArrayList<String>(this.urls_);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the source
  ** 
  */
  
  public String getSource() {
    return (src_);
  }
  
  /***************************************************************************
  **
  ** Set the source
  ** 
  */
  
  public void setSource(String src) {
    src_ = src;
    return;
  }    
  
  /***************************************************************************
  **
  ** Set the target
  ** 
  */
  
  public void setTarget(String targ) {
    targ_ = targ;
    return;
  }      
  
  /***************************************************************************
  **
  ** Get the target
  ** 
  */
  
  public String getTarget() {
    return (targ_);
  }
  
  /***************************************************************************
  **
  ** Get the sign
  ** 
  */
  
  public int getSign() {
    return (sign_);
  }
  
  /***************************************************************************
  **
  ** Get the landing pad
  */
  
  public int getLandingPad() {
    return (pad_);
  }

  /***************************************************************************
  **
  ** Get the launch pad
  */
  
  public int getLaunchPad() {
    return (lpad_);
  }
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLandingPad(int landingPad) {
    pad_ = landingPad;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the landing pad
  */
  
  public void setLaunchPad(int launchPad) {
    lpad_ = launchPad;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the target experimental verification
  */
  
  public int getTargetLevel() {
    return (tnote_);
  }
  
  /***************************************************************************
  **
  ** Set the link sign
  */
  
  public void setSign(int newSign) {
    sign_ = newSign;
    if (sign_ == NEGATIVE) {
      signTag_ = "-";
    } else if (sign_ == POSITIVE) {
      signTag_ = "+";
    } else {
      signTag_ = null;
    }    
    return; 
  }
  
  /***************************************************************************
  **
  ** Set the target experimental verification
  */
  
  public void setTargetLevel(int newTarget) {
    tnote_ = newTarget;
    return;
  }

  /***************************************************************************
  **
  ** Get the url iterator
  **
  */
  
  public Iterator<String> getURLs() {
    return (urls_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the url count
  **
  */
  
  public int getURLCount() {
    return (urls_.size());
  }      
 
  /***************************************************************************
  **
  ** Add a URL (I/O usage)
  **
  */
  
  public void addUrl(String url) {
    urls_.add(url);
    return;
  }    
  
  /***************************************************************************
  **
  ** Set all URLs
  **
  */
  
  public void setAllUrls(List<String> urls) {
    urls_.clear();
    urls_.addAll(urls);
    return;
  }      
  
  /***************************************************************************
  **
  ** Get the description
  **
  */
  
  public String getDescription() {
    return (description_);
  }
  
  /***************************************************************************
  **
  ** Set the description
  **
  */
  
  public void setDescription(String description) {
    description_ = description;    
    return;
  }  

  /***************************************************************************
  **
  ** Append to the description
  **
  */
  
  public void appendDescription(String description) {
    if (description_ == null) {
      description_ = description;
    } else {
      description_ = description_.concat(description);
    }
    description_ = CharacterEntityMapper.unmapEntities(description_, false);    
    return;
  }

  /***************************************************************************
  **
  ** Get display string
  */

  public String getDisplayString(Genome genome, boolean typePreface) {  
    Node srcNode = genome.getNode(src_);
    Node trgNode = genome.getNode(targ_);
    String format = mapSignToDisplay(dacx_, sign_);
    String linkMsg = MessageFormat.format(format, new Object[] {srcNode.getDisplayString(genome, typePreface), 
                                                                trgNode.getDisplayString(genome, typePreface)});
    return (linkMsg);
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("DBLinkage: name = " + name_ + " id = " + id_ + " source = " + src_ + 
            " target = " + targ_ + " sign = " + sign_ + " pad = " + pad_ + " lpad = " + lpad_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<link");
    out.print(" label=\"");
    out.print((name_ == null) ? "" : CharacterEntityMapper.mapEntities(name_, false));    
    out.print("\" src=\"");
    out.print(src_);
    out.print("\" targ=\"");
    out.print(targ_);
    if (signTag_ != null) {
      out.print("\" sign=\"");
      out.print(signTag_);
    }
    out.print("\" id=\"");
    out.print(id_);
    out.print("\" targPad=\"");
    out.print(pad_);
    
    if (lpad_ != 0) {
      out.print("\" launchPad=\"");
      out.print(lpad_);
    }
    
    if (tnote_ != 0) {
      out.print("\" targNote=\"");
      out.print(tnote_);
    }
    
    if (starg_ != null) {
      out.print("\" subTarg=\"");
      out.print(starg_);
    }
    
    if (!urls_.isEmpty() || (description_ != null)) {
      out.println("\" >");
      (new CommonGenomeItemCode()).writeDescUrlToXML(out, ind, urls_, description_, "link");
      ind.indent(); 
      out.println("</link>");   
    } else {
      out.println("\" />");
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Linkage creation
  **
  */
  
  public static DBLinkage buildFromXML(DataAccessContext dacx,
                                       Attributes attrs) throws IOException {
    String name = null;
    String id = null;
    String src = null;
    String targ = null;
    String sign = null;
    String pad = null;
    String lpad = null;
    String tnote = null;
    String starg = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("label")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("id")) {
          id = val;
        } else if (key.equals("src")) {
          src = val;
        } else if (key.equals("targ")) {
          targ = val;
        } else if (key.equals("sign")) {
          sign = val;
        } else if (key.equals("targPad")) {
          pad = val;          
        } else if (key.equals("launchPad")) {
          lpad = val;          
        } else if (key.equals("targNote")) {
          tnote = val;          
        } else if (key.equals("subTarg")) {
          starg = val;          
        }
      }
    }
    
    return (new DBLinkage(dacx, name, id, src, targ, sign, pad, lpad, tnote, starg));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("link");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the description keyword
  **
  */
  
  public static String descriptionKeyword() {
    return ("link" + CommonGenomeItemCode.FT_TAG_ROOT);
  }
  
  /***************************************************************************
  **
  ** Return the URL keyword
  **
  */
  
  public static String urlKeyword() {
    return ("link" + CommonGenomeItemCode.URL_TAG_ROOT);
  }  
  
  /***************************************************************************
  **
  ** Return possible sign values
  */
  
  public static Vector<ChoiceContent> getSignChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    retval.add(signForCombo(dacx, POSITIVE));
    retval.add(signForCombo(dacx, NEGATIVE)); 
    retval.add(signForCombo(dacx, NONE));     
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent signForCombo(DataAccessContext dacx, int sign) {
    return (new ChoiceContent(dacx.getRMan().getString("lcreate." + mapToSignTag(sign)), sign));
  }  

  /***************************************************************************
  **
  ** Get link signs
  **
  */
   
  public static Set<String> linkSigns() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(NEGATIVE_STR);
    retval.add(NONE_STR);
    retval.add(POSITIVE_STR); 
    return (retval);
  }  

  /***************************************************************************
  **
  ** Map link signs
  */

  public static String mapSignToDisplay(DataAccessContext dacx, int val) {
    String signTag = mapToSignTag(val);
    return (dacx.getRMan().getString("linkage." + signTag + "Format"));
  }  
 
  /***************************************************************************
  **
  ** Map link signs
  */

  public static String mapToSignTag(int val) {
    switch (val) {
      case NEGATIVE:
        return (NEGATIVE_STR);
      case NONE:
        return (NONE_STR);
      case POSITIVE:
        return (POSITIVE_STR);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Invert link signs
  */

  public static int invertSign(int val) {
    switch (val) {
      case NEGATIVE:
        return (POSITIVE);
      case NONE:
        return (NONE);
      case POSITIVE:
        return (NEGATIVE);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map link signs to values
  */

  public static int mapFromSignTag(String tag) {
    if (tag.equals(NEGATIVE_STR)) {
      return (NEGATIVE);
    } else if (tag.equals(NONE_STR)) {
      return (NONE);
    } else if (tag.equals(POSITIVE_STR)) {
      return (POSITIVE);
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Return possible evidence choices
  */
  
  public static Vector<ChoiceContent> getEvidenceChoices(DataAccessContext dacx) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i <= MAX_LEVEL; i++) {
      retval.add(evidenceTypeForCombo(dacx, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent evidenceTypeForCombo(DataAccessContext dacx, int eviLev) {
    return (new ChoiceContent(dacx.getRMan().getString("lprop." + mapToEvidenceTag(eviLev)), eviLev));
  }

  /***************************************************************************
  **
  ** Evidence levels
  **
  */

  public static List<String> linkEvidence() {
    ArrayList<String> retval = new ArrayList<String>();
    retval.add(LEV_NONE_STR);
    retval.add(LEV_1_STR);
    retval.add(LEV_2_STR);
    retval.add(LEV_3_STR);   
    retval.add(LEV_4_STR);   
    retval.add(LEV_5_STR);   
    retval.add(LEV_6_STR);   
    retval.add(LEV_7_STR);   
    retval.add(LEV_8_STR);   
    retval.add(LEV_9_STR);   
    retval.add(LEV_10_STR);
    return (retval);
  }  
 
  /***************************************************************************
  **
  ** Map evidence values
  */

  public static String mapToEvidenceTag(int val) {
    switch (val) {
      case LEVEL_NONE:
        return (LEV_NONE_STR);
      case LEVEL_1:
        return (LEV_1_STR);
      case LEVEL_2:
        return (LEV_2_STR);
      case LEVEL_3:
        return (LEV_3_STR);
      case LEVEL_4:
        return (LEV_4_STR);
      case LEVEL_5:
        return (LEV_5_STR);
      case LEVEL_6:
        return (LEV_6_STR);
      case LEVEL_7:
        return (LEV_7_STR);
      case LEVEL_8:
        return (LEV_8_STR);
      case LEVEL_9:
        return (LEV_9_STR);
      case LEVEL_10:
        return (LEV_10_STR);        
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map evidence strings to values
  */

  public static int mapFromEvidenceTag(String tag) {
    if (tag.equals(LEV_NONE_STR)) {
      return (LEVEL_NONE);
    } else if (tag.equals(LEV_1_STR)) {
      return (LEVEL_1);
    } else if (tag.equals(LEV_2_STR)) {
      return (LEVEL_2);
    } else if (tag.equals(LEV_3_STR)) {
      return (LEVEL_3);    
    } else if (tag.equals(LEV_4_STR)) {
      return (LEVEL_4);    
    } else if (tag.equals(LEV_5_STR)) {
      return (LEVEL_5);    
    } else if (tag.equals(LEV_6_STR)) {
      return (LEVEL_6);    
    } else if (tag.equals(LEV_7_STR)) {
      return (LEVEL_7);    
    } else if (tag.equals(LEV_8_STR)) {
      return (LEVEL_8);    
    } else if (tag.equals(LEV_9_STR)) {
      return (LEVEL_9);    
    } else if (tag.equals(LEV_10_STR)) {
      return (LEVEL_10);          
    } else {
      throw new IllegalArgumentException();
    }
  }  
}
