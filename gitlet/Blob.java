package gitlet;

import static gitlet.Utils.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class Blob implements Serializable{

    private String hash; //blobID
    private byte[] fileContents; //contents stored in blob
    private File filename;  //the file in working directory
    private final String WorkingDirectoryFilePath; //file path in working directory, created by user

    private File blobFileName;   // the blob file stored under object directory

    public Blob(File filename) {
        this.filename = filename;
        fileContents = Utils.readContents(filename);
        hash = generateHash();
        WorkingDirectoryFilePath = filename.getPath();
        blobFileName = Utils.join(Repository.blobs, hash);

    }

    /**generate blob's sha1 code*/
    private String generateHash() {
        return Utils.sha1(filename.getPath(), fileContents);
    }

    /**get the blob's sha1 code*/
    public String getHash() {
        return hash;
    }

    /** get the blob's contents*/
    public byte[] getContents() {
        return fileContents;
    }


    /**Save the blob for future use*/
    public void saveBlob() throws IOException {
        File currBlob = blobFileName;
        currBlob.createNewFile();
        writeObject(currBlob, this);
    }




}


