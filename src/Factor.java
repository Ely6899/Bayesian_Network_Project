import java.util.*;
import java.util.Comparator;


/**
 * This class is the main data holder of the factor tables for the algorithms. It holds every essential data for the algorithms to use,
 * with the main one being the factor table, which holds the probabilities of each node, in the correct logical order, in a Hash-Table.
 * Each instance of Factor is build based on the corresponding data of the VariableNode instance.
 */
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
            factorClone.setFactorTable(new Hashtable<>(this.getFactorTable()));
        } catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return factorClone;
    }


    /**
     * Performs comparison between two factor tables in relation to table row count
     * and variable ascii value sum when 2 tables have the same amount of rows.
     */
    public static Comparator<Factor> factorComparator = (factor1, factor2) -> {
        int sizeFactor1 = factor1.getFactorSize();
        int sizeFactor2 = factor2.getFactorSize();
        if(sizeFactor1 != sizeFactor2){
            return sizeFactor1 - sizeFactor2;
        }
        else{
            int asciiFactor1 = factor1.getAsciiSumOfVars();
            int asciiFactor2 = factor2.getAsciiSumOfVars();
            return asciiFactor1 - asciiFactor2;
        }
    };


    /**
     * Performs instantiation of a single factor, by filtering the given var, with the value given in the val parameter.
     * @param var Variable we wish to instantiate in the table.
     * @param val Value of the variable we wish to instantiate. Meaning, filter the variable by given value.
     */
    public void instantiate(String var, String val){
        String[] factorVars = this.getFactorVars();
        int varIndex = 0;

        //Find the given var's index from the String array of vars(Will correspond to value on TableKey in the table).
        for(int i = 0; i < factorVars.length; i++){
            if(factorVars[i].equals(var)){
                varIndex = i;
                break;
            }
        }

        Enumeration<TableKey> keySet = this.factorTable.keys(); //Key-set of the given factor table.
        //Iterate through each key, and delete specific key to table if the value of val doesn't correspond to the value that was given as a parameter.
        //Current key checked.
        while(keySet.hasMoreElements()){
            TableKey currKey = keySet.nextElement();
            if(!currKey.getKeys()[varIndex].equals(val))
                this.factorTable.remove(currKey);
        }

        Hashtable<TableKey, Double> tempTable = new Hashtable<>(); //New temp table for removing evidence column.
        //This loop iterates after instantiation. Removes single value evidence column.
        Enumeration<TableKey> newKeySet = this.factorTable.keys();
        while(newKeySet.hasMoreElements()){
            TableKey currKey = newKeySet.nextElement(); //Current original key
            double probVal = this.factorTable.get(currKey); //Save original probability value to keep correct probability values.
            String[] currVarArr = currKey.getKeys(); //Current key values.
            String[] newKey = new String[currVarArr.length - 1]; //New array of values.
            int insertionTemp = 0;

            for(int i = 0; i < currVarArr.length; i++){
                if(i != varIndex){
                    newKey[insertionTemp++] = currVarArr[i];
                }
            }
            tempTable.put(new TableKey(newKey), probVal);
        }

        //Section of editing the new variables of the new fully instantiated factorTable. Saves are local to given object.
        String[] vars = this.getFactorVars(); //Variables of given factor table.
        String[] newVars = new String[vars.length - 1]; //New variables of given factor table we wish to edit.
        int valInsertionTemp = 0;

        for (int i = 0; i < vars.length; i++) {
            if (i != varIndex)
                newVars[valInsertionTemp++] = vars[i];
        }

        this.setFactorTable(tempTable); //Set new table to current table.
        this.setVars(newVars); //Set new vars to current table.
    }


    /**
     * Checks whenever a given variable is a column in the table.
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

    /*
     * Setters of Factor class
     * */


    /**
     * Sets the parents array of a given Factor with the new newParents parameter.
     * @param newParents String array of new parents.
     */
    private void setParents(String[] newParents){
        this.parents = newParents;
    }


    /**
     * Sets the vars array of a given Factor instance.
     * @param newVars New variables we with to put in given Factor instance.
     */
    public void setVars(String[] newVars){
        this.vars = newVars;
    }


    /**
     * Sets the Factor's factor-table field.
     * @param newFactorTable new factor-table value. Meaning, a new table for the given Factor instance.
     */
    public void setFactorTable(Hashtable<TableKey, Double> newFactorTable){
        this.factorTable = newFactorTable;
    }
}
