package filetreeviewsample;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javafx.concurrent.Task;

/**
 * File copy Task
 * 
 * @author tomo
 */
public class FileCopyTask extends Task<Void> {
    private Path source;
    private Path target;
    private boolean isReplaceCopy;

    public FileCopyTask(Path source, Path target) {
        this.source = source;
        this.target = target;
    }
    
    @Override
    protected Void call() throws Exception {
        this.isReplaceCopy = Files.exists(this.target, LinkOption.NOFOLLOW_LINKS);
        Files.copy(this.source, this.target, StandardCopyOption.REPLACE_EXISTING);
        return null;
    }

    public boolean isReplaceCopy() {
        return isReplaceCopy;
    }
    
}
