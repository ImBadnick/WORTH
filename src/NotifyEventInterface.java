import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote{
    public void notifyEvent(String nickName, String status) throws RemoteException;
    public void notifyEventChat(String address, int port) throws RemoteException;
    public void notifyEventProjectCancel(String address, int port) throws RemoteException;
}


