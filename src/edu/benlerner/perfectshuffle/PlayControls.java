package edu.benlerner.perfectshuffle;

import java.lang.ref.WeakReference;

import com.mpatric.mp3agic.Mp3File;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PlayControls extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
  SeekBar mProgress;
  TextView mStartTime;
  View mainView;
  ToggleButton mPlay;
  TextView mEndTime;
  TextView mSongName;
  TextView mAlbumName;
  TextView mArtistName;
  ImageView mAlbumArt;

  private static final int REFRESH = 1;
  private static final int PLAYSONG = 2;
  
  private static class RefreshHandler extends Handler {
    final WeakReference<PlayControls> pc;
    public RefreshHandler(PlayControls pc) {
      this.pc = new WeakReference<PlayControls>(pc);
    }
    PlayControls getPC() {
      return this.pc.get();
    }
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case REFRESH:
        long next = this.getPC().refreshNow();
        this.getPC().queueNextRefresh(next);
        break;
      case PLAYSONG:
        this.getPC().playSong((String)msg.obj);
      default:
        break;
      }
    }
  };
  RefreshHandler mRefreshHandler;
  private void queueNextRefresh(long delay) {
    try {
      if (this.mRefreshHandler != null && (MusicUtils.sService == null || MusicUtils.sService.isPlaying())) {
        Message msg = mRefreshHandler.obtainMessage(REFRESH);
        mRefreshHandler.removeMessages(REFRESH);
        mRefreshHandler.sendMessageDelayed(msg, delay);
      }
    } catch (RemoteException e) {
    }
  } 

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mRefreshHandler = new RefreshHandler(this);
  }
  @Override
  public void onDestroy() {
    this.mRefreshHandler.removeMessages(REFRESH);
    this.mRefreshHandler = null;
    super.onDestroy();
  }
  @Override
  public void onDestroyView() {
    this.mainView = null;
    this.mEndTime = null;
    this.mPlay = null;
    this.mProgress = null;
    this.mStartTime = null;
    this.mSongName = null;
    this.mAlbumName = null;
    this.mArtistName = null;
    this.mAlbumArt = null;
    super.onDestroyView();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.mainView = inflater.inflate(R.layout.current_song, container, false);

    final PlayControls frag = this;
    this.mStartTime = (TextView)mainView.findViewById(R.id.startTime);
    this.mProgress = (SeekBar)mainView.findViewById(R.id.seekBar);
    this.mPlay = (ToggleButton)mainView.findViewById(R.id.play);
    this.mEndTime = (TextView)mainView.findViewById(R.id.endTime);
    this.mArtistName = (TextView)mainView.findViewById(R.id.artistName);
    this.mAlbumName = (TextView)mainView.findViewById(R.id.albumName);
    this.mSongName = (TextView)mainView.findViewById(R.id.songTitle);
    this.mAlbumArt = (ImageView)mainView.findViewById(R.id.albumArt);
    this.mPlay.setOnClickListener(this);
    try {
      this.mPlay.setChecked(MusicUtils.sService.isPlaying());
    } catch (Exception e) {
    }
    ((ImageButton)this.mainView.findViewById(R.id.rew)).setOnClickListener(this);
    ((ImageButton)this.mainView.findViewById(R.id.fwd)).setOnClickListener(this);
    ((SeekBar)this.mainView.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
    readInfoFromService();
    return this.mainView;
  }
  
  @Override
  public void onResume() {
    super.onResume();
    long next = refreshNow();
    queueNextRefresh(next);
    readInfoFromService();
  }
  boolean readInfoStillNeeded = false;
  public void readInfoFromService() {
    try {
      if (MusicUtils.sService == null || this.mainView == null) {
        this.readInfoStillNeeded = true;
        queueNextRefresh(200);
        return;
      }
      this.mPlay.setChecked(!MusicUtils.sService.isPlaying());
      int width = this.mAlbumArt.getWidth();
      int height = this.mAlbumArt.getHeight();
      if (width <= 0) {
        this.mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          public void onGlobalLayout() {
            if (getView().getWidth() > 0) {
              getView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
              readInfoFromService();
            }
          }
        });
        return;
      }
      this.readInfoStillNeeded = false;
      long albumId = MusicUtils.sService.getAlbumId();
      if (albumId == -1 && MusicUtils.sService.getPath() != null) {
        MusicUtils.sService.openFile(MusicUtils.sService.getPath());
        albumId = MusicUtils.sService.getAlbumId();
      }
      this.mEndTime.setText(MusicUtils.makeTimeString(this.getActivity(), MusicUtils.sService.duration() / 1000));
      this.mArtistName.setText(MusicUtils.sService.getArtistName());
      this.mAlbumName.setText(MusicUtils.sService.getAlbumName());
      this.mSongName.setText(MusicUtils.sService.getTrackName());
      Bitmap art = null;
      if (albumId != -1) {
        art = MusicUtils.getArtworkQuick(this.getActivity(), albumId, width, height, true);
      }
      if (art == null) {
        art = MusicUtils.getFileArt(MusicUtils.sService.getPath());
        if (art != null) {
          if (art.getWidth() < art.getHeight())
            width = (int)(height * ((float)art.getWidth() / art.getHeight()));
          else
            height = (int)(width * ((float)art.getHeight() / art.getWidth()));
          art = Bitmap.createScaledBitmap(art, width, height, true);
        } else {
          // Try harder, one last time
          Mp3File file = new Mp3File(MusicUtils.sService.getPath(), false);
          if (file.hasId3v2Tag() && file.getId3v2Tag().getAlbumImage() != null)
            if (this.pendingScan == null)
              this.pendingScan = new SingleMediaScanner(this.getActivity(), this.getActivity(), MusicUtils.sService.getPath());
        }
      }
      if (art != null) {
        this.mAlbumArt.setImageBitmap(art);
      } else {
        this.mAlbumArt.setImageResource(R.drawable.eighth_notes);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  private SingleMediaScanner pendingScan = null;
  private class SingleMediaScanner implements MediaScannerConnectionClient {

    private MediaScannerConnection mMs;
    private String filePath;
    private Activity activity;

    public SingleMediaScanner(Activity activity, Context context, String filePath) {
      this.activity = activity;
        this.filePath = filePath;
        mMs = new MediaScannerConnection(context, this);
        mMs.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mMs.scanFile(this.filePath, null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mMs.disconnect();
        pendingScan = null;
        final Context context = this.activity.getApplicationContext();
        final NotificationManager mNotificationManager = (NotificationManager)this.activity.getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = android.R.drawable.ic_dialog_info;
        Intent notificationIntent = new Intent();
        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        CharSequence tickerText;
        String result;
        if (uri != null) {
          tickerText = "Rescan file succeeded";
          result = uri.toString();
        } else {
          tickerText = "Rescan file failed";
          result = "Not mapped to anything";
        }
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, path, result, contentIntent);
        mNotificationManager.notify(0, notification);
    }

  }
  
  public void onClick(View v) {
    if (MusicUtils.sService == null) return;
    switch (v.getId()) {
    case R.id.play:
      ToggleButton play = (ToggleButton)v;
      //play.setChecked(!play.isChecked());
      try {
        if (play.isChecked()) {
          MusicUtils.sService.pause();
        } else {
          MusicUtils.sService.play();
          long next = refreshNow();
          queueNextRefresh(next);
          break;
        }
      } catch (RemoteException e) {
      }
      break;
    case R.id.rew:
      try {
        MusicUtils.sService.seek(0);
        refreshNow();
      } catch (RemoteException e) {
      }
    }
  }
  public void playSong(String path) {
    try {
      if (MusicUtils.sService == null) return;
      if (MusicUtils.sService.isPlaying())
        MusicUtils.sService.stop();
      MusicUtils.sService.openFile(path);
      MusicUtils.sService.play();
      long next = refreshNow();
      queueNextRefresh(next);
      readInfoFromService();
    } catch (RemoteException e) {
      e.printStackTrace();
    }    
  }

  private boolean userSeekingDontRefresh = false;
  private long refreshNow() {
    if (this.readInfoStillNeeded)
      readInfoFromService();
    if (MusicUtils.sService == null || this.mProgress == null) return 500;
    try {
      long pos = MusicUtils.sService.position();
      long duration = MusicUtils.sService.duration();
      if (!userSeekingDontRefresh) {
        if ((pos >= 0) && (duration > 0)) {
          Context cxt = this.getActivity();
          if (cxt != null)
            this.mStartTime.setText(MusicUtils.makeTimeString(this.getActivity(), pos / 1000));
          int progress = (int)(1000 * pos / duration);
          this.mProgress.setProgress(progress);
  
          if (!MusicUtils.sService.isPlaying()) { return 500; }
        } else {
          this.mProgress.setProgress(1000);
        }
      }
      // calculate the number of milliseconds until the next full second, so
      // the counter can be updated at just the right time
      long remaining = 1000 - (pos % 1000);

      // approximate how often we would need to refresh the slider to
      // move it smoothly
      int width = this.mProgress.getWidth();
      if (width == 0) width = 320;
      long smoothrefreshtime = duration / width;

      if (smoothrefreshtime > remaining) return remaining;
      if (smoothrefreshtime < 20) return 20;
      return smoothrefreshtime;
    } catch (RemoteException ex) {
    }
    return 500;
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (fromUser) {
      try {
        if (MusicUtils.sService.isPlaying()) {
          MusicUtils.sService.seek((int)((float)MusicUtils.sService.duration() * ((float)progress / seekBar.getMax())));
        }
      } catch (RemoteException e) {
      }
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    this.userSeekingDontRefresh = true;
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    this.userSeekingDontRefresh = false;
  }
}
