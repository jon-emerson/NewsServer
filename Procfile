web:       with_jmap java $JAVA_OPTS -cp target/classes:target/dependency/* -Dcom.sun.media.imageio.disableCodecLib=true -javaagent:newrelic/newrelic.jar -Dnewrelic.config.file=newrelic.yml com.janknspank.server.NewsServer
crawl:     sh target/bin/ArticleCrawler
prune:     sh target/bin/PruneMongoDatabase
pushdaily: sh target/bin/PushDailyNotifications
updatesocialengagements: sh target/bin/UpdateSocialEngagements

