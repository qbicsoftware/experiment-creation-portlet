package life.qbic.portal.model;

import java.util.Map;
import life.qbic.datamodel.experiments.ExperimentType;

public class PreliminaryOpenbisExperiment {

  private Map<String,String> properties;
  private String code;
  private ExperimentType type;
  
  public PreliminaryOpenbisExperiment(ExperimentType type,
      Map<String, String> props) {
    this.type = type;
    this.properties = props;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ExperimentType getType() {
    return type;
  }

  public void setType(ExperimentType type) {
    this.type = type;
  }
  
  @Override
  public String toString() {
    String res = code+" ("+type+"): "+properties;
    return res;
  }
  
}