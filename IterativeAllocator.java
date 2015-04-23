package a2;

import java.util.*;

public class IterativeAllocator {

	/**
	 * @precondition: Neither of the inputs are null or contain null elements.
	 *                The parameter donations is a list of distinct donations
	 *                such that for each d in donations, d.getTotal() equals
	 *                d.getUnspent(); and for each p in projects
	 *                p.allocatedFunding() equals 0.
	 * @postcondition: returns false if there no way to completely fund all of
	 *                 the given projects using the donations, leaving both the
	 *                 input list of donations and set of projects unmodified;
	 *                 otherwise returns true and allocates to each project
	 *                 funding from the donations. The allocation to each
	 *                 project must be complete and may not violate the
	 *                 conditions of the donations.
	 */

	public static boolean canAllocate(List<Donation> donations,
			Set<Project> projects) {
		Graph g = createGraph(projects, donations);
		Set<List<Project>> paths = getPaths(projects, g);
		
		// Try allocation
		if(canCompleteAllocate(paths, donations)) {
			completeAllocate(paths, donations);
		}
		
		// Are all the projects fully funded?
		for(Project project: projects) {
			// No
			if(!project.fullyFunded()) {
				// Deallocate any allocations that we have tried to make
				for(Project p: projects) {
					p.deallocateAll();
				}
				return false;
			}
		}
		
		return true;
			
	}
	
	/*
	 * Check if there is an edge between p1 and p2.
	 */
	private static Edge findEdges(Project p1, Project p2, List<Donation> donations){
		// Is is an edge between p1 and p2?
		for(Donation donation: donations) {
			if(donation.canBeUsedFor(p1) == true && donation.canBeUsedFor(p2) == true) {
				if(donation.getUnspent() > 0) {
					Edge edge = new Edge(p1, p2);
					return edge;
				}
			}
		}
		
		return null;			
	}
	
	/*
	 * Create a graph and add edges to it.
	 */
	private static Graph createGraph(Set<Project> vertices, List<Donation> donations) {
		Graph g = new Graph(vertices);
		for(Project p1 : vertices) {
			for(Project p2: vertices) {
				if(!(p1.equals(p2))){
					Edge e = findEdges(p1, p2, donations);
					if(e != null) {
						g.insertEdge(e);
					}
				}
			}
		}
		return g;
	}
		
	/*
	 * Transfer donation along the path
	 */
	private static void transferAlongPath(int x, List<Project> path, List<Donation> donations) {
		int n = path.size();
		// for i=n-2 to 0 transferring x dollars of allocated funds from path[i] to path[i+1]
		for(int i = n - 2; i >= 0; i--) {
			path.get(i + 1).transfer(x, path.get(i));
		}
		
		// allocating x dollars from the available donations to project path[0]
		for(Donation donation: donations) {
			if(donation.canBeUsedFor(path.get(0))) {
				int allocateAmount = donation.getUnspent();
				if(allocateAmount > 0) {
					// Not need that much?
					if(x < allocateAmount) {
						allocateAmount = x;
					}
					if(x > 0) {
						path.get(0).allocate(donation, allocateAmount);
						// Need less and less
						x -= allocateAmount;
					}
				}
			}
		}
	}

	/*
	 * Complete all allocations
	 */
	private static void completeAllocate(Set<List<Project>> paths, List<Donation> donations) {
		for(Donation donation: donations){
			for(List<Project> path: paths) {
				// let x be how much the last project of the path which needs funding
				int x = path.get(path.size() - 1).neededFunds();
				// Not enough money for the donation to support the last project?
				if( donation.getUnspent() < x) {
					// let x be how much the donation is left
					x = donation.getUnspent();
				}
				if(pathConditionTest(x, path, donations)){
					transferAlongPath(x, path, donations);
				}
			}
		}
	}	
		
