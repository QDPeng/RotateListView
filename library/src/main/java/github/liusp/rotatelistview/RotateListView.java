package github.liusp.rotatelistview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;

/**
 * 可旋转的环形listview
 *
 * @author liusp
 * @since 2016/07/08
 */
public class RotateListView extends ViewGroup {
    // Event listeners
    private OnItemClickListener onItemClickListener = null;
    private OnItemSelectedListener onItemSelectedListener = null;
    private OnRotationFinishedListener onRotationFinishedListener = null;
    private OnTodayItemSelectedListener onTodayItemSelectedListener = null;

    // Sizes of the ViewGroup
    private int circleWidth, circleHeight;
    private int radius = 0;

    private int childWidth = 0;
    private int childHeight = 0;

    // Touch detection
    private GestureDetector gestureDetector;
    // Detecting inverse rotations
    private boolean[] quadrantTouched;
    // Settings of the ViewGroup
    private int speed = 25;
    private float angle = 90;
    private float totalAngle = 0;
    private FirstChildLocation firstChildPosition = FirstChildLocation.South;
    private boolean isRotating = true;

    private ItemView tappedView = null;
    private int tappedViewAngle = -1;
    private int selected = 0;
    private int entityCount;
    private boolean isFirstItem, isEndItem;
    private int todayIndex = 0;

    // Rotation animator
    private ObjectAnimator animator;
    private int textColor = Color.WHITE;
    private int textSize = 14;//sp
    //circle menu background view
    private View circleBg;
    private View circleUpView;
    //define six item views
    private static final int ITEM_COUNT = 6;
    private ArrayList<ItemEntity> entitys = null;

    public RotateListView(Context context) {
        this(context, null);
        init();
    }

    public RotateListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public RotateListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Initializes the ViewGroup and modifies it's default behavior by the
     * passed attributes
     */
    protected void init() {
        gestureDetector = new GestureDetector(getContext(),
                new MyGestureListener());
        quadrantTouched = new boolean[]{false, false, false, false, false};
        // Needed for the ViewGroup to be drawn
        setWillNotDraw(false);
        setItems();

    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        if (isFirstItem) {
            if (tappedViewAngle == 30) {
                this.angle = angle % 360;
                setChildAngles();
                return;
            } else {
                setChildLayout(0);
                return;
            }

        }
        if (isEndItem) {
            if (tappedViewAngle == 150) {
                this.angle = angle % 360;
                setChildAngles();
                return;
            } else {
                setChildLayout(entityCount - 1);
                return;
            }

        }
        tappedViewAngle = -1;
        this.angle = angle % 360;
        setChildAngles();
    }


    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setFirstChildPosition(FirstChildLocation position) {
        this.firstChildPosition = position;
    }

