import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.*;

public class MainToFile {
    public static void main(String[] args) throws Exception {
        String fileFrom = "test.mk";
        String fileTo = "test.ll";
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fileTo)), true));

        String errFile = "err";
        System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(errFile)), true));

        ANTLRFileStream input = new ANTLRFileStream(fileFrom);

        MKLexer lexer = new MKLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MKParser parser = new MKParser(tokens);

        ParseTree tree = parser.file();
        ParseTreeWalker walker = new ParseTreeWalker();

        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        BufferedReader reader;
        boolean syntaxError = false;
        try {
            reader = new BufferedReader(new FileReader(errFile));
            String line = reader.readLine();
            if (line != null)
                syntaxError = true;
            while (line != null) {
                System.err.println("Syntax error at " + line + " in " + fileFrom);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (syntaxError)
            System.exit(1);

        walker.walk(new LLVMActions(fileFrom), tree);

    }
}