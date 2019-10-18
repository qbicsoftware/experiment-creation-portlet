package life.qbic.portlet.openbis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;

public class RegisterableExperiment {

  private String code;
  private String type;
  private Map<String, Object> properties;

  public RegisterableExperiment(String code, ExperimentType type,
      Map<String, Object> metadata) {
    this.code = code;
    this.type = type.toString();
    this.properties = metadata;
  }

  public String getType() {
    return type;
  }

  public String getCode() {
    return code;
  }


  public Map<String, Object> getProperties() {
    return properties;
  }

  public Map<String, String> getStringProperties() {
    Map<String, String> res = new HashMap<>();
    for (String key : properties.keySet()) {
      res.put(key, properties.get(key).toString());
    }
    return res;
  }

}
