package org.kevoree.library.cloud.docker.docker

import java.io.File
import java.io.BufferedWriter
import java.nio.file.Files
import org.kevoree.serializer.JSONModelSerializer
import org.kevoree.ContainerRoot
import java.io.FileWriter

/**
 * Created by leiko on 20/05/14.
 */
class Dockerfile(model : ContainerRoot, rootPasswd : String) {

    val model : ContainerRoot = model

    val content : String = """
        FROM        phusion/baseimage:0.9.10

        # Set correct environment variables.
        ENV         HOME /root
        WORKDIR     /root

        # Use baseimage-docker's init system.
        CMD         ["/sbin/my_init"]

        # add webupd8team ppa (to install Java from official sources)
        RUN         echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list
        RUN         echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list
        RUN         apt-key adv --recv-keys --keyserver keyserver.ubuntu.com C2518248EEA14886
        RUN         apt-get update
        RUN         echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
        RUN         echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
        RUN         apt-get install -y oracle-java7-installer

        # ssh conf
        RUN         sed -i \
                        -e 's/^#*\(PermitRootLogin\) .*/\1 yes/' \
                        -e 's/^#*\(PasswordAuthentication\) .*/\1 yes/' \
                        -e 's/^#*\(UsePAM\) .*/\1 no/' \
                        /etc/ssh/sshd_config
        RUN         echo "root:$rootPasswd"|chpasswd

        # add Kevoree Java platform
        RUN         ["wget", "http://oss.sonatype.org/service/local/artifact/maven/redirect?r=public&g=org.kevoree.platform&a=org.kevoree.platform.standalone&v=RELEASE", "-O", "/root/kevoree.jar"]

        # add bootstrap model from host to container
        ADD         boot.json /root/boot.json

        # Clean up APT when done.
        RUN         apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
    """

    fun getFile() : File {
        var writer : BufferedWriter

        // create Dockerfile folder
        var dfileFolderPath = Files.createTempDirectory("docker_")
        var dfileFolder : File = File(dfileFolderPath.toString())

        // retrieve current model and serialize it to JSON
        var serializer = JSONModelSerializer()
        var modelJson = serializer.serialize(model)!!

        // create temp model
        var modelFile : File = File(dfileFolder, "boot.json")
        writer = BufferedWriter(FileWriter(modelFile))
        writer.write(modelJson)
        writer.close()

        // create Dockerfile
        var dockerFile : File = File(dfileFolder, "Dockerfile")
        writer = BufferedWriter(FileWriter(dockerFile))
        writer.write(content)
        writer.close()

        return dfileFolder
    }
}