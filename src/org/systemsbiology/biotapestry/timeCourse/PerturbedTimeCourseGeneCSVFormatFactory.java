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

package org.systemsbiology.biotapestry.timeCourse;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;

import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.perturb.CSVData;
import org.systemsbiology.biotapestry.perturb.PertDictionary;
import org.systemsbiology.biotapestry.perturb.PertProperties;
import org.systemsbiology.biotapestry.perturb.PertSource;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.CSVParser;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This reads in perturbed spatial expression data from CSV
*/

public class PerturbedTimeCourseGeneCSVFormatFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final int BAD_VALUES_    = -1;  
  private static final int GENE_AND_PERT_ = 0;  
  private static final int PERT_VALUE_    = 1;    
    
  private static final String GENE_AND_PERT_STR_ = "pertGene";
  private static final String PERT_VALUE_STR_ = "pertGeneEntry"; 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final String UNDEFINED_PERTURBATION              = "PECSV_UNDEFINED_PERTURBATION";
  public static final String TOO_FEW_TOKENS                      = "PECSV_TOO_FEW_TOKENS";
  public static final String INVALID_NUMBER                      = "PECSV_INVALID_NUMBER";
  public static final String INCORRECT_INSTRUCTION_DEFINITION    = "PECSV_INCORRECT_INSTRUCTION_DEFINITION";  
  public static final String DUPLICATE_DEFINITION                = "PECSV_DUPLICATE_DEFINITION";  
  public static final String BAD_TIME_DEFINITION                 = "PECSV_BAD_TIME_DEFINITION";   
  public static final String BAD_EXPRESSION_TAG                  = "PECSV_BAD_EXPRESSION_TAG";
  public static final String MISSING_VARIABLE_VALUE              = "PECSV_MISSING_VARIABLE_VALUE";
  public static final String MISSING_CTRL_VALUE                  = "PECSV_MISSING_CTRL_VALUE";
  public static final String NON_EMPTY_VARIABLE_VALUE            = "PECSV_NON_EMPTY_VARIABLE_VALUE";
  public static final String BAD_CONFIDENCE_TAG                  = "PECSV_BAD_CONFIDENCE_TAG";
  public static final String NON_EXISTENT_GENE                   = "PECSV_NON_EXISTENT_GENE";
  public static final String NO_REGION_TIME_MATCH                = "PECSV_NO_REGION_TIME_MATCH";  
  public static final String NON_MATCHING_CONTROL                = "PECSV_NON_MATCHING_CONTROL";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private UndoFactory uFac_;
  private TimeCourseData tcd_;
  private TimeAxisDefinition tad_;
  private PerturbationData pd_;
       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbedTimeCourseGeneCSVFormatFactory(TimeCourseData tcd, TimeAxisDefinition tad, PerturbationData pd, UndoFactory uFac) {
    uFac_ = uFac;
    tcd_ = tcd;
    tad_ = tad;
    pd_ = pd;     
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Build the whole thing from a csv file.
  */

  public boolean readPerturbedExpressionCSV(File infile, DataAccessContext dacx) throws IOException, InvalidInputException {
    
    HashMap<String, Map<PertSources, GeneEntry>> pertGenes = new HashMap<String, Map<PertSources, GeneEntry>>();
    HashMap<String, Map<PertSources, List<PertEntry>>> pertGeneEntries = new HashMap<String, Map<PertSources, List<PertEntry>>>();
    readCSV(infile, pertGenes, pertGeneEntries);
    return (applyProperties(pertGenes, pertGeneEntries, dacx));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Do reading from the file
  */

  private void readCSV(File infile, Map<String, Map<PertSources, GeneEntry>> pertGenes, 
                       Map<String, Map<PertSources, List<PertEntry>>> pertGeneEntries) throws IOException, InvalidInputException {
    
    BufferedReader in = new BufferedReader(new FileReader(infile));    
    //
    // Read in the lines.
    //
    
    CSVParser csvp = new CSVParser(true);
      
    String line = null;
    int lineNumber = -1;
    try {
      while ((line = in.readLine()) != null) {
        lineNumber++;
        if (line.trim().equals("")) {
          continue;
        }
        List<String> tokens = csvp.processCSVLine(line);
        if (tokens.isEmpty()) {
          continue;
        }
        if (tokens.get(0).trim().startsWith("#")) {
          continue;  
        }
        int tokenCategory = getCategory(tokens.get(0));
        switch (tokenCategory) {
          case GENE_AND_PERT_:
            GeneEntry ge = new GeneEntry(pd_, tokens, lineNumber);
            Map<PertSources, GeneEntry> forGene = pertGenes.get(DataUtil.normKey(ge.targetGene));
            if (forGene == null) {
              forGene = new HashMap<PertSources, GeneEntry>();
              pertGenes.put(DataUtil.normKey(ge.targetGene), forGene);
            }
            if (forGene.get(ge.pss) != null) {
              throw new InvalidInputException(DUPLICATE_DEFINITION, lineNumber);
            }
            forGene.put(ge.pss, ge);
            break;
          case PERT_VALUE_:
            PertEntry pe = new PertEntry(pd_, tad_, tokens, lineNumber);
            Map<PertSources, List<PertEntry>> forGene2 = pertGeneEntries.get(DataUtil.normKey(pe.targetGene));
            if (forGene2 == null) {
              forGene2 = new HashMap<PertSources, List<PertEntry>>();
              pertGeneEntries.put(DataUtil.normKey(pe.targetGene), forGene2);
            }
            List<PertEntry> forPert = forGene2.get(pe.pss);
            if (forPert == null) {
              forPert = new ArrayList<PertEntry>();
              forGene2.put(pe.pss, forPert);
            }
            forPert.add(pe);
            break;          
          case BAD_VALUES_:
          default:
            throw new InvalidInputException(INCORRECT_INSTRUCTION_DEFINITION, lineNumber);
        }      
      }
    } catch (IOException ioex) {
      in.close();
      throw ioex;
    }
    
    in.close();
    return;
  }
  
  /***************************************************************************
  ** 
  ** Figure out token category
  */

  private int getCategory(String tok) {
    if (tok == null) {
      return (BAD_VALUES_);
    } else if (tok.equalsIgnoreCase(GENE_AND_PERT_STR_)) {
      return (GENE_AND_PERT_);
    } else if (tok.equalsIgnoreCase(PERT_VALUE_STR_)) {
      return (PERT_VALUE_);      
    } else {
      return (BAD_VALUES_);
    }
  }

  /***************************************************************************
  **
  ** Apply the CSV to the database
  ** 
  */
  
  private boolean applyProperties(Map<String, Map<PertSources, GeneEntry>> pertGenes, 
                                  Map<String, Map<PertSources, List<PertEntry>>> pertGeneEntries, DataAccessContext dacx) throws InvalidInputException {


    //
    // Get the genes built:
    //
    
    HashMap<String, Map<PertSources, PerturbedTimeCourseGene>> builtGenes = new HashMap<String, Map<PertSources, PerturbedTimeCourseGene>>();
    Iterator<String> pgit = pertGenes.keySet().iterator();
    while (pgit.hasNext()) {
      String targName = pgit.next();   
      Map<PertSources, GeneEntry> forGene = pertGenes.get(targName);
      Iterator<PertSources> fgit = forGene.keySet().iterator();
      TimeCourseGene tcg = tcd_.getTimeCourseData(targName);
      if (tcg == null) {
        throw new InvalidInputException(NON_EXISTENT_GENE, forGene.get(fgit.next()).lineNumber);
      }
      while (fgit.hasNext()) {
        PertSources pss = fgit.next();
        GeneEntry ge = forGene.get(pss);
        PerturbedTimeCourseGene ptcg = tcg.getPerturbedState(ge.pss);
        if (ptcg == null) {
          ptcg = new PerturbedTimeCourseGene(pd_, ge.pss.clone(), tcd_.getGeneTemplate());
        } else {
          ptcg = ptcg.clone();
        }
        ptcg.setInternalOnly(ge.isInternal);
        ptcg.setTimeCourseNote((ge.note.length() == 0) ? null : ge.note);
        ptcg.setConfidence(ge.baseConfidence);
        boolean usingDefCtrl = ptcg.usingDistinctControlExpr();
        if (!usingDefCtrl && ge.defineControl) {
          ptcg.setForDistinctControlExpr(tcd_.getGeneTemplate());
        } else if (usingDefCtrl && !ge.defineControl) {
          ptcg.dropDistinctControlExpr();
        }      
        Map<PertSources, PerturbedTimeCourseGene> builtForGene = builtGenes.get(targName);
        if (builtForGene == null) {
          builtForGene = new HashMap<PertSources, PerturbedTimeCourseGene>();
          builtGenes.put(targName, builtForGene);
        }
        if (builtForGene.get(ge.pss) != null) {
          throw new InvalidInputException(DUPLICATE_DEFINITION, ge.lineNumber);
        }
        builtForGene.put(ge.pss, ptcg);
      }
    }
    
    //
    // Add entries:
    //
    
    Iterator<String> tpgit = pertGeneEntries.keySet().iterator();
    while (tpgit.hasNext()) {
      String targName = tpgit.next();   
      Map<PertSources, List<PertEntry>> forGene = pertGeneEntries.get(targName);
      Map<PertSources, PerturbedTimeCourseGene> builtForGene = builtGenes.get(targName);
      if (builtForGene == null) {
        builtForGene = new HashMap<PertSources, PerturbedTimeCourseGene>();
        builtGenes.put(targName, builtForGene);
      }      
      Iterator<PertSources> fgit = forGene.keySet().iterator();
      TimeCourseGene tcg = tcd_.getTimeCourseData(targName);
      if (tcg == null) {
        PertSources pss = fgit.next();
        List<PertEntry> forPert = forGene.get(pss);      
        throw new InvalidInputException(NON_EXISTENT_GENE, forPert.get(0).lineNumber);      
      }      
      while (fgit.hasNext()) {
        PertSources pss = fgit.next();
        List<PertEntry> forPert = forGene.get(pss);
        PerturbedTimeCourseGene ptcg = builtForGene.get(pss);
        if (ptcg == null) {
          ptcg = tcg.getPerturbedState(pss);
          if (ptcg == null) {
            ptcg = new PerturbedTimeCourseGene(pd_, pss.clone(), tcd_.getGeneTemplate());
          } else {
            ptcg = ptcg.clone();
          }
          builtForGene.put(pss, ptcg);
        }
      
        //
        // Install expressions that match:
        //
        
        installInMatchingExpr(ptcg, forPert, false, ptcg.getConfidence());
          
        //
        // Install control expressions that match:
        //
        
        if (ptcg.usingDistinctControlExpr()) {
          installInMatchingExpr(ptcg, forPert, true, ptcg.getConfidence());
        } else {      
          compareToMatchingExpr(tcg, forPert);      
        }
      } 
    }
       
    UndoSupport support = uFac_.provideUndoSupport("undo.pertExprFromCSV", dacx);
    Iterator<String> fgit = builtGenes.keySet().iterator();
    boolean doit = false;
    while (fgit.hasNext()) {
      String targName = fgit.next();
      TimeCourseGene tcg = tcd_.getTimeCourseData(targName);
      Map<PertSources, PerturbedTimeCourseGene> builtForGene = builtGenes.get(targName);
      TimeCourseChange tcc = tcd_.startGeneUndoTransaction(tcg.getName());
      Iterator<PertSources> bfgit = builtForGene.keySet().iterator();
      boolean doForMe = false;
      while (bfgit.hasNext()) {
        PertSources pss = bfgit.next();
        PerturbedTimeCourseGene ptcg = builtForGene.get(pss);        
        tcg.setPerturbedState(pss.clone(), ptcg);
        doForMe = true;
      }
      if (doForMe) {
        tcc = tcd_.finishGeneUndoTransaction(tcg.getName(), tcc);
        support.addEdit(new TimeCourseChangeCmd(tcc));
        doit = true;
      }
    }
    if (doit) {    
      support.addEvent(new GeneralChangeEvent(dacx.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
      support.finish();
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Install matching expressions
  */
     
  private void installInMatchingExpr(PerturbedTimeCourseGene ptcg, List<PertEntry> forPert, 
                                     boolean forControl, int baseConfidence) throws InvalidInputException {
    
    int numForPert = forPert.size();
    for (int i = 0; i < numForPert; i++) {
      PertEntry pe = forPert.get(i);     
      Iterator<ExpressionEntry> pexit = (forControl) ? ptcg.getControlExpressions() : ptcg.getExpressions();
      boolean matched = false;
      while (pexit.hasNext()) {
        ExpressionEntry ee = pexit.next();    
        if (!DataUtil.keysEqual(ee.getRegion(), pe.region)) {
          continue;
        }
        if (ee.getTime() != pe.time) {
          continue;
        }
        //
        // Matched on region and time
        //
        
        matched = true;
        if (forControl && (pe.ctrlExp == null)) {
          throw new InvalidInputException(MISSING_CTRL_VALUE, pe.lineNumber);
        }
        int exp = (forControl) ? pe.ctrlExp.intValue() : pe.pertExp;
        Double val = (forControl) ? pe.ctrlVal : pe.pertVal;
        ee.setExpression(exp);
        if (val != null) {
          if (exp != ExpressionEntry.VARIABLE) {
            throw new InvalidInputException(NON_EMPTY_VARIABLE_VALUE, pe.lineNumber);
          }
          ee.setVariableLevel(val.doubleValue());
        } else {
          if (exp == ExpressionEntry.VARIABLE) {
            throw new InvalidInputException(MISSING_VARIABLE_VALUE, pe.lineNumber);
          }
          ee.setVariableLevel(0.0);  // Actually being ignored
        }
        if (!forControl && (pe.confidence != null)) {
          int useConf = TimeCourseGene.reverseMapEntryConfidence(baseConfidence, pe.confidence.intValue());
          ee.setConfidence(useConf);
        }
        break;
      }
      if (!matched) {
        throw new InvalidInputException(NO_REGION_TIME_MATCH, pe.lineNumber);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Compare to matching expression
  */
     
  private void compareToMatchingExpr(TimeCourseGene tcg, List<PertEntry> forPert) throws InvalidInputException {   
    int numForPert = forPert.size();
    for (int i = 0; i < numForPert; i++) {
      PertEntry pe = forPert.get(i);
      Iterator<ExpressionEntry> texit = tcg.getExpressions();
      boolean matched = false;
      while (texit.hasNext()) {
        ExpressionEntry ee = texit.next();
        if (!DataUtil.keysEqual(ee.getRegion(), pe.region)) {
          continue;
        }
        if (ee.getTime() != pe.time) {
          continue;
        }
        //
        // Matched on region and time
        //
        matched = true;
        
        //
        // Nothing provided, nothing to check:
        //
        if (pe.ctrlExp == null) {
          continue;
        }
        //
        // Gotta match, or we are done:
        //        
        int wtExp = ee.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
        int ctrlExp = pe.ctrlExp.intValue();
        if (wtExp != ctrlExp) {
          throw new InvalidInputException(NON_MATCHING_CONTROL, pe.lineNumber);
        }       
        if (wtExp == ExpressionEntry.VARIABLE) {
          if (pe.ctrlVal == null) {
            throw new InvalidInputException(MISSING_VARIABLE_VALUE, pe.lineNumber);
          }
          if (ee.getVariableLevelForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED) != pe.ctrlVal.doubleValue()) {
            throw new InvalidInputException(NON_MATCHING_CONTROL, pe.lineNumber);
          }
        } else if (pe.ctrlVal != null) {
          throw new InvalidInputException(NON_EMPTY_VARIABLE_VALUE, pe.lineNumber);
        }
        break;
      }
      if (!matched) {
        throw new InvalidInputException(NO_REGION_TIME_MATCH, pe.lineNumber);
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Break apart the experiment type string.  Note we are only supporting 
  ** single perturbations at the moment
  */

  private static List<String> experimentParse(PerturbationData pd, String value, int lineNumber) throws InvalidInputException {

    CSVData.ExperimentTokens exptok = new CSVData.ExperimentTokens(); 
    PertDictionary pDict = pd.getPertDictionary();   
    Iterator<String> pdkit = pDict.getKeys();
    while (pdkit.hasNext()) {
      String key = pdkit.next();
      PertProperties pProps = pDict.getPerturbProps(key);
      if (exptok.haveAMatch(value, pProps.getType(), pProps.getAbbrev())) { 
        Iterator<String> sdkit = pd.getSourceDefKeys();
        while (sdkit.hasNext()) {
          String sdkey = sdkit.next();
          PertSource ps = pd.getSourceDef(sdkey);
          if (ps.getExpType(pDict).equals(pProps) && DataUtil.keysEqual(exptok.base, ps.getSourceName(pd))) {
            ArrayList<String> retval = new ArrayList<String>();
            retval.add(ps.getID());
            return (retval);
          }
        }
      }
    }
    throw new InvalidInputException(UNDEFINED_PERTURBATION, lineNumber);  
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Collect up state from blocks
  */  
  
  public static class GeneEntry {    
 
    String targetGene;
    PertSources pss;
    int baseConfidence;
    String note;
    boolean isInternal;
    boolean defineControl;
    int lineNumber;
 
    public GeneEntry(PerturbationData pd, List<String> tokens, int lineNumber) throws InvalidInputException {
      this.lineNumber = lineNumber;
      int tokSize = tokens.size();
      if (tokSize < 7) {
        throw new InvalidInputException(TOO_FEW_TOKENS, lineNumber);
      }
      // token 0 is the command
      targetGene = tokens.get(1);
      pss = new PertSources(experimentParse(pd, tokens.get(2), lineNumber));
      try {
        baseConfidence = TimeCourseGene.mapToConfidence(tokens.get(3).trim());
      } catch (IllegalArgumentException iaex) {
        throw new InvalidInputException(BAD_CONFIDENCE_TAG, lineNumber);
      }      
      note = tokens.get(4).trim();
      isInternal = Boolean.valueOf(tokens.get(5).trim()).booleanValue();
      defineControl = Boolean.valueOf(tokens.get(6).trim()).booleanValue();
    }
  }
 
  /***************************************************************************
  ** 
  ** Collect up state from blocks
  */  
  
  public static class PertEntry {    
 
    String targetGene;
    PertSources pss;
    String region;
    int time;
    int pertExp;
    Double pertVal;
    Integer ctrlExp;
    Double ctrlVal;
    Integer confidence;
    int lineNumber;
 
    public PertEntry(PerturbationData pd, TimeAxisDefinition tad, List<String> tokens, int lineNumber) throws InvalidInputException {
      this.lineNumber = lineNumber;
      int tokSize = tokens.size();    
      if (tokSize < 6) {
        throw new InvalidInputException(TOO_FEW_TOKENS, lineNumber);
      }
      // token 0 is the command
      targetGene = tokens.get(1);
      pss = new PertSources(experimentParse(pd, tokens.get(2), lineNumber));
      region = tokens.get(3);
      
      Integer parsed = tad.timeStringParse(tokens.get(4));
      if ((parsed == null) || (parsed.intValue() < 0)) {
        throw new InvalidInputException(BAD_TIME_DEFINITION, lineNumber);
      }
      time = parsed.intValue();

      boolean needVar = false;
      boolean needCtrlVar = false;
      try {
        pertExp = ExpressionEntry.mapFromExpressionTag(tokens.get(5));
        needVar = (pertExp == ExpressionEntry.VARIABLE);
        if (tokSize > 8) {
          ctrlExp = new Integer (ExpressionEntry.mapFromExpressionTag(tokens.get(8)));
          needCtrlVar = (ctrlExp.intValue() == ExpressionEntry.VARIABLE);
        }
      } catch (IllegalArgumentException iaex) {
        throw new InvalidInputException(BAD_EXPRESSION_TAG, lineNumber);
      }
   
      String pertValStr = (tokSize > 6) ? tokens.get(6) : null;   
      if (needVar) {
        if (pertValStr == null) {
          throw new InvalidInputException(MISSING_VARIABLE_VALUE, lineNumber);    
        }
        try {
          pertVal = new Double(pertValStr);
        } catch (NumberFormatException nfex) {
          throw new InvalidInputException(INVALID_NUMBER, lineNumber);
        }      
        if ((pertVal.doubleValue() < 0.0) || (pertVal.doubleValue() > 1.0)) {
          throw new InvalidInputException(INVALID_NUMBER, lineNumber);
        }
      } else {
        if ((pertValStr != null) && !pertValStr.trim().equals("")) {
          throw new InvalidInputException(NON_EMPTY_VARIABLE_VALUE, lineNumber);    
        }        
      }

      String ctrlValStr = (tokSize > 9) ? tokens.get(9) : null;
      if (needCtrlVar) {
        if (ctrlValStr == null) {
          throw new InvalidInputException(MISSING_VARIABLE_VALUE, lineNumber);    
        }
        try {
          ctrlVal = new Double(ctrlValStr);
        } catch (NumberFormatException nfex) {
          throw new InvalidInputException(INVALID_NUMBER, lineNumber);
        }      
        if ((ctrlVal.doubleValue() < 0.0) || (ctrlVal.doubleValue() > 1.0)) {
          throw new InvalidInputException(INVALID_NUMBER, lineNumber);
        }
      } else {
        if ((ctrlValStr != null) && !ctrlValStr.trim().equals("")) {
          throw new InvalidInputException(NON_EMPTY_VARIABLE_VALUE, lineNumber);    
        }        
      }

      if (tokSize > 7) {
        try {
          String confStr = tokens.get(7).trim();
          if (confStr.equals("")) {
            confidence = new Integer(TimeCourseGene.USE_BASE_CONFIDENCE);
          } else {
            confidence = new Integer(TimeCourseGene.mapToConfidence(confStr));
          }
        } catch (IllegalArgumentException iaex) {
          throw new InvalidInputException(BAD_CONFIDENCE_TAG, lineNumber);
        }     
      }
    }
  }
}
