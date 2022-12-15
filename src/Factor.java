import java.util.*;
import java.util.Comparator;

public class Factor implements Cloneable{
    private final String factorName; //Factor node name.

    private String[] parents; //Factor node array of parents.

    private String[] vars;

    private Hashtable<TableKey, Double> factorTable; //Factor node factor table!

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
        this.vars = new String[parents.length + 1];
        this.vars[0] = name;
        System.arraycopy(parents, 0, this.vars, 1, this.vars.length - 1);
    }


    @Override
    public String toString() {
        return "P("+this.factorName + "|" + Arrays.toString(parents) + ") => " + factorTable.toString() + "\n";
    }


    /**
     * Clones a factor object by value.
     * @return The same object, cloned by value.
     * @throws CloneNotSupportedException Whenever object doesn't support the cloning method.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        Factor factorClone = null;
        try{
            factorClone = (Factor) super.clone();
            factorClone.setParents(this.getFactorParents().clone());
            //noinspection unchecked
            factorClone.setFactorTable((Hashtable<TableKey, Double>) this.getFactorTable().clone());
        } catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return factorClone;
    }


    /**
     * Performs comparison between two factor tables in relation to table row count.
     */
    public static Comparator<Factor> factorComparator = (factor1, factor2) -> {
        int sizeFactor1 = factor1.getFactorSize();
        int sizeFactor2 = factor2.getFactorSize();

        int asciiFactor1 = factor1.getAsciiSumOfVars();
        int asciiFactor2 = factor2.getAsciiSumOfVars();

        //Ascending order
        return (sizeFactor1 - sizeFactor2) + (asciiFactor1 - asciiFactor2);
    };


    /**
     * Performs instantiation of a single factor, by filtering the given var, with the value given in the val parameter.
     * @param var Variable we wish to instantiate.
     * @param val Value of the variable we wish to instantiate. Meaning, filter the variable by given value.
     */
    public void instantiate(String var, String val){
        String[] factorVars = this.getFactorVars();
        int varIndex = 0;

        //Find the given var's index from the String of vars(Will correspond to value on TableKey in the table).
        for(int i = 0; i < factorVars.length; i++){
            if(factorVars[i].equals(var)){
                varIndex = i;
                break;
            }
        }

        Enumeration<TableKey> keySet = this.factorTable.keys(); //Key-set of the given factor table.
        //Iterate through each key, and add specific key to table if the value of val corresponds to the value that was given as a parameter.
        //Current key checked.
        while(keySet.hasMoreElements()){
            TableKey currKey = keySet.nextElement();
            if(!currKey.getKeys()[varIndex].equals(val))
                this.factorTable.remove(currKey);
        }

    }


    /**
     * Checks whenever
     * @param var Variable to search.
     * @return true whenever the variable is located in given Factor instance. false otherwise.
     */
    public boolean varInFactor(String var){
        String[] factorVars = getFactorVars();
        for (String factorVar : factorVars) {
            if (factorVar.equals(var))
                return true;
        }
        return false;
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


    /**
     * Returns a united array of all the variables in the given factor instance.
     * @return The array of all the given factor's variables.
     */
    public String[] getFactorVars(){
        return this.vars;
    }


    /**
     * Returns the number of rows of the factor table.
     * @return The number of rows of the factor table.
     */
    public int getFactorSize(){
        return this.factorTable.size();
    }


    /**
     * Sums the ascii value of all variables in the factor.
     * @return The sum of each ascii value of a variable.
     */
    public int getAsciiSumOfVars(){
        String[] vars = this.getFactorVars();
        int sum = 0;

        for(String var : vars){
            for(int i = 0; i < var.length(); i++){
                sum += var.charAt(i);
            }
        }
        return sum;
    }


    /**
     * Sets the parents array of a given Factor with the new newParents parameter.
     * @param newParents String array of new parents.
     */
    private void setParents(String[] newParents){
        this.parents = newParents;
    }

    public void setVars(String[] newVars){
        this.vars = newVars;
    }


    /**
     * Sets the Factor's factor-table field.
     * @param newFactorTable new factor-table value.
     */
    public void setFactorTable(Hashtable<TableKey, Double> newFactorTable){
        this.factorTable = newFactorTable;
    }
}
