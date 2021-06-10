package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Cynch {

    static boolean hadError = false;

    // Allows for the interpreter to run code in two ways:
    // 1. From the command line with a path to the file
    // 2. Using a prompt to write one line at a time
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: cynch [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    // Runs the file at the path given from the command line
    private static void runFile(String path) throws IOException {
        byte bytes[] = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
    }

    // Runs the code given through the prompt
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.println("> ");
            String line = reader.readLine();

            // Exit the prompt by typing CTRL-D
            if (line == null)
                break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // Print the tokens
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        // Reports the location and error
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

}