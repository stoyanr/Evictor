rem @echo off

call git checkout master

call mvn clean install -DskipTests

call git checkout gh-pages

cd evictor
rd /s /q javadoc
md javadoc
xcopy /s target\site\apidocs\* javadoc
xcopy /s target\*.jar lib
cd ..

call git add .
call git commit -m "Updated javadoc and binaries"
call git push origin gh-pages

call git checkout master