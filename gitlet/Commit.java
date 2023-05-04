package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.Date;

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
    private Date timestamp;
    /** the tracked parent  */
    private String parent; //the file name where the commit object can be found, in this case is the sha1 of the parent commit

    /**the SHA1 for each commit object*/
    private String sha1;

    /**the commit file actually stored under the object file*/
    private File commitFileName;

    /** the add file's actual name, a string that is actually stored in commit object*/


    /* TODO: fill in the rest of this class. */

    public Commit(){
        message = "initial commit";
        timestamp = new Date(0);
        parent = null;
        sha1 = generateSHA1();
        commitFileName = Utils.join(Repository.OBJECT_DIR, sha1);
    }


    /**
     * Saves the commit to a file for future use.
     */
    public void save() {
        File currCo = Utils.join(Repository.commits, sha1);
        Utils.writeObject(currCo, this);
    }

    private String generateSHA1() {
        return Utils.sha1(Utils.serialize(this));
    }

    public String getSha1() {
        return sha1;
    }

    // need a method to get the working directory's file name, stored as string in commit object
    public String getWorkingDirectoryFileName() {
        return null;
    }
}
