package life.qbic.portal.portlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.model.ExperimentTemplate;
import life.qbic.portal.model.ExtendedOpenbisExperiment;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.portal.views.BottomUpMSView;
import life.qbic.portal.views.DeglycosylationView;
import life.qbic.portal.views.IWizardStep;
import life.qbic.portal.views.BiologicsCultureCreationView;
import life.qbic.portal.views.ProteinCreationView;
import life.qbic.portal.views.RegistrationView;
import life.qbic.portal.views.TopDownMSView;
import life.qbic.portlet.components.ProjectInformationComponent;
import life.qbic.portlet.openbis.OpenbisV3APIWrapper;
import life.qbic.portlet.openbis.OpenbisV3CreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;

/**
 * Entry point for portlet project-overview-portlet. This class derives from {@link QBiCPortletUI},
 * which is found in the {@code portal-utils-lib} library.
 * 
 * @see <a href=https://github.com/qbicsoftware/portal-utils-lib>portal-utils-lib</a>
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class OverviewUIPortlet extends QBiCPortletUI {

  public static boolean development = true;
  public static boolean v3API = true;
  private OpenbisV3APIWrapper v3;
  OpenbisV3CreationController creationController;
  public static String MSLabelingMethods;
  public static String tmpFolder;

  private ConfigurationManager config;

  private IOpenBisClient openbis;

  private final TabSheet tabs = new TabSheet();
  private Table optionsTable;
  private Table expTable;
  private Button addExperiment;
  private Map<String, String> cellLines;
  private Map<String, String> species;

  OpenbisV3ReadController readController;
  ProjectInformationComponent projSelection;

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

    v3 = new OpenbisV3APIWrapper(config.getDataSourceUrl(), config.getDataSourceUser(),
        config.getDataSourcePassword(), user);
    readController = new OpenbisV3ReadController(v3);
    creationController = new OpenbisV3CreationController(readController, user, v3);
    species = v3.getVocabLabelToCode("Q_NCBI_TAXONOMY");
    cellLines = v3.getVocabLabelToCode("Q_CELL_LINES");

    projSelection =
        new ProjectInformationComponent(readController.getSpaceNames(), new HashSet<>());
    HorizontalLayout topFilters = new HorizontalLayout();
    topFilters.setCaption("Select your Project or filter by Barcode");
    topFilters.addComponent(projSelection);
    topFilters.setSpacing(true);

    VerticalLayout barcodeFilter = new VerticalLayout();
    barcodeFilter.setSpacing(true);
    HorizontalLayout barcodeButtons = new HorizontalLayout();
    barcodeButtons.setSpacing(true);
    Button searchBarcode = new Button("Search");
    Button resetBarcode = new Button("Reset Filter");

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

            ExtendedOpenbisExperiment exp = readController.getExperimentWithSamplesByID(id);
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
    expTable.addContainerProperty("Samples", Integer.class, null);
    contextLayout.addComponent(expTable);

    addExperiment = new Button("Add Experiment");
    addExperiment.setEnabled(false);
    contextLayout.addComponent(addExperiment);

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

    projSelection.getSpaceBox().addValueChangeListener(new ValueChangeListener() {

      public void valueChange(ValueChangeEvent event) {
        projSelection.resetProjects();
        resetExperiments();

        String space = projSelection.getSpaceCode();
        if (space != null) {
          List<String> projects = new ArrayList<String>();
          for (String c : readController.getProjectCodesOfSpace(space)) {
            String code = c;
            // String name = dbm.getProjectName("/" + space + "/" + code);
            // if (name != null && name.length() > 0) {
            // if (name.length() >= 80)
            // name = name.substring(0, 80) + "...";
            // code += " (" + name + ")";
            // }
            projects.add(code);
          }

          projSelection.addProjects(projects);
          projSelection.enableProjectBox(true);
          projSelection.setVisible(true);
        }
      }
    });

    projSelection.getSpaceBox().setValue("A4B_TESTING");

    projSelection.getProjectBox().addValueChangeListener(new ValueChangeListener() {

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

    // inputs to check
    String space = (String) projSelection.getSpaceBox().getValue();
    String existingProject = projSelection.getSelectedProject().toUpperCase();

    // String existingProject = (String) projSelection.getProjectBox().getValue();


    Set<ExperimentTemplate> options = new HashSet<>();
    if (space != null && !space.isEmpty()) {
      // space is set
      if (existingProject != null && !existingProject.isEmpty()) {
        // known project selected, will deactivate generation
        projSelection.tryEnableCustomProject("");

        options = fillExperimentSampleMapsAndReturnOptions(
            readController.getExperimentsWithSamplesOfProject(existingProject));

        String designExpID = ExperimentCodeFunctions.getInfoExperimentID(space, existingProject);
        OpenbisExperiment expDesign = readController.getExperimentByID(designExpID);

      } else {
        // can create new project
        projSelection.getProjectField().setEnabled(true);
      }
    }
    fillOptionTable(options);
  }

  private Set<ExperimentTemplate> fillExperimentSampleMapsAndReturnOptions(
      List<ExtendedOpenbisExperiment> experiments) {
    Set<ExperimentTemplate> options = new HashSet<>();

    experimentCodeToSamples = new HashMap<>();
    optionToExperiments = new HashMap<>();
    for (ExtendedOpenbisExperiment e : experiments) {
      if (e.getSamples().size() > 0) {
        experimentCodeToSamples.put(e.getExperimentCode(), e.getSamples());
        for (ExperimentTemplate template : getPossibleTemplatesForExperimentType(e.getType(),
            e.getSamples())) {
          if (optionToExperiments.containsKey(template.getName())) {
            optionToExperiments.get(template.getName()).add(e);
          } else {
            optionToExperiments.put(template.getName(), new ArrayList<>(Arrays.asList(e)));
          }
        }
        options.addAll(getPossibleTemplatesForExperimentType(e.getType(), e.getSamples()));
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
      row.add(e.getSamples().size());
      expTable.addItem(row.toArray(new Object[row.size()]), e);
      if (exps.size() == 1) {
        expTable.setValue(e);
      }
    }
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
  }

  protected void resetExperiments() {
    optionsTable.setVisible(false);
    expTable.setVisible(false);
  }

  private void experimentTemplateToWizardTabs(String option, ExtendedOpenbisExperiment exp) {
    List<Sample> oldSamples = experimentCodeToSamples.get(exp.getExperimentCode());

    tabs.removeAllComponents();
    tabs.addTab(contextLayout, "Context");
    String space = (String) projSelection.getSpaceBox().getValue();
    String project = projSelection.getSelectedProject().toUpperCase();
    RegistrationView registrationView = new RegistrationView(readController, creationController,
        experimentCodeToSamples, space, project);
    switch (option) {
      case "Add new medical products":
        BiologicsCultureCreationView extracts =
            new BiologicsCultureCreationView(oldSamples, cellLines);
        extracts.setNextStep(registrationView);
        tabs.addTab(extracts, "Biologics creation");
        tabs.addTab(registrationView, "Finish");
        break;
      case "Add new samples":
        ProteinCreationView protView = new ProteinCreationView(oldSamples, exp);
        protView.setNextStep(registrationView);
        tabs.addTab(protView, "Protein Extraction");
        tabs.addTab(registrationView, "Finish");
        break;
      case "Deglycosylation of proteins":
        DeglycosylationView deglycView =
            new DeglycosylationView(oldSamples, readController, creationController, space, project);
        // deglycView.setNextStep(registrationView);
        tabs.addTab(deglycView, "Deglycosylation");
        break;
      case "Top-down proteomics":
        TopDownMSView topDownView =
            new TopDownMSView(oldSamples, readController, creationController, space, project);
        // topDownView.setNextStep(registrationView);
        tabs.addTab(topDownView, "Top-down");
        break;
      case "Bottom-up proteomics":
        BottomUpMSView bottomUpView =
            new BottomUpMSView(oldSamples, readController, creationController, space, project);
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
    List<Sample> res = new ArrayList<>();
    for (Sample s : samples) {
      String val = s.getProperties().get(type);
      if (filters.contains(val)) {
        res.add(s);
      }
    }
    return res;
  }

  private List<ExperimentTemplate> getPossibleTemplatesForExperimentType(ExperimentType expType,
      List<Sample> previousLevel) {
    List<ExperimentTemplate> templates = new ArrayList<>();
    // tissues + proteins
    templates.add(new ExperimentTemplate("Add new medical products",
        "Create new samples (and aliquots) of new biologics (e.g. mABs).",
        ExperimentType.Q_EXPERIMENTAL_DESIGN).addFilter("Q_NCBI_ORGANISM", "10029"));
    // proteins
    templates.add(new ExperimentTemplate("Add new samples",
        "Create new samples stemming from existing biologics.", ExperimentType.Q_SAMPLE_EXTRACTION)
            .addFilter("Q_PRIMARY_TISSUE", "CELL_LINE"));
    // N-glycans and deglycosylated proteins
    templates.add(new ExperimentTemplate("Deglycosylation of proteins",
        "Creates N-glycan and deglycosylated protein samples from protein samples.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS"));
    // Mass Spec Runs
    templates.add(new ExperimentTemplate("Top-down proteomics",
        "Measures intact protein samples using Mass Spectrometry.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS"));
    // Mass Spec Runs
    templates.add(new ExperimentTemplate("Bottom-up proteomics",
        "Measures peptide mixtures of digested proteins using Mass Spectrometry.",
        ExperimentType.Q_SAMPLE_PREPARATION).addFilter("Q_SAMPLE_TYPE", "PROTEINS")
            .addFilter("Q_SAMPLE_TYPE", "PROTEINS"));
    List<ExperimentTemplate> res = new ArrayList<>();
    for (ExperimentTemplate t : templates) {
      if (expType.equals(t.getParentExperimentType())) {
        if (t.getFilterCode() == null) {
          res.add(t);
        } else {
          int filtered = filterByProperty(previousLevel, t.getFilterCode(),
              new HashSet<String>(Arrays.asList(t.getFilterValue()))).size();
          if (filtered > 0) {
            res.add(t);
          }
        }
      }
      // TODO if no experiment is available, do something? or new projects should automatically
      // create the experimental design (species)
      // if (res.isEmpty()) {
      // res.add(new ExperimentTemplate("Add new medical products",
      // "Create new samples (and aliquots) of new biologics (e.g. mABs).", null));
      // }
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
