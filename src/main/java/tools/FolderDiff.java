package tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.rac021.charset.validator.CharsetDetector;
import org.apache.commons.io.FileUtils;
import org.apache.tika.parser.txt.CharsetMatch;
import text.DiffPatch;
import text.DiffPatch.Patch;
import util.FileFilter;
import util.Serializer;

public class FolderDiff {
    static int nbrFiles = 0;
    static int nbrVisitedFiles = 0;

    static OutputStream diffLog;
    static File diffFolder = new File("C:/APPLIS/DIFF");
    static DiffPatch diffPatch = new DiffPatch();
    private static Serializer<LinkedList<Patch>> serializer = new Serializer<>();
    static Set<String> constants = new HashSet<>();
    private static CharsetDetector charsetDetector = new CharsetDetector();
    public static File fromFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy");
    private static boolean cleanCodeReformat = false;
    public static String include =  "";//".*pom\\.xml";
    public static void recusifDelete(String path)
            throws IOException, InterruptedException {
        if (!new File(path).exists())
            return;
        Path pathToBeDeleted = Paths.get(path);

        Files.walk(pathToBeDeleted)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        Thread.sleep(1000);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        File toFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2");
        folderDiff(diffFolder, fromFolder, toFolder,(include).split(","), (FolderEncoder.exclude).split(","));
    }

    public static void folderDiff(File diffFolder, File fromFolder, File toFolder, String[] in_exts, String[] ex_exts) throws IOException, InterruptedException {
        String diffLogPath= diffFolder.getAbsolutePath()+".log";
        new File(diffLogPath).delete();
        diffLog = new BufferedOutputStream(new FileOutputStream(diffLogPath));
        recusifDelete( diffFolder.getAbsolutePath() );
        diffFolder.mkdirs();

        folderDiff_(diffFolder, fromFolder, toFolder,  in_exts, ex_exts);

        System.out.println("----> Task ends : "+nbrVisitedFiles+" files visited, re-encoding "+nbrFiles+" files.");
        diffLog.close();
    }

    private static void folderDiff_(File diffFolder, File fromFolder, File toFolder, String[] in_exts, String[] ex_exts) throws IOException {
        processFolderDiff(diffFolder, fromFolder.getAbsolutePath(), fromFolder, toFolder, in_exts, ex_exts, true);
        processFolderDiff(diffFolder, toFolder.getAbsolutePath(), toFolder, fromFolder, in_exts, ex_exts, false);
        System.out.println(Arrays.toString(constants.toArray()));
    }

    private static void processFolderDiff( File diffFolder, String rootFolder, File folder, File folder2, String[] in_exts, String[] ex_exts,
                                           boolean firstPass) throws IOException {
        for (File file :folder.listFiles()) {
            if (!FileFilter.match(in_exts, ex_exts, file, true)) {
                continue;
            }
            System.out.println("Processing "+file.getAbsolutePath());
            File otherFile = new File(folder2.getAbsoluteFile()+"/"+file.getName());
            String destPath = diffFolder.getAbsolutePath() + file.getAbsolutePath().replace(rootFolder, "");
            File targetFile = getTargetFile(firstPass, otherFile, destPath);
            if (file.isDirectory()) {
                if (otherFile.exists()) {
                    processFolderDiff(diffFolder, rootFolder, file, otherFile, in_exts, ex_exts, firstPass);
                } else {
                    if (firstPass) {
                        FileUtils.forceMkdirParent(targetFile);
                    }else if (FileFilter.match(in_exts, ex_exts, file, false))
                        FileUtils.copyDirectory(file, targetFile);
                }
            } else {
                nbrVisitedFiles++;
                if (otherFile.exists()) {
                    String content1 = getContent(file, cleanCodeReformat);
                    if (file.getName().indexOf(".")!=-1)
                        constants.add( file.getName().substring(file.getName().lastIndexOf(".")) );
                    if (firstPass){
                        String content2 = getContent(otherFile, cleanCodeReformat);
                        if (cleanCodeReformat(content1).equals(cleanCodeReformat(content2)))
                           continue;
                        LinkedList<Patch> patchs = diffPatch.patch_make(content1, content2);
                        LinkedList<Patch> cleanPatchs = new LinkedList<>();
                        for (Patch patch :patchs) {
                            for ( int i=0; i<patch.diffs.size(); i++ ) {
                                DiffPatch.Diff diff = patch.diffs.get(i);
                                if (!diff.operation.equals(DiffPatch.Operation.EQUAL))
                                    if (!cleanCodeReformat(diff.text).isEmpty()) {
                                        if (diff.operation.equals(DiffPatch.Operation.DELETE) && i+1<patch.diffs.size()) {
                                            DiffPatch.Diff nextDiff = patch.diffs.get(i+1);
                                            if (nextDiff.operation.equals(DiffPatch.Operation.INSERT))
                                                if (cleanCodeReformat(diff.text).equals(cleanCodeReformat(nextDiff.text))) {
                                                    i++;
                                                    break;
                                                }
                                        }
                                        cleanPatchs.add( patch );
                                        break;
                                    }
                            }
                        }
                        if ( cleanPatchs.size()>0 ) {
                            diffLog.write((file.getAbsolutePath()+"\n").getBytes());
                            System.out.println(file.getAbsolutePath());
                            for (Patch p : cleanPatchs) {
                                System.out.println(p);
                                diffLog.write(p.toString().getBytes());
                            }
                            FileUtils.forceMkdirParent(targetFile);
                            serializer.serialize(patchs, targetFile.getAbsolutePath());
                        }
                    }
                } else {
                    if (firstPass) {
                        FileUtils.forceMkdirParent(targetFile);
                        targetFile.createNewFile();
                    } else {
                        try {
                            FileUtils.copyFile(file, targetFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static File getTargetFile(boolean firstPass, File otherFile, String destPath) {
        return new File(destPath + (otherFile.exists()? PatchOperation.patch:
                (firstPass? PatchOperation.deleted: PatchOperation.added)));
    }

    public static String getContent(File file, boolean cleanCodeReformat) throws IOException {
        String charset = getCharset(file);
        String content = FileUtils.readFileToString(file, charset);
        return cleanCodeReformat?cleanCodeReformat(content):content;
    }

    public static String cleanCodeReformat(String code){
        return code
                    .replaceAll(" ", "").replaceAll("\r", "")
                    .replaceAll("\n", "").replaceAll("\t", "")
                    .replaceAll("\\*", "").replaceAll("\\{", "")
                    .replaceAll("\\}", "").replaceAll("/", "");
    }

    public static String getCharset(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        CharsetMatch charsetMatch = FolderEncoder.charsetDetector.setText( new BufferedInputStream(fis) ).detect();
        String charset = charsetMatch.getName();
        if (charsetMatch.getConfidence()<50) {
            Charset charset_ = charsetDetector.detectCharset(file.getAbsolutePath());
            if (charset_!=null)
                charset = charset_.name();
        }

        if (!charset.equals("UTF-8"))
            charset = "ISO-8859-1";
        return charset;
    }

}
