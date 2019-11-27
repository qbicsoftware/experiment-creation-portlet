package life.qbic.portlet.openbis;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.exceptions.NotFetchedException;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.portal.model.ExtendedOpenbisExperiment;

public class OpenbisV3ReadController {

  private OpenbisV3APIWrapper v3Wrapper;
  private static final Logger logger = LogManager.getLogger(OpenbisV3ReadController.class);

  public OpenbisV3ReadController(OpenbisV3APIWrapper v3) {
    v3Wrapper = v3;
  }

  public List<String> getSpaceNames() {
    List<String> res = new ArrayList<>();
    SearchResult<Space> searchResults = v3Wrapper.getSpacesForUser();
    for (Space s : searchResults.getObjects()) {
      res.add(s.getCode());
    }
    return res;
  }

  public Map<String, String> getPropertiesOfExperimentType(
      life.qbic.datamodel.experiments.ExperimentType type) {
    Map<String, String> res = new HashMap<>();
    SearchResult<ExperimentType> searchResults = v3Wrapper.getExperimentTypeByCode(type.toString());
    if (!searchResults.getObjects().isEmpty()) {
      ExperimentType found = searchResults.getObjects().get(0);
      List<PropertyAssignment> props = found.getPropertyAssignments();
      for (PropertyAssignment prop : props) {
        PropertyType t = prop.getPropertyType();
        res.put(t.getCode(), t.getLabel());
      }
      return res;
    }
    logger.error("Property types for experiment type " + type.toString()
        + " could not be fetched, because experiment type could not be found.");
    return res;
  }

  public List<String> getProjectCodesOfSpace(String space) {
    List<String> res = new ArrayList<>();
    SearchResult<Project> searchResults = v3Wrapper.getProjectsOfSpace(space);
    for (Project p : searchResults.getObjects()) {
      res.add(p.getCode());
    }
    return res;
  }

  public OpenbisExperiment getExperimentByID(String id) {
    Experiment e = v3Wrapper.getExperimentByID(id);
    if (e != null) {
      return apiExperimentToQBiCAdaptor(e);
    }
    return null;
  }

  public ExtendedOpenbisExperiment getExperimentWithSamplesByID(String id,
      boolean fetchParentSamples) {
    Experiment e = v3Wrapper.getExperimentWithSamplesByID(id, fetchParentSamples);
    if (e != null) {
      return (ExtendedOpenbisExperiment) apiExperimentToQBiCAdaptor(e);
    }
    return null;
  }

  public List<OpenbisExperiment> getExperimentsOfProject(String projectCode) {
    List<Experiment> exps = v3Wrapper.getExperimentsOfProject(projectCode);
    List<OpenbisExperiment> res = new ArrayList<>();
    for (Experiment e : exps) {
      OpenbisExperiment ex = apiExperimentToQBiCAdaptor(e);
      if (ex != null)
        res.add(ex);
    }
    return res;
  }

  public Date getExperimentModificationDate(String experimentID) {
    try {
      return v3Wrapper.getExperimentByID(experimentID).getModificationDate();
    } catch (NullPointerException e) {
      return null;
    }
  }

  private OpenbisExperiment apiExperimentToQBiCAdaptor(Experiment e) {

    Map<String, Object> props = new HashMap<>();
    for (String key : e.getProperties().keySet()) {
      props.put(key, e.getProperties().get(key));
    }
    String type = e.getType().getCode();
    try {
      life.qbic.datamodel.experiments.ExperimentType.valueOf(type);
    } catch (IllegalArgumentException ex) {
      logger.warn(type
          + " experiment type is unknown. Consider adding it to the data model. Ignoring this experiment.");
      return null;
    }
    try {
      Person registrator = e.getRegistrator();
      String person = registrator.getFirstName() + " " + registrator.getLastName();
      person = person.trim();
      if (person.isEmpty()) {
        person = registrator.getUserId();
      }
      return new ExtendedOpenbisExperiment(e.getCode(),
          life.qbic.datamodel.experiments.ExperimentType.valueOf(type), props, e.getSamples(),
          e.getRegistrationDate(), person);
    } catch (NotFetchedException ex) {
      return new OpenbisExperiment(e.getCode(),
          life.qbic.datamodel.experiments.ExperimentType.valueOf(type), props);
    }
  }

  public List<ExtendedOpenbisExperiment> getExperimentsWithSamplesOfProject(String projectCode,
      boolean fetchParentSamples) {
    List<Experiment> exps =
        v3Wrapper.getExperimentsWithSamplesOfProject(projectCode, fetchParentSamples);
    List<ExtendedOpenbisExperiment> res = new ArrayList<>();
    for (Experiment e : exps) {
      ExtendedOpenbisExperiment ex = (ExtendedOpenbisExperiment) apiExperimentToQBiCAdaptor(e);
      if (ex != null)
        res.add(ex);
    }
    return res;
  }

  public boolean spaceExists(String space) {
    return !v3Wrapper.getSpace(space).getObjects().isEmpty();
  }

  public boolean projectExists(String project) {
    return !v3Wrapper.getProject(project).getObjects().isEmpty();
  }

  public boolean experimentWithIDExists(String experimentID) {
    return v3Wrapper.getExperimentByID(experimentID) != null;
  }

  public boolean sampleExists(String sampleCode) {
    return !v3Wrapper.searchSampleWithCode(sampleCode).getObjects().isEmpty();
  }

  public Sample findSampleByCode(String code) {
    for (Sample s : v3Wrapper.searchSampleWithCode(code).getObjects()) {
      return s;
    }
    return null;
  }

  public Map<String, String> getVocabLabelsToCodes(String vocabulary) {
    return v3Wrapper.getVocabLabelToCode(vocabulary);
  }

}
