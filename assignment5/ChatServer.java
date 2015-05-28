import ChatApp.*;          // The package containing our stubs.
import org.omg.CosNaming.*; // HelloServer will use the naming service.
import org.omg.CosNaming.NamingContextPackage.*; // ..for exceptions.
import org.omg.CORBA.*;     // All CORBA applications need these classes.
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.util.HashMap;
import java.util.Map;

class ChatImpl extends ChatPOA
{
    private ORB orb;
    private HashMap<ChatCallback, String> registered_users =
      new HashMap<ChatCallback, String>();

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public void join(ChatCallback callobj, String name)
    {
      for (Map.Entry<ChatCallback, String> entry : registered_users.entrySet())
      {
        if (entry.getKey().equals(callobj))
        {
          sendMsg(callobj, "You are already registered", true);
          return;
        }
        if (entry.getValue().equals(name))
        {
          sendMsg(callobj, "Name already in use", true);
          return;
        }
      }
      sendMsgAll(name + " has joined the chat", true);
      registered_users.put(callobj, name);
      sendMsg(callobj, "Welcome " + name, true);
    }

    public void post(ChatCallback callobj, String msg)
    {
      String name = registered_users.get(callobj);
      if (name == null)
      {
        sendMsg(callobj, "You are not registered on the server", true);
        return;
      }
      sendMsgAll("<" + name + ">" + msg, false);
    }

    public void leave(ChatCallback callobj)
    {
      String name = registered_users.get(callobj);
      if (name == null)
      {
        sendMsg(callobj, "You are not registered on the server", true);
        return;
      }
      registered_users.remove(callobj);
      sendMsgAll(name + " has left the chat", true);
      sendMsg(callobj, "Bye " + name, true);
    }

    public void list(ChatCallback callobj)
    {
      sendMsg(callobj, "Currently online users:", true);
      for (String name : registered_users.values())
      {
        sendMsg(callobj, name, true);
      }

    }

    public void game(ChatCallback cc, String marker)
    {

    }

    public void place(ChatCallback cc, String coords)
    {

    }

    private void sendMsgAll(String msg, boolean systemMsg)
    {
      for (ChatCallback client : registered_users.keySet())
      {
        sendMsg(client, msg, systemMsg);
      }
    }

    private void sendMsg(ChatCallback client, String msg,
      boolean systemMsg)
    {
      msg = systemMsg ? "!-- " + msg : msg;
      client.callback(msg);
    }

}

public class ChatServer
{
    public static void main(String args[])
    {
	try {
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);

	    // create servant (impl) and register it with the ORB
	    ChatImpl chatImpl = new ChatImpl();
	    chatImpl.setORB(orb);

	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa =
		POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
	    rootpoa.the_POAManager().activate();

	    // get the root naming context
	    org.omg.CORBA.Object objRef =
		           orb.resolve_initial_references("NameService");
	    NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

	    // obtain object reference from the servant (impl)
	    org.omg.CORBA.Object ref =
		rootpoa.servant_to_reference(chatImpl);
	    Chat cref = ChatHelper.narrow(ref);

	    // bind the object reference in naming
	    String name = "Chat";
	    NameComponent path[] = ncRef.to_name(name);
	    ncRef.rebind(path, cref);

	    // Application code goes below
	    System.out.println("ChatServer ready and waiting ...");

	    // wait for invocations from clients
	    orb.run();
	}

	catch(Exception e) {
	    System.err.println("ERROR : " + e);
	    e.printStackTrace(System.out);
	}

	System.out.println("ChatServer Exiting ...");
    }

}
