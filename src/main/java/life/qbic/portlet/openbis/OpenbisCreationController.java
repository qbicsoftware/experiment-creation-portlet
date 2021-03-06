package life.qbic.portlet.openbis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.persons.OpenbisSpaceUserRole;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.study.Qexperiment;


/**
 * Provides methods to register new entities to openBIS using the openBIS Client API. Also performs
 * some simple sanity checks before sending data to openBIS.
 * 
 * @author Andreas Friedrich
 * 
 */
public class OpenbisCreationController implements IOpenbisCreationController {
  final int RETRY_UNTIL_SECONDS_PASSED = 5;
  final int SPLIT_AT_ENTITY_SIZE = 100;
  private IOpenBisClient openbis;
  private OpenbisV3ReadController v3ReadController;
  private static final Logger logger = LogManager.getLogger(OpenbisCreationController.class);
  private String errors;
  private String user;


  public OpenbisCreationController(IOpenBisClient openbis, OpenbisV3ReadController v3ReadController,
      String user) {
    this.openbis = openbis;
    this.v3ReadController = v3ReadController;
    this.user = user;
  }

  /**
   * Interact with an ingestion service script registered for the openBIS instance
   * 
   * @param ingestionService Name of the ingestions service script registered at openBIS
   * @param params HashMap of String parameter names and their arguments for the ingestion service
   */
  public void openbisGenericIngest(String ingestionService, HashMap<String, Object> params) {
    openbis.ingest("DSS1", ingestionService, params);
  }

