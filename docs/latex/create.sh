pdflatex tudkom.tex
bibtex tudkom >> students.txt
makeglossaries tudkom
pdflatex tudkom.tex
pdflatex tudkom.tex
./clear.sh
