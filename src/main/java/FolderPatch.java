import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import text.DiffPatch;
import tools.FolderDiff;
import tools.FolderEncoder;
import tools.PatchOperation;
import util.Serializer;

public class FolderPatch {
    static int nbrFiles = 0;
    static int nbrVisitedFiles = 0;
    static File diffFolder = new File("C:/APPLIS/DIFF");
    static DiffPatch diffPatch = new DiffPatch();
    private static Serializer<LinkedList<DiffPatch.Patch>> serializer = new Serializer<>();
    static Set<String> constants = new HashSet<>();
    static File targetFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2");
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        patchFolder(targetFolder, diffFolder, null, (FolderEncoder.exclude).split(","));
        System.out.println("----> Task ends : "+nbrVisitedFiles+" files visited, re-encoding "+nbrFiles+" files.");
    }

    public static void patchFolder( File targetFolder, File diffFolder, String[] in_exts, String[] ex_exts) throws IOException, ClassNotFoundException {
        patchFolder_(targetFolder, new File(diffFolder.getAbsolutePath()), diffFolder, in_exts, ex_exts);
        System.out.println(Arrays.toString(constants.toArray()));
    }

    private static void patchFolder_(File targetFolder, File diffFolder, File folder, String[] in_exts, String[] ex_exts) throws IOException,
            ClassNotFoundException {
        String pathSuffix =
                            folder.getAbsolutePath().substring(diffFolder.getAbsolutePath().length());
        pathSuffix = cleanFromPatchOperation(pathSuffix);
        for (File file :folder.listFiles()) {
            File targetFile = new File( targetFolder.getAbsolutePath() + pathSuffix + "/" + cleanFromPatchOperation(file.getName()));
            if (file.isDirectory()) {
                if (file.getName().endsWith(PatchOperation.added.toString())) {
                    FileUtils.copyDirectory(file, targetFile);
                } else if (file.getName().endsWith(PatchOperation.deleted.toString())) {
                    FileUtils.deleteDirectory(targetFile);
                }
                else
                    patchFolder_(targetFolder, diffFolder, file, in_exts, ex_exts);
            } else {
                if (file.getName().endsWith(PatchOperation.replace.toString())) {
                    FileUtils.copyFile(file, targetFile);
                }else
                if (file.getName().endsWith(PatchOperation.added.toString())) {
                    FileUtils.copyFile(file, targetFile);
                } else if (file.getName().endsWith(PatchOperation.deleted.toString())) {
                    targetFile.delete();
                } else {
                    try {
                        LinkedList<DiffPatch.Patch> patchs = serializer.deserialize(file.getAbsolutePath());
                        String charset = FolderDiff.getCharset(targetFile);
                        String newContent = (String) diffPatch.patch_apply(patchs, FolderDiff.getContent(targetFile, false))[0];
                        FileUtils.write(targetFile, newContent, charset);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }
    }

    private static String cleanFromPatchOperation(String pathSuffix) {
        for (PatchOperation patchOperation : PatchOperation.values())
            pathSuffix = pathSuffix.replace(patchOperation.toString(), "");
        return pathSuffix;
    }

}
