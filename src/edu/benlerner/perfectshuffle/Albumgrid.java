package edu.benlerner.perfectshuffle;

import edu.benlerner.perfectshuffle.LibraryCache.AlbumInfo;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Albumgrid extends Fragment {

  private GridView     mGridView;
  private LibraryCache cache;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.albumgrid, container, false);
    this.mGridView = (GridView)view.findViewById(R.id.grid_view);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    this.cache = new LibraryCache();
    this.updateAlbumList();
  }

  protected void updateAlbumList() {
    // Setup our onItemClickListener to emulate the onListItemClick() method of
    // ListFragment.
    LibraryCache.CacheStructure cache = this.cache.GetAlbumCache(this.getActivity());
    this.mGridView.setAdapter(new AlbumgridViewAdapter(this.getActivity(), R.layout.albumgrid_item, cache));
    mGridView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onGridItemClick((GridView)parent, view, position, id);
      }
    });
  }

  public void onGridItemClick(GridView g, View v, int position, long id) {
    Activity activity = getActivity();

    if (activity != null) {
      AlbumgridItemHolder item = (AlbumgridItemHolder)this.mGridView.getAdapter().getItem(position);

      // Display a simple Toast to demonstrate that the click event is working.
      // Notice that Fragments have a
      // getString() method just like an Activity, so that you can quickly
      // access your localized Strings.
      Toast.makeText(activity, "Clicked on " + item.txtAlbumName.getText(), Toast.LENGTH_SHORT).show();
    }
  }

  class AlbumgridViewAdapter extends BaseAdapter {
    Context                  context          = null;
    int                      layoutResourceId = 0;
    LibraryCache.AlbumInfo[] albums           = null;

    public AlbumgridViewAdapter(Context context, int layoutResourceId, LibraryCache.CacheStructure cache) {
      this.context = context;
      this.layoutResourceId = layoutResourceId;
      if (cache == null) {
        this.albums = new AlbumInfo[0];
      } else {
        this.albums = new AlbumInfo[cache.allAlbums.size()];
        String[] albumNames = new String[this.albums.length];
        albumNames = cache.getAlbums().toArray(albumNames);
        java.util.Arrays.sort(albumNames);
        for (int i = 0; i < albumNames.length; i++)
          this.albums[i] = cache.allAlbums.get(albumNames[i]);
      }
    }
    public int getCount() {
      return this.albums.length;
    }
    public Object getItem(int position) {
      if (this.albums != null && position >= 0 && position < getCount()) {
        return this.albums[position];
      }
      return null;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
      View item = convertView;
      AlbumgridItemHolder holder = null;
      if (item == null) {
        LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();
        item = inflater.inflate(this.layoutResourceId, parent, false);

        holder = new AlbumgridItemHolder();
        holder.thumbnail = (ImageView) item.findViewById(R.id.albumgridItemThumbnail);
        holder.txtAlbumName= (TextView) item.findViewById(R.id.albumgridItemName);
        holder.txtNumTracks = (TextView) item.findViewById(R.id.albumgridItemNumTracks);

        item.setTag(holder);
      } else {
        holder = (AlbumgridItemHolder) item.getTag();
      }

      AlbumInfo info = this.albums[position];
      holder.txtAlbumName.setText(info.albumName);
      holder.txtNumTracks.setText(info.numTracks + " tracks");
      if (info.thumbnail == null) {
        holder.thumbnail.setImageResource(android.R.drawable.gallery_thumb);
      } else {
        Bitmap imageBmp = BitmapFactory.decodeByteArray(info.thumbnail, 0, info.thumbnail.length);
        holder.thumbnail.setImageBitmap(imageBmp);
      }
      return item;
    }
    public long getItemId(int position) {
      if (this.albums != null && position >= 0 && position < getCount()) {
        return this.albums[position].albumName.hashCode();
      }
      return 0;
    }
  }
  
  static class AlbumgridItemHolder {
    ImageView thumbnail;
    TextView txtAlbumName;
    TextView txtNumTracks;
  }
}
