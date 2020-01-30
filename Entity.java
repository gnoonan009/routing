import java.util.ArrayList;
import java.util.Arrays;

// Code for an entity in the network. This is where you should implement the
// distance-vector algorithm.

// Entity that represents a node in the network.
//
// Each function should be implemented so that the Entity can be instantiated
// multiple times and successfully run a distance-vector routing algorithm.
public class Entity {
	int index;
	int number_of_entities;
	
	int[] neighbors;
	int[][] routingTable;
	ArrayList<ArrayList<Integer>> paths;

	// This initialization function will be called at the beginning of the
	// simulation to setup all entities.
	//
	// Arguments:
	// - `entity_index`:    The id of this entity.
	// - `number_entities`: Number of total entities in the network.
	//
	// Return Value: None.
	public Entity(int entity_index, int number_of_entities) {
		
		this.index = entity_index;
		this.number_of_entities = number_of_entities;
		
		
		/*
		 * Setup routing table
		 * Any cell where row = column set to zero b/c an entity hop to itself has zero distance
		 * All other values set to 999
		 */
		routingTable = new int[number_of_entities][number_of_entities];
		paths = new ArrayList<ArrayList<Integer>>();
		
		
		for(int r = 0; r < number_of_entities; r++) {
			paths.add(new ArrayList<Integer>());
			if(r == index) {
				paths.get(r).add(r);
			}
			for(int c = 0; c < number_of_entities; c++) {
				if(r == c) {
					routingTable[r][c] = 0;
				}else {
					routingTable[r][c] = 999;
				}
			}
		}
		
		
	}
	
	public void printPaths(String text) {
		System.out.println(text);
		for(int i = 0; i < number_of_entities; i++) {
			System.out.println(i+"|"+paths.get(i).toString());
		}
		
	}
	
	public void printRoutingTable(String text) {
		System.out.print(text+"\n");
		System.out.print(" |");
		for(int x = 0; x < number_of_entities; x++) {
			System.out.print(" "+x+" |");
		}
		System.out.print("\n");
		for(int r = 0; r < number_of_entities; r++) {
			System.out.print(r+"|");
			for(int c = 0; c < number_of_entities; c++) {
				if(routingTable[r][c] < 100) {
					System.out.print(" "+routingTable[r][c]+" |");
				}else {
					System.out.print(routingTable[r][c]+"|");
				}
				
			}
			System.out.print("\n");
		}
		
		
	}
	
	public int[] getCosts(){
		int[] costs = new int[number_of_entities];
		
		for(int i = 0; i < number_of_entities; i++) {
			costs[i] = routingTable[index][i];
		}
		return costs;
	}
	

	// This function will be called at the beginning of the simulation to
	// provide a list of neighbors and the costs on those one-hop links.
	//
	// Arguments:
	// - `neighbor_costs`:  Array of (entity_index, cost) tuples for
	//                      one-hop neighbors of this entity in this network.
	//
	// Return Value: This function should return an array of `Packet`s to be
	// sent from this entity (if any) to neighboring entities.
	public Packet[] initialize_costs(Pair<Integer, Integer> neighbor_costs[]) {
		neighbors = new int[neighbor_costs.length];
		Packet[] packets = new Packet[neighbor_costs.length];
		
		for(int i = 0; i < neighbor_costs.length; i++) {
			neighbors[i] = neighbor_costs[i].x;
			//Variable for current neighbor in neighbor_costs[]
			Pair<Integer,Integer> neighbor = neighbor_costs[i];
			//Add each neighbor's cost to routing table
			routingTable[index][neighbor.x] = neighbor.y;
			//Set next hop to neighbor as the neighbor's index
			paths.get(neighbor.x).add(neighbor.x);
			//Create packet to send to current neighbor
			Packet packet = new Packet(neighbor.x, getCosts());
			//Add to packets
			packets[i] = packet;
		}
		//printRoutingTable("\nCosts initialized for entity "+index);
		//printPackets(packets);
		//printPaths("Initial paths for entity "+index);
		return packets;
	}
	
	public void printPackets(Packet[] packets) {
		for(int i = 0; i < packets.length; i++) {
			System.out.print("\nEntity "+index+ " sending packet to "+packets[i].get_destination()+"\n");
		}
	}
	
