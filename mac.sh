path=`pwd`
mac=$(ifconfig | grep 'HWaddr' |awk '{print $5}' | head -n 1)
echo $mac
sed -ie s/^paasmerId=.*/paasmerId=$PAASMER/ $path/src/main/resources/com/paasmer/devicesdkjava/util/config.properties
