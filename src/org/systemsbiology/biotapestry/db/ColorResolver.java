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

package org.systemsbiology.biotapestry.db;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.ui.NamedColor;

/****************************************************************************
**
** Interface for color sources
*/

public interface ColorResolver {

  /***************************************************************************
  ** 
  ** Get the color
  */

  public Color getColor(String colorKey);
  
  /***************************************************************************
  **
  ** Return from a cycle of active colors
  */
  
  public String activeColorCycle(int i);
   
  /***************************************************************************
  **
  ** Return from a cycle of inactive colors
  */
  
  public String inactiveColorCycle(int i);
  
  /***************************************************************************
  ** 
  ** Get the named color
  */

  public NamedColor getNamedColor(String colorKey);  
  
  /***************************************************************************
  **
  ** Return an active color
  */
  
  public String distinctActiveColor();
   
  /***************************************************************************
  **
  ** Return an inactive color
  */
  
  public String distinctInactiveColor();
  
  /***************************************************************************
  **
  ** Get next color
  */
  
  public String getNextColor(GenomeSource gs, LayoutSource ls);
  
  /***************************************************************************
  **
  ** Get rarest colors
  */
  
  public void getRarestColors(List<String> colorList, List<String> remainingColors, Genome genome, Layout lo);
  
  /***************************************************************************
  **
  ** Get number of colors
  */  
  
  public int getNumColors();    
   
  /***************************************************************************
  **
  ** Get gene color
  */
  
  public String getGeneColor(int i);
  
  
  /***************************************************************************
  **
  ** Set the color for the given name
  */
  
  public void setColor(String itemId, NamedColor color);
 
  /***************************************************************************
  **
  ** Return an iterator over all the color keys
  */
  
  public Iterator<String> getColorKeys();
 
  /***************************************************************************
  **
  ** Update the color set
  */
  
  public GlobalChange updateColors(Map<String, NamedColor> namedColors);
  
  /***************************************************************************
  **
  ** Return the colors you cannot delete
  */
  
  public Set<String> cannotDeleteColors();

  
  /***************************************************************************
  **
  ** Get the next color label
  */
  
  public String getNextColorLabel();

}
