package com.placefinder.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.placefinder.DTO.Photo;
import com.placefinder.DTO.Place;
import com.placefinder.MapActivity;
import com.placefinder.R;
import com.placefinder.network.ServerRequests;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.List;

public class ItemPagerAdapter extends android.support.v4.view.PagerAdapter {

    private static final String TAG = "ItemPagerAdapter";
    private StorageReference mImageRef;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private List<Bitmap> mBitmapItems;
    private List<Photo> mPhotos;
    private Place mSelectedPlace;
    private String mCurrentUserUid;

    public ItemPagerAdapter(Context context, List<Bitmap> items, List<Photo> photos, StorageReference imageRef, String uId) {
        this.mContext = context;
        this.mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mBitmapItems = items;
        this.mPhotos = photos;
        this.mImageRef = imageRef;
        this.mCurrentUserUid = uId;
    }

    public void updateDataSource(Place selectedPlace, boolean isJustCreatedPlace){
        mSelectedPlace = selectedPlace;
        clearData();
        if(!isJustCreatedPlace)
            new ServerRequests.GetAllPlaceImages(this).execute(mSelectedPlace.getId());
    }

    public void clearData(){
        mBitmapItems.clear();
        mPhotos.clear();
        notifyDataSetChanged();
    }

    public void deleteAllImages(){
        for (Photo p : mPhotos){
            mImageRef.child(String.valueOf(mSelectedPlace.getId())).child(p.getFilename()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //Toast.makeText(mContext, "Cannot delete place image from storage", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addImage(Bitmap image, Photo photo){
        mPhotos.add(photo);
        mBitmapItems.add(image);
        notifyDataSetChanged();
    }

    public void onImageAddingFinished(final Uri imageUri, final InputStream isDisplay, final ResponseEntity<Photo> photo, Place selectedPlace){
        if(photo.getStatusCode() == HttpStatus.CREATED) {
            UploadTask uploadTask = mImageRef.child(String.valueOf(selectedPlace.getId())).child(photo.getBody().getFilename()).putFile(imageUri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e(TAG, "Error uploading image from phone storage by uri : " + imageUri);
                    Toast.makeText(mContext, "Cannot add image", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    Bitmap image = BitmapFactory.decodeStream(isDisplay);
                    addImage(image, photo.getBody());
                }
            });
        }
        else
            Toast.makeText(mContext, "Cannot add image", Toast.LENGTH_SHORT).show();
    }

    public void onGettingAllPlaceImagesFinished(ResponseEntity<List<Photo>> listResponseEntity){
        if(listResponseEntity.getStatusCode() == HttpStatus.OK){
            for(Photo p : listResponseEntity.getBody()){
                getPhotoFromCloudStorage(p);
            }
        }
    }

    private void getPhotoFromCloudStorage(final Photo p) {
        final long ONE_MEGABYTE = 1024 * 1024;
        mImageRef.child(String.valueOf(mSelectedPlace.getId())).child(p.getFilename()).getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mPhotos.add(p);
                mBitmapItems.add(bitmap);
                notifyDataSetChanged();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                //Toast.makeText(mContext, "Failed to download image from storage: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestRemoveImage(int position){
        new ServerRequests.DeletePhotoTask(this, position).execute(mPhotos.get(position).getId());
    }

    public void removingImageError(){
        Toast.makeText(mContext, "Failed to remove image", Toast.LENGTH_SHORT).show();
    }

    public void removingImageSuccess(final int position){
        mImageRef.child(String.valueOf(mSelectedPlace.getId())).child(mPhotos.get(position).getFilename()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                removeImage(position);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                removingImageError();
            }
        });
    }

    private void removeImage(int position){
        mPhotos.remove(position);
        mBitmapItems.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mBitmapItems.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.imageView);
        imageView.setImageBitmap(mBitmapItems.get(position));
        imageView.setTag(position);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final int position = (int) view.getTag();
                if(!mCurrentUserUid.equals(mPhotos.get(position).getOwnerGoogleId())){
                    //Toast.makeText(mContext, "Sorry, you can`t delete this photo.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.AlertDialogCustom));
                builder.setMessage("Delete photo?")
                        .setCancelable(true)
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.cancel();
                                        requestRemoveImage(position);
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });
        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
}
