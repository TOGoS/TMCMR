package togos.minecraft.maprend.gui.decoration;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import togos.minecraft.maprend.gui.DisplayFrustum;

/**
 * This decoration provides basic drag and zoom support, where you can set the button used for dragging as well as zoom speed and direction. Both
 * functionalities are optional and can be disabled separatedly. If you disable both, this class will still forward mouse moved events to the view frustum. This
 * might be useful if you use manual zooming logic externally and still want to zoom around the mouse center as it does normally.
 */
public class DragScrollDecoration extends Region {

	/** Creates an instance of this class that will drag with the right mouse button and a scroll factor of 1/10. */
	public DragScrollDecoration(DisplayFrustum frustum) {
		this(frustum, MouseButton.SECONDARY, 0.1d);
	}

	/**
	 * @param dragButton The button that must be pressed to activate dragging. <code>null</code> will disable dragging.
	 * @param scrollFactor Higher values will increase the zoomed amount per scroll. Zero deactivates scrolling. Negative values invert the scroll direction.
	 */
	public DragScrollDecoration(DisplayFrustum frustum, MouseButton dragButton, double scrollFactor) {
		setOnMouseMoved(e -> frustum.mouseMove(e.getX(), e.getY()));
		setOnMouseDragged(e -> frustum.mouseMove(e.getX(), e.getY()));

		if (dragButton != null) {
			setOnMousePressed(e -> {
				if (e.getButton().ordinal() == dragButton.ordinal())
					frustum.buttonPress(true);
			});
			setOnMouseReleased(e -> {
				if (e.getButton().ordinal() == dragButton.ordinal())
					frustum.buttonPress(false);
			});
		}
		if (scrollFactor != 0)
			setOnScroll(e -> frustum.mouseScroll(e.getTextDeltaY() * scrollFactor));
	}
}