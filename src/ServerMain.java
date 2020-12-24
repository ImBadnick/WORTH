import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.sun.org.apache.xpath.internal.operations.Mult;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

class CallBackInfo{ //Info for callbacks
    private NotifyEventInterface client;
    private String userNickname;
    public CallBackInfo(NotifyEventInterface client, String userNickname){
        this.client = client;
        this.userNickname = userNickname;
    }

    public NotifyEventInterface getClient(){ return this.client; }
    public String getuserNickname(){ return this.userNickname; }

    public void setClient(NotifyEventInterface client){ this.client = client; }
    public void setUserNickname(String userNickname){ this.userNickname = userNickname; }
}


class Result<T> implements Serializable{
    private String code;
    private ArrayList<T> list;

    public Result(String code, ArrayList<T> list){
        this.list = list;
        this.code = code;
    }

    public ArrayList<T> getList(){ return this.list; }
    public String getCode() { return this.code; }

    public void setList(ArrayList<T> list){ this.list = list;}
    public void setCode(String code){ this.code = code;}
}

class multicastINFO implements Serializable{
    private String ipAddress;
    private int port;
    public multicastINFO(String ipAddress, int port){
        this.ipAddress = ipAddress;
        this.port = port;
    }
    public String getIpAddress(){ return this.ipAddress; }
    public int getPort(){ return this.port; }
}

class LoginResult<T> implements Serializable{
    private String code;
    private ArrayList<T> list;
    private ArrayList<multicastINFO> multicastinfo;

    public LoginResult(String code, ArrayList<T> list, ArrayList<multicastINFO> multicastinfo){
        this.list = list;
        this.code = code;
        this.multicastinfo = multicastinfo;
    }

    public ArrayList<T> getList(){ return this.list; }
    public String getCode() { return this.code; }
    public ArrayList<multicastINFO> getMulticastinfo(){ return this.multicastinfo;}

    public void setList(ArrayList<T> list){ this.list = list;}
    public void setCode(String code){ this.code = code;}
    public void setMulticastinfo(ArrayList<multicastINFO> multicastinfo) { this.multicastinfo = multicastinfo;}
}


class NicknameStatusPair implements Serializable {
    String nickname;
    String status;

    public NicknameStatusPair(String nickname, String status){
        this.nickname = nickname;
        this.status = status;
    }

    public String getNickname(){
        return this.nickname;
    }
    public String getStatus() { return this.status; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setStatus(String status) { this.status = status; }
}

class chatINFO implements Serializable {
    String code;
    String ipAddress;
    public chatINFO(String code, String ipAddress){
        this.code = code;
        this.ipAddress = ipAddress;
    }

    public void setCode(String code) { this.code = code; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCode() { return code; }
    public String getIpAddress() { return ipAddress; }
}

public class ServerMain extends RemoteObject implements ServerMainInterface,ServerMainInterfaceRMI{
    private final List <CallBackInfo> clients;
    private List<User> Users;
    private List<Project> Projects;
    private final Map<SocketChannel,List<byte[]>> dataMap;
    private static final int RMIport = 16617;
    private static final int TCPport = 20700;
    private MulticastIPGenerator MipGenerator;
    private Random random;
    private File backupDir;
    private ObjectMapper mapper;
    private File userFile,projectFile,MipGeneratorFile;

    public ServerMain(){
        super();
        backupDir = new File("./Backup");
        this.mapper = new ObjectMapper();
        backup();
        this.random = new Random();
        dataMap = new HashMap<>();
        clients = new ArrayList<>();
    }

    public void backup(){
        this.userFile = new File(backupDir + "/Users.json");
        this.projectFile = new File(backupDir + "/Projects.json");
        this.MipGeneratorFile = new File(backupDir + "/MipGenerator.json");
        if(!backupDir.exists()) {
            backupDir.mkdir();
        }

        try {
            if(!userFile.exists()){
                this.Users = new ArrayList<>();
                userFile.createNewFile();
                mapper.writeValue(userFile, Users);
            } else {this.Users = new ArrayList<>(Arrays.asList(mapper.readValue(userFile,User[].class)));}

            if(!projectFile.exists()){
                this.Projects = new ArrayList<>();
                projectFile.createNewFile();
                mapper.writeValue(projectFile, Projects);
            } else {this.Projects =  new ArrayList<>(Arrays.asList(mapper.readValue(projectFile,Project[].class)));
            }

            if(!MipGeneratorFile.exists()){
                this.MipGenerator = new MulticastIPGenerator(224,0,0,0);
                MipGeneratorFile.createNewFile();
                mapper.writeValue(MipGeneratorFile, MipGenerator);
            } else {this.MipGenerator = mapper.readValue(MipGeneratorFile,MulticastIPGenerator.class);}

        } catch (IOException e) { e.printStackTrace(); }

    }


