package dockingFX.core;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
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

    initializeWindowSize();
    initializeScene();
    initializeTabEvents();
  }

  /* API BELOW */

  public double getWidth() { return floatingStage.getWidth(); }
  public double getHeight() { return floatingStage.getHeight(); }

  public void setWidth(double width) { floatingStage.setWidth(width); }
  public void setHeight(double height) { floatingStage.setHeight(height); }

  public double getX() { return floatingStage.getX(); }
  public double getY() { return floatingStage.getY(); }

  public void setX(double x) { floatingStage.setX(x); }
  public void setY(double y) { floatingStage.setY(y); }

  public Node getContent() { return floatingTabPane.getTabs().getFirst().getContent(); }
  public void setContent(Node content) { floatingTabPane.getTabs().getFirst().setContent(content); }

  public void setOnClose(EventHandler<WindowEvent> event) { this.onCloseEvent = event; }
  public void setOnDockEvent(EventHandler<ActionEvent> onDockEvent) { this.onDockEvent = onDockEvent; }
  public void setOnUndockEvent(EventHandler<ActionEvent> onUndockEvent) { this.onUndockEvent = onUndockEvent; }

  public void setDockOnClose(boolean isDockOnClose) { this.isDockOnClose = isDockOnClose; }
  public boolean isDockOnClose() { return isDockOnClose; }

  /* CALLBACKS BELOW */

  void onTabDropped(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();

    if (docker.dockIndicator.isMouseInsideIndicator(mouseX, mouseY)) {
      dockTabIfNecessary(mouseX, mouseY);
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
    updateWindowOpacityOnDrag();
  }

  private void onTabDockedDragged(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();

    double dragDistance = calculateDragDistance(mouseX, mouseY);
    double decorationBarHeight = docker.mainStage.getHeight() - docker.mainStage.getScene().getHeight();

    floatingStage.setX(mouseX - xOffset);
    floatingStage.setY(mouseY - yOffset - decorationBarHeight);

    if (dragDistance > UNDOCK_MINIMUM_DISTANCE) {
      docker.undockTab(this);
      updateWindowOpacityOnDrag();
    }
  }

  private void onTabPressed(MouseEvent event) {
    double mouseX = event.getScreenX();
    double mouseY = event.getScreenY();

    if (isDocked) {
      calculateTabOffset(mouseX, mouseY);
    } else {
      xOffset = event.getSceneX();
      yOffset = event.getSceneY();
    }
    dragStartPoint = new Point2D(mouseX, mouseY);
  }

  private void onFloatingClose(WindowEvent event) {
    if (!isDockOnClose) {
      handleCustomCloseEvent(event);
      return;
    }

    Docker.DockPosition nearestSide = calculateNearestDockPosition();
    docker.dockTab(this, null, nearestSide);
    event.consume();
  }

  /* HELPER METHODS */

  private void initializeWindowSize() {
    Node content = floatingTabPane.getTabs().getFirst().getContent();
    double contentWidth = (content == null) ? 0 : content.prefWidth(-1);
    double contentHeight = (content == null) ? 0 : content.prefHeight(-1);
    floatingStage.setWidth(contentWidth > 0 ? contentWidth : DEFAULT_FLOATING_WIDTH);
    floatingStage.setHeight(contentHeight > 0 ? contentHeight : DEFAULT_FLOATING_HEIGHT);
  }

  private void initializeScene() {
    Scene floatingScene = new Scene(floatingTabPane, DEFAULT_FLOATING_WIDTH, DEFAULT_FLOATING_HEIGHT);
    floatingStage.setScene(floatingScene);
    floatingScene.getRoot().applyCss();
  }

  private void initializeTabEvents() {
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

    floatingStage.setOnCloseRequest(this::onFloatingClose);
    floatingTabPane.setOnMouseReleased(this::onTabDropped);
  }

  private void dockTabIfNecessary(double mouseX, double mouseY) {
    TabPane targetTabPane = docker.findTabPaneUnderMouse(mouseX, mouseY);
    floatingTabPane.setMouseTransparent(true);

    if (targetTabPane != null) {
      docker.dockTab(this, targetTabPane, docker.dockIndicator.indicatorPosition);
    } else if (!docker.isMouseInsideMainScene(mouseX, mouseY)) {
      docker.dockTab(this, null, docker.dockIndicator.indicatorPosition);
    }

    Platform.runLater(() -> floatingTabPane.setMouseTransparent(false));
  }

  private void updateWindowOpacityOnDrag() {
    if (!docker.isWindowOpaqueOnDragging && floatingStage.getOpacity() == 1) {
      floatingStage.setOpacity(DEFAULT_FLOATING_OPACITY);
    }
  }

  private double calculateDragDistance(double mouseX, double mouseY) {
    double dragOffsetX = mouseX - dragStartPoint.getX();
    double dragOffsetY = mouseY - dragStartPoint.getY();
    return Math.sqrt(dragOffsetX * dragOffsetX + dragOffsetY * dragOffsetY);
  }

  private void calculateTabOffset(double mouseX, double mouseY) {
    Tab targetTab = (Tab) floatingTabPane.getTabs().getFirst().getUserData();
    TabPane targetTabPane = targetTab.getTabPane();
    Region targetTabHeaderArea = (Region) targetTabPane.lookup(".tab-header-area");

    Insets targetTabHeaderAreaPadding = targetTabHeaderArea.getPadding();
    Point2D targetTabOffset = targetTab.getGraphic().screenToLocal(mouseX, mouseY);

    double tabOffsetX = targetTabOffset.getX() + targetTabHeaderAreaPadding.getLeft();
    double headerOffsetX = targetTabHeaderArea.screenToLocal(mouseX, mouseY).getX();

    xOffset = Math.min(tabOffsetX, headerOffsetX);
    yOffset = targetTabOffset.getY() + targetTabHeaderAreaPadding.getTop();
  }

  private Docker.DockPosition calculateNearestDockPosition() {
    double floatingX = floatingStage.getX();
    double floatingY = floatingStage.getY();
    double floatingWidth = floatingStage.getWidth();
    double floatingHeight = floatingStage.getHeight();
    Stage mainStage = docker.mainStage;

    double floatingCenterX = floatingX + floatingWidth / 2;
    double floatingCenterY = floatingY + floatingHeight / 2;

    double distanceToLeft = floatingCenterX - mainStage.getX();
    double distanceToRight = mainStage.getX() + mainStage.getWidth() - floatingCenterX;
    double distanceToTop = floatingCenterY - mainStage.getY();
    double distanceToBottom = mainStage.getY() + mainStage.getHeight() - floatingCenterY;

    Docker.DockPosition nearestSide = Docker.DockPosition.TOP;
    double minDistance = distanceToTop;

    if (minDistance > distanceToLeft) {
      minDistance = distanceToLeft;
      nearestSide = Docker.DockPosition.LEFT;
    }

    if (minDistance > distanceToRight) {
      minDistance = distanceToRight;
      nearestSide = Docker.DockPosition.RIGHT;
    }

    if (minDistance > distanceToBottom) {
      minDistance = distanceToBottom;
      nearestSide = Docker.DockPosition.BOTTOM;
    }

    return nearestSide;
  }

  private void handleCustomCloseEvent(WindowEvent event) {
    if (onCloseEvent != null) {
      onCloseEvent.handle(event);
    }
  }
}
