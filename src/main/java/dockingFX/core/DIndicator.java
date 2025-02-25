package dockingFX.core;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Represents a dock indicator that shows where a docked window will be placed.
 *
 * @author Hsuan-Kai Liao
 */
public class DIndicator {
  // Constants
  private static final int DOCK_INDICATOR_WIDTH = 25;
  private static final int DOCK_INDICATOR_HEIGHT = 25;
  private static final double INDICATOR_INNER_SHIFT_OFFSET = 20;
  private static final double INDICATOR_OUTER_SHIFT_OFFSET = 10;

  // Indicator attributes
  final Docker docker;
  final Stage indicatorStage;
  Docker.DockPosition indicatorPosition;

  /**
   * Constructs a dock indicator with the given docker.
   * @param docker the docker that the dock indicator is associated with
   */
  DIndicator(Docker docker) {
    this.docker = docker;

    // Initialize the dock indicator
    indicatorStage = new Stage();
    indicatorStage.initStyle(StageStyle.UNDECORATED);
    indicatorStage.setAlwaysOnTop(true);
    StackPane dockRoot = new StackPane(new Label("⚓"));
    dockRoot.getStyleClass().add("dock-indicator");
    Scene dockScene = new Scene(dockRoot, DOCK_INDICATOR_WIDTH, DOCK_INDICATOR_HEIGHT);
    indicatorStage.setScene(dockScene);
    indicatorStage.setOpacity(0);
    indicatorStage.setX(-DOCK_INDICATOR_WIDTH);
    indicatorStage.setY(-DOCK_INDICATOR_HEIGHT);
    indicatorStage.show();
  }

  /* PACKAGE-PRIVATE METHODS */

  void showDockIndicator(double mouseX, double mouseY) {
    TabPane tp = docker.findTabPaneUnderMouse(mouseX, mouseY);
    if (tp == null) {
      // Check if the mouse is outside the bounds of the mainStage
      if (docker.isMouseInsideMainScene(mouseX, mouseY)) {
        hideDockIndicator();
        return;
      } else {
        // Get the midpoints of the edges of the mainStage
        Point2D[] edgeMidpoints = getStageEdgeMidpoints(docker.mainStage);
        Map.Entry<Docker.DockPosition, Point2D> nearestEdgeMidpoint = getClosestEdge(edgeMidpoints, mouseX, mouseY);
        indicatorPosition = nearestEdgeMidpoint.getKey();
        updateIndicatorPosition(nearestEdgeMidpoint.getValue(), indicatorPosition, false);

        // Set the style of the dock indicator
        indicatorStage.setOpacity(1);
      }
    } else {
      // Get the midpoints of the edges of the TabPane
      Point2D[] edgeMidpoints = getTabPaneEdgeMidpoints(tp);
      Map.Entry<Docker.DockPosition, Point2D> nearestEdgeMidpoint = getClosestEdge(edgeMidpoints, mouseX, mouseY);
      indicatorPosition = nearestEdgeMidpoint.getKey();
      updateIndicatorPosition(nearestEdgeMidpoint.getValue(), indicatorPosition, true);

      // Set the style of the dock indicator
      indicatorStage.setOpacity(1);
    }

    // Show the dock indicator
    if (!indicatorStage.isShowing()) {
      indicatorStage.show();
    }
  }

  void hideDockIndicator() {
    indicatorStage.setOpacity(0);
    indicatorStage.setX(-DOCK_INDICATOR_WIDTH);
    indicatorStage.setY(-DOCK_INDICATOR_HEIGHT);
  }

  void updateIndicatorPosition(Point2D nearestEdgeMidpoint, Docker.DockPosition dockPosition, boolean inOrOutShift) {
    if (nearestEdgeMidpoint == null) return;

    double newX = nearestEdgeMidpoint.getX();
    double newY = nearestEdgeMidpoint.getY();

    double shift = inOrOutShift ? INDICATOR_INNER_SHIFT_OFFSET : -INDICATOR_OUTER_SHIFT_OFFSET;

    switch (dockPosition) {
      case LEFT:
        newX += shift;
        break;
      case RIGHT:
        newX -= shift;
        break;
      case TOP:
        newY += shift;
        if (!inOrOutShift) {
          newY += docker.mainStage.getHeight() - docker.mainStage.getScene().getHeight();
        }
        break;
      case BOTTOM:
        newY -= shift;
        break;
    }

    // Update the position of the dock indicator
    indicatorStage.setX(newX - indicatorStage.getWidth() / 2);
    indicatorStage.setY(newY - indicatorStage.getHeight() / 2);
  }

