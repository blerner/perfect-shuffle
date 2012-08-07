package edu.benlerner.perfectshuffle;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Pair;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetroBarFragment extends Fragment {
  //id of innermost currently focused TextView in shelves
  private int                mTabState = -1;
  private List<LinearLayout> shelves   = new ArrayList<LinearLayout>(3);
  private List<TextView>     texts     = new ArrayList<TextView>(10);
  private List<ViewPager>    pagers    = new ArrayList<ViewPager>(5);
  private SparseIntArray parentShelfMap = new SparseIntArray(10);
  float normalSize;
  float largerSize;
  float spPerPx;
  int viewToGoTo = R.id.home;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View mainView = inflater.inflate(R.layout.metrobar, container, false);

    TextView temp = new TextView(this.getActivity());
    temp.setTextAppearance(this.getActivity(), android.R.attr.textAppearanceLarge);
    temp.setText("Measurement");
    temp.measure(0, 0);
    this.spPerPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1.0f, this.getResources().getDisplayMetrics());
    this.normalSize = temp.getTextSize(); // / this.spPerPx;
    this.largerSize = this.normalSize * 1.6f;

    if (savedInstanceState != null) {
      viewToGoTo = savedInstanceState.getInt("tab");
    }
    return mainView;
  }
  public void initialize(FrameLayout content) {
    LinearLayout topShelf = (LinearLayout)this.getView().findViewById(R.id.topShelf);
    this.parentShelfMap.put(R.id.topShelf, -1);
    connectTopShelfViews(this.getView(), topShelf, content);
    this.getView().findViewById(viewToGoTo).performClick();
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mTabState != -1)
      outState.putInt("tab", mTabState);
  }
  private void connectTopShelfViews(View view, LinearLayout shelf, FrameLayout content) {
    if (shelf == null) return;
    this.shelves.add(shelf);
    for (int i = 0; i < shelf.getChildCount(); i++) {
      TextView text = (TextView)shelf.getChildAt(i);
      ViewPager pager = (ViewPager)content.getChildAt(i);
      this.texts.add(text);
      this.pagers.add(pager);
      this.parentShelfMap.put(text.getId(), shelf.getId());
      Object tagObj = text.getTag();
      int tag = 0;
      LinearLayout nextShelf = null;
      if (tagObj == null)
        tagObj = "";
      String[] tagParts = tagObj.toString().split("/");
      if (tagParts[0].equals("id")) {
        tag = getResources().getIdentifier(tagParts[1], "id", this.getActivity().getPackageName());
        nextShelf = (LinearLayout)view.findViewById(tag);
        pager.setOnPageChangeListener(new PageChangeListener(nextShelf));
        this.shelves.add(nextShelf);
        this.parentShelfMap.put(nextShelf.getId(), shelf.getId());
        pager.setAdapter(new TabShelfAdapter(this.getFragmentManager(), nextShelf));
        pager.setCurrentItem(0);
        for (int j = 0; j < nextShelf.getChildCount(); j++) {
          TextView nextText = (TextView)nextShelf.getChildAt(j);
          nextText.setOnClickListener(new NestedTabClickListener(this, nextText, shelf, pager));
          nextText.setTag(pager);
          this.texts.add(nextText);
        }
      } else {
        pager.setAdapter(new TabShelfAdapter(this.getFragmentManager(), text));
      }
      text.setTag(pager); // should be the appropriate ViewPager 
      text.setOnClickListener(new TopTabClickListener(this, text, nextShelf, pager));
    }
  }
  
  private static class TabShelfAdapter extends FragmentPagerAdapter {
    ArrayList<Pair<String, Fragment>> fragments;
    SparseIntArray textIndices = new SparseIntArray(5);
    public TabShelfAdapter(FragmentManager fm, TextView... texts) {
      super(fm);
      this.fragments = new ArrayList<Pair<String, Fragment>>(texts.length);
      for (int i = 0; i < texts.length; i++) {
        addFragment(texts[i]);
        this.textIndices.put(texts[i].getId(), i);
      }
    }
    public TabShelfAdapter(FragmentManager fm, LinearLayout shelf) {
      super(fm);
      this.fragments = new ArrayList<Pair<String, Fragment>>(shelf.getChildCount());
      for (int i = 0; i < shelf.getChildCount(); i++) {
        TextView text = (TextView)shelf.getChildAt(i);
        addFragment(text);
        this.textIndices.put(text.getId(), i);
      }
    }
    public int getIndexOf(TextView text) {
      return this.textIndices.get(text.getId());
    }
    private void addFragment(TextView text) {
      Object tagObj = text.getTag();
      if (tagObj == null)
        tagObj = "";
      String tag = tagObj.toString();
      String[] tagParts = tag.split("/");
      if (tagParts[0].equals("playlist")) {
        this.fragments.add(new Pair<String, Fragment>(tag, new Playlist()));
      } else if (tagParts[0].equals("albumgrid")) {
        this.fragments.add(new Pair<String, Fragment>(tag, new Albumgrid()));
      } else if (tagParts[0].equals("options")) {
        this.fragments.add(new Pair<String, Fragment>(tag, new Options()));
      } else if (tagParts[0].equals("play")) {
        this.fragments.add(new Pair<String, Fragment>(tag, new PlayControls()));
      } else {
        this.fragments.add(new Pair<String, Fragment>(tag, PlaceholderFragment.CreateInstance(text.getText())));
      }
    }

    @Override
    public int getCount() {
      return this.fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
      if (position < 0 || position >= this.fragments.size())
        return null;
      return this.fragments.get(position).second;
    }
  }
  
  private class TopTabClickListener implements OnClickListener {
    MetroBarFragment fragment;
    TextView text;
    LinearLayout shelfToShow;
    ViewPager pagerToShow;
    public TopTabClickListener(MetroBarFragment fragment, TextView text, LinearLayout shelfToShow, ViewPager pagerToShow) {
      this.fragment = fragment;
      this.text = text;
      this.shelfToShow = shelfToShow;
      this.pagerToShow = pagerToShow;
    }
    public void onClick(View v) {
      if (this.pagerToShow.getVisibility() == View.VISIBLE)
        return;
      AnimatorSet set = new AnimatorSet();
      final int duration = 500;
      List<Animator> anims = new ArrayList<Animator>(10);
      for (ViewPager p : this.fragment.pagers) {
        if (p != this.pagerToShow && p.getVisibility() != View.GONE)
          anims.add(ObjectAnimator.ofFloat(p, "alpha", p.getAlpha(), 0.0f));
        else if (p == this.pagerToShow) {
          p.setVisibility(View.VISIBLE);
          p.setAlpha(0.0f);
          anims.add(ObjectAnimator.ofFloat(p, "alpha", 0.0f, 1.0f));
        }
      }
      animateTextHighlight(anims, this.text);
      if (this.shelfToShow != null)
        animateTextHighlight(anims, (TextView)this.shelfToShow.getChildAt(0));
      set.playTogether(anims);
      set.setDuration(duration);
      set.start();
      for (ViewPager p : this.fragment.pagers) {
        if (p != this.pagerToShow)
          p.setVisibility(View.GONE);
      }
      if (this.shelfToShow != null)
        hideShelvesUnlessAncestorOf(this.shelfToShow);
      else
        hideShelvesUnlessAncestorOf((LinearLayout)this.fragment.getView().findViewById(R.id.topShelf));
      this.pagerToShow.setCurrentItem(0);
    }
  }
  
  private void animateTextHighlight(List<Animator> anims, TextView text) {
    for (TextView t : this.texts) {
      if (t.getParent() == text.getParent()) {
        if (t != text) {
          int color = t.getTextColors().getDefaultColor();
          final int white = getResources().getColor(android.R.color.white);
          if (color != white) {
            anims.add(ColorAnimator.ofColor(t, color, white));
          }
        } else {
          int color = text.getTextColors().getDefaultColor();
          final int blue = getResources().getColor(android.R.color.holo_blue_dark);
          if (color != blue) {
            anims.add(ColorAnimator.ofColor(text, color, blue));
          }
        }
      }
    }
  }

  private class NestedTabClickListener implements OnClickListener {
    MetroBarFragment fragment;
    TextView         text;
    LinearLayout     thisShelf;
    ViewPager        pagerToShow;

    public NestedTabClickListener(MetroBarFragment fragment, TextView text, LinearLayout thisShelf, ViewPager pagerToShow) {
      this.fragment = fragment;
      this.text = text;
      this.thisShelf = thisShelf;
      this.pagerToShow = pagerToShow;
    }

    public void onClick(View v) {
      AnimatorSet set = new AnimatorSet();
      final int duration = 500;
      List<Animator> anims = new ArrayList<Animator>(4);
      animateTextHighlight(anims, this.text);
//      if (this.targetShelf != null)
//        animateShelves(anims, this.targetShelf);
//      else
//        animateShelves(anims, this.thisShelf);
      set.playTogether(anims);
      set.setDuration(duration);
      set.start();
      gotoViewFor(this.text);
    }
  }

  private boolean isAncestorShelfOf(int ancestorId, int shelfId) {
    if (shelfId == ancestorId) return true;
    if (shelfId == -1) return false;
    return isAncestorShelfOf(ancestorId, this.parentShelfMap.get(shelfId));
  }

  private void animateShelves(List<Animator> anims, LinearLayout target) {
    if (this.mTabState == -1) return;
    for (LinearLayout shelf : this.shelves) {
      for (int i = 0; i < shelf.getChildCount(); i++) {
        TextView text = (TextView)shelf.getChildAt(i);
        float size = text.getTextSize() / this.spPerPx;
        if (shelf == target) {
          anims.add(ObjectAnimator.ofFloat(text, "textSize", size, normalSize));
        } else if (isAncestorShelfOf(shelf.getId(), target.getId())) {
          anims.add(ObjectAnimator.ofFloat(text, "textSize", size, largerSize));
        }
      }
      if (shelf == target) {
        anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 1.0f));
      } else if (isAncestorShelfOf(shelf.getId(), target.getId())) {
        anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 1.0f));
      } else {
        anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 0.0f));
      }
      shelf.setPivotX(shelf.getScrollX());
      shelf.setPivotY(shelf.getHeight());
    }
  }
  
  private void hideShelvesUnlessAncestorOf(LinearLayout shelf) {
    int shelfId = shelf.getId();
    for (LinearLayout s : this.shelves) {
      if (this.isAncestorShelfOf(s.getId(), shelfId))
        s.setVisibility(View.VISIBLE);
      else
        s.setVisibility(View.GONE);
    }
  }

  public void gotoDefaultShelfViewFor(LinearLayout shelf) {
    if (shelf == null) return;
    for (int i = 0; i < shelf.getChildCount(); i++) {
      final TextView text = (TextView)shelf.getChildAt(i);
      if (text.isSelected()) {
        gotoViewFor(text);
        return;
      }
    }
    gotoViewFor((TextView)shelf.getChildAt(0));
  }

  public Fragment gotoViewFor(TextView text) {
    // mTabState keeps track of which tab is currently displaying its contents.
    // Perform a check to make sure the list tab content isn't already
    // displaying.

    ViewPager pager = (ViewPager)text.getTag();
    if (pager == null)
      return null;

    Fragment ret = null;
    // Update the mTabState
    mTabState = text.getId();

    TabShelfAdapter shelfAdapter = (TabShelfAdapter)pager.getAdapter();
    int index = shelfAdapter.getIndexOf(text);
    ret = shelfAdapter.getItem(index);
    pager.setCurrentItem(index, true);
    return ret;
  }

  private class PageChangeListener implements OnPageChangeListener {
    LinearLayout shelf;
    public PageChangeListener(LinearLayout shelf) {
      this.shelf = shelf;
    }
    public void onPageScrollStateChanged(int state) {
    }
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    public void onPageSelected(int position) {
      TextView text = (TextView)this.shelf.getChildAt(position);
      AnimatorSet set = new AnimatorSet();
      final int duration = 500;
      List<Animator> anims = new ArrayList<Animator>(10);
      animateTextHighlight(anims, text);
      set.playTogether(anims);
      set.setDuration(duration);
      set.start();     
    }
  }
  
  public final static class PlaceholderFragment extends Fragment {
    public static PlaceholderFragment CreateInstance(CharSequence caption) {
      Bundle args = new Bundle();
      args.putCharSequence("caption", caption);
      PlaceholderFragment ret = new PlaceholderFragment();
      ret.setArguments(args);
      return ret;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      CharSequence caption = getArguments().getCharSequence("caption");
      View view = inflater.inflate(R.layout.default_empty_view, container, false);

      TextView text = (TextView)view.findViewById(R.id.placeholder);
      text.setText(caption);
      return view;
    }
  }
  
  static class ColorAnimator extends ValueAnimator {
    public static ValueAnimator ofColor(final TextView target, int... colors) {
      ValueAnimator ret = ValueAnimator.ofInt(colors);
      ret.setEvaluator(new ArgbEvaluator());
      ret.addUpdateListener(new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
          int value = (Integer)animation.getAnimatedValue();
          target.setTextColor(value);
          target.requestLayout();
        }
      });
      return ret;
    }
  }
}