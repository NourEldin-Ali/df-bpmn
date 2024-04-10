package it.unibz.deltabpmn.processschema.core;

/**
 * Generates indexes for MCMT transitions used for encoding DAB blocks
 */
final class IndexGenerator {
    private int counter;
    private final String index = "--{%s}";

    public IndexGenerator() {
        this.counter = 1;
    }

    /**
     * @return The next index string
     */
    public String getNext() {
        String result = "";
        switch (this.counter) {
            case 1:
                result = String.format(index, "1st");
                break;
            case 2:
                result = String.format(index, "2nd");
                break;
            case 3:
                result = String.format(index, "3rd");
                break;
            default:
                result = String.format(index, this.counter + "th");
                break;
        }
        counter++;
        return result;
    }
}
