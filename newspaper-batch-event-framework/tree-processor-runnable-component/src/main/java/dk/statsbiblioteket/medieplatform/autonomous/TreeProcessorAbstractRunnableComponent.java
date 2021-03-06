package dk.statsbiblioteket.medieplatform.autonomous;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.sbutil.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.TreeIterator;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.fedora3.ConfigurableFilter;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.fedora3.IteratorForFedora3;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.filesystem.transforming.TransformingIteratorForFileSystems;
import dk.statsbiblioteket.util.Streams;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * An abstract runnable component that uses SBOI for event triggers and DOMS for event storage,
 * and provides access to a tree iterator.
 */
@SuppressWarnings("deprecation")//Credentials
public abstract class TreeProcessorAbstractRunnableComponent extends AbstractRunnableComponent<Batch> {
    private static final String BATCH_STRUCTURE = "BATCHSTRUCTURE";
    private EnhancedFedora fedora;
    private Logger log = LoggerFactory.getLogger(getClass());

    protected TreeProcessorAbstractRunnableComponent(Properties properties) {
        super(properties);
    }

    /**
     * Create a tree iterator for the given batch. It will use the properties construct to get necessary properties
     * 1. useFileSystem: boolean: Determines if the batch should be read from the filesystem (in this case 2-5 are
     * used)
     * 2. scratch: path: The folder where the batch resides
     * 3. groupingChar: Char. The character that separates the prefix and the postfix. Default "."
     * 4. dataFilePattern: The regular expression pattern to identify datafiles. Default ".*\\.jp2$"
     * 5. checksumPostFix: The postfix to append to filenames to get their checksum files. Default ".md5"
     * 6. fedora.admin.username: The username for communicating with fedora
     * 7. fedora.admin.password: The password used for communicating with fedora
     * 8. fedora.server: The fedora server used
     * 9. fedora.iterator.attributenames Datastream names in fedora used for attributes
     * 10. fedora.iterator.predicatenames Predicates of relations in fedora used for generating tree
     *
     * @param batch the batch
     *
     * @return a tree iterator
     */
    protected TreeIterator createIterator(Batch batch) {
        String dataFilePattern = getProperties().getProperty(ConfigConstants.ITERATOR_DATAFILEPATTERN, TransformingIteratorForFileSystems.DATA_FILE_PATTERN_JP2_VALUE);
        boolean useFileSystem = Boolean.parseBoolean(
                getProperties().getProperty(ConfigConstants.ITERATOR_USE_FILESYSTEM, "true"));

        if (useFileSystem) {
            File scratchDir = new File(getProperties().getProperty(ConfigConstants.ITERATOR_FILESYSTEM_BATCHES_FOLDER));
            File batchDir = new File(scratchDir, batch.getFullID());
            String groupingChar = Pattern
                    .quote(getProperties().getProperty(ConfigConstants.ITERATOR_FILESYSTEM_GROUPINGCHAR, "."));

            String checksumPostFix = getProperties().getProperty(ConfigConstants.ITERATOR_FILESYSTEM_CHECKSUMPOSTFIX,
                                                                 TransformingIteratorForFileSystems.CHECKSUM_POSTFIX_DEFAULT_VALUE);
            String[] ignoredFiles = getProperties().getProperty(ConfigConstants.ITERATOR_FILESYSTEM_IGNOREDFILES,
                                                                TransformingIteratorForFileSystems.IGNORED_FILES_DEFAULT_VALUE)
                    .split(",");
            for (int i = 0; i < ignoredFiles.length; i++) {
                ignoredFiles[i] = ignoredFiles[i].trim();
            }
            return new TransformingIteratorForFileSystems(batchDir, groupingChar, dataFilePattern, checksumPostFix,
                                                          Arrays.asList(ignoredFiles));

        } else {
            Client client = Client.create();
            client.addFilter(
                    new HTTPBasicAuthFilter(
                            getProperties().getProperty(ConfigConstants.DOMS_USERNAME),
                            getProperties().getProperty(ConfigConstants.DOMS_PASSWORD)));

            String pid;
            try {
                EnhancedFedora fedora = getEnhancedFedora();
                pid = getRoundTripObject(batch, fedora);
            } catch (MalformedURLException | PIDGeneratorException | BackendMethodFailedException | JAXBException |
                    BackendInvalidCredsException e) {
                log.error("Unable to initialise iterator", e);
                throw new InitialisationException("Unable to initialise iterator", e);
            }

            return new IteratorForFedora3(
                    pid, client, getProperties().getProperty(ConfigConstants.DOMS_URL), new ConfigurableFilter(
                    Arrays.asList(getProperties().getProperty(ConfigConstants.ITERATOR_DOMS_ATTRIBUTENAMES).split(",")), Arrays.asList(
                    getProperties().getProperty(ConfigConstants.ITERATOR_DOMS_PREDICATENAMES).split(","))), dataFilePattern);
        }
    }

