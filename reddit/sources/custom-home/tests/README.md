Host tests validate normalization independently of Android. They do not test Android navigation.
Run:
  ../tools/jdk21/bin/javac --release 17 -cp build/extension -d build/tests tests/SubredditNameTest.java
  ../tools/jdk21/bin/java -cp build/extension:build/tests SubredditNameTest
