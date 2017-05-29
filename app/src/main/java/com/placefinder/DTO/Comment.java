package com.placefinder.DTO;



public class Comment {

    private long id;
    private String ownerGoogleId;
    private String authorName;
    private String text;

    public Comment(){

    }

    public Comment(String commentText, String commentAuthorName){
        text = commentText;
        authorName = commentAuthorName;
    }

    public String getOwnerGoogleId() {
        return ownerGoogleId;
    }

    public void setOwnerGoogleId(String ownerGoogleId) {
        this.ownerGoogleId = ownerGoogleId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
