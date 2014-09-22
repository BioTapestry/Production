/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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
  
/***************************************************************************
**
** Info needed to undo a genome change
**
*/
  
public class GenomeChange {
  public Gene gOrig;
  public Node nOrig;
  public Linkage lOrig;
  public Group grOrig;
  public Note ntOrig;
  public Gene gNew;
  public Node nNew;
  public Linkage lNew;
  public Group grNew;
  public Note ntNew;
  public String genomeKey;
  public String nameNew;
  public String nameOld;
  public String longNameNew;
  public String longNameOld;
  public String descNew;
  public String descOld;
  public boolean timeChanged;
  public boolean timedOld;
  public boolean timedNew;
  public int minTimeOld;
  public int minTimeNew;
  public int maxTimeOld;
  public int maxTimeNew;
}
