package life.qbic.portal.steps.views;

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
import com.vaadin.ui.TextArea;
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
import life.qbic.portal.steps.AWizardStep;
import life.qbic.portal.steps.IWizardStep;

public class BiologicsCultureCreationView extends AWizardStep implements IWizardStep {

  private Map<String, String> cellLines;

  private ComboBox cultureBox;
  // private TextField company;
  // private TextField biologicName;
  private TextField batchNum;
  private TextField proteinNum;
  private TextArea freeTextInfo;
  private Table sampleInfos;
  private Sample speciesSample;

  public BiologicsCultureCreationView(ExtendedOpenbisExperiment previousLevel, Map<String, String> cellLines) {
    speciesSample = previousLevel.getSamples().get(0);

    this.cellLines = cellLines;
    cultureBox = new ComboBox("Base Cell Culture", cellLines.keySet());
    cultureBox.setStyleName(Styles.boxTheme);
    cultureBox.setValue("CHO-K1 cell");
    batchNum = new TextField("Batch Number");
    batchNum.setStyleName(Styles.fieldTheme);
    // company = new TextField("Name of Company");
    // biologicName = new TextField("Name of Biologic");
    proteinNum = new TextField("Number of Protein Samples");
    // company.setStyleName(Styles.fieldTheme);
    // biologicName.setStyleName(Styles.fieldTheme);
    proteinNum.setStyleName(Styles.fieldTheme);

    proteinNum.setRequired(true);
    proteinNum.setConverter(new StringToIntegerConverter());
    proteinNum
        .addValidator(new IntegerRangeValidator("Must be a number between 1 and 100.", 1, 100));

    freeTextInfo = new TextArea("Additional Preparation Information");

    addComponent(cultureBox);
    // addComponent(company);
    // addComponent(biologicName);
    addComponent(batchNum);
    addComponent(proteinNum);
    addComponent(freeTextInfo);

    sampleInfos = new Table("Sample Information (optional)");
    sampleInfos.setStyleName(Styles.tableTheme);
    sampleInfos.addContainerProperty("Name", TextField.class, null);
    sampleInfos.addContainerProperty("Pretreatment", TextField.class, null);
    sampleInfos.addContainerProperty("Dilution", TextField.class, null);
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
    String val =
        ((TextField) sampleInfos.getItem(id).getItemProperty(colname).getValue()).getValue();
    if (val == null) {
      val = "";
    }
    return val;
  }

  public void initTable(int numOfSamples) {
    sampleInfos.setVisible(true);
    for (int i = 0; i < numOfSamples; i++) {
      // Create the table row.
      List<Object> row = new ArrayList<Object>();


      TextField extIDField = new StandardTextField();
      // extIDField.setWidth("95px");
      extIDField.setImmediate(true);
      row.add(extIDField);

      TextField treatmentField = new StandardTextField();
      treatmentField.setImmediate(true);
      row.add(treatmentField);

      TextField dilutionField = new StandardTextField();
      dilutionField.setImmediate(true);
      row.add(dilutionField);

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
    Map<String, Object> proteinExpProps = new HashMap<>();
    String cultureName = "Cell culture";
    // if (!company.getValue().isEmpty()) {
    // cultureName += " at " + company.getValue();
    // }
    cultureExpProps.put("Q_SECONDARY_NAME", cultureName);
    proteinExpProps.put("Q_ADDITIONAL_INFO", freeTextInfo.getValue());
    proteinExpProps.put("Q_SECONDARY_NAME", "Batch " + batchNum.getValue());

    PreliminaryOpenbisExperiment cultureExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_EXTRACTION, cultureExpProps);
    PreliminaryOpenbisExperiment proteinExp =
        new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, proteinExpProps);

    HashMap<String, Object> cultureProps = new HashMap<>();
    String cellLine = (String) cultureBox.getValue();
    cultureProps.put("Q_PRIMARY_TISSUE", "CELL_LINE");
    cultureProps.put("Q_TISSUE_DETAILED", cellLines.get(cellLine));
    TSVSampleBean cultureSample = new TSVSampleBean("c", SampleType.Q_BIOLOGICAL_SAMPLE,
        "representative culture sample", cultureProps);
    cultureSample.addParentID(speciesSample.getCode());
    samplesToRegister.put(cultureExp, Arrays.asList(cultureSample));

    List<ISampleBean> samples = new ArrayList<>();

    for (Object id : sampleInfos.getItemIds()) {
      Map<String, Object> props = new HashMap<>();
      String info = parseTextFieldValue("Description", id);
      String extID = parseTextFieldValue("Name", id);
      String dilution = parseTextFieldValue("Dilution", id);
      String pretreat = parseTextFieldValue("Pretreatment", id);

      props.put("Q_EXTERNALDB_ID", extID);
      props.put("Q_PRE_TREATMENT", pretreat);
      props.put("Q_SAMPLE_TYPE", "PROTEINS");
      props.put("Q_ADDITIONAL_INFO", dilution);
      // TODO ?
//      props.put("Q_DILUTION", dilution);
      TSVSampleBean s = new TSVSampleBean(id.toString(), SampleType.Q_TEST_SAMPLE, info, props);
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
    return cultureBox.isValid() && proteinNum.isValid() && batchNum.isValid()
        && freeTextInfo.isValid();
  }

}
