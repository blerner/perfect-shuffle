package edu.benlerner.perfectshuffle;

import java.util.ArrayList;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ExpandoGroup extends TableLayout {

  TableRow headerRow = null;
  TableRow contentRow = null;
  ImageView expando = null;
  TextView label = null;
  LinearLayout content = null;
  Space spacer = null;
  private int drawCollapseId = -1;
  private int drawExpandedId = -1;
  private int captionAppearance = -1;
  private String caption = "";
  final int animationDuration = 500;
  private boolean expanded = true;
  public ExpandoGroup(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.ExpandoGroup);
    this.caption = a.getString(R.styleable.ExpandoGroup_caption);
    if (this.caption == null)
      this.caption = "";
    this.drawCollapseId = 
        a.getResourceId(R.styleable.ExpandoGroup_collapseDrawable, R.drawable.navigation_collapse);
    this.drawExpandedId = 
        a.getResourceId(R.styleable.ExpandoGroup_expandDrawable, R.drawable.navigation_expand);
    this.captionAppearance =
        a.getResourceId(R.styleable.ExpandoGroup_captionAppearance, android.R.attr.textAppearanceLarge);
    a.recycle();
    initTableView(context);
  }
  public ExpandoGroup(Context context) {
    super(context);
    initTableView(context);
  }
  private void initTableView(Context context) {
    this.expanded = true;
    this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    addHeaderRow(context);
    addContentRow(context);
    this.headerRow.setOnClickListener(new OnHeaderClick(this));
    LayoutTransition transition = new LayoutTransition();
    AnimatorSet animAppear = new AnimatorSet();
    animAppear.setDuration(animationDuration).playTogether(
        ObjectAnimator.ofFloat(contentRow, "alpha", 0, 1),
        ObjectAnimator.ofFloat(contentRow, "scaleY", 0, 1));
    transition.setAnimator(LayoutTransition.APPEARING, animAppear);
    AnimatorSet animDisappear = new AnimatorSet();
    animDisappear.setDuration(animationDuration).playTogether(
        ObjectAnimator.ofFloat(contentRow, "alpha", 1, 0),
        ObjectAnimator.ofFloat(contentRow, "scaleY", 1, 0));
    transition.setAnimator(LayoutTransition.DISAPPEARING, animDisappear);
    transition.setStartDelay(LayoutTransition.APPEARING, 0);
    transition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
    this.setLayoutTransition(transition);
  }
  private boolean measuringOnlyDirectKids = false;
  private boolean isMeasuringOnlyDirectKids() {
    return measuringOnlyDirectKids;
  }
  private void setMeasuringOnlyDirectKids(boolean measuringOnlyDirectKids) {
    this.measuringOnlyDirectKids = measuringOnlyDirectKids;
  }
  private boolean drawingDontMove = false;
  public boolean isDrawingDontMove() {
    return drawingDontMove;
  }
  public void setDrawingDontMove(boolean drawingDontMove) {
    this.drawingDontMove = drawingDontMove;
  }
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    boolean m = this.isMeasuringOnlyDirectKids();
    this.setMeasuringOnlyDirectKids(true);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    if (this.isInEditMode() && !this.isDrawingDontMove()) {
//      for (int i = 0; i < this.content.getChildCount(); i++) {
//        this.content.getChildAt(i).offsetLeftAndRight(this.content.getLeft());
//        this.content.getChildAt(i).offsetTopAndBottom(this.contentRow.getTop());
//      }
//    }
    this.setMeasuringOnlyDirectKids(m);
    LayoutTransition transition = this.getLayoutTransition();
    transition.setAnimator(LayoutTransition.CHANGE_APPEARING,
        MarginAnimator.ofMargin(this, MarginAnimator.Margin.BOTTOM, -contentRow.getMeasuredHeight(), 0)
            .setDuration(animationDuration));
    transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING,
        MarginAnimator.ofMargin(this, MarginAnimator.Margin.BOTTOM, contentRow.getMeasuredHeight(), 0)
            .setDuration(animationDuration));
  }
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    boolean m = this.isMeasuringOnlyDirectKids();
    this.setMeasuringOnlyDirectKids(true); 
    super.onLayout(changed, l, t, r, b);
    if (this.isInEditMode() && !this.isDrawingDontMove()) {
      for (int i = 0; i < this.content.getChildCount(); i++) {
        this.content.getChildAt(i).offsetLeftAndRight(this.content.getLeft());
        this.content.getChildAt(i).offsetTopAndBottom(this.contentRow.getTop());
        this.content.getChildAt(i).setTranslationX(-this.content.getLeft());
        this.content.getChildAt(i).setTranslationY(-this.contentRow.getTop());
      }
    }
    this.setMeasuringOnlyDirectKids(m);
  }
  
  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    boolean d = this.isDrawingDontMove();
    this.setDrawingDontMove(true);
    boolean ret = super.drawChild(canvas, child, drawingTime);
    this.setDrawingDontMove(d);
    return ret;
  }
