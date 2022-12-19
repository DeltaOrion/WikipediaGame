package me.jacob.proj.model;

public class UpdateStatus {

    private boolean updateTitle;
    private String oldTitle;
    private boolean updateDescription;
    private boolean updateLinks;


    public boolean isUpdateTitle() {
        return updateTitle;
    }

    public void setUpdateTitle(boolean updateTitle) {
        this.updateTitle = updateTitle;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public void setOldTitle(String oldTitle) {
        this.oldTitle = oldTitle;
    }

    public boolean isUpdateDescription() {
        return updateDescription;
    }

    public void setUpdateDescription(boolean updateDescription) {
        this.updateDescription = updateDescription;
    }

    public boolean isUpdateLinks() {
        return updateLinks;
    }

    public void setUpdateLinks(boolean updateLinks) {
        this.updateLinks = updateLinks;
    }

    public boolean updateOccurred() {
        return updateLinks || updateTitle || updateDescription;
    }
}
