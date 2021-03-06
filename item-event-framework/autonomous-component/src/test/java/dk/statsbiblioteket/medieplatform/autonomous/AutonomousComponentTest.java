package dk.statsbiblioteket.medieplatform.autonomous;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.test.TestingServer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AutonomousComponentTest {
    private static final String BATCHID = "40005";
    private static final long DEFAULT_TIMEOUT = 3600000;
    private static final int ROUNDTRIPNUMBER = 1;
    TestingServer testingServer;
    AutonomousComponent<Item> autonoumous;
    private TestingComponent component;
    private CuratorFramework lockClient;

    @BeforeMethod
    public void setUp() throws Exception {
        testingServer = new TestingServer();
        component = new TestingComponent(null);

        Item testBatch = new Item(BATCHID);
        Event testEvent = new Event();
        testEvent.setEventID("Data_Received");
        testEvent.setSuccess(true);
        testEvent.setDate(new Date());
        testEvent.setDetails("");

        testBatch.setEventList(new ArrayList<>(Arrays.asList(testEvent)));

        component.setItems(new ArrayList<>(Arrays.asList(testBatch)));

        lockClient = CuratorFrameworkFactory.newClient(
                testingServer.getConnectString(), new ExponentialBackoffRetry(1000, 3));

            lockClient.start();
            autonoumous = new AutonomousComponent<>(component,
                    lockClient,
                    1,
                                                           1,
                    Arrays.asList("Data_Received"), null, null, null,
                    DEFAULT_TIMEOUT,
                    DEFAULT_TIMEOUT,
                    DEFAULT_TIMEOUT,
                                                           100,
                    component.getEventTrigger(),
                    component.getEventStorer());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        lockClient.close();
    }

    /**
     * This test performs the following tasks 1. request a specific batch from the event client, to make sure the thing
     * is there and in the right state 2. starts the autonomous component, with parameters indicating that it should
     * poll for batches in the state checked above. 3. Request the batch afterwards, to check that the new state have
     * been added from the work in 2.
     *
     * @throws Exception
     */
    @Test
    public void testPollAndWork() throws Exception {

        Item batch = component.getItem(BATCHID);
        List<Event> events = batch.getEventList();
        boolean testEventFound = false;
        for (Event event : events) {
            if (event.getEventID().equals("Data_Archived")) {
                testEventFound = true;
            }
        }
        Assert.assertFalse(testEventFound,"Found test event before test, invalid test");

        autonoumous.call();

        Item batchAfter = component.getItem(BATCHID);
        List<Event> eventsAfter = batchAfter.getEventList();

        for (Event event : eventsAfter) {
            if (event.getEventID().equals("Data_Archived")) {
                testEventFound = true;
            }
        }
        Assert.assertTrue(testEventFound,"Test event not found after test");

    }
}
