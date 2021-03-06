package beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.Api;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;

/**
 * Created by willpride on 4/13/16.
 */
@Api(description = "A menu command")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {
    private int index;
    private String displayText;
    private String audioUri;
    private String imageUri;
    private NavIconState navigationState;

    enum NavIconState {
        NEXT, JUMP
    }

    public NavIconState getNavigationState() {
        return navigationState;
    }

    public void setNavigationState(NavIconState navigatonState) {
        this.navigationState = navigatonState;
    }

    public Command(){}

    public Command(MenuDisplayable menuDisplayable, int index, CommCareSession session){
        super();
        this.setIndex(index);
        this.setDisplayText(menuDisplayable.getDisplayText());
        this.setImageUri(menuDisplayable.getImageURI());
        this.setAudioUri(menuDisplayable.getAudioURI());
        this.setNavigationState(getIconState(menuDisplayable, session));
    }

    private NavIconState getIconState(MenuDisplayable menuDisplayable, CommCareSession session) {
        NavIconState iconChoice = NavIconState.NEXT;

        //figure out some icons
        if (menuDisplayable instanceof Entry) {
            SessionDatum datum = session.getNeededDatum((Entry)menuDisplayable);
            if (datum == null || !(datum instanceof EntityDatum)) {
                iconChoice = NavIconState.JUMP;
            }
        }
        return iconChoice;
    }

    public int getIndex() {
        return index;
    }

    private void setIndex(int index) {
        this.index = index;
    }

    public String getDisplayText() {
        return displayText;
    }

    private void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getAudioUri() {
        return audioUri;
    }

    private void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    public String getImageUri() {
        return imageUri;
    }

    private void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    @Override
    public String toString(){
        return "Command [index=" + index + ", text=" + displayText + ", " +
                "audioUri=" + audioUri + ", imageUri=" + imageUri + "]";
    }
}
