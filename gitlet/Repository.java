package gitlet;

import java.io.File;
import java.io.IOException;
import jdk.jfr.consumer.RecordingFile;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * .gitlet/
     *  - objects/
     *    - blob/
     *    - commits/
     *  - refs            the newest commit in all branches
     *    - heads            a ref that points to the tip (latest commit) of a branch.
     *      - master/main
     *    - remotes
     *  - HEAD               the currently checked-out branch's latest commit in Gitlet/ a commit currently checked out in the working directory, a specific git ref./checkout command will move HEAD to a specific commit.
     *  - stages/index
     *    */
    public static final File OBJECT_DIR = join (GITLET_DIR, "objects");
    public static final File blobs = join(OBJECT_DIR, "blobs");
    public static final File commits = join(OBJECT_DIR, "commits");
    public static final File REFS_DIR = join (GITLET_DIR, "refs");
    public static final File heads = join(REFS_DIR, "heads");
    public static final File remotes = join(REFS_DIR, "remotes");
    public static final File HEAD_FILE= join(GITLET_DIR, "HEAD");
    public static final File STAGES_FILE = join(GITLET_DIR, "stages");

    /* TODO: fill in the rest of this class. */

    /**set up the gitlet structure, based on design and real git structure*/
    public static void setUp() throws IOException{
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            OBJECT_DIR.mkdir();
            blobs.mkdir();
            commits.mkdir();
            REFS_DIR.mkdir();
            heads.mkdir();
            remotes.mkdir();
            HEAD_FILE.createNewFile();
            STAGES_FILE.createNewFile();

        }
    }

}
