package life.qbic.portal.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.components.StandardTextField;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public class BiologicsCultureCreationView extends AWizardStep implements IWizardStep {

  private Map<String, String> cellLines;

  private ComboBox cultureBox;
  private TextField company;
  private TextField biologicName;
  private TextField proteinNum;
  private Table sampleInfos;
  private Sample speciesSample;

  public BiologicsCultureCreationView(List<Sample> previousLevel, Map<String, String> cellLines) {
    speciesSample = previousLevel.get(0);

    this.cellLines = cellLines;
    cultureBox = new ComboBox("Base Cell Culture", cellLines.keySet());
    cultureBox.setStyleName(Styles.boxTheme);
    cultureBox.setValue("CHO-K1 cell");
    company = new TextField("Name of Company");
    biologicName = new TextField("Name of Biologic");
    proteinNum = new TextField("Number of Protein Samples");
    company.setStyleName(Styles.fieldTheme);
    biologicName.setStyleName(Styles.fieldTheme);
    proteinNum.setStyleName(Styles.fieldTheme);

    proteinNum.setRequired(true);
    proteinNum.setConverter(new StringToIntegerConverter());
    proteinNum
        .addValidator(new IntegerRangeValidator("Must be a number between 1 and 100.", 1, 100));

    addComponent(cultureBox);
    addComponent(company);
    addComponent(biologicName);
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
    Map<String, Object> cultureExpProps = new HashMap<>();
    String cultureName = "Representative for cell culture";
    if (!company.getValue().isEmpty()) {
      cultureName += " at " + company.getValue();
    }
    cultureExpProps.put("Q_SECONDARY_NAME", cultureName);
    cultureExpProps.put("Q_ADDITIONAL_INFO", biologicName.getValue());
    Map<String, Object> proteinExpProps = new HashMap<>();
    proteinExpProps.put("Q_SECONDARY_NAME", "Preparation of biologic " + biologicName.getValue());
    PreliminaryOpenbisExperiment cultureExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_EXTRACTION, cultureExpProps);
    PreliminaryOpenbisExperiment proteinExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, proteinExpProps);

    HashMap<String, Object> cultureProps = new HashMap<>();
    String cellLine = (String) cultureBox.getValue();
    cultureProps.put("Q_PRIMARY_TISSUE", "CELL_LINE");
    cultureProps.put("Q_TISSUE_DETAILED", cellLines.get(cellLine));// TODO does this make sense?
    TSVSampleBean cultureSample = new TSVSampleBean("c", SampleType.Q_BIOLOGICAL_SAMPLE,
        "representative culture sample", cultureProps);
    cultureSample.addParentID(speciesSample.getCode());
    samplesToRegister.put(cultureExp, Arrays.asList(cultureSample));

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
    return cultureBox.isValid() && proteinNum.isValid() && company.isValid()
        && biologicName.isValid();
  }

}
