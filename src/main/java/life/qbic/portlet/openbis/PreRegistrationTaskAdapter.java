package life.qbic.portlet.openbis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.model.ExtendedOpenbisExperiment;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public class PreRegistrationTaskAdapter {

  private static final Logger logger = LogManager.getLogger(PreRegistrationTaskAdapter.class);

  private OpenbisV3ReadController openbisReader;
  private String project;
  private String space;
  private String firstFreeBarcode;
  private int firstFreeEntityID;
  private int firstFreeExperimentID;

  public PreRegistrationTaskAdapter(OpenbisV3ReadController readController) {
    openbisReader = readController;
  }

  public void setProject(String space, String project) {
    this.space = space;
    this.project = project;
  }

  public void resetAndAlignBarcodeCounter() {
    logger.info("finding free codes for experiments and samples of project " + project);
    List<ExtendedOpenbisExperiment> exps =
        openbisReader.getExperimentsWithSamplesOfProject(project, false);

    firstFreeEntityID = 1;
    firstFreeExperimentID = 1;

    String base = project + "001A";
    firstFreeBarcode = base + SampleCodeFunctions.checksum(base);
    List<Sample> samples = new ArrayList<>();
    for (ExtendedOpenbisExperiment exp : exps) {
      samples.addAll(exp.getSamples());
      String code = exp.getExperimentCode();
      String[] split = code.split(project + "E");
      if (code.startsWith(project + "E") && split.length > 1) {
        int num = 0;
        try {
          num = Integer.parseInt(split[1]);
        } catch (Exception e2) {
        }
        if (firstFreeExperimentID <= num)
          firstFreeExperimentID = num + 1;
      }
    }
    for (Sample s : samples) {
      String code = s.getCode();
      if (SampleCodeFunctions.isQbicBarcode(code)) {
        if (SampleCodeFunctions.compareSampleCodes(firstFreeBarcode, code) <= 0) {
          firstFreeBarcode = SampleCodeFunctions.incrementSampleCode(code);
        }
      } else if (s.getType().toString().equals((SampleType.Q_BIOLOGICAL_ENTITY.toString()))) {
        int num = Integer.parseInt(s.getCode().split("-")[1]);
        if (num >= firstFreeEntityID)
          firstFreeEntityID = num + 1;
      }
    }
  }

  public void createNewCodesForExperimentsAndSamples(
      LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> samplesToRegister) {

    Map<String, String> oldCodesToNew = new HashMap<>();

    for (PreliminaryOpenbisExperiment exp : samplesToRegister.keySet()) {

      String expCode = project + "E" + firstFreeExperimentID;
      exp.setCode(expCode);
      firstFreeExperimentID++;

      for (ISampleBean b : samplesToRegister.get(exp)) {
        if (b instanceof TSVSampleBean) {
          TSVSampleBean s = (TSVSampleBean) b;
          String code = "";
          if (s.getType().equals(SampleType.Q_BIOLOGICAL_ENTITY)) {
            code = project + "ENTITY-" + firstFreeEntityID;
            firstFreeEntityID++;
          } else {
            code = firstFreeBarcode;
            firstFreeBarcode = SampleCodeFunctions.incrementSampleCode(code);
          }
          oldCodesToNew.put(s.getCode(), code);
          s.setSpace(space);
          s.setProject(project);
          s.setCode(code);
          s.setExperiment(expCode);

          List<String> parents = s.getParentIDs();
          s.setParents(new ArrayList<ISampleBean>());
          List<String> newParents = new ArrayList<String>();
          for (String oldParent : parents) {
            if (oldCodesToNew.containsKey(oldParent)) {
              newParents.add(oldCodesToNew.get(oldParent));
            } else {
              if (!SampleCodeFunctions.isQbicBarcode(oldParent)) {
                logger
                    .warn("Parent could not be translated, because no code mapping was found for: "
                        + oldParent);
              }
              newParents = parents;
            }
          }
          for (String p : newParents) {
            s.addParentID(p);
          }
        } else {
          logger.error(
              "Sample code cannot be set because the sample implementing ISampleBean is of the wrong class type.");
        }
      }
    }
  }

}
