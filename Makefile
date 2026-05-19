MAKEFLAGS += --no-print-directory

BINDIR := bin
JAR     := $(BINDIR)/pathkeeper.jar

.PHONY: all build test run run-gui clean install-jbang compat-test

all: build

build:
	mvn -q package -DskipTests
	@echo "Fat jar: $(JAR)"

test:
	mvn -q test

# Run the CLI from the fat jar
run: build
	java -jar $(JAR) $(ARGS)

# Launch the GUI from the fat jar
run-gui: build
	java -jar $(JAR) gui

clean:
	mvn -q clean
	@rm -f $(JAR)

# Install via jbang (requires jbang on PATH)
# Usage: make install-jbang
install-jbang:
	jbang app install --name pathkeeper pathkeeper.java

# Run the Python compatibility test suite against the built jar
# Requires: uv on PATH, and the pathkeeper (Python) repo at ../pathkeeper
# Usage: make compat-test
compat-test: build
	cd ../pathkeeper && PK="java -jar $(CURDIR)/$(JAR)" uv run pytest tests_compatibility/ -v
