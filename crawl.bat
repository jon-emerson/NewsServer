:: @echo off
:: mvn package
:: if %errorlevel% neq 0 exit /b %errorlevel%
java -cp target\classes:"target\NewsServer-1.0-SNAPSHOT.jar:target\dependency\*" ^
    -Dfile.encoding=UTF8 ^
    -Xdebug ^
    -Xrunjdwp:transport=dt_socket,address=8009,server=y,suspend=n ^
    -Dcom.sun.media.imageio.disableCodecLib=true ^
    com.janknspank.ArticleCrawler