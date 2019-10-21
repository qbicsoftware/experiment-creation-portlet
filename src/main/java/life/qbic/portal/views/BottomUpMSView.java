package life.qbic.portal.views;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.portal.parsing.ExperimentalDesignType;
import life.qbic.portal.parsing.SamplePreparator;
import life.qbic.portlet.openbis.OpenbisV3CreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;

public class BottomUpMSView extends ImportRegisterView implements IWizardStep {

  public BottomUpMSView(List<Sample> previousLevel, OpenbisV3ReadController readController,
      OpenbisV3CreationController controller, String space, String project) {
    super(ExperimentalDesignType.BottomUp, previousLevel, readController, controller, space,
        project);
  }

  @Override
  public boolean isValid() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> translateParsedExperiments(
      SamplePreparator preparator) {
    msProperties = preparator.getSpecialExperimentsOfType(ExperimentType.Q_MS_MEASUREMENT);
    protProperties = preparator.getSpecialExperimentsOfType(ExperimentType.Q_SAMPLE_PREPARATION);
    Map<String, PreliminaryOpenbisExperiment> experimentsByCode = new LinkedHashMap<>();
    for (PreliminaryOpenbisExperiment e : protProperties) {
      experimentsByCode.put(e.getCode(), e);
    }
    for (PreliminaryOpenbisExperiment e : msProperties) {
      experimentsByCode.put(e.getCode(), e);
    }
    LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> res = new LinkedHashMap<>();
    for (List<ISampleBean> samples : preparator.getProcessed()) {
      for (ISampleBean s : samples) {
        String expCode = s.getExperiment();
        PreliminaryOpenbisExperiment exp = experimentsByCode.get(expCode);
        if (res.containsKey(exp)) {
          res.get(exp).add(s);
        } else {
          List<ISampleBean> samps = new ArrayList<>();
          samps.add(s);
          res.put(exp, samps);
        }
      }
    }
    return res;
  }

  @Override
  public void registrationDone(String errors) {
    // TODO Auto-generated method stub

  }


}
