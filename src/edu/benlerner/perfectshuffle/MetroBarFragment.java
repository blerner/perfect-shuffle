package edu.benlerner.perfectshuffle;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MetroBarFragment extends Fragment {
  private int                mTabState = -1;// id of innermost currently focused TextView in shelves
  private List<LinearLayout> shelves   = new ArrayList<LinearLayout>(3);
  private List<TextView>     texts     = new ArrayList<TextView>(10);

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.metrobar, container, false);

    // Grab the tab buttons from the layout and attach event handlers. The code
    // just uses standard
    // buttons for the tab widgets. These are bad tab widgets, design something
    // better, this is just
    // to keep the code simple.
    LinearLayout topShelf = (LinearLayout)view.findViewById(R.id.topShelf);
    connectShelfViews(inflater, view, topShelf);
    int viewToGoTo = R.id.home;
    if (savedInstanceState != null) {
      viewToGoTo = savedInstanceState.getInt("tab");
    }
    view.findViewById(viewToGoTo).performClick();
    return view;
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mTabState != -1)
      outState.putInt("tab", mTabState);
  }
  private void connectShelfViews(LayoutInflater inflater, View view, LinearLayout shelf) {
    if (shelf == null) return;
    this.shelves.add(shelf);
    for (int i = 0; i < shelf.getChildCount(); i++) {
      TextView text = (TextView)shelf.getChildAt(i);
      this.texts.add(text);
      Object tagObj = text.getTag();
      int tag = 0;
      LinearLayout nextShelf = null;
      if (tagObj != null) {
        String[] tagParts = tagObj.toString().split("/");
        if (tagParts[0].equals("id")) {
          tag = getResources().getIdentifier(tagParts[1], "id", this.getActivity().getPackageName());
          nextShelf = (LinearLayout)view.findViewById(tag);
          nextShelf.setTag(shelf);
          text.setTag(nextShelf);
        } else if (tagParts[0].equals("playlist")) {
          text.setTag(new Playlist());
        } else if (tagParts[0].equals("albumgrid")) {
          text.setTag(new Albumgrid());
        } else if (tagParts[0].equals("options")) {
          text.setTag(new Options());
        }
        connectShelfViews(inflater, view, nextShelf);
      }
      text.setOnClickListener(new TabClickListener(this, text, shelf, nextShelf));
    }
  }

  private class TabClickListener implements OnClickListener {
    MetroBarFragment fragment;
    TextView         text;
    LinearLayout     targetShelf;
    LinearLayout     thisShelf;

    public TabClickListener(MetroBarFragment fragment, TextView text, LinearLayout thisShelf, LinearLayout targetShelf) {
      this.fragment = fragment;
      this.text = text;
      this.thisShelf = thisShelf;
      this.targetShelf = targetShelf;
    }

    public void onClick(View v) {
      for (TextView t : this.fragment.texts) {
        if (t != this.text)
          t.setTextColor(getResources().getColor(android.R.color.white));
        else
          this.text.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
      }
      if (this.targetShelf != null) {
        hideShelvesUnlessAncestorOf(this.targetShelf);
        gotoDefaultShelfViewFor(this.targetShelf);
      } else {
        hideShelvesUnlessAncestorOf(this.thisShelf);
        gotoViewFor(this.text);
      }
    }
  }

  private boolean isAncestorShelfOf(LinearLayout ancestor, LinearLayout shelf) {
    if (shelf == ancestor) return true;
    if (shelf == null) return false;
    return isAncestorShelfOf(ancestor, (LinearLayout)shelf.getTag());
  }

  private void hideShelvesUnlessAncestorOf(LinearLayout shelf) {
    for (LinearLayout s : this.shelves) {
      if (this.isAncestorShelfOf(s, shelf))
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

  public void gotoViewFor(TextView text) {
    // mTabState keeps track of which tab is currently displaying its contents.
    // Perform a check to make sure the list tab content isn't already
    // displaying.

    if (mTabState != text.getId()) {
      // Update the mTabState
      mTabState = text.getId();

      // Fragments have access to their parent Activity's FragmentManager. You
      // can obtain the FragmentManager like this.
      FragmentManager fm = getFragmentManager();

      if (fm != null) {
        // Perform the FragmentTransaction to load in the list tab content.
        // Using FragmentTransaction#replace will destroy any Fragments
        // currently inside R.id.fragment_content and add the new Fragment
        // in its place.
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        Object tag = text.getTag();
        if (tag != null && tag instanceof Fragment) {
          ft.replace(R.id.fragment_content, (Fragment)tag);
        } else {
          ft.replace(R.id.fragment_content, PlaceholderFragment.CreateInstance(text.getText()));
        }
        ft.commit();
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
}