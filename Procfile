web:       java $JAVA_OPTS -cp target/classes:target/dependency/* -Dcom.sun.media.imageio.disableCodecLib=true com.janknspank.server.NewsServer
crawl:     sh target/bin/ArticleCrawler
prune:     sh target/bin/PruneMongoDatabase
pushdaily: sh target/bin/PushDailyNotifications
pinterest: sh target/bin/PinterestPinner
updatesocialengagements: sh target/bin/UpdateSocialEngagements
entityidtoindustryrelevances: sh target/bin/EntityIdToIndustryRelevances

