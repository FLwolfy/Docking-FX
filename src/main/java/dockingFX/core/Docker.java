package dockingFX.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A docking system that allows users to dock and undock floating windows to the main stage.
 *
 * @author Hsuan-Kai Liao
 */
public class Docker {
  // Constants
  private static final int DOCK_OUTSIDE_OFFSET = 10;

  // Instance variables
  private final List<SplitPane> splitPanes = new ArrayList<>();
  private final List<DWindow> floatingWindows = new ArrayList<>();
  private DWindow undockNewWindow = null;

  // Docker attributes
  final DIndicator dockIndicator;
  final Stage mainStage;

  // Docker settings
  boolean isWindowOpaqueOnDragging = false;

  /**
   * The position of the docking.
   */
  public enum DockPosition {
    NONE, LEFT, RIGHT, TOP, BOTTOM, CENTER
  }

  /* APIS BELOW */

  /**
   * Creates a new Docker with the specified main stage and dimensions.
   * After creating a Docker, users should NOT modify the scene of the main stage.
   *
   * @param mainStage the main stage of the docking system
   */
  public Docker(Stage mainStage) {
    this.mainStage = mainStage;

    SplitPane splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.HORIZONTAL);
    splitPane.getStyleClass().add("dock-split-pane");
    splitPanes.add(splitPane);

    // Set the main stage's scene
    Scene mainScene = new Scene(splitPane, mainStage.getWidth(), mainStage.getHeight());
    mainStage.setScene(mainScene);

    // Create the dock indicator
    this.dockIndicator = new DIndicator(this);

    // Set the main stage's event listeners
    mainStage.setOnShown(event -> {
      for (DWindow floatingWindow : floatingWindows) {
        if (floatingWindow.floatingStage.getOpacity() != 0) {
          floatingWindow.floatingStage.show();
          Platform.runLater(floatingWindow.floatingStage::toFront);
        }
      }
    });
    mainStage.setOnCloseRequest(event -> {
      for (DWindow floatingWindow : floatingWindows) {
        floatingWindow.floatingStage.close();
      }
      dockIndicator.indicatorStage.close();
      floatingWindows.clear();
    });

