package life.qbic.portlet.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.portal.Styles;
import life.qbic.portal.model.ExtendedOpenbisExperiment;

public class SampleInfoComponent extends VerticalLayout {

  Button closeButton;

  public SampleInfoComponent(ExtendedOpenbisExperiment e, Map<String, String> propertyTranslation) {
    setSpacing(true);
    setMargin(true);
    for (String key : e.getMetadata().keySet()) {
      Label info = new Label();
      String caption = key;
      if (propertyTranslation.containsKey(key)) {
        caption = propertyTranslation.get(key);
      }
      info.setCaption(caption);
      info.setValue(e.getMetadata().get(key).toString());
      addComponent(info);
    }
    Table samples = new Table();
    samples.setStyleName(Styles.tableTheme);
    samples.addContainerProperty("Name", String.class, "");
    samples.addContainerProperty("Pre-Treatment", String.class, "");
    samples.addContainerProperty("Dilution/Buffer", String.class, "");
    samples.addContainerProperty("Description", String.class, "");
    samples.addContainerProperty("Barcode", String.class, null);

    for (Sample s : e.getSamples()) {
      Map<String, String> props = s.getProperties();
      List<Object> row = new ArrayList<>();
      row.add(props.get("Q_EXTERNALDB_ID"));
      row.add(props.get("Q_PRE_TREATMENT"));
      row.add(props.get("Q_SECONDARY_NAME")); // TODO
      row.add(props.get("Q_ADDITIONAL_INFO"));
      row.add(s.getCode());
      samples.addItem(row.toArray(new Object[row.size()]), s);
    }
    samples.setPageLength(samples.size());
    addComponent(samples);
    closeButton = new Button("Close");
    addComponent(closeButton);
  }

  public Button getCloseButton() {
    return closeButton;
  }

}
