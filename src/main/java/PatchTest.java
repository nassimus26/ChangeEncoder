import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.http.util.Asserts;
import text.DiffPatch;
import tools.FolderDiff;
import tools.FolderEncoder;

import static text.DiffPatch.*;

public class PatchTest {
    private static DiffPatch diffPatch = new DiffPatch();
    public static void main(String[] args) throws IOException, InterruptedException {
        File diffFolder = new File("C:/APPLIS/DIFF");
        File fromFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy\\administration\\src\\main\\java\\com\\siris" +
                "\\extranet\\administration\\solDirAdm\\LdapUsers");
        File targetFolder = new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2\\administration\\src\\main\\java\\com\\siris" +
                "\\extranet\\administration\\solDirAdm\\LdapUsers");

        FolderDiff.folderDiff(diffFolder, fromFolder, targetFolder, null, FolderEncoder.exclude.split(","));
/*
        String content1 = FolderDiff.getContent( , true );
        String content2 = FolderDiff.getContent( new File("C:\\APPLIS\\HELIOSDEV\\IntelliJWorkspace\\eco-legacy2\\administration\\src\\main\\java\\com\\siris" +
                "\\extranet\\administration\\solDirAdm\\LdapUsers\\LdapUser.java"), true );

        LinkedList<Patch> patchs = diffPatch.patch_make(content1, content2);
        for (Patch p:patchs)
            System.out.println(p);
        String newContent2 = (String) diffPatch.patch_apply(patchs, content1)[0];
        Asserts.check(content2.equals(newContent2), "Fail");*/
    }
}