  /**
   * Creates a space in openBIS and adds (existing) users with different rights using ingestion
   * scripts on the server
   * 
   * @param name The name of the space to create
   * @param userInfo HashMap of type HashMap<OpenbisSpaceUserRole,ArrayList<String>> containing one
   *        or more users of one or more types
   */
  @Override
  public boolean registerSpace(String name, String description,
      HashMap<OpenbisSpaceUserRole, ArrayList<String>> userInfo) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", name);
    params.put("registration_user", user);
    for (OpenbisSpaceUserRole type : OpenbisSpaceUserRole.values()) {
      if (userInfo.containsKey(type))
        params.put(type.toString().toLowerCase(), userInfo.get(type));
    }
    // call ingestion service for space creation
    openbis.ingest("DSS1", "register-space", params);
    return true;
  }

  /**
   * Create a project belonging to a space in openBIS using ingestion scripts on the server
   * 
   * @param space Existing space the project to create should resides in
   * @param name Name of the project to create
   * @param description Project description
   * @return false, if the specified space doesn't exist, resulting in failure, true otherwise
   */
  @Override
  public boolean registerProject(String space, String name, String description) {
    errors = "";
    if (!v3ReadController.spaceExists(space)) {
      errors = space + " does not exist!";
      logger.error(errors);
      return false;
    }
    logger.info("Creating project " + name + " in space " + space);
    if (description == null || description.isEmpty()) {
      description = "Created using the project wizard.";
      logger.warn("No project description input found. Setting standard info.");
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("user", user);
    params.put("code", name);
    params.put("space", space);
    params.put("desc", description);
    openbis.ingest("DSS1", "register-proj", params);
    return true;
  }

  /**
   * Create an experiment belonging to a project (and space) using ingestion scripts on the server
   * 
   * @param space Existing space in openBis
   * @param project Existing project in the space that this experiment will belong to
   * @param expType openBIS experiment type
   * @param name Experiment name
   * @param map Additional properties of the experiment
   * @return false, if the specified project doesn't exist, resulting in failure, true otherwise
   */
  @Override
  public boolean registerExperiment(String space, String project, ExperimentType expType,
      String name, Map<String, String> map) {
    logger.info("Creating experiment " + name);
    errors = "";
    if (!v3ReadController.projectExists(project)) {
      errors = project + " in " + space + " does not exist.";
      logger.error(errors);
      return false;
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", name);
    params.put("type", expType.toString());
    params.put("project", project);
    params.put("space", space);
    params.put("properties", map);
    params.put("user", user);
    openbis.ingest("DSS1", "register-exp", params);
    return true;
  }

  @Override
  public boolean registerExperiments(String space, String proj, List<RegisterableExperiment> exps) {
    errors = "";
    int step = 100;
    int max = RETRY_UNTIL_SECONDS_PASSED * 1000;
    List<String> codes = new ArrayList<String>();
    List<String> types = new ArrayList<String>();
    List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();
    for (RegisterableExperiment e : exps) {
      String expID = ExperimentCodeFunctions.getExperimentIdentifier(space, proj, e.getCode());
      if (!v3ReadController.experimentWithIDExists(expID)) {
        codes.add(e.getCode());
        types.add(e.getType());
        props.add(e.getProperties());
      }
    }
    if (codes.size() > 0) {
      while (!v3ReadController.projectExists(proj) && max > 0) {
        try {
          Thread.sleep(step);
          max -= step;
        } catch (InterruptedException e) {
          logger.error("thread sleep waiting for project creation interruped.");
          e.printStackTrace();
        }
      }
      logger.info("Creating experiments " + codes);
      if (!v3ReadController.projectExists(proj)) {
        errors = proj + " in " + space + " does not exist. Not creating experiments.";
        logger.error(errors);
        return false;
      }
      Map<String, Object> params = new HashMap<String, Object>();
      params.put("codes", codes);
      params.put("types", types);
      params.put("project", proj);
      params.put("space", space);
      params.put("properties", props);
      params.put("user", user);
      openbis.ingest("DSS1", "register-exp", params);
    }
    return true;
  }

  private List<List<ISampleBean>> splitSamplesIntoBatches(List<ISampleBean> samples,
      int targetSize) {
    List<List<ISampleBean>> res = new ArrayList<List<ISampleBean>>();
    int size = samples.size();
    if (size < targetSize)
      return new ArrayList<List<ISampleBean>>(Arrays.asList(samples));
    for (int i = 0; i < size / targetSize; i++) {
      int from = i * targetSize;
      int to = (i + 1) * targetSize;
      res.add(samples.subList(from, to));
      if (to > size - targetSize && to != size)
        res.add(samples.subList(to, size));
    }
    return res;
  }

  /**
   * this is the one normally called!
   * 
   * @param tsvSampleHierarchy
   * @param description
   * @param secondaryName
   * @param bar
   * @param info
   * @param ready
   * @param user
   */
  @Override
  public void registerProjectWithExperimentsAndSamplesBatchWise(
      final List<List<ISampleBean>> tsvSampleHierarchy, final String description,
      final Set<PreliminaryOpenbisExperiment> experiments, final ProgressBar bar, final Label info,
      final Runnable ready, Map<String, Map<String, String>> entitiesToUpdate,
      final boolean isPilot) {
    errors = "";

    ISampleBean first = tsvSampleHierarchy.get(0).get(0);
    String space = first.getSpace();
    String project = first.getProject();

    RegisterableProject p =
        new RegisterableProject(space, project, description, experiments, isPilot);
    for (String experiment : entitiesToUpdate.keySet()) {
      String expID = ExperimentCodeFunctions.getExperimentIdentifier(p.getSpace(),
          p.getProjectCode(), experiment);
      Date modificationDate = v3ReadController.getExperimentModificationDate(expID);
      if (modificationDate == null) {
        errors = "Could not find experiment " + expID + ", not registering samples!";
        logger.error(errors);
        return;
      }
      long modificationTime = modificationDate.getTime();

      updateExperiment(expID, entitiesToUpdate.get(experiment));

      long newModificationTime = modificationTime;
      double TIMEOUT = 10000;

      while (newModificationTime == modificationTime && TIMEOUT > 0) {
        Date newModificationDate = v3ReadController.getExperimentModificationDate(expID);
        newModificationTime = newModificationDate.getTime();
        TIMEOUT -= 300;
        try {
          Thread.sleep(300);
        } catch (InterruptedException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

      }
      if (TIMEOUT < 0) {
        errors = "could not update existing experimental design, not registering samples!";
        logger.error(errors);
        return;
      } else {
        logger.debug("completed update of experimental design successfully");
      }
    }

    logger.debug("User sending samples: " + user);
    Thread t = new Thread(new Runnable() {
      volatile int current = -1;

      @Override
      public void run() {
        info.setCaption("Collecting information");
        UI.getCurrent().access(new UpdateProgressBar(bar, info, 0.01));
        List<RegisterableExperiment> exps = p.getExperiments();

        String space = p.getSpace().toUpperCase();
        String project = p.getProjectCode();
        String desc = p.getDescription();

        int splitSteps = 0;
        // find out which experiments have so many samples they should be sent in multiple packages
        for (List<ISampleBean> level : tsvSampleHierarchy) {
          if (level.size() > SPLIT_AT_ENTITY_SIZE) {
            splitSteps += 1;
          }
        }
        final int todo = tsvSampleHierarchy.size() + splitSteps + 1;
        // bar
        current++;
        double frac = current * 1.0 / todo;
        UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
        info.setCaption("Registering Project and Experiments");
        boolean success = true;
        if (!v3ReadController.projectExists(project)) {
          success = registerProject(space, project, desc);
          if (!success) {
            errors = "Project could not be registered.";
          }
        }
        if (success) {
          success = registerExperiments(space, project, exps);
          if (!success) {
            errors = "Experiments could not be registered.";
          }
        }
        if (!success) {
          // experiments were not registered, break registration
          bar.setVisible(false);
          info.setCaption("An error occured.");
          UI.getCurrent().setPollInterval(-1);
          UI.getCurrent().access(ready);
          return;
        }
        // current++;
        // double frac = current * 1.0 / todo;
        // UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
        int i = 0;
        for (List<ISampleBean> level : tsvSampleHierarchy) {
          i++;
          logger.info("registration of level " + i);
          info.setCaption("Registering samples");
          info.setValue("");
          current++;
          frac = current * 1.0 / todo;
          UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
          boolean batchSuccess;
          if (level.size() > SPLIT_AT_ENTITY_SIZE) {
            for (List<ISampleBean> batch : splitSamplesIntoBatches(level, SPLIT_AT_ENTITY_SIZE)) {
              batchSuccess = registerSampleBatch(batch);
              if (!batchSuccess) {
                bar.setVisible(false);
                info.setCaption("An error occured.");
                UI.getCurrent().setPollInterval(-1);
                UI.getCurrent().access(ready);
                return;
              }
              current++;
              frac = current * 1.0 / todo;
              UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));
            }
          } else {
            batchSuccess = registerSampleBatch(level);
            if (!batchSuccess) {
              bar.setVisible(false);
              info.setCaption("An error occured.");
              UI.getCurrent().setPollInterval(-1);
              UI.getCurrent().access(ready);
              return;
            }
          }
        }
        current++;
        frac = current * 1.0 / todo;
        UI.getCurrent().access(new UpdateProgressBar(bar, info, frac));

        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().access(ready);
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
  }

  @Override
  public boolean registerSampleBatch(List<ISampleBean> samples) {
    int wait_time = RETRY_UNTIL_SECONDS_PASSED * 1000;
    String s = null;
    String p = null;
    String e = null;
    if (samples.size() == 0)
      return true;
    // to speed up things only the first sample and its experiment is checked for existence, might
    // lead to errors
    ISampleBean first = samples.get(0);
    if (!first.getExperiment().equals(e)) {

      s = first.getSpace();
      p = first.getProject();
      e = first.getExperiment();
      String expID = ExperimentCodeFunctions.getExperimentIdentifier(s, p, e);
      int step = 200;
      while (!v3ReadController.experimentWithIDExists(expID) && wait_time > 0) {
        try {
          Thread.sleep(step);
          wait_time -= step;
        } catch (InterruptedException exc) {
          logger.error("thread sleep waiting for project creation interruped.");
          exc.printStackTrace();
        }
      }
      if (!v3ReadController.experimentWithIDExists(expID)) {
        errors = e + " not found in " + p + " (" + s + ") Stopping registration of samples.";
        logger.error(errors + " This will most likely lead to openbis errors or lost samples!");
        return false;
      }
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("user", user);

    for (ISampleBean sample : samples) {
      if (v3ReadController.sampleExists(sample.getCode())) {
        logger.warn(sample.getCode() + " already exists in " + p
            + " Removing this sample from registration process.");
      } else {
        String space = sample.getSpace();
        String project = sample.getProject();
        String exp = sample.getExperiment();
        List<String> parents = sample.getParentIDs();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("code", sample.getCode());
        map.put("space", space);
        map.put("project", project);
        map.put("experiment", exp);
        map.put("type", sample.getType().toString());
        if (!sample.getSecondaryName().isEmpty()) {
          map.put("Q_SECONDARY_NAME", sample.getSecondaryName());
        }
        if (!parents.isEmpty())
          map.put("parents", parents);
        map.put("metadata", sample.getMetadata());
        params.put(sample.getCode(), map);
      }
    }
    logger.info("Sending batch of new samples to Ingestion Service.");
    openbis.ingest("DSS1", "register-sample-batch", params);
    return true;
  }

  public String getErrors() {
    return errors;
  }

  @Override
  public boolean setupEmptyProject(String space, String project, String description)
      throws JAXBException {
    registerProject(space, project, description);
    StudyXMLParser p = new StudyXMLParser();
    JAXBElement<Qexperiment> res =
        p.createNewDesign(new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    String emptyStudy = p.toString(res);

    String exp = project + "_INFO";
    String code = project + "000";

    Map<String, String> props = new HashMap<>();
    props.put("Q_EXPERIMENTAL_SETUP", emptyStudy);

    boolean success =
        registerExperiment(space, project, ExperimentType.Q_PROJECT_DETAILS, exp, props);
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info experiment could not be registered.";
      logger.error(errors);
      return false;
    }
    ISampleBean infoSample = new TSVSampleBean(code, exp, project, space,
        SampleType.Q_ATTACHMENT_SAMPLE, "", new ArrayList<String>(), new HashMap<String, Object>());
    success = registerSampleBatch(new ArrayList<ISampleBean>(Arrays.asList(infoSample)));
    if (!success) {
      // experiments were not registered, break registration
      errors = "Info sample could not be registered.";
      logger.error(errors);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public void updateExperiment(String expID, Map<String, String> map) {
    // TODO Auto-generated method stub

  }

}
