public class Instance {
    private String name;
    private Classy classy;

    public Instance(String name, Classy classy) {
        this.name = name;
        this.classy = classy;
    }

    public String getName() {
        return name;
    }

    public Classy getClassy() {
        return classy;
    }
}
