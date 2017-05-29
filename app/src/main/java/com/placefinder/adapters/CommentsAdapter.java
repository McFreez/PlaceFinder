package com.placefinder.adapters;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.placefinder.DTO.Comment;
import com.placefinder.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> implements View.OnClickListener {


    private List<Comment> mComments;

    public CommentsAdapter(List<Comment> comments){
        mComments = comments;
    }

    @Override
    public void onClick(View view) {
        int position = (int) view.getTag();
        mComments.remove(position);
        notifyDataSetChanged();
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

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        int layoutIdForListen = R.layout.comments_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListen, parent, false);
        CommentViewHolder viewHolder = new CommentViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CommentViewHolder holder, int position) {
        holder.bind(mComments.get(position).getAuthorName(), mComments.get(position).getText());
        holder.deleteComment.setTag(position);
        holder.deleteComment.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }
}
