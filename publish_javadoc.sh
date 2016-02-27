#!/bin/sh

git checkout master

mvn clean install -DskipTests

git checkout gh-pages

rm -rf javadoc
mkdir javadoc
cp target/site/apidocs/* javadoc/*

git add .
git commit -m "Updated javadoc"
git push origin gh-pages

git checkout master