//  @Override
//  protected void dispatchDraw(Canvas canvas) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.dispatchDraw(canvas);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  protected boolean getChildStaticTransformation(View child, Transformation t) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    boolean ret = super.getChildStaticTransformation(child, t);
//    this.measuringOnlyDirectKids = m;
//    return ret;
//  }
//  @Override
//  public void getFocusedRect(Rect r) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.getFocusedRect(r);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    boolean ret = super.getGlobalVisibleRect(r, globalOffset);
//    this.measuringOnlyDirectKids = m;
//    return ret;
//  }
//  @Override
//  public void getLocationInWindow(int[] location) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.getLocationInWindow(location);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  public void getLocationOnScreen(int[] location) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.getLocationOnScreen(location);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  public void getHitRect(Rect outRect) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.getHitRect(outRect);
//    this.measuringOnlyDirectKids = m;
//  }
  
//  @Override
//  protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.measureChildren(widthMeasureSpec, heightMeasureSpec);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
//      int parentHeightMeasureSpec, int heightUsed) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
//    this.measuringOnlyDirectKids = m;
//  }
//  @Override
//  public void draw(Canvas canvas) {
//    boolean m = this.measuringOnlyDirectKids;
//    this.measuringOnlyDirectKids = true;
//    super.draw(canvas);
//    this.measuringOnlyDirectKids = m;
//  }
  @Override
  public int getChildCount() {
    if (this.isMeasuringOnlyDirectKids())
      return super.getChildCount();
    int ret = super.getChildCount();
    if (this.content != null) ret += this.content.getChildCount();
    return ret;
  }
  @Override
  public View getChildAt(int index) {
    if (this.isMeasuringOnlyDirectKids())
      return super.getChildAt(index);
    if (this.content != null && index >= super.getChildCount())
      return this.content.getChildAt(index - super.getChildCount());
    return super.getChildAt(index);
  }
  @Override
  public boolean getChildVisibleRect(View child, Rect r, Point offset) {
    //if (this.measuringOnlyDirectKids || this.content == null)
      return super.getChildVisibleRect(child, r, offset);
    //return this.content.getChildVisibleRect(child, r, offset);
  }
  @Override
  public void getDrawingRect(Rect outRect) {
    boolean m = this.isMeasuringOnlyDirectKids();
    this.setMeasuringOnlyDirectKids(true);
    super.getDrawingRect(outRect);
    this.setMeasuringOnlyDirectKids(m);
  }
  
  class OnHeaderClick implements OnClickListener {
    ExpandoGroup table;
    public OnHeaderClick(ExpandoGroup table) {
      this.table = table;
    }
    public void onClick(View v) {
      if (this.table.expanded) {
        this.table.expando.setImageResource(R.drawable.navigation_expand);
        MarginLayoutParams params = (MarginLayoutParams)this.table.getLayoutParams();
        params.bottomMargin = this.table.contentRow.getMeasuredHeight();
        this.table.getLayoutTransition().hideChild(this.table, this.table.contentRow);
        this.table.contentRow.setVisibility(View.GONE);
      } else {
        this.table.expando.setImageResource(R.drawable.navigation_collapse);
        this.table.contentRow.setVisibility(View.VISIBLE);
        MarginLayoutParams params = (MarginLayoutParams)this.table.getLayoutParams();
        params.bottomMargin = -this.table.contentRow.getMeasuredHeight();
        this.table.getLayoutTransition().showChild(this.table, this.table.contentRow);
      }
      this.table.expanded = !this.table.expanded;
    }
  }
  private float dipToPx(float dip) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, this.getResources().getDisplayMetrics());
  }
  private void addHeaderRow(Context context) {
    this.headerRow = new TableRow(context);
    this.headerRow.setClickable(true);
    this.headerRow.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
    this.headerRow.setGravity(Gravity.CENTER_VERTICAL);
    
    this.expando = new ImageView(context);
    int eightDip = (int)dipToPx(8);
    TableRow.LayoutParams p = new TableRow.LayoutParams(0);
    p.setMargins(eightDip, eightDip, eightDip, eightDip);
    this.expando.setImageResource(this.getCollapseDrawable());
    this.headerRow.addView(this.expando, p);
    
    this.label = new TextView(context, null, this.getCaptionAppearance());
    this.label.setText(this.getCaption());
    this.headerRow.addView(this.label, new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    
    super.addView(this.headerRow, -1, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    
  }
  private void addContentRow(Context context) {
    this.contentRow = new TableRow(context);
    this.contentRow.setPivotY(0);
    
    this.spacer = new Space(context);
    TableRow.LayoutParams p = new TableRow.LayoutParams(0);
    this.contentRow.addView(this.spacer, p);
    
    this.content = new LinearLayout(context);
    this.content.setOrientation(LinearLayout.VERTICAL);
    this.contentRow.addView(this.content, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
    
    super.addView(this.contentRow, -1, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
  }
  @Override
  public void addView(View child) {
    this.content.addView(child);
  }
  @Override
  public void addView(View child, int index) {
    this.content.addView(child, index);
  }
  @Override
  public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
    this.content.addView(child, index, params);
  }
  @Override
  public void addView(View child, int width, int height) {
    this.content.addView(child, width, height);
  }
  @Override
  public void addView(View child, android.view.ViewGroup.LayoutParams params) {
    this.content.addView(child, params);
  }
  @Override
  public void removeAllViews() {
    this.content.removeAllViews();
  }
  @Override
  public void removeAllViewsInLayout() {
    this.content.removeAllViewsInLayout();
  }
  @Override
  public void removeView(View view) {
    this.content.removeView(view);
  }
  @Override
  public void removeViewAt(int index) {
    this.content.removeViewAt(index);
  }
  @Override
  public void removeViewInLayout(View view) {
    this.content.removeViewInLayout(view);
  }
  @Override
  public void removeViews(int start, int count) {
    this.content.removeViews(start, count);
  }
  @Override
  public void removeViewsInLayout(int start, int count) {
    this.content.removeViewsInLayout(start, count);
  }
  @Override
  public void addFocusables(ArrayList<View> views, int direction) {
    this.content.addFocusables(views, direction);
  }
  @Override
  public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
    this.content.addFocusables(views, direction, focusableMode);
  }
  @Override
  public void addTouchables(ArrayList<View> views) {
    this.content.addTouchables(views);
  }
  public int getCollapseDrawable() {
    return drawCollapseId;
  }
  public void setCollapseDrawable(int drawCollapseId) {
    this.drawCollapseId = drawCollapseId;
    this.invalidate();
    this.requestLayout();
  }
  public int getExpandedDrawable() {
    return drawExpandedId;
  }
  public void setExpandedDrawable(int drawExpandedId) {
    this.drawExpandedId = drawExpandedId;
    this.invalidate();
    this.requestLayout();
  }
  public String getCaption() {
    return caption;
  }
  public void setCaption(String caption) {
    this.caption = caption;
    this.invalidate();
    this.requestLayout();
  }
  public int getCaptionAppearance() {
    return captionAppearance;
  }
  public void setCaptionAppearance(int captionAppearance) {
    this.captionAppearance = captionAppearance;
    this.invalidate();
    this.requestLayout();
  }
  public static class MarginAnimator extends ValueAnimator {
    public enum Margin { TOP, LEFT, BOTTOM, RIGHT };
    public static ValueAnimator ofMargin(final View target, final Margin whichMargin, int... values) {
      ValueAnimator ret = ValueAnimator.ofInt(values);
      ret.addUpdateListener(new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
          int value = (Integer)animation.getAnimatedValue();
          MarginLayoutParams params = (MarginLayoutParams)target.getLayoutParams();
          switch (whichMargin) {
          case TOP:
            params.topMargin = value;
            break;
          case BOTTOM:
            params.bottomMargin = value;
            break;
          case LEFT:
            params.leftMargin = value;
            break;
          case RIGHT:
            params.rightMargin = value;
            break;
          }
          target.requestLayout();
        }});
      return ret;
    }
  }
  public static class SizeAnimator extends ValueAnimator {
    public enum Size { WIDTH, HEIGHT };
    public static ValueAnimator ofSize(final View target, final Size whichSize, int... values) {
      ValueAnimator ret = ValueAnimator.ofInt(values);
      ret.addUpdateListener(new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
          int value = (Integer)animation.getAnimatedValue();
          MarginLayoutParams params = (MarginLayoutParams)target.getLayoutParams();
          switch (whichSize) {
          case WIDTH:
            params.width = value;
            break;
          case HEIGHT:
            params.height = value;
            break;
          }
          target.requestLayout();
        }});
      return ret;
    }
  }
}
