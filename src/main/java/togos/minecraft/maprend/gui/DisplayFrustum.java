package togos.minecraft.maprend.gui;

import java.util.Observable;
import org.joml.AABBd;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * An object of this class represents the viewport frustum of a {@link WorldRendererCanvas}. All units are in world coordinates by default, meaning one unit is
 * equal to one block.
 */
public class DisplayFrustum {

	public static final int		MAX_ZOOM_LEVEL	= 7;															// up to 9 without rounding errors

	/** The size of the viewport is needed to calculate the frustum's size in world coordinates. */
	public final DoubleProperty	widthProperty	= new SimpleDoubleProperty(800);
	/** The size of the viewport is needed to calculate the frustum's size in world coordinates. */
	public final DoubleProperty	heightProperty	= new SimpleDoubleProperty(500);

	/**
	 * A linear scaling factor to zoom in or out on the map. Larger values mean zoom in, smaller values mean zoom out. Avoid changing this property directly
	 * because it leads to inconsistency between scale and zoom. Use {@link #zoomProperty} instead. <br/>
	 * A change in this value will scale the map to the new scaling value accordingly. The center of the scaling operation will be the mouse's current position.
	 * Then, {@link #frustumChanged()} is called.
	 */
	public final DoubleProperty	scaleProperty	= new SimpleDoubleProperty(1);
	/**
	 * The zoom is unlike the scale not linear, but on an exponential basis. It is defined as <code>scale=Math.pow(2, zoom)</code>. A zoom of zero means a scale
	 * of 1:1, each unit increase means zooming out by a factor of 2, a each unit decrease results in zooming in by a factor of 2.<br/>
	 * Each change of this value will result in {@link #scaleProperty} to be adjusted and the frustum to be recaltulated.
	 */
	public final DoubleProperty	zoomProperty	= new SimpleDoubleProperty(0);
	/**
	 * Add a listener to get notified when the frustum has changed, which usually results in the need of redrawing the map to reflect the changes in the
	 * viewport.
	 */
	public Observable			repaint			= new Observable() {

													@Override
													public synchronized void notifyObservers(Object arg) {
														setChanged();											// Why the hell is this method protected?
														super.notifyObservers(arg);
													}
												};

	protected boolean			dragging		= false;
	protected Vector3d			mousePos		= new Vector3d();
	protected Vector3d			translation		= new Vector3d(0, 0, 0);
	protected Timeline			zoomTimeline;
	protected AABBd				frustum;

	public DisplayFrustum() {
		scaleProperty.bind(Bindings.createDoubleBinding(() -> Math.pow(2, zoomProperty.get()), zoomProperty));
		scaleProperty.addListener((e, oldScale, newScale) -> {
			// In world coordinates
			Vector3d cursorPos = new Vector3d(mousePos).div(oldScale.doubleValue()).sub(translation);
			translation.add(cursorPos);
			translation.div(newScale.doubleValue() / oldScale.doubleValue());
			translation.sub(cursorPos);

			frustumChanged();
		});

		widthProperty.addListener(e -> frustumChanged());
		heightProperty.addListener(e -> frustumChanged());
	}

	/**
	 * Sets the new location of the mouse.
	 * 
	 * @param x the new x-coordinate of the mouse, in local coordinates of the according canvas.
	 * @param y the new y-coordinate of the mouse, in local coordinates of the according canvas.
	 * @see Node#boundsInLocalProperty()
	 */
	public void mouseMove(double x, double y) {
		Vector3d lastCursor = new Vector3d(mousePos);
		mousePos.set(x, y, 0);

		if (dragging) {
			// Difference in world space
			Vector3d dragDist = new Vector3d(lastCursor).sub(mousePos).negate();
			double scale = scaleProperty.get();
			translation.add(new Vector3d(dragDist.x / scale, dragDist.y / scale, 0));
			frustumChanged();
		}
	}

	/**
	 * Zooms in or out around the position currently shown by the mouse cursor. The zooming will be animated with an eased interpolation for 100 milliseconds.
	 * 
	 * @param deltaZoom The amount of change to the zoom property. 1 means zoom out by a factor of 2, -1 means zoom in by a factor of 2.
	 * @see #zoomProperty
	 */
	public void mouseScroll(double deltaZoom) {
		double currentValue = zoomProperty.get();
		double missingTime = 0;
		if (zoomTimeline != null) {
			missingTime = (zoomTimeline.getTotalDuration().toMillis() - zoomTimeline.getCurrentTime().toMillis()) / 2;
			zoomTimeline.jumpTo("end");
		}
		double scale = zoomProperty.get();
		zoomProperty.set(currentValue);
		scale += deltaZoom;
		if (scale > MAX_ZOOM_LEVEL)
			scale = MAX_ZOOM_LEVEL;
		if (scale < -MAX_ZOOM_LEVEL)
			scale = -MAX_ZOOM_LEVEL;
		zoomTimeline = new Timeline(new KeyFrame(Duration.millis(100 + missingTime), new KeyValue(zoomProperty, scale, Interpolator.EASE_BOTH)));
		zoomTimeline.play();
	}

	/** Call this to tell if the mouse is currently dragging or not. */
	public void buttonPress(boolean pressed) {
		dragging = pressed;
	}

	/**
	 * Rebuilds the view frustum and calls for a repaint. This method is called automatically after changing any of the relevant values, but there might be the
	 * need of requesting a recalculation manually.
	 * 
	 * @see #getFrustum()
	 * @see #repaint
	 */
	public void frustumChanged() {
		frustum = new AABBd(// TODO optimize
				new Vector3d(000, 000, 0).div(scaleProperty.get()).sub(translation.x, translation.y, 0),
				new Vector3d(widthProperty.get() - 000, heightProperty.get() - 000, 0).div(scaleProperty.get()).sub(translation.x, translation.y, 0));
		// frustum = new AABBd(-Double.MAX_VALUE, -Double.MAX_VALUE, 0, Double.MAX_VALUE, Double.MAX_VALUE, 0);
		// System.out.println(frustum);
		repaint.notifyObservers();
	}

	/**
	 * Returns the currently visible part of the world. The coordinates are world coordinates, relative to the world's origin with one unit corresponding to one
	 * // * block. The vertical range of this AABBd (the z axis) is from zero to zero. This object is <i>not</i> immutable and should not be changed.
	 * {@link #frustumChanged()} replaces it with a new instance on every call.
	 */
	public AABBd getFrustum() {
		return frustum;
	}

	/**
	 * The zoom level is just a rounded version of {@link #zoomProperty}. The result can be interpreted as level of detail as per common definition. Note that
	 * when zoomed in this returns negative values. A "negative" LOD (increase of detail) is needed because JavaFX does not support nearest image interpolation
	 * to display the image correctly.
	 */
	public int getZoomLevel() {
		return (int) Math.ceil(zoomProperty.get());
	}

	public Vector3dc getMousePos() {
		return mousePos;
	}

	public Vector3dc getTranslation() {
		return translation;
	}
}