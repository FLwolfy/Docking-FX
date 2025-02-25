package dockingFX;

import dockingFX.core.DWindow;
import dockingFX.core.Docker;
import java.util.Objects;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Main extends Application {

  // The Docker CSS Template
  private static final String DOCKER_STYLE_LIGHT_CSS = Objects.requireNonNull(Main.class.getResource("/dockingFX/dock-style-light.css")).toExternalForm();
  private static final String DOCKER_STYLE_DARK_CSS = Objects.requireNonNull(Main.class.getResource("/dockingFX/dock-style-dark.css")).toExternalForm();

  @Override
  public void start(Stage primaryStage) {
    // Create a new Docker instance
    Docker docker = new Docker(primaryStage);

    // Create some windows
    DWindow dockingWindow1 = docker.createDWindow(new SimpleStringProperty("Auto Docked Window 1"), new Rectangle(100, 100, Color.RED), Docker.DockPosition.LEFT);
    DWindow dockingWindow2 = docker.createDWindow(new SimpleStringProperty("Auto Docked Window 2"), new Circle(100, Color.BLUE), Docker.DockPosition.TOP);
    DWindow dockingWindow3 = docker.createDWindow(new SimpleStringProperty("Auto Docked Window 3"), new Rectangle(100, 100, Color.GREEN), Docker.DockPosition.RIGHT);
    DWindow dockingWindow4 = docker.createDWindow(new SimpleStringProperty("Auto Docked Window 4"), new Circle(100, Color.GOLD), Docker.DockPosition.BOTTOM);
    DWindow dockingWindow5 = docker.createDWindow(new SimpleStringProperty("Undocked Closable Window"), new Rectangle(100, 100, Color.BLACK), Docker.DockPosition.NONE);

    // Configure the windows
    dockingWindow1.setDockOnClose(true);  // This is the default setting
    dockingWindow2.setDockOnClose(true);  // This is the default setting
    dockingWindow3.setDockOnClose(true);  // This is the default setting
    dockingWindow4.setDockOnClose(true);  // This is the default setting
    dockingWindow5.setDockOnClose(false);
    dockingWindow5.setOnClose(event -> {
      System.out.println("Window 5 closed");
    });

    // Configure the docker
    docker.reformat();                              // <-- Automatically reformat the windows
    // docker.setWindowOpaqueOnDragging(true);      // <-- Uncomment this line to disable transparency

    // Set the docker UI style
    docker.addStyleSheet(DOCKER_STYLE_LIGHT_CSS);
    // docker.addStyleSheet(DOCKER_STYLE_DARK_CSS); // <-- Uncomment this line to use the dark theme

    // Show the stage
    primaryStage.setTitle("DockingFX");
    primaryStage.setWidth(800);
    primaryStage.setHeight(600);
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }

}