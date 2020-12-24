import java.io.IOException;

public interface ServerMainInterface  {

    /*
    The login method is used to login in the system. Server response with:
    1) If the login was ok -> returns "OK"
    2) If the login was not ok -> returns the error
    Possible Errors:
    - User already logged in
    - Wrong password
    - User or password are empty!
    - User list is empty!
    Implementation: Has to be done when TCP connection is up. Server has to response with list of user and online/offline users
                    SERVER MUST UPDATE THE USERS REGISTERED AND THEIR STATUS WITH A CALLBACK SYSTEM.
     */
    public LoginResult<NicknameStatusPair> login(String nickUtente, String password) throws IOException;

    /*
    The logout method is a simply logout.
    Implementation: Command sent by TCP
     */
    public String logout(String nickUtente);

    /*
    The listUsers method is used to list the nicks of users in the system and their status.
    Possible situations:
    - LIST NULL
    Implementation: Command sent by TCP
     */
    public Result<NicknameStatusPair> listUsers();

    /*
    The listOnlineUsers method is used to list the nicks of users in the system that are online
    Possible situations:
    - LIST NULL
    Implementation: Command sent by TCP
     */
    public Result<String> listOnlineUsers();

    /*
    The listProjects is used to list all the projects that the user participates
    Possible situations:
    - LIST NULL
    Implementation: Command sent by TCP
     */
    public Result<String> listProjects(String nickUtente);

    /*
    The createProject method is used to create a new project.
    1) If the project can be created -> Add the user that created it to the project
    2) return error -> "Project cant be created! it exists yet"
    Implementation: Command sent by TCP
     */
    public String createProject(String projectName,String nickUtente);

    /*
    The addMember method is used to add an user to a Project.
    1) If the user is registered to the system -> the server adds it without confirm
    2) Possible errors:
       - The user is not registered -> returns "The user can't be added to the project 'cause he's not registered
       - The project doesn't exist -> returns "The user can't be added to the project 'cause the project doesn't exists
     */
    public String addMember(String projectName, String nickUtente, String addingUser);

    /*
    The showMembers method is used to get the list of members that participate to a project
    Possible situation:
    - Project doesn't exist -> Returns null
    - List NULL
     */
    public Result<String> showMembers(String projectName, String nickUtente);

    /*
    Possible status of a card:
    1) TODO
    2) InProgress
    3) ToBeRevised
    4) DONE
     */

    /*
    The showCards method is used to get the list of cards in a project
    Possible situations:
    - Project doesn't exists -> Returns null
    - List NULL
     */
    public Result<String> showCards(String projectName, String nickUtente);


    /*
    The showCard method is used to get the info of a specific card in a project
    Possible situations:
    - Project doesn't exists -> Returns null
    - List NULL
     */
    public Result<String> showCard(String projectName, String cardName, String nickUtente);


    /*
    The addCard method is used to add the card
     */
    public String addCard(String projectName, String cardName, String description, String nickUtente);

    /*
    The moveCard method its used to move a card from a list to another one
    Possible situations:
    - Project doesn't exists -> returns "Card can't be moved, the project doesn't exists"
    - FromList name error -> returns "Error in the name of fromList"
    - ToList name error -> returns "Error in the name of ToList"
    - cardName is not in the fromList -> returns "Card doesn't exists in the fromList"
    - Can't move the card from the fromList to the toList 'cause the movement is not permitted
     */
    public String moveCard(String projectName, String cardName, String fromList, String movetoList, String nickUtente);

    /*
    The getCardHistory method returns the history of the card -> movements of the card in the status lists
    Possible situations:
    - Project doesnt exists -> return NULL
    - CardName doesnt exists in the project -> Return NULL
    - LIST NULL
     */
    public Result<String> getCardHistory(String projectName, String cardName,String nickUtente);

    /*
    The readChat method shows all the messages wrote in the chat of the project
    TODO
    Possible situations:

     */
    public chatINFO readChat(String ProjectName, String nickUtente);

    /*
    The sendChatMsg method sends a message in the chat of the project
     */
    public chatINFO sendChatMsg(String ProjectName, String nickUtente);

    /*
    The cancelProject method delete a project. It can be done only if all the cards are in status DONE.
    Possible situations:
    - Project canceled -> Success message
    - ProjectName doesn't exists -> Error message
    - All the cards are not in status DONE -> Error message
     */
    public String cancelProject(String projectName, String nickUtente);


}
