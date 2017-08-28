package togos.minecraft.maprend.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.joml.AABBd;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Robot;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import togos.minecraft.maprend.BoundingRect;
import togos.minecraft.maprend.RegionMap;
import togos.minecraft.maprend.RegionRenderer;
import togos.minecraft.maprend.gui.RenderedRegion.RenderingState;
import togos.minecraft.maprend.io.RegionFile;

@SuppressWarnings("restriction")
public class WorldRendererFX extends Canvas implements Runnable {

	public static final int					THREAD_COUNT	= 4;
	public static final int					MAX_ZOOM_LEVEL	= 7;												// up to 9 without rounding errors

	protected RegionRenderer				renderer;
	protected RenderedMap					map;

	protected boolean						dragging		= false;
	protected Vector3d						mousePos		= new Vector3d();
	protected Vector3d						translation		= new Vector3d(0, 0, 0);
	protected double						realScale		= 1;
	protected DoubleProperty				scaleProperty	= new SimpleDoubleProperty();
	protected Timeline						scaleTimeline;
	protected AABBd							frustum;

	protected ScheduledThreadPoolExecutor	executor;
	protected final List<Future<?>>			submitted		= Collections.synchronizedList(new LinkedList<>());

	protected GraphicsContext				gc				= getGraphicsContext2D();
	protected Robot							robot			= Application.GetApplication().createRobot();

