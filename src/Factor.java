import java.util.Arrays;
import java.util.Hashtable;

public class Factor {
    private final String factorName; //Factor node name.

    private final String[] parents; //Factor node array of parents.

    private final Hashtable<TableKey, Double> factorTable; //Factor node factor table!

    /**
     * Builds the full factor instance.
     * @param name Name of the factor.
     * @param parents Parents of the factor(Given values).
     * @param factorTable Full factor table(Built in BayesianNetwork class).
     */
    public Factor(String name, String[] parents, Hashtable<TableKey, Double> factorTable){
        this.factorName = name;
        this.parents = parents;
        this.factorTable = factorTable;
    }

    @Override
    public String toString() {
        return "P("+this.factorName + "|" + Arrays.toString(parents) + ") => " + factorTable.toString() + "\n";
    }

    /*
    * Getters of Factor class
    * */

    /**
     * Return the name of the factor given.
     * @return Name of the factor.
     */
    public String getFactorName(){
        return this.factorName;
    }

    /**
     * Returns the array of parents represented in strings.
     * @return Array of the parents of the given node.
     */
    public String[] getFactorParents(){
        return this.parents;
    }

    /**
     * Returns the hashTable of the given node.
     * @return HashTable representing the full factor.
     */
    public Hashtable<TableKey, Double> getFactorTable(){
        return this.factorTable;
    }
}
