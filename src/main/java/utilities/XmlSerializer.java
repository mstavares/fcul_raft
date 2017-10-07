package utilities;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.HashMap;


public class XmlSerializer {

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
