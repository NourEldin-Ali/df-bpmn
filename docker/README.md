# Docker

DF-BPMN provides a Docker image to run the BPMN modeler as a Container in Docker or Kubernetes. 

The DF-BPMN Docker image is based on the [official NodeJS image (node:16-buster)](https://hub.docker.com/_/node). The container image contains a prebuild appliction and exposes the port 3000

In the Dockerfile we are using start script as the entrypoint:

    ENTRYPOINT [ "/usr/src/app/start.sh" ]

**Note:** The start script is setting the environment param `--hostname=0.0.0.0` which is important to allow access form outside the container. Find more details also [here](https://dev.to/hagevvashi/don-t-forget-to-give-host-0-0-0-0-to-the-startup-option-of-webpack-dev-server-using-docker-1483) and [here](https://github.com/theia-ide/theia-apps/tree/master/theia-cpp-docker).

## Build

To build the docker image from sources run:

	$ docker build . -t alinoureldin/df-bpmn --build-arg OPENAI_KEY=<open-api-key>

## Run

To run the docker image locally run:

	$ docker run --env OPENAI_KEY=<open-api-key> --name="df-bpmn" --rm -p 3000:3000 alinoureldin/df-bpmn
      
After starting the container the applicaiton is available on 

	http://localhost:3000/#/usr/src/app/workspace
	      
To stop the container run:

	$ docker stop df-bpmn

# Push to Docker-Hub

To push the image manually to a docker repo:

	$ docker build . -t alinoureldin/df-bpmn:latest
	$ docker push alinoureldin/df-bpmn:latest

