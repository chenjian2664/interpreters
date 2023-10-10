package org.example.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class Lox
{
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadRuntimeError = false;
    static boolean hadError = false;

    private static int fb(int n)
    {
        if (n < 2) return n;
        return fb(n - 1) + fb(n - 2);
    }

    public static void main(String[] args)
            throws IOException
    {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runPrompt();
        }

//        long before = System.currentTimeMillis();
//        System.out.println(fb(40));
//        long after = System.currentTimeMillis();
//        System.out.println(after - before);
//        runFile("fb.lox");

    }

    private static void runFile(String path)
            throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt()
            throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (Objects.equals(line, "exit")) {
                break;
            }
            run(line);
            hadError = false;
        }
    }

    private static void run(String source)
    {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> stmts = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) {
            return;
        }

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(stmts);

        if (hadError) {
            return;
        }

        interpreter.interpret(stmts);
    }

    static void error(int line, String message)
    {
        report(line, "", message);
    }

    private static void report(int line, String where,
            String message)
    {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message)
    {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        }
        else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(Interpreter.RuntimeError error)
    {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
