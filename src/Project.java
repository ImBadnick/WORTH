import ProjectUtils.NicknameStatusPair;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

@JsonPropertyOrder({ "id", "Cards", "TodoCards", "InProgressCards", "ToBeRevisedCards", "DoneCards", "Members", "MulticastAddress", "port" })
public class Project {
    private String id;
    private ArrayList<Card> Cards;
    private ArrayList<String> TodoCards;
    private ArrayList<String> InProgressCards;
    private ArrayList<String> ToBeRevisedCards;
    private ArrayList<String> DoneCards;
    private ArrayList<String> Members;
    private String MulticastAddress;
    private int port;
    private String dirPath;
    @JsonIgnore
    private File projectDir;
    @JsonIgnore
    private final ObjectMapper mapper;


    public Project(String id,String nickUtente,String MulticastAddress, int port) {
        this.id = id;
        this.Cards = new ArrayList<>();
        this.TodoCards = new ArrayList<>();
        this.InProgressCards = new ArrayList<>();
        this.ToBeRevisedCards = new ArrayList<>();
        this.DoneCards = new ArrayList<>();
        this.Members = new ArrayList<>();
        this.MulticastAddress = MulticastAddress;
        this.port = port;
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Members.add(nickUtente);
        this.dirPath = "./Backup/" + id;
        projectDir = new File(dirPath);
        if(!projectDir.exists()) projectDir.mkdir();
    }

    public Project() {
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public boolean isMember(String nickname){
        if(Members.isEmpty()) return false;
        for(String user : Members)
            if(user.equalsIgnoreCase(nickname)) return true;
        return false;
    }

    public String addMember(String nickUtente){
        for(String user : Members)
            if(user.equalsIgnoreCase(nickUtente)) return "The user added is already in the project!";
        Members.add(nickUtente);
        return "ok";
    }

    public String addCard(String cardName,String description){
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName)) //Checks if the card already exists in the project
                return "Card already exists in the project!";
        Card card = new Card(cardName,description);
        Cards.add(card);
        TodoCards.add(cardName);
        //Create card file info
        File cardFile = new File(projectDir + "/" + cardName + ".json");
        if(!cardFile.exists()) {
            try {
                cardFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }
        //Backup card file
        try {
            mapper.writeValue(cardFile, card);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "OK";
    }

    public String moveCard(String cardName, String fromList, String toList){
        try{
            Card.cardStatus.valueOf(fromList.toUpperCase());
        }catch(IllegalArgumentException ex){ return "Not a valid fromList";}
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName)) { //Searching the card
                //MOVING THE CARD FROM fromlist TO tolist
                if (!toList.equalsIgnoreCase("todo") && !toList.equalsIgnoreCase("inprogress") && !toList.equalsIgnoreCase("toberevised") && !toList.equalsIgnoreCase("done"))
                    return "Error to list";
                switch (card.getCurrentList().toLowerCase()) {
                    case "todo":
                        if (!fromList.equalsIgnoreCase("todo")) return "Error from list!";
                        if (!toList.equalsIgnoreCase("inprogress"))
                            return "Can't move card from: " + fromList + " to: " + toList;
                        card.changeCurrentList(toList.toUpperCase());
                        TodoCards.remove(cardName);
                        InProgressCards.add(cardName);
                        break;

                    case "inprogress":
                        if (!fromList.equalsIgnoreCase("inprogress")) return "Error from list!";
                        if (!toList.equalsIgnoreCase("done") && !toList.equalsIgnoreCase("toberevised"))
                            return "Can't move card from: " + fromList + " to: " + toList;
                        if (fromList.equalsIgnoreCase("done")) {
                            card.changeCurrentList(toList.toUpperCase());
                            InProgressCards.remove(cardName);
                            DoneCards.add(cardName);
                        } else {
                            card.changeCurrentList(toList.toUpperCase());
                            InProgressCards.remove(cardName);
                            ToBeRevisedCards.add(cardName);
                        }
                        break;

                    case "toberevised":
                        if (!fromList.equalsIgnoreCase("toberevised")) return "Error from list!";
                        if (!toList.equalsIgnoreCase("done") && !toList.equalsIgnoreCase("inprogress"))
                            return "Can't move card from: " + fromList + " to: " + toList;
                        if (fromList.equalsIgnoreCase("done")) {
                            card.changeCurrentList(toList.toUpperCase());
                            ToBeRevisedCards.remove(cardName);
                            DoneCards.add(cardName);
                        } else {
                            card.changeCurrentList(toList.toUpperCase());
                            ToBeRevisedCards.remove(cardName);
                            InProgressCards.add(cardName);
                        }
                        break;

                    case "done":
                        if (!fromList.equalsIgnoreCase("done")) return "Error from list!";
                        return "Can't move card from: " + fromList + " to: " + toList;
                }
                File cardFile = new File(projectDir + "/" + card.getName() + ".json");
                try {
                    mapper.writeValue(cardFile, card);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "ok";
            }
        return "Card not found";
    }


