package gitlet;

import static gitlet.Utils.join;

import java.io.File;
import java.io.IOException;

public class Gitlet{
    /** all the gitlet related command will be put in this class, the fields are files and
     * directories constructed in Repository class*/
    static File CWD = Repository.CWD;
    static File GITLET_DIR = Repository.GITLET_DIR;
    File object = Repository.OBJECT_DIR;
    File blobs = Repository.blobs;
    File commits = Repository.commits;
    File REFS_DIR = Repository.REFS_DIR;
    static File heads = Repository.heads;
    File remotes = Repository.remotes;
    static File HEAD_FILE = Repository.HEAD_FILE;
    static File STAGES_FILE = Repository.STAGES_FILE;

    /** the current commit in the current branch*/
    public static Commit currCommit;

    /** the current branch in gitlet*/
    public static String currBranch;

    /**create the .gitlet directory and all the related structure, with the init argv
     * Creates a new Gitlet version-control system in the current directory. This system will
     * automatically start with one commit: a commit that contains no files and has the commit
     * message initial commit (just like that, with no punctuation). It will have a single branch:
     * master, which initially points to this initial commit, and master will be the current branch.
     * The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970 in
     * whatever format you choose for dates (this is called “The (Unix) Epoch”, represented
     * internally by the time 0.) Since the initial commit in all repositories created by Gitlet
     * will have exactly the same content, it follows that all repositories will automatically
     * share this commit (they will all have the same UID) and all commits in all repositories
     * will trace back to it.*/
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        Repository.setUp();
        initialCommit();
        currBranch = "master";
        //HEAD  points to initialCommit.
        setHEAD(currBranch);
        //heads contains the newest commit in all branches, now only have a master branch
        Utils.writeContents(join(heads, currBranch), currCommit.getSha1());
        //set the stage
        Stage stage = new Stage();
        Utils.writeObject(STAGES_FILE, stage); ///
    }
    /** Helper method for init command, now the current commit is the initial commit, and save
     * the initial commit in Commit directory. */
    private static void initialCommit() {
        Commit initialCommit = new Commit();
        currCommit = initialCommit;
        currCommit.save(); //for future persistent
    }

    /**set HEAD points to the specific branch's latest commit*/
    private static void setHEAD(String branch) {
        String branchPath = join(heads, branch).getPath();
        Utils.writeContents(HEAD_FILE, branchPath);
    }

    /** Adds a copy of the file as it currently exists to the staging area (see the description of
     * the commit command). For this reason, adding a file is also called staging the file for
     * addition. Staging an already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere in .gitlet. If the current
     * working version of the file is identical to the version in the current commit, do not
     * stage it to be added, and remove it from the staging area if it is already there (as
     * can happen when a file is changed, added, and then changed back to it’s original version).
     * The file will no longer be staged for removal (see gitlet rm), if it was at the time of
     * the command.*/
    public static void add(String filename) {
        //add the file from working directory, first create a blob object, then add stage has this
        //blob object
        File fileInWorkingDirectory = Utils.join(CWD, filename);
        Blob addedBlob = new Blob(fileInWorkingDirectory);
    }
}
