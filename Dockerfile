FROM node:16-buster

# Install app dependencies
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev openjdk-11-jre

# Install base utilities
RUN apt-get update \
    && apt-get install -y build-essential \
    && apt-get install -y wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install miniconda
ENV CONDA_DIR /opt/conda
RUN wget --quiet https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
    /bin/bash ~/miniconda.sh -b -p /opt/conda

# Put conda in path so we can use conda activate
ENV PATH=$CONDA_DIR/bin:$PATH

#python library needed to working
RUN conda install langchain -c conda-forge
# RUN conda install -c conda-forge openai


# Python enviroment for the server connected to openAI
ARG API_KEY
ENV OPENAI_KEY=$API_KEY
ARG ORGA_KEY
ENV OPENAI_ORGA_KEY=$ORGA_KEY


# Downloading and installing Maven
# 1- Define a constant with the version of maven you want to install
ARG MAVEN_VERSION=3.8.8         

# 2- Define a constant with the working directory
ARG USER_HOME_DIR="/root"


# 4- Define the URL where maven can be downloaded from
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

# 5- Create the directories, download maven, validate the download, install it, remove downloaded file and set links
RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && echo "Downlaoding maven" \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  \
  && echo "Unziping maven" \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# 6- Define environmental variables required by Maven, like Maven_Home directory and where the maven repo is located
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN mvn --version

# Create app directory
# WORKDIR /usr/src/app/open-bpmn
WORKDIR /usr/src/app
RUN mkdir /usr/src/app/workspace

# Copy GLSP PROJECTS
# COPY . /usr/src/app/open-bpmn
RUN git clone https://github.com/NourEldin-Ali/open-bpmn.git

# BUILD GLSP Server part
WORKDIR /usr/src/app/open-bpmn/
RUN mvn clean install -DskipTest
WORKDIR /usr/src/app/open-bpmn/open-bpmn.glsp-server
RUN mvn clean install -DskipTest

RUN rm -r /usr/src/app/open-bpmn/open-bpmn.glsp-client/workspace/

# Build GLSP Client part
WORKDIR /usr/src/app/open-bpmn/open-bpmn.glsp-client
RUN yarn

# Copy Start script
# WORKDIR /usr/src/app
# COPY scripts/start.sh start.sh

EXPOSE 3000

ENTRYPOINT ["/usr/src/app/open-bpmn/scripts/start.sh"]
