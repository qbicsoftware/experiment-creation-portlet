package life.qbic.portal.steps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.components.Uploader;
import life.qbic.portal.model.ExtendedOpenbisExperiment;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.portal.model.SampleSummaryBean;
import life.qbic.portal.parsing.ExperimentalDesignType;
import life.qbic.portal.parsing.IExperimentalDesignReader;
import life.qbic.portal.parsing.SamplePreparator;
import life.qbic.portal.parsing.VocabularyValidator;
import life.qbic.portal.portlet.OverviewUIPortlet;
import life.qbic.portlet.components.MissingInfoComponent;
import life.qbic.portlet.components.SampleInfoComponent;
import life.qbic.portlet.openbis.IOpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;
import life.qbic.utils.TimeUtils;

public abstract class ImportRegisterView extends ARegistrationView {

  private VerticalLayout infos;
  private Upload upload;
  private MissingInfoComponent questionaire;
  private List<List<ISampleBean>> samples;
  private ExperimentalDesignType designType;
  private SamplePreparator preparator;
  protected List<Map<String, Object>> metadataList;
  protected List<PreliminaryOpenbisExperiment> msProperties;
  protected List<PreliminaryOpenbisExperiment> protProperties;
  private List<String> barcodes;
  private Uploader uploader;

  private static final Logger logger = LogManager.getLogger(ImportRegisterView.class);

  public ImportRegisterView(ExperimentalDesignType type, ExtendedOpenbisExperiment previousLevel,
      OpenbisV3ReadController readController, IOpenbisCreationController controller, String space,
      String project) {
    super(readController, controller, space, project);
    barcodes = collectBarcodes(previousLevel.getSamples());
    designType = type;

    infos = new VerticalLayout();
    infos.setCaption("Format Information");

    infos.addComponent(createTSVDownloadComponent(designType, barcodes));

    uploader = new Uploader(OverviewUIPortlet.tmpFolder);
    upload = new Upload("Upload your file here", uploader);
    upload.setButtonCaption("Upload");
    initUploadListeners(uploader);

    Button proteinInfo = new Button("Protein Information");

    Map<String, String> propertyTranslation =
        readController.getPropertiesOfExperimentType(previousLevel.getType());

    proteinInfo.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        Window subWindow = new Window(" Protein Information");
        subWindow.setWidth("600px");

        SampleInfoComponent sc = new SampleInfoComponent(previousLevel, propertyTranslation);
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

    addComponent(proteinInfo);
    HorizontalLayout optionsInfo = new HorizontalLayout();
    optionsInfo.addComponent(infos);

    // design type selection and info
    addComponent(optionsInfo);

    addComponent(upload);

    // preview = new Button("Preview Sample Graph");
    // preview.setEnabled(false);
    // addComponent(preview);

    // missing info input layout
    questionaire = new MissingInfoComponent();
    addComponent(questionaire);

    super.initViewComponents();

    setMargin(true);
    setSpacing(true);

  }

  private List<String> collectBarcodes(List<Sample> samples) {
    List<String> res = new ArrayList<>();
    for (Sample s : samples) {
      res.add(s.getCode());
    }
    return res;
  }

  @Override
  public void activate() {
    super.activate();
    nextButton.setVisible(false);
  }

  private Component createTSVDownloadComponent(ExperimentalDesignType type, List<String> barcodes) {
    VerticalLayout v = new VerticalLayout();
    v.setSpacing(true);
    Label l = new Label(type.getDescription());
    l.setWidth("300px");
    v.addComponent(l);
    Button button = new Button("Download Template");
    v.addComponent(button);

    File example = new File(
        getClass().getClassLoader().getResource("examples/" + type.getFileName()).getFile());

    File downloadFile = example;
    try {
      downloadFile = createFileWithBarcodes(example, barcodes);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logger.warn(
          "could not add barcodes to example file. download will be standard example template.");
    }
    FileDownloader tsvDL = new FileDownloader(new FileResource(downloadFile));
    tsvDL.extend(button);
    return v;
  }

  public static void main(String[] args) {
    List<String> tests =
        new ArrayList<>(Arrays.asList("xexample.rar", "example2.rar", "examplkjbnjasbda"));
    for (String t : tests) {
      System.out.println(t.replaceAll("example.*\\.rar", ""));
    }
  }

