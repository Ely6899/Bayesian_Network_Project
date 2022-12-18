import java.util.Arrays;


/**
 * TableKey class is a class which represents the key of every factor table.
 * It encapsulates an array of strings which represents the values of the variables in a specific row in order.
 */
public class TableKey {
    private final String[] keys; //Represents an array of query truth values. Used as a key for factor tables.

    /**
     * Initialize the parameter by giving an array of truth values.
     * @param keys String array of truth values we wish to store in a specified TableKey object.
     */
    public TableKey(String[] keys){
        this.keys = keys;
    }

    @Override
    public String toString() {
        return Arrays.toString(keys);
    }

    /*
    * This documentation refers to both equals() and hashCode() methods!:
    * equals() is overridden in order to allow the program to properly compare between TableKey objects.
    * hashCode() is overridden in order to allow fetching hashMap values by specifying values of TableKey.
    * */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableKey tableKey = (TableKey) o;
        return Arrays.equals(keys, tableKey.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }


    /**
     * Gets the keys array of the TableKey instance.
     * @return Array of strings representing truth values of a factor table row.
     */
    public String[] getKeys(){
        return this.keys;
    }
}
