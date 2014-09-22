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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.HashSet;

import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This class supports SIF output
*/

public class SifSupport {
  
  public static final String NEGATIVE_STR_EXP = "REPRESSES";
  public static final String NONE_STR_EXP     = "REGULATES";
  public static final String POSITIVE_STR_EXP = "PROMOTES";  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private DBGenome genome_;
  private GenomeInstance gi_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Basic constructor
  */

  public SifSupport(DBGenome genome) {
    genome_ = genome;
  }
  
  /***************************************************************************
  **
  ** Basic constructor
  */

  public SifSupport(GenomeInstance gi) {
    gi_ = gi;
  }
  
  /***************************************************************************
  **
  ** Write the genome as SIF:
  **
  */
  
  public void writeSIF(PrintWriter out) {
    Genome useGenome = (genome_ == null) ? (Genome)gi_ : (Genome)genome_;
    HashSet<String> seen = new HashSet<String>();
    HashMap<String, String> outputs = new HashMap<String, String>();
    Iterator<Linkage> links = useGenome.getLinkageIterator();
    while (links.hasNext()) {
      Linkage lnk = links.next();
      String src = lnk.getSource();
      String trg = lnk.getTarget();
      int sign = lnk.getSign();
      seen.add(src);
      seen.add(trg);
      writeSIFLink(out, src, trg, sign, outputs, useGenome);
    }
    
    Iterator<Gene> genes = useGenome.getGeneIterator();
    while (genes.hasNext()) {
      Gene g = genes.next();
      if (!seen.contains(g.getID())) {
        out.println(sifNodeName(g, outputs, useGenome));
      }
    }
    Iterator<Node> nodes = useGenome.getNodeIterator();
    while (nodes.hasNext()) {
      Node n = nodes.next();
      String id = n.getID();
      if (!seen.contains(id)) {
        out.println(sifNodeName(n, outputs, useGenome));
      }
    }
    return; 
  }
  
  /***************************************************************************
  **
  ** Write the link to SIF
  ** WJRL 7/25/14: For some bizzare reason (pretty-printing?), the strings were being blank-padded.
  ** This is nuts, and I've ditched that.
  **
  */
  
  private void writeSIFLink(PrintWriter out, String src, String trg, int sign, Map<String, String> outputs, Genome useGenome) {
    String srcName = sifNodeName(useGenome.getNode(src), outputs, useGenome);
    String trgName = sifNodeName(useGenome.getNode(trg), outputs, useGenome);
    out.print(srcName);
    out.print("\t");
    String edge = null;    
    if (sign == Linkage.POSITIVE) {
      edge = POSITIVE_STR_EXP;
    } else if (sign == Linkage.NEGATIVE) {
      edge = NEGATIVE_STR_EXP;
    } else if (sign == Linkage.NONE) {
      edge = NONE_STR_EXP;
    }
    out.print(edge);   
    out.print("\t");
    out.println(trgName);    
    return;
  } 

  /***************************************************************************
  **
  ** Generate sif node name
  **
  */
  
  private String sifNodeName(Node node, Map<String, String> outputs, Genome useGenome) {
    String groupPrefix = (useGenome == gi_) ? sifRegionPrefix((NodeInstance)node, gi_) : "";
    String activeSuffix = (useGenome == gi_) ? sifActivitySuffix((NodeInstance)node, gi_) : "";      
    
    if (node.getNodeType() == Node.GENE) {
      String retval = padForUnique(node, groupPrefix + node.getName() + activeSuffix, outputs);
      outputs.put(node.getID(), retval);
      return (retval);
    } else {
      String name = node.getName();
      String prefix;
      if ((name == null) || name.trim().equals("")) {
        prefix = "(" + DBNode.mapToTag(node.getNodeType()) + ")";
      } else {
        prefix = name;
      }
      String retval = padForUnique(node, groupPrefix + prefix + "." + node.getID() + activeSuffix, outputs);
      outputs.put(node.getID(), retval);
      return (retval);
    }
  }
  
  /***************************************************************************
  **
  ** Generate sif region prefix
  **
  */

  private String sifRegionPrefix(NodeInstance node, GenomeInstance gi) {  
    GroupMembership memb = gi.getNodeGroupMembership(node);
    if (memb.mainGroups.size() == 0) {
      return ("[--no region--" + node.getID() + "]:"); 
    } else if (memb.mainGroups.size() != 1) {
      throw new IllegalStateException();
    }
    String parent = memb.mainGroups.iterator().next(); 
    Group parentGroup = gi.getGroup(parent);
    String activeSub = parentGroup.getActiveSubset();
 
    if (activeSub != null) {
      if (memb.subGroups.size() == 0) {
        return ("[--no region--" + node.getID() + "]:"); 
      }
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      Iterator<String> sit = memb.subGroups.iterator();
      while (sit.hasNext()) {
        String subID = sit.next();
        Group subGroup = gi.getGroup(subID);
        buf.append(subGroup.getInheritedDisplayName(gi));
        if (sit.hasNext()) {
          buf.append(":");
        }
      }
      buf.append("]:");
      return (buf.toString());
    }
    return ("[" + parentGroup.getInheritedDisplayName(gi) + "]:");      
  }
  
 /***************************************************************************
  **
  ** Generate sif region prefix
  **
  */

  private String sifActivitySuffix(NodeInstance node, GenomeInstance gi) {  
    int activity = node.getActivity();
    if (activity == NodeInstance.ACTIVE) {
      return ("");
    }
    String activTag = NodeInstance.mapActivityTypes(activity);
    return ("{" + activTag + "}");
  }
 
  /***************************************************************************
  **
  ** Insurance against namespace collisions
  */
  
  private String padForUnique(Node node, String name, Map<String, String> outputs) {
    String retval = outputs.get(node.getID());
    if (retval == null) {
      retval = name;
      while (DataUtil.containsKey(outputs.values(), retval)) {
        retval = "_" + retval;
      }
    }
    return (retval);
  }
}
