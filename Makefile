build:
	javac -cp ".:./lib/rabbitmq.jar" src/*.java

clean:
	rm src/*.class

runMain:
	java -cp "src:./lib/rabbitmq.jar" Main cities/portland.osm

runWorker:
	java -cp "src:./lib/rabbitmq.jar" Worker cities/portland.osm
