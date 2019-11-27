package life.qbic.portal.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.components.StandardTextField;
import life.qbic.portal.model.ExtendedOpenbisExperiment;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public class ProteinCreationView extends AWizardStep implements IWizardStep {


  private TextField proteinNum;
  private Table sampleInfos;
  private Sample cultureSample;
  private ExtendedOpenbisExperiment cultureExperiment;

  public ProteinCreationView(List<Sample> previousLevel, ExtendedOpenbisExperiment oldExp) {
    cultureSample = previousLevel.get(0);
    cultureExperiment = oldExp;

    proteinNum = new TextField("Number of Protein Samples");
    proteinNum.setStyleName(Styles.fieldTheme);

    proteinNum.setRequired(true);
    proteinNum.setConverter(new StringToIntegerConverter());
    proteinNum
        .addValidator(new IntegerRangeValidator("Must be a number between 1 and 100.", 1, 100));

    addComponent(proteinNum);

    sampleInfos = new Table("Sample Information");
    sampleInfos.setStyleName(Styles.tableTheme);
    sampleInfos.addContainerProperty("Name", TextField.class, null);
    sampleInfos.addContainerProperty("Description", TextField.class, null);
    sampleInfos.setVisible(false);

    // sampleInfos.setColumnWidth("Description", 250);
    // sampleInfos.setColumnWidth("Name", 250);

    addComponent(sampleInfos);

    proteinNum.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        sampleInfos.removeAllItems();
        sampleInfos.setVisible(false);
        if (proteinNum.isValid()) {
          int proteins = Integer.parseInt(proteinNum.getValue());
          initTable(proteins);
        }
      }
    });
    proteinNum.setValue("1");
  }
  
  private String parseTextFieldValue(String colname, Object id) {
    return ((TextField) sampleInfos.getItem(id).getItemProperty(colname).getValue()).getValue();
  }

  public void initTable(int numOfSamples) {
    sampleInfos.setVisible(true);
    for (int i = 0; i < numOfSamples; i++) {
      // Create the table row.
      List<Object> row = new ArrayList<Object>();


      TextField extIDField = new StandardTextField();
      extIDField.setWidth("95px");
      extIDField.setImmediate(true);
      row.add(extIDField);

      TextField secNameField = new StandardTextField();
      secNameField.setImmediate(true);
      row.add(secNameField);

      sampleInfos.addItem(row.toArray(new Object[row.size()]), i);
    }
    sampleInfos.setPageLength(numOfSamples);
  }

  @Override
  public void next() {
    addExperimentsAndSamples();
    next.collectEntitiesToRegister(samplesToRegister);
    super.next();
  }

  private void addExperimentsAndSamples() {
    Map<String, Object> proteinExpProps = new HashMap<>();
    String biologic = (String) cultureExperiment.getMetadata().get("Q_ADDITIONAL_INFO");
    proteinExpProps.put("Q_SECONDARY_NAME",
        "Biologic " + biologic);
    PreliminaryOpenbisExperiment proteinExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, proteinExpProps);

    List<ISampleBean> samples = new ArrayList<>();

    for (Object id : sampleInfos.getItemIds()) {
      Map<String, Object> props = new HashMap<>();
      String secName = parseTextFieldValue("Description", id);
      if (secName == null)
        secName = "";

      String extID = parseTextFieldValue("Name", id);
      if (extID == null)
        extID = "";

      props.put("Q_EXTERNALDB_ID", extID);
      props.put("Q_SAMPLE_TYPE", "PROTEINS");
      TSVSampleBean s = new TSVSampleBean(id.toString(), SampleType.Q_TEST_SAMPLE, secName, props);
      s.addParentID(cultureSample.getCode());
      samples.add(s);
    }
    samplesToRegister.put(proteinExp, samples);
  }

  @Override
  public void activate() {
    super.activate();
  }

  @Override
  public boolean isValid() {
    return proteinNum.isValid();
  }

}
