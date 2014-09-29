package org.kevoree.library.cloud.docker.wrapper;

import org.kevoree.ContainerRoot;
import org.kevoree.Dictionary;
import org.kevoree.TypeDefinition;
import org.kevoree.Value;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.library.DockerNode;
import org.kevoree.library.cloud.docker.client.DockerApi;
import org.kevoree.library.cloud.docker.client.DockerClientImpl;
import org.kevoree.library.cloud.docker.client.DockerException;
import org.kevoree.library.cloud.docker.model.*;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import us.monoid.json.JSONException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by duke on 9/29/14.
 */
public class DockerNodeWrapper extends KInstanceWrapper {

    private DockerNode dockerNode;

    public DockerNodeWrapper(DockerNode d) {
        this.dockerNode = d;
    }

    private DockerClientImpl docker = new DockerClientImpl();
    private String containerID = null;
    private HostConfig hostConf = null;

    @Override
    public boolean kInstanceStart(ContainerRoot model) throws DockerException, JSONException {
        if (containerID != null) {
            final String id = containerID.substring(0, 12);
            // attach container stdout/stderr to System.out/err
            final InputStream stream = docker.attach(containerID, false, true, false, true, true);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        final InputStream final_stream = stream;
                        // https://docs.docker.com/reference/api/docker_remote_api_v1.13/#attach-to-a-container
                        // implementation of the Stream payload hack
                        byte[] header = new byte[8];
                        int read = final_stream.read(header);
                        PrintStream output;
                        while (read != -1) {
                            // detect STD type
                            if (header[0] == DockerApi.ATTACH_STDERR) {
                                output = System.err;
                            } else {
                                output = System.out;
                            }
                            // strip content from package (separate STD header from message content)
                            ByteBuffer bb = ByteBuffer.wrap(header, 4, 4);
                            byte[] payload = new byte[bb.getInt()];
                            if (stream.read(payload) > 0) {
                                String content = new String(payload);
                                // output logs properly
                                if (content != System.getProperty("line.separator")) {
                                    output.print("${modelElement.name} > ");
                                }
                                output.print(content);
                            }
                            // read what's left in the stream
                            read = stream.read(header);
                        }
                    } catch (IOException e) {
                        Log.error("DockerClient: attach({}) stream error", id.substring(0, 12));
                        Log.debug(e.getStackTrace().toString());
                    }
                }
            });
            t.start();

            // start container
            Log.info("Starting docker container {} ...", id);
            docker.start(containerID, hostConf);
            ContainerDetail detail = docker.getContainer(containerID);
            String ipAddress = detail.getNetworkSettings().getIpAddress();
            dockerNode.getModelService().submitScript("network ${modelElement.name}.lan.ip $ipAddress", new UpdateCallback() {
                @Override
                public void run(Boolean aBoolean) {

                }
            });
            Log.info("Docker container $id started at $ipAddress");
        }
        return true;
    }

    @Override
    public boolean kInstanceStop(ContainerRoot model) throws Exception {
        if (containerID != null) {
            String id = containerID.substring(0, 12);
            // stop container
            docker.stop(containerID);
            Log.info("Docker container $id successfully stopped");
            // commit container
            CommitConfig conf = new CommitConfig();
            conf.setContainer(containerID);
            String repo = dockerNode.getCommitRepo();
            if (repo == null) {
                throw new Exception("DockerNode attribute \"commitRepo\" must be set.");
            }
            String tag = dockerNode.getCommitTag();
            if (tag == null || tag.length() == 0) {
                tag = "latest";
            }
            conf.setRepo("$repo/${modelElement.name}");
            conf.setTag(tag);
            conf.setMessage(dockerNode.getCommitMsg());
            conf.setAuthor(dockerNode.getCommitAuthor());
            docker.commit(conf);
            Log.info("Container $id commited into ${conf.getRepo()}:$tag");

            // delete stopped container
            docker.deleteContainer(containerID);
            Log.info("Container ${containerID!!.substring(0, 12)} successfully deleted");
        }
        return true;
    }

    @Override
    public void create() throws Exception {
        ContainerConfig conf = new ContainerConfig();
        hostConf = new HostConfig();
        String repo = dockerNode.getCommitRepo();
        if (repo == null) {
            throw new Exception("DockerNode attribute \"commitRepo\" must be set.");
        }
        String tag = dockerNode.getCommitTag();
        if (tag == null || tag.length() == 0) {
            tag = "latest";
        }
        String imageName = "$repo/${modelElement.name}:$tag";
        // check if imageName is available locally
        if (isLocallyAvailable(imageName)) {
            // imageName is available locally: use it
            conf.setImage(imageName);
            if (isChildOf(getModelElement().getTypeDefinition(), "DockerNode")) {
                // child node is a DockerNode
                Dictionary dic = getModelElement().getDictionary();
                if (dic != null) {
                    configureDockerNode(conf);
                }

            } else if (isChildOf(getModelElement().getTypeDefinition(), "JavaNode")) {
                configureJavaNode(conf);
            }

        } else {
            // imageName is not available locally
            Log.info("Looking for $repo/${modelElement.name} on remote Docker registry...");
            List<ImageInfo> searchRes = docker.searchImage("$repo/${modelElement.name}");
            if (searchRes.size() > 0) {
                // image available remotely: pulling it
                docker.pull("$repo/${modelElement.name}");
                conf.setImage(imageName);
                if (isChildOf(getModelElement().getTypeDefinition(), "DockerNode")) {
                    // child node is a DockerNode
                    Dictionary dic = getModelElement().getDictionary();
                    if (dic != null) {
                        configureDockerNode(conf);
                    }

                } else if (isChildOf(getModelElement().getTypeDefinition(), "JavaNode")) {
                    configureJavaNode(conf);
                }
            } else {
                // imageName is not available remotely
                if (isChildOf(getModelElement().getTypeDefinition(), "DockerNode")) {
                    // child node is a DockerNode
                    Dictionary dic = getModelElement().getDictionary();
                    if (dic != null) {
                        Value childImage = dic.findValuesByID("image");
                        if (childImage != null && childImage.getValue() != null && childImage.getValue().length() > 0) {
                            conf.setImage(childImage.getValue());
                            configureDockerNode(conf);
                        } else {
                            throw new Exception("DockerNode cannot start a DockerNode if no image is set AND no commitRepo/childNode.name:commitTag image exists");
                        }
                    } else {
                        throw new Exception("DockerNode cannot start a DockerNode if no image is set AND no commitRepo/childNode.name:commitTag image exists");
                    }

                } else if (isChildOf(getModelElement().getTypeDefinition(), "JavaNode")) {
                    // use kevoree/watchdog image when child node is a JavaNode (or one of its subtypes excepted DockerNode)
                    conf.setImage("kevoree/watchdog");
                    configureJavaNode(conf);
                } else {
                    throw new Exception("DockerNode does not handle ${modelElement.typeDefinition!!.name} (only DockerNode & JavaNode)");
                }
            }
        }
//
//        // config for every containers
//        conf.setAttachStdout(true)
//        conf.setAttachStderr(true)

        // create container
        try {
            ContainerInfo contInfos = docker.createContainer(conf, getModelElement().getName());
            containerID = contInfos.getId();
        } catch (DockerException e) {
            ImageConfig imgConf = new ImageConfig();
            imgConf.setFromImage(conf.getImage().split(":")[0]);
            System.out.println(imgConf);
            docker.pull(imgConf);
            ContainerInfo contInfos = docker.createContainer(conf, getModelElement().getName());
            containerID = contInfos.getId();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (containerID != null) {
            // push image
            String repo = dockerNode.getCommitRepo();
            if (repo == null) {
                throw new Exception("DockerNode attribute \"commitRepo\" must be set.");
            }
            String tag = dockerNode.getCommitTag();
            if (tag == null || tag.length() == 0) {
                tag = "latest";
            }
            boolean pushOnDestroy = dockerNode.getPushOnDestroy();
            if (pushOnDestroy) {
                AuthConfig auth = new AuthConfig();
                auth.setUsername(dockerNode.getAuthUsername());
                auth.setPassword(dockerNode.getAuthPassword()); // FIXME protect password in kevoree model...
                auth.setEmail(dockerNode.getAuthEmail());
                auth.setServerAddress(dockerNode.getPushRegistry());
                docker.push("$repo/${modelElement.name}", tag, auth);
            }
        }
    }

    private boolean isChildOf(TypeDefinition source, String target) {
        if (source.getName().equals(target)) {
            return true;
        } else if (source.getSuperTypes().size() == 0) {
            return false;
        } else {
            for (TypeDefinition parent : source.getSuperTypes()) {
                if (parent.getName().equals(target)) {
                    return true;
                } else {
                    boolean isChild = isChildOf(parent, target);
                    if (isChild) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Checks whether or not given "image" is available locally in Docker
     *
     * @param {String} full image tag (repo/name:tag)
     * @return {Boolean} true if image is available locally; false otherwise
     */
    public boolean isLocallyAvailable(String image) throws DockerException, JSONException {
        List<Image> images = docker.getImages();
        for (Image img : images) {
            for (String tag : img.getRepoTags()) {
                if (tag.equals(image)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void configureDockerNode(ContainerConfig conf) {
        Value cpuSharesVal = getModelElement().getDictionary().findValuesByID("cpuShares");
        if (cpuSharesVal != null && cpuSharesVal.getValue() != null && cpuSharesVal.getValue().length() > 0) {
            conf.setCpuShares(Integer.parseInt(cpuSharesVal.getValue()));
        }
        Value memoryVal = getModelElement().getDictionary().findValuesByID("memory");
        if (memoryVal != null && memoryVal.getValue() != null && memoryVal.getValue().length() > 0) {
            conf.setMemoryLimit(Long.parseLong(memoryVal.getValue()) * 1024 * 1024); // memoryVal.value is in MBytes
        }
        Value cmdVal = getModelElement().getDictionary().findValuesByID("cmd");
        if (cmdVal != null && cmdVal.getValue() != null && cmdVal.getValue().length() > 0) {
            conf.setCmd(cmdVal.getValue().split(" "));
        }
    }

    public void configureJavaNode(ContainerConfig conf) throws IOException {
        ContainerRoot model = (ContainerRoot) getModelElement().eContainer();
        // create and store serialized model in temp dir
        File dfileFolder = new File(Files.createTempDirectory("kevoree_").toString());

        // retrieve current model and serialize it to JSON
        JSONModelSerializer serializer = new JSONModelSerializer();
        String modelJson = serializer.serialize(model);

        // create temp model
        File modelFile = new File(dfileFolder, "boot.json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
        writer.write(modelJson);
        writer.close();
        HashMap<String, Object> volumes = new HashMap<String, Object>();
        volumes.put(dfileFolder.getAbsolutePath(), new HashMap<String, String>());
        conf.setVolumes(volumes);
        hostConf.setBinds(Arrays.asList("${dfileFolder.getAbsolutePath()}:${dfileFolder.getAbsolutePath()}:rw").toArray(new String[0]));
        conf.setCmd(Arrays.asList(
                "java",
                "-Dnode.name=${modelElement.name}",
                "-Dnode.bootstrap=${modelFile.getAbsolutePath()}",
                "-jar",
                "/root/kevboot.jar",
                "release"
        ).toArray(new String[0]));
    }


}
