import ProjectUtils.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unchecked")
public class ClientMain extends RemoteObject implements NotifyEventInterface{
    private static final long serialVersionUID = 3330110084717693575L;
    private List<NicknameStatusPair> systemUsers; //Listing users in the system
    private static final int RMIport = 16617; //RMI port
    private static final int TCPport = 20700; //TCP port for connection
    private static final String ServerAddress = "127.0.0.1";
    private String userName; //Saving the logged in userName
    private final List<multicastConnectInfo> Multicastsockets; //List of MulticastSocket Info

    public ClientMain(){
        super();
        this.userName = null; //Current userName connected
        this.Multicastsockets = new ArrayList<>(); //List of multicast sockets for projects chat
    }

    public void start(){
        boolean loggedIn = false; //Checks if the
        boolean cycle = true;
        SocketChannel socketChannel; //SocketChannel for TCP
        try {
            //Setup RMI
            Registry registry = LocateRegistry.getRegistry(RMIport);
            ServerMainInterfaceRMI stub = (ServerMainInterfaceRMI) registry.lookup("ServerRMI");
            //Input scanner
            Scanner in = new Scanner(System.in); //
            //Setup server connection
            socketChannel = SocketChannel.open(); //Apertura socket
            socketChannel.connect(new InetSocketAddress(ServerAddress, TCPport));
            //Setup Callback System
            NotifyEventInterface callbackObj = this;
            NotifyEventInterface stubCallBack = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
            //Buffer for receive datagrams
            byte[] buffer = new byte[8192];
            while(cycle){
                String command = in.nextLine();
                String[] splittedCommand = command.split(" ");

                switch(splittedCommand[0].toLowerCase()) {
                    case "register":
                        register(splittedCommand,stub);
                        break;
                    case "login":
                        if(loggedIn){ //Checks if the client is logged in the system
                            System.out.println("You are already logged in! Logout before please.");
                            break;
                        }
                        boolean result = login(command,socketChannel);
                        if (result){ //If the login went ok -> Register for callbacks
                            loggedIn = true;
                            System.out.println("Registering for callback");
                            stub.registerForCallback(stubCallBack,splittedCommand[1]); //Register for callbacks
                        }
                        break;

                    case "logout":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        result = logout(command,socketChannel);
                        if (result){ //if the logout went ok -> Unregister for callbacks
                            loggedIn = false;
                            System.out.println("Unregistering for callback");
                            stub.unregisterForCallback(stubCallBack); //Unregister for callbacks
                        }
                        break;

                    case "listusers":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listusers();
                        break;

                    case "listonlineusers":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listonlineusers();
                        break;

                    case "listprojects":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listprojects(command,socketChannel);
                        break;

                    case "createproject":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        createProject(command,socketChannel);
                        break;

                    case "addmember":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        addMember(command,socketChannel);
                        break;

                    case "showmembers":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showMembers(command,socketChannel);
                        break;

                    case "showcards":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showCards(command,socketChannel);
                        break;

                    case "showcard":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showCard(command,socketChannel);
                        break;

                    case "addcard":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        System.out.print("Insert a description of the card: ");
                        String description = in.nextLine();
                        addCard(command,description,socketChannel);
                        break;

                    case "movecard":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        moveCard(command,socketChannel);
                        break;

                    case "getcardhistory":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        getCardHistory(command,socketChannel);
                        break;

                    case "cancelproject":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        cancelProject(command,socketChannel);
                        break;

                    case "readchat":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        readChat(command,buffer, socketChannel);
                        break;

                    case "sendchatmsg":
                        if(!loggedIn) { //Checks if the client is logged in the system
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        sendChatmsg(command,in, socketChannel);
                        break;

                    case "help":
                        help();
                        break;

                    case "exit": //If the command is "exit", client shutdown
                        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
                        System.out.println("Doing the exit!");
                        cycle = false;
                        break;

                    default: //All others commands
                        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
                        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
                        String response = ((String) ois.readObject()).trim();
                        System.out.println(response);
                        break;

                }

            }

            System.out.println("Unregistering for callback");
            stub.unregisterForCallback(stubCallBack);

            System.exit(0);




        }catch (Exception e) {  e.printStackTrace(); }
    }

    public void register(String[] splittedCommand, ServerMainInterfaceRMI stub) throws RemoteException {
        String result;
        if(splittedCommand.length<3) result=stub.register("","");
        else if(splittedCommand.length>3) result = "Too much arguments";
        else result = stub.register(splittedCommand[1],splittedCommand[2]); //Calls the RMI method of the Server for registration
        System.out.println(result);
    }

    public boolean login(String command, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String[] splitcommand = command.split(" ");
        LoginResult<NicknameStatusPair> loginResult;
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        loginResult = (LoginResult<NicknameStatusPair>) ois.readObject();
        if(loginResult.getCode().equalsIgnoreCase("OK")) {
            System.out.println("OK");
            systemUsers = loginResult.getList(); //Gets the registered user list
            userName = splitcommand[1];
            if(loginResult.getMulticastinfo()!=null){ //Prepares the projects chats
                for(multicastINFO minfo : loginResult.getMulticastinfo())
                {
                    MulticastSocket ms;
                    try {
                        ms = new MulticastSocket(minfo.getPort());
                        ms.joinGroup(InetAddress.getByName(minfo.getIpAddress()));
                        ms.setSoTimeout(2000);
                        Multicastsockets.add(new multicastConnectInfo(ms, minfo.getIpAddress(), minfo.getPort()));
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
            return true;
        }
        else System.out.println(loginResult.getCode());
        return false;
    }

    public boolean logout(String command, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = ((String) ois.readObject()).trim();
        if(result.equalsIgnoreCase("OK")) {
            System.out.println("OK");
            userName = null;
            return true;
        }
        else System.out.println(result);
        return false;
    }

    public void listusers() {
        System.out.println("Printing the list of users and their status:");
        for (NicknameStatusPair user : systemUsers)
            System.out.println("- User: " + user.getNickname() + " status: " + user.getStatus());
    }

    public void listonlineusers() {
        System.out.println("Printing the list of online users:");
        for (NicknameStatusPair user : systemUsers)
            if(user.getStatus().equalsIgnoreCase("online"))
                System.out.println("- User: " + user.getNickname());
    }

    public void listprojects(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result<String>) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of user's project:");
        for (String user : result.getList())
            System.out.println("- User: " + user);
    }

    public void createProject(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = ((String) ois.readObject()).trim();
        System.out.println(result);
    }

    public void addMember(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = ((String) ois.readObject()).trim();
        System.out.println(result);
    }

    public void showMembers(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result<String>) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of users in the project:");
        for (String user : result.getList())
            System.out.println("- User: " + user);
    }

    public void showCards(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<NicknameStatusPair> result = (Result<NicknameStatusPair>) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of cards in the project:");
        for (NicknameStatusPair card : result.getList())
            System.out.println("- Card: " + card.getNickname() + " status: " + card.getStatus());
    }

    public void showCard(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result<String>) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        List<String> info = result.getList();
        System.out.println("Printing the info of the card:");
        System.out.println("-CardName:" + info.get(0));
        System.out.println("-CardDescription:" + info.get(1));
        System.out.println("-CurrentList:" + info.get(2));
    }

    public void addCard(String command, String description, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String[] splittedCommand = command.split(" ");
        if(splittedCommand.length<3){
            System.out.println("ProjectName or CardName are empty");
            return;
        }
        else if(splittedCommand.length>3){
            System.out.println("Too much arguments");
            return;
        }
        command = command.trim();
        command = command + " " + description;
        command = command.trim();
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = (String) ois.readObject();
        //Printing
        System.out.println(result);

    }

    public void moveCard(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = (String) ois.readObject();
        //Printing
        System.out.println(result);

    }

    public void getCardHistory(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result<String>) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }

