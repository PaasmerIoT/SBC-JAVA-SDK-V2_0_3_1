
#!/bin/bash
#
# existing versions are not overwritten/deleted
# seamless upgrades/downgrades
# $M2_HOME points to latest *installed* (not released)

# config
mirror=http://mirror.cc.columbia.edu/pub
#mirror=http://www-us.apache.org/dist
mvnversion=3.5.0
p=`pwd`
# install
mvnidentifier="apache-maven-${mvnversion}"
wget -N "${mirror}/software/apache/maven/maven-3/${mvnversion}/binaries/${mvnidentifier}-bin.tar.gz"
sudo tar xzf ${mvnidentifier}-bin.tar.gz 
#sudo ln -sfn "/opt/${mvnidentifier}" /opt/maven/latest
sudo chmod 777 /etc/profile.d/maven.sh
sudo printf "export M2_HOME=$p/$mvnidentifier\nexport PATH=\$PATH:\$M2_HOME/bin" > /etc/profile.d/maven.sh
. /etc/profile.d/maven.sh

# check
mvn -version

