package life.qbic.portal.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.experiments.OpenbisExperiment;

public class ExtendedOpenbisExperiment extends OpenbisExperiment {

  private List<Sample> samples;
  private String registrator;
  private Date registrationDate;

  public ExtendedOpenbisExperiment(String code, ExperimentType type, Map<String, Object> props,
      List<Sample> samples, Date registrationDate, String registrator) {
    super(code, type, props);
    this.samples = samples;
    this.registrator = registrator;
    this.registrationDate = registrationDate;
  }

  public String getUserFriendlyNameOrCode() {
    Map<String, Object> props = getMetadata();
    String name = (String) props.get("Q_SECONDARY_NAME");
    String info = (String) props.get("Q_ADDITIONAL_INFO");
    if (name != null && !name.isEmpty())
      return name;
    if (info != null && !info.isEmpty())
      return info;
    return getExperimentCode();
  }

  public List<Sample> getSamples() {
    return samples;
  }

  public String getRegistrator() {
    return registrator;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public String getRegistrationDateString() {
    SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
    if (registrationDate != null)
      return dt1.format(registrationDate);
    return "unknown";
  }

}
