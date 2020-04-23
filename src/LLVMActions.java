import java.util.HashMap;
import java.util.Stack;

public class LLVMActions extends MKBaseListener {
    private HashMap<String, Variable> globalVariables = new HashMap<String, Variable>();
    private LLVMGenerator generator = new LLVMGenerator(this, globalVariables);
    private Stack<ExplicitExpression> expressionStack = new Stack<ExplicitExpression>();
    private Configuration configuration = new Configuration();
    private int line = 0;
    private String fileName;

    public LLVMActions(String fileName) {
        this.fileName = fileName;
    }

    private class ExplicitExpression {
        private Object value;
        private VariableType type;
        private Variable variable;

        public ExplicitExpression(Object value, VariableType type) {
            this.value = value;
            this.type = type;
            this.variable = null;
            expressionStack.push(this);
        }

        public ExplicitExpression(Variable variable) {
            this.variable = variable;
            this.value = variable.getValue();
            this.type = variable.getType();
            expressionStack.push(this);
        }

        public Object getValue() {
            return value;
        }

        public VariableType getType() {
            return type;
        }

        public Variable getVariable() {
            return variable;
        }

        @Override
        public String toString() {
            return "ExplicitExpression{" +
                    "value=" + value +
                    ", type=" + type +
                    '}';
        }
    }

    private void assignVariable(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        switch (scope) {
            case GLOBAL:
                globalVariables.put(name, variable);
                generator.assignVariable(variable);
                break;
        }
    }

    private void declareVariable(Variable variable) {
        String name = variable.getName();
        VariableScope scope = variable.getScope();
        VariableType type = variable.getType();

        switch (scope) {
            case GLOBAL:
                if (globalVariables.containsKey(name)) {
                    printError("declaring already existing lady " + name);
                } else {
                    globalVariables.put(name, variable);
                    generator.declareVariable(variable);
                    if (type != VariableType.NONE && type != VariableType.STR) {
                        generator.assignVariable(variable);
                    }
                }
                break;
        }
    }

    @Override
    public void exitInt(MKParser.IntContext context) {
        line = context.getStart().getLine();

        Integer value = Integer.parseInt(context.INT().getText());
        new ExplicitExpression(value, VariableType.INT);
    }

    @Override
    public void exitReal(MKParser.RealContext context) {
        line = context.getStart().getLine();

        Double value = Double.parseDouble(context.REAL().getText());
        new ExplicitExpression(value, VariableType.REAL);
    }

    @Override
    public void exitStr(MKParser.StrContext context) {
        line = context.getStart().getLine();

        String value = context.STR().getText();
        new ExplicitExpression(value, VariableType.STR);
    }

    @Override
    public void exitName(MKParser.NameContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        if (globalVariables.containsKey(name)) {
            Variable variable = globalVariables.get(name);
            new ExplicitExpression(variable);
        } else {

        }
    }

    @Override
    public void exitVariable_declaration(MKParser.Variable_declarationContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        Object value = null;
        VariableType type = null;

        if (context.ASSIGN() == null) {
            type = VariableType.NONE;
        } else {
            ExplicitExpression explicitExpression = expressionStack.pop();
            value = explicitExpression.getValue();
            type = explicitExpression.getType();
        }

        Variable variable = new Variable(VariableScope.GLOBAL, type, name, value);
        declareVariable(variable);
    }

    @Override
    public void exitVariable_assignment(MKParser.Variable_assignmentContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        ExplicitExpression explicitExpression = expressionStack.pop();
        Object value = explicitExpression.getValue();
        VariableType type = explicitExpression.getType();

        if (globalVariables.containsKey(name)) {
            Variable variable = globalVariables.get(name);

            changeType(variable, type);
            variable.setValue(value);

            assignVariable(variable);
        } else {
            printError("assigning to non-existing lady " + name);
        }
    }

    @Override
    public void exitPrint(MKParser.PrintContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Variable variable = null;
        Object object = null;
        VariableType type = VariableType.NONE;

        if (name == null) {
            ExplicitExpression explicitExpression = expressionStack.pop();
            variable = explicitExpression.getVariable();
            object = explicitExpression.getValue();
            type = explicitExpression.getType();
        } else {
            if (globalVariables.containsKey(name)) {
                variable = globalVariables.get(name);
                object = variable.getValue();
                type = variable.getType();
            } else {
                printError("printing non-existing lady " + name);
            }
        }

        switch (type) {
            case INT:
                if (variable == null) {
                    generator.printInt(object.toString());
                } else {
                    generator.printInt(variable);
                }
                break;
            case REAL:
                if (variable == null) {
                    generator.printReal(object.toString());
                } else {
                    generator.printReal(variable);
                }
                break;
            case STR:
                if (variable == null) {
                    generator.printStr((String) object);
                } else {
                    generator.printStr(variable);
                }
                break;
        }
    }

    @Override
    public void exitScan_int(MKParser.Scan_intContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        if (globalVariables.containsKey(name)) {
            Variable variable = globalVariables.get(name);

            changeType(variable, VariableType.INT);

            generator.scanInt(variable);
        } else {
            printError("getting non-existing lady " + name + " to hear");
        }
    }

    @Override
    public void exitScan_real(MKParser.Scan_realContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        if (globalVariables.containsKey(name)) {
            Variable variable = globalVariables.get(name);

            changeType(variable, VariableType.REAL);

            generator.scanReal(variable);
        } else {
            printError("getting non-existing lady " + name + " to hear");
        }
    }

    @Override
    public void exitFile(MKParser.FileContext context) {
        line = context.getStart().getLine();

        generator.generateOutput();
    }


    public void printError(String message) {
        System.err.println("Error at line " + line + " - " + message + " in " + fileName);
        System.exit(1);
    }

    private void changeType(Variable variable, VariableType newType) {
        variable.setType(newType);
        variable.increaseMemoryIndex();
        generator.declareVariable(variable);
    }
}

