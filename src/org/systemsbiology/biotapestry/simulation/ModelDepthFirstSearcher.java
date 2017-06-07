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

public class ModelDepthFirstSearcher {
    public enum EdgeType {
        DFS_TREE_EDGE,
        DFS_BACK_EDGE,
        DFS_FORWARD_EDGE,
        DFS_CROSS_EDGE,
    }

    public class DFSState {
        private int time_;
        private HashSet<String> visited_;
        private HashMap<String, Integer> entryTimes_;
        private HashMap<String, Integer> exitTimes_;
        private List<QueueEntry> visitOrder_;
        private Map<String, String> parents_;
        private Map<Link, EdgeType> edgeClasses_;


        public DFSState() {
            time_ = 0;
            visited_ = new HashSet<String>();
            entryTimes_ = new HashMap<String, Integer>();
            exitTimes_ = new HashMap<String, Integer>();
            visitOrder_ = new ArrayList<QueueEntry>();
            parents_ = new HashMap<String, String>();
            edgeClasses_ = new HashMap<Link, EdgeType>();
        }

        public void markVertexParent(String targetVertexID, String sourceVertexID) {
            if (sourceVertexID != null) {
                parents_.put(targetVertexID, sourceVertexID);
            }
        }

        public int visit(String targetVertexID, String sourceVertexID, int depth) {
            if (! visited_.contains(targetVertexID)) {
                time_ += 1;
                visited_.add(targetVertexID);
                visitOrder_.add(new QueueEntry(depth, targetVertexID, time_));
                entryTimes_.put(targetVertexID, time_);
            }

            return time_;
        }

        public EdgeType classifyEdge(String source, String target, List<EdgeClassifiedHandler> handlers) {
            EdgeType type;

            if (source.equals(target)) {
                type = EdgeType.DFS_BACK_EDGE;
            }
            else if (parents_.get(target) != null && parents_.get(target).equals(source)) {
                type = EdgeType.DFS_TREE_EDGE;
            }
            else if (isVisited(target) && exitTimes_.get(target) == null) {
                type = EdgeType.DFS_BACK_EDGE;
            }
            else if (exitTimes_.get(target) != null && (entryTimes_.get(target) > entryTimes_.get(source))) {
                type = EdgeType.DFS_FORWARD_EDGE;
            }
            else {
                type = EdgeType.DFS_CROSS_EDGE;
            }

            edgeClasses_.put(new Link(source, target), type);

            for (EdgeClassifiedHandler handler : handlers) {
                handler.handleEdgeClassified(source, target, type);
            }

            return type;
        }

        public EdgeType getEdgeClass(String source, String target) {
            return edgeClasses_.get(new Link(source, target));
        }

        public boolean isVisited(String vertexID) {
            return visited_.contains(vertexID);
        }

        public int exitVertex(String vertexID) {
            time_ += 1;
            exitTimes_.put(vertexID, time_);
            return time_;
        }

        public int getEntryTime(String vertexID) {
            return entryTimes_.get(vertexID);
        }

        public int getExitTime(String vertexID) {
            return exitTimes_.get(vertexID);
        }

        public String getParent(String vertexID) {
            return parents_.get(vertexID);
        }