	/*
	 * A path DFS between two vertices
	 * Find a path from Project a to Project b
	 */
	private static boolean pathDFS(Graph g, Project a, Project b, List<Project> path, List<Project> known){
		known.add(a);
		path.add(a);
		// Does vertex a reach vertex b? Stop here
		if(a.equals(b)) {
			return true;
		}
		
		// Any outgoing edges for vertex a?
		for(Edge edge: g.outgoingEdges(a)){
			// Get the vertex associated with that edge
			Project vertex = edge.getDesP();
			// Has the vertex already been visited?
			if(!known.contains(vertex)){
				// No!
				known.add(vertex);
				if(pathDFS(g, vertex, b, path, known) == true) {
					return true;
				}
				// Can't form a path! Remove vertex b from path
				if(path.size() != 0) {
					path.remove(path.size() - 1);
				}
			}
		}
		
		// Can't form a path! Remove the vertex a from path
		if(path.size() != 0){
			path.remove(path.size() - 1);
		}
		return false;
	}

	/*
	 * Get a list of all the paths using path DFS
	 */
	private static Set<List<Project>> getPaths(Set<Project> vertices, Graph g) {
		Set<List<Project>> paths = new HashSet<List<Project>>();
		
		for(Project a: vertices) {
			for(Project b: vertices) {
				List<Project> path = new ArrayList<Project>();
				List<Project> known = new ArrayList<Project>();
				pathDFS(g, a, b, path, known);
					
				// Is a path between vertex a and vertex b???
				if(!(path.isEmpty())) {
					paths.add(path);
				}				
			}
		}
		return paths;
	}
	
	/*
	 * Check if the path satisfies (1)-(3)
	 */
	private static boolean pathConditionTest(int x, List<Project> path, List<Donation> donations){
		int numOfVertices = path.size();
		Project startVertice = path.get(0);
		Project endVertice = path.get(numOfVertices - 1);
		int underfunded = endVertice.neededFunds();
		
		if(x <= 0) {
			return false;
		}
		
		// (1)
		if(underfunded < x) {
			return false;
		}
		
		// (2)
		int canAllocateAmount = 0;
		for(Donation donation: donations) {
			if(donation.canBeUsedFor(startVertice)){
				canAllocateAmount += donation.getUnspent();
			}
		}
		if(canAllocateAmount < x) {
			return false;
		}
		
		// (3)
		for (int i = numOfVertices - 2; i >= 0; i--) {
			int availableAmount = 0;
			// Get the current allocations of path[i]
			Map<Donation, Integer> paCurrentAllocations = path.get(i).getAllocations();
			// What are the names of the donations?
			Set<Donation> donationAllocations = paCurrentAllocations.keySet();
			
			for(Donation donation: donationAllocations){
				if(donation.canBeUsedFor(path.get(i+1))) {
					// How much can be transfer from path[i] to path[i+1]?
					availableAmount += paCurrentAllocations.get(donation);
				}
			}
			
			//Must be at least x dollar!
			if (availableAmount < x) {
				return false;
			}
		}
		
		return true;
	}
	
	/*
	 * Check if there is a path satisfies (1)-(3)
	 */
	private static boolean canCompleteAllocate(Set<List<Project>> paths, List<Donation> donations){
		for(Donation donation: donations){
			for(List<Project> path: paths) {
				// let x be the last project of the path which needs funding
				int x = path.get(path.size() - 1).neededFunds();
				// Not enough money for the donation to support the last project?
				if( donation.getUnspent() < x) {
					// let x be the
					x = donation.getUnspent();
				}
				if(pathConditionTest(x, path, donations)){
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * An edge is composed of a start vertex and a destination vertex
	 */
 	private static class Edge {
		private Project projectStart;
		private Project projectDestin;
		
		private Edge(Project projectStart, Project projectDestin){
			this.projectStart = projectStart;
			this.projectDestin = projectDestin;
		}
		
		private Project getSrcP(){
			return projectStart;
		}
		
		private Project getDesP(){
			return projectDestin;
		}
			
	}
	
 	/*
 	 * A graph store a set of vertices and a list of edges
 	 */
	private static class Graph { 
		private Set<Project> vertices;
		private List<Edge> edges;

		
		private Graph(Set<Project> projects){
			this.vertices = projects;
			edges = new ArrayList<Edge>();
		}
						
		private List<Edge> outgoingEdges(Project project) {
			List<Edge> tempOutGoingEdges = new ArrayList<Edge>();
			for(Edge edge: edges) {
				if(edge.getSrcP().equals(project)) {
					tempOutGoingEdges.add(edge);
				}
			}
			return tempOutGoingEdges;
		}
				
		private void insertEdge(Edge edge){
			edges.add(edge);		
		}
								

				
	}
}
