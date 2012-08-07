package edu.benlerner.perfectshuffle;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class Albumgrid extends Fragment {

  private GridView     mGridView;
  private BitmapDrawable defaultAlbumArt;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.albumgrid, container, false);
    this.mGridView = (GridView)view.findViewById(R.id.grid_view);
    this.defaultAlbumArt = new BitmapDrawable(this.getResources(), BitmapFactory.decodeResource(this.getResources(), R.drawable.eighth_notes));
    
    FrameLayout buffer = new FrameLayout(this.getActivity());
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    ImageView img = new ImageView(this.getActivity());
    img.setImageDrawable(this.defaultAlbumArt);
    buffer.addView(img, layoutParams);
    buffer.forceLayout();
    buffer.measure(1000, 1000);
    this.mGridView.setColumnWidth(buffer.getMeasuredWidth());
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    this.updateAlbumList();
  }

  Cursor cursor = null;
  final static String[] noStrings = new String[0];
  final static int[] noInts = new int[0];
  protected void updateAlbumList() {
    // Setup our onItemClickListener to emulate the onListItemClick() method of
    // ListFragment.
    //LibraryCache.CacheStructure cache = this.cache.GetAlbumCache(this.getActivity());
    String[] cols = { MediaStore.Audio.Albums._ID,
        //MediaStore.Audio.Albums.ALBUM_ART,
        MediaStore.Audio.Albums.ALBUM};//,
//        MediaStore.Audio.Albums.NUMBER_OF_SONGS};
    Activity act = this.getActivity();
    ContentResolver cr = act.getContentResolver();
    Cursor audioCursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, cols, 
        null, null, 
        MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE ASC");
    //String[] from = {MediaStore.Audio.Albums.ALBUM_ART};//,
//        MediaStore.Audio.Albums.ALBUM,
//        MediaStore.Audio.Albums.NUMBER_OF_SONGS};
    //int[] to = {R.id.albumgridItemThumbnail};//,
//        R.id.albumgridItemName,
//        R.id.albumgridItemNumTracks};
    SimpleCursorAdapter adapter = 
        new CachedSimpleCursorAdapter(act, R.layout.albumgrid_item, audioCursor, noStrings, noInts, 0);
    this.cursor = audioCursor;
    this.getActivity().startManagingCursor(audioCursor);
    this.mGridView.setAdapter(adapter);
    
    mGridView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onGridItemClick((GridView)parent, view, position, id);
      }
    });
  }

  public void onGridItemClick(GridView g, View v, int position, long id) {
    Activity activity = getActivity();

    if (activity != null) {
      final int titleCol = this.cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
      this.cursor.moveToPosition(position);
      final String title = this.cursor.getString(titleCol);
      
      // Display a simple Toast to demonstrate that the click event is working.
      // Notice that Fragments have a
      // getString() method just like an Activity, so that you can quickly
      // access your localized Strings.
      Toast.makeText(activity, "Clicked on " + title, Toast.LENGTH_SHORT).show();
    }
  }

  class CachedSimpleCursorAdapter extends SimpleCursorAdapter {

    public CachedSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = super.getView(position, convertView, parent);
      long album_id = this.getItemId(position);
      ImageView img = (ImageView)v.findViewById(R.id.albumgridItemThumbnail);
      Drawable bm = MusicUtils.getCachedArtwork(v.getContext(), album_id, defaultAlbumArt);
      img.setImageDrawable(bm);
      return v;
    }    
  }
}
