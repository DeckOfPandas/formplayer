package application;

import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import hq.CaseAPIs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.json.JsonActionUtils;
import org.commcare.modern.process.FormRecordProcessorHelper;
import org.commcare.util.cli.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.MenuRepo;
import repo.SessionRepo;
import requests.InstallRequest;
import requests.NewFormRequest;
import services.InstallService;
import services.RestoreService;
import services.XFormService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "sessioncontroller", description = "Operations for navigating CommCare Menus, Cases, and Forms")
@RestController
@EnableAutoConfiguration
public class SessionController {

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private XFormService xFormService;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private MenuRepo menuRepo;

    @Autowired
    private InstallService installService;

    Log log = LogFactory.getLog(SessionController.class);
    ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Start a new form entry session")
    @RequestMapping(value = Constants.URL_NEW_SESSION , method = RequestMethod.POST)
    public NewFormSessionResponse newFormResponse(@RequestBody NewSessionRequestBean newSessionBean) throws Exception {
        log.info("New form requests with bean: " + newSessionBean);
        NewFormRequest newFormRequest = new NewFormRequest(newSessionBean, sessionRepo, xFormService, restoreService);
        NewFormSessionResponse newSessionResponse = newFormRequest.getResponse();
        log.info("Return new session response: " + newSessionResponse);
        return newSessionResponse;
    }

    @ApiOperation(value = "Answer the question at the given index")
    @RequestMapping(value = Constants.URL_ANSWER_QUESTION, method = RequestMethod.POST)
    public AnswerQuestionResponseBean answerQuestion(@RequestBody AnswerQuestionRequestBean answerQuestionBean) throws Exception {
        log.info("Answer question with bean: " + answerQuestionBean);
        SerializableFormSession session = sessionRepo.find(answerQuestionBean.getSessionId());
        FormSession formEntrySession = new FormSession(session);

        JSONObject resp = JsonActionUtils.questionAnswerToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                answerQuestionBean.getAnswer() != null? answerQuestionBean.getAnswer().toString() : null,
                answerQuestionBean.getFormIndex());

        updateSession(formEntrySession, session);

