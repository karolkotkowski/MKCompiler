import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

public class LLVMActions extends MKBaseListener {
    private HashMap<String, Variable> globalVariables = new HashMap<String, Variable>();
    private LLVMGenerator generator = new LLVMGenerator(globalVariables);
    private Stack<ExplicitExpression> stack = new Stack<ExplicitExpression>();
    private Configuration configuration = new Configuration();

    private class ExplicitExpression {
        private Object value;
        private VariableType type;
        private Variable variable;

        public ExplicitExpression(Object value, VariableType type) {
            this.value = value;
            this.type = type;
            this.variable = null;
            stack.push(this);
        }

        public ExplicitExpression(Variable variable) {
            this.variable = variable;
            this.value = variable.getValue();
            this.type = variable.getType();
            stack.push(this);
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
                if (globalVariables.containsKey(name)) {

                } else {
                    //error
                }
                break;
        }
    }

    private void declareVariable(Variable variable) {
        String name = variable.getName();
        VariableScope scope = variable.getScope();
        switch (scope) {
            case GLOBAL:
                if (globalVariables.containsKey(name)) {
                    //error
                } else {
                    globalVariables.put(name, variable);
                    generator.declareVariable(variable);
                }
                break;
        }
    }

    @Override
    public void exitInt(MKParser.IntContext context) {
        Integer value = Integer.parseInt(context.INT().getText());
        new ExplicitExpression(value, VariableType.INT);
    }

    @Override
    public void exitStr(MKParser.StrContext context) {
        String value = context.STR().getText();
        new ExplicitExpression(value, VariableType.STR);
    }

    @Override
    public void exitName(MKParser.NameContext context) {
        String name = context.NAME().getText();

        if (globalVariables.containsKey(name)) {
            Variable variable = globalVariables.get(name);
            new ExplicitExpression(variable);
        } else {
            System.out.println("give error here - assigning to non-existing variable");
            System.exit(0);
        }
    }

    @Override
    public void exitVariable_declaration(MKParser.Variable_declarationContext context) {
        //int line = context.getStart().getLine();
        String name = context.NAME().getText();
        Object value = null;
        VariableType type = null;

        if (context.ASSIGN() == null) {
            type = VariableType.NONE;
        } else {
            ExplicitExpression explicitExpression = stack.pop();
            value = explicitExpression.getValue();
            type = explicitExpression.getType();
        }

        Variable variable = new Variable(VariableScope.GLOBAL, type, name, value);
        declareVariable(variable);
    }

    @Override
    public void exitPrint(MKParser.PrintContext context) {
        ExplicitExpression explicitExpression = stack.pop();
        Variable variable = explicitExpression.getVariable();
        Object object = explicitExpression.getValue();
        switch (explicitExpression.getType()) {
            case INT:
                if (variable == null) {
                    generator.printInt(object.toString());
                } else {
                    generator.printInt(variable);
                }
                break;
            case REAL:

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
    public void exitFile(MKParser.FileContext context) {
        generator.generateOutput();
    }
}

