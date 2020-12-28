import ProjectUtils.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerMain extends RemoteObject implements ServerMainInterface,ServerMainInterfaceRMI{
    private static final long serialVersionUID = -932570530032326976L;
    private final List <CallBackInfo> clients; //Clients callback info
    private List<User> Users; //Users registered
    private List<Project> Projects; //Project list
    private final Map<SocketChannel,List<byte[]>> dataMap; //Used to save and send response to the client
    private static final int RMIport = 16617; //RMI PORT
    private static final int TCPport = 20700; //TCP PORT
    private MulticastIPGenerator MipGenerator; //MulticastIPGenerator
    private final Random random; //Random
    private final File backupDir; //Backup directory
    private final ObjectMapper mapper; //ObjectMapper for json
    private File userFile,projectFile,MipGeneratorFile; //Files for backup
    private final List<multicastConnectInfo> Multicastsockets; //List of multicast server's info

    public ServerMain(){
        super();
        backupDir = new File("./Backup");
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.Multicastsockets = new ArrayList<>();
        this.random = new Random();
        dataMap = new HashMap<>();
        clients = new ArrayList<>();
        RestoreBackup();
    }

    public void RestoreBackup(){ //Restoring backup
        this.userFile = new File(backupDir + "/Users.json");
        this.projectFile = new File(backupDir + "/Projects.json");
        this.MipGeneratorFile = new File(backupDir + "/MipGenerator.json");
        if(!backupDir.exists()) { //If backup directory doesn't exists -> create
           backupDir.mkdir();
        }
        try {
            if(!userFile.exists()){ //If users file doesn't exists
                this.Users = new ArrayList<>(); //Initialize user list
                userFile.createNewFile(); //Create file
                mapper.writeValue(userFile, Users); //Write the empty JsonARRAY to json file
            } else {this.Users = new ArrayList<>(Arrays.asList(mapper.readValue(userFile,User[].class)));} //Backup from json file

            if(!projectFile.exists()){ //If projects file doesn't exists
                this.Projects = new ArrayList<>(); //Initialize project list
                projectFile.createNewFile(); //Create file
                mapper.writeValue(projectFile, Projects); //Write the empty JsonARRAY to json file
            } else {
                this.Projects =  new ArrayList<>(Arrays.asList(mapper.readValue(projectFile,Project[].class))); //Backup from json file
                //Setup server projects multicast info
                MulticastSocket ms;
                String ip; int port;
                for(Project project : Projects)
                {
                    try {
                        ip = project.getMulticastAddress();
                        port = project.getPort();
                        ms = new MulticastSocket(port);
                        ms.joinGroup(InetAddress.getByName(ip));
                        ms.setSoTimeout(2000);
                        Multicastsockets.add(new multicastConnectInfo(ms, ip, port));
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }

            if(!MipGeneratorFile.exists()){ //If MulticastIPGenerator file doesn't exists
                this.MipGenerator = new MulticastIPGenerator(224,0,0,0); //Initialize MulticastIPGenerator
                MipGeneratorFile.createNewFile(); //Create file
                mapper.writeValue(MipGeneratorFile, MipGenerator); //Write MulticastIPGenerator proprieties to JSON file
            } else {
                if(!Projects.isEmpty()) this.MipGenerator = mapper.readValue(MipGeneratorFile,MulticastIPGenerator.class); //Backup from json file
                else this.MipGenerator = new MulticastIPGenerator(224,0,0,0); //Initialize MulticastIPGenerator
            }
        } catch (IOException e) { e.printStackTrace(); }

    }


    public void start(){
        try {
            String response;
            String command;
            String[] Splittedcommand;
            List<byte[]> Data; ByteArrayOutputStream baos; ObjectOutputStream oos; byte[] res;

            ServerSocketChannel srvSkt = ServerSocketChannel.open();  //Opening listening socket
            srvSkt.socket().bind(new InetSocketAddress(TCPport)); //Configuring socket
            srvSkt.configureBlocking(false);

            //Setup selector
            Selector selector = Selector.open();
            srvSkt.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("SYSTEM: server is online");

            while (true) {
                try {
                    selector.select(); //Selecting
                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    try {
                        if (key.isAcceptable()) { //"Catching" connection requests
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();
                            System.out.println("SYSTEM: Accepted connection from " + client);
                            client.configureBlocking(false);
                            dataMap.put(client, new ArrayList<>());
                            client.register(selector, SelectionKey.OP_READ);
                            key.attach(null);
                        }
                        else if (key.isReadable()) { //"Catching" read requests
                            SocketChannel client = (SocketChannel) key.channel();
                            //Reading command requested
                            ByteBuffer buffer = ByteBuffer.allocate(128);
                            client.read(buffer);
                            command = new String(buffer.array()).trim();
                            Splittedcommand = command.split(" ");
                            System.out.println("Command requested: " + command);
                            switch(Splittedcommand[0].toLowerCase()){
                                case "login":
                                    LoginResult<NicknameStatusPair> loginResponse;
                                    if(Splittedcommand.length<3) loginResponse = login("","");
                                    else if(Splittedcommand.length>3) loginResponse = new LoginResult<>("Too much arguments", null, null);
                                    else {
                                        loginResponse = login(Splittedcommand[1], Splittedcommand[2]);
                                        if(loginResponse.getCode().equalsIgnoreCase("ok")) {
                                            key.attach(Splittedcommand[1]);
                                        }
                                    }
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(loginResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "logout":
                                    if(Splittedcommand.length<2) response = logout("");
                                    else if(Splittedcommand.length>2) response = "Too much arguments";
                                    else {
                                        if(((String) key.attachment()).equalsIgnoreCase(Splittedcommand[1]))
                                        {
                                            response = logout(Splittedcommand[1]);
                                            if(response.equalsIgnoreCase("ok")) key.attach(null);
                                        }
                                        else response = "You are trying to logout another user!";
                                    }
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listusers":
                                    Result<NicknameStatusPair> ListUsersResponse;
                                    if(Splittedcommand.length>1) ListUsersResponse = new Result<>("Too much arguments", null);
                                    else ListUsersResponse = listUsers();
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ListUsersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listonlineusers":
                                    Result<String> ListOnlineUsersResponse;
                                    if(Splittedcommand.length>1) ListOnlineUsersResponse = new Result<>("Too much arguments", null);
                                    else ListOnlineUsersResponse = listOnlineUsers();
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ListOnlineUsersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listprojects":
                                    Result<String> ListProjects;
                                    if(Splittedcommand.length>1) ListProjects = new Result<>("Too much arguments", null);
                                    else ListProjects = listProjects((String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ListProjects);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "createproject":
                                    if(Splittedcommand.length<2) response = createProject("","");
                                    else if(Splittedcommand.length>2) response = "Too much arguments";
                                    else response = createProject(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "addmember":
                                    if(Splittedcommand.length<3) response = addMember("","","");
                                    else if(Splittedcommand.length>3) response = "Too much arguments";
                                    else response = addMember(Splittedcommand[1],Splittedcommand[2],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "showmembers":
                                    Result<String> ShowMembersResponse;
                                    if(Splittedcommand.length<2) ShowMembersResponse = showMembers("",(String) key.attachment());
                                    else if(Splittedcommand.length>2) ShowMembersResponse = new Result<>("Too much arguments", null);
                                    else ShowMembersResponse = showMembers(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ShowMembersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "showcards":
                                    Result<NicknameStatusPair> ShowCardsResponse;
                                    if(Splittedcommand.length<2) ShowCardsResponse = showCards("",(String) key.attachment());
                                    else if(Splittedcommand.length>2) ShowCardsResponse = new Result<>("Too much arguments", null);
                                    else ShowCardsResponse = showCards(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ShowCardsResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "showcard":
                                    Result<String> ShowCardResponse;
                                    if(Splittedcommand.length<3) ShowCardResponse = showCard("","", (String) key.attachment());
                                    else if(Splittedcommand.length>3) ShowCardResponse = new Result<>("Too much arguments", null);
                                    else ShowCardResponse = showCard(Splittedcommand[1],Splittedcommand[2],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ShowCardResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "addcard":
                                    StringBuilder description = new StringBuilder();
                                    for(int i=3; i<Splittedcommand.length;i++) description.append(Splittedcommand[i]).append(" ");
                                    response = addCard(Splittedcommand[1],Splittedcommand[2],description.toString(),(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;


                                case "movecard":
                                    if(Splittedcommand.length<5) response = "ProjectName,CardName,InitialList or FinalList are empty";
                                    else if(Splittedcommand.length>5) response = "Too much arguments";
                                    else response = moveCard(Splittedcommand[1],Splittedcommand[2],Splittedcommand[3],Splittedcommand[4],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "getcardhistory":
                                    Result<String> getCardHistoryResponse;
                                    if(Splittedcommand.length<3) getCardHistoryResponse = getCardHistory("","", (String) key.attachment());
                                    else if(Splittedcommand.length>3) getCardHistoryResponse = new Result<>("Too much arguments", null);
                                    else getCardHistoryResponse = getCardHistory(Splittedcommand[1],Splittedcommand[2],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(getCardHistoryResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "cancelproject":
                                    if(Splittedcommand.length<2) response = "ProjectName is empty";
                                    else if(Splittedcommand.length>2) response = "Too much arguments";
                                    else response = cancelProject(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "readchat":
                                    chatINFO readChatResponse;
                                    if(Splittedcommand.length<2) readChatResponse = new chatINFO("ProjectName is empty",null);
                                    else if(Splittedcommand.length>2) readChatResponse = new chatINFO("Too much arguments",null);
                                    else readChatResponse = readChat(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(readChatResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "sendchatmsg":
                                    chatINFO sendChatResponse;
                                    if(Splittedcommand.length<2) sendChatResponse = new chatINFO("ProjectName is empty",null);
                                    else if(Splittedcommand.length>2) sendChatResponse = new chatINFO("Too much arguments",null);
                                    else sendChatResponse = sendChatMsg(Splittedcommand[1],(String) key.attachment());
                                    //PREPARING RESPONSE TO SEND
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(sendChatResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "":
                                case "exit":
                                    this.dataMap.remove(client);
                                    if(key.attachment()!=null) logout((String) key.attachment());
                                    client.close();
                                    key.cancel();
                                    System.out.println("SYSTEM: Closing connection: " + client.socket().getRemoteSocketAddress());
                                    break;

                                default:
                                    response = "Error: Command not found!";
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                            }
                            key.interestOps(SelectionKey.OP_WRITE); //Listening only write operation
                        }
                        else if (key.isWritable()) { //Catching write requests
                            //SENDING THE RESPONSE
                            SocketChannel client = (SocketChannel) key.channel();
                            Data = this.dataMap.get(client);
                            Iterator<byte[]> items = Data.iterator();
                            while (items.hasNext()) {
                                byte[] item = items.next();
                                items.remove();
                                client.write(ByteBuffer.wrap(item));
                            }
                            key.interestOps(SelectionKey.OP_READ); //Listening only reading operation
                        }
                    } catch (IOException | CancelledKeyException e) {
                        key.cancel();
                        try { key.channel().close(); }
                        catch (IOException ignored) {}
                    }
                }

            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public String register(String nickUtente, String password) throws RemoteException {
        System.out.println("Command requested: register " + nickUtente + " " + password);
        if(nickUtente.isEmpty() || password.isEmpty())
            return "Error: Nickname or Password are empty - user not registered";
        for(User user : Users) //Checking if the user is already registered
            if(user.getNickname().equalsIgnoreCase(nickUtente)) return "Error: " + nickUtente + " already exists";

        //Creating the new User and adding to the User list
        User u = new User(nickUtente,password);
        update(nickUtente, "offline");
        Users.add(u);
        try { //Saving for backup
            mapper.writeValue(userFile, Users);
        } catch (IOException e) { e.printStackTrace(); }

        return "User added with success";
    }


    public LoginResult<NicknameStatusPair> login(String nickUtente, String password) throws IOException {
        String code = null; //Status code of the login
        LoginResult<NicknameStatusPair> lr; //Result of login method
        List<multicastINFO> multicastList = new ArrayList<>(); //Multicast info of projects that nickUtente is member
        boolean tmp = false; //True = logged in with success
        List<NicknameStatusPair> list = new ArrayList<>(); //List of users in the system (Username - status)

        if(nickUtente.isEmpty() || password.isEmpty()) code = "Nickname or Password are empty!"; //Checking if the nickUtente or Password are empty
        else{
            if (Users.isEmpty()) code = "Users list is empty!. You need to be registered first."; //Checking if the users list is empty
            else{
                for(User user : Users) //Searching the nickUtente in users registered in the system
                {
                    if(user.getNickname().equalsIgnoreCase(nickUtente)) //Checking if the nickUtente equals the registered user
                        if(user.getPassword().equals(password)){ //Checking if the password is correct
                            if(user.getStatus().equals("offline")) { //Checking if the user is offline
                                tmp = true; //True = logged in with success
                                update(nickUtente, "online");  //CALLBACKS!
                                user.setStatus("online"); //Changes the status of user to "ONLINE"
                                for(Project project : Projects) //Multicast info of projects that user is member
                                    if(project.isMember(nickUtente))
                                        multicastList.add(new multicastINFO(project.getMulticastAddress(),project.getPort()));
                            }
                            else code = "User is already online, please logout first!";
                        } else code = "Wrong user password";
                    list.add(new NicknameStatusPair(user.getNickname(),user.getStatus())); //Creating the list of the system's users
                }
                if (!tmp && code == null) code = "User not found in the system, register it first.";
            }
        }

        if (tmp) lr = new LoginResult<>("OK",list,multicastList); //If the login went fine -> ok -> Return "ok", list of users, list of all project's multicastInfo
        else lr = new LoginResult<>(code,null,null);

        return lr;
    }


    public String logout(String nickUtente) {
        if(nickUtente.isEmpty()) return "Nickname is empty";
        if(Users.isEmpty()) return "Users list is empty!. You need to be logged in first.";
        for(User user : Users) { //Searching the nickUtente in users registered in the system
            if(user.getNickname().equalsIgnoreCase(nickUtente)) //Checking if the nickUtente equals the registered user
                if(user.getStatus().equalsIgnoreCase("online")){ //Checking if the user is online
                    try {
                        update(nickUtente,"offline"); //CALLBACKS!
                    }catch(RemoteException e) { e.printStackTrace(); }
                    user.setStatus("offline"); //Changes the status of user to "OFFLINE"
                    return "ok";
                } else return "User is already offline -> error";
        }
        return "User doesn't exists";
    }

    public Result<NicknameStatusPair> listUsers() { //Lists all the users registered in the system
        if(Users.isEmpty()) return new Result<>("User list is empty!",null);
        List<NicknameStatusPair> listUsers = new ArrayList<>();
        for(User user : Users)
            listUsers.add(new NicknameStatusPair(user.getNickname(),user.getStatus()));
        return new Result<>("ok", listUsers);
    }

    public Result<String> listOnlineUsers() { //Lists all the users registered online in the system
        if(Users.isEmpty()) return new Result<>("User list is empty!",null);
        List<String> listOnlineUsers = new ArrayList<>();
        for(User user : Users)
            if(user.getStatus().equalsIgnoreCase("online"))
                listOnlineUsers.add(user.getNickname());
        if(listOnlineUsers.isEmpty()) return new Result<>("Online users list is empty!",null);

        return new Result<>("ok", listOnlineUsers);
    }

    public Result<String> listProjects(String nickUtente) {
        if(Projects.isEmpty()) return new Result<>("Project list is empty!",null);
        List<String> listUserProjects = new ArrayList<>();
        for(Project project: Projects) //Searching the project
            if(project.isMember(nickUtente)) //Checks if the nickUtente is a project's member
                listUserProjects.add(project.getID());  //Creating the list of the projects

        if(listUserProjects.isEmpty()) return new Result<>("User doesn't participate to any project!",null);

        return new Result<>("ok", listUserProjects);
    }

    public String createProject(String projectName, String nickUtente) {
        if(projectName.isEmpty()) return "ProjectName is empty";
        for(Project project : Projects) //Checks if the project already exists
            if(project.getID().equalsIgnoreCase(projectName))
                return "The project exists already!";
        String ip = this.MipGenerator.generateIP(); //Generates a random multicast IP
        if(ip.equalsIgnoreCase("error")) return "IP GENERATOR ERROR!";
        int port = random.nextInt((65535-1025+1))+1025; //Generates a random port [1025-65535]
        Project project = new Project(projectName,nickUtente,ip,port); //Creates a new project
        System.out.println(ip + " " + project.getPort());
        Projects.add(project); //Adds the project to the projects list
        try { //Updates the backup
            mapper.writeValue(projectFile, Projects);
            mapper.writeValue(MipGeneratorFile,MipGenerator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try { //Callback to the user to update the multicast info
            updateMulticast(project,nickUtente);
        } catch (RemoteException e) { e.printStackTrace(); }

        //Add the server to the multicast group to write cards moves
        MulticastSocket ms;
        try {
            ms = new MulticastSocket(port);
            ms.joinGroup(InetAddress.getByName(ip));
            ms.setSoTimeout(2000);
            Multicastsockets.add(new multicastConnectInfo(ms, ip, port));
        } catch (IOException e) { e.printStackTrace(); }

        return "ok";
    }

    public String addMember(String projectName, String nickUtente, String addingUser) { //nickUtente = user to add ---- addingUser = user that adds
        boolean Projectfound = false, userFound = false;
        String code = "ERROR";
        if(projectName.isEmpty() || nickUtente.isEmpty()) return "ProjectName or NickUtente is empty";
        if(Projects.isEmpty()) return "Project list is empty!";

        for(User user : Users) //Checking if the member is registered in the system
            if (user.getNickname().equalsIgnoreCase(nickUtente)) {
                userFound = true;
                break;
            }
        if(!userFound) return "The user is not registered in the system!";

        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) { //Checks if is the project
                if(!project.isMember(addingUser)) return "AddingUser isn't member of the project!"; //Checks if the addingUser is member of the project
                code = project.addMember(nickUtente); //Adds the nickUtente to the project
                Projectfound = true;
                if(code.equalsIgnoreCase("ok")){ //If add went ok, else -> User is already a member of the project
                    System.out.println("ciao");
                    try {
                        mapper.writeValue(projectFile, Projects); //Updating backup info
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        updateMulticast(project,nickUtente); //CALLBACK FOR MULTICASTING INFO TO THE USER ADDED TO THE PROJECT
                    } catch (RemoteException e) { e.printStackTrace(); }
                }
            }
        if(!Projectfound) return "The project doesn't exist";

        return code;
    }

    public Result<String> showMembers(String projectName, String nickUtente) {
        List<String> list;
        if(Projects.isEmpty()) return new Result<>("Project list is empty!",null);
        if(projectName.isEmpty()) return new Result<>("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return new Result<>("User doesn't participate to any project!",null); //checking if the user is member of the project
                list = project.getMembers(); //List of user that are members of the project
                if(list == null) return new Result<>("Member list is empty",null); //IN CASE OF NULL LIST
                else return new Result<>("ok",list);
            }
        return new Result<>("The project doesn't exist",null);
    }

    public Result<NicknameStatusPair> showCards(String projectName, String nickUtente) {
        List<NicknameStatusPair> list;
        if(Projects.isEmpty()) return new Result<>("Project list is empty!",null);
        if(projectName.isEmpty()) return new Result<>("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return new Result<>("User doesn't participate to any project!",null); //checking if the user is member of the project
                list = project.getCardsName(); //List of cards of the project
                if(list == null) return new Result<>("Cards list is empty",null); //IN CASE OF NULL LIST
                else return new Result<>("ok",list);
            }
        return new Result<>("The project doesn't exist",null);
    }

    public Result<String> showCard(String projectName, String cardName, String nickUtente){
        List<String> info;
        if(Projects.isEmpty()) return new Result<>("Project list is empty!",null);
        if(projectName.isEmpty() || cardName.isEmpty()) return new Result<>("ProjectName or cardName are empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return new Result<>("User doesn't participate to any project!",null); //checking if the user is member of the project
                info = project.getCardInfo(cardName); //Card info
                if(info == null) return new Result<>("Card not found",null); //If the card is not in the project -> null info
                else return new Result<>("ok",info);
            }
        return new Result<>("The project doesn't exist",null);
    }

    public String addCard(String projectName, String cardName, String description, String nickUtente){
        if(Projects.isEmpty()) return "Project list is empty";
        if(projectName.isEmpty() || cardName.isEmpty()) return "ProjectName or cardName are empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!"; //checking if the user is member of the project
                String code = project.addCard(cardName,description);
                if(code.equalsIgnoreCase("ok")){ //if card added with success
                    try {
                        mapper.writeValue(projectFile, Projects); //Updating backup info
                    } catch (IOException e) { e.printStackTrace(); }
                }
                return code;
            }
        return "The project doesn't exist";

    }

    public String moveCard(String projectName, String cardName, String fromList, String movetoList, String nickUtente) {
        if(Projects.isEmpty()) return "Project list is empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))  //Searching project
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!"; //checking if the user is member of the project
                String code = project.moveCard(cardName,fromList,movetoList);
                if(code.equalsIgnoreCase("ok")){ //If the card moved with success
                    try {
                        mapper.writeValue(projectFile, Projects); //Updating backup info
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Server sends in the chat the update
                    String message = "Card: " + cardName +" moved from list " + fromList + " to list " + movetoList;
                    byte[] buffer = message.getBytes();
                    DatagramPacket datagram;
                    System.out.println("ok: sending update card move msg -> " + message);
                    for(multicastConnectInfo ms : Multicastsockets) {
                        if (ms.getIpAddress().equalsIgnoreCase(project.getMulticastAddress())) {
                            try {
                                datagram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ms.getIpAddress()), ms.getPort());
                                ms.getSocket().send(datagram);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return code;
            }
        return "The project doesn't exist";
    }

    public Result<String> getCardHistory(String projectName, String cardName, String nickUtente) {
        List<String> history;
        if(Projects.isEmpty()) return new Result<>("Project list is empty!",null);
        if(projectName.isEmpty() || cardName.isEmpty()) return new Result<>("ProjectName or cardName are empty",null);
        for(Project project : Projects) //Searching project
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return new Result<>("User doesn't participate to any project!",null); //checking if the user is member of the project
                history = project.getCardHistory(cardName); //Getting card history
                if(history == null) return new Result<>("Card not found",null); //In case card is not found
                else return new Result<>("ok",history);
            }
        return new Result<>("The project doesn't exist",null);
    }

    public chatINFO readChat(String ProjectName, String nickUtente) {
        if(Projects.isEmpty()) return new chatINFO("Project list is empty!",null);
        if(ProjectName.isEmpty()) return new chatINFO("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(ProjectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return new chatINFO("User doesn't participate to any project!",null); //checking if the user is member of the project
                return new chatINFO("ok",project.getMulticastAddress()); //Getting chat info
            }
        return new chatINFO("The project doesn't exists", null);
    }

    public chatINFO sendChatMsg(String ProjectName, String nickUtente) {
        if(Projects.isEmpty()) return new chatINFO("Project list is empty!",null);
        if(ProjectName.isEmpty()) return new chatINFO("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(ProjectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return new chatINFO("User doesn't participate to any project!",null); //checking if the user is member of the project
                return new chatINFO("ok",project.getMulticastAddress());  //Getting chat info
            }
        return new chatINFO("The project doesn't exists", null);
    }

    public String cancelProject(String projectName, String nickUtente) {
        if(Projects.isEmpty()) return "Project list is empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) //Searching project
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!"; //checking if the user is member of the project
                if(!project.isDone()) return "All cards in the project are not in status: DONE"; //Checking if all card are in status "DONE"
                Projects.remove(project); //Removing project
                try {
                    if(Projects.isEmpty()){ //Reset multicast IP generator if project list is empty
                        MipGenerator.reset();
                        mapper.writeValue(MipGeneratorFile, MipGenerator);
                    }
                    mapper.writeValue(projectFile, Projects); //Updating backup
                } catch (IOException e) {
                    e.printStackTrace();
                }
                project.cancelProjectDir();
                try {
                    updateMulticastALL(project); //CALLBACK MULTICAST INFO
                } catch (RemoteException e) { e.printStackTrace(); }
                //SERVER LEAVES GROUP
                multicastConnectInfo cancelMS = null;
                for(multicastConnectInfo ms : Multicastsockets)
                    if(ms.getIpAddress().equalsIgnoreCase(project.getMulticastAddress())) {
                        try {
                            ms.getSocket().leaveGroup(InetAddress.getByName(project.getMulticastAddress()));
                            cancelMS = ms;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                if(cancelMS!=null) Multicastsockets.remove(cancelMS);

                return "ok";
            }
        return "The project doesn't exist";
    }


    //RMI CALLBACKS METHODS

    public synchronized void registerForCallback (NotifyEventInterface ClientInterface, String nickUtente) throws RemoteException { //CALLBACK REGISTER
        boolean contains = clients.stream()
                .anyMatch(client -> ClientInterface.equals(client.getClient()));
        if (!contains){
            clients.add(new CallBackInfo(ClientInterface,nickUtente));
            System.out.println("CALLBACK SYSTEM: New client registered." );
        }
    }

    public synchronized void unregisterForCallback(NotifyEventInterface Client) throws RemoteException { //CALLBACK UNREGISTER
        CallBackInfo user = clients.stream()
                .filter(client -> Client.equals(client.getClient()))
                .findAny()
                .orElse(null);
        if (user!=null) {
            clients.remove(user);
            System.out.println("CALLBACK SYSTEM: Client unregistered");
        }
        else System.out.println("CALLBACK SYSTEM: Unable to unregister client.");
    }

    public void update(String nickName, String status) throws RemoteException { //METHOD USED TO UPDATE CLIENTS USERS LIST
        doCallbacks(nickName,status);
    }
    private synchronized void doCallbacks(String nickName, String status) throws RemoteException { //METHOD USED TO UPDATE CLIENTS USERS LIST
        LinkedList<NotifyEventInterface> errors = new LinkedList<>();
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        for (CallBackInfo callbackinfoUser : clients) {
            NotifyEventInterface client = callbackinfoUser.getClient();
            try {
                client.notifyEvent(nickName, status);
            } catch (RemoteException e) {
                errors.add(client);
            }
        }
        if(!errors.isEmpty()) {
            System.out.println("CALLBACK SYSTEM: Unregistering clients that caused an error!");
            for(NotifyEventInterface Nei : errors) unregisterForCallback(Nei);
        }
        System.out.println("CALLBACK SYSTEM: Callbacks complete.");
    }


    public void updateMulticast(Project project,String nickName) throws RemoteException { //METHOD USED TO UPDATE CLIENT'S MULTICAST INFO LIST
        doChatCallBacks(project,nickName);
    }
    private synchronized void doChatCallBacks(Project project,String nickName) throws RemoteException { //METHOD USED TO UPDATE CLIENT'S MULTICAST INFO LIST
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        for (CallBackInfo callbackinfoUser : clients) {
            NotifyEventInterface client = callbackinfoUser.getClient();
            if (callbackinfoUser.getuserNickname().equalsIgnoreCase(nickName))
                client.notifyEventChat(project.getMulticastAddress(), project.getPort());
        }
        System.out.println("CALLBACK SYSTEM: Callbacks complete.");
    }


    public void updateMulticastALL(Project project) throws RemoteException { //METHOD USED TO UPDATE ALL CLIENT'S MULTICAST INFO LIST IN CASE OF PROJECT CANCEL
        doChatCallBacksALL(project);
    }
    private synchronized void doChatCallBacksALL(Project project) throws RemoteException { //METHOD USED TO UPDATE ALL CLIENT'S MULTICAST INFO LIST IN CASE OF PROJECT CANCEL
        LinkedList<NotifyEventInterface> errors = new LinkedList<>();
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        for (CallBackInfo callbackinfoUser : clients) {
            NotifyEventInterface client = callbackinfoUser.getClient();
            try {
                if (project.isMember(callbackinfoUser.getuserNickname()))
                    client.notifyEventProjectCancel(project.getMulticastAddress(), project.getPort());
            } catch (RemoteException e) {
                errors.add(client);
            }
        }
        if(!errors.isEmpty()) {
            System.out.println("CALLBACK SYSTEM: Unregistering clients that caused an error!");
            for(NotifyEventInterface Nei : errors) unregisterForCallback(Nei);
        }
        System.out.println("CALLBACK SYSTEM: Callbacks complete.");
    }


    public static void main(String[] args){
        ServerMain server = new ServerMain();
        try{
            //RMI SETUP
            ServerMainInterfaceRMI stub = (ServerMainInterfaceRMI) UnicastRemoteObject.exportObject(server, 0);
            LocateRegistry.createRegistry(RMIport);
            Registry registry = LocateRegistry.getRegistry(RMIport);
            registry.rebind("ServerRMI",stub);
        } catch (RemoteException e) {
            System.out.println("RMI SYSTEM: Communication error " + e.toString());
        }


        server.start();

    }


}
