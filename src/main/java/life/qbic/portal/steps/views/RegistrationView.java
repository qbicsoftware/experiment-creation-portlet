package life.qbic.portal.steps.views;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.steps.ARegistrationView;
import life.qbic.portal.steps.IWizardStep;
import life.qbic.portlet.openbis.IOpenbisCreationController;
import life.qbic.portlet.openbis.OpenbisV3ReadController;
import life.qbic.utils.TimeUtils;

public class RegistrationView extends ARegistrationView implements IWizardStep {

  private static final Logger logger = LogManager.getLogger(RegistrationView.class);

  // private Table summary;

  public RegistrationView(OpenbisV3ReadController readController,
      IOpenbisCreationController controller, Map<String, List<Sample>> experimentCodeToSamples,
      String space, String project) {
    super(readController, controller, space, project);
    super.initViewComponents();
  }

  @Override
  public void activate() {
    super.activate();
    nextButton.setVisible(false);
    setSummaryAndEnableRegistration();
  }

  @Override
  public void registrationDone(String errors) {
    if (errors.isEmpty()) {
      logger.info("Sample registration complete!");
      Styles.notification("Registration complete!", "Registration of samples complete.",
          NotificationType.SUCCESS);
      register.setEnabled(false);
      finish.setEnabled(true);
    } else {
      String feedback = "Sample registration could not be completed. Reason: " + errors;
      logger.error(feedback);
      Styles.notification("Registration failed!", feedback, NotificationType.ERROR);
    }
  }

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  /**
   * returns barcodes of last level for now
   */
  protected void prepareBarcodeDownload(List<List<ISampleBean>> samples) {
    StringBuilder builder = new StringBuilder(1000);

    int numLevels = samples.size();
    // TODO this is only true if there is only one level of analytes!
    List<ISampleBean> proteins = samples.get(numLevels - 1);

    builder.append("QBiC Code\t" + "Sample Name\t" + "Sample Description" + "\n");

    for (ISampleBean s : proteins) {
      Map<String, Object> props = s.getMetadata();
      builder.append(
          s.getCode() + "\t" + props.get("Q_EXTERNALDB_ID") + "\t" + s.getSecondaryName() + "\n");
    }

    setTSVWithBarcodes(builder.toString(), TimeUtils.getCurrentTimestampString() + "_barcodes");
    downloadTSV.setEnabled(true);
    downloadTSV.setVisible(true);
  }
}
