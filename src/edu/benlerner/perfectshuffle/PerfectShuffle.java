package edu.benlerner.perfectshuffle;

import java.lang.ref.WeakReference;
import java.util.List;

import android.animation.Animator;
import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.util.TypedValue;
import android.view.Menu;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.benlerner.perfectshuffle.ExpandoGroup.SizeAnimator;
import edu.benlerner.perfectshuffle.ExpandoGroup.SizeAnimator.Size;
import edu.benlerner.perfectshuffle.MetroBarFragment.AnimationReason;
import edu.benlerner.perfectshuffle.MusicUtils.Caches;
import edu.benlerner.perfectshuffle.MusicUtils.ServiceToken;

public class PerfectShuffle extends FragmentActivity {
  private ServiceToken mToken;

  private static final int QUIT = 2;
  private static final int RESCAN = 3;
  public static final int AUTO_COLLAPSE = 4;

  
  private void collapseMetroBar() {
    MetroBarFragment metrobar = (MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
    metrobar.collapseIfStillNeeded();
  }
  //private PlayControls currentPlaying;
  private void initializeUI() {
    MetroBarFragment metrobar = (MetroBarFragment)this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
    metrobar.initialize((FrameLayout)this.findViewById(R.id.fragment_content));
    final FrameLayout metrobarContainer = (FrameLayout)this.findViewById(R.id.metrobarContainer);
    final int halfHeightDip = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, this.getResources().getDisplayMetrics());
    final int fullHeightDip = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, this.getResources().getDisplayMetrics());
    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
      metrobarContainer.getLayoutParams().height = fullHeightDip;
      metrobar.setDisplayMode(MetroBarFragment.DisplayMode.FULL_HEIGHT, null);
    } else {
      metrobar.setDisplayMode(MetroBarFragment.DisplayMode.ONE_LINE, null);
      metrobarContainer.getLayoutParams().height = halfHeightDip;
      metrobar.addCustomAnimator(new MetroBarFragment.CustomAnimator() {
        @Override
        public void animateFragment(List<Animator> anims, MetroBarFragment fragment, AnimationReason reason) {
          switch (reason) {
          case COLLAPSE:
            if (anims != null) {
              anims.add(SizeAnimator.ofSize(metrobarContainer, Size.HEIGHT, fullHeightDip, halfHeightDip));
            } else {
              metrobarContainer.getLayoutParams().height = halfHeightDip;
            }
            break;
          case EXPAND:
            if (anims != null) {
              anims.add(SizeAnimator.ofSize(metrobarContainer, Size.HEIGHT, halfHeightDip, fullHeightDip));
            } else {
              metrobarContainer.getLayoutParams().height = fullHeightDip;
            }
            break;
          case SETUP:
            break;
          }
        }
      });
    }
    //this.currentPlaying = (PlayControls)metrobar.getViewFor((TextView)this.findViewById(R.id.current));
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Playlist.TestPlaylist();
    super.onCreate(savedInstanceState);
    this.mToken = MusicUtils.bindToService(this);
    setContentView(R.layout.activity_perfect_shuffle);
    initializeUI();
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    this.mPerfectShuffleHandler = new PerfectShuffleHandler(this);
    this.mScanListener  = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mPerfectShuffleHandler.sendEmptyMessage(RESCAN);
      }
    };
    this.mPlayListener = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        MetroBarFragment mb = (MetroBarFragment)PerfectShuffle.this.getSupportFragmentManager().findFragmentById(R.id.metrobar);
        PlayControls pc = (PlayControls)mb.getViewFor((TextView)PerfectShuffle.this.findViewById(R.id.current));
        pc.readInfoFromService();
      }
    };
    this.mPerfectShuffleHandler.sendEmptyMessage(RESCAN);
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
    f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
    f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    f.addDataScheme("file");
    registerReceiver(mScanListener, f);
    f = new IntentFilter();
    f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
    f.addAction(MediaPlaybackService.META_CHANGED);
    registerReceiver(mPlayListener, f);
    this.destroyForConfigChange = false;
  }
  public void sendEmptyMessage(int message) {
    this.mPerfectShuffleHandler.sendEmptyMessage(message);
  }
  public void sendEmptyMessageDelayed(int message, int delayMillis) {
    this.mPerfectShuffleHandler.sendEmptyMessageDelayed(message, delayMillis);
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
    unregisterReceiver(mPlayListener);
  }
  
  public float dipToPx(float dip) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, this.getResources().getDisplayMetrics());
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
      PerfectShuffle shuffle;
      switch (msg.what) {
      case AUTO_COLLAPSE:
        shuffle = this.getShuffle();
        shuffle.collapseMetroBar();
        break;
      case RESCAN:
        shuffle = this.getShuffle();
        int width = (int)shuffle.dipToPx(96.f);
        int height = width;
        shuffle.new PreloadAlbumArtTask(shuffle, width, height)
          .execute(new BitmapDrawable(shuffle.getResources(), BitmapFactory.decodeResource(shuffle.getResources(), R.drawable.eighth_notes)));
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
  private BroadcastReceiver mPlayListener;

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
    int width, height;
    public PreloadAlbumArtTask(Activity act, int width, int height) {
      this.act = act;
      this.cr = act.getContentResolver();
      this.foundAnything = false;
      this.width = width;
      this.height = height;
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
        if (MusicUtils.getCachedArtwork(act, audioCursor.getInt(0), width, height, null, Caches.SMALL) == null) {
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
          if (!found) {
            Bitmap defaultIcon = params[0].getBitmap();
            MusicUtils.getCachedArtwork(act, audioCursor.getInt(0), defaultIcon.getWidth(), defaultIcon.getHeight(), defaultIcon, Caches.SMALL);
          }
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
