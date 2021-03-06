package dk.statsbiblioteket.medieplatform.autonomous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/** This is a sample component to serve as a guide to developers */
public class SampleComponent {

    private static Logger log = LoggerFactory.getLogger(SampleComponent.class);

    /**
     * The class must have a main method, so it can be started as a command line tool
     *
     * @param args the arguments.
     *
     * @throws Exception
     * @see AutonomousComponentUtils#parseArgs(String[])
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting with args {}", new Object[]{args});

        //Parse the args to a properties construct
        Properties properties = AutonomousComponentUtils.parseArgs(args);

        //make a new runnable component from the properties
        RunnableComponent<Batch> component = new SampleRunnableComponent(properties);

        CallResult<Batch> result = NewspaperBatchAutonomousComponentUtils.startAutonomousComponent(properties,
                                                                                             component
                                                                                             );
        System.out.print(result);
        System.exit(result.containsFailures());
    }
}
