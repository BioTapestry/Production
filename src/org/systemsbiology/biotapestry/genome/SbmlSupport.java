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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;

import org.systemsbiology.biotapestry.db.SimParamSource;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;


/****************************************************************************
**
** This helps ouput SBML for a genome
*/

public class SbmlSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  private SimParamSource sps_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Not-null constructor
  */

  public SbmlSupport(SimParamSource sps) {
    sps_ = sps;
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
  
  public void writeSBML(StringBuffer buf, Indenter ind, Genome genome) {
    ind.indent();
    buf.append("<model ");
    buf.append("name=\"");
    String modelName = genome.getName().replaceAll(" ", "_");

    buf.append(CharacterEntityMapper.mapEntities(modelName, false));
    buf.append("\" >\n");
    ind.up().indent();
    buf.append("<listOfUnitDefinitions>\n");
    ind.up().indent();    
    buf.append("<unitDefinition name=\"volume\">\n");
    ind.up().indent();
    buf.append("<listOfUnits>\n");
    ind.up().indent();
    buf.append("<unit kind=\"dimensionless\"/>\n");
    ind.down().indent(); 
    buf.append("</listOfUnits>\n");
    ind.down().indent();  
    buf.append("</unitDefinition>\n");
    ind.indent();
    buf.append("<unitDefinition name=\"substance\">\n");
    ind.up().indent();
    buf.append("<listOfUnits>\n");
    ind.up().indent();
    buf.append("<unit kind=\"item\"/>\n");
    ind.down().indent();
    buf.append("</listOfUnits>\n");
    ind.down().indent();
    buf.append("</unitDefinition>\n");
    ind.down().indent();
    buf.append("</listOfUnitDefinitions>\n");
    ind.indent();
    buf.append("<listOfCompartments>\n");
    ind.up().indent();    
    buf.append("<compartment name=\"A\" />\n");
    ind.down().indent();
    buf.append("</listOfCompartments>\n");
    HashMap<String, UniqueTracker> tracker = new HashMap<String, UniqueTracker>(); 
    HashMap<String, String> uniqueNames = new HashMap<String, String>();
    writeSpecies(buf, ind, "A", genome, tracker, uniqueNames);
    //
    // Do rules first and stash to get parameters
    //
    StringBuffer localBuf = new StringBuffer();
    HashSet<String> set = new HashSet<String>();
    Indenter bufIndent = new Indenter(localBuf, ind.getIndent());
    bufIndent.setCurrLevel(ind.getCurrLevel());
    writeRules(localBuf, bufIndent, "A", set, genome, tracker, uniqueNames);
    ind.setCurrLevel(bufIndent.getCurrLevel());
    //Set<String> missedParams = 
    writeParameters(buf, ind, "A", set, genome);
    //if (!missedParams.isEmpty()) {
    //  System.err.println("Missed! " + missedParams);
    //}
    buf.append(localBuf);
    writeReactions(buf, ind, "A", genome, tracker, uniqueNames);       
    ind.down().indent();      
    buf.append("</model>\n");
    return;
  }
  
 /***************************************************************************
  **
  ** Get required parameters
  */
  
  public Set<String> requiredGeneParameters(Gene gene, Genome genome) {
    
    HashMap<String, UniqueTracker> tracker = new HashMap<String, UniqueTracker>(); 
    HashMap<String, String> uniqueNames = new HashMap<String, String>();
    StringBuffer buf = new StringBuffer();
    String geneID = gene.getID();
    
    HashSet<String> retval = new HashSet<String>();

    //
    // Always need these:
    //
    
    retval.add("initVal");   // FIX ME    
    buildParam("DN", geneID, buf, retval, false);      
    buildParam("IM", geneID, buf, retval, false);     
    buildParam("KD", geneID, buf, retval, false);       
 
    //
    // Figure out the inputs:
    //
    
    SortedSet<String> inputs = new TreeSet<String>();
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getTarget().equals(geneID)) {
        inputs.add(lnk.getSource());
      }
    }
    int n = inputs.size();
    
    if (n == 0) {
      return (retval);
    }
    
    for (int m = 1; m <= n; m++) {
      buildParam("DN", geneID, buf, retval, false);
      long termCount = binomialCoefficient(n, m); 
      for (int j = 0; j < termCount; j++) {
        SortedSet<String> subset = nChooseM(inputs, m, j);
        if (subset.size() > 1) {
          buildCoop("KQ", subset, geneID, buf, retval, false);
        }
        Iterator<String> sit = subset.iterator();
        while (sit.hasNext()) {
          String input = sit.next();          
          buildParamTimesSpecie("KR", geneID, genome.getNode(input), "DONT_CARE", buf, retval, tracker, uniqueNames, false);
        }
      }
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get required parameters
  */
  
  public Set<String> requiredNonGeneParameters(Node node, Genome genome) {
    
    StringBuffer buf = new StringBuffer();
    String nodeID = node.getID();    
    HashSet<String> retval = new HashSet<String>();

    //
    // Always need these:
    //
    
    retval.add("initVal");  

    if(0 == countLinksTerminatingOnNode(node, genome))
    {
        // if the node has no links terminating on it, then it needs a KD for degradation
        buildParam("KD", nodeID, buf, retval, false);       
    }
    else
    {
        // This node has one or more links terminating on it, so it does not need to have a KD
        // (it is modelled as a boundary species governed by a fixed formula, and is not dynamical)

        // However, we need to define a "amplification" parameter for the protein-protein
        // interactions summarized by this node
        buildParam("Kmult", nodeID, buf, retval, false);
    }
    
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  /***************************************************************************
  **
  ** Extract the node ID from the parameter
  */
  
  public static String extractNodeIdFromParam(String param) {
    return (param.substring(param.lastIndexOf('_') + 1));
  }
  
  /***************************************************************************
  **
  ** Extract the base parameter from the parameter
  */
  
  public static String extractBaseIdFromParam(String param) {
    int index = param.lastIndexOf('_');
    return (param.substring(0, (index == -1) ? param.length() : index));    
  }
  
  /***************************************************************************
  **
  ** Extract the root parameter from the parameter
  */
  
  public static String extractRootIdFromParam(String param) {
    int index = param.indexOf('_');
    return (param.substring(0, (index == -1) ? param.length() : index));
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Write the species part of SBML
  **
  ** cmpt            the name of the SBML compartment that the species live in
  */
  
  private void writeSpecies(StringBuffer buf, Indenter ind, String cmpt, Genome genome,
                            HashMap<String, UniqueTracker> tracker, HashMap<String, String> uniqueNames) {
    //
    // We now list all species.  Each species corresponds to an output pad
    // on a node.
    //
    ind.indent();
    buf.append("<listOfSpecies>\n");
    ind.up();

    //
    // Crank through each node and issue the species.
    //
    
    HashMap<String, String> sMap = new HashMap<String, String>();
    HashMap<String, String> initVals = new HashMap<String, String>();
    StringBuffer localBuf = new StringBuffer();
    
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nid = node.getID();
      DBInternalLogic dbil = ((DBNode)node).getInternalLogic();
      if (dbil != null) {
        String initVal = dbil.getSimulationParam(sps_, "initVal", node.getNodeType());
        if (initVal != null) {
          initVals.put(nid, initVal);
        }
      }
      sMap.put(nid, buildSpecie(node, cmpt, localBuf, tracker, uniqueNames, false));
    }
    
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String gid = gene.getID();
      DBInternalLogic dbil = ((DBGene)gene).getInternalLogic();
      if (dbil != null) {
        String initVal = dbil.getSimulationParam(sps_, "initVal", gene.getNodeType());
        if (initVal != null) {
          initVals.put(gid, initVal);
        }
      }
      sMap.put(gid, buildSpecie(gene, cmpt, localBuf, tracker, uniqueNames, false));
    }    
 
    Iterator<String> smit = sMap.keySet().iterator();
    while (smit.hasNext()) {
      String sid = smit.next();
      String initVal = initVals.get(sid);
      ind.indent();
      buf.append("<species name=\"");
      buf.append(sMap.get(sid));
      buf.append("\" compartment=\"");
      buf.append(cmpt);         
      Node node = genome.getNode(sid);

      if(node.getNodeType() == Node.GENE ||
         0 == countLinksTerminatingOnNode(node, genome))
      {
          if (initVal != null) {
              buf.append("\" initialAmount=\"");
              buf.append(initVal);
          } else {
              buf.append("\" initialAmount=\"0");
          }
          buf.append("\" boundaryCondition=\"false\" />\n");
      }
      else
      {
          buf.append("\" boundaryCondition=\"true\" />\n");
      }
    }
    ind.down().indent();      
    buf.append("</listOfSpecies>\n");
    return;
  }

  private int countLinksTerminatingOnNode(Node node, Genome genome)
    {
        int linkCount = 0;
        Iterator<Linkage> linkIt = genome.getLinkageIterator();
        String nodeID = node.getID();
        while(linkIt.hasNext())
        {
            Linkage linkage = linkIt.next();
            String linkTargetID = linkage.getTarget();
            if(linkTargetID.equals(nodeID))
            {
                ++linkCount;
            }
        }
        return(linkCount);
    }

  /***************************************************************************
  **
  ** Write the parameters part of SBML
  **
  */
  
  private Set<String> writeParameters(StringBuffer buf, Indenter ind, String cmpt, 
                                      HashSet<String> dict, Genome genome) {

    ind.indent();
    buf.append("<listOfParameters>\n");    
    TreeSet<String> retval = new TreeSet<String>();
    ind.up();

    //
    // Crank through the needed parameters, gather up the values, and write them out.
    //
    
    Iterator<String> dit = dict.iterator();
    while (dit.hasNext()) {
      String param = dit.next();
      String nodeID = extractNodeIdFromParam(param);
      String paramPrefix = extractBaseIdFromParam(param);      
      DBNode dbn = (DBNode)genome.getNode(nodeID);
      DBInternalLogic dbil = dbn.getInternalLogic();
      if (dbil != null) {
        String val = dbil.getSimulationParam(sps_, paramPrefix, dbn.getNodeType());
        if (val != null) {
          ind.indent();
          buf.append("<parameter name=\"");
          buf.append(param);
          buf.append("\" value=\"");
          buf.append(val);
          buf.append("\" />\n");
        } else {
          retval.add(param);
        }
      } else {
        retval.add(param);
      }   
    }
    
    //
    // Also write out parameters for each parameter rule.
    //
    
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nid = node.getID();

      if(0 == countLinksTerminatingOnNode(node, genome))
      {
          ind.indent();
          buf.append("<parameter name=\"");
          buildParam("DEGRF", nid, cmpt, buf, null, true);
          buf.append("\" />\n");
      }
    }
    
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String gid = gene.getID();
      if (geneHasInputs(gid, genome)) {
        ind.indent();
        buf.append("<parameter name=\"");
        buildParam("Y", gid, cmpt, buf, null, true);
        buf.append("\" />\n");

        ind.indent();
        buf.append("<parameter name=\"");
        buildParam("CRMAS", gid, cmpt, buf, null, true);
        buf.append("\" />\n");

        ind.indent();
        buf.append("<parameter name=\"");
        buildParam("TRNLR", gid, cmpt, buf, null, true);
        buf.append("\" />\n");
      }
      
      ind.indent();
      buf.append("<parameter name=\"");
      buildParam("DEGRF", gid, cmpt, buf, null, true);
      buf.append("\" />\n");
    }
    ind.down().indent();      
    buf.append("</listOfParameters>\n");
    return (retval);
  }

  /***************************************************************************
  **
  ** Determine if gene has inputs
  **
  */
  
  private boolean geneHasInputs(String geneID, Genome genome) {
    
    Iterator<Linkage> lit = genome.getLinkageIterator();
    while (lit.hasNext()) {
      Linkage lnk = lit.next();
      if (lnk.getTarget().equals(geneID)) {
        return (true);
      }
    }
    return (false);
  }
    
  /***************************************************************************
  **
  ** Write the rules part of SBML
  **
  */
  
  private void writeRules(StringBuffer buf, Indenter ind, String cmpt, 
                          HashSet<String> dict, Genome genome, HashMap<String, UniqueTracker> tracker, 
                          HashMap<String, String> uniqueNames) {
                            
    ind.indent();
    buf.append("<listOfRules>\n");    
    ind.up();

    //
    // For each gene, write out the fractional saturation formula:
    //
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      SortedSet<String> inputs = new TreeSet<String>();
      HashMap<String, Boolean> signs = new HashMap<String, Boolean>();
      String geneID = gene.getID();
      Iterator<Linkage> lit = genome.getLinkageIterator();
      while (lit.hasNext()) {
        Linkage lnk = lit.next();
        if (lnk.getTarget().equals(geneID)) {
          String src = lnk.getSource();
          inputs.add(lnk.getSource());
          signs.put(src, Boolean.valueOf(lnk.getSign() == Linkage.NEGATIVE));
        }
      }
      
      if (!inputs.isEmpty()) {
        ind.indent();
        buf.append("<parameterRule name=\"");
        buildParam("Y", geneID, cmpt, buf, null, true);
        buf.append("\" formula=\"");
        DBInternalLogic dbil = ((DBGene)gene).getInternalLogic();
        if (dbil == null) {
          throw new IllegalArgumentException();
        }
        writeFormula(dbil.getFunctionType(), geneID, inputs, signs, cmpt, buf, 
                     dict, tracker, uniqueNames, genome, true);
        buf.append("\" />\n");

        //
        // Write out the activating strength and transcription rate rules:
        //

        ind.indent();      
        buf.append("<parameterRule name=\"");
        buildParam("CRMAS", geneID, cmpt, buf, null, true);      
        buf.append("\"");
        buf.append(" formula=\"");
        buildParam("Y", geneID, cmpt, buf, null, true);
        buf.append(" * ");      
        buildParam("IM", geneID, buf, dict, true);
        buf.append("\" />\n");      

        ind.indent();
        buf.append("<parameterRule name=\"");
        buildParam("TRNLR", geneID, cmpt, buf, null, true);      
        buf.append("\"");
        buf.append(" formula=\"");
        buildParam("CRMAS", geneID, cmpt, buf, null, true);
        //
        // This 2 is a hardwired value for 2 molecules per minute 
        // for rate of mRNA translation!  Make it configurable!
        //
        buf.append(" * 2.0\" />\n");
      }
      
      ind.indent();      
      buf.append("<parameterRule name=\"");
      buildParam("DEGRF", geneID, cmpt, buf, null, true);      
      buf.append("\"");
      buf.append(" formula=\"");
      buildParam("KD", geneID, buf, dict, true);
      buf.append(" * ");      
      buildSpecie(gene, cmpt, buf, tracker, uniqueNames, true);
      buf.append("\" />\n");
    }
    
    //
    // For each node, write out the degredation formula:
    //
    
    HashMap<String, Integer> nodeInputs = new HashMap<String, Integer>();
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
      Node node = nit.next();
      String nodeID = node.getID();

      if(countLinksTerminatingOnNode(node, genome) == 0)
      {
          ind.indent();      
          buf.append("<parameterRule name=\"");
          buildParam("DEGRF", nodeID, cmpt, buf, null, true);      
          buf.append("\"");
          buf.append(" formula=\"");
          buildParam("KD", nodeID, buf, dict, true);
          buf.append(" * ");      
          buildSpecie(node, cmpt, buf, tracker, uniqueNames, true);
          buf.append("\" />\n");       
      }
      else
      {
          DBInternalLogic internalLogic = ((DBNode) node).getInternalLogic();
          int functionType = internalLogic.getFunctionType();
          Iterator<Linkage> lit = genome.getLinkageIterator();
          nodeInputs.clear();
          while(lit.hasNext())
          {
              Linkage lnk = lit.next();
              if(lnk.getTarget().equals(nodeID))
              {
                  // this link is an input for the node
                  int linkSign = lnk.getSign();
                  if(linkSign != Linkage.NONE)
                  {
                      nodeInputs.put(lnk.getSource(), new Integer(linkSign));
                  }
              }
          }

          if(nodeInputs.size() == 0)
          {
              throw new IllegalStateException("aggregating node with no inputs");
          }

          ind.indent();
          buf.append("<speciesConcentrationRule species=\"");
          buildSpecie(node, cmpt, buf, tracker, uniqueNames, true);
          buf.append("\" formula=\"");

          String operand = null;
          switch(functionType)
          {
              case DBInternalLogic.AND_FUNCTION:
                  operand = new String("*");
                  break;
              case DBInternalLogic.OR_FUNCTION:
                  operand = new String("+");
                  break;
              case DBInternalLogic.XOR_FUNCTION:
                  throw new IllegalStateException("illegal state; encountered a aggregating node with an XOR function type");
              default:
                  throw new IllegalStateException("illegal state; encountered a aggregating node with an unknown function type");
                    
          }

          StringBuffer formBuf = new StringBuffer();
          buildParam("Kmult", nodeID, formBuf, dict, true);
          formBuf.append(" * (");
          // iterate through all the inputs for this node
          Set<String> keySet = nodeInputs.keySet();
          LinkedList<String> keyList = new LinkedList<String>(keySet);
          Collections.sort(keyList);
          Iterator<String> inputIter = keyList.iterator();
          boolean firstInput = true;

          while(inputIter.hasNext())
          {
              String inputName = inputIter.next();
              Integer inputTypeObj = nodeInputs.get(inputName);
              int inputType = inputTypeObj.intValue();

              Node inputNode = genome.getNode(inputName);
              String inputSpeciesName = buildSpecie(inputNode, cmpt, new StringBuffer(), tracker, uniqueNames, false);
              if(! firstInput)
              {
                  formBuf.append(operand);
              }
              else
              {
                  firstInput = false;
              }
                    

              switch(inputType)
              {
                  case Linkage.POSITIVE:
                      formBuf.append(" " + inputSpeciesName + " ");
                      break;

                  case Linkage.NEGATIVE:
                      formBuf.append(" 1/(1 + ");
                      formBuf.append(inputSpeciesName);
                      formBuf.append(") ");
                      break;

                  case Linkage.NONE:
                      throw new IllegalStateException("cannot generate SBML for a diagram with a linkage with a state of \"NONE\"");
              }

          }
          formBuf.append(")");
          buf.append(formBuf.toString() + "\" />\n");
      }
    } 

    ind.down().indent();
    buf.append("</listOfRules>\n");
    return;
  }

  /***************************************************************************
  **
  ** Write the reactions part of SBML
  **
  */
  
  private void writeReactions(StringBuffer buf, Indenter ind, String cmpt, Genome genome, 
                              HashMap<String, UniqueTracker> tracker, HashMap<String, String> uniqueNames) {
    
    ind.indent();
    buf.append("<listOfReactions>\n");
    ind.up();
    
    //
    // Every gene WITH INPUTS gets a translation reaction
    //  
 
    Iterator<Gene> git = genome.getGeneIterator();
    while (git.hasNext()) {
      Gene gene = git.next();
      String geneID = gene.getID();
      if (geneHasInputs(geneID, genome)) {
        ind.indent();
        buf.append("<reaction name=\"");
        buildParam("REAC", geneID, cmpt, buf, null, true);
        buf.append("\" reversible=\"false\">\n");
        ind.up();
//        ind.indent();
//        buf.append("<listOfReactants />\n");
        ind.indent();
        buf.append("<listOfProducts>\n");
        ind.up().indent();    
        buf.append("<speciesReference species=\"");
        buildSpecie(gene, cmpt, buf, tracker, uniqueNames, true);
        buf.append("\"/>\n");
        ind.down().indent();    
        buf.append("</listOfProducts>\n");
        ind.indent();    
        buf.append("<kineticLaw formula=\"");
        buildParam("TRNLR", geneID, cmpt, buf, null, true);
        buf.append("\" />\n");
        ind.down().indent();    
        buf.append("</reaction>\n");
      }
    }
    
    //
    // Every specie degrades:
    //

    HashSet<String> ids = new HashSet<String>();
    Iterator<Node> nit = genome.getNodeIterator();
    while (nit.hasNext()) {
        Node node = nit.next();
        //int nodeType = node.getNodeType();
        // SR:  aggregating nodes will be defined by a parameter rule,
        // so they are not dynamical; therefore, they do not
        // have a degradation reaction in the model:
        if(countLinksTerminatingOnNode(node, genome) == 0)
        {
            ids.add(node.getID());
        }
    }
    git = genome.getGeneIterator();    
    while (git.hasNext()) {
        Gene gene = git.next();
        ids.add(gene.getID());
    }
    
    Iterator<String> idit = ids.iterator();
    while (idit.hasNext()) {
      String id = idit.next();
      //Node node = genome.getNode(id);
      //int nodeType = node.getNodeType();
          ind.indent();
          buf.append("<reaction name=\"");
          buildParam("DEGR", id, cmpt, buf, null, true);
          buf.append("\" reversible=\"false\">\n");
          ind.up().indent();
          buf.append("<listOfReactants>\n");
          ind.up().indent();    
          buf.append("<speciesReference species=\"");
          buildSpecie(genome.getNode(id), cmpt, buf, tracker, uniqueNames, true);
          buf.append("\"/>\n");
          ind.down().indent();     
          buf.append("</listOfReactants>\n");
//      ind.indent();
//      buf.append("<listOfProducts />\n");
          ind.indent();    
          buf.append("<kineticLaw formula=\"");
          buildParam("DEGRF", id, cmpt, buf, null, true);
          buf.append("\" />\n");
          ind.down().indent();
          buf.append("</reaction>\n");
    }
    
    ind.down().indent();
    buf.append("</listOfReactions>\n");
    return;
  }
  
  /***************************************************************************
  **
  ** Write the formula
  */
  
  private String writeFormula(int logicFunction, String out, SortedSet<String> inputs, 
                              HashMap<String, Boolean> signs, String cmpt,
                              StringBuffer buf, HashSet<String> dict, HashMap<String, UniqueTracker> tracker, 
                              HashMap<String, String> uniqueNames, Genome genome, boolean append) {
    
    if (!append) {
      buf.setLength(0);
    }
    
    int size = inputs.size();
    
    //
    // Write the numerator:
    //
    
    buf.append("(");    
    for (int m = 1; m <= size; m++) {
      buildTermsOfSize(logicFunction, inputs, signs, out, m, 
                       cmpt, buf, dict, tracker, uniqueNames, genome, true);      
      if (m < size) {
        buf.append(" + ");
      }
    }
       
    //
    // Write the Denominator
    //

    buf.append(") / (");
    buildDenominator(logicFunction, inputs, out, cmpt, buf, 
                     dict, tracker, uniqueNames, genome, true);
    buf.append(")");
    
    return ((append) ? null : buf.toString());
  } 
  
  /***************************************************************************
  **
  ** Build the denominator
  */
  
  private String buildDenominator(int logicFunction, SortedSet<String> inputs, String out, 
                                  String cmpt, StringBuffer buf, HashSet<String> dict, HashMap<String, UniqueTracker> tracker, 
                                  HashMap<String, String> uniqueNames, Genome genome, boolean append) {

    if (!append) {
      buf.setLength(0);
    }

    int n = inputs.size();    
    
    //
    // Write the constant term:
    //

    buf.append("(");
    buildParam("DN", out, buf, dict, true);
    if (n > 1) {
      buf.append("^");
      buf.append(n);
      buf.append(".0");
    }
    buf.append(")");    

    //
    // Write the non-specific binding terms:
    //

    buf.append(" + (");
    if (n > 1) {
      buf.append("(");
      buildParam("DN", out, buf, dict, true);
      if (n > 2) {
        buf.append("^");
        buf.append(Integer.toString(n - 1));
        buf.append(".0");
      }
      buf.append(")*");
    }
    buf.append("(");
    Iterator<String> iit = inputs.iterator();
    while (iit.hasNext()) {
      String input = iit.next();
      buildSpecie(genome.getNode(input), cmpt, buf, tracker, uniqueNames, true);
      if (iit.hasNext()) buf.append("+");
    }
    buf.append("))");
    
    //
    // Write the specific binding terms:
    //

    buf.append(" + ");    

    for (int m = 1; m <= n; m++) {
      buildTermsOfSize(logicFunction, inputs, null, out, m, cmpt, buf, 
                       dict, tracker, uniqueNames, genome, true);      
      if (m < n) {
        buf.append(" + ");
      }
    }
    return ((append) ? null : buf.toString());
  }

  /***************************************************************************
  **
  ** Build fractional saturation terms
  */
  
  private String buildTermsOfSize(int logicFunction, SortedSet<String> inputs, 
                                  HashMap<String, Boolean> signs, String out, int m,
                                  String cmpt, StringBuffer buf, HashSet<String> dict, 
                                  HashMap<String, UniqueTracker> tracker, HashMap<String, String> uniqueNames, 
                                  Genome genome, boolean append) {

    if (!append) {
      buf.setLength(0);
    }

    int n = inputs.size();    

    buf.append("(");
    int exp = n - m;
    if (exp > 0) {
      buf.append("(");
      buildParam("DN", out, buf, dict, true);
      if (exp > 1) {
        buf.append("^");
        buf.append(exp);
        buf.append(".0");
      }
      buf.append(")*(");
    }
    long termCount = binomialCoefficient(n, m); 
    for (int j = 0; j < termCount; j++) {
      SortedSet<String> subset = nChooseM(inputs, m, j);
      boolean useTerm = getTruthValue(subset, inputs, logicFunction, signs);
      if (useTerm) {
        if (subset.size() > 1) {
          buildCoop("KQ", subset, out, buf, dict, true);
          buf.append("*(");
        }
        Iterator<String> sit = subset.iterator();
        while (sit.hasNext()) {
          String input = sit.next();          
          buildParamTimesSpecie("KR", out, genome.getNode(input), cmpt, buf, dict, tracker, uniqueNames, true);
          if (sit.hasNext()) {
            buf.append("*");
          } else if (subset.size() > 1) {
            buf.append(")");
          }
        }
      } else {
        buf.append("0");
      }
      if (j < (termCount - 1)) {
        buf.append("+");
      }
    }
    if (exp > 0) {
       buf.append(")");
    }
    buf.append(")");  
    return ((append) ? null : buf.toString());
  }
  
  /***************************************************************************
  **
  ** N choose m
  */
  
  private SortedSet<String> nChooseM(SortedSet<String> s, int m, int which) {
  
    //
    // I imagine there's a closed form method for this calculation.
    // This hack will do for now.
    //
    
    int size = s.size();
    if ((which < 0) || (which >= binomialCoefficient(size, m))) {
      System.err.println("Bad which: " + which + " size: " + size + " bc: " + binomialCoefficient(size, m));
      throw new IllegalArgumentException();
    }
    int count = -1;
    int selection = -1;
    while (count != which) {
      selection++;      
      if (nBitsOn(selection, m)) {
        count++;
      }
    }
    return (generateSubset(s, selection));
  }  

  /***************************************************************************
  **
  ** Return binomial coefficient
  */
  
  private long binomialCoefficient(int n, int m) {
    
    if (m > n) {
      throw new IllegalArgumentException();
    }
    
    long num = 1;
    for (int i = n; i > m; i--) {
      num *= i;
    }
    
    long denom = 1;
    for (int i = (n - m); i > 1; i--) {
      denom *= i;
    }
    
    return (num / denom);
  }  

 /***************************************************************************
  **
  ** Answer if n bits are on
  */
  
  private boolean nBitsOn(int num, int bitcount) {
    
    int shifted = num;
    int count = 0;
    
    while (shifted != 0) {
      if ((shifted & 0x01) == 1) count++;
      shifted >>>= 1;
    }
    
    return (count == bitcount);
  }
  
 /***************************************************************************
  **
  ** Return a reduced set based on bits
  */
  
  private SortedSet<String> generateSubset(SortedSet<String> set, int which) {
    
    SortedSet<String> retval = new TreeSet<String>();
    Iterator<String> sit = set.iterator();
    int slotnum = 0;
    while (sit.hasNext()) {
      String obj = sit.next();
      if (((which >>> slotnum++) & 0x01) == 1) {
        retval.add(obj);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Determine the boolean output of the given combination, or always true if
  ** the input map is null (used for building denominators).
  */
  
  private boolean getTruthValue(Set<String> inputSubset, Set<String> allInputs, 
                                int logicFunction, Map<String, Boolean> inputSigns) {
    //
    // If we are not given input values, then we always return true:
    //
    
    if (inputSigns == null) {
      return (true);
    }
    
    //
    // The subset represents the "ON" input values.  All the others in inputs
    // are "OFF".  Invert the inputs as needed based on the input signs, then
    // see if the logic function is satisfied.
    //
    
    ArrayList<Boolean> converted = new ArrayList<Boolean>();
    Iterator<String> aiit = allInputs.iterator();
    while (aiit.hasNext()) {
      String input = aiit.next();
      boolean convVal = inputSubset.contains(input);
      Boolean sign = inputSigns.get(input);
      if (sign.booleanValue()) {
        convVal = !convVal;     
      }
      converted.add(Boolean.valueOf(convVal));
    }
    boolean retval = false;
    int sum = 0;
    Iterator<Boolean> cit = converted.iterator();
    boolean firstPass = true;
    while (cit.hasNext()) {
      boolean input = cit.next().booleanValue();
      switch (logicFunction) {
        case DBInternalLogic.AND_FUNCTION:
          if (firstPass) retval = true;
          if (!input) {
            return (false);
          }
          break;
        case DBInternalLogic.OR_FUNCTION:
          if (firstPass) retval = false;
          if (input) {
            return (true);
          }
          break;
        case DBInternalLogic.XOR_FUNCTION:
          sum += (input) ? 1 : 0;
          retval = (sum == 1);
          break;
        default:
          throw new IllegalArgumentException();  
      }
      firstPass = false;
    }
    return (retval);
  }  
    
  /***************************************************************************
  **
  ** Write the param*specie term
  */
  
  private String buildParamTimesSpecie(String prefix, String out, Node node, String cmpt, 
                                       StringBuffer buf, HashSet<String> dict, HashMap<String, UniqueTracker> tracker, 
                                       HashMap<String, String> uniqueNames, boolean append) {
    if (!append) {
      buf.setLength(0);
    }
    buf.append("(");
    buildParam(prefix, node.getID(), out, buf, dict, true);    
    buf.append("*"); 
    buildSpecie(node, cmpt, buf, tracker, uniqueNames, true);
    buf.append(")");
    return ((append) ? null : buf.toString());
  }  
  
  /***************************************************************************
  **
  ** Write the specie
  */
  
  private String buildSpecie(Node node, String cmpt, StringBuffer buf, HashMap<String, UniqueTracker> tracker, 
                             HashMap<String, String> uniqueNames, boolean append) {
    if (!append) {
      buf.setLength(0);
    }
    buf.append("bt");    
    buf.append(buildUniqueDisplayName(node, tracker, uniqueNames));
    buf.append('_');
    buf.append(cmpt);
    return ((append) ? null : buf.toString());
  }

  /***************************************************************************
  **
  ** Build a coop coefficient
  */
  
  private String buildCoop(String prefix, SortedSet<String> inputs, String output,
                           StringBuffer buf, HashSet<String> dict, boolean append) { 
    if (!append) {
      buf.setLength(0);
    }
    int startPos = buf.length();    
    buf.append(prefix);
    buf.append('_');
    Iterator<String> iit = inputs.iterator();
    while (iit.hasNext()) {
      String input = iit.next(); 
      buf.append(input);
      buf.append('_');
    }
    buf.append(output);
    if (dict != null) {
      dict.add(buf.substring(startPos));
    }    
    return ((append) ? null : buf.toString());
  }  

  /***************************************************************************
  **
  ** Build a parameter for a specific input
  */
  
  private String buildParam(String prefix, String input, String output,
                            StringBuffer buf, HashSet<String> dict, boolean append) {
     
    if (!append) {
      buf.setLength(0);
    }
    int startPos = buf.length();
    buf.append(prefix);
    buf.append('_');
    buf.append(input);
    buf.append('_');
    buf.append(output);
    if (dict != null) {
      dict.add(buf.substring(startPos));
    }
    return ((append) ? null : buf.toString());
  }  

  /***************************************************************************
  **
  ** Build a general parameter for a specie
  */
  
  private String buildParam(String prefix, String output,
                            StringBuffer buf, HashSet<String> dict, boolean append) {
                              
    if (!append) {
      buf.setLength(0);
    }
    int startPos = buf.length();    
    buf.append(prefix);
    buf.append('_');
    buf.append(output);
    if (dict != null) {
      dict.add(buf.substring(startPos));
    }    
    return ((append) ? null : buf.toString());
  }
  

  
  /***************************************************************************
  **
  ** Node display names are not guaranteed to be unique (e.g. "Ubiq"), but we
  ** can't use internal unique IDs for presentation to the user.  Build a unique
  ** display name for each species.  FIX ME!!! Not consistent across different
  ** executions/program runs???
  */
  
  private String buildUniqueDisplayName(Node node, HashMap<String, UniqueTracker> tracker, 
                                        HashMap<String, String> uniqueNames) {
      
    String retval =  uniqueNames.get(node.getID());
    if (retval != null) {
      return (retval);
    }
    
    String name = node.getName();
    if (name.trim().equals("")) {
      name = "unnamed";
    }
    
    if (name.indexOf(" ") != -1) {
      name = name.replaceAll(" ", "_");
    }
       
    UniqueTracker trackEntry = tracker.get(name);
    if (trackEntry == null) {
      trackEntry = new UniqueTracker(name);
      tracker.put(name, trackEntry);
    }

    if (trackEntry.count == 0) {
      trackEntry.count = 1;
      retval = name;
    } else {
      retval = name + "_" + trackEntry.count++;
    }
    
    uniqueNames.put(node.getID(), retval);
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Used for tracking and generating unique species names.
  */
  
  class UniqueTracker {
    String display;
    int count;
    
    UniqueTracker(String display) {
      this.display = display;
      this.count = 0;
    }
  }  
}
