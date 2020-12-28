package ProjectUtils;

import java.io.Serializable;
import java.util.List;

public class Result<T> implements Serializable { //Used to return results to the client
    private String code;
    private List<T> list;

    public Result(String code, List<T> list){
        this.list = list;
        this.code = code;
    }

    public List<T> getList(){ return this.list; }
    public String getCode() { return this.code; }

    public void setList(List<T> list){ this.list = list;}
    public void setCode(String code){ this.code = code;}
}
