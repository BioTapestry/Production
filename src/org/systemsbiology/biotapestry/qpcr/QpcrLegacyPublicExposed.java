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
import java.util.Map;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.perturb.PertDisplayOptions;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** With the retirement of the QPCR package (except for legacy input and
** formatting the classic perturbation table presentation), all classes
** in the package EXCEPT THIS are now default (essentially package-only;
** we are not subclassing) visibility!  This ensure that all dependencies
** on the old code are limited to the following calls!
*/

public class QpcrLegacyPublicExposed {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private QPCRData legacyQPCR_;
  private QPCRData qpcrForDisplay_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static String LEGACY_BATCH_PREFIX = "_BT_";

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  public QpcrLegacyPublicExposed() {  
  }
  
  QpcrLegacyPublicExposed(QPCRData data) {
    legacyQPCR_ = data;
  }
    
  public boolean columnDefinitionsUsed() {
    return (qpcrForDisplay_.columnDefinitionsUsed());
  }
  
  public void transferFromLegacy(PerturbationData pd, DataMapSource dms, DisplayOptions dOpt, TimeAxisDefinition tad) {
    legacyQPCR_.transferFromLegacy(pd, dms, dOpt, tad);
    return;
  }
  
  public void createQPCRFromPerts(PerturbationData pd, TimeAxisDefinition tad, PerturbationDataMaps pdms,
                                  DBGenome genome, ResourceManager rMan) {
    QpcrDisplayGenerator qdg = new QpcrDisplayGenerator();
    qpcrForDisplay_ = qdg.createQPCRFromPerts(pd, tad, pdms, genome, rMan);
    return;
  }
  
  public boolean readyForDisplay() {
    return (qpcrForDisplay_ != null);
  }  
  
  public void dropCurrentStateForDisplay() {
    qpcrForDisplay_ = null;
    return;
  }
  
  public String getHTML(String geneId, String sourceID, boolean noCss,
                        boolean bigScreen, DBGenome dbGenome, 
                        PerturbationData pd, TimeAxisDefinition tad, ResourceManager rMan) {
    PertDisplayOptions dOpt = pd.getPertDisplayOptions();
    Map<String, String> colors = dOpt.getMeasurementDisplayColors();
    QpcrTablePublisher qtp = new QpcrTablePublisher(bigScreen, colors, dOpt);
    return (qpcrForDisplay_.getHTML(geneId, sourceID, qtp, dbGenome, pd, tad, rMan));
  }
  
  public boolean publish(PrintWriter out, PerturbationData pd, TimeAxisDefinition tad, PertDisplayOptions dOpt, ResourceManager rMan) {
    Map<String, String> colors = dOpt.getMeasurementDisplayColors();
    QpcrTablePublisher qtp = new QpcrTablePublisher(colors, dOpt);
    return (qtp.publish(out, qpcrForDisplay_, pd, tad, rMan));
  }    
}
  
 