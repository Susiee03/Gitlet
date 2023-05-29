package gitlet;

// TODO: any imports you need here

import static gitlet.Repository.CWD;
import static gitlet.Repository.OBJECT_DIR;

import static gitlet.Repository.blobs;
import static gitlet.Utils.join;
import static gitlet.Utils.readObject;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Shuyao YU
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The timestamp for this Commit. */
    private String timestamp;
    /** the tracked parent  */
    private String parent; //the file name where the commit object can be found, in this case is the sha1 of the parent commit

    /** the second parent, used in merge. */
    private String second_parent;

    /**the SHA1 for each commit object*/
    private String sha1;

    /**the commit file actually stored under the object file*/
    private File commitFileName;

    /** the commit object tracked blob files, key is the file name, value is the blob sha1*/
    private Map<String,String> tracked = new HashMap<>();


    /** Initial Commit, commit at the first time*/
    public Commit(){
        message = "initial commit";
        timestamp = dateToTimeStamp(new Date(0));
        parent = "";
        sha1 = generateSHA1();
        commitFileName = Utils.join(Repository.commits, sha1);
    }

    /** Commit command after the initial commit*/
    public Commit(String message, String parent, String second_parent) {
        timestamp = dateToTimeStamp(new Date());
        this.message = message;
        this.parent = parent;
        this.second_parent = second_parent;
        sha1 = generateSHA1();
    }

    /** transfer the time stamp to the required date format, for passing the test*/
    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
    /**
     * Saves the commit to a file for future use.
     */
    public void save() {
        File currCo = Utils.join(Repository.commits, sha1);
        Utils.writeObject(currCo, this);
    }
    /** Generate the commit sha1 hashcode*/
    private String generateSHA1() {
        return Utils.sha1(message,timestamp,parent);
    }

    /** get the commit sha1 */
    public String getSha1() {
        return sha1;
    }


    /** get the current commit's tracked blobs*/
    public Map<String,String> getTracked() {
        return tracked;
    }


    /** set the tracked blobs file from the parent commit*/
    public void setTracked (Map<String,String> parentTracked){
        tracked = parentTracked;
    }

    /** add the tracked blob in the stage for addtion part, blobFileName is usually created by user*/
    public void addTracked(String blobFileName, String blobSha1) {
        tracked.put(blobFileName, blobSha1);
    }


    /** untracked the blob in current commit, when got the rm command*/
    public void untracked(String blobFileName) {
        tracked.remove(blobFileName);
    }

    /** Get the parent of commit.*/
    public String getParent(){
        return parent;
    }

    /** Get the second parent of commit, used in merge. */
    public String getSecond_parent() {
        return second_parent;
    }

    /** Get the time stamp in string. */
    public String getTimestamp() {
        return timestamp;
    }

    /** get the commit message*/
    public String getMessage () {
        return message;
    }




}
