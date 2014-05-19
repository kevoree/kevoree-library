FROM        ubuntu:trusty
MAINTAINER  Maxime Tricoire <max.tricoire@gmail.com>

# conf openssh-server
RUN         apt-get install -y openssh-server
RUN         mkdir /var/run/sshd
RUN         echo 'root:screencast' |chpasswd

# expose container's port 22 & start SSH daemon
EXPOSE      22
CMD         /usr/sbin/sshd -D