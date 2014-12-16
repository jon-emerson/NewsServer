#!/bin/bash

opennlp TokenNameFinderTrainer -type person -model opennlp/en-newsserver-person.bin -lang en -data trainingdata/en-newsserver-person.train -encoding UTF-8
opennlp TokenNameFinderTrainer -type organization -model opennlp/en-newsserver-organization.bin -lang en -data trainingdata/en-newsserver-organization.train -encoding UTF-8
opennlp TokenNameFinderTrainer -type location -model opennlp/en-newsserver-location.bin -lang en -data trainingdata/en-newsserver-location.train -encoding UTF-8

