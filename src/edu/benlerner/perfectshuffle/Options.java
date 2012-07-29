package edu.benlerner.perfectshuffle;

import java.util.ArrayList;
import java.util.List;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

public class Options extends Fragment {
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.options, null);
    List<TableLayout> tables = AllChildren((ViewGroup)view, TableLayout.class);
    for (int i = 0; i < tables.size(); i++) {
      final TableLayout table = tables.get(i);
      final TableRow header = (TableRow)table.findViewById(R.id.header);
      final TableRow content = (TableRow)table.findViewById(R.id.content);
      final ImageView expander = (ImageView)header.findViewById(R.id.expander);
      header.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          if ((Integer)expander.getTag() == R.drawable.navigation_collapse) {
            expander.setImageResource(R.drawable.navigation_expand);
            expander.setTag(R.drawable.navigation_expand);
            MarginLayoutParams params = (MarginLayoutParams)table.getLayoutParams();
            params.bottomMargin = content.getMeasuredHeight();
            table.getLayoutTransition().hideChild(table, content);
            content.setVisibility(View.GONE);
          } else {
            expander.setImageResource(R.drawable.navigation_collapse);
            expander.setTag(R.drawable.navigation_collapse);
            content.setVisibility(View.VISIBLE);
            MarginLayoutParams params = (MarginLayoutParams)table.getLayoutParams();
            params.bottomMargin = -content.getMeasuredHeight();
            table.getLayoutTransition().showChild(table, content);          }
        }
      });
    }
    return view;
  }
  public static <T> List<T> AllChildren(ViewGroup v, Class<T> klass) {
    ArrayList<T> ret = new ArrayList<T>();
    int count = v.getChildCount();
    for (int i = 0;i < count; i++) {
      View child = v.getChildAt(i);
      if (klass.isInstance(child)) {
        ret.add(klass.cast(child));
      } else if (ViewGroup.class.isInstance(child)) {
        ret.addAll(AllChildren((ViewGroup)child, klass));
      }
    }
    return ret;
  }

  @Override
  public void onResume() {
    super.onResume();
    List<TableLayout> tables = AllChildren((ViewGroup)this.getView(), TableLayout.class);
    for (int i = 0; i < tables.size(); i++) {
      final TableLayout table = tables.get(i);
      TableRow header = (TableRow)table.findViewById(R.id.header);
      TableRow content = (TableRow)table.findViewById(R.id.content);
      ImageView expander = (ImageView)header.findViewById(R.id.expander);
      expander.setTag(R.drawable.navigation_collapse);
      content.setPivotY(0);
      content.addOnLayoutChangeListener(new OnLayoutChangeListener() {
        // Must be in a LayoutChangeListener because only then do we know the
        // measuredHeight
        public void onLayoutChange(View content, int left, int top, int right, int bottom, int oldLeft, int oldTop,
            int oldRight, int oldBottom) {
          final int duration = 500;
          LayoutTransition transition = new LayoutTransition();
          AnimatorSet animAppear = new AnimatorSet();
          animAppear.setDuration(duration).playTogether(
              ObjectAnimator.ofFloat(content, "alpha", 0, 1),
              ObjectAnimator.ofFloat(content, "scaleY", 0, 1));
          transition.setAnimator(LayoutTransition.APPEARING, animAppear);
          transition.setAnimator(LayoutTransition.CHANGE_APPEARING,
              MarginAnimator.ofMargin(table, MarginAnimator.Margin.BOTTOM, -content.getMeasuredHeight(), 0)
                  .setDuration(duration));
          AnimatorSet animDisappear = new AnimatorSet();
          animDisappear.setDuration(duration).playTogether(
              ObjectAnimator.ofFloat(content, "alpha", 1, 0),
              ObjectAnimator.ofFloat(content, "scaleY", 1, 0));
          transition.setAnimator(LayoutTransition.DISAPPEARING, animDisappear);
          transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING,
              MarginAnimator.ofMargin(table, MarginAnimator.Margin.BOTTOM, content.getMeasuredHeight(), 0)
                  .setDuration(duration));
          transition.setStartDelay(LayoutTransition.APPEARING, 0);
          transition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
          table.setLayoutTransition(transition);
        }
      });
    }
  }
  
  static class MarginAnimator extends ValueAnimator {
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
}
