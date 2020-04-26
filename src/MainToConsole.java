import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Stack;

public class MainToConsole {
    public static void main(String[] args) throws Exception {
        String fileName = "test.mk";

        ANTLRFileStream input = new ANTLRFileStream(fileName);

        MKLexer lexer = new MKLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MKParser parser = new MKParser(tokens);

        ParseTree tree = parser.file();

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(fileName), tree);


    }
}