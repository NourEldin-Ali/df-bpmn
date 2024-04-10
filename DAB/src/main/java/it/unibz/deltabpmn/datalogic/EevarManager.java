package it.unibz.deltabpmn.datalogic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import it.unibz.deltabpmn.dataschema.elements.Sort;
import it.unibz.deltabpmn.exception.EevarOverflowException;

import java.util.Collection;
import java.util.Map;


/**
 * A class for managing eevars.
 * Used also to control the number of eevars as the current version of MCMT doesn't support more than 10 eevars.
 */
public final class EevarManager {
    private static Multimap<Sort, String> eevarList = HashMultimap.create();
    private static int maxSize = 10;

    private enum EEVARS{a,b,c,d,e,f,g,h,o,p}; //a predefined set of eevars

    /**
     * A method that remembers an eevar in the current DAB specification and returns its string representation.
     *
     * @param sort The eevar sort.
     * @return The string representation of the eevar.
     * @throws EevarOverflowException Appears when more than 10 eevars have been used in the specification.
     */
    public static String addEevar(Sort sort) throws EevarOverflowException {
        // check the size of the eevar_list
        if (eevarList.size() == maxSize) {
            throw new EevarOverflowException("The limit of the allowed number of eevars has been reached!"); // throw exception
        } else {
            //ToDo: this supports a "finite" provision of eevars (this is fine as soon as we support only 10!!!)
            String eevarToReserve =  EEVARS.values()[eevarList.size()].name();
            eevarList.put(sort, eevarToReserve);
            return eevarToReserve;
        }
    }

    /**
     * @param sort The eevar sort
     * @return The collection of sorted eevars.
     */
    public static Collection<String> getEevarWithSort(Sort sort) {
        return eevarList.get(sort);
    }

    /**
     * @return A string containing all eevar declarations in MCMT.
     */
    public static String generateEevarDeclarations() {
        String result = "";
        for (Map.Entry<Sort, String> entry : eevarList.entries()) {
            result += ":eevar " + entry.getValue() + " " + entry.getKey().getSortName() + "\n";
        }
        return result;
    }

    /**
     * A method returns a string representation of the eevar sort given only the name of the eevar.
     *
     * @param name The eevar variable name.
     * @return The eevar sort.
     */
    public static Sort getSortByVariable(String name) {
        Multimap<String, Sort> inverse = Multimaps.invertFrom(eevarList,
                ArrayListMultimap.<String, Sort>create());
        return inverse.get(name).iterator().next();
    }


}
