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

package org.systemsbiology.biotapestry.app;

import java.awt.font.FontRenderContext;
import java.util.ArrayList;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.nav.GroupSettingSource;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.OverlayStateOracle;
import org.systemsbiology.biotapestry.ui.ZoomTargetSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;

/***************************************************************************
**
** This DataAccessContext can be handed to e.g. UI components that need to
** act on whatever the CURRENT Genome is.
*/

public class DynamicDataAccessContext extends DataAccessContext {
  private BTState appState_;
                                
  public DynamicDataAccessContext(BTState appState) {
    super(appState);
    appState_ = appState;
  }
  
  public DynamicDataAccessContext(DataAccessContext dacx, BTState appState) {
    super(dacx);
    appState_ = appState;
  }

  @Override
  public Genome getGenome() {
    String key = appState_.getGenome();
    return ((key == null) ? null : getGenomeSource().getGenome(key));
  }
  
  @Override
  public GenomeInstance getGenomeAsInstance() {
    return ((GenomeInstance)getGenome());
  }
  
  @Override
  public DBGenome getGenomeAsDBGenome() {
    return ((DBGenome)getGenome());
  }
 
  @Override 
  public String getGenomeID() {
    return (appState_.getGenome());
  }
  
  @Override
  public String getLayoutID() {
    return (appState_.getLayoutKey());
  }
  
  @Override
  public Layout getLayout() {
    return (lSrc.getLayout(getLayoutID()));
  }
}