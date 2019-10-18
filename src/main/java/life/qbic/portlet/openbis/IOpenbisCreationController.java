package life.qbic.portlet.openbis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public interface IOpenbisCreationController {

  public boolean registerSpace(String name, String description,
      HashMap<OpenbisSpaceUserRole, ArrayList<String>> userInfo);

  public boolean registerProject(String space, String name, String description);

  public boolean registerExperiment(String space, String project, ExperimentType expType,
      String name, Map<String, String> map);

  public boolean registerExperiments(String space, String proj, List<RegisterableExperiment> exps);

  public void registerProjectWithExperimentsAndSamplesBatchWise(
      final List<List<ISampleBean>> tsvSampleHierarchy, final String description,
      final Set<PreliminaryOpenbisExperiment> informativeExperiments, final ProgressBar bar, final Label info,
      final Runnable ready, Map<String, Map<String, String>> entitiesToUpdate,
      final boolean isPilot);

  public boolean registerSampleBatch(List<ISampleBean> samples);

  public String getErrors();

  public boolean setupEmptyProject(String space, String project, String description)
      throws JAXBException;

  void updateExperiment(String expID, Map<String, String> map);
}
