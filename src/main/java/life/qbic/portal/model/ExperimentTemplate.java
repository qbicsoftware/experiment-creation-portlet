package life.qbic.portal.model;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.SampleType;

public class ExperimentTemplate {

  private String name;
  private String description;
  private ExperimentType experimentType;
  private SampleType sampleType;
  private String filterCode;
  private String filterValue;
  private ExperimentType parentExperimentType;

  public ExperimentTemplate(String name, String description, ExperimentType experimentType,
      SampleType sampleType) {
    super();
    this.description = description;
    this.name = name;
    this.experimentType = experimentType;
    this.sampleType = sampleType;
  }

  public ExperimentTemplate(String name, String description, ExperimentType parentExpType) {
    this.parentExperimentType = parentExpType;
    this.name = name;
    this.description = description;
  }
  
  public ExperimentType getParentExperimentType() {
    return parentExperimentType;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ExperimentType getExperimentType() {
    return experimentType;
  }

  public void setExperimentType(ExperimentType experimentType) {
    this.experimentType = experimentType;
  }

  public SampleType getSampleType() {
    return sampleType;
  }

  public void setSampleType(SampleType sampleType) {
    this.sampleType = sampleType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((experimentType == null) ? 0 : experimentType.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((sampleType == null) ? 0 : sampleType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ExperimentTemplate other = (ExperimentTemplate) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (experimentType != other.experimentType)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (sampleType != other.sampleType)
      return false;
    return true;
  }

  public ExperimentTemplate addFilter(String code, String value) {
    this.filterCode = code;
    this.filterValue = value;
    return this;
  }

  public String getFilterCode() {
    return filterCode;
  }

  public void setFilterCode(String filterCode) {
    this.filterCode = filterCode;
  }

  public String getFilterValue() {
    return filterValue;
  }

  public void setFilterValue(String filterValue) {
    this.filterValue = filterValue;
  }

}
