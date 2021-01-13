package azkaban.container;

import azkaban.AzkabanCommonModule;
import azkaban.common.ExecJettyServerModule;
import azkaban.database.AzkabanDatabaseSetup;
import azkaban.database.AzkabanDatabaseUpdater;
import azkaban.db.DatabaseOperator;
import azkaban.execapp.AzkabanExecutorServerTest;
import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutorLoader;
import azkaban.project.ProjectFileHandler;
import azkaban.project.ProjectLoader;
import azkaban.spi.AzkabanEventReporter;
import azkaban.test.Utils;
import azkaban.utils.Props;
import azkaban.utils.TestUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.servlet.Context;

import static azkaban.Constants.ConfigurationKeys.*;
import static azkaban.ServiceProvider.*;
import static azkaban.container.FlowContainer.*;
import static azkaban.utils.TestUtils.*;
import static java.util.Objects.*;
import static org.mockito.Mockito.*;


public class FlowContainerTest {

  public static final String AZKABAN_LOCAL_TEST_STORAGE = "AZKABAN_LOCAL_TEST_STORAGE";
  public static final String AZKABAN_DB_SQL_PATH = "azkaban-db/src/main/sql";

  public static final Props props = new Props();
  private static DatabaseOperator dbOperator;

  private ExecutorLoader executorLoader;
  private ProjectLoader projectLoader;
  private AzkabanEventReporter eventReporter;
  private ExecJettyServerModule jettyServer;
  private AzkabanCommonModule commonModule;

  private static Path azkabanRoot;

  @BeforeClass
  public static void setUp() throws Exception {
    props.put("database.type", "h2");
    props.put("h2.path", "./h2");
    props.put(AZKABAN_EVENT_REPORTING_ENABLED, 0);
    dbOperator = Utils.initTestDB();
    SERVICE_PROVIDER.unsetInjector();
    final URL resource = AzkabanExecutorServerTest.class.getClassLoader().getResource("test.file");
    final String dummyResourcePath = requireNonNull(resource).getPath();
    final Path resources = Paths.get(dummyResourcePath).getParent();
    azkabanRoot = resources.getParent().getParent().getParent().getParent();
    FlowContainer.setInjector(props);
  }

  @AfterClass
  public static void destroyDB() {
    try {
      dbOperator.update("DROP ALL OBJECTS");
      dbOperator.update("SHUTDOWN");
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setup() throws Exception {
    this.executorLoader = mock(ExecutorLoader.class);
    this.projectLoader = mock(ProjectLoader.class);
  }

  /**
   * FIXME: This test is incomplete for now as there is an expected merge conflict which will
   * change interface with submitFlow. Once new code is merged in FlowContainer,
   * this test will need to be completed by invoking a flow.
   * @throws Exception
   */
  @Test
  public void testExecSimple() throws Exception {
    final ExecutableFlow execFlow = createTestExecutableFlowFromYaml("basicflowyamltest", "basic_flow");
    execFlow.setExecutionId(1);
    final ProjectFileHandler handler = new ProjectFileHandler(1, 1, 1, "testUser", "zip", "test.zip",
        1, null, null, null, "111.111.111.111");
    when(this.projectLoader.fetchProjectMetaData(anyInt(), anyInt())).thenReturn(handler);

    final FlowContainer flowContainer = SERVICE_PROVIDER.getInstance(FlowContainer.class);
    flowContainer.start();
    FlowContainer.launchCtrlMsgListener(flowContainer);
  }
}