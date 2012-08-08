package edu.benlerner.perfectshuffle;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
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
  private int                mTabState = R.id.home;
  private List<LinearLayout> shelves   = new ArrayList<LinearLayout>(3);
  private List<TextView>     texts     = new ArrayList<TextView>(10);
  private List<ViewPager>    pagers    = new ArrayList<ViewPager>(5);
  private SparseIntArray parentShelfMap = new SparseIntArray(10);
  private SparseIntArray parentTextMap = new SparseIntArray(2); // maps from "Current" or "Playing" in the second shelf to "Now Playing" in the top shelf
  final int DURATION = 500;
  float normalSize;
  float largerSize;
  int topShelfScrollX;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View mainView = inflater.inflate(R.layout.metrobar, container, false);

    TextView temp = new TextView(this.getActivity(),null,android.R.attr.textAppearanceLarge);
    temp.setText("Measurement");
    temp.measure(0, 0);
    this.normalSize = temp.getTextSize();
    this.largerSize = this.normalSize * 2.5f;

    if (savedInstanceState != null) {
      this.setActiveTab(savedInstanceState.getInt("tab"));
      this.topShelfScrollX = savedInstanceState.getInt("topShelfScrollX");
    }
    return mainView;
  }
  public void initialize(FrameLayout content) {
    View view = this.getView();
    if (view == null) return;
    LinearLayout topShelf = (LinearLayout)view.findViewById(R.id.topShelf);
    this.parentShelfMap.put(R.id.topShelf, -1);
    connectTopShelfViews(view, topShelf, content);
  }
  public void onResume() {
    super.onResume();
    initializeTabView(this.getView(), this.getActiveTab());
  }
  private void initializeTabView(final View view, int tabToShow) {
    TextView text = (TextView)view.findViewById(tabToShow);
    TextView topText = null;
    if (text == null) return;
    int topTextId = this.parentTextMap.get(tabToShow);
    if (topTextId != 0) {
      topText = (TextView)view.findViewById(topTextId);
    } else {
      topText = text;
      text = null;
    }
    
    
    if (topText != null)
      animateTextHighlight(null, topText);
    if (text != null) {
      ViewPager pagerToShow = (ViewPager)text.getTag();
      for (ViewPager p : this.pagers) {
        if (p == pagerToShow)
          p.setVisibility(View.VISIBLE);
        else
          p.setVisibility(View.GONE);
      }
      animateTextHighlight(null, text);
      LinearLayout shelf = (LinearLayout)text.getParent();
      hideShelvesUnlessAncestorOf(shelf);
      animateShelves(null, shelf);
      FadingHorizontalScrollView topShelfScroll = (FadingHorizontalScrollView)this.getView().findViewById(R.id.topShelfScroll);
      topShelfScroll.setInitialScrollX(this.topShelfScrollX);
      gotoViewFor(text);
    }
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (getActiveTab() != -1) {
      outState.putInt("tab", getActiveTab());
      outState.putInt("topShelfScrollX", ((FadingHorizontalScrollView)this.getView().findViewById(R.id.topShelfScroll)).getScrollX());
    }
  }
  private void connectTopShelfViews(View view, LinearLayout shelf, FrameLayout content) {
    if (shelf == null) return;
    this.shelves.clear();
    this.pagers.clear();
    this.texts.clear();
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
        pager.setOnPageChangeListener(new PageChangeListener(pager, nextShelf));
        this.shelves.add(nextShelf);
        this.parentShelfMap.put(nextShelf.getId(), shelf.getId());
        pager.setAdapter(new TabShelfAdapter(this.getFragmentManager(), nextShelf));
        pager.setCurrentItem(0);
        for (int j = 0; j < nextShelf.getChildCount(); j++) {
          TextView nextText = (TextView)nextShelf.getChildAt(j);
          this.parentTextMap.put(nextText.getId(), text.getId());
          nextText.setOnClickListener(new NestedTabClickListener(nextText, nextShelf));
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
  
  private static class TabShelfAdapter extends FragmentStatePagerAdapter {
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
      LinearLayout topShelf = (LinearLayout)this.fragment.getView().findViewById(R.id.topShelf);
      AnimatorSet set = new AnimatorSet();
      List<Animator> anims = new ArrayList<Animator>(10);
      for (ViewPager p : this.fragment.pagers) {
        if (p != this.pagerToShow && p.getVisibility() != View.GONE)
          anims.add(ObjectAnimator.ofFloat(p, "alpha", p.getAlpha(), 0.0f));
        else if (p == this.pagerToShow) {
          p.setVisibility(View.VISIBLE);
          anims.add(ObjectAnimator.ofFloat(p, "alpha", p.getAlpha(), 1.0f));
        }
      }
      animateScrollToView(anims, this.text);
      animateTextHighlight(anims, this.text);
      if (this.shelfToShow != null) {
        TextView childToShow = (TextView)this.shelfToShow.getChildAt(0);
        animateTextHighlight(anims, childToShow);
        animateShelves(anims, this.shelfToShow);
        setActiveTab(childToShow.getId());
      } else {
        animateShelves(anims, topShelf);
        setActiveTab(this.text.getId());
      }
      set.playTogether(anims);
      set.setDuration(DURATION);
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
  
  private void animateScrollToView(List<Animator> anims, TextView text) {
    FadingHorizontalScrollView shelfScroll = (FadingHorizontalScrollView)text.getParent().getParent();
    int left = text.getLeft();
    if (this.parentShelfMap.get(((View)text.getParent()).getId()) == -1 &&
        text.getTextSize() == normalSize) {
      // in the top shelf
      left = (int)(text.getLeft() * 1.6f);
    }
    left -= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 5, this.getResources().getDisplayMetrics());
    if (anims != null)
      anims.add(ObjectAnimator.ofInt(shelfScroll, "scrollX", shelfScroll.getScrollX(), left));
    else
      shelfScroll.scrollTo(left, shelfScroll.getScrollY());
  }
  
  private void animateTextHighlight(List<Animator> anims, TextView text) {
    for (TextView t : this.texts) {
      if (t.getParent() == text.getParent()) {
        if (t != text) {
          int color = t.getTextColors().getDefaultColor();
          final int white = getResources().getColor(android.R.color.white);
          if (color != white) {
            if (anims != null)
              anims.add(ColorAnimator.ofColor(t, color, white));
            else
              t.setTextColor(white);
          }
        } else {
          int color = text.getTextColors().getDefaultColor();
          final int blue = getResources().getColor(android.R.color.holo_blue_dark);
          if (color != blue) {
            if (anims != null)
              anims.add(ColorAnimator.ofColor(text, color, blue));
            else
              text.setTextColor(blue);
          }
        }
      }
    }
  }

  private class NestedTabClickListener implements OnClickListener {
    TextView         text;
    LinearLayout     thisShelf;

    public NestedTabClickListener(TextView text, LinearLayout thisShelf) {
      this.text = text;
      this.thisShelf = thisShelf;
    }

    public void onClick(View v) {
      AnimatorSet set = new AnimatorSet();
      List<Animator> anims = new ArrayList<Animator>(4);
      animateTextHighlight(anims, this.text);
      hideShelvesUnlessAncestorOf(this.thisShelf);
      animateShelves(anims, this.thisShelf);
      set.playTogether(anims);
      set.setDuration(DURATION);
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
    for (int i = 0; i < target.getChildCount(); i++) {
      TextView text = (TextView)target.getChildAt(i);
      float size = text.getTextSize();
      if (anims != null)
        anims.add(TextSizeAnimator.animate(text, size, normalSize));
      else
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, normalSize);
      text.setPivotY(text.getHeight());
    }
    for (LinearLayout shelf : this.shelves) {
      if (shelf == target) {
        if (anims != null)
          anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 1.0f));
        else
          shelf.setAlpha(1.0f);
      } else if (isAncestorShelfOf(shelf.getId(), target.getId())) {
        if (anims != null)
          anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 1.0f));
        else
          shelf.setAlpha(1.0f);
        for (int i = 0; i < shelf.getChildCount(); i++) {
          TextView text = (TextView)shelf.getChildAt(i);
          float size = text.getTextSize();
          if (anims != null)
            anims.add(TextSizeAnimator.animate(text, size, largerSize));
          else
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, largerSize);
          text.setPivotY(text.getHeight());
        }
      } else {
        if (anims != null)
          anims.add(ObjectAnimator.ofFloat(shelf, "alpha", shelf.getAlpha(), 1.0f));
        else
          shelf.setAlpha(0.0f);
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

  public Fragment getViewFor(TextView text) {
    ViewPager pager = (ViewPager)text.getTag();
    if (pager == null)
      return null;

    Fragment ret = null;

    TabShelfAdapter shelfAdapter = (TabShelfAdapter)pager.getAdapter();
    int index = shelfAdapter.getIndexOf(text);
    ret = shelfAdapter.getItem(index);
    return ret;
  }
  public Fragment gotoViewFor(TextView text) {
    ViewPager pager = (ViewPager)text.getTag();
    if (pager == null)
      return null;

    Fragment ret = null;
    // Update the mTabState
    setActiveTab(text.getId());

    TabShelfAdapter shelfAdapter = (TabShelfAdapter)pager.getAdapter();
    int index = shelfAdapter.getIndexOf(text);
    ret = shelfAdapter.getItem(index);
    pager.setCurrentItem(index, true);
    return ret;
  }

  public int getActiveTab() {
    return mTabState;
  }
  protected void setActiveTab(int mTabState) {
    this.mTabState = mTabState;
    String idName;
    try {
      idName = this.getResources().getResourceName(mTabState);
    } catch (NotFoundException e) {
      idName = "UNKNOWN ID";
    }
    Log.v("MetroBarFragment", "Setting active tab to " + idName);
  }

  private class PageChangeListener implements OnPageChangeListener {
    LinearLayout shelf;
    ViewPager pager;
    public PageChangeListener(ViewPager pager, LinearLayout shelf) {
      this.shelf = shelf;
      this.pager = pager;
    }
    public void onPageScrollStateChanged(int state) {
    }
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    public void onPageSelected(int position) {
      TextView text = (TextView)this.shelf.getChildAt(position);
      MetroBarFragment.this.setActiveTab(text.getId());
      AnimatorSet set = new AnimatorSet();
      List<Animator> anims = new ArrayList<Animator>(10);
      animateTextHighlight(anims, text);
      set.playTogether(anims);
      set.setDuration(DURATION);
      set.start();
      TabShelfAdapter shelfAdapter = (TabShelfAdapter)this.pager.getAdapter();
      Fragment active = shelfAdapter.getItem(position);
      if (active instanceof PlayControls) {
        PlayControls pc = (PlayControls)active;
        pc.readInfoFromService();
      }
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
 
  static class TextSizeAnimator extends ValueAnimator {
    public static ValueAnimator animate(final TextView target, float... sizes) {
      ValueAnimator ret = ValueAnimator.ofFloat(sizes);
      ret.addUpdateListener(new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
          target.setTextSize(TypedValue.COMPLEX_UNIT_PX, (Float)animation.getAnimatedValue());
        }
      });
      return ret;
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