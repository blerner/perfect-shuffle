package edu.benlerner.perfectshuffle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class FadingHorizontalScrollView extends HorizontalScrollView {

  private int fadeColor = 0xffffffff;
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

  @Override
  public int getSolidColor() {
    return this.fadeColor;
  }

}
