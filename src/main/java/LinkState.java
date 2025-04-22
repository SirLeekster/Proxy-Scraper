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
    
    // Node class: represents a vertex in the network
    static class Node {
        int nodeNumber;
        
        public Node(int nodeNumber) {
            this.nodeNumber = nodeNumber;
        }
        
        public int getNodeNumber() {
            return nodeNumber;
        }
        
        @Override
        public String toString() {
            return String.valueOf(nodeNumber);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return nodeNumber == node.nodeNumber;
        }
        
        @Override
        public int hashCode() {
            return Integer.hashCode(nodeNumber);
        }
    }
    
    // Edge class: represents a link between nodes with an associated cost
    static class Edge {
        Node destination;
        double cost;
        
        public Edge(Node destination, double cost) {
            this.destination = destination;
            this.cost = cost;
        }
    }
    
    // Entry class: helper for the priority queue in Dijkstra’s algorithm.
    // It stores a node and the current known distance from the source.
    static class Entry implements Comparable<Entry> {
        Node node;
        double distance;
        
        public Entry(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(Entry other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    
    // Graph representation: maps a node to its list of adjacent edges.
    Map<Node, List<Edge>> graph = new HashMap<>();
    
    // Data structures for Dijkstra's algorithm:
    // distances: shortest known distance from the source to each node.
    // previous: used to reconstruct the shortest path.
    Map<Node, Double> distances = new HashMap<>();
    Map<Node, Node> previous = new HashMap<>();
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java LinkState <network file> <source node id>");
            return;
        }
        String filename = args[0];
        int sourceId = Integer.parseInt(args[1]);
        
        LinkState ls = new LinkState();
        ls.readFile(filename);
        ls.runDijkstra(sourceId);
        ls.printPaths(sourceId);
    }
    
    // Reads the network topology from a file.
    // First line: number of nodes (n). Nodes are assumed to be 0,1,...,n-1.
    // Each subsequent line: "n1 n2 cost", representing a bidirectional link.
    private void readFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();
            if (line == null) {
                System.out.println("Empty file");
                return;
            }
            int numNodes = Integer.parseInt(line.trim());
            
            // Create nodes and initialize graph entries.
            Map<Integer, Node> nodeMap = new HashMap<>();
            for (int i = 0; i < numNodes; i++) {
                Node node = new Node(i);
                nodeMap.put(i, node);
                graph.put(node, new ArrayList<>());
            }
            
            // Read each link and add edges in both directions (bidirectional)
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
                
                // Add edge from node1 to node2 and vice versa.
                graph.get(node1).add(new Edge(node2, cost));
                graph.get(node2).add(new Edge(node1, cost));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error opening file: " + e);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e);
        }
    }
    
    // Runs Dijkstra's algorithm from the source node.
    private void runDijkstra(int sourceId) {
        PriorityQueue<Entry> pq = new PriorityQueue<>();
        
        // Initialize distances: source node distance is 0; others are infinity.
        for (Node node : graph.keySet()) {
            if (node.getNodeNumber() == sourceId) {
                distances.put(node, 0.0);
                pq.add(new Entry(node, 0.0));
            } else {
                distances.put(node, Double.MAX_VALUE);
            }
            previous.put(node, null);
        }
        
        // Process nodes until the priority queue is empty.
        while (!pq.isEmpty()) {
            Entry currentEntry = pq.poll();
            Node currentNode = currentEntry.node;
            double currentDistance = currentEntry.distance;
            
            // If this entry is outdated, skip it.
            if (currentDistance > distances.get(currentNode)) continue;
            
            // Relax the edges.
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
    
    // Reconstructs and prints the shortest path from the source to every other node.
    private void printPaths(int sourceId) {
        for (Node node : graph.keySet()) {
            if (node.getNodeNumber() == sourceId) continue;
            double cost = distances.get(node);
            if (cost == Double.MAX_VALUE) {
                System.out.println("Node " + node + " is unreachable from source " + sourceId);
            } else {
                List<Node> path = new ArrayList<>();
                for (Node at = node; at != null; at = previous.get(at)) {
                    path.add(at);
                }
                Collections.reverse(path);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < path.size(); i++) {
                    sb.append(path.get(i));
                    if (i < path.size() - 1) {
                        sb.append("->");
                    }
                }
                System.out.println("shortest path to node " + node + " is " + sb.toString() + " with cost " + cost);
            }
        }
    }
}
