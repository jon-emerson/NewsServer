web:       java $JAVA_OPTS -cp target/classes:target/dependency/* -Dcom.sun.media.imageio.disableCodecLib=true com.janknspank.server.NewsServer
prune:     sh target/bin/PruneMongoDatabase
crawl:     sh target/bin/ArticleCrawler
pushdaily: sh target/bin/PushDailyNotifications
