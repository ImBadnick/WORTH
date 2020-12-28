import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class Card {
    public enum cardStatus { TODO, INPROGRESS, TOBEREVISED, DONE}
    private String name;
    private String description;
    private cardStatus currentList;
    private List<cardStatus> cardHistory;

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        this.currentList = cardStatus.TODO;
        this.cardHistory = new ArrayList<>();
        this.cardHistory.add(cardStatus.TODO);
    }

    public Card(){}

    @JsonIgnore
    public void changeCurrentList(String toList){ //Changes the current list to the "ToList" value
        this.currentList = cardStatus.valueOf(toList.toUpperCase());
        this.cardHistory.add(cardStatus.valueOf(toList.toUpperCase()));
    }
    @JsonIgnore
    public List<String> getInfo(){ //Gets card information
        List<String> info = new ArrayList<>();
        info.add(this.name);
        info.add(this.description);
        info.add(this.currentList.name());
        return info;
    }
    public List<String> getCardHistory(){ //Gets card history list
        List<String> history = new ArrayList<>();
        for(cardStatus status : cardHistory)
            history.add(status.name());
        return history;
    }
    public String getName(){
        return this.name;
    }
    public String getCurrentList(){
        return this.currentList.name();
    }

    public void setCardHistory(List<cardStatus> cardHistory) { this.cardHistory = cardHistory; }
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