  boolean isMouseInsideIndicator(double mouseX, double mouseY) {
    double indicatorX = indicatorStage.getX();
    double indicatorY = indicatorStage.getY();
    double indicatorWidth = indicatorStage.getWidth();
    double indicatorHeight = indicatorStage.getHeight();

    return mouseX >= indicatorX && mouseX <= indicatorX + indicatorWidth &&
        mouseY >= indicatorY && mouseY <= indicatorY + indicatorHeight;
  }

  /* HELPER METHODS */

  static private Point2D[] getTabPaneEdgeMidpoints(TabPane tabPane) {
    Bounds bounds = tabPane.localToScreen(tabPane.getBoundsInLocal());

    double leftX = bounds.getMinX();
    double rightX = bounds.getMaxX();
    double topY = bounds.getMinY();
    double bottomY = bounds.getMaxY();

    double centerX = (leftX + rightX) / 2;
    double centerY = (topY + bottomY) / 2;

    return new Point2D[]{
        new Point2D(leftX, centerY),   // Left center
        new Point2D(rightX, centerY),  // Right center
        new Point2D(centerX, topY),    // Top center
        new Point2D(centerX, bottomY), // Bottom center
        new Point2D(centerX, centerY)  // Center
    };
  }

  static private Point2D[] getStageEdgeMidpoints(Stage stage) {
    double stageX = stage.getScene().getWindow().getX();
    double stageY = stage.getScene().getWindow().getY();
    double stageWidth = stage.getWidth();
    double stageHeight = stage.getHeight();

    // Calculate the midpoints of the stage edges
    double leftMidY = stageY + stageHeight / 2;  // Y-coordinate of left edge midpoint
    double rightMidY = stageY + stageHeight / 2; // Y-coordinate of right edge midpoint
    double topMidX = stageX + stageWidth / 2;  // X-coordinate of top edge midpoint
    double bottomMidX = stageX + stageWidth / 2; // X-coordinate of bottom edge midpoint

    return new Point2D[]{
        new Point2D(stageX, leftMidY),   // Left edge midpoint
        new Point2D(stageX + stageWidth, rightMidY), // Right edge midpoint
        new Point2D(topMidX, stageY),   // Top edge midpoint
        new Point2D(bottomMidX, stageY + stageHeight) // Bottom edge midpoint
    };
  }

  static private Map.Entry<Docker.DockPosition, Point2D> getClosestEdge(Point2D[] midpoints, double mouseX, double mouseY) {
    Map<Docker.DockPosition, Point2D> edges = new HashMap<>();
    edges.put(Docker.DockPosition.LEFT, midpoints[0]);   // Left center
    edges.put(Docker.DockPosition.RIGHT, midpoints[1]);  // Right center
    edges.put(Docker.DockPosition.TOP, midpoints[2]);    // Top center
    edges.put(Docker.DockPosition.BOTTOM, midpoints[3]); // Bottom center

    // Check if there is a center point
    if (midpoints.length >= 5) {
      edges.put(Docker.DockPosition.CENTER, midpoints[4]); // Center
    }

    Docker.DockPosition closestEdge = null;
    Point2D closestPoint = null;
    double minDistance = Double.MAX_VALUE;

    for (Map.Entry<Docker.DockPosition, Point2D> entry : edges.entrySet()) {
      double distance = entry.getValue().distance(mouseX, mouseY);
      if (distance < minDistance) {
        minDistance = distance;
        closestEdge = entry.getKey();
        closestPoint = entry.getValue();
      }
    }

    return new AbstractMap.SimpleEntry<>(closestEdge, closestPoint);
  }
}
