import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private ObjectMapper mapper;


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
        Members.add(nickUtente);
        this.dirPath = "./Backup/" + id;
        projectDir = new File(dirPath);
        if(!projectDir.exists()) projectDir.mkdir();
    }

    public Project() {
        this.mapper = new ObjectMapper();
    }


    public String getID(){ return this.id; }

    public File getProjectDir() {
        return projectDir;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
        projectDir = new File(dirPath);
        if(!projectDir.exists()) projectDir.mkdir();
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
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

    public ArrayList<String> getMembers(){
        if(Members.isEmpty()) return null;
        return (ArrayList<String>) this.Members.clone();
    }

    @JsonIgnore
    public ArrayList<String> getCardsName(){
        if(Cards.isEmpty()) return null;
        ArrayList<String> list = new ArrayList<>();
        for(Card card : Cards)
            list.add(card.getName());
        return list;
    }

    public ArrayList<Card> getCards(){
        return this.Cards;
    }


    public ArrayList<String> getCardInfo(String cardName){
        if(Cards.isEmpty()) return null;
        ArrayList<String> info = null;
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName))
                info = card.getInfo();
        return info;
    }

    public String addCard(String cardName,String description){
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName))
                return "Card already exists in the project!";
        Card card = new Card(cardName,description);
        Cards.add(card);
        TodoCards.add(cardName);
        File cardFile = new File(projectDir + "/" + cardName + ".json");
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
            if(card.getName().equalsIgnoreCase(cardName)) {
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
                File cardFile = new File(projectDir + "/" + cardName + ".json");
                try {
                    mapper.writeValue(cardFile, card);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "ok";
            }
        return "Card not found";
    }

    public ArrayList<String> getCardHistory(String cardName){
        if(Cards.isEmpty()) return null;
        ArrayList<String> history = null;
        for(Card card : Cards)
            if(card.getName().equalsIgnoreCase(cardName))
                history = card.getCardHistory();
        return history;
    }

    @JsonIgnore
    public boolean isDone(){
        if(Cards.isEmpty()) return true;
        for(Card card : Cards)
            if(!card.getCurrentList().equalsIgnoreCase("done")) return false;
        return true;
    }

    public int getPort(){
        return this.port;
    }
    public String getMulticastAddress(){
        return this.MulticastAddress;
    }

    public ArrayList<String> getDoneCards() {
        return DoneCards;
    }

    public ArrayList<String> getInProgressCards() {
        return InProgressCards;
    }

    public ArrayList<String> getToBeRevisedCards() {
        return ToBeRevisedCards;
    }

    public ArrayList<String> getTodoCards() {
        return TodoCards;
    }

    public void setCards(ArrayList<Card> cards) {
        Cards = cards;
    }

    public void setDoneCards(ArrayList<String> doneCards) {
        DoneCards = doneCards;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInProgressCards(ArrayList<String> inProgressCards) {
        InProgressCards = inProgressCards;
    }

    public void setMembers(ArrayList<String> members) {
        Members = members;
    }

    public void setTodoCards(ArrayList<String> todoCards) {
        TodoCards = todoCards;
    }

    public void setToBeRevisedCards(ArrayList<String> toBeRevisedCards) {
        ToBeRevisedCards = toBeRevisedCards;
    }

    public void setMulticastAddress(String multicastAddress) {
        MulticastAddress = multicastAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
