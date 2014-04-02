package dk.statsbiblioteket.medieplatform.autonomous;

import java.util.Collection;
import java.util.Iterator;

/** The interface to the Summa Batch Object Index */
public interface EventTrigger {


    /**
     * Perform a search for batches matching the given criteria. All results are checked against DOMS
     * as we do not trust that the SBOI have the most current edition.
     *
     * @param pastSuccessfulEvents Events that the batch must have sucessfully experienced
     * @param pastFailedEvents     Events that the batch must have experienced, but which failed
     * @param futureEvents         Events that the batch must not have experienced
     *
     * @return An iterator over the found batches
     * @throws dk.statsbiblioteket.medieplatform.autonomous.CommunicationException if the communication failed
     */
    public Iterator<Batch> getTriggeredBatches(Collection<String> pastSuccessfulEvents, Collection<String> pastFailedEvents,
                                               Collection<String> futureEvents) throws
                                                                                            CommunicationException;

    /**
     * Perform a search for batches matching the given criteria. All results are checked against DOMS
     * as we do not trust that the SBOI have the most current edition.
     *
     * @param pastSuccessfulEvents Events that the batch must have sucessfully experienced
     * @param pastFailedEvents     Events that the batch must have experienced, but which failed
     * @param futureEvents         Events that the batch must not have experienced
     * @param batches              The resulting iterator will only contain hits from this collection.
     *
     * @return An iterator over the found batches
     * @throws dk.statsbiblioteket.medieplatform.autonomous.CommunicationException if the communication failed
     */
    public Iterator<Batch> getTriggeredBatches(Collection<String> pastSuccessfulEvents, Collection<String> pastFailedEvents,
                                               Collection<String> futureEvents, Collection<Batch> batches) throws
                                                                                                  CommunicationException;
}
