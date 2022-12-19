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


/**
 * This is the primary class of the program, which builds and holds all the bayesian network data(Both Variable and Factor) as lists.
 * In addition, it performs all algorithms, simple deduction(func1) and variable elimination(func2).
 * It also contains helper functions which help the algorithms to perform correctly.
 * At the start of the program, it also contains a function which parses the xml file given in the input of BayesianNetwork constructor.
 * The BayesianNetwork constructor performs the entire network building.
 */
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
        File xmlFile = new File(xmlName);
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

                //Iterate though all possible parents and add them to the data.
                for(int i = 0; i < parentTagCount; i++){
                    possibleParents[i] = definitionElement.getElementsByTagName("GIVEN").item(i).getTextContent();
                }

                //Iterate though all possible outcomes and add them to the data.
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
     * This function fits the logic of building the factor tables, therefore it only works for building the factors.
     * The function works for a single permutation given. It doesn't iterate through all possibilities.
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
                permutateByOne(parentHelperArr, Arrays.copyOfRange(outcomeCounts, 1, outcomeCounts.length)); //Use regular logic when necessary.
            }
            indexArr[0] = nameArrHelperString[0];
            System.arraycopy(parentHelperArr, 0, indexArr, 1, indexArr.length - 1); //Save permutation changes.
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
    private boolean IsProbabilityValueDirect(String[] names){
        Factor factor = getFactorByName(names[0]);
        String[] factorParents = factor.getFactorParents();
        ArrayList<String> parentList = new ArrayList<>();
        Collections.addAll(parentList, factorParents); //Add to arraylist all actual parents of relevant factor.

        String[] queryParents = new String[names.length - 1];
        System.arraycopy(names, 1, queryParents, 0, names.length - 1); //Save given query parents in separate array.

        boolean allFound = true;
        for(String givenParent: queryParents){
            allFound = parentList.contains(givenParent);
            if(!allFound)
                break;
        }

        return allFound && queryParents.length == factorParents.length; //If only part of the parents are in the query, return false.
    }


    /**
     * This functions handles fetching directly the probability value of a given query, if it is possible.
     * @param names Variables of given query.
     * @param truthValsArr Truth values of given query.
     * @return Probability value of query directly from the factor table.
     */
    private double getDirectProbability(String[] names, String[] truthValsArr){
        Factor factor = getFactorByName(names[0]);
        Hashtable<TableKey, Double> factorTable = getFactorByName(names[0]).getFactorTable();

        String[] factorParents = factor.getFactorParents();
        String[] queryParents = new String[names.length - 1];
        String[] fitOrderValues = new String[truthValsArr.length];

        System.arraycopy(names, 1, queryParents, 0, names.length - 1);
        fitOrderValues[0] = truthValsArr[0];
        int tempCounter = 1;
        for (String factorParent : factorParents) {
            for (int j = 0; j < queryParents.length; j++) {
                if (factorParent.equals(queryParents[j])) {
                    fitOrderValues[tempCounter++] = truthValsArr[j];
                }
            }
        }
        return factorTable.get(new TableKey(fitOrderValues));
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
     * which also uses the helper function IsProbabilityValueDirect() for fetching the specific probability value given the vars values.
     * In the end, after each iteration is done, normalize, and print the probability value, followed by the number of addition and multiplications,
     * respectively.
     * @param names Names of the given query.
     * @param truthValsArr The values given with the names in the same order.
     * @return A string of the answer.
     */
    public String func1(String[] names, String[] truthValsArr) {
        //Decimal format for correct answer printing.
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");

        //If query can be obtainable directly
        if (IsProbabilityValueDirect(names)) {
            double probability = getDirectProbability(names, truthValsArr);
            return decimalFormat.format(probability) + ",0,0";
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
            double answer = numerator / normalizationAlpha;
            return decimalFormat.format(answer) + "," + (additionCount - 1)+ "," + multiCount;
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

            if(IsProbabilityValueDirect(vars)){
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
     * The orders of eliminations of the hiddens is sorted in alphabetical order.
     * This functions uses the helper functions:
     * getAncestorsOfNode(), findRelevantFactorsOfFunc2(), discardOneValued(), join(), eliminate(), getProductOfJoinedTable(),
     * fromIndexToValuesTable(), getOutcomesFromGivenFactorColumn(), permutateByOne()
     * Some of those functions are helpers of other helper functions.
     * @param names Names of the given query.
     * @param truthValsArr The values given with the names in the same order.
     * @return A string of the answer.
     */
    public String func2(String[] names, String[] truthValsArr){
        //Decimal format for correct answer printing(5 digits after the dot).
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        //If query can be obtainable directly
        if (IsProbabilityValueDirect(names)) {
            double probability = getDirectProbability(names, truthValsArr);
            return decimalFormat.format(probability) + ",0,0";
        }
        else{
            ArrayList<String> relevantFactors = findRelevantFactorsOfFunc2(names); //Stores only relevant factors which will be found by BFS algorithm.

            //Relevant data gathering.
            ArrayList<Factor> tempFactors = new ArrayList<>(); //A copy of relevant factors. Makes sure original data is not modified.
            for(Factor factor: factorNodes){
                try {
                    if(relevantFactors.contains(factor.getFactorName()))
                        tempFactors.add((Factor) factor.clone()); //Factor will be added to our tempFactor list if it was found to be relevant.
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }

            List<String> nameList = Arrays.asList(names);
            String[] hidden = new String[relevantFactors.size() - names.length]; //Non-vars array.
            int insertionTemp = 0;

            //Loop iterates through all variable nodes. If a node isn't in the list, it is considered hidden.
            //This loop builds the hidden array.
            for (Factor currFactor : tempFactors) {
                if (!nameList.contains(currFactor.getFactorName())) {
                    hidden[insertionTemp++] = currFactor.getFactorName();
                }
                if(insertionTemp == hidden.length)
                    break;
            }

            String[] evidence = new String[names.length - 1];
            System.arraycopy(names, 1, evidence, 0, evidence.length);
            String[] query = new String[1];
            query[0] = names[0];

            //Loop of instantiations.
            for(int temp = 0; temp < evidence.length; temp++){
                String checkedVar = evidence[temp]; //Variable we wish to instantiate in tables
                String checkedVarValue = truthValsArr[temp + 1]; //Value of the variable we wish to keep.
                //Iterate through all factors of given evidence to filter.
                for (Factor currFactor : tempFactors) {
                    if (currFactor.varInFactor(checkedVar))
                        currFactor.instantiate(checkedVar, checkedVarValue);
                }
            }
            discardOneValued(tempFactors); //One valued factors after instantiation can be removed from the algorithm entirely.

            Arrays.sort(hidden); //Sort hidden variables(For variable elimination alphabetical order)

            int additionCounter = 0;
            int multCount = 0;

            //In every iteration, perform joins and elimination on the hidden variable in the iteration.
            for(String hiddenString: hidden) {
                ArrayList<Factor> hiddenFactors = new ArrayList<>(); //List of all factors that contain the hidden value.

                //Find all factors that contain the hidden evidence that will be eliminated
                for (Factor currFactor : tempFactors) {
                    if (currFactor.varInFactor(hiddenString))
                        hiddenFactors.add(currFactor);
                }

                if(hiddenFactors.size() == 0) //If there are no factors left for the hidden array, skip it.
                    continue;

                hiddenFactors.sort(Factor.factorComparator); //Sort by table size and variable ASCII sum when necessary(ascending)

                if(hiddenFactors.size() > 1){
                    //Joining loop
                    for(int i = 1; i < hiddenFactors.size(); i++){
                        multCount += join(hiddenFactors.get(i -1), hiddenFactors.get(i)); //Perform join on two tables in the order.
                        tempFactors.remove(hiddenFactors.get(i - 1)); //Remove previous table from entire data, to replicate table joining.
                    }
                }

                Factor eliminationFactor = hiddenFactors.get(hiddenFactors.size() - 1); //Last factor in the hidden factors is the one we remove the hidden column from.

                additionCounter += eliminate(eliminationFactor, hiddenString); //Eliminate the hidden variable from the factor.
                discardOneValued(tempFactors); //One valued factors left after joining can be discarded.
            }

            ArrayList<Factor> queryFactors = new ArrayList<>(); //Factors containing the query variable.
            //Loop to add all factors which contain the query variable (should be 2)
            for(Factor tempFactor: tempFactors){
                if(tempFactor.varInFactor(query[0]))
                    queryFactors.add(tempFactor);
            }
            queryFactors.sort(Factor.factorComparator); //Sort by table size and variable ASCII sum(ascending)

            //Joining loop
            for(int i = 1; i < queryFactors.size(); i++){
                multCount += join(queryFactors.get(i -1), queryFactors.get(i)); //Last joins on query tables.
            }

            Factor finalFactor = queryFactors.get(queryFactors.size() - 1); //Last factor in the hidden factors is the one we remove the hidden column from.
            Hashtable<TableKey, Double> finalTable = finalFactor.getFactorTable();
            double normalizationSum = 0.0;

            Enumeration<TableKey> keySetOfFinalFactor = finalFactor.getFactorTable().keys();

            //Normalization of the final table.
            while(keySetOfFinalFactor.hasMoreElements()){
                normalizationSum += finalTable.get(keySetOfFinalFactor.nextElement());
                additionCounter++;
            }


            Enumeration<TableKey> keySetOfFinalFactorNormalization = finalFactor.getFactorTable().keys();
            //Apply normalization on the probabilities.
            while(keySetOfFinalFactorNormalization.hasMoreElements()){
                TableKey currKey = keySetOfFinalFactorNormalization.nextElement();
                double rowValue = finalTable.get(currKey);
                finalTable.put(currKey, rowValue / normalizationSum);
            }
            String[] queryValue = new String[1];
            queryValue[0] = truthValsArr[0]; //The desired query value.
            double answer = finalTable.get(new TableKey(queryValue)); //Answer according to query value.
            return decimalFormat.format(answer)+","+(additionCounter - 1)+","+multCount;
        }
    }


    /**
     * Applies reverse BFS on the variable node to find its ancestors.
     * Required to filter out unnecessary factors from te variable elimination algorithm.
     * Meaning, that every node which isn't an ancestor of the query and evidence variables, can be discarded from the algorithm.
     * @param node Variable node with to find its ancestors.
     * @return An array list of the ancestors of the given node.
     */
    private ArrayList<String> getAncestorsOfNode(VariableNode node){
        ArrayList<String> ancestors = new ArrayList<>();
        Queue<String> traversalQueue = new LinkedList<>();
        traversalQueue.add(node.getVariableNodeName());
        while(!traversalQueue.isEmpty()){
            String ancestor = traversalQueue.remove();
            ancestors.add(ancestor);
            String[] currNodeParents = getNodeByName(ancestor).getParents();
            Collections.addAll(traversalQueue, currNodeParents);
        }
        return ancestors;
    }


    /**
     * This function uses getAncestorsOfNode() to find the relevant nodes names and saves them in the array list.
     * The main difference is that this function takes care of duplicate finds, therefore, it returns the unique names.
     * @param names The variable names of the given query.
     * @return Arraylist of relevant Strings which are required for the algorithm.
     */
    private ArrayList<String> findRelevantFactorsOfFunc2(String[] names){
        ArrayList<String> relevantFactors = new ArrayList<>();
        for(String name: names){
            ArrayList<String> potentials = getAncestorsOfNode(getNodeByName(name));
            for(String potential: potentials){
                if(!relevantFactors.contains(potential))
                    relevantFactors.add(potential);
            }
        }
        return relevantFactors;
    }


    /**
     * Deletes any factor which has 1 value, they can be discarded.
     * @param tempFactors Arraylist of the factors of algorithm 2.
     */
    private void discardOneValued(ArrayList<Factor> tempFactors){
        tempFactors.removeIf(factor -> factor.getFactorSize() == 1);
    }


    /**
     * Performs a single join between 2 factors, while changing the second factor with the new factor.
     * @param prevFactor First factor in multiplication to be joined.
     * @param currFactor Second factor in multiplication to be joined.
     * @return The number of multiplications the join had.
     */
    private int join(Factor prevFactor, Factor currFactor){
        String[] currVars = currFactor.getFactorVars(); //Variables of the second factor.
        String[] prevVars = prevFactor.getFactorVars(); //Variables of the first factor.
        Hashtable<String, String[]> varOutComes = new Hashtable<>(); //HashTable containing each var's possible outcomes.
        ArrayList<String> newTableVars = new ArrayList<>(); //Keeps the order of insertions in check.

        //Updates relevant vars of joined table from the prev factor.
        for(String var: prevVars){
            if(!varOutComes.containsKey(var)){
                newTableVars.add(var);
                varOutComes.put(var, getOutcomesFromGivenFactorColumn(prevFactor,prevVars,var));
            }
        }

        //Updates relevant vars of joined table from the prev factor. Makes sure duplicates aren't added twice.
        for(String var: currVars){
            if(!varOutComes.containsKey(var)){
                newTableVars.add(var);
                varOutComes.put(var, getOutcomesFromGivenFactorColumn(currFactor,currVars,var));
            }
        }

        int[] outcomeCounts = new int[newTableVars.size()]; //Array of outcomes of the new table variables.

        //Build the outcomeCounts array.
        for(int i = 0; i < varOutComes.size(); i++){
            outcomeCounts[i] = varOutComes.get(newTableVars.get(i)).length;
        }

        int[] indexArr = new int[outcomeCounts.length]; //Array of indices which represents values of keys.

        //Iterate through outcomeCounts array to get new joined table's number of rows.
        int rows = 1;
        for (int outcomeCount : outcomeCounts) {
            rows *= outcomeCount;
        }

        Hashtable<TableKey, Double> joinedTable = new Hashtable<>(); //New joined table.
        //Iterate to build joined table(Key wise)
        for(int i = 0; i < rows; i++){
            TableKey newKey = fromIndexToValuesTable(indexArr, newTableVars, varOutComes);
            joinedTable.put(newKey, 1.0);
            permutateByOne(indexArr, outcomeCounts);
        }

        int mults = getProductOfJoinedTable(prevFactor, currFactor, joinedTable, newTableVars); //Applies the multiplication of the tables.

        //Set the second factor's table and variables in the table.
        currFactor.setFactorTable(joinedTable);
        currFactor.setVars(newTableVars.toArray(new String[0]));
        return mults;
    }


    /**
     * Performs the variable elimination of a given factor, eliminating the given hiddenString.
     * @param factor Factor we with to eliminate variable from.
     * @param hiddenString Variable we wish to eliminate from the factor given.
     * @return Number of additions performed in the elimination.
     */
    private int eliminate(Factor factor, String hiddenString){
        int varIndex = 0; //Index of the variable we wish to eliminate.
        String[] factorVars = factor.getFactorVars(); //The variables of the factor.
        String[] newFactorVars = new String[factorVars.length - 1]; //The new vars after elimination.
        int insertionTemp = 0;

        //Iterate through the variables to find the index of the variable we wish to eliminate.
        //And add to newFactorVars the variables we want to keep.
        for(int i = 0; i < factorVars.length; i++){
            if(factorVars[i].equals(hiddenString)){
                varIndex = i;
            }else{
                newFactorVars[insertionTemp++] = factorVars[i];
            }
        }

        Hashtable<TableKey, Double> currTable = factor.getFactorTable(); //Table we eliminate from.
        Hashtable<TableKey, Double> newEliminatedTable = new Hashtable<>(); //New table after elimination.
        int additionCounter = 0;
        Enumeration<TableKey> keySet = currTable.keys();

        //Iterate through the keys of the table.
        while(keySet.hasMoreElements()){
            TableKey currTableKey = keySet.nextElement();
            String[] currKeyArr = currTableKey.getKeys();
            String[] newVals = new String[newFactorVars.length];
            int insertNewValsTemp = 0;

            //Iterate to build the values of the relevant variables.
            for(int i = 0; i < currKeyArr.length; i++){
                if(i != varIndex)
                    newVals[insertNewValsTemp++] = currKeyArr[i];
            }

            //If the new vals is not in the joined table in the first place, skip it in the loop.
            if(newEliminatedTable.containsKey(new TableKey(newVals))){
                continue;
            }

            Enumeration<TableKey> keySetTemp = currTable.keys(); //Will iterate through the current table again. It is a sub iteration.
            double sum = 0.0;
            int additions = 0; //Local additions of the given key.
            while(keySetTemp.hasMoreElements()){
                TableKey currTableKeyTemp = keySetTemp.nextElement();
                String[] currKeyArrTemp = currTableKeyTemp.getKeys();
                String[] newValsTemp = new String[newFactorVars.length];
                int insertNewValsTemp2 = 0;

                for(int i = 0; i < currKeyArrTemp.length; i++){
                    if(i != varIndex)
                        newValsTemp[insertNewValsTemp2++] = currKeyArrTemp[i];
                }

                //If the new array of values matches the relevant array of values in the beginning, it fits the addition.
                if(Arrays.equals(newValsTemp, newVals)){
                    sum += currTable.get(currTableKeyTemp);
                    additions += 1;
                }
            }
            //Add new row to eliminated table.
            newEliminatedTable.put(new TableKey(newVals), sum);
            additionCounter += additions - 1;
        }
        //Set the factor's table to the new eliminated factor. Both table and vars like.
        factor.setVars(newFactorVars);
        factor.setFactorTable(newEliminatedTable);
        return additionCounter;
    }


    /**
     * Performs the after joining multiplication.
     * @param prevFactor First factor of joining.
     * @param currFactor Second factor of joining.
     * @param joinedTable The new simple joined table.
     * @param newTableVars The new variables of the new joined table.
     * @return The amount of multiplications done by the joining.
     */
    private int getProductOfJoinedTable(Factor prevFactor, Factor currFactor, Hashtable<TableKey, Double> joinedTable, ArrayList<String> newTableVars) {
        String[] prevFactorVars = prevFactor.getFactorVars();
        String[] currFactorVars = currFactor.getFactorVars();
        String[] joinedTableVars = newTableVars.toArray(new String[0]);

        Hashtable<TableKey, Double> prevFactorTable = prevFactor.getFactorTable();
        Hashtable<TableKey, Double> currFactorTable = currFactor.getFactorTable();
        int subArrayEndIndex = prevFactorVars.length;

        Enumeration<TableKey> prevKeys = prevFactorTable.keys();
        Enumeration<TableKey> currKeys = currFactorTable.keys();

        int multiCount = 0;
        //Iteration through the first table in the multiplication.
        //Multiply the values of the first table to the values of the
        while(prevKeys.hasMoreElements()){
            TableKey key = prevKeys.nextElement();
            String[] keyArr = key.getKeys();
            double prob = prevFactorTable.get(key);
            Enumeration<TableKey> joinedKeys = joinedTable.keys();
            while(joinedKeys.hasMoreElements()){
                TableKey joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeysSubArray = Arrays.copyOfRange(joinedKey.getKeys(),0, subArrayEndIndex);
                if(Arrays.equals(keyArr, joinedKeysSubArray)){
                    joinedTable.put(joinedKey, joinedTableProb * prob);
                }
            }
        }

        //Iterate through second table keys.
        //Multiply the second table's values to the joined table.
        while(currKeys.hasMoreElements()){
            TableKey currKey = currKeys.nextElement();
            String[] currKeyArr = currKey.getKeys();
            double prob = currFactorTable.get(currKey);

            Enumeration<TableKey> joinedKeys = joinedTable.keys();
            //Iterate through joined table keys.
            while(joinedKeys.hasMoreElements()){
                TableKey joinedKey = joinedKeys.nextElement();
                double joinedTableProb = joinedTable.get(joinedKey);
                String[] joinedKeyVals = joinedKey.getKeys();
                String[] newOrderedVals = new String[currFactorVars.length]; //New String array to compare with array from second table.
                int tempCounter = 0;

                //Build the newOrderedVals for each iteration of joinedTable keys.
                //Order of keys int the second table won't fit order of keys in the joined table. reorganize values to match arrays.
                for (String currFactorVar : currFactorVars) {
                    for (int joinedColumn = 0; joinedColumn < joinedTableVars.length; joinedColumn++) {
                        if (currFactorVar.equals(joinedTableVars[joinedColumn])) {
                            newOrderedVals[tempCounter++] = joinedKeyVals[joinedColumn];
                        }
                    }
                }
                if(Arrays.equals(newOrderedVals, currKeyArr)){
                    joinedTable.put(joinedKey, joinedTableProb * prob);
                    multiCount++;
                }
            }
        }
        return multiCount;
    }


    /**
     * Converts an array of indices representing keys, to actual array of keys, which will then transform it to
     * a TableKey instance which is valid as a key of a factor table.
     * @param indexArr Array of indices representing key values.
     * @param varList List of variables.
     * @param varTable A table of variables and their possible outcomes.
     * @return A new TableKey of relevant key values, which will be used as a key in a factor table.
     */
    private TableKey fromIndexToValuesTable(int[] indexArr, ArrayList<String> varList, Hashtable<String, String[]> varTable){
        String[] values = new String[indexArr.length];
        for(int i = 0; i < varList.size(); i++){
            values[i] = varTable.get(varList.get(i))[indexArr[i]];
        }
        return new TableKey(values);
    }


    /**
     * Fetches the outcomes of a given column in a factor table.
     * @param factor Factor instance.
     * @param vars Variables of the factor.
     * @param var Variable we wish to get its outcomes
     * @return An array of outcomes of a variable from a given factor.
     */
    private String[] getOutcomesFromGivenFactorColumn(Factor factor, String[] vars, String var){
        int varIndex = -1;
        //Find the index of the variable we wish to find its values.
        for(int i = 0; i < vars.length; i++){
            if(vars[i].equals(var)){
                varIndex = i;
                break;
            }
        }

        ArrayList<String> uniqueValuesOfColumn = new ArrayList<>(); //List of found values.
        Enumeration<TableKey> keySet = factor.getFactorTable().keys();
        while(keySet.hasMoreElements()){
            TableKey currKey = keySet.nextElement();
            String value = currKey.getKeys()[varIndex];
            if(!uniqueValuesOfColumn.contains(value))
                uniqueValuesOfColumn.add(value);
        }
        return uniqueValuesOfColumn.toArray(new String[0]);
    }



    public String func3(String[] names, String[] truthValsArr){
        //Decimal format for correct answer printing(5 digits after the dot).
        DecimalFormat decimalFormat = new DecimalFormat("#.#####");
        //If query can be obtainable directly
        if (IsProbabilityValueDirect(names)) {
            double probability = getDirectProbability(names, truthValsArr);
            return decimalFormat.format(probability) + ",0,0";
        }
        else{
            ArrayList<String> relevantFactors = findRelevantFactorsOfFunc2(names); //Stores only relevant factors which will be found by BFS algorithm.

            //Relevant data gathering.
            ArrayList<Factor> tempFactors = new ArrayList<>(); //A copy of relevant factors. Makes sure original data is not modified.
            for(Factor factor: factorNodes){
                try {
                    if(relevantFactors.contains(factor.getFactorName()))
                        tempFactors.add((Factor) factor.clone()); //Factor will be added to our tempFactor list if it was found to be relevant.
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }

            List<String> nameList = Arrays.asList(names);
            String[] hidden = new String[relevantFactors.size() - names.length]; //Non-vars array.
            int insertionTemp = 0;

            //Loop iterates through all variable nodes. If a node isn't in the list, it is considered hidden.
            //This loop builds the hidden array.
            for (Factor currFactor : tempFactors) {
                if (!nameList.contains(currFactor.getFactorName())) {
                    hidden[insertionTemp++] = currFactor.getFactorName();
                }
                if(insertionTemp == hidden.length)
                    break;
            }

            String[] evidence = new String[names.length - 1];
            System.arraycopy(names, 1, evidence, 0, evidence.length);
            String[] query = new String[1];
            query[0] = names[0];

            //Loop of instantiations.
            for(int temp = 0; temp < evidence.length; temp++){
                String checkedVar = evidence[temp]; //Variable we wish to instantiate in tables
                String checkedVarValue = truthValsArr[temp + 1]; //Value of the variable we wish to keep.
                //Iterate through all factors of given evidence to filter.
                for (Factor currFactor : tempFactors) {
                    if (currFactor.varInFactor(checkedVar))
                        currFactor.instantiate(checkedVar, checkedVarValue);
                }
            }
            discardOneValued(tempFactors); //One valued factors after instantiation can be removed from the algorithm entirely.

            hiddenVariablesEliminationSorting(hidden);//Sort hidden variables(For variable elimination alphabetical order)

            int additionCounter = 0;
            int multCount = 0;

            //In every iteration, perform joins and elimination on the hidden variable in the iteration.
            for(String hiddenString: hidden) {
                ArrayList<Factor> hiddenFactors = new ArrayList<>(); //List of all factors that contain the hidden value.

                //Find all factors that contain the hidden evidence that will be eliminated
                for (Factor currFactor : tempFactors) {
                    if (currFactor.varInFactor(hiddenString))
                        hiddenFactors.add(currFactor);
                }

                if(hiddenFactors.size() == 0) //If there are no factors left for the hidden array, skip it.
                    continue;

                hiddenFactors.sort(Factor.factorComparator); //Sort by table size and variable ASCII sum when necessary(ascending)

                if(hiddenFactors.size() > 1){
                    //Joining loop
                    for(int i = 1; i < hiddenFactors.size(); i++){
                        multCount += join(hiddenFactors.get(i -1), hiddenFactors.get(i)); //Perform join on two tables in the order.
                        tempFactors.remove(hiddenFactors.get(i - 1)); //Remove previous table from entire data, to replicate table joining.
                    }
                }

                Factor eliminationFactor = hiddenFactors.get(hiddenFactors.size() - 1); //Last factor in the hidden factors is the one we remove the hidden column from.

                additionCounter += eliminate(eliminationFactor, hiddenString); //Eliminate the hidden variable from the factor.
                discardOneValued(tempFactors); //One valued factors left after joining can be discarded.
            }

            ArrayList<Factor> queryFactors = new ArrayList<>(); //Factors containing the query variable.
            //Loop to add all factors which contain the query variable (should be 2)
            for(Factor tempFactor: tempFactors){
                if(tempFactor.varInFactor(query[0]))
                    queryFactors.add(tempFactor);
            }
            queryFactors.sort(Factor.factorComparator); //Sort by table size and variable ASCII sum(ascending)

            //Joining loop
            for(int i = 1; i < queryFactors.size(); i++){
                multCount += join(queryFactors.get(i -1), queryFactors.get(i)); //Last joins on query tables.
            }

            Factor finalFactor = queryFactors.get(queryFactors.size() - 1); //Last factor in the hidden factors is the one we remove the hidden column from.
            Hashtable<TableKey, Double> finalTable = finalFactor.getFactorTable();
            double normalizationSum = 0.0;

            Enumeration<TableKey> keySetOfFinalFactor = finalFactor.getFactorTable().keys();

            //Normalization of the final table.
            while(keySetOfFinalFactor.hasMoreElements()){
                normalizationSum += finalTable.get(keySetOfFinalFactor.nextElement());
                additionCounter++;
            }


            Enumeration<TableKey> keySetOfFinalFactorNormalization = finalFactor.getFactorTable().keys();
            //Apply normalization on the probabilities.
            while(keySetOfFinalFactorNormalization.hasMoreElements()){
                TableKey currKey = keySetOfFinalFactorNormalization.nextElement();
                double rowValue = finalTable.get(currKey);
                finalTable.put(currKey, rowValue / normalizationSum);
            }
            String[] queryValue = new String[1];
            queryValue[0] = truthValsArr[0]; //The desired query value.
            double answer = finalTable.get(new TableKey(queryValue)); //Answer according to query value.
            return decimalFormat.format(answer)+","+(additionCounter - 1)+","+multCount;
        }
    }


    /**
     * The heuristic logic used for the algorithm is the min-neighbors one.
     * Meaning, it decides the order of elimination by sorting each variable's number of inward neighbors.
     * This function takes care of the sorting of the nodes themselves, the sorter used is
     * implemented in the VariableNode class.
     *
     * @param hidden Array of hidden variables in strings.
     */
    private void hiddenVariablesEliminationSorting(String[] hidden){
        Arrays.sort(hidden); //Sort alphabetically the hidden array.

        List<String> hiddenList = Arrays.asList(hidden); //Used for checking containment.
        List<VariableNode> hiddenNodes = new ArrayList<>(); //New list we wish to make.

        //Iteration which adds the nodes relevant according to the hidden list.
        for(VariableNode variableNode: variableNodes){
            if(hiddenList.contains(variableNode.getVariableNodeName())){
                hiddenNodes.add(variableNode);
            }
        }

        hiddenNodes.sort(VariableNode.variableNodeComparator); //Sorts the variables themselves.

        //Override given values with new ones in order given by the comparator.
        for(int j = 0; j < hidden.length; j++){
            hidden[j] = hiddenNodes.get(j).getVariableNodeName();
        }
    }




    /**
     * Iterates the given index array by one. Iteration is pointed from the right to the left.
     * Important for factor building and rebuilding.
     * As well as permuting through all possible permutations.
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
            if((nextSwitch &&(indexArr[j] == 0 && prevVal == outcomeCounts[j] - 1))){
                prevVal = indexArr[j - 1];
                indexArr[j - 1] += 1;
                indexArr[j - 1] %= outcomeCounts[j - 1];
                nextSwitch = indexArr[j - 1] == 0 && prevVal == outcomeCounts[j - 1] - 1 && prevVal != 0;
            }
            else{
                nextSwitch = (indexArr[j - 1] + 1 == outcomeCounts[j - 1]);
            }
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
