package life.qbic.portal.steps;

import java.util.LinkedHashMap;
import java.util.List;
import com.vaadin.ui.TabSheet;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public interface IWizardStep {

  void setNextStep(IWizardStep w);
  
//  void resetInputs();
  
  void activate();
  
  void next();

  void setTabs(TabSheet tabs);
  
  void collectEntitiesToRegister(LinkedHashMap<PreliminaryOpenbisExperiment,List<ISampleBean>> samplesPerLevel);
  
  boolean isValid();
}
