import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Card {
    public enum cardStatus { TODO, INPROGRESS, TOBEREVISED, DONE}
    private String name;
    private String description;
    private cardStatus currentList;
    private ArrayList<cardStatus> cardHistory;

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        this.currentList = cardStatus.TODO;
        this.cardHistory = new ArrayList<>();
        this.cardHistory.add(cardStatus.TODO);
    }

    public Card(){}

    public String getName(){
        return this.name;
    }

    @JsonIgnore
    public ArrayList<String> getInfo(){
        ArrayList<String> info = new ArrayList<String>();
        info.add(this.name);
        info.add(this.description);
        info.add(this.currentList.name());
        return info;
    }

    public String getCurrentList(){
        return this.currentList.name();
    }

    @JsonIgnore
    public void changeCurrentList(String toList){
        this.currentList = cardStatus.valueOf(toList.toUpperCase());
        this.cardHistory.add(cardStatus.valueOf(toList.toUpperCase()));
    }

    public ArrayList<String> getCardHistory(){
        ArrayList<String> history = new ArrayList<>();
        for(cardStatus status : cardHistory)
            history.add(status.name());
        return history;
    }

    public void setCardHistory(ArrayList<cardStatus> cardHistory) {
        this.cardHistory = cardHistory;
    }

    public void setCurrentList(String currentList) {
        this.currentList = cardStatus.valueOf(currentList.toUpperCase());
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
