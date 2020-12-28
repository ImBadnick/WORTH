import ProjectUtils.PasswordUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private String nickname;
    private String status;
    private String salt;
    private String SecurePassword;

    public User(String nickname, String password){
        this.nickname = nickname;
        this.status = "offline";
        this.salt = PasswordUtils.getSalt(30);
        this.SecurePassword = PasswordUtils.generateSecurePassword(password,salt);
    }

    public User(){
        this.status = "offline";
    }

    @JsonIgnore
    public void setStatus(String status) { this.status = status; }
    public void setNickName(String nickname){  this.nickname = nickname; }
    public void setSalt(String salt) { this.salt = salt; }
    public void setSecurePassword(String securePassword) { this.SecurePassword = securePassword; }

    @JsonIgnore
    public String getStatus() { return this.status; }
    public String getNickname(){ return this.nickname;}
    public String getSalt() { return salt; }
    public String getSecurePassword() { return SecurePassword; }

    public boolean passwordMatch(String password){
        boolean passwordMatch = PasswordUtils.verifyUserPassword(password, this.SecurePassword, salt);
        return passwordMatch;
    }

}
