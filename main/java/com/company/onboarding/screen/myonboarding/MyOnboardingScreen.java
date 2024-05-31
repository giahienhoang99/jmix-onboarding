package com.company.onboarding.screen.myonboarding;

import com.company.onboarding.entity.User;
import com.company.onboarding.entity.UserStep;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.CheckBox;
import io.jmix.ui.component.Component;
import io.jmix.ui.component.Label;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

@UiController("MyOnboardingScreen")
@UiDescriptor("my-onboarding-screen.xml")
public class MyOnboardingScreen extends Screen {
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private CollectionLoader<UserStep> userStepsDl;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Label<Object> totalStepsLabel;

    @Autowired
    private Label<Object> completedStepsLabel;

    @Autowired
    private Label<Object> overdueStepsLabel;

    @Autowired
    private CollectionContainer<UserStep> userStepsDc;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        User user = (User) currentAuthentication.getUser();
        userStepsDl.setParameter("user", user);
        userStepsDl.load();

        updateLabels();
    }

    @Subscribe(id = "userStepsDc", target = Target.DATA_CONTAINER)
    public void onUserStepsDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<UserStep> event) {
        updateLabels();
    }

    @Install(to = "userStepsTable.completed", subject = "columnGenerator")
    private Component userStepsTableCompletedColumnGenerator(final UserStep userStep) {
        CheckBox checkBox = uiComponents.create(CheckBox.class);
        checkBox.setValue(userStep.getCompletedDate() != null);
        checkBox.addValueChangeListener(e -> {
            if (userStep.getCompletedDate() == null) {
                userStep.setCompletedDate(LocalDate.now());
            } else {
                userStep.setCompletedDate(null);
            }
        });
        return checkBox;
    }

    private void updateLabels() {
        totalStepsLabel.setValue("Total steps: " + userStepsDc.getItems().size());

        long completedCount = userStepsDc.getItems().stream()
                .filter(us -> us.getCompletedDate() != null)
                .count();
        completedStepsLabel.setValue("Completed steps: " + completedCount);

        long overdueCount = userStepsDc.getItems().stream()
                .filter(us -> isOverdue(us))
                .count();
        overdueStepsLabel.setValue("Overdue steps: " + overdueCount);
    }

    private boolean isOverdue(UserStep us) {
        return us.getCompletedDate() == null
                && us.getDueDate() != null
                && us.getDueDate().isBefore(LocalDate.now());
    }

    @Autowired
    private DataContext dataContext;

    @Subscribe("saveButton")
    public void onSaveButtonClick(final Button.ClickEvent event) {
        dataContext.commit();
        close(StandardOutcome.COMMIT);
    }

    @Subscribe("discardButton")
    public void onDiscardButtonClick(final Button.ClickEvent event) {
        close(StandardOutcome.DISCARD);
    }

    @Install(to = "userStepsTable", subject = "styleProvider")
    private String userStepsTableStyleProvider( UserStep entity, String property) {
        if ("dueDate".equals(property) && isOverdue(entity)) {
            return "overdue-step";
        }
        return null;
    }
}