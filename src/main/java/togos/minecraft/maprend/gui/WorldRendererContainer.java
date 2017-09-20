package togos.minecraft.maprend.gui;

import java.util.Objects;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

/**
 * <p>
 * Wraps a {@link WorldRendererCanvas} into an AnchorPane and a BorderPane. This is because a {@link Canvas} such as the renderer needs to have a preferred size
 * set to properly work in many layouts. This wrapper will make sure that the canvas will always use exactly as much of the space available - no less and no
 * more. It is recommended to use this class in most layout situations.
 * </p>
 */
public class WorldRendererContainer extends AnchorPane {

	public final WorldRendererCanvas renderer;

	public WorldRendererContainer(WorldRendererCanvas renderer) {
		this.renderer = Objects.requireNonNull(renderer);

		BorderPane wrapper = new BorderPane(renderer);
		wrapper.setStyle("-fx-background-color: red");
		wrapper.setPickOnBounds(true);
		renderer.widthProperty().bind(wrapper.widthProperty());
		renderer.heightProperty().bind(wrapper.heightProperty());
		AnchorPane.setTopAnchor(wrapper, 0D);
		AnchorPane.setBottomAnchor(wrapper, 0D);
		AnchorPane.setLeftAnchor(wrapper, 0D);
		AnchorPane.setRightAnchor(wrapper, 0D);
		getChildren().add(wrapper);
	}
}