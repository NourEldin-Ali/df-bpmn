[**DataFlow BPMN**](https://www.researchgate.net/publication/371043087_Zooming_in_for_Clarity_Towards_low-code_modeling_for_Activity_Data_Flow) is a free BPMN 2.0 modeling platform that can be extended and customized by any BPMN 2.0 compliant execution engine.
DF-BPMN is based on the [Eclipse Graphical Language Server Platform (GLSP)](https://www.eclipse.org/glsp/) and on [Open BPMN project](https://github.com/imixs/open-bpmn) providing an open-source framework for building diagram editors based on modern web technologies.

[*DataFlow BPMN paper*](https://www.researchgate.net/publication/371043087_Zooming_in_for_Clarity_Towards_low-code_modeling_for_Activity_Data_Flow)

[*DataFlow BPMN techninal report (how to use DF-BPMN)*](https://drive.google.com/file/d/1OlyvdmG6lZWu_PqOhf6OgEkZ6cwT6RPa/view?usp=sharing)

<h2 align="center"><a href="https://noureldin-ali.github.io/open-bpmn/toturial/" target="_blank">Start tutorial</a></h2>

<h2 align="center"><a href="http://172.171.161.217:3000/#/usr/src/app/workspace" target="_blank">Start the Online Demo</a></h2>

![Screenshot (20)](https://user-images.githubusercontent.com/61513661/225958345-bfc13903-fa42-45ee-b59c-dd783cbfb5ea.png)

## What is BPMN 2.0?

The [Business Process Model and Notation](https://www.omg.org/spec/BPMN/) (BPMN 2.0) is an open standard to describe business processes that can be visualized in diagram editors and executed by process engines compliant with the BPMN 2.0 standard. This makes BPMN an interoperable, interchangeable and open standard in the field of business process management.

BPMN was intended for users at all levels, from the business analysts who create the initial design, to the developers who implement the technical details, and finally, to the business users responsible for managing and monitoring the processes.

As a XML language proposed by the [Object Management Group](https://www.omg.org/spec/BPMN/) (OMG), BPMN
is not only a notation for describing business workflows but also higher-level collaborations between business partners and the choreography of information flows between applications, microservices and cloud platforms.


## Extensibility 

BPMN 2.0 introduces an extensibility mechanism that allows extending standard BPMN elements with additional properties and behavior. It can be used by modeling tools to add non-standard elements or Artifacts to satisfy a specific need, such as the unique requirements of a vertical domain, and still have a valid BPMN Core.

One goal of *DA BPMN* is to allow the developer to model BPMN includes all the data needed in each task.


## Architecture

DA BPMN is based on the [Eclipse Graphical Language Server Platform (GLSP)](https://www.eclipse.org/glsp/) and provides the following building blocks:

- [open-bpmn.metamodel](./open-bpmn.metamodel/README.md) - an open BPMN 2.0 metamodel
- [open-bpmn.glsp-server](./open-bpmn.glsp-server/README.md) - the GLSP Server implementation
- [open-bpmn.glsp-client](./open-bpmn.glsp-client/README.md) - the GLSP Client components and Theia integration

### DF-BPMN - BPMN 2.0 Metamodel

DF-BPMN provides a BPMN 2.0 Metamodel based on pure java. This library can be used to generate a BPMN model programmatically as also import or export a model form any .bpmn file. So in case you want to implement you own BPMN workflow engine the DF-BPMN Metamodel is the perfect library to work with BPMN 2.0 files. THe DF-BPMN Metamodel is based o the `org.w3c.dom` XML API and includes a set of junit test classes which may be helpful to implement you own business logic. 

 - [DF-BPMN Metamodel](./open-bpmn.metamodel/README.md)

- [OpenBPMN Metamodel](./open-bpmn.metamodel/README.md)

### DF-BPMN GLSP-Server

The [open-bpmn.glsp-server](./open-bpmn.glsp-server/README.md) provides the GLSP Server part. The server part is responsible to load and store the diagram from a .bpmn file.


### DF-BPMN GLSP-Client

The [DF-bpmn.glsp-client](./open-bpmn.glsp-client/README.md) is the GLSP Client part of DF-BPMN providing the graphical modeling tool. 

# Tutorials

# Requirement
## NodeJS 
### On linux
We use nodejs on Linux Debian during development. To manage version of nodejs in debian see: https://phoenixnap.com/kb/update-node-js-version

For development with Eclipse Theia the expected version is ">=10.11.0 <17". For that reason we tested with following version  16.11.0. You can list all current versions [here](https://nodejs.org/en/download/releases/). 

In case you have install npm you can install a specific nodejs version with:

	$ sudo n 16.11.0
 
 
To install typescript run:

	$ sudo npm install -g typescript 

### On winodws
We use nodejs 16.11.0 on windows 11 and windows 10. 

For development with Eclipse Theia the expected version is ">=10.11.0 <17". For that reason we tested with following version  16.11.0. You can list all current versions [here](https://nodejs.org/en/download/releases/). 

## Other requirements
In our build on widowns we use maven 3.8.6 and yarn 1.22.19.

You need to download [maven](https://maven.apache.org/download.cgi) and [yarn](https://classic.yarnpkg.com/lang/en/docs/install/#windows-stable) from official sites.


# Build and Run 

## On Linux
To build the complete project run 

    $ ./build.sh

This will build the server module with maven and the client modules with yarn. The script finally automatically starts the application.

The Application can be started from a Web Browser

    http://localhost:3000/

## On Windows

	$ mvn clean install  -DskipTests
	$ cd  open-bpmn.glsp-client/
	$ yarn
	$ yarn start 

The Application can be started from a Web Browser

	http://localhost:3000/

 ## openAI API
 	$ cd integration-openai
  	$ python app.py <api-key>
	
## Using docker

	$ docker pull alinoureldin/df-bpmn:latest
	$ docker run --name="df-bpmn" --rm -p 3000:3000 -p 3001:3001  --env OPENAI_KEY=<open-api-key> -it -v /path/to/bonita-workspace:/usr/src/app/bonita alinoureldin/df-bpmn

	
The Application can be started from a Web Browser

	http://localhost:3000/#/usr/src/app/workspace
	
More detail about build your own code in docker [here](./docker/README.md)

## Locally build for Development

During development you can run the frontend and backend in separate tasks. This gives you more control over the CLient and the Backend Component.

To build & start the GLSP Server only, run:

    $ ./build.sh -b

To build & start the GLSP Client only, run:

    $ ./build.sh -f

To start the GLSP Client without building, run:

    $ ./build.sh -s

For a full clean & reinstall of the GLSP Client (after upgrades), run:

    $ ./build.sh -c -i

You will find more details in the [Client Section](./open-bpmn.glsp-client/README.md) and the [Server Section](./open-bpmn.glsp-server/README.md).

## Development

DA BPMN is based on [Eclipse GLSP](https://www.eclipse.org/glsp/) and adapts the different concepts in various ways. The following sections provide details about the development with Eclipse GLSP and the solutions used in DA BPMN.

 - [Build your Own EMF Model](./doc/BPMN_EMF.md)
 - [Tool Palette](./doc/TOOL_PALETTE.md)
 - [Custom Element Views](./doc/CUSTOM_VIEWS.md)
 - [Ports](./doc/PORTS.md)
 
 
 

	
