package edu.benlerner.perfectshuffle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class FadingHorizontalScrollView extends HorizontalScrollView {

  private int fadeColor = 0xffffffff;
  private int initialScrollX = -1;
  public FadingHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.fadeColor = attrs.getAttributeUnsignedIntValue(null, "cacheColorHint", this.fadeColor);
  }
  public FadingHorizontalScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.fadeColor = attrs.getAttributeUnsignedIntValue(null, "cacheColorHint", this.fadeColor);
  }
  public FadingHorizontalScrollView(Context context) {
    super(context);
  }

  public void setInitialScrollX(int scrollX) {
    this.initialScrollX = scrollX;
  }
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (this.getWidth() == 0) return;
    if (this.initialScrollX >= 0) {
      scrollTo(this.initialScrollX, this.getScrollY());
      this.initialScrollX = -1;
    }
  }
  @Override
  public int getSolidColor() {
    return this.fadeColor;
  }

}
