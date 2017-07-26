# Roma [![Build Status](https://travis-ci.org/pedrovgs/Roma.svg?branch=master)](https://travis-ci.org/pedrovgs/Roma)

[Apache Spark](https://spark.apache.org/) project written in [Scala](https://www.scala-lang.org/) used to perform real time sentiment analysis on top of Twitter's streaming API.

![SparkLogo](https://github.com/pedrovgs/SparkPlayground/raw/master/art/sparkLogo.png)

## Build and test this project

To build and test this project you can execute ``sbt test``. You can also use ``sbt`` interactive mode (you just have to execute ``sbt`` in your terminal) and then use the triggered execution to execute your tests using the following commands inside the interactive mode:

```
~ test // Runs every test in your project
~ test-only *AnySpec // Runs specs matching with the filter passed as param.
``` 

## Configuration

This project needs some private Twitter OAuth configuration you can set creating a new file named ``application.conf`` into the ``/src/main/resources/`` folder. Here you have an example:

```
twitter4j {
  oauth {
    consumerKey = YOUR_CONSUMER_KEY
    consumerSecret = YOUR_CONSUMER_SECRET
    accessToken = YOUR_ACCESS_TOKEN
    accessTokenSecret = YOUR_ACCESS_TOKEN_SECRET
  }
}

```

You can get these credentials by creating a Twitter application [here](https://apps.twitter.com/).

## Running on a cluster

Spark applications are developed to run on a cluster. Before to run your app you need to generate a ``.jar`` file you can submit to Spark to be executed. You can generate the ``roma.jar`` file executing ``sbt assembly``. This will generate a binary file you can submit using ``spark-submit`` command. Ensure your local Spark version is ``Spark 2.1.1``. 

You can submit this application to your local spark installation executing these commands:

``
sbt assembly
./submitToLocalSpark.sh
``

You can submit this application to a dockerized Spark cluster using these commands:

```
sbt assembly
cd docker
docker-compse up -d
cd ..
./submitToDockerizedSpark.sh
```

Developed By
------------

* Pedro Vicente Gómez Sánchez - <pedrovicente.gomez@gmail.com>

<a href="https://twitter.com/pedro_g_s">
  <img alt="Follow me on Twitter" src="https://image.freepik.com/iconos-gratis/twitter-logo_318-40209.jpg" height="60" width="60"/>
</a>
<a href="https://es.linkedin.com/in/pedrovgs">
  <img alt="Add me to Linkedin" src="https://image.freepik.com/iconos-gratis/boton-del-logotipo-linkedin_318-84979.png" height="60" width="60"/>
</a>

License
-------

    Copyright 2017 Pedro Vicente Gómez Sánchez

    Licensed under the GNU General Public License, Version 3 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.gnu.org/licenses/gpl-3.0.en.html

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