    public void setRotating(boolean rotating) {
        isRotating = rotating;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setCircleBg(View view) {
        this.circleBg = view;
    }

    public void setCircleUpView(View view) {
        this.circleUpView = view;
    }

    public void setEntitys(ArrayList<ItemEntity> list) {
        if (list == null || list.size() == 0) return;
        this.entitys = list;
        entityCount = list.size();
    }

    public void setTodayIndex(int index) {
        if (index < 0 || index >= entityCount) {
            return;
        }
        this.todayIndex = index;
    }

    private void setItems() {
        removeAllViews();
        for (int i = 0; i < ITEM_COUNT; i++) {
            ItemView itemView = new ItemView(getContext());
            itemView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            itemView.setPosition(i);
            itemView.setText(null);
            itemView.setTextVisible(true);
            itemView.setTextColor(textColor);
            itemView.setTextSize(textSize);
            itemView.setSmallText(null);
            if (i == 0) {
                itemView.setDataIndex(0);
                itemView.setIncreaseId(0);
            } else {
                itemView.setDataIndex(ITEM_COUNT - i);
                itemView.setIncreaseId(ITEM_COUNT - i);
            }
            addView(itemView, i);
        }
    }

    /**
     * Returns the currently selected menu
     *
     * @return the view which is currently the closest to the first item
     * position
     */
    public ItemView getSelectedItem() {
        return (selected >= 0) ? (ItemView) getChildAt(selected) : null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The sizes of the ViewGroup
        circleHeight = getHeight();
        circleWidth = getWidth();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxChildWidth = 0;
        int maxChildHeight = 0;

        // Measure once to find the maximum child size.
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            maxChildWidth = Math.max(maxChildWidth, child.getMeasuredWidth());
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }

        // Measure again for each child to be exactly the same size.
        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxChildWidth,
                MeasureSpec.EXACTLY);
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxChildHeight,
                MeasureSpec.EXACTLY);

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }

        setMeasuredDimension(resolveSize(maxChildWidth, widthMeasureSpec),
                resolveSize(maxChildHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int layoutWidth = r - l;
        int layoutHeight = b - t;

        // Laying out the child views
        final int childCount = getChildCount();
        int left, top;
        radius = (layoutWidth <= layoutHeight) ? layoutWidth * 2 / 5
                : layoutHeight * 2 / 5;

        childWidth = radius;
        childHeight = radius / 2;

        float angleDelay = 360.0f / getChildCount();

        for (int i = 0; i < childCount; i++) {
            final ItemView child = (ItemView) getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            if (angle > 360) {
                angle -= 360;
            } else {
                if (angle < 0) {
                    angle += 360;
                }
            }

            child.setPosition(i);

            left = Math.round((float) (((layoutWidth / 2) - childWidth / 2) + radius
                    * Math.cos(Math.toRadians(angle))));
            top = Math.round((float) (((layoutHeight / 2) - childHeight / 2) + radius
                    * Math.sin(Math.toRadians(angle))));

            child.layout(left, top, left + childWidth, top + childHeight);
            angle += angleDelay;
        }
    }

    /**
     * Rotates the given view to the firstChildPosition
     *
     * @param view the view to be rotated
     */
    private void rotateViewToCenter(ItemView view) {
        if (isRotating) {
            float destAngle = (firstChildPosition.getValue() - view.getAngle());

            if (destAngle < 0) {
                destAngle += 360;
            }

            if (destAngle > 180) {
                destAngle = -1 * (360 - destAngle);
            }
            animateTo(angle + destAngle, 7500 / speed);
        }
    }

    private void rotateButtons(float degrees) {
        angle += degrees;
        setChildAngles();
    }

    private void animateTo(float endDegree, long duration) {
        if (animator != null && animator.isRunning()
                || Math.abs(angle - endDegree) < 1) {
            return;
        }
        animator = ObjectAnimator.ofFloat(this, "angle", angle,
                endDegree);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            private boolean wasCanceled = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (wasCanceled) {
                    return;
                }

                if (onRotationFinishedListener != null) {
                    onRotationFinishedListener.onRotationFinished(getSelectedItem());
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                wasCanceled = true;
            }
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator = null;
        }
    }

    private android.os.Handler handler = new android.os.Handler();

    public void setChildLayout(int listId) {
        if (listId < 0 || listId > entityCount) {
            return;
        }
        if (listId == 0) {
            isFirstItem = true;
        } else {
            isFirstItem = false;
        }
        if (listId == entityCount - 1) {
            isEndItem = true;
        } else {
            isEndItem = false;
        }
        int index = listId % 6;
        if (index != 0) {
            selected = 6 - index;
        } else {
            selected = index;
        }
        this.totalAngle = listId * 60 + 90;
        float localAngle = (90 + index * 60) % 360;
        this.angle = localAngle;
        int left, top, childCount = getChildCount();
        float angleDelay = 360.0f / childCount;
        if (circleBg != null) {
            float rotate = localAngle + 30;
            ViewHelper.setRotation(circleBg, rotate);
        }

        for (int i = 0; i < childCount; i++) {
            if (localAngle > 360) {
                localAngle -= 360;
            } else {
                if (localAngle < 0) {
                    localAngle += 360;
                }
            }

            final ItemView child = (ItemView) getChildAt(i);

            left = Math.round((float) (((circleWidth / 2) - childWidth / 2) + radius
                    * Math.cos(Math.toRadians(localAngle))));
            top = Math.round((float) (((circleHeight / 2) - childHeight / 2) + radius
                    * Math.sin(Math.toRadians(localAngle))));
            child.setAngle(localAngle);
            if (i == selected) {
                child.setTextSize(16);
            } else {
                child.setTextSize(textSize);
            }
            int mAngle = Math.round(localAngle);
            switch (mAngle) {
                case 90:
                    child.setIncreaseId(listId);
                    break;
                case 150:
                    child.setIncreaseId(listId - 1);
                    break;
                case 210:
                    child.setIncreaseId(listId - 2);
                    break;
                case 30:
                    child.setIncreaseId(listId + 1);
                    break;
                case 330:
                    child.setIncreaseId(listId + 2);
                    break;
                case 270:
                    child.setIncreaseId(listId + 3);
                    break;

            }

            if (entitys != null && child.getIncreaseId() >= 0 && child.getIncreaseId() < entityCount) {
                child.setText(entitys.get(child.getIncreaseId()).getName());
                child.setSmallText(entitys.get(child.getIncreaseId()).getSubName());
            } else {
                child.setText(null);
                child.setSmallText(null);
            }

            ViewHelper.setRotation(child, localAngle + 270);
            child.layout(left, top, left + childWidth, top + childHeight);
            localAngle += angleDelay;
        }


    }


    private synchronized void asyncSetChildAngles() {

        int left, top, childCount = getChildCount();
        float angleDelay = 360.0f / childCount;
        float localAngle = angle;
        if (circleBg != null) {
            float rotate = localAngle + 30;
            ViewHelper.setRotation(circleBg, rotate);
        }
        for (int i = 0; i < childCount; i++) {
            if (localAngle > 360) {
                localAngle -= 360;
            } else {
                if (localAngle < 0) {
                    localAngle += 360;
                }
            }

            final ItemView child = (ItemView) getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            left = Math.round((float) (((circleWidth / 2) - childWidth / 2) + radius
                    * Math.cos(Math.toRadians(localAngle))));
            top = Math.round((float) (((circleHeight / 2) - childHeight / 2) + radius
                    * Math.sin(Math.toRadians(localAngle))));
            float itemAngle = child.getAngle();
            if (i == 0) {
                if (itemAngle >= 0 && itemAngle <= 60) {
                    if (localAngle >= 300 && localAngle <= 360) {
                        totalAngle -= itemAngle + (360 - localAngle);
                    } else {
                        totalAngle += localAngle - itemAngle;
                    }

                } else if (itemAngle >= 300 && itemAngle <= 360) {
                    if (localAngle >= 0 && localAngle <= 60) {
                        totalAngle += localAngle + (360 - itemAngle);
                    } else {
                        totalAngle += localAngle - itemAngle;
                    }
                } else {
                    totalAngle += localAngle - itemAngle;
                }

            }


            if (localAngle >= 240 && localAngle <= 300) {
                int rotateTimes = (int) ((totalAngle - 90) / 180);
                if (localAngle > itemAngle) {
                    int newIndex = child.getDataIndex();
                    if (rotateTimes % 2 != 0) {
                        newIndex = newIndex + (rotateTimes - rotateTimes / 2) * 6;
                    } else {
                        newIndex = newIndex + (rotateTimes / 2) * 6;
                    }
                    child.setIncreaseId(newIndex);
                }
            }
            if (localAngle >= 180 && localAngle <= 240) {
                if (localAngle < itemAngle) {
                    float delta = itemAngle - localAngle;
                    int rotateTimes = Math.round((totalAngle - 90 + delta) / 180);
                    int newIndex = child.getDataIndex();
                    if (rotateTimes % 2 != 0) {
                        newIndex = newIndex + (rotateTimes - rotateTimes / 2 - 1) * 6;
                    } else {
                        if (newIndex == 0) {
                            newIndex = newIndex + (rotateTimes / 2) * 6;
                        } else {
                            newIndex = newIndex + (rotateTimes / 2 - 1) * 6;
                        }

                    }
                    child.setIncreaseId(newIndex >= 0 ? newIndex : child.getDataIndex());
                }
            }
            int increaseId = child.getIncreaseId();
            if (entitys != null && increaseId >= 0 && increaseId < entityCount) {
                if (increaseId == 5 && totalAngle - 90 <= 60) {
                    child.setText(null);
                    child.setSmallText(null);
                } else {
                    child.setText(entitys.get(increaseId).getName());
                    child.setSmallText(entitys.get(increaseId).getSubName());
                }

            } else {
                child.setText(null);
            }

            child.setAngle(localAngle);
            if (localAngle >= 60 && localAngle <= 120) {
                child.setTextSize(16);
                if (onTodayItemSelectedListener != null) {
                    onTodayItemSelectedListener.onTodayShow(increaseId == todayIndex, Math.abs(localAngle - 90));
                }
            } else {
                child.setTextSize(textSize);
            }

            ViewHelper.setRotation(child, localAngle - 90);
            float distance = Math.abs(localAngle - firstChildPosition.getValue());
            float halfAngleDelay = angleDelay / 2;
            boolean isFirstItem = distance < halfAngleDelay
                    || distance > (360 - halfAngleDelay);
            if (isFirstItem && selected != child.getPosition()) {
                selected = child.getPosition();
                if (onItemSelectedListener != null && isRotating) {
                    onItemSelectedListener.onItemSelected(child);
                }
            }

            child.layout(left, top, left + childWidth, top + childHeight);
            localAngle += angleDelay;
        }
        if (totalAngle - 90 <= 5) {
            isFirstItem = true;
        } else {
            isFirstItem = false;
        }
        float endAngle = (entityCount - 1) * 60;
        if (totalAngle - 90 >= endAngle) {
            isEndItem = true;
        } else {
            isEndItem = false;
        }
    }

    public void setChildAngles() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                asyncSetChildAngles();
            }
        });
    }

    /**
     * @return The angle of the unit circle with the image views center
     */
    private double getPositionAngle(double xTouch, double yTouch) {
        double x = xTouch - (circleWidth / 2d);
        double y = circleHeight - yTouch - (circleHeight / 2d);

        switch (getPositionQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 2:
            case 3:
                return 180 - (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                // ignore, does not happen
                return 0;
        }
    }

    /**
     * @return The quadrant of the position
     */
    private static int getPositionQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }


    // Touch helpers
    private double touchStartAngle;
    private boolean didMove = false;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (isEnabled()) {
            gestureDetector.onTouchEvent(event);
            if (isRotating) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (circleUpView != null && inRangeOfView(circleUpView, event)) {
                            return false;
                        }
                        // reset the touched quadrants
                        for (int i = 0; i < quadrantTouched.length; i++) {
                            quadrantTouched[i] = false;
                        }

                        stopAnimation();
                        touchStartAngle = getPositionAngle(event.getX(),
                                event.getY());
                        didMove = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (circleUpView != null && inRangeOfView(circleUpView, event)) {
                            return false;
                        }
                        double currentAngle = getPositionAngle(event.getX(),
                                event.getY());
                        float deltAngle = (float) (touchStartAngle - currentAngle);
                        if (deltAngle < 0) {
                            //anti clockwise
                            if (isFirstItem) {
                                return false;
                            }

                        } else if (deltAngle > 0) {
                            //clockwise
                            if (isEndItem) {
                                return false;
                            }
                        }
                        rotateButtons(deltAngle);
                        touchStartAngle = currentAngle;
                        didMove = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (didMove) {
                            rotateViewToCenter((ItemView) getChildAt(selected));
                        }
                        break;
                }
            }

            // set the touched quadrant to true
            quadrantTouched[getPositionQuadrant(event.getX()
                    - (circleWidth / 2), circleHeight - event.getY()
                    - (circleHeight / 2))] = true;
            return true;
        }
        return false;
    }

    private class MyGestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            if (!isRotating) {
                return false;
            }

            // get the quadrant of the start and the end of the fling
            int q1 = getPositionQuadrant(e1.getX() - (circleWidth / 2),
                    circleHeight - e1.getY() - (circleHeight / 2));
            int q2 = getPositionQuadrant(e2.getX() - (circleWidth / 2),
                    circleHeight - e2.getY() - (circleHeight / 2));

            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math
                    .abs(velocityY))
                    || (q1 == 3 && q2 == 3)
                    || (q1 == 1 && q2 == 3)
                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math
                    .abs(velocityY))
                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
                    || (q1 == 2 && q2 == 4 && quadrantTouched[3])
                    || (q1 == 4 && q2 == 2 && quadrantTouched[3])) {
                // the inverted rotations
                animateTo(getCenteredAngle(angle - (velocityX + velocityY) / 25),
                        25000 / speed);
            } else {
                // the normal rotation
                animateTo(getCenteredAngle(angle + (velocityX + velocityY) / 25),
                        25000 / speed);
            }

            return true;
        }

        private float getCenteredAngle(float angle) {
            float angleDelay = 360 / getChildCount();
            float localAngle = angle % 360;

            if (localAngle < 0) {
                localAngle = 360 + localAngle;
            }

            for (float i = firstChildPosition.getValue(); i < firstChildPosition.getValue() + 360; i += angleDelay) {
                float locI = i % 360;
                float diff = localAngle - locI;
                if (Math.abs(diff) < angleDelay / 2) {
                    angle -= diff;
                    break;
                }
            }

            return angle;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int tappedViewsPosition = pointToPosition(e.getX(), e.getY());

            if (tappedViewsPosition >= 0) {
                tappedView = (ItemView) getChildAt(tappedViewsPosition);
                tappedView.setPressed(true);
            }

            if (tappedView != null) {
                if (selected == tappedViewsPosition) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(tappedView);
                    }
                } else {
                    tappedViewAngle = Math.round(tappedView.getAngle());
                    rotateViewToCenter(tappedView);
                    if (!isRotating) {
                        if (onItemSelectedListener != null) {
                            onItemSelectedListener.onItemSelected(tappedView);
                        }

                        if (onItemClickListener != null) {
                            onItemClickListener.onItemClick(tappedView);
                        }
                    }
                }
                return true;
            }
            return super.onSingleTapUp(e);
        }

        private int pointToPosition(float x, float y) {
            for (int i = 0; i < getChildCount(); i++) {
                View item = getChildAt(i);
                if (item.getLeft() < x && item.getRight() > x
                        & item.getTop() < y && item.getBottom() > y) {
                    return i;
                }
            }
            return -1;
        }
    }

    public enum FirstChildLocation {
        East(0), South(90), West(180), North(270);

        private float value;

        FirstChildLocation(float position) {
            this.value = position;
        }

        public float getValue() {
            return this.value;
        }

    }

    public interface OnTodayItemSelectedListener {
        void onTodayShow(boolean isToday, float deltaAngle);
    }

    public void setOnTodayItemSelectedListener(OnTodayItemSelectedListener todayListener) {
        this.onTodayItemSelectedListener = todayListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ItemView view);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(ItemView view);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnRotationFinishedListener {
        void onRotationFinished(ItemView view);
    }

    public void setOnRotationFinishedListener(OnRotationFinishedListener onRotationFinishedListener) {
        this.onRotationFinishedListener = onRotationFinishedListener;
    }


    private boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if (ev.getX() < x || ev.getX() > (x + view.getWidth()) || ev.getY() < y || ev.getY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }


    /**
     * Custom text and icon for the {@link #RotateListView} class.
     */
    public static class ItemView extends LinearLayout {
        private TextView textView;
        private TextView smallTextView;
        // Angle is used for the positioning on the circle
        private float angle = 0;
        // Position represents the index of this view in the view groups children array
        private int position = 0;
        private int dataIndex;
        private int increaseId;

        public int getIncreaseId() {
            return increaseId;
        }

        public void setIncreaseId(int increaseId) {
            this.increaseId = increaseId;
        }

        public int getDataIndex() {
            return dataIndex;
        }

        public void setDataIndex(int dataIndex) {
            this.dataIndex = dataIndex;
        }

        /**
         * Return the angle of the view.
         *
         * @return Returns the angle of the view in degrees.
         */
        public float getAngle() {
            return angle;
        }

        /**
         * Set the angle of the view.
         *
         * @param angle The angle to be set for the view.
         */
        public void setAngle(float angle) {
            this.angle = angle;
        }

        /**
         * Return the position of the view.
         *
         * @return Returns the position of the view.
         */
        public int getPosition() {
            return position;
        }

        /**
         * Set the position of the view.
         *
         * @param position The position to be set for the view.
         */
        public void setPosition(int position) {
            this.position = position;
        }

        /**
         * Set the text of the view.
         *
         * @param text The text to be set for the view.
         */
        public void setText(String text) {
            textView.setText(text);

        }

        public void setSmallText(String text) {
            smallTextView.setText(text);
        }


        public void setTextVisible(boolean iconVisible) {
            textView.setVisibility(iconVisible ? VISIBLE : GONE);
        }

        public void setTextColor(@ColorInt int color) {
            textView.setTextColor(color);
            smallTextView.setTextColor(color);
        }

        public void setTextSize(float textSize) {
            textView.setTextSize(textSize);
            smallTextView.setTextSize(textSize);
        }


        public ItemView(Context context) {
            super(context);
            int wrapContent = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(new ViewGroup.LayoutParams(wrapContent, wrapContent));
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            textView = new TextView(context);
            smallTextView = new TextView(context);
            smallTextView.setPadding(0, 10, 0, 0);
            textView.setGravity(Gravity.CENTER);
            smallTextView.setGravity(Gravity.CENTER);
            addView(textView);
            addView(smallTextView);
        }

    }

}
