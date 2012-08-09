package edu.benlerner.perfectshuffle;

import java.lang.ref.WeakReference;

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
import edu.benlerner.perfectshuffle.MusicUtils.ServiceToken;

public class PerfectShuffle extends FragmentActivity {
  private ServiceToken mToken;

  private static final int QUIT = 2;
  private static final int RESCAN = 3;

  
  //private PlayControls currentPlaying;
  private void initializeUI() {
    MetroBarFragment metrobar = (MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
    metrobar.initialize((FrameLayout)this.findViewById(R.id.fragment_content));
    //this.currentPlaying = (PlayControls)metrobar.getViewFor((TextView)this.findViewById(R.id.current));
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mToken = MusicUtils.bindToService(this);
    setContentView(R.layout.activity_perfect_shuffle);
    initializeUI();
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    new PreloadAlbumArtTask(this)
      .execute(new BitmapDrawable(this.getResources(), BitmapFactory.decodeResource(this.getResources(), R.drawable.eighth_notes)));
    this.mPerfectShuffleHandler = new PerfectShuffleHandler(this);
    this.mScanListener  = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mPerfectShuffleHandler.sendEmptyMessage(RESCAN);
      }
    };
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
    f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
    f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    f.addDataScheme("file");
    registerReceiver(mScanListener, f);
    this.destroyForConfigChange = false;
  }
  boolean destroyForConfigChange;
  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    this.destroyForConfigChange = true;
    return super.onRetainCustomNonConfigurationInstance();
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (!this.destroyForConfigChange) {
      try {
        if (MusicUtils.sService != null && MusicUtils.sService.isPlaying())
          MusicUtils.sService.stop();
      } catch (RemoteException e) {
      }
    }
    MusicUtils.unbindFromService(mToken);
    unregisterReceiver(mScanListener);
  }
  

  private static class PerfectShuffleHandler extends Handler {
    final WeakReference<PerfectShuffle> shuffle;
    public PerfectShuffleHandler(PerfectShuffle shuffle) {
      this.shuffle = new WeakReference<PerfectShuffle>(shuffle);
    }
    PerfectShuffle getShuffle () {
      return this.shuffle.get();
    }
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case RESCAN:
        //MetroBarFragment mb = (MetroBarFragment)PerfectShuffle.this.getFragmentManager().findFragmentById(R.id.metrobar);
        Toast.makeText(this.getShuffle(), "Going to rescan albums", Toast.LENGTH_SHORT).show();
        break;
      case QUIT:
//                // This can be moved back to onCreate once the bug that prevents
//                // Dialogs from being started from onCreate/onResume is fixed.
//                new AlertDialog.Builder(PerfectShuffle.this)
//                        .setTitle(R.string.service_start_error_title)
//                        .setMessage(R.string.service_start_error_msg)
//                        .setPositiveButton(R.string.service_start_error_button,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                        finish();
//                                    }
//                                })
//                        .setCancelable(false)
//                        .show();
        break;

      default:
        break;
      }
    }
  };
  
  
  private BroadcastReceiver mScanListener;

  private Handler           mPerfectShuffleHandler;

  public void playSong(String path) {
    MetroBarFragment mb = (MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
    PlayControls pc = (PlayControls)mb.getViewFor((TextView)this.findViewById(R.id.current));
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
    boolean foundAnything;
    public PreloadAlbumArtTask(Activity act) {
      this.act = act;
      this.cr = act.getContentResolver();
      this.foundAnything = false;
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
          this.foundAnything = true;
          Cursor albumCursor = this.cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colsData,
              where, new String[] { String.valueOf(audioCursor.getInt(0)) }, null);
          albumCursor.moveToPosition(-1);
          boolean found = false;
          while (albumCursor.moveToNext())
            if (MusicUtils.getCachedFileArt(audioCursor.getInt(0), albumCursor.getString(1), params[0])) {
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
      if (this.foundAnything)
        Toast.makeText(this.act, "Finished preloading album art", Toast.LENGTH_SHORT).show();
    }
  }
}
