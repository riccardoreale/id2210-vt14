.PHONY : all clean pics

PDF := report.pdf

OUTDIR := ./out

LATEX := pdflatex
BIBTEX := bibtex
OPTS := -halt-on-error -interaction nonstopmode
OPTS += -output-directory $(OUTDIR)

all: $(PDF)

clean:
	rm -rf $(PDF) $(OUTDIR)

%.pdf : %.tex $(OUTDIR)
	$(LATEX) $(OPTS) $< &&\
	$(LATEX) $(OPTS) $< >/dev/null &&\
	$(LATEX) $(OPTS) $< >/dev/null

$(OUTDIR) :
	mkdir $@
	ln -s $@/$(PDF) $(PDF)
