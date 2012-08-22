package edu.benlerner.perfectshuffle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import android.util.Log;
import android.util.Pair;

public class Playlist {
  public class PlaylistItem {
    int songId;
    String songPath;
  }
  public interface PlaylistEntry {
    public int size();
    public Collection<PlaylistItem> items();
  }
  public final class SingleEntry implements PlaylistEntry {
    PlaylistItem item;
    
    public SingleEntry(int id, String path) {
      this.item = new PlaylistItem();
      this.item.songId = id;
      this.item.songPath = path;
    }
    
    @Override
    public int size() {
      return 1;
    }

    @Override
    public Collection<PlaylistItem> items() {
      ArrayList<PlaylistItem> ret = new ArrayList<PlaylistItem>(1);
      ret.add(this.item);
      return ret;
    }
  }
  public final class SeveralEntries implements PlaylistEntry {
    ArrayList<PlaylistItem> entries;
    public SeveralEntries(Collection<PlaylistItem> entries) {
      this.entries = new ArrayList<Playlist.PlaylistItem>(entries);
    }
    @Override
    public int size() {
      return this.entries.size();
    }
    @Override
    public Collection<PlaylistItem> items() {
      return this.entries;
    }
  }
  ArrayList<Integer> songs;
  Random rand = new Random();
  public static void TestPlaylist() {
    Playlist pl = new Playlist(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    Log.d("Playlist", "Playlist is initially " + pl.debug());
    pl.shuffleFromTo(4,  8);
    Log.d("Playlist", "Playlist after shuffleFromTo(4, 8) is " + pl.debug());
    pl = new Playlist(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
    Log.d("Playlist", "Playlist is initially " + pl.debug());
    pl.insertShuffledAfter(5, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
    Log.d("Playlist", "Playlist after insertShuffledAfter(5, 100--109) is " + pl.debug());
    pl = new Playlist(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    Log.d("Playlist", "Playlist is initially " + pl.debug());
    pl.insertShuffledAfter(5, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109);
    Log.d("Playlist", "Playlist after insertShuffledAfter(5, 100--109) is " + pl.debug());
  }
  private String debug() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (this.songs.size() > 1)
      sb.append(this.songs.get(0));
    for (int i = 1; i < this.songs.size(); i++) {
      sb.append(", ");
      sb.append(String.valueOf(this.songs.get(i)));
    }
    sb.append("}");
    return sb.toString();
  }
  public Collection<Integer> SongIds() {
    return this.songs;
  }
  public Playlist(int... songIds) {
    this.songs = new ArrayList<Integer>(songIds.length);
    for (Integer songId : songIds)
      this.songs.add(songId);
  }
  public Playlist(Collection<? extends Integer> songIds) {
    this.songs = new ArrayList<Integer>(songIds);
  }
  public void shuffleAll() {
    this.shuffleFromTo(0, this.songs.size());
  }
  public void shuffleFrom(int index) {
    this.shuffleFromTo(index, this.songs.size());
  }
  public void shuffleFromTo(int start, int end) {
    /* Perform a Fisher-Yates shuffle between the two indices
    To shuffle an array a of n elements (indices 0..n-1):
      for i from n - 1 downto 1 do
           j <- random integer with 0 <= j <= i
           exchange a[j] and a[i]
    Generalizing 0 to start, and (n-1) to end, gives the code below
    */
    for (int i = end-1; i > start; i--) {
      int j = start + this.rand.nextInt(i - start + 1);
      Collections.swap(this.songs, i, j);
    }
  }
  public void append(int... songIds) {
    this.songs.ensureCapacity(this.songs.size() + songIds.length);
    for (Integer songId : songIds)
      this.songs.add(songId);    
  }
  public void append(Collection<? extends Integer> songIds) {
    this.songs.addAll(songIds);
  }
  public void insertShuffledAfter(int index, int... songIds) {
    Integer[] songs = new Integer[songIds.length];
    for (int i = 0; i < songIds.length; i++)
      songs[i] = songIds[i];
    insertShuffledAfter(index, songs);
  }
  public void insertShuffledAfter(int index, Collection<? extends Integer> songIds) {
    insertShuffledAfter(index, songIds.toArray(new Integer[0]));
  }
  public void insertShuffledAfter(int index, Integer[] songIds) {
    ArrayList<Pair<Integer, Integer>> songs = new ArrayList<Pair<Integer, Integer>>(songIds.length);
    int scale = this.songs.size() + songIds.length - index;
    for (int i = 0; i < songIds.length; i++)
      songs.add(new Pair<Integer, Integer>(songIds[i], index + this.rand.nextInt(scale)));
    Collections.sort(songs, new Comparator<Pair<Integer, Integer>>() {
      @Override
      public int compare(Pair<Integer, Integer> lhs, Pair<Integer, Integer> rhs) {
        int ret = lhs.second.compareTo(rhs.second);
        if (ret == 0) return lhs.first.compareTo(rhs.first);
        else return ret;
      }
    });
    StringBuilder sb = new StringBuilder();
    sb.append("{(").append(songs.get(0).first).append(", ").append(songs.get(0).second).append(")");
    for (int i = 1; i < songs.size(); i++) {
      sb.append(", (").append(songs.get(i).first).append(", ").append(songs.get(i).second).append(")");
    }
    sb.append("}");
    Log.d("Playlist", "Playlist Shuffle insertions are " + sb.toString());    
    this.songs.ensureCapacity(this.songs.size() + songIds.length);
    for (int i = 0; i < songIds.length; i++)
      this.songs.add(null);
    int toBeInserted = songIds.length - 1;
    int i = this.songs.size() - 1;
    while (toBeInserted >= 0) {
      while (i > songs.get(toBeInserted).second.intValue()) {
        this.songs.set(i, this.songs.get(i - toBeInserted - 1));
        this.songs.set(i - toBeInserted - 1, null);
        i--;
      }
      while (toBeInserted >= 0 && i <= songs.get(toBeInserted).second.intValue()) {
        this.songs.set(i, songs.get(toBeInserted--).first);
        i--;
      }
    }
  }
}
