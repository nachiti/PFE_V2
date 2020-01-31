import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ExtracteurDuPathdepuisFichierXml {

    private static String path = "";
    private static String root = "";
    private static List<Node> listLastElements = new ArrayList<Node>();
    private static List<String> listPaths = new ArrayList<String>(); //liste pour chaque fichier
    private static final String DIR_XML_FILES = "src/main/resources"; //dir of xml files
    private static final String DIR_PATHS_FILE = "out/pathsFile"; //dir of generated file
    private static final String BITMAP_REPRESENTATION_CSV = "bitmapRepresentation.csv";//resultat
    private static final String FILE_NAME_LIST_ALL_PATHS = "listAllPathsWhitoutDuplic.txt";//fichier du resultat
    private static List<List<String>> matrix = new ArrayList<>(); //tableaux qui contient le resultat


    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {


        List<String> listAllPaths = new ArrayList<String>(); // liste de toutes les paths sans duplica
        Files.createDirectories(Paths.get(DIR_PATHS_FILE)); //dossier pour stocker des fichiers generer

        File folder = new File(DIR_XML_FILES); // Dossier des fichiers xml
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    System.out.println(file.getName());
                    listAllPaths.addAll(genneratorOfPathFile(file.getName()));
                }
            }
        }else {
            System.out.println("le dossier '"+folder.getPath()+"' est vide");
        }

        List<String> listAllPathsWhitoutDuplic = listAllPaths.stream().distinct().collect(Collectors.toList());
        writePathInFile(listAllPathsWhitoutDuplic, "../../"+FILE_NAME_LIST_ALL_PATHS);
        //ajouter les paths dans la premiere cologne du tableau
        addPathInFirstColumOfTab();
        //parcourir toutes les fichiers qui contient les paths de chaque fichier xml
        //et remplir le tableaux par 1 ou 0, si le fichier contient un path donnée
        completeTabForAllFiles();
        //Enregistrer le tableau dans un fichier csv
        saveTabIntoCsvFile();
        int sum = 0;
        for (List<String> rowData : matrix) {
            sum=sum +(Integer.parseInt(rowData.get(1))*Integer.parseInt(rowData.get(2)));

        }
        System.out.println(sum);
    }


    /**
     * Permet de generer le chemin de la racine à la feuille
     * pour chaque element feuille d'un fichier xml
     *
     * @param xmlfileName nom du fichier xml
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static List<String> genneratorOfPathFile(String xmlfileName) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(DIR_XML_FILES+"/"+xmlfileName));
        Element rootElement = document.getDocumentElement();
        root = rootElement.getTagName();
        path = "";
        listLastElements = new ArrayList<Node>();
        listPaths = new ArrayList<String>();
        NodeList allElement = document.getElementsByTagName("*");
        for (int i = 0; i < allElement.getLength(); i++) {
            //stocker les derniers elements de l'arbre
            if (!haschildNodeElement(allElement.item(i).getChildNodes())) {
                listLastElements.add(allElement.item(i));
            }
        }
        for (Node listLastElement : listLastElements) {
            concatNode(listLastElement);
        }
        //ecrire les paths dans un fichier
        writePathInFile(listPaths, xmlfileName);
        return listPaths;
    }


    /**
     * methode recursive, qui permet de concatené le nom du noeud
     * en parcourant l'arbre du derniers element d'un chemin jusqu'a la racine
     *
     * @param node
     */
    private static void concatNode(Node node) {
        if (path.isEmpty()) {
            path = node.getNodeName();
        } else {
            //je concatene avec le parent
            path = node.getNodeName() + "/" + path;
        }
        //si le parent d'un noeud est different de la racine
        if (!node.getParentNode().getNodeName().equals(root)) {
            concatNode(node.getParentNode());
        } else {
            //une fois arriver à la racine, je stock le path et je le reinitialise pour un autre chemin
            listPaths.add(root + "/" + path);
            // System.out.println(root + "/" + path);
            path = "";
        }
    }

    /**
     * verifie s'il a un fils de type Element
     *
     * @param nodeList
     * @return
     */
    private static boolean haschildNodeElement(NodeList nodeList) {
        for (int j = 0; j < nodeList.getLength(); j++) {
            Node childNode = nodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * permet d'ecrire dans un fichier txt , les paths genenerer pour chaque fichier xml
     *
     * @param listofPaths
     * @param xmlfileName
     */
    private static void writePathInFile(List<String> listofPaths, String xmlfileName) {
        xmlfileName = xmlfileName.replace(".xml", ".txt");
        try {
            FileWriter fw = new FileWriter(DIR_PATHS_FILE + "/" + xmlfileName);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter fichierSortie = new PrintWriter(bw);
            for (String paths : listofPaths) {
                //Ecriture
                fichierSortie.println(paths);
            }
            fichierSortie.close();
            System.out.println("Le fichier " + xmlfileName + " a été créé!");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }


    /**
     * ajouter les paths dans la premiere colonne du tableau
     *
     * @throws IOException
     */
    private static void addPathInFirstColumOfTab() throws IOException {
        File file = new File(FILE_NAME_LIST_ALL_PATHS);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        int index = 0;
        while ((st = br.readLine()) != null) {
            matrix.add(new ArrayList<>());
            matrix.get(index).add(st);
            index++;
        }
    }

    /**
     * parcourir toutes les fichiers qui contiens les paths de chaque fichier xml
     * et remplir le tableaux par 1 ou 0, si le fichier contient un path donnée
     *
     * @throws IOException
     */
    private static void completeTabForAllFiles() throws IOException {
        List<File> listFile = Files.walk(Paths.get(DIR_PATHS_FILE))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        for (File f : listFile) {
            for (List<String> ligne : matrix) {
                String path = ligne.get(0);
                if (isPathInFile(path, f)) {
                    ligne.add("1");
                } else {
                    ligne.add("0");
                }
            }
        }
    }

    /**
     * permet de verifier si le path est présent dans le fichier
     *
     * @param path
     * @param file
     * @return
     * @throws IOException
     */
    private static boolean isPathInFile(String path, File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null) {
            if (st.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enregistrer le tableau dans un fichier csv
     *
     * @throws IOException
     */
    private static void saveTabIntoCsvFile() throws IOException {
        FileWriter csvWriter = new FileWriter(BITMAP_REPRESENTATION_CSV);
        for (List<String> rowData : matrix) {
            csvWriter.append(String.join(",", rowData));
            csvWriter.append("\n");
        }
        csvWriter.flush();
        csvWriter.close();
    }

}


