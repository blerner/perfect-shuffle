package edu.benlerner.perfectshuffle;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
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

public class PlayControls extends Fragment {
  SeekBar mProgress;

  private static final int REFRESH = 1;
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
      default:
        break;
      }
    }
  };
  RefreshHandler mRefreshHandler;
  private void queueNextRefresh(long delay) {
    try {
      if (MusicUtils.sService == null || MusicUtils.sService.isPlaying()) {
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
    super.onDestroy();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.current_song, container, false);
    final PlayControls frag = this;
    this.mProgress = (SeekBar)view.findViewById(R.id.seekBar);
    View.OnClickListener clickHandler = new View.OnClickListener() {
      public void onClick(View v) {
        frag.onClick(v);
      }
    };
    ToggleButton play = ((ToggleButton)view.findViewById(R.id.play));
    play.setOnClickListener(clickHandler);
    try {
      play.setChecked(MusicUtils.sService.isPlaying());
    } catch (Exception e) {
    }
    ((ImageButton)view.findViewById(R.id.rew)).setOnClickListener(clickHandler);
    ((ImageButton)view.findViewById(R.id.fwd)).setOnClickListener(clickHandler);
    return view;
  }
  @Override
  public void onResume() {
    super.onResume();
    long next = refreshNow();
    queueNextRefresh(next);
    readInfoFromService();
  }
  public void readInfoFromService() {
    try {
      View view = this.getView();
      if (view == null) return;
      ImageView albumArt = (ImageView)view.findViewById(R.id.albumArt);
      int width = albumArt.getWidth();
      if (width <= 0) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          public void onGlobalLayout() {
            if (getView().getWidth() > 0) {
              getView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
              readInfoFromService();
            }
          }
        });
        return;
      }
      albumArt.setImageResource(R.drawable.eighth_notes);
      if (MusicUtils.sService == null) return;
      TextView endTime = (TextView)this.getView().findViewById(R.id.endTime);
      endTime.setText(MusicUtils.makeTimeString(this.getActivity(), MusicUtils.sService.duration() / 1000));
      long albumId = MusicUtils.sService.getAlbumId();
      Bitmap art = null;
      if (albumId != -1) {
        art = MusicUtils.getArtworkQuick(this.getActivity(), albumId, width, width);
      }
      if (art == null) {
        art = MusicUtils.getFileArt(MusicUtils.sService.getPath());
        art = Bitmap.createScaledBitmap(art, width, width, true);
      }
      if (art != null) {
        albumArt.setImageBitmap(art);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  void onClick(View v) {
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
      } catch (RemoteException e) {
      }
    }
  }
  public void playSong(String path) {
    try {
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

  private long refreshNow() {
    IMediaPlaybackService mService = MusicUtils.sService;
    if (mService == null || this.mProgress == null) return 500;
    try {
      long pos = mService.position();
      long duration = mService.duration();
      if ((pos >= 0) && (duration > 0)) {
        // mCurrentTime.setText(MusicUtils.makeTimeString(this.getActivity(),
        // pos / 1000));
        int progress = (int)(1000 * pos / duration);
        mProgress.setProgress(progress);

        if (!mService.isPlaying()) { return 500; }
      } else {
        mProgress.setProgress(1000);
      }
      // calculate the number of milliseconds until the next full second, so
      // the counter can be updated at just the right time
      long remaining = 1000 - (pos % 1000);

      // approximate how often we would need to refresh the slider to
      // move it smoothly
      int width = mProgress.getWidth();
      if (width == 0) width = 320;
      long smoothrefreshtime = duration / width;

      if (smoothrefreshtime > remaining) return remaining;
      if (smoothrefreshtime < 20) return 20;
      return smoothrefreshtime;
    } catch (RemoteException ex) {
    }
    return 500;
  }
}
