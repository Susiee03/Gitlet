# Gitlet

#### An individual project implemented by java. It implements the basic git command, including init, add, commit, log, status, checkout, branch, and merge.
#### The design documentation can be found in gitlet-design.md file, in gitlet folder.

#### The basic gitlet structure I made:
      .gitlet/
       - objects/
         - blob/
         - commits/
       - refs            the newest commit in all branches
         - heads            a ref that points to the tip (latest commit) of a branch.
           - master/main
         - remotes
       - HEAD               the currently checked-out branch's latest commit in Gitlet/ a commit currently checked out in the working directory, a specific git ref./checkout                               command will move HEAD to a specific commit.
       - stages/index
       
       
#### Method to get the whole project:
* Opening the local terminal, find a directory you'd like to store the project, type git clone https://github.com/Susiee03/Gitlet.git
* After finishing git clone, using the IDE(Intellij IDEA) to navigate the directory you choose, and open it. Then you could see the whole project.


#### Related link of the project:
* This is the cs61b course project, details can be found at: https://sp21.datastructur.es/materials/proj/proj2/proj2 
