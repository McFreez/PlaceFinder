package com.placefinder.adapters;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.placefinder.DTO.Comment;
import com.placefinder.DTO.Place;
import com.placefinder.MapActivity;
import com.placefinder.R;
import com.placefinder.network.ServerRequests;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> implements View.OnClickListener {


    private FirebaseUser mCurrentUser;
    private MapActivity mContext;
    private Place mSelectedPlace;
    private EditText mCommentTextEditText;
    private ImageView mButtonCreateComment;
    private LinearLayout mCommentsReplacer;
    private StorageReference mUserImagesRef;
    private List<Comment> mComments;

    public CommentsAdapter(List<Comment> comments, MapActivity context, FirebaseUser currentUser, Place selectedPlace, EditText commentText, ImageView buttonCreateComment, StorageReference userImagesRef, LinearLayout commentsReplacer){
        mComments = comments;
        mContext = context;
        mCurrentUser = currentUser;
        mSelectedPlace = selectedPlace;
        mCommentTextEditText = commentText;
        mButtonCreateComment = buttonCreateComment;
        mUserImagesRef = userImagesRef;
        mCommentsReplacer = commentsReplacer;

        mButtonCreateComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideKeyboard();

                if(mCommentTextEditText.getText().toString().equals("")){
                    return;
                }

                Comment comment = new Comment();
                comment.setOwnerGoogleId(mCurrentUser.getUid());
                comment.setAuthorName(mCurrentUser.getDisplayName());
                comment.setText(mCommentTextEditText.getText().toString());

                mCommentTextEditText.setText("");

                new ServerRequests.PostCommentTask(CommentsAdapter.this, mSelectedPlace).execute(comment);
            }
        });

        if(mComments.size() == 0)
            showCommentsReplacer();
        else
            hideCommentsReplacer();
    }

    private void showCommentsReplacer(){
        mCommentsReplacer.setVisibility(View.VISIBLE);
    }

    private void hideCommentsReplacer(){
        mCommentsReplacer.setVisibility(View.GONE);
    }

    private void hideKeyboard(){
        View view = mContext.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View view) {
        int position = (int) view.getTag();
        new ServerRequests.DeleteCommentTask(CommentsAdapter.this, position).execute(mComments.get(position).getId());
    }

    public void removingImageSuccess(int position){
        mComments.remove(position);
        notifyDataSetChanged();

        if(mComments.size() == 0)
            showCommentsReplacer();
        else
            hideCommentsReplacer();
    }

    public void removingImageError(){
        Toast.makeText(mContext, "Failed to remove comment", Toast.LENGTH_SHORT).show();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder{
        CircleImageView authorImage;
        TextView authorName;
        TextView text;
        ImageButton deleteComment;

        public CommentViewHolder(View itemView) {
            super(itemView);

            authorImage = (CircleImageView) itemView.findViewById(R.id.comment_user_image);
            authorName = (TextView) itemView.findViewById(R.id.comment_user_name);
            text = (TextView) itemView.findViewById(R.id.comment_text);
            deleteComment = (ImageButton) itemView.findViewById(R.id.comment_delete_button);
        }

        void bind(String authorNameString, String textString){
            authorName.setText(authorNameString);
            text.setText(textString);
        }
    }

    public void updateDataSource(Place selectedPlace, boolean isJustCreatedPlace){
        mSelectedPlace = selectedPlace;
        clearData();
        if(!isJustCreatedPlace){
            new ServerRequests.GetAllPlaceComments(this, mSelectedPlace).execute(mSelectedPlace.getId());
        }
    }

    public void clearData(){
        mComments.clear();
        if(mComments.size() == 0)
            showCommentsReplacer();
        else
            hideCommentsReplacer();
        notifyDataSetChanged();
    }

    public void onSavingCommentFinished(ResponseEntity<Comment> commentResponseEntity){
        if(commentResponseEntity.getStatusCode() == HttpStatus.CREATED){
            mComments.add(commentResponseEntity.getBody());
            notifyDataSetChanged();
            if(mComments.size() > 0)
                hideCommentsReplacer();
        }else
        {
            Toast.makeText(mContext, "Cannot add comment", Toast.LENGTH_SHORT).show();
        }
    }

    public void onGettingAllPlacesCommentsFinished(ResponseEntity<List<Comment>> listResponseEntity){
        if(listResponseEntity.getStatusCode() == HttpStatus.OK){
            mComments.clear();
            mComments.addAll(listResponseEntity.getBody());
            notifyDataSetChanged();
        }

        if(mComments.size() == 0)
            showCommentsReplacer();
        else
            hideCommentsReplacer();
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        hideCommentsReplacer();

        Context context = parent.getContext();
        int layoutIdForListen = R.layout.comments_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListen, parent, false);
        CommentViewHolder viewHolder = new CommentViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final CommentViewHolder holder, int position) {
        holder.bind(mComments.get(position).getAuthorName(), mComments.get(position).getText());

        final long ONE_MEGABYTE = 1024 * 1024;
        mUserImagesRef.child(mComments.get(position).getOwnerGoogleId()).getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.authorImage.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                //Toast.makeText(mContext, "Failed to download image from storage: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        if(mComments.get(position).getOwnerGoogleId().equals(mCurrentUser.getUid())) {
            holder.deleteComment.setVisibility(View.VISIBLE);
            holder.deleteComment.setTag(position);
            holder.deleteComment.setOnClickListener(this);
        }
        else
            holder.deleteComment.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }
}
