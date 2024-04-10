package it.unibz.deltabpmn.verification.mcmt;

/**
 * Class used for processing names of various elements in accordance with the naming policy currently adopted in MCMT.
 */
public final class NameManager {

    /**
     * Removes "-" from names, replaces next character with its upper case variant, capitalises the first character of the string.
     * @param name
     * @return
     */
    public static String normaliseName(String name) {
        int ind = name.indexOf("-");
        while (ind != -1) {
            String toReplace = name.substring(ind, ind + 2);
            name = name.replaceAll(toReplace, toReplace.toUpperCase().substring(1));
            ind = name.indexOf("-");
        }
        ind = name.indexOf("_");
        while (ind != -1) {
            String toReplace = name.substring(ind, ind + 2);
            name = name.replaceAll(toReplace, toReplace.toUpperCase().substring(1));
            ind = name.indexOf("_");
        }
        return capitalize(name);
    }

    /**
     * Removes "-" from names and replaces next character with its upper case variant.
     * @param name
     * @return
     */
    public static String normaliseDataElementName(String name) {
        int ind = name.indexOf("-");
        while (ind != -1) {
            String toReplace = name.substring(ind, ind + 2);
            name = name.replaceAll(toReplace, toReplace.toUpperCase().substring(1));
            ind = name.indexOf("-");
        }
        ind = name.indexOf("_");
        while (ind != -1) {
            String toReplace = name.substring(ind, ind + 2);
            name = name.replaceAll(toReplace, toReplace.toUpperCase().substring(1));
            ind = name.indexOf("_");
        }
        return name;
    }

    public static String capitalize(String name){
        return Character.toUpperCase(name.charAt(0))+name.substring(1);
    }
}