    // This is used for the first undock event
    mainStage.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
      if (undockNewWindow != null) {
        undockNewWindow.onTabUndockedDragged(event);
      }
    });
    mainStage.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
      if (undockNewWindow != null) {
        undockNewWindow.onTabDropped(event);
        undockNewWindow = null;
      }
    });
  }

  /**
   * Creates a floating window with the specified title and content.
   *
   * @param title the title of the floating window
   * @param content the content of the floating window
   * @param dockPosition the default dock position of the floating window
   * @return the floating window
   */
  public DWindow createDWindow(StringProperty title, Node content, DockPosition dockPosition) {
    Stage floatingStage = new Stage();
    floatingStage.initStyle(StageStyle.UTILITY);

    // Create a TabPane to hold the content
    TabPane floatingTabPane = new TabPane();
    floatingTabPane.getStyleClass().add("dock-tab-pane");

    Label tabLabel = new Label();
    tabLabel.getStyleClass().add("dock-tab-label");
    tabLabel.textProperty().bind(title);

    Tab tab = new Tab();
    tab.setGraphic(tabLabel);
    tab.getStyleClass().add("dock-tab");
    tab.setClosable(false);
    tab.setContent(content);
    floatingTabPane.getTabs().add(tab);

    // Create a DWindow object
    DWindow dWindow = new DWindow(floatingStage, floatingTabPane, this);
    floatingWindows.add(dWindow);

    // Initial dock check
    if (dockPosition != null && dockPosition != DockPosition.NONE) {
      floatingStage.setOpacity(0);
      dockTab(dWindow, null, dockPosition);
    }

    return dWindow;
  }

  /**
   * Returns the main docker stage of the docking system.
   *
   * @return the main docker stage
   */
  public Stage getMainStage() {
    return mainStage;
  }

  /**
   * Adds a style sheet to the docker and the dock indicator.
   *
   * @param styleSheet the style sheet to add
   */
  public void addStyleSheet(String styleSheet) {
    mainStage.getScene().getStylesheets().add(styleSheet);
    dockIndicator.indicatorStage.getScene().getStylesheets().add(styleSheet);

    // Add the style sheet to the floating windows
    for (DWindow floatingWindow : floatingWindows) {
      if (floatingWindow.floatingStage.getScene() != null) {
        floatingWindow.floatingStage.getScene().getStylesheets().add(styleSheet);
      }
    }
  }

  /**
   * Clears all style sheets from the docker and the dock indicator.
   */
  public void clearStyleSheets() {
    mainStage.getScene().getStylesheets().clear();
    dockIndicator.indicatorStage.getScene().getStylesheets().clear();

    // Clear the style sheets from the floating windows
    for (DWindow floatingWindow : floatingWindows) {
      if (floatingWindow.floatingStage.getScene() != null) {
        floatingWindow.floatingStage.getScene().getStylesheets().clear();
      }
    }
  }

  /**
   * Reformats the docker to adjust the divider positions of the split panes.
   */
  public void reformat() {
    // Recursively reformat SplitPanes
    Consumer<SplitPane> recursiveReformat = new Consumer<>() {
      @Override
      public void accept(SplitPane splitPane) {
        int size = splitPane.getItems().size();
        if (size < 2) return;

        double[] positions = new double[size - 1];
        for (int i = 0; i < positions.length; i++) {
          positions[i] = (i + 1) / (double) size;
        }
        splitPane.setDividerPositions(positions);

        // Recursively divide child SplitPanes
        for (Node node : splitPane.getItems()) {
          if (node instanceof SplitPane childSplitPane) {
            accept(childSplitPane);
          }
        }
      }
    };

    // Get the main SplitPane
    SplitPane mainSplitPane = (SplitPane) mainStage.getScene().getRoot();
    recursiveReformat.accept(mainSplitPane);
  }

  /* SETTINGS */

  public void setWindowOpaqueOnDragging(boolean isOpaque) {
    this.isWindowOpaqueOnDragging = isOpaque;
  }

  /* DOCKING CORE */

  void dockTab(DWindow dWindow, TabPane destTabPane, DockPosition dockPosition) {
    // Add the floating TabPane to the target TabPane
    addTabToDocker(dWindow.floatingTabPane, destTabPane, dockPosition);

    // Hide the floating stage
    dWindow.isDocked = true;
    dWindow.floatingStage.setOpacity(0);
    dWindow.floatingStage.hide();

    // Call the onDockEvent
    if (dWindow.onDockEvent != null) {
      dWindow.onDockEvent.handle(null);
    }
  }

  void undockTab(DWindow dWindow) {
    // Undock the TabPane from the Docker
    TabPane floatingTabPane = dWindow.floatingTabPane;
    Stage floatingWindow = dWindow.floatingStage;

    // Get the target Tab and TabPane
    Tab targetTab = (Tab) floatingTabPane.getTabs().getFirst().getUserData();
    TabPane targetTabPane = targetTab.getTabPane();

    // Pass back the content to the original Tab
    Node content = targetTab.getContent();
    dWindow.floatingTabPane.getTabs().getFirst().setContent(content);
    targetTab.setContent(null);

    // Remove the TabPane from the Docker
    if (targetTabPane.getTabs().size() == 1) {
      removeTabFromDocker(targetTab);
      removeTabPaneFromDocker(targetTabPane);
      collapseSplitPanes();
    } else {
      removeTabFromDocker(targetTab);
    }

    // Set the scene of the floating window
    if (floatingTabPane.getScene() == null) {
      Scene newScene = new Scene(floatingTabPane);
      newScene.getStylesheets().setAll(mainStage.getScene().getStylesheets());
      floatingWindow.setScene(newScene);
    }

    // Set the current position of the floating window
    double height = targetTabPane.getHeight();
    double width = targetTabPane.getWidth();
    floatingWindow.setWidth(width);
    floatingWindow.setHeight(height);
    floatingWindow.setOpacity(1);
    floatingWindow.show();
    undockNewWindow = dWindow;

    // Call the onUndockEvent
    if (dWindow.onUndockEvent != null) {
      dWindow.onUndockEvent.handle(null);
    }

    // Set the isDocked flag to false
    dWindow.isDocked = false;
  }

  /* PACKAGE-PRIVATE METHODS */

  boolean isMouseInsideMainScene(double mouseX, double mouseY) {
    double sceneX = mainStage.getScene().getWindow().getX();
    double sceneY = mainStage.getScene().getWindow().getY();
    double sceneWidth = mainStage.getScene().getWidth();
    double sceneHeight = mainStage.getScene().getHeight();
    double decorationBarHeight = mainStage.getHeight() - sceneHeight;

    return mouseX >= sceneX + DOCK_OUTSIDE_OFFSET && mouseX <= sceneX - DOCK_OUTSIDE_OFFSET + sceneWidth &&
        mouseY >= sceneY + DOCK_OUTSIDE_OFFSET + decorationBarHeight && mouseY <= sceneY - DOCK_OUTSIDE_OFFSET + sceneHeight;
  }

  TabPane findTabPaneUnderMouse(double mouseX, double mouseY) {
    for (SplitPane splitPane : splitPanes) {
      for (Node node : splitPane.getItems()) {
        if (node instanceof TabPane tabPane) {
          Bounds screenBounds = tabPane.localToScreen(tabPane.getBoundsInLocal());
          if (screenBounds != null && screenBounds.contains(mouseX, mouseY)) {
            return tabPane;
          }
        }
      }
    }
    return null;
  }

  void removeFloatingWindow(DWindow dWindow) {
    floatingWindows.remove(dWindow);
  }

  /* HELPER METHODS */

  private SplitPane findParentSplitPane(Node child) {
    for (SplitPane parent : splitPanes) {
      if (parent.getItems().contains(child)) {
        return parent;
      }
    }
    return null;
  }

  private void addTabToDocker(TabPane srcTabPane, TabPane destTabPane, DockPosition dockPosition) {
    // Invalid dock position
    assert dockPosition != DockPosition.NONE && dockPosition != null;

    // Get splitPanes
    SplitPane targetSplitPane = findParentSplitPane(destTabPane);
    targetSplitPane = (destTabPane == null) ? (SplitPane) mainStage.getScene().getRoot() : targetSplitPane == null ? new SplitPane() : targetSplitPane;
    targetSplitPane.getStyleClass().add("dock-split-pane");

    // Get the srcTabPane properties
    Tab srcTab = srcTabPane.getTabs().getFirst();
    Label srcTabLabel = (Label) srcTab.getGraphic();
    Node srcTabContent = srcTab.getContent();

    // Create a new Tab with the same properties with the source Tab, and pass the content
    Label newTabLabel = new Label();
    newTabLabel.getStyleClass().add("dock-tab-label");
    newTabLabel.textProperty().bind(srcTabLabel.textProperty());
    newTabLabel.setOnMousePressed(event -> srcTabLabel.getOnMousePressed().handle(event));
    newTabLabel.setOnMouseDragged(event -> srcTabLabel.getOnMouseDragged().handle(event));
    Tab newTab = new Tab();
    newTab.getStyleClass().add("dock-tab");
    newTab.setClosable(srcTab.isClosable());
    newTab.setGraphic(newTabLabel);
    newTab.setContent(srcTabContent);
    srcTab.setContent(null);

    // Link the new tab to the old tab
    srcTab.setUserData(newTab);

    // Center dock position case
    if (dockPosition == DockPosition.CENTER) {
      assert destTabPane != null;
      destTabPane.getTabs().add(newTab);
      destTabPane.getSelectionModel().select(newTab);
    }

    // Other dock positions
    else {
      // Create a new TabPane
      TabPane newTabPane = new TabPane();
      newTabPane.getStyleClass().add("dock-tab-pane");
      newTabPane.getTabs().add(newTab);
      newTabPane.getSelectionModel().select(newTab);

      // Initial divider positions
      double[] originalPositions = targetSplitPane.getDividerPositions();
      double[] newPositions = new double[originalPositions.length + 1];

      // Check dock position
      boolean isHorizontal = (dockPosition == DockPosition.LEFT
          || dockPosition == DockPosition.RIGHT);
      boolean shouldFrontInsert = (dockPosition == DockPosition.LEFT
          || dockPosition == DockPosition.TOP);
      boolean shouldInsertDirectly =
          targetSplitPane.getOrientation() == (isHorizontal ? Orientation.HORIZONTAL
              : Orientation.VERTICAL)
              && !targetSplitPane.getItems().isEmpty();

      // Update Index
      int index = (destTabPane == null) ? -1 : targetSplitPane.getItems().indexOf(destTabPane);
      index = (index != -1) ? index : shouldFrontInsert ? 0 : targetSplitPane.getItems().size() - 1;

      // Create a new SplitPane if the target SplitPane is not empty
      SplitPane newSplitPane = new SplitPane();
      newSplitPane.getStyleClass().add("dock-split-pane");

      // Check if the target SplitPane is empty
      if (shouldInsertDirectly) {
        targetSplitPane.getItems().add(shouldFrontInsert ? index : index + 1, newTabPane);

        // New divider positions
        if (originalPositions.length != 0) {
          System.arraycopy(originalPositions, 0, newPositions, 0, index);
          double newDividerPosition = (index == 0) ? originalPositions[0] / 2.0
              : (index == originalPositions.length) ? (originalPositions[index - 1] + 1.0) / 2.0
                  : (originalPositions[index - 1] + originalPositions[index]) / 2.0;
          newPositions[index] = newDividerPosition;
          System.arraycopy(originalPositions, index, newPositions, index + 1,
              originalPositions.length - index);
          targetSplitPane.setDividerPositions(newPositions);
        }
      } else {
        newSplitPane.setOrientation(isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        newSplitPane.getItems().add(newTabPane);

        if (destTabPane != null) {
          newSplitPane.getItems().add(shouldFrontInsert ? 1 : 0, destTabPane);
          if (!targetSplitPane.getItems().isEmpty()) {
            targetSplitPane.getItems().set(index, newSplitPane);
          }
        } else {
          newSplitPane.getItems().add(shouldFrontInsert ? 1 : 0, targetSplitPane);
        }

        // New divider positions
        if (originalPositions.length != 0) {
          targetSplitPane.setDividerPositions(originalPositions);
        }
      }

      // Remove the old SplitPane if it is empty
      if (targetSplitPane.getItems().isEmpty()) {
        newSplitPane.getItems().remove(targetSplitPane);
        splitPanes.remove(targetSplitPane);
      }

      // Set the new root of the main stage
      if (!newSplitPane.getItems().isEmpty()) {
        splitPanes.add(newSplitPane);
        if (destTabPane == null) {
          mainStage.getScene().setRoot(newSplitPane);
        }
      }
    }
  }

  private void removeTabFromDocker(Tab tab) {
    for (SplitPane splitPane : splitPanes) {
      for (Node node : splitPane.getItems()) {
        if (node instanceof TabPane tabPane && tabPane.getTabs().contains(tab)) {
          tabPane.getTabs().remove(tab);
          return;
        }
      }
    }
  }

  private void removeTabPaneFromDocker(TabPane tabPane) {
    for (SplitPane splitPane : splitPanes) {
      if (splitPane.getItems().contains(tabPane)) {
        // Store the original positions and count
        double[] originalPositions = splitPane.getDividerPositions();
        int originalCount = splitPane.getItems().size();

        // Remove the tab pane from the split pane
        int removedIndex = splitPane.getItems().indexOf(tabPane);
        splitPane.getItems().remove(tabPane);

        // Restore the divider positions
        if (originalCount > 1) {
          double[] newPositions = new double[originalPositions.length - 1];
          for (int i = 0; i < newPositions.length; i++) {
            if (i < removedIndex - 1) {
              newPositions[i] = originalPositions[i];
            } else if (i == removedIndex - 1) {
              newPositions[i] = (originalPositions[i] + originalPositions[i + 1]) / 2;
            } else {
              newPositions[i] = originalPositions[i + 1];
            }
          }

          splitPane.setDividerPositions(newPositions);
        }
        return;
      }
    }
  }

  private void collapseSplitPanes() {
    // Recursively collapse SplitPanes
    Consumer<SplitPane> recursiveCollapse = new Consumer<>() {
      @Override
      public void accept(SplitPane parentSplitPane) {
        // Empty checks and collapsing single-child SplitPanes
        List<SplitPane> emptySplitPanes = new ArrayList<>();
        for (int index = 0; index < parentSplitPane.getItems().size(); index++) {
          Node node = parentSplitPane.getItems().get(index);
          if (!(node instanceof SplitPane childSplitPane)) {
            continue;
          }

          // Recursively collapse child SplitPanes
          accept(childSplitPane);

          // If the SplitPane is empty, mark it for removal
          if (childSplitPane.getItems().isEmpty()) {
            emptySplitPanes.add(childSplitPane);
          }

          // If the SplitPane has only one child, then promote that child
          else if (childSplitPane.getItems().size() == 1) {
            Node child = childSplitPane.getItems().getFirst();

            // If the child is a SplitPane with the same orientation, merge them
            if (child instanceof SplitPane grandChildSplitPane && grandChildSplitPane.getOrientation() == parentSplitPane.getOrientation()) {
              // Get the original divider positions
              double[] parentDividerPositions = parentSplitPane.getDividerPositions();
              double[] childDividerPositions = grandChildSplitPane.getDividerPositions();

              // Calculate the total size of the grandchild SplitPane
              int leftIndex = index - 1;
              double leftPos = (leftIndex >= 0) ? parentDividerPositions[leftIndex] : 0.0;
              double rightPos = (index < parentDividerPositions.length) ? parentDividerPositions[leftIndex + 1] : 1.0;
              double totalSize = rightPos - leftPos;

              // Remove the child SplitPane and add the grandchild SplitPane
              parentSplitPane.getItems().remove(index);
              parentSplitPane.getItems().addAll(index, grandChildSplitPane.getItems());

              // Calculate the new divider positions
              List<Double> newDividers = new ArrayList<>();
              for (int i = 0; i < index; i++) {
                newDividers.add(parentDividerPositions[i]);
              }
              for (double divider : childDividerPositions) {
                newDividers.add(leftPos + divider * totalSize);
              }
              for (int i = index; i < parentDividerPositions.length; i++) {
                newDividers.add(parentDividerPositions[i]);
              }

              // Update the indices
              index--;
              index += grandChildSplitPane.getItems().size();

              // Set the new divider positions
              parentSplitPane.setDividerPositions(newDividers.stream().mapToDouble(Double::doubleValue).toArray());

              // Add the grandchild SplitPane to the empty list
              emptySplitPanes.add(childSplitPane);
              emptySplitPanes.add(grandChildSplitPane);
            } else {
              // Restore the original divider positions
              double[] parentDividerPositions = parentSplitPane.getDividerPositions();
              childSplitPane.getItems().remove(child);
              parentSplitPane.getItems().set(index, child);
              parentSplitPane.setDividerPositions(parentDividerPositions);

              emptySplitPanes.add(childSplitPane);
            }
          }
        }
        splitPanes.removeAll(emptySplitPanes);
        parentSplitPane.getItems().removeAll(emptySplitPanes);
      }
    };

    // Get the main SplitPane
    SplitPane mainSplitPane = (SplitPane) mainStage.getScene().getRoot();
    recursiveCollapse.accept(mainSplitPane);

    // Promote the child SplitPane if the main SplitPane has only one child
    if (mainSplitPane.getItems().size() == 1 && mainSplitPane.getItems().getFirst() instanceof SplitPane childSplitPane) {
      mainSplitPane.setOrientation(childSplitPane.getOrientation());
      mainSplitPane.getItems().setAll(childSplitPane.getItems());
      mainSplitPane.setDividerPositions(childSplitPane.getDividerPositions());
      childSplitPane.getItems().clear();
      splitPanes.remove(childSplitPane);
    }
  }

}
