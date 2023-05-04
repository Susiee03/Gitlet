package gitlet;

import static gitlet.Utils.*;
import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable{

    private String hash; //blobID
    private byte[] fileContents; //contents stored in blob
    private File fileName;  //the file in working directory
    private final String WorkingDirectoryFilePath; //file path in working directory, created by user

    private File blobFileName;   // the blob file stored under object directory

    public Blob(File filename) {
        fileName = filename;
        hash = generateHash();
        fileContents = Utils.readContents(fileName);
        WorkingDirectoryFilePath = fileName.getPath();
        blobFileName = Utils.join(Repository.OBJECT_DIR, hash);

    }

    /**generate blob's sha1 code*/
    private String generateHash() {
        return Utils.sha1(serialize(this));
    }

    /**get the blob's sha1 code*/
    public String getHash() {
        return hash;
    }


    /**Save the blob for future use*/
    public void saveBlob(){
        File currBlob = blobFileName;
        writeObject(currBlob, serialize(this));
    }

}
