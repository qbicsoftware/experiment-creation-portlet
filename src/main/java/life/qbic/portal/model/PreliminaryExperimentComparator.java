package life.qbic.portal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamodel.experiments.ExperimentType;

public class PreliminaryExperimentComparator implements Comparator<PreliminaryOpenbisExperiment> {

  private List<ExperimentType> typeOrder =
      new ArrayList<>(Arrays.asList(ExperimentType.Q_EXPERIMENTAL_DESIGN,
          ExperimentType.Q_SAMPLE_EXTRACTION, ExperimentType.Q_SAMPLE_PREPARATION));

  private static final PreliminaryExperimentComparator instance =
      new PreliminaryExperimentComparator();

  public static PreliminaryExperimentComparator getInstance() {
    return instance;
  }

  private PreliminaryExperimentComparator() {}

  @Override
  public int compare(PreliminaryOpenbisExperiment o1, PreliminaryOpenbisExperiment o2) {
    int one = typeOrder.indexOf(o1.getType());
    int two = typeOrder.indexOf(o2.getType());
    if (one == -1) {
      one = Integer.MAX_VALUE;
    }
    if (two == -1) {
      two = Integer.MAX_VALUE;
    }
    return new Integer(one).compareTo(new Integer(two));
  }

}
