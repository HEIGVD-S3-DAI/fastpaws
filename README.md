# FastPaws

- [About](#about)
- [Installation](#installation)
- [Usage](#usage)
- [References](#references)
- [Authors](#authors)

## About

## Installation

Start by cloning the repository:

```bash
git clone https://github.com/HEIGVD-S3-DAI/fastpaws.git
```

Make sure you make java jdk>=21 installed on your machine and follow the steps below:

```bash
# Download the dependencies and their transitive dependencies
./mvnw dependency:go-offline
```

```bash
# Package the application
./mvnw package
```

## Usage

Optionally, create an alias to the jar application with the command below:

```bash
alias fastpaws="java -jar target/java-fastpaws-1.0-SNAPSHOT.jar"
```

To see a list of avaiable commands, run:

```bash
fastpaws -h
```

TODO

## References

- https://play.typeracer.com

## Authors

- Leonard Cseres [@leoanrdcser](https://github.com/leonardcser)
- Aude Laydu [@eau2](https://github.com/eau2)