	public boolean shouldRecalculate(Packet packet) {		
		for(int i = 0; i < number_of_entities; i++) {
			if(routingTable[packet.get_source()][i] != packet.get_costs()[i]) {
				return true;
			}
		}
		return false;
	}
	
	public void updateOtherEntityDV(Packet packet) {
		int row = packet.get_source();
		
		for(int i = 0; i < number_of_entities; i++) {
			routingTable[row][i] = packet.get_costs()[i];
		}
	}
	
	

	// This function is called when a packet arrives for this entity.
	//
	// Arguments:
	// - `packet`: The incoming packet of type `Packet`.
	//
	// Return Value: This function should return an array of `Packet`s to be
	// sent from this entity (if any) to neighboring entities.
	public Packet[] update(Packet packet) {

		
		//Check if distance vector received is identical to contents of routing table row
		if(!shouldRecalculate(packet)) {
			return new Packet[0];
		}
		
		/*
		 * If this entity receives a different DV than previously stored for that neighbor in the routing table
		 * Update that entity's DV in the current entity's routing table
		 */
		updateOtherEntityDV(packet);
		
		/*
		 * Adjust current entity's DV with data from packet
		 * Use Bellman-Ford Algorithm
		 */
		
		//Cost from current entity to packet source entity
		int costToEntity = routingTable[index][packet.get_source()];
		
		for(int y = 0; y < number_of_entities; y++) {
			//Distance from current entity to source + cost from source to y
			int costThroughEntity = costToEntity + packet.get_costs()[y];
			//If that distance is less than current value in routingTable, replace
			if(costThroughEntity < routingTable[index][y]) {
				//If there's an intermediate node, we need to add its path
				ArrayList<Integer> temp = new ArrayList<Integer>();
				temp.add(packet.get_source());
				paths.set(y, temp);
				
				routingTable[index][y] = costThroughEntity;
				//This part needs to be fixed
				
				
			}
		}
		//printPaths("Paths after entity "+index+ " received packet from entity "+packet.get_source());
		//printRoutingTable("Entity "+index+" after receiving a packet from entity "+packet.get_source());
		
		/*
		 * Re-send updated DV to neighbors
		 */
		Packet[] packets = new Packet[neighbors.length];
		for(int i = 0; i < neighbors.length; i++) {
			Packet p = new Packet(neighbors[i], getCosts());
			packets[i] = p;
		}
		//printPackets(packets);
		return packets;

	}
	
	public class CustomPair
	{
	    private final Integer next_hop;
	    private final Integer cost;

	    public CustomPair(Integer nH, Integer c)
	    {
	    	next_hop = nH;
	        cost = c;
	    }

	    public Integer key()   { return next_hop; }
	    public Integer value() { return cost; }
	}
	
	void printCosts(Pair<Integer, Integer>[] list) {
		for(int i = 0; i < list.length; i++) {
			System.out.print("("+list[i].x+","+list[i].y+")");
		}
	}


	// This function is used by the simulator to retrieve the calculated routes
	// and costs from an entity. This is most useful at the end of the
	// simulation to collect the resulting routing state.
	//
	// Return Value: This function should return an array of (next_hop, cost)
	// tuples for _every_ entity in the network based on the entity's current
	// understanding of those costs. The array should be sorted such that the
	// first element of the array is the next hop and cost to entity index 0,
	// second element is to entity index 1, etc.
	public Pair<Integer, Integer>[] get_all_costs() {
		Pair<Integer, Integer>[] costs = (Pair<Integer, Integer>[]) new Pair[number_of_entities];

		
		for(int i = 0; i < number_of_entities; i++) {
			costs[i] = new Pair<Integer,Integer>(forward_next_hop(i), routingTable[index][i]);
		}
		return costs;

	}

	// Return the best next hop for a packet with the given destination.
	//
	// Arguments:
	// - `destination`: The final destination of the packet.
	//
	// Return Value: The index of the best neighboring entity to use as the
	// next hop.
	public int forward_next_hop(int destination) {
		int hop = paths.get(destination).get(0);
		
		while(hop != paths.get(hop).get(0)) {
			hop = paths.get(hop).get(0);
			
		}
		return hop;
	}
}
