import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import java.io.*;

public class FolderEncoder {

    static CharsetDetector charsetDetector = new CharsetDetector();
    static String[] charsetsToBeTested = CharsetDetector.getAllDetectableCharsets();
    static int nbrFiles = 0;
    static int nbrVisitedFiles = 0;
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("FolderEncoder").build()
                .defaultHelp(true)
                .description("Re-encode folder content with a new encoding recursively.");
        parser.addArgument("-p", "--path")
                .help("Path of the Folder");
        parser.addArgument("-enc", "--encoding")
                .help("The new file encoding")
                .choices(charsetsToBeTested).setDefault("UTF-8");
        parser.addArgument("-inc", "--inc")
                .help("Regular expressions for included paths separated by ',' ");
        parser.addArgument("-exc", "--exc")
                .help("Regular expressions for excluded paths separated by ',' ");
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
            if (file.isDirectory())
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
                        File target = new File(originalName);
                        try (InputStreamReader br = new InputStreamReader(new FileInputStream(new File(tmpName)), charsetMatch.getName());
                             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), targetEncoding))){
                                char[] buffer = new char[16384];
                                int read;
                                while ((read = br.read(buffer)) != -1)
                                    bw.write(buffer, 0, read);
                            nbrFiles++;
                            success = true;
                        } catch (Exception e) {
                            new File(originalName).delete();
                            new File(tmpName).renameTo(new File(originalName));
                        }
                        if (success)
                            new File(tmpName).delete();
                    }
                }
            }
        }
    }
}
