import java.util.HashMap;

public class Configuration {

    private final HashMap<String, String> systemVariables = new HashMap<String, String>();

    public Configuration() {
        systemVariables.put("printInt", "@sysvar_printint");
        systemVariables.put("printReal", "@sysvar_printreal");
        systemVariables.put("printValue", "@sysvar_printval");
        systemVariables.put("printf", "@printf");
        systemVariables.put("scanf", "@scanf");
        systemVariables.put("scanInt", "@sysvar_scanint");
        systemVariables.put("scanReal", "@sysvar_scanreal");
    }

    public HashMap<String, String> getSystemVariables() {
        return systemVariables;
    }
}
