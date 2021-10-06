build:
	mkdir out out/production out/production/RabbitMQ && javac -cp ".:./lib/rabbitmq.jar" src/*.java -d out/production/RabbitMQ

clean:
	rm -r out

runMain:
	java -cp "out/production/RabbitMQ/.:./lib/rabbitmq.jar" Main 

runWorker:
	java -cp "out/production/RabbitMQ:./lib/rabbitmq.jar" Worker cities/tampa.osm
