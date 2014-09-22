/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.io.PrintWriter;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This helps ouput SBML for a genome
**
** FIX ME?  Delete this class?? No longer used???
*/

public class AlgSbmlSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public AlgSbmlSupport() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Write the genome using SBML
  **
  */
  
  public void writeSBML(PrintWriter out, Indenter ind, Genome genome) {
    ind.indent();
    out.print("<model ");
    out.print("name=\"");
    out.print(CharacterEntityMapper.mapEntities(genome.getName(), false));
    out.println("\" >");
    ind.up().indent();
    out.println("<listOfCompartments>");
    ind.up().indent();    
    out.println("<compartment name=\"A\" />");
    ind.down().indent();
    out.println("</listOfCompartments>");
    writeSpecies(out, ind, "A", genome);
    writeParameters(out, ind, genome);
    writeRules(out, ind, "A", genome);
    writeReactions(out, ind, "A", genome);    
    ind.down().indent();      
    out.println("</model>");
    return;
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Write the species part of SBML
  **
  */
  
  private void writeSpecies(PrintWriter out, Indenter ind, String cmpt, Genome genome) {
    //
    // We now list all species.  Each species corresponds to an occupied pad
    // on a node.  For output pads, we have two species, since the reaction will
    // take place between those two species, unless the output is from a boundary
    // condition.
    //
    ind.indent();
    out.println("<listOfSpecies>");
    
    //
    // Figure out the boundary condition species by seeing which nodes are sources
    // but never targets.
    //
    
    HashSet<String> sourceOnly = new HashSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      sourceOnly.add(lnk.getSource());
    }
    lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      sourceOnly.remove(lnk.getTarget());
    }
    
    StringBuffer buf = new StringBuffer();
    TreeSet<String> sSet = new TreeSet<String>();
    TreeSet<String> bcSet = new TreeSet<String>();
    ind.up();
    lit = genome.getLinkageIterator();
    //
    // Crank through each link and issue the source and the target, unless it
    // has already been issued:
    //
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      String source = lnk.getSource();
      String id = lnk.getSource();
      if (sourceOnly.contains(source)) {
        bcSet.add(buildPostSourceSpecie(id, cmpt, buf, false));
      } else {
        sSet.add(buildSourceSpecie(id, cmpt, buf, false));
        sSet.add(buildPostSourceSpecie(id, cmpt, buf, false));
      }
      sSet.add(buildTargetSpecie(lnk, cmpt, buf, false));
    }
    
    Iterator<String> bcit = bcSet.iterator();
    while (bcit.hasNext()) {
      String specie = bcit.next();
      ind.indent();
      out.print("<specie name=\"");
      out.print(specie);
      out.print("\" compartment=\"");
      out.print(cmpt);
      out.print("\" initialAmount=\"0");
      out.println("\" boundaryCondition=\"true\" />");
    }
    
    Iterator<String> ssit = sSet.iterator();
    while (ssit.hasNext()) {
      String specie = ssit.next();
      ind.indent();
      out.print("<specie name=\"");
      out.print(specie);
      out.print("\" compartment=\"");
      out.print(cmpt);   
      out.println("\" initialAmount=\"0\" />");
    }    
   
    ind.down().indent();      
    out.println("</listOfSpecies>");
    return;
  }

  /***************************************************************************
  **
  ** Write the parameters part of SBML
  **
  */
  
  private void writeParameters(PrintWriter out, Indenter ind, Genome genome) {
    //
    // Boolean outputs need a parameter of 0.5 for each link
    //
    ind.indent();
    out.println("<listOfParameters>");
    
    StringBuffer buf = new StringBuffer();
    TreeSet<String> pSet = new TreeSet<String>();
    ind.up();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    //
    // Crank through each link and issue a parameter for each.  Then
    // crank through nodes and issue the necessary parameters.
    //
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      pSet.add(buildParam(lnk, "p", buf, false));
      pSet.add(buildParam(lnk, "f", buf, false));      
    }
    
    Iterator<String> psit = pSet.iterator();
    while (psit.hasNext()) {
      String param = psit.next();
      ind.indent();
      out.print("<parameter name=\"");
      out.print(param);
      out.println("\" value=\"1\" />");
    }

    ind.down().indent();      
    out.println("</listOfParameters>");
    return;
  }

  /***************************************************************************
  **
  ** Write the rules part of SBML
  **
  */
  
  private void writeRules(PrintWriter out, Indenter ind, String cmpt, Genome genome) {
    ind.indent();
    out.println("<listOfRules>");
    
    StringBuffer buf = new StringBuffer();
    TreeSet<Formula> rSet = new TreeSet<Formula>(new FormulaComparator());
    ind.up();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    //
    // Crank through each link and issue a formula for each.  Then
    // crank through nodes and issue the necessary formula.
    //
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      rSet.add(buildFormula(lnk, buf, cmpt, genome));
    }
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      Formula form = buildFormulaForNode(node, buf, cmpt, genome);
      if (form != null) {
        rSet.add(form);
      }
    }
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      Formula form = buildFormulaForNode(gene, buf, cmpt, genome);
      if (form != null) {
        rSet.add(form);
      }      
    }    

    Iterator<Formula> rsit = rSet.iterator();
    while (rsit.hasNext()) {
      Formula formula = rsit.next();
      ind.indent();
      out.print("<specieConcentrationRule specie=\"");
      out.print(formula.specie);
      out.println("\"");
      ind.up().indent();
      out.print("formula=\"");
      out.print(formula.formula);
      out.println("\" />");
      ind.down();
    }

    ind.down().indent();
    out.println("</listOfRules>");
    return;
  }

  /***************************************************************************
  **
  ** Write the reactions part of SBML
  **
  */
  
  private void writeReactions(PrintWriter out, Indenter ind, String cmpt, Genome genome) {
    ind.indent();
    out.println("<listOfReactions>");

    //
    // Figure out the nodes that need reactions written out.  First calculate the
    // nodes that are only sources, never targets.
    //
    
    HashSet<String> sourceOnly = new HashSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      sourceOnly.add(lnk.getSource());
    }
    lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      sourceOnly.remove(lnk.getTarget());
    }    
    
    //
    // Now figure out which nodes and genes are NOT in the source only 
    // category.  Those are the ones we are writing out.
    // 

    StringBuffer buf = new StringBuffer();
    TreeSet<String> rSet = new TreeSet<String>();
    ind.up();
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();
      if (!sourceOnly.contains(nodeID)) {
        rSet.add(nodeID);
      }
    }
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String geneID = gene.getID();
      if (!sourceOnly.contains(geneID)) {
        rSet.add(geneID);
      }
    }            

    Iterator<String> rsit = rSet.iterator();
    while (rsit.hasNext()) {
      String nodeID = rsit.next();
      String postSrcSpecie = buildPostSourceSpecie(nodeID, cmpt, buf, false);
      String srcSpecie = buildSourceSpecie(nodeID, cmpt, buf, false);
      buf.setLength(0);
      String tauTerm = buf.append("Tau_bt").append(nodeID).toString();
      ind.indent();
      out.print("<reaction name=\"j_");
      out.print(postSrcSpecie);
      out.println("\" reversible=\"true\">");
      ind.up().indent();
      out.println("<listOfReactants>");
      ind.up().indent();    
      out.print("<specieReference specie=\"");      
      out.print(srcSpecie);
      out.println("\"/>");
      ind.down().indent();     
      out.println("</listOfReactants>");
      ind.indent();
      out.println("<listOfProducts>");
      ind.up().indent();    
      out.print("<specieReference specie=\"");
      out.print(postSrcSpecie);
      out.println("\"/>");
      ind.down().indent();    
      out.println("</listOfProducts>");
      ind.indent();    
      out.print("<kineticLaw formula=\"");
      out.print("(");
      out.print(srcSpecie);
      out.print(" - ");
      out.print(postSrcSpecie);      
      out.print(")/");
      out.print(tauTerm);
      out.println("\">");
      ind.up().indent();
      out.println("<listOfParameters>");
      ind.up().indent();    
      out.print("<parameter name=\"");
      out.print("tauTerm");
      out.println("\" value=\"1\" />");
      ind.down().indent();    
      out.println("</listOfParameters>");
      ind.down().indent();    
      out.println("</kineticLaw>");
      ind.down().indent();    
      out.println("</reaction>");
    }
    ind.down().indent();
    out.println("</listOfReactions>");
    return;
  }  

  /***************************************************************************
  **
  ** Write the specie for a link target
  */
  
  private String buildTargetSpecie(Linkage lnk, String cmpt, StringBuffer buf, boolean append) {
    String src = lnk.getTarget();
    int landingPad = lnk.getLandingPad();
    if (!append) {
      buf.setLength(0);
    }
    buf.append("bt");    
    buf.append(src);
    buf.append("_i");
    buf.append(landingPad);
    buf.append('_');
    buf.append(cmpt);
    return ((append) ? null : buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Write the pre-reaction specie for a link source
  */
  
  private String buildSourceSpecie(String id, String cmpt, StringBuffer buf, boolean append) {
    if (!append) {
      buf.setLength(0);
    }
    buf.append("bt");    
    buf.append(id);
    buf.append("_input_");
    buf.append(cmpt);
    return ((append) ? null : buf.toString());
  }  

  /***************************************************************************
  **
  ** Write the post-reaction specie for a link source
  */
  
  private String buildPostSourceSpecie(String id, String cmpt, StringBuffer buf, boolean append) {
    if (!append) {
      buf.setLength(0);
    }
    buf.append("bt");    
    buf.append(id);
    buf.append('_');
    buf.append(cmpt);
    return ((append) ? null : buf.toString());
  } 
  
  /***************************************************************************
  **
  ** Write the post-reaction specie for a link source
  */
  
  private String buildParam(Linkage lnk, String prefix, StringBuffer buf, boolean append) {
    String src = lnk.getSource();
    String trg = lnk.getTarget();
    int landingPad = lnk.getLandingPad();
    if (!append) {
      buf.setLength(0);
    }
    buf.append(prefix);
    buf.append('_');
    buf.append("bt");    
    buf.append(src);
    buf.append('_');
    buf.append("bt");        
    buf.append(trg);
    buf.append("_i");
    buf.append(landingPad);
    return ((append) ? null : buf.toString());
  }  

  /***************************************************************************
  **
  ** Write the formula for a link
  */
  
  private Formula buildFormula(Linkage lnk, StringBuffer buf, String cmpt, Genome genome) {

    String specie = buildTargetSpecie(lnk, cmpt, buf, false);
    
    //
    // What we do depends on whether the target needs normalization?
    //

    buf.setLength(0);
    
    String trg = lnk.getTarget();
    Node node = genome.getNode(trg);
    boolean normalize = (node.getNodeType() == Node.GENE);  // FIX ME!

    int sign = lnk.getSign();
   
    if (!normalize) {  // FIX ME - NOT HANDLING NEGATION ON BUBBLES!
      buildFormulaTerm(lnk, buf, cmpt, true);
    } else if (sign == Linkage.NEGATIVE) {
      buf.append("1 / (1 + "); 
      buildFormulaTerm(lnk, buf, cmpt, true);
      buf.append(")");
    } else {
      buildFormulaTerm(lnk, buf, cmpt, true);
      buf.append(" / (1 + "); 
      buildFormulaTerm(lnk, buf, cmpt, true);
      buf.append(")");
    }
    String formula = buf.toString();
    Formula form = new Formula(specie, formula);   
    return (form);
  }  

  /***************************************************************************
  **
  ** Write the formula for a node
  */
  
  private Formula buildFormulaForNode(Node node, StringBuffer buf, 
                                      String cmpt, Genome genome) {

    String id = node.getID();
    String specie = buildSourceSpecie(id, cmpt, buf, false);
 
    String formula = null;    
    boolean doAnd = (node.getNodeType() == Node.GENE);  // FIX ME!
   
    if (doAnd) {
      buf.setLength(0);      
      HashSet<Integer> seenPads = new HashSet<Integer>();
      boolean first = true;
      // For each active pad:
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage lnk = lit.next();
        String targ = lnk.getTarget();
        if (!targ.equals(id)) {
          continue;
        }
        Integer padObj = new Integer(lnk.getLandingPad());
        if (seenPads.contains(padObj)) {
          continue;
        }
        seenPads.add(padObj);
        if (!first) {
          buf.append(" * ");
        } else {
          first = false;
        }
        buildTargetSpecie(lnk, cmpt, buf, true);
      }
      formula = buf.toString();
    } else {  // FIX ME - NEED TO DEAL WITH MULTIPLE LINKS TO 1 PAD
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage lnk = lit.next();
        String targ = lnk.getTarget();
        if (!targ.equals(id)) {
          continue;
        }
        formula = buildTargetSpecie(lnk, cmpt, buf, false);
        break;
      } 
    }  

    return ((formula == null) ? null : new Formula(specie, formula));
  }  
  
  /***************************************************************************
  **
  ** Write the formula term for a link
  */
  
  private String buildFormulaTerm(Linkage lnk, StringBuffer buf, String cmpt, boolean append) {

    String src = lnk.getSource();
    if (!append) {
      buf.setLength(0);
    }
    buildParam(lnk, "f", buf, true);
    buf.append(" * ");
    buildPostSourceSpecie(src, cmpt, buf, true);
    buf.append('^');
    buildParam(lnk, "p", buf, true);    
    return ((append) ? null : buf.toString());
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Holds string/formula pair for SBML output
  */
  
  class Formula {
    String specie;
    String formula;
    
    Formula(String specie, String formula) {
      this.specie = specie;
      this.formula = formula;
    }
    
    public int hashCode() {
      return (specie.hashCode());
    }  
 
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof Formula)) {
        return (false);
      }
      Formula otherF = (Formula)other;

      boolean matchS = (this.specie == null) ? (otherF.specie == null)
                                             : (this.specie.equals(otherF.specie));
      if (!matchS) {
        return (false);
      }
          
      boolean matchF = (this.formula == null) ? (otherF.formula == null)
                                              : (this.formula.equals(otherF.formula));   
    
      return (matchF);
    }
  }
  
  /***************************************************************************
  **
  ** Sorts formulas
  **
  */
  
  class FormulaComparator implements Comparator<Formula> {
    public int compare(Formula form1, Formula form2) {     
      int spcCompare = (form1.specie.compareTo(form2.specie));
      if (spcCompare == 0) {
        return (form1.formula.compareTo(form2.formula));
      } else {
        return (spcCompare); 
      }
    }
  } 
}
