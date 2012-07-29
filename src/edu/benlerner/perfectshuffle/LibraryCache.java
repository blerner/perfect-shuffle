package edu.benlerner.perfectshuffle;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

public final class LibraryCache {
  
  private static String     LIBRARY_CACHE = "library.dat";
  private static final File MEDIA_PATH    = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

  public CacheStructure CreateAlbumCache(Context ctx, CacheStructure cache) {
    if (cache == null) {
      cache = new CacheStructure();
    }
    if (cache.allAlbums == null)
      cache.allAlbums = new HashMap<String, LibraryCache.AlbumInfo>();
    if (cache.tracks != null) {
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      for (AlbumInfo info : cache.allAlbums.values())
        info.numTracks = 0;
      for (TrackInfo track : cache.tracks) {
        mmr.setDataSource(track.filename);
        String albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        AlbumInfo info = cache.allAlbums.get(albumName);
        if (info == null) {
          info = new AlbumInfo();
          info.albumName = albumName;
          info.numTracks = 0;
          info.thumbnail = mmr.getEmbeddedPicture();
          cache.allAlbums.put(albumName, info);
        }
        if (info.albumName.equals(null))
          info.albumName = "<Single Tracks>";
        info.numTracks++;
        mmr.release();
      }
    } else {
      ArrayList<AlbumInfo> albums = SubtreeFilter.listAlbums(this, MEDIA_PATH, new Mp3Filter());
      cache.allAlbums = new HashMap<String, AlbumInfo>(albums.size());
      for (AlbumInfo album : albums)
        cache.allAlbums.put(album.albumName, album);
    }
    return cache;
  }
  public CacheStructure CreateCache(Context ctx) {
    CacheStructure cache = new CacheStructure();
    cache.tracks = new TrackInfo[1000];
    cache.tracks = SubtreeFilter.listFiles(this, MEDIA_PATH, new Mp3Filter()).toArray(cache.tracks);
    return cache;
  }
  public CacheStructure GetCache(Context ctx) {
    File cacheDir = ctx.getCacheDir();
    File cacheFile = new File(cacheDir, LibraryCache.LIBRARY_CACHE);
    try {
      FileInputStream fos = new FileInputStream(cacheFile);
      ObjectInputStream oos = new ObjectInputStream(fos);
      CacheStructure cache = (CacheStructure)oos.readObject();
      oos.close();
      return cache;
    } catch (Exception e) {
      CacheStructure cache = CreateCache(ctx);
      WriteCache(ctx, cache);
      return cache;
    }
  }
  public CacheStructure GetAlbumCache(Context ctx) {
    File cacheDir = ctx.getCacheDir();
    File cacheFile = new File(cacheDir, LibraryCache.LIBRARY_CACHE);
    try {
      FileInputStream fos = new FileInputStream(cacheFile);
      ObjectInputStream oos = new ObjectInputStream(fos);
      CacheStructure cache = (CacheStructure)oos.readObject();
      oos.close();
      return cache;
    } catch (Exception e) {
      CacheStructure cache = CreateAlbumCache(ctx, null);
      WriteCache(ctx, cache);
      return cache;
    }
  }
  public boolean WriteCache(Context ctx, CacheStructure cache) {
    File cacheDir = ctx.getCacheDir();
    File cacheFile = new File(cacheDir, LibraryCache.LIBRARY_CACHE);
    try {
      FileOutputStream fos = new FileOutputStream(cacheFile);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(cache);
      oos.close();
      return true;
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
  }
  
  public class CacheStructure implements Serializable {
    private static final long serialVersionUID = -4298658269935832948L;
    TrackInfo[] tracks;
    HashMap<String, AlbumInfo> allAlbums;
    HashMap<String, List<Integer>> allArtists;
    HashMap<String, List<Integer>> allGenres;
    public Set<String> getAlbums() {
      return this.allAlbums.keySet();
    }
    public Set<String> getArtists() {
      return this.allArtists.keySet();
    }
    public Set<String> getGenres() {
      return this.allGenres.keySet();
    }
    public List<TrackInfo> mapTrack(List<Integer> trackIds) {
      if (trackIds == null) return null;
      List<TrackInfo> ret = new ArrayList<TrackInfo>(trackIds.size());
      for (int i = 0; i < trackIds.size(); i++) {
        ret.add(this.tracks[trackIds.get(i)]);
      }
      return ret;
    }
    public AlbumInfo getAlbumTracks(String album) {
      return this.allAlbums.get(album);
    }
    public List<TrackInfo> getArtistTracks(String album) {
      return mapTrack(this.allArtists.get(album));
    }
    public List<TrackInfo> getGenreTracks(String album) {
      return mapTrack(this.allGenres.get(album));
    }
  }
  
  public class TrackInfo implements Serializable {
    private static final long serialVersionUID = -8192073843733982197L;
    public String filename;
    public String txtTitle;
    public String txtSubtitle;
    public String group;
    public String trackNum;
  }
  public class AlbumInfo implements Serializable {
    private static final long serialVersionUID = 9199831358833886277L;
    public String albumName;
    public byte[] thumbnail;
    public int    numTracks;
  }
  
  private interface Callable<Ret, T, U> {
    Ret go(T t, U u, int length);
  }
  static class SubtreeFilter {
    public static ArrayList<TrackInfo> listFiles(LibraryCache c, File source, FilenameFilter filter) {
      ArrayList<TrackInfo> files = new ArrayList<TrackInfo>();
      helper(c, source, files, filter, false, new Callable<TrackInfo, LibraryCache, File>() {
        public TrackInfo go(LibraryCache c, File file, int length) {
          return trackInfo(c, file);
        }
      });
      return files;
    }
    public static ArrayList<AlbumInfo> listAlbums(LibraryCache c, File source, FilenameFilter filter) {
      ArrayList<AlbumInfo> albums = new ArrayList<AlbumInfo>();
      assert (source.isDirectory());
      helper(c, source, albums, filter, true, new Callable<AlbumInfo, LibraryCache, File>() {
        public AlbumInfo go(LibraryCache c, File file, int length) {
          AlbumInfo ret = albumInfo(c, file);
          ret.numTracks = length;
          return ret;
        }
      });
      return albums;
    }

    private static <T> void helper(LibraryCache c, File source, ArrayList<T> files, FilenameFilter filter, boolean oneAndDone, Callable<T, LibraryCache, File> action) {
      boolean seenFile = false;
      File[] sourceFiles = source.listFiles();
      for (File file : sourceFiles) {
        if (file.isFile()) {
          if (!oneAndDone || !seenFile) {
            if (filter.accept(file, file.getName())) {
              files.add(action.go(c, file, sourceFiles.length));
              seenFile = true;
            }
          }
        } else {
          helper(c, file, files, filter, oneAndDone, action);
        }
      }
    }
    private static TrackInfo trackInfo(LibraryCache c, File file) {
      TrackInfo ret = c.new TrackInfo();
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      mmr.setDataSource(file.getAbsolutePath());

      ret.filename = file.getAbsolutePath();
      
      ret.txtTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
      
      ret.txtSubtitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            
      ret.trackNum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
      try {
        Mp3File mp3 = new Mp3File(file.getAbsolutePath(), false);
        if (mp3.hasId3v2Tag()) {
          ID3v2 tag = mp3.getId3v2Tag();
          ret.group = tag.getGrouping();
        }
      } catch (InvalidDataException e) {
      } catch (UnsupportedTagException e) {
      } catch (IOException e) {
      }
      mmr.release();
      return ret;
    }
    private static AlbumInfo albumInfo(LibraryCache c, File file) {
      AlbumInfo ret = c.new AlbumInfo();
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      mmr.setDataSource(file.getAbsolutePath());
      ret.albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
      if (ret.albumName == null)
        ret.albumName = "<Single tracks>";
      ret.thumbnail = mmr.getEmbeddedPicture();
      mmr.release();
      return ret;
    }
  }

  class Mp3Filter implements FilenameFilter {
    public boolean accept(File dir, String name) {
      return (name.endsWith(".mp3"));
    }
  }
}

