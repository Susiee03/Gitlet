package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class Stage implements Serializable {
    /** a stage area after commit command, for add the file name and blob*/
    private Map<String, String> addStage;

    /** the stage area for remove, will figure out the data structure later*/
    //private


    /** add the blob into stage for addition area*/
    public void stageForAddition() {

    }
}
