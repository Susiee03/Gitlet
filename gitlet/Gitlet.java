package gitlet;

import static gitlet.Utils.*;
import static java.lang.Integer.MAX_VALUE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Gitlet {
  /**
   * All the gitlet related command will be put in this class, the fields are files and directories
   * constructed in Repository class.
   */
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

  /**
   * the current commit in the current branch
   */
  public static Commit currCommit;

  /**
   * The current branch in gitlet.
   */
  public static String currBranch;

  /**
   * Create the .gitlet directory and all the related structure, with the init argv.
   */
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

  /**
   * Helper method for init command, now the current commit is the initial commit, and save the
   * initial commit in Commit directory.
   */
  private static void initialCommit() {
    Commit initialCommit = new Commit();
    currCommit = initialCommit;
    currCommit.save(); //for future persistent
  }

  /**
   * Set HEAD points to the specific branch's latest commit.
   */
  private static void setHEAD(String branch) {
    String branchPath = join("refs", "heads", branch).getPath();
    Utils.writeContents(HEAD, branchPath);
  }

  /**
   * Adds a copy of the file as it currently exists to the staging area.
   */
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
        if (!stage.getAddStage().isEmpty()) {
          if (stage.getAddStage().get(filename).equals(addedBlobID)) {
            stage.getAddStage().remove(filename);
          }
        } else if (stage.getRemoveStage().contains(filename)) {
          stage.getRemoveStage().remove(filename);
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

      //when the current commit doesn't contain the blob with the same name, meaning the
      //blob is first time created and added.
      else {
        addedBlob.saveBlob();
        stage.stageForAddition(filename, addedBlobID);
      }

    }
    // save stage obj
    stage.save();
  }

  /**
   * Return the current commit, from the HEAD FILE get the latest commit sha1.
   */
  private static Commit retrieveCurrentCommit() {
    String commitPath = readContentsAsString(HEAD); //HEAD points to the latest commit, refs/heads/master
    File latestCommit = Utils.join(GITLET_DIR, commitPath);
    String hash = readContentsAsString(latestCommit);
    return retrieveCommit(hash);
  }

  /**
   * Helper method, to retrieve the commit when having the sha1 code from HEAD. Get the commit file
   * from objects/commits
   */
  private static Commit retrieveCommit(String hash) {
    File cPath = join(commits, hash);
    return Utils.readObject(cPath, Commit.class);
  }

  /**
   * Commit command, saves a snapshot of tracked files in the current commit and staging area so
   * they can be restored later.
   */
  public static void commit(String message) throws IOException {
    //first, check whether the commit is valid, meaning whether it is in stage for addition
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    if (stage.getAddStage().isEmpty() && stage.getRemoveStage().isEmpty()) {
      System.out.println("No changes added to the commit.");
      System.exit(0);
    }

    //then generate a new commit, parent is the previous commit,clone the previous commit for the first time commit
    Commit parentCommit = retrieveCurrentCommit();
    String parent = parentCommit.getSha1();
    currCommit = new Commit(message, parent, "");

    //New commit tracked the saved files. By default, each commit's snapshot will be exactly the same as its parents,
    //it will keep versions of files exactly as they are, and not update them
    currCommit.setTracked(parentCommit.getTracked());

    //update the contents of files it is tracking that have been staged for addition at the time of commit,
    //save and start tracking any files that were staged for addition but not tracked by its parent
    updateStageToCommit(currCommit);

    //files tracked in the current commit may be untracked in the new commit, as the result of being staged for removal.
    updateStageForRemovalToCommit(currCommit);

    //submit the commit,and master points to new commit
    currBranch = readCurrBranch();
    submitCommit(currCommit, currBranch);

    //stage area should be cleared
    stage.clear();

    //save the stage
    stage.save();
  }

  /**
   * Get the current branch.
   */
  private static String readCurrBranch() {
    return readContentsAsString(HEAD);
  }


  /** Update and start tracking any files that were staged for addition, but weren't tracked by
   * it's parent commit.  */
  private static void updateStageToCommit(Commit newCommit) {
    Stage currStage = Utils.readObject(STAGES_FILE, Stage.class);
    Map<String,String> addStageArea = currStage.getAddStage();
    for (String key: addStageArea.keySet()) {
      newCommit.addTracked(key, addStageArea.get(key));
    }
  }

  /** Untracked the file in stage for removal area, if user first uses rm command. */
  private static void updateStageForRemovalToCommit(Commit newCommit) {
    Stage currStage = Utils.readObject(STAGES_FILE, Stage.class);
    for (String filename: currStage.getRemoveStage()) {
      newCommit.untracked(filename);
    }
  }

  /** Submit the commit into the current branch, add the commit under the object folder, and
   * make the HEAD points to the latest commit. */
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


  /** Rm command, unstage the file if it is currently in staged for addition. If the file is
   * tracked in current commit, stage it for removal and remove it in CWD.*/
  public static void rm(String filename) throws IOException {
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    File fileInWorkingDirectory = Utils.join(CWD, filename);
    currCommit = retrieveCurrentCommit();

    //unstage the file if it is currently staged for addition.
    if (stage.getAddStage().containsKey(filename)) {
      stage.remove(filename);
    }

    //stage it for removal if the file is tracked in current commit,and also exist in CWD
    //Note: it needs to be committed to remove the file in repository.
    else if (fileInWorkingDirectory.exists() &&
        currCommit.getTracked().containsKey(filename)) {
      stage.stageForRemoval(filename);
      //remove it from CWD
      Utils.restrictedDelete(fileInWorkingDirectory);
    }

    //stage it for removal, if the file istracked in current commit,but not exists in CWD
    else if (!fileInWorkingDirectory.exists() &&
        currCommit.getTracked().containsKey(filename)) {
      stage.stageForRemoval(filename);
    }

    //fail: file is neither staged nor tracked by the head commit
    else {
      System.out.println("No reason to remove the file.");
      System.exit(0);
    }

    //save the stage, after the rm command
    stage.save();
  }

  /** Log command. List the commit information. Until it reaches the init commit.  */
  public static void log() {
    currCommit = retrieveCurrentCommit();
    Commit commitIterator = currCommit;

    while(!commitIterator.getParent().equals("")) {
      logPrint(commitIterator);
      commitIterator = retrieveCommit(commitIterator.getParent());
    }
    System.out.println("===");
    System.out.println("commit "  + commitIterator.getSha1());
    System.out.println("Date: " + commitIterator.getTimestamp());
    System.out.println(commitIterator.getMessage() + "\n");

  }


  /** Log print information helper function. */
  private static void logPrint(Commit commit) {
    System.out.println("===");
    System.out.println("commit "  + commit.getSha1());
    if (!commit.getSecond_parent().isEmpty()) {
      System.out.println("Merge: " + commit.getParent().substring(0,6) + " " + commit.getSecond_parent().substring(0,6));
    }
    System.out.println("Date: " + commit.getTimestamp());
    System.out.println(commit.getMessage() + "\n");
  }

  /** Like log, except displays information about all commits ever made. The order of the commits
   * does not matter.*/
  public static void global_log() {
    List<String> commitRecord = Utils.plainFilenamesIn(commits);
    for (String sha1: commitRecord) {
      Commit currCommit = retrieveCommit(sha1);
      logPrint(currCommit);
    }
  }

  /** Prints out the ids of all commits that have the given commit message, one per line.
   * If there are multiple such commits, prints the id on separate lines. Note: it
   * doesn't exist in real git. */
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

  /** Displays what branches currently exists, and mark the current branch with a *. Also displays
   * what files have been staged for addition or removal. */
  public static void status() {
    branchStatus();
    stageStatus();
    notStagedFileStatus();
    untrackedStatus();
  }

  /** Status helper function, prints the branch status. */
  private static void branchStatus() {
    System.out.println("=== Branches ===");
    currBranch = readCurrBranch();
    String cBranch = currBranch.substring(currBranch.lastIndexOf("\\") +1);
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
    System.out.println();
  }


  /** Status helper function, prints the stage status. */
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
    System.out.println();
  }

  /** Status helper function, prints the modification files in CWD, but not staged for commit.
   * Modified in CWD but not use add command,  */
  private static void notStagedFileStatus() {
    System.out.println("=== Modifications Not Staged For Commit ===");
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    currCommit = retrieveCurrentCommit();
    List<String> fileInCWD = Utils.plainFilenamesIn(CWD);
    Map<String, String> trackedByCommit = currCommit.getTracked();
    Map<String, String> trackedByAddStage= stage.getAddStage();
    List<String> trackedByRemovedStage = stage.getRemoveStage();


    for (String file: fileInCWD) {
      Blob blob = new Blob (Utils.join(CWD, file));

      //tracked by current commit, changed in CWD, but not staged.
      if (trackedByCommit.containsKey(file)) {
        if (!trackedByAddStage.containsKey(file) && !trackedByCommit.get(file).equals(blob.getHash())) {
          System.out.println(file + " (modified)");
        }
      }

      //staged for addition, but with different contents than in CWD
      if (trackedByAddStage.containsKey(file) && !trackedByAddStage.get(file).equals(blob.getHash())) {
        System.out.println(file + " (modified)");
      }
    }

    //staged for addition, but delete in CWD
    for (String file: trackedByAddStage.keySet()) {
      if (!fileInCWD.contains(file)) {
        System.out.println(file + " (deleted)");
      }
    }

    //not staged for removal, but tracked in current commit and delete from CWD
    for (String file: trackedByCommit.keySet()) {
      if (!trackedByRemovedStage.contains(file) && !fileInCWD.contains(file)) {
        System.out.println(file + " (deleted)");
      }
    }
    System.out.println();
  }


  /** Status helper function, prints the untracked status. */
  private static void untrackedStatus() {
    System.out.println("=== Untracked Files ===");
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    currCommit = retrieveCurrentCommit();
    List<String> fileInCWD = Utils.plainFilenamesIn(CWD);
    for (String file: fileInCWD) {
      if (!currCommit.getTracked().containsKey(file) && !stage.getRemoveStage().contains(file)
          && !stage.getAddStage().containsKey(file)) {
        System.out.println(file);
      }
    }
    System.out.println();
  }


  /** First case of checkout. Takes the version of the file as it exists in the head commit and
   * put it in CWD, overwriting the version of file that's already there if it exists. The new
   * version of file is not staged. */

  public static void checkout(String filename) throws IOException {
    currCommit = retrieveCurrentCommit();

    checkoutHelper(filename, currCommit);
  }


  /** Helper method for the checkout case 1, and can be used in checkout case 2. */

  private static void checkoutHelper(String filename, Commit commit) throws IOException {
    if (commit.getTracked().containsKey(filename)) {
      String blobSha1 = commit.getTracked().get(filename);
      File originInBlob = Utils.join(blobs, blobSha1);
      Blob blob = Utils.readObject(originInBlob, Blob.class);
      byte[] contents = blob.getContents();
      File fileInCWD = Utils.join(CWD, filename);

      if (!fileInCWD.exists()) {
        fileInCWD.createNewFile();
        Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
      } else {
        Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
      }
    }

    else {
      System.out.println("File does not exist in that commit.");
      System.exit(0);
    }
  }

  /** Case 2 of checkout. Takes the version of file as it exists in the commit with the given id, and
   * put it in the working directory, overwriting the version of file that's already there if there
   * is one. The new version of file is not staged. */
  public static void checkout(String commitID, String filename) throws IOException {
    currCommit = retrieveCurrentCommit();
    Commit commitIterator = currCommit;

    if (currCommit.getSha1().equals(commitID)) {
      checkoutHelper(filename, currCommit);
    }

    else {
      while (!commitIterator.getParent().equals("")) {
        if (commitIterator.getParent().equals(commitID)) {
          String parentID = commitIterator.getParent();
          commitIterator = retrieveCommit(parentID);
          checkoutHelper(filename, commitIterator);
          return;

        } else {
          String parentID = commitIterator.getParent();
          commitIterator = retrieveCommit(parentID);
        }
      }
      System.out.println("No commit with that id exists.");
      System.exit(0);
    }
  }

  /** Case 3 checkout. Takes all files in the commit at the head of given branch, and puts them in CWD.
   * Overwriting the version of files that are already there if they exist. Also, at the end of command,
   * the given branch will be the current branch(HEAD). Any files that are tracked n current branch
   * but are not presented in checkout branch are deleted. The staging area are cleared, unless the
   * checkout branch is the current branch. */
  public static void checkoutBranch(String branchName) throws IOException {
    checkCurrentBranch(branchName);
    checkBranchExists(branchName);

    currCommit = retrieveCurrentCommit();
    currBranch = branchName;
    setHEAD(currBranch);     //move the HEAD pointer to checkout branch
    Commit target = retrieveCurrentCommit(); //commit under checkout branch

    //file tracked by both current commit and target commit
    List<String> fileTrackedBoth = findFileBothTracked(currCommit, target);

    //compare the both tracked file's blobID, if different, CWD overwrite the file same as file
    // tracked by target
    compareTrackedFiles(fileTrackedBoth, currCommit, target);

    //file only tracked by current commit, not the target commit
    List<String> fileOnlyTrackedByCurr = findFileOnlyTrackedByCurr(currCommit,target);
    //delete them in CWD directly
    for (String file: fileOnlyTrackedByCurr) {
      File fileInCWD = Utils.join(CWD, file);
      Utils.restrictedDelete(fileInCWD);
    }

    //file only tracked by target commit, not current commit.
    List<String> fileOnlyTrackedByTarget = findFileOnlyTrackedByTarget(currCommit, target); //
    putIntoCWD(fileOnlyTrackedByTarget, target);

    //clear the stages
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    stage.clear();
    stage.save();
  }

  /** Helper method, check whether the checkout branch is the current branch. */
  private static void checkCurrentBranch(String branchName) {
    currBranch = readCurrBranch();
    String cBranch = currBranch.substring(currBranch.lastIndexOf("\\") +1);
    if (cBranch.equals(branchName)) {
      System.out.println("No need to checkout the current branch.");
      System.exit(0);
    }
  }

  /** Helper method, check whether the branch exists in .gitlet. */
  private static void checkBranchExists(String branchName) {
    List<String> branchList = Utils.plainFilenamesIn(heads);
    if (!branchList.contains(branchName)) {
      System.out.println("No such branch exists.");
      System.exit(0);
    }
  }

  /** Helper method, find the file both tracked in current commit and target commit. */
  private static List<String> findFileBothTracked(Commit curr, Commit target) {
    List<String> fileTrackedByBoth = new ArrayList<>();
    for (String file: curr.getTracked().keySet()) {
      for (String key: target.getTracked().keySet()) {
        if (file.equals(key)) {
          fileTrackedByBoth.add(file);
        }
      }
    }
    return fileTrackedByBoth;
  }

  /** Helper method, find the file just tracked by current commit. */
  private static List<String> findFileOnlyTrackedByCurr(Commit curr, Commit commit) {

    List<String> fileTrackedOnlyByCurr = new ArrayList<>();
    if (commit.getTracked().keySet().isEmpty()) {
      for (String file: curr.getTracked().keySet()) {
        fileTrackedOnlyByCurr.add(file);
      }
    }

    for (String file: curr.getTracked().keySet()) {
      if (!commit.getTracked().keySet().contains(file)) {
        fileTrackedOnlyByCurr.add(file);
      }
    }
    return  fileTrackedOnlyByCurr;
  }

  /** Helper method, find the file just tracked by target commit. */
  private static List<String> findFileOnlyTrackedByTarget(Commit curr, Commit commit) {
    List<String> fileTrackedOnlyByTarget = new ArrayList<>();
    if (curr.getTracked().keySet().isEmpty()) {
      for (String file: commit.getTracked().keySet()){
        fileTrackedOnlyByTarget.add(file);
      }
      return fileTrackedOnlyByTarget;
    }

    for (String file: commit.getTracked().keySet()) {
      if (! curr.getTracked().keySet().contains(file)) {
        fileTrackedOnlyByTarget.add(file);
      }
    }
    return  fileTrackedOnlyByTarget;
  }

  /** Helper method, used for compare both tracked files in two commits, if different,
   * CWD overwrite the file same as file tracked by target*/
  private static void compareTrackedFiles(List<String> fileTrackedByBoth, Commit cCommit, Commit target) {
    for (String file : fileTrackedByBoth) {
      String blobSha1InCurrCommit = cCommit.getTracked().get(file);
      String blobSha1InTargetCommit = target.getTracked().get(file);
      if (!blobSha1InCurrCommit.equals(blobSha1InTargetCommit)) {
        File fileInCWD = Utils.join(CWD, file);
        File blobInTarget = Utils.join(blobs, blobSha1InTargetCommit);
        byte[] contents = Utils.readContents(blobInTarget);
        Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
      }
    }
  }


  /** Helper method. Put the file only tracked by target commit into the CWD. Raise error if there
   * are untracked files in CWD. */
  private static void putIntoCWD(List<String> fileOnlyTrackedByTarget, Commit target)
      throws IOException {

    if (fileOnlyTrackedByTarget.isEmpty()) {
      return;
    }

    //Put the target commit tracked files in CWD.
    for (String file: fileOnlyTrackedByTarget) {
      File fileInCWD = Utils.join(CWD, file);
      //If the CWD exists the file with the same name, meaning it is untracked.
      if (fileInCWD.exists()) {
        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
        System.exit(0);
      }

      //otherwise, it is tracked.
      fileInCWD.createNewFile();
      String blobSha1InTargetCommit = target.getTracked().get(file);

      File blobInTarget = Utils.join(blobs, blobSha1InTargetCommit);
      Blob b =readObject(blobInTarget, Blob.class);
      Utils.writeContents(fileInCWD, new String(b.getContents(), StandardCharsets.UTF_8));
/*    byte[] contents = Utils.readContents(blobInTarget);
      Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
      //    It will cause GBK failed in test
      */
    }
  }


  /** Creates a new branch with a given name, and points at the current head commit. A name for
   * reference (sha1 identifier) to a commit node. This command doesn't switch to the newly created
   * branch. Default branch is master/main. */
  public static void branch(String branchName) throws IOException {
    checkBranchAlreadyExist(branchName);

    File newBranch = Utils.join(heads, branchName);
    newBranch.createNewFile();

    //points to the HEAD pointer pointed commit
    currCommit = retrieveCurrentCommit();
    String commitSha1 = currCommit.getSha1();
    Utils.writeContents(newBranch, commitSha1);
  }

  /** Helper method, check whether the branch is already exists in .gitlet. */
  private static void checkBranchAlreadyExist(String branchName) {
    List<String> branchList = Utils.plainFilenamesIn(heads);
    for (String branch: branchList) {
      if (branch.equals(branchName)) {
        System.out.println("A branch with that name already exists.");
        System.exit(0);
      }
    }
  }

  /** Deletes the branch with the given name. Only deletes the pointer associated with the branch,
   * doesn't delete all commits that were created under the branch, or anything like that. */
  public static void rm_branch(String branchName) {
    checkWhetherBranchExists(branchName);

    currBranch = readCurrBranch();                 //refs/heads/cBranch
    String cBranch = currBranch.substring(currBranch.lastIndexOf("\\")+1);
    if (branchName.equals(cBranch)) {
      System.out.println("Cannot remove the current branch.");
      System.exit(0);
    }

    File deleteBranch = Utils.join(heads, branchName);
    deleteBranch.delete(); //Utils.restrictDelete mainly used to delete CWD files.
  }


  /** Helper method, check whether the removed branch is exists, if not, aborts.*/
  private static void checkWhetherBranchExists(String branchName) {
    List<String> branchList = Utils.plainFilenamesIn(heads);
    if (! branchList.contains(branchName)) {
      Utils.message("A branch with that name does not exist.");
      System.exit(0);
    }
  }

  /** Reset command. Checks out all files tracked by the given commit. Removes tracked files that are
   * not presented in that commit. Also moves current branch's head to that commit node. The staging
   * area is cleared. Similar to a checkout commit, but the current branch head is also changed.
   * Updating the index/stage, moving the head. */
  public static void reset(String commitID) throws IOException {
    checkCommitExists(commitID);

    //remove tracked files in current commit that are not presented in reset target commit.
    currCommit = retrieveCurrentCommit();
    Commit resetTarget = retrieveCommit(commitID);

    List<String> fileTrackedByBoth = findFileBothTracked(currCommit, resetTarget);
    compareTrackedFiles(fileTrackedByBoth, currCommit, resetTarget);  //overwrite

    List<String> fileOnlyTrackedByCurr = findFileOnlyTrackedByCurr(currCommit, resetTarget);
    for (String file: fileOnlyTrackedByCurr) {
      Utils.restrictedDelete(file);
    }

    List<String> fileOnlyTrackedByReset = findFileOnlyTrackedByTarget(currCommit,resetTarget);
    //This method considered the untracked files error
    putIntoCWD(fileOnlyTrackedByReset, resetTarget);

    //moving the current branch points to the reset target commit.
    currBranch = readCurrBranch();  //refs/heads/master
    String cBranch = currBranch.substring(currBranch.lastIndexOf("\\")+1); //master
    File headsFile = Utils.join(heads, cBranch);
    headsFile.createNewFile();
    Utils.writeContents(headsFile, commitID);

    //moving the HEAD to reset commit as well
    setHEAD(cBranch);

    //clear the staging area
    Stage stage = Utils.readObject(STAGES_FILE, Stage.class);
    stage.clear();
    stage.save();
  }

  /** Helper method, check whether the commit exists with that commitID. */
  private static void checkCommitExists(String commitID) {
    List<String> commitList = Utils.plainFilenamesIn(commits);
    if (! commitList.contains(commitID)) {
      System.out.println("No commit with that id exists.");
      System.exit(0);
    }
  }


  /** Merge command, merge files from the given branch to the current branch. */
  public static void merge(String givenBranch) throws IOException {
    Stage stage = readObject(STAGES_FILE, Stage.class);
    if (!stage.getAddStage().isEmpty() || !stage.getRemoveStage().isEmpty()) {
      System.out.println("You have uncommitted changes.");
      System.exit(0);
    }

    checkWhetherBranchExists(givenBranch);
    currBranch = readCurrBranch();
    String cBranch = currBranch.substring(currBranch.lastIndexOf("\\")+1);
    if (cBranch.equals(givenBranch)) {
      System.out.println("Cannot merge a branch with itself.");
      System.exit(0);
    }

    currCommit = retrieveCurrentCommit();
    for (String fileInCWD :
        Objects.requireNonNull(plainFilenamesIn(CWD))) {
      if (!currCommit.getTracked().containsKey(fileInCWD)){
        if (!stage.getAddStage().containsKey(fileInCWD)){
          message("There is an untracked file in the way; delete it, or add and commit it first.");
          System.exit(0);
        }
      }
      if (stage.getRemoveStage().contains(fileInCWD)){
        message("There is an untracked file in the way; delete it, or add and commit it first.");
        System.exit(0);
      }
    }

    String message = "Merged " + givenBranch + " into " + cBranch + ".";
    File givenBranchPoint = Utils.join(heads, givenBranch);
    String givenBranchHash = Utils.readContentsAsString(givenBranchPoint);

    Commit givenBranchCommit = retrieveCommit(givenBranchHash);  //commit pointed by branchName
    Commit splitCommit = findSplitCommit(currCommit, givenBranchCommit);

    checkWhetherMergeFinished(splitCommit, givenBranchCommit);
    checkWhetherFastForward(splitCommit, currCommit, givenBranch);

    Map<String, String> splitMap = getCommitMap(splitCommit);
    Map<String, String> currentCommitMap = getCommitMap(currCommit);
    Map<String, String> givenCommitMap = getCommitMap(givenBranchCommit);

    //put master commit into merge commit temporary, then check whether the blob need to be changed or delete
    Commit mergeCommit = new Commit(message, currCommit.getSha1(), givenBranchCommit.getSha1());  //currCommit;
    mergeCommit.setTracked(currCommit.getTracked());

    for (String id: splitMap.keySet()) {
      if (currentCommitMap.containsKey(id) && !givenCommitMap.containsKey(id)) {
        String modifiedFileName = currentCommitMap.get(id);
        //merge case 1, if files modified in given branch after split point, but not modified in
        //current branch, keep the modified version, and staged for addition.
        if (givenCommitMap.containsValue(modifiedFileName)) {
          mergeCommit.getTracked().remove(id);
          for (String s : givenCommitMap.keySet()) {
            if (givenCommitMap.get(s).equals(modifiedFileName)) {
              mergeCommit.addTracked(modifiedFileName, s);
              stage.stageForAddition(modifiedFileName, s);
              List<String> overWriteFile = new ArrayList<>();
              overWriteFile.add(givenCommitMap.get(s));
              compareTrackedFiles(overWriteFile, mergeCommit, givenBranchCommit);
            }
          }
        }
        //merge case 6, files present in split, unmodified in current branch, removed in the given branch,
        //should be removed and untracked.
        else {
          stage.stageForRemoval(modifiedFileName);
          stage.save();
          File fileInCWD = Utils.join(CWD, modifiedFileName);
          Utils.restrictedDelete(fileInCWD);
        }
      }

      //merge case 2, files being modified in current branch, but not in given branch, keep the file
      //merge case 7, files present in split, unmodified in given branch, removed in current branch, should remain absent.
      else if (!currentCommitMap.containsKey(id) && givenCommitMap.containsKey(id)) {
        continue;
      }

      //merge case 3, file being modified in both current and given branch
      else if (!currentCommitMap.containsKey(id) && !givenCommitMap.containsKey(id)) {
        //if they are modified in the same way, do nothing

        //Merge case 8, otherwise, there is a conflict. Need to deal with the conflict here.
        String filename = splitMap.get(id);
        checkIfConflict(filename, currCommit, givenBranchCommit, stage);
      }
    }

    //merge case 4, files not present at split point, but only presented in current branch. Keep them, nothing we need to do here.
    //Conflict merge case 8, file not exist in split but has different contents in current commit and given branch commit.
    removeRepeated(splitMap,currentCommitMap);
    removeRepeated(splitMap,givenCommitMap);
    for (String s: currentCommitMap.keySet()) {
      if (!splitMap.containsKey(s) && !givenCommitMap.containsKey(s)) {
        String filename = currentCommitMap.get(s);
        if (!splitMap.containsValue(filename) && !givenCommitMap.containsValue(filename)) {
          continue;
        }
        else if (!splitMap.containsValue(filename) && givenCommitMap.containsValue(filename)) {
          checkIfConflict(filename, currCommit, givenBranchCommit, stage);
        }
      }
    }

    //merge case 5, files not present at split point, only presented in given branch. Checked out and staged.
    //Conflict merge case 8, file not exist in split but has different contents in current commit and given branch commit.
    removeRepeated(currentCommitMap, givenCommitMap);
    for (String s: givenCommitMap.keySet()) {
      if (!splitMap.containsKey(s) && !currentCommitMap.containsKey(s)) {
        String filename = givenCommitMap.get(s);
        if (!splitMap.containsValue(filename) && !currentCommitMap.containsValue(filename)) {
          List<String> file = new ArrayList<>();
          file.add(filename);
          putIntoCWD(file, givenBranchCommit);
          stage.stageForAddition(filename, s);
          stage.save();
        }
        else if (!splitMap.containsValue(filename) && currentCommitMap.containsValue(filename)){
          checkIfConflict(filename, currCommit, givenBranchCommit, stage);
        }
      }
    }

    //tracked the file in stage for addition, and untracked the file in stage for removal
    updateStageToCommit(mergeCommit);
    updateStageForRemovalToCommit(mergeCommit);

    //submit the merge commit and clear the stage.
    submitCommit(mergeCommit, currBranch);
    stage.clear();
    stage.save();


  }


  /** Helper method, find the split commit for the two branch, for the further merge.
   * Using bfs, traverse both commit until reaches the init commit. */
  private static Commit findSplitCommit(Commit headC, Commit branchC) {
    Map<String, Integer> headCMap = new HashMap<>();
    int i=1;
    Commit iter = headC;
    Commit iter2 = headC;
    while (!iter.getParent().isEmpty()) {
      String hash = iter.getSha1();
      headCMap.put(hash, i);
      i++;
      if (!iter2.getSecond_parent().isEmpty()) {
        String hash2 = iter2.getSecond_parent();
        headCMap.put(hash2, i);
        i++;
        iter2 = retrieveCommit(iter2.getSecond_parent());
      }
      iter = retrieveCommit(iter.getParent());
    }
    headCMap.put(iter.getSha1(), i);

    Map<String, Integer> branchCMap = new HashMap<>();
    int j=1;
    Commit iterator = branchC;
    Commit iterator2 = branchC;
    while (!iterator.getParent().isEmpty()) {
      String hash = iterator.getSha1();
      branchCMap.put(hash, j);
      j++;
      if (!iterator2.getSecond_parent().isEmpty()) {
        String hash2 = iterator2.getSecond_parent();
        headCMap.put(hash2, j);
        j++;
        iterator2 = retrieveCommit(iterator2.getSecond_parent());
      }
      iterator = retrieveCommit(iterator.getParent());
    }
    branchCMap.put(iterator.getSha1(), j);

    int depth = MAX_VALUE;
    String tmp = "";
    for (String commitID: branchCMap.keySet()) {
      if (headCMap.containsKey(commitID)) {
        if (depth > headCMap.get(commitID)) {
          tmp = commitID;
          depth = headCMap.get(commitID);
        }
      }
    }
    Commit splitCommit = retrieveCommit(tmp);
    return splitCommit;
  }

  /** Helper method, check whether the merge is finished, meaning that split commit
   * is the same commit as the given branch.*/
  private static void checkWhetherMergeFinished(Commit split, Commit branchC) {
    if (split.getSha1().equals(branchC.getSha1())) {
      System.out.println("Given branch is an ancestor of the current branch.");
      System.exit(0);
    }
  }

  /** Helper method, check whether it is fast forwarded. In this case, the split commit
   * is the current branch, Then we check out the given branch.
   * If it is, update the HEAD and show the fast-forward message.*/
  private static void checkWhetherFastForward(Commit splitCommit, Commit HEADCommit, String branchName)
      throws IOException {
    if (splitCommit.getSha1().equals(HEADCommit.getSha1())) {
      System.out.println("Current branch fast-forwarded.");
      //update the file in CWD, move HEAD point to the branch pointed commit.
      checkoutBranch(branchName);
    }
  }

  /** Helper method, create a map of specific commit, key is the commitID, value
   * is the filename. */
  private static Map<String,String> getCommitMap(Commit commit) {
    Map<String, String> commitMap = new HashMap<>();
    if (commit.getTracked().isEmpty()) {
      return commitMap;
    }
    for (String key: commit.getTracked().keySet()) {
      commitMap.put(commit.getTracked().get(key), key);
    }
    return commitMap;
  }


  /** Helper method, check whether there is a conflict, if file doesn't exist in split but has
   * different contents in current commit and given branch commit.*/
  private static void checkIfConflict(String filename, Commit cCommit, Commit givenCommit, Stage stage)
      throws IOException {
    boolean conflict = false;
    //key is the blob id, value is the filename;
    Map<String, String> currCommitMap = getCommitMap(cCommit);
    Map<String, String> givenCommitMap = getCommitMap(givenCommit);
    String currBlobID = "";
    String givenBlobID = "";
    for (String s: currCommitMap.keySet()) {
      if (currCommitMap.get(s).equals(filename)) {
        currBlobID = s;
      }
    }
    for (String g: givenCommitMap.keySet()) {
      if (givenCommitMap.get(g).equals(filename)) {
        givenBlobID = g;
      }
    }

    //both current commit and given commit track the file with same name, need to check
    //whether the contents is the same as well.
    if (currCommitMap.containsValue(filename) && givenCommitMap.containsValue(filename)) {
      if (!currBlobID.equals(givenBlobID)) {
        conflict = true;
      }
    }

    //file exists in split commit, but delete in both current commit and given commit.
    else if (!currCommitMap.containsValue(filename) && !givenCommitMap.containsValue(filename)){
      conflict = false;
    }

    //file exists split commit, tracked by current commit and untracked by given commit, or untracked by current commit
    // and tracked by given commit. Conflict.
    else {
      conflict = true;
    }

    if (conflict) {
      String currBranchContents = "";
      String givenBranchContents = "";
      if (!currBlobID.isEmpty()) {
        File currBlob = Utils.join(blobs, currBlobID);
        Blob currB = readObject(currBlob, Blob.class);
        currBranchContents = new String(currB.getContents(), StandardCharsets.UTF_8);
      }
      if (!givenBlobID.isEmpty()) {
        File givenBlob = Utils.join(blobs, givenBlobID);
        Blob givenB = Utils.readObject(givenBlob, Blob.class);
        givenBranchContents = new String(givenB.getContents(), StandardCharsets.UTF_8);
      }
      String conflictContents = "<<<<<<< HEAD\n" + currBranchContents + "=======\n" + givenBranchContents + ">>>>>>>\n";
      File conflictFile = join(CWD, filename);
      conflictFile.createNewFile();
      Utils.writeContents(conflictFile, conflictContents);

      //stage the result
      Blob b = new Blob(conflictFile);
      stage.stageForAddition(filename, b.getHash());
      stage.save();
      System.out.println("Encountered a merge conflict.");
    }
  }

  /** Helper function, removed the already considered file in split commit map, and only
   * consider the remained file tracked in currMap or givenMap. */
  private static void removeRepeated(Map<String, String> splitMap, Map<String, String> otherMap){
    for (String key: splitMap.keySet()) {
      if (otherMap.containsKey(key)) {
        otherMap.remove(key);
      }
    }
  }

}