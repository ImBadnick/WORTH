import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerMainInterfaceRMI extends Remote {

    /*
    The register method is used to insert a new user in the system. Server response with:
    1) If the registration was ok -> returns "User added with success"
    2) If the registration was not ok -> Returns the error
    Possible Errors:
    - User already registered in the system
    - Password or User empty
    Implementation: RMI - THE REGISTRATION HAS TO BE PERSISTENT!
     */
    public String register(String nickUtente, String password) throws RemoteException;

    public void registerForCallback (NotifyEventInterface ClientInterface,String nickUtente) throws RemoteException;

    public void unregisterForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
}
