rem @echo off

call git checkout master

call mvn clean install -DskipTests

call git checkout gh-pages

rd /s /q javadoc
md javadoc
xcopy /s target\apidocs\* javadoc

call git add .
call git commit -m "Updated javadoc"
call git push origin gh-pages

call git checkout master