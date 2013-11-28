package dk.statsbiblioteket.medieplatform.autonomous;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.ExponentialBackoffRetry;

import dk.statsbibliokeket.newspaper.batcheventFramework.BatchEventClient;
import dk.statsbibliokeket.newspaper.batcheventFramework.BatchEventClientImpl;

public class AutonomousComponentUtils {
    private static Logger log = LoggerFactory.getLogger(AutonomousComponentUtils.class);

    /**
     * Create an autonomous component from a runnable component and start it. Stuff is configured from the included
     * properties
     *
     * @param properties the properties to use
     * @param component  the runnable component to invoke
     *
     * @return the result of the invocation. A map from batch Full IDs to results. If the execution failed, a message
     *         will be printed to std.err and the result map will be empty
     *
     *         lockserver: string: url to the zookeeper server
     *         summa: string, url to the summa webservice
     *         domsUrl: string, url to the fedora doms instance
     *         domsUser: string; username when writing events to the doms batch objects
     *         domsPass: string: password when writing events to the doms batch objects
     *         pidGenerator: String: url to the pidgenerator service
     *         maxThreads: Integer: The number of batches to work on concurrently. Default 1
     *         maxRuntimeForWorkers: Long: The number of milliseconds to wait before forcebly killing worker threads. Default one hour
     *         pastSuccessfulEvents: String list, comma separated: The list of event IDs that the batch must have experienced successfully in order to be eligble to be worked on by this component
     *         pastFailedEvents: String list, comma separated: The list of event IDs that the batch must have experienced without success in order to be eligble to be worked on by this component
     *         futureEvents: String list, comma separated: The list of event IDs that the batch must NOT have experienced in order to be eligble to be worked on by this component
     *
     *
     */
    public static CallResult startAutonomousComponent(Properties properties,
                                                                RunnableComponent component) {
        //Make a client for the lock framework, and start it
        CuratorFramework lockClient = CuratorFrameworkFactory.newClient(
                properties.getProperty(ConfigConstants.AUTONOMOUS_LOCKSERVER_URL),
                new ExponentialBackoffRetry(1000, 3));
        lockClient.start();

        //Make a batch event client to query and store events
        BatchEventClient eventClient = new BatchEventClientImpl(
                properties.getProperty(ConfigConstants.AUTONOMOUS_SBOI_URL),
                properties.getProperty(ConfigConstants.DOMS_URL),
                properties.getProperty(ConfigConstants.DOMS_USERNAME),
                properties.getProperty(ConfigConstants.DOMS_PASSWORD),
                properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));


        //This is the number of batches that will be worked on in parallel per invocation
        int simultaneousProcesses = Integer.parseInt(properties.getProperty(ConfigConstants.AUTONOMOUS_MAXTHREADS, "1"));
        //This is the timeout when attempting to lock SBOI
        long timeoutWaitingToLockSBOI = 5000l;
        //This is the timeout when attempting to lock a batch before working on it
        long timeoutWaitingToLockBatch = 2000l;
        //After this time, the worker thread will be terminated, even if not complete
        long maxRunTimeForWorker = Long.parseLong(properties.getProperty(
                ConfigConstants.AUTONOMOUS_MAX_RUNTIME, 60 * 60 * 1000l+""));



        //Use all the above to make the autonomous component
        AutonomousComponent autonoumous = new AutonomousComponent(
                component,
                lockClient,
                eventClient,
                simultaneousProcesses,
                toEvents(properties.getProperty(ConfigConstants.AUTONOMOUS_PAST_SUCCESSFUL_EVENTS)),
                toEvents(properties.getProperty(ConfigConstants.AUTONOMOUS_PAST_FAILED_EVENTS)),
                toEvents(properties.getProperty(ConfigConstants.AUTONOMOUS_FUTURE_EVENTS)),
                timeoutWaitingToLockSBOI,
                timeoutWaitingToLockBatch,
                maxRunTimeForWorker);


        try {//Start the component
            //This call will return when the work is done
            return autonoumous.call();
        } catch (CouldNotGetLockException e) {
            System.err.println(
                    "Could not lock SBOI. Is this component running already? SBOI is locked to this component's name");
            log.error("Could not get lock on SBOI", e);
            return new CallResult("Could not get lock on SBOI");
        } catch (LockingException e) {
            System.err.println(
                    "Failed to communicate with the locking server. Check that the locking server is running and "
                    + "network connectivity");
            log.error("Failed to communicate with zookeeper", e);
            return new CallResult("Failed to communicate with zookeeper");
        } catch (CommunicationException e) {
            System.err.println("Failed to communicate with the backend systems. The work done is lost.");
            log.error("Commmunication exception when invoking backend services", e);
            return new CallResult("Commmunication exception when invoking backend services");
        } finally {
            lockClient.close();
        }
    }

    /**
     * Convert the events list from the properties file. It consist of a comma-separated list of event ids
     *
     * @param events the event list
     *
     * @return as a list
     */
    private static List<String> toEvents(String events) {
        String[] eventSplits = events.split(",");
        List<String> result = new ArrayList<>();
        for (String eventSplit : eventSplits) {
            if (!eventSplit.trim().isEmpty()) {
                result.add(eventSplit.trim());
            }
        }
        return result;
    }

    /**
     * Sample method to parse properties. This is probably not the best way to do this
     * It makes a new properties, with the system defaults. It then scan the args for a the string "-c". If found
     * it expects the next arg to be a path to a properties file.
     *
     * @param args the command line args
     *
     * @return as a properties
     * @throws java.io.IOException if the properties file could not be read
     */
    public static Properties parseArgs(String[] args)
            throws
            IOException {
        Properties properties = new Properties(System.getProperties());
        for (int i = 0;
             i < args.length;
             i++) {
            String arg = args[i];
            if (arg.equals("-c")) {
                String configFile = args[i + 1];
                properties.load(new FileInputStream(configFile));
            }
        }
        return properties;
    }
}
