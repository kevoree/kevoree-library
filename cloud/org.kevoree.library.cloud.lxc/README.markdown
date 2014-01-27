# LXC Node type

This Kevoree node allows you to manage LXC container using Kevoree

For more information about LXC, please visit the [LXC website](http://linuxcontainers.org/)

## Physical Host configuration

We provide a set of scripts to install LXC and Kevoree on Ubuntu system (currently test on Ubuntu 13.10):

```bash
wget "https://raw2.github.com/kevoree/kevoree-library/master/cloud/org.kevoree.library.cloud.lxc/host-config/lxc-install" --content-disposition
wget "https://raw2.github.com/kevoree/kevoree-library/master/cloud/org.kevoree.library.cloud.lxc/host-config/kevoree-install" --content-disposition

sudo bash lxc-install
sudo bash kevoree-install
```
You must answer some questions about the versions of Kevoree stuff you want to use. If you don't know what is the version you need, please use the *RELEASE* as answer to get the most recent stable versions.

After running this command lines, you can start the Kevoree service using the following line:
```bash
sudo service kevoree start
```
For more information about the Kevoree service, please visit the [Kevoree watchdog website](https://github.com/dukeboard/kevoree-watchdog).

You can also take a look to the [kevoree cloud web page](https://github.com/kevoree/kevoree-library/tree/master/cloud/org.kevoree.library.cloud.web) which is installed by default and available on http://&lt;ip&gt;:8080

