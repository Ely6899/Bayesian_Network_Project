import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {
    //Nodes of the xml values as is.
    private final ArrayList<VariableNode> variableNodes;

    //Modified variables, containing a full factor table
    private final ArrayList<Factor> factorNodes;

    //Amount of nodes in the Bayesian network.
    private final int count;


    /**
     * The bayesian network constructor builds the simplistic variable nodes, which act as simple data containers.
     * And then it constructs the Factor nodes, which are the advanced VariableNode nodes, represented with a full
     * factor table.
     * @param xmlFileName Name of the xml file we wish to construct a bayesian network with.
     */
    public BayesianNetwork(String xmlFileName){
        variableNodes = addVariableNodesToNetwork(parseXML(xmlFileName));
        count = variableNodes.size();
        factorNodes = addFactorNodesToNetwork();
    }


    @Override
    public String toString() {
        return factorNodes.toString();
    }


    /**
     * This functions acts as a builder of the document class, which will be able to read through
     * the xml file given as a parameter.
     * @param xmlName Name of a xml file we wish to get data from.
     * @return Document object, capable of reading through a xml file.
     */
    private Document parseXML(String xmlName){
        File xmlFile = new File("src/" + xmlName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        try {
            return builder.parse(xmlFile);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * Generates the network's primitive variable nodes, returns them as an array list.
     * Each item contains the primitive variable, used to store valuable data of a specific node.
     * @param doc Document object which allows parsing of the xml file it was assigned to parse in the constructor.
     * @return Array list of generated primitive variable nodes.
     */
    private ArrayList<VariableNode> addVariableNodesToNetwork(Document doc) {
        NodeList definitionTags = doc.getElementsByTagName("DEFINITION"); //Get all DEFINITION tags.
        NodeList variableTags = doc.getElementsByTagName("VARIABLE"); //Get all VARIABLE tags.
        ArrayList<VariableNode> networkList = new ArrayList<>();

        //Iterate through every element of the xml file.
        for(int temp = 0; temp < variableTags.getLength(); temp++){
            //Get the i-th element, both in definition and variable form respectively.
            Node definitionNode = definitionTags.item(temp);
            Node variableNode = variableTags.item(temp);

            if((definitionNode.getNodeType() == Node.ELEMENT_NODE) && (variableNode.getNodeType() == Node.ELEMENT_NODE)){
                //Get access to nodes data.
                Element definitionElement = (Element) definitionNode;
                Element vaElement = (Element) variableNode;

                //Get all data and save it to a new variable object, and put it into the network list.
                int parentTagCount = definitionElement.getElementsByTagName("GIVEN").getLength(); //Amount of parents of an element.
                int outcomeTagCount = vaElement.getElementsByTagName("OUTCOME").getLength(); //Amount of outcomes of an element.
                String name = definitionElement.getElementsByTagName("FOR").item(0).getTextContent();
                String table = definitionElement.getElementsByTagName("TABLE").item(0).getTextContent(); //Probability values string.
                String[] possibleOutcomes = new String[outcomeTagCount];
                String[] possibleParents = new String[parentTagCount];
                for(int i = 0; i < parentTagCount; i++){
                    possibleParents[i] = definitionElement.getElementsByTagName("GIVEN").item(i).getTextContent();
                }
                for(int j = 0; j < outcomeTagCount; j++){
                    possibleOutcomes[j] = vaElement.getElementsByTagName("OUTCOME").item(j).getTextContent();
                }
                networkList.add(new VariableNode(name, possibleOutcomes, possibleParents, table));
            }
        }
        return networkList;
    }


    /**
     * This complex function adds for each variable node, a new object in the form of Factor, which is
     * almost the same as variable node. The main difference is the full factor table we wish to generate
     * for every variable in the bayesian network.
     * This function works by iterating on an array of indices, representing the values(in order) of the vars
     * of the current node in the iteration.
     * The iteration over the indices happens in the helper function permutateByOneFromLeft().
     * Each array of indices is then translated in the helper function fromIndexToValues().
     * The helper functions work for each row of the table.
     * After all rows were inserted properly, iterate to the next variable and repeat the process.
     * @return Array list of Factor objects, containing mainly a full factor table.
     */
    private ArrayList<Factor> addFactorNodesToNetwork() {
        ArrayList<Factor> factorNodes = new ArrayList<>();
        for(int i = 0; i < count; i++){
            //Preparing important variables for building the table.
            Hashtable<TableKey, Double> factorTable= new Hashtable<>();
            VariableNode currVariable = variableNodes.get(i);

            int colCount = currVariable.getVarCount(); //Each var is a column
            String[] vars = currVariable.getVars(); //Should equal to column count.
            double[] probabilities = currVariable.getProbabilities(); //Probabilities to be inserted according to variable.
            //int rowCount = probabilities.length; //Amount of possible permutations. Depends on product of each column's outcome count.


            int[] indexArr = new int[colCount]; //Arr representing vars value indices.
            int[] outcomeCountArr = new int[colCount]; //Each var outcome count in order. Used for permutation calculations.

            //Build outcome counts array of the factor table.
            for(int j = 0; j < outcomeCountArr.length; j++){
                int currOutcomeCount = getNodeByName(vars[j]).getOutcomeCount();
                outcomeCountArr[j] = currOutcomeCount;
            }

            for (double probability : probabilities) {
                factorTable.put(fromIndexToValues(indexArr, vars), probability); //Table insertion.(Local factor)
                permutateByOneFromLeft(indexArr, outcomeCountArr); //Index permutation added by 1.
            }
            factorNodes.add(new Factor(currVariable.getVariableNodeName(), currVariable.getParents(), factorTable));
        }
        return factorNodes;
    }


    /**
     * This function iterates over all available options it can iterate through, given each
     * var's count of outcomes. The arrays are aligned in a matching order, since order of elements
     * is constant throughout the functions.
     * @param indexArr Array of indices representing pointers to vars values in order.
     * @param outcomeCounts Array of the count of outcomes for each var in order(needed for modulu calculations).
     */
    private void permutateByOneFromLeft(int[] indexArr, int[] outcomeCounts){

        //Index array splitting.
        int[] nameArrHelperString  = Arrays.copyOfRange(indexArr, 0, 1);
        int[] parentHelperArr = Arrays.copyOfRange(indexArr, 1, indexArr.length);

        if(parentHelperArr.length == 0){
            indexArr[0]++;
        }
        else{
            int prevVal = nameArrHelperString[0];
            nameArrHelperString[0] += 1;
            nameArrHelperString[0] %= outcomeCounts[0];

            if(prevVal != 0 && prevVal == outcomeCounts[0] - 1){
                permutateByOne(parentHelperArr, Arrays.copyOfRange(outcomeCounts, 1, outcomeCounts.length));
            }
            indexArr[0] = nameArrHelperString[0];
            System.arraycopy(parentHelperArr, 0, indexArr, 1, indexArr.length - 1);
        }
    }


    /**
     * Numbers won't work for us when we wish to locate probabilities given queries.
     * Therefore, this function helps in creating the proper value array given the index array.
     * @param indexArr Array of indices representing pointers to vars values in order.
     * @param vars The variables of a given variable node.
     * @return TableKey object representing array of truth values(which represents columns in theory).
     */
    private TableKey fromIndexToValues(int[] indexArr, String[] vars){
        String[] values = new String[vars.length];
        for(int i = 0; i < values.length; i++){
            VariableNode currVar = getNodeByName(vars[i]);
            values[i] = currVar.getPossibleOutcomes()[indexArr[i]]; //Index correspondence
        }
        return new TableKey(values);
    }


    /**
     * Returns whenever the names given from a permutation allow fetching the probability value directly.
     * @param names String array of vars from a given permutation.
     * @return true whenever a probability value can be fetched directly(without calculations) from the given names.
     */
    private boolean getProbabilityValue(String[] names){
        Factor factor = getFactorByName(names[0]);
        String[] evidence = new String[names.length - 1];
        System.arraycopy(names, 1 , evidence, 0, names.length - 1);
        return Arrays.equals(factor.getFactorParents(), evidence);
    }


    /**
     * This is the simple deduction function designed to deduct a query's probability in the most 'brute force' approach way.
     * This function also keeps track of the addition and multiplication amounts the algorithm performs to calculate the probability.
     * The algorithm performs this by iterating through the possible permutations the query holds, which depends on the
     * amount of non-vars which were not passed in the query.
     * The Iteration of those permutations works with an appropriate array representing the index positioning of the non-vars
     * values. The iterating of those pointers is done with the helper function permutateByOne().
     * Each permutation of the non-vars is kept in a table, and is united with the vars table, which also iterates in values for normalization
     * purposes. The union of the vars and non-vars tables is done with the helper function tableUnion().
     * For each valid full permutation, we multiply each var in the permutation, with the given parents each has, while maintaining the truth
     * values for each var multiplied, throughout the entire multiplication. This is done in the helper function getValueFromGivenPermutation(),
     * which also uses the helper function getProbabilityValue() for fetching the specific probability value given the vars values.
     * In the end, after each iteration is done, normalize, and print the probability value, followed by the number of addition and multiplications,
     * respectively.
     * @param names Names of the given query.
     * @param truthValsArr The values given with the names in the same order.
     */
    public void func1(String[] names, String[] truthValsArr) {
        //Decimal format for correct answer printing.
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");

        //If query can be obtainable directly
        if (getProbabilityValue(names)) {
            Hashtable<TableKey, Double> factorTable = getFactorByName(names[0]).getFactorTable();
            TableKey tableKey = new TableKey(truthValsArr);
            System.out.println(factorTable.get(tableKey));
        } else {
            List<String> nameList = Arrays.asList(names);
            String[] nonVars = new String[count - names.length]; //Non-vars array.
            int additionPermutationCount = 1;
            int insertionTemp = 0;

            //Loop iterates through all variable nodes. If a node isn't in the list, it is considered nonVar.
            //This loop builds the nonVars array.
            for (VariableNode currVariable : variableNodes) {
                if (!nameList.contains(currVariable.getVariableNodeName())) {
                    additionPermutationCount *= currVariable.getOutcomeCount();
                    nonVars[insertionTemp++] = currVariable.getVariableNodeName();
                }
            }

            //int additionCountFromFormula = ((additionPermutationCount - 1) * getNodeByName(names[0]).getOutcomeCount()) + 1;//Last addition is normalization addition.
            //int multiCountConstFromFormula = (count - 1) * additionPermutationCount * getNodeByName(names[0]).getOutcomeCount();
            double numerator = 0;
            double secondaryOptions = 0;
            int additionCount = 0, multiCount = 0;
            /*
             Storing all outcomes of the query main name which allows iterating through all possible probabilities
             of it, since it requires them all for normalization.
             */
            String[] queryNameOutcomes = getNodeByName(names[0]).getPossibleOutcomes();

            //This loop deals with building the proper vars-values table. Also checks whenever the permutation is the numerator one required.
            for (String queryNameOutcome : queryNameOutcomes) {
                Hashtable<String, String> evidenceTable = new Hashtable<>();
                boolean numeratorFlag = false;
                for (int row = 0; row < names.length; row++) {
                    //Insertion of the main var.
                    if (row == 0) {
                        evidenceTable.put(names[row], queryNameOutcome);
                    }
                    //Insertion of the rest of the vars(Non-main ones)
                    else {
                        evidenceTable.put(names[row], truthValsArr[row]); //Insertion values from parents are constant from given query
                    }
                }

                //Checks whenever current iteration relates to the permutation needed to add to the numerator.
                if (queryNameOutcome.equals(truthValsArr[0]))
                    numeratorFlag = true;

                //This part of the code handles addition of all possible permutations.
                int[] outcomeIndices = new int[nonVars.length];
                int[] outcomeCount = new int[nonVars.length];

                //Get outcome counts of non-vars in respective order.
                for (int i = 0; i < nonVars.length; i++) {
                    outcomeCount[i] = getNodeByName(nonVars[i]).getOutcomeCount();
                }

                //Iterate through all permutations
                for (int i = 0; i < additionPermutationCount; i++) {
                    Hashtable<String, String> nonEvidenceTable = new Hashtable<>();
                    for (int j = 0; j < nonVars.length; j++) {
                        VariableNode currNode = getNodeByName(nonVars[j]);
                        nonEvidenceTable.put(currNode.getVariableNodeName(), currNode.getPossibleOutcomes()[outcomeIndices[j]]); //Insert respective outcome value.
                    }
                    if (numeratorFlag)
                        numerator += getValueFromGivenPermutation(tableUnion(evidenceTable, nonEvidenceTable));
                    else
                        secondaryOptions += getValueFromGivenPermutation(tableUnion(evidenceTable, nonEvidenceTable));

                    permutateByOne(outcomeIndices, outcomeCount);//After each iteration, permutate the outcome index array by 1.
                    multiCount += (count - 1);
                    additionCount++;
                }
            }
            double normalizationAlpha = numerator + secondaryOptions;
            System.out.println(decimalFormat.format(numerator / normalizationAlpha)+","+(additionCount - 1)+","+multiCount);
        }
    }


    /**
     * Iterates the given index array by one. Iteration is pointed from the right to the left.
     * @param indexArr Array of indices representing pointers to vars values in order.
     * @param outcomeCounts Array of the count of outcomes for each var in order(needed for modulu calculations).
     */
    private void permutateByOne(int[] indexArr, int[] outcomeCounts){
        int indexArrLength = indexArr.length;
        int prevVal = indexArr[indexArrLength - 1];
        indexArr[indexArrLength - 1] += 1;
        indexArr[indexArrLength - 1] %= outcomeCounts[indexArrLength - 1];
        boolean nextSwitch = true;
        for(int j = indexArr.length - 1; j >=1; j--){
            if(nextSwitch &&(indexArr[j] == 0 && prevVal == outcomeCounts[j] - 1 && prevVal != 0)){
                prevVal = indexArr[j - 1];
                indexArr[j - 1] += 1;
                indexArr[j - 1] %= outcomeCounts[j - 1];
                nextSwitch = indexArr[j - 1] == 0 && prevVal == outcomeCounts[j - 1] - 1 && prevVal != 0;
            }
        }
    }


    /**
     * This function is used to unite 2 Hash-tables into 1 Hash-table.
     * The function unites the first table with the second, and returns the table assigned to the first table parameter, in its new form.
     * @param table1 Hash-table of vars and their values.
     * @param table2 Hash-table of vars and their values(Usually the vars are non-evidence).
     * @return A new united table of vars and values, representing a permutation.
     */
    private Hashtable<String, String> tableUnion(Hashtable<String, String> table1, Hashtable<String, String> table2){
        Enumeration<String> keySetOfTable2 = table2.keys();
        for(int i = 0; i < table2.keySet().size(); i++){
            String currKey = keySetOfTable2.nextElement();
            table1.put(currKey, table2.get(currKey));
        }
        return table1;
    }


    /**
     * This function Calculates the probability value of the given permutation, represented by a Hash-table.
     * @param permutationTable Hash-table representing a permutation.
     * @return probability value of the given permutation.
     */
    private double getValueFromGivenPermutation(Hashtable<String, String> permutationTable){
        double result = 1;
        Enumeration<String> keySet = permutationTable.keys();
        for(int i = 0; i < permutationTable.size(); i++){
            String variable = keySet.nextElement();
            VariableNode currVar = getNodeByName(variable);
            String[] vars = new String[currVar.getVarCount()];
            vars[0] = variable;
            //Get the relevant vars array from the given variable name in the iteration.
            for(int j = 1; j < vars.length; j++){
                vars[j] = currVar.getParents()[j - 1];
            }

            if(getProbabilityValue(vars)){
                String[] varsTruthTable = new String[vars.length];//Factor key to look for

                //Get the truth values of each var, according to the input of the permutation table.
                for(int m = 0; m < vars.length; m++){
                    varsTruthTable[m] = permutationTable.get(vars[m]);
                }
                result *= getFactorByName(variable).getFactorTable().get(new TableKey(varsTruthTable));//Multiply probability.
            }
        }
        return result;
    }


    /**
     * func2() calculates the probability of a given query, and its values by performing variable elimination on the factors,
     * which greatly reduces the number of calculations required to reach the answer.
     * @param names Names of the given query.
     * @param truthTableArr The values given with the names in the same order.
     */
    public void func2(String[] names, String[] truthTableArr){
        //Decimal format for correct answer printing.
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");

        //Relevant data gathering.
        ArrayList<Factor> tempFactors = new ArrayList<>();
        for(Factor factor: factorNodes){
            try {
                tempFactors.add((Factor) factor.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> nameList = Arrays.asList(names);
        String[] hidden = new String[count - names.length]; //Non-vars array.
        int insertionTemp = 0;

        //Loop iterates through all variable nodes. If a node isn't in the list, it is considered hidden.
        //This loop builds the hidden array.
        for (VariableNode currVariable : variableNodes) {
            if (!nameList.contains(currVariable.getVariableNodeName())) {
                hidden[insertionTemp++] = currVariable.getVariableNodeName();
            }
        }

        String[] evidence = new String[names.length - 1];
        System.arraycopy(names, 1, evidence, 0, evidence.length);
        String[] query = new String[1];
        query[0] = names[0];

        //Loop of instantiations.
        for(int temp = 0; temp < evidence.length; temp++){
            String checkedVar = evidence[temp]; //Variable we with to instantiate in tables
            String checkedVarValue = truthTableArr[temp + 1]; //Value of the variable we wish to keep.
            //Iterate through all factors of given evidence to filter.
            for (Factor currFactor : tempFactors) {
                if (currFactor.varInFactor(checkedVar))
                    currFactor.instantiate(checkedVar, checkedVarValue);
            }
        }
        Arrays.sort(hidden); //Sort hidden variables(For variable elimination alphabetical order)

        for(String hiddenString: hidden) {
            ArrayList<Factor> hiddenFactors = new ArrayList<>();

            //Find all factors that contain the hidden evidence that will be eliminated
            for (Factor currFactor : tempFactors) {
                if (currFactor.varInFactor(hiddenString))
                    hiddenFactors.add(currFactor);
            }

            Collections.sort(hiddenFactors, Factor.factorComparator); //Sort by table size(ascending)
        }
    }

    public void func3(String[] names, String[] truthTableArr){
        if(getProbabilityValue(names)){
            Hashtable<TableKey, Double> factorTable = getFactorByName(names[0]).getFactorTable();
            TableKey tableKey = new TableKey(truthTableArr);
            System.out.println(factorTable.get(tableKey));
        }
    }



    /*
    * Getters of BayesianNetwork class
    * */

    /**
     * Returns a variable node by specifying its index on the list.
     * @param index Index of the list to fetch the variable node.
     * @return A variable node in index i of the list.
     * @throws IndexOutOfBoundsException if the index is not in the list.
     */
    public VariableNode getNodeByIndex(int index) throws IndexOutOfBoundsException{
        try{
            return this.variableNodes.get(index);
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }


    /**
     * Returns a variable node by specifying its name by iterating through
     * the entire list until we find a matching name.
     * @param name Name of the variable we wish to find on the list.
     * @return The variable node we wanted to find.
     */
    public VariableNode getNodeByName(String name){
        for(int i = 0; i < count; i++){
            if(getNodeByIndex(i).getVariableNodeName().equals(name))
                return getNodeByIndex(i);
        }
        return null;
    }


    /**
     * Gets the parents of a given variable node in a form of the parent nodes
     * in an array.
     * @param variable A variable node which we need its parents from.
     * @return An array of variable nodes, which represent the parents of the node
     * we gave in the input.
     */
    private VariableNode[] getParentsOfNode(VariableNode variable){
        String[] parentsString = variable.getParents();
        VariableNode[] parents = new VariableNode[variable.getParentCount()];
        for(int i = 0; i < variable.getParentCount(); i++){
            parents[i] = getNodeByName(parentsString[i]);
        }
        return parents;
    }


    /**
     * This function returns the Factor object located in the index which was given.
     * @param index Index of the array list.
     * @return Factor object in the index given.
     * @throws IndexOutOfBoundsException Whenever the array list doesn't contain given index.
     */
    public Factor getFactorByIndex(int index) throws IndexOutOfBoundsException{
        try{
            return factorNodes.get(index);
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }


    /**
     * Returns the factor object corresponding to the name that was asked.
     * @param name Name of the factor object we wish to find in the array list.
     * @return Returns the factor object when it was found by name comparisons in iteration. Returns null otherwise.
     */
    public Factor getFactorByName(String name) {
        name = name.replace(" ", "");
        for (int i = 0; i < factorNodes.size(); i++) {
            if (name.equals(factorNodes.get(i).getFactorName()))
                return getFactorByIndex(i);
        }
        return null;
    }


}