    /**
     * Retrieve the batch structure from DOMS or from the file system.
     * If the property "batchStructure.useFileSystem" is true (default), retrieve the structure from the
     * "batchStructure.storageDir"
     * otherwise retrieve it from the datastream named MANIFEST on the round trip object
     *
     * @param batch the batch in question
     *
     * @return the input stream to the batch structure
     * @throws IOException if an inputstream could not be opened
     */
    public InputStream retrieveBatchStructure(Batch batch) throws IOException {
        boolean useFileSystem = Boolean.parseBoolean(
                getProperties().getProperty(ConfigConstants.ITERATOR_USE_FILESYSTEM, "true"));
        if (useFileSystem) {
            File batchStructureFile = getBatchStructureFile(batch);
            return new FileInputStream(batchStructureFile);
        } else {
            String pid;
            try {
                EnhancedFedora fedora = getEnhancedFedora();
                pid = getRoundTripObject(batch, fedora);
                String batchStructure = fedora.getXMLDatastreamContents(pid, BATCH_STRUCTURE);
                return new ByteArrayInputStream(batchStructure.getBytes("UTF-8"));

            } catch (BackendInvalidResourceException | MalformedURLException | PIDGeneratorException |
                    BackendMethodFailedException | JAXBException |
                    BackendInvalidCredsException e) {
                log.error("Unable to retrieve batch structure", e);
                throw new InitialisationException("Unable to retrieve batch structure", e);
            }
        }
    }

    /**
     * Utility method to get the batch structure file.
     *
     * @param batch the batch in question
     *
     * @return a file object denoting the path to the structure file (which might not exist)
     */
    private File getBatchStructureFile(Batch batch) {
        File scratchDir = new File(getProperties().getProperty(ConfigConstants.AUTONOMOUS_BATCH_STRUCTURE_STORAGE_DIR));
        return new File(scratchDir, batch.getFullID() + ".batchStructure.xml");
    }

    /**
     * Store the batch structure, either in DOMS or on the filesystem.
     * If the property "batchStructure.useFileSystem" is true (default), store the structure in the
     * "batchStructure.storageDir"
     * otherwise store it in the datastream named MANIFEST on the round trip object
     *
     * @param batch          the batch in question
     * @param batchStructure the batch structure as an UTF-8 inputstream
     *
     * @throws IOException if the storing failed
     */
    public void storeBatchStructure(Batch batch, InputStream batchStructure) throws IOException {
        boolean useFileSystem = Boolean.parseBoolean(
                getProperties().getProperty(ConfigConstants.ITERATOR_USE_FILESYSTEM, "true"));
        if (useFileSystem) {
            File batchStructureFile = getBatchStructureFile(batch);
            FileOutputStream output = new FileOutputStream(batchStructureFile);
            Streams.pipe(batchStructure, output);
        } else {
            String pid;
            try {
                EnhancedFedora fedora = getEnhancedFedora();
                pid = getRoundTripObject(batch, fedora);
                fedora.modifyDatastreamByValue(
                        pid, BATCH_STRUCTURE, null,null,toString(batchStructure).getBytes(),null,"text/xml", "Updating batch structure",null);
            } catch (BackendInvalidResourceException | MalformedURLException | PIDGeneratorException |
                    BackendMethodFailedException | JAXBException |
                    BackendInvalidCredsException e) {
                log.error("Unable to store batch structure", e);
                throw new InitialisationException("Unable to retrieve batch structure", e);
            }
        }

    }

    /**
     * Utility method to get the round trip object for a given batch
     *
     * @param batch  the batch in question
     * @param fedora the enhanced fedora interface
     *
     * @return the found pid or null
     * @throws BackendInvalidCredsException if the credentials are insufficient
     * @throws BackendMethodFailedException if something failed in the backend
     */
    private String getRoundTripObject(Batch batch, EnhancedFedora fedora) throws
                                                                          BackendInvalidCredsException,
                                                                          BackendMethodFailedException {

        List<String> pids = fedora.findObjectFromDCIdentifier("path:" + batch.getFullID());
        if (pids.isEmpty()) {
            return null;
        } else {
            if (pids.size() > 1) {
                log.warn("Apparently found more than one round trip for this round trip '{}'", batch.getFullID());
            }
            return pids.get(0);
        }

    }

    /**
     * Utility method to initialise an enhanced fedora object
     *
     * @return the enhanced fedora object
     * @throws MalformedURLException if the URL in "fedora.server" is invalid
     * @throws PIDGeneratorException if the pid generator webservice choked again. Should not be possible
     * @throws JAXBException         if jaxb fails to understand the wsdl
     */
    @SuppressWarnings("deprecation")//Credentials
    private synchronized EnhancedFedora getEnhancedFedora() throws
                                                            MalformedURLException,
                                                            PIDGeneratorException,
                                                            JAXBException {
        int fedoraRetries = Integer.parseInt(getProperties().getProperty(ConfigConstants.FEDORA_RETRIES, "1"));
        int fedoraDelayBetweenRetries = Integer.parseInt(getProperties().getProperty(ConfigConstants.FEDORA_DELAY_BETWEEN_RETRIES, "100"));
        if (fedora == null) {
            fedora = new EnhancedFedoraImpl(
                    new Credentials(
                            getProperties().getProperty(ConfigConstants.DOMS_USERNAME),
                            getProperties().getProperty(ConfigConstants.DOMS_PASSWORD)),
                    getProperties().getProperty(ConfigConstants.DOMS_URL),
                    null,
                    null,
                    fedoraRetries, fedoraDelayBetweenRetries);
        }
        return fedora;
    }

    /**
     * Utility method to read an inputstream to a string. Why is this not in SBUtils already?
     *
     * @param stream the stream to read
     *
     * @return the stream as a string
     * @throws IOException if the stream could not be read
     */
    private String toString(InputStream stream) throws IOException {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        Streams.pipe(stream, temp);
        return new String(temp.toByteArray(), "UTF-8");
    }
}
