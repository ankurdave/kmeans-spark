export SPARK_HOME=/work/ankurd/spark

SCALA_SOURCES = *.scala
CLASSPATH = build/spark.jar:build/spark-dep.jar

ifeq ($(USE_FSC),1)
  COMPILER_NAME = fsc
else
  COMPILER_NAME = scalac
endif

ifeq ($(SCALA_HOME),)
  COMPILER = $(COMPILER_NAME)
else
  COMPILER = $(SCALA_HOME)/bin/$(COMPILER_NAME)
endif


all: jar

jar: build/kmeans-spark.jar

build/kmeans-spark.jar: build/spark.jar $(SCALA_SOURCES)
	mkdir -p build/classes
	$(COMPILER) -d build/classes -classpath build/classes:$(CLASSPATH) $(SCALA_SOURCES)
	jar cf build/kmeans-spark.jar -C build/classes .

build/spark.jar:
	make -C $(SPARK_HOME) jar
	mkdir -p build
	cp $(SPARK_HOME)/build/{spark,spark-dep}.jar build/


default: all

clean:
	rm -rf build

.phony: default all clean jar
