package edu.benlerner.perfectshuffle;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.format.Time;
import android.view.View;
import android.view.Window;

public class MusicUtils {

  private final static long[] sEmptyList = new long[0];

  public static long[] getSongListForCursor(Cursor cursor) {
    if (cursor == null) { return sEmptyList; }
    int len = cursor.getCount();
    long[] list = new long[len];
    cursor.moveToFirst();
    int colidx = -1;
    try {
      colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
    } catch (IllegalArgumentException ex) {
      colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
    }
    for (int i = 0; i < len; i++) {
      list[i] = cursor.getLong(colidx);
      cursor.moveToNext();
    }
    return list;
  }

  public static long[] getSongListForArtist(Context context, long id) {
    final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
    String where = MediaStore.Audio.Media.ARTIST_ID + "=" + id + " AND " + MediaStore.Audio.Media.IS_MUSIC + "=1";
    Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where, null,
        MediaStore.Audio.Media.ALBUM_KEY + "," + MediaStore.Audio.Media.TRACK);

    if (cursor != null) {
      long[] list = getSongListForCursor(cursor);
      cursor.close();
      return list;
    }
    return sEmptyList;
  }

  public static long[] getSongListForAlbum(Context context, long id) {
    final String[] ccols = new String[] { MediaStore.Audio.Media._ID };
    String where = MediaStore.Audio.Media.ALBUM_ID + "=" + id + " AND " + MediaStore.Audio.Media.IS_MUSIC + "=1";
    Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where, null,
        MediaStore.Audio.Media.TRACK);

    if (cursor != null) {
      long[] list = getSongListForCursor(cursor);
      cursor.close();
      return list;
    }
    return sEmptyList;
  }

  public static long[] getSongListForPlaylist(Context context, long plid) {
    final String[] ccols = new String[] { MediaStore.Audio.Playlists.Members.AUDIO_ID };
    Cursor cursor = query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", plid), ccols, null,
        null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);

    if (cursor != null) {
      long[] list = getSongListForCursor(cursor);
      cursor.close();
      return list;
    }
    return sEmptyList;
  }

  public static long[] getAllSongs(Context context) {
    Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media._ID },
        MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
    try {
      if (c == null || c.getCount() == 0) { return null; }
      int len = c.getCount();
      long[] list = new long[len];
      for (int i = 0; i < len; i++) {
        c.moveToNext();
        list[i] = c.getLong(0);
      }

      return list;
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder, int limit) {
    try {
      ContentResolver resolver = context.getContentResolver();
      if (resolver == null) { return null; }
      if (limit > 0) {
        uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
      }
      return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
    } catch (UnsupportedOperationException ex) {
      return null;
    }

  }

  public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder) {
    return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
  }

  public static boolean isMediaScannerScanning(Context context) {
    boolean result = false;
    Cursor cursor = query(context, MediaStore.getMediaScannerUri(), new String[] { MediaStore.MEDIA_SCANNER_VOLUME },
        null, null, null);
    if (cursor != null) {
      if (cursor.getCount() == 1) {
        cursor.moveToFirst();
        result = "external".equals(cursor.getString(0));
      }
      cursor.close();
    }

    return result;
  }

  public static void setSpinnerState(Activity a) {
    if (isMediaScannerScanning(a)) {
      // start the progress spinner
      a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);

      a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
    } else {
      // stop the progress spinner
      a.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
    }
  }

  static protected Uri getContentURIForPath(String path) {
    return Uri.fromFile(new File(path));
  }

  /*
   * Try to use String.format() as little as possible, because it creates a new
   * Formatter every time you call it, which is very inefficient. Reusing an
   * existing Formatter more than tripled the speed of makeTimeString(). This
   * Formatter/StringBuilder are also used by makeAlbumSongsLabel()
   */
  /*
   * private static StringBuilder sFormatBuilder = new StringBuilder(); private
   * static Formatter sFormatter = new Formatter(sFormatBuilder,
   * Locale.getDefault()); private static final Object[] sTimeArgs = new
   * Object[5]; public static String makeTimeString(Context context, long secs)
   * { String durationformat = context.getString( secs < 3600 ?
   * R.string.durationformatshort : R.string.durationformatlong); Provide
   * multiple arguments so the format can be changed easily by modifying the
   * xml. sFormatBuilder.setLength(0); final Object[] timeArgs = sTimeArgs;
   * timeArgs[0] = secs / 3600; timeArgs[1] = secs / 60; timeArgs[2] = (secs /
   * 60) % 60; timeArgs[3] = secs; timeArgs[4] = secs % 60; return
   * sFormatter.format(durationformat, timeArgs).toString(); }
   */

  // A really simple BitmapDrawable-like class, that doesn't do
  // scaling, dithering or filtering.
  private static class FastBitmapDrawable extends Drawable {
    private Bitmap mBitmap;

    public FastBitmapDrawable(Bitmap b) {
      mBitmap = b;
    }

    @Override
    public void draw(Canvas canvas) {
      canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
  }

  private static final BitmapFactory.Options   sBitmapOptionsCache = new BitmapFactory.Options();
  private static final BitmapFactory.Options   sBitmapOptions      = new BitmapFactory.Options();
  private static final Uri                     sArtworkUri         = Uri
                                                                       .parse("content://media/external/audio/albumart");
  private static final HashMap<Long, Drawable> sArtCache           = new HashMap<Long, Drawable>();

  static {
    // for the cache,
    // 565 is faster to decode and display
    // and we don't want to dither here because the image will be scaled down
    // later
    sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
    sBitmapOptionsCache.inDither = false;

    sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
    sBitmapOptions.inDither = false;
  }

  public static void clearAlbumArtCache() {
    synchronized (sArtCache) {
      sArtCache.clear();
    }
  }

  public static Drawable getCachedArtwork(Context context, long album_id, BitmapDrawable defaultArtwork) {
    Drawable d = null;
    synchronized (sArtCache) {
      d = sArtCache.get(album_id);
    }
    if (d == null && defaultArtwork != null) {
      d = defaultArtwork;
      final Bitmap icon = defaultArtwork.getBitmap();
      int w = icon.getWidth();
      int h = icon.getHeight();
      Bitmap b = MusicUtils.getArtworkQuick(context, album_id, w, h);
      if (b != null) {
        d = new FastBitmapDrawable(b);
        synchronized (sArtCache) {
          // the cache may have changed since we checked
          Drawable value = sArtCache.get(album_id);
          if (value == null) {
            sArtCache.put(album_id, d);
          } else {
            d = value;
          }
        }
      }
    }
    return d;
  }
  
  public static boolean getCachedFileArt(Context context, long album_id, String song_path, BitmapDrawable defaultArtwork) {
   MediaMetadataRetriever mmr = new MediaMetadataRetriever();
   mmr.setDataSource(song_path);

   byte[] image = mmr.getEmbeddedPicture();
   if (image == null) {
     return false;
   } else {
     Bitmap imageBmp = BitmapFactory.decodeByteArray(image, 0, image.length);
     final Bitmap icon = defaultArtwork.getBitmap();
     int w = icon.getWidth();
     int h = icon.getHeight();
     Bitmap b = Bitmap.createScaledBitmap(imageBmp, w, h, true); 
     FastBitmapDrawable d = new FastBitmapDrawable(b);
     synchronized (sArtCache) {
       sArtCache.put(album_id, d);
     }
     return true;
   }
  }

  // Get album art for specified album. This method will not try to
  // fall back to getting artwork directly from the file, nor will
  // it attempt to repair the database.
  public static Bitmap getArtworkQuick(Context context, long album_id, int w, int h) {
    ContentResolver res = context.getContentResolver();
    Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
    if (uri != null) {
      ParcelFileDescriptor fd = null;
      try {
        fd = res.openFileDescriptor(uri, "r");
        int sampleSize = 1;

        // Compute the closest power-of-two scale factor
        // and pass that to sBitmapOptionsCache.inSampleSize, which will
        // result in faster decoding and better quality
        sBitmapOptionsCache.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);
        int nextWidth = sBitmapOptionsCache.outWidth >> 1;
        int nextHeight = sBitmapOptionsCache.outHeight >> 1;
        while (nextWidth > w && nextHeight > h) {
          sampleSize <<= 1;
          nextWidth >>= 1;
          nextHeight >>= 1;
        }

        sBitmapOptionsCache.inSampleSize = sampleSize;
        sBitmapOptionsCache.inJustDecodeBounds = false;
        Bitmap b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);

        if (b != null) {
          // finally rescale to exactly the size we need
          if (sBitmapOptionsCache.outWidth != w || sBitmapOptionsCache.outHeight != h) {
            Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);
            // Bitmap.createScaledBitmap() can return the same bitmap
            if (tmp != b) b.recycle();
            b = tmp;
          }
        }

        return b;
      } catch (FileNotFoundException e) {
      } finally {
        try {
          if (fd != null) fd.close();
        } catch (IOException e) {
        }
      }
    }
    return null;
  }

  /**
   * Get album art for specified album. You should not pass in the album id for
   * the "unknown" album here (use -1 instead) This method always returns the
   * default album art icon when no album art is found.
   */
  public static Bitmap getArtwork(Context context, long song_id, long album_id) {
    return getArtwork(context, song_id, album_id, true);
  }

  /**
   * Get album art for specified album. You should not pass in the album id for
   * the "unknown" album here (use -1 instead)
   */
  public static Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefault) {

    if (album_id < 0) {
      // This is something that is not in the database, so get the album art
      // directly
      // from the file.
      if (song_id >= 0) {
        Bitmap bm = getArtworkFromFile(context, song_id, -1);
        if (bm != null) { return bm; }
      }
      if (allowdefault) { return getDefaultArtwork(context); }
      return null;
    }

    ContentResolver res = context.getContentResolver();
    Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
    if (uri != null) {
      InputStream in = null;
      try {
        in = res.openInputStream(uri);
        return BitmapFactory.decodeStream(in, null, sBitmapOptions);
      } catch (FileNotFoundException ex) {
        // The album art thumbnail does not actually exist. Maybe the user
        // deleted it, or
        // maybe it never existed to begin with.
        Bitmap bm = getArtworkFromFile(context, song_id, album_id);
        if (bm != null) {
          if (bm.getConfig() == null) {
            bm = bm.copy(Bitmap.Config.RGB_565, false);
            if (bm == null && allowdefault) { return getDefaultArtwork(context); }
          }
        } else if (allowdefault) {
          bm = getDefaultArtwork(context);
        }
        return bm;
      } finally {
        try {
          if (in != null) {
            in.close();
          }
        } catch (IOException ex) {
        }
      }
    }

    return null;
  }

  // get album art for specified file
  private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
    Bitmap bm = null;

    if (albumid < 0 && songid < 0) { throw new IllegalArgumentException("Must specify an album or a song id"); }

    try {
      if (albumid < 0) {
        Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
        if (pfd != null) {
          FileDescriptor fd = pfd.getFileDescriptor();
          bm = BitmapFactory.decodeFileDescriptor(fd);
        }
      } else {
        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
        if (pfd != null) {
          FileDescriptor fd = pfd.getFileDescriptor();
          bm = BitmapFactory.decodeFileDescriptor(fd);
        }
      }
    } catch (IllegalStateException ex) {
    } catch (FileNotFoundException ex) {
    }
    return bm;
  }

  private static Bitmap getDefaultArtwork(Context context) {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
    return BitmapFactory.decodeStream(context.getResources().openRawResource(R.drawable.eighth_notes), null, opts);
  }

  static void setBackground(View v, Bitmap bm) {

    if (bm == null) {
      v.setBackgroundResource(0);
      return;
    }

    int vwidth = v.getWidth();
    int vheight = v.getHeight();
    int bwidth = bm.getWidth();
    int bheight = bm.getHeight();
    float scalex = (float)vwidth / bwidth;
    float scaley = (float)vheight / bheight;
    float scale = Math.max(scalex, scaley) * 1.3f;

    Bitmap.Config config = Bitmap.Config.ARGB_8888;
    Bitmap bg = Bitmap.createBitmap(vwidth, vheight, config);
    Canvas c = new Canvas(bg);
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setFilterBitmap(true);
    ColorMatrix greymatrix = new ColorMatrix();
    greymatrix.setSaturation(0);
    ColorMatrix darkmatrix = new ColorMatrix();
    darkmatrix.setScale(.3f, .3f, .3f, 1.0f);
    greymatrix.postConcat(darkmatrix);
    ColorFilter filter = new ColorMatrixColorFilter(greymatrix);
    paint.setColorFilter(filter);
    Matrix matrix = new Matrix();
    matrix.setTranslate(-bwidth / 2, -bheight / 2); // move bitmap center to
                                                    // origin
    matrix.postRotate(10);
    matrix.postScale(scale, scale);
    matrix.postTranslate(vwidth / 2, vheight / 2); // Move bitmap center to view
                                                   // center
    c.drawBitmap(bm, matrix, paint);
    v.setBackgroundDrawable(new BitmapDrawable(bg));
  }

  static int getCardId(Context context) {
    ContentResolver res = context.getContentResolver();
    Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
    int id = -1;
    if (c != null) {
      c.moveToFirst();
      id = c.getInt(0);
      c.close();
    }
    return id;
  }

  static class LogEntry {
    Object item;
    long   time;

    LogEntry(Object o) {
      item = o;
      time = System.currentTimeMillis();
    }

    void dump(PrintWriter out) {
      sTime.set(time);
      out.print(sTime.toString() + " : ");
      if (item instanceof Exception) {
        ((Exception)item).printStackTrace(out);
      } else {
        out.println(item);
      }
    }
  }

  private static LogEntry[] sMusicLog = new LogEntry[100];
  private static int        sLogPtr   = 0;
  private static Time       sTime     = new Time();

  static void debugLog(Object o) {

    sMusicLog[sLogPtr] = new LogEntry(o);
    sLogPtr++;
    if (sLogPtr >= sMusicLog.length) {
      sLogPtr = 0;
    }
  }

  static void debugDump(PrintWriter out) {
    for (int i = 0; i < sMusicLog.length; i++) {
      int idx = (sLogPtr + i);
      if (idx >= sMusicLog.length) {
        idx -= sMusicLog.length;
      }
      LogEntry entry = sMusicLog[idx];
      if (entry != null) {
        entry.dump(out);
      }
    }
  }
}
