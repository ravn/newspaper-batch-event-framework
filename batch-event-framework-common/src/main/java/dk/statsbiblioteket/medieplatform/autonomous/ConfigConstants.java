package dk.statsbiblioteket.medieplatform.autonomous;

public class ConfigConstants {

    //Doms
    public static final String DOMS_USERNAME = "doms.username";
    public static final String DOMS_PASSWORD = "doms.password";
    public static final String DOMS_URL = "doms.url";
    public static final String DOMS_PIDGENERATOR_URL = "doms.pidgenerator.url";

    //MFPak
    public static final String MFPAK_URL = "mfpak.postgres.url";
    public static final String MFPAK_USER = "mfpak.postgres.user";
    public static final String MFPAK_PASSWORD = "mfpak.postgres.password";


    //Batch iterator
    public static final String ITERATOR_USE_FILESYSTEM = "iterator.useFileSystem";
    public static final String ITERATOR_FILESYSTEM_BATCHES_FOLDER = "iterator.filesystem.batches.folder";
    public static final String ITERATOR_DOMS_ATTRIBUTENAMES = "iterator.doms.attributenames";
    public static final String ITERATOR_DOMS_PREDICATENAMES = "iterator.doms.predicatenames";

    //Autonomous component framework
    public static final String AUTONOMOUS_LOCKSERVER_URL = "autonomous.lockserver.url";
    public static final String AUTONOMOUS_SBOI_URL = "autonomous.sboi.url";
    public static final String AUTONOMOUS_PAST_SUCCESSFUL_EVENTS = "autonomous.pastSuccessfulEvents";
    public static final String AUTONOMOUS_PAST_FAILED_EVENTS = "autonomous.pastFailedEvents";
    public static final String AUTONOMOUS_FUTURE_EVENTS = "autonomous.futureEvents";
    public static final String AUTONOMOUS_MAXTHREADS = "autonomous.maxThreads";
    public static final String AUTONOMOUS_MAX_RUNTIME = "autonomous.maxRuntimeForWorkers";


}