#
# Makefile that runs tests that checks various chicory options.
#

# StackAr sources are used for option testing
DAIKONDIR ?= $(DAIKONDIR)
SRC_DIR = $(DAIKONDIR)/tests/sources
SRC_FILES = DataStructures/Comparable.java \
			DataStructures/Hashable.java \
			DataStructures/MyInteger.java \
			DataStructures/Overflow.java \
			DataStructures/StackAr.java \
			DataStructures/StackArTester.java \
			DataStructures/Underflow.java
CLASS_FILES = $(subst .java,.class,$(SRC_FILES))
TARGET_ARGS = DataStructures.StackArTester
DAIKON_ARGS = --no_text_output --config config.txt -o $@
CHICORY     = $(DAIKONDIR)/java/ChicoryPremain.jar
PREMAIN		= $(CHICORY)

# On cygwin, translate the premain path to windows format.  Only do so if
# this has not already been done (which is determined by looking for
# semicolons (:) in the name.  Only do this for PREMAIN, not for CHICORY
# as CHICORY is used by make which will expect unix format
ifeq (cygwin,$(OSTYPE))
  ifneq (;,$(findstring :,$(PREMAIN)))
    PREMAIN := '$(shell cygpath -w "$(PREMAIN)")'
  endif
endif


TESTS		= online pptsel pptomit nesting stdvis varomit samples misc

HERE := $(shell /bin/pwd)

DAIKONDIR ?= $(DAIKONDIR)

# Standard setup of CLASSPATH that works for Linux, Windows, and MacOSX
include $(DAIKONDIR)/tests/Makefile.classpath

diffs : $(foreach x, $(TESTS), $x.daikon.diff)

# obtain and compile the sources
compile : $(CLASS_FILES)
$(CLASS_FILES) :
	tar cf - -C $(SRC_DIR) $(SRC_FILES) | tar xf - -C $(HERE)
	javac -g `find . -name *.java`

online.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory \
	  --daikon-online --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

pptsel.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --ppt-select-pattern 'push|pop' \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

pptomit.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --ppt-omit-pattern 'push|pop' \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

nesting.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --nesting-depth 0 \
	  --daikon-online --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

stdvis.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --std-visibility \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

varomit.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --omit-var theArray \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

samples.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --sample-start=100 \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

misc.inv.gz : $(CLASS_FILES) $(CHICORY)
	java daikon.Chicory --purity-file=StackAr.pure \
	--premain=$(PREMAIN) --heap_size=100m \
	  --daikon --daikon-args '$(DAIKON_ARGS)' $(TARGET_ARGS)

%.daikon.diff : %.inv.gz %.inv.txt.goal
	java daikon.PrintInvariants $< > $*.inv.txt
	-diff -w -u $*.inv.txt.goal $*.inv.txt > $@
	@cat $@

update-goals:
	$(foreach x, $(TESTS), \
	  touch $x.inv.txt && cp $x.inv.txt $x.inv.txt.goal; )
	$(foreach x, $(TESTS), rm -f $x.daikon.diff; )

clean:
	/bin/rm -rf *.inv.txt *.inv.gz *.diff *.dtrace.gz \
	$(CLASS_FILES) $(SRC_FILES)

show-vars:
	@echo CLASS_FILES = $(CLASS_FILES)