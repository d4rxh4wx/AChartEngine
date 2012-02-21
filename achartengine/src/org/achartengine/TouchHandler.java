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

import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.RoundChart;
import org.achartengine.chart.XYChart;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.tools.Pan;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.Zoom;
import org.achartengine.tools.ZoomListener;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * The main handler of the touch events.
 */
public class TouchHandler implements ITouchHandler {
  /** The chart renderer. */
  private DefaultRenderer mRenderer;
  /** The old x coordinate. */
  private float oldX;
  /** The old y coordinate. */
  private float oldY;
  /** The old x2 coordinate. */
  private float oldX2;
  /** The old y2 coordinate. */
  private float oldY2;
  /** The zoom buttons rectangle. */
  private RectF zoomR = new RectF();
  /** The pan tool. */
  private Pan mPan;
  /** The zoom for the pinch gesture. */
  private Zoom mPinchZoom;
  /** The graphical view. */
  private GraphicalView graphicalView;
  
  private boolean panningX = false;
  private boolean panningY = false;
  
  private boolean zoomingX = false;
  private boolean zoomingY = false;
  
  private GestureDetector gestureDetector;

  private class ScrollAnimatorUpdateListener implements AnimatorUpdateListener {

		private GraphicalView view;
		private float oldX;
		private float oldY;
		private float newX;
		
		public ScrollAnimatorUpdateListener(GraphicalView view) {
			this.view = view;
		}

		//@Override
		public void onAnimationUpdate(ValueAnimator valueAnimator) {
			mPan.apply(oldX, oldY, newX, oldY);
			this.view.repaint();
			this.oldX = newX;
		}

		public void setOldX(float oldX) {
			this.oldX = oldX;
		}

		public void setOldY(float oldY) {
			this.oldY = oldY;
		}

		@SuppressWarnings("unused")
		public float getNewX() {
			return newX;
		}

		public void setNewX(float newX) {
			this.newX = newX;
		}
		
		

		
		
		
		
	}
  
