package org.kevoree.library.java.editor.service.merge;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.kevoree.ContainerRoot;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.npm.resolver.NpmResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Created by leiko on 23/01/14.
 */
public class NpmMergeService implements MergeService {

    @Override
    public ContainerRoot process(Collection<Library> libraries, Set<String> repos) {
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        ContainerRoot model = factory.createContainerRoot();
        DefaultModelCompare compare = new DefaultModelCompare();
        JSONModelLoader loader = new JSONModelLoader();
        NpmResolver resolver = new NpmResolver();

        for (Library lib : libraries) {
            for (String version : lib.getVersions()) {
                try {
                    File resolved = resolver.resolve(lib.getArtifactId(), version);
                    if (resolved != null && resolved.exists()) {
                        TarArchiveInputStream tarInput = new TarArchiveInputStream(new FileInputStream(resolved));
                        try {
                            TarArchiveEntry entry;
                            while (null!=(entry=tarInput.getNextTarEntry())) {
                                if (entry.getName().equals("package/kevlib.json")) {
                                    int entrySize = (int) entry.getSize();
                                    byte[] content = new byte[entrySize];
                                    tarInput.read(content, 0, entrySize);
                                    ContainerRoot pkgModel = (ContainerRoot) loader.loadModelFromStream(new ByteArrayInputStream(content)).get(0);
                                    compare.merge(model, pkgModel).applyOn(model);
                                }
                            }
                        } catch (Exception e) {
                            Log.error(e.getMessage());
                        } finally {
                            tarInput.close();
                        }
                    } else {
                        Log.warn("NpmMergeService: Can't resolve TypeDefinition {}:{}", lib.getArtifactId(), version);
                    }
                } catch (Exception e) {
                    Log.error("NpmMergeService: "+e.getMessage());
                }
            }
        }

        return model;
    }
}
