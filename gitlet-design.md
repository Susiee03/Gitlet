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
* init() - initial commit (need the Commit class), single master branch, set all related files and directories.
* ![img_3.png](img_3.png)
* 
* add(String filename) - needs blobs, add stages, be careful about the rm.  ##only one file may be added at one time.
![img_4.png](img_4.png)
* 


### Class 2: Commit 
//learnt from the helper video, also can be used in blob class
![img.png](img.png)

#### Fields:
* String message - contains the message of the commit
* Date timeStamp - time at which the commit was created
* String parent - the parent commit of a commit object, but stored in commit object as a string, for tracking
* String sha1 - the commit id
* File commitFileName - the actual commit file stored under the object directory


#### Methods:

//need a method to get the working directory's file name
, stored as string in commit object



### Class 3: Blob
//another object, the structure is similar to Commit, but 
easier to construct

#### Fields
1. Field 1
2. Field 2







### Class 4: Stage
//from the helper video and slides, after the commit, 
the staging area looks like:
![img_2.png](img_2.png)

## Algorithms

## Persistence

