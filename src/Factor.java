import java.util.*;

public class Factor implements Cloneable{
    private final String factorName; //Factor node name.

    private String[] parents; //Factor node array of parents.

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
    }



    @Override
    public String toString() {
        return "P("+this.factorName + "|" + Arrays.toString(parents) + ") => " + factorTable.toString() + "\n";
    }

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
        String[] factorVars = new String[parents.length + 1];
        factorVars[0] = factorName;
        System.arraycopy(parents, 0, factorVars, 1, factorVars.length - 1);
        return factorVars;
    }

    private void setParents(String[] newParents){
        this.parents = newParents;
    }

    private void setFactorTable(Hashtable<TableKey, Double> newFactorTable){
        this.factorTable = newFactorTable;
    }
}
