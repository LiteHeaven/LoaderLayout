package com.nykj.loaderlayout.loader;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.nykj.loaderlayout.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * 加载视图，用来替换SwipeRefreshLayout
 *
 * 1.阻尼拉动
 * 2.同时支持上下拉，和{@link androidx.core.view.NestedScrollingChild}配合使用
 * 3.可以自定义“错误视图”和“载中视图”，使用xml或直接代码调用
 * Create by liangy on 2020/7/14
 */
public class LoaderLayout extends FrameLayout implements NestedScrollingParent3 {

    private static final String TAG = LoaderLayout.class.getSimpleName();

    private NestedScrollingParentHelper parentHelper = new NestedScrollingParentHelper(this);

    private Scroller scroller = new Scroller(getContext(), new DecelerateInterpolator());

    private IProgressViewHolder progressViewHolder;

    private IErrorViewHolder errorViewHolder;

    /**
     * 结果监听
     */
    private LoaderListener loaderListener;

    /**
     * 定义为浮点型防止滑动做阻尼计算时（要除以阻尼系数）丢失小数
     */
    private float scrollY = 0;

    private LoaderControllerImpl controller = new LoaderControllerImpl(this);

    private int progressViewHeight = 0;

    private boolean debug = isDebuggable();

    // ----------------------------------------
    // constructor
    // ----------------------------------------

    public LoaderLayout(@NonNull Context context) {
        this(context, null);
    }

