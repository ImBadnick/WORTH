import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private String nickname;
    private String password;
    private String status;

    public User(String nickname, String password){
        this.nickname = nickname;
        this.password = password;
        this.status = "offline";
    }

    public User(){
        this.status = "offline";
    }

    public void setNickName(String nickname){  this.nickname = nickname; }
    public void setPassword(String password){  this.password = password; }

    @JsonIgnore
    public void setStatus(String status) { this.status = status; }

    public String getNickname(){ return this.nickname;}
    public String getPassword() { return this.password; }

    @JsonIgnore
    public String getStatus() { return this.status; }
}
