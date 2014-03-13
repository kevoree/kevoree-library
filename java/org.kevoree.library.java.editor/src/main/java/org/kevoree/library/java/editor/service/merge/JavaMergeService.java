package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.loader.JSONModelLoader;
import org.kevoree.log.Log;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 22/01/14
 * Time: 18:50
 */
public class JavaMergeService implements MergeService {

    @Override
    public ContainerRoot process(Collection<Library> libraries, List<String> repos) {
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        DefaultModelCompare compare = new DefaultModelCompare();
        JSONModelLoader loader = new JSONModelLoader();
        ContainerRoot model = factory.createContainerRoot();
        MavenResolver resolver = new MavenResolver();
        Set<String> repositories = new HashSet<String>();
        repositories.addAll(repos);

        for (Library lib : libraries) {
            for (String version : lib.getVersions()) {
                File resolved = resolver.resolve(lib.getGroupId(), lib.getArtifactId(), version, "jar", repositories);
                if (resolved != null && resolved.exists()) {
                    try {
                        JarFile jar = new JarFile(new File(resolved.getAbsolutePath()));
                        JarEntry entry = jar.getJarEntry("KEV-INF/lib.json");
                        if (entry != null) {
                            ContainerRoot remoteModel = (ContainerRoot) loader.loadModelFromStream(jar.getInputStream(entry)).get(0);
                            compare.merge(model, remoteModel).applyOn(model);
                        }
                    } catch (IOException e) {
                        Log.error("Bad JAR file ", e);
                    }
                } else {
                    Log.warn("Can't resolve TypeDefinition {}:{}:{}", lib.getGroupId(), lib.getArtifactId(), version);
                }
            }
        }

        return model;
    }
}
