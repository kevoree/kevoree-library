package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.npm.resolver.NpmResolver;

import java.io.File;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by leiko on 23/01/14.
 */
public class NpmMergeService implements MergeService {

    @Override
    public ContainerRoot process(Collection<Library> libraries) {
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
                        ZipFile tarball = new ZipFile(new File(resolved.getAbsolutePath()));
                        ZipEntry entry = tarball.getEntry("kevlib.json");
                        if (entry != null) {
                            ContainerRoot remoteModel = (ContainerRoot) loader.loadModelFromStream(tarball.getInputStream(entry)).get(0);
                            compare.merge(model, remoteModel).applyOn(model);
                        }
                    } else {
                        Log.warn("Can't resolve TypeDefinition {}:{}", lib.getArtifactId(), version);
                    }
                } catch (Exception e) {
                    Log.error(e.getMessage());
                }
            }
        }

        return model;
    }
}
