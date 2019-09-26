import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import tools.FolderDiff;
import tools.FolderEncoder;

public class INT_OPE_PATCH {
    //static File fromFolder = new File("C:\\APPLIS\\eco-legacy");
    //static File targetFolder = new File("C:\\APPLIS\\eco-legacy2");
    static File fromFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy");
    static File targetFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2");
    static File diffFolder = new File("C:/APPLIS/DIFF2");

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        if (true) {
            //FolderEncoder.encode(fromFolder, "UTF-8", null, FolderEncoder.exclude.split(","));
            FolderDiff.folderDiff(diffFolder, fromFolder, targetFolder, null, FolderEncoder.exclude.split(","));
        } else {
           // File gitIgnoreFile = new File(".gitignore");
          //  FileUtils.copyFile(gitIgnoreFile,  new File(fromFolder.getAbsoluteFile()+"/"+gitIgnoreFile.getName()));
            FolderEncoder.encode( fromFolder, "UTF-8", null, FolderEncoder.exclude.split(","));
            FolderPatch.patchFolder( fromFolder, diffFolder,  null, FolderEncoder.exclude.split(","));
        }
    }
}
