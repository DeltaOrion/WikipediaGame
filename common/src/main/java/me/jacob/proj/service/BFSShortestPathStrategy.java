package me.jacob.proj.service;

import me.jacob.proj.model.WikiPage;

import java.util.*;

public class BFSShortestPathStrategy implements ShortestPathStrategy {

    @Override
    public List<List<WikiPage>> getShortestPaths(WikiPage start, WikiPage end) {
        /*
         * TODO
         *   - Cache results and evict on update.
         */

        List<List<WikiPage>> result = new ArrayList<>();

        //deal with edge case when we are travelling to and from the same node
        if(start.equals(end)) {
            List<WikiPage> path = new ArrayList<>();
            path.add(end);
            result.add(path);
            return result;
        }

        //init BFS, layer order traversal
        Queue<Node> queue = new ArrayDeque<>();
        Node startNode = new Node(start,true);
        Map<WikiPage,Node> nodeMap = new HashMap<>();
        queue.add(startNode);

        int layer = 0;
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            //get the amount of nodes in the layer
            int size = queue.size();

            //loop through all of the nodes in the layer
            for(int i=0; i<size;i++) {
                Node node = queue.poll();
                for(WikiPage neighbour : node.page.getNeighbours()) {
                    if(neighbour.isRemoved())
                        continue;
                    //we want the node to add itself to the list for all of its neighbours if either
                    //the node is unvisited or if it has been visited, it was added in this layer!
                    Node neighbourNode = getOrMake(neighbour,nodeMap);
                    if(!neighbourNode.visited) {
                        //mark that we have visited and which layer we visited on
                        queue.add(neighbourNode);
                        neighbourNode.prev.add(node);
                        neighbourNode.visited = true;
                        neighbourNode.visitLayer = layer;
                    } else if (neighbourNode.visitLayer == layer) {
                        //if we already visited the node in this layer add to the junction
                        neighbourNode.prev.add(node);
                    }

                    if(neighbourNode.page.equals(end))
                        found = true;
                }
            }

            layer++;
        }

        Node finalNode = nodeMap.get(end);
        if(finalNode==null)
            return new ArrayList<>();

        //use backtracking to convert the node into usable shortest paths!
        convertToPaths(finalNode,result,new ArrayList<>());

        return result;
    }

    private void convertToPaths(Node node, List<List<WikiPage>> result, List<WikiPage> currPath) {
        currPath.add(node.page);
        if(node.start) {
            //termination condition, we have reached the start of the line
            List<WikiPage> path = new ArrayList<>();
            //reverse the generated path (we went backwards) and add the start node to the front
            for(int i=currPath.size()-1;i>=0;i--) {
                path.add(currPath.get(i));
            }
            //add the shortest path
            result.add(path);
            return;
        }

        for(Node page : node.prev) {
            //add the page, go one depth down and then backtrack
            convertToPaths(page,result,currPath);
            currPath.remove(currPath.size()-1);
        }
    }

    private Node getOrMake(WikiPage page, Map<WikiPage,Node> nodeMap) {
        Node node = nodeMap.get(page);
        if(node==null) {
            node = new Node(page,false);
            nodeMap.put(page,node);
        }
        return node;
    }

    private class Node {

        private WikiPage page;
        private List<Node> prev;
        private boolean visited;
        private int visitLayer;
        private boolean start;

        public Node(WikiPage page, boolean start) {
            this.page = page;
            this.start = start;
            this.prev = new ArrayList<>();
            visited = false;
            this.visitLayer = 0;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "page=" + page.getTitle() +
                    '}';
        }
    }
}
