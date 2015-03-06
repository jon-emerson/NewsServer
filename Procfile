web:       java $JAVA_OPTS -cp target/classes:target/dependency/* -Dcom.sun.media.imageio.disableCodecLib=true com.janknspank.server.NewsServer
prune:     sh target/bin/PruneMongoDatabase
crawl:     java $JAVA_OPTS -cp target/classes:target/dependency/* -Xdebug -Xrunjdwp:transport=dt_socket,address=jonemerson.ddns.net:9999 -Dcom.sun.media.imageio.disableCodecLib=true com.janknspank.crawler.ArticleCrawler
pushdaily: sh target/bin/PushDailyNotifications
