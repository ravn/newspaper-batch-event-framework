package dk.statsbiblioteket.medieplatform.autonomous.iterator.fedora3;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.AbstractTests;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.TreeIterator;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class IteratorForFedora3Test extends AbstractTests {

    private TreeIterator iterator;

    @Override
    public TreeIterator getIterator() throws URISyntaxException, IOException {
        if (iterator == null) {


            Properties properties = new Properties();
            properties.load(new FileReader(new File(System.getProperty("integration.test.newspaper.properties"))));
            System.out.println(properties.getProperty("fedora.admin.username"));
            Client client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(properties.getProperty("fedora.admin.username"), properties
                    .getProperty("fedora.admin.password")));

            String pid;
            try {
                EnhancedFedoraImpl fedora = new EnhancedFedoraImpl(new Credentials(properties.getProperty("fedora.admin.username"),
                                                                                   properties.getProperty("fedora.admin.password")),
                                                                   properties.getProperty("fedora.server")
                                                                             .replaceFirst("/(objects)?/?$", ""),
                                                                   null,
                                                                   null);
                pid = fedora.findObjectFromDCIdentifier("path:B400022028246-RT1").get(0);
            } catch (PIDGeneratorException | BackendMethodFailedException | JAXBException | BackendInvalidCredsException e) {
                throw new RuntimeException(e);
            }

            // The uuid below is for a test object ingested by someone and may never be deleted?!

            iterator = new IteratorForFedora3(pid, "B400022028246-RT1",
                                              client, properties.getProperty("fedora.server"), new TestFilter());
        }
        return iterator;
    }

    static class TestFilter implements ContentModelFilter {

        public boolean isAttributeDatastream(String dsid, List<String> types) {
            List<String> names = Arrays.asList("MODS", "FILM", "EDITION", "ALTO", "MIX");
            return names.contains(dsid);
        }

        public boolean isChildRel(String predicate, List<String> types) {
            if (predicate.contains("#hasPart")){
                return true;
            }
            return false;
        }
    }

    @Test(groups = "integrationTest", enabled = true)
    public void testIterator() throws Exception {
        super.testIterator(true,false);
    }

    @Test(groups = "integrationTest", enabled = true)
    public void testIteratorWithSkipping() throws Exception {
        super.testIteratorWithSkipping(false,false);
    }
}
