package ProjectUtils;

import java.io.Serializable;
import java.util.ArrayList;

public class Result<T> implements Serializable { //Used to return results to the client
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
