/*
**    Copyright (C) 2003-2015 Institute for Systems Biology
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

package org.systemsbiology.biotapestry.simulation;

import org.systemsbiology.biotapestry.analysis.Link;

import java.util.*;


/***************************************************************************
 *
 * Class for finding feedback loops in the model
 *
 */
public class GraphFeedbackFinder {
    private Set<String> allNodes_;
    private Set<Link> allEdges_;

    public GraphFeedbackFinder(Set<String> nodes, Set<Link> links) {
        allNodes_ = new HashSet<String>(nodes);
        allEdges_ = new HashSet<Link>(links);
    }

    public Set<Link> run(List<String> startNodeIDList) {
        Set<Link> result = new HashSet<Link>();

        for (String startNodeID : startNodeIDList) {
            List<EdgeClassifiedHandler> echs = new ArrayList<EdgeClassifiedHandler>();
            EdgeCollector collector = new EdgeCollector(ModelDepthFirstSearcher.EdgeType.DFS_BACK_EDGE);
            echs.add(collector);

            ModelDepthFirstSearcher searcher = new ModelDepthFirstSearcher(allNodes_, allEdges_, echs);
            searcher.run(startNodeID);

            Set<Link> found = new HashSet<Link>(collector.getBackEdges());

            result.addAll(found);
        }

        return result;
    }
}
