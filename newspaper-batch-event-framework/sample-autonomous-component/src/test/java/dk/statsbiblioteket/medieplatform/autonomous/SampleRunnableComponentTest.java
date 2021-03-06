package dk.statsbiblioteket.medieplatform.autonomous;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleRunnableComponentTest {
    @Test
    public void testdoWorkOnItem() throws Exception {
        SampleRunnableComponent runnableComponent = new MockupIteratorSuper(System.getProperties());

        ResultCollector result = new ResultCollector(
                runnableComponent.getComponentName(), runnableComponent.getComponentVersion(),100);
        Batch batch = new Batch("60000");
        runnableComponent.doWorkOnItem(batch, result);
        Assert.assertTrue(result.isSuccess());

    }
}
