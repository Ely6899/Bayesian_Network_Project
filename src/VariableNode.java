import java.util.Arrays;

public class VariableNode {
    private final String nodeName; //Node main name

    private final String[] possibleOutcomes; //Possible outcomes of the node.

    private final String[] parents; //The parents of the node. Kept as string names.

    private final double[] probabilityValues; //Array of probability values.

    private final int outcomeCount, parentCount; //Counters of outcomes and parents, respectively.

    /**
     * Builds a single primitive variable node. Stores outcome and parent counts as well.
     * @param nodeName Name of the node.
     * @param possibleOutcomes The possible outcomes for the node.
     * @param parents The parents of the node.
     * @param stringValues The probability values given to the node, given its parents.
     */
    public VariableNode(String nodeName, String[] possibleOutcomes, String[] parents, String stringValues){
        this.nodeName = nodeName;
        this.possibleOutcomes = possibleOutcomes;
        this.parents = parents;
        this.probabilityValues = parseProbabilityValues(stringValues);
        this.outcomeCount = possibleOutcomes.length;
        this.parentCount = parents.length;
    }

    @Override
    public String toString() {
        return "\n{" +
                "nodeName='" + nodeName + '\'' +
                ", outcomeCount=" + outcomeCount +
                ", possibleOutcomes=" + Arrays.toString(possibleOutcomes) +
                ", parentCount=" + parentCount +
                ", parents=" + Arrays.toString(parents) +
                ", Number of rows=" + getNumberOfProbabilities() +
                ", probabilityValues=" + Arrays.toString(probabilityValues) +
                "}\n";
    }

    /**
     * Parses the string of double values into an array of double values, in the original order of the xml.
     * @param stringValues String of double values, each separated by a single space.
     * @return An array of doubles, each containing a double value from the xml file, in the same order.
     */
    private double[] parseProbabilityValues(String stringValues){
        String[] stringValueArr = stringValues.split(" ");
        double[] values = new double[stringValueArr.length];
        for(int i = 0; i < values.length; i++){
            values[i] = Double.parseDouble(stringValueArr[i]);
        }
        return values;
    }


    /*
    * Getters of VariableNode class
    */

    /**
     * Return node name.
     * @return Node name of given node.
     */
    public String getVariableNodeName() {
        return nodeName;
    }

    /**
     * Returns the array of possible outcomes.
     * @return Array of possible outcomes of given node.
     */
    public String[] getPossibleOutcomes(){
        return possibleOutcomes;
    }

    /**
     * Returns the array of the parents.
     * @return Array of parents of the given Node.
     */
    public String[] getParents(){
        return this.parents;
    }

    /**
     * Returns all variables. both name and parents, in an array of strings.
     * @return All variables in an array of strings.
     */
    public String[] getVars(){
        String[] vars = new String[getVarCount()];
        vars[0] = nodeName;
        System.arraycopy(parents, 0, vars, 1, vars.length - 1);
        return vars;
    }

    /**
     * Returns the amount of parents
     * @return Amount of parents of the given node.
     */
    public int getParentCount() {
        return parentCount;
    }

    /**
     * Returns the amount of outcomes the node can have.
     * @return Amount of possible outcomes of given node.
     */
    public int getOutcomeCount() {
        return outcomeCount;
    }

    /**
     * Returns the amount of variables the given node has.
     * @return Amount of variables the given node has.
     */
    public int getVarCount(){
        return parentCount + 1;
    }

    /**
     * Returns the array of probabilities
     * @return Array of probabilities of the given node(Double values).
     */
    public double[] getProbabilities(){
        return probabilityValues;
    }

    /**
     * Returns a probability value, given an index of the array.
     * @param index Index of a probability value.
     * @return Probability value at given index.
     * @throws IndexOutOfBoundsException Whenever the index doesn't exist in array.
     */
    public double getProbabilityAtIndex(int index) throws IndexOutOfBoundsException{
        try{
            return probabilityValues[index];
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public int getNumberOfProbabilities(){
        return probabilityValues.length;
    }


}
