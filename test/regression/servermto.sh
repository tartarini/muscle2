#!/bin/bash

mto --topology mto-topology.cfg --config mto-config-$1.cfg --logFile server-$1.log --debug --MPWPath --logLevel TRACE --channels 64
