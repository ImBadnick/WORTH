package ProjectUtils;
import java.io.Serializable;

public class NicknameStatusPair implements Serializable { //Used to return to the client the information nickname-status
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