        AnswerQuestionResponseBean responseBean = mapper.readValue(resp.toString(), AnswerQuestionResponseBean.class);
        responseBean.setSequenceId(formEntrySession.getSequenceId() + 1);
        log.info("Answer response: " + responseBean);
        return responseBean;
    }

    @ApiOperation(value = "Get the current question (deprecated)")
    @RequestMapping(value = Constants.URL_CURRENT, method = RequestMethod.GET)
    @ResponseBody
    public CurrentResponseBean getCurrent(@RequestBody CurrentRequestBean currentRequestBean) throws Exception {
        log.info("Current request: " + currentRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(currentRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        CurrentResponseBean currentResponseBean = new CurrentResponseBean(formEntrySession);
        log.info("Current response: " + currentResponseBean);
        return currentResponseBean;
    }
    @ApiOperation(value = "Submit the current form")
    @RequestMapping(value = Constants.URL_SUBMIT_FORM, method = RequestMethod.POST)
    @ResponseBody
    public SubmitResponseBean submitForm(@RequestBody SubmitRequestBean submitRequestBean) throws Exception {
        log.info("Submit form with bean: " + submitRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(submitRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        FormRecordProcessorHelper.processXML(formEntrySession.getSandbox(), formEntrySession.submitGetXml());
        SubmitResponseBean submitResponseBean = new SubmitResponseBean(formEntrySession);
        log.info("Submit response bean: " + submitResponseBean);
        return submitResponseBean;
    }

    @ApiOperation(value = "Get the current instance XML")
    @RequestMapping(value = Constants.URL_GET_INSTANCE, method = RequestMethod.POST)
    @ResponseBody
    public GetInstanceResponseBean getInstance(@RequestBody GetInstanceRequestBean getInstanceRequestBean) throws Exception {
        log.info("Get instance request: " + getInstanceRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(getInstanceRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        GetInstanceResponseBean getInstanceResponseBean = new GetInstanceResponseBean(formEntrySession);
        log.info("Get instance response: " + getInstanceResponseBean);
        return getInstanceResponseBean;
    }

    @ApiOperation(value = "Evaluate the given XPath under the current context")
    @RequestMapping(value = Constants.URL_EVALUATE_XPATH, method = RequestMethod.GET)
    @ResponseBody
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        log.info("Evaluate XPath Request: " + evaluateXPathRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(evaluateXPathRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
        log.info("Evaluate XPath Response: " + evaluateXPathResponseBean);
        return evaluateXPathResponseBean;
    }

    @ApiOperation(value = "Expand the repeat at the given index")
    @RequestMapping(value = Constants.URL_NEW_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public RepeatResponseBean newRepeat(@RequestBody RepeatRequestBean newRepeatRequestBean) throws Exception {
        log.info("New repeat: " + newRepeatRequestBean);
        SerializableFormSession serializableFormSession = sessionRepo.find(newRepeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);

        JsonActionUtils.descendRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                newRepeatRequestBean.getIx());

        updateSession(formEntrySession, serializableFormSession);

        JSONObject response = JsonActionUtils.getCurrentJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel());
        RepeatResponseBean repeatResponseBean = mapper.readValue(response.toString(), RepeatResponseBean.class);
        log.info("New response: " + repeatResponseBean);
        return repeatResponseBean;
    }

    @ApiOperation(value = "Delete the repeat at the given index")
    @RequestMapping(value = Constants.URL_DELETE_REPEAT, method = RequestMethod.POST)
    @ResponseBody
    public RepeatResponseBean deleteRepeat(@RequestBody RepeatRequestBean repeatRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = sessionRepo.find(repeatRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);

        JSONObject resp = JsonActionUtils.deleteRepeatToJson(formEntrySession.getFormEntryController(),
                formEntrySession.getFormEntryModel(),
                repeatRequestBean.getIx(), repeatRequestBean.getFormIndex());

        updateSession(formEntrySession, serializableFormSession);

        return mapper.readValue(resp.toString(), RepeatResponseBean.class);
    }

    @ApiOperation(value = "Filter the user's casedb given a predicate expression")
    @RequestMapping(value = Constants.URL_FILTER_CASES, method = RequestMethod.GET)
    public CaseFilterResponseBean filterCasesHQ(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        String caseResponse = CaseAPIs.filterCases(filterRequest);
        return new CaseFilterResponseBean(caseResponse);
    }

    @ApiOperation(value = "Fitler the user's casedb given a predicate expression returning all case data")
    @RequestMapping(value = Constants.URL_FILTER_CASES_FULL, method = RequestMethod.GET)
    public CaseFilterFullResponseBean filterCasesFull(@RequestBody CaseFilterRequestBean filterRequest) throws Exception {
        filterRequest.setRestoreService(restoreService);
        CaseBean[] caseResponse = CaseAPIs.filterCasesFull(filterRequest);
        return new CaseFilterFullResponseBean(caseResponse);
    }

    @ApiOperation(value = "Sync the user's database with the server")
    @RequestMapping(value = Constants.URL_SYNC_DB, method = RequestMethod.POST)
    public SyncDbResponseBean syncUserDb(@RequestBody SyncDbRequestBean syncRequest) throws Exception {
        log.info("SyncDb Request: " + syncRequest);
        syncRequest.setRestoreService(restoreService);
        String restoreXml = syncRequest.getRestoreXml();
        CaseAPIs.restoreIfNotExists(syncRequest.getUsername(), restoreXml);
        return new SyncDbResponseBean();
    }

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public SessionBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        InstallRequest installRequest = new InstallRequest(installRequestBean, restoreService, menuRepo, installService);
        return getNextMenu(installRequest.getMenuSession(), true);
    }

    /**
     * Make a menu selection, return a new Menu or a Form to display. This form will load a MenuSession object
     * from persistence, make the selection, and update the record. If the selection began form entry, this will
     * also create a new FormSession object.
     *
     * @param menuSelectBean Give the selection to be made on the current MenuSession
     *                       (could be a module, form, or case selection)
     * @return A MenuResponseBean or a NewFormSessionResponse
     * @throws Exception
     */
    @ApiOperation(value = "Make the given menu selection and return the next set of options, or a form to play.")
    @RequestMapping(value = Constants.URL_MENU_SELECT, method = RequestMethod.POST)
    public SessionBean selectMenu(@RequestBody MenuSelectBean menuSelectBean) throws Exception {
        MenuSession menuSession = getMenuSession(menuSelectBean.getSessionId());
        boolean redrawing = menuSession.handleInput(menuSelectBean.getSelection());
        menuRepo.save(menuSession.serialize());
        return getNextMenu(menuSession, redrawing);
    }

    private SessionBean getNextMenu(MenuSession menuSession, boolean redrawing) throws Exception {

        OptionsScreen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        if(redrawing){
            nextScreen = menuSession.getCurrentScreen();
        }else{
            nextScreen = menuSession.getNextScreen();
        }

        // No next menu screen? Start form entry!
        if (nextScreen == null){
            return menuSession.startFormEntry(sessionRepo);
        }
        else{
            MenuResponseBean menuResponseBean = new MenuResponseBean();
            menuResponseBean.setSessionId(menuSession.getSessionId());
            // We're looking at a module or form menu
            if(nextScreen instanceof MenuScreen){
                MenuScreen menuScreen = (MenuScreen) nextScreen;
                menuResponseBean.setMenuType(Constants.MENU_MODULE);
                menuResponseBean.setOptions(getMenuRows(menuScreen));
            }
            // We're looking at a case list or detail screen (probably)
            else if (nextScreen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen) nextScreen;
                menuResponseBean.setMenuType(Constants.MENU_ENTITY);
                menuResponseBean.setOptions(getMenuRows(entityScreen.getCurrentScreen()));
            }
            return menuResponseBean;
        }
    }

    private void updateSession(FormSession formEntrySession, SerializableFormSession serialSession) throws IOException {
        serialSession.setFormXml(formEntrySession.getFormXml());
        serialSession.setInstanceXml(formEntrySession.getInstanceXml());
        serialSession.setSequenceId(formEntrySession.getSequenceId() + 1);
        sessionRepo.save(serialSession);
    }

    private MenuSession getMenuSession(String sessionId) throws Exception {
        return new MenuSession(menuRepo.find(sessionId), restoreService, installService);
    }

    private HashMap<Integer, String> getMenuRows(OptionsScreen nextScreen){
        String[] rows = nextScreen.getOptions();
        HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
        for(int i=0; i <rows.length; i++){
            optionsStrings.put(i, rows[i]);
        }
        return optionsStrings;
    }

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("message", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }



}