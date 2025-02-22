package dockingFX.core;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * The floating window of the docking system.
 * Users can modify the floating window's size, position, and content.
 * The UI style of the floating window is corresponding to the default style of the docker's mainStage.
 *
 * @author Hsuan-Kai Liao
 */
public class DWindow {
  // Constants
  private static final double DEFAULT_FLOATING_WIDTH = 250;
  private static final double DEFAULT_FLOATING_HEIGHT = 200;
  private static final double DEFAULT_FLOATING_OPACITY = 0.5;
  private static final int UNDOCK_MINIMUM_DISTANCE = 20;

  // Dragging attributes
  double xOffset = 0;
  double yOffset = 0;
  private Point2D dragStartPoint = null;

  // Docking attributes
  final Docker docker;
  final Stage floatingStage;
  final TabPane floatingTabPane;
  boolean isDocked = false;

  // Listeners
  private EventHandler<WindowEvent> onCloseEvent;
  EventHandler<ActionEvent> onDockEvent;
  EventHandler<ActionEvent> onUndockEvent;
  
  // Settings
  private boolean isDockOnClose = true;

  /**
   * Constructs a floating window with the given stage and tab pane.
   * @param floatingStage the stage of the floating window
   * @param floatingTabPane the tab pane of the floating window that contains the content
   */
  DWindow(Stage floatingStage, TabPane floatingTabPane, Docker docker) {
    this.docker = docker;
    this.floatingStage = floatingStage;
    this.floatingTabPane = floatingTabPane;

    // Adjust the size of the floating window based on the content size
    Node content = floatingTabPane.getTabs().getFirst().getContent();
    double contentWidth;
    double contentHeight;
    if (content == null) {
      contentWidth = 0;
      contentHeight = 0;
    } else {
      contentWidth = content.prefWidth(-1);
      contentHeight = content.prefHeight(-1);
    }
    floatingStage.setWidth(contentWidth > 0 ? contentWidth : DEFAULT_FLOATING_WIDTH);
    floatingStage.setHeight(contentHeight > 0 ? contentHeight : DEFAULT_FLOATING_HEIGHT);

    // Set the initial scene of the floating window
    Scene floatingScene = new Scene(floatingTabPane, DEFAULT_FLOATING_WIDTH, DEFAULT_FLOATING_HEIGHT);
    floatingStage.setScene(floatingScene);
    floatingScene.getRoot().applyCss();

    // Add event listeners for tabs for dragging and docking
    Tab tab = floatingTabPane.getTabs().getFirst();
    Node tabHeaderArea = tab.getGraphic();
    if (tabHeaderArea != null) {
      tabHeaderArea.setOnMousePressed(this::onTabPressed);
      tabHeaderArea.setOnMouseDragged(event -> {
        if (isDocked) {
          onTabDockedDragged(event);
        } else {
          onTabUndockedDragged(event);
        }
      });
    }

    // When closed, dock to the nearest side of the main window
    floatingStage.setOnCloseRequest(this::onFloatingClose);
    floatingTabPane.setOnMouseReleased(this::onTabDropped);
  }

  /* API BELOW */

  /**
   * Returns the width of the floating window.
   * @return the width of the floating window
   */
  public double getWidth() {
    return floatingStage.getWidth();
  }

  /**
   * Returns the height of the floating window.
   * @return the height of the floating window
   */
  public double getHeight() {
    return floatingStage.getHeight();
  }

  /**
   * Sets the width of the floating window.
   * @param width the width of the floating window
   */
  public void setWidth(double width) {
    floatingStage.setWidth(width);
  }

  /**
   * Sets the height of the floating window.
   * @param height the height of the floating window
   */
  public void setHeight(double height) {
    floatingStage.setHeight(height);
  }

  /**
   * Returns the x-coordinate of the floating window.
   * @return the x-coordinate of the floating window
   */
  public double getX() {
    return floatingStage.getX();
  }

  /**
   * Returns the y-coordinate of the floating window.
   * @return the y-coordinate of the floating window
   */
  public double getY() {
    return floatingStage.getY();
  }

  /**
   * Sets the x-coordinate of the floating window.
   * @param x the x-coordinate of the floating window
   */
  public void setX(double x) {
    floatingStage.setX(x);
  }

  /**
   * Sets the y-coordinate of the floating window.
   * @param y the y-coordinate of the floating window
   */

  public void setY(double y) {
    floatingStage.setY(y);
  }

  /**
   * Returns the content of the floating window.
   * @return the content Node of the floating window
   */
  public Node getContent() {
    return floatingTabPane.getTabs().getFirst().getContent();
  }

  /**
   * Sets the content of the floating window.
   * @param content the new content Node of the floating window
   */
  public void setContent(Node content) {
    floatingTabPane.getTabs().getFirst().setContent(content);
  }

  /**
   * Sets the event handler for the close event.
   * This will only be called when the isDockOnClose is set to false.
   * @param event the event handler for the close event
   */
  public void setOnClose(EventHandler<WindowEvent> event) {
    this.onCloseEvent = event;
  }

  /**
   * Sets the event handler for the dock event.
   * @param onDockEvent the event handler for the dock event
   */
  public void setOnDockEvent(EventHandler<ActionEvent> onDockEvent) {
    this.onDockEvent = onDockEvent;
  }

