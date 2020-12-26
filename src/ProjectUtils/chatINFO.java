package ProjectUtils;
import java.io.Serializable;

public class chatINFO implements Serializable { //Used by the server to return the chatINFO of the project to the client (To read and send msg)
    private String code;
    private String ipAddress;
    public chatINFO(String code, String ipAddress){
        this.code = code;
        this.ipAddress = ipAddress;
    }

    public void setCode(String code) { this.code = code; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCode() { return code; }
    public String getIpAddress() { return ipAddress; }
}
