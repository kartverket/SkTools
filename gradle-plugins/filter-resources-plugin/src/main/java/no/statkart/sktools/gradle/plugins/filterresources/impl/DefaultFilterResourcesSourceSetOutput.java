package no.statkart.sktools.gradle.plugins.filterresources.impl;

import no.statkart.sktools.gradle.plugins.filterresources.FilterResourcesSourceSetOutput;
import org.gradle.api.internal.file.CompositeFileCollection;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection;
import org.gradle.api.internal.file.collections.FileCollectionResolveContext;
import org.gradle.api.internal.tasks.TaskResolver;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Plugin extension for {@link org.gradle.api.tasks.SourceSetOutput}
 * @author Leif Lislegård
 */
public class DefaultFilterResourcesSourceSetOutput extends CompositeFileCollection implements FilterResourcesSourceSetOutput {

    private DefaultConfigurableFileCollection outputDirectories;
    private Object filteredResourcesDir;
    private FileResolver fileResolver;


    String sourceSetDisplayName;

    public DefaultFilterResourcesSourceSetOutput(String sourceSetDisplayName, FileResolver fileResolver, TaskResolver taskResolver) {
        this.fileResolver = fileResolver;
        this.sourceSetDisplayName = sourceSetDisplayName;

        this.fileResolver = fileResolver;
        String displayName = String.format("%s filtered output", sourceSetDisplayName);
        outputDirectories = new DefaultConfigurableFileCollection(displayName, fileResolver, taskResolver, new Callable() {
            public Object call() throws Exception {
                return getFilterResourcesOutputDir();
            }
        });

    }

    @Override
    public void resolve(FileCollectionResolveContext context) {
        context.add(outputDirectories);
    }

    @Override
    public FilterResourcesSourceSetOutput filterResourcesOutput(Object filteredResourcesDir) {
        this.filteredResourcesDir = filteredResourcesDir;
        return this;
    }

    @Override
    public File getFilterResourcesOutputDir() {
        if (filteredResourcesDir == null) {
            return null;
        }
        return fileResolver.resolve(filteredResourcesDir);
    }

    @Override
    public String getDisplayName() {
        return outputDirectories.getDisplayName();
    }

}
