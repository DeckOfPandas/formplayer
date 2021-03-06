package tests;

import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import org.commcare.cases.model.Case;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import sqlitedb.UserDB;
import utils.FileUtils;
import utils.TestContext;

import java.util.Hashtable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimTests extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    public void testQueryScreen() throws Exception {

        UserSqlSandbox sandbox = new UserSqlSandbox(new UserDB("caseclaimdomain", "caseclaimusername", null));
        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        configureQueryMock();
        configureSyncMock();
        Hashtable<String, String> queryDictionary = new Hashtable<>();
        queryDictionary.put("name", "Burt");
        EntityListResponse responseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryDictionary,
                EntityListResponse.class);
        assert responseBean.getEntities().length == 1;
        assert responseBean.getEntities()[0].getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 20;

        // When we sync afterwards, include new case and case-claim 
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml();

        CommandListResponseBean commandResponse = sessionNavigateWithQuery(new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaim",
                queryDictionary,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 2;
        assert commandResponse.getSelections().length == 2;
        assert commandResponse.getSelections()[1].equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 22;
    }

    @Test
    public void testAlreadyOwnCase() throws Exception {

        UserSqlSandbox sandbox = new UserSqlSandbox(new UserDB("caseclaimdomain", "caseclaimusername", null));
        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();
        Hashtable<String, String> queryDictionary = new Hashtable<>();
        queryDictionary.put("name", "Burt");

        configureQueryMockOwned();
        configureSyncMock();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml();

        CommandListResponseBean response = sessionNavigateWithQuery(new String[]{"1", "action 1", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
                "caseclaim",
                queryDictionary,
                CommandListResponseBean.class);
        assert response.getSelections().length == 2;
    }

    private void configureSyncMock() {
        when(syncRequester.makeSyncRequest(anyString(), anyString(), any(HttpHeaders.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
    }

    private void configureQueryMock() {
        when(queryRequester.makeQueryRequest(anyString(), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
    }

    private void configureQueryMockOwned() {
        when(queryRequester.makeQueryRequest(anyString(), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response_owned.xml"));
    }
}
