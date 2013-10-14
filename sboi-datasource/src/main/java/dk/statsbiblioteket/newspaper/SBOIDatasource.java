package dk.statsbiblioteket.newspaper;

import dk.statsbibliokeket.newspaper.batcheventFramework.SBOIClientImpl;

import dk.statsbiblioteket.newspaper.batcheventFramework.NewspaperIDFormatter;
import dk.statsbiblioteket.newspaper.batcheventFramework.PremisManipulatorFactory;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.Batch;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.CommunicationException;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.DataSource;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.Event;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.EventID;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.NotFoundException;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.NotWorkingProperlyException;
import dk.statsbiblioteket.newspaper.processmonitor.datasources.SBOIInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Datasource implemented to SBOI
 */
public class SBOIDatasource implements DataSource {

    private SBOIDatasourceConfiguration configuration;

    private SBOIInterface client = null;

    public SBOIDatasource(SBOIDatasourceConfiguration configuration) {
        this.configuration = configuration;
    }

    private synchronized SBOIInterface getClient(){
        if (client == null){
            client = new SBOIClientImpl(configuration.getSummaLocation(), new PremisManipulatorFactory(new NewspaperIDFormatter(),
                                                                         PremisManipulatorFactory.TYPE));
        }
        return client;
    }

    @Override
    public List<Batch> getBatches(boolean includeDetails, Map<String, String> filters) throws NotWorkingProperlyException {
        try {
            Iterator<Batch> batches = getClient().getBatches(Arrays.asList(EventID.Data_Received), new ArrayList<EventID>(), new ArrayList<EventID>());
            List<Batch> results = new ArrayList<>();
            while (batches.hasNext()) {
                Batch next = batches.next();
                Batch better = getClient().getBatch(next.getBatchID(), next.getRoundTripNumber());
                results.add(better);
            }
            return stripDetails(results.iterator(),includeDetails);
        } catch (CommunicationException | NotFoundException e) {
            throw new NotWorkingProperlyException("Failed to communicate with SBOI",e);
        }

    }

    /**
     * Strip details if required from the batched
     * @param batches the batches to strip
     * @param includeDetails true if details should be stripped
     * @return the batches as a list
     */
    private List<Batch> stripDetails(Iterator<Batch> batches, boolean includeDetails) {
        ArrayList<Batch> result = new ArrayList<>();
        while (batches.hasNext()) {
            Batch next = batches.next();
            result.add(stripDetails(next,includeDetails));
        }
        return result;
    }

    /**
     * Strip details on a single batch
     * @param batch the batch
     * @param includeDetails true if details should be stripped
     * @return the batch
     */
    private Batch stripDetails(Batch batch, boolean includeDetails) {
        if (!includeDetails){
            List<Event> events = batch.getEventList();
            for (Event event : events) {
                event.setDetails(null);
            }
        }
        return batch;
    }

    @Override
    public Batch getBatch(Long batchID, Integer roundTripNumber, boolean includeDetails) throws NotFoundException, NotWorkingProperlyException {
        try {
            return stripDetails(getClient().getBatch(batchID,roundTripNumber), includeDetails);
        } catch (CommunicationException e) {
            throw new NotWorkingProperlyException(e);
        }

    }

    @Override
    public Event getBatchEvent(Long batchID, Integer roundTripNumber, EventID eventID, boolean includeDetails) throws NotFoundException, NotWorkingProperlyException {
        Batch batch = getBatch(batchID,roundTripNumber, includeDetails);
        for (Event event : batch.getEventList()) {
            if (event.getEventID().equals(eventID)){
                return event;
            }
        }
        throw new NotFoundException("Event not found");
    }
}
