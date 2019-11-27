package life.qbic.portal.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import com.vaadin.server.FileDownloader;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Button.ClickEvent;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.portlet.components.ExperimentSummaryTable;
import life.qbic.portlet.openbis.IOpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;
import life.qbic.portlet.openbis.PreRegistrationTaskAdapter;
import life.qbic.portlet.openbis.RegisteredToOpenbisReadyRunnable;

public abstract class ARegistrationView extends AWizardStep {

  protected Button register;
  // private MissingInfoComponent questionaire;
  // protected ExperimentSummaryTable summary;
  protected Label summaryLabel;
  // private Button preview;
  protected Label registerInfo;
  protected ProgressBar bar;
  protected Button downloadTSV;
  protected Button finish;
  private PreRegistrationTaskAdapter preRegTaskManager;
  private IOpenbisCreationController controller;
  protected OpenbisV3ReadController readController;

  public ARegistrationView(OpenbisV3ReadController readController,
      IOpenbisCreationController controller, String space, String project) {
    this.controller = controller;
    this.readController = readController;

    preRegTaskManager = new PreRegistrationTaskAdapter(readController);
    preRegTaskManager.setProject(space, project);
    finish = new Button("Finish");
    finish.setEnabled(false);
  }

  private void initListeners() {
    ARegistrationView view = this;

    register.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        List<List<ISampleBean>> samples = new ArrayList<>(samplesToRegister.values());
        preRegTaskManager.resetAndAlignBarcodeCounter();
        preRegTaskManager.createNewCodesForExperimentsAndSamples(samplesToRegister);

        prepareBarcodeDownload(samples);
        register.setEnabled(false);
        showRegistrationProgress();
        controller.registerProjectWithExperimentsAndSamplesBatchWise(samples,
            "project description placeholder", samplesToRegister.keySet(), bar, registerInfo,
            new RegisteredToOpenbisReadyRunnable(view), new HashMap<>(), false);
      }
    });

    finish.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        goToFirstTab();
      }
    });
  }

  public boolean summaryIsSet() {
    return !summaryLabel.getCaption().isEmpty();
    // return (summary.size() > 0);
  }

  public void resetAfterUpload() {
    summaryLabel.setCaption("");
    summaryLabel.setVisible(false);
    // summary.removeAllItems();
    // summary.setVisible(false);
    registerInfo.setVisible(false);
    bar.setVisible(false);
    if (downloadTSV != null)
      removeComponent(downloadTSV);
  }

  public void setTSVWithBarcodes(String tsvContent, String name) {
    if (downloadTSV != null)
      removeComponent(downloadTSV);
    downloadTSV = new Button("Download Barcodes");
    addComponent(downloadTSV);
    FileDownloader tsvDL = new FileDownloader(PortalUtils.getFileStream(tsvContent, name, "tsv"));
    tsvDL.extend(downloadTSV);
    downloadTSV.setVisible(false);
  }

  protected abstract void prepareBarcodeDownload(List<List<ISampleBean>> samples);

  public void setSummaryAndEnableRegistration() {
    // summary.setSamples(samplesToRegister);
    // summary.setVisible(true);
    summaryLabel.setCaption("Summary");
    String info = getRegSummary(samplesToRegister);
    summaryLabel.setValue(info);
    summaryLabel.setVisible(true);
    setRegEnabled(true);
  }

  private String getRegSummary(
      LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> samplesToRegister) {
    int prot = 0;
    int ms = 0;
    for (PreliminaryOpenbisExperiment e : samplesToRegister.keySet()) {
      int samples = samplesToRegister.get(e).size();
      switch (e.getType()) {
        case Q_SAMPLE_PREPARATION:
          prot += samples;
          break;
        case Q_MS_MEASUREMENT:
          ms += samples;
        default:
          break;
      }
    }
    if (ms > 0) {
      return ms + " mass spectrometry runs are ready to be registered.";
    } else {
      return prot + " protein samples are ready to be registered.";
    }
  }

  public void setRegEnabled(boolean b) {
    register.setEnabled(b);
    register.setVisible(b);
  }

  public void initViewComponents() {
    // preview = new Button("Preview Sample Graph");
    // preview.setEnabled(false);
    // addComponent(preview);

    // summary of imported samples
    // summary = new ExperimentSummaryTable();
    // summary.setVisible(false);
    // addComponent(summary);

    summaryLabel = new Label();
    summaryLabel.setVisible(false);
    addComponent(summaryLabel);

    // sample registration button
    register = new Button("Register All");
    register.setVisible(false);
    addComponent(register);

    // registration progress information
    registerInfo = new Label();
    bar = new ProgressBar();
    registerInfo.setVisible(false);
    bar.setVisible(false);
    addComponent(registerInfo);
    addComponent(bar);
    initListeners();
  }

  public void showRegistrationProgress() {
    bar.setVisible(true);
    registerInfo.setVisible(true);
    buttons.addComponent(finish);
  }

  public abstract void registrationDone(String errors);

}
