import ChatApp.*;          // The package containing our stubs
import org.omg.CosNaming.*; // HelloClient will use the naming service.
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;     // All CORBA applications need these classes.
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class ChatCallbackImpl extends ChatCallbackPOA
{
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public void callback(String notification)
    {
        System.out.println("\r" + notification);
        System.out.print("% ");
    }
}

public class ChatClient
{
    static Chat chatImpl;

    public static void main(String args[])
    {
	    try {
	       // create and initialize the ORB
	      ORB orb = ORB.init(args, null);

	      // create servant (impl) and register it with the ORB
	      ChatCallbackImpl chatCallbackImpl = new ChatCallbackImpl();
	      chatCallbackImpl.setORB(orb);

	      // get reference to RootPOA and activate the POAManager
	      POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootpoa.the_POAManager().activate();

	      // get the root naming context
	      org.omg.CORBA.Object objRef =
          orb.resolve_initial_references("NameService");
	      NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

	      // resolve the object reference in naming
	      String name = "Chat";
	      chatImpl = ChatHelper.narrow(ncRef.resolve_str(name));

	      // obtain callback reference for registration w/ server
	      org.omg.CORBA.Object ref =
		      rootpoa.servant_to_reference(chatCallbackImpl);
	      ChatCallback cref = ChatCallbackHelper.narrow(ref);

	      // Application code goes below
	      run(cref);
	    }
    catch(Exception e){
	    System.out.println("ERROR : " + e);
	    e.printStackTrace(System.out);
	  }
  }

  public static void run(ChatCallback cref)
  {
    BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
    String line;

    boolean running = true;

    while(running)
    {
      try{
        System.out.print("\r% ");
        line = read.readLine();
        if (line.equals("quit")) running = false;
        else handleCommand(cref, line);
      }
      catch (IOException e)
      {
        System.out.println("Error taking input");
        e.printStackTrace(System.out);
        System.exit(1);
      }
    }
  }

  public static void handleCommand(ChatCallback cref, String cmd)
  {
    String[] command_words = cmd.split(" ");
    switch(command_words[0])
    {
      case "join":
        if (command_words.length < 2)
        {
          System.out.println("!-- Must enter a name to join");
        }
        else
        {
          chatImpl.join(cref, command_words[1]);
        }
        break;
      case "post":
        int index = cmd.indexOf(" ");
        if (index == -1) break;
        chatImpl.post(cref, cmd.substring(index));
      break;
      case "leave":
        chatImpl.leave(cref);
      break;
      case "list":
        chatImpl.list(cref);
      break;
      case "fiveinarow":
        String error_marker = "!-- Must select a marker (X/O)";
        if (command_words.length < 2)
        {
          System.out.println(error_marker);
        }
        else if (command_words[0].toLowerCase().equals("x") ||
          command_words[0].toLowerCase().equals("o"))
        {
            System.out.println(error_marker);
        }
        else{
          chatImpl.game(cref, command_words[0]);
        }
      break;
      case "place":
        String error_coords = "!-- Must enter coordinates (e.g. A0)";
        if (command_words.length < 2)
        {
          System.out.println(error_coords);
          break;
        }
        Pattern p = Pattern.compile("^\\w\\d$");
        Matcher m = p.matcher(command_words[1]);
        if(!m.find())
        {
          System.out.println(error_coords);
        }
        else{
          chatImpl.place(cref, command_words[1]);
        }
      break;
      default:
        System.out.println("!-- Unknown command");

    }
  }
}
