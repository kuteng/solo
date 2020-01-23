
mvn clean package -DskipTests=true

# export RUNTIME_DB="MYSQL"
# exportJDBC_USERNAME="yandong"
# exportJDBC_PASSWORD="yandong"
# exportJDBC_DRIVER="com.mysql.cj.jdbc.Driver"
# exportJDBC_URL="jdbc:mysql://10.227.10.195:3306/soloblog?useUnicode=yes&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"

export RUNTIME_DB="H2"
export JDBC_USERNAME="root"
export JDBC_PASSWORD="123456"
export JDBC_DRIVER="org.h2.Driver"
export JDBC_URL="jdbc:h2:/opt/solo/h2/db;MODE=MYSQL"

cd target/solo

java -cp "lib/*:." org.b3log.solo.Server
