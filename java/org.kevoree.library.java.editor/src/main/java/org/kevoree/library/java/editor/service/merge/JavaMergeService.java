package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.api.BootstrapService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.json.JSONModelLoader;

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

    private BootstrapService bootstrapService;

    public JavaMergeService(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;

    }

    @Override
    public ContainerRoot process(Collection<Library> libraries, Set<String> repos) {
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        ModelCompare compare = new ModelCompare(factory);
        JSONModelLoader loader = new JSONModelLoader(factory);
        ContainerRoot model = factory.createContainerRoot();

        for (Library lib : libraries) {
            for (String version : lib.getVersions()) {
                File resolved = bootstrapService.resolve("mvn:"+lib.getGroupId() + ":" + lib.getArtifactId() + ":" + version + ":" + "jar", repos);
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
