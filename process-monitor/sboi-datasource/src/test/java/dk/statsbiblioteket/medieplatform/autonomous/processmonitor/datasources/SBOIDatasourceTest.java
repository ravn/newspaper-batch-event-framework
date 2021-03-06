package dk.statsbiblioteket.medieplatform.autonomous.processmonitor.datasources;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.BatchItemFactory;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;
import dk.statsbiblioteket.util.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class SBOIDatasourceTest extends TCKTestSuite {

    private static final long MINUTES = 60 * 1000;
    private DataSource dataSource;

    @Override
    public synchronized DataSource getDataSource() {
        if (dataSource == null) {
            Properties props = new Properties();
            try {
                props.load(new FileReader(new File(System.getProperty("integration.test.newspaper.properties"))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            SBOIDatasourceConfiguration conf = new SBOIDatasourceConfiguration();
            conf.setSummaLocation(props.getProperty(ConfigConstants.AUTONOMOUS_SBOI_URL));
            conf.setDomsLocation(props.getProperty(ConfigConstants.DOMS_URL));
            conf.setDomsUser(props.getProperty(ConfigConstants.DOMS_USERNAME));
            conf.setDomsPassword(props.getProperty(ConfigConstants.DOMS_PASSWORD));
            conf.setDomsPidGenLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
            conf.setDomsRetries(props.getProperty(ConfigConstants.FEDORA_RETRIES, "3"));
            conf.setDomsDelayBetweenRetries(props.getProperty(ConfigConstants.FEDORA_DELAY_BETWEEN_RETRIES, "100"));
            dataSource = new SBOIDatasource(conf);

            DomsEventStorageFactory<Batch> domsEventStorageFactory = new DomsEventStorageFactory<>();
            domsEventStorageFactory.setFedoraLocation(conf.getDomsLocation());
            domsEventStorageFactory.setUsername(conf.getDomsUser());
            domsEventStorageFactory.setPassword(conf.getDomsPassword());
            domsEventStorageFactory.setPidGeneratorLocation(props.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
            domsEventStorageFactory.setItemFactory(new BatchItemFactory());
            DomsEventStorage<Batch> domsClient;
            try {
                domsClient = domsEventStorageFactory.createDomsEventStorage();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                Batch testBatch = domsClient.getItemFromFullID(Batch.formatFullID(getValidBatchID().getLeft(),
                                                                      getValidBatchID().getRight()));
            } catch (CommunicationException | NotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return dataSource;
    }

    @Override
    public Pair<String, Integer> getValidBatchID() {
        return new Pair<>("400022028241", 1);
    }

    @Override
    public Pair<String, Integer> getInvalidBatchID() {
        return new Pair<>("300022028241", 1);
    }

    @Override
    public String getValidAndSucessfullEventIDForValidBatch() {
        return "Data_Received";
    }
}