	public WorldRendererFX(RegionRenderer renderer) {
		executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(THREAD_COUNT);
		map = new RenderedMap(executor);// Executors.newScheduledThreadPool(THREAD_COUNT / 2));
		executor.scheduleAtFixedRate(() -> {
			try {
				// TODO execute more often if something changes and less often if not
				// update upscaled/downscaled images of chunks
				int level = (int) Math.ceil(scaleProperty.get());
				if (map.updateImage(level, frustum))
					repaint();
			} catch (Throwable e) {
				e.printStackTrace();
				throw e;
			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
		executor.scheduleWithFixedDelay(map::evictCache, 10, 10, TimeUnit.SECONDS);

		executor.setKeepAliveTime(20, TimeUnit.SECONDS);
		executor.allowCoreThreadTimeOut(true);

		this.renderer = Objects.requireNonNull(renderer);

		{// Listeners
			setOnMouseMoved(e -> mouseMove(e.getX(), e.getY()));
			setOnMouseDragged(e -> mouseMove(e.getX(), e.getY()));
			setOnMousePressed(e -> {
				if (e.getButton().ordinal() == 3)
					buttonPress(true);
			});
			setOnMouseReleased(e -> {
				if (e.getButton().ordinal() == 3)
					buttonPress(false);
			});
			setOnScroll(e -> mouseScroll(e.getTextDeltaY()));
			InvalidationListener l = e -> {
				frustumChanged();
				repaint();
			};
			widthProperty().addListener(l);
			heightProperty().addListener(l);
			scaleProperty.addListener(e -> onScrollChanged());
		}

		// loadWorld(file);
		invalidateTextures();
		frustumChanged();
		repaint();
	}

	public void loadWorld(File file) {
		// regions.clear();
		// regions = RegionMap.load(file, BoundingRect.INFINITE).regions.stream().collect(Collectors.toMap(r -> new Vector2i(r.rx, r.rz), r -> new
		// RenderedRegion2(r)));
		// map.clear();
		map.clearReload(RegionMap.load(file, BoundingRect.INFINITE).regions);
		invalidateTextures();
	}

	public void invalidateTextures() {
		map.invalidateAll();
		for (int i = 0; i < THREAD_COUNT; i++)
			executor.submit(this);
	}

	public void shutDown() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		map.close();
	}

	/** Queues in a repaint event calling {@link renderWorld} from the JavaFX Application Thread */
	public void repaint() {
		Platform.runLater(this::renderWorld);
	}

	/** Requires the projection to be set up to {@code GL11.glOrtho(0, width, height, 0, -1, 1);} and to be called from the JavaFX Application Thread. */
	public void renderWorld() {
		gc = getGraphicsContext2D();
		// gc.setStroke(Color.GREEN.deriveColor(0, 1, 1, .2));
		gc.setLineWidth(10);
		// gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setFill(new Color(0.2f, 0.2f, 0.6f, 1.0f));
		gc.fillRect(0, 0, getWidth(), getHeight());

		gc.save();
		gc.scale(realScale, realScale);
		gc.translate(translation.x, translation.y);

		int level = (int) Math.ceil(scaleProperty.get());
		map.draw(gc, level, frustum, realScale);
		gc.restore();

		if (map.isNothingLoaded()) {
			gc.setFill(Color.WHITE);
			gc.setFont(Font.font(20));
			gc.fillText("No regions loaded", 10, getHeight() - 10);
		}

		// gc.strokeRect(100, 100, getWidth() - 200, getHeight() - 200);
		// gc.strokeRect(0, 0, getWidth() - 0, getHeight() - 0);
	}

	public void mouseMove(double x, double y) {
		Vector3d lastCursor = new Vector3d(mousePos);
		mousePos.set(x, y, 0);

		if (dragging) {
			// Difference in world space
			Vector3d dragDist = new Vector3d(lastCursor).sub(mousePos).negate();
			translation.add(new Vector3d(dragDist.x / realScale, dragDist.y / realScale, 0));
			frustumChanged();
			repaint();

			// TODO limit dragging to prevent the map of going out of bounds

			// Wrap mouse when dragging beyond bounds. TODO fix and add it back
			// Bounds screen = localToScreen(getBoundsInLocal());
			// int mx = robot.getMouseX();
			// int my = robot.getMouseY();
			//
			// int dx = 0, dy = 0;
			// if (mx <= screen.getMinX()) {
			// dx = 1;
			// robot.mouseMove((int) (screen.getMaxX() - 2), my);
			// }
			// if (mx >= screen.getMaxX() - 1) {
			// dx = -1;
			// robot.mouseMove((int) (screen.getMinX() + 1), my);
			// }
			// if (my <= screen.getMinY()) {
			// dy = 1;
			// robot.mouseMove(mx, (int) (screen.getMaxY() - 2));
			// }
			// if (my >= screen.getMaxY() - 1) {
			// dy = -1;
			// robot.mouseMove(mx, (int) (screen.getMinY() + 1));
			// }
			//
			// dragDist = new Vector3d(screen.getMaxX() - screen.getMinX(), screen.getMaxY() - screen.getMinY(), 0).mul(dx, dy, 0).negate();
			// translation.add(new Vector3d(dragDist.x / realScale, dragDist.y / realScale, 0));
		}
	}

	public void mouseScroll(double dy) {
		double currentValue = scaleProperty.get();
		double missingTime = 0;
		if (scaleTimeline != null) {
			missingTime = (scaleTimeline.getTotalDuration().toMillis() - scaleTimeline.getCurrentTime().toMillis()) / 2;
			scaleTimeline.jumpTo("end");
		}
		double scale = scaleProperty.get();
		scaleProperty.set(currentValue);
		scale += dy / 10;
		if (scale > MAX_ZOOM_LEVEL)
			scale = MAX_ZOOM_LEVEL;
		if (scale < -MAX_ZOOM_LEVEL)
			scale = -MAX_ZOOM_LEVEL;
		scaleTimeline = new Timeline(new KeyFrame(Duration.millis(100 + missingTime), new KeyValue(scaleProperty, scale, Interpolator.EASE_BOTH)));
		scaleTimeline.play();
	}

	private void onScrollChanged() {
		double oldScale = realScale;
		realScale = Math.pow(2, scaleProperty.get());
		// In world coordinates
		Vector3d cursorPos = new Vector3d(mousePos).div(oldScale).sub(translation);
		translation.add(cursorPos);
		translation.div(realScale / oldScale);
		translation.sub(cursorPos);
		frustumChanged();
		repaint();
	}

	public void buttonPress(boolean pressed) {
		dragging = pressed;
	}

	/* Rebuild view frustum */
	protected void frustumChanged() {
		frustum = new AABBd(
				new Vector3d(000, 000, 0).div(realScale).sub(translation.x, translation.y, 0),
				new Vector3d(getWidth() - 000, getHeight() - 000, 0).div(realScale).sub(translation.x, translation.y, 0));
		// frustum = new AABBd(-Double.MAX_VALUE, -Double.MAX_VALUE, 0, Double.MAX_VALUE, Double.MAX_VALUE, 0);
		// System.out.println(frustum);
	}

	public boolean isVisible(Vector2f point) {
		return true;
	}

	public boolean isVisible(Vector2i region) {
		return true;
	}

	public RegionRenderer getRegionRenderer() {
		return renderer;
	}

	@Override
	public void run() {
		RenderedRegion region = null;
		region = nextRegion();
		if (region == null)
			return;
		repaint();
		try {
			try (RegionFile rf = new RegionFile(region.region.regionFile)) {
				BufferedImage texture2 = null;
				do {
					texture2 = renderer.render(rf);
				} while (region.valid.compareAndSet(RenderingState.REDRAW, RenderingState.DRAWING) && !Thread.interrupted());

				WritableImage texture = SwingFXUtils.toFXImage(texture2, null);
				region.setImage(texture);
				repaint();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			region.valid.set(RenderingState.VALID);
			executor.submit(this);
		}
	}

	/** Returns the next Region to render */
	protected synchronized RenderedRegion nextRegion() {
		// In region coordinates
		Vector3d cursorPos = new Vector3d(mousePos).div(realScale).sub(translation).div(512).sub(.5, .5, 0);

		Comparator<RenderedRegion> comp = (a, b) -> Double.compare(new Vector3d(a.position.x(), a.position.y(), 0).sub(cursorPos).length(), new Vector3d(b.position.x(), b.position.y(), 0).sub(cursorPos).length());
		RenderedRegion min = null;
		for (RenderedRegion r : map.get(0).values())
			if (r.valid.get() == RenderingState.INVALID && (min == null || comp.compare(min, r) > 0))
				min = r;
		if (min != null)
			// min got handled by another Thread already (while we were still searching), so get a new one
			if (!min.valid.compareAndSet(RenderingState.INVALID, RenderingState.DRAWING))
			return nextRegion();
		return min;
	}

	/** return a*2^n */
	public static int pow2(int a, int n) {
		if (n > 0)
			return a << n;
		else if (n < 0)
			return a >> -n;
		else
			return a;
	}
}
