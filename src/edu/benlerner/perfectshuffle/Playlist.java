package edu.benlerner.perfectshuffle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Playlist extends ListFragment {

  private List<File>        songs           = null;
  private MediaPlayer       mp              = null;
  private int               currentPosition = 0;
  //private LibraryCache      cache           = null;

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
    this.mp = new MediaPlayer();
    //this.cache = new LibraryCache();
    this.updateSongList();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (this.mp.isPlaying()) this.mp.stop();
    this.mp.release();
    this.mp = null;
    //this.cache = null;
  }

  protected void updateSongList() {
    //LibraryCache.CacheStructure cache = this.cache.CreateAlbumCache(this.getActivity(), null);
    this.songs = new ArrayList<File>(0);
    ArrayAdapter<File> songList = new PlaylistViewAdapter(this.getActivity(), R.layout.playlist_item, this.songs);
    this.setListAdapter(songList);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    currentPosition = position;
    playSong(songs.get(position).getAbsolutePath());
  };

  protected void playSong(String songPath) {
    try {

      mp.reset();
      mp.setDataSource(songPath);
      mp.prepare();
      mp.start();

      // Setup listener so next song starts automatically
      mp.setOnCompletionListener(new OnCompletionListener() {
        public void onCompletion(MediaPlayer arg0) {
          nextSong();
        }
      });

    } catch (IOException e) {
      Log.v(getString(R.string.app_name), e.getMessage());
    }
  }

  protected void nextSong() {
    if (++currentPosition >= songs.size()) {
      // Last song, just reset currentPosition
      currentPosition = 0;
    } else {
      // Play next song
      playSong(songs.get(currentPosition).getAbsolutePath());
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.activity_song_player, menu);
  }
}

class PlaylistViewAdapter extends ArrayAdapter<File> {
  Context    context          = null;
  int        layoutResourceId = 0;
  List<File> files            = null;

  public PlaylistViewAdapter(Context context, int layoutResourceId, List<File> files) {
    super(context, layoutResourceId, files);
    this.context = context;
    this.layoutResourceId = layoutResourceId;
    this.files = files;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = convertView;
    PlaylistItemHolder holder = null;

    if (row == null) {
      LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();
      row = inflater.inflate(this.layoutResourceId, parent, false);

      holder = new PlaylistItemHolder();
      holder.thumbnail = (ImageView) row.findViewById(R.id.playlistItemThumbnail);
      holder.txtTitle = (TextView) row.findViewById(R.id.playlistItemTitle);
      holder.txtSubtitle = (TextView) row.findViewById(R.id.playlistItemSubtitle);

      row.setTag(holder);
    } else {
      holder = (PlaylistItemHolder) row.getTag();
    }

    File file = files.get(position);
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    mmr.setDataSource(file.getAbsolutePath());

    holder.txtTitle.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
    holder.txtTitle.setSelected(true);

    holder.txtSubtitle.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
    holder.txtSubtitle.setSelected(true);

    byte[] image = mmr.getEmbeddedPicture();
    if (image == null) {
      holder.thumbnail.setImageResource(android.R.drawable.gallery_thumb);
    } else {
      Bitmap imageBmp = BitmapFactory.decodeByteArray(image, 0, image.length);
      holder.thumbnail.setImageBitmap(imageBmp);
    }

    return row;
  }

  static class PlaylistItemHolder {
    ImageView thumbnail;
    TextView  txtTitle;
    TextView  txtSubtitle;
  }
}

