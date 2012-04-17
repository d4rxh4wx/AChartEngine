/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.achartengine;

import java.util.Collections;
import java.util.List;

import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.RoundChart;
import org.achartengine.chart.XYChart;
import org.achartengine.model.Point;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.tools.FitZoom;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.Zoom;
import org.achartengine.tools.ZoomListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * The view that encapsulates the graphical chart.
 */
public class GraphicalView extends View {
  /** The chart to be drawn. */
  private AbstractChart mChart;
  /** The chart renderer. */
  private DefaultRenderer mRenderer;
  /** The view bounds. */
  private Rect mRect = new Rect();
  /** The user interface thread handler. */
  private Handler mHandler;
  /** The zoom buttons rectangle. */
  private RectF mZoomR = new RectF();
  /** The zoom in icon. */
  private Bitmap zoomInImage;
  /** The zoom out icon. */
  private Bitmap zoomOutImage;
  /** The fit zoom icon. */
  private Bitmap fitZoomImage;
  /** The zoom area size. */
  private int zoomSize = 50;
  /** The zoom buttons background color. */
  private static final int ZOOM_BUTTONS_COLOR = Color.argb(175, 150, 150, 150);
  /** The zoom in tool. */
  private Zoom mZoomIn;
  /** The zoom out tool. */
  private Zoom mZoomOut;
  /** The fit zoom tool. */
  private FitZoom mFitZoom;
  /** The paint to be used when drawing the chart. */
  private Paint mPaint = new Paint();
  /** The touch handler. */
  private ITouchHandler mTouchHandler;
  /** The old x coordinate. */
  private float oldX;
  /** The old y coordinate. */
  private float oldY;

    public GraphicalView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        AbstractChart chart = createInitialChart();
        init(chart);
    }

    public GraphicalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        AbstractChart chart = createInitialChart();
        init(chart);
    }

    public GraphicalView(Context context) {
        super(context);
        AbstractChart chart = createInitialChart();
        init(chart);
    }

    /**
     * Creates an initial chart. Required by constructors invoked by the XML
     * inflater. Override this method if when you extend {@link GraphicalView},
     * which does not support inflation thus throws a NullPointerException.
     * 
     * @return chart
     */
    protected AbstractChart createInitialChart() {
        // this doesn't have an initial chart
        throw new NullPointerException(
                "createInitialChart() should be overriden to use XML!");
    }

/**
   * Creates a new graphical view.
   * 
   * @param context the context
   * @param chart the chart to be drawn
   */
  public GraphicalView(Context context, AbstractChart chart) {
    super(context);
    init(chart);
  }

