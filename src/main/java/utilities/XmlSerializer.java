package utilities;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.HashMap;

/** Esta classe é utilizada para ler e escrever os ficheiros
 *  de configuração.
 */
public class XmlSerializer {

    /** Lê o ficheiro de configuração xml e devolve um HashMap
     *  com os dados lidos.
     */
    public static HashMap<String, String> readConfig(String fileName) {
        HashMap<String,String> map = null;
        try {
            XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileName)));
            map = (HashMap<String, String>)decoder.readObject();
            decoder.close();
        } catch (IOException e){
            System.out.println(e);
        }
        return map;
    }

    /** Este método recebe um HashMap e escreve-o para o ficheiro
     *  de configuração de xml.
     */
    public static void saveConfig(String fileName, HashMap<String, String> map) {
        try {
            XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(fileName)));
            encoder.writeObject(map);
            encoder.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
