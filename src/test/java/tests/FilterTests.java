package tests;

import auth.HqAuth;
import beans.CaseFilterResponseBean;
import beans.SyncDbRequestBean;
import beans.SyncDbResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import services.RestoreService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FilterTests extends BaseTestClass {

    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testRestoreFilter() throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");

        MvcResult result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        CaseFilterResponseBean caseFilterResponseBean0 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        String[] caseArray0 = caseFilterResponseBean0.getCases();
        assert(caseArray0.length == 3);
        assert(caseArray0[0].equals("2aa41fcf4d8a464b82b171a39959ccec"));

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_2.json");
        result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        CaseFilterResponseBean caseFilterResponseBean1 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        String[] caseArray1 = caseFilterResponseBean1.getCases();
        assert(caseArray1.length == 9);

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_3.json");
        result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        CaseFilterResponseBean caseFilterResponseBean2 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        String[] caseArray2 = caseFilterResponseBean2.getCases();
        assert(caseArray2.length == 1);
        assert(caseArray2[0].equals("e7ed3658d7394415a4bba5edc7055f1d"));

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_4.json");
        result = this.mockMvc.perform(
                post("/filter_cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        CaseFilterResponseBean caseFilterResponseBean3 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        String[] caseArray3 = caseFilterResponseBean3.getCases();
    }

    @Test
    public void testSyncDb() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);

        assert(!SqlSandboxUtils.databaseFolderExists(UserSqlSandbox.DEFAULT_DATBASE_PATH));

        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");

        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);

        MvcResult result = this.mockMvc.perform(
                post("/sync_db")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(syncDbRequestBean)))
                .andExpect(status().isOk())
                .andReturn();

        SyncDbResponseBean syncDbResponseBean = mapper.readValue(result.getResponse().getContentAsString(),
                SyncDbResponseBean.class);

        assert(syncDbResponseBean.getStatus().equals("success"));

        assert(SqlSandboxUtils.databaseFolderExists(UserSqlSandbox.DEFAULT_DATBASE_PATH));

        UserSqlSandbox sandbox = SqlSandboxUtils.getStaticStorage(syncDbRequestBean.getUsername());

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert(15 == caseStorage.getNumRecords());
    }
}