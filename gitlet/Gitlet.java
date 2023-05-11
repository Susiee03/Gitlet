package gitlet;

import static gitlet.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gitlet{
    /** all the gitlet related command will be put in this class, the fields are files and
     * directories constructed in Repository class*/
    static File CWD = Repository.CWD;
    static File GITLET_DIR = Repository.GITLET_DIR;
    File object = Repository.OBJECT_DIR;
    static File blobs = Repository.blobs;
    static File commits = Repository.commits;
    File REFS_DIR = Repository.REFS_DIR;
    static File heads = Repository.heads;
    File remotes = Repository.remotes;
    static File HEAD = Repository.HEAD;
    static File STAGES_FILE = Repository.STAGES_FILE;

    /** the current commit in the current branch*/
    public static Commit currCommit;

    /** the current branch in gitlet*/
    public static String currBranch;


    /** Create the .gitlet directory and all the related structure, with the init argv
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

        //set and save the initial stage
        Stage stage = new Stage();
        stage.save();

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
        String branchPath = join("refs", "heads", branch).getPath();
        Utils.writeContents(HEAD, branchPath);
    }



    /** Adds a copy of the file as it currently exists to the staging area. For this reason, adding
     * a file is also called staging the file for
     * addition. Staging an already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere in .gitlet. If the current
     * working version of the file is identical to the version in the current commit, do not
     * stage it to be added, and remove it from the staging area if it is already there (as
     * can happen when a file is changed, added, and then changed back to it’s original version).
     * The file will no longer be staged for removal (see gitlet rm), if it was at the time of
     * the command.*/

    public static void add(String filename) throws IOException {
        //get the file from working directory
        File fileInWorkingDirectory = Utils.join(CWD, filename);
        if (!fileInWorkingDirectory.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        //create the specific blob object
        Blob addedBlob = new Blob(fileInWorkingDirectory);
        //String addedBlobFileName = addedBlob.getWorkingDirectoryFilePath();
        String addedBlobID = addedBlob.getHash();


        //get the stage object from the file
        Stage stage = readObject(STAGES_FILE, Stage.class);

        //get the current commit object from the file
        currCommit = retrieveCurrentCommit();

        // whether the current commit contains the file with the same name, if contains:
        if (currCommit.getTracked().containsKey(filename)) {//get(addedBlobFileName).equals(addedBlobID)) {

            //when the new blob id is same as the current commit tracked blob id, don't add it.
            //remove it from the staging area if it is already there
            if (currCommit.getTracked().get(filename).equals(addedBlobID)) {
                if (! stage.getAddStage().isEmpty()) {
                    if (stage.getAddStage().get(filename).equals(addedBlobID)) {
                        stage.getAddStage().remove(filename);
                    }
                }

            }

            //new blob id is different from the current commit tracked blob id, put the blob in stage
            //for addition area
            else {
                addedBlob.saveBlob();
                stage.stageForAdditionUpdateBlob(filename, addedBlobID);
            }
        }

        //not contains
        else {
            //when stage for addition contains the blob:
            if (stage.getAddStage().containsKey(filename)) {
                //working directory's blob's content changed, already-staged file overwrites the previous
                // entry in the staging area with the new contents.
                if (!stage.getAddStage().get(filename).equals(addedBlobID)) {
                    addedBlob.saveBlob();
                    stage.stageForAdditionUpdateBlob(filename, addedBlobID);
                }
            }

            //*when the current commit doesn't contain the blob with the same name, meaning the
              //blob is first time created and added.
            else {
                addedBlob.saveBlob();
                stage.stageForAddition(filename, addedBlobID);
            }

        }
        // save stage obj
        stage.save();
    }

    /** return the current commit, from the HEAD FILE get the latest commit sha1. */
    private static Commit retrieveCurrentCommit(){
        String commitPath = readContentsAsString(HEAD); //HEAD points to the latest commit, refs/heads/master
        File latestCommit = Utils.join(GITLET_DIR, commitPath);
        String hash = readContentsAsString(latestCommit);
        return retrieveCommit(hash);
    }

    /** helper method, to retrieve the commit when having the sha1 code from HEAD.
     * Get the commit file from objects/commits */
    private static Commit retrieveCommit(String hash){
        File cPath = join(commits,hash);
        return Utils.readObject(cPath,Commit.class);
    }


    /**Description: Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit. The commit is said to be tracking the saved files. By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; it will keep versions of files exactly as they are, and not update them. A commit will only update the contents of files it is tracking that have been staged for addition at the time of commit, in which case the commit will now include the version of the file that was staged instead of the version it got from its parent. A commit will save and start tracking any files that were staged for addition but weren’t tracked by its parent. Finally, files tracked in the current commit may be untracked in the new commit as a result being staged for removal by the rm command (below).
     The bottom line: By default a commit has the same file contents as its parent. Files staged for addition and removal are the updates to the commit. Of course, the date (and likely the mesage) will also different from the parent.

     The staging area is cleared after a commit.
     The commit command never adds, changes, or removes files in the working directory (other than those in the .gitlet directory). The rm command will remove such files, as well as staging them for removal, so that they will be untracked after a commit.
     Any changes made to files after staging for addition or removal are ignored by the commit command, which only modifies the contents of the .gitlet directory. For example, if you remove a tracked file using the Unix rm command (rather than Gitlet’s command of the same name), it has no effect on the next commit, which will still contain the (now deleted) version of the file.
     After the commit command, the new commit is added as a new node in the commit tree.
     The commit just made becomes the “current commit”, and the head pointer now points to it. The previous head commit is this commit’s parent commit.
     Each commit should contain the date and time it was made.
     Each commit has a log message associated with it that describes the changes to the files in the commit. This is specified by the user. The entire message should take up only one entry in the array args that is passed to main.
     To include multiword messages, you’ll have to surround them in quotes.
     Each commit is identified by its SHA-1 id, which must include the file (blob) references of its files, parent reference, log message, and commit time.*/
    public static void commit(String message) throws IOException {
        //first, check whether the commit is valid, meaning whether it is in stage for addition
        Stage stage =Utils.readObject(STAGES_FILE, Stage.class);
        if (stage.getAddStage().isEmpty() && stage.getRemoveStage().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        //then generate a new commit, parent is the previous commit, and clone the previous commit, for the first time commit
        Commit parentCommit= retrieveCurrentCommit();
        String parent = parentCommit.getSha1();
        currCommit = new Commit(message, parent);

        //New commit tracked the saved files. By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; it will keep versions of files exactly as they are, and not update them.
        currCommit.setTracked(parentCommit.getTracked());

        //update the contents of files it is tracking that have been staged for addition at the time of commit,
        //save and start tracking any files that were staged for addition but weren’t tracked by its parent.
        updateStageToCommit(currCommit);

        // files tracked in the current commit may be untracked in the new commit as a result being
        // staged for removal by the rm command
        updateStageForRemovalToCommit(currCommit);


        //submit the commit, and master points to the new commit
        currBranch = readCurrBranch();
        submitCommit(currCommit, currBranch);

        //stage area should be cleared
        stage.clear();

        // save stage obj
        stage.save();

    }


    /** get the current branch*/
    private static String readCurrBranch() {
        return readContentsAsString(HEAD);
    }


    /** update and start tracking any files that were staged for addition but
     * weren’t tracked by its parent commit.*/
    private static void updateStageToCommit(Commit newCommit) {
        Stage currStage = Utils.readObject(STAGES_FILE, Stage.class);
        Map<String,String> addStageArea = currStage.getAddStage();
        for (String key: addStageArea.keySet()) {
            newCommit.addTracked(key, addStageArea.get(key));
        }
    }

    /** untracked the file in stage for removal area, if user first uses rm then commit */
    private static void updateStageForRemovalToCommit(Commit newCommit) {
        Stage currStage = Utils.readObject(STAGES_FILE, Stage.class);
        for (String filePath: currStage.getRemoveStage()) {
            newCommit.untracked(filePath);
        }
    }

    /** submit the commit into the current branch, add the commit under the object folder,
     * and make the */
    private static void submitCommit(Commit commit, String branch) throws IOException {
        String commitSha1 = commit.getSha1();
        File currentCommit = Utils.join(commits, commitSha1); // name a file of commit first
        currentCommit.createNewFile(); //create a new file under object folder
        Utils.writeObject(currentCommit, commit); //write the commit in the file
        commit.save();

        // update the refs/heads/branch to current commit, it writes the latest commit sha1
        File headsFile = join(GITLET_DIR, branch);
        headsFile.createNewFile();
        Utils.writeContents(headsFile, commitSha1);
    }


    /** Unstage the file if it is currently staged for addition. If the file is tracked in the
     * current commit, stage it for removal and remove the file from the working directory if the
     * user has not already done so (do not remove it unless it is tracked in the current commit).
     * Used for remove the file from working directory and stages at the same time,
     * the file content in working directory must be same as file in stage for addition*/
    public static void rm(String filename) throws IOException {
        Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
        File fileInWorkingDirectory = Utils.join(CWD, filename);
        currCommit = retrieveCurrentCommit();

        //unstage the file if it is currently staged for addition.
        if (stage.getAddStage().containsKey(fileInWorkingDirectory.getPath())) {
            stage.remove(fileInWorkingDirectory.getPath());
        }

        //stage it for removal if the file is tracked in current commit, and also exist in CWD.
        //Note: it needs to be committed to remove the file in repository.
        else if (fileInWorkingDirectory.exists() &&
            currCommit.getTracked().containsKey(fileInWorkingDirectory.getPath())) {
            stage.stageForRemoval(fileInWorkingDirectory.getPath());

            //remove the file from the working directory
            Utils.restrictedDelete(fileInWorkingDirectory);
        }


        //stage it for removal if the file is tracked in current commit, but not exist in CWD
        else if (!fileInWorkingDirectory.exists() &&
            currCommit.getTracked().containsKey(fileInWorkingDirectory.getPath())) {
            stage.stageForRemoval(fileInWorkingDirectory.getPath());
        }

        //fail: file is neither staged nor tracked by the head commit
        else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        //save the stage, after the rm command
        stage.save();
    }



    /** Starting at the current head commit, display information about each commit backwards along
     * the commit tree until the initial commit, following the first parent commit links, ignoring
     * any second parents found in merge commits.*/
    public static void log() {
        System.out.println("===");
        currCommit = retrieveCurrentCommit();
        Commit commitIterator = currCommit;

        while(!commitIterator.getParent().equals("")) {
            logPrint(commitIterator);

            commitIterator = retrieveCommit(commitIterator.getParent());
        }
        System.out.println("commit "  + commitIterator.getSha1());
        System.out.println("Date: " + commitIterator.getTimestamp());
        System.out.println(commitIterator.getMessage() + "\n");

        //TODO: doesn't consider the merge case, will figure it out later. The info is different.
    }


    /** log print information helper function*/
    private static void logPrint(Commit commit) {
        System.out.println("commit "  + commit.getSha1());
        System.out.println("Date: " + commit.getTimestamp());
        System.out.println(commit.getMessage() + "\n");
        System.out.println("===");
    }

    /** Like log, except displays information about all commits ever made. The order of the commits
     * does not matter.*/
    public static void global_log() {
        List<String> commitRecord = Utils.plainFilenamesIn(commits);
        for (String sha1: commitRecord) {
            Commit currCommit = retrieveCommit(sha1);
            logPrint(currCommit);
        }
        //TODO: 没有考虑merge的情况。will modify this part later
    }

    /** Prints out the ids of all commits that have the given commit message, one per line. If there
     * are multiple such commits, it prints the ids out on separate lines.
     * Doesn't exist in real git command. */
    public static void find(String commitMessage) {
        List<String> commitRecord = Utils.plainFilenamesIn(commits);
        List<String> ids = new ArrayList<>();
        for (String sha1: commitRecord) {
            Commit currCommit = retrieveCommit(sha1);
            if (currCommit.getMessage().equals(commitMessage)) {
                ids.add(currCommit.getSha1());
            }
        }

        if (ids.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        else {
            for (String id: ids) {
                System.out.println(id);
            }
        }
    }


    /**  Displays what branches currently exist, and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal. */
    public static void status() {
        branchStatus();
        System.out.println();
        stageStatus();
        //TODO: extra credit, will do it later
        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println("=== Untracked Files ===");

    }

    /** status helper function, prints the branch status*/
    private static void branchStatus() {
        System.out.println("=== Branches ===");
        currBranch = readCurrBranch();
        String cBranch = currBranch.substring(currBranch.lastIndexOf("/") +1);
        System.out.println("*" + cBranch);

        //other branches, all branches are stored in refs/heads
        List<String> allBranches = Utils.plainFilenamesIn(heads);
        if (allBranches.size() > 1) {
            for (String branch: allBranches) {
                if (!branch.equals(cBranch)) {
                    System.out.println(branch);
                }
            }
        }
    }

    /** status helper function, prints the stage status*/
    private static void stageStatus() {
        Stage currStage = Utils.readObject(STAGES_FILE, Stage.class);

        System.out.println("=== Staged Files ==="); //stage for addition
        Map<String, String> addStage = currStage.getAddStage();
        if (!addStage.isEmpty()) {
            List<String> sortedFile = new ArrayList<>(addStage.keySet());
            Collections.sort(sortedFile);
            for (String file : sortedFile) {
                System.out.println(file);
            }
        }

        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> removedStage = currStage.getRemoveStage();
        if (!removedStage.isEmpty()) {
            Collections.sort(removedStage);
            for (String file: removedStage) {
                System.out.println(file);
            }
        }
    }


    /** First use case of checkout. Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version of the file that’s already
     * there if there is one. The new version of the file is not staged. */

    /* case 1*/
    public static void checkout(String filename) throws IOException {
        currCommit = retrieveCurrentCommit();

        if (currCommit.getTracked().containsKey(filename)) {
            String blobSha1 = currCommit.getTracked().get(filename);
            File originInBlob = Utils.join(blobs, blobSha1);
            Blob blob = Utils.readObject(originInBlob, Blob.class); 
            byte[] contents = blob.getContents();

            File fileInCWD = Utils.join(CWD, filename);
            if (!fileInCWD.exists()) {
                fileInCWD.createNewFile();
                Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));

            }
            else {
                Utils.writeContents(fileInCWD,new String(contents, StandardCharsets.UTF_8));
            }
        }

        else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Takes the version of the file as it exists in the commit with the given id, and puts it in
     * the working directory, overwriting the version of the file that’s already there if there is
     * one. The new version of the file is not staged.*/
    /* case 2*/
    public static void checkout(String commitID, String filename) {

    }


}