    public void start(){
        try {
            String response;
            String command;
            String[] Splittedcommand;
            List<byte[]> Data; ByteArrayOutputStream baos; ObjectOutputStream oos; byte[] res;

            ServerSocketChannel srvSkt = ServerSocketChannel.open();  //Apertura del socket di ascolto
            srvSkt.socket().bind(new InetSocketAddress(TCPport)); //Configurazione del socket
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
                        if (key.isAcceptable()) { //"Catturo" le richieste di connessione
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();
                            System.out.println("SYSTEM: Accepted connection from " + client);
                            client.configureBlocking(false);
                            dataMap.put(client, new ArrayList<>());
                            SelectionKey key2 = client.register(selector, SelectionKey.OP_READ);
                            key.attach(null);
                        }
                        else if (key.isReadable()) { //"Catturo" le richieste di lettura
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(128);
                            client.read(buffer);
                            command = new String(buffer.array()).trim();
                            Splittedcommand = command.split(" ");
                            System.out.println("Command requested: " + command);
                            switch(Splittedcommand[0].toLowerCase()){
                                case "login":
                                    LoginResult<NicknameStatusPair> loginResponse;
                                    if(Splittedcommand.length<3) loginResponse = login("","");
                                    else if(Splittedcommand.length>3) loginResponse = new LoginResult("Too much arguments",null,null);
                                    else {
                                        loginResponse = login(Splittedcommand[1], Splittedcommand[2]);
                                        if(loginResponse.getCode().equalsIgnoreCase("ok")) {
                                            key.attach(Splittedcommand[1]);
                                        }
                                    }
                                    //Preparing the object to send back
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
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(response);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listusers":
                                    Result<NicknameStatusPair> ListUsersResponse;
                                    if(Splittedcommand.length>1) ListUsersResponse = new Result("Too much arguments", null);
                                    else ListUsersResponse = listUsers();
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ListUsersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listonlineusers":
                                    Result<String> ListOnlineUsersResponse;
                                    if(Splittedcommand.length>1) ListOnlineUsersResponse = new Result("Too much arguments", null);
                                    else ListOnlineUsersResponse = listOnlineUsers();
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ListOnlineUsersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "listprojects":
                                    Result<String> ListProjects;
                                    if(Splittedcommand.length>1) ListProjects = new Result("Too much arguments", null);
                                    else ListProjects = listProjects((String) key.attachment());
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
                                    else if(Splittedcommand.length>2) ShowMembersResponse = new Result("Too much arguments", null);
                                    else ShowMembersResponse = showMembers(Splittedcommand[1],(String) key.attachment());
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ShowMembersResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "showcards":
                                    Result<String> ShowCardsResponse;
                                    if(Splittedcommand.length<2) ShowCardsResponse = showCards("",(String) key.attachment());
                                    else if(Splittedcommand.length>2) ShowCardsResponse = new Result("Too much arguments", null);
                                    else ShowCardsResponse = showCards(Splittedcommand[1],(String) key.attachment());
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
                                    else if(Splittedcommand.length>3) ShowCardResponse = new Result("Too much arguments", null);
                                    else ShowCardResponse = showCard(Splittedcommand[1],Splittedcommand[2],(String) key.attachment());
                                    Data = this.dataMap.get(client);
                                    baos = new ByteArrayOutputStream( );
                                    oos = new ObjectOutputStream(baos);
                                    oos.writeObject(ShowCardResponse);
                                    res = baos.toByteArray( );
                                    Data.add(res);
                                    break;

                                case "addcard":
                                    StringBuilder description = new StringBuilder("");
                                    for(int i=3; i<Splittedcommand.length;i++) description.append(Splittedcommand[i] + " ");
                                    response = addCard(Splittedcommand[1],Splittedcommand[2],description.toString(),(String) key.attachment());
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
                                    else if(Splittedcommand.length>3) getCardHistoryResponse = new Result("Too much arguments", null);
                                    else getCardHistoryResponse = getCardHistory(Splittedcommand[1],Splittedcommand[2],(String) key.attachment());
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
                            key.interestOps(SelectionKey.OP_WRITE); //Voglio ascoltare solo le operazioni di scrittura associate a quel client
                        }
                        else if (key.isWritable()) { //"Catturo" le richieste di scrittura
                            SocketChannel client = (SocketChannel) key.channel();
                            Data = this.dataMap.get(client);
                            Iterator<byte[]> items = Data.iterator();
                            while (items.hasNext()) {
                                byte[] item = items.next();
                                items.remove();
                                client.write(ByteBuffer.wrap(item));
                            }
                            key.interestOps(SelectionKey.OP_READ);
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
        for(User user : Users)
            if(user.getNickname().equalsIgnoreCase(nickUtente)) return "Error: " + nickUtente + " already exists";
        User u = new User(nickUtente,password);
        update(nickUtente, "offline");
        Users.add(u);
        try {
            mapper.readTree(userFile);
            mapper.writeValue(userFile, Users);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "User added with success";
    }


    public LoginResult<NicknameStatusPair> login(String nickUtente, String password) throws IOException {
        String code = null;
        LoginResult<NicknameStatusPair> lr;
        ArrayList<multicastINFO> multicastList = new ArrayList<>();
        boolean tmp = false;
        ArrayList<NicknameStatusPair> list = new ArrayList<>();
        //Checking about nickname, password, empty list ecc...
        if(nickUtente.isEmpty() || password.isEmpty()) code = "Nickname or Password are empty!";
        else{
            if (Users.isEmpty()) code = "Users list is empty!. You need to be registered first.";
            else{
                for(User user : Users)
                {
                    if(user.getNickname().equalsIgnoreCase(nickUtente))
                        if(user.getPassword().equals(password)){
                            if(user.getStatus().equals("offline")) {
                                tmp = true;
                                update(nickUtente, "online");
                                user.setStatus("online");
                                for(Project project : Projects)
                                    if(project.isMember(nickUtente))
                                        multicastList.add(new multicastINFO(project.getMulticastAddress(),project.getPort()));
                            }
                            else code = "User is already online, please logout first!";
                        } else code = "Wrong user password";
                    list.add(new NicknameStatusPair(user.getNickname(),user.getStatus()));
                }
                if (!tmp && code == null) code = "User not found in the system, register it first.";
            }
        }
        //If the checking its ok -> Login , else -> Error code
        if (tmp) lr = new LoginResult("OK",list,multicastList);
        else lr = new LoginResult(code,null,null);

        return lr;
    }


    public String logout(String nickUtente) {
        if(nickUtente.isEmpty()) return "Nickname is empty";
        if(Users.isEmpty()) return "Users list is empty!. You need to be logged in first.";
        for(User user : Users) {
            if(user.getNickname().equalsIgnoreCase(nickUtente))
                if(user.getStatus().equalsIgnoreCase("online")){
                    try {
                        update(nickUtente,"offline");
                    }catch(RemoteException e) { e.printStackTrace(); }
                    user.setStatus("offline");
                    return "ok";
                } else return "User is already offline -> error";
        }
        return "User doesn't exists";
    }

    public Result<NicknameStatusPair> listUsers() {
        if(Users.isEmpty()) return new Result("User list is empty!",null);
        ArrayList<NicknameStatusPair> listUsers = new ArrayList<>();
        for(User user : Users)
            listUsers.add(new NicknameStatusPair(user.getNickname(),user.getStatus()));
        return new Result<NicknameStatusPair>("ok",listUsers);
    }

    public Result<String> listOnlineUsers() {
        if(Users.isEmpty()) return new Result("User list is empty!",null);
        ArrayList<String> listOnlineUsers = new ArrayList<>();
        for(User user : Users)
            if(user.getStatus().equalsIgnoreCase("online"))
                listOnlineUsers.add(user.getNickname());
        if(listOnlineUsers.isEmpty()) return new Result("Online users list is empty!",null);

        return new Result<String>("ok",listOnlineUsers);
    }

    public Result<String> listProjects(String nickUtente) {
        if(Projects.isEmpty()) return new Result("Project list is empty!",null);
        ArrayList<String> listUserProjects = new ArrayList<>();
        for(Project project: Projects)
            if(project.isMember(nickUtente))
                listUserProjects.add(project.getID());

        if(listUserProjects.isEmpty()) return new Result("User doesn't participate to any project!",null);

        return new Result<String>("ok",listUserProjects);
    }

    public String createProject(String projectName, String nickUtente) {
        if(projectName.isEmpty()) return "ProjectName is empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
                return "The project exists already!";
        String ip = this.MipGenerator.generateIP();
        if(ip.equalsIgnoreCase("error")) return "IP GENERATOR ERROR!";
        Project project = new Project(projectName,nickUtente,ip,(random.nextInt((65535-1025+1))+1025));
        System.out.println(ip + " " + project.getPort());
        Projects.add(project);
        try {
            mapper.writeValue(projectFile, Projects);
            mapper.writeValue(MipGeneratorFile,MipGenerator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            updateMulticast(project,nickUtente);
        } catch (RemoteException e) { e.printStackTrace(); }
        return "ok";
    }

    public void generateIP(){

    }

    public String addMember(String projectName, String nickUtente, String addingUser) {
        boolean Projectfound = false, userFound = false;
        String code = "ERROR";
        if(projectName.isEmpty() || nickUtente.isEmpty()) return "ProjectName or NickUtente is empty";
        if(Projects.isEmpty()) return "Project list is empty!";

        for(User user : Users)
            if(user.getNickname().equalsIgnoreCase(nickUtente)) userFound = true;
        if(!userFound) return "The user is not registered in the system!";

        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName)) {
                if(!project.isMember(addingUser)) return "User doesn't participate to any project!";
                code = project.addMember(nickUtente);
                Projectfound = true;
                try {
                    mapper.writeValue(projectFile, Projects);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    updateMulticast(project,nickUtente);
                } catch (RemoteException e) { e.printStackTrace(); }
            }
        if(!Projectfound) return "The project doesn't exist";

        return code;
    }

    public Result<String> showMembers(String projectName, String nickUtente) {
        ArrayList<String> list;
        if(Projects.isEmpty()) return new Result("Project list is empty!",null);
        if(projectName.isEmpty()) return new Result("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return new Result("User doesn't participate to any project!",null);
                list = project.getMembers();
                if(list == null) return new Result("Member list is empty",null);
                else return new Result("ok",list);
            }
        return new Result("The project doesn't exist",null);
    }

    public Result<String> showCards(String projectName, String nickUtente) {
        ArrayList<String> list;
        if(Projects.isEmpty()) return new Result("Project list is empty!",null);
        if(projectName.isEmpty()) return new Result("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return new Result("User doesn't participate to any project!",null);
                list = project.getCardsName();
                if(list == null) return new Result("Cards list is empty",null);
                else return new Result("ok",list);
            }
        return new Result("The project doesn't exist",null);
    }

    public Result<String> showCard(String projectName, String cardName, String nickUtente){
        ArrayList<String> info;
        if(Projects.isEmpty()) return new Result("Project list is empty!",null);
        if(projectName.isEmpty() || cardName.isEmpty()) return new Result("ProjectName or cardName are empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return new Result("User doesn't participate to any project!",null);
                info = project.getCardInfo(cardName);
                if(info == null) return new Result("Card not found",null);
                else return new Result("ok",info);
            }
        return new Result("The project doesn't exist",null);
    }

    public String addCard(String projectName, String cardName, String description, String nickUtente){
        if(Projects.isEmpty()) return "Project list is empty";
        if(projectName.isEmpty() || cardName.isEmpty()) return "ProjectName or cardName are empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!";
                String code = project.addCard(cardName,description);
                try {
                    mapper.writeValue(projectFile, Projects);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return code;
            }
        return "The project doesn't exist";

    }

    public String moveCard(String projectName, String cardName, String fromList, String movetoList, String nickUtente) {
        if(Projects.isEmpty()) return "Project list is empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!";
                String code = project.moveCard(cardName,fromList,movetoList);
                try {
                    mapper.writeValue(projectFile, Projects);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return code;
            }
        return "The project doesn't exist";
    }

    public Result<String> getCardHistory(String projectName, String cardName, String nickUtente) {
        ArrayList<String> history;
        if(Projects.isEmpty()) return new Result("Project list is empty!",null);
        if(projectName.isEmpty() || cardName.isEmpty()) return new Result("ProjectName or cardName are empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return new Result("User doesn't participate to any project!",null);
                history = project.getCardHistory(cardName);
                if(history == null) return new Result("Card not found",null);
                else return new Result("ok",history);
            }
        return new Result("The project doesn't exist",null);
    }

    public chatINFO readChat(String ProjectName, String nickUtente) {
        if(Projects.isEmpty()) return new chatINFO("Project list is empty!",null);
        if(ProjectName.isEmpty()) return new chatINFO("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(ProjectName))
            {
                if(!project.isMember(nickUtente)) return new chatINFO("User doesn't participate to any project!",null);
                return new chatINFO("ok",project.getMulticastAddress());
            }
        return new chatINFO("The project doesn't exists", null);
    }

    public chatINFO sendChatMsg(String ProjectName, String nickUtente) {
        if(Projects.isEmpty()) return new chatINFO("Project list is empty!",null);
        if(ProjectName.isEmpty()) return new chatINFO("ProjectName is empty",null);
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(ProjectName))
            {
                if(!project.isMember(nickUtente)) return new chatINFO("User doesn't participate to any project!",null);
                return new chatINFO("ok",project.getMulticastAddress());
            }
        return new chatINFO("The project doesn't exists", null);
    }

    public String cancelProject(String projectName, String nickUtente) {
        if(Projects.isEmpty()) return "Project list is empty";
        for(Project project : Projects)
            if(project.getID().equalsIgnoreCase(projectName))
            {
                if(!project.isMember(nickUtente)) return "User doesn't participate to any project!";
                if(!project.isDone()) return "All cards in the project are not in status: DONE";
                Projects.remove(project);
                try {
                    mapper.writeValue(projectFile, Projects);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    updateMulticastALL(project);
                } catch (RemoteException e) { e.printStackTrace(); }
                return "ok";
            }
        return "The project doesn't exist";
    }




    public synchronized void registerForCallback (NotifyEventInterface ClientInterface, String nickUtente) throws RemoteException {
        boolean contains = clients.stream()
                .anyMatch(client -> ClientInterface.equals(client.getClient()));
        if (!contains){
            clients.add(new CallBackInfo(ClientInterface,nickUtente));
            System.out.println("CALLBACK SYSTEM: New client registered." );
        }
    }

    public synchronized void unregisterForCallback(NotifyEventInterface Client) throws RemoteException {
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

    public void update(String nickName, String status) throws RemoteException {
        doCallbacks(nickName,status);
    }

    public void updateMulticast(Project project,String nickName) throws RemoteException {
        doChatCallBacks(project,nickName);
    }

    public void updateMulticastALL(Project project) throws RemoteException {
        doChatCallBacksALL(project);
    }

    private synchronized void doChatCallBacksALL(Project project) throws RemoteException {
        LinkedList<NotifyEventInterface> errors = new LinkedList<>();
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        Iterator i = clients.iterator( );
        while (i.hasNext()) {
            CallBackInfo callbackinfoUser = (CallBackInfo) i.next();
            NotifyEventInterface client = callbackinfoUser.getClient();
            try {
                if(project.isMember(callbackinfoUser.getuserNickname()))
                    client.notifyEventProjectCancel(project.getMulticastAddress(), project.getPort());
            }catch (RemoteException e) { errors.add(client);}
        }
        if(!errors.isEmpty()) {
            System.out.println("CALLBACK SYSTEM: Unregistering clients that caused an error!");
            for(NotifyEventInterface Nei : errors) unregisterForCallback(Nei);
        }
        System.out.println("CALLBACK SYSTEM: Callbacks complete.");
    }

    private synchronized void doChatCallBacks(Project project,String nickName) throws RemoteException {
        LinkedList<NotifyEventInterface> errors = new LinkedList<>();
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        Iterator i = clients.iterator( );
        while (i.hasNext()) {
            CallBackInfo callbackinfoUser = (CallBackInfo) i.next();
            NotifyEventInterface client = callbackinfoUser.getClient();
            if(callbackinfoUser.getuserNickname().equalsIgnoreCase(nickName))
                client.notifyEventChat(project.getMulticastAddress(), project.getPort());
        }
        System.out.println("CALLBACK SYSTEM: Callbacks complete.");
    }

    private synchronized void doCallbacks(String nickName, String status) throws RemoteException {
        LinkedList<NotifyEventInterface> errors = new LinkedList<>();
        System.out.println("CALLBACK SYSTEM: Starting callbacks.");
        Iterator i = clients.iterator( );
        while (i.hasNext()) {
            CallBackInfo callbackinfoUser = (CallBackInfo) i.next();
            NotifyEventInterface client = callbackinfoUser.getClient();
            try {
                client.notifyEvent(nickName,status);
            }catch (RemoteException e) { errors.add(client);}
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
