package org.kevoree.library.cloud.docker

import com.kpelykh.docker.client.DockerClient
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import org.kevoree.library.cloud.docker.docker.Dockerfile
import java.io.StringWriter
import org.apache.commons.io.IOUtils
import org.kevoree.log.Log
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.serializer.JSONModelSerializer
import java.nio.file.Files

/**
 * Created by leiko on 20/05/14.
 */
fun main(args: Array<String>) {
    var docker = DockerClient("http://localhost:4243");
    var writer : BufferedWriter

    // create Dockerfile folder
    var dfileFolderPath = Files.createTempDirectory("docker_")
    var dfileFolder : File = File(dfileFolderPath.toString())

    // empty model (to test boot.json creation)
    var factory = DefaultKevoreeFactory()
    var serializer = JSONModelSerializer()
    var model = factory.createContainerRoot()
    var modelJson = serializer.serialize(model)!!

    // create temp model
    var modelFile : File = File(dfileFolder, "boot.json")
    writer = BufferedWriter(FileWriter(modelFile))
    writer.write(modelJson)
    writer.close()

    // create Dockerfile
    var dockerFile : File = File(dfileFolder, "Dockerfile")
    writer = BufferedWriter(FileWriter(dockerFile))
    var dfile = Dockerfile("password")
    writer.write(dfile.content)
    writer.close()

    // build Docker image using Dockerfile
    var res = docker.build(dfileFolder)
    var logwriter = StringWriter()

    try {
        var itr = IOUtils.lineIterator(res!!.getEntityInputStream(), "UTF-8");
        while (itr!!.hasNext()) {
            var line = itr!!.next();
            logwriter.write(line);
            Log.info(line);
        }
    } finally {
        IOUtils.closeQuietly(res!!.getEntityInputStream());
    }
}