private void init(AbstractChart chart) {
    mChart = chart;
    mHandler = new Handler();
    if (mChart instanceof XYChart) {
      mRenderer = ((XYChart) mChart).getRenderer();
    } else {
      mRenderer = ((RoundChart) mChart).getRenderer();
    }
    if (mRenderer.isZoomButtonsVisible()) {
      zoomInImage = BitmapFactory.decodeStream(GraphicalView.class
          .getResourceAsStream("image/zoom_in.png"));
      zoomOutImage = BitmapFactory.decodeStream(GraphicalView.class
          .getResourceAsStream("image/zoom_out.png"));
      fitZoomImage = BitmapFactory.decodeStream(GraphicalView.class
          .getResourceAsStream("image/zoom-1.png"));
    }

    if (mRenderer instanceof XYMultipleSeriesRenderer
        && ((XYMultipleSeriesRenderer) mRenderer).getMarginsColor() == XYMultipleSeriesRenderer.NO_COLOR) {
      ((XYMultipleSeriesRenderer) mRenderer).setMarginsColor(mPaint.getColor());
    }
    if (mRenderer.isZoomEnabled() && mRenderer.isZoomButtonsVisible()
        || mRenderer.isExternalZoomEnabled()) {
      mZoomIn = new Zoom(mChart, true, mRenderer.getZoomRate());
      mZoomOut = new Zoom(mChart, false, mRenderer.getZoomRate());
      mFitZoom = new FitZoom(mChart);
    }
    int version = 7;
    try {
      version = Integer.valueOf(Build.VERSION.SDK);
    } catch (Exception e) {
      // do nothing
    }
    if (version < 7) {
      mTouchHandler = new TouchHandlerOld(this, mChart);
    } else {
      mTouchHandler = new TouchHandler(this, mChart);
    }
}

  public void overrideXYMarginsColor(int color) {
		// hack du projet initial achartengine pour simuler que la zone de
		// dessin ne s'étende pas au delà des axes des abscisses et des
		// ordonnées
		if (mRenderer instanceof XYMultipleSeriesRenderer) {
			((XYMultipleSeriesRenderer) mRenderer).setMarginsColor(color);
		}
  }

  /**
   * Returns the current series selection object.
   * 
   * @return the series selection
   */
  public SeriesSelection getCurrentSeriesAndPoint() {
    return mChart.getSeriesAndPointForScreenCoordinate(new Point(oldX, oldY));
  }

  public List<SeriesSelection> getAllCurrentSeriesAndPoint() {
    if (mChart instanceof XYChart) {
      XYChart chart = (XYChart) mChart;
      return chart.getSeriesAndPointMatchingXValue(new Point(oldX, oldY));
    }
    return Collections.emptyList();
  }
  
  /**
   * Transforms the currently selected screen point to a real point.
   * 
   * @param scale the scale
   * @return the currently selected real point
   */
  public double[] toRealPoint(int scale) {
    if (mChart instanceof XYChart) {
      XYChart chart = (XYChart) mChart;
      return chart.toRealPoint(oldX, oldY, scale);
    }
    return null;
  }

  Handler myHandler = new AnimationHandler();
  
  class AnimationHandler extends Handler {
	  @Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		super.handleMessage(msg);
		invalidate();
	}
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.getClipBounds(mRect);
    int top = mRect.top;
    int left = mRect.left;
    int width = mRect.width();
    int height = mRect.height();
    if (mRenderer.isInScroll()) {
      top = 0;
      left = 0;
      width = getMeasuredWidth();
      height = getMeasuredHeight();
    }
    mChart.draw(canvas, left, top, width, height, mPaint);
    if (mRenderer != null && mRenderer.isZoomEnabled() && mRenderer.isZoomButtonsVisible()) {
      mPaint.setColor(ZOOM_BUTTONS_COLOR);
      zoomSize = Math.max(zoomSize, Math.min(width, height) / 7);
      mZoomR.set(left + width - zoomSize * 3, top + height - zoomSize * 0.775f, left + width, top
          + height);
      canvas.drawRoundRect(mZoomR, zoomSize / 3, zoomSize / 3, mPaint);
      float buttonY = top + height - zoomSize * 0.625f;
      canvas.drawBitmap(zoomInImage, left + width - zoomSize * 2.75f, buttonY, null);
      canvas.drawBitmap(zoomOutImage, left + width - zoomSize * 1.75f, buttonY, null);
      canvas.drawBitmap(fitZoomImage, left + width - zoomSize * 0.75f, buttonY, null);
    }
    long delay = mRenderer.getDelayAnimation() / (mChart.getNumberOfAnimatedSteps() * 1L);
    if (mRenderer.isAnimated() && mChart.isAnimatable() && !mChart.isAnimationFinished()) {
    	repaintDelayed(delay);
    }
  }

  /**
   * Sets the zoom rate.
   * 
   * @param rate the zoom rate
   */
  public void setZoomRate(float rate) {
    if (mZoomIn != null && mZoomOut != null) {
      mZoomIn.setZoomRate(rate);
      mZoomOut.setZoomRate(rate);
    }
  }

  /**
   * Do a chart zoom in.
   */
  public void zoomIn() {
    if (mZoomIn != null) {
      mZoomIn.apply();
      repaint();
    }
  }

  /**
   * Do a chart zoom out.
   */
  public void zoomOut() {
    if (mZoomOut != null) {
      mZoomOut.apply();
      repaint();
    }
  }

  /**
   * Do a chart zoom reset / fit zoom.
   */
  public void zoomReset() {
    if (mFitZoom != null) {
      mFitZoom.apply();
      mZoomIn.notifyZoomResetListeners();
      repaint();
    }
  }

  /**
   * Adds a new zoom listener.
   * 
   * @param listener zoom listener
   */
  public void addZoomListener(ZoomListener listener, boolean onButtons, boolean onPinch) {
    if (onButtons) {
      if (mZoomIn != null) {
        mZoomIn.addZoomListener(listener);
        mZoomOut.addZoomListener(listener);
      }
      if (onPinch) {
        mTouchHandler.addZoomListener(listener);
      }
    }
  }

  /**
   * Removes a zoom listener.
   * 
   * @param listener zoom listener
   */
  public synchronized void removeZoomListener(ZoomListener listener) {
    if (mZoomIn != null) {
      mZoomIn.removeZoomListener(listener);
      mZoomOut.removeZoomListener(listener);
    }
    mTouchHandler.removeZoomListener(listener);
  }

  /**
   * Adds a new pan listener.
   * 
   * @param listener pan listener
   */
  public void addPanListener(PanListener listener) {
    mTouchHandler.addPanListener(listener);
  }

  /**
   * Removes a pan listener.
   * 
   * @param listener pan listener
   */
  public void removePanListener(PanListener listener) {
    mTouchHandler.removePanListener(listener);
  }

  protected RectF getZoomRectangle() {
    return mZoomR;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      // save the x and y so they can be used in the click and long press
      // listeners
      oldX = event.getX();
      oldY = event.getY();
    }
    if (mRenderer != null && (mRenderer.isPanEnabled() || mRenderer.isZoomEnabled())) {
      if (mTouchHandler.handleTouch(event)) {
        return true;
      }
    }
    return super.onTouchEvent(event);
  }
  
  /**
   * Schedule a view content repaint with a delay.
   * @param delayMillis delay in milliseconds
   */
  public void repaintDelayed(final long delayMillis) {
	  
    mHandler.postDelayed(new Runnable() {
      public void run() {
        invalidate();
      }
    }, delayMillis);
  }

  /**
   * Schedule a view content repaint.
   */
  public void repaint() {
    mHandler.post(new Runnable() {
      public void run() {
        invalidate();
      }
    });
  }

  /**
   * Schedule a view content repaint, in the specified rectangle area.
   * 
   * @param left the left position of the area to be repainted
   * @param top the top position of the area to be repainted
   * @param right the right position of the area to be repainted
   * @param bottom the bottom position of the area to be repainted
   */
  public void repaint(final int left, final int top, final int right, final int bottom) {
    mHandler.post(new Runnable() {
      public void run() {
        invalidate(left, top, right, bottom);
      }
    });
  }

  /**
   * Saves the content of the graphical view to a bitmap.
   * 
   * @return the bitmap
   */
  public Bitmap toBitmap() {
    setDrawingCacheEnabled(false);
    if (!isDrawingCacheEnabled()) {
      setDrawingCacheEnabled(true);
    }
    if (mRenderer.isApplyBackgroundColor()) {
      setDrawingCacheBackgroundColor(mRenderer.getBackgroundColor());
    }
    setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    return getDrawingCache(true);
  }

  public DefaultRenderer getRenderer() {
    return mRenderer;
  }

  
  
}