package edu.benlerner.perfectshuffle;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.widget.Toast;

public class PerfectShuffle extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_perfect_shuffle);
    new PreloadAlbumArtTask(this)
      .execute(new BitmapDrawable(this.getResources(), BitmapFactory.decodeResource(this.getResources(), R.drawable.eighth_notes)));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_perfect_shuffle, menu);
    return true;
  }
  private class PreloadAlbumArtTask extends AsyncTask<BitmapDrawable, Void, Void> {
    Activity act;
    ContentResolver cr;
    public PreloadAlbumArtTask(Activity act) {
      this.act = act;
      this.cr = act.getContentResolver();
    }
    
    @Override
    protected Void doInBackground(BitmapDrawable... params) {
      if (params.length == 0)
        return null;
      final String[] cols = { MediaStore.Audio.Albums._ID };
      final String[] colsData = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA };
      Cursor audioCursor = this.cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, cols, null, null,
          MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE ASC");
      audioCursor.moveToPosition(-1);
      final String where = android.provider.MediaStore.Audio.Media.ALBUM_ID  + "=?";
      while (audioCursor.moveToNext()) {
        if (MusicUtils.getCachedArtwork(act, audioCursor.getInt(0), null) == null) {
          Cursor albumCursor = this.cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colsData,
              where, new String[] { String.valueOf(audioCursor.getInt(0)) }, null);
          albumCursor.moveToPosition(-1);
          boolean found = false;
          while (albumCursor.moveToNext())
            if (MusicUtils.getCachedFileArt(act, audioCursor.getInt(0), albumCursor.getString(1), params[0])) {
              found = true;
              break;
            }
          if (!found)
            MusicUtils.getCachedArtwork(act, audioCursor.getInt(0), params[0]);
          albumCursor.close();
        }
      }
      audioCursor.close();
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      Toast.makeText(this.act, "Finished preloading album art", Toast.LENGTH_SHORT).show();
    }
  }
}
