package life.qbic.portal.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;

public class BottomUpDesignReaderTest {

  private static final Logger logger = LogManager.getLogger(BottomUpDesignReaderTest.class);
  private final String tsv = "a4b_bottomup_format.tsv";

  @Test
  public void testReadSamples() throws Exception {
    BottomUpDesignReader r = new BottomUpDesignReader();
    File example = new File(getClass().getClassLoader().getResource(tsv).getFile());
    List<ISampleBean> samples = r.readSamples(example, false);
    
    assertEquals(10, samples.size());
    assertTrue(r.error == null || r.error.isEmpty());
  }

  @Test
  public void testExperimentMetadata() throws Exception {
    BottomUpDesignReader r = new BottomUpDesignReader();
    File example = new File(getClass().getClassLoader().getResource(tsv).getFile());
    SamplePreparator p = new SamplePreparator();
    p.processTSV(example, r, false);
    
    List<List<ISampleBean>> beans = p.getProcessed();
    assertEquals(5, beans.get(0).size());
    assertEquals(5, beans.get(1).size());
    assertEquals(SampleType.Q_TEST_SAMPLE, beans.get(0).get(0).getType());
    assertEquals(SampleType.Q_MS_RUN, beans.get(1).get(0).getType());
    p.getSpecialExperimentsOfType(ExperimentType.Q_SAMPLE_PREPARATION);
    p.getSpecialExperimentsOfType(ExperimentType.Q_MS_MEASUREMENT);
    
    assertTrue(r.error == null || r.error.isEmpty());
  }

  @Test
  public void testParsingError() throws Exception {
    BottomUpDesignReader r = new BottomUpDesignReader();
    File example = new File(getClass().getClassLoader().getResource("wrongColumn.tsv").getFile());
    r.readSamples(example, false);
    assertTrue(!r.error.isEmpty());
  }

}
