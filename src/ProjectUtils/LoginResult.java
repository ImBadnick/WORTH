package ProjectUtils;
import java.io.Serializable;
import java.util.List;

public class LoginResult<T> extends Result<T> implements Serializable { //Used by the server to return Login operation result
    private List<multicastINFO> multicastinfo;

    public LoginResult(String code, List<T> list, List<multicastINFO> multicastinfo){
        super(code,list);
        this.multicastinfo = multicastinfo;
    }

    public List<multicastINFO> getMulticastinfo(){ return this.multicastinfo;}
    public void setMulticastinfo(List<multicastINFO> multicastinfo) { this.multicastinfo = multicastinfo;}
}

