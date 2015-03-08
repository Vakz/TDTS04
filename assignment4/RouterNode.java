import javax.swing.*;        

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  final private boolean kPoisonReverse = true;
  private int[] costs = new int[RouterSimulator.NUM_NODES]; // Physical costs
  private int[] distance_vector = new int[RouterSimulator.NUM_NODES]; // Calculated distance vector
  private int[] first_hop = new int[RouterSimulator.NUM_NODES]; // First hop for each path

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] costs) {
    myID = ID;
    this.sim = sim;
    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");

    System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);
    System.arraycopy(costs, 0, this.distance_vector, 0, RouterSimulator.NUM_NODES);

    // At this point, only routes to neighbors are known, and thus
    // the first hop of any route will be that neighbor.
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      first_hop[i] = costs[i] == RouterSimulator.INFINITY ? myID : i;
    }
    printDistanceTable();
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      broadcastUpdates();
    }
  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
    boolean anything_changed = false;
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      int distance_cost = costs[pkt.sourceid] + pkt.mincost[i];
      if (distance_cost < distance_vector[i])
      {
        distance_vector[i] = distance_cost;
        first_hop[i] = pkt.sourceid;
        anything_changed = true;
      }
    }

    if (anything_changed) broadcastUpdates();
  }

  private void broadcastUpdates()
  {
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      // Don't send updates to non-neighbors or self
      if (costs[i] == RouterSimulator.INFINITY) continue;
      if (i == myID) continue;
      int[] tempArray = new int[RouterSimulator.NUM_NODES];
      System.arraycopy(distance_vector, 0, tempArray, 0, RouterSimulator.NUM_NODES);
      RouterPacket pkt = new RouterPacket(myID, i, tempArray);
      
      sendUpdate(pkt);
    }
  }
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    // Never send updates to non-neighbors
    
    if (costs[pkt.destid] == RouterSimulator.INFINITY) return;
    
    if (kPoisonReverse)
    {
      for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
      {
        if (first_hop[i] == pkt.destid) pkt.mincost[i] = RouterSimulator.INFINITY;
      }
    }
    
    sim.toLayer2(pkt);

  }
  

  //--------------------------------------------------
  public void printDistanceTable() {
	  myGUI.println("Current table for " + myID +
			"  at time " + sim.getClocktime());
          myGUI.println("DistanceTable");
          String header = String.format("%8s |", "dist");
          for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
          {
            header += String.format("%8s", Integer.toString(i));
          }
          header += "\n";
          for (int i = 0; i < RouterSimulator.NUM_NODES+2; ++i)
          {
            header += "---------";
          }
          myGUI.println(header);
          String line = String.format(" %-8s|", "cost");
          for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
          {
            line += String.format("%8s", Integer.toString(distance_vector[i]));
          }
          myGUI.println(line);
          line = String.format(" %-8s|", "route");
          for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
          {
            line += String.format("%8s", Integer.toString(first_hop[i]));
          }
          myGUI.println(line + "\n\n");
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    costs[dest] = newcost;
    RouterPacket pkt = new RouterPacket(myID, myID, costs);
    recvUpdate(pkt);
  }

}
