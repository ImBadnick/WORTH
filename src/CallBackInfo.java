public class CallBackInfo{ //Info for callbacks
    private NotifyEventInterface client;
    private String userNickname;
    public CallBackInfo(NotifyEventInterface client, String userNickname){
        this.client = client;
        this.userNickname = userNickname;
    }

    //Getters
    public NotifyEventInterface getClient(){ return this.client; }
    public String getuserNickname(){ return this.userNickname; }

    //Setters
    public void setClient(NotifyEventInterface client){ this.client = client; }
    public void setUserNickname(String userNickname){ this.userNickname = userNickname; }
}