  private File createFileWithBarcodes(File example, List<String> barcodes) throws IOException {
    Path path = example.toPath();
    Charset charset = StandardCharsets.UTF_8;

    String content = new String(Files.readAllBytes(path), charset);

    String header = content.split("\r")[0];

    content = content.replace(header, "");

    String res = header + "\n";
    for (String barcode : barcodes) {
      // String line = content.replaceAll("example", barcode + "example");
      String line = content.replaceAll("barcode_placeholder", barcode).trim();
      res += line + "\n";
    }

    System.out.println(res);
    res = res.replaceAll("example.*\\.rar", "");
    System.out.println(res);
    String tmpPath = Paths.get(OverviewUIPortlet.tmpFolder,
        TimeUtils.getCurrentTimestampString() + path.getFileName()).toString();

    FileUtils.writeByteArrayToFile(new File(tmpPath), res.getBytes(charset));

    return new File(tmpPath);
  }

  public List<List<ISampleBean>> getSamples() {
    return samples;
  }

  private void initUploadListeners(Uploader uploader) {
    // Listen for events regarding the success of upload.
    upload.addFailedListener(uploader);
    upload.addSucceededListener(uploader);
    FinishedListener uploadFinListener = new FinishedListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8413963075202260180L;

      public void uploadFinished(FinishedEvent event) {
        // currentDesignTypes = new HashSet<>();
        String uploadError = uploader.getError();
        File file = uploader.getFile();
        resetAfterUpload();
        if (file.getPath().endsWith("up_")) {
          String msg = "No file selected.";
          logger.warn(msg);
          Styles.notification("Failed to read file.", msg, NotificationType.ERROR);
          if (!file.delete())
            logger.error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
        } else {
          if (uploadError == null || uploadError.isEmpty()) {
            String msg = "Upload successful!";
            logger.info(msg);
            try {
              setRegEnabled(false);
              preparator = new SamplePreparator();
              Map<String, Set<String>> experimentTypeVocabularies = new HashMap<>();
              // TODO use
              // experimentTypeVocabularies.put("Q_ANTIBODY", vocabs.getAntibodiesMap().keySet());
              // experimentTypeVocabularies.put("Q_CHROMATOGRAPHY_TYPE",
              // new HashSet<>(vocabs.getChromTypesMap().values()));

              Map<String, String> deviceMap = readController.getVocabLabelsToCodes("Q_MS_DEVICES");
              Map<String, String> enzymeMap =
                  readController.getVocabLabelsToCodes("Q_DIGESTION_PROTEASES");
              Map<String, String> chromTypes =
                  readController.getVocabLabelsToCodes("Q_CHROMATOGRAPHY_TYPES");
              Map<String, String> purificationMethods =
                  readController.getVocabLabelsToCodes("Q_PROTEIN_PURIFICATION_METHODS");
              // Map<String, String> taxMap =
              // openbis.getVocabCodesAndLabelsForVocab("Q_NCBI_TAXONOMY");
              // Map<String, String> tissueMap =
              // openbis.getVocabCodesAndLabelsForVocab("Q_PRIMARY_TISSUES");
              // Map<String, String> deviceMap =
              // openbis.getVocabCodesAndLabelsForVocab("Q_MS_DEVICES");
              // Map<String, String> cellLinesMap =
              // openbis.getVocabCodesAndLabelsForVocab("Q_CELL_LINES");
              // Map<String, String> chromTypes =
              // openbis.getVocabCodesAndLabelsForVocab("Q_CHROMATOGRAPHY_TYPES");
              // Map<String, String> purificationMethods =
              // openbis.getVocabCodesAndLabelsForVocab("Q_PROTEIN_PURIFICATION_METHODS");
              // Map<String, String> antibodiesWithLabels =
              // openbis.getVocabCodesAndLabelsForVocab("Q_ANTIBODY");

              Map<String, String> fractionationTypes =
                  readController.getVocabLabelsToCodes("Q_MS_FRACTIONATION_PROTOCOLS");
              Map<String, String> enrichmentTypes =
                  readController.getVocabLabelsToCodes("Q_MS_ENRICHMENT_PROTOCOLS");
              Map<String, String> lcmsMethods =
                  readController.getVocabLabelsToCodes("Q_MS_LCMS_METHODS");
              Map<String, String> labelingMethods =
                  readController.getVocabLabelsToCodes("Q_LABELING_TYPES");

              experimentTypeVocabularies.put("Q_MS_DEVICE", new HashSet<>(deviceMap.values()));
              experimentTypeVocabularies.put("Q_MS_LCMS_METHOD",
                  new HashSet<>(lcmsMethods.values()));
              experimentTypeVocabularies.put("Q_MS_FRACTIONATION_METHOD",
                  new HashSet<String>(fractionationTypes.values()));
              experimentTypeVocabularies.put("Q_MS_ENRICHMENT_METHOD",
                  new HashSet<String>(enrichmentTypes.values()));
              experimentTypeVocabularies.put("Q_LABELING_METHOD",
                  new HashSet<String>(labelingMethods.values()));
              // TODO
              experimentTypeVocabularies.put("Q_DIGESTION_METHOD", enzymeMap.keySet());
              VocabularyValidator validator = new VocabularyValidator(experimentTypeVocabularies);

              Map<String, Map<String, String>> catToVocabulary = new HashMap<>();


              catToVocabulary.put("LC Column", chromTypes);
              catToVocabulary.put("Sample Cleanup", purificationMethods);
              catToVocabulary.put("MS Device", deviceMap);
              catToVocabulary.put("Fractionation Type", fractionationTypes);
              catToVocabulary.put("Enrichment", enrichmentTypes);
              catToVocabulary.put("Labeling Type", labelingMethods);
              catToVocabulary.put("Labeling/Derivitisation", labelingMethods);

              IExperimentalDesignReader reader = designType.getParser();
              boolean parseGraph = false;

              boolean readSuccess = preparator.processTSV(file, reader, parseGraph);
              boolean vocabValid = true;
              if (readSuccess) {
                msProperties =
                    preparator.getSpecialExperimentsOfType(ExperimentType.Q_MS_MEASUREMENT);
                protProperties =
                    preparator.getSpecialExperimentsOfType(ExperimentType.Q_SAMPLE_PREPARATION);

                metadataList = new ArrayList<>();
                for (PreliminaryOpenbisExperiment e : msProperties) {
                  metadataList.add(e.getProperties());
                }
                for (PreliminaryOpenbisExperiment e : protProperties) {
                  metadataList.add(e.getProperties());
                }
                Map<String, Set<String>> pretransformedProperties = new HashMap<>();

                // TODO
                // pretransformedProperties.put("Fractionation_Enrichment_Placeholder", new
                // HashSet<>(
                // Arrays.asList("Q_MS_FRACTIONATION_METHOD", "Q_MS_ENRICHMENT_METHOD")));

                vocabValid = validator.transformAndValidateExperimentMetadata(metadataList,
                    pretransformedProperties);
                logger.debug(metadataList);

                // TODO
                vocabValid = true;
                Map<String, List<String>> parsedCategoryToValues = preparator
                    .getParsedCategoriesToValues(new ArrayList<String>(Arrays.asList("LC Column",
                        "Sample Cleanup", "MS Device", "Fractionation Type", "Enrichment",
                        "Labeling Type", "Labeling/Derivitisation")));

                Map<String, Set<String>> keyToFields = new HashMap<>();
                keyToFields.put("Q_MS_DEVICE", new HashSet<>(Arrays.asList("MS Device")));
                keyToFields.put("Q_CHROMATOGRAPHY_TYPE", new HashSet<>(Arrays.asList("LC Column")));
                keyToFields.put("Q_MS_ENRICHMENT_METHOD",
                    new HashSet<>(Arrays.asList("Enrichment")));
                keyToFields.put("Q_MS_PURIFICATION_METHOD",
                    new HashSet<>(Arrays.asList("Sample Cleanup")));
                keyToFields.put("Q_MS_FRACTIONATION_METHOD",
                    new HashSet<>(Arrays.asList("Fractionation Type")));
                keyToFields.put("Q_LABELING_METHOD",
                    new HashSet<>(Arrays.asList("Labeling Type", "Labeling/Derivitisation")));
                ValueChangeListener missingInfoFilledListener = new ValueChangeListener() {

                  @Override
                  public void valueChange(ValueChangeEvent event) {
                    boolean infoComplete = questionaire.isValid();

                    if (infoComplete) {
                      for (Map<String, Object> props : metadataList) {
                        for (String key : props.keySet()) {
                          if (keyToFields.containsKey(key)) {
                            for (String val : keyToFields.get(key)) {
                              String entry = (String) props.get(key);
                              String newVal = questionaire.getVocabularyCodeForValue(val, entry);
                              if (newVal != null) {
                                props.put(key, newVal);// TODO test
                              }
                            }
                          }
                        }
                      }
                      logger.debug("after replacement:");
                      logger.debug(metadataList);
                    }
                  }
                };


                questionaire.init(parsedCategoryToValues, catToVocabulary,
                    missingInfoFilledListener);

                // for (ISampleBean b : level) {
                // TSVSampleBean t = (TSVSampleBean) b;
                //
                // String extID = (String) t.getMetadata().get("Q_EXTERNALDB_ID");
                //
                // if (extIDToSample.containsKey(extID)) {
                // existing.add(t);
                // extCodeToBarcode.put(extID, extIDToSample.get(extID).getCode());
                // } else {
                // t.setProject(project);
                // t.setSpace(space);
                // String code = "";
                // Map<String, Object> props = t.getMetadata();
                // switch (t.getType()) {
                // case Q_BIOLOGICAL_ENTITY:
                // code = project + "ENTITY-" + entityNum;
                // String newVal = questionaire.getVocabularyLabelForValue("Species",
                // props.get("Q_NCBI_ORGANISM"));
                // props.put("Q_NCBI_ORGANISM", taxMap.get(newVal));
                // entityNum++;
                // break;
                // case Q_BIOLOGICAL_SAMPLE:
                // try {
                // incrementOrCreateBarcode(project);
                // } catch (TooManySamplesException e) {
                // overflow = true;
                // }
                // code = nextBarcode;
                // newVal = questionaire.getVocabularyLabelForValue("Tissues",
                // props.get("Q_PRIMARY_TISSUE"));
                // props.put("Q_PRIMARY_TISSUE", tissueMap.get(newVal));
                // break;
                // case Q_TEST_SAMPLE:
                // try {
                // incrementOrCreateBarcode(project);
                // } catch (TooManySamplesException e) {
                // overflow = true;
                // }
                // code = nextBarcode;
                // newVal = questionaire.getVocabularyLabelForValue("Analytes",
                // props.get("Q_SAMPLE_TYPE"));
                // props.put("Q_SAMPLE_TYPE", newVal);
                //
              }
              if (readSuccess && vocabValid) {
                List<SampleSummaryBean> summaries = preparator.getSummary();
                for (SampleSummaryBean s : summaries) {
                  // String translation = reverseTaxMap.get(s.getFullSampleContent());
                  String translation = s.getFullSampleContent();
                  if (translation != null)
                    s.setSampleContent(translation);
                }
                Styles.notification("Upload successful",
                    "Experiment was successfully uploaded and read.", NotificationType.SUCCESS);
                handleImportResults(summaries);

              } else {
                if (!readSuccess) {
                  String error = preparator.getError();
                  Styles.notification("Failed to read file.", error, NotificationType.ERROR);
                } else {
                  String error = validator.getError();
                  Styles.notification("Failed to process file.", error, NotificationType.ERROR);
                }
                if (!file.delete())
                  logger.error(
                      "uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
              }
            } catch (IOException | JAXBException e) {
              e.printStackTrace();
              logger.error(e);
            }
          } else {
            // view.showError(error);
            Styles.notification("Failed to upload file.", uploadError, NotificationType.ERROR);
            if (!file.delete())
              logger
                  .error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
          }
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
  }

  @Override
  public void registrationDone(String errors) {
    if (errors.isEmpty()) {
      logger.info("Sample registration complete!");
      Styles.notification("Registration complete!", "Registration of samples complete.",
          NotificationType.SUCCESS);
      register.setEnabled(false);

      downloadTSV.setEnabled(true);
      downloadTSV.setVisible(true);
      finish.setEnabled(true);
    } else {
      String feedback = "Sample registration could not be completed. Reason: " + errors;
      logger.error(feedback);
      Styles.notification("Registration failed!", feedback, NotificationType.ERROR);
    }
  }

  protected void handleImportResults(List<SampleSummaryBean> summaries) {
    samplesToRegister = translateParsedExperiments(preparator);
    setSummaryAndEnableRegistration();
  }

  protected abstract LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> translateParsedExperiments(
      SamplePreparator preparator);

  @Override
  protected void prepareBarcodeDownload(List<List<ISampleBean>> levels) {
    List<String> tsv = preparator.getOriginalTSV();

    StringBuilder builder = new StringBuilder(6000);

    Map<String, String> fileNameToBarcode = new HashMap<String, String>();
    for (List<ISampleBean> samples : levels) {
      for (ISampleBean s : samples) {
        if (s.getType().equals(SampleType.Q_MS_RUN)) {
          Map<String, Object> props = s.getMetadata();
          fileNameToBarcode.put(props.get("File").toString(), s.getCode());
          props.remove("File");
        }
      }
    }
    int filePos = -1;
    for (String line : tsv) {
      String[] splt = line.split("\t");
      if (filePos < 0) {
        filePos = Arrays.asList(splt).indexOf("File Name");// TODO generalize?
        builder.append("QBiC Code\t" + line + "\n");
      } else {
        String file = splt[filePos];
        String code = fileNameToBarcode.get(file);
        builder.append(code + "\t" + line + "\n");
      }
    }

    setTSVWithBarcodes(builder.toString(),
        uploader.getFileNameWithoutExtension() + "_with_barcodes");
  }



}
