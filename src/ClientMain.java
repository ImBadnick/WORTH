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
import java.util.Scanner;

public class ClientMain extends RemoteObject implements NotifyEventInterface{
    private ArrayList<NicknameStatusPair> systemUsers; //Listing users in the system
    private static final int RMIport = 16617; //RMI port
    private static final int TCPport = 20700; //TCP port for connection
    private static final String ServerAddress = "127.0.0.1";
    private String userName; //Saving the logged in userName
    private ArrayList<multicastClientInfo> Multicastsockets; //List of MulticastSocket Info

    public class multicastClientInfo{ //Class used to save the multicast information retrieved from the server.
        private MulticastSocket socket; //Multicast Socket
        private String address; //Multicast Address
        private int port; //Multicast Port

        public multicastClientInfo(MulticastSocket socket, String address, int port){
            this.socket = socket;
            this.address = address;
            this.port = port;
        }

        public void setAddress(String address) { this.address = address; }
        public void setSocket(MulticastSocket socket) { this.socket = socket; }
        public void setPort(int port) { this.port = port; }

        public MulticastSocket getSocket() { return socket; }
        public String getAddress() { return address; }
        public int getPort() { return port; }
    }

    public ClientMain(){
        super();
        this.userName = null;
        this.Multicastsockets = new ArrayList<>();
    }

