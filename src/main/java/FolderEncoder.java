import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import static net.sourceforge.argparse4j.ArgumentParsers.*;

public class FolderEncoder {

    static CharsetDetector charsetDetector = new CharsetDetector();
    static String[] charsetsToBeTested = CharsetDetector.getAllDetectableCharsets();
    static int nbrFiles = 0;
    static int nbrVisitedFiles = 0;
    public static void main(String[] args) {
        if (args==null || args.length==0)
            args = "-p C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy -enc UTF-8 -inc .+\\.java,.+\\.html,.+\\.jsp".split(" ");
            //args = "-p C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\FolderEncoder\\src\\main\\java\\test -inc .+\\.java".split(" ");
        ArgumentParser parser = newFor("FolderEncoder").build()
                .defaultHelp(true)
                .description("Re-encode folder content with a new encoding recursively.");
        parser.addArgument("-p", "--path")
                .help("Path of the Folder");
        parser.addArgument("-enc", "--encoding")
                .help("The new file encoding")
                .choices(charsetsToBeTested).setDefault("UTF-8");
        parser.addArgument("-inc", "--inc")
                .help("Regular expressions for included paths separated by ',' ex: .+\\.java (*.java) ");
        parser.addArgument("-exc", "--exc")
                .help("Regular expressions for excluded paths separated by ',' ex: .+\\.java (*.java) ");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        String path = ns.getString("path");
        if (path==null) {
            parser.printHelp();
            System.exit(1);
        }
        String in_exts = ns.getString("inc");
        String[] in_exts_ = null;
        if (in_exts!=null)
            in_exts_ = in_exts.split(",");
        String ex_exts = ns.getString("exc");
        String[] ex_exts_ = null;
        if (ex_exts!=null)
            ex_exts_ = ex_exts.split(",");
        File folder = new File(path);
        encode(folder, ns.getString("encoding"), in_exts_, ex_exts_);
        System.out.println("----> Task ends : "+nbrVisitedFiles+" files visited, re-encoding "+nbrFiles+" files.");
    }

    private static void encode(File folder, String targetEncoding, String[] in_exts, String[] ex_exts) {
        for (File file :folder.listFiles()) {
            CharsetMatch charsetMatch = null;
            if (file.isDirectory() && !file.getName().equals("out"))
                encode(file, targetEncoding, in_exts, ex_exts);
            else {
                nbrVisitedFiles++;
                if (in_exts!=null) {
                    boolean match = false;
                    for (String ext : in_exts) {
                        if (file.getAbsolutePath().matches(ext)){
                            match = true;
                            break;
                        }
                    }
                    if (!match)
                        continue;
                }
                if (ex_exts!=null) {
                    boolean match = false;
                    for (String ext : ex_exts) {
                        if (file.getAbsolutePath().matches(ext)){
                            match = true;
                            break;
                        }
                    }
                    if (match)
                        continue;
                }
                System.out.println("Detecting file encoding for " + file.getAbsolutePath());
                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    charsetMatch = charsetDetector.setText(is).detect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (charsetMatch == null) {
                    System.out.println("Charset not detected for file " + file.getAbsolutePath());
                } else if (!charsetMatch.getName().equals(targetEncoding)) {
                    System.out.println("Changing file encoding " + file.getAbsolutePath() + " from " + charsetMatch.getName()+" to "+targetEncoding);
                    String originalName = file.getAbsolutePath();
                    String tmpName = originalName + "_tmp";
                    new File(tmpName).delete();
                    boolean success = false;
                    if (file.renameTo(new File(tmpName))) {
                        try {
                            String content = null;
                            String header = "<%@ page contentType=\"text/html; charset=UTF-8\" %>\n";
                            if (originalName.endsWith(".jsp")) {
                                content = "";
                                String fileContent = FileUtils.readFileToString(new File(tmpName), charsetMatch.getName());
                                String str = fileContent.replaceAll("ISO-8859-1", "UTF-8").replaceAll("iso-8859-1", "UTF-8");
                                int start = str.indexOf("<%@ page");
                                if ( start != -1 ) {
                                    String currentHeader = str.substring(start, str.indexOf("%>", start)+2);
                                    String restOfFile = str.substring(str.indexOf("%>", start)+2);
                                    if (currentHeader.contains("contentType")) {
                                        if (currentHeader.contains("text/html") && !currentHeader.contains("charset"))
                                            throw new Exception("special case "+file.getAbsolutePath());
                                        currentHeader = currentHeader.replace("ISO-8859-1", "UTF-8").replace("iso-8859-1", "UTF-8");
                                    }else {
                                        currentHeader = currentHeader.replace("%>", " contentType=\"text/html; charset=UTF-8\" %>");
                                    }
                                    content += str.substring(0, start) + currentHeader + restOfFile;
                                }else
                                    content += header+fileContent;
                            }else {
                                content += FileUtils.readFileToString(new File(tmpName), charsetMatch.getName()).replaceAll("ISO-8859-1", "UTF-8").replaceAll("iso-8859-1", "UTF-8");
                            }
                            //content = content.replaceAll("D:\\Donnees", "C:\\APPLIS");
                            while (content.startsWith("null"))
                                content = content.substring(4);
                            //content = content.substring(content.indexOf("'à'")+1, content.indexOf("'à'")+2);
                            StringBuffer buffer = new StringBuffer();
                            char[] cs = content.toCharArray();
                            for (int i=0; i<content.length(); i++) { // special char
                                char c = cs[i];
                                if (c == '\'' && i + 1 < content.length()) {
                                    if (cs[i+1] >= 192 && cs[i+1] <= 255) {
                                        buffer.append("(char)" + (int)cs[i+1]);
                                        i+=2;
                                    } else
                                        buffer.append(c);
                                } else
                                    buffer.append(c);
                            }

                            FileUtils.write(new File(originalName), buffer.toString(), "UTF-8");

                            nbrFiles++;
                            success = true;
                        } catch (Exception e) {
                            new File(originalName).delete();
                            new File(tmpName).renameTo(new File(originalName));
                        }
                        if (success) {
                            new File(tmpName).delete();
                            if (originalName.matches(".*src\\\\main\\\\java\\\\.*\\.properties")) {
                                String newName = originalName.replace(".properties", ".utf8.properties");
                                new File(newName).delete();
                                new File(originalName).renameTo(new File(newName));
                            }
                        }
                    }
                }
            }
        }
    }
}