        public List<QueueEntry> getVisitOrder() {
            return visitOrder_;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PUBLIC INNER CLASSES
    //
    ////////////////////////////////////////////////////////////////////////////

    public static class QueueEntry {
        public int depth;
        public String name;
        public int time_;

        public QueueEntry(int depth, String name, int time) {
            this.depth = depth;
            this.name = name;
            this.time_ = time;
        }

        public String toString() {
            return (name + " depth = " + depth + " time = " + time_);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof QueueEntry)) {
                return false;
            }
            if (obj == this) {
                return true;
            }

            QueueEntry other = (QueueEntry)obj;

            return this.depth == other.depth &&
                    this.name.equals(other.name) &&
                    this.time_ == other.time_;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE VARIABLES
    //
    ////////////////////////////////////////////////////////////////////////////

    private Set<String> allNodes_;
    private Set<Link> allEdges_;
    // private ArrayList<String> nodeOrder_;
    private ArrayList<Link> edgeOrder_;

    private List<EdgeClassifiedHandler> edgeClassifiedHandlers_;

    /**
     * Constructor.
     *
     * @param nodes Set of nodes identifiers.
     * @param links Set of links.
     */
    public ModelDepthFirstSearcher(Set<String> nodes, Set<Link> links) {
        allNodes_ = new HashSet<String>();
        allEdges_ = new HashSet<Link>();
        edgeClassifiedHandlers_ = new ArrayList<EdgeClassifiedHandler>();

        Iterator<String> ni = nodes.iterator();
        Iterator<Link> li = links.iterator();

        while (ni.hasNext()) {
            allNodes_.add(ni.next());
        }

        while (li.hasNext()) {
            Link link = li.next();
            allEdges_.add(link.clone());
        }

        edgeOrder_ = buildEdgeOrder(links);
    }

    public ModelDepthFirstSearcher(Set<String> nodes, Set<Link> links, List<EdgeClassifiedHandler> handlers) {
        this(nodes, links);

        for (EdgeClassifiedHandler handler : handlers) {
            edgeClassifiedHandlers_.add(handler);
        }
    }

    /**
     * Constructor.
     *
     * @param nodes List of nodes identifiers.
     * @param links List of links.
     */
    public ModelDepthFirstSearcher(List<String> nodes, List<Link> links) {
        allNodes_ = new HashSet<String>();
        allEdges_ = new HashSet<Link>();
        edgeOrder_ = new ArrayList<Link>();
        edgeClassifiedHandlers_ = new ArrayList<EdgeClassifiedHandler>();

        Iterator<Link> li = links.iterator();

        allNodes_.addAll(nodes);

        while (li.hasNext()) {
            Link link = li.next();
            if (!allEdges_.contains(link)) {
                edgeOrder_.add(link.clone());
            }
            allEdges_.add(link.clone());
        }
    }

    public ModelDepthFirstSearcher(List<String> nodes, List<Link> links, List<EdgeClassifiedHandler> handlers) {
        this(nodes, links);

        for (EdgeClassifiedHandler handler : handlers) {
            edgeClassifiedHandlers_.add(handler);
        }
    }

    public DFSState run(String startNodeID) {
        Map<String, ArrayList<String>> outEdges = calcOutboundEdges(allEdges_);
        List<QueueEntry> retval = new ArrayList<QueueEntry>();

        // The times are initially unset for all vertices
        HashMap<String, Integer> entryTimes = new HashMap<String, Integer>();
        HashMap<String, Integer> exitTimes = new HashMap<String, Integer>();

        DFSState state = new DFSState();

        visit(startNodeID, null, retval, entryTimes, exitTimes, outEdges, 0, state);

        return state;
    }

    private void visit(String vertexID, String sourceVertexID, List<QueueEntry> results,
                       HashMap<String, Integer> entryTimes, HashMap<String, Integer> exitTimes,
                       Map<String, ArrayList<String>> edgesFromSrc, int depth,
                       DFSState state) {

        if (state.isVisited(vertexID)) {
            return;
        }

        // Vertex is newly discovered
        state.visit(vertexID, sourceVertexID, depth);

        ArrayList<String> outEdges = edgesFromSrc.get(vertexID);
        if (outEdges == null) {
            state.exitVertex(vertexID);
        }
        else {
            Iterator<String> eit = outEdges.iterator();
            while (eit.hasNext()) {
                String targ = eit.next();
                if (!state.isVisited(targ)) {
                    state.markVertexParent(targ, vertexID);
                    state.classifyEdge(vertexID, targ, edgeClassifiedHandlers_);

                    visit(targ, vertexID, results, entryTimes, exitTimes, edgesFromSrc, depth + 1, state);
                }
                else {
                    state.classifyEdge(vertexID, targ, edgeClassifiedHandlers_);
                }
            }

            state.exitVertex(vertexID);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////

    /***************************************************************************
     **
     ** Returns a sorted list of links. Order is based on the comparator of the
     ** Link-class.
     */

    private ArrayList<Link> buildEdgeOrder(Set<Link> edgeSet) {
        ArrayList<Link> result= new ArrayList<Link>();
        result.addAll(edgeSet);
        Collections.sort(result);

        return result;
    }

    /***************************************************************************
     **
     ** Returns the target vertices of outbound edges from given source
     ** vertex.
     */

    private ArrayList<String> getOutEdges(String sourceID) {
        ArrayList<String> result = new ArrayList<String>();

        Iterator<Link> eit = edgeOrder_.iterator();
        while (eit.hasNext()) {
            Link link = eit.next();
            if (sourceID.equals(link.getSrc())) {
                result.add(link.getTrg());
            }
        }

        return result;
    }

    /***************************************************************************
     **
     ** Build map from node to outbound edges
     */

    private Map<String, ArrayList<String>> calcOutboundEdges(Set<Link> edges) {

        HashMap<String, ArrayList<String>> retval = new HashMap<String, ArrayList<String>>();
        Iterator<Link> li = edges.iterator();

        while (li.hasNext()) {
            Link link = li.next();
            String trg = link.getTrg();
            String src = link.getSrc();
            ArrayList<String> forSrc = retval.get(src);
            if (forSrc == null) {
                forSrc = getOutEdges(src);
                retval.put(src, forSrc);
            }
        }
        return (retval);
    }
}
