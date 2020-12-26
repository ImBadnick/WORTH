package ProjectUtils;
import java.io.Serializable;
import java.util.ArrayList;

public class LoginResult<T> extends Result<T> implements Serializable { //Used by the server to return Login operation result
    private ArrayList<multicastINFO> multicastinfo;

    public LoginResult(String code, ArrayList<T> list, ArrayList<multicastINFO> multicastinfo){
        super(code,list);
        this.multicastinfo = multicastinfo;
    }

    public ArrayList<multicastINFO> getMulticastinfo(){ return this.multicastinfo;}
    public void setMulticastinfo(ArrayList<multicastINFO> multicastinfo) { this.multicastinfo = multicastinfo;}
}

