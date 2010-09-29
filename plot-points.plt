#!/usr/bin/gnuplot -persist

plot "points.dat" using 1:2 with points, \
	"output.dat" using 1:2 with points