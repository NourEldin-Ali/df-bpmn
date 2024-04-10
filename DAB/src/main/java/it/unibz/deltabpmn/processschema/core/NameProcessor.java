package it.unibz.deltabpmn.processschema.core;

//ToDo: merger with the NameManager class
public final class NameProcessor {

    private static int transitionCounter;

    private NameProcessor() {
        transitionCounter = 0;
    }

    public static String getTransitionName(String name) {
        transitionCounter++;
        return "[t" + transitionCounter + "] = " + name;
    }

    public static IndexGenerator getIndexGenerator() {
        return new IndexGenerator();
    }
}
