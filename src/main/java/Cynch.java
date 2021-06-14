package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Cynch {

    // Static so that successive calls use the same interpreter
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

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
        if (hadRuntimeError) System.exit(70);
    }

    // Runs the code given through the prompt
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();

            // Exit the prompt by typing CTRL-D
            if (line == null) break;
            if (line.equalsIgnoreCase("Quit")) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    // Reports the given error with information
    private static void report(int line, String where, String message) {
        // Reports the location and error
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // Shows the error to the user
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println("\n[line " + error.token.line
        + "] " + error.getMessage());

        hadRuntimeError = true;
    }
}