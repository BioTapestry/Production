/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.util.TaggedSet;
  
/***************************************************************************
**
** Info needed to undo a Network Overlay change
**
*/
  
public class NetworkOverlayChange {
  public NetModule nmOrig;
  public NetModule nmNew;
  public NetModuleLinkage nmlOrig;
  public NetModuleLinkage nmlNew;  
  public int index;
  public boolean nameChange;
  public String nameOrig;
  public String nameNew;
  public boolean descChange;
  public String descOrig;
  public String descNew;
  public String noOwnerKey;
  public int ownerMode;
  public String overlayKey;
  public boolean firstViewChanged;
  public TaggedSet firstViewModOrig;
  public TaggedSet firstViewRevOrig;
  public TaggedSet firstViewModNew;
  public TaggedSet firstViewRevNew;
}
