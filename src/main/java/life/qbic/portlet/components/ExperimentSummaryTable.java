package life.qbic.portlet.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import com.vaadin.data.Item;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;

public class ExperimentSummaryTable extends Table {

  private static final Map<String, String> abbreviations;
  static {
    abbreviations = new HashMap<String, String>();
    abbreviations.put("M Rna", "mRNA");
    abbreviations.put("R Rna", "rRNA");
    abbreviations.put("Dna", "DNA");
    abbreviations.put("Rna", "RNA");
  };

  public ExperimentSummaryTable() {
    super();
    setCaption("Summary");
    addContainerProperty("Type", String.class, null);
    addContainerProperty("Content", String.class, null);// TODO more width
    addContainerProperty("Samples", Integer.class, null);
    setStyleName(ValoTheme.TABLE_SMALL);
    setPageLength(1);
  }

  public void setSamples(Map<PreliminaryOpenbisExperiment, List<ISampleBean>> experiments) {
    removeAllItems();
    int i = 0;
    for (PreliminaryOpenbisExperiment exp : experiments.keySet()) {
      i++;
      List<ISampleBean> samples = experiments.get(exp);
      String content = "";//translateContent(samples);
      content = WordUtils.capitalizeFully(content.replace("_", " "));
      for (String key : abbreviations.keySet()) {
        content = content.replace(key, abbreviations.get(key));
      }
      int amount = samples.size();
      String type = "Unknown";
      SampleType sampleType = samples.get(0).getType();
      switch (sampleType) {
        case Q_BIOLOGICAL_ENTITY:
          type = "Sample Sources";
          break;
        case Q_BIOLOGICAL_SAMPLE:
          type = "Sample Extracts";
          break;
        case Q_TEST_SAMPLE:
          type = "Sample Preparations";
          break;
        default:
          type = sampleType.toString();
      }
//      if (b.isPartOfSplit())
//        type = "Split " + type;
//      if (b.isPool())
//        type = "Pooled " + type;
      addItem(new Object[] {type, content, amount}, i);
    }
    setPageLength(i);
    styleTable();
  }

  private String translateContent(List<ISampleBean> list) {
    // TODO Auto-generated method stub
    return null;
  }

  private String parseCell(Object id, String propertyName) {
    Item item = getItem(id);
    return (String) item.getItemProperty(propertyName).getValue();
  }

  private void styleTable() {
    String base = "blue-hue";
    List<String> styles = new ArrayList<String>();
    for (int i = 1; i < 7; i++)
      styles.add(base + Integer.toString(i));

    // Set cell style generator
    setCellStyleGenerator(new Table.CellStyleGenerator() {

      @Override
      public String getStyle(Table source, Object itemId, Object propertyId) {
        String type = parseCell(itemId, "Type");
        if (type.contains("Sample Sources"))
          return styles.get(0);
        else if (type.contains("Sample Extracts"))
          return styles.get(1);
        else if (type.contains("Sample Preparations")) {
          String analyte = parseCell(itemId, "Content");
          if (!analyte.equals("Peptides"))
            return styles.get(2);
          else
            return styles.get(3);
        } else if (type.contains("Mass Spectrometry Run"))
          return styles.get(4);
        else
          return "";
      }

    });
  }

}
