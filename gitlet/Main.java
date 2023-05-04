package gitlet;

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
        String firstArg = args[0];
        switch(firstArg) {
            /**get the current working directory*/
            case "init":
                // TODO: handle the `init` command
                Gitlet.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command

                break;
            case "commit":

        }
    }
}
