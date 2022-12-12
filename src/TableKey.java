import java.util.Arrays;

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

    public String[] getKeys(){
        return this.keys;
    }
}