    public void start(){
        boolean result;
        boolean loggedIn = false;
        boolean cycle = true;
        SocketChannel socketChannel;
        try {
            //Setup RMI
            Registry registry = LocateRegistry.getRegistry(RMIport);
            ServerMainInterfaceRMI stub = (ServerMainInterfaceRMI) registry.lookup("ServerRMI");
            Scanner in = new Scanner(System.in);
            socketChannel = SocketChannel.open(); //Apertura socket
            socketChannel.connect(new InetSocketAddress(ServerAddress, TCPport)); //Connessione al server
            NotifyEventInterface callbackObj = this;
            NotifyEventInterface stubCallBack = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
            byte[] buffer = new byte[8192];
            while(cycle){
                String command = in.nextLine();
                String[] splittedCommand = command.split(" ");

                switch(splittedCommand[0].toLowerCase()) {
                    case "register":
                        register(splittedCommand,stub);
                        break;
                    case "login":
                        if(loggedIn){
                            System.out.println("You are already logged in! Logout before please.");
                            break;
                        }
                        result = login(command,socketChannel);
                        if (result){
                            loggedIn = true;
                            System.out.println("Registering for callback");
                            stub.registerForCallback(stubCallBack,splittedCommand[1]);
                        }
                        break;

                    case "logout":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        result = logout(command,socketChannel);
                        if (result){
                            loggedIn = false;
                            System.out.println("Unregistering for callback");
                            stub.unregisterForCallback(stubCallBack);
                        }
                        break;

                    case "listusers":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listusers(command,socketChannel);
                        break;

                    case "listonlineusers":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listonlineusers(command,socketChannel);
                        break;

                    case "listprojects":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        listprojects(command,socketChannel);
                        break;

                    case "createproject":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        createProject(command,socketChannel);
                        break;

                    case "addmember":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        addMember(command,socketChannel);
                        break;

                    case "showmembers":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showMembers(command,socketChannel);
                        break;

                    case "showcards":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showCards(command,socketChannel);
                        break;

                    case "showcard":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        showCard(command,socketChannel);
                        break;

                    case "addcard":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        System.out.print("Insert a description of the card: ");
                        String description = in.nextLine();
                        addCard(command,description,socketChannel);
                        break;

                    case "movecard":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        moveCard(command,socketChannel);
                        break;

                    case "getcardhistory":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        getCardHistory(command,socketChannel);
                        break;

                    case "cancelproject":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        cancelProject(command,socketChannel);
                        break;

                    case "readchat":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        readChat(command,buffer, socketChannel);
                        break;

                    case "sendchatmsg":
                        if(!loggedIn) {
                            System.out.println("You need to be logged in the system before!");
                            break;
                        }
                        sendChatmsg(command,in, socketChannel);
                        break;

                    case "exit":
                        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
                        System.out.println("Doing the exit!");
                        cycle = false;
                        break;

                    default:
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
        if(splittedCommand.length!=3) result=stub.register("","");
        else result = stub.register(splittedCommand[1],splittedCommand[2]);
        System.out.println(result);
    }

    public boolean login(String command, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String[] splitcommand = command.split(" ");
        LoginResult<NicknameStatusPair> loginResult;
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        loginResult = (LoginResult) ois.readObject();
        if(loginResult.getCode().equalsIgnoreCase("OK")) {
            System.out.println("OK");
            systemUsers = loginResult.getList();
            userName = splitcommand[1];
            if(loginResult.getMulticastinfo()!=null){
                for(multicastINFO minfo : loginResult.getMulticastinfo())
                {
                    MulticastSocket ms;
                    try {
                        ms = new MulticastSocket(minfo.getPort());
                        ms.joinGroup(InetAddress.getByName(minfo.getIpAddress()));
                        ms.setSoTimeout(2000);
                        Multicastsockets.add(new multicastClientInfo(ms, minfo.getIpAddress(), minfo.getPort()));
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
            return true;
        }
        else System.out.println(loginResult.getCode());
        return false;
    }

    public boolean logout(String command, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String[] splitcommand = command.split(" ");
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

    public void listusers(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<NicknameStatusPair> result = (Result) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of users and their status:");
        for (NicknameStatusPair user : result.getList())
            System.out.println("- User: " + user.getNickname() + " status: " + user.getStatus());
    }

    public void listonlineusers(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of online users:");
        for (String user : result.getList())
            System.out.println("- User: " + user);
    }

    public void listprojects(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result) ois.readObject();
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
        if(result.equalsIgnoreCase("OK")) System.out.println("OK");
        else System.out.println(result);
    }

    public void addMember(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        String result = ((String) ois.readObject()).trim();
        if(result.equalsIgnoreCase("OK")) System.out.println("OK");
        else System.out.println(result);
    }

    public void showMembers(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result) ois.readObject();
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
        Result<String> result = (Result) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        System.out.println("Printing the list of cards in the project:");
        for (String card : result.getList())
            System.out.println("- Card: " + card);
    }

    public void showCard(String command,SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        //Send
        socketChannel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));
        //Receive
        ObjectInputStream ois = new ObjectInputStream(socketChannel.socket().getInputStream());
        Result<String> result = (Result) ois.readObject();
        //Printing
        if(result.getList()==null){
            System.out.println(result.getCode());
            return;
        }
        ArrayList<String> info = result.getList();
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
        command = new StringBuilder().append(command).append(" ").append(description).toString();
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
        Result<String> result = (Result) ois.readObject();
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
        chatINFO result = (chatINFO) ois.readObject();
        if(result.getCode().equalsIgnoreCase("ok"))
        {
            System.out.println(" ----------- Reading chat ------------- ");
            for(multicastClientInfo ms : Multicastsockets) {
                if (ms.getAddress().equalsIgnoreCase(result.getIpAddress())) {
                    while(true){
                        dp = new DatagramPacket(buffer, buffer.length);
                        try{
                            ms.getSocket().receive(dp);
                            String s = new String(dp.getData());
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
        chatINFO result = (chatINFO) ois.readObject();
        if(result.getCode().equalsIgnoreCase("ok"))
        {
            System.out.println("ok: sending message -> " + message);
            for(multicastClientInfo ms : Multicastsockets) {
                if (ms.getAddress().equalsIgnoreCase(result.getIpAddress())) {
                    datagram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ms.getAddress()), ms.getPort());
                    ms.getSocket().send(datagram);
                }
            }
        }
        else{
            System.out.println("Couldnt send the msg");
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


    public void notifyEvent(String nickName, String status) throws RemoteException {
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

    public void notifyEventChat(String address, int port){
        String returnMessage = "Update event received: " + address + " " + port;
        System.out.println(returnMessage);
        MulticastSocket ms;
        try {
            ms = new MulticastSocket(port);
            ms.joinGroup(InetAddress.getByName(address));
            ms.setSoTimeout(2000);
            Multicastsockets.add(new multicastClientInfo(ms,address,port));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void notifyEventProjectCancel(String address, int port){
        String returnMessage = "Cancel project update event received: " + address + " " + port;
        System.out.println(returnMessage);
        for(multicastClientInfo ms : Multicastsockets)
            if(ms.getAddress().equalsIgnoreCase(address)) {
                try {
                    ms.getSocket().leaveGroup(InetAddress.getByName(address));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }


    public void printUserList(){
        Iterator i = systemUsers.iterator( );
        while (i.hasNext()) {
            NicknameStatusPair user = (NicknameStatusPair) i.next();
            System.out.println("User: " + user.getNickname() + " status: " + user.getStatus());
        }
    }

    public static void main(String[] args){
        ClientMain client = new ClientMain();
        client.start();

    }


}
