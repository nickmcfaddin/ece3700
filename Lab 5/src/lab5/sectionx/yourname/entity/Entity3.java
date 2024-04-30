package lab5.sectionx.yourname.entity;
import lab5.sectionx.yourname.logic.NetworkSimulator;
public class Entity3 extends Entity
{    
    // Perform any necessary initialization in the constructor
    int[] connections = new int[distanceTable.length];              //known connections stored here
    
    // Perform any necessary initialization in the constructor
    public Entity3()
    {
        System.out.println("At time: " + NetworkSimulator.time + ". Creating Node 3.");
        
        // Initialize infinite distance for each entry of distance table
        for (int i = 0; i < distanceTable.length; i++){
            for (int j = 0; j < distanceTable.length; j++){
                distanceTable[i][j] = 999;
            }
        }
        
        // Initialize distance table with known values from Node 3
        distanceTable[0][0] = 7;
        distanceTable[2][2] = 2;
        distanceTable[3][3] = 0;
        
        // Initialize known connections
        connections[0] = 7;
        connections[1] = 999;
        connections[2] = 2;
        connections[3] = 0;
        
        // Create packet to send to each neighbour
        Packet packet1 = new Packet(3, 0, connections);
        Packet packet2 = new Packet(3, 2, connections);
        
        // Send created packets to each neighbour
        NetworkSimulator.toLayer2(packet1);
        NetworkSimulator.toLayer2(packet2);
        
        // Print Node 3's distance table
        System.out.println("At time: " + NetworkSimulator.time + ". Initial Node 3 distance table:");
        printDT();
    }
    
    // Handle updates when a packet is received.  Students will need to call
    // NetworkSimulator.toLayer2() with new packets based upon what they
    // send to update.  Be careful to construct the source and destination of
    // the packet correctly.  Read the warning in NetworkSimulator.java for more
    // details.
    public void update(Packet p)
    {
        // Get source and destination of received packet
        int source = p.getSource();
        int destination = p.getDest();
        
        System.out.println("At time: " + NetworkSimulator.time + ". Node " + destination + " received routing packet from node " + source + ".");
        
        // For each row in the distance table if the value in the table is greater than value from the received packet + distance from source Node, update the Node to the new minimum
        for (int i = 0; i < distanceTable.length; i++){
            if (distanceTable[i][source] > distanceTable[source][source] + p.getMincost(i)){
                
                System.out.println("At time: " + NetworkSimulator.time + ". Update occurs at Node " + destination + ".");
                
                // Alter the value in the distance table to the smaller of the two if it needs to be
                distanceTable[i][source] = distanceTable[source][source] + p.getMincost(i);
                
                // Create an array to store the new minimum distance from the Node, fill it with 0's and infinity's depending on if it is the destination Node or not
                int[] newMins = new int[distanceTable.length];
                for (int j = 0; j < newMins.length; j++){
                    if (j == destination){
                        newMins[j] = 0;
                    } 
                    else{
                        newMins[j] = 999;
                    }
                }
                
                // Fill the array with the minimums from the distance table
                for (int j = 0; j < distanceTable.length ; j++){
                    for (int k = 0; k < distanceTable.length ; k++){
                        if (newMins[j] > distanceTable[j][k]){
                            newMins[j] = distanceTable[j][k];
                        }
                    }
                }
                
                // For each of our known connections that are not the destination or an unknown
                for (int j = 0; j < connections.length; j++){
                    if (connections[j] != 0 && connections[j] != 999){
                        
                        System.out.println("At time: " + NetworkSimulator.time + ". Sending updated minimum costs for Node " + destination + " to Node " + j + ".");
                        
                        // Encapsulate and send the packet to the adjacent node with the updated travel time
                        Packet packet = new Packet(destination,j,newMins);
                        NetworkSimulator.toLayer2(packet);
                    }
                }
                
                // Print the distance table out for this Node
                System.out.println("New distance table at Node " + destination + ":");
                printDT();
                System.out.println();
            }
        }
    }
    
    public void linkCostChangeHandler(int whichLink, int newCost){}
    
    public void printDT()
    {
        System.out.println("         via");
        System.out.println(" D3 |   0   2");
        System.out.println("----+--------");
        for (int i = 0; i < NetworkSimulator.NUMENTITIES; i++)
        {
            if (i == 3)
            {
                continue;
            }
            
            System.out.print("   " + i + "|");
            for (int j = 0; j < NetworkSimulator.NUMENTITIES; j += 2)
            {
               
                if (distanceTable[i][j] < 10)
                {    
                    System.out.print("   ");
                }
                else if (distanceTable[i][j] < 100)
                {
                    System.out.print("  ");
                }
                else 
                {
                    System.out.print(" ");
                }
                
                System.out.print(distanceTable[i][j]);
            }
            System.out.println();
        }
    }
}
