import java.util.HashMap;
import java.util.Stack;

public class LLVMActions extends MKBaseListener {
    private HashMap<String, GlobalVarExpression> globalVariables = new HashMap<String, GlobalVarExpression>();
    private LLVMGenerator generator = new LLVMGenerator(this, globalVariables);
    private Stack<Expression> expressionStack = new Stack<Expression>();
    private Configuration configuration = new Configuration();
    private int line = 0;
    private String fileName;

    public LLVMActions(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void exitAdd(MKParser.AddContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.ADD);
    }

    @Override
    public void exitMultiply(MKParser.MultiplyContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.MUL);
    }

    @Override
    public void exitDivide(MKParser.DivideContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.DIV);
    }

    @Override
    public void exitSubtract(MKParser.SubtractContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.SUB);
    }

    private void calculate(CalculationType calculationType) {
        Expression rightExpression = expressionStack.pop();
        Expression leftExpression = expressionStack.pop();

        UnnamedVarExpression result = generator.calculate(leftExpression, calculationType, rightExpression);
        expressionStack.push(result);
    }

    @Override
    public void exitInt(MKParser.IntContext context) {
        line = context.getStart().getLine();

        Integer value = Integer.parseInt(context.INT().getText());

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.INT, value);
        expressionStack.push(expression);
    }

    @Override
    public void exitReal(MKParser.RealContext context) {
        line = context.getStart().getLine();

        Double value = Double.parseDouble(context.REAL().getText());

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.REAL, value);
        expressionStack.push(expression);
    }

    /*@Override
    public void exitStr(MKParser.StrContext context) {
        line = context.getStart().getLine();

        String value = context.STR().getText();

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.STR, value);
        expressionStack.push(expression);
    }*/

    @Override
    public void exitName(MKParser.NameContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Expression expression = null;
        if (globalVariables.containsKey(name)) {
            expression = globalVariables.get(name);
        } else {
            printError("using non-existing lady " + name);
        }

        expressionStack.push(expression);
    }

    @Override
    public void exitVariable_declaration(MKParser.Variable_declarationContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        ObjectType objectType = ObjectType.VARIABLE;

        if (context.ASSIGN() == null) {
            generator.declareVariable(new GlobalVarExpression(objectType, DataType.NONE, name, 0));
        } else {
            Expression rightExpression = expressionStack.pop();
            DataType dataType = rightExpression.getDataType();

            GlobalVarExpression leftExpression = new GlobalVarExpression(objectType, dataType, name, 0);
            generator.declareVariable(leftExpression);

            generator.assignVariable(leftExpression, rightExpression);
        }


    }

    @Override
    public void exitVariable_assignment(MKParser.Variable_assignmentContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Expression rightExpression = expressionStack.pop();
        GlobalVarExpression leftExpression = new GlobalVarExpression(ObjectType.VARIABLE, DataType.NONE, name, 0);

        generator.assignVariable(leftExpression, rightExpression);
    }

    @Override
    public void exitPrint(MKParser.PrintContext context) {
        line = context.getStart().getLine();

        Expression expression = expressionStack.pop();
        generator.print(expression);
    }

    @Override
    public void exitScan_int(MKParser.Scan_intContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        scan(DataType.INT, name);
    }

    @Override
    public void exitScan_real(MKParser.Scan_realContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        scan(DataType.REAL, name);
    }

    private void scan(DataType dataType, String name) {
        Expression expression = null;

        if (globalVariables.containsKey(name)) {
            expression = globalVariables.get(name);
        } else {
            printError("getting non-existing lady " + name + " to hear");
        }

        generator.scan(dataType, (GlobalVarExpression) expression);
    }

    @Override
    public void exitFile(MKParser.FileContext context) {
        line = context.getStart().getLine();

        generator.generateOutput();
    }


    public void printError(String message) {
        System.err.println("Compilation error at line " + line + " - " + message + " in " + fileName);
        System.exit(1);
    }
}

