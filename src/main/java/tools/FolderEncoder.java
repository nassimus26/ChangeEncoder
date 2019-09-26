package tools;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import util.FileFilter;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static net.sourceforge.argparse4j.ArgumentParsers.*;

public class FolderEncoder {
    static String[] hosts_suffix = new String[] {"e0", "ct0"};
    static List<String> hosts;
    static List<String> hosts2 = new ArrayList<>();
    static List<String> shortHosts = new ArrayList<>();
    static List<String> shortHosts2 = new ArrayList<>();
    static List<String> hosts_rex = new ArrayList<>();
    static List<String> false_hosts_rex = new ArrayList<>();
    static List<String> false_hosts_rex2 = new ArrayList<>();
    static {
        try {
            hosts = Files.readAllLines( Paths.get(Thread.currentThread().getContextClassLoader().getResource("hosts.txt").toURI()));
            for (String h : hosts) {
                String host = h.substring(0, h.indexOf("."));
                String host2 = "";
                for (char c:host.toCharArray()) {
                    host2 += c+"_";
                }
                shortHosts.add(host);

                hosts_rex.add(h.replace(host, host2).replaceAll("\\.", "_"));
                String shortVersion = StringUtils.replaceEach(host, hosts_suffix, new String[]{"", ""});
                hosts2.add(StringUtils.replaceEach(h, hosts_suffix, new String[]{"", ""}));
                shortHosts2.add(shortVersion);
                false_hosts_rex.add(h+h.substring(h.indexOf(".")));
                false_hosts_rex2.add(shortVersion+h.substring(h.indexOf(".")));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
    //.*\.png
    public static String exclude =  ".*\\.class,.*\\.xls,.*\\.pdf,.*\\.png,.*\\.svg,.*\\.gif,.*\\.jpg,.*\\.jpeg,.*build,.*out,.*target,.*\\.git,.*\\.idea," +
                                    ".*\\.svn,.*\\.settings,.*\\.classpath,.*\\.project,.*\\.jar,.*\\.iml,.*\\.cgi,.*\\.swf,.*\\.ksh,.*\\.zargo,.*\\.eot," +
                                    ".*\\.crontab,.*\\.lasso,.*\\.file,.*\\.save,.*\\.next,.*\\.new,.*\\.otf,.*\\.zip,.*\\.cvsignore," +
                                    ".*\\.template,.*\\.mov,.*\\.ctl,.*\\.jsi,.*\\.md,.*\\.MF,.*\\.xlsx,.*\\.smd,.*\\.wsdd,.*\\.xsd,.*\\.doc," +
                                    ".*\\.crt,.*\\.ascx,.*\\.cfc,.*\\.tld,.*\\.cfm,.*\\.afp,.*\\.jrxml,.*\\.ttf,.*\\.dtd,.*\\.cer,.*\\.xsl,.*\\.js," +
                                    ".*\\.fla,.*\\.htc,.*\\.ppt,.*\\.jasper,.*\\.bmp,.*Thumbs\\.db";

    public static String include =  null;
    public static CharsetDetector charsetDetector = new CharsetDetector();
    static String[] charsetsToBeTested = CharsetDetector.getAllDetectableCharsets();
    static int nbrFiles = 0;
    static int nbrVisitedFiles = 0;

    public static void main(String[] args) throws IOException {
        //-p C:\APPLIS\HELIOSDEV\IntelliJWorkspace\extranet_ent_ope2 -exc .*\.svg,.*\.gif,.*\.jpg,.*\.jpeg,.*\.idea.*,.*\.svn.*,.*\.settings.*,.*\.classpath.*,.*\.project.*,.*\.jar,.*\.iml
        if (args==null || args.length==0)
            args = ("-p C:\\APPLIS\\target\\rubriques -exc "+exclude).split(" ");
            //args = ("-p C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2 -enc UTF-8 -inc .+\\.java,.+\\.html,.+\\.jsp,.+\\.csv,.+\\.css,.+\\.js," +
             //               ".+\\.properties").split(" ");

        ArgumentParser parser = newFor("tools.FolderEncoder").build()
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
        File gitIgnoreFile = new File(".gitignore");
        FileUtils.copyFile(gitIgnoreFile,  new File(folder.getAbsoluteFile()+"/"+gitIgnoreFile.getName()));
        encode(folder, ns.getString("encoding"), in_exts_, ex_exts_);
        System.out.println("----> Task ends : "+nbrVisitedFiles+" files visited, re-encoding "+nbrFiles+" files.");
    }

    public static void encode(File folder, String targetEncoding, String[] in_exts, String[] ex_exts) {
        for (File file :folder.listFiles()) {
            if (!FileFilter.match(in_exts, ex_exts, file, true)) {
                continue;
            }

            CharsetMatch charsetMatch = null;
            if (file.isDirectory())
                encode(file, targetEncoding, in_exts, ex_exts);
            else {
                nbrVisitedFiles++;
                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    charsetMatch = charsetDetector.setText(is).detect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (charsetMatch == null) {
                    System.out.println("Charset not detected for file " + file.getAbsolutePath());
                } else { // if (!charsetMatch.getName().equals(targetEncoding))
                    System.out.println("Changing file encoding " + file.getAbsolutePath() + " from " + charsetMatch.getName()+" to "+targetEncoding);
                    String fileName = file.getAbsolutePath();
                    try {
                        String content = FileUtils.readFileToString(new File(fileName), charsetMatch.getName());
                        String header = "<%@ page contentType=\"text/html; charset="+targetEncoding+"\" %>\n";
                        if (fileName.endsWith(".jsp")) {
                            String str = content;
                            int start = str.indexOf("<%@ page");
                            if ( start != -1 ) {
                                String currentHeader = str.substring(start, str.indexOf("%>", start)+2);
                                String restOfFile = str.substring(str.indexOf("%>", start)+2);
                                if (currentHeader.contains("contentType")) {
                                    if (currentHeader.contains("text/html") && !currentHeader.contains("charset"))
                                        throw new Exception("special case "+file.getAbsolutePath());
                                    currentHeader = fixPageEncoding(targetEncoding, currentHeader);
                                } else {
                                    currentHeader = currentHeader.replace("%>", " contentType=\"text/html; charset=" + targetEncoding + "\" %>");
                                }
                                content = str.substring(0, start) + currentHeader + restOfFile;
                                content = fixBlancPageURL(content);
                            } else
                                content = header + content;
                        } else if (fileName.endsWith(".java")) {
                            content = fixBlancPageURL(content);
                        }
                        content = fixHostsNames(content);
                        content = fixPageEncoding(targetEncoding, content);
                        while (content.startsWith("null"))
                            content = content.substring(4);
                        StringBuffer buffer = new StringBuffer();
                        /*
                        char[] cs = content.toCharArray();
                        for (int i=0; i<content.length(); i++) { // special char
                            char c = cs[i];
                            if (c == '\'' && i + 1 < content.length()) {
                                if (cs[i+1] >= 192 && cs[i+1] <= 255) {
                                    String before = content.substring(i-8, i);
                                    if (before.equals("replace(")){
                                        buffer.append("(char)" + (int)cs[i+1]);
                                        i+=2;
                                    } else
                                        buffer.append(c);
                                } else
                                    buffer.append(c);
                            } else
                                buffer.append(c);
                        }*/

                        FileUtils.write(new File(fileName), content, targetEncoding);
                        if (fileName.endsWith("_fr.properties") || fileName.endsWith("_en.properties")) {
                            String newName = fileName.replace("_fr.properties", "_fr.utf8.properties");
                            newName = newName.replace("_en.properties", "_en.utf8.properties");
                            if (new File(newName).exists()) {
                                new File(fileName).delete();
                            }            else  new File(fileName).renameTo(new File(newName));
                        }
                        nbrFiles++;
                    } catch (Exception e) {

                    }

                }
            }
        }
    }

    private static String fixBlancPageURL(String content) {
        return content.replaceAll("/blanc.html", "/pages/blanc.html");
    }

    private static String fixHostsNames(String content) {
        content = StringUtils.replaceEach(content, hosts.toArray(new String[0]), hosts_rex.toArray(new String[0]));
        content = StringUtils.replaceEach(content, hosts2.toArray(new String[0]), hosts_rex.toArray(new String[0]));
        content = StringUtils.replaceEach(content, shortHosts.toArray(new String[0]), hosts_rex.toArray(new String[0]));
        content = StringUtils.replaceEach(content, shortHosts2.toArray(new String[0]), hosts_rex.toArray(new String[0]));
        content = StringUtils.replaceEach(content, hosts_rex.toArray(new String[0]), hosts.toArray(new String[0]));
    //    content = StringUtils.replaceEach(content, false_hosts_rex.toArray(new String[0]), hosts.toArray(new String[0]));
    //    content = StringUtils.replaceEach(content, false_hosts_rex2.toArray(new String[0]), hosts.toArray(new String[0]));
        return content;
    }

    private static String fixPageEncoding(String targetEncoding, String content) {
        //big error
        return content.replaceAll("ISO-8859-1", targetEncoding).replaceAll("iso-8859-1", targetEncoding).replaceAll("windows-1250", targetEncoding).replaceAll("Cp1252",
                targetEncoding).replaceAll(
                "windows-1252", targetEncoding);
        //.replaceAll("text/html", "text/html; charset="+targetEncoding)
    }
}
