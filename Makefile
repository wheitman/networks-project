JC =javac
.SUFFIXES:.java .class
.java.class:
	$(JC) $*.java

CLASSES = \
	Server.java\
	Client.java \
	Main.java

default:CLASSES

classes:$(CLASSES:.java=.class)

clean:\
	$(RM) *.class