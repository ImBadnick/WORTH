package ProjectUtils;
import java.io.Serializable;

public class multicastINFO implements Serializable { //Used to add in the login Result the multicast info of the projects
    private String ipAddress;
    private int port;
    public multicastINFO(String ipAddress, int port){
        this.ipAddress = ipAddress;
        this.port = port;
    }
    public String getIpAddress(){ return this.ipAddress; }
    public int getPort(){ return this.port; }

    public void setPort(int port) { this.port = port; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
