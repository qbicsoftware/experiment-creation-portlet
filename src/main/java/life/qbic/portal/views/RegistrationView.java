package life.qbic.portal.views;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portlet.openbis.OpenbisV3CreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;

public class RegistrationView extends ARegistrationView implements IWizardStep {

  private static final Logger logger = LogManager.getLogger(RegistrationView.class);

  // private Table summary;

  public RegistrationView(OpenbisV3ReadController readController,
      OpenbisV3CreationController controller, Map<String, List<Sample>> experimentCodeToSamples,
      String space, String project) {
    super(readController, controller, space, project);
    //
    // summary = new Table("Summary");
    // summary.addContainerProperty("Type", String.class, null);
    // summary.addContainerProperty("Samples", String.class, null);
    // summary.setStyleName(Styles.tableTheme);
    // summary.setPageLength(samplesToRegister.size());
    //
    // Button register = new Button("Save new Experiments");
    // download = new Button("Download Barcodes");
    // download.setEnabled(false);
    //
    // ProgressBar bar = new ProgressBar();
    // bar.setVisible(false);
    //
    // Label info = new Label();
    // info.setVisible(false);
    //
    // addComponent(summary);
    // addComponent(register);
    // addComponent(info);
    // addComponent(bar);
    // addComponent(download);

    // RegistrationView view = this;
    //
    // register.addClickListener(new Button.ClickListener() {
    //
    // @Override
    // public void buttonClick(ClickEvent event) {
    // List<List<ISampleBean>> samples = new ArrayList<>(samplesToRegister.values());
    // System.out.println(samples);
    // preRegTaskManager.resetAndAlignBarcodeCounter();
    // preRegTaskManager.createNewCodesForExperimentsAndSamples(samplesToRegister);
    // System.out.println(samples);
    //
    // controller.registerProjectWithExperimentsAndSamplesBatchWise(samples,
    // "project description placeholder", samplesToRegister.keySet(), bar, info,
    // new RegisteredToOpenbisReadyRunnable(view), new HashMap<>(), false);
    // }
    // });
    super.initViewComponents();
  }

  @Override
  public void activate() {
    super.activate();
    nextButton.setVisible(false);


    setSummaryAndEnableRegistration();//TODO set reg enabled, or put both in abstract parent class
    // for (PreliminaryOpenbisExperiment exp : samplesToRegister.keySet()) {
    // List<Object> row = new ArrayList<Object>();
    //
    // row.add(exp.getProperties().get("Q_SECONDARY_NAME"));
    // row.add(Integer.toString(samplesToRegister.get(exp).size()));
    //
    // summary.addItem(row.toArray(new Object[row.size()]), exp);
    // }
  }

  @Override
  public void registrationDone(String errors) {
    if (errors.isEmpty()) {
      logger.info("Sample registration complete!");
      Styles.notification("Registration complete!", "Registration of samples complete.",
          NotificationType.SUCCESS);
      register.setEnabled(false);

      //TODO these are null
//      downloadTSV.setEnabled(true);
//      downloadTSV.setVisible(true);
    } else {
      String feedback = "Sample registration could not be completed. Reason: " + errors;
      logger.error(feedback);
      Styles.notification("Registration failed!", feedback, NotificationType.ERROR);
    }
  }
  //
  // private void prepareBarcodeCounter() {
  // String base = projectCode + "001A";
  // firstFreeBarcode = base + SampleCodeFunctions.checksum(base);
  // for (String code : experimentCodesToSamples.keySet()) {
  // String[] split = code.split(projectCode + "E");
  // if (code.startsWith(projectCode + "E") && split.length > 1) {
  // int num = 0;
  // try {
  // num = Integer.parseInt(split[1]);
  // } catch (Exception e2) {
  // }
  // if (firstFreeExperimentID <= num)
  // firstFreeExperimentID = num + 1;
  // }
  // }
  // List<Sample> samples = experimentCodesToSamples.values().stream().flatMap(List::stream)
  // .collect(Collectors.toList());
  //
  // for (Sample s : samples) {
  // String code = s.getCode();
  // if (SampleCodeFunctions.isQbicBarcode(code)) {
  // if (SampleCodeFunctions.compareSampleCodes(firstFreeBarcode, code) <= 0) {
  // firstFreeBarcode = SampleCodeFunctions.incrementSampleCode(code);
  // }
  // } else if (s.getType().toString().equals((SampleType.Q_BIOLOGICAL_ENTITY.toString()))) {
  // int num = Integer.parseInt(s.getCode().split("-")[1]);
  // if (num >= firstFreeEntityID)
  // firstFreeEntityID = num + 1;
  // }
  // }
  // }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  protected void prepareBarcodeDownload(List<List<ISampleBean>> samples) {
    // TODO Auto-generated method stub
    
  }
}
