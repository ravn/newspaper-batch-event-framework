package dk.statsbiblioteket.autonomous.iterator.filesystem;

import dk.statsbiblioteket.autonomous.AbstractTests;
import dk.statsbiblioteket.autonomous.iterator.common.TreeIterator;
import dk.statsbiblioteket.autonomous.iterator.filesystem.transforming.TransformingIteratorForFileSystems;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;


public class TransformingIteratorForFileSystemsTest extends AbstractTests {

    private TreeIterator iterator;

    @Override
    public TreeIterator getIterator() throws URISyntaxException {
        if (iterator == null){
            File file = new File(Thread.currentThread().getContextClassLoader().getResource("batch").toURI());
            System.out.println(file);
            iterator = new TransformingIteratorForFileSystems(file,"\\.",".*\\.jp2",".md5");
        }
        return iterator;

    }



    @Override
    @Test
    public void testIterator() throws Exception {
        super.testIterator();
    }

    @Override
    @Test
    public void testIteratorWithSkipping() throws Exception {
        super.testIteratorWithSkipping();
    }
}