package gitlet;

import static gitlet.Utils.error;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  mostly be calling helper methods in the Repository class.
 *  @author Shuyao YU
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *  java gitlet.Main add file
     */
    public static void main(String[] args) throws IOException {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            /**get the current working directory*/
            case "init":
                // TODO: handle the `init` command
                Gitlet.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if (args.length < 2) {
                    throw error("Please enter a add file message.");
                }
                validateNumArgs("add", args, 2);
                checkIfInitialized();
                Gitlet.add(args[1]);
                break;
            case "commit":
                if (args.length < 2) {
                    throw error("Please enter a commit message.");
                }
                validateNumArgs("commit", args, 2);
                checkIfInitialized();
                Gitlet.commit(args[1]);
                break;

            case "rm":
                if (args.length < 2) {
                    throw error("Please enter a remove file message.");
                }
                validateNumArgs("rm", args, 2);
                checkIfInitialized();
                Gitlet.rm(args[1]);
                break;

            case "log":
                checkIfInitialized();
                Gitlet.log();
                break;

            case "global-log":
                checkIfInitialized();
                Gitlet.global_log();
                break;

            case "find":
                if (args.length < 2) {
                    throw error("Please enter a find commit message.");
                }
                validateNumArgs("find", args, 2);
                checkIfInitialized();
                Gitlet.find(args[1]);
                break;

            case "status":
                checkIfInitialized();
                Gitlet.status();
                break;

            case "checkout":
                checkIfInitialized();
                switch (args.length) {
                    case 3:
                        if (args[1].equals("--")) {
                            Gitlet.checkout(args[2]);
                            break;
                        }
                    case 4:
                        if (args[2].equals("--")) {
                            Gitlet.checkout(args[1], args[3]);
                            break;
                        }
                    case 2:
                        Gitlet.checkoutBranch(args[1]);
                        break;
                }
                break;

            case "branch":
                checkIfInitialized();
                validateNumArgs("branch", args, 2);
                Gitlet.branch(args[1]);
                break;

            case "rm-branch":
                checkIfInitialized();
                validateNumArgs("rm-branch", args, 2);
                Gitlet.rm_branch(args[1]);
                break;

            case "reset":
                checkIfInitialized();
                validateNumArgs("reset", args, 2);
                Gitlet.reset(args[1]);
                break;

            case "merge":
                checkIfInitialized();
                validateNumArgs("merge", args, 2);
                Gitlet.merge(args[1]);
                break;
        }

    }


    /** check whether the user input the correct command. */
    private static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                String.format("Invalid number of arguments for: %s.", cmd));
        }
    }


    /** check whether the .gitlet has been initialized */
    private static void checkIfInitialized() {
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

}
