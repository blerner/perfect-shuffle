package edu.benlerner.perfectshuffle;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PlayControls extends Fragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.current_song, container, false);
    final PlayControls frag = this;
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
      MusicUtils.sService.openFile(path);
      ((TextView)this.getView().findViewById(R.id.endTime))
        .setText(MusicUtils.makeTimeString(this.getActivity(), MusicUtils.sService.duration() / 1000));
      MusicUtils.sService.play();
    } catch (RemoteException e) {
      e.printStackTrace();
    }    
  }
}
