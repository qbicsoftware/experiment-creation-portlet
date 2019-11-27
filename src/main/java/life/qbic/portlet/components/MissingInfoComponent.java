package life.qbic.portlet.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;
import life.qbic.portal.Styles;

public class MissingInfoComponent extends VerticalLayout {

  private Map<String, List<ComboBox>> catToBoxes;
  private Map<String, Map<String, String>> catToVocabulary;

  public MissingInfoComponent() {
    setSpacing(true);
  }

  public boolean isValid() {
    boolean boxesValid = true;
    for (List<ComboBox> list : catToBoxes.values())
      for (ComboBox b : list)
        boxesValid &= (b.getValue() != null);
    return boxesValid;
  }

  public void init(Map<String, List<String>> missingCategoryToValues,
      Map<String, Map<String, String>> catToVocabulary, ValueChangeListener infoCompleteListener) {
    setCaption("Experiment information (please complete)");

    catToBoxes = new HashMap<String, List<ComboBox>>();
    this.catToVocabulary = catToVocabulary;

    for (String cat : missingCategoryToValues.keySet()) {
      List<ComboBox> boxes = new ArrayList<ComboBox>();
      for (String value : missingCategoryToValues.get(cat)) {
        Set<String> vocab = new HashSet<String>(catToVocabulary.get(cat).keySet());
        ComboBox b = new ComboBox(value, vocab);
        b.setNullSelectionAllowed(false);
        b.setStyleName(Styles.boxTheme);
        b.setFilteringMode(FilteringMode.CONTAINS);
        boolean match = false;
        for (String vVal : vocab) {
          if (vVal.equalsIgnoreCase(value)) {
            match = true;
            b.setValue(vVal);
            b.setEnabled(false);
            break;
          }
        }
        if (!match) {
          b.addValueChangeListener(infoCompleteListener);
          b.setRequiredError("Please find the closest option.");
          b.setRequired(true);
        }
        boxes.add(b);
        addComponent(b);
      }
      catToBoxes.put(cat, boxes);
    }
  }

  public String getVocabularyLabelForValue(String cat, String entry) {
    if (catToBoxes.containsKey(cat)) {
      for (ComboBox b : catToBoxes.get(cat))
        if (b.getCaption().equals(entry))
          return b.getValue().toString();
    }
    return null;
  }

  public String getVocabularyCodeForValue(String cat, String entry) {
    String label = getVocabularyLabelForValue(cat, entry);
    Map<String, String> vocab = catToVocabulary.get(cat);
    if (vocab.containsKey(label)) {
      return vocab.get(label);
    }
    // TODO Auto-generated method stub
    return label;
  }
}
