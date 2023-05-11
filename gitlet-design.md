# Gitlet Design Document

**Name**: Shuyao YU

## Classes and Data Structures
![img_1.png](img_1.png)
* .gitlet/
  - objects/
    - blob/
    - commits/
  - refs
    - heads
      - master/main  store sha1 id
      - other branches
    - remote
  - stages/same sa index in git
  - HEAD  :a pointer, stores a path to tell git where is the HEAD commit

### Class 1: Repository
ensure the structure of gitlet, and created the persistence.

#### Fields

* Files followed by the structures

#### Methods:
* setUp() - follow the directory and file structure, set up the .gitlet
* file.



### Class 2: Gitlet
// wrap all the gitlet command into this class.
#### Fields:
* files and directory from the Repository class
* currBranch record the current branch of gitlet command
* currCommit record the latest commit in the current branch

#### Methods:
* init() - initial commit (need the Commit class), single master branch, set all related files and directories.
* ![img_3.png](img_3.png)
* 
* add(String filename) - needs blobs, add stages, be careful about the rm.  ##only one file may be added at one time.
![img_4.png](img_4.png)
* 

* commit(String message) - 



* rm (String filename) - 

### Class 3: Commit 
//learnt from the helper video, also can be used in blob class
![img.png](img.png)

#### Fields:
* String message - contains the message of the commit
* Date timeStamp - time at which the commit was created
* String parent - the parent commit of a commit object, but stored in commit object as a string, for tracking
* String sha1 - the commit id
* File commitFileName - the actual commit file stored under the object directory


#### Methods:

//in commit, the key of the map is the file name usually created by user, value is blob's sha1.


After the commit command, the .gitlet looks like:
![img_5.png](img_5.png)

### Class 4: Blob
//another object, the structure is similar to Commit, but 
easier to construct.For each blob object, blob's filename is blob sha1, content is the bytes

#### Fields
1. Field 1
2. Field 2







### Class 5: Stage
//from the helper video and slides, after the commit, 
the staging area looks like:
![img_2.png](img_2.png)

## Algorithms

## Persistence

