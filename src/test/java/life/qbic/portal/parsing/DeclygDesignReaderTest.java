package life.qbic.portal.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.portal.model.MSRunCollection;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.model.SamplePreparationRun;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.xml.study.TechnologyType;

public class DeclygDesignReaderTest {

  private static final Logger logger = LogManager.getLogger(DeclygDesignReaderTest.class);

  private final String tsv = "a4b_deglycosylation_ms_format.tsv";

  @Test
  public void testReadSamples() throws Exception {
    DeclygDesignReader r = new DeclygDesignReader();
    File example = new File(getClass().getClassLoader().getResource(tsv).getFile());
    List<ISampleBean> samples = r.readSamples(example, false);
    assertEquals(8, samples.size());

    assertTrue(r.error == null || r.error.isEmpty());
  }

  @Test
  public void testExperimentMetadata() throws Exception {
    DeclygDesignReader r = new DeclygDesignReader();
    File example = new File(getClass().getClassLoader().getResource(tsv).getFile());
    SamplePreparator p = new SamplePreparator();
    p.processTSV(example, r, false);
    
    List<List<ISampleBean>> beans = p.getProcessed();
    assertEquals(4, beans.get(0).size());
    assertEquals(4, beans.get(1).size());
    assertEquals(SampleType.Q_TEST_SAMPLE, beans.get(0).get(0).getType());
    assertEquals(SampleType.Q_MS_RUN, beans.get(1).get(0).getType());
    p.getSpecialExperimentsOfType(ExperimentType.Q_SAMPLE_PREPARATION);
    p.getSpecialExperimentsOfType(ExperimentType.Q_MS_MEASUREMENT);

  }

  @Test
  public void testParsingError() throws Exception {
    DeclygDesignReader r = new DeclygDesignReader();
    File example = new File(getClass().getClassLoader().getResource("wrongColumn.tsv").getFile());
    r.readSamples(example, false);
    assertTrue(!r.error.isEmpty());
  }
}
