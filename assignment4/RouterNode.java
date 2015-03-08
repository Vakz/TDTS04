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
      first_hop[i] = costs[i] == RouterSimulator.INFINITY ? myId : i;
    }
    printDistanceTable();
    RouterPacket pkt = new RouterPacket(myID, 0, distance_vector);
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      // Don't send updates to non-neighbors or self
      if (costs[i] == RouterSimulator.INIFINTY) continue;
      if (i == myID) continue;
      pkt.destid = i;
      sendUpdate(pkt);
    }
  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
    boolean anything_changed = false;
    for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
    {
      distance_cost = costs[pkt.sourceid] + pkt.mincosts[i];
      if (distance_cost < distance_vector[i])
      {
        distance_vector[i] = distance_cost;
        first_hop[i] = pkt.sourceid;
        anything_changed = true;
      }
    }

    if (anything_changed)
    {
      for (int i = 0; i < RouterSimulator.NUM_NODES; ++i)
      {
        // Don't send updates to non-neighbors or self
        if (costs[i] == RouterSimulator.INIFINTY) continue;
        if (i == myID) continue;
        pkt.destid = i;
        sendUpdate(pkt);
      }
    }
  }
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    // Never send updates to non-neighbors
    if (costs[pkt.destid] == RouterSimulator.INIFINITY) break;

    /* If Poison Reverse is active, each node will receieve a unique copy of the distance vector,
     in order to set first hop to inifinity. If Poison Reverse is not active, this is simply not necessary,
     and each node will just receieve a pointer to this' distance vector.
    */
    if (kPoisonReverse)
    {
      int[] tempArray = new int[RouterSimulator.NUM_NODES];
      for (int i = 0; i < RouterSimulator.NUM_NODES;  ++i)
      {
        tempArray[i] = (first_hop[i] == pkt.destid) ? RouterSimulator.INIFINITY : pkt.mincost[i];
      }
      pkt.mincost = tempArray;
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
            header += String.Format("%8i ", i);
          }
          header += "\n";
          for (int i = 0; i < RouterSimulator.NUM_NODES+1; ++i)
          {
            header += "--------";
          }
          myGUI.println(header);
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    costs[dest] = newcost;
    RouterPacket pkt = new RouterPacket(myID, myID, costs);
    recvUpdate(pkt);
  }

}