    public LoaderLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoaderLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LoaderLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs){
        this.debug = isDebuggable();

        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.LoaderLayout, 0, 0);
        String factoryClassName = typedArray.getString(R.styleable.LoaderLayout_factoryClassName);
        log("factoryClassName = " + factoryClassName);
        typedArray.recycle();

        ILoaderWidgetFactory factory = createViewByClassName(factoryClassName);
        if (factory != null){
            controller.setWidgetFactory(factory);
        }
    }

    void initializeByFactory(ILoaderWidgetFactory factory){
        log("initializeByFactory = " + factory);
        initErrorLayout(factory);
        initProgressView(factory);
        showErrorLayout(false);
    }

    // ----------------------------------------
    // NestedScrollingParent3
    // ----------------------------------------

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        log(String.format(Locale.getDefault(), "onNestedScroll type = %d dyConsumed = %d, dyUnconsumed = %d", type, dyConsumed, dyUnconsumed));
        if (type == ViewCompat.TYPE_TOUCH){
            updateLayoutByDeltaY(dyUnconsumed);
        }
    }

    // ----------------------------------------
    // NestedScrollingParent2
    // ----------------------------------------

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                                       int type) {
        boolean result = false;
        if (type == ViewCompat.TYPE_TOUCH){
            result = (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        }
        log("onStartNestedScroll type = " + type + " axes = " + axes + " result = " + result);
        return result;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                                       int type) {
        log("onNestedScrollAccepted type = " + type);
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        log("onStopNestedScroll type = " + type);
        parentHelper.onStopNestedScroll(target, type);
        if (type == ViewCompat.TYPE_TOUCH){
            onStopNestedScroll();
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, null);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                                  int type) {
        int dyConsumed = 0;
        if (type == ViewCompat.TYPE_TOUCH){
            if (target instanceof NestedScrollView){
                NestedScrollView nestedScrollView = (NestedScrollView) target;

                // 如果NestedScrollView没在“被拖拽”状态则不能进行消费
                // 原因详情请查看isNestedScrollViewBeingDragged方法的注释
                if (!isNestedScrollViewBeingDragged(nestedScrollView)){
                    return;
                }
            }

            if (scrollY != 0){
                // 全部消费
                dyConsumed = dy;
                updateLayoutByDeltaY(dyConsumed);

                consumed[1] = dyConsumed;
            }
        }
        log("onNestedPreScroll type = " + type + " dy = " + dyConsumed);
    }

    // ----------------------------------------
    // NestedScrollingParent
    // ----------------------------------------

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        onNestedScrollAccepted(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(View target) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        // 先把scroll处理好吧
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }

    // ----------------------------------------
    // internal
    // ----------------------------------------

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        // 后续添加的视图被覆盖在z-order的底部
        View progressItemView = progressViewHolder != null ? progressViewHolder.getItemView() : null;
        View errorItemView = errorViewHolder != null ? errorViewHolder.getItemView() : null;

        if (child != progressItemView && child != errorItemView){
            index = 0;
        }
        super.addView(child, index, params);
    }

    @Override
    public void computeScroll() {
        if (!scroller.computeScrollOffset()){
            return;
        }
        int y = scroller.getCurrY();
        scrollTo(0, y);
        invalidate();
    }

    private void log(String log){
        if (debug){
            Log.i(TAG, log);
        }
    }

    private boolean isDebuggable(){
        boolean result = false;
        try {
            ApplicationInfo info = getContext().getApplicationInfo();
            if (info != null){
                result = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void updateLayoutByDeltaY(int deltaY){
        float lastScrollY = this.scrollY;
        float dstScrollY = scrollY + deltaY / 3f;


        // onNestedPreScroll只处理scrollY!=0即上下拉的状态，onNestedScroll只处理scrollY=0即无拉伸状态
        // 无拉伸时，第一次进入上拉或下拉由child分发来的onNestedScroll决定
        // 进入拉伸状态后，由onNestedPreScroll主动拦截child的滚动事
        //
        // 拉伸状态的退出有以下情况：
        // √.ACTION_UP触发嵌套滚动结束，此时由偏移量决定进入初始状态或是加载中状态
        // √.下拉出现头部菊花时不松手向上划回使菊花逐渐消失，当菊花消失的瞬间退出拉伸状态
        //
        // 此处如果符号发生改变会使状态错乱，要先变为无拉伸状态
        if (lastScrollY * dstScrollY < 0){
            dstScrollY = 0;
        }
        log("updateLayoutByDeltaY " + dstScrollY + " scrollY = " + scrollY + " deltaY = " + deltaY);
        updateLayout(dstScrollY, false);
    }

    private void updateLayout(float scrollY, boolean directly){
        log("updateLayout " + scrollY);

        // fling会多产生一个嵌套滚动实例，会重复调用onLoadingInner导致新的scrollY和老的一样
        // 在这里如果重复了，就不需要再滚、不需要停止前一个进行中的滚动实例
        if (this.scrollY != scrollY){
            scroller.forceFinished(true);

            int from = (int) this.scrollY;
            int to = (int) scrollY;

            if (directly){
                this.scrollTo(0, to);
            }else{
                int dy = to - from;
                log("startScroll " + from + " dy " + dy);
                scroller.startScroll(0, from ,0, dy, 200);
                invalidate();
            }
        }

        if (scrollY != 0){
            View progressItemView = progressViewHolder.getItemView();
            FrameLayout.LayoutParams lp = (LayoutParams) progressItemView.getLayoutParams();
            if (lp != null){
                lp.gravity = scrollY < 0 ? Gravity.TOP : Gravity.BOTTOM;
                progressItemView.setLayoutParams(lp);
            }
        }
        this.scrollY = scrollY;
    }

    private int getProgressViewHeight(){
        return progressViewHeight;
    }

    private void onStopNestedScroll(){
        int h = getProgressViewHeight();
        if (h <= 0){
            return;
        }

        float scrollY = this.scrollY;
        log("onStopNestedScroll scrollY = " + scrollY + " h = " + h);
        if (scrollY < -h){
            onLoading(true);
        }else if (scrollY > h){
            onLoading(false);
        }else{
            stopLoading();
        }
    }

    private void onLoading(final boolean top){
        log("onLoading " + top);
        if (loaderListener != null){
            if (top){
                loaderListener.onHeaderLoading();
            }else{
                loaderListener.onFooterLoading();
            }
        }

        int h = getProgressViewHeight();
        if (h > 0){
            if (top){
                updateLayout(-h, false);
            }else{
                updateLayout(h, false);
            }
        }
    }

    /**
     * 判断NestedScrollView是不是处在正在被拖拽的状态
     *
     * 为什么非要引入这个方法，先从{@link androidx.core.view.NestedScrollingParent#onNestedScroll(View, int, int, int, int)}说起。
     * onNestedScroll主要用来来处理{@link androidx.core.view.NestedScrollingChild}处理剩下的滚动，它只给了你被child消费的距离以及
     * 还没有被child消费的距离。
     * 很多精细动作需要借助{@link androidx.core.view.NestedScrollingParent#onNestedPreScroll(View, int, int, int[])}
     * 方法来控制。
     * 比如当加载中视图出现时，此时如果反向滑动，如果parent只处理onNestedScroll方法，那按照默认的行为，child显然是可以消费
     * 反向滑动事件的，结果使列表反向滑动，但此时我们想要的是“加载中”视图反向移动，需要parent去消费，这显然与默认行为不符。
     *
     * 此时onNestedPreScroll方法登场。
     * 但onNestedPreScroll在处理child是NestedScrollView的情况会出问题。
     * {@link NestedScrollView#onTouchEvent(MotionEvent)}处理ACTION_MOVE时，首先把mLastMotionY与当前坐标做差作为
     * onNestedPreScroll的dy参数，然后根据偏移判断是否大于touchSlop，如果大于则进入被拖拽状态，此时mIsBeingDragged
     * 变量变为true。
     *
     * 问题来了，当我们处理onNestedPreScroll方法时，如果消费掉所有的dy，将导致ACTION_MOVE在稍后判断偏移是否大于
     * touchSlop时结果都变为false，变为false则无法进入被拖拽状态（mIsBeingDragged=true），无法进入拖拽状态则
     * mLastMotionY永远都是ACTION_DOWN时的坐标，所以每次ACTION_MOVE事件产生时，onNestedPreScroll方法收到的dy
     * 值等于ACTION_MOVE与ACTION_DOWN间的距离，而不是这次滑动的距离。这就导致了错误。
     *
     * 解决：我们需要在onNestedPreScroll方法中判断，如果NestedScrollView没有进入拖拽状态，则不能消费滑动事件
     * 否则将使onNestedPreScroll方法收到的dy值出现错误。
     *
     * @param scrollView NestedScrollView实例
     * @return 是否处在“被拖拽”状态
     */
    public static boolean isNestedScrollViewBeingDragged(NestedScrollView scrollView){
        boolean isBeingDragged = true;
        try {
            Field field = NestedScrollView.class.getDeclaredField("mIsBeingDragged");
            field.setAccessible(true);
            Object value = field.get(scrollView);
            if (value instanceof Boolean){
                isBeingDragged = (boolean)value;
            }
            field.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isBeingDragged;
    }

    private void initErrorLayout(ILoaderWidgetFactory factory){
        if (errorViewHolder == null){
            errorViewHolder = factory.createErrorView(this);
            this.addView(errorViewHolder.getItemView());
            showErrorLayoutInner(true);
        }
    }

    private void initProgressView(ILoaderWidgetFactory factory){
        if (progressViewHolder == null){
            progressViewHolder = factory.createProgressView(this);

            /*
             * 高度测量
             */
            View view = progressViewHolder.getItemView();
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            int msW = MeasureSpec.makeMeasureSpec(dm.widthPixels, MeasureSpec.EXACTLY);
            int msH = MeasureSpec.makeMeasureSpec(dm.heightPixels, MeasureSpec.AT_MOST);
            view.measure(msW, msH);
            progressViewHeight = view.getMeasuredHeight();
            log("progressViewHeight = " + progressViewHeight);

            FrameLayout.LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp == null){
                lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            }
            lp.gravity = Gravity.TOP;
            lp.topMargin = -progressViewHeight;
            lp.bottomMargin = -progressViewHeight;
            this.addView(view, lp);
        }
    }

    /**
     * 是否展示错误布局收口。
     * @param show 展示
     */
    private void showErrorLayoutInner(boolean show){
        log("showErrorLayoutInner " + show);
        errorViewHolder.getItemView().setVisibility(show ? VISIBLE : INVISIBLE);
    }

    private <T> T createViewByClassName(String className){
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            Object view = constructor.newInstance();
            return (T) view;
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }catch (NoSuchMethodException e){
            e.printStackTrace();
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (controller.getFactory() == null){
            throw new IllegalStateException("widget factory must be initialized at first time!");
        }
    }

    // ----------------------------------------
    // interface
    // ----------------------------------------

    public void stopLoading(){
        updateLayout(0, false);
    }

    public void startHeaderLoading(){
        log("startHeaderLoading");
        onLoading(true);
    }

    public void startFooterLoading(){
        log("startFooterLoading");
        onLoading(false);
    }

    public void setLoaderListener(LoaderListener loaderListener) {
        this.loaderListener = loaderListener;
    }

    public void showErrorLayout(boolean show) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View each = getChildAt(i);
            if (each == errorViewHolder.getItemView()) {
                showErrorLayoutInner(show);
            } else if (each == progressViewHolder.getItemView()) {
                // no-op
            } else {
                each.setVisibility(View.VISIBLE);
            }
        }
    }

    public LoaderController getController(){
        return controller;
    }
}
