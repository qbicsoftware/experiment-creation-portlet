package life.qbic.portal.model;

import java.util.HashSet;
import java.util.Set;
import life.qbic.datamodel.ms.LigandPrepRun;

/**
 * defines sample preparation experiments based on uniqueness of parameters. e.g. different
 * digestion enzymes or fractionation types or preparation dastes warrant a new experiment instance,
 * while the same protocols and time points will be stored in one experiment
 * 
 * @author afriedrich
 *
 */
public class SamplePreparationRun {

  private String sourceID;
  private String prepDate;
  private Set<String> digestionEnzymes;
  private String fractionationType;

  public SamplePreparationRun(String proteinParent, String prepDate, String fracType) {
    // super(proteinParent, "N/A", prepDate, fracType);
    this.sourceID = proteinParent;
    this.prepDate = prepDate;
    this.fractionationType = fracType;
    this.digestionEnzymes = new HashSet<>();
  }

  public SamplePreparationRun(String proteinParent, String prepDate, Set<String> enzymes,
      String fracType) {
    // super(proteinParent, "N/A", prepDate, fracType);
    this.sourceID = proteinParent;
    this.prepDate = prepDate;
    this.fractionationType = fracType;
    this.digestionEnzymes = enzymes;
  }

  @Override
  public String toString() {
    String res = sourceID + " (" + fractionationType + ") on " + prepDate + "\nUsed enzymes:";
    for (String e : digestionEnzymes) {
      res += "\n" + e;
    }
    return res;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((digestionEnzymes == null) ? 0 : digestionEnzymes.hashCode());
    result = prime * result + ((fractionationType == null) ? 0 : fractionationType.hashCode());
    result = prime * result + ((prepDate == null) ? 0 : prepDate.hashCode());
    result = prime * result + ((sourceID == null) ? 0 : sourceID.hashCode());
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
    SamplePreparationRun other = (SamplePreparationRun) obj;
    if (digestionEnzymes == null) {
      if (other.digestionEnzymes != null)
        return false;
    } else if (!digestionEnzymes.equals(other.digestionEnzymes))
      return false;
    if (fractionationType == null) {
      if (other.fractionationType != null)
        return false;
    } else if (!fractionationType.equals(other.fractionationType))
      return false;
    if (prepDate == null) {
      if (other.prepDate != null)
        return false;
    } else if (!prepDate.equals(other.prepDate))
      return false;
    if (sourceID == null) {
      if (other.sourceID != null)
        return false;
    } else if (!sourceID.equals(other.sourceID))
      return false;
    return true;
  }

}
