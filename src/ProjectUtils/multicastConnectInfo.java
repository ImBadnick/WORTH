package ProjectUtils;
import java.net.MulticastSocket;

public class multicastConnectInfo extends multicastINFO{ //Class used to save the multicast information retrieved from the server.
    private MulticastSocket socket; //Multicast Socket

    public multicastConnectInfo(MulticastSocket socket, String address, int port){
        super(address,port);
        this.socket = socket;
    }

    public void setSocket(MulticastSocket socket) { this.socket = socket; }
    public MulticastSocket getSocket() { return socket; }
}
