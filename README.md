# Introduction 
This is the code base of control plane extensions which uses the Controlplane(an EDC component) as dependency.

# Getting Started

# Build and Test
To build the image run the below command on top level of the repo
    docker build -t localhost:5432/edc-controlplane .
    docker push localhost:5432/edc-controlplane

# Contribute
Author :  Aastha Rai
To contribute in the project by writing extensions, use the folder extensions/controlplane/src/main/java/org/eclipse/edc/extensions
The same extension needs to be registered as a service in the extensions/controlplane/src/main/resources/META-INF/services - check the existing service for reference.