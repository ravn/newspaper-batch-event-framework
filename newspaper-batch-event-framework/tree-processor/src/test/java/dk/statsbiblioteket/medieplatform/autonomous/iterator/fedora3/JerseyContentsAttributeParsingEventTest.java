package dk.statsbiblioteket.medieplatform.autonomous.iterator.fedora3;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.AttributeParsingEvent;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class JerseyContentsAttributeParsingEventTest {

    private static final String PID = "contentTest:1";
    private WebResource objectResource;

    @BeforeMethod(groups = {"externalTest"})
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileReader(new File(System.getProperty("integration.test.newspaper.properties"))));
        System.out.println(properties.getProperty(ConfigConstants.DOMS_USERNAME));
        Client client = Client.create();
        client.addFilter(
                new HTTPBasicAuthFilter(
                        properties.getProperty(ConfigConstants.DOMS_USERNAME),
                        properties.getProperty(ConfigConstants.DOMS_PASSWORD)));

        WebResource resource = client.resource(properties.getProperty(ConfigConstants.DOMS_URL));
        objectResource = resource.path("/objects/").path(PID);
        try {
            objectResource.delete();

        } catch (Exception e) {
            try {
                objectResource.queryParam("state", "I").put();
                objectResource.delete();
            } catch (Exception e2) {
                //ignore
            }
        }

        objectResource.queryParam("state", "I").post();
        objectResource.queryParam("state", "I").put();


    }

    @AfterMethod(groups = {"externalTest"})
    public void tearDown() throws Exception {
        objectResource.delete();

    }

    @Test(groups = {"externalTest"})
    public void testGetChecksum() throws Exception {
        objectResource.path("/datastreams/")
                      .path(JerseyContentsAttributeParsingEvent.CONTENTS)
                      .queryParam("dsLocation", "http://statsbiblioteket.dk")
                      .queryParam(
                              "controlGroup", "R")
                      .post();
        objectResource.path("/relationships/new")
                      .queryParam(
                              "subject",
                              "info:fedora/" + "contentTest:1" + "/" + JerseyContentsAttributeParsingEvent.CONTENTS)
                      .queryParam(
                              "predicate", URLEncoder.encode(JerseyContentsAttributeParsingEvent.HAS_CHECKSUM, "UTF-8"))
                      .queryParam("object", "checksum")
                      .queryParam("isLiteral", "true")
                      .post();

        AttributeParsingEvent attribute = new JerseyContentsAttributeParsingEvent(
                "testContentName", objectResource, PID);

        assertEquals(attribute.getChecksum(), "checksum");


    }
}
