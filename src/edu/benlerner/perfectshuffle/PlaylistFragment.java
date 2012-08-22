package edu.benlerner.perfectshuffle;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.mpatric.mp3agic.Mp3File;

public class Playlist extends ListFragment {

  private int            currentPosition = 0;
  private Cursor         cursor          = null;
  private BitmapDrawable defaultAlbumArt;

  // private LibraryCache cache = null;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.playlist, null);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    this.updateSongList();
    this.defaultAlbumArt = new BitmapDrawable(this.getResources(), BitmapFactory.decodeResource(this.getResources(),
        R.drawable.eighth_notes));
  }

  protected void updateSongList() {
    String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
    String selection = MediaStore.Audio.Media.DATA + " like ?";
    String[] selectionArgs = { dirPath + "%" };
    String[] cols = { MediaStore.Audio.Media._ID, 
        MediaStore.Audio.Media.DATA, 
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID};
    Activity act = this.getActivity();
    ContentResolver cr = act.getContentResolver();
    Cursor audioCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, selection, selectionArgs,
        MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC, " + MediaStore.Audio.Media.TRACK + " COLLATE NOCASE ASC");
    String[] from = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM };
    int[] to = { R.id.playlistItemTitle, R.id.playlistItemSubtitle };
    SimpleCursorAdapter adapter = new PlaylistSimpleCursorAdapter(act, R.layout.playlist_item, audioCursor, from, to, 0);
    act.startManagingCursor(audioCursor);
    this.cursor = audioCursor;
    this.setListAdapter(adapter);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    currentPosition = position;
    
    final int dataCol = this.cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
    this.cursor.moveToPosition(position);
    final String path = this.cursor.getString(dataCol);
    try {
      Mp3File file = new Mp3File(path, false);
      String group = file.getId3v2Tag().getGrouping();
      if (group == null)
        group = "<null>";
      Toast.makeText(this.getActivity(), "Group is " + group, Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // What does TITLE_KEY actually do?  No one knows...
    //Toast.makeText(this.getActivity(), "Group is " + this.cursor.getString(this.cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY)), Toast.LENGTH_SHORT).show();
    ((PerfectShuffle)this.getActivity()).playSong(path);
  };

  protected void nextSong() {
    if (++currentPosition >= this.cursor.getCount()) {
      // Last song, just reset currentPosition
      currentPosition = 0;
    } else {
      this.onListItemClick(null, null, currentPosition, -1);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.activity_song_player, menu);
  }

  class PlaylistSimpleCursorAdapter extends SimpleCursorAdapter {
    public PlaylistSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      this.cursor = c;
    }

    Cursor  cursor;
    Context context          = null;
    int     layoutResourceId = 0;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = super.getView(position, convertView, parent);
      final int albumIdCol = this.cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
      this.cursor.moveToPosition(position);
      long album_id = this.cursor.getLong(albumIdCol);
      ImageView img = (ImageView)row.findViewById(R.id.playlistItemThumbnail);
      Drawable bm = MusicUtils.getCachedArtwork(row.getContext(), album_id, defaultAlbumArt);
      img.setImageDrawable(bm);
      return row;
    }
  }
}