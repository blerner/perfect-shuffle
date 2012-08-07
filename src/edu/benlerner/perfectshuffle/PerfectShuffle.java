package edu.benlerner.perfectshuffle;

import java.lang.ref.WeakReference;

import edu.benlerner.perfectshuffle.MusicUtils.ServiceToken;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PerfectShuffle extends FragmentActivity {
  private ServiceToken mToken;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mToken = MusicUtils.bindToService(this);
    setContentView(R.layout.activity_perfect_shuffle);
    ((MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar))
      .initialize((FrameLayout)this.findViewById(R.id.fragment_content));
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    new PreloadAlbumArtTask(this)
      .execute(new BitmapDrawable(this.getResources(), BitmapFactory.decodeResource(this.getResources(), R.drawable.eighth_notes)));
    this.mReScanHandler = new RescanHandler(this);
    this.mScanListener  = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mReScanHandler.sendEmptyMessage(0);
      }
    };
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
    f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
    f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    f.addDataScheme("file");
    registerReceiver(mScanListener, f);
  }
  private static class RescanHandler extends Handler {
    final WeakReference<PerfectShuffle> shuffle;
    public RescanHandler(PerfectShuffle shuffle) {
      this.shuffle = new WeakReference<PerfectShuffle>(shuffle);
    }
    PerfectShuffle getShuffle () {
      return this.shuffle.get();
    }
    @Override
    public void handleMessage(Message msg) {
      //MetroBarFragment mb = (MetroBarFragment)PerfectShuffle.this.getFragmentManager().findFragmentById(R.id.metrobar);
      Toast.makeText(this.getShuffle(), "Going to rescan albums", Toast.LENGTH_SHORT).show();
    }
  }
  
  private BroadcastReceiver mScanListener;

  private Handler           mReScanHandler;
  @Override
  protected void onDestroy() {
    super.onDestroy();
    try {
      MusicUtils.sService.stop();
    } catch (RemoteException e) {
    }
    MusicUtils.unbindFromService(mToken);
    unregisterReceiver(mScanListener);
  }
  
  public void playSong(String path) {
    MetroBarFragment mb = (MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
    PlayControls pc = (PlayControls)mb.gotoViewFor((TextView)this.findViewById(R.id.current));
    pc.playSong(path);
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
