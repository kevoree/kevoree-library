sudo apt-get -y update
sudo apt-get -y install lxc
sudo apt-get -y install software-properties-common
echo | sudo add-apt-repository ppa:webupd8team/java
sudo apt-get -y update
sudo apt-get -y install oracle-java7-installer
sudo apt-get -y curl



brctl addbr br0
brctl addif br0 eth1
ifconfig br0 192.168.1.1 netmask 255.255.255.0 up

/sbin/ifconfig eth1 192.168.1.1 netmask 255.255.255.0 promisc up
echo 1 > /proc/sys/net/ipv4/ip_forward
iptables -t nat -A POSTROUTING -o wlan0 -j SNAT --to-source 10.20.41.39

