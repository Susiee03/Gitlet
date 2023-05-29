package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stage implements Serializable {
    /** A stage area for commit command, it has add stage area and remove stage area.*/
    private Map<String, String> addStage = new HashMap<>();

    /** The stage area for remove, will figure out the data structure later.*/
    private List<String> removeStage = new ArrayList<>();


    /** Add the blob into stage for addition area, key is the blob's filename, usually same
     * as the file name in CWD, value is blob's sha1 hash code. */
    public void stageForAddition(String file, String blobID) {
        addStage.put(file, blobID);
    }


    /** Put the blob into stage for removal area, put blob's filename, usually same as blob
     *  in working directory. */
    public void stageForRemoval(String filename) {
        removeStage.add(filename);
    }


    /** The blob with the same name, but change the content, so when stage for addition,
     * add stage part need to be update blob's info. */
    public void stageForAdditionUpdateBlob(String file, String blobID) {
        addStage.put(file, blobID);
    }

    /** Get the map in add stage, as usual, the key is blob's filename, value is the blob's sha1 code. */
    public Map<String, String> getAddStage() {
        return addStage;
    }

    /** Get the remove stage blob file name in date for removal area. */
    public List<String> getRemoveStage() {
        return removeStage;
    }

    /** After the commit, the stage for addition area should be cleared. */
    public void clear() {
        addStage.clear();
        removeStage.clear();
    }

    /** Remove the file in stage for addition, for rm command, filename same
     * as the file name in CWD. */
    public void remove(String filename) {
        addStage.remove(filename);
    }

    /** Save the stage. */
    public void save() {
        Utils.writeObject(Repository.STAGES_FILE, this);
    }

}