        Iterator<String> iterator = result.getList().iterator();
        System.out.println("Printing the history of the card");
        while (iterator.hasNext()) {
            String history = iterator.next();
            //Do stuff
            if (!iterator.hasNext())
                System.out.print(history);
            else
                System.out.print(history + " -> ");

        }
    }

    public void readChat(String command, byte[] buffer, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        DatagramPacket dp;
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        chatINFO result = (chatINFO) ois.readObject(); //Gets the multicast address and port of the project
        if(result.getCode().equalsIgnoreCase("ok"))
        {
            System.out.println(" ----------- Reading chat ------------- ");
            for(multicastConnectInfo ms : Multicastsockets) { //Reads the messages on the right MulticastSocket
                if (ms.getIpAddress().equalsIgnoreCase(result.getIpAddress())) {
                    while(true){
                        dp = new DatagramPacket(buffer, buffer.length);
                        try{
                            ms.getSocket().receive(dp);
                            String s = new String(dp.getData(), 0, dp.getLength());
                            System.out.println(s);
                        }catch(SocketTimeoutException e) {
                            System.out.println(" ----- No more msg, Finished ------- ");
                            break;
                        }
                    }

                }
            }
        }
        else System.out.println(result.getCode());
    }

    public void sendChatmsg(String command, Scanner sc, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String message;
        System.out.println("Insert msg to send: ");
        message = sc.nextLine();
        message = userName + ": " + message;
        byte[] buffer = message.getBytes();
        DatagramPacket datagram;
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        chatINFO result = (chatINFO) ois.readObject(); //Gets the multicast address and port of the project
        if(result.getCode().equalsIgnoreCase("ok"))
        {
            System.out.println("ok: sending message -> " + message);
            for(multicastConnectInfo ms : Multicastsockets) { //Sends the message to the right MulticastSocket
                if (ms.getIpAddress().equalsIgnoreCase(result.getIpAddress())) {
                    datagram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ms.getIpAddress()), ms.getPort());
                    ms.getSocket().send(datagram);
                }
            }
        }
        else{
            System.out.println("Couldn't send the msg");
            System.out.println("ERROR: " + result.getCode());
        }
    }

    public void cancelProject(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = (String) ois.readObject();
        //Printing
        System.out.println(result);
    }

    public void help(){
        System.out.println("----------------- Commands syntax -----------------");
        System.out.println("ATTENTION: ALL THE COMMANDS ARE NOT CASE SENSITIVE!");
        System.out.println("register 'nickUtente' 'password' -> Command used to register a new user to the system");
        System.out.println("login 'nickUtente' 'password' -> Command used to login in the system with a specific user");
        System.out.println("logout 'nickUtente' -> Command used to logout from a specific user");
        System.out.println("listUsers -> Command used to list the users registered in the system");
        System.out.println("listOnlineUsers -> Command used to list the users that are online in the system");
        System.out.println("listProjects -> Command used to list the projects that the user is member");
        System.out.println("createProject 'projectName' -> Command used to create a new project");
        System.out.println("addMember 'projectName' 'nickUtente' -> Command used to add a user as a member in the project");
        System.out.println("showMembers 'projectName' -> Command used to show the members of the project");
        System.out.println("showCards 'projectName' -> Command used to show the cards of the project");
        System.out.println("showCard 'projectName' -> Command used to show the info of a specific card in the project");
        System.out.println("addCard 'projectName' 'cardName' -> Command used to add a new card to the project");
        System.out.println("moveCard 'projectName' 'cardName' 'StartingList' 'DestinationList' -> Command used to move a card from a list to another (POSSIBLE LISTS: TODO, INPROGRESS; TOBEREVISED, DONE)");
        System.out.println("getCardHistory 'projectName' 'cardName' -> Command used to get the history of the movements of a card in the project");
        System.out.println("readChat 'projectName' -> Command used to read the messages sent in the project's chat");
        System.out.println("sendchatmsg 'projectName' -> Command used to send a message in the project's chat");
        System.out.println("cancelProject 'projectName' -> Command used to cancel a project in the system");
        System.out.println("----------------- Commands syntax -----------------");
    }


    public void notifyEvent(String nickName, String status) { //Method used to update registered user list and their status
        boolean found = false;
        String returnMessage = "Update event received: " + nickName + " " + status;
        System.out.println(returnMessage);
        for(NicknameStatusPair ns: systemUsers)
            if(ns.getNickname().equalsIgnoreCase(nickName)) {
                found = true;
                if(!ns.getStatus().equalsIgnoreCase(status)) ns.setStatus(status);
            }
        if(!found) systemUsers.add(new NicknameStatusPair(nickName,status));
    }

    public void notifyEventChat(String address, int port){ //Method used to update the multicast projects info
        String returnMessage = "Update event received: " + address + " " + port;
        System.out.println(returnMessage);
        MulticastSocket ms;
        try {
            ms = new MulticastSocket(port);
            ms.joinGroup(InetAddress.getByName(address));
            ms.setSoTimeout(2000);
            Multicastsockets.add(new multicastConnectInfo(ms, address, port));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void notifyEventProjectCancel(String address, int port){ //Method used to update the multicast projects info when a project get canceled
        multicastConnectInfo cancelMS = null;
        String returnMessage = "Cancel project update event received: " + address + " " + port;
        System.out.println(returnMessage);
        for(multicastConnectInfo ms : Multicastsockets)
            if(ms.getIpAddress().equalsIgnoreCase(address)) {
                try {
                    ms.getSocket().leaveGroup(InetAddress.getByName(address));
                    cancelMS = ms;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        if(cancelMS!=null) Multicastsockets.remove(cancelMS);
    }


    public static void main(String[] args){
        ClientMain client = new ClientMain();
        client.start();

    }
}
