package life.qbic.portal.portlet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.model.ExperimentTemplate;
import life.qbic.portal.model.ExtendedOpenbisExperiment;
import life.qbic.portal.steps.IWizardStep;
import life.qbic.portal.steps.views.BiologicsCultureCreationView;
import life.qbic.portal.steps.views.BottomUpMSView;
import life.qbic.portal.steps.views.DeglycosylationView;
import life.qbic.portal.steps.views.RegistrationView;
import life.qbic.portal.steps.views.TopDownMSView;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.portlet.components.AdminWindow;
import life.qbic.portlet.components.SampleInfoComponent;
import life.qbic.portlet.openbis.IOpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisV3APIWrapper;
import life.qbic.portlet.openbis.OpenbisV3CreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;
import life.qbic.xml.manager.HistoryReader;
import life.qbic.xml.notes.Note;

/**
 * Entry point for portlet experiment-creation-portlet. This class derives from
 * {@link QBiCPortletUI}, which is found in the {@code portal-utils-lib} library.
 * 
 * @see <a href=https://github.com/qbicsoftware/portal-utils-lib>portal-utils-lib</a>
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class OverviewUIPortlet extends QBiCPortletUI {

  public static boolean development = false;
  public static boolean v3Registration = false;
  private OpenbisV3APIWrapper v3;
  private IOpenbisCreationController creationController;
  public static String tmpFolder;

  private ConfigurationManager config;

  private IOpenBisClient openbis;

  private final TabSheet tabs = new TabSheet();
  private final String A4B_SPACE = "A4B_TESTING";
  private final String PROJECT_CATEGORY_NAME = "Project (Therapeutic Protein)";
  private Table optionsTable;
  private Table expTable;
  private Button addExperiment;
  private Map<String, String> cellLines;
  private Map<String, String> projectNameToCode = new HashMap<>();

  private OpenbisV3ReadController readController;
  private ComboBox projectSelection;
  private Button resultsButton;

  private Map<String, List<Sample>> experimentCodeToSamples;
  private Map<String, List<ExtendedOpenbisExperiment>> optionToExperiments;

  private final VerticalLayout contextLayout = new VerticalLayout();
  private boolean isAdmin = false;

  private static final Logger LOG = LogManager.getLogger(OverviewUIPortlet.class);

  @Override
  protected Layout getPortletContent(final VaadinRequest request) {
    LOG.info("Generating content for {}", OverviewUIPortlet.class);

    tabs.addStyleName(ValoTheme.TABSHEET_FRAMED);
    tabs.setImmediate(true);
    tabs.addTab(contextLayout, "Context");
    contextLayout.setMargin(true);
    contextLayout.setSpacing(true);
    // setContent(new VerticalLayout(tabs));

    config = ConfigurationManagerFactory.getInstance();
    tmpFolder = config.getTmpFolder();
    String user = getUser();

    if (user == null) {
      contextLayout.addComponent(new Label("User not found. Are you logged in?"));
      return contextLayout;
    }
    // TODO initialize db manager
    try {
      LOG.info("trying to connect to openbis");
      this.openbis = new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(),
          config.getDataSourceUrl());
      this.openbis.login();
      v3 = new OpenbisV3APIWrapper(config.getDataSourceUrl(), config.getDataSourceUser(),
          config.getDataSourcePassword(), user);
    } catch (Exception e) {
      LOG.error(
          "User \"" + user + "\" could not connect to openBIS and has been informed of this.");
      contextLayout.addComponent(new Label(
          "Data Management System could not be reached. Please try again later or contact us."));
      return contextLayout;
    }
    DBConfig mysqlConfig = new DBConfig(config.getMysqlHost(), config.getMysqlPort(),
        config.getMysqlDB(), config.getMysqlUser(), config.getMysqlPass());
    DBManager dbm = new DBManager(mysqlConfig);

    v3 = new OpenbisV3APIWrapper(config.getDataSourceUrl(), config.getDataSourceUser(),
        config.getDataSourcePassword(), user);
    readController = new OpenbisV3ReadController(v3);
    if (v3Registration) {
      creationController = new OpenbisV3CreationController(readController, user, v3);
    } else {
      creationController = new OpenbisCreationController(openbis, readController, user);
    }
    cellLines = v3.getVocabLabelToCode("Q_CELL_LINES");

    // TODO workaround for API v3?
    List<String> spaces = openbis.getUserSpaces(user);
    // TODO move this to config or replace by user role on liferay, once out of testing phase
    if (!spaces.contains(A4B_SPACE)) {
      contextLayout.addComponent(new Label(
          "You are not authorized to create new samples for this project. Please contact our Helpdesk if you think this is an error."));
      return contextLayout;
    }
    // TODO remove project creation options, replace component
    // projSelection = new ProjectInformationComponent(spaces, new HashSet<>());

    projectSelection = new ComboBox();
    projectSelection.setCaption(PROJECT_CATEGORY_NAME);
    projectSelection.setFilteringMode(FilteringMode.CONTAINS);
    projectSelection.setImmediate(true);
    projectSelection.setStyleName(Styles.boxTheme);

    resultsButton = new Button("Results");
    resultsButton.setEnabled(false);

    VerticalLayout left = new VerticalLayout();
    left.setSpacing(true);
    left.addComponent(projectSelection);
    left.addComponent(resultsButton);

    HorizontalLayout topFilters = new HorizontalLayout();
    topFilters.setCaption("Select your Project or filter by Protein Barcode");
    topFilters.addComponent(left);
    topFilters.setSpacing(true);

    VerticalLayout barcodeFilter = new VerticalLayout();
    barcodeFilter.setSpacing(true);
    HorizontalLayout barcodeButtons = new HorizontalLayout();
    barcodeButtons.setSpacing(true);
    Button searchBarcode = new Button("Search");
    Button resetBarcode = new Button("Reset Filters");

    barcodeButtons.addComponent(searchBarcode);
    barcodeButtons.addComponent(resetBarcode);

    TextField barcodeInput = new TextField("QBiC Code");
    barcodeInput.setStyleName(Styles.fieldTheme);

    barcodeFilter.addComponent(barcodeInput);
    barcodeFilter.addComponent(barcodeButtons);

    searchBarcode.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String val = barcodeInput.getValue();
        if (val != null && !val.isEmpty()) {
          Sample s = readController.findSampleByCode(val);
          if (s != null) {
            String id = s.getExperiment().getIdentifier().getIdentifier();

            ExtendedOpenbisExperiment exp = readController.getExperimentWithSamplesByID(id, true);
            Set<ExperimentTemplate> options =
                fillExperimentSampleMapsAndReturnOptions(Arrays.asList(exp));
            fillOptionTable(options);
          } else {
            Styles.notification("Sample not found.",
                "No sample with this barcode was found in the system.", NotificationType.ERROR);
          }
        }
      }
    });

    resetBarcode.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String val = barcodeInput.getValue();
        if (val != null && !val.isEmpty()) {
          fireProjectChange();
        }
      }
    });

    topFilters.addComponent(barcodeFilter);
    contextLayout.addComponent(topFilters);

    optionsTable = new Table("Options based on existing experiments");
    optionsTable.setVisible(false);
    optionsTable.setImmediate(true);
    optionsTable.setSelectable(true);
    optionsTable.setStyleName(Styles.tableTheme);
    optionsTable.addContainerProperty("Name", String.class, null);
    optionsTable.addContainerProperty("Description", String.class, null);
    // optionsTable.addContainerProperty("-", String.class, null);
    contextLayout.addComponent(optionsTable);

    expTable = new Table("Experiment Options");
    expTable.setVisible(false);
    expTable.setImmediate(true);
    expTable.setSelectable(true);
    expTable.setStyleName(Styles.tableTheme);
    expTable.addContainerProperty("Name", String.class, null);
    expTable.addContainerProperty("Description", String.class, null);
    expTable.setColumnWidth("Description", 250);
    expTable.addContainerProperty("Registration Date", String.class, null);
    // expTable.addContainerProperty("Created by", String.class, null);
    expTable.addContainerProperty("Info", Button.class, null);
    contextLayout.addComponent(expTable);

    // expTable.addValueChangeListener(new ValueChangeListener() {
    //
    // @Override
    // public void valueChange(ValueChangeEvent event) {
    // displayProteinInformation();
    // }
    // });

    addExperiment = new Button("Add Experiment");
    addExperiment.setEnabled(false);
    contextLayout.addComponent(addExperiment);
    if (isAdmin) {
      AdminWindow a = new AdminWindow(readController, creationController, dbm, A4B_SPACE);
      contextLayout.addComponent(a.getStartButton());
    }

    addExperiment.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String option = (String) optionsTable.getValue();
        ExtendedOpenbisExperiment exp = (ExtendedOpenbisExperiment) expTable.getValue();
        experimentTemplateToWizardTabs(option, exp);
      }
    });

    expTable.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        addExperiment.setEnabled(expTable.getValue() != null);
      }
    });

    List<String> projects = new ArrayList<String>();
    for (String code : readController.getProjectCodesOfSpace(A4B_SPACE)) {
      String name = dbm.getProjectName("/" + A4B_SPACE + "/" + code);
      if (name != null && name.length() > 0) {
        if (name.length() >= 80)
          name = name.substring(0, 80) + "...";
        name += " (" + code + ")";
      }
      if (name.isEmpty()) {
        name = code;// TODO test
      }
      projects.add(name);
      projectNameToCode.put(name, code);
    }
    projectSelection.addItems(projects);

    projectSelection.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        fireProjectChange();
      }
    });

    optionsTable.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = optionsTable.getValue();
        expTable.select(expTable.getNullSelectionItemId());
        expTable.setVisible(false);
        if (val != null) {
          fillExperimentTable(optionToExperiments.get(val));
        }
      }
    });
    return new VerticalLayout(tabs);
  }

  private void fireProjectChange() {
    resetExperiments();
    boolean projectSelected = projectSelection.getValue() != null;
    resultsButton.setEnabled(projectSelected);
    if (projectSelected) {
      // inputs to check
      String space = A4B_SPACE;
      String existingProject = projectSelection.getValue().toString();
      String projectCode = projectNameToCode.get(existingProject);

      Set<ExperimentTemplate> options = new HashSet<>();
      if (projectCode != null && !projectCode.isEmpty()) {

        List<ExtendedOpenbisExperiment> experiments =
            readController.getExperimentsWithSamplesOfProject(projectCode, true);
        options = fillExperimentSampleMapsAndReturnOptions(experiments);

        String designExpID = ExperimentCodeFunctions.getInfoExperimentID(space, projectCode);
        for (ExtendedOpenbisExperiment e : experiments) {
          if (designExpID.endsWith(e.getExperimentCode())) {
            prepareResultsWindow(e.getSamples());
          }
          // TODO
          // OpenbisExperiment expDesign = readController.getExperimentByID(designExpID);
        }

      }
      fillOptionTable(options);
    }

  }

  private void prepareResultsWindow(List<Sample> projectSamples) {
    if (projectSamples.size() != 1) {
      LOG.error("incorrect amount of samples on project level found: " + projectSamples.size());
    } else {
      Sample s = projectSamples.get(0);
      String notesXML = s.getProperties().get("Q_NOTES");
      List<Note> notes = new ArrayList<Note>();

      if (notesXML != null) {
        try {
          notes = HistoryReader.parseNotes(notesXML).getValue().getNote();
        } catch (JAXBException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      // TODO reference selected TP

      Window subWindow = new Window(" Results");
      subWindow.setWidth("450px");

      VerticalLayout layout = new VerticalLayout();
      layout.setSpacing(true);
      layout.setMargin(true);

      VerticalLayout comments = new VerticalLayout();
      layout.addComponent(comments);
      for (Note n : notes) {
        showNewNote(comments, n);
      }

      TextArea t = new TextArea("Add Findings for this Therapeutic Protein");
      t.setWidth("300px");
      Button addResult = new Button("Add");
      addResult.addClickListener(new Button.ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
          String comment = t.getValue();
          if (!comment.isEmpty()) {
            t.setValue("");
            // use some date format
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            Note note = new Note();
            note.setComment(comment);
            note.setUsername(getUser());
            note.setTime(ft.format(dNow));

            showNewNote(comments, note);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("id", s.getIdentifier().getIdentifier());
            params.put("user", note.getUsername());
            params.put("comment", note.getComment());
            params.put("time", note.getTime());
            openbis.ingest("DSS1", "add-to-xml-note", params);
          }
        }
      });

      layout.addComponent(t);

      Button ok = new Button("Close");
      ok.addClickListener(new ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
          subWindow.close();
        }
      });
      HorizontalLayout buttons = new HorizontalLayout();
      buttons.setSpacing(true);
      buttons.addComponent(addResult);
      buttons.addComponent(ok);
      layout.addComponent(buttons);

      subWindow.setContent(layout);
      // Center it in the browser window
      subWindow.center();
      subWindow.setModal(true);
      subWindow.setIcon(FontAwesome.STAR);
      subWindow.setResizable(false);

      Collection<ClickListener> oldListeners =
          (Collection<ClickListener>) resultsButton.getListeners(ClickEvent.class);
      for (ClickListener old : oldListeners) {
        resultsButton.removeClickListener(old);
      }

      resultsButton.addClickListener(new Button.ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {
          UI.getCurrent().addWindow(subWindow);
        }
      });
    }

  }

  private void showNewNote(VerticalLayout comments, Note n) {
    Label l = new Label();
    l.setCaption(translateName(n.getUsername()) + " (" + translateTime(n.getTime()) + ")");
    l.setValue(n.getComment());
    comments.addComponent(l);
  }

  private String translateName(String username) {
    String res = username;
    try {
      res = getLiferayUser(username);
    } catch (Exception e) {
      LOG.warn("Caught " + e.toString()
          + " trying to find full user name. Probably not running on liferay. Using username instead.");
    }
    if (res == null)
      return username;
    return res;
  }

  public String getLiferayUser(String userID) {
    Company company = null;
    long companyId = 1;
    String userString = "";
    try {
      String webId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID);
      company = CompanyLocalServiceUtil.getCompanyByWebId(webId);
      companyId = company.getCompanyId();
      LOG.debug(
          String.format("Using webId %s and companyId %d to get Portal User", webId, companyId));
    } catch (PortalException | SystemException e) {
      LOG.error("liferay error, could not retrieve companyId. Trying default companyId, which is "
          + companyId, e.getStackTrace());
    }

    User user = null;
    try {
      user = UserLocalServiceUtil.getUserByScreenName(companyId, userID);
    } catch (PortalException | SystemException e) {
    }

    if (user == null) {
      LOG.warn(String.format("Openbis user %s appears to not exist in Portal", userID));
      userString = userID;
    } else {
      String fullname = user.getFullName();
      return fullname;
//      String email = user.getEmailAddress();
//      userString += ("<a href=\"mailto:");
//      userString += (email);
//      userString += ("\" style=\"color: #0068AA; text-decoration: none\">");
//      userString += (fullname);
//      userString += ("</a>");
    }
    return userString;
  }

  private String translateTime(String time) {
    String res = time.replace("T", " ");
    res = res.split("\\.")[0];
    return res;
  }

  private Set<ExperimentTemplate> fillExperimentSampleMapsAndReturnOptions(
      List<ExtendedOpenbisExperiment> experiments) {
    Set<ExperimentTemplate> options = new HashSet<>();

    experimentCodeToSamples = new HashMap<>();
    optionToExperiments = new HashMap<>();
    for (ExtendedOpenbisExperiment e : experiments) {
      if (e.getSamples().size() > 0) {
        experimentCodeToSamples.put(e.getExperimentCode(), e.getSamples());
        for (ExperimentTemplate template : getPossibleTemplatesForExperimentType(e.getType(), e)) {
          if (optionToExperiments.containsKey(template.getName())) {
            optionToExperiments.get(template.getName()).add(e);
          } else {
            optionToExperiments.put(template.getName(), new ArrayList<>(Arrays.asList(e)));
          }
        }
        options.addAll(getPossibleTemplatesForExperimentType(e.getType(), e));
      }
    }
    return options;
  }

  protected void fillExperimentTable(List<ExtendedOpenbisExperiment> exps) {
    expTable.removeAllItems();
    expTable.setPageLength(exps.size());
    expTable.setVisible(true);

    for (ExtendedOpenbisExperiment e : exps) {
      List<Object> row = new ArrayList<>();
      row.add(e.getUserFriendlyNameOrCode());
      row.add(getDescriptionOfExperiment(e));
      row.add(e.getRegistrationDateString());
      // row.add(e.getRegistrator());
      Button info = new Button();
      info.setEnabled(false);
      info.setVisible(false);
      if (e.getType().equals(ExperimentType.Q_SAMPLE_PREPARATION)) {
        info = createInfoButtonAndPopup(e);
      }
      row.add(info);

      expTable.addItem(row.toArray(new Object[row.size()]), e);
      if (exps.size() == 1) {
        expTable.setValue(e);
      }
    }
  }

  private Button createInfoButtonAndPopup(ExtendedOpenbisExperiment e) {
    Button info = new Button();
    info.setWidth("35");
    info.setIcon(FontAwesome.CLIPBOARD);

    Map<String, String> propertyTranslation =
        readController.getPropertiesOfExperimentType(e.getType());

    info.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        createExperimentInfoWindow(e);
      }

      private void createExperimentInfoWindow(ExtendedOpenbisExperiment e) {
        Window subWindow = new Window(" Protein Information");
        subWindow.setWidth("600px");

        SampleInfoComponent sc = new SampleInfoComponent(e, propertyTranslation);
        sc.getCloseButton().addClickListener(new ClickListener() {

          @Override
          public void buttonClick(ClickEvent event) {
            subWindow.close();
          }
        });
        subWindow.setContent(sc);
        // Center it in the browser window
        subWindow.center();
        subWindow.setModal(true);
        subWindow.setIcon(FontAwesome.CLIPBOARD);
        subWindow.setResizable(false);
        UI.getCurrent().addWindow(subWindow);
      }
    });
    return info;
  }

  protected void fillOptionTable(Set<ExperimentTemplate> options) {
    optionsTable.removeAllItems();
    optionsTable.setPageLength(options.size());
    optionsTable.setVisible(true);

    for (ExperimentTemplate e : options) {
      List<Object> row = new ArrayList<>();
      row.add(e.getName());
      row.add(e.getDescription());
      optionsTable.addItem(row.toArray(new Object[row.size()]), e.getName());
    }
    // styleTable(optionsTable);
  }

  private void styleTable(Table t) {
    String base = "blue-hue";
    List<String> styles = new ArrayList<String>();
    for (int i = 1; i < 7; i++)
      styles.add(base + Integer.toString(i));

    // Set cell style generator
    t.setCellStyleGenerator(new Table.CellStyleGenerator() {

      @Override
      public String getStyle(Table source, Object itemId, Object propertyId) {
        if (itemId.equals("Add protein information")) {
          return styles.get(5);
        }
        return styles.get(0);
      }

    });
  }

  protected void resetExperiments() {
    optionsTable.setVisible(false);
    expTable.setVisible(false);
  }

  private void experimentTemplateToWizardTabs(String option, ExtendedOpenbisExperiment exp) {

    tabs.removeAllComponents();
    tabs.addTab(contextLayout, "Context");
    String space = A4B_SPACE;
    String projectName = projectSelection.getValue().toString();
    String projectCode = projectNameToCode.get(projectName);
    RegistrationView registrationView = new RegistrationView(readController, creationController,
        experimentCodeToSamples, space, projectCode);
    switch (option) {
      case "Add protein information":
        BiologicsCultureCreationView extracts = new BiologicsCultureCreationView(exp, cellLines);
        extracts.setNextStep(registrationView);
        tabs.addTab(extracts, "Biologics creation");
        tabs.addTab(registrationView, "Finish");
        break;
      // case "Add new samples":
      // ProteinCreationView protView = new ProteinCreationView(oldSamples, exp);
      // protView.setNextStep(registrationView);
      // tabs.addTab(protView, "Protein Extraction");
      // tabs.addTab(registrationView, "Finish");
      // break;
      case "Deglycosylation of proteins":
        DeglycosylationView deglycView =
            new DeglycosylationView(exp, readController, creationController, space, projectCode);
        // deglycView.setNextStep(registrationView);
        tabs.addTab(deglycView, "Deglycosylation");
        break;
      case "Top-down proteomics":
        TopDownMSView topDownView =
            new TopDownMSView(exp, readController, creationController, space, projectCode);
        // topDownView.setNextStep(registrationView);
        tabs.addTab(topDownView, "Top-down");
        break;
      case "Bottom-up proteomics":
        BottomUpMSView bottomUpView =
            new BottomUpMSView(exp, readController, creationController, space, projectCode);
        // bottomUpView.setNextStep(registrationView);
        tabs.addTab(bottomUpView, "Bottom-up");
        break;
      default:
        LOG.error("No forms could be created for experiment template " + option
            + ". This case needs to be implemented.");
        Styles.notification("Option not available.",
            "This experiment option is not available at the moment.", NotificationType.DEFAULT);
        return;
    }
    initWizard();
  }

  // add tabs to each wizard tab and activates next tab
  private void initWizard() {
    int count = tabs.getComponentCount();
    for (int i = 0; i < count; i++) {
      Component t = tabs.getTab(i).getComponent();
      if (t instanceof IWizardStep) {
        IWizardStep w = (IWizardStep) t;
        w.setTabs(tabs);
      }
    }
    IWizardStep w = (IWizardStep) tabs.getTab(1).getComponent();
    w.activate();
  }

  private List<Sample> filterByProperty(List<Sample> samples, String type, Set<String> filters) {
    if (type == null) {
      return samples;
    }
    List<Sample> res = new ArrayList<>();
    for (Sample s : samples) {
      String val = s.getProperties().get(type);
      if (filters.contains(val)) {
        res.add(s);
      }
    }
    return res;
  }

  private List<Sample> filterByParentType(List<Sample> samples, SampleType type,
      boolean strictExlusion) {
    if (type == null) {
      return samples;
    }
    List<Sample> res = new ArrayList<>();
    for (Sample s : samples) {
      // at least one parent of wanted type
      boolean lenient = false;
      // all parents of wanted type
      boolean strict = true;
      for (Sample p : s.getParents()) {
        boolean correctParent = type.toString().equals(p.getType().getCode());
        lenient |= correctParent;
        strict &= correctParent;
      }
      if (strict || (lenient && !strictExlusion)) {
        res.add(s);
      }
    }
    return res;
  }

  private List<ExperimentTemplate> getPossibleTemplatesForExperimentType(ExperimentType expType,
      ExtendedOpenbisExperiment previousLevel) {
    List<ExperimentTemplate> templates = new ArrayList<>();
    // tissues + proteins
    templates.add(new ExperimentTemplate("Add protein information",
        "Add new samples for this biologic. (only UKE)", ExperimentType.Q_EXPERIMENTAL_DESIGN)
            .addFilter("Q_NCBI_ORGANISM", "10029"));
    // proteins
    // templates.add(new ExperimentTemplate("Add new samples",
    // "Create new samples stemming from existing biologics.", ExperimentType.Q_SAMPLE_EXTRACTION)
    // .addFilter("Q_PRIMARY_TISSUE", "CELL_LINE"));
    // N-glycans and deglycosylated proteins
    templates.add(new ExperimentTemplate("Deglycosylation of proteins",
        "Creates N-glycan and deglycosylated protein samples from protein samples.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS")
            .addParentTypeFilter(SampleType.Q_BIOLOGICAL_SAMPLE, false));
    // Mass Spec Runs
    templates.add(new ExperimentTemplate("Top-down proteomics",
        "Measures intact protein samples using Mass Spectrometry.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS")
            .addParentTypeFilter(SampleType.Q_BIOLOGICAL_SAMPLE, false));
    // Mass Spec Runs
    templates.add(new ExperimentTemplate("Bottom-up proteomics",
        "Measures peptide mixtures of digested proteins using Mass Spectrometry.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS")
            .addParentTypeFilter(SampleType.Q_BIOLOGICAL_SAMPLE, false));
    List<ExperimentTemplate> res = new ArrayList<>();
    for (ExperimentTemplate t : templates) {
      if (expType.equals(t.getParentExperimentType())) {
        List<Sample> filteredByProp = filterByProperty(previousLevel.getSamples(),
            t.getFilterCode(), new HashSet<String>(Arrays.asList(t.getFilterValue())));
        List<Sample> filtered = filterByParentType(filteredByProp, t.getParentFilterType(), false);
        if (filtered.size() > 0) {
          res.add(t);
        }
      }
    }
    return res;
  }

  private String getUser() {
    String res = "";
    if (PortalUtils.isLiferayPortlet()) {
      LOG.info("Prototype Portlet (wizard 2.0) is running on Liferay and user is logged in.");
      res = PortalUtils.getUser().getScreenName();
    } else {
      if (development) {
        LOG.warn("Checks for local dev version successful. User is granted admin status.");
        res = "admin";
        isAdmin = true;
        LOG.warn("User is connected to: " + config.getDataSourceUrl());
      } else {
        LOG.info("User \"" + res + "\" not found. Probably running on Liferay and not logged in.");
        return null;
      }
    }
    return res;
  }

  public String getDescriptionOfExperiment(ExtendedOpenbisExperiment e) {
    String res = "";
    List<Sample> samples = e.getSamples();
    switch (e.getType()) {
      case Q_EXPERIMENTAL_DESIGN:
        Set<String> species = new HashSet<>();
        for (Sample s : samples) {
          String translated =
              v3.translateVocabCode(s.getProperties().get("Q_NCBI_ORGANISM"), "Q_NCBI_TAXONOMY");
          species.add(translated);
        }
        res = "Sources: " + StringUtils.join(species, ", ");
        break;
      // TODO pooled labeled samples
      case Q_SAMPLE_EXTRACTION:
        Set<String> tissues = new HashSet<>();
        String detailedTissue = "";
        for (Sample s : samples) {
          String translated =
              v3.translateVocabCode(s.getProperties().get("Q_PRIMARY_TISSUE"), "Q_PRIMARY_TISSUES");
          tissues.add(translated);
          detailedTissue = s.getProperties().get("Q_TISSUE_DETAILED");
        }
        String tissString = StringUtils.join(tissues, ", ");
        if (tissString.equals("Cell Line") && !detailedTissue.isEmpty()) {
          res = detailedTissue + " cells";
        } else {
          res = tissString + " extracts";
        }
        break;
      // TODO pooled labeled samples and digestions
      case Q_SAMPLE_PREPARATION:
        Set<String> molecules = new HashSet<>();
        for (Sample s : samples) {
          String translated =
              v3.translateVocabCode(s.getProperties().get("Q_SAMPLE_TYPE"), "Q_SAMPLE_TYPES");
          molecules.add(translated);
        }
        res = StringUtils.join(molecules, ", ");
        break;
      case Q_MHC_LIGAND_EXTRACTION:
        res = "MHC ligands";
        break;
      default:
        break;
    }
    return res;
  }
}
