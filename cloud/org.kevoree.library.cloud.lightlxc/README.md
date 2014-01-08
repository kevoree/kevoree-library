sudo apt-get -y update
sudo apt-get -y install lxc
sudo apt-get -y install software-properties-common
echo | sudo add-apt-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install oracle-java7-installer
sudo apt-get -y curl
curl -L "http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=org.kevoree.watchdog&a=org.kevoree.watchdog&v=RELEASE&p=deb" > org.kevoree.watchdog.deb
sudo dpkg -i org.kevoree.watchdog*.deb


sudo service kevoree start