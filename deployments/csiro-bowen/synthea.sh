#!/usr/bin/env bash
set -e

rsync -avz --delete master/ hb-15-cdc001.it.csiro.au:/opt/docker/clinsight
ssh hb-15-cdc001.it.csiro.au "docker run -v /opt/docker/clinsight/synthea/modules:/usr/src/synthea/src/main/resources/modules:ro -v /opt/docker/clinsight/synthea/synthea.properties:/usr/src/synthea/src/main/resources/synthea.properties:ro -v /mnt/cl_warehouse_1:/usr/share/warehouse --entrypoint=\"\" docker-registry.it.csiro.au/clinsight/synthea bash -c \"mkdir -p /usr/share/warehouse/synthea/$2 && /usr/src/synthea/run_synthea -p $1 --exporter.baseDirectory /usr/share/warehouse/synthea/$2\""

ssh hb-15-cdc001.it.csiro.au "docker run --entrypoint=\"\" -v /mnt/cl_warehouse_1:/usr/share/warehouse uhopper/hadoop-namenode:2.7.2 bash -c \"hadoop distcp /usr/share/warehouse/synthea/$2/fhir_stu3 hdfs://hb-15-cdc001.it.csiro.au:8020/staging/synthea/$2 && rm -rf /usr/share/warehouse/synthea\""