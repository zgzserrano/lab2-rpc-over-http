Firstly I have create 'work' branch using git checkout -b work. Then I have included the branch name in ci.yml.

To make a workable server i have implemented the missing code and finally i check it works.



Server
./gradlew :server:build 
./gradlew :server:bootRun

Client
./gradlew :client:build 
./gradlew :client:bootRun

Server has responsed "traduceme" so it works
