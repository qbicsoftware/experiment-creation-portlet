package life.qbic.portlet.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import com.vaadin.data.validator.CompositeValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.portal.portlet.DBManager;
import life.qbic.portlet.openbis.IOpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;
import life.qbic.portlet.openbis.PreRegistrationTaskAdapter;
import life.qbic.portlet.openbis.ProjectNameValidator;
import life.qbic.portlet.openbis.RegisteredToOpenbisReadyRunnable;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.study.Qexperiment;
import life.qbic.xml.study.TechnologyType;

public class AdminWindow extends Window {

  private Button startButton;
  private IOpenbisCreationController creationController;
  private DBManager dbm;
  private PreRegistrationTaskAdapter preRegTaskManager;
  private String space;
  private OpenbisV3ReadController readController;

  public AdminWindow(OpenbisV3ReadController readController,
      IOpenbisCreationController creationController, DBManager dbm, String space) {
    this.space = space;
    this.creationController = creationController;
    this.readController = readController;
    preRegTaskManager = new PreRegistrationTaskAdapter(readController);
    this.dbm = dbm;
    this.startButton = new Button("New Project");
    startButton.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        createUI();
      }
    });
  }

  protected void createUI() {
    TextField tp = new TextField("Therapeutic Protein");
    tp.setValidationVisible(true);
    tp.setRequired(true);
    tp.setStyleName(Styles.fieldTheme);
    TextField project = new TextField("QBiC Project Code");
    project.setStyleName(Styles.fieldTheme);

    CompositeValidator vd = new CompositeValidator();
    RegexpValidator p = new RegexpValidator("Q[A-Xa-x0-9]{4}",
        "Project must have length of 5, start with Q and not contain Y or Z");
    vd.addValidator(p);
    vd.addValidator(new ProjectNameValidator(readController));
    project.addValidator(vd);
    project.setImmediate(true);
    project.setValidationVisible(true);

    setWidth("450px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);

    VerticalLayout comments = new VerticalLayout();
    layout.addComponent(comments);

    Button register = new Button("Register");
    Button close = new Button("Close");
    register.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (isValid()) {
          preRegTaskManager.setProject(space, project.getValue());
          try {
            createProject("/" + space + "/" + project, tp.getValue());
          } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }

      private boolean isValid() {// TODO
        return !(tp.isEmpty() || project.isEmpty());
      }
    });

    layout.addComponent(tp);
    layout.addComponent(project);

    close.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        close();
      }
    });
    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setSpacing(true);
    buttons.addComponent(register);
    buttons.addComponent(close);
    layout.addComponent(buttons);

    setContent(layout);
    // Center it in the browser window
    center();
    setModal(true);
    setIcon(FontAwesome.STAR);
    setResizable(false);

    UI.getCurrent().addWindow(this);
  }

  protected void createProject(String projectID, String tp) throws JAXBException {
    String description = "Experiments on therapeutic protein " + tp;

    PreliminaryOpenbisExperiment hamsterExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_EXPERIMENTAL_DESIGN, new HashMap<>());

    HashMap<String, Object> entityProps = new HashMap<>();
    entityProps.put("Q_NCBI_ORGANISM", "10029");
    TSVSampleBean entity = new TSVSampleBean("c", SampleType.Q_BIOLOGICAL_ENTITY, "", entityProps);
    LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> samplesToRegister =
        new LinkedHashMap<>();
    samplesToRegister.put(hamsterExp, Arrays.asList(entity));

    // TODO test

    ISampleBean infoSample =
        new TSVSampleBean("i", SampleType.Q_ATTACHMENT_SAMPLE, "", new HashMap<String, Object>());

    samplesToRegister.put(prepareXMLPropertyForNewExperiment(), Arrays.asList(infoSample));

    preRegTaskManager.resetAndAlignBarcodeCounter();
    preRegTaskManager.createNewCodesForExperimentsAndSamples(samplesToRegister);

    List<List<ISampleBean>> samples = new ArrayList<>(samplesToRegister.values());

    creationController.registerProjectWithExperimentsAndSamplesBatchWise(samples, description,
        samplesToRegister.keySet(), new ProgressBar(), new Label(),
        new RegisteredToOpenbisReadyRunnable(this), new HashMap<>(), false);

    dbm.addProjectToDB(projectID, tp);
  }

  private PreliminaryOpenbisExperiment prepareXMLPropertyForNewExperiment() throws JAXBException {
    Map<String, Object> propsMap = new HashMap<>();
    StudyXMLParser p = new StudyXMLParser();
    JAXBElement<Qexperiment> minDesign = p.createNewDesign(new HashSet<>(),
        new ArrayList<>(Arrays.asList(new TechnologyType("Sample Preparation"))), new HashMap<>(),
        new HashMap<>());
    propsMap.put("Q_EXPERIMENTAL_SETUP", p.toString(minDesign));

    PreliminaryOpenbisExperiment exp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_PROJECT_DETAILS, propsMap);
    return exp;
  }

  // public void registrationDone(String errors) {
  // if (errors.isEmpty()) {
  // logger.info("Sample registration complete!");
  // Styles.notification("Registration complete!", "Registration of samples complete.",
  // NotificationType.SUCCESS);
  // register.setEnabled(false);
  // finish.setEnabled(true);
  // } else {
  // String feedback = "Sample registration could not be completed. Reason: " + errors;
  // logger.error(feedback);
  // Styles.notification("Registration failed!", feedback, NotificationType.ERROR);
  // }
  // }

  public Button getStartButton() {
    return startButton;
  }

}
