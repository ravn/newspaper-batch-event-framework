package dk.statsbiblioteket.medieplatform.autonomous;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.util.xml.DOM;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DomsEventStorageIntegrationTest {

    @Test(groups = {"externalTest"})
    public void testAddEventToBatch1() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        DomsEventStorageFactory factory = new DomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        DomsEventStorage domsEventStorage = factory.createDomsEventStorage();

        String batchId = getRandomBatchId();
        Integer roundTripNumber = 1;
        Date timestamp = new Date(0);
        String eventID = "Data_Received";
        String details = "Details here";

        Credentials creds = new Credentials(
                props.getProperty(ConfigConstants.DOMS_USERNAME), props.getProperty(ConfigConstants.DOMS_PASSWORD));
        EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(
                creds,
                props.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                null);
        NewspaperIDFormatter formatter = new NewspaperIDFormatter();


        try {
            domsEventStorage.addEventToBatch(batchId, roundTripNumber, "agent", timestamp, details, eventID, true);

            Batch batch = domsEventStorage.getBatch(batchId, roundTripNumber);
            Assert.assertEquals(batch.getBatchID(), batchId);
            Assert.assertEquals(batch.getRoundTripNumber(), roundTripNumber);

            boolean found = false;
            for (Event event : batch.getEventList()) {
                if (event.getEventID().equals(eventID)) {
                    found = true;
                    Assert.assertEquals(event.getDate(), timestamp);
                    Assert.assertEquals(event.getDetails(), details);
                    Assert.assertEquals(event.isSuccess(), true);
                }
            }
            Assert.assertTrue(found);


            Integer newRoundTripNumber = roundTripNumber + 5;
            domsEventStorage.addEventToBatch(batchId, newRoundTripNumber, "agent", timestamp, details, eventID, true);

            batch = domsEventStorage.getBatch(batchId, newRoundTripNumber);
            Assert.assertEquals(batch.getBatchID(), batchId);
            Assert.assertEquals(batch.getRoundTripNumber(), newRoundTripNumber);

            found = false;
            for (Event event : batch.getEventList()) {
                if (event.getEventID().equals(eventID)) {
                    found = true;
                    Assert.assertEquals(event.getDate(), timestamp);
                    Assert.assertEquals(event.getDetails(), details);
                    Assert.assertEquals(event.isSuccess(), true);
                }
            }
            Assert.assertTrue(found);

        } finally {
            String pid = fedora.findObjectFromDCIdentifier(formatter.formatBatchID(batchId)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }
            pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }

        }
    }

    /**
     * Create a round-trip object with an EVENTS datastream. Create a backup of the datastream and check
     * that it is actually identical to the original.
     *
     * @throws Exception
     */
    @Test(groups = {"externalTest"})
    public void testBackupEventsForBatch() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        DomsEventStorageFactory factory = new DomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        EventStorer eventStorer = factory.createDomsEventStorage();

        String batchId = getRandomBatchId();
        Integer roundTripNumber = 1;
        Date first = new Date(0);

        Date timestamp = new Date(100);
        String eventID = "Data_Received";
        String details = "Details here " + UUID.randomUUID().toString();

        Credentials creds = new Credentials(
                props.getProperty(ConfigConstants.DOMS_USERNAME), props.getProperty(ConfigConstants.DOMS_PASSWORD));
        EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(
                creds,
                props.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                null);
        NewspaperIDFormatter formatter = new NewspaperIDFormatter();


        try {
            List<String> pidsBefore = fedora.findObjectFromDCIdentifier(
                    formatter.formatFullID(batchId, roundTripNumber));
            for (String pid : pidsBefore) {
                fedora.deleteObject(pid, "cleaning up before test");

            }


            eventStorer.addEventToBatch(
                    batchId, roundTripNumber, "agent", first, "initial event", "InitialEvent", true);
            Thread.sleep(1000);
            long beforeUpdate = System.currentTimeMillis();

            Thread.sleep(1000);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", timestamp, details, eventID, true);
            Thread.sleep(1000);

            long afterUpdate = System.currentTimeMillis();

            Thread.sleep(1000);
            eventStorer.triggerWorkflowRestartFromFirstFailure(batchId, roundTripNumber, 3, 10000, eventID);
            Thread.sleep(1000);

            long afterReset = System.currentTimeMillis();
            /*
            String backupEvents = ((DomsEventStorage) eventStorer).backupEventsForBatch(batchId, roundTripNumber);

            assertTrue(
                    backupEvents.matches("EVENTS_[0-9]{1,}"),
                    "Failed to create backup events datastream. Unexpected name '" + backupEvents + "'");
*/
            String pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            String originalEvents = fedora.getXMLDatastreamContents(pid, "EVENTS", beforeUpdate);
            String updatedEvents = fedora.getXMLDatastreamContents(pid, "EVENTS", afterUpdate);
            String revertedEvents = fedora.getXMLDatastreamContents(pid, "EVENTS", afterReset);
            String finalEvents = fedora.getXMLDatastreamContents(pid, "EVENTS");
            assertFalse(originalEvents.contains(details), pretty(originalEvents));
            assertFalse(revertedEvents.contains(details), pretty(revertedEvents));
            assertFalse(finalEvents.contains(details), pretty(finalEvents));
            assertTrue(updatedEvents.contains(details), pretty(updatedEvents));

            assertEquals(pretty(revertedEvents), pretty(originalEvents));
            assertEquals(pretty(finalEvents), pretty(originalEvents));
            assertEquals(pretty(revertedEvents), pretty(finalEvents));

        } finally {

            List<String> pids = fedora.findObjectFromDCIdentifier(formatter.formatBatchID(batchId));
            for (String pid : pids) {
                fedora.deleteObject(pid, "cleaning up after test");
            }
            pids = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber));
            for (String pid : pids) {
                fedora.deleteObject(pid, "cleaning up after test");
            }

        }

    }

    /**
     * Create a round-trip object with an EVENTS datastream. Call the method to trigger a restart and check that
     * all events from the first failure are removed.
     *
     * @throws Exception
     */
    @Test(groups = {"externalTest"})
    public void testTriggerWorkflowRestart() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        DomsEventStorageFactory factory = new DomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        EventStorer eventStorer = factory.createDomsEventStorage();

        String batchId = getRandomBatchId();
        Integer roundTripNumber = 1;
        String details = "Details here";

        Credentials creds = new Credentials(
                props.getProperty(ConfigConstants.DOMS_USERNAME), props.getProperty(ConfigConstants.DOMS_PASSWORD));
        EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(
                creds,
                props.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                null);
        NewspaperIDFormatter formatter = new NewspaperIDFormatter();

        try {
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(100), details, "e1", true);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(200), details, "e2", true);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(300), details, "e3", true);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(400), details, "e4", false);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(500), details, "e5", true);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(600), details, "e6", false);
            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(700), details, "e7", true);

            eventStorer.triggerWorkflowRestartFromFirstFailure(batchId, roundTripNumber, 10, 1000L);

            String pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            String events = fedora.getXMLDatastreamContents(pid, "EVENTS");
            assertTrue(events.contains("e1"));
            assertTrue(events.contains("e2"));
            assertTrue(events.contains("e3"));
            assertFalse(events.contains("e4"));
            assertFalse(events.contains("e5"));
            assertFalse(events.contains("e6"));
            assertFalse(events.contains("e7"));
        } finally {
            String pid = fedora.findObjectFromDCIdentifier(formatter.formatBatchID(batchId)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }
            pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }

        }

    }

    /**
     * In this test, we add a failure to the list with a timestamp earlier than all other events and then
     * trigger the restart. This should empty the event list. We also check that calling the trigger again does not
     * result in an error.
     *
     * @throws Exception
     */
    @Test(groups = {"externalTest"})
    public void testTriggerWorkflowRestartEmptyEventList() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(pathToProperties));

        DomsEventStorageFactory factory = new DomsEventStorageFactory();
        factory.setFedoraLocation(props.getProperty(ConfigConstants.DOMS_URL));
        factory.setUsername(props.getProperty(ConfigConstants.DOMS_USERNAME));
        factory.setPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
        factory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        EventStorer eventStorer = factory.createDomsEventStorage();

        String batchId = getRandomBatchId();
        Integer roundTripNumber = 1;
        Credentials creds = new Credentials(
                props.getProperty(ConfigConstants.DOMS_USERNAME), props.getProperty(ConfigConstants.DOMS_PASSWORD));
        EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(
                creds,
                props.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                null);
        NewspaperIDFormatter formatter = new NewspaperIDFormatter();

        try {
            String details = "Details here";

            eventStorer.addEventToBatch(batchId, roundTripNumber, "agent", new Date(-1000L), details, "e1", false);

            eventStorer.triggerWorkflowRestartFromFirstFailure(batchId, roundTripNumber, 10, 1000L);


            String pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            String events = fedora.getXMLDatastreamContents(pid, "EVENTS");
            assertFalse(events.contains("event"), events);
            eventStorer.triggerWorkflowRestartFromFirstFailure(batchId, roundTripNumber, 10, 1000L);
        } finally {
            String pid = fedora.findObjectFromDCIdentifier(formatter.formatBatchID(batchId)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }
            pid = fedora.findObjectFromDCIdentifier(formatter.formatFullID(batchId, roundTripNumber)).get(0);
            if (pid != null) {
                fedora.deleteObject(pid, "cleaning up after test");
            }

        }

    }

    private String getRandomBatchId() {
        return "4000220252" + Math.round(Math.random() * 100);
    }


    public static String pretty(String doc) throws IOException, TransformerException {
        Document dom = DOM.stringToDOM(doc, true);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter output = new StringWriter();
        transformer.transform(new DOMSource(dom), new StreamResult(output));
        return output.toString();
    }
}
