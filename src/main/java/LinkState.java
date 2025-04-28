import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class LinkState {
    
    //node class
    static class Node {
        int nodeNumber;
        
        public Node(int nodeNumber) {
            this.nodeNumber = nodeNumber;
        }

        public int getNodeId() {
            return nodeNumber;
        }
        
        public String printNode() {
            return String.valueOf(nodeNumber);
        }
        
        public boolean isSameNode(Node other) {
            if (other == null) return false;
            return this.nodeNumber == other.nodeNumber;
        }
    }
    
    //edge class
    static class Edge {
        Node destination;
        double cost;
        
        public Edge(Node destination, double cost) {
            this.destination = destination;
            this.cost = cost;
        }
    }
    
    //entry class for priority queue
    static class Entry implements Comparable<Entry> {
        Node node;
        double distance;
        
        public Entry(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
        
        public int compareTo(Entry other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    
    //graph is a map from node to list of edges
    Map<Node, List<Edge>> graph = new HashMap<>();
    
    Map<Node, Double> distances = new HashMap<>();
    Map<Node, Node> previous = new HashMap<>();
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: java LinkState <network file> <source node id>");
            return;
        }
        String filename = args[0];
        int sourceId = Integer.parseInt(args[1]);
        
        LinkState ls = new LinkState();
        ls.readFile(filename);
        ls.runDijkstra(sourceId);
        ls.printPaths(sourceId);
    }
    
    //read the network file
    private void readFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();
            if (line == null) {
                System.out.println("empty file");
                return;
            }
            int numNodes = Integer.parseInt(line.trim());
            
            //create nodes
            Map<Integer, Node> nodeMap = new HashMap<>();
            for (int i = 0; i < numNodes; i++) {
                Node node = new Node(i);
                nodeMap.put(i, node);
                graph.put(node, new ArrayList<>());
            }
            
            //read each link
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;
                int n1 = Integer.parseInt(parts[0]);
                int n2 = Integer.parseInt(parts[1]);
                double cost = Double.parseDouble(parts[2]);
                
                Node node1 = nodeMap.get(n1);
                Node node2 = nodeMap.get(n2);
                if (node1 == null || node2 == null) continue;
                
                graph.get(node1).add(new Edge(node2, cost));
                graph.get(node2).add(new Edge(node1, cost));
            }
        } catch (FileNotFoundException e) {
            System.out.println("error opening file: " + e);
        } catch (IOException e) {
            System.out.println("error reading file: " + e);
        }
    }
    
    //run dijkstra from source node
    private void runDijkstra(int sourceId) {
        PriorityQueue<Entry> pq = new PriorityQueue<>();
        
        for (Node node : graph.keySet()) {
            if (node.getNodeId() == sourceId) {
                distances.put(node, 0.0);
                pq.add(new Entry(node, 0.0));
            } else {
                distances.put(node, Double.MAX_VALUE);
            }
            previous.put(node, null);
        }
        
        while (!pq.isEmpty()) {
            Entry currentEntry = pq.poll();
            Node currentNode = currentEntry.node;
            double currentDistance = currentEntry.distance;
            
            if (currentDistance > distances.get(currentNode)) continue;
            
            for (Edge edge : graph.get(currentNode)) {
                Node neighbor = edge.destination;
                double newDist = distances.get(currentNode) + edge.cost;
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, currentNode);
                    pq.add(new Entry(neighbor, newDist));
                }
            }
        }
    }
    
    //print all paths from source
    private void printPaths(int sourceId) {
        for (Node node : graph.keySet()) {
            if (node.getNodeId() == sourceId) continue;
            double cost = distances.get(node);
            if (cost == Double.MAX_VALUE) {
                System.out.println("node " + node.printNode() + " is unreachable from source " + sourceId);
            } else {
                List<Node> path = new ArrayList<>();
                for (Node at = node; at != null; at = previous.get(at)) {
                    path.add(at);
                }
                Collections.reverse(path);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < path.size(); i++) {
                    sb.append(path.get(i).printNode());
                    if (i < path.size() - 1) {
                        sb.append("->");
                    }
                }
                System.out.println("shortest path to node " + node.printNode() + " is " + sb.toString() + " with cost " + cost);
            }
        }
    }
}
