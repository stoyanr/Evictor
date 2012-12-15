rem @echo off

call git checkout master

call mvn javadoc:javadoc

call git checkout gh-pages

cd evictor
rd /s /q javadoc
md javadoc
xcopy /s target\site\apidocs\* javadoc
cd ..

call git add .
call git commit -m "Updated javadoc"
call git push origin gh-pages

call git checkout master