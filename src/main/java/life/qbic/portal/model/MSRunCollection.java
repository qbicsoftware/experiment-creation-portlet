package life.qbic.portal.model;

public class MSRunCollection {

  private SamplePreparationRun ligandPrep;
  private String runDate;
  private String device;
  private String col;

  public MSRunCollection(SamplePreparationRun ligandPrep, String runDate, String msDevice,
      String lcCol) {
    this.ligandPrep = ligandPrep;
    this.runDate = runDate;
    this.device = msDevice;
    this.col = lcCol;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ligandPrep == null) ? 0 : ligandPrep.hashCode());
    result = prime * result + ((runDate == null) ? 0 : runDate.hashCode());
    result = prime * result + ((device == null) ? 0 : device.hashCode());
    result = prime * result + ((col == null) ? 0 : col.hashCode());
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
    MSRunCollection other = (MSRunCollection) obj;
    if (ligandPrep == null) {
      if (other.ligandPrep != null)
        return false;
    } else if (!ligandPrep.equals(other.ligandPrep))
      return false;
    if (runDate == null) {
      if (other.runDate != null)
        return false;
    } else if (!runDate.equals(other.runDate))
      return false;
    if (device == null) {
      if (other.device != null)
        return false;
    } else if (!device.equals(other.device))
      return false;
    if (col == null) {
      if (other.col != null)
        return false;
    } else if (!col.equals(other.col))
      return false;
    return true;
  }



}
