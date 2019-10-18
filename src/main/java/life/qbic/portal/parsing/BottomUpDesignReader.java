package life.qbic.portal.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.portal.model.MSRunCollection;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.model.SamplePreparationRun;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;

public class BottomUpDesignReader extends MSDesignReader {

  private static final Logger logger = LogManager.getLogger(BottomUpDesignReader.class);

  public BottomUpDesignReader() {
    this.mandatoryColumns = new ArrayList<String>(
        Arrays.asList("Digestion", "Preparation Date", "MS Run Date", "File Name", "MS Device",
            "Protein Barcode", "Enrichment/Fractionation Type", "Fraction Name"));
    this.mandatoryFilled = new ArrayList<String>(Arrays.asList("Digestion", "MS Device",
        "Preparation Date", "MS Run Date", "File Name", "Protein Barcode"));
    this.optionalCols =
        new ArrayList<String>(Arrays.asList("Chromatography Type", "LCMS Method", "Comment"));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, new HashMap<>());
    // headersToTypeCodePerSampletype.put("SampleType.Q_MS_RUN", msRunMetadata);
  }

  public static void main(String[] args) throws IOException {
    BottomUpDesignReader r = new BottomUpDesignReader();

    File example = new File(
        r.getClass().getClassLoader().getResource("examples/a4b_bottomup_format.tsv").getFile());

    System.out.println(r.readSamples(example, false));
    System.out.println(r.getExperimentInfos());
    // System.out.println(r.getTSVByRows());

  }

  /**
   * Reads in a TSV file containing openBIS samples that should be registered. Returns a List of
   * TSVSampleBeans containing all the necessary information to register each sample with its meta
   * information to openBIS, given that the types and parents exist.
   * 
   * @param file
   * @return ArrayList of TSVSampleBeans
   * @throws IOException
   */
  public List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException {
    tsvByRows = new ArrayList<String>();

    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        error = "Wrong number of columns in row " + i;
        reader.close();
        return null;
      }
    }
    reader.close();

    String[] header = data.get(0);
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<String, Integer>();
    List<Integer> meta = new ArrayList<Integer>();
    List<Integer> factors = new ArrayList<Integer>();
    List<Integer> loci = new ArrayList<Integer>();
    int numOfLevels = 3;

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : mandatoryColumns) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    for (i = 0; i < header.length; i++) {
      int position = mandatoryColumns.indexOf(header[i]);
      if (position == -1)
        position = optionalCols.indexOf(header[i]);
      if (position > -1) {
        headerMapping.put(header[i], i);
        meta.add(i);
      } else {
        meta.add(i);
      }
    }
    // create samples
    List<ISampleBean> beans = new ArrayList<>();
    List<List<ISampleBean>> order = new ArrayList<>();
    Map<String, TSVSampleBean> prepKeyToSample = new HashMap<>();
    Map<SamplePreparationRun, Map<String, String>> expIDToFracExp = new HashMap<>();
    Map<MSRunCollection, Map<String, String>> msIDToMSExp = new HashMap<>();

    int rowID = 0;
    int sampleID = 0;
    for (String[] row : data) {
      rowID++;
      boolean special = false;
      if (!special) {
        for (String col : mandatoryFilled) {
          if (row[headerMapping.get(col)].isEmpty()) {
            error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
            return null;
          }
        }
        // mandatory fields that need to be filled to identify sources and samples
        String prepDate = row[headerMapping.get("Preparation Date")];
        // String ligandExtrID = sourceID + "-" + tissue + "-" + prepDate + "-" + antibody;
        String msRunDate = row[headerMapping.get("MS Run Date")];
        String fName = row[headerMapping.get("File Name")];

        String proteinParent = row[headerMapping.get("Protein Barcode")];

        String fracType = "";
        String fracName = "";
        String enzymesString = row[headerMapping.get("Digestion")];
        Set<String> enzymes = parseDigestionEnzymes(enzymesString);
        if (headerMapping.containsKey("Enrichment/Fractionation Type")) {
          fracType = row[headerMapping.get("Enrichment/Fractionation Type")];
          fracName = row[headerMapping.get("Fraction Name")];
        }

        while (order.size() < numOfLevels) {
          order.add(new ArrayList<ISampleBean>());
        }
        // always one new measurement per row
        // chromatography options are stored on the MS level
        // if there is fractionation or enrichment, a new protein experiment and samples are needed
        // this is the case if fractionation type is not empty
        // the number of fractions is taken from the fraction names as well as the source barcode
        // (protein barcode)
        // so all fractions from the same protein sample end up in the same fractionation experiment
        // IF the fractionation/enrichment type is the same
        // digestion of proteins is taken as a second criteria for new experiments: different
        // digestion types need new samples + experiments
        SamplePreparationRun samplePrepRun = null;
        String prepID = proteinParent + " digest ";
        if (!fracName.isEmpty()) {
          prepID = fracType + "_" + fracName + "_" + prepID;
        }
        prepID = prepID + enzymesString;
        samplePrepRun = new SamplePreparationRun(proteinParent, prepDate, enzymes, fracType);
        TSVSampleBean prepSample = prepKeyToSample.get(prepID);
        if (prepSample == null) {
          sampleID++;
          prepSample = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
              prepID, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          prepSample.addParentID(proteinParent);

          proteinParent = Integer.toString(sampleID);

          prepSample.addProperty("Q_EXTERNALDB_ID", prepID);
          prepSample.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");

          order.get(0).add(prepSample);
          prepKeyToSample.put(prepID, prepSample);

          prepSample.setExperiment(Integer.toString(samplePrepRun.hashCode()));
          Map<String, String> samplePrepExpMetadata = expIDToFracExp.get(samplePrepRun);
          if (samplePrepExpMetadata == null) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("Q_FRACTIONATION_TYPE", fracType);
            metadata.put("Q_DIGESTION_ENZYMES", enzymesString);
            expIDToFracExp.put(samplePrepRun,
                parseFracExperimentData(row, headerMapping, metadata));
          } else
            expIDToFracExp.put(samplePrepRun,
                parseFracExperimentData(row, headerMapping, samplePrepExpMetadata));
        } else {
          proteinParent = prepSample.getCode();
        }
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, loci, SampleType.Q_MS_RUN));
        MSRunCollection msRuns = new MSRunCollection(samplePrepRun, msRunDate);
        msRun.setExperiment(Integer.toString(msRuns.hashCode()));
        Map<String, String> msExperiment = msIDToMSExp.get(msRuns);
        if (msExperiment == null)
          msIDToMSExp.put(msRuns, parseMSExperimentData(row, headerMapping, new HashMap<>()));
        msRun.addParentID(proteinParent);
        msRun.addProperty("File", fName);

        order.get(1).add(msRun);
      }
    }
    experimentInfos = new HashMap<>();

    // fractionation experiments
    List<PreliminaryOpenbisExperiment> fracExperiments = new ArrayList<>();
    for (SamplePreparationRun prepRun : expIDToFracExp.keySet()) {
      Map<String, String> map = expIDToFracExp.get(prepRun);
      // map.put("Code", Integer.toString(prepRun.hashCode()));// used to match samples to their
      // experiments later
      // msExperiments.add(map);
      PreliminaryOpenbisExperiment e =
          new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, map);
      e.setCode(Integer.toString(prepRun.hashCode()));
      fracExperiments.add(e);
    }
    experimentInfos.put(ExperimentType.Q_SAMPLE_PREPARATION, fracExperiments);

    // MS experiments
    List<PreliminaryOpenbisExperiment> msExperiments = new ArrayList<>();
    for (MSRunCollection runCollection : msIDToMSExp.keySet()) {
      Map<String, String> map = msIDToMSExp.get(runCollection);
      // map.put("Code", Integer.toString(runCollection.hashCode()));// used to match samples to
      // their
      // experiments later
      // msExperiments.add(map);
      PreliminaryOpenbisExperiment e =
          new PreliminaryOpenbisExperiment(ExperimentType.Q_MS_MEASUREMENT, map);
      e.setCode(Integer.toString(runCollection.hashCode()));
      msExperiments.add(e);
    }
    experimentInfos.put(ExperimentType.Q_MS_MEASUREMENT, msExperiments);
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    return beans;
  }

  @Override
  public Set<String> getAnalyteSet() {
    return new HashSet<String>(Arrays.asList("PROTEINS"));
  }

  @Override
  // TODO
  public int countEntities(File file) throws IOException {
    return 0;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    // TODO Auto-generated method stub
    return null;
  }

}
