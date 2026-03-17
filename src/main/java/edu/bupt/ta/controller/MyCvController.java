package edu.bupt.ta.controller;

import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MyCvController {

    private final TabPane view = new TabPane();

    public MyCvController(ServiceRegistry services, User user) {
        Tab profile = new Tab("Edit Profile", new ApplicantProfileController(services, user).getView());
        profile.setClosable(false);
        Tab resume = new Tab("Resume Information", new ResumeInfoController(services, user).getView());
        resume.setClosable(false);
        view.getTabs().addAll(profile, resume);
    }

    public Parent getView() {
        return view;
    }
}
