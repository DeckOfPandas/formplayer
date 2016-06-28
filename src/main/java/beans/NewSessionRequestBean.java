package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import objects.SessionData;

import java.util.Map;

/**
 * Request to start a new form entry sessios
 * Optionally contains instanceContent for form editing or incomplete forms
 */
@JsonIgnoreProperties
public class NewSessionRequestBean {
    private String formUrl;
    private String lang;
    private Map<String, String> hqAuth;
    private SessionData sessionData;
    private Map<String, Object> formContext;
    private String instanceContent;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public NewSessionRequestBean(){}

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
    @JsonGetter(value = "form-url")
    public String getFormUrl() {
        return formUrl;
    }
    @JsonSetter(value = "form-url")
    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }
    @JsonGetter(value = "hq_auth")
    public Map<String, String> getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(Map<String, String> hqAuth) {
        this.hqAuth = hqAuth;
    }
    @JsonGetter(value = "session-data")
    public SessionData getSessionData() {
        return sessionData;
    }
    @JsonSetter(value = "session-data")
    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }
    @JsonGetter(value = "form_context")
    public Map<String, Object> getFormContext() {
        return formContext;
    }
    @JsonSetter(value = "form_context")
    public void setFormContext(Map<String, Object> formContext) {
        this.formContext = formContext;
    }

    @JsonGetter(value = "instance-content")
    public String getInstanceContent() {
        return instanceContent;
    }
    @JsonSetter(value = "instance-content")
    public void setInstanceContent(String instanceContent) {
        this.instanceContent = instanceContent;
    }

    public String toString(){
        return "New Session Request Bean [form-url=" + formUrl +
                ", auth=" + hqAuth +
                ", sessionData= " + sessionData +
                ", instanceContent=" + instanceContent +"]";
    }
}
