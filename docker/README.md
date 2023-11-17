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

	$ docker run --name="df-bpmn" --rm -p 3000:3000 -p 3001:3001 alinoureldin/df-bpmn --env OPENAI_KEY=<open-api-key>
      
After starting the container the applicaiton is available on 

	http://localhost:3000/#/usr/src/app/workspace
	      
To stop the container run:

	$ docker stop df-bpmn
## Workspace
For the integration with Bonita studio, you should add `-v` to mount the workspace of bonita project within the docker image

	$ docker run --name="df-bpmn" --rm -p 3000:3000 -p 3001:3001  --env OPENAI_KEY=<open-api-key> -it -v /path/to/bonita-workspace:/usr/src/app/bonita alinoureldin/df-bpmn

When you need to export a DF-BPMN project to Bonita proc, you should add the `/bonita-project-name/` and the BDM of bonita will automatically imported, and the export of DF-BPMN will be within download within your Bonita project.

# Push to Docker-Hub

To push the image manually to a docker repo:

	$ docker build . -t alinoureldin/df-bpmn:latest
	$ docker push alinoureldin/df-bpmn:latest

