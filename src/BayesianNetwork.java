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
            int rowCount = 1; //Amount of possible permutations. Depends on product of each column's outcome count.
            String[] vars = currVariable.getVars(); //Should equal to column count.
            double[] probabilities = currVariable.getProbabilities(); //Probabilities to be inserted according to variable.

            int[] indexArr = new int[colCount]; //Arr representing vars value indices.
            int[] outcomeCountArr = new int[colCount]; //Each var outcome count in order. Used for permutation calculations.

            for(int j = 0; j < outcomeCountArr.length; j++){
                int currOutcomeCount = getNodeByName(vars[j]).getOutcomeCount();
                outcomeCountArr[j] = currOutcomeCount;
                rowCount *= currOutcomeCount;
            }

            for(int j = 0; j < rowCount; j++){
                factorTable.put(fromIndexToValues(indexArr, vars), probabilities[j]); //Table insertion.(Local factor)
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
     * @return Iterated indexArr.
     */
    private int[] permutateByOneFromLeft(int[] indexArr, int[] outcomeCounts){
        int prevVal = indexArr[0];

        indexArr[0] += 1;
        indexArr[0] %= outcomeCounts[0];

        for(int i = 0; i < indexArr.length - 1; i++){
            int lastIndexPointer = indexArr.length - 1 - i;
            if(indexArr[i] == 0 && prevVal != 0){
                prevVal = indexArr[lastIndexPointer];
                indexArr[lastIndexPointer] += 1;
                indexArr[lastIndexPointer] %= outcomeCounts[lastIndexPointer];
            }
        }
        return indexArr;
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



    //Finds whenever you can infer probability without calculations.
    private boolean getProbabilityValue(String[] names){
        Factor factor = getFactorTableByName(names[0]);
        String[] evidence = new String[names.length - 1];
        System.arraycopy(names, 1 , evidence, 0, names.length - 1);
        return Arrays.equals(factor.getFactorParents(), evidence);
    }


    public void func1(String[] names, String[] truthTableArr) {
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");

        //If query can be obtainable directly
        if (getProbabilityValue(names)) {
            Hashtable<TableKey, Double> factorTable = getFactorTableByName(names[0]).getFactorTable();
            TableKey tableKey = new TableKey(truthTableArr);
            System.out.println(factorTable.get(tableKey));
        } else {
            List<String> nameList = Arrays.asList(names);
            String[] nonEvidence = new String[count - names.length]; //Non-evidence array.
            int additionPermutationCount = 1;
            int insertionTemp = 0;
            for (int i = 0; i < variableNodes.size(); i++) {
                VariableNode currVariable = variableNodes.get(i);
                if (!nameList.contains(currVariable.getVariableNodeName())) {
                    additionPermutationCount *= currVariable.getOutcomeCount();
                    nonEvidence[insertionTemp++] = currVariable.getVariableNodeName();
                }
            }
            int additionCount = ((additionPermutationCount - 1) * getNodeByName(names[0]).getOutcomeCount()) + 1;//Last addition is normalization addition.
            int multiCount = (count - 1) * additionPermutationCount * getNodeByName(names[0]).getOutcomeCount();
            double numerator = 0;
            double secondaryOptions = 0;

            String[] queryNameOutcomes = getNodeByName(names[0]).getPossibleOutcomes();

            for(int nameOutcomeIndex = 0; nameOutcomeIndex < queryNameOutcomes.length; nameOutcomeIndex++){
                Hashtable<String, String> evidenceTable = new Hashtable<>();
                if(nameOutcomeIndex == 0){
                    //The constant query values of the given evidence variables.
                    for (int i = 0; i < names.length; i++) {
                        evidenceTable.put(names[i], truthTableArr[i]);
                    }
                }
                else{
                    for(int i = 0; i < names.length; i++){
                        if(i == 0)
                            evidenceTable.put(names[i], queryNameOutcomes[nameOutcomeIndex]);
                        else{
                            evidenceTable.put(names[i], truthTableArr[i]);
                        }
                    }
                }
                //This part of the code handles addition of all possible permutations.
                int[] outcomeIndices = new int[nonEvidence.length];
                int[] outcomeCount = new int[nonEvidence.length];
                for (int i = 0; i < nonEvidence.length; i++) {
                    outcomeCount[i] = getNodeByName(nonEvidence[i]).getOutcomeCount();
                }

                for (int i = 0; i < additionPermutationCount; i++) {
                    Hashtable<String, String> nonEvidenceTable = new Hashtable<>();
                    for (int j = 0; j < nonEvidence.length; j++) {
                        VariableNode currNode = getNodeByName(nonEvidence[j]);
                        nonEvidenceTable.put(currNode.getVariableNodeName(), currNode.getPossibleOutcomes()[outcomeIndices[j]]);
                    }
                    if(nameOutcomeIndex == 0)
                        numerator += getValueFromGivenPermutation(tableUnion(evidenceTable, nonEvidenceTable));
                    else{
                        secondaryOptions += getValueFromGivenPermutation(tableUnion(evidenceTable, nonEvidenceTable));
                    }
                    permutateByOne(outcomeIndices, outcomeCount);
                }
            }
            double normalizationAlpha = numerator + secondaryOptions;
            System.out.println(decimalFormat.format(numerator / normalizationAlpha)+","+additionCount+","+multiCount);

        }
    }


    private int[] permutateByOne(int[] indexArr, int[] outcomeCounts){
        int indexArrLength = indexArr.length;
        int prevVal = indexArr[indexArrLength - 1];
        indexArr[indexArrLength - 1] += 1;
        indexArr[indexArrLength - 1] %= outcomeCounts[indexArrLength - 1];
        for(int j = indexArr.length - 1; j >=1; j--){
            if(indexArr[j] == 0 && prevVal != 0){
                prevVal = indexArr[j - 1];
                indexArr[j - 1] += 1;
                indexArr[j - 1] %= outcomeCounts[j - 1];
            }
        }
        return indexArr;
    }

    private Hashtable<String, String> tableUnion(Hashtable<String, String> table1, Hashtable<String, String> table2){
        Enumeration<String> keySetOfTable2 = table2.keys();
        for(int i = 0; i < table2.keySet().size(); i++){
            String currKey = keySetOfTable2.nextElement();
            table1.put(currKey, table2.get(currKey));
        }
        return table1;
    }

    private double getValueFromGivenPermutation(Hashtable<String, String> permutationTable){
        double result = 1;
        Enumeration<String> keySet = permutationTable.keys();
        for(int i = 0; i < permutationTable.size(); i++){
            String variable = keySet.nextElement();
            VariableNode currVar = getNodeByName(variable);
            String[] multiParamVarCount = new String[currVar.getVarCount()];
            multiParamVarCount[0] = variable;
            for(int j = 1; j < multiParamVarCount.length; j++){
                multiParamVarCount[j] = currVar.getParents()[j - 1];
            }
            if(getProbabilityValue(multiParamVarCount)){
                String[] varsTruthTable = new String[multiParamVarCount.length];//Factor key to look for
                for(int m = 0; m < multiParamVarCount.length; m++){
                    varsTruthTable[m] = permutationTable.get(multiParamVarCount[m]);
                }
                result *= getFactorTableByName(variable).getFactorTable().get(new TableKey(varsTruthTable));
            }
        }
        return result;
    }


    public void func2(String[] names, String[] truthTableArr){
        if(getProbabilityValue(names)){
            Hashtable<TableKey, Double> factorTable = getFactorTableByName(names[0]).getFactorTable();
            TableKey tableKey = new TableKey(truthTableArr);
            System.out.println(factorTable.get(tableKey));
        }
    }

    public void func3(String[] names, String[] truthTableArr){
        if(getProbabilityValue(names)){
            Hashtable<TableKey, Double> factorTable = getFactorTableByName(names[0]).getFactorTable();
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

    public Factor getFactorTableByIndex(int index) throws RuntimeException{
        try{
            return factorNodes.get(index);
        } catch (IndexOutOfBoundsException e){
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    public Factor getFactorTableByName(String name) {
        name = name.replace(" ", "");
        for (int i = 0; i < factorNodes.size(); i++) {
            if (name.equals(factorNodes.get(i).getFactorName()))
                return getFactorTableByIndex(i);
        }
        return null;
    }


}