    @JsonIgnore
    public ArrayList<NicknameStatusPair> getCardsName(){ //Get all the cards names in the project
        if(Cards.isEmpty()) return null;
        ArrayList<NicknameStatusPair> list = new ArrayList<>();
        for(Card card : Cards)
            list.add(new NicknameStatusPair(card.getName(),card.getCurrentList()));
        return list;
    }

    @JsonIgnore
    public boolean isDone(){ //Checks if all the cards status are "DONE"
        if(Cards.isEmpty()) return true;
        for(Card card : Cards)
            if(!card.getCurrentList().equalsIgnoreCase("done")) return false;
        return true;
    }

    @JsonIgnore
    public void cancelProjectDir(){
        try {
            Files.walk(projectDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public ArrayList<String> getCardHistory(String cardName){ //Gets the history list of the card
        if(Cards.isEmpty()) return null;
        ArrayList<String> history = null;
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName))
                history = card.getCardHistory();
        return history;
    }

    public ArrayList<String> getCardInfo(String cardName){ //Gets the card informations
        if(Cards.isEmpty()) return null;
        ArrayList<String> info = null;
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName))
                info = card.getInfo();
        return info;
    }

    public ArrayList<String> getMembers(){ //Gets the members in the project
        if(Members.isEmpty()) return null;
        return (ArrayList<String>) this.Members.clone();
    }

    public String getID(){ return this.id; }
    public File getProjectDir() {return projectDir; }
    public String getDirPath() {
        return dirPath;
    }
    public ArrayList<Card> getCards(){
        return this.Cards;
    }
    public ArrayList<String> getDoneCards() { return DoneCards; }
    public int getPort(){
        return this.port;
    }
    public String getMulticastAddress(){
        return this.MulticastAddress;
    }
    public ArrayList<String> getInProgressCards() { return InProgressCards; }
    public ArrayList<String> getToBeRevisedCards() { return ToBeRevisedCards; }
    public ArrayList<String> getTodoCards() { return TodoCards; }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
        projectDir = new File(dirPath);
        if(!projectDir.exists()) projectDir.mkdir();
    }
    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }
    public void setCards(ArrayList<Card> cards) { Cards = cards; }
    public void setDoneCards(ArrayList<String> doneCards) { DoneCards = doneCards; }
    public void setId(String id) { this.id = id; }
    public void setInProgressCards(ArrayList<String> inProgressCards) { InProgressCards = inProgressCards; }
    public void setMembers(ArrayList<String> members) { Members = members; }
    public void setTodoCards(ArrayList<String> todoCards) { TodoCards = todoCards; }
    public void setToBeRevisedCards(ArrayList<String> toBeRevisedCards) { ToBeRevisedCards = toBeRevisedCards; }
    public void setMulticastAddress(String multicastAddress) { MulticastAddress = multicastAddress; }
    public void setPort(int port) { this.port = port; }



}