  /**
   * Creates a new graphical view.
   * 
   * @param view the graphical view
   * @param chart the chart to be drawn
   */
  public TouchHandler(final GraphicalView view, AbstractChart chart) {
	gestureDetector = new GestureDetector(view.getContext(), new GestureDetector.OnGestureListener() {
		
		private final float MIN_VELOCITY = ViewConfiguration.getMinimumFlingVelocity();
		private final float MIN_DISTANCE = 100;
		private final int TIME = 1000;
		
		private ObjectAnimator anim;
		private final Interpolator animInterpolator = new DecelerateInterpolator();
		
		private final ScrollAnimatorUpdateListener animListener = new ScrollAnimatorUpdateListener(view);
		
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
		
		public void onShowPress(MotionEvent e) {
			
		}
		
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY) {
			return false;
		}
		
		public void onLongPress(MotionEvent e) {
			
		}
		
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX,
				float velocityY) {
			final float ev1x = event1.getX();
            final float ev1y = event1.getY();
            final float ev2x = event2.getX();
            //final float ev2y = event2.getY();
            final float xdiff = Math.abs(ev1x - ev2x);
            //final float ydiff = Math.abs(ev1y - ev2y);
            final float xvelocity = Math.abs(velocityX);
            //final float yvelocity = Math.abs(velocityY);
            
            if(xvelocity > this.MIN_VELOCITY && xdiff > this.MIN_DISTANCE) {
                	animListener.setOldX(ev1x);
                	animListener.setOldY(ev1y);
                	animListener.setNewX(ev1x);
                	anim = ObjectAnimator.ofFloat(animListener, "newX", ev1x + velocityX * TIME / 1000);
        			anim.addUpdateListener(animListener);
        			anim.setInterpolator(animInterpolator);
        			anim.setDuration(TIME);
        			anim.start();
        			return true;
                    
            }
			return false;
		}
		
		public boolean onDown(MotionEvent e) {
			if (anim != null && anim.isRunning()) {
				anim.cancel();
			}
			return false;
		}
		
		
	});
    graphicalView = view;
    zoomR = graphicalView.getZoomRectangle();
    if (chart instanceof XYChart) {
      mRenderer = ((XYChart) chart).getRenderer();
    } else {
      mRenderer = ((RoundChart) chart).getRenderer();
    }
    if (mRenderer.isPanEnabled()) {
      mPan = new Pan(chart);
    }
    if (mRenderer.isZoomEnabled()) {
      mPinchZoom = new Zoom(chart, true, 1);
    }
  }

  /**
   * Handles the touch event.
   * 
   * @param event the touch event
   */
  public boolean handleTouch(MotionEvent event) {
	if (gestureDetector.onTouchEvent(event)) {
		return true;
	}
    int action = event.getAction();
    if (mRenderer != null && action == MotionEvent.ACTION_MOVE) {
      if (oldX >= 0 || oldY >= 0) {
        float newX = event.getX(0);
        float newY = event.getY(0);
        if (event.getPointerCount() > 1 && (oldX2 >= 0 || oldY2 >= 0) && mRenderer.isZoomEnabled()) {
          float newX2 = event.getX(1);
          float newY2 = event.getY(1);
          float newDeltaX = Math.abs(newX - newX2);
          float newDeltaY = Math.abs(newY - newY2);
          float oldDeltaX = Math.abs(oldX - oldX2);
          float oldDeltaY = Math.abs(oldY - oldY2);
          float ratioDeltaX = newDeltaX / oldDeltaX;
          float ratioDeltaY = newDeltaY / oldDeltaY;
          // which one is further to 1 ? if so, then zooming on this axis
          boolean zoomingOnXAxis = Math.abs(1 - ratioDeltaX) < Math.abs(1 - ratioDeltaY);
          float zoomRate = 1;
          if (mRenderer.isZoomStrict()) {
        	  if (zoomingX) {
        		  zoomRate = newDeltaX / oldDeltaX;
        	  } else if (zoomingY) {
        		  zoomRate = newDeltaY / oldDeltaY;
        	  } else if (zoomingOnXAxis) {
        		  zoomRate = newDeltaX / oldDeltaX;
        		  zoomingX = true;
        	  } else {
        		  zoomRate = newDeltaY / oldDeltaY;
        		  zoomingY = true;
        	  }
          } else {
        	  zoomingX = true;
        	  zoomingY = true;
        	  if (zoomingOnXAxis) {
                  zoomRate = newDeltaX / oldDeltaX;
        	  } else {
        		  zoomRate = newDeltaY / oldDeltaY;
        	  }
          }
          if (zoomRate > 0.909 && zoomRate < 1.1 && zoomRate != 1) {
            mPinchZoom.setZoomRate(zoomRate);
            mPinchZoom.apply(zoomingX, zoomingY);
          }
          oldX2 = newX2;
          oldY2 = newY2;
        } else if (mRenderer.isPanEnabled()) {
          if (mRenderer.isPanStrict()) {
        	if (panningX) {
        		newY = oldY;
        	} else if (panningY) {
        		newX = oldX;
        	} else if (Math.abs(newX - oldX) >= Math.abs(newY - oldY)) {
        		panningX = true;
        	} else {
        		panningY = true;
        	}
          }
          mPan.apply(oldX, oldY, newX, newY);
          oldX2 = 0;
          oldY2 = 0;
        }
        oldX = newX;
        oldY = newY;
        graphicalView.repaint();
        return true;
      }
    } else if (action == MotionEvent.ACTION_DOWN) {
      oldX = event.getX(0);
      oldY = event.getY(0);
      panningX = false;
      panningY = false;
      zoomingX = false;
      zoomingY = false;
      if (mRenderer != null && mRenderer.isZoomEnabled() && zoomR.contains(oldX, oldY)) {
        if (oldX < zoomR.left + zoomR.width() / 3) {
          graphicalView.zoomIn();
        } else if (oldX < zoomR.left + zoomR.width() * 2 / 3) {
          graphicalView.zoomOut();
        } else {
          graphicalView.zoomReset();
        }
        return true;
      }
    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
      oldX = 0;
      oldY = 0;
      oldX2 = 0;
      oldY2 = 0;
      if (action == MotionEvent.ACTION_POINTER_UP) {
        oldX = -1;
        oldY = -1;
      }
      panningX = false;
      panningY = false;
      zoomingX = false;
      zoomingY = false;
    }
    return !mRenderer.isClickEnabled();
  }

  /**
   * Adds a new zoom listener.
   * 
   * @param listener zoom listener
   */
  public void addZoomListener(ZoomListener listener) {
    if (mPinchZoom != null) {
      mPinchZoom.addZoomListener(listener);
    }
  }

  /**
   * Removes a zoom listener.
   * 
   * @param listener zoom listener
   */
  public void removeZoomListener(ZoomListener listener) {
    if (mPinchZoom != null) {
      mPinchZoom.removeZoomListener(listener);
    }
  }

  /**
   * Adds a new pan listener.
   * 
   * @param listener pan listener
   */
  public void addPanListener(PanListener listener) {
    if (mPan != null) {
      mPan.addPanListener(listener);
    }
  }

  /**
   * Removes a pan listener.
   * 
   * @param listener pan listener
   */
  public void removePanListener(PanListener listener) {
    if (mPan != null) {
      mPan.removePanListener(listener);
    }
  }
}