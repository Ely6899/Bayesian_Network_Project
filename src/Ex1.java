import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ex1 {


    /**
     * Filters out the variable names of a given probability query.
     * Filtering is done using REGEX.
     * @param query probability string query (Example: P(B=T|J=T,M=T)). Needs to be the exact format!
     * @return String array of the query variable names.
     */
    private static String[] getQueryNames(String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern = Pattern.compile("([a-z A-Z\\d]*)=");
        Matcher matcher = pattern.matcher(query);
        while(matcher.find()){
            stringList.add(matcher.group(1));
        }
        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr); //First element always the name element we wish to find. The rest are the parents.
    }

    /**
     * Filters out the variable values given in the probability query.
     * Filtering is done using REGEX.
     * @param query probability string query (Example: P(B=T|J=T,M=T)). Needs to be the exact format!
     * @return String array of the query variable names
     */
    private static String[] getQueryIndex(String query){
        List<String> stringList = new ArrayList<>();
        Pattern pattern = Pattern.compile("=([a-z A-Z\\d]*)");
        Matcher matcher = pattern.matcher(query);
        while(matcher.find()){
            stringList.add(matcher.group(1));
        }
        String[] namesArr = new String[stringList.size()];
        return stringList.toArray(namesArr);
    }


    /**
     * Reads an input text file which needs to contain the xml file we read from,
     * and it needs to contain probability queries followed by a number indicating the function used to
     * calculate the probability of the given query.
     * @param input Input text file. Must contain in the first line the xml file we wish to read from.
     */
    private static void readFromXmlInputFile(String input){
        BufferedReader br;
        BayesianNetwork bayesianNetwork;
        FileOutputStream fileOutputStream;
        try {
            //let BufferedReader read the content of the given input file.
            br = new BufferedReader(new FileReader(input));
            fileOutputStream = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            String line;
            try {
                line = br.readLine(); //Input file line reader.

                //This is the first iteration of the line reader, reading the given xml file name.
                //Parsing of the XML is done in BayesianNetwork class.
                bayesianNetwork = new BayesianNetwork(line);
                line = br.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //Iterate until the end of the file.
            while (line != null) {
                try {
                    char funcInput = line.charAt(line.length() - 1); //Function number input.
                    String query = line.substring(0,line.length() - 2); //Query string input.
                    String answer;
                    //Switch given an input number.
                    switch (funcInput){
                        case '1':
                            answer = bayesianNetwork.func1(getQueryNames(query), getQueryIndex(query)) + "\n";
                            fileOutputStream.write(answer.getBytes());
                            break;
                        case '2':
                            answer = bayesianNetwork.func2(getQueryNames(query), getQueryIndex(query)) + "\n";
                            fileOutputStream.write(answer.getBytes());
                            break;
                        case '3':
                            answer = bayesianNetwork.func3(getQueryNames(query), getQueryIndex(query)) + "\n";
                            fileOutputStream.write(answer.getBytes());
                            break;
                        default:
                            System.out.println("Invalid input");
                            break;
                    }
                    line = br.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }finally {
            try {
                br.close();
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Main will run the xml reading function, which will parse the input, the XML, and run through the input file.
    public static void main(String[] args) {
        readFromXmlInputFile("input.txt");
    }
}
