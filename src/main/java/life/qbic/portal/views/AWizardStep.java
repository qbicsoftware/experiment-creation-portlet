package life.qbic.portal.views;

import java.util.LinkedHashMap;
import java.util.List;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.portal.Styles;
import life.qbic.portal.model.PreliminaryOpenbisExperiment;
import life.qbic.portal.portlet.OverviewUIPortlet;
import com.vaadin.ui.TabSheet.Tab;

public abstract class AWizardStep extends VerticalLayout {

  protected IWizardStep next;
  protected TabSheet tabs;
  private Button cancel = new Button("Cancel");
  protected Button nextButton = new Button("Next");
  protected LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> samplesToRegister =
      new LinkedHashMap<>();
  protected String error = "Please fill in all necessary information.";

  public void setNextStep(IWizardStep w) {
    next = w;
  }

  public void next() {
    if (next != null) {
      next.activate();
    }
  }

  public void collectEntitiesToRegister(
      LinkedHashMap<PreliminaryOpenbisExperiment, List<ISampleBean>> samplesPerLevel) {
    this.samplesToRegister = samplesPerLevel;
  }

  public void activate() {
    setSpacing(true);
    setMargin(true);
    int count = tabs.getComponentCount();
    for (int i = 0; i < count; i++) {
      Tab t = tabs.getTab(i);
      if (this.equals(t.getComponent())) {
        t.setEnabled(true);
        tabs.setSelectedTab(t);
      } else {
        t.setEnabled(false);
      }
    }

    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setSpacing(true);
    buttons.addComponent(cancel);

    // TODO add notification before removing?
    cancel.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        createCancelWarningDialogue();
      }
    });
    buttons.addComponent(nextButton);

    nextButton.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (isValid()) {
          next();
        } else {
          Styles.notification("Please complete the necessary information.", error,
              Styles.NotificationType.DEFAULT);
        }
      }
    });
    addComponent(buttons);
  }

  private void createCancelWarningDialogue() {
    Window subWindow = new Window(" Cancel Experiment Creation?");
    subWindow.setWidth("400px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);
    Label preInfo = new Label(
        "Cancel current experiment input and go back? Any information not saved will be lost.");
    layout.addComponent(preInfo);
    Button ok = new Button("Yes, I'm sure");
    Button no = new Button("Don't cancel");
    ok.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        Component target = tabs.getTab(0).getComponent();
        String caption = tabs.getTab(0).getCaption();

        tabs.removeAllComponents();
        tabs.addTab(target, caption);
        subWindow.close();
      }
    });
    no.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        subWindow.close();
      }
    });
    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setSpacing(true);
    buttons.addComponent(ok);
    buttons.addComponent(no);
    layout.addComponent(buttons);

    subWindow.setContent(layout);
    // Center it in the browser window
    subWindow.center();
    subWindow.setModal(true);
    subWindow.setIcon(FontAwesome.EXCLAMATION_TRIANGLE);
    subWindow.setResizable(false);
    OverviewUIPortlet ui = (OverviewUIPortlet) UI.getCurrent();
    ui.addWindow(subWindow);
  }

  protected abstract boolean isValid();

  public void setTabs(TabSheet tabs) {
    this.tabs = tabs;
  }
}