  /**
   * Sets the event handler for the undock event.
   * @param onUndockEvent the event handler for the undock event
   */
  public void setOnUndockEvent(EventHandler<ActionEvent> onUndockEvent) {
    this.onUndockEvent = onUndockEvent;
  }

  /* SETTINGS */

  /**
   * Sets whether the floating window will be docked onto the docker when the close button is clicked.
   */
  public void setDockOnClose(boolean isDockOnClose) {
    this.isDockOnClose = isDockOnClose;
  }

  /**
   * Returns whether the floating window will be docked onto the docker when the close button is clicked.
   */
  public boolean isDockOnClose() {
    return isDockOnClose;
  }

  /* CALLBACKS BELOW */

  void onTabDropped(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();

    if (docker.dockIndicator.isMouseInsideIndicator(mouseX, mouseY)) {
      TabPane targetTabPane = docker.findTabPaneUnderMouse(mouseX, mouseY);

      // Let the UI set the hoverProperty to false
      floatingTabPane.setMouseTransparent(true);

      if (targetTabPane != null) {
        docker.dockTab(this, targetTabPane, docker.dockIndicator.indicatorPosition);
      } else if (!docker.isMouseInsideMainScene(mouseX, mouseY)) {
        docker.dockTab(this, null, docker.dockIndicator.indicatorPosition);
      }

      // Reset the mouse transparency
      Platform.runLater(() -> floatingTabPane.setMouseTransparent(false));
    }

    docker.dockIndicator.hideDockIndicator();
    floatingStage.setOpacity(1);
  }

  void onTabUndockedDragged(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();
    double decorationBarHeight = floatingStage.getHeight() - floatingStage.getScene().getHeight();

    floatingStage.setX(mouseX - xOffset);
    floatingStage.setY(mouseY - yOffset - decorationBarHeight);

    docker.dockIndicator.showDockIndicator(mouseX, mouseY);

    if (!docker.isWindowOpaqueOnDragging && floatingStage.getOpacity() == 1) {
      floatingStage.setOpacity(DEFAULT_FLOATING_OPACITY);
    }
  }

  private void onTabDockedDragged(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();

    double dragOffsetX = mouseX - dragStartPoint.getX();
    double dragOffsetY = mouseY - dragStartPoint.getY();
    double dragDistance = Math.sqrt(dragOffsetX * dragOffsetX + dragOffsetY * dragOffsetY);

    Bounds bounds = floatingTabPane.localToScreen(floatingTabPane.getBoundsInLocal());
    xOffset = mouseX - bounds.getMinX() - dragOffsetX;
    yOffset = mouseY - bounds.getMinY() - dragOffsetY;

    floatingStage.setX(mouseX - xOffset);
    floatingStage.setY(mouseY - yOffset);

    if (dragDistance > UNDOCK_MINIMUM_DISTANCE) {
      docker.undockTab(this);

      if (!docker.isWindowOpaqueOnDragging) {
        floatingStage.setOpacity(DEFAULT_FLOATING_OPACITY);
      }
    }
  }

  private void onTabPressed(MouseEvent event) {
    xOffset = event.getSceneX();
    yOffset = event.getSceneY();
    dragStartPoint = new Point2D(event.getScreenX(), event.getScreenY());
  }

  private void onFloatingClose(WindowEvent event) {
    // Check isDockOnClose to prevent docking when the window is closed by the user
    if (!isDockOnClose) {
      // Call the user-defined event handler
      if (onCloseEvent != null) {
        onCloseEvent.handle(event);
      }

      // Close the window
      docker.removeFloatingWindow(this);
      floatingStage.close();
      return;
    }

    double floatingX = floatingStage.getX();
    double floatingY = floatingStage.getY();
    double floatingWidth = floatingStage.getWidth();
    double floatingHeight = floatingStage.getHeight();
    Stage mainStage = docker.mainStage;

    double mainX = mainStage.getX();
    double mainY = mainStage.getY();
    double mainWidth = mainStage.getWidth();
    double mainHeight = mainStage.getHeight();

    // Calculate the center of the floating window
    double floatingCenterX = floatingX + floatingWidth / 2;
    double floatingCenterY = floatingY + floatingHeight / 2;

    // Calculate the distances to each side of the main window
    double distanceToLeft = floatingCenterX - mainX;
    double distanceToRight = mainX + mainWidth - floatingCenterX;
    double distanceToTop = floatingCenterY - mainY;
    double distanceToBottom = mainY + mainHeight - floatingCenterY;

    // Determine the nearest side
    Docker.DockPosition nearestSide = null; // Default if no side is closest
    double minDistance = Double.MAX_VALUE;

    if (distanceToLeft < minDistance) {
      minDistance = distanceToLeft;
      nearestSide = Docker.DockPosition.LEFT;
    }
    if (distanceToRight < minDistance) {
      minDistance = distanceToRight;
      nearestSide = Docker.DockPosition.RIGHT;
    }
    if (distanceToTop < minDistance) {
      minDistance = distanceToTop;
      nearestSide = Docker.DockPosition.TOP;
    }
    if (distanceToBottom < minDistance) {
      nearestSide = Docker.DockPosition.BOTTOM;
    }

    docker.dockTab(this, null, nearestSide);

    // Prevent the window from closing
    event.consume();
  }
}
