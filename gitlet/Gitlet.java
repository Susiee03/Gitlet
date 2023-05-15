package gitlet;

import static gitlet.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Gitlet {
  /**
   * all the gitlet related command will be put in this class, the fields are files and directories
   * constructed in Repository class
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
   * the current branch in gitlet
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
    currCommit = new Commit(message, parent);

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

    //TODO: doesn't consider the merge case, will figure it out later. The info is different.
  }


  /** Log print information helper function. */
  private static void logPrint(Commit commit) {
    System.out.println("===");
    System.out.println("commit "  + commit.getSha1());
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
    //TODO: didn't consider the merge situation. will modify this part later
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
    System.out.println();
    stageStatus();
    //TODO: extra credit, will do it later
    System.out.println("=== Modifications Not Staged For Commit ===\n");

    System.out.println("=== Untracked Files ===\n");

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

    //compare the both tracked file's blobID, if different, CWD overwrite the file same as file tracked by target
    for (String file: fileTrackedBoth) {
      String blobSha1InCurrCommit = currCommit.getTracked().get(file);
      String blobSha1InTargetCommit = target.getTracked().get(file);
      if (!blobSha1InCurrCommit.equals(blobSha1InTargetCommit)) {
        File fileInCWD = Utils.join(CWD, file);
        File blobInTarget = Utils.join(blobs, blobSha1InTargetCommit);
        byte[] contents = Utils.readContents(blobInTarget);
        Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
      }
    }

    //file only tracked by current commit, not the target commit
    List<String> fileOnlyTrackedByCurr = findFileOnlyTrackedByCurr(currCommit,target);
    //delete them in CWD directly
    for (String file: fileOnlyTrackedByCurr) {
      File fileInCWD = Utils.join(CWD, file);
      Utils.restrictedDelete(fileInCWD);
    }

    //file only tracked by target commit, not current commit.
    List<String> fileOnlyTrackedByTarget = findFileOnlyTrackedByTarget(currCommit, target);
    //Put it in CWD, if the CWD exists the file with the same name, meaning it is untracked.
    for (String file: fileOnlyTrackedByTarget) {
      File fileInCWD = Utils.join(CWD, file);
      if (fileInCWD.exists()) {
        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
        System.exit(0);
      }
      //or the CWD has other files, that doesn't track by target commit
      List<String> fileListInCWD = Utils.plainFilenamesIn(CWD);
      for (String s: fileListInCWD) {
        if (! fileOnlyTrackedByTarget.contains(s)) {
          System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
          System.exit(0);
        }
      }

      //otherwise, it is not untracked.
      fileInCWD.createNewFile();
      String blobSha1InTargetCommit = target.getTracked().get(file);
      File blobInTarget = Utils.join(blobs, blobSha1InTargetCommit);
      byte[] contents = Utils.readContents(blobInTarget);
      Utils.writeContents(fileInCWD, new String(contents, StandardCharsets.UTF_8));
    }

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
    if (!Objects.requireNonNull(branchList).contains(branchName)) {
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
      for (String key: commit.getTracked().keySet()) {
        if (!file.equals(key)) {
          fileTrackedOnlyByCurr.add(file);
        }
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
    }

    for (String file: commit.getTracked().keySet()) {
      for (String key: curr.getTracked().keySet()) {
        if (!file.equals(key)) {
          fileTrackedOnlyByTarget.add(file);
        }
      }
    }
    return  fileTrackedOnlyByTarget;
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